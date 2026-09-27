package com.squidink.alloy.core.data.datasource

import android.os.BatteryManager
import com.squidink.alloy.core.proc.NetStats

/**
 * Legacy data class for battery information.
 * @deprecated Use [com.squidink.alloy.core.domain.repository.BatteryInfo] instead.
 */
@Deprecated("Use com.squidink.alloy.core.domain.repository.BatteryInfo instead")
data class LegacyBatteryInfo(
    val level: Int = 0,
    val scale: Int = 100,
    val percentage: Int = 0,
    val health: Int = BatteryManager.BATTERY_HEALTH_UNKNOWN,
    val status: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
    val temperature: Int = 0, // tenths of a degree Celsius
    val voltage: Int = 0, // millivolts
    val isCharging: Boolean = false,
) {
    fun getTemperatureCelsius(): Float = temperature / 10f

    fun getHealthString(): String = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Overvoltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        else -> "Unknown"
    }

    fun getStatusString(): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
        else -> "Unknown"
    }
}

/**
 * Legacy data class containing system statistics.
 * @deprecated Use [com.squidink.alloy.core.domain.repository.SystemStats] instead.
 */
@Deprecated("Use com.squidink.alloy.core.domain.repository.SystemStats instead")
data class SystemStatsData(
    val memoryUsedBytes: Long = 0,
    val memoryTotalBytes: Long = 0,
    val memoryPercent: Float = 0f,
    val cpuPercent: Float = 0f,
    val netStats: NetStats = NetStats(),
    val timestamp: Long = System.currentTimeMillis()
)
