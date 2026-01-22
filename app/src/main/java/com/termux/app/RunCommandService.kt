package com.termux.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.File

/**
 * RunCommandService - Execute commands from external apps.
 *
 * Allows apps like Tasker to run commands in MobileCLI.
 * Protected by com.termux.permission.RUN_COMMAND permission.
 *
 * Usage from Tasker:
 * Intent action: com.termux.RUN_COMMAND
 * Extra: com.termux.RUN_COMMAND_PATH = "/data/data/com.termux/files/home/script.sh"
 * Extra: com.termux.RUN_COMMAND_ARGUMENTS = arrayOf("arg1", "arg2")
 * Extra: com.termux.RUN_COMMAND_WORKDIR = "/data/data/com.termux/files/home"
 * Extra: com.termux.RUN_COMMAND_BACKGROUND = true
 */
class RunCommandService : Service() {

    companion object {
        private const val TAG = "RunCommandService"

        const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val commandPath = intent.getStringExtra(EXTRA_COMMAND_PATH)
        if (commandPath == null) {
            Log.e(TAG, "No command path provided")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val arguments = intent.getStringArrayExtra(EXTRA_ARGUMENTS) ?: emptyArray()
        val workdir = intent.getStringExtra(EXTRA_WORKDIR) ?: "${filesDir.absolutePath}/home"
        val background = intent.getBooleanExtra(EXTRA_BACKGROUND, false)

        Log.i(TAG, "Running command: $commandPath with ${arguments.size} args, background=$background")

        // Validate command path
        val commandFile = File(commandPath)
        if (!commandFile.exists()) {
            Log.e(TAG, "Command file does not exist: $commandPath")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // Forward to TermuxService
        try {
            val termuxIntent = Intent(this, TermuxService::class.java).apply {
                putExtra("run_command_path", commandPath)
                putExtra("run_command_args", arguments)
                putExtra("run_command_workdir", workdir)
                putExtra("run_command_background", background)
            }
            startService(termuxIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start TermuxService", e)
        }

        stopSelf(startId)
        return START_NOT_STICKY
    }
}
