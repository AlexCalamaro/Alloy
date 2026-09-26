package com.squidink.alloy.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for RSS feed operations.
 *
 * Defines the contract for RSS feed data access without exposing
 * implementation details (Room DAO, network clients, etc.)
 */
interface IRssFeedRepository {

    // ==================== Feed Subscriptions ====================

    /**
     * Add a new feed subscription.
     *
     * @param feed The feed to add
     */
    suspend fun addFeedSubscription(feed: RssFeed)

    /**
     * Update an existing feed subscription.
     *
     * @param feed The feed with updated data
     */
    suspend fun updateFeedSubscription(feed: RssFeed)

    /**
     * Delete a feed subscription by ID.
     *
     * @param feedId The feed ID to delete
     */
    suspend fun deleteFeedSubscription(feedId: String)

    /**
     * Get a specific feed subscription by ID.
     *
     * @param feedId The feed ID
     * @return The feed or null if not found
     */
    suspend fun getFeedSubscriptionById(feedId: String): RssFeed?

    /**
     * Get a feed subscription by URL.
     *
     * @param url The feed URL
     * @return The feed or null if not found
     */
    suspend fun getFeedSubscriptionByUrl(url: String): RssFeed?

    /**
     * Observe all feed subscriptions as a Flow.
     *
     * @return Flow emitting the current list of feeds and updates
     */
    fun getAllFeedSubscriptions(): Flow<List<RssFeed>>

    /**
     * Observe only enabled feed subscriptions as a Flow.
     *
     * @return Flow emitting enabled feeds sorted by last fetched
     */
    fun getEnabledFeedSubscriptions(): Flow<List<RssFeed>>

    /**
     * Get a list of enabled feed subscriptions.
     *
     * @return List of enabled feeds
     */
    suspend fun getEnabledFeedSubscriptionsList(): List<RssFeed>

    // ==================== Feed Items ====================

    /**
     * Add a new feed item.
     *
     * @param item The item to add
     */
    suspend fun addFeedItem(item: RssFeedItem)

    /**
     * Add multiple feed items.
     *
     * @param items The list of items to add
     */
    suspend fun addFeedItems(items: List<RssFeedItem>)

    /**
     * Update an existing feed item.
     *
     * @param item The item with updated data
     */
    suspend fun updateFeedItem(item: RssFeedItem)

    /**
     * Delete a feed item by ID.
     *
     * @param itemId The item ID to delete
     */
    suspend fun deleteFeedItem(itemId: String)

    /**
     * Get a specific feed item by ID.
     *
     * @param itemId The item ID
     * @return The item or null if not found
     */
    suspend fun getFeedItemById(itemId: String): RssFeedItem?

    /**
     * Get all feed items for a specific feed URL.
     *
     * @param feedUrl The feed URL
     * @return List of items sorted by publication date (newest first)
     */
    suspend fun getFeedItemsByFeedUrl(feedUrl: String): List<RssFeedItem>

    /**
     * Get feed items for a specific feed URL with a limit.
     *
     * @param feedUrl The feed URL
     * @param limit Maximum number of items to return
     * @return List of items sorted by publication date (newest first)
     */
    suspend fun getFeedItemsByFeedUrlLimited(feedUrl: String, limit: Int): List<RssFeedItem>

    /**
     * Observe all feed items as a Flow.
     *
     * @return Flow emitting the current list of items and updates
     */
    fun getAllFeedItems(): Flow<List<RssFeedItem>>

    /**
     * Observe only unread feed items as a Flow.
     *
     * @return Flow emitting unread items sorted by publication date
     */
    fun getUnreadFeedItems(): Flow<List<RssFeedItem>>

    /**
     * Observe favorite feed items as a Flow.
     *
     * @return Flow emitting favorite items sorted by publication date
     */
    fun getFavoriteFeedItems(): Flow<List<RssFeedItem>>

    /**
     * Observe recent feed items with a limit as a Flow.
     *
     * @param limit Maximum number of items to emit
     * @return Flow emitting recent items sorted by publication date
     */
    fun getRecentFeedItemsLimited(limit: Int): Flow<List<RssFeedItem>>

    /**
     * Get the count of unread feed items.
     *
     * @return Number of unread items
     */
    suspend fun getUnreadFeedItemsCount(): Int

    /**
     * Mark a feed item as read.
     *
     * @param itemId The item ID to mark as read
     */
    suspend fun markItemAsRead(itemId: String)

    /**
     * Mark all items for a feed as read.
     *
     * @param feedUrl The feed URL
     */
    suspend fun markFeedItemsAsRead(feedUrl: String)

    /**
     * Toggle favorite status for a feed item.
     *
     * @param itemId The item ID
     * @param isFavorite The favorite status to set
     */
    suspend fun toggleFavorite(itemId: String, isFavorite: Boolean)

    /**
     * Delete all feed items for a specific feed URL.
     *
     * @param feedUrl The feed URL
     */
    suspend fun deleteFeedItemsByFeedUrl(feedUrl: String)

    /**
     * Clean up old items for a feed, keeping only the most recent.
     *
     * @param feedUrl The feed URL
     * @param keepCount Number of items to keep
     */
    suspend fun cleanupOldItems(feedUrl: String, keepCount: Int)

    /**
     * Delete all feed items.
     */
    suspend fun deleteAllFeedItems()

    /**
     * Delete all feed subscriptions.
     */
    suspend fun deleteAllFeedSubscriptions()
}
