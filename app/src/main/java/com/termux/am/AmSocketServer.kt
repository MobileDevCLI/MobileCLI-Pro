package com.termux.am

import android.content.Context
import android.content.Intent
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.Uri
import android.os.Process
import android.util.Log
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AmSocketServer - Unix socket server for executing am (Activity Manager) commands.
 *
 * This is the proper way to execute am commands from within Termux/MobileCLI.
 * It's 10x faster than app_process approach and has proper permissions.
 *
 * Protocol:
 * 1. Client connects to socket
 * 2. Client sends command string (space-separated args)
 * 3. Client shuts down write side
 * 4. Server sends: exit_code\0stdout\0stderr\0
 *
 * Socket path: /data/data/com.termux/files/apps/com.termux/termux-am/am.sock
 *
 * Based on: https://github.com/termux/termux-am-socket
 */
class AmSocketServer(private val context: Context) {

    companion object {
        private const val TAG = "AmSocketServer"

        // Socket path - matches real Termux
        const val SOCKET_DIR = "/data/data/com.termux/files/apps/com.termux/termux-am"
        const val SOCKET_NAME = "am.sock"
        const val SOCKET_PATH = "$SOCKET_DIR/$SOCKET_NAME"

        // For filesystem socket (not abstract namespace)
        private const val SOCKET_ADDRESS_PREFIX = "\u0000" // Abstract namespace prefix
    }

    private var serverSocket: LocalServerSocket? = null
    private var executor: ExecutorService? = null
    private val isRunning = AtomicBoolean(false)
    private val myUid = Process.myUid()

    /**
     * Start the socket server.
     */
    fun start(): Boolean {
        if (isRunning.get()) {
            Log.w(TAG, "Server already running")
            return true
        }

        try {
            // Create socket directory
            val socketDir = File(SOCKET_DIR)
            if (!socketDir.exists()) {
                socketDir.mkdirs()
                Log.i(TAG, "Created socket directory: $SOCKET_DIR")
            }

            // Remove old socket file if exists
            val socketFile = File(SOCKET_PATH)
            if (socketFile.exists()) {
                socketFile.delete()
                Log.i(TAG, "Removed old socket file")
            }

            // Create server socket using filesystem namespace (not abstract)
            // This allows the socket to be accessed by file path
            serverSocket = LocalServerSocket(SOCKET_PATH)

            // Start executor for handling clients
            executor = Executors.newCachedThreadPool()
            isRunning.set(true)

            // Start accept loop in background
            executor?.execute {
                acceptLoop()
            }

            Log.i(TAG, "Socket server started at $SOCKET_PATH")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start socket server", e)
            stop()
            return false
        }
    }

    /**
     * Stop the socket server.
     */
    fun stop() {
        isRunning.set(false)

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        }
        serverSocket = null

        executor?.shutdownNow()
        executor = null

        // Clean up socket file
        try {
            File(SOCKET_PATH).delete()
        } catch (e: Exception) {
            // Ignore
        }

