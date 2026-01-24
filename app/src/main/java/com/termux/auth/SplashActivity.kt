package com.termux.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.termux.MainActivity
import com.termux.R
import com.termux.SetupWizard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash Activity - Entry point for MobileCLI Pro.
 *
 * Flow:
 * 1. Check if user has valid local license
 *    - Yes + not expired → Go to SetupWizard/MainActivity
 *    - Yes + needs verification → Verify online, then continue
 *    - No → Go to LoginActivity
 *
 * 2. If logged in but no Pro access → PaywallActivity
 * 3. If everything valid → SetupWizard (if first time) or MainActivity
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    private lateinit var licenseManager: LicenseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        licenseManager = LicenseManager(this)

        lifecycleScope.launch {
            // Brief delay for splash to show
            delay(500)
            checkAuthStatus()
        }
    }

    private suspend fun checkAuthStatus() {
        Log.i(TAG, "Checking auth status...")

        // Step 1: Check if we have a valid local license
        if (licenseManager.hasValidLocalLicense()) {
            Log.i(TAG, "Valid local license found")

            // Check if it needs periodic verification
            if (licenseManager.needsVerification()) {
                Log.i(TAG, "License needs verification")
                verifyAndProceed()
            } else {
                // License is valid and fresh - proceed to app
                proceedToApp()
            }
        } else {
            // No valid license - check if logged in
            Log.i(TAG, "No valid local license")

            if (SupabaseClient.isLoggedIn()) {
                Log.i(TAG, "User is logged in, forcing fresh server check for subscription")
                // Force fresh check from server - bypass any stale cache
                val result = licenseManager.forceVerifyLicense()
                if (result.isSuccess) {
                    val license = result.getOrNull()!!
                    Log.i(TAG, "Server check result: tier=${license.tier}, isPro=${license.isPro()}")
                    if (license.isPro()) {
                        // Has Pro access - proceed to app
                        proceedToApp()
                    } else {
                        // No Pro access - show paywall (trial or expired)
                        goToPaywall()
                    }
                } else {
                    // Server check failed - show paywall
                    Log.w(TAG, "Server check failed: ${result.exceptionOrNull()?.message}")
                    goToPaywall()
                }
            } else {
                // Not logged in - go to login
                Log.i(TAG, "User not logged in, going to login")
                goToLogin()
            }
        }
    }

    private suspend fun verifyAndProceed() {
        try {
            val result = licenseManager.verifyLicense()

            if (result.isSuccess) {
                val license = result.getOrNull()!!
                Log.i(TAG, "License verified: tier=${license.tier}")

                if (license.isPro() || (license.tier == "free" && !license.isExpired())) {
                    proceedToApp()
                } else {
                    // License expired or invalid
                    goToPaywall()
                }
            } else {
                // Verification failed (network error, etc.)
                // Allow offline use if license not too old
                val license = licenseManager.getLicenseInfo()
                if (license != null && !license.isExpired()) {
                    Log.i(TAG, "Verification failed but license still valid, allowing offline use")
                    proceedToApp()
                } else {
                    goToLogin()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Verification error", e)
            // Allow offline use if we have any license
            if (licenseManager.hasValidLocalLicense()) {
                proceedToApp()
            } else {
                goToLogin()
            }
        }
    }

    private fun proceedToApp() {
        Log.i(TAG, "Proceeding to app")

        // Check if setup wizard needs to run
        if (!SetupWizard.isSetupComplete(this)) {
            startActivity(Intent(this, SetupWizard::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun goToLogin() {
        Log.i(TAG, "Going to login")
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun goToPaywall() {
        Log.i(TAG, "Going to paywall")
        startActivity(Intent(this, PaywallActivity::class.java))
        finish()
    }
}
