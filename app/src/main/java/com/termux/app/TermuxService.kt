package com.termux.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.termux.MainActivity
import com.termux.am.AmSocketServer
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

/**
 * TermuxService - Background service for terminal session management.
 *
 * CRITICAL for long-running sessions (days/weeks/months):
 * - Sessions persist even when activity is destroyed
 * - Wake lock support (CPU stays awake)
 * - WiFi lock support (network stays connected)
 * - Automatic reconnection when activity returns
 * - Survives configuration changes (rotation, split-screen)
 * - Foreground notification (required for Android 8+)
 *
 * Actions handled:
 * - com.termux.service_wake_lock: Acquire wake lock
 * - com.termux.service_wake_unlock: Release wake lock
 * - com.termux.service_stop: Stop the service
 */
class TermuxService : Service() {

    companion object {
        private const val TAG = "TermuxService"
        private const val NOTIFICATION_ID = 1337
        private const val CHANNEL_ID = "termux_service_channel"
        private const val MAX_SESSIONS = 10

        // Action constants - MUST match what shell scripts send
        const val ACTION_WAKE_LOCK = "com.termux.service_wake_lock"
        const val ACTION_WAKE_UNLOCK = "com.termux.service_wake_unlock"
        const val ACTION_STOP_SERVICE = "com.termux.service_stop"
        const val ACTION_SERVICE_EXECUTE = "com.termux.service_execute"
        const val ACTION_CREATE_SESSION = "com.termux.service_create_session"

        // Singleton instance for quick access
        @Volatile
        private var instance: TermuxService? = null

        fun getInstance(): TermuxService? = instance
    }

    // Binder for activity connection
    private val binder = LocalBinder()

    // Session management
    private val sessions = mutableListOf<TerminalSession>()
    private var sessionClient: TerminalSessionClient? = null

    // Wake/WiFi locks for background operation
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var isWakeLockHeld = false

    // Am socket server for proper URL opening
    private var amSocketServer: AmSocketServer? = null

    // File-based am command polling (fallback when socket client not available)
    private val commandHandler = Handler(Looper.getMainLooper())
    private var commandWatcherRunnable: Runnable? = null
    private val commandPollInterval = 200L // Poll every 200ms
    private val homeDir: File by lazy { File(filesDir, "home") }
    private val termuxDir: File by lazy { File(homeDir, ".termux") }
    private val commandFile: File by lazy { File(termuxDir, "am_command") }
    private val resultFile: File by lazy { File(termuxDir, "am_result") }

    inner class LocalBinder : Binder() {
        val service: TermuxService get() = this@TermuxService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "TermuxService created - sessions will persist")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // Start the Am socket server for proper URL opening
        // This is 10x faster than app_process and has proper permissions
        startAmSocketServer()

