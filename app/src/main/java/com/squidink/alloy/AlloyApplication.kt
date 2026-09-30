package com.squidink.alloy

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class annotated for Hilt dependency injection.
 */
@HiltAndroidApp
class AlloyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("sqlcipher")
            Log.d(TAG, "SQLCipher native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load SQLCipher native library", e)
        }
    }

    companion object {
        private const val TAG = "AlloyApplication"
    }
}
