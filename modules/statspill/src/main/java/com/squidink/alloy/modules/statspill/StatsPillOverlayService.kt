package com.squidink.alloy.modules.statspill

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.squidink.alloy.core.design.AlloyTheme
import com.squidink.alloy.core.proc.ProcReader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * SYSTEM_ALERT_WINDOW floating overlay pill displaying live vitals using 100% Jetpack Compose.
 */
@AndroidEntryPoint
class StatsPillOverlayService : LifecycleService() {

    @Inject lateinit var procReader: ProcReader

    private var windowManager: WindowManager? = null
    private var overlayComposeView: ComposeView? = null
    private var serviceJob: Job? = null

    private var liveTextState by mutableStateOf("RAM: ... MB")

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        startForegroundPillNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupComposeOverlayView()
        startTelemetryLoop()
    }

    private fun startForegroundPillNotification() {
        val channelId = "stats_overlay_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alloy Live Vitals Overlay", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alloy Live Vitals")
            .setContentText("Live Desktop Overlay Active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupComposeOverlayView() {
        overlayComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AlloyTheme {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = liveTextState,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 32
            y = 32
        }

        windowManager?.addView(overlayComposeView, params)
    }

    private fun startTelemetryLoop() {
        serviceJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                val mem = withContext(Dispatchers.IO) { procReader.readMemInfo() }
                val memUsedMb = (mem.totalMemKb - mem.availableMemKb) / 1024
                liveTextState = "RAM: $memUsedMb MB used"
                delay(1000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        overlayComposeView?.let { windowManager?.removeView(it) }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
