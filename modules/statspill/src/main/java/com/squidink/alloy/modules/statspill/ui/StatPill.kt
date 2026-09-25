package com.squidink.alloy.modules.statspill.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.squidink.alloy.modules.statspill.CornerPosition
import com.squidink.alloy.modules.statspill.StatsSettings
import java.util.Locale

/**
 * Stat pill overlay that displays CPU, RAM, and network stats.
 * CPU and RAM text colors reflect their usage (green → yellow → red).
 * Network stays neutral since it can't be measured as percentage.
 */
@Composable
fun StatPill(
    stats: ResourceStats,
    settings: StatsSettings,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    if (!settings.showPill) return
    
    val cpuPercent = stats.cpuUsagePercent ?: 0f
    val cpuColor = calculateUsageColor(cpuPercent)
    
    val totalMemKb = stats.memInfo.totalMemKb
    val availMemKb = stats.memInfo.availableMemKb
    val usedMemKb = totalMemKb - availMemKb
    val ramPercent = if (totalMemKb > 0) usedMemKb.toFloat() / totalMemKb else 0f
    val ramColor = calculateUsageColor(ramPercent)
    
    val networkColor = MaterialTheme.colorScheme.onSurface
    
    val alignment = settings.cornerPosition.toAlignment()
    
    Box(
        modifier = modifier
            .padding(16.dp)
            .padding(contentPadding),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                MaterialTheme.shapes.small
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // CPU line
                stats.cpuUsagePercent?.let { cpu ->
                    Text(
                        text = String.format(Locale.US, "CPU: %.0f%%", cpu * 100),
                        color = cpuColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // RAM line
                Text(
                    text = String.format(Locale.US, "RAM: %d/%d MB", usedMemKb / 1024, totalMemKb / 1024),
                    color = ramColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Network line (neutral color)
                Text(
                    text = String.format(Locale.US, "Net: ↓%.1f ↑%.1f", stats.netStats.rxBytesPerSecond, stats.netStats.txBytesPerSecond),
                    color = networkColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Convert CornerPosition to Alignment
 */
fun CornerPosition.toAlignment(): Alignment {
    return when (this) {
        CornerPosition.TOP_LEFT -> Alignment.TopStart
        CornerPosition.TOP_RIGHT -> Alignment.TopEnd
        CornerPosition.BOTTOM_LEFT -> Alignment.BottomStart
        CornerPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
    }
}
