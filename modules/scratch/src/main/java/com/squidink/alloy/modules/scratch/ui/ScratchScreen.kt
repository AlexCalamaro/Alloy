package com.squidink.alloy.modules.scratch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.modules.scratch.ScratchPane
import com.squidink.alloy.modules.scratch.ScratchUiAction
import com.squidink.alloy.modules.scratch.ScratchViewModel

@Composable
fun ScratchScreen(
    viewModel: ScratchViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Header with timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Pinned Scratchpad", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))

            Card(modifier = Modifier.padding(start = 8.dp).desktopHover()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val minutes = uiState.timerSeconds / 60
                    val seconds = uiState.timerSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Button(onClick = { viewModel.onAction(ScratchUiAction.ToggleTimer) }) {
                        Text(if (uiState.isTimerRunning) "Pause" else "Start")
                    }
                    IconButton(onClick = { viewModel.onAction(ScratchUiAction.ResetTimer) }) {
                        Text("↺", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab selector
        TabRow(
            selectedTabIndex = uiState.activePane.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Tab(
                selected = uiState.activePane == ScratchPane.TEXT,
                onClick = { viewModel.onAction(ScratchUiAction.SetPane(ScratchPane.TEXT)) },
                text = { Text("Text") },
            )
            Tab(
                selected = uiState.activePane == ScratchPane.CHECKLIST,
                onClick = { viewModel.onAction(ScratchUiAction.SetPane(ScratchPane.CHECKLIST)) },
                text = { Text("Checklist") },
            )
            Tab(
                selected = uiState.activePane == ScratchPane.STOPWATCH,
                onClick = { viewModel.onAction(ScratchUiAction.SetPane(ScratchPane.STOPWATCH)) },
                text = { Text("Stopwatch") },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pane content
        when (uiState.activePane) {
            ScratchPane.TEXT -> {
                TextEditorPane(
                    content = uiState.noteContent,
                    onContentChange = { viewModel.onAction(ScratchUiAction.UpdateContent(it)) },
                )
            }

            ScratchPane.CHECKLIST -> {
                ChecklistPane(
                    items = uiState.checklistItems,
                    onAddItem = { viewModel.onAction(ScratchUiAction.AddChecklistItem(it)) },
                    onToggleItem = { viewModel.onAction(ScratchUiAction.ToggleChecklistItem(it)) },
                    onUpdateItem = { itemId, text -> viewModel.onAction(ScratchUiAction.UpdateChecklistItemText(itemId, text)) },
                    onDeleteItem = { viewModel.onAction(ScratchUiAction.DeleteChecklistItem(it)) },
                )
            }

            ScratchPane.STOPWATCH -> {
                StopwatchPane(
                    seconds = uiState.stopwatchSeconds,
                    laps = uiState.stopwatchLaps,
                    isRunning = uiState.isStopwatchRunning,
                    onStart = { viewModel.onAction(ScratchUiAction.StartStopwatch) },
                    onStop = { viewModel.onAction(ScratchUiAction.StopStopwatch) },
                    onReset = { viewModel.onAction(ScratchUiAction.ResetStopwatch) },
                    onLap = { viewModel.onAction(ScratchUiAction.AddLap) },
                )
            }
        }
    }
}

@Composable
private fun TextEditorPane(
    content: String,
    onContentChange: (String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier.fillMaxSize(),
            label = { Text("Scratch Notes (.md)") },
            maxLines = Int.MAX_VALUE,
        )
    }
}

@Composable
private fun ChecklistPane(
    items: List<com.squidink.alloy.modules.scratch.ChecklistItem>,
    onAddItem: (String) -> Unit,
    onToggleItem: (String) -> Unit,
    onUpdateItem: (String, String) -> Unit,
    onDeleteItem: (String) -> Unit,
) {
    var newItemText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Add new item
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("New checklist item...") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newItemText.isNotBlank()) {
                        onAddItem(newItemText)
                        newItemText = ""
                    }
                },
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Checklist items
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { item ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .desktopHover(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = item.isCompleted,
                        onCheckedChange = { onToggleItem(item.id) },
                    )
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                        color = if (item.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = { onDeleteItem(item.id) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                item {
                    Text(
                        "No checklist items. Add one above!",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray,
                    )
                }
            }
        }
    }
}

@Composable
private fun StopwatchPane(
    seconds: Int,
    laps: List<Int>,
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        val minutes = seconds / 60
        val secs = seconds % 60

        Text(
            text = String.format("%02d:%02d", minutes, secs),
            style = MaterialTheme.typography.displayLarge,
            color = if (isRunning) Color.Green else MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Button(onClick = if (isRunning) onStop else onStart, modifier = Modifier.padding(end = 8.dp)) {
                Text(if (isRunning) "Stop" else "Start")
            }
            Button(onClick = onLap, enabled = isRunning, modifier = Modifier.padding(end = 8.dp)) {
                Text("Lap")
            }
            Button(onClick = onReset) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (laps.isNotEmpty()) {
            Text("Laps", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(laps.reversed().withIndex().toList()) { (index, lapSeconds) ->
                    val lapMinutes = lapSeconds / 60
                    val lapSecs = lapSeconds % 60
                    val prevLap = if (index < laps.reversed().size - 1) laps.reversed()[index + 1] else 0
                    val split = lapSeconds - prevLap
                    val splitMin = split / 60
                    val splitSec = split % 60

                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "#${laps.size - index}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(40.dp),
                            )
                            Text(
                                text = String.format("%02d:%02d", lapMinutes, lapSecs),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "(+${String.format("%02d:%02d", splitMin, splitSec)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                "No laps recorded. Press 'Lap' while running!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
