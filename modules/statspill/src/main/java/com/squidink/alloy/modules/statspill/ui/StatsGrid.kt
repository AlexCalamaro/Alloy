package com.squidink.alloy.modules.statspill.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.squidink.alloy.modules.statspill.StatsUiState
import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import com.squidink.alloy.modules.statspill.domain.model.CombinedTelemetry
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import java.util.Locale

/**
 * Responsive grid displaying all system telemetry cards.
 * On expanded screens (>=840dp), uses a two-column grid.
 * On compact/medium screens, uses a single-column list.
 */
@Composable
fun StatsGrid(
    stats: CombinedTelemetry,
    settings: StatsUiState,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = deriveWindowSizeClass()
    val isExpanded = windowSizeClass.isExpanded()
    val statConfigs = buildStatConfigs(stats, settings)

    if (isExpanded) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(statConfigs.size) { index ->
                StatCard(config = statConfigs[index])
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(statConfigs.size) { index ->
                StatCard(config = statConfigs[index])
            }
        }
    }
}

@Composable
private fun buildStatConfigs(
    stats: CombinedTelemetry,
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
                        String.format(Locale.US, "%.0f%%", cpuPercent)
                    } else {
                        ""
                    },
                    secondaryValues = listOf(
                        StatValue(
                            label = "Usage",
                            value = if (settings.usePercentages) {
                                String.format(Locale.US, "%.1f%%", cpuPercent)
                            } else {
                                String.format(Locale.US, "%.1f", cpuPercent)
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
                    primaryValue = if (settings.usePercentages) {
                        String.format(Locale.US, "%.0f%%", ramPercent)
                    } else {
                        ""
                    },
                    secondaryValues = listOf(
                        StatValue(label = "Total", value = "$totalMemMb MB"),
                        StatValue(label = "Used", value = "$usedMemMb MB"),
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
                        value = String.format(Locale.US, "%.1f KB/s", stats.networkStats.rxBytesPerSecond),
                        style = MaterialTheme.typography.bodyLarge
                    ),
                    StatValue(
                        label = "Upload",
                        value = String.format(Locale.US, "%.1f KB/s", stats.networkStats.txBytesPerSecond),
                        style = MaterialTheme.typography.bodyLarge
                    )
                ),
                colorScheme = CardColorScheme.SURFACE_VARIANT
            )
        )

        // Storage / Disk Card
        stats.diskStats?.let { diskStats ->
            add(
                StatCardConfig(
                    title = "Storage",
                    primaryValue = if (settings.usePercentages) {
                        String.format(Locale.US, "%.0f%%", diskStats.percentUsed)
                    } else {
                        ""
                    },
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
                secondaryValues = listOfNotNull(
                    StatValue(
                        label = "Status",
                        value = if (stats.batteryInfo.isCharging) "Charging" else "Discharging"
                    ),
                    stats.batteryInfo.temperature.takeIf { it > 0 }?.let { temp ->
                        StatValue(label = "Temp", value = "${temp / 10f}°C")
                    }
                ),
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
                    secondaryValues = listOfNotNull(
                        thermal.batteryTemperature?.let { StatValue(label = "Battery", value = "${it}°C") },
                        thermal.cpuTemperature?.let { StatValue(label = "CPU", value = "${it}°C") },
                        thermal.skinTemperature?.let { StatValue(label = "Skin", value = "${it}°C") }
                    ).ifEmpty {
                        listOf(StatValue(label = "", value = "Temperature sensor not reporting"))
                    },
                    colorScheme = calculateTemperatureColor(maxTemp).toCardColorScheme()
                )
            )
        }
    }
}

@Composable
private fun Color.toCardColorScheme(): CardColorScheme {
    return when (this) {
        MaterialTheme.colorScheme.primaryContainer -> CardColorScheme.PRIMARY_CONTAINER
        MaterialTheme.colorScheme.error -> CardColorScheme.ERROR
        Color(0xFFFFA726) -> CardColorScheme.WARNING
        else -> CardColorScheme.SURFACE_VARIANT
    }
}
