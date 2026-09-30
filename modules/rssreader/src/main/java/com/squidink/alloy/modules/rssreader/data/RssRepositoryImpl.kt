package com.squidink.alloy.modules.rssreader.data

import com.squidink.alloy.core.domain.common.model.RssFeed
import com.squidink.alloy.core.domain.common.model.RssFeedItem
import com.squidink.alloy.core.domain.common.repository.IRssFeedRepository
import com.squidink.alloy.modules.rssreader.data.network.GoogleNewsUrlBuilder
import com.squidink.alloy.modules.rssreader.data.network.IRssHttpEngine
import com.squidink.alloy.modules.rssreader.db.RssFeedDao
import com.squidink.alloy.modules.rssreader.db.RssFeedItemEntity
import com.squidink.alloy.modules.rssreader.db.RssFeedSubscriptionEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the RSS feed repository.
 * Coordinates local Room database caching and remote OkHttp network fetching
 * with intelligent retention and cache pruning policies.
 */
@Singleton
class RssRepositoryImpl @Inject constructor(
    private val rssFeedDao: RssFeedDao,
    private val rssHttpEngine: IRssHttpEngine
) : IRssFeedRepository {

    // ==================== Feed Subscriptions ====================

    override suspend fun addFeedSubscription(feed: RssFeed) {
        val entity = feed.toEntity()
        rssFeedDao.insertFeedSubscription(entity)
    }

    override suspend fun updateFeedSubscription(feed: RssFeed) {
        val entity = feed.toEntity()
        rssFeedDao.updateFeedSubscription(entity)
    }

    override suspend fun deleteFeedSubscription(feedId: String) {
        val feed = rssFeedDao.getFeedSubscriptionById(feedId)
        if (feed != null) {
            rssFeedDao.deleteFeedItemsByFeedUrl(feed.url)
            rssFeedDao.deleteFeedSubscriptionById(feedId)
        }
    }

    override suspend fun getFeedSubscriptionById(feedId: String): RssFeed? {
        return rssFeedDao.getFeedSubscriptionById(feedId)?.toDomain()
    }

    override suspend fun getFeedSubscriptionByUrl(url: String): RssFeed? {
        return rssFeedDao.getFeedSubscriptionByUrl(url)?.toDomain()
    }

    override fun getAllFeedSubscriptions(): Flow<List<RssFeed>> {
        return rssFeedDao.getAllFeedSubscriptions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEnabledFeedSubscriptions(): Flow<List<RssFeed>> {
        return rssFeedDao.getEnabledFeedSubscriptions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEnabledFeedSubscriptionsList(): List<RssFeed> {
        return rssFeedDao.getEnabledFeedSubscriptionsList().map { it.toDomain() }
    }

    // ==================== Feed Items ====================

    override suspend fun addFeedItem(item: RssFeedItem) {
        val entity = item.toEntity()
        rssFeedDao.insertFeedItem(entity)
    }

    override suspend fun addFeedItems(items: List<RssFeedItem>) {
        val entities = items.map { it.toEntity() }
        rssFeedDao.insertFeedItems(entities)
    }

    override suspend fun updateFeedItem(item: RssFeedItem) {
        val entity = item.toEntity()
        rssFeedDao.updateFeedItem(entity)
    }

    override suspend fun deleteFeedItem(itemId: String) {
        rssFeedDao.deleteFeedItemById(itemId)
    }

    override suspend fun getFeedItemById(itemId: String): RssFeedItem? {
        return rssFeedDao.getFeedItemById(itemId)?.toDomain()
    }

    override suspend fun getFeedItemsByFeedUrl(feedUrl: String): List<RssFeedItem> {
        return rssFeedDao.getFeedItemsByFeedUrl(feedUrl).map { it.toDomain() }
    }

    override suspend fun getFeedItemsByFeedUrlLimited(feedUrl: String, limit: Int): List<RssFeedItem> {
        return rssFeedDao.getFeedItemsByFeedUrlLimited(feedUrl, limit).map { it.toDomain() }
    }

    override fun getAllFeedItems(): Flow<List<RssFeedItem>> {
        return rssFeedDao.getAllFeedItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getUnreadFeedItems(): Flow<List<RssFeedItem>> {
        return rssFeedDao.getUnreadFeedItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteFeedItems(): Flow<List<RssFeedItem>> {
        return rssFeedDao.getFavoriteFeedItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentFeedItemsLimited(limit: Int): Flow<List<RssFeedItem>> {
        return rssFeedDao.getRecentFeedItemsLimited(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getUnreadFeedItemsCount(): Int {
        return rssFeedDao.getUnreadFeedItemsCount()
    }

    override suspend fun markItemAsRead(itemId: String) {
        rssFeedDao.markItemAsRead(itemId)
    }

    override suspend fun markFeedItemsAsRead(feedUrl: String) {
        rssFeedDao.markFeedItemsAsRead(feedUrl)
    }

    override suspend fun markAllItemsAsRead() {
        rssFeedDao.markAllItemsAsRead()
    }

    override suspend fun toggleFavorite(itemId: String, isFavorite: Boolean) {
        rssFeedDao.toggleFavorite(itemId, isFavorite)
    }

    override suspend fun deleteFeedItemsByFeedUrl(feedUrl: String) {
        rssFeedDao.deleteFeedItemsByFeedUrl(feedUrl)
    }

    override suspend fun cleanupOldItems(feedUrl: String, keepCount: Int) {
        rssFeedDao.cleanupOldItems(feedUrl, keepCount)
    }

    override suspend fun deleteAllFeedItems() {
        rssFeedDao.deleteAllFeedItems()
    }

    override suspend fun deleteAllFeedSubscriptions() {
        rssFeedDao.deleteAllFeedSubscriptions()
    }

    // ==================== Network Sync & Retention Operations ====================

    override suspend fun refreshFeed(feedId: String): Result<Unit> {
        return runCatching {
            val feed = rssFeedDao.getFeedSubscriptionById(feedId)
                ?: throw IllegalArgumentException("Feed $feedId not found")

            val parsed = rssHttpEngine.fetchFeed(feed.url)

            // Update feed title/description if not set
            val resolvedTitle = feed.title ?: parsed.title ?: "RSS Feed"
            val resolvedImage = feed.imageUrl ?: parsed.imageUrl
            if (feed.title == null || feed.imageUrl == null) {
                rssFeedDao.updateFeedSubscription(
                    feed.copy(
                        title = resolvedTitle,
                        imageUrl = resolvedImage,
                        lastFetchedAt = System.currentTimeMillis()
                    )
                )
            } else {
                rssFeedDao.updateFeedSubscription(
                    feed.copy(lastFetchedAt = System.currentTimeMillis())
                )
            }

            // Map and insert items with IGNORE strategy to preserve existing user read/favorite state
            val entities = parsed.items.map { item ->
                RssFeedItemEntity(
                    id = item.id,
                    feedUrl = feed.url,
                    feedTitle = resolvedTitle,
                    title = item.title,
                    description = item.description,
                    link = item.link,
                    author = item.author,
                    pubDate = item.pubDate,
                    imageUrl = item.imageUrl,
                    isRead = false,
                    isFavorite = false,
                    createdAt = System.currentTimeMillis()
                )
            }

            if (entities.isNotEmpty()) {
                rssFeedDao.insertFeedItemsIgnore(entities)
            }

            // Intelligent cache pruning:
            // 1. Keep only maxItemsToKeep recent items for this feed (ignoring favorites)
            rssFeedDao.cleanupOldItems(feed.url, feed.maxItemsToKeep)

            // 2. Prune read items older than 14 days (ignoring favorites)
            val fourteenDaysAgo = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
            rssFeedDao.cleanupExpiredReadItems(fourteenDaysAgo)
        }
    }

    override suspend fun refreshAllFeeds(): Result<Unit> = coroutineScope {
        runCatching {
            var feeds = rssFeedDao.getEnabledFeedSubscriptionsList()
            if (feeds.isEmpty()) {
                ensureDefaultFeed()
                feeds = rssFeedDao.getEnabledFeedSubscriptionsList()
            }

            val deferredResults = feeds.map { feed ->
                async { refreshFeed(feed.id) }
            }
            deferredResults.awaitAll()
            Unit
        }
    }

    override suspend fun cleanupExpiredReadItems(cutoffDays: Int): Int {
        val cutoffMs = System.currentTimeMillis() - (cutoffDays.toLong() * 24 * 60 * 60 * 1000)
        return rssFeedDao.cleanupExpiredReadItems(cutoffMs)
    }

    override suspend fun deleteReadFeedItems(): Int {
        return rssFeedDao.deleteReadFeedItems()
    }

    override suspend fun ensureDefaultFeed(): RssFeed {
        val existing = rssFeedDao.getFeedSubscriptionByUrl(GoogleNewsUrlBuilder.DEFAULT_FEED_URL)
        if (existing != null) {
            return existing.toDomain()
        }

        // Check if there are any feeds at all
        val allFeeds = rssFeedDao.getAllFeedSubscriptions().firstOrNull().orEmpty()
        if (allFeeds.isNotEmpty()) {
            return allFeeds.first().toDomain()
        }

        val defaultSubscription = RssFeed(
            id = UUID.randomUUID().toString(),
            url = GoogleNewsUrlBuilder.DEFAULT_FEED_URL,
            title = GoogleNewsUrlBuilder.DEFAULT_FEED_TITLE,
            description = "Google News Technology Feed",
            imageUrl = null,
            isEnabled = true,
            lastFetchedAt = 0,
            fetchIntervalMinutes = 60,
            maxItemsToKeep = 100,
            createdAt = System.currentTimeMillis()
        )
        addFeedSubscription(defaultSubscription)
        return defaultSubscription
    }
}

// ==================== Entity to Domain Mappers ====================

private fun RssFeedSubscriptionEntity.toDomain(): RssFeed {
    return RssFeed(
        id = id,
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        isEnabled = isEnabled,
        lastFetchedAt = lastFetchedAt,
        fetchIntervalMinutes = fetchIntervalMinutes,
        maxItemsToKeep = maxItemsToKeep,
        createdAt = createdAt
    )
}

private fun RssFeed.toEntity(): RssFeedSubscriptionEntity {
    return RssFeedSubscriptionEntity(
        id = id,
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        isEnabled = isEnabled,
        lastFetchedAt = lastFetchedAt,
        fetchIntervalMinutes = fetchIntervalMinutes,
        maxItemsToKeep = maxItemsToKeep,
        createdAt = createdAt
    )
}

private fun RssFeedItemEntity.toDomain(): RssFeedItem {
    return RssFeedItem(
        id = id,
        feedUrl = feedUrl,
        feedTitle = feedTitle,
        title = title,
        description = description,
        link = link,
        author = author,
        pubDate = pubDate,
        imageUrl = imageUrl,
        isRead = isRead,
        isFavorite = isFavorite,
        createdAt = createdAt
    )
}

private fun RssFeedItem.toEntity(): RssFeedItemEntity {
    return RssFeedItemEntity(
        id = id,
        feedUrl = feedUrl,
        feedTitle = feedTitle,
        title = title,
        description = description,
        link = link,
        author = author,
        pubDate = pubDate,
        imageUrl = imageUrl,
        isRead = isRead,
        isFavorite = isFavorite,
        createdAt = createdAt
    )
}
