package com.termux.app

import android.content.BroadcastReceiver
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

/**
 * TermuxOpenReceiver - Handles file and URL opening broadcasts.
 *
 * This receiver MUST be at com.termux.app.TermuxOpenReceiver to match
 * the real Termux scripts which broadcast to this exact component name.
 *
 * The xdg-open and termux-open scripts send:
 * am broadcast -n "com.termux/com.termux.app.TermuxOpenReceiver" -d "URL"
 */
class TermuxOpenReceiver : BroadcastReceiver() {

    /**
     * ContentProvider for sharing files from Termux filesystem.
     * URI format: content://com.termux.files/path/to/file
     */
    class ContentProvider : android.content.ContentProvider() {

        companion object {
            private const val TAG = "TermuxFilesProvider"
        }

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor? = null

        override fun getType(uri: Uri): String {
            val path = uri.path ?: return "application/octet-stream"
            val extension = path.substringAfterLast('.', "")
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: "application/octet-stream"
        }

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ): Int = 0

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            val path = uri.path ?: throw FileNotFoundException("No path in URI: $uri")

            // Security: Only allow files under Termux directories
            val filesDir = context?.filesDir ?: throw FileNotFoundException("No context")
            val homeDir = File(filesDir, "home")
            val prefixDir = File(filesDir, "usr")

            val file = File(path)
            val canonicalPath = file.canonicalPath

            if (!canonicalPath.startsWith(homeDir.canonicalPath) &&
                !canonicalPath.startsWith(prefixDir.canonicalPath)) {
                throw SecurityException("Access denied to path: $path")
            }

            if (!file.exists()) {
                throw FileNotFoundException("File not found: $path")
            }

            val accessMode = when {
                mode.contains("w") -> ParcelFileDescriptor.MODE_READ_WRITE
                else -> ParcelFileDescriptor.MODE_READ_ONLY
            }

            return ParcelFileDescriptor.open(file, accessMode)
        }
    }

    companion object {
        private const val TAG = "TermuxOpenReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.data
        if (uri == null) {
            Log.e(TAG, "No URI provided in intent")
            return
        }

        Log.i(TAG, "Received open request for: $uri")
        Log.i(TAG, "Action: ${intent.action}")
        Log.i(TAG, "Scheme: ${uri.scheme}")

        val scheme = uri.scheme ?: ""

        // Handle based on scheme
        when {
            // For non-file URLs (http, https, mailto, etc.) - open directly
            scheme != "file" -> {
                openUrl(context, intent.action ?: Intent.ACTION_VIEW, uri, intent)
            }
            // For file:// URLs - also try to open directly
            else -> {
                openFile(context, intent.action ?: Intent.ACTION_VIEW, uri, intent)
            }
        }
    }

    private fun openUrl(context: Context, action: String, uri: Uri, originalIntent: Intent) {
        try {
            val openIntent = Intent(action, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // Check for chooser extra
                if (originalIntent.getBooleanExtra("chooser", false)) {
                    // Wrap in chooser
                    val chooser = Intent.createChooser(this, "Open with")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                    return
                }
            }

            context.startActivity(openIntent)
            Log.i(TAG, "Successfully started activity for URL: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $uri", e)
        }
    }

    private fun openFile(context: Context, action: String, uri: Uri, originalIntent: Intent) {
        try {
            // For files, we need to handle content-type
            val contentType = originalIntent.getStringExtra("content-type")

            val openIntent = Intent(action).apply {
                if (contentType != null) {
                    setDataAndType(uri, contentType)
                } else {
                    data = uri
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Check for chooser extra
            if (originalIntent.getBooleanExtra("chooser", false)) {
                val chooser = Intent.createChooser(openIntent, "Open with")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } else {
                context.startActivity(openIntent)
            }

            Log.i(TAG, "Successfully started activity for file: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file: $uri", e)

            // Fallback: try as URL
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                Log.i(TAG, "Fallback succeeded for: $uri")
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback also failed: $uri", e2)
            }
        }
    }
}
