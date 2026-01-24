package com.termux

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.os.IBinder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.ContextMenu
import android.view.KeyEvent
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MobileCLI Main Activity - Terminal UI Only
 *
 * This activity handles:
 * - Terminal display and interaction
 * - Multi-session management
 * - Extra keys (CTRL, ALT, etc.)
 * - Navigation drawer
 *
 * Setup wizard is handled by SetupWizard.kt
 */
class MainActivity : AppCompatActivity(), TerminalViewClient, TerminalSessionClient {

    companion object {
        private const val TAG = "MobileCLI"
        private const val MAX_SESSIONS = 10
        private const val PREFS_NAME = "mobilecli_prefs"
        private const val KEY_DEV_MODE = "developer_mode_enabled"
        private const val VERSION_TAP_THRESHOLD = 7
        private const val VERSION_TAP_TIMEOUT = 2000L
        private const val KEY_CURRENT_SESSION = "current_session_index"
        private const val KEY_SELECTED_AI = "selected_ai"
        // Context menu item IDs
        private const val MENU_COPY_ALL = 1
        private const val MENU_PASTE = 2
        private const val MENU_SELECT_ALL = 3
        private const val MENU_NEW_SESSION = 4
        private const val MENU_KILL_SESSION = 5
        private const val MENU_RESET_TERMINAL = 6
    }

    // Terminal
    private lateinit var terminalView: TerminalView
    private lateinit var bootstrapInstaller: BootstrapInstaller
    // Sessions are now managed by TermuxService for persistence across activity recreation
    private val sessions: List<TerminalSession> get() = termuxService?.getSessions() ?: emptyList()
    private var currentSessionIndex: Int
        get() = termuxService?.getCurrentSessionIndex() ?: 0
        set(value) { termuxService?.setCurrentSessionIndex(value) }
    private val session: TerminalSession? get() = sessions.getOrNull(currentSessionIndex)

    // UI
    private lateinit var drawerLayout: DrawerLayout
    private var gestureDetector: GestureDetectorCompat? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    // Keyboard tracking
    private var isKeyboardVisible = false
    private var keyboardHeightThreshold = 0

    // Modifier keys state
    private var ctrlPressed = false
    private var altPressed = false

    // URL watcher
    private var urlWatcherRunnable: Runnable? = null
    private val urlWatchInterval = 500L

    // Text size
    private var currentTextSize = 28f
    private val minTextSize = 14f
    private val maxTextSize = 56f

    // Developer mode
    private var developerModeEnabled = false
    private var versionTapCount = 0
    private var lastVersionTapTime = 0L
    private var devOptionsDivider: View? = null
    private var devOptionsHeader: TextView? = null
    private var devModeToggle: TextView? = null
    private var installDevToolsItem: TextView? = null
    private var versionText: TextView? = null

    // Service binding for wake lock
    private var termuxService: com.termux.app.TermuxService? = null
    private var serviceBound = false
    private var wakeLockEnabled = false
    private var powerModeEnabled = false

    // Drawer items that need state updates
    private var wakeLockToggle: TextView? = null
    private var powerModeToggle: TextView? = null

    // Pending AI launch (for after service connects)
    private var pendingAILaunch: String? = null

    // Service connection - handles reconnection to existing sessions
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? com.termux.app.TermuxService.LocalBinder
            termuxService = binder?.service
            serviceBound = true
            Log.i(TAG, "TermuxService connected")

            // Update service with our session client
            termuxService?.setSessionClient(this@MainActivity)

            // Check for existing sessions to reconnect to
            if (termuxService?.hasExistingSessions() == true) {
                Log.i(TAG, "Reconnecting to ${sessions.size} existing session(s)")
                reconnectToExistingSessions()
            } else {
                Log.i(TAG, "No existing sessions, creating new one")
                createSession()
            }

