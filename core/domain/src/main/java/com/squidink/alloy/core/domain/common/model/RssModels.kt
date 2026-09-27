package com.squidink.alloy.core.domain.common.model

/**
 * Domain model for an RSS feed subscription.
 * Represents a feed that the user has subscribed to.
 */
data class RssFeed(
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

/**
 * Domain model for an RSS feed item (article).
 * Represents a single article from an RSS feed.
 */
data class RssFeedItem(
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
