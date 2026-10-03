package com.squidink.alloy.modules.llmhost.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.modules.llmhost.LlmHostUiAction
import com.squidink.alloy.modules.llmhost.LlmHostUiState
import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import java.security.SecureRandom
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmHostSettingsPanel(
    state: LlmHostUiState,
    onAction: (LlmHostUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var portInput by remember(state.serverConfig.port) { mutableStateOf(state.serverConfig.port.toString()) }
    var tokenInput by remember(state.serverConfig.authToken) { mutableStateOf(state.serverConfig.authToken) }
    var selectedBackend by remember(state.serverConfig.backend) { mutableStateOf(state.serverConfig.backend) }
    var expandedBackend by remember { mutableStateOf(false) }

    // Model Tuning Parameters
    val initialTuning = state.tuningConfig
    var temperature by remember(initialTuning.temperature) { mutableFloatStateOf(initialTuning.temperature) }
    var topP by remember(initialTuning.topP) { mutableFloatStateOf(initialTuning.topP) }
    var maxTokens by remember(initialTuning.maxTokens) { mutableIntStateOf(initialTuning.maxTokens) }
    var contextSize by remember(initialTuning.contextSize) { mutableIntStateOf(initialTuning.contextSize) }
    var threadCount by remember(initialTuning.threadCount) { mutableIntStateOf(initialTuning.threadCount) }

    val availableCpus = remember { Runtime.getRuntime().availableProcessors().coerceAtLeast(1) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Port Configuration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .desktopHover(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Server Port",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Port for the loopback server on 127.0.0.1 (default: 8787)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bearer Token Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .desktopHover(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Authentication Bearer Token",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bearer token required by external apps querying the localhost API.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("API Token") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val bytes = ByteArray(TOKEN_BYTES_COUNT)
                            SecureRandom().nextBytes(bytes)
                            tokenInput = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate Token")
                    }
                }
            }
        }

        // Hardware Backend Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .desktopHover(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hardware Execution Backend",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Preferred hardware backend for LiteRT models (GGUF models always run on CPU via llama.cpp).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedBackend,
                    onExpandedChange = { expandedBackend = it }
                ) {
                    OutlinedTextField(
                        value = selectedBackend.displayName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = TextFieldDefaults.colors(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBackend)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expandedBackend,
                        onDismissRequest = { expandedBackend = false }
                    ) {
                        EngineBackend.entries.forEach { backend ->
                            DropdownMenuItem(
                                text = { Text(backend.displayName) },
                                onClick = {
                                    selectedBackend = backend
                                    expandedBackend = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Model Generation & Tuning Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .desktopHover(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "  Model Generation & Tuning",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fine-tune inference hyper-parameters across LiteRT and llama.cpp backends.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Temperature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Temperature", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = String.format(Locale.US, "%.2f", temperature),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = temperature,
                    onValueChange = { temperature = (it * 100).roundToInt() / 100f },
                    valueRange = 0.0f..2.0f,
                    steps = 39,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Lower values produce deterministic output; higher values increase randomness.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Top-P
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Top-P (Nucleus)", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = String.format(Locale.US, "%.2f", topP),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = topP,
                    onValueChange = { topP = (it * 100).roundToInt() / 100f },
                    valueRange = 0.05f..1.0f,
                    steps = 18,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Cumulative probability cutoff for nucleus token sampling.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Max Output Tokens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Max Output Tokens", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "$maxTokens tokens",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = { maxTokens = it.roundToInt() },
                    valueRange = 128f..4096f,
                    steps = 30,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Maximum generation length for completions.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Context Size (n_ctx)
                Text(text = "Context Window Size (llama.cpp n_ctx)", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1024, 2048, 4096, 8192).forEach { ctx ->
                        FilterChip(
                            selected = contextSize == ctx,
                            onClick = { contextSize = ctx },
                            label = { Text("$ctx") }
                        )
                    }
                }
                Text(
                    text = "Larger context consumes more RAM. 2048 is recommended on Googlebook OS.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                // CPU Threads
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "CPU Worker Threads", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "$threadCount / $availableCpus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = threadCount.toFloat(),
                    onValueChange = { threadCount = it.roundToInt() },
                    valueRange = 1f..availableCpus.toFloat(),
                    steps = (availableCpus - 2).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Number of CPU cores allocated to llama.cpp computation.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Button(
            onClick = {
                val port = portInput.toIntOrNull() ?: DEFAULT_PORT
                onAction(
                    LlmHostUiAction.UpdateServerConfig(
                        port = port,
                        authToken = tokenInput,
                        backend = selectedBackend
                    )
                )
                onAction(
                    LlmHostUiAction.UpdateTuningConfig(
                        ModelTuningConfig(
                            temperature = temperature,
                            maxTokens = maxTokens,
                            contextSize = contextSize,
                            topP = topP,
                            threadCount = threadCount
                        )
                    )
                )
                onAction(LlmHostUiAction.DismissSettings)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Host Settings")
        }
    }
}

private const val TOKEN_BYTES_COUNT = 24
private const val DEFAULT_PORT = 8787
