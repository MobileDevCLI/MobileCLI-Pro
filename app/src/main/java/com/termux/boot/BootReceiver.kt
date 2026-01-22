package com.termux.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File

/**
 * BootReceiver - Run scripts on device boot.
 *
 * Similar to Termux:Boot functionality.
 * Executes scripts in ~/.termux/boot/ when device boots.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TermuxBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.i(TAG, "Boot completed, checking for boot scripts")

        // Check for boot scripts directory
        val bootDir = File(context.filesDir, "home/.termux/boot")
        if (!bootDir.exists() || !bootDir.isDirectory) {
            Log.i(TAG, "No boot scripts directory found")
            return
        }

        val scripts = bootDir.listFiles { file ->
            file.isFile && file.canExecute()
        }

        if (scripts.isNullOrEmpty()) {
            Log.i(TAG, "No executable boot scripts found")
            return
        }

        Log.i(TAG, "Found ${scripts.size} boot script(s)")

        // Start TermuxService to run scripts
        try {
            val serviceIntent = Intent(context, com.termux.app.TermuxService::class.java).apply {
                putExtra("boot_scripts", scripts.map { it.absolutePath }.toTypedArray())
            }
            context.startService(serviceIntent)
            Log.i(TAG, "Started TermuxService for boot scripts")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start TermuxService", e)
        }
    }
}
