package com.termux.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.termux.MainActivity
import com.termux.R
import com.termux.SetupWizard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Paywall Activity for MobileCLI Pro.
 *
 * Shows subscription options and handles:
 * - Free trial (7 days)
 * - Pro subscription ($15/month)
 * - Redirects to Stripe checkout via website
 */
class PaywallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PaywallActivity"

        // Stripe checkout URL (handled by your website)
        private const val CHECKOUT_URL = "https://mobilecli.com/checkout"
        private const val PRICING_URL = "https://mobilecli.com/pricing.html"

        fun start(context: Context) {
            val intent = Intent(context, PaywallActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    private lateinit var licenseManager: LicenseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paywall)

        licenseManager = LicenseManager(this)

        setupUI()
    }

    private fun setupUI() {
        // Start Trial Button
        findViewById<Button>(R.id.start_trial_button).setOnClickListener {
            startFreeTrial()
        }

        // Subscribe Button
        findViewById<Button>(R.id.subscribe_button).setOnClickListener {
            openCheckout()
        }

        // Restore Purchase link
        findViewById<TextView>(R.id.restore_purchase).setOnClickListener {
            restorePurchase()
        }

        // Update trial info based on license
        updateTrialInfo()
    }

    private fun updateTrialInfo() {
        val license = licenseManager.getLicenseInfo()

        if (license != null && license.tier == "free") {
            val daysLeft = license.daysUntilExpiry()
            if (daysLeft > 0) {
                findViewById<TextView>(R.id.trial_info).text =
                    "You have $daysLeft days left in your trial"
            } else {
                findViewById<TextView>(R.id.trial_info).text =
                    "Your trial has expired"
                findViewById<Button>(R.id.start_trial_button).visibility = View.GONE
            }
        }
    }

    private fun startFreeTrial() {
        // Already registered during login - license has 7 day expiry
        // Just proceed to the app
        Toast.makeText(this, "Starting your 7-day free trial!", Toast.LENGTH_SHORT).show()
        proceedToApp()
    }

    private fun openCheckout() {
        // Get user email for checkout prefill
        val email = SupabaseClient.getCurrentUserEmail() ?: ""
        val userId = SupabaseClient.getCurrentUserId() ?: ""

        // Build checkout URL with user info
        val checkoutUrl = Uri.parse(PRICING_URL)
            .buildUpon()
            .appendQueryParameter("email", email)
            .appendQueryParameter("user_id", userId)
            .build()
            .toString()

        // Open in Custom Tab (better UX than WebView)
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(this, Uri.parse(checkoutUrl))
        } catch (e: Exception) {
            // Fallback to browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
            startActivity(intent)
        }

        // When they come back, we'll check subscription status
        Toast.makeText(this, "Complete payment in browser, then return here", Toast.LENGTH_LONG).show()
    }

    private fun restorePurchase() {
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Re-verify license from server
                val result = licenseManager.verifyLicense()

                if (result.isSuccess) {
                    val license = result.getOrNull()!!
                    if (license.isPro()) {
                        Toast.makeText(
                            this@PaywallActivity,
                            "Subscription restored!",
                            Toast.LENGTH_SHORT
                        ).show()
                        proceedToApp()
                    } else {
                        Toast.makeText(
                            this@PaywallActivity,
                            "No active subscription found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@PaywallActivity,
                        "Could not restore: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                Toast.makeText(
                    this@PaywallActivity,
                    "Restore failed. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun proceedToApp() {
        // Check if setup is complete
        if (!SetupWizard.isSetupComplete(this)) {
            startActivity(Intent(this, SetupWizard::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    override fun onResume() {
        super.onResume()

        // Check if user subscribed while in browser
        lifecycleScope.launch {
            delay(500) // Brief delay to ensure Stripe webhook processed

            val result = licenseManager.verifyLicense()
            if (result.isSuccess) {
                val license = result.getOrNull()!!
                if (license.isPro()) {
                    Toast.makeText(
                        this@PaywallActivity,
                        "Subscription activated!",
                        Toast.LENGTH_SHORT
                    ).show()
                    proceedToApp()
                }
            }
        }
    }

    override fun onBackPressed() {
        // Don't allow back from paywall - must subscribe or start trial
        Toast.makeText(this, "Please start a trial or subscribe to continue", Toast.LENGTH_SHORT).show()
    }
}
