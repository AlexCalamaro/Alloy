package com.squidink.alloy.modules.rssreader.widget

import com.squidink.alloy.core.navigation.Screens
import com.squidink.alloy.modules.rssreader.db.RssFeedItemEntity
import com.squidink.alloy.modules.rssreader.ui.formatRelativeTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RssReaderGlanceWidgetTest {

    @Test
    fun testEntityToDomainMapping() {
        val entity = RssFeedItemEntity(
            id = "test-id-123",
            feedUrl = "https://example.com/rss.xml",
            feedTitle = "Tech News",
            title = "Android 15 Released",
            description = "All about the new features.",
            link = "https://example.com/article1",
            author = "Jane Doe",
            pubDate = 1700000000000L,
            imageUrl = "https://example.com/image.png",
            isRead = false,
            isFavorite = true,
            createdAt = 1700000000000L
        )

        val domain = entity.toDomain()

        assertEquals("test-id-123", domain.id)
        assertEquals("https://example.com/rss.xml", domain.feedUrl)
        assertEquals("Tech News", domain.feedTitle)
        assertEquals("Android 15 Released", domain.title)
        assertEquals("All about the new features.", domain.description)
        assertEquals("https://example.com/article1", domain.link)
        assertEquals("Jane Doe", domain.author)
        assertEquals(1700000000000L, domain.pubDate)
        assertEquals("https://example.com/image.png", domain.imageUrl)
        assertEquals(false, domain.isRead)
        assertEquals(true, domain.isFavorite)
    }

    @Test
    fun testFormatRelativeTimeJustNow() {
        val now = System.currentTimeMillis()
        assertEquals("Just now", formatRelativeTime(now - 10_000L)) // 10 seconds ago
    }

    @Test
    fun testFormatRelativeTimeMinutesAgo() {
        val now = System.currentTimeMillis()
        val fiveMinutesAgo = now - (5 * 60 * 1000L)
        assertEquals("5m ago", formatRelativeTime(fiveMinutesAgo))
    }

    @Test
    fun testFormatRelativeTimeHoursAgo() {
        val now = System.currentTimeMillis()
        val twoHoursAgo = now - (2 * 60 * 60 * 1000L)
        assertEquals("2h ago", formatRelativeTime(twoHoursAgo))
    }

    @Test
    fun testArticleLauncherConstants() {
        assertEquals("com.squidink.alloy.modules.rssreader.widget.ARTICLE_ID", ArticleLauncherActivity.EXTRA_ARTICLE_ID)
        assertEquals("com.squidink.alloy.modules.rssreader.widget.ARTICLE_URL", ArticleLauncherActivity.EXTRA_ARTICLE_URL)
    }

    @Test
    fun testNavigationTargetScreenConstant() {
        assertEquals("rss_reader", Screens.RssReader.route)
    }
}
