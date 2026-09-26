package com.squidink.alloy.core.domain.repository

/**
 * Data class for battery information.
 * This is the domain model independent of Android framework specifics.
 */
data class BatteryInfo(
    val level: Int = 0,
    val scale: Int = 100,
    val percentage: Int = 0,
    val health: Int = 0,
    val status: Int = 0,
    val temperature: Int = 0, // tenths of a degree Celsius
    val voltage: Int = 0, // millivolts
    val isCharging: Boolean = false,
) {
    fun getTemperatureCelsius(): Float = temperature / 10f
}

/**
 * Data class for network statistics.
 * This is the domain model independent of implementation details.
 */
data class NetStats(
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxBytesPerSecond: Float = 0f,
    val txBytesPerSecond: Float = 0f,
)

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
