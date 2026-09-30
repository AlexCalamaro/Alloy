package com.squidink.alloy.modules.rssreader

import com.squidink.alloy.core.domain.common.model.RssFeed
import com.squidink.alloy.core.domain.common.model.RssFeedItem
import com.squidink.alloy.core.domain.common.repository.IRssFeedRepository
import com.squidink.alloy.modules.rssreader.domain.usecase.AddFeedUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.DeleteFeedUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.MarkArticleReadUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.ObserveArticlesUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.ObserveFeedsUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.PurgeCacheUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.RefreshFeedsUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.ToggleFavoriteUseCase
import com.squidink.alloy.modules.rssreader.domain.usecase.UpdateFeedUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class RssReaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: IRssFeedRepository

    private val feedsFlow = MutableStateFlow<List<RssFeed>>(emptyList())
    private val articlesFlow = MutableStateFlow<List<RssFeedItem>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock()

        whenever(mockRepository.getAllFeedSubscriptions()).thenReturn(feedsFlow)
        whenever(mockRepository.getAllFeedItems()).thenReturn(articlesFlow)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RssReaderViewModel {
        return RssReaderViewModel(
            observeFeedsUseCase = ObserveFeedsUseCase(mockRepository),
            observeArticlesUseCase = ObserveArticlesUseCase(mockRepository),
            refreshFeedsUseCase = RefreshFeedsUseCase(mockRepository),
            addFeedUseCase = AddFeedUseCase(mockRepository),
            updateFeedUseCase = UpdateFeedUseCase(mockRepository),
            deleteFeedUseCase = DeleteFeedUseCase(mockRepository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(mockRepository),
            markArticleReadUseCase = MarkArticleReadUseCase(mockRepository),
            purgeCacheUseCase = PurgeCacheUseCase(mockRepository)
        )
    }

    @Test
    fun `initial state collects feeds and articles and computes unread count`() = runTest {
        val testFeed = RssFeed(
            id = "1",
            url = "https://example.com/rss",
            title = "Test Feed",
            description = null,
            imageUrl = null
        )
        val testArticles = listOf(
            RssFeedItem(
                id = "a1",
                feedUrl = "https://example.com/rss",
                feedTitle = "Test Feed",
                title = "Android 15 Released",
                description = "Details about Android 15",
                link = "https://example.com/1",
                author = null,
                pubDate = 1000L,
                imageUrl = null,
                isRead = false,
                isFavorite = false
            ),
            RssFeedItem(
                id = "a2",
                feedUrl = "https://example.com/rss",
                feedTitle = "Test Feed",
                title = "Kotlin Multiplatform",
                description = "KMP updates",
                link = "https://example.com/2",
                author = null,
                pubDate = 2000L,
                imageUrl = null,
                isRead = true,
                isFavorite = true
            )
        )

        feedsFlow.value = listOf(testFeed)
        articlesFlow.value = testArticles

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.feeds.size)
        assertEquals(2, viewModel.uiState.value.feedItems.size)
        assertEquals(1, viewModel.uiState.value.unreadCount)
    }

    @Test
    fun `search query filters feed items`() = runTest {
        val testArticles = listOf(
            RssFeedItem(
                id = "a1",
                feedUrl = "https://example.com/rss",
                feedTitle = "Feed",
                title = "Android 15",
                description = "OS update",
                link = null,
                author = null,
                pubDate = 1000L,
                imageUrl = null
            ),
            RssFeedItem(
                id = "a2",
                feedUrl = "https://example.com/rss",
                feedTitle = "Feed",
                title = "SpaceX Launch",
                description = "Rocket launch",
                link = null,
                author = null,
                pubDate = 2000L,
                imageUrl = null
            )
        )
        articlesFlow.value = testArticles

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(RssReaderUiAction.UpdateSearchQuery("Android"))
        assertEquals(1, viewModel.uiState.value.feedItems.size)
        assertEquals("Android 15", viewModel.uiState.value.feedItems.first().title)
    }

    @Test
    fun `filter type UNREAD filters only unread articles`() = runTest {
        val testArticles = listOf(
            RssFeedItem(
                id = "a1",
                feedUrl = "https://example.com/rss",
                feedTitle = "Feed",
                title = "Unread Article",
                description = null,
                link = null,
                author = null,
                pubDate = 1000L,
                imageUrl = null,
                isRead = false
            ),
            RssFeedItem(
                id = "a2",
                feedUrl = "https://example.com/rss",
                feedTitle = "Feed",
                title = "Read Article",
                description = null,
                link = null,
                author = null,
                pubDate = 2000L,
                imageUrl = null,
                isRead = true
            )
        )
        articlesFlow.value = testArticles

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(RssReaderUiAction.SetFilterType(ArticleFilter.UNREAD))
        assertEquals(1, viewModel.uiState.value.feedItems.size)
        assertEquals("Unread Article", viewModel.uiState.value.feedItems.first().title)
    }

    @Test
    fun `markItemAsRead delegates to markArticleReadUseCase`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(RssReaderUiAction.MarkItemAsRead("item-123"))
        advanceUntilIdle()

        verify(mockRepository).markItemAsRead("item-123")
    }

    @Test
    fun `toggleFavorite delegates to toggleFavoriteUseCase`() = runTest {
        val testItem = RssFeedItem(
            id = "item-123",
            feedUrl = "https://example.com/rss",
            feedTitle = "Feed",
            title = "Article",
            description = null,
            link = null,
            author = null,
            pubDate = 1000L,
            imageUrl = null,
            isFavorite = false
        )
        articlesFlow.value = listOf(testItem)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(RssReaderUiAction.ToggleFavorite("item-123"))
        advanceUntilIdle()

        verify(mockRepository).toggleFavorite("item-123", true)
    }
}
