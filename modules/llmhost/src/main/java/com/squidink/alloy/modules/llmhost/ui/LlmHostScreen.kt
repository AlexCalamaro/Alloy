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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.layout.DetailPaneScaffold
import com.squidink.alloy.modules.llmhost.LlmHostUiAction
import com.squidink.alloy.modules.llmhost.LlmHostUiEffect
import com.squidink.alloy.modules.llmhost.LlmHostViewModel
import com.squidink.alloy.modules.llmhost.ui.components.ModelManagerCard
import com.squidink.alloy.modules.llmhost.ui.components.ModelRecommendationsCard
import com.squidink.alloy.modules.llmhost.ui.components.ModelTestCard
import com.squidink.alloy.modules.llmhost.ui.components.ServerStatusCard
import com.squidink.alloy.modules.llmhost.ui.components.StorageInfoCard

@Composable
fun LlmHostScreen(
    viewModel: LlmHostViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LlmHostUiEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is LlmHostUiEffect.CopyToClipboard -> {
                    snackbarHostState.showSnackbar("Copied ${effect.label} to clipboard")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "LLM Engine Host",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Headless on-device inference (LiteRT GPU & llama.cpp CPU) for Googlebook apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { viewModel.onAction(LlmHostUiAction.OpenSettings) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Host Settings")
                }
            }

            // 1. Server Status & Toggle Card
            ServerStatusCard(
                status = uiState.hostStatus,
                config = uiState.serverConfig,
                activeModelName = uiState.installedModel?.fileName,
                onToggle = { enable -> viewModel.onAction(LlmHostUiAction.ToggleHost(enable)) }
            )

            // 2. Storage & Model Status Card
            StorageInfoCard(
                installedModels = uiState.installedModels,
                activeModel = uiState.activeModel ?: uiState.installedModel,
                storageUsage = uiState.storageUsage,
                onSelectActiveModel = { model -> viewModel.onAction(LlmHostUiAction.SelectActiveModel(model)) },
                onDeleteSpecificModel = { model -> viewModel.onAction(LlmHostUiAction.DeleteSpecificModel(model)) }
            )

            // 3. Test Feature: Run Sample Prompt
            ModelTestCard(
                testState = uiState.testInference,
                isModelInstalled = uiState.installedModel != null,
                onRunPrompt = { prompt -> viewModel.onAction(LlmHostUiAction.RunTestPrompt(prompt)) }
            )

            // 4. Model Downloader Card (Hugging Face)
            ModelManagerCard(
                urlInput = uiState.selectedDownloadUrl,
                tokenInput = uiState.hfTokenInput,
                downloadProgress = uiState.downloadProgress,
                onUrlChange = { url -> viewModel.onAction(LlmHostUiAction.UpdateUrlInput(url)) },
                onTokenChange = { token -> viewModel.onAction(LlmHostUiAction.UpdateTokenInput(token)) },
                onStartDownload = { url, token -> viewModel.onAction(LlmHostUiAction.StartDownload(url, token)) },
                onCancelDownload = { viewModel.onAction(LlmHostUiAction.CancelDownload) }
            )

            // 5. Recommended Models for Googlebook OS
            ModelRecommendationsCard(
                recommendations = uiState.recommendedModels,
                onSelectModel = { model -> viewModel.onAction(LlmHostUiAction.SelectRecommendedModel(model)) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Settings Side Sheet
        DetailPaneScaffold(
            isOpen = uiState.isSettingsOpen,
            onDismiss = { viewModel.onAction(LlmHostUiAction.DismissSettings) },
            title = "LLM Host Settings"
        ) {
            LlmHostSettingsPanel(
                state = uiState,
                onAction = viewModel::onAction
            )
        }
    }
}
