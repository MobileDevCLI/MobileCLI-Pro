package com.termux.auth

import android.util.Log
import android.content.Intent
import android.net.Uri
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.FlowType
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

private const val TAG = "SupabaseClient"

/**
 * Supabase client singleton for MobileCLI Pro.
 * Handles authentication and database access.
 */
object SupabaseClient {

    // Supabase project credentials
    // These are safe to include in client-side code (anon key has RLS protection)
    private const val SUPABASE_URL = "https://mwxlguqukyfberyhtkmg.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im13eGxndXF1a3lmYmVyeWh0a21nIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njc0OTg5ODgsImV4cCI6MjA4MzA3NDk4OH0.VdpU9WzYpTyLeVX9RaXKBP3dNNNf0t9YkQfVf7x_TA8"

    private var _client: io.github.jan.supabase.SupabaseClient? = null

    val client: io.github.jan.supabase.SupabaseClient
        get() {
            if (_client == null) {
                Log.i(TAG, "Initializing Supabase client...")
                try {
                    _client = createSupabaseClient(
                        supabaseUrl = SUPABASE_URL,
                        supabaseKey = SUPABASE_ANON_KEY
                    ) {
                        install(Auth) {
                            // Use PKCE flow for mobile OAuth
                            flowType = FlowType.PKCE
                            // Configure custom scheme for redirect
                            scheme = "com.termux"
                            host = "login-callback"
                        }
                        install(Postgrest) {
                            // Configure postgrest settings
                        }
                    }
                    Log.i(TAG, "Supabase client initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize Supabase client", e)
                    throw e
                }
            }
            return _client!!
        }

    val auth get() = client.auth
    val db get() = client.postgrest

    /**
     * Sign up with email and password.
     */
    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with email and password.
     */
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google OAuth (browser-based flow).
     * Returns the OAuth URL to open in browser.
     */
    suspend fun signInWithGoogle(): Result<Unit> {
        return try {
            auth.signInWith(Google)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get the OAuth URL for a provider.
     * Use this for browser-based OAuth flow.
     */
    suspend fun getGoogleOAuthUrl(redirectUrl: String): String {
        return try {
            // Build OAuth URL manually for PKCE flow
            val baseUrl = "${SUPABASE_URL}/auth/v1/authorize"
            val params = buildString {
                append("provider=google")
                append("&redirect_to=${Uri.encode(redirectUrl)}")
                append("&flowType=pkce")
            }
            "$baseUrl?$params"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build OAuth URL", e)
            throw e
        }
    }

    /**
     * Sign out the current user.
     */
    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
        }
    }

    /**
     * Get current user ID if logged in.
     */
    fun getCurrentUserId(): String? {
        return try {
            auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user ID", e)
            null
        }
    }

    /**
     * Check if user is logged in.
     */
    fun isLoggedIn(): Boolean {
        return try {
            auth.currentUserOrNull() != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check login status", e)
            false
        }
    }

    /**
     * Get current user's email.
     */
    fun getCurrentUserEmail(): String? {
        return try {
            auth.currentUserOrNull()?.email
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user email", e)
            null
        }
    }

    /**
     * Handle OAuth deep link callback.
     * Call this when receiving a deep link intent.
     * For PKCE flow, extracts the code parameter and exchanges it for a session.
     */
    suspend fun handleDeepLink(uri: Uri): Boolean {
        return try {
            Log.i(TAG, "Handling deep link: $uri")

            // For PKCE flow, the callback contains a 'code' parameter
            val code = uri.getQueryParameter("code")
            if (code != null) {
                Log.i(TAG, "Found authorization code, exchanging for session...")
                auth.exchangeCodeForSession(code)
                Log.i(TAG, "Session exchange complete, user logged in: ${isLoggedIn()}")
                return isLoggedIn()
            }

            // Check for error in callback
            val error = uri.getQueryParameter("error")
            val errorDescription = uri.getQueryParameter("error_description")
            if (error != null) {
                Log.e(TAG, "OAuth error: $error - $errorDescription")
                return false
            }

            // For implicit flow (fragment-based tokens)
            val fragment = uri.fragment
            if (fragment != null && fragment.contains("access_token")) {
                Log.i(TAG, "Found access token in fragment, importing session...")
                auth.importAuthToken(uri.getQueryParameter("access_token") ?: "")
                return isLoggedIn()
            }

            Log.w(TAG, "No code, token, or error found in deep link")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle deep link", e)
            false
        }
    }

    /**
     * Handle OAuth deep link from Intent.
     */
    suspend fun handleDeepLink(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        return handleDeepLink(uri)
    }
}
