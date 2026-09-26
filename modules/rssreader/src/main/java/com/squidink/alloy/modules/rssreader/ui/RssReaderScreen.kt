package com.squidink.alloy.modules.rssreader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.domain.repository.RssFeed
import com.squidink.alloy.core.domain.repository.RssFeedItem
import com.squidink.alloy.modules.rssreader.RssReaderUiAction
import com.squidink.alloy.modules.rssreader.RssReaderUiState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main RSS Reader screen composable.
 * Displays feed subscriptions and feed items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssReaderScreen(
    state: RssReaderUiState,
    onAction: (RssReaderUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RSS Reader") },
                actions = {
                    IconButton(onClick = { onAction(RssReaderUiAction.RefreshFeeds) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    if (state.unreadCount > 0) {
                        BadgeContainer {
                            Badge {
                                Text(text = state.unreadCount.toString())
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Show add feed dialog */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Feed")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onAction(RssReaderUiAction.UpdateSearchQuery(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search feeds...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true
            )

            // Main content
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Feeds list
                    FeedsList(
                        feeds = state.feeds,
                        selectedFeed = state.selectedFeed,
                        onFeedClick = { onAction(RssReaderUiAction.SelectFeed(it)) },
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                    )

                    // Feed items or empty state
                    if (state.selectedFeed != null) {
                        FeedItemsList(
                            items = state.feedItems,
                            onItemClick = { /* Navigate to article */ },
                            onMarkAsRead = { onAction(RssReaderUiAction.MarkItemAsRead(it)) },
                            onToggleFavorite = { onAction(RssReaderUiAction.ToggleFavorite(it)) },
                            onDelete = { onAction(RssReaderUiAction.DeleteItem(it)) },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        EmptyState(
                            message = "Select a feed to view articles",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Error message
            state.error?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * List of RSS feed subscriptions.
 */
@Composable
private fun FeedsList(
    feeds: List<RssFeed>,
    selectedFeed: RssFeed?,
    onFeedClick: (RssFeed) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalDivider(width = 1.dp, color = Color.LightGray)
    ) {
        Text(
            text = "Subscriptions (${feeds.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn {
            items(feeds, key = { it.id }) { feed ->
                FeedItemRow(
                    feed = feed,
                    isSelected = feed.id == selectedFeed?.id,
                    onClick = { onFeedClick(feed) }
                )
            }

            if (feeds.isEmpty()) {
                item {
                    EmptyState(
                        message = "No feeds subscribed",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Individual feed row.
 */
@Composable
private fun FeedItemRow(
    feed: RssFeed,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Feed icon
            Icon(
                imageVector = Icons.Default.RssFeed,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(end = 12.dp)
            )

            // Feed title
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feed.title ?: feed.url,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = feed.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * List of RSS feed items (articles).
 */
@Composable
private fun FeedItemsList(
    items: List<RssFeedItem>,
    onItemClick: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            FeedItemCard(
                item = item,
                onClick = { onItemClick(item.id) },
                onMarkAsRead = { onMarkAsRead(item.id) },
                onToggleFavorite = { onToggleFavorite(item.id) },
                onDelete = { onDelete(item.id) }
            )
        }

        if (items.isEmpty()) {
            item {
                EmptyState(
                    message = "No articles in this feed",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Individual feed item card.
 */
@Composable
private fun FeedItemCard(
    item: RssFeedItem,
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Description preview
            item.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date
                Text(
                    text = formatDate(item.pubDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Actions
                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    IconButton(onClick = onMarkAsRead) {
                        Icon(
                            imageVector = if (item.isRead) Icons.Default.CheckCircle else Icons.Default.Circle,
                            contentDescription = "Mark as read",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state placeholder.
 */
@Composable
private fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Format timestamp to readable date.
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Vertical divider composable.
 */
@Composable
private fun Modifier.verticalDivider(
    width: dp,
    color: Color
): Modifier {
    return this.then(
        Modifier
            .width(width)
            .background(color)
    )
}
