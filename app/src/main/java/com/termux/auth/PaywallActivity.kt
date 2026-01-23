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
import androidx.activity.OnBackPressedCallback
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
 * - Pro subscription ($15/month via PayPal)
 * - Redirects to PayPal subscription checkout
 */
class PaywallActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PaywallActivity"

        // PayPal subscription URL (Subscription Plan)
        private const val PAYPAL_PLAN_ID = "P-3RH33892X5467024SNFZON2Y"
        private const val PAYPAL_SUBSCRIBE_URL = "https://www.paypal.com/webapps/billing/plans/subscribe?plan_id=$PAYPAL_PLAN_ID"

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
        setupBackHandler()
    }

    private fun setupBackHandler() {
        // Modern back press handling (replaces deprecated onBackPressed)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Don't allow back from paywall - must subscribe or start trial
                Toast.makeText(this@PaywallActivity, "Please start a trial or subscribe to continue", Toast.LENGTH_SHORT).show()
            }
        })
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
        // Get user ID to pass to PayPal for webhook matching
        val userId = SupabaseClient.getCurrentUserId()

        // Build PayPal URL with custom_id for user matching
        // This allows webhook to match user even if PayPal email differs from login email
        val subscribeUrl = if (userId != null) {
            "$PAYPAL_SUBSCRIBE_URL&custom_id=$userId"
        } else {
            PAYPAL_SUBSCRIBE_URL
        }

        Log.d(TAG, "Opening PayPal checkout with user_id: $userId")

        // Open PayPal subscription page
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(this, Uri.parse(subscribeUrl))
        } catch (e: Exception) {
            // Fallback to browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(subscribeUrl))
            startActivity(intent)
        }

        // When they come back, we'll check subscription status
        Toast.makeText(this, "Complete payment in PayPal, then return here", Toast.LENGTH_LONG).show()
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

        // Check if user subscribed while in PayPal browser
        lifecycleScope.launch {
            // Wait for PayPal IPN webhook to be processed by Supabase
            // PayPal typically sends IPN within 1-2 seconds
            delay(2000)

            val result = licenseManager.verifyLicense()
            if (result.isSuccess) {
                val license = result.getOrNull()!!
                if (license.isPro()) {
                    Toast.makeText(
                        this@PaywallActivity,
                        "Subscription activated! Welcome to Pro!",
                        Toast.LENGTH_SHORT
                    ).show()
                    proceedToApp()
                } else {
                    // Maybe webhook hasn't processed yet - update UI
                    updateTrialInfo()
                }
            }
        }
    }

}
