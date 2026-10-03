package com.squidink.alloy.modules.llmhost.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service hosting the localhost LiteRT LLM HTTP server.
 *
 * Ensures the Ktor server and LiteRT engine remain active in memory
 * when other desktop windows or applications interact with the loopback API.
 */
@AndroidEntryPoint
class LlmHostServerService : LifecycleService() {

    @Inject
    lateinit var repository: ILlmHostRepository

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP_HOST) {
            Logger.i(TAG, "Received STOP intent, stopping host server")
            lifecycleScope.launch {
                repository.stopServer()
                stopSelf()
            }
            return START_NOT_STICKY
        }

        startForegroundNotification()
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = CHANNEL_ID
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            "Alloy LLM Host Server",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        val stopIntent = Intent(this, LlmHostServerService::class.java).apply {
            action = ACTION_STOP_HOST
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val port = repository.getServerConfig().port
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alloy LLM Host Active")
            .setContentText("Listening on 127.0.0.1:$port (LiteRT)")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, 0)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val TAG = "LlmHostServerService"
        const val ACTION_STOP_HOST = "com.squidink.alloy.modules.llmhost.ACTION_STOP_HOST"
        const val CHANNEL_ID = "llm_host_channel"
        const val NOTIFICATION_ID = 2001
        private const val REQUEST_CODE_STOP = 1002
    }
}
