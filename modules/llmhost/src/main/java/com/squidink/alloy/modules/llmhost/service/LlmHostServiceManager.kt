package com.squidink.alloy.modules.llmhost.service

import android.content.Context
import android.content.Intent
import com.squidink.alloy.core.common.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of [LlmHostServerService].
 */
@Singleton
class LlmHostServiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun startService() {
        try {
            val intent = Intent(context, LlmHostServerService::class.java)
            context.startForegroundService(intent)
            Logger.i(TAG, "Started LlmHostServerService")
        } catch (e: IllegalStateException) {
            Logger.e(TAG, "Failed to start LlmHostServerService due to invalid state", e)
        } catch (e: SecurityException) {
            Logger.e(TAG, "Failed to start LlmHostServerService due to security restriction", e)
        }
    }

    fun stopService() {
        try {
            val intent = Intent(context, LlmHostServerService::class.java)
            context.stopService(intent)
            Logger.i(TAG, "Stopped LlmHostServerService")
        } catch (e: IllegalStateException) {
            Logger.w(TAG, "Failed to stop LlmHostServerService due to invalid state", e)
        } catch (e: SecurityException) {
            Logger.w(TAG, "Failed to stop LlmHostServerService due to security restriction", e)
        }
    }

    companion object {
        private const val TAG = "LlmHostServiceManager"
    }
}
