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
import androidx.compose.ui.unit.dp
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
        Text("System Telemetry Vitals", style = MaterialTheme.typography.headlineSmall)
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
                Text("CPU Utilization", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val cpuText = uiState.cpuUsagePercent?.let { String.format(Locale.US, "%.1f%%", it) } ?: "Calculating..."
                Text("CPU Usage: $cpuText", style = MaterialTheme.typography.bodyLarge)
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
                Text("RAM Usage", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val totalMb = uiState.memInfo.totalMemKb / 1024
                val availMb = uiState.memInfo.availableMemKb / 1024
                val usedMb = totalMb - availMb
                Text("Total Memory: $totalMb MB")
                Text("Used Memory: $usedMb MB")
                Text("Available Memory: $availMb MB")
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
                Text("Battery Status", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val battery = uiState.batteryInfo
                Text("Level: ${battery.percentage}% (${battery.level}/${battery.scale})")
                Text("Status: ${battery.getStatusString()}")
                Text("Health: ${battery.getHealthString()}")
                Text("Temperature: ${battery.getTemperatureCelsius()}°C")
                Text("Voltage: ${battery.voltage} mV")
                if (battery.isCharging) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡ Charging", color = Color.Green)
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
                    Text("Live Overlay Pill Mode", style = MaterialTheme.typography.titleMedium)
                    Text("Float live CPU/RAM/battery vitals over all desktop windows", style = MaterialTheme.typography.bodySmall)
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
            Text("Refresh Now")
        }
    }
}