        Log.i(TAG, "Socket server stopped")
    }

    /**
     * Accept loop - runs in background thread.
     */
    private fun acceptLoop() {
        Log.i(TAG, "Accept loop started")

        while (isRunning.get()) {
            try {
                val client = serverSocket?.accept() ?: break

                // Handle client in separate thread
                executor?.execute {
                    handleClient(client)
                }

            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(TAG, "Error accepting client", e)
                }
            }
        }

        Log.i(TAG, "Accept loop ended")
    }

    /**
     * Handle a client connection.
     */
    private fun handleClient(client: LocalSocket) {
        try {
            // Validate client credentials
            val credentials = client.peerCredentials
            val clientUid = credentials.uid

            // Only allow same user or root
            if (clientUid != myUid && clientUid != 0) {
                Log.w(TAG, "Rejecting client with UID $clientUid (expected $myUid or 0)")
                sendError(client, 1, "Permission denied: UID mismatch")
                return
            }

            Log.d(TAG, "Accepted client with UID $clientUid, PID ${credentials.pid}")

            // Read command from client
            val inputStream = client.inputStream
            val command = readCommand(inputStream)

            if (command.isNullOrBlank()) {
                sendError(client, 1, "Empty command")
                return
            }

            Log.i(TAG, "Received command: $command")

            // Parse and execute command
            val result = executeAmCommand(command)

            // Send response
            sendResponse(client, result)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
            try {
                sendError(client, 1, "Internal error: ${e.message}")
            } catch (e2: Exception) {
                // Ignore
            }
        } finally {
            try {
                client.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Read command string from client.
     */
    private fun readCommand(input: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        val buf = ByteArray(1024)

        try {
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                buffer.write(buf, 0, n)
            }
        } catch (e: Exception) {
            // Client may have closed connection
        }

        return buffer.toString(Charsets.UTF_8).trim()
    }

    /**
     * Execute an am command.
     */
    private fun executeAmCommand(command: String): AmResult {
        val args = command.split(Regex("\\s+"))

        if (args.isEmpty()) {
            return AmResult(1, "", "No command specified")
        }

        return when (args[0]) {
            "start" -> executeStart(args.drop(1))
            "startservice" -> executeStartService(args.drop(1))
            "broadcast" -> executeBroadcast(args.drop(1))
            "force-stop" -> executeForceStop(args.drop(1))
            "--version" -> AmResult(0, "0.9.0-mobilecli", "")
            else -> AmResult(1, "", "Unknown command: ${args[0]}")
        }
    }

    /**
     * Execute 'am start' command.
     */
    private fun executeStart(args: List<String>): AmResult {
        try {
            val intent = parseIntent(args)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)

            val output = buildString {
                append("Starting: Intent { ")
                intent.action?.let { append("act=$it ") }
                intent.data?.let { append("dat=$it ") }
                intent.component?.let { append("cmp=$it ") }
                append("}")
            }

            return AmResult(0, output, "")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start activity", e)
            return AmResult(1, "", "Error: ${e.message}")
        }
    }

    /**
     * Execute 'am startservice' command.
     */
    private fun executeStartService(args: List<String>): AmResult {
        try {
            val intent = parseIntent(args)
            context.startService(intent)
            return AmResult(0, "Starting service: ${intent.component}", "")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service", e)
            return AmResult(1, "", "Error: ${e.message}")
        }
    }

    /**
     * Execute 'am broadcast' command.
     */
    private fun executeBroadcast(args: List<String>): AmResult {
        try {
            val intent = parseIntent(args)
            context.sendBroadcast(intent)
            return AmResult(0, "Broadcasting: ${intent.action}", "")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast", e)
            return AmResult(1, "", "Error: ${e.message}")
        }
    }

    /**
     * Execute 'am force-stop' command.
     */
    private fun executeForceStop(args: List<String>): AmResult {
        // Can't force-stop other apps without system permissions
        return AmResult(1, "", "force-stop requires system permissions")
    }

    /**
     * Parse intent from command-line arguments.
     * Supports: -a (action), -d (data), -t (type), -c (category), -n (component),
     *           -e/-es (string extra), -ei (int extra), -ez (boolean extra),
     *           --user (ignored, always uses current user)
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
                "-el", "--el" -> {
                    if (i + 2 < args.size) {
                        val key = args[++i]
                        val value = args[++i].toLongOrNull() ?: 0L
                        intent.putExtra(key, value)
                    }
                }
                "-ef", "--ef" -> {
                    if (i + 2 < args.size) {
                        val key = args[++i]
                        val value = args[++i].toFloatOrNull() ?: 0f
                        intent.putExtra(key, value)
                    }
                }
                "--user" -> {
                    // Ignore user flag, always use current user
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

    /**
     * Send successful response.
     */
    private fun sendResponse(client: LocalSocket, result: AmResult) {
        try {
            val output = client.outputStream

            // Send exit code (null-terminated)
            output.write("${result.exitCode}\u0000".toByteArray())

            // Send stdout (null-terminated)
            output.write("${result.stdout}\u0000".toByteArray())

            // Send stderr (null-terminated)
            output.write("${result.stderr}\u0000".toByteArray())

            output.flush()

        } catch (e: Exception) {
            Log.e(TAG, "Error sending response", e)
        }
    }

    /**
     * Send error response.
     */
    private fun sendError(client: LocalSocket, exitCode: Int, message: String) {
        sendResponse(client, AmResult(exitCode, "", message))
    }

    /**
     * Result of an am command.
     */
    data class AmResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )
}
