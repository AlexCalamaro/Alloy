package com.squidink.alloy.modules.rssreader.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleNewsUrlBuilderTest {

    @Test
    fun `default feed url points to Google News Technology in US English`() {
        val defaultUrl = GoogleNewsUrlBuilder.DEFAULT_FEED_URL
        assertTrue("Expected topic TECHNOLOGY in default URL", defaultUrl.contains("topic/TECHNOLOGY"))
        assertTrue("Expected hl=en-US", defaultUrl.contains("hl=en-US"))
        assertTrue("Expected gl=US", defaultUrl.contains("gl=US"))
        assertTrue("Expected ceid=US:en", defaultUrl.contains("ceid=US:en"))
        assertEquals("Google News: Technology", GoogleNewsUrlBuilder.DEFAULT_FEED_TITLE)
    }

    @Test
    fun `top stories produces base feed URL without topic slug`() {
        val url = GoogleNewsUrlBuilder.buildUrl(
            category = GoogleNewsCategory.TOP_STORIES,
            language = GoogleNewsLanguage.US
        )
        assertEquals("https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en", url)
    }

    @Test
    fun `topic category produces topic endpoint URL`() {
        val url = GoogleNewsUrlBuilder.buildUrl(
            category = GoogleNewsCategory.BUSINESS,
            language = GoogleNewsLanguage.US
        )
        assertEquals(
            "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=en-US&gl=US&ceid=US:en",
            url
        )
    }

    @Test
    fun `search query uses search endpoint and URL encodes query`() {
        val url = GoogleNewsUrlBuilder.buildUrl(
            query = "Android Compose",
            language = GoogleNewsLanguage.US
        )
        assertTrue(url.startsWith("https://news.google.com/rss/search?q=Android+Compose"))
        assertTrue(url.contains("hl=en-US&gl=US&ceid=US:en"))
    }

    @Test
    fun `search query with recency operator incorporates when parameter`() {
        val url = GoogleNewsUrlBuilder.buildUrl(
            query = "AI",
            recency = GoogleNewsRecency.PAST_DAY,
            language = GoogleNewsLanguage.US
        )
        assertTrue(url.contains("q=AI+when%3A1d"))
    }

    @Test
    fun `international language preset configures hl gl and ceid correctly`() {
        val url = GoogleNewsUrlBuilder.buildUrl(
            category = GoogleNewsCategory.TECHNOLOGY,
            language = GoogleNewsLanguage.JAPAN
        )
        assertTrue(url.contains("hl=ja-JP&gl=JP&ceid=JP:ja"))
    }

    @Test
    fun `buildTitle formats appropriately for topic, search, and recency`() {
        val title1 = GoogleNewsUrlBuilder.buildTitle(
            category = GoogleNewsCategory.SCIENCE,
            language = GoogleNewsLanguage.US
        )
        assertEquals("Google News: Science", title1)

        val title2 = GoogleNewsUrlBuilder.buildTitle(
            query = "SpaceX",
            recency = GoogleNewsRecency.PAST_WEEK,
            language = GoogleNewsLanguage.UK
        )
        assertEquals("Google News: \"SpaceX\" (Past Week) [GB]", title2)
    }
}
