package com.squidink.alloy.modules.statspill.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.modules.statspill.StatsSettings
import com.squidink.alloy.modules.statspill.StatsViewModel
import java.util.Locale

/**
 * Resource stats data for UI display.
 */
data class ResourceStat(
    val type: ResourceType,
    val name: String,
    val usagePercent: Float,
    val used: Long,
    val total: Long,
    val unit: String
)

enum class ResourceType {
    CPU,
    RAM,
    NETWORK
}

@Composable
fun StatsGrid(
    stats: ResourceStats,
    settings: StatsSettings,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        // CPU Card
        item {
            val cpuPercent = stats.cpuUsagePercent ?: 0f
            val backgroundColor = calculateUsageColor(cpuPercent)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .desktopHover(),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CPU",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (settings.usePercentages) {
                                String.format(Locale.US, "%.1f%%", cpuPercent * 100)
                            } else {
                                "Usage indicator"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = if (settings.usePercentages) {
                            String.format(Locale.US, "%.0f%%", cpuPercent * 100)
                        } else {
                            "--"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // RAM Card
        item {
            val totalMemKb = stats.memInfo.totalMemKb
            val availMemKb = stats.memInfo.availableMemKb
            val usedMemKb = totalMemKb - availMemKb
            val ramPercent = if (totalMemKb > 0) usedMemKb.toFloat() / totalMemKb else 0f
            val backgroundColor = calculateUsageColor(ramPercent)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .desktopHover(),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RAM",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total: ${totalMemKb / 1024} MB",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Used: ${usedMemKb / 1024} MB",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Available: ${availMemKb / 1024} MB",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Network Card (no color coding)
        item {
            val net = stats.netStats
            val rxText = String.format(Locale.US, "%.1f KB/s", net.rxBytesPerSecond)
            val txText = String.format(Locale.US, "%.1f KB/s", net.txBytesPerSecond)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .desktopHover(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Network",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "↓ $rxText",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "↑ $txText",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculate color based on usage percentage.
 * Returns a smooth gradient from green (0%) → yellow (50%) → red (100%).
 */
@Composable
fun calculateUsageColor(percent: Float): Color {
    val clampedPercent = percent.coerceIn(0f, 1f)
    
    return when {
        clampedPercent <= 0.5f -> {
            // Green to Yellow (0-50%)
            val t = clampedPercent * 2
            Color(
                red = 0f + (1f - 0f) * t,
                green = 1f,
                blue = 0f
            )
        }
        else -> {
            // Yellow to Red (50-100%)
            val t = (clampedPercent - 0.5f) * 2
            Color(
                red = 1f,
                green = 1f - t,
                blue = 0f
            )
        }
    }
}

/**
 * Data class wrapping all stats for the grid display.
 */
data class ResourceStats(
    val cpuUsagePercent: Float?,
    val memInfo: com.squidink.alloy.core.proc.MemInfo,
    val netStats: com.squidink.alloy.core.proc.NetStats
)

/**
 * Extension to convert StatsUiState to ResourceStats
 */
fun com.squidink.alloy.modules.statspill.StatsUiState.toResourceStats(): ResourceStats {
    return ResourceStats(
        cpuUsagePercent = cpuUsagePercent,
        memInfo = memInfo,
        netStats = netStats
    )
}
