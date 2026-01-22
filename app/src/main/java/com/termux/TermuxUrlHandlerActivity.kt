package com.termux

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log

/**
 * TermuxUrlHandlerActivity - Opens URLs from shell with proper Activity context.
 *
 * This activity solves Android 10+ background activity start restrictions.
 * Shell scripts can send intents to this activity which then opens URLs
 * in the browser with proper foreground context.
 */
class TermuxUrlHandlerActivity : Activity() {

    companion object {
        private const val TAG = "TermuxUrlHandler"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri != null) {
            openUrl(uri)
        } else {
            Log.w(TAG, "No URI provided")
        }

        finish()
    }

    private fun openUrl(uri: Uri) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(browserIntent)
            Log.i(TAG, "Opened URL: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $uri", e)
        }
    }
}
