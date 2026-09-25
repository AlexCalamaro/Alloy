package com.squidink.alloy.modules.statspill.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.R
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.core.layout.DetailPaneScaffold
import com.squidink.alloy.core.permissions.AppPermission
import com.squidink.alloy.core.permissions.ui.PermissionRationaleDialog
import com.squidink.alloy.modules.statspill.StatsUiAction
import com.squidink.alloy.modules.statspill.StatsUiEffect
import com.squidink.alloy.modules.statspill.StatsViewModel

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val effects by viewModel.effect.collectAsState(initial = null)
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val showPermissionDialog = remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Handle effects (toasts and permission requests)
    LaunchedEffect(effects) {
        effects?.let { effect ->
            when (effect) {
                is StatsUiEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is StatsUiEffect.OpenOverlayPermissionSettings -> {
                    showPermissionDialog.value = true
                }
            }
        }
    }

    // Convert uiState to ResourceStats for grid display
    val resourceStats = uiState.toResourceStats()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.stats_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Stats Grid with color-coded backgrounds
            StatsGrid(
                stats = resourceStats,
                settings = settings,
                modifier = Modifier.weight(1f)
            )

            // Settings button
            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 16.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }

            // Live Mode Floating Overlay Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .desktopHover(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.stats_overlay_mode),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.stats_overlay_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = uiState.isLiveOverlayActive,
                        onCheckedChange = { viewModel.onAction(StatsUiAction.ToggleLiveOverlay(it)) },
                    )
                }
            }

            // Refresh button
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.onAction(StatsUiAction.RefreshNow) },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(stringResource(R.string.stats_refresh))
                }
            }
        }

        // Detail pane overlay for settings
        DetailPaneScaffold(
            isOpen = showSettings,
            onDismiss = { showSettings = false },
            title = "Stats Settings"
        ) {
            SettingsPanel(
                settings = settings,
                onSettingsChange = viewModel::updateSettings
            )
        }
    }

    // Permission Rationale Dialog for Overlay Permission
    if (showPermissionDialog.value) {
        PermissionRationaleDialog(
            permission = AppPermission.SystemOverlay,
            onGrantClick = {
                viewModel.onAction(StatsUiAction.OpenOverlayPermissionSettings)
                showPermissionDialog.value = false
            },
            onSettingsClick = {
                viewModel.onAction(StatsUiAction.OpenOverlayPermissionSettings)
                showPermissionDialog.value = false
            },
            onDismiss = {
                showPermissionDialog.value = false
            }
        )
    }
}
