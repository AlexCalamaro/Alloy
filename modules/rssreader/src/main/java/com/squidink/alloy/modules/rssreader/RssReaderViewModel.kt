package com.squidink.alloy.modules.rssreader

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.core.domain.repository.IRssFeedRepository
import com.squidink.alloy.core.domain.repository.RssFeed
import com.squidink.alloy.core.domain.repository.RssFeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RssReaderUiState(
    val feeds: List<RssFeed> = emptyList(),
    val feedItems: List<RssFeedItem> = emptyList(),
    val selectedFeed: RssFeed? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val unreadCount: Int = 0,
    val error: String? = null,
) : UiState

sealed interface RssReaderUiAction : UiAction {
    data class AddFeed(
        val url: String,
        val title: String? = null,
    ) : RssReaderUiAction

    data class UpdateFeed(
        val feed: RssFeed,
    ) : RssReaderUiAction

    data class DeleteFeed(
        val feedId: String,
    ) : RssReaderUiAction

    data class SelectFeed(
        val feed: RssFeed?,
    ) : RssReaderUiAction

    data class UpdateSearchQuery(
        val query: String,
    ) : RssReaderUiAction

    data class MarkItemAsRead(
        val itemId: String,
    ) : RssReaderUiAction

    data class MarkFeedAsRead(
        val feedUrl: String,
    ) : RssReaderUiAction

    data class ToggleFavorite(
        val itemId: String,
    ) : RssReaderUiAction

    data class DeleteItem(
        val itemId: String,
    ) : RssReaderUiAction

    object RefreshFeeds : RssReaderUiAction
    object RefreshCurrentFeed : RssReaderUiAction
    object ClearError : RssReaderUiAction
}

sealed interface RssReaderUiEffect : UiEffect {
    data class ShowToast(
        val message: String,
    ) : RssReaderUiEffect

    data class NavigateToFeedDetails(
        val feedUrl: String,
    ) : RssReaderUiEffect

    data class NavigateToArticle(
        val articleUrl: String,
    ) : RssReaderUiEffect
}

