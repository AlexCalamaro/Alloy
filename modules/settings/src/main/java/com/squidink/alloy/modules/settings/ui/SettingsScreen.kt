package com.squidink.alloy.modules.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.DesktopVerticalScrollbar
import com.squidink.alloy.core.design.R
import com.squidink.alloy.modules.settings.SettingsUiAction
import com.squidink.alloy.modules.settings.SettingsUiState
import com.squidink.alloy.modules.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            AppearanceSection(uiState = uiState, onAction = onAction)

            Spacer(modifier = Modifier.height(8.dp))

            SystemSection(uiState = uiState, onAction = onAction)

            Spacer(modifier = Modifier.height(8.dp))

            CacheSection(uiState = uiState, onAction = onAction)
        }

        DesktopVerticalScrollbar(
            scrollState = scrollState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun AppearanceSection(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_appearance_header),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_dynamic_color_title),
            description = stringResource(R.string.settings_dynamic_color_desc),
            checked = uiState.dynamicColor,
            onCheckedChange = { onAction(SettingsUiAction.SetDynamicColor(it)) }
        )
    }
}

@Composable
private fun SystemSection(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_system_header),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_percentages_title),
            description = stringResource(R.string.settings_percentages_desc),
            checked = uiState.usePercentages,
            onCheckedChange = { onAction(SettingsUiAction.SetUsePercentages(it)) }
        )
    }
}

@Composable
private fun CacheSection(
    uiState: SettingsUiState,
    onAction: (SettingsUiAction) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_cache_header),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_cache_title),
            description = stringResource(R.string.settings_cache_desc),
            checked = uiState.cacheEnabled,
            onCheckedChange = { onAction(SettingsUiAction.SetCacheEnabled(it)) }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
