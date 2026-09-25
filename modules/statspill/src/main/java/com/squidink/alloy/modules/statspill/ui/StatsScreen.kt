package com.squidink.alloy.modules.statspill.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.R
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.modules.statspill.StatsUiAction
import com.squidink.alloy.modules.statspill.StatsViewModel
import java.util.Locale

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // CPU Usage Card
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .desktopHover(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.stats_cpu_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val cpuText = uiState.cpuUsagePercent?.let { String.format(Locale.US, "%.1f%%", it) } ?: "Calculating..."
                Text(stringResource(R.string.stats_cpu_usage, cpuText), style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Memory Card
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .desktopHover(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.stats_ram_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val totalMb = uiState.memInfo.totalMemKb / 1024
                val availMb = uiState.memInfo.availableMemKb / 1024
                val usedMb = totalMb - availMb
                Text(stringResource(R.string.stats_ram_total, totalMb))
                Text(stringResource(R.string.stats_ram_used, usedMb))
                Text(stringResource(R.string.stats_ram_avail, availMb))
            }
        }

        // Network Speed Card
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .desktopHover(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.stats_network_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val net = uiState.netStats
                val rxText = String.format(Locale.US, "%.1f KB/s", net.rxKbps)
                val txText = String.format(Locale.US, "%.1f KB/s", net.txKbps)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.stats_download, rxText), color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.stats_upload, txText), color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        // Battery Card
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .desktopHover(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.stats_battery_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val battery = uiState.batteryInfo
                Text(stringResource(R.string.stats_battery_level, battery.percentage, battery.level, battery.scale))
                Text("Status: ${battery.getStatusString()}")
                Text("Health: ${battery.getHealthString()}")
                Text("Temperature: ${battery.getTemperatureCelsius()}°C")
                Text("Voltage: ${battery.voltage} mV")
                if (battery.isCharging) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.stats_battery_charging), color = Color.Green)
                    }
                }
            }
        }

        // Live Mode Floating Overlay Toggle Card
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .desktopHover(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.stats_overlay_mode), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.stats_overlay_desc), style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = uiState.isLiveOverlayActive,
                    onCheckedChange = { viewModel.onAction(StatsUiAction.ToggleLiveOverlay(it)) },
                )
            }
        }

        Button(
            onClick = { viewModel.onAction(StatsUiAction.RefreshNow) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.stats_refresh))
        }
    }
}
