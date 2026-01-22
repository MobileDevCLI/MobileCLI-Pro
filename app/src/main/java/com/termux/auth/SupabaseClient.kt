package com.termux.auth

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

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
                            // Configure auth settings
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
     * Sign in with Google OAuth.
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
}
