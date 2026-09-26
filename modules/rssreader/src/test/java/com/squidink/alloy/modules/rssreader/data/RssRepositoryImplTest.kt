package com.squidink.alloy.modules.rssreader.data

import com.squidink.alloy.modules.rssreader.db.RssFeedDao
import com.squidink.alloy.modules.rssreader.db.RssFeedItemEntity
import com.squidink.alloy.modules.rssreader.db.RssFeedSubscriptionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class RssRepositoryImplTest {

    private lateinit var mockDao: RssFeedDao
    private lateinit var repository: RssRepositoryImpl

    @Before
    fun setup() {
        mockDao = mock()
        repository = RssRepositoryImpl(mockDao)
    }

    @Test
    fun `addFeedSubscription inserts entity into dao`() = runTest {
        val feed = createTestFeed()
        
        repository.addFeedSubscription(feed)
        
        verify(mockDao).insertFeedSubscription(any())
    }

    @Test
    fun `deleteFeedSubscription deletes by id`() = runTest {
        val feedId = "test-feed-id"
        
        repository.deleteFeedSubscription(feedId)
        
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
    fun `toggleFavorite calls dao with correct parameters`() = runTest {
        val itemId = "test-item-id"
        val isFavorite = true
        
        repository.toggleFavorite(itemId, isFavorite)
        
        verify(mockDao).toggleFavorite(itemId, isFavorite)
    }

    @Test
    fun `addFeedItems inserts multiple items`() = runTest {
        val items = listOf(
            createTestItem("1"),
            createTestItem("2"),
            createTestItem("3")
        )
        
        repository.addFeedItems(items)
        
        verify(mockDao).insertFeedItems(any())
    }

    @Test
    fun `cleanupOldItems calls dao with correct parameters`() = runTest {
        val feedUrl = "https://example.com/feed"
        val keepCount = 50
        
        repository.cleanupOldItems(feedUrl, keepCount)
        
        verify(mockDao).cleanupOldItems(feedUrl, keepCount)
    }

    @Test
    fun `deleteAllFeedItems clears the database`() = runTest {
        repository.deleteAllFeedItems()
        
        verify(mockDao).deleteAllFeedItems()
    }

    @Test
    fun `deleteAllFeedSubscriptions clears all subscriptions`() = runTest {
        repository.deleteAllFeedSubscriptions()
        
        verify(mockDao).deleteAllFeedSubscriptions()
    }

    // Helper functions
    private fun createTestFeed() = com.squidink.alloy.core.domain.repository.RssFeed(
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

    private fun createTestItem(id: String) = com.squidink.alloy.core.domain.repository.RssFeedItem(
        id = id,
        feedUrl = "https://example.com/feed",
        feedTitle = "Test Feed",
        title = "Test Item $id",
        description = "Description $id",
        link = "https://example.com/item/$id",
        author = "Test Author",
        pubDate = System.currentTimeMillis(),
        imageUrl = null,
        isRead = false,
        isFavorite = false
    )
}