        // Start file-based command polling (fallback for when socket client not available)
        startCommandWatcher()
    }

    /**
     * Start the Am socket server.
     * This allows shell commands to execute am commands with proper app permissions.
     */
    private fun startAmSocketServer() {
        try {
            amSocketServer = AmSocketServer(this).apply {
                if (start()) {
                    Log.i(TAG, "Am socket server started at ${AmSocketServer.SOCKET_PATH}")
                } else {
                    Log.e(TAG, "Failed to start Am socket server")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Am socket server", e)
        }
    }

    /**
     * Stop the Am socket server.
     */
    private fun stopAmSocketServer() {
        try {
            amSocketServer?.stop()
            amSocketServer = null
            Log.i(TAG, "Am socket server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Am socket server", e)
        }
    }

    /**
     * Start the file-based command watcher.
     * This polls for am commands written by shell scripts.
     */
    private fun startCommandWatcher() {
        termuxDir.mkdirs()

        commandWatcherRunnable = object : Runnable {
            override fun run() {
                try {
                    if (commandFile.exists()) {
                        val command = commandFile.readText().trim()
                        commandFile.delete()

                        if (command.isNotEmpty()) {
                            Log.i(TAG, "Received am command: $command")
                            val result = executeAmCommand(command)
                            resultFile.writeText(result)
                            Log.i(TAG, "Command result written")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing command", e)
                    try {
                        resultFile.writeText("1\nError: ${e.message}\n")
                    } catch (e2: Exception) {
                        // Ignore
                    }
                }
                commandHandler.postDelayed(this, commandPollInterval)
            }
        }
        commandHandler.postDelayed(commandWatcherRunnable!!, commandPollInterval)
        Log.i(TAG, "Command watcher started")
    }

    /**
     * Stop the file-based command watcher.
     */
    private fun stopCommandWatcher() {
        commandWatcherRunnable?.let { commandHandler.removeCallbacks(it) }
        commandWatcherRunnable = null
        Log.i(TAG, "Command watcher stopped")
    }

    /**
     * Execute an am command and return result string.
     * Format: exit_code\nstdout\nstderr
     */
    private fun executeAmCommand(command: String): String {
        val args = command.split(Regex("\\s+"))

        if (args.isEmpty()) {
            return "1\n\nNo command specified"
        }

        return try {
            when (args[0]) {
                "start" -> executeAmStart(args.drop(1))
                "startservice" -> executeAmStartService(args.drop(1))
                "broadcast" -> executeAmBroadcast(args.drop(1))
                "--version" -> "0\n0.9.0-mobilecli-v54\n"
                else -> "1\n\nUnknown command: ${args[0]}"
            }
        } catch (e: Exception) {
            "1\n\nError: ${e.message}"
        }
    }

    /**
     * Execute 'am start' command.
     */
    private fun executeAmStart(args: List<String>): String {
        val intent = parseIntent(args)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Use PendingIntent for clean identity
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent, flags
        )
        pendingIntent.send()

        val output = buildString {
            append("Starting: Intent { ")
            intent.action?.let { append("act=$it ") }
            intent.data?.let { append("dat=$it ") }
            intent.component?.let { append("cmp=$it ") }
            append("}")
        }

        return "0\n$output\n"
    }

    /**
     * Execute 'am startservice' command.
     */
    private fun executeAmStartService(args: List<String>): String {
        val intent = parseIntent(args)
        startService(intent)
        return "0\nStarting service: ${intent.component}\n"
    }

    /**
     * Execute 'am broadcast' command.
     */
    private fun executeAmBroadcast(args: List<String>): String {
        val intent = parseIntent(args)
        sendBroadcast(intent)
        return "0\nBroadcasting: ${intent.action}\n"
    }

    /**
     * Parse intent from command-line arguments.
     */
    private fun parseIntent(args: List<String>): Intent {
        val intent = Intent()
        var i = 0

        while (i < args.size) {
            when (args[i]) {
                "-a" -> {
                    if (i + 1 < args.size) {
                        intent.action = args[++i]
                    }
                }
                "-d" -> {
                    if (i + 1 < args.size) {
                        intent.data = Uri.parse(args[++i])
                    }
                }
                "-t" -> {
                    if (i + 1 < args.size) {
                        intent.type = args[++i]
                    }
                }
                "-c" -> {
                    if (i + 1 < args.size) {
                        intent.addCategory(args[++i])
                    }
                }
                "-n" -> {
                    if (i + 1 < args.size) {
                        val componentStr = args[++i]
                        val parts = componentStr.split("/")
                        if (parts.size == 2) {
                            val className = if (parts[1].startsWith(".")) {
                                parts[0] + parts[1]
                            } else {
                                parts[1]
                            }
                            intent.setClassName(parts[0], className)
                        }
                    }
                }
                "-e", "-es", "--es" -> {
                    if (i + 2 < args.size) {
                        val key = args[++i]
                        val value = args[++i]
                        intent.putExtra(key, value)
                    }
                }
                "-ei", "--ei" -> {
                    if (i + 2 < args.size) {
                        val key = args[++i]
                        val value = args[++i].toIntOrNull() ?: 0
                        intent.putExtra(key, value)
                    }
                }
                "-ez", "--ez" -> {
                    if (i + 2 < args.size) {
                        val key = args[++i]
                        val value = args[++i].lowercase() == "true"
                        intent.putExtra(key, value)
                    }
                }
                "--user" -> {
                    if (i + 1 < args.size) i++
                }
                "-f" -> {
                    if (i + 1 < args.size) {
                        val flags = args[++i].toIntOrNull() ?: 0
                        intent.flags = intent.flags or flags
                    }
                }
                "-p" -> {
                    if (i + 1 < args.size) {
                        intent.`package` = args[++i]
                    }
                }
            }
            i++
        }

        return intent
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "onStartCommand: action=$action")

        when (action) {
            ACTION_WAKE_LOCK -> {
                acquireWakeLock()
                updateNotification()
            }
            ACTION_WAKE_UNLOCK -> {
                releaseWakeLock()
                updateNotification()
                // Stop service if no sessions and no wake lock
                if (sessions.isEmpty() && !isWakeLockHeld) {
                    stopSelf()
                }
            }
            ACTION_STOP_SERVICE -> {
                releaseWakeLock()
                stopForeground(true)
                stopSelf()
            }
            ACTION_SERVICE_EXECUTE -> {
                // Future: handle remote command execution
                Log.i(TAG, "Execute command requested")
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "TermuxService destroyed")
        instance = null
        stopCommandWatcher()
        stopAmSocketServer()
        releaseWakeLock()
        // Clean up sessions
        sessions.forEach { it.finishIfRunning() }
        sessions.clear()
        super.onDestroy()
    }

    // =====================================================
    // Session Management
    // =====================================================

    fun getSessions(): List<TerminalSession> = sessions.toList()

    fun getSessionCount(): Int = sessions.size

    fun setSessionClient(client: TerminalSessionClient) {
        sessionClient = client
        // Update all existing sessions with the new client
        sessions.forEach { session ->
            try {
                val field = session.javaClass.getDeclaredField("mClient")
                field.isAccessible = true
                field.set(session, client)
            } catch (e: Exception) {
                Log.w(TAG, "Could not update session client: ${e.message}")
            }
        }
    }

    /**
     * Create a new terminal session. This is the PRIMARY way to create sessions.
     * Sessions created here persist even when the activity is destroyed.
     */
    fun createSession(
        shell: String,
        cwd: String,
        args: Array<String>,
        env: Array<String>,
        client: TerminalSessionClient
    ): TerminalSession? {
        if (sessions.size >= MAX_SESSIONS) {
            Log.w(TAG, "Maximum sessions ($MAX_SESSIONS) reached")
            return null
        }

        return try {
            val session = TerminalSession(
                shell,
                cwd,
                args,
                env,
                2000, // transcript rows
                client
            )
            sessions.add(session)
            sessionClient = client
            updateNotification()
            Log.i(TAG, "Session created in service, total: ${sessions.size}")
            session
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create session in service", e)
            null
        }
    }

    fun addSession(session: TerminalSession) {
        sessions.add(session)
        updateNotification()
        Log.i(TAG, "Session added, total: ${sessions.size}")
    }

    fun removeSession(session: TerminalSession) {
        sessions.remove(session)
        updateNotification()
        Log.i(TAG, "Session removed, remaining: ${sessions.size}")

        // Don't stop service when sessions empty - keep alive for reconnection
        // Only stop if explicitly requested via ACTION_STOP_SERVICE
    }

    /**
     * Check if service has existing sessions that can be reconnected.
     */
    fun hasExistingSessions(): Boolean = sessions.isNotEmpty()

    /**
     * Get the current session index (for UI state restoration).
     */
    fun getCurrentSessionIndex(): Int = currentSessionIndex

    private var currentSessionIndex = 0

    fun setCurrentSessionIndex(index: Int) {
        if (index in sessions.indices) {
            currentSessionIndex = index
        }
    }

    // =====================================================
    // Wake Lock Management
    // =====================================================

    fun acquireWakeLock() {
        if (isWakeLockHeld) {
            Log.i(TAG, "Wake lock already held")
            return
        }

        try {
            // Acquire CPU wake lock
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MobileCLI::TermuxWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }

            // Acquire WiFi lock
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "MobileCLI::TermuxWifiLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }

            isWakeLockHeld = true
            Log.i(TAG, "Wake lock acquired - CPU and WiFi will stay awake")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    fun releaseWakeLock() {
        if (!isWakeLockHeld) {
            Log.i(TAG, "No wake lock to release")
            return
        }

        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null

            wifiLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wifiLock = null

            isWakeLockHeld = false
            Log.i(TAG, "Wake lock released - device can sleep")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }

    fun isWakeLockHeld(): Boolean = isWakeLockHeld

    // =====================================================
    // Notification Management
    // =====================================================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Terminal Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps terminal sessions running in background"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        // Intent to open the app
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to stop the service
        val stopIntent = Intent(this, TermuxService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to toggle wake lock
        val wakeLockIntent = Intent(this, TermuxService::class.java).apply {
            action = if (isWakeLockHeld) ACTION_WAKE_UNLOCK else ACTION_WAKE_LOCK
        }
        val wakeLockPendingIntent = PendingIntent.getService(
            this, 1, wakeLockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sessionInfo = when {
            sessions.isEmpty() -> "No active sessions"
            sessions.size == 1 -> "1 session"
            else -> "${sessions.size} sessions"
        }

        val wakeLockInfo = if (isWakeLockHeld) " • Wake lock held" else ""

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("MobileCLI")
            .setContentText("$sessionInfo$wakeLockInfo")
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(if (isWakeLockHeld) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Exit",
                stopPendingIntent
            )
            .addAction(
                android.R.drawable.ic_lock_lock,
                if (isWakeLockHeld) "Release lock" else "Acquire lock",
                wakeLockPendingIntent
            )

        return builder.build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }
}