@HiltViewModel
class RssReaderViewModel @Inject constructor(
    private val rssFeedRepository: IRssFeedRepository,
) : BaseViewModel<RssReaderUiState, RssReaderUiAction, RssReaderUiEffect>(
        RssReaderUiState()
    ) {

    init {
        // Observe all feed subscriptions
        observeFeedSubscriptions()

        // Observe unread count
        observeUnreadCount()

        // Observe feed items when a feed is selected
        observeFeedItems()
    }

    private fun observeFeedSubscriptions() {
        rssFeedRepository.getAllFeedSubscriptions()
            .onEach { feeds ->
                updateState { it.copy(feeds = filterBySearch(feeds)) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            val count = rssFeedRepository.getUnreadFeedItemsCount()
            updateState { it.copy(unreadCount = count) }
        }
    }

    private fun observeFeedItems() {
        rssFeedRepository.getAllFeedItems()
            .onEach { items ->
                val filtered = if (uiState.value.selectedFeed != null) {
                    items.filter { it.feedUrl == uiState.value.selectedFeed?.url }
                } else {
                    items
                }
                updateState { it.copy(feedItems = filtered) }
            }
            .launchIn(viewModelScope)
    }

    private fun filterBySearch(feeds: List<RssFeed>): List<RssFeed> {
        val query = uiState.value.searchQuery
        return if (query.isBlank()) {
            feeds
        } else {
            feeds.filter {
                it.title?.contains(query, ignoreCase = true) == true ||
                it.url.contains(query, ignoreCase = true) ||
                it.description?.contains(query, ignoreCase = true) == true
            }
        }
    }

    override fun onAction(action: RssReaderUiAction) {
        when (action) {
            is RssReaderUiAction.AddFeed -> addFeed(action.url, action.title)
            is RssReaderUiAction.UpdateFeed -> updateFeed(action.feed)
            is RssReaderUiAction.DeleteFeed -> deleteFeed(action.feedId)
            is RssReaderUiAction.SelectFeed -> selectFeed(action.feed)
            is RssReaderUiAction.UpdateSearchQuery -> updateSearchQuery(action.query)
            is RssReaderUiAction.MarkItemAsRead -> markItemAsRead(action.itemId)
            is RssReaderUiAction.MarkFeedAsRead -> markFeedAsRead(action.feedUrl)
            is RssReaderUiAction.ToggleFavorite -> toggleFavorite(action.itemId)
            is RssReaderUiAction.DeleteItem -> deleteItem(action.itemId)
            is RssReaderUiAction.RefreshFeeds -> refreshAllFeeds()
            is RssReaderUiAction.RefreshCurrentFeed -> refreshCurrentFeed()
            is RssReaderUiAction.ClearError -> clearError()
        }
    }

    private fun addFeed(url: String, title: String?) {
        viewModelScope.launch {
            try {
                val newFeed = RssFeed(
                    id = UUID.randomUUID().toString(),
                    url = url.trim(),
                    title = title,
                    description = null,
                    imageUrl = null,
                    isEnabled = true,
                    lastFetchedAt = 0,
                    fetchIntervalMinutes = 60,
                    maxItemsToKeep = 100
                )
                rssFeedRepository.addFeedSubscription(newFeed)
                sendEffect(RssReaderUiEffect.ShowToast("Feed added: $url"))
            } catch (e: Exception) {
                updateState { it.copy(error = "Failed to add feed: ${e.message}") }
            }
        }
    }

    private fun updateFeed(feed: RssFeed) {
        viewModelScope.launch {
            try {
                rssFeedRepository.updateFeedSubscription(feed)
                sendEffect(RssReaderUiEffect.ShowToast("Feed updated"))
            } catch (e: Exception) {
                updateState { it.copy(error = "Failed to update feed: ${e.message}") }
            }
        }
    }

    private fun deleteFeed(feedId: String) {
        viewModelScope.launch {
            try {
                val feed = rssFeedRepository.getFeedSubscriptionById(feedId)
                feed?.let {
                    rssFeedRepository.deleteFeedItemsByFeedUrl(it.url)
                    rssFeedRepository.deleteFeedSubscription(feedId)
                    sendEffect(RssReaderUiEffect.ShowToast("Feed deleted"))
                }
            } catch (e: Exception) {
                updateState { it.copy(error = "Failed to delete feed: ${e.message}") }
            }
        }
    }

    private fun selectFeed(feed: RssFeed?) {
        updateState { it.copy(selectedFeed = feed) }
    }

    private fun updateSearchQuery(query: String) {
        updateState { it.copy(searchQuery = query) }
    }

    private fun markItemAsRead(itemId: String) {
        viewModelScope.launch {
            try {
                rssFeedRepository.markItemAsRead(itemId)
                updateUnreadCount()
            } catch (e: Exception) {
                updateState { it.copy(error = "Failed to mark item as read: ${e.message}") }
            }
        }
    }

    private fun markFeedAsRead(feedUrl: String) {
        viewModelScope.launch {
            try {
                rssFeedRepository.markFeedItemsAsRead(feedUrl)
                updateUnreadCount()
                sendEffect(RssReaderUiEffect.ShowToast("Feed marked as read"))
            } catch (e: Exception) {
                updateState { it.copy(error = "Failed to mark feed as read: ${e.message}") }
            }
        }
    }

    private fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            try {
                val item = rssFeedRepository.getFeedItemById(itemId)
                item?.let {
                    rssFeedRepository.toggleFavorite(itemId, !it.isFavorite)
                }
            } catch (e: Exception) {
                updateState { it.copy(error = "Failed to toggle favorite: ${e.message}") }
            }
        }
    }

    private fun deleteItem(itemId: String) {
        viewModelScope.launch {
            try {
                rssFeedRepository.deleteFeedItem(itemId)
                updateUnreadCount()
                sendEffect(RssReaderUiEffect.ShowToast("Item deleted"))
            } catch (e: Exception) {
                updateState { it.copy(error = "Failed to delete item: ${e.message}") }
            }
        }
    }

    private fun refreshAllFeeds() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // TODO: Implement actual RSS feed fetching
                // This would use an RSS parsing library to fetch from URLs
                val feeds = rssFeedRepository.getEnabledFeedSubscriptionsList()
                feeds.forEach { feed ->
                    // fetchFeedItems(feed)
                }
                sendEffect(RssReaderUiEffect.ShowToast("Feeds refreshed"))
            } catch (e: Exception) {
                updateState { it.copy(error = "Failed to refresh feeds: ${e.message}") }
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    private fun refreshCurrentFeed() {
        val currentFeed = uiState.value.selectedFeed
        if (currentFeed != null) {
            updateState { it.copy(isLoading = true) }
            viewModelScope.launch {
                try {
                    // TODO: Implement actual RSS feed fetching
                    // fetchFeedItems(currentFeed)
                    sendEffect(RssReaderUiEffect.ShowToast("Feed refreshed"))
                } catch (e: Exception) {
                    updateState { it.copy(error = "Failed to refresh feed: ${e.message}") }
                } finally {
                    updateState { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun clearError() {
        updateState { it.copy(error = null) }
    }

    private fun updateUnreadCount() {
        viewModelScope.launch {
            val count = rssFeedRepository.getUnreadFeedItemsCount()
            updateState { it.copy(unreadCount = count) }
        }
    }

    /**
     * Fetch items from a specific RSS feed.
     * TODO: Implement with RSS parsing library
     */
    private suspend fun fetchFeedItems(feed: RssFeed) {
        // This would use an RSS parsing library like:
        // - com.romandanylyk:rssreader
        // - org.simpleframework.xml
        // 
        // Example:
        // val rssClient = RssClient.Builder().build()
        // val feedData = rssClient.fetch(feed.url)
        // val items = feedData.items.map { rssItem ->
        //     RssFeedItem(
        //         id = UUID.randomUUID().toString(),
        //         feedUrl = feed.url,
        //         feedTitle = feed.title,
        //         title = rssItem.title,
        //         description = rssItem.description,
        //         link = rssItem.link,
        //         author = rssItem.author,
        //         pubDate = rssItem.pubDate,
        //         imageUrl = rssItem.imageUrl
        //     )
        // }
        // rssFeedRepository.addFeedItems(items)
    }
}
