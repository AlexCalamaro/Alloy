package com.squidink.alloy.modules.statspill.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.layout.deriveWindowSizeClass
import com.squidink.alloy.core.layout.isExpanded
import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import com.squidink.alloy.modules.statspill.StatsUiState
import java.util.Locale

/**
 * Stats grid that displays all stat cards in a responsive layout.
 * On expanded screens (>=840dp), uses a two-column grid.
 * On smaller screens, uses a single-column list.
 *
 * @param stats The resource stats to display
 * @param settings The UI settings for display options
 * @param modifier Modifier to apply to the grid
 */
@Composable
fun StatsGrid(
    stats: ResourceStats,
    settings: StatsUiState,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = deriveWindowSizeClass()
    val isExpanded = windowSizeClass.isExpanded()
    
    // Build list of stat card configurations
    val statConfigs = buildStatConfigs(stats, settings)
    
    if (isExpanded) {
        // Two-column grid for expanded screens
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize()
        ) {
            items(statConfigs.size) { index ->
                StatCard(
                    config = statConfigs[index],
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    } else {
        // Single-column list for smaller screens
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            items(statConfigs.size) { index ->
                StatCard(
                    config = statConfigs[index],
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Build the list of stat card configurations from the resource stats.
 */
@Composable
private fun buildStatConfigs(
    stats: ResourceStats,
    settings: StatsUiState
): List<StatCardConfig> {
    return buildList {
        // CPU Card
        stats.systemStats?.let { systemStats ->
            val cpuPercent = systemStats.cpuPercent
            add(
                StatCardConfig(
                    title = "CPU",
                    primaryValue = if (settings.usePercentages) {
                        String.format(Locale.US, "%.0f%%", cpuPercent * 100)
                    } else {
                        ""
                    },
                    secondaryValues = listOf(
                        StatValue(
                            label = "Usage",
                            value = if (settings.usePercentages) {
                                String.format(Locale.US, "%.1f%%", cpuPercent * 100)
                            } else {
                                "Usage indicator"
                            }
                        )
                    ),
                    colorScheme = calculatePercentageColor(cpuPercent).toCardColorScheme()
                )
            )
        }
        
        // RAM Card
        stats.systemStats?.let { systemStats ->
            val totalMemMb = systemStats.memoryTotalBytes / (1024 * 1024)
            val usedMemMb = systemStats.memoryUsedBytes / (1024 * 1024)
            val ramPercent = systemStats.memoryPercent
            
            add(
                StatCardConfig(
                    title = "RAM",
                    primaryValue = "",
                    secondaryValues = listOf(
                        StatValue(label = "Total", value = "${totalMemMb} MB"),
                        StatValue(label = "Used", value = "${usedMemMb} MB"),
                        StatValue(
                            label = "Available",
                            value = "${(systemStats.memoryTotalBytes - systemStats.memoryUsedBytes) / (1024 * 1024)} MB"
                        )
                    ),
                    colorScheme = calculatePercentageColor(ramPercent).toCardColorScheme()
                )
            )
        }
        
        // Network Card
        add(
            StatCardConfig(
                title = "Network",
                primaryValue = "",
                secondaryValues = listOf(
                    StatValue(
                        label = "Download",
                        value = String.format(Locale.US, "%.1f KB/s", stats.netStats.rxBytesPerSecond),
                        style = MaterialTheme.typography.bodyLarge
                    ),
                    StatValue(
                        label = "Upload",
                        value = String.format(Locale.US, "%.1f KB/s", stats.netStats.txBytesPerSecond),
                        style = MaterialTheme.typography.bodyLarge
                    )
                ),
                colorScheme = CardColorScheme.SURFACE_VARIANT
            )
        )
        
        // Disk Card
        stats.diskStats?.let { diskStats ->
            add(
                StatCardConfig(
                    title = "Storage",
                    primaryValue = "",
                    secondaryValues = listOf(
                        StatValue(label = "Total", value = diskStats.formatTotalBytes()),
                        StatValue(label = "Used", value = diskStats.formatUsedBytes()),
                        StatValue(
                            label = "Free",
                            value = String.format(Locale.US, "%.1f GB", diskStats.freeBytes / (1024.0 * 1024.0 * 1024.0))
                        ),
                        StatValue(
                            label = "Usage",
                            value = String.format(Locale.US, "%.1f%% used", diskStats.percentUsed)
                        )
                    ),
                    colorScheme = calculatePercentageColor(diskStats.percentUsed).toCardColorScheme()
                )
            )
        }
        
        // Battery Card
        add(
            StatCardConfig(
                title = "Battery",
                primaryValue = "${stats.batteryInfo.percentage}%",
                secondaryValues = listOf(
                    StatValue(
                        label = "Status",
                        value = if (stats.batteryInfo.isCharging) "Charging" else "Discharging"
                    ),
                    stats.batteryInfo.temperature.takeIf { it > 0 }?.let { temp ->
                        StatValue(label = "Temp", value = "${temp / 10f}°C")
                    }
                ).filterNotNull(),
                colorScheme = calculateBatteryColor(stats.batteryInfo.percentage).toCardColorScheme()
            )
        )
        
        // Thermal Card
        stats.thermalStats?.let { thermal ->
            val maxTemp = thermal.maxTemperature
            add(
                StatCardConfig(
                    title = "Temperature",
                    primaryValue = "",
                    secondaryValues = listOf(
                        thermal.batteryTemperature?.let { StatValue(label = "Battery", value = "${it}°C") },
                        thermal.cpuTemperature?.let { StatValue(label = "CPU", value = "${it}°C") },
                        thermal.skinTemperature?.let { StatValue(label = "Skin", value = "${it}°C") }
                    ).filterNotNull().ifEmpty {
                        listOf(StatValue(label = "", value = "Temperature data not available"))
                    },
                    colorScheme = calculateTemperatureColor(maxTemp).toCardColorScheme()
                )
            )
        }
    }
}

/**
 * Helper function to convert a Color to CardColorScheme.
 */
@Composable
private fun Color.toCardColorScheme(): CardColorScheme {
    return when (this) {
        MaterialTheme.colorScheme.primaryContainer -> CardColorScheme.PRIMARY_CONTAINER
        MaterialTheme.colorScheme.error -> CardColorScheme.ERROR
        Color(0xFFFFA726) -> CardColorScheme.WARNING
        else -> CardColorScheme.SURFACE_VARIANT
    }
}

/**
 * Format free bytes for display.
 */
fun DiskStats.formatFreeBytes(): String {
    return when {
        freeBytes >= 1L * 1024 * 1024 * 1024 -> String.format("%.1f GB", freeBytes / (1024.0 * 1024.0 * 1024.0))
        freeBytes >= 1024 * 1024 -> String.format("%.1f MB", freeBytes / (1024.0 * 1024.0))
        freeBytes >= 1024 -> String.format("%.1f KB", freeBytes / 1024.0)
        else -> "$freeBytes B"
    }
}

/**
 * Data class wrapping all stats for the grid display.
 */
data class ResourceStats(
    val systemStats: SystemStats?,
    val netStats: NetworkStats,
    val diskStats: DiskStats?,
    val batteryInfo: BatteryInfo,
    val thermalStats: ThermalStats?
)

/**
 * Extension to convert StatsUiState to ResourceStats.
 */
fun com.squidink.alloy.modules.statspill.StatsUiState.toResourceStats(): ResourceStats {
    return ResourceStats(
        systemStats = systemStats,
        netStats = netStats,
        diskStats = diskStats,
        batteryInfo = batteryInfo,
        thermalStats = thermalStats
    )
}