            // Handle pending AI launch after service is ready
            pendingAILaunch?.let { ai ->
                pendingAILaunch = null
                launchAI(ai)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            termuxService = null
            serviceBound = false
            Log.i(TAG, "TermuxService disconnected")
        }
    }

    /**
     * Reconnect to sessions that persisted in the service.
     * This is called when activity is recreated but service kept sessions alive.
     */
    private fun reconnectToExistingSessions() {
        val existingSessions = sessions
        if (existingSessions.isEmpty()) {
            createSession()
            return
        }

        // Attach to the current session
        val sessionToAttach = existingSessions.getOrNull(currentSessionIndex) ?: existingSessions.first()
        terminalView.attachSession(sessionToAttach)
        updateTerminalSize()
        updateSessionTabs()

        Log.i(TAG, "Reconnected to session ${currentSessionIndex + 1} of ${existingSessions.size}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if setup is complete
        if (!SetupWizard.isSetupComplete(this)) {
            startActivity(Intent(this, SetupWizard::class.java))
            finish()
            return
        }

        bootstrapInstaller = BootstrapInstaller(this)

        // Self-healing: regenerate API scripts if overwritten by termux-api package
        bootstrapInstaller.regenerateApiScriptsIfNeeded()

        setContentView(R.layout.activity_main)
        setupTerminal()
        setupExtraKeys()
        setupDrawer()
        setupDeveloperMode()
        setupKeyboardListener()
        setupGestureDetector()

        // Start and bind service FIRST - it manages sessions
        bindTermuxService()

        // Apply startup settings
        applyStartupSettings()

        // NOTE: Session creation now happens in serviceConnection.onServiceConnected()
        // This ensures sessions persist in the service even when activity is destroyed

        // Handle selected AI from setup wizard (defer until service connects)
        val selectedAI = intent.getStringExtra("selected_ai")
        if (selectedAI != null && selectedAI != SetupWizard.AI_NONE) {
            pendingAILaunch = selectedAI
        }

        // Start URL watcher
        setupUrlWatcher()

        Log.i(TAG, "MainActivity created, savedInstanceState=${savedInstanceState != null}")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current session index for restoration
        outState.putInt(KEY_CURRENT_SESSION, currentSessionIndex)
        Log.i(TAG, "State saved: sessionIndex=$currentSessionIndex")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore session index (will be applied when service reconnects)
        val savedIndex = savedInstanceState.getInt(KEY_CURRENT_SESSION, 0)
        termuxService?.setCurrentSessionIndex(savedIndex)
        Log.i(TAG, "State restored: sessionIndex=$savedIndex")
    }

    // ═══════════════════════════════════════════════════════════════
    // Terminal Setup
    // ═══════════════════════════════════════════════════════════════

    private fun setupTerminal() {
        terminalView = findViewById(R.id.terminal_view)
        terminalView.setTerminalViewClient(this)
        terminalView.setTextSize(currentTextSize.toInt())
        terminalView.requestFocus()

        // Register for context menu - enables "More" button in text selection
        registerForContextMenu(terminalView)
    }

    private fun createSession() {
        if (termuxService == null) {
            Log.w(TAG, "Cannot create session - service not connected")
            Toast.makeText(this, "Waiting for service...", Toast.LENGTH_SHORT).show()
            return
        }

        if (sessions.size >= MAX_SESSIONS) {
            Toast.makeText(this, "Maximum sessions reached", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val cwd = bootstrapInstaller.homeDir
            val shell = File(bootstrapInstaller.prefixDir, "bin/login").absolutePath

            // Use the comprehensive environment from BootstrapInstaller
            // This includes BROWSER=termux-open-url which is critical for OAuth flows
            val env = bootstrapInstaller.getEnvironment()

            // Create session through service for persistence
            val session = termuxService?.createSession(
                shell,
                cwd.absolutePath,
                arrayOf(shell, "-l"),
                env,
                this as TerminalSessionClient
            )

            if (session != null) {
                currentSessionIndex = sessions.size - 1
                terminalView.attachSession(session)
                updateTerminalSize()
                updateSessionTabs()
                Log.i(TAG, "Created new session ${currentSessionIndex + 1}")
            } else {
                Toast.makeText(this, "Failed to create session", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create session", e)
            Toast.makeText(this, "Failed to create session", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTerminalSize() {
        if (terminalView.width <= 0 || terminalView.height <= 0) return

        try {
            val rendererField = terminalView.javaClass.getDeclaredField("mRenderer")
            rendererField.isAccessible = true
            val renderer = rendererField.get(terminalView)

            val fontWidthField = renderer.javaClass.getDeclaredField("mFontWidth")
            fontWidthField.isAccessible = true
            var fontWidthPx = (fontWidthField.get(renderer) as Number).toInt()

            val fontHeightField = renderer.javaClass.getDeclaredField("mFontLineSpacing")
            fontHeightField.isAccessible = true
            var fontHeightPx = (fontHeightField.get(renderer) as Number).toInt()

            if (fontWidthPx <= 0 || fontHeightPx <= 0) {
                val density = resources.displayMetrics.density
                fontHeightPx = (currentTextSize * density * 1.2f).toInt()
                fontWidthPx = (currentTextSize * density * 0.6f).toInt()
            }

            val columns = terminalView.width / fontWidthPx
            val rows = terminalView.height / fontHeightPx

            if (columns > 0 && rows > 0) {
                session?.updateSize(columns, rows)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update terminal size", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Extra Keys
    // ═══════════════════════════════════════════════════════════════

    private fun setupExtraKeys() {
        findViewById<Button>(R.id.btn_esc)?.setOnClickListener { sendKey(27) }
        findViewById<Button>(R.id.btn_ctrl)?.setOnClickListener { toggleCtrl() }
        findViewById<Button>(R.id.btn_alt)?.setOnClickListener { toggleAlt() }
        findViewById<Button>(R.id.btn_tab)?.setOnClickListener { sendKey(9) }
        findViewById<Button>(R.id.btn_home)?.setOnClickListener { sendSpecialKey("home") }
        findViewById<Button>(R.id.btn_end)?.setOnClickListener { sendSpecialKey("end") }
        findViewById<Button>(R.id.btn_up)?.setOnClickListener { sendSpecialKey("up") }
        findViewById<Button>(R.id.btn_down)?.setOnClickListener { sendSpecialKey("down") }
        findViewById<Button>(R.id.btn_left)?.setOnClickListener { sendSpecialKey("left") }
        findViewById<Button>(R.id.btn_right)?.setOnClickListener { sendSpecialKey("right") }
        findViewById<Button>(R.id.btn_pgup)?.setOnClickListener { sendSpecialKey("pgup") }
        findViewById<Button>(R.id.btn_pgdn)?.setOnClickListener { sendSpecialKey("pgdn") }

        // Symbol keys
        findViewById<Button>(R.id.btn_dash)?.setOnClickListener { sendChar('-') }
        findViewById<Button>(R.id.btn_slash)?.setOnClickListener { sendChar('/') }
        findViewById<Button>(R.id.btn_backslash)?.setOnClickListener { sendChar('\\') }
        findViewById<Button>(R.id.btn_pipe)?.setOnClickListener { sendChar('|') }
        findViewById<Button>(R.id.btn_tilde)?.setOnClickListener { sendChar('~') }
        findViewById<Button>(R.id.btn_underscore)?.setOnClickListener { sendChar('_') }
        findViewById<Button>(R.id.btn_colon)?.setOnClickListener { sendChar(':') }
        findViewById<Button>(R.id.btn_quote)?.setOnClickListener { sendChar('"') }

        // More button
        findViewById<Button>(R.id.btn_more)?.setOnClickListener { showMoreOptions() }
    }

    private fun toggleCtrl() {
        ctrlPressed = !ctrlPressed
        findViewById<Button>(R.id.btn_ctrl)?.apply {
            setTextColor(if (ctrlPressed) 0xFF4CAF50.toInt() else 0xFF00FF00.toInt())
        }
    }

    private fun toggleAlt() {
        altPressed = !altPressed
        findViewById<Button>(R.id.btn_alt)?.apply {
            setTextColor(if (altPressed) 0xFF4CAF50.toInt() else 0xFF00FF00.toInt())
        }
    }

    private fun sendKey(keyCode: Int) {
        session?.write(byteArrayOf(keyCode.toByte()), 0, 1)
        resetModifiers()
    }

    private fun sendChar(char: Char) {
        val byte = char.code.toByte()
        session?.write(byteArrayOf(byte), 0, 1)
        resetModifiers()
    }

    private fun sendSpecialKey(key: String) {
        val sequence = when (key) {
            "up" -> "\u001b[A"
            "down" -> "\u001b[B"
            "right" -> "\u001b[C"
            "left" -> "\u001b[D"
            "home" -> "\u001b[H"
            "end" -> "\u001b[F"
            "pgup" -> "\u001b[5~"
            "pgdn" -> "\u001b[6~"
            else -> return
        }
        session?.write(sequence.toByteArray(), 0, sequence.length)
        resetModifiers()
    }

    private fun resetModifiers() {
        ctrlPressed = false
        altPressed = false
        findViewById<Button>(R.id.btn_ctrl)?.setTextColor(0xFF00FF00.toInt())
        findViewById<Button>(R.id.btn_alt)?.setTextColor(0xFF00FF00.toInt())
    }

    // ═══════════════════════════════════════════════════════════════
    // Drawer & Navigation
    // ═══════════════════════════════════════════════════════════════

    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)

        // Account button - Industry standard account management
        findViewById<TextView>(R.id.nav_account)?.setOnClickListener {
            com.termux.auth.AccountActivity.start(this)
            drawerLayout.closeDrawers()
        }

        findViewById<TextView>(R.id.nav_new_session)?.setOnClickListener {
            createSession()
            drawerLayout.closeDrawers()
        }

        findViewById<TextView>(R.id.nav_settings)?.setOnClickListener {
            showSettings()
            drawerLayout.closeDrawers()
        }

        findViewById<TextView>(R.id.nav_keyboard)?.setOnClickListener {
            toggleKeyboard()
            drawerLayout.closeDrawers()
        }

        findViewById<TextView>(R.id.nav_text_size)?.setOnClickListener {
            showTextSizeDialog()
            drawerLayout.closeDrawers()
        }

        // Wake lock toggle
        wakeLockToggle = findViewById(R.id.nav_wake_lock)
        wakeLockToggle?.setOnClickListener {
            toggleWakeLock()
        }

        // Power mode (skip permission prompts for Claude)
        powerModeToggle = findViewById(R.id.nav_power_mode)
        powerModeToggle?.setOnClickListener {
            togglePowerMode()
        }

        findViewById<TextView>(R.id.nav_install_ai)?.setOnClickListener {
            showAIInstallMenu()
            drawerLayout.closeDrawers()
        }

        // Vercel CLI
        findViewById<TextView>(R.id.nav_vercel)?.setOnClickListener {
            installVercelCLI()
            drawerLayout.closeDrawers()
        }

        // GitHub CLI
        findViewById<TextView>(R.id.nav_github)?.setOnClickListener {
            installGitHubCLI()
            drawerLayout.closeDrawers()
        }

        // Supabase CLI
        findViewById<TextView>(R.id.nav_supabase)?.setOnClickListener {
            installSupabaseCLI()
            drawerLayout.closeDrawers()
        }

        // AI Briefing - Fetches comprehensive documentation for AI assistants
        findViewById<TextView>(R.id.nav_ai_briefing)?.setOnClickListener {
            showAIBriefing()
            drawerLayout.closeDrawers()
        }

        // Update - Fetches latest updates/instructions from website
        findViewById<TextView>(R.id.nav_update)?.setOnClickListener {
            checkForUpdates()
            drawerLayout.closeDrawers()
        }

        findViewById<TextView>(R.id.nav_help)?.setOnClickListener {
            showHelp()
            drawerLayout.closeDrawers()
        }

        findViewById<TextView>(R.id.nav_about)?.setOnClickListener {
            showAbout()
            drawerLayout.closeDrawers()
        }

        findViewById<TextView>(R.id.nav_licenses)?.setOnClickListener {
            showLicenses()
            drawerLayout.closeDrawers()
        }

        findViewById<TextView>(R.id.nav_privacy)?.setOnClickListener {
            showPrivacyPolicy()
            drawerLayout.closeDrawers()
        }

        findViewById<TextView>(R.id.nav_terms)?.setOnClickListener {
            showTermsOfService()
            drawerLayout.closeDrawers()
        }
    }

    private fun updateSessionTabs() {
        val tabsContainer = findViewById<LinearLayout>(R.id.session_tabs)
        val tabsScrollView = findViewById<View>(R.id.session_tabs_container)

        if (sessions.size <= 1) {
            tabsScrollView?.visibility = View.GONE
            return
        }

        tabsScrollView?.visibility = View.VISIBLE
        tabsContainer?.removeAllViews()

        sessions.forEachIndexed { index, _ ->
            val tab = TextView(this).apply {
                text = " ${index + 1} "
                textSize = 14f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(24, 12, 24, 12)
                setBackgroundColor(if (index == currentSessionIndex) 0xFF2a2a2a.toInt() else 0x00000000)
                setOnClickListener { switchToSession(index) }
            }
            tabsContainer?.addView(tab)
        }
    }

    private fun switchToSession(index: Int) {
        if (index in sessions.indices) {
            currentSessionIndex = index
            terminalView.attachSession(sessions[index])
            updateSessionTabs()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Developer Mode (7-tap activation)
    // ═══════════════════════════════════════════════════════════════

    private fun setupDeveloperMode() {
        // Load saved state
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        developerModeEnabled = prefs.getBoolean(KEY_DEV_MODE, false)

        // Get developer options UI elements
        devOptionsDivider = findViewById(R.id.dev_options_divider)
        devOptionsHeader = findViewById(R.id.nav_dev_options_header)
        devModeToggle = findViewById(R.id.nav_dev_mode)
        installDevToolsItem = findViewById(R.id.nav_install_dev_tools)
        versionText = findViewById(R.id.nav_version)

        // Set up version text click for 7-tap activation
        versionText?.setOnClickListener {
            val now = System.currentTimeMillis()

            if (now - lastVersionTapTime > VERSION_TAP_TIMEOUT) {
                versionTapCount = 0
            }

            versionTapCount++
            lastVersionTapTime = now

            if (developerModeEnabled) {
                // Already enabled - show status
                Toast.makeText(this, "Developer mode is already enabled", Toast.LENGTH_SHORT).show()
            } else {
                val remaining = VERSION_TAP_THRESHOLD - versionTapCount
                when {
                    remaining <= 0 -> {
                        enableDeveloperMode()
                    }
                    remaining <= 3 -> {
                        Toast.makeText(this, "$remaining taps to enable developer mode", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Set up dev mode toggle click
        devModeToggle?.setOnClickListener {
            toggleDeveloperMode()
        }

        // Set up install dev tools click
        installDevToolsItem?.setOnClickListener {
            installDeveloperTools()
            drawerLayout.closeDrawers()
        }

        // Update UI state
        updateDeveloperModeUI()
    }

    private fun enableDeveloperMode() {
        developerModeEnabled = true
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEV_MODE, true)
            .apply()

        Toast.makeText(this, "Developer mode enabled!", Toast.LENGTH_LONG).show()
        updateDeveloperModeUI()
    }

    private fun toggleDeveloperMode() {
        developerModeEnabled = !developerModeEnabled
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEV_MODE, developerModeEnabled)
            .apply()

        val status = if (developerModeEnabled) "enabled" else "disabled"
        Toast.makeText(this, "Developer mode $status", Toast.LENGTH_SHORT).show()
        updateDeveloperModeUI()
    }

    private fun updateDeveloperModeUI() {
        val visibility = if (developerModeEnabled) View.VISIBLE else View.GONE

        devOptionsDivider?.visibility = visibility
        devOptionsHeader?.visibility = visibility
        devModeToggle?.visibility = visibility
        installDevToolsItem?.visibility = visibility

        devModeToggle?.text = if (developerModeEnabled) "Developer Mode: ON" else "Developer Mode: OFF"
    }

    private fun installDeveloperTools() {
        Toast.makeText(this, "Installing developer tools...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            delay(500)
            withContext(Dispatchers.Main) {
                val command = "install-dev-tools\n"
                session?.write(command.toByteArray(), 0, command.length)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Service Binding (Wake Lock)
    // ═══════════════════════════════════════════════════════════════

    private fun bindTermuxService() {
        try {
            val intent = Intent(this, com.termux.app.TermuxService::class.java)

            // Start as foreground service for persistence
            // This keeps the service alive even when activity is destroyed
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            // Bind with BIND_AUTO_CREATE to keep service alive
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.i(TAG, "TermuxService started and bound for session persistence")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind TermuxService", e)
        }
    }

    private fun unbindTermuxService() {
        if (serviceBound) {
            try {
                unbindService(serviceConnection)
                serviceBound = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unbind TermuxService", e)
            }
        }
    }

    private fun toggleWakeLock() {
        wakeLockEnabled = !wakeLockEnabled

        if (termuxService != null) {
            if (wakeLockEnabled) {
                termuxService?.acquireWakeLock()
                Toast.makeText(this, "Wake lock enabled - screen can turn off", Toast.LENGTH_SHORT).show()
            } else {
                termuxService?.releaseWakeLock()
                Toast.makeText(this, "Wake lock disabled", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Service not connected", Toast.LENGTH_SHORT).show()
            wakeLockEnabled = false
        }

        updateWakeLockUI()
    }

    private fun updateWakeLockUI() {
        val text = if (wakeLockEnabled) "Wake Lock: ON" else "Wake Lock: OFF"
        val color = if (wakeLockEnabled) 0xFF4CAF50.toInt() else 0xFFFFFFFF.toInt()
        wakeLockToggle?.text = text
        wakeLockToggle?.setTextColor(color)
    }

    private fun togglePowerMode() {
        AlertDialog.Builder(this)
            .setTitle("Power Mode")
            .setMessage("Launch Claude with auto-accept enabled.\n\nClaude will automatically approve tool calls without asking for permission.\n\nUse with caution - AI will execute commands autonomously.")
            .setPositiveButton("Launch") { _, _ ->
                powerModeEnabled = true
                val powerModeFile = File(File(bootstrapInstaller.homeDir, ".termux"), "power_mode")
                try {
                    powerModeFile.parentFile?.mkdirs()
                    powerModeFile.writeText("enabled")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write power mode file", e)
                }
                val cmd = "claude --dangerously-skip-permissions\n"
                session?.write(cmd.toByteArray(), 0, cmd.length)
                Toast.makeText(this, "Launching Claude with Power Mode...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePowerModeUI() {
        // Power Mode button is now static, no ON/OFF state shown
    }

    private fun applyStartupSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Apply wake lock on startup if enabled
        if (prefs.getBoolean("wake_lock_startup", false)) {
            // Delay to ensure service is bound
            uiHandler.postDelayed({
                if (termuxService != null && !wakeLockEnabled) {
                    toggleWakeLock()
                }
            }, 1000)
        }

        // Load saved text size
        val savedTextSize = prefs.getFloat("text_size", 28f)
        if (savedTextSize != currentTextSize) {
            currentTextSize = savedTextSize
            terminalView.setTextSize(currentTextSize.toInt())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Keyboard
    // ═══════════════════════════════════════════════════════════════

    private fun setupKeyboardListener() {
        val rootView = window.decorView.rootView
        keyboardHeightThreshold = resources.displayMetrics.heightPixels / 4

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keyboardHeight = screenHeight - rect.bottom
            isKeyboardVisible = keyboardHeight > keyboardHeightThreshold
        }
    }

    private fun toggleKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        if (isKeyboardVisible) {
            imm.hideSoftInputFromWindow(terminalView.windowToken, 0)
        } else {
            terminalView.requestFocus()
            imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Gesture Detection
    // ═══════════════════════════════════════════════════════════════

    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                terminalView.requestFocus()
                if (!isKeyboardVisible) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT)
                }
                return true
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector?.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    // ═══════════════════════════════════════════════════════════════
    // URL Watcher
    // ═══════════════════════════════════════════════════════════════

    private fun setupUrlWatcher() {
        val urlFile = File(File(bootstrapInstaller.homeDir, ".termux"), "url_to_open")

        urlWatcherRunnable = object : Runnable {
            override fun run() {
                try {
                    if (urlFile.exists()) {
                        val url = urlFile.readText().trim()
                        urlFile.delete()

                        if (url.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "URL watcher error", e)
                }
                uiHandler.postDelayed(this, urlWatchInterval)
            }
        }
        uiHandler.postDelayed(urlWatcherRunnable!!, urlWatchInterval)
    }

    // ═══════════════════════════════════════════════════════════════
    // AI Launch
    // ═══════════════════════════════════════════════════════════════

    private fun launchAI(ai: String) {
        lifecycleScope.launch {
            delay(1000) // Wait for session to be ready

            // Add --dangerously-skip-permissions flag if Power Mode is ON
            val command = when (ai) {
                SetupWizard.AI_CLAUDE -> if (powerModeEnabled) "claude --dangerously-skip-permissions\n" else "claude\n"
                SetupWizard.AI_GEMINI -> "gemini\n"
                SetupWizard.AI_CODEX -> "codex\n"
                else -> return@launch
            }

            withContext(Dispatchers.Main) {
                session?.write(command.toByteArray(), 0, command.length)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Dialogs
    // ═══════════════════════════════════════════════════════════════

    private fun showMoreOptions() {
        val options = arrayOf("Copy All", "Paste", "New Session", "Kill Session", "Reset Terminal")
        AlertDialog.Builder(this)
            .setTitle("Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> copyAllToClipboard()
                    1 -> pasteFromClipboard()
                    2 -> createSession()
                    3 -> killCurrentSession()
                    4 -> resetTerminal()
                }
            }
            .show()
    }

    private fun showTextSizeDialog() {
        val sizes = arrayOf("Small (20)", "Medium (28)", "Large (36)", "Extra Large (44)")
        AlertDialog.Builder(this)
            .setTitle("Text Size")
            .setItems(sizes) { _, which ->
                currentTextSize = when (which) {
                    0 -> 20f
                    1 -> 28f
                    2 -> 36f
                    3 -> 44f
                    else -> 28f
                }
                terminalView.setTextSize(currentTextSize.toInt())
                updateTerminalSize()

                // Save preference
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putFloat("text_size", currentTextSize)
                    .apply()
            }
            .show()
    }

    private fun showAIInstallMenu() {
        val options = arrayOf(
            "Launch Claude",
            "Launch Gemini",
            "Launch Codex",
            "---",
            "Reinstall AI Tools"
        )
        AlertDialog.Builder(this)
            .setTitle("AI Tools")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchAI(SetupWizard.AI_CLAUDE)
                    1 -> launchAI(SetupWizard.AI_GEMINI)
                    2 -> launchAI(SetupWizard.AI_CODEX)
                    4 -> reinstallAITools()
                }
            }
            .show()
    }

    private fun reinstallAITools() {
        AlertDialog.Builder(this)
            .setTitle("Reinstall AI Tools")
            .setMessage("This will reinstall Claude Code, Gemini CLI, and Codex CLI.\n\nThis may take a few minutes.")
            .setPositiveButton("Reinstall") { _, _ ->
                Toast.makeText(this, "Reinstalling AI tools...", Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    delay(500)
                    withContext(Dispatchers.Main) {
                        // Run reinstall commands - create .gyp config first for native modules
                        val commands = """
                            echo "Reinstalling AI tools..."
                            mkdir -p ~/.gyp && printf "{\n  'variables': {\n    'android_ndk_path': ''\n  }\n}\n" > ~/.gyp/include.gypi
                            echo "Created ~/.gyp/include.gypi for native module builds"
                            npm install -g @anthropic-ai/claude-code
                            npm install -g @google/gemini-cli
                            npm install -g @openai/codex
                            echo "Done! AI tools reinstalled."
                            echo "Note: Run 'gemini' to start (may need 'gemini --debug' to authenticate)"
                        """.trimIndent() + "\n"
                        session?.write(commands.toByteArray(), 0, commands.length)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun installVercelCLI() {
        AlertDialog.Builder(this)
            .setTitle("Install Vercel CLI")
            .setMessage("This opens a new terminal tab to install Vercel CLI.\n\nComplete the login in browser, then switch back to your main tab.\n\nClaude can then use 'vercel' commands.")
            .setPositiveButton("Install") { _, _ ->
                createSession()  // Opens new terminal tab
                lifecycleScope.launch {
                    delay(500)  // Wait for session
                    withContext(Dispatchers.Main) {
                        val cmd = "npm i -g vercel && vercel login\n"
                        session?.write(cmd.toByteArray(), 0, cmd.length)
                        Toast.makeText(this@MainActivity, "Installing Vercel CLI in new tab...", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun installGitHubCLI() {
        AlertDialog.Builder(this)
            .setTitle("Install GitHub CLI")
            .setMessage("This opens a new terminal tab to install GitHub CLI.\n\nComplete the login in browser, then switch back to your main tab.\n\nClaude can then use 'gh' and 'git' commands.")
            .setPositiveButton("Install") { _, _ ->
                createSession()  // Opens new terminal tab
                lifecycleScope.launch {
                    delay(500)  // Wait for session
                    withContext(Dispatchers.Main) {
                        val cmd = "pkg install -y gh git && gh auth login\n"
                        session?.write(cmd.toByteArray(), 0, cmd.length)
                        Toast.makeText(this@MainActivity, "Installing GitHub CLI in new tab...", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun installSupabaseCLI() {
        AlertDialog.Builder(this)
            .setTitle("Install Supabase CLI")
            .setMessage("This opens a new terminal tab to install Supabase CLI.\n\nRequires Go + Supabase (~130MB download).\n\nComplete the login in browser, then switch back to your main tab.\n\nClaude can then use 'supabase' commands.")
            .setPositiveButton("Install") { _, _ ->
                createSession()
                lifecycleScope.launch {
                    delay(500)
                    withContext(Dispatchers.Main) {
                        val cmd = "echo 'Installing Supabase CLI...' && pkg install -y golang && go install github.com/supabase/cli@v1.220.0 && mv ~/go/bin/cli ~/go/bin/supabase 2>/dev/null; grep -q 'go/bin' ~/.bashrc || echo 'export PATH=\"\$HOME/go/bin:\$PATH\"' >> ~/.bashrc && export PATH=\"\$HOME/go/bin:\$PATH\" && echo '' && echo 'Supabase CLI installed! Starting login...' && echo '' && supabase login\n"
                        session?.write(cmd.toByteArray(), 0, cmd.length)
                        Toast.makeText(this@MainActivity, "Installing Supabase CLI in new tab...", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * AI Briefing - Fetches comprehensive MobileCLI documentation for AI assistants.
     * This allows Claude/Gemini/Codex to understand the full capabilities of the environment.
     * Documentation is hosted on mobilecli.com for security (hides GitHub structure from users).
     */
    private fun showAIBriefing() {
        val cmd = "curl -s https://mobilecli.com/ai-briefing.md\n"
        session?.write(cmd.toByteArray(), 0, cmd.length)
        Toast.makeText(this, "Loading AI Briefing...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Update - Fetches latest updates and instructions from mobilecli.com.
     * Allows the website to remotely provide updates, new commands, or instructions
     * to users without requiring an app rebuild. Content is controlled via website.
     */
    private fun checkForUpdates() {
        val cmd = "curl -s https://mobilecli.com/update.md\n"
        session?.write(cmd.toByteArray(), 0, cmd.length)
        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
    }

    private fun showHelp() {
        AlertDialog.Builder(this)
            .setTitle("MobileCLI Help")
            .setMessage("""
                Commands:
                • claude - Start Claude Code AI
                • gemini - Start Gemini CLI
                • codex - Start Codex CLI
                • pkg install <package> - Install packages

                Gestures:
                • Swipe from left - Open menu
                • Pinch - Zoom text
                • Tap - Show keyboard
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle("MobileCLI")
            .setMessage("""
                Version 2.0.0

                AI-Powered Mobile Terminal
                by MobileCLI Team

                ROOT WITHOUT ROOT
                79 Android permissions give you root-equivalent
                access to sensors, cameras, SMS, calls, contacts,
                GPS, Bluetooth, NFC, IR, biometrics, and more.

                Built on open source software.
                See "Open Source Licenses" for attributions.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLicenses() {
        AlertDialog.Builder(this)
            .setTitle("Open Source Licenses")
            .setMessage("""
                MobileCLI is built with open source technology.

                TERMINAL RENDERING
                terminal-view & terminal-emulator libraries
                Apache License 2.0

                Based on Android Terminal Emulator by Jack Palevich
                Adapted by Fredrik Fornwall and contributors

                Source: github.com/termux
                License: apache.org/licenses/LICENSE-2.0
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPrivacyPolicy() {
        AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage("""
                MOBILECLI PRIVACY POLICY
                Last Updated: January 2026

                WHAT WE COLLECT
                MobileCLI does NOT collect, transmit, or store any
                personal data on external servers. All data remains
                on your device.

                PERMISSIONS
                MobileCLI requests 79 Android permissions to provide
                root-equivalent functionality. These permissions allow
                the app and AI assistants to:
                - Access device sensors and hardware
                - Read/write files and media
                - Access contacts, SMS, call logs
                - Use camera, microphone, location
                - Control Bluetooth, NFC, IR
                - Send notifications

                All permission usage is LOCAL ONLY. No data is sent
                to MobileCLI servers.

                AI ASSISTANTS
                When you use Claude, Gemini, or Codex, your queries
                are sent to their respective providers (Anthropic,
                Google, OpenAI). Review their privacy policies for
                details on how they handle your data.

                DATA STORAGE
                All terminal data, configurations, and installed
                packages are stored locally in the app's private
                directory.

                CONTACT
                Questions? Contact the MobileCLI Team.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showTermsOfService() {
        AlertDialog.Builder(this)
            .setTitle("Terms of Service")
            .setMessage("""
                MOBILECLI TERMS OF SERVICE
                Last Updated: January 2026

                ACCEPTANCE OF TERMS
                By installing, accessing, or using MobileCLI, you
                agree to be bound by these Terms of Service.

                DISCLAIMER OF WARRANTIES
                MOBILECLI IS PROVIDED "AS IS" WITHOUT WARRANTY OF
                ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
                LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
                FITNESS FOR A PARTICULAR PURPOSE, AND
                NONINFRINGEMENT.

                LIMITATION OF LIABILITY
                IN NO EVENT SHALL THE MOBILECLI TEAM, ITS
                DEVELOPERS, CONTRIBUTORS, OR AFFILIATES BE LIABLE
                FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
                EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING BUT
                NOT LIMITED TO PROCUREMENT OF SUBSTITUTE GOODS OR
                SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
                BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
                THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
                LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
                OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
                THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
                OF SUCH DAMAGE.

                ASSUMPTION OF RISK
                You acknowledge that MobileCLI provides powerful
                system-level access including:
                - 79 Android permissions
                - AI assistants that can execute commands
                - Power Mode for autonomous AI operation
                - Access to sensors, files, contacts, SMS, etc.

                YOU ASSUME ALL RISK AND LIABILITY for any actions
                taken by the app or AI assistants. You are solely
                responsible for:
                - Commands executed in the terminal
                - AI assistant actions and outputs
                - Data accessed or modified
                - Messages sent (SMS, etc.)
                - Any consequences of Power Mode usage

                INDEMNIFICATION
                You agree to indemnify, defend, and hold harmless
                the MobileCLI Team from any claims, damages, or
                expenses arising from your use of the app.

                USER RESPONSIBILITY
                You are responsible for ensuring your use of
                MobileCLI complies with all applicable laws and
                regulations in your jurisdiction.

                MODIFICATIONS
                We reserve the right to modify these terms at any
                time. Continued use constitutes acceptance.

                By using MobileCLI, you acknowledge that you have
                read, understood, and agree to these terms.
            """.trimIndent())
            .setPositiveButton("I Agree", null)
            .show()
    }

    private fun showSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val options = arrayOf(
            "Text Size",
            "Wake Lock on Startup: ${if (prefs.getBoolean("wake_lock_startup", false)) "ON" else "OFF"}",
            "Default AI: ${prefs.getString("default_ai", "None") ?: "None"}",
            "Reset App (Run Setup Again)"
        )

        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showTextSizeDialog()
                    1 -> toggleWakeLockStartup()
                    2 -> showDefaultAIDialog()
                    3 -> confirmResetApp()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun toggleWakeLockStartup() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getBoolean("wake_lock_startup", false)
        prefs.edit().putBoolean("wake_lock_startup", !current).apply()

        val status = if (!current) "enabled" else "disabled"
        Toast.makeText(this, "Wake lock on startup $status", Toast.LENGTH_SHORT).show()

        // Re-show settings to see updated value
        showSettings()
    }

    private fun showDefaultAIDialog() {
        val options = arrayOf("None", "Claude", "Gemini", "Codex")
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        AlertDialog.Builder(this)
            .setTitle("Default AI on Startup")
            .setItems(options) { _, which ->
                val aiChoice = when (which) {
                    1 -> "Claude"
                    2 -> "Gemini"
                    3 -> "Codex"
                    else -> "None"
                }
                prefs.edit().putString("default_ai", aiChoice).apply()
                Toast.makeText(this, "Default AI set to $aiChoice", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun confirmResetApp() {
        AlertDialog.Builder(this)
            .setTitle("Reset App")
            .setMessage("This will clear all settings and run the setup wizard again.\n\nYour terminal data and installed packages will NOT be deleted.")
            .setPositiveButton("Reset") { _, _ ->
                // Clear setup complete flag
                getSharedPreferences("mobilecli_setup", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()

                // Restart app
                val intent = Intent(this, SetupWizard::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copyAllToClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val transcript = session?.emulator?.screen?.transcriptText ?: return
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Terminal", transcript))
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        session?.write(text.toByteArray(), 0, text.length)
    }

    private fun killCurrentSession() {
        if (sessions.size <= 1) {
            Toast.makeText(this, "Cannot kill last session", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionToKill = sessions.getOrNull(currentSessionIndex) ?: return
        termuxService?.removeSession(sessionToKill)
        sessionToKill.finishIfRunning()

        if (currentSessionIndex >= sessions.size) {
            currentSessionIndex = sessions.size - 1
        }
        val newSession = sessions.getOrNull(currentSessionIndex)
        if (newSession != null) {
            terminalView.attachSession(newSession)
        }
        updateSessionTabs()
    }

    private fun resetTerminal() {
        session?.reset()
        terminalView.postInvalidate()
    }

    // ═══════════════════════════════════════════════════════════════
    // TerminalViewClient Implementation
    // ═══════════════════════════════════════════════════════════════

    override fun onTextChanged(changedSession: TerminalSession) {
        if (changedSession == session) {
            terminalView.postInvalidate()
        }
    }

    override fun onTitleChanged(changedSession: TerminalSession) {}

    override fun onSessionFinished(finishedSession: TerminalSession) {
        runOnUiThread {
            val index = sessions.indexOf(finishedSession)
            if (index < 0) return@runOnUiThread

            // Remove the dead session from service
            termuxService?.removeSession(finishedSession)

            Toast.makeText(this, "Session closed", Toast.LENGTH_SHORT).show()

            // Check if we have any sessions left
            if (sessions.isEmpty()) {
                // Create a new session since we need at least one
                createSession()
            } else {
                // Switch to another session if the finished one was current
                if (index == currentSessionIndex || currentSessionIndex >= sessions.size) {
                    currentSessionIndex = (sessions.size - 1).coerceAtLeast(0)
                }
                // Attach to the new current session
                sessions.getOrNull(currentSessionIndex)?.let { newSession ->
                    terminalView.attachSession(newSession)
                }
                updateSessionTabs()
            }
        }
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Terminal", text))
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(this).toString()
            session?.emulator?.paste(text)
        }
    }

    override fun onBell(session: TerminalSession) {
        // Optional: vibrate or beep
    }

    override fun onColorsChanged(session: TerminalSession) {
        terminalView.postInvalidate()
    }

    override fun onTerminalCursorStateChange(state: Boolean) {}

    override fun logError(tag: String?, message: String?) {
        Log.e(tag ?: TAG, message ?: "Unknown error")
    }

    override fun logWarn(tag: String?, message: String?) {
        Log.w(tag ?: TAG, message ?: "Unknown warning")
    }

    override fun logInfo(tag: String?, message: String?) {
        Log.i(tag ?: TAG, message ?: "")
    }

    override fun logDebug(tag: String?, message: String?) {
        Log.d(tag ?: TAG, message ?: "")
    }

    override fun logVerbose(tag: String?, message: String?) {
        Log.v(tag ?: TAG, message ?: "")
    }

    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {
        Log.e(tag ?: TAG, message, e)
    }

    override fun logStackTrace(tag: String?, e: Exception?) {
        Log.e(tag ?: TAG, "Error", e)
    }

    override fun onScale(scale: Float): Float {
        val newSize = (currentTextSize * scale).coerceIn(minTextSize, maxTextSize)
        if (newSize != currentTextSize) {
            currentTextSize = newSize
            terminalView.setTextSize(currentTextSize.toInt())
            updateTerminalSize()
        }
        return scale
    }

    override fun onSingleTapUp(e: MotionEvent?) {
        terminalView.requestFocus()
        if (!isKeyboardVisible) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false

    override fun shouldEnforceCharBasedInput(): Boolean = true

    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false

    override fun isTerminalViewSelected(): Boolean = true

    override fun copyModeChanged(copyMode: Boolean) {}

    override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean {
        return false
    }

    override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?): Boolean {
        // Return false to allow TerminalView's text selection handles to appear
        // The native ActionMode menu (Copy, Paste, More) will show automatically
        // "More" button triggers onCreateContextMenu via registerForContextMenu()
        return false
    }

    // ═══════════════════════════════════════════════════════════════
    // Context Menu (triggered by "More" button in text selection)
    // ═══════════════════════════════════════════════════════════════

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu?.setHeaderTitle("Terminal")
        menu?.add(0, MENU_COPY_ALL, 0, "Copy All")
        menu?.add(0, MENU_PASTE, 1, "Paste")
        menu?.add(0, MENU_SELECT_ALL, 2, "Select All")
        menu?.add(0, MENU_NEW_SESSION, 3, "New Session")
        menu?.add(0, MENU_KILL_SESSION, 4, "Kill Session")
        menu?.add(0, MENU_RESET_TERMINAL, 5, "Reset Terminal")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_COPY_ALL -> {
                copyAllToClipboard()
                true
            }
            MENU_PASTE -> {
                pasteFromClipboard()
                true
            }
            MENU_SELECT_ALL -> {
                copyAllToClipboard()
                Toast.makeText(this, "All text copied to clipboard", Toast.LENGTH_SHORT).show()
                true
            }
            MENU_NEW_SESSION -> {
                createSession()
                true
            }
            MENU_KILL_SESSION -> {
                killCurrentSession()
                true
            }
            MENU_RESET_TERMINAL -> {
                resetTerminal()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun readControlKey(): Boolean = ctrlPressed

    override fun readAltKey(): Boolean = altPressed

    override fun readFnKey(): Boolean = false

    override fun readShiftKey(): Boolean = false

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean {
        resetModifiers()
        return false
    }

    override fun onEmulatorSet() {
        updateTerminalSize()
    }

    override fun getTerminalCursorStyle(): Int = TerminalEmulator.TERMINAL_CURSOR_STYLE_BLOCK

    // ═══════════════════════════════════════════════════════════════
    // Lifecycle - Sessions persist across activity recreation
    // ═══════════════════════════════════════════════════════════════

    override fun onResume() {
        super.onResume()
        terminalView.onScreenUpdated()

        // Re-attach to current session if we have one
        if (serviceBound && sessions.isNotEmpty()) {
            val currentSession = sessions.getOrNull(currentSessionIndex)
            if (currentSession != null) {
                terminalView.attachSession(currentSession)
                termuxService?.setSessionClient(this)
                Log.i(TAG, "Resumed with session ${currentSessionIndex + 1} of ${sessions.size}")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Save session index in case we get killed
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_CURRENT_SESSION, currentSessionIndex)
            .apply()
        Log.i(TAG, "Stopped, saved session index: $currentSessionIndex")
    }

    override fun onDestroy() {
        super.onDestroy()
        urlWatcherRunnable?.let { uiHandler.removeCallbacks(it) }
        // Don't stop the service - let sessions persist!
        // Only unbind, don't stop
        unbindTermuxService()
        Log.i(TAG, "Destroyed, sessions remain in service")
    }

    /**
     * Handle configuration changes (rotation, split-screen, etc.)
     * The activity handles these instead of restarting, preserving session state.
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "Configuration changed: orientation=${newConfig.orientation}, screenLayout=${newConfig.screenLayout}")

        // Update terminal size after configuration change
        uiHandler.postDelayed({
            updateTerminalSize()
            terminalView.onScreenUpdated()
        }, 100)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawers()
        } else if (isKeyboardVisible) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(terminalView.windowToken, 0)
        } else {
            moveTaskToBack(true)
        }
    }
}
