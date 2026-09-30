package com.squidink.alloy.modules.statspill

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.graphics.PixelFormat
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.squidink.alloy.core.data.repository.SettingsRepository
import com.squidink.alloy.core.design.AlloyTheme
import com.squidink.alloy.modules.statspill.domain.usecase.ObserveTelemetryUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * SYSTEM_ALERT_WINDOW floating overlay pill displaying live vitals using 100% Jetpack Compose.
 * Observes real-time telemetry from [ObserveTelemetryUseCase] across app and desktop windows.
 */
@AndroidEntryPoint
class StatsPillOverlayService : LifecycleService(), SavedStateRegistryOwner {

    @Inject lateinit var observeTelemetryUseCase: ObserveTelemetryUseCase
    @Inject lateinit var settingsRepository: SettingsRepository

    private var windowManager: WindowManager? = null
    private var overlayComposeView: ComposeView? = null
    private var currentParams: WindowManager.LayoutParams? = null

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var liveTextState by mutableStateOf("Vitals: ...")
    private var currentCornerPosition by mutableStateOf(CornerPosition.TOP_RIGHT)
    private var currentUsePercentages by mutableStateOf(true)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        startForegroundPillNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        observeSettings()
        setupComposeOverlayView()
        observeTelemetry()
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            launch {
                settingsRepository.observeCornerPosition().collect { positionName ->
                    currentCornerPosition = try {
                        CornerPosition.valueOf(positionName)
                    } catch (e: IllegalArgumentException) {
                        CornerPosition.TOP_RIGHT
                    }
                    updateOverlayPosition()
                }
            }

            launch {
                settingsRepository.observeUsePercentages().collect { usePerc ->
                    currentUsePercentages = usePerc
                }
            }
        }
    }

    private fun updateOverlayPosition() {
        currentParams?.let { params ->
            params.gravity = currentCornerPosition.toGravity()
            overlayComposeView?.let { view ->
                windowManager?.updateViewLayout(view, params)
            }
        }
    }

    private fun startForegroundPillNotification() {
        val channelId = "stats_overlay_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Alloy Live Vitals Overlay", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alloy Live Vitals")
            .setContentText("Live Desktop Overlay Active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, 0)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupComposeOverlayView() {
        overlayComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(this@StatsPillOverlayService)
            setViewTreeSavedStateRegistryOwner(this@StatsPillOverlayService)
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
            gravity = currentCornerPosition.toGravity()
            x = 32
            y = 32
        }

        currentParams = params
        windowManager?.addView(overlayComposeView, params)
    }

    private fun observeTelemetry() {
        lifecycleScope.launch {
            observeTelemetryUseCase().collect { telemetry ->
                val cpuPercent = telemetry.systemStats?.cpuPercent
                val cpuText = cpuPercent?.let {
                    if (currentUsePercentages) {
                        String.format(Locale.US, "%.1f%%", it)
                    } else {
                        String.format(Locale.US, "%.1f", it)
                    }
                } ?: "--"

                val ramText = telemetry.systemStats?.let { stats ->
                    val memUsedMb = stats.memoryUsedBytes / (1024 * 1024)
                    val memTotalMb = stats.memoryTotalBytes / (1024 * 1024)
                    if (currentUsePercentages) {
                        String.format(Locale.US, "%.1f%%", stats.memoryPercent)
                    } else {
                        String.format(Locale.US, "%d/%d MB", memUsedMb, memTotalMb)
                    }
                } ?: "--"

                val rxText = String.format(Locale.US, "%.0f KB/s", telemetry.networkStats.rxBytesPerSecond)
                val txText = String.format(Locale.US, "%.0f KB/s", telemetry.networkStats.txBytesPerSecond)
                liveTextState = "CPU: $cpuText | RAM: $ramText | ↓$rxText ↑$txText"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayComposeView?.let { windowManager?.removeView(it) }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}

/**
 * Convert CornerPosition to Android Gravity constant
 */
fun CornerPosition.toGravity(): Int {
    return when (this) {
        CornerPosition.TOP_LEFT -> Gravity.TOP or Gravity.START
        CornerPosition.TOP_RIGHT -> Gravity.TOP or Gravity.END
        CornerPosition.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
        CornerPosition.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
    }
}
