package com.squidink.alloy.modules.rssreader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.core.domain.common.model.RssFeed
import com.squidink.alloy.modules.rssreader.RssReaderUiAction
import com.squidink.alloy.modules.rssreader.RssReaderUiState

/**
 * Slide-in Settings Sidebar for RSS Reader, embedded in DetailPaneScaffold.
 * Mirrors the StatsPill SettingsPanel structure and design language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssSettingsPanel(
    state: RssReaderUiState,
    onAction: (RssReaderUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedMaxItems by remember { mutableStateOf(false) }
    var expandedRetention by remember { mutableStateOf(false) }

    val maxItemOptions = listOf(25, 50, 100, 200)
    val retentionDayOptions = listOf(7, 14, 30)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ==================== Storage & Cache Stats Card ====================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .desktopHover(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Encrypted Storage (Room SQLCipher)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Articles:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${state.feedItems.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Unread Articles:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${state.unreadCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Favorite Articles:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${state.feedItems.count { it.isFavorite }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // ==================== Retention Configuration Card ====================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .desktopHover(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Intelligent Cache Policies",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Max items per feed
                ExposedDropdownMenuBox(
                    expanded = expandedMaxItems,
                    onExpandedChange = { expandedMaxItems = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    OutlinedTextField(
                        value = "Keep at most: ${state.maxItemsPerFeed} articles/feed",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMaxItems) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMaxItems,
                        onDismissRequest = { expandedMaxItems = false }
                    ) {
                        maxItemOptions.forEach { count ->
                            DropdownMenuItem(
                                text = { Text("$count articles per feed") },
                                onClick = {
                                    onAction(RssReaderUiAction.UpdateMaxItemsPerFeed(count))
                                    expandedMaxItems = false
                                }
                            )
                        }
                    }
                }

                // Read retention days
                ExposedDropdownMenuBox(
                    expanded = expandedRetention,
                    onExpandedChange = { expandedRetention = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = "Prune read articles after: ${state.retentionDays} days",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRetention) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRetention,
                        onDismissRequest = { expandedRetention = false }
                    ) {
                        retentionDayOptions.forEach { days ->
                            DropdownMenuItem(
                                text = { Text("$days days") },
                                onClick = {
                                    onAction(RssReaderUiAction.UpdateRetentionDays(days))
                                    expandedRetention = false
                                }
                            )
                        }
                    }
                }

                // Actions: Clear Read & Purge Ancient
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onAction(RssReaderUiAction.ClearReadArticles) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Clear Read", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { onAction(RssReaderUiAction.PurgeAncientArticles) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Purge Old", maxLines = 1)
                    }
                }
            }
        }

        // ==================== Feed Subscriptions List ====================
        Text(
            text = "Active Subscriptions (${state.feeds.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        state.feeds.forEach { feed ->
            FeedSubscriptionRow(
                feed = feed,
                onToggleEnabled = { enabled ->
                    onAction(RssReaderUiAction.ToggleFeedEnabled(feed.id, enabled))
                },
                onDelete = {
                    onAction(RssReaderUiAction.DeleteFeed(feed.id))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.feeds.isEmpty()) {
            Text(
                text = "No feeds subscribed. Use the + button to add feeds.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun FeedSubscriptionRow(
    feed: RssFeed,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .desktopHover(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feed.title ?: feed.url,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = feed.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = feed.isEnabled,
                onCheckedChange = onToggleEnabled,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete feed",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
