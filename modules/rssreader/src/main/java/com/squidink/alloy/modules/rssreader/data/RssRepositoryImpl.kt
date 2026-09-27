package com.squidink.alloy.modules.rssreader.data

import com.squidink.alloy.core.domain.common.repository.IRssFeedRepository
import com.squidink.alloy.core.domain.common.model.RssFeed
import com.squidink.alloy.core.domain.common.model.RssFeedItem
import com.squidink.alloy.modules.rssreader.db.RssFeedDao
import com.squidink.alloy.modules.rssreader.db.RssFeedItemEntity
import com.squidink.alloy.modules.rssreader.db.RssFeedSubscriptionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of the RSS feed repository.
 * Bridges the database layer with the domain layer.
 */
class RssRepositoryImpl @Inject constructor(
    private val rssFeedDao: RssFeedDao
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
        rssFeedDao.deleteFeedSubscriptionById(feedId)
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
