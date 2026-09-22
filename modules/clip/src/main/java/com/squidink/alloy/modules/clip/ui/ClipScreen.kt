package com.squidink.alloy.modules.clip.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.modules.clip.ClipUiAction
import com.squidink.alloy.modules.clip.ClipViewModel
import com.squidink.alloy.modules.clip.TransformationType

@Composable
fun ClipScreen(
    viewModel: ClipViewModel,
    modifier: Modifier = Modifier
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
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            val filteredClips = uiState.clips.filter {
                it.textContent.contains(uiState.searchQuery, ignoreCase = true)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredClips) { clip ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { viewModel.onAction(ClipUiAction.SelectClip(clip)) }
                            .desktopHover()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(clip.textContent, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            Text("Source: ${clip.sourceApp}", style = MaterialTheme.typography.bodySmall)
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
                    Text(selected.textContent, modifier = Modifier.padding(16.dp))
                }

                Text("Quick Transformations:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.UPPERCASE)) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("UPPERCASE")
                    }

                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.LOWERCASE)) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("lowercase")
                    }

                    Button(
                        onClick = { viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.TRIM)) }
                    ) {
                        Text("Trim")
                    }
                }
            } else {
                Text("Select a clip to view details and apply transformations", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
