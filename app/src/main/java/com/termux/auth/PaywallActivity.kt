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

        // Post-payment polling configuration
        private const val POLL_INTERVAL_MS = 3000L  // Check every 3 seconds
        private const val MAX_POLL_TIME_MS = 30000L // Give up after 30 seconds

        fun start(context: Context) {
            val intent = Intent(context, PaywallActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    private lateinit var licenseManager: LicenseManager
    private var isPolling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paywall)

        licenseManager = LicenseManager(this)

        setupUI()
        setupBackHandler()

        // Handle deep link from payment success page
        handlePaymentSuccessDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handlePaymentSuccessDeepLink(it) }
    }

    private fun handlePaymentSuccessDeepLink(intent: Intent) {
        val uri = intent.data
        if (uri != null) {
            val isPaymentSuccess = uri.scheme == "com.termux" && uri.host == "payment-success" ||
                                   uri.host == "www.mobilecli.com" && uri.path?.startsWith("/success") == true

            if (isPaymentSuccess) {
                Log.i(TAG, "Payment success deep link received: $uri")
                startPaymentPolling()
            }
        }
    }

    /**
     * Start polling for subscription activation after payment.
     * PayPal webhook may take a few seconds to process.
     */
    private fun startPaymentPolling() {
        if (isPolling) return
        isPolling = true

        Log.i(TAG, "Starting payment verification polling...")

        // Show processing state
        showProcessingState("Processing payment...")

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            var attempts = 0

            while (isPolling && (System.currentTimeMillis() - startTime) < MAX_POLL_TIME_MS) {
                attempts++
                Log.i(TAG, "Payment poll attempt $attempts")

                updateStatusMessage("Verifying payment... (attempt $attempts)")

                // Force fresh check from server
                val result = licenseManager.forceVerifyLicense()

                if (result.isSuccess) {
                    val license = result.getOrNull()!!
                    Log.i(TAG, "Poll result: tier=${license.tier}, isPro=${license.isPro()}")

                    if (license.isPro()) {
                        isPolling = false
                        hideProcessingState()
                        Toast.makeText(this@PaywallActivity, "Payment successful! Welcome to Pro!", Toast.LENGTH_SHORT).show()
                        proceedToApp()
                        return@launch
                    }
                }

                // Wait before next attempt
                delay(POLL_INTERVAL_MS)
            }

            // Polling timed out - subscription not activated yet
            isPolling = false
            hideProcessingState()
            showPaymentPendingMessage()
        }
    }

    private fun showProcessingState(message: String) {
        runOnUiThread {
            findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE
            findViewById<Button>(R.id.subscribe_button).isEnabled = false
            findViewById<Button>(R.id.start_trial_button).isEnabled = false
            findViewById<TextView>(R.id.restore_purchase).isEnabled = false

            // Update status message if view exists
            findViewById<TextView>(R.id.status_message)?.let {
                it.text = message
                it.visibility = View.VISIBLE
            }
        }
    }

    private fun updateStatusMessage(message: String) {
        runOnUiThread {
            findViewById<TextView>(R.id.status_message)?.text = message
        }
    }

    private fun hideProcessingState() {
        runOnUiThread {
            findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
            findViewById<Button>(R.id.subscribe_button).isEnabled = true
            findViewById<Button>(R.id.start_trial_button).isEnabled = true
            findViewById<TextView>(R.id.restore_purchase).isEnabled = true
            findViewById<TextView>(R.id.status_message)?.visibility = View.GONE
        }
    }

    private fun showPaymentPendingMessage() {
        runOnUiThread {
            Toast.makeText(
                this,
                "Payment received! It may take a moment to activate. Tap 'Check Again' to verify.",
                Toast.LENGTH_LONG
            ).show()

            // Show the check again section
            findViewById<LinearLayout>(R.id.check_again_section)?.visibility = View.VISIBLE
        }
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

        // Check Again button (in help section)
        findViewById<Button>(R.id.check_again_button)?.setOnClickListener {
            restorePurchase()
        }

        // Contact Support button
        findViewById<Button>(R.id.contact_support_button)?.setOnClickListener {
            openSupportEmail()
        }

        // Update trial info based on license
        updateTrialInfo()
    }

    private fun openSupportEmail() {
        val userId = SupabaseClient.getCurrentUserId() ?: "unknown"
        val userEmail = SupabaseClient.getCurrentUserEmail() ?: "unknown"

        val subject = "MobileCLI Pro - Subscription Issue"
        val body = """
            Hi MobileCLI Support,

            I'm having trouble with my subscription.

            User ID: $userId
            Email: $userEmail
            Issue: [Please describe your issue]

            Thanks!
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@mobilecli.com"))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            // No email app - copy support email to clipboard
            Toast.makeText(this, "Email support@mobilecli.com for help", Toast.LENGTH_LONG).show()
        }
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

        // Mark that we're starting a payment flow
        // This triggers verification polling when user returns
        markPaymentStarted()

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

        // When they come back, we'll automatically check subscription status
        Toast.makeText(this, "Complete payment in PayPal - we'll verify when you return", Toast.LENGTH_LONG).show()
    }

    private fun restorePurchase() {
        Log.i(TAG, "Restore purchase clicked")

        // Immediate feedback
        Toast.makeText(this, "Checking subscription (force refresh)...", Toast.LENGTH_SHORT).show()

        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.visibility = View.VISIBLE

        // Disable buttons while checking
        findViewById<TextView>(R.id.restore_purchase).isEnabled = false
        findViewById<Button>(R.id.check_again_button)?.isEnabled = false

        lifecycleScope.launch {
            try {
                Log.i(TAG, "Force verifying license with server (cache bypassed)...")

                // Force fresh check from server - bypass all cache
                val result = licenseManager.forceVerifyLicense()

                Log.i(TAG, "License result: ${result.isSuccess}, ${result.getOrNull()}")

                if (result.isSuccess) {
                    val license = result.getOrNull()!!
                    Log.i(TAG, "License tier: ${license.tier}, isPro: ${license.isPro()}")

                    if (license.isPro()) {
                        Toast.makeText(
                            this@PaywallActivity,
                            "Subscription restored! Welcome back!",
                            Toast.LENGTH_LONG
                        ).show()
                        proceedToApp()
                    } else {
                        // Show help options
                        showNoSubscriptionHelp()
                    }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Restore failed: $errorMsg")
                    Toast.makeText(
                        this@PaywallActivity,
                        "Could not verify: $errorMsg",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Restore failed with exception", e)
                Toast.makeText(
                    this@PaywallActivity,
                    "Restore failed: ${e.message}. Please check your internet connection.",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
                findViewById<TextView>(R.id.restore_purchase).isEnabled = true
                findViewById<Button>(R.id.check_again_button)?.isEnabled = true
            }
        }
    }

    private fun showNoSubscriptionHelp() {
        runOnUiThread {
            // Show check again section with help info
            findViewById<LinearLayout>(R.id.check_again_section)?.visibility = View.VISIBLE

            Toast.makeText(
                this,
                "No active subscription found. If you just paid, try again in a moment or contact support.",
                Toast.LENGTH_LONG
            ).show()
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

    private var paymentInProgress = false

    /**
     * Mark that payment checkout has started.
     * Used to trigger verification on resume.
     */
    private fun markPaymentStarted() {
        paymentInProgress = true
    }

    override fun onResume() {
        super.onResume()
        updateTrialInfo()

        // If we were in a payment flow and returned, start polling
        if (paymentInProgress && !isPolling) {
            paymentInProgress = false
            Log.i(TAG, "Returned from payment flow, starting verification polling")

            // Brief delay to let PayPal's browser close
            lifecycleScope.launch {
                delay(500)
                startPaymentPolling()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false  // Stop polling if activity is destroyed
    }

}
