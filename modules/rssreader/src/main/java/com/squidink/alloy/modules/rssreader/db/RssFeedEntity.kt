package com.squidink.alloy.modules.rssreader.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity for RSS feed items.
 * Stores individual articles from RSS feeds with their metadata.
 */
@Entity(
    tableName = "rss_feed_items",
    indices = [
        Index(value = ["feedUrl"], name = "idx_feed_url"),
        Index(value = ["pubDate"], name = "idx_pub_date"),
        Index(value = ["isRead"], name = "idx_is_read")
    ]
)
data class RssFeedItemEntity(
    @PrimaryKey
    val id: String,

    val feedUrl: String,
    val feedTitle: String?,
    val title: String,
    val description: String?,
    val link: String?,
    val author: String?,
    val pubDate: Long,
    val imageUrl: String?,
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Database entity for RSS feed subscriptions.
 * Stores user-subscribed RSS feeds with their configuration.
 */
@Entity(
    tableName = "rss_feeds",
    indices = [
        Index(value = ["url"], name = "idx_feed_url_unique", unique = true)
    ]
)
data class RssFeedSubscriptionEntity(
    @PrimaryKey
    val id: String,

    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val isEnabled: Boolean = true,
    val lastFetchedAt: Long = 0,
    val fetchIntervalMinutes: Int = 60,
    val maxItemsToKeep: Int = 100,
    val createdAt: Long = System.currentTimeMillis()
)
