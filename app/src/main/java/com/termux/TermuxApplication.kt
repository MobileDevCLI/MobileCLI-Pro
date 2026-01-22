package com.termux

import android.app.Application
import android.util.Log

/**
 * MobileCLI Application class.
 * Handles app-wide initialization.
 */
class TermuxApplication : Application() {

    companion object {
        private const val TAG = "TermuxApplication"

        @Volatile
        private var instance: TermuxApplication? = null

        fun getInstance(): TermuxApplication? = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "MobileCLI Application initialized")
    }
}
