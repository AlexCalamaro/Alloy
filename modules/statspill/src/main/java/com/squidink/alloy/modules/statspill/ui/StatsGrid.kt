package com.squidink.alloy.modules.statspill.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.layout.deriveWindowSizeClass
import com.squidink.alloy.core.layout.isExpanded
import com.squidink.alloy.modules.statspill.StatsUiState
import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import com.squidink.alloy.modules.statspill.domain.model.CombinedTelemetry
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import com.squidink.alloy.modules.statspill.domain.model.NetworkTransportType
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import java.util.Locale

/**
 * Responsive grid displaying all system telemetry cards.
 * On expanded screens (>=840dp), uses a two-column staggered grid.
 * On compact/medium screens, uses a single-column list.
 */
@Composable
fun StatsGrid(
    stats: CombinedTelemetry,
    settings: StatsUiState,
    modifier: Modifier = Modifier
) {
    val cards = buildCardList(stats, settings)
    StatsGrid(
        cards = cards,
        modifier = modifier
    )
}

/**
 * Responsive grid that renders a list of card composables.
 * Use this overload when passing a customized or reordered set of cards.
 */
@Composable
fun StatsGrid(
    cards: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = deriveWindowSizeClass()
    val isExpanded = windowSizeClass.isExpanded()

    if (isExpanded) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp
        ) {
            items(cards.size) { index ->
                cards[index]()
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(cards.size) { index ->
                cards[index]()
            }
        }
    }
}

/**
 * Builds the default ordered list of telemetry card composables.
 * To reorder or omit cards, simply adjust the order of calls in this list.
 */
@Composable
fun buildCardList(
    stats: CombinedTelemetry,
    settings: StatsUiState
): List<@Composable () -> Unit> = buildList {
    stats.systemStats?.let { systemStats ->
        add { CpuCard(systemStats = systemStats, settings = settings) }
        add { RamCard(systemStats = systemStats, settings = settings) }
    }
    add { NetworkCard(networkStats = stats.networkStats) }
    stats.diskStats?.let { diskStats ->
        add { StorageCard(diskStats = diskStats, settings = settings) }
    }
    add { BatteryCard(batteryInfo = stats.batteryInfo) }
    stats.thermalStats?.let { thermalStats ->
        add { TemperatureCard(thermalStats = thermalStats) }
    }
}

// ============================================================================
// Individual Stat Card Composables
// ============================================================================

/**
 * CPU telemetry card displaying overall usage percentage, core count,
 * and 1m/5m/15m system load average.
 */
@Composable
fun CpuCard(
    systemStats: SystemStats,
    modifier: Modifier = Modifier,
    usePercentages: Boolean = true
) {
    val cpuPercent = systemStats.cpuPercent
    val primaryValue = if (usePercentages) {
        String.format(Locale.US, "%.0f%%", cpuPercent)
    } else {
        ""
    }
    val secondaryValues = listOfNotNull(
        StatValue(
            label = "Usage",
            value = if (usePercentages) {
                String.format(Locale.US, "%.1f%%", cpuPercent)
            } else {
                String.format(Locale.US, "%.1f", cpuPercent)
            }
        ),
        StatValue(label = "Cores", value = "${systemStats.cpuCores} Cores"),
        systemStats.systemLoadAverage.takeIf { it.isNotEmpty() }?.let { loadList ->
            StatValue(
                label = "Load Avg",
                value = loadList.joinToString(" ") { String.format(Locale.US, "%.2f", it) }
            )
        }
    )

    StatCard(
        config = StatCardConfig(
            title = "CPU",
            primaryValue = primaryValue,
            secondaryValues = secondaryValues,
            customBackgroundColor = calculatePercentageColor(
                percentage = cpuPercent,
                higherIsBetter = false
            )
        ),
        modifier = modifier
    )
}

@Composable
fun CpuCard(
    systemStats: SystemStats,
    settings: StatsUiState,
    modifier: Modifier = Modifier
) = CpuCard(systemStats = systemStats, modifier = modifier, usePercentages = settings.usePercentages)

@Composable
fun CpuStatCard(
    systemStats: SystemStats,
    modifier: Modifier = Modifier,
    usePercentages: Boolean = true
) = CpuCard(systemStats = systemStats, modifier = modifier, usePercentages = usePercentages)

/**
 * RAM telemetry card displaying memory usage percentage, total, used, and available MB.
 */
