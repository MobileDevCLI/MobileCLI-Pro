package com.termux

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

/**
 * TermuxAmDispatcherActivity - Handles 'am start' commands from terminal.
 *
 * Android 10+ blocks starting activities from background contexts.
 * This transparent activity receives intents from shell and dispatches
 * them with proper foreground Activity context.
 *
 * Usage from shell:
 * am start -n "com.termux/.TermuxAmDispatcherActivity" \
 *   --es target_action "android.intent.action.VIEW" \
 *   --es target_data "https://example.com"
 */
class TermuxAmDispatcherActivity : Activity() {

    companion object {
        private const val TAG = "TermuxAmDispatcher"
        const val EXTRA_TARGET_ACTION = "target_action"
        const val EXTRA_TARGET_DATA = "target_data"
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_TARGET_CLASS = "target_class"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            dispatchIntent()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch intent", e)
        }

        finish()
    }

    private fun dispatchIntent() {
        val targetAction = intent.getStringExtra(EXTRA_TARGET_ACTION) ?: Intent.ACTION_VIEW
        val targetData = intent.getStringExtra(EXTRA_TARGET_DATA)
        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        val targetClass = intent.getStringExtra(EXTRA_TARGET_CLASS)

        val dispatchIntent = Intent(targetAction).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (targetData != null) {
                data = android.net.Uri.parse(targetData)
            }

            if (targetPackage != null && targetClass != null) {
                setClassName(targetPackage, targetClass)
            } else if (targetPackage != null) {
                setPackage(targetPackage)
            }
        }

        startActivity(dispatchIntent)
        Log.i(TAG, "Dispatched intent: action=$targetAction, data=$targetData")
    }
}
