package com.squidink.alloy.modules.statspill.domain.model

/**
 * System statistics data.
 * Implements [StatType] for unified handling in the stats system.
 */
data class SystemStats(
    override val id: String = "system",
    override val name: String = "System",
    override val category: StatCategory = StatCategory.SYSTEM,
    override val timestamp: Long,
    val memoryUsedBytes: Long,
    val memoryTotalBytes: Long,
    val memoryPercent: Float,
    val cpuPercent: Float
) : StatType

/**
 * Data class for battery information.
 * This is the domain model independent of Android framework specifics.
 * Implements [StatType] for unified handling in the stats system.
 */
data class BatteryInfo(
    override val id: String = "battery",
    override val name: String = "Battery",
    override val category: StatCategory = StatCategory.POWER,
    override val timestamp: Long = System.currentTimeMillis(),
    val level: Int = 0,
    val scale: Int = 100,
    val percentage: Int = 0,
    val health: Int = 0,
    val status: Int = 0,
    val temperature: Int = 0, // tenths of a degree Celsius
    val voltage: Int = 0, // millivolts
    val isCharging: Boolean = false,
) : StatType {
    fun getTemperatureCelsius(): Float = temperature / 10f
}

/**
 * Data class for network statistics.
 * This is the domain model independent of implementation details.
 * Implements [StatType] for unified handling in the stats system.
 */
data class NetworkStats(
    override val id: String = "network",
    override val name: String = "Network",
    override val category: StatCategory = StatCategory.NETWORK,
    override val timestamp: Long = System.currentTimeMillis(),
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxBytesPerSecond: Float = 0f,
    val txBytesPerSecond: Float = 0f,
) : StatType



/**
 * Data class for disk/storage statistics.
 * Implements [StatType] for unified handling in the stats system.
 */
data class DiskStats(
    override val id: String = "disk",
    override val name: String = "Storage",
    override val category: StatCategory = StatCategory.STORAGE,
    override val timestamp: Long = System.currentTimeMillis(),
    val totalBytes: Long = 0,
    val usedBytes: Long = 0,
    val freeBytes: Long = 0,
    val percentUsed: Float = 0f
) : StatType {
    /**
     * Calculate percentage of free space.
     */
    val percentFree: Float
        get() = if (totalBytes > 0) (freeBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f
    
    /**
     * Format used bytes as human-readable string.
     */
    fun formatUsedBytes(): String = formatBytes(usedBytes)
    
    /**
     * Format total bytes as human-readable string.
     */
    fun formatTotalBytes(): String = formatBytes(totalBytes)
    
    private companion object {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1L * 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
                bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }
}

/**
 * Data class for thermal/temperature statistics.
 * Implements [StatType] for unified handling in the stats system.
 */
data class ThermalStats(
    override val id: String = "thermal",
    override val name: String = "Temperature",
    override val category: StatCategory = StatCategory.THERMAL,
    override val timestamp: Long = System.currentTimeMillis(),
    val batteryTemperature: Float? = null,
    val cpuTemperature: Float? = null,
    val skinTemperature: Float? = null
) : StatType {
    /**
     * Get the maximum temperature reading available.
     */
    val maxTemperature: Float?
        get() = listOf(batteryTemperature, cpuTemperature, skinTemperature)
            .filterNotNull()
            .maxOrNull()
    
    /**
     * Get the average temperature from all available readings.
     */
    val averageTemperature: Float?
        get() = listOfNotNull(batteryTemperature, cpuTemperature, skinTemperature)
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat()
}

/**
 * Consolidated telemetry snapshot aggregating all domain statistics.
 */
data class CombinedTelemetry(
    val systemStats: SystemStats? = null,
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val networkStats: NetworkStats = NetworkStats(),
    val diskStats: DiskStats? = null,
    val thermalStats: ThermalStats? = null,
    val timestamp: Long = System.currentTimeMillis()
)