@Composable
fun RamCard(
    systemStats: SystemStats,
    modifier: Modifier = Modifier,
    usePercentages: Boolean = true
) {
    val totalMemMb = systemStats.memoryTotalBytes / (1024 * 1024)
    val usedMemMb = systemStats.memoryUsedBytes / (1024 * 1024)
    val ramPercent = systemStats.memoryPercent

    val primaryValue = if (usePercentages) {
        String.format(Locale.US, "%.0f%%", ramPercent)
    } else {
        ""
    }

    val secondaryValues = listOf(
        StatValue(label = "Total", value = "$totalMemMb MB"),
        StatValue(label = "Used", value = "$usedMemMb MB"),
        StatValue(
            label = "Available",
            value = "${(systemStats.memoryTotalBytes - systemStats.memoryUsedBytes) / (1024 * 1024)} MB"
        )
    )

    StatCard(
        config = StatCardConfig(
            title = "RAM",
            primaryValue = primaryValue,
            secondaryValues = secondaryValues,
            customBackgroundColor = calculatePercentageColor(
                percentage = ramPercent,
                higherIsBetter = false
            )
        ),
        modifier = modifier
    )
}

@Composable
fun RamCard(
    systemStats: SystemStats,
    settings: StatsUiState,
    modifier: Modifier = Modifier
) = RamCard(systemStats = systemStats, modifier = modifier, usePercentages = settings.usePercentages)

@Composable
fun RamStatCard(
    systemStats: SystemStats,
    modifier: Modifier = Modifier,
    usePercentages: Boolean = true
) = RamCard(systemStats = systemStats, modifier = modifier, usePercentages = usePercentages)

/**
 * Network telemetry card displaying active transport type (Wi-Fi, Cellular, etc.),
 * link speed, download/upload throughput, and metered status.
 */
@Composable
fun NetworkCard(
    networkStats: NetworkStats,
    modifier: Modifier = Modifier
) {
    val primaryValue = if (networkStats.transportType != NetworkTransportType.UNKNOWN) {
        networkStats.transportType.getDisplayName()
    } else {
        ""
    }

    val secondaryValues = listOfNotNull(
        networkStats.formatLinkSpeed()?.let {
            StatValue(label = "Link Speed", value = it)
        },
        StatValue(
            label = "Download",
            value = String.format(Locale.US, "%.1f KB/s", networkStats.rxBytesPerSecond),
            style = MaterialTheme.typography.bodyLarge
        ),
        StatValue(
            label = "Upload",
            value = String.format(Locale.US, "%.1f KB/s", networkStats.txBytesPerSecond),
            style = MaterialTheme.typography.bodyLarge
        ),
        networkStats.isMetered?.let {
            StatValue(label = "Data", value = if (it) "Metered" else "Unmetered")
        }
    )

    StatCard(
        config = StatCardConfig(
            title = "Network",
            primaryValue = primaryValue,
            secondaryValues = secondaryValues,
            colorScheme = CardColorScheme.SURFACE_VARIANT
        ),
        modifier = modifier
    )
}

@Composable
fun NetworkStatCard(
    networkStats: NetworkStats,
    modifier: Modifier = Modifier
) = NetworkCard(networkStats = networkStats, modifier = modifier)

/**
 * Storage / Disk telemetry card displaying volume breakdown (Internal + SD/OTG),
 * total, used, free GB, and usage percentage.
 */
@Composable
fun StorageCard(
    diskStats: DiskStats,
    modifier: Modifier = Modifier,
    usePercentages: Boolean = true
) {
    val hasMultipleVolumes = diskStats.volumes.size > 1
    val primaryValue = if (usePercentages) {
        String.format(Locale.US, "%.0f%%", diskStats.percentUsed)
    } else {
        ""
    }

    val secondaryValues = if (hasMultipleVolumes) {
        diskStats.volumes.map { volume ->
            StatValue(
                label = volume.name,
                value = "${volume.formatUsedBytes()} / ${volume.formatTotalBytes()} (${String.format(Locale.US, "%.0f%%", volume.percentUsed)})"
            )
        }
    } else {
        listOfNotNull(
            diskStats.volumes.firstOrNull()?.let { StatValue(label = "Drive", value = it.name) },
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
        )
    }

    StatCard(
        config = StatCardConfig(
            title = "Storage",
            primaryValue = primaryValue,
            secondaryValues = secondaryValues,
            customBackgroundColor = calculatePercentageColor(
                percentage = diskStats.percentUsed,
                higherIsBetter = false
            )
        ),
        modifier = modifier
    )
}

