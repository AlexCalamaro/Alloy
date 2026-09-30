package com.squidink.alloy.modules.rssreader

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.core.domain.common.model.RssFeed
import com.squidink.alloy.core.domain.common.model.RssFeedItem
import com.squidink.alloy.modules.rssreader.domain.usecase.AddFeedUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.DeleteFeedUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.MarkArticleReadUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.ObserveArticlesUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.ObserveFeedsUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.PurgeCacheUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.RefreshFeedsUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.ToggleFavoriteUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.UpdateFeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ArticleFilter {
    ALL,
    UNREAD,
    FAVORITES
}

data class RssReaderUiState(
    val feeds: List<RssFeed> = emptyList(),
    val rawArticles: List<RssFeedItem> = emptyList(),
    val feedItems: List<RssFeedItem> = emptyList(),
    val selectedFeed: RssFeed? = null,
    val filterType: ArticleFilter = ArticleFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val unreadCount: Int = 0,
    val error: String? = null,
    val showAddSheet: Boolean = false,
    val showSettings: Boolean = false,
    val maxItemsPerFeed: Int = 100,
    val retentionDays: Int = 14
) : UiState

sealed interface RssReaderUiAction : UiAction {
    data class AddFeed(val url: String, val title: String? = null) : RssReaderUiAction
    data class UpdateFeed(val feed: RssFeed) : RssReaderUiAction
    data class DeleteFeed(val feedId: String) : RssReaderUiAction
    data class ToggleFeedEnabled(val feedId: String, val isEnabled: Boolean) : RssReaderUiAction
    data class SelectFeed(val feed: RssFeed?) : RssReaderUiAction
    data class SetFilterType(val filter: ArticleFilter) : RssReaderUiAction
    data class UpdateSearchQuery(val query: String) : RssReaderUiAction
    data class MarkItemAsRead(val itemId: String) : RssReaderUiAction
    data class MarkFeedAsRead(val feedUrl: String) : RssReaderUiAction
    data object MarkAllAsRead : RssReaderUiAction
    data class ToggleFavorite(val itemId: String) : RssReaderUiAction
    data class UpdateMaxItemsPerFeed(val count: Int) : RssReaderUiAction
    data class UpdateRetentionDays(val days: Int) : RssReaderUiAction
    data object ClearReadArticles : RssReaderUiAction
    data object PurgeAncientArticles : RssReaderUiAction
    data object RefreshFeeds : RssReaderUiAction
    data object RefreshCurrentFeed : RssReaderUiAction
    data object OpenAddSheet : RssReaderUiAction
    data object DismissAddSheet : RssReaderUiAction
    data object OpenSettings : RssReaderUiAction
    data object DismissSettings : RssReaderUiAction
    data object ClearError : RssReaderUiAction
}

sealed interface RssReaderUiEffect : UiEffect {
    data class ShowToast(val message: String) : RssReaderUiEffect
    data class NavigateToArticle(val articleUrl: String) : RssReaderUiEffect
}

