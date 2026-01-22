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

        // Skip login button (demo mode)
        findViewById<TextView>(R.id.skip_login).setOnClickListener {
            skipLogin()
        }
    }

    /**
     * Skip login for development/testing.
     * Long press on error text area to activate.
     */
    private fun skipLogin() {
        // Go directly to app (SetupWizard or MainActivity)
        if (!SetupWizard.isSetupComplete(this)) {
            startActivity(Intent(this, SetupWizard::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
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
                    setLoading(false)
                    Toast.makeText(
                        this@LoginActivity,
                        "Account created! Please check your email to confirm.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Signup failed", e)
                setLoading(false)
                showError(getErrorMessage(e))
            }
        }
    }

    private fun loginWithGoogle() {
        setLoading(true)

        lifecycleScope.launch {
            try {
                // Start Google OAuth flow with custom redirect
                SupabaseClient.auth.signInWith(Google) {
                    redirectUrl = "com.termux://login-callback"
                }

                Log.i(TAG, "Google login initiated")
                // OAuth will redirect back to app via deep link

            } catch (e: Exception) {
                Log.e(TAG, "Google login failed", e)
                setLoading(false)
                showError("Google sign-in failed. Please try again.")
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle OAuth callback deep link
        intent?.data?.let { uri ->
            Log.i(TAG, "Received OAuth callback: $uri")
            lifecycleScope.launch {
                try {
                    SupabaseClient.auth.handleDeepLink(uri)
                    if (SupabaseClient.isLoggedIn()) {
                        onLoginSuccess()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to handle OAuth callback", e)
                    showError("Login failed. Please try again.")
                }
            }
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

            if (result.isSuccess) {
                val license = result.getOrNull()!!
                Log.i(TAG, "Device registered. Tier: ${license.tier}")

                runOnUiThread {
                    if (license.tier == "free") {
                        // Show paywall for free users
                        PaywallActivity.start(this@LoginActivity)
                    } else {
                        // Pro user - go to setup or main
                        proceedToApp()
                    }
                }
            } else {
                // Registration failed but login succeeded
                // Allow access with basic tier
                Log.w(TAG, "Device registration failed: ${result.exceptionOrNull()?.message}")
                runOnUiThread {
                    PaywallActivity.start(this@LoginActivity)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Post-login setup failed", e)
            runOnUiThread {
                setLoading(false)
                showError("Setup failed. Please try again.")
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

    private fun setLoading(loading: Boolean) {
        runOnUiThread {
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            loginButton.isEnabled = !loading
            signupButton.isEnabled = !loading
            googleButton.isEnabled = !loading
            emailInput.isEnabled = !loading
            passwordInput.isEnabled = !loading
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            errorText.text = message
            errorText.visibility = View.VISIBLE
        }
    }

    private fun getErrorMessage(e: Exception): String {
        val message = e.message ?: "Unknown error"
        return when {
            message.contains("Invalid login credentials") -> "Invalid email or password"
            message.contains("Email not confirmed") -> "Please confirm your email first"
            message.contains("User already registered") -> "An account with this email already exists"
            message.contains("network") -> "Network error. Please check your connection."
            else -> message
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if user logged in via OAuth redirect
        if (SupabaseClient.isLoggedIn()) {
            lifecycleScope.launch {
                onLoginSuccess()
            }
        }
    }
}
