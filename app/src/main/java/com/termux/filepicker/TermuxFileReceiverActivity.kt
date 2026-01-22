package com.termux.filepicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

/**
 * TermuxFileReceiverActivity - Receives files shared from other apps.
 *
 * When users share files TO MobileCLI (via Android share sheet),
 * this activity receives them and copies to ~/downloads/.
 */
class TermuxFileReceiverActivity : Activity() {

    companion object {
        private const val TAG = "TermuxFileReceiver"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            Intent.ACTION_SEND -> handleSend()
            Intent.ACTION_VIEW -> handleView()
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
            }
        }

        finish()
    }

    private fun handleSend() {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (uri != null) {
            saveFile(uri)
        } else {
            // Check for text
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text != null) {
                saveText(text)
            }
        }
    }

    private fun handleView() {
        val uri = intent.data
        if (uri != null) {
            saveFile(uri)
        }
    }

    private fun saveFile(uri: Uri) {
        try {
            val downloadsDir = File(filesDir, "home/downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            // Get filename from URI
            var filename = uri.lastPathSegment ?: "shared_file"
            if (filename.contains("/")) {
                filename = filename.substringAfterLast("/")
            }

            val destFile = File(downloadsDir, filename)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(this, "Saved to ~/downloads/$filename", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Saved file: ${destFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save file", e)
            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveText(text: String) {
        try {
            val downloadsDir = File(filesDir, "home/downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val filename = "shared_text_${System.currentTimeMillis()}.txt"
            val destFile = File(downloadsDir, filename)
            destFile.writeText(text)

            Toast.makeText(this, "Saved to ~/downloads/$filename", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Saved text: ${destFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save text", e)
            Toast.makeText(this, "Failed to save text", Toast.LENGTH_SHORT).show()
        }
    }
}
