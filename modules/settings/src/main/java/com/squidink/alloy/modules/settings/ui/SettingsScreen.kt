package com.squidink.alloy.modules.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squidink.alloy.modules.settings.SettingsUiAction
import com.squidink.alloy.modules.settings.SettingsUiState
import com.squidink.alloy.modules.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateTo: (String) -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateTo = onNavigateTo,
        onNavigateUp = onNavigateUp,
    )
}

@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
    onNavigateTo: (String) -> Unit,
    onNavigateUp: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Add your screen content here
        }
    }
}
