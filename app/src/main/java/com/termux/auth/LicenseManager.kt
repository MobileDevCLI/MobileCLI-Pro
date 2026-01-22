package com.termux.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date

/**
 * Manages license verification and storage for MobileCLI Pro.
 *
 * Flow:
 * 1. User logs in
 * 2. App registers device with Supabase
 * 3. Supabase returns license key + expiration
 * 4. License stored locally (encrypted)
 * 5. App works offline using local license
 * 6. Every 30 days, re-verify when online
 */
class LicenseManager(private val context: Context) {

    companion object {
        private const val TAG = "LicenseManager"
        private const val PREFS_NAME = "mobilecli_license"
        private const val KEY_LICENSE_KEY = "license_key"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_TIER = "tier"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_LAST_VERIFIED = "last_verified"
        private const val KEY_DEVICE_ID = "device_id"

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
     * Get unique device ID for this installation.
     */
    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            // Generate a unique device ID
            deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    /**
     * Get device name for display.
     */
    fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    /**
     * Check if user has a valid local license.
     * Returns true if license exists and hasn't expired.
     */
    fun hasValidLocalLicense(): Boolean {
        val licenseKey = prefs.getString(KEY_LICENSE_KEY, null) ?: return false
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)

        // Check if license has expired
        if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            Log.i(TAG, "Local license has expired")
            return false
        }

        return licenseKey.isNotEmpty()
    }

    /**
     * Check if license needs re-verification (every 30 days).
     */
    fun needsVerification(): Boolean {
        val lastVerified = prefs.getLong(KEY_LAST_VERIFIED, 0)
        val elapsed = System.currentTimeMillis() - lastVerified
        return elapsed > VERIFICATION_INTERVAL
    }

    /**
     * Get stored license info.
     */
    fun getLicenseInfo(): LicenseInfo? {
        val licenseKey = prefs.getString(KEY_LICENSE_KEY, null) ?: return null
        return LicenseInfo(
            licenseKey = licenseKey,
            userId = prefs.getString(KEY_USER_ID, null) ?: "",
            userEmail = prefs.getString(KEY_USER_EMAIL, null) ?: "",
            tier = prefs.getString(KEY_TIER, "free") ?: "free",
            expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0),
            lastVerified = prefs.getLong(KEY_LAST_VERIFIED, 0)
        )
    }

    /**
     * Register this device with Supabase and get a license.
     * Called after successful login.
     */
    suspend fun registerDevice(): Result<LicenseInfo> = withContext(Dispatchers.IO) {
        try {
            val userId = SupabaseClient.getCurrentUserId()
                ?: return@withContext Result.failure(Exception("Not logged in"))

            val userEmail = SupabaseClient.getCurrentUserEmail() ?: ""
            val deviceId = getDeviceId()
            val deviceName = getDeviceName()

            Log.i(TAG, "Registering device: $deviceId ($deviceName)")

            // Call the register_device function in Supabase
            val result = SupabaseClient.db.rpc(
                "register_device",
                mapOf(
                    "p_user_id" to userId,
                    "p_device_id" to deviceId,
                    "p_device_name" to deviceName
                )
            ).decodeAs<JsonObject>()

            val success = result["success"]?.jsonPrimitive?.boolean ?: false
            if (!success) {
                val error = result["error"]?.jsonPrimitive?.content ?: "Unknown error"
                return@withContext Result.failure(Exception(error))
            }

            val licenseKey = result["license_key"]?.jsonPrimitive?.content ?: ""
            val tier = result["tier"]?.jsonPrimitive?.content ?: "free"

            // For free tier, license expires in 7 days (trial)
            // For paid tier, it's set by subscription period end
            val expiresAt = if (tier == "free") {
                System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days trial
            } else {
                // Parse from result if available, otherwise 30 days
                System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000)
            }

            // Store license locally
            val licenseInfo = LicenseInfo(
                licenseKey = licenseKey,
                userId = userId,
                userEmail = userEmail,
                tier = tier,
                expiresAt = expiresAt,
                lastVerified = System.currentTimeMillis()
            )
            saveLicense(licenseInfo)

            Log.i(TAG, "Device registered successfully. Tier: $tier, License: $licenseKey")
            Result.success(licenseInfo)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to register device", e)
            Result.failure(e)
        }
    }

    /**
     * Verify the current license with Supabase.
     * Called periodically (every 30 days) when online.
     */
    suspend fun verifyLicense(): Result<LicenseInfo> = withContext(Dispatchers.IO) {
        try {
            val licenseKey = prefs.getString(KEY_LICENSE_KEY, null)
                ?: return@withContext Result.failure(Exception("No license stored"))

            val deviceId = getDeviceId()

            Log.i(TAG, "Verifying license: $licenseKey")

            // Call the verify_license function in Supabase
            val result = SupabaseClient.db.rpc(
                "verify_license",
                mapOf(
                    "p_license_key" to licenseKey,
                    "p_device_id" to deviceId
                )
            ).decodeAs<JsonObject>()

            val valid = result["valid"]?.jsonPrimitive?.boolean ?: false
            if (!valid) {
                val error = result["error"]?.jsonPrimitive?.content ?: "License invalid"
                clearLicense()
                return@withContext Result.failure(Exception(error))
            }

            val tier = result["tier"]?.jsonPrimitive?.content ?: "free"
            val userId = result["user_id"]?.jsonPrimitive?.content ?: ""

            // Update stored license
            val licenseInfo = LicenseInfo(
                licenseKey = licenseKey,
                userId = userId,
                userEmail = prefs.getString(KEY_USER_EMAIL, null) ?: "",
                tier = tier,
                expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0),
                lastVerified = System.currentTimeMillis()
            )
            saveLicense(licenseInfo)

            Log.i(TAG, "License verified successfully. Tier: $tier")
            Result.success(licenseInfo)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify license", e)
            // Don't clear license on network error - allow offline use
            Result.failure(e)
        }
    }

    /**
     * Save license to encrypted storage.
     */
    private fun saveLicense(license: LicenseInfo) {
        prefs.edit()
            .putString(KEY_LICENSE_KEY, license.licenseKey)
            .putString(KEY_USER_ID, license.userId)
            .putString(KEY_USER_EMAIL, license.userEmail)
            .putString(KEY_TIER, license.tier)
            .putLong(KEY_EXPIRES_AT, license.expiresAt)
            .putLong(KEY_LAST_VERIFIED, license.lastVerified)
            .apply()
    }

    /**
     * Clear stored license (on logout or invalid license).
     */
    fun clearLicense() {
        prefs.edit()
            .remove(KEY_LICENSE_KEY)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_TIER)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_LAST_VERIFIED)
            .apply()
    }

    /**
     * Check if user has Pro tier access.
     */
    fun hasProAccess(): Boolean {
        val tier = prefs.getString(KEY_TIER, "free")
        return tier == "pro" || tier == "team"
    }

    /**
     * Check if user is in trial period.
     */
    fun isInTrial(): Boolean {
        val tier = prefs.getString(KEY_TIER, "free")
        return tier == "free" && hasValidLocalLicense()
    }
}

/**
 * Data class representing license information.
 */
data class LicenseInfo(
    val licenseKey: String,
    val userId: String,
    val userEmail: String,
    val tier: String,  // "free", "pro", "team"
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
