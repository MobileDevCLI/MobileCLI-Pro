package com.termux.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.termux.R
import kotlinx.coroutines.launch

/**
 * Account/Settings Activity for MobileCLI Pro.
 *
 * Industry-standard account management:
 * - View profile (email)
 * - View subscription status
 * - Manage subscription (opens PayPal)
 * - Logout
 * - Delete account (with confirmation)
 */
class AccountActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AccountActivity"
        private const val PAYPAL_SUBSCRIPTIONS_URL = "https://www.paypal.com/myaccount/autopay"

        fun start(context: Context) {
            context.startActivity(Intent(context, AccountActivity::class.java))
        }
    }

    private lateinit var licenseManager: LicenseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Account"

        licenseManager = LicenseManager(this)

        setupUI()
        loadAccountInfo()
    }

    private fun setupUI() {
        // Logout button
        findViewById<Button>(R.id.logout_button).setOnClickListener {
            showLogoutConfirmation()
        }

        // Manage subscription button
        findViewById<Button>(R.id.manage_subscription_button).setOnClickListener {
            openManageSubscription()
        }

        // Delete account button (if you want this feature)
        findViewById<Button>(R.id.delete_account_button)?.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun loadAccountInfo() {
        // Email
        val email = SupabaseClient.getCurrentUserEmail() ?: "Not logged in"
        findViewById<TextView>(R.id.account_email).text = email

        // User ID (for support)
        val userId = SupabaseClient.getCurrentUserId() ?: "Unknown"
        findViewById<TextView>(R.id.account_user_id).text = "ID: ${userId.take(8)}..."

        // Subscription status
        val license = licenseManager.getLicenseInfo()
        val statusText = when {
            license == null -> "Not logged in"
            license.isPro() -> "Pro Subscriber"
            license.tier == "free" && license.daysUntilExpiry() > 0 ->
                "Trial (${license.daysUntilExpiry()} days left)"
            else -> "Expired"
        }
        findViewById<TextView>(R.id.subscription_status).text = statusText

        // Show/hide manage subscription based on status
        val manageButton = findViewById<Button>(R.id.manage_subscription_button)
        manageButton.text = if (license?.isPro() == true) {
            "Manage Subscription"
        } else {
            "Subscribe to Pro"
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out? You can log back in anytime with the same account.")
            .setPositiveButton("Log Out") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                // Clear local license cache
                licenseManager.clearCache()

                // Sign out from Supabase
                SupabaseClient.signOut()

                Toast.makeText(this@AccountActivity, "Logged out", Toast.LENGTH_SHORT).show()

                // Go to login screen
                LoginActivity.start(this@AccountActivity)
                finishAffinity() // Close all activities

            } catch (e: Exception) {
                Log.e(TAG, "Logout failed", e)
                Toast.makeText(this@AccountActivity, "Logout failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openManageSubscription() {
        val license = licenseManager.getLicenseInfo()

        if (license?.isPro() == true) {
            // Open PayPal subscription management
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_SUBSCRIPTIONS_URL))
            startActivity(intent)
        } else {
            // Go to paywall to subscribe
            PaywallActivity.start(this)
            finish()
        }
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("This will permanently delete your account and all data. This action cannot be undone.\n\nIf you have an active subscription, please cancel it first in PayPal.")
            .setPositiveButton("Delete") { _, _ ->
                // For now, just show a message - actual deletion requires backend support
                Toast.makeText(this, "Please email mobiledevcli@gmail.com to delete your account", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
