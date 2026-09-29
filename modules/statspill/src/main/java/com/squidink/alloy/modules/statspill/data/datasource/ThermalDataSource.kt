package com.squidink.alloy.modules.statspill.data.datasource

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import com.squidink.alloy.modules.statspill.domain.model.ThermalStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for thermal and temperature statistics.
 * Reads battery temperature via sticky battery broadcast,
 * queries reactive thermal throttling status via [PowerManager],
 * and probes sysfs thermal zones for CPU temperatures where accessible.
 */
@Singleton
class ThermalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : StatDataSource<ThermalStats> {

    /**
     * Read current thermal statistics.
     */
    override suspend fun read(): ThermalStats = withContext(Dispatchers.IO) {
        try {
            val batteryTemp = getBatteryTemperatureCelsius()
            val cpuTemp = getCpuTemperatureCelsius()
            val status = getThermalStatus()

            ThermalStats(
                timestamp = System.currentTimeMillis(),
                batteryTemperature = batteryTemp,
                cpuTemperature = cpuTemp,
                thermalStatus = status
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading thermal stats", e)
            ThermalStats(
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Observe thermal statistics as a flow.
     * Polls at 0.2Hz (every 5 seconds) as temperature shifts slowly.
     */
    override fun observe(): Flow<ThermalStats> = flow {
        while (true) {
            emit(read())
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun getThermalStatus(): ThermalStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ThermalStatus.NONE
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return ThermalStatus.NONE
        return when (powerManager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.SHUTDOWN
            else -> ThermalStatus.UNKNOWN
        }
    }

    private fun getBatteryTemperatureCelsius(): Float? {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val tenthsOfCelsius = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            if (tenthsOfCelsius > 0) {
                tenthsOfCelsius / 10f
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error obtaining battery temperature", e)
            null
        }
    }

    private fun getCpuTemperatureCelsius(): Float? {
        return try {
            val baseDir = File("/sys/class/thermal")
            if (!baseDir.exists() || !baseDir.canRead()) return null

            val zones = baseDir.listFiles { _, name -> name.startsWith("thermal_zone") } ?: return null
            var maxCpuTemp: Float? = null

            for (zone in zones) {
                val typeFile = File(zone, "type")
                val tempFile = File(zone, "temp")
                if (typeFile.exists() && typeFile.canRead() && tempFile.exists() && tempFile.canRead()) {
                    val type = typeFile.readText().trim().lowercase()
                    if (type.contains("cpu") || type.contains("soc") || type.contains("tsens") || type.contains("cluster")) {
                        val rawTemp = tempFile.readText().trim().toFloatOrNull() ?: continue
                        val tempC = if (rawTemp > 1000f) rawTemp / 1000f else rawTemp
                        if (tempC in 15.0f..115.0f) {
                            if (maxCpuTemp == null || tempC > maxCpuTemp) {
                                maxCpuTemp = tempC
                            }
                        }
                    }
                }
            }
            maxCpuTemp
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "ThermalDataSource"
        const val POLL_INTERVAL_MS = 5000L // 5 seconds
    }
}
