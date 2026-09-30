package com.squidink.alloy.modules.rssreader.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for RSS feed operations.
 * Provides CRUD operations for both feed subscriptions and feed items.
 */
@Dao
interface RssFeedDao {

    // ==================== Feed Subscriptions ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedSubscription(entity: RssFeedSubscriptionEntity)

    @Update
    suspend fun updateFeedSubscription(entity: RssFeedSubscriptionEntity)

    @Delete
    suspend fun deleteFeedSubscription(entity: RssFeedSubscriptionEntity)

    @Query("DELETE FROM rss_feeds WHERE id = :feedId")
    suspend fun deleteFeedSubscriptionById(feedId: String)

    @Query("SELECT * FROM rss_feeds WHERE id = :feedId")
    suspend fun getFeedSubscriptionById(feedId: String): RssFeedSubscriptionEntity?

    @Query("SELECT * FROM rss_feeds WHERE url = :url")
    suspend fun getFeedSubscriptionByUrl(url: String): RssFeedSubscriptionEntity?

    @Query("SELECT * FROM rss_feeds ORDER BY title ASC")
    fun getAllFeedSubscriptions(): Flow<List<RssFeedSubscriptionEntity>>

    @Query("SELECT * FROM rss_feeds WHERE isEnabled = 1 ORDER BY lastFetchedAt ASC")
    fun getEnabledFeedSubscriptions(): Flow<List<RssFeedSubscriptionEntity>>

    @Query("SELECT * FROM rss_feeds WHERE isEnabled = 1")
    suspend fun getEnabledFeedSubscriptionsList(): List<RssFeedSubscriptionEntity>

    // ==================== Feed Items ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedItem(entity: RssFeedItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedItems(entities: List<RssFeedItemEntity>)

    @Update
    suspend fun updateFeedItem(entity: RssFeedItemEntity)

    @Delete
    suspend fun deleteFeedItem(entity: RssFeedItemEntity)

    @Query("DELETE FROM rss_feed_items WHERE id = :itemId")
    suspend fun deleteFeedItemById(itemId: String)

    @Query("SELECT * FROM rss_feed_items WHERE id = :itemId")
    suspend fun getFeedItemById(itemId: String): RssFeedItemEntity?

    @Query("SELECT * FROM rss_feed_items WHERE feedUrl = :feedUrl ORDER BY pubDate DESC")
    suspend fun getFeedItemsByFeedUrl(feedUrl: String): List<RssFeedItemEntity>

    @Query("SELECT * FROM rss_feed_items WHERE feedUrl = :feedUrl ORDER BY pubDate DESC LIMIT :limit")
    suspend fun getFeedItemsByFeedUrlLimited(feedUrl: String, limit: Int): List<RssFeedItemEntity>

    @Query("SELECT * FROM rss_feed_items ORDER BY pubDate DESC")
    fun getAllFeedItems(): Flow<List<RssFeedItemEntity>>

    @Query("SELECT * FROM rss_feed_items WHERE isRead = 0 ORDER BY pubDate DESC")
    fun getUnreadFeedItems(): Flow<List<RssFeedItemEntity>>

    @Query("SELECT * FROM rss_feed_items WHERE isFavorite = 1 ORDER BY pubDate DESC")
    fun getFavoriteFeedItems(): Flow<List<RssFeedItemEntity>>

    @Query("SELECT * FROM rss_feed_items ORDER BY pubDate DESC LIMIT :limit")
    fun getRecentFeedItemsLimited(limit: Int): Flow<List<RssFeedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFeedItemsIgnore(entities: List<RssFeedItemEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM rss_feed_items WHERE isRead = 0")
    suspend fun getUnreadFeedItemsCount(): Int

    @Query("UPDATE rss_feed_items SET isRead = 1 WHERE id = :itemId")
    suspend fun markItemAsRead(itemId: String)

    @Query("UPDATE rss_feed_items SET isRead = 1 WHERE feedUrl = :feedUrl")
    suspend fun markFeedItemsAsRead(feedUrl: String)

    @Query("UPDATE rss_feed_items SET isRead = 1")
    suspend fun markAllItemsAsRead()

    @Query("UPDATE rss_feed_items SET isFavorite = :isFavorite WHERE id = :itemId")
    suspend fun toggleFavorite(itemId: String, isFavorite: Boolean)

    @Query("DELETE FROM rss_feed_items WHERE feedUrl = :feedUrl")
    suspend fun deleteFeedItemsByFeedUrl(feedUrl: String)

    @Query("DELETE FROM rss_feed_items WHERE id IN (:itemIds)")
    suspend fun deleteFeedItemsByIds(itemIds: List<String>)

    // ==================== Intelligent Retention & Cache Queries ====================

    @Query("DELETE FROM rss_feed_items WHERE feedUrl = :feedUrl AND isFavorite = 0 AND id NOT IN (SELECT id FROM rss_feed_items WHERE feedUrl = :feedUrl ORDER BY pubDate DESC LIMIT :keepCount)")
    suspend fun cleanupOldItems(feedUrl: String, keepCount: Int)

    @Query("DELETE FROM rss_feed_items WHERE isRead = 1 AND isFavorite = 0 AND pubDate < :cutoffTimestamp")
    suspend fun cleanupExpiredReadItems(cutoffTimestamp: Long): Int

    @Query("DELETE FROM rss_feed_items WHERE isRead = 1 AND isFavorite = 0")
    suspend fun deleteReadFeedItems(): Int

    @Query("SELECT COUNT(*) FROM rss_feed_items")
    suspend fun getTotalArticlesCount(): Int

    @Query("SELECT COUNT(*) FROM rss_feed_items WHERE isFavorite = 1")
    suspend fun getFavoriteArticlesCount(): Int

    @Query("DELETE FROM rss_feed_items")
    suspend fun deleteAllFeedItems()

    @Query("DELETE FROM rss_feeds")
    suspend fun deleteAllFeedSubscriptions()
}
