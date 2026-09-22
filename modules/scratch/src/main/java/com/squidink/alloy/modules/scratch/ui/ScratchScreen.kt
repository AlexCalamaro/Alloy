package com.squidink.alloy.modules.scratch.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.modules.scratch.ScratchUiAction
import com.squidink.alloy.modules.scratch.ScratchViewModel

@Composable
fun ScratchScreen(
    viewModel: ScratchViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pinned Scratchpad", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))

            // Timer Widget
            Card(modifier = Modifier.padding(start = 8.dp).desktopHover()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val minutes = uiState.timerSeconds / 60
                    val seconds = uiState.timerSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Button(onClick = { viewModel.onAction(ScratchUiAction.ToggleTimer) }) {
                        Text(if (uiState.isTimerRunning) "Pause" else "Start")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Note Editor
        OutlinedTextField(
            value = uiState.noteContent,
            onValueChange = { viewModel.onAction(ScratchUiAction.UpdateContent(it)) },
            modifier = Modifier.fillMaxSize(),
            label = { Text("Scratch Notes (.md)") }
        )
    }
}
