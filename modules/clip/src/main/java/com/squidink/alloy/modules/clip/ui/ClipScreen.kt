package com.squidink.alloy.modules.clip.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.modules.clip.ClipUiAction
import com.squidink.alloy.modules.clip.ClipViewModel
import com.squidink.alloy.modules.clip.TransformationType

@Composable
fun ClipScreen(
    viewModel: ClipViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Row(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Clips List Pane
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text("Clipboard Workbench", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onAction(ClipUiAction.UpdateSearchQuery(it)) },
                label = { Text("Search Clipboard...") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )

            // Pinned filter
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("Show:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = uiState.showPinnedOnly,
                    onClick = { viewModel.onAction(ClipUiAction.TogglePinnedOnly(!uiState.showPinnedOnly)) },
                    label = { Text("Pinned Only") },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${uiState.clips.size} clips", style = MaterialTheme.typography.bodySmall)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.clips, key = { it.id }) { clip ->
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable { viewModel.onAction(ClipUiAction.SelectClip(clip)) }
                                .desktopHover(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Pin indicator
                            if (clip.isPinned) {
                                Icon(
                                    imageVector = Icons.Filled.Pin,
                                    contentDescription = "Pinned",
                                    tint = Color(0xFFFFA726),
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(clip.textContent, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                Text("Source: ${clip.sourceApp}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    formatTimestamp(clip.timestamp),
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                )
                            }

                            // Pin/Unpin button
                            IconButton(
                                onClick = { viewModel.onAction(ClipUiAction.TogglePin(clip.id)) },
                            ) {
                                Icon(
                                    imageVector =
                                        if (clip.isPinned) {
                                            Icons.Filled.Pin
                                        } else {
                                            Icons.Outlined.Pin
                                        },
                                    contentDescription = if (clip.isPinned) "Unpin" else "Pin",
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = { viewModel.onAction(ClipUiAction.DeleteClip(clip.id)) },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                )
                            }
                        }
                    }
                }
            }
        }

        // Preview & Transformation Pane
        Column(modifier = Modifier.weight(1f)) {
            Text("Selected Clip & Transformations", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            val selected = uiState.selectedClip
            if (selected != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(selected.textContent, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selected.isPinned) {
                                Icon(
                                    imageVector = Icons.Filled.Pin,
                                    contentDescription = "Pinned",
                                    tint = Color(0xFFFFA726),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("Source: ${selected.sourceApp}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Text("Quick Transformations:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // First row
                Row {
                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.UPPER_CASE)) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("UPPER")
                    }
                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.LOWER_CASE)) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("lower")
                    }
                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.TRIM)) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Trim")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Second row
                Row {
                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.SORT_LINES_ASC)) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Sort ↑")
                    }
                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.DEDUPE_LINES)) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Dedupe")
                    }
                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.JSON_PRETTY)) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("JSON")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Third row
                Row {
                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.BASE64_ENCODE)) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("B64+")
                    }
                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.BASE64_DECODE)) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("B64-")
                    }
                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.URL_ENCODE)) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("URL+")
                    }
                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.URL_DECODE)) },
                    ) {
                        Text("URL-")
                    }
                }
            } else {
                Text("Select a clip to view details and apply transformations", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}
