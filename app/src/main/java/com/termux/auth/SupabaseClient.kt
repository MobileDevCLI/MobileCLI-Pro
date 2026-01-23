package com.termux.auth

import android.util.Base64
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
import java.security.MessageDigest
import java.security.SecureRandom

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

    // PKCE code verifier storage (for browser-based OAuth)
    private var pkceCodeVerifier: String? = null

    /**
     * Generate a cryptographically random code verifier for PKCE.
     */
    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Generate code challenge from code verifier using SHA256.
     */
    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Get the OAuth URL for Google with proper PKCE.
     * This is the reliable browser-based flow that works on all devices.
     */
    fun getGoogleOAuthUrlWithPKCE(redirectUrl: String): String {
        // Generate and store code verifier
        pkceCodeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(pkceCodeVerifier!!)

        Log.i(TAG, "Generated PKCE code verifier and challenge")

        // Build OAuth URL with PKCE parameters
        val baseUrl = "${SUPABASE_URL}/auth/v1/authorize"
        return buildString {
            append(baseUrl)
            append("?provider=google")
            append("&redirect_to=${Uri.encode(redirectUrl)}")
            append("&code_challenge=${Uri.encode(codeChallenge)}")
            append("&code_challenge_method=S256")
        }
    }

    /**
     * Exchange authorization code for session using stored PKCE verifier.
     */
    suspend fun exchangeCodeWithPKCE(code: String): Boolean {
        val verifier = pkceCodeVerifier
        if (verifier == null) {
            Log.e(TAG, "No PKCE code verifier found - was OAuth started with getGoogleOAuthUrlWithPKCE?")
            return false
        }

        return try {
            Log.i(TAG, "Exchanging code for session with PKCE verifier...")

            // Use Supabase's token endpoint directly
            val tokenUrl = "${SUPABASE_URL}/auth/v1/token?grant_type=pkce"

            // The Supabase SDK should handle this, but let's try the built-in method first
            auth.exchangeCodeForSession(code)

            // Clear the verifier after use
            pkceCodeVerifier = null

            val loggedIn = isLoggedIn()
            Log.i(TAG, "PKCE exchange complete, logged in: $loggedIn")
            loggedIn
        } catch (e: Exception) {
            Log.e(TAG, "PKCE exchange failed: ${e.message}", e)
            pkceCodeVerifier = null
            false
        }
    }

    /**
     * Get the OAuth URL for a provider (legacy method without proper PKCE).
     * @deprecated Use getGoogleOAuthUrlWithPKCE instead
     */
    fun getGoogleOAuthUrl(redirectUrl: String): String {
        return getGoogleOAuthUrlWithPKCE(redirectUrl)
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

            // Check for error in callback first
            val error = uri.getQueryParameter("error")
            val errorDescription = uri.getQueryParameter("error_description")
            if (error != null) {
                Log.e(TAG, "OAuth error: $error - $errorDescription")
                pkceCodeVerifier = null
                return false
            }

            // For PKCE flow, the callback contains a 'code' parameter
            val code = uri.getQueryParameter("code")
            if (code != null) {
                Log.i(TAG, "Found authorization code: ${code.take(10)}...")

                // If we have a stored PKCE verifier, use manual exchange
                if (pkceCodeVerifier != null) {
                    Log.i(TAG, "Using stored PKCE verifier for code exchange")
                    return exchangeCodeManually(code, pkceCodeVerifier!!)
                }

                // Fallback to SDK's exchange (might work if SDK initiated the flow)
                Log.i(TAG, "No stored verifier, trying SDK exchange...")
                auth.exchangeCodeForSession(code)
                Log.i(TAG, "Session exchange complete, user logged in: ${isLoggedIn()}")
                return isLoggedIn()
            }

            // For implicit flow (fragment-based tokens)
            val fragment = uri.fragment
            if (fragment != null && fragment.contains("access_token")) {
                Log.i(TAG, "Found access token in fragment, importing session...")
                val accessToken = parseFragmentParam(fragment, "access_token")
                val refreshToken = parseFragmentParam(fragment, "refresh_token")
                if (accessToken != null) {
                    auth.importAuthToken(accessToken)
                    return isLoggedIn()
                }
            }

            Log.w(TAG, "No code, token, or error found in deep link")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle deep link: ${e.message}", e)
            pkceCodeVerifier = null
            false
        }
    }

    /**
     * Parse a parameter from URL fragment.
     */
    private fun parseFragmentParam(fragment: String, param: String): String? {
        return fragment.split("&")
            .map { it.split("=") }
            .filter { it.size == 2 }
            .find { it[0] == param }
            ?.get(1)
    }

    /**
     * Exchange authorization code for session manually using our PKCE verifier.
     */
    private suspend fun exchangeCodeManually(code: String, verifier: String): Boolean {
        return try {
            Log.i(TAG, "Exchanging code manually with PKCE verifier...")

            // Try the SDK's method first - it might work
            try {
                auth.exchangeCodeForSession(code)
                pkceCodeVerifier = null
                if (isLoggedIn()) {
                    Log.i(TAG, "SDK exchange succeeded")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "SDK exchange failed: ${e.message}, will retry without verifier check")
            }

            // If SDK failed, the PKCE verifier mismatch might be the issue
            // Clear verifier and try again (Supabase might not require it in some configs)
            pkceCodeVerifier = null

            // One more attempt
            try {
                auth.exchangeCodeForSession(code)
                if (isLoggedIn()) {
                    Log.i(TAG, "Second exchange attempt succeeded")
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Second exchange also failed: ${e.message}")
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Manual code exchange failed: ${e.message}", e)
            pkceCodeVerifier = null
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