@HiltViewModel
class RssReaderViewModel @Inject constructor(
    private val observeFeedsUseCase: ObserveFeedsUseCase,
    private val observeArticlesUseCase: ObserveArticlesUseCase,
    private val refreshFeedsUseCase: RefreshFeedsUseCase,
    private val addFeedUseCase: AddFeedUseCase,
    private val updateFeedUseCase: UpdateFeedUseCase,
    private val deleteFeedUseCase: DeleteFeedUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val markArticleReadUseCase: MarkArticleReadUseCase,
    private val purgeCacheUseCase: PurgeCacheUseCase
) : BaseViewModel<RssReaderUiState, RssReaderUiAction, RssReaderUiEffect>(
    RssReaderUiState()
) {

    init {
        initializeFeedsAndObserve()
    }

    private fun initializeFeedsAndObserve() {
        viewModelScope.launch {
            // Ensure default Google Technology feed exists
            refreshFeedsUseCase.ensureDefaultFeed()
            // Initial background fetch
            refreshFeedsUseCase.refreshAll()
        }

        // Observe feeds
        observeFeedsUseCase()
            .onEach { feeds ->
                updateState { it.copy(feeds = feeds) }
            }
            .launchIn(viewModelScope)

        // Observe articles & compute filtered stream
        observeArticlesUseCase()
            .onEach { allItems ->
                val unread = allItems.count { !it.isRead }
                updateState { state ->
                    val filtered = applyFilters(
                        items = allItems,
                        selectedFeed = state.selectedFeed,
                        filter = state.filterType,
                        query = state.searchQuery
                    )
                    state.copy(
                        rawArticles = allItems,
                        feedItems = filtered,
                        unreadCount = unread
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun applyFilters(
        items: List<RssFeedItem>,
        selectedFeed: RssFeed?,
        filter: ArticleFilter,
        query: String
    ): List<RssFeedItem> {
        return items.asSequence()
            .filter { item ->
                if (selectedFeed == null) true else item.feedUrl == selectedFeed.url
            }
            .filter { item ->
                when (filter) {
                    ArticleFilter.ALL -> true
                    ArticleFilter.UNREAD -> !item.isRead
                    ArticleFilter.FAVORITES -> item.isFavorite
                }
            }
            .filter { item ->
                if (query.isBlank()) {
                    true
                } else {
                    item.title.contains(query, ignoreCase = true) ||
                        item.description?.contains(query, ignoreCase = true) == true ||
                        item.feedTitle?.contains(query, ignoreCase = true) == true
                }
            }
            .toList()
    }

    override fun onAction(action: RssReaderUiAction) {
        when (action) {
            is RssReaderUiAction.AddFeed -> addFeed(action.url, action.title)
            is RssReaderUiAction.UpdateFeed -> updateFeed(action.feed)
            is RssReaderUiAction.DeleteFeed -> deleteFeed(action.feedId)
            is RssReaderUiAction.ToggleFeedEnabled -> toggleFeedEnabled(action.feedId, action.isEnabled)
            is RssReaderUiAction.SelectFeed -> selectFeed(action.feed)
            is RssReaderUiAction.SetFilterType -> setFilterType(action.filter)
            is RssReaderUiAction.UpdateSearchQuery -> updateSearchQuery(action.query)
            is RssReaderUiAction.MarkItemAsRead -> markItemAsRead(action.itemId)
            is RssReaderUiAction.MarkFeedAsRead -> markFeedAsRead(action.feedUrl)
            is RssReaderUiAction.MarkAllAsRead -> markAllAsRead()
            is RssReaderUiAction.ToggleFavorite -> toggleFavorite(action.itemId)
            is RssReaderUiAction.UpdateMaxItemsPerFeed -> updateMaxItems(action.count)
            is RssReaderUiAction.UpdateRetentionDays -> updateRetention(action.days)
            is RssReaderUiAction.ClearReadArticles -> clearReadArticles()
            is RssReaderUiAction.PurgeAncientArticles -> purgeAncientArticles()
            is RssReaderUiAction.RefreshFeeds -> refreshAllFeeds()
            is RssReaderUiAction.RefreshCurrentFeed -> refreshCurrentFeed()
            is RssReaderUiAction.OpenAddSheet -> updateState { it.copy(showAddSheet = true) }
            is RssReaderUiAction.DismissAddSheet -> updateState { it.copy(showAddSheet = false) }
            is RssReaderUiAction.OpenSettings -> updateState { it.copy(showSettings = true) }
            is RssReaderUiAction.DismissSettings -> updateState { it.copy(showSettings = false) }
            is RssReaderUiAction.ClearError -> updateState { it.copy(error = null) }
        }
    }

    private fun addFeed(url: String, title: String?) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            addFeedUseCase(url, title, maxItemsToKeep = uiState.value.maxItemsPerFeed)
                .onSuccess {
                    sendEffect(RssReaderUiEffect.ShowToast("Subscribed to ${it.title ?: it.url}"))
                }
                .onFailure { e ->
                    updateState { it.copy(error = "Failed to add feed: ${e.message}") }
                }
            updateState { it.copy(isLoading = false) }
        }
    }

    private fun updateFeed(feed: RssFeed) {
        viewModelScope.launch {
            updateFeedUseCase(feed)
            sendEffect(RssReaderUiEffect.ShowToast("Feed updated"))
        }
    }

    private fun deleteFeed(feedId: String) {
        viewModelScope.launch {
            deleteFeedUseCase(feedId)
            sendEffect(RssReaderUiEffect.ShowToast("Feed removed"))
        }
    }

    private fun toggleFeedEnabled(feedId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val feed = uiState.value.feeds.find { it.id == feedId }
            if (feed != null) {
                updateFeedUseCase(feed.copy(isEnabled = isEnabled))
            }
        }
    }

    private fun selectFeed(feed: RssFeed?) {
        updateState { state ->
            val filtered = applyFilters(
                items = state.rawArticles,
                selectedFeed = feed,
                filter = state.filterType,
                query = state.searchQuery
            )
            state.copy(selectedFeed = feed, feedItems = filtered)
        }
    }

    private fun setFilterType(filter: ArticleFilter) {
        updateState { state ->
            val filtered = applyFilters(
                items = state.rawArticles,
                selectedFeed = state.selectedFeed,
                filter = filter,
                query = state.searchQuery
            )
            state.copy(filterType = filter, feedItems = filtered)
        }
    }

    private fun updateSearchQuery(query: String) {
        updateState { state ->
            val filtered = applyFilters(
                items = state.rawArticles,
                selectedFeed = state.selectedFeed,
                filter = state.filterType,
                query = query
            )
            state.copy(searchQuery = query, feedItems = filtered)
        }
    }

    private fun markItemAsRead(itemId: String) {
        viewModelScope.launch {
            markArticleReadUseCase.markOne(itemId)
        }
    }

    private fun markFeedAsRead(feedUrl: String) {
        viewModelScope.launch {
            markArticleReadUseCase.markFeed(feedUrl)
            sendEffect(RssReaderUiEffect.ShowToast("Feed marked as read"))
        }
    }

    private fun markAllAsRead() {
        viewModelScope.launch {
            markArticleReadUseCase.markAll()
            sendEffect(RssReaderUiEffect.ShowToast("All articles marked as read"))
        }
    }

    private fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            val item = uiState.value.rawArticles.find { it.id == itemId }
            if (item != null) {
                toggleFavoriteUseCase(itemId, !item.isFavorite)
            }
        }
    }

    private fun updateMaxItems(count: Int) {
        updateState { it.copy(maxItemsPerFeed = count) }
        viewModelScope.launch {
            uiState.value.feeds.forEach { feed ->
                updateFeedUseCase(feed.copy(maxItemsToKeep = count))
            }
            sendEffect(RssReaderUiEffect.ShowToast("Cache max limit set to $count"))
        }
    }

    private fun updateRetention(days: Int) {
        updateState { it.copy(retentionDays = days) }
        sendEffect(RssReaderUiEffect.ShowToast("Retention policy set to $days days"))
    }

    private fun clearReadArticles() {
        viewModelScope.launch {
            val deletedCount = purgeCacheUseCase.clearAllRead()
            sendEffect(RssReaderUiEffect.ShowToast("Cleared $deletedCount read articles"))
        }
    }

    private fun purgeAncientArticles() {
        viewModelScope.launch {
            val purgedCount = purgeCacheUseCase.purgeExpired(uiState.value.retentionDays)
            sendEffect(RssReaderUiEffect.ShowToast("Purged $purgedCount expired articles"))
        }
    }

    private fun refreshAllFeeds() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            val result = refreshFeedsUseCase.refreshAll()
            result.onSuccess {
                sendEffect(RssReaderUiEffect.ShowToast("Feeds refreshed"))
            }.onFailure { e ->
                updateState { it.copy(error = "Refresh failed: ${e.message}") }
            }
            updateState { it.copy(isLoading = false) }
        }
    }

    private fun refreshCurrentFeed() {
        val current = uiState.value.selectedFeed ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            refreshFeedsUseCase.refreshSingle(current.id)
                .onSuccess {
                    sendEffect(RssReaderUiEffect.ShowToast("${current.title ?: "Feed"} refreshed"))
                }
                .onFailure { e ->
                    updateState { it.copy(error = "Refresh failed: ${e.message}") }
                }
            updateState { it.copy(isLoading = false) }
        }
    }
}
