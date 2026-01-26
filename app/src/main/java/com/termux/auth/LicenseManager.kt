package com.termux.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Manages subscription/license verification for MobileCLI Pro.
 *
 * PRIVACY-FOCUSED: Only stores minimal data needed for license verification.
 * - User ID (from Supabase auth)
 * - Subscription status (active/trial/expired)
 * - Expiry date
 *
 * NO personal data, NO payment details stored locally.
 *
 * Flow:
 * 1. User logs in via Supabase Auth
 * 2. App checks subscription status from Supabase
 * 3. Status cached locally (encrypted) for offline use
 * 4. Re-verify every 30 days when online
 */
class LicenseManager(private val context: Context) {

    companion object {
        private const val TAG = "LicenseManager"
        private const val PREFS_NAME = "mobilecli_license"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_STATUS = "status"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_LAST_VERIFIED = "last_verified"

        // Re-verify every 30 days (in milliseconds)
        private const val VERIFICATION_INTERVAL = 30L * 24 * 60 * 60 * 1000
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create encrypted prefs, falling back to regular", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Subscription data from Supabase.
     * Must include ALL columns from database to avoid JSON parsing errors.
     */
    @Serializable
    data class Subscription(
        val id: String? = null,
        val user_id: String,
        val status: String,
        val paypal_subscription_id: String? = null,
        val paypal_payer_id: String? = null,
        val created_at: String? = null,
        val updated_at: String? = null,
        val expires_at: String? = null,
        val trial_started_at: String? = null,
        val trial_reminder_sent: Boolean? = null,
        val payment_failed_at: String? = null,
        val last_payment_at: String? = null,
        val cancelled_at: String? = null,
        val cancel_reason: String? = null,
        val admin_notes: String? = null
    )

    /**
     * Check subscription status from Supabase and cache locally.
     * Call this after login.
     */
    suspend fun checkSubscription(): Result<SubscriptionStatus> = withContext(Dispatchers.IO) {
        try {
            val userId = SupabaseClient.getCurrentUserId()
                ?: return@withContext Result.failure(Exception("Not logged in"))

            Log.i(TAG, "Checking subscription for user: $userId")

            // Query subscriptions table
            val subscriptions = SupabaseClient.client.postgrest
                .from("subscriptions")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Subscription>()

            val subscription = subscriptions.firstOrNull()

            if (subscription == null) {
                // No subscription found - this shouldn't happen if trigger works
                // Create a trial status locally
                Log.w(TAG, "No subscription found, creating local trial")
                val trialStatus = SubscriptionStatus(
                    userId = userId,
                    status = "trial",
                    expiresAt = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
                    lastVerified = System.currentTimeMillis()
                )
                saveStatus(trialStatus)
                return@withContext Result.success(trialStatus)
            }

            // Parse expiry date (ISO 8601 format from Supabase)
            val expiresAt = subscription.expires_at?.let {
                try {
                    // Parse ISO 8601 timestamp (e.g., "2026-01-29T23:13:39.000Z")
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    // Remove milliseconds and timezone suffix if present
                    val cleanDate = it.replace(Regex("\\.[0-9]+Z?$"), "").replace("Z", "")
                    sdf.parse(cleanDate)?.time ?: throw Exception("Parse failed")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse expires_at: $it", e)
                    // Default: 7 days for trial, 30 days for active
                    if (subscription.status == "active") {
                        System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
                    } else {
                        System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)
                    }
                }
            } ?: System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)

            val status = SubscriptionStatus(
                userId = userId,
                status = subscription.status,
                expiresAt = expiresAt,
                lastVerified = System.currentTimeMillis()
            )

            // Cache locally
            saveStatus(status)

            Log.i(TAG, "Subscription verified: status=${status.status}, expires=${status.expiresAt}")
            Result.success(status)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to check subscription", e)

            // If offline, return cached status
            val cached = getCachedStatus()
            if (cached != null) {
                Log.i(TAG, "Using cached subscription status")
                return@withContext Result.success(cached)
            }

            Result.failure(e)
        }
    }

    /**
     * Get cached subscription status (for offline use).
     */
    fun getCachedStatus(): SubscriptionStatus? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val status = prefs.getString(KEY_STATUS, null) ?: return null

        return SubscriptionStatus(
            userId = userId,
            status = status,
            expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0),
            lastVerified = prefs.getLong(KEY_LAST_VERIFIED, 0)
        )
    }

    /**
     * Check if user has valid access (active or valid trial).
     */
    fun hasValidAccess(): Boolean {
        val status = getCachedStatus() ?: return false

        // Active subscription - always valid
        if (status.status == "active") {
            return true
        }

        // Trial - check if not expired
        if (status.status == "trial" && !status.isExpired()) {
            return true
        }

        return false
    }

    /**
     * Check if user has paid Pro access.
     */
    fun hasProAccess(): Boolean {
        val status = getCachedStatus() ?: return false
        return status.status == "active"
    }

    /**
     * Check if user is in trial period.
     */
    fun isInTrial(): Boolean {
        val status = getCachedStatus() ?: return false
        return status.status == "trial" && !status.isExpired()
    }

    /**
     * Check if subscription needs re-verification.
     */
    fun needsVerification(): Boolean {
        val lastVerified = prefs.getLong(KEY_LAST_VERIFIED, 0)
        val elapsed = System.currentTimeMillis() - lastVerified
        return elapsed > VERIFICATION_INTERVAL
    }

    /**
     * Save subscription status to encrypted local storage.
     */
    private fun saveStatus(status: SubscriptionStatus) {
        prefs.edit()
            .putString(KEY_USER_ID, status.userId)
            .putString(KEY_STATUS, status.status)
            .putLong(KEY_EXPIRES_AT, status.expiresAt)
            .putLong(KEY_LAST_VERIFIED, status.lastVerified)
            .apply()
    }

    /**
     * Clear cached subscription (on logout).
     */
    fun clearCache() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_STATUS)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_LAST_VERIFIED)
            .apply()
    }

    /**
     * Force a fresh server check, bypassing all cache.
     * Use this after payment or when "Restore Purchase" is clicked.
     */
    suspend fun forceServerCheck(): Result<SubscriptionStatus> = withContext(Dispatchers.IO) {
        // Clear all cached data first
        clearCache()

        Log.i(TAG, "Force server check - cache cleared, checking subscription...")

        // Now check subscription fresh from server
        checkSubscription()
    }

    /**
     * For backward compatibility with existing code.
     */
    fun hasValidLocalLicense(): Boolean = hasValidAccess()

    fun getLicenseInfo(): LicenseInfo? {
        val status = getCachedStatus() ?: return null
        return LicenseInfo(
            licenseKey = "", // Not used anymore
            userId = status.userId,
            userEmail = SupabaseClient.getCurrentUserEmail() ?: "",
            tier = if (status.status == "active") "pro" else "free",
            expiresAt = status.expiresAt,
            lastVerified = status.lastVerified
        )
    }

    suspend fun verifyLicense(): Result<LicenseInfo> {
        val result = checkSubscription()
        return result.map { status ->
            LicenseInfo(
                licenseKey = "",
                userId = status.userId,
                userEmail = SupabaseClient.getCurrentUserEmail() ?: "",
                tier = if (status.status == "active") "pro" else "free",
                expiresAt = status.expiresAt,
                lastVerified = status.lastVerified
            )
        }
    }

    /**
     * Force verify license from server, bypassing cache.
     * Use this for "Restore Purchase" and post-payment checks.
     */
    suspend fun forceVerifyLicense(): Result<LicenseInfo> {
        val result = forceServerCheck()
        return result.map { status ->
            LicenseInfo(
                licenseKey = "",
                userId = status.userId,
                userEmail = SupabaseClient.getCurrentUserEmail() ?: "",
                tier = if (status.status == "active") "pro" else "free",
                expiresAt = status.expiresAt,
                lastVerified = status.lastVerified
            )
        }
    }

    /**
     * Register device and get license info.
     * Alias for verifyLicense() for backward compatibility.
     */
    suspend fun registerDevice(): Result<LicenseInfo> = verifyLicense()

    fun clearLicense() = clearCache()
}

/**
 * Subscription status data.
 */
data class SubscriptionStatus(
    val userId: String,
    val status: String,  // "active", "trial", "cancelled", "expired"
    val expiresAt: Long,
    val lastVerified: Long
) {
    fun isExpired(): Boolean = expiresAt > 0 && System.currentTimeMillis() > expiresAt
    fun isActive(): Boolean = status == "active"
    fun daysUntilExpiry(): Int {
        val remaining = expiresAt - System.currentTimeMillis()
        return (remaining / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    }
}

/**
 * Legacy license info for backward compatibility.
 */
data class LicenseInfo(
    val licenseKey: String,
    val userId: String,
    val userEmail: String,
    val tier: String,
    val expiresAt: Long,
    val lastVerified: Long
) {
    fun isExpired(): Boolean = expiresAt > 0 && System.currentTimeMillis() > expiresAt
    fun isPro(): Boolean = tier == "pro" || tier == "team"
    fun daysUntilExpiry(): Int {
        val remaining = expiresAt - System.currentTimeMillis()
        return (remaining / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    }
}
