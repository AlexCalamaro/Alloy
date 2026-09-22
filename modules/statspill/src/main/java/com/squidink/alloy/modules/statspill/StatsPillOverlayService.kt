package com.squidink.alloy.modules.statspill

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
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
 * SYSTEM_ALERT_WINDOW floating overlay pill displaying live vitals at 1Hz.
 */
@AndroidEntryPoint
class StatsPillOverlayService : Service() {

    @Inject lateinit var procReader: ProcReader

    private var windowManager: WindowManager? = null
    private var overlayView: TextView? = null
    private var serviceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupOverlayView()
        startTelemetryLoop()
    }

    private fun setupOverlayView() {
        overlayView = TextView(this).apply {
            text = "RAM: ... MB"
            setBackgroundColor(0xCC000000.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(24, 12, 24, 12)
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

        windowManager?.addView(overlayView, params)
    }

    private fun startTelemetryLoop() {
        serviceJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                val mem = withContext(Dispatchers.IO) { procReader.readMemInfo() }
                val memUsedMb = (mem.totalMemKb - mem.availableMemKb) / 1024
                overlayView?.text = "RAM: ${memUsedMb} MB used"
                delay(1000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        overlayView?.let { windowManager?.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
