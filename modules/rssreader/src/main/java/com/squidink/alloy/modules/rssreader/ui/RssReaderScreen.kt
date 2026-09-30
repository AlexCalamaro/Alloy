package com.squidink.alloy.modules.rssreader.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.layout.DetailPaneScaffold
import com.squidink.alloy.core.layout.deriveWindowSizeClass
import com.squidink.alloy.core.layout.isExpanded
import com.squidink.alloy.modules.rssreader.ArticleFilter
import com.squidink.alloy.modules.rssreader.RssReaderUiAction
import com.squidink.alloy.modules.rssreader.RssReaderUiEffect
import com.squidink.alloy.modules.rssreader.RssReaderUiState
import com.squidink.alloy.modules.rssreader.RssReaderViewModel

/**
 * Top-level Stateful RSS Reader screen composable.
 */
@Composable
fun RssReaderScreen(
    viewModel: RssReaderViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RssReaderUiEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is RssReaderUiEffect.NavigateToArticle -> {
                    openInNativeBrowser(context, effect.articleUrl)
                }
            }
        }
    }

    RssReaderScreen(
        state = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/**
 * Stateless RSS Reader Screen supporting adaptive layout and slide-in settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssReaderScreen(
    state: RssReaderUiState,
    onAction: (RssReaderUiAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val windowSizeClass = deriveWindowSizeClass()
    val isExpandedScreen = windowSizeClass.isExpanded()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "RSS Reader",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        state.selectedFeed?.let { feed ->
                            Text(
                                text = feed.title ?: feed.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Mark all read button
                    if (state.unreadCount > 0) {
                        IconButton(onClick = { onAction(RssReaderUiAction.MarkAllAsRead) }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Mark all as read"
                            )
                        }
                    }

                    // Refresh button
                    IconButton(
                        onClick = {
                            if (state.selectedFeed != null) {
                                onAction(RssReaderUiAction.RefreshCurrentFeed)
                            } else {
                                onAction(RssReaderUiAction.RefreshFeeds)
                            }
                        }
                    ) {
                        if (state.unreadCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(text = if (state.unreadCount > 99) "99+" else state.unreadCount.toString())
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh feeds")
                            }
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh feeds")
                        }
                    }

                    // Settings Sidebar Toggle
                    IconButton(onClick = { onAction(RssReaderUiAction.OpenSettings) }) {
                        Icon(Icons.Default.Settings, contentDescription = "RSS Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(RssReaderUiAction.OpenAddSheet) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Feed")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { onAction(RssReaderUiAction.UpdateSearchQuery(it)) },
                    placeholder = { Text("Search articles or topics...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onAction(RssReaderUiAction.UpdateSearchQuery("")) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Filter chips carousel (All, Unread, Favorites, plus Subscribed Feeds)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.selectedFeed == null && state.filterType == ArticleFilter.ALL,
                        onClick = {
                            onAction(RssReaderUiAction.SelectFeed(null))
                            onAction(RssReaderUiAction.SetFilterType(ArticleFilter.ALL))
                        },
                        label = { Text("All Feeds") }
                    )

                    FilterChip(
                        selected = state.filterType == ArticleFilter.UNREAD,
                        onClick = {
                            val next = if (state.filterType == ArticleFilter.UNREAD) ArticleFilter.ALL else ArticleFilter.UNREAD
                            onAction(RssReaderUiAction.SetFilterType(next))
                        },
                        label = { Text("Unread (${state.unreadCount})") }
                    )

                    FilterChip(
                        selected = state.filterType == ArticleFilter.FAVORITES,
                        onClick = {
                            val next = if (state.filterType == ArticleFilter.FAVORITES) ArticleFilter.ALL else ArticleFilter.FAVORITES
                            onAction(RssReaderUiAction.SetFilterType(next))
                        },
                        label = { Text("Favorites") }
                    )

                    // Feed-specific filter chips
                    state.feeds.forEach { feed ->
                        FilterChip(
                            selected = state.selectedFeed?.id == feed.id,
                            onClick = {
                                val next = if (state.selectedFeed?.id == feed.id) null else feed
                                onAction(RssReaderUiAction.SelectFeed(next))
                            },
                            label = {
                                Text(
                                    text = feed.title ?: feed.url,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                // Loading progress bar
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                }

                // Main Adaptive Feed List
                if (state.feedItems.isEmpty() && !state.isLoading) {
                    EmptyArticlesState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    if (isExpandedScreen) {
                        // Staggered multi-column grid on Expanded screens (tablets / desktop)
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Adaptive(minSize = 340.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalItemSpacing = 16.dp
                        ) {
                            items(state.feedItems, key = { it.id }) { item ->
                                RssArticleCard(
                                    item = item,
                                    onOpenArticle = { url ->
                                        onAction(RssReaderUiAction.MarkItemAsRead(item.id))
                                        openInNativeBrowser(context, url)
                                    },
                                    onToggleFavorite = { onAction(RssReaderUiAction.ToggleFavorite(item.id)) },
                                    onToggleRead = { onAction(RssReaderUiAction.MarkItemAsRead(item.id)) }
                                )
                            }
                        }
                    } else {
                        // Single-column list on Compact / Medium screens (phones)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.feedItems, key = { it.id }) { item ->
                                RssArticleCard(
                                    item = item,
                                    onOpenArticle = { url ->
                                        onAction(RssReaderUiAction.MarkItemAsRead(item.id))
                                        openInNativeBrowser(context, url)
                                    },
                                    onToggleFavorite = { onAction(RssReaderUiAction.ToggleFavorite(item.id)) },
                                    onToggleRead = { onAction(RssReaderUiAction.MarkItemAsRead(item.id)) }
                                )
                            }
                        }
                    }
                }
            }

            // Slide-over Settings Sidebar using DetailPaneScaffold
            DetailPaneScaffold(
                isOpen = state.showSettings,
                onDismiss = { onAction(RssReaderUiAction.DismissSettings) },
                title = "RSS Settings"
            ) {
                RssSettingsPanel(
                    state = state,
                    onAction = onAction
                )
            }

            // Material 3 Modal Bottom Sheet for Adding Feeds
            if (state.showAddSheet) {
                RssAddFeedSheet(
                    onDismiss = { onAction(RssReaderUiAction.DismissAddSheet) },
                    onAddFeed = { url, title ->
                        onAction(RssReaderUiAction.AddFeed(url, title))
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyArticlesState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No articles found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Subscribe to feeds or adjust your filter query",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
