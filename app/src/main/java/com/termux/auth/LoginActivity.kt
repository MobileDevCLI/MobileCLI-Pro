package com.termux.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
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
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

/**
 * Login/Signup Activity for MobileCLI Pro.
 *
 * Supports:
 * - Email + password authentication
 * - Google OAuth (opens browser)
 *
 * After successful login, registers device and gets license.
 */
class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"

        fun start(context: Context) {
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    private lateinit var licenseManager: LicenseManager

    // UI Elements
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button
    private lateinit var googleButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        licenseManager = LicenseManager(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        signupButton = findViewById(R.id.signup_button)
        googleButton = findViewById(R.id.google_button)
        progressBar = findViewById(R.id.progress_bar)
        errorText = findViewById(R.id.error_text)
    }

    private fun setupListeners() {
        loginButton.setOnClickListener {
            hideKeyboard()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (validateInput(email, password)) {
                login(email, password)
            }
        }

        signupButton.setOnClickListener {
            hideKeyboard()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (validateInput(email, password)) {
                signup(email, password)
            }
        }

        googleButton.setOnClickListener {
            hideKeyboard()
            loginWithGoogle()
        }

        // Skip login REMOVED for production release
        // Users must authenticate to use the app
    }

    private fun validateInput(email: String, password: String): Boolean {
        errorText.visibility = View.GONE

        if (email.isEmpty()) {
            showError("Please enter your email")
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email")
            return false
        }

        if (password.isEmpty()) {
            showError("Please enter your password")
            return false
        }

        if (password.length < 6) {
            showError("Password must be at least 6 characters")
            return false
        }

        return true
    }

    private fun login(email: String, password: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                SupabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                Log.i(TAG, "Login successful for $email")
                onLoginSuccess()

            } catch (e: Exception) {
                Log.e(TAG, "Login failed", e)
                setLoading(false)
                showError(getErrorMessage(e))
            }
        }
    }

    private fun signup(email: String, password: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                SupabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                Log.i(TAG, "Signup successful for $email")

                // Show confirmation message
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Account created! Please check your email to verify.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // After signup, user needs to verify email
                // For now, we'll let them proceed (Supabase handles verification)
                onLoginSuccess()

            } catch (e: Exception) {
                Log.e(TAG, "Signup failed", e)
                setLoading(false)
                showError(getErrorMessage(e))
            }
        }
    }

    private fun loginWithGoogle() {
        setLoading(true)

        try {
            // Use browser-based OAuth with PKCE - works on ALL devices
            // Unlike Credential Manager, this doesn't fail silently
            val redirectUrl = "com.termux://login-callback"
            val oauthUrl = SupabaseClient.getGoogleOAuthUrlWithPKCE(redirectUrl)

            Log.i(TAG, "Opening Google OAuth in browser: $oauthUrl")

            // Open in Chrome Custom Tab for better UX
            try {
                val customTabsIntent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                customTabsIntent.launchUrl(this, Uri.parse(oauthUrl))
                Log.i(TAG, "Opened Chrome Custom Tab for OAuth")
            } catch (e: Exception) {
                // Fallback to regular browser
                Log.w(TAG, "Custom tab failed, using browser", e)
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl))
                startActivity(browserIntent)
            }
            // Don't set loading to false - wait for callback in onNewIntent

        } catch (e: Exception) {
            Log.e(TAG, "Google login failed: ${e.message}", e)
            setLoading(false)
            showError("Google sign-in failed. Please try again or use email login.")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle OAuth callback deep link
        intent?.data?.let { uri ->
            Log.i(TAG, "Received OAuth callback: $uri")
            setLoading(true)
            lifecycleScope.launch {
                try {
                    val success = SupabaseClient.handleDeepLink(uri)
                    if (success && SupabaseClient.isLoggedIn()) {
                        onLoginSuccess()
                    } else {
                        setLoading(false)
                        showError("Login failed. Please try again.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to handle OAuth callback", e)
                    setLoading(false)
                    showError("Login failed. Please try again.")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset loading state if user returns from OAuth without completing
        // Don't auto-redirect here - SplashActivity handles initial auth check
        // This prevents "immediately kicks away" bug from stale sessions
        if (progressBar.visibility == View.VISIBLE) {
            // Give brief delay for deep link to arrive, then reset if no login
            progressBar.postDelayed({
                if (!isFinishing && progressBar.visibility == View.VISIBLE) {
                    setLoading(false)
                }
            }, 2000)
        }
    }

    /**
     * Called after successful authentication.
     * Registers device and gets license before proceeding.
     */
    private suspend fun onLoginSuccess() {
        try {
            // Register device and get license
            val result = licenseManager.registerDevice()

            runOnUiThread {
                setLoading(false)

                if (result.isSuccess) {
                    val license = result.getOrNull()!!
                    Log.i(TAG, "Device registered, license tier: ${license.tier}")

                    // Proceed based on license
                    if (license.isPro()) {
                        // Pro user - go directly to app
                        proceedToApp()
                    } else {
                        // Free/trial user - go to paywall
                        goToPaywall()
                    }
                } else {
                    // Registration failed - still let them proceed but with limited access
                    Log.w(TAG, "Device registration failed: ${result.exceptionOrNull()?.message}")
                    goToPaywall()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onLoginSuccess error", e)
            runOnUiThread {
                setLoading(false)
                goToPaywall()
            }
        }
    }

    private fun proceedToApp() {
        // Check if setup wizard needs to run
        if (!SetupWizard.isSetupComplete(this)) {
            startActivity(Intent(this, SetupWizard::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun goToPaywall() {
        startActivity(Intent(this, PaywallActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !loading
        signupButton.isEnabled = !loading
        googleButton.isEnabled = !loading
        emailInput.isEnabled = !loading
        passwordInput.isEnabled = !loading
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun getErrorMessage(e: Exception): String {
        val message = e.message ?: "Unknown error"
        return when {
            message.contains("Invalid login credentials") -> "Invalid email or password"
            message.contains("Email not confirmed") -> "Please verify your email first"
            message.contains("User already registered") -> "An account with this email already exists"
            message.contains("Password should be at least") -> "Password must be at least 6 characters"
            message.contains("Unable to validate email") -> "Please enter a valid email address"
            message.contains("network") || message.contains("timeout") -> "Network error. Please check your connection."
            else -> "Error: $message"
        }
    }
}