@Composable
fun StorageCard(
    diskStats: DiskStats,
    settings: StatsUiState,
    modifier: Modifier = Modifier
) = StorageCard(diskStats = diskStats, modifier = modifier, usePercentages = settings.usePercentages)

@Composable
fun StorageStatCard(
    diskStats: DiskStats,
    modifier: Modifier = Modifier,
    usePercentages: Boolean = true
) = StorageCard(diskStats = diskStats, modifier = modifier, usePercentages = usePercentages)

/**
 * Battery telemetry card displaying battery percentage, charging state with power source,
 * battery health, live current (mA charge/drain rate), voltage, and battery temperature.
 */
@Composable
fun BatteryCard(
    batteryInfo: BatteryInfo,
    modifier: Modifier = Modifier
) {
    val secondaryValues = listOfNotNull(
        StatValue(
            label = "Status",
            value = if (batteryInfo.isCharging) {
                "Charging (${batteryInfo.pluggedSource.getDisplayName()})"
            } else {
                "Discharging"
            }
        ),
        StatValue(
            label = "Health",
            value = batteryInfo.getHealthString()
        ),
        batteryInfo.getCurrentMilliAmperes()?.let { mA ->
            StatValue(
                label = if (mA > 0) "Charge Rate" else "Drain Rate",
                value = "${if (mA > 0) "+" else ""}$mA mA"
            )
        },
        batteryInfo.voltage.takeIf { it > 0 }?.let { mV ->
            StatValue(
                label = "Voltage",
                value = String.format(Locale.US, "%.2f V", mV / 1000f)
            )
        },
        batteryInfo.temperature.takeIf { it > 0 }?.let { temp ->
            StatValue(label = "Temp", value = "${temp / 10f}°C")
        }
    )

    StatCard(
        config = StatCardConfig(
            title = "Battery",
            primaryValue = "${batteryInfo.percentage}%",
            secondaryValues = secondaryValues,
            customBackgroundColor = calculateBatteryColor(batteryInfo.percentage)
        ),
        modifier = modifier
    )
}

@Composable
fun BatteryStatCard(
    batteryInfo: BatteryInfo,
    modifier: Modifier = Modifier
) = BatteryCard(batteryInfo = batteryInfo, modifier = modifier)

/**
 * Thermal telemetry card prioritizing direct SoC temperature and OS thermal throttling status.
 * If direct CPU sensors are restricted by the OS, thermal throttling state is highlighted
 * without duplicating battery temperature as the hero metric.
 */
@Composable
fun TemperatureCard(
    thermalStats: ThermalStats,
    modifier: Modifier = Modifier
) {
    val hasDirectCpu = thermalStats.cpuTemperature != null
    val primaryValue = if (hasDirectCpu) {
        String.format(Locale.US, "%.1f°C", thermalStats.cpuTemperature)
    } else {
        thermalStats.thermalStatus.getDisplayName()
    }

    val secondaryValues = if (hasDirectCpu) {
        listOfNotNull(
            StatValue(label = "SoC Temp", value = String.format(Locale.US, "%.1f°C", thermalStats.cpuTemperature)),
            StatValue(label = "SoC State", value = thermalStats.thermalStatus.getDisplayName()),
            thermalStats.batteryTemperature?.let { StatValue(label = "Battery Temp", value = String.format(Locale.US, "%.1f°C", it)) },
            thermalStats.skinTemperature?.let { StatValue(label = "Skin Temp", value = String.format(Locale.US, "%.1f°C", it)) }
        )
    } else {
        listOfNotNull(
            StatValue(label = "SoC Throttle", value = thermalStats.thermalStatus.getDisplayName()),
            thermalStats.batteryTemperature?.let { StatValue(label = "Battery Sensor", value = String.format(Locale.US, "%.1f°C", it)) },
            StatValue(label = "SoC Sensor", value = "Restricted by OS"),
            thermalStats.skinTemperature?.let { StatValue(label = "Skin Sensor", value = String.format(Locale.US, "%.1f°C", it)) }
        )
    }

    StatCard(
        config = StatCardConfig(
            title = "Temperature",
            primaryValue = primaryValue,
            secondaryValues = secondaryValues,
            customBackgroundColor = calculateTemperatureColor(
                cpuTemp = thermalStats.cpuTemperature,
                thermalStatus = thermalStats.thermalStatus,
                fallbackTemp = thermalStats.batteryTemperature
            )
        ),
        modifier = modifier
    )
}

@Composable
fun TemperatureStatCard(
    thermalStats: ThermalStats,
    modifier: Modifier = Modifier
) = TemperatureCard(thermalStats = thermalStats, modifier = modifier)
