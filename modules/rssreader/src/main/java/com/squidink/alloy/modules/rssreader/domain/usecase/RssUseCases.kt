package com.squidink.alloy.modules.rssreader.domain.usecase

import com.squidink.alloy.core.domain.common.model.RssFeed
import com.squidink.alloy.core.domain.common.model.RssFeedItem
import com.squidink.alloy.core.domain.common.repository.IRssFeedRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class ObserveFeedsUseCase @Inject constructor(
    private val repository: IRssFeedRepository
) {
    operator fun invoke(): Flow<List<RssFeed>> = repository.getAllFeedSubscriptions()
}

class ObserveArticlesUseCase @Inject constructor(
    private val repository: IRssFeedRepository
) {
    operator fun invoke(): Flow<List<RssFeedItem>> = repository.getAllFeedItems()
}

class RefreshFeedsUseCase @Inject constructor(
    private val repository: IRssFeedRepository
) {
    suspend fun refreshAll(): Result<Unit> = repository.refreshAllFeeds()

    suspend fun refreshSingle(feedId: String): Result<Unit> = repository.refreshFeed(feedId)

    suspend fun ensureDefaultFeed(): RssFeed = repository.ensureDefaultFeed()
}

class AddFeedUseCase @Inject constructor(
    private val repository: IRssFeedRepository
) {
    suspend operator fun invoke(
        url: String,
        title: String?,
        description: String? = null,
        maxItemsToKeep: Int = 100
    ): Result<RssFeed> = runCatching {
        val trimmedUrl = url.trim()
        require(trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
            "URL must start with http:// or https://"
        }

        val existing = repository.getFeedSubscriptionByUrl(trimmedUrl)
        if (existing != null) {
            return@runCatching existing
        }

        val newFeed = RssFeed(
            id = UUID.randomUUID().toString(),
            url = trimmedUrl,
            title = title?.trim()?.ifEmpty { null },
            description = description,
            imageUrl = null,
            isEnabled = true,
            lastFetchedAt = 0,
            fetchIntervalMinutes = 60,
            maxItemsToKeep = maxItemsToKeep,
            createdAt = System.currentTimeMillis()
        )
        repository.addFeedSubscription(newFeed)
        repository.refreshFeed(newFeed.id)
        newFeed
    }
}

class UpdateFeedUseCase @Inject constructor(
    private val repository: IRssFeedRepository
) {
    suspend operator fun invoke(feed: RssFeed) {
        repository.updateFeedSubscription(feed)
    }
}

class DeleteFeedUseCase @Inject constructor(
    private val repository: IRssFeedRepository
) {
    suspend operator fun invoke(feedId: String) {
        repository.deleteFeedSubscription(feedId)
    }
}

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: IRssFeedRepository
) {
    suspend operator fun invoke(itemId: String, isFavorite: Boolean) {
        repository.toggleFavorite(itemId, isFavorite)
    }
}

class MarkArticleReadUseCase @Inject constructor(
    private val repository: IRssFeedRepository
) {
    suspend fun markOne(itemId: String) {
        repository.markItemAsRead(itemId)
    }

    suspend fun markFeed(feedUrl: String) {
        repository.markFeedItemsAsRead(feedUrl)
    }

    suspend fun markAll() {
        repository.markAllItemsAsRead()
    }
}

class PurgeCacheUseCase @Inject constructor(
    private val repository: IRssFeedRepository
) {
    suspend fun clearAllRead(): Int = repository.deleteReadFeedItems()

    suspend fun purgeExpired(days: Int): Int = repository.cleanupExpiredReadItems(days)
}
