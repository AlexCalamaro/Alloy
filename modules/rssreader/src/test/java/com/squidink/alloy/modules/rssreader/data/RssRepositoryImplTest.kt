package com.squidink.alloy.modules.rssreader.data

import com.squidink.alloy.core.domain.common.model.RssFeed
import com.squidink.alloy.core.domain.common.model.RssFeedItem
import com.squidink.alloy.modules.rssreader.data.network.GoogleNewsUrlBuilder
import com.squidink.alloy.modules.rssreader.data.network.ParsedFeed
import com.squidink.alloy.modules.rssreader.data.network.ParsedFeedItem
import com.squidink.alloy.modules.rssreader.data.network.IRssHttpEngine
import com.squidink.alloy.modules.rssreader.db.RssFeedDao
import com.squidink.alloy.modules.rssreader.db.RssFeedSubscriptionEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RssRepositoryImplTest {

    private lateinit var mockDao: RssFeedDao
    private lateinit var mockHttpEngine: IRssHttpEngine
    private lateinit var repository: RssRepositoryImpl

    @Before
    fun setup() {
        mockDao = mock()
        mockHttpEngine = mock()
        repository = RssRepositoryImpl(mockDao, mockHttpEngine)
    }

    @Test
    fun `addFeedSubscription inserts entity into dao`() = runTest {
        val feed = createTestFeed()
        repository.addFeedSubscription(feed)
        verify(mockDao).insertFeedSubscription(any())
    }

    @Test
    fun `deleteFeedSubscription deletes by id and cleans up items`() = runTest {
        val feedId = "test-feed-id"
        whenever(mockDao.getFeedSubscriptionById(feedId)).thenReturn(
            RssFeedSubscriptionEntity(
                id = feedId,
                url = "https://example.com/feed",
                title = "Test",
                description = null,
                imageUrl = null,
                isEnabled = true
            )
        )

        repository.deleteFeedSubscription(feedId)

        verify(mockDao).deleteFeedItemsByFeedUrl("https://example.com/feed")
        verify(mockDao).deleteFeedSubscriptionById(feedId)
    }

    @Test
    fun `getUnreadFeedItemsCount returns correct count`() = runTest {
        whenever(mockDao.getUnreadFeedItemsCount()).thenReturn(5)
        val count = repository.getUnreadFeedItemsCount()
        assertEquals(5, count)
    }

    @Test
    fun `markItemAsRead calls dao update`() = runTest {
        val itemId = "test-item-id"
        repository.markItemAsRead(itemId)
        verify(mockDao).markItemAsRead(itemId)
    }

    @Test
    fun `markAllItemsAsRead calls dao markAllItemsAsRead`() = runTest {
        repository.markAllItemsAsRead()
        verify(mockDao).markAllItemsAsRead()
    }

    @Test
    fun `toggleFavorite calls dao with correct parameters`() = runTest {
        val itemId = "test-item-id"
        val isFavorite = true
        repository.toggleFavorite(itemId, isFavorite)
        verify(mockDao).toggleFavorite(itemId, isFavorite)
    }

    @Test
    fun `refreshFeed fetches from network and inserts items with retention pruning`() = runTest {
        val feedId = "feed-1"
        val feedUrl = "https://example.com/rss"
        whenever(mockDao.getFeedSubscriptionById(feedId)).thenReturn(
            RssFeedSubscriptionEntity(
                id = feedId,
                url = feedUrl,
                title = "My Feed",
                description = null,
                imageUrl = null,
                isEnabled = true,
                maxItemsToKeep = 50
            )
        )

        whenever(mockHttpEngine.fetchFeed(feedUrl)).thenReturn(
            ParsedFeed(
                title = "My Feed",
                description = "Desc",
                imageUrl = null,
                items = listOf(
                    ParsedFeedItem(
                        id = "item-1",
                        title = "Article 1",
                        description = "Snippet",
                        link = "https://example.com/1",
                        author = null,
                        pubDate = 1000L,
                        imageUrl = null
                    )
                )
            )
        )

        val result = repository.refreshFeed(feedId)
        assertTrue(result.isSuccess)

        verify(mockDao).insertFeedItemsIgnore(any())
        verify(mockDao).cleanupOldItems(feedUrl, 50)
        verify(mockDao).cleanupExpiredReadItems(any())
    }

    @Test
    fun `ensureDefaultFeed inserts Google Technology feed when database is empty`() = runTest {
        whenever(mockDao.getFeedSubscriptionByUrl(GoogleNewsUrlBuilder.DEFAULT_FEED_URL)).thenReturn(null)
        whenever(mockDao.getAllFeedSubscriptions()).thenReturn(flowOf(emptyList()))

        val defaultFeed = repository.ensureDefaultFeed()

        assertEquals(GoogleNewsUrlBuilder.DEFAULT_FEED_URL, defaultFeed.url)
        assertEquals(GoogleNewsUrlBuilder.DEFAULT_FEED_TITLE, defaultFeed.title)
        verify(mockDao).insertFeedSubscription(any())
    }

    private fun createTestFeed() = RssFeed(
        id = "test-id",
        url = "https://example.com/feed",
        title = "Test Feed",
        description = "Test Description",
        imageUrl = null,
        isEnabled = true,
        lastFetchedAt = 0,
        fetchIntervalMinutes = 60,
        maxItemsToKeep = 100
    )
}
