package com.squidink.alloy.core.domain.repository

/**
 * Data class for battery information.
 * This is the domain model independent of Android framework specifics.
 */
data class BatteryInfo(
    val level: Int = 0,
    val scale: Int = 100,
    val percentage: Int = 0,
    val health: Int = 0,
    val status: Int = 0,
    val temperature: Int = 0, // tenths of a degree Celsius
    val voltage: Int = 0, // millivolts
    val isCharging: Boolean = false,
) {
    fun getTemperatureCelsius(): Float = temperature / 10f
}

/**
 * Data class for network statistics.
 * This is the domain model independent of implementation details.
 */
data class NetStats(
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxBytesPerSecond: Float = 0f,
    val txBytesPerSecond: Float = 0f,
)
