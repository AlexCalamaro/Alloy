package com.squidink.alloy.core.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable container for a settings section with a title.
 *
 * Provides consistent spacing and typography for settings groups.
 *
 * @param title The section title displayed at the top
 * @param modifier Modifier to apply to the root container
 * @param content The settings content to display within the section
 */
@Composable
fun SettingSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

/**
 * Reusable info card for displaying tips, warnings, or helpful information.
 *
 * Uses a primary container color with reduced opacity for visual distinction.
 *
 * @param message The message text to display
 * @param modifier Modifier to apply to the card
 */
@Composable
fun InfoCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Reusable card for displaying a single setting with optional subtitle and value.
 *
 * Provides a consistent card layout with proper spacing for setting elements.
 *
 * @param title The setting title
 * @param subtitle Optional description or hint text
 * @param value Optional displayed value
 * @param modifier Modifier to apply to the card
 * @param content Additional custom content to display at the end of the card
 */
@Composable
fun SettingCard(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            
            subtitle?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            value?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            content()
        }
    }
}

/**
 * Reusable container for feature detail content with a title header.
 *
 * Provides the standard layout for all FeatureDetail implementations.
 *
 * @param title The feature detail title displayed at the top
 * @param modifier Modifier to apply to the root container
 * @param content The feature-specific content to display
 */
@Composable
fun FeatureDetailSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        content()
    }
}
