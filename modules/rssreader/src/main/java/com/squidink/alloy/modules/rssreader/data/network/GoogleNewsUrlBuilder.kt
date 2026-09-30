package com.squidink.alloy.modules.rssreader.data.network

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class GoogleNewsCategory(val slug: String, val displayName: String) {
    TOP_STORIES("", "Top Stories"),
    TECHNOLOGY("TECHNOLOGY", "Technology"),
    BUSINESS("BUSINESS", "Business"),
    WORLD("WORLD", "World"),
    NATION("NATION", "Nation"),
    SCIENCE("SCIENCE", "Science"),
    HEALTH("HEALTH", "Health"),
    SPORTS("SPORTS", "Sports"),
    ENTERTAINMENT("ENTERTAINMENT", "Entertainment")
}

enum class GoogleNewsLanguage(
    val displayName: String,
    val hl: String,
    val gl: String,
    val ceid: String
) {
    US("English (United States)", "en-US", "US", "US:en"),
    UK("English (United Kingdom)", "en-GB", "GB", "GB:en"),
    CANADA("English (Canada)", "en-CA", "CA", "CA:en"),
    SPAIN("Spanish (Spain)", "es-ES", "ES", "ES:es"),
    FRANCE("French (France)", "fr-FR", "FR", "FR:fr"),
    GERMANY("German (Germany)", "de-DE", "DE", "DE:de"),
    JAPAN("Japanese (Japan)", "ja-JP", "JP", "JP:ja")
}

enum class GoogleNewsRecency(val operator: String, val displayName: String) {
    ANY("", "Any Time"),
    PAST_HOUR("when:1h", "Past Hour"),
    PAST_DAY("when:1d", "Past 24 Hours"),
    PAST_WEEK("when:7d", "Past Week"),
    PAST_MONTH("when:30d", "Past Month"),
    PAST_YEAR("when:1y", "Past Year")
}

/**
 * First-party URL and title builder for Google News RSS feeds.
 * Follows the official Google News RSS parameter schema and search operators.
 * Reference: https://cloro.dev/blog/google-news-rss/
 */
object GoogleNewsUrlBuilder {

    const val DEFAULT_FEED_URL =
        "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en"
    const val DEFAULT_FEED_TITLE = "Google News: Technology"

    /**
     * Builds a full Google News RSS feed URL based on topic, query, recency, and language.
     */
    fun buildUrl(
        category: GoogleNewsCategory? = null,
        query: String? = null,
        recency: GoogleNewsRecency? = null,
        language: GoogleNewsLanguage = GoogleNewsLanguage.US
    ): String {
        val cleanQuery = query?.trim().orEmpty()
        val recencyOp = recency?.operator.orEmpty()

        val localeParams = "hl=${language.hl}&gl=${language.gl}&ceid=${language.ceid}"

        // If a query or recency operator is specified, we must use the search endpoint: /rss/search?q=...
        if (cleanQuery.isNotEmpty() || recencyOp.isNotEmpty()) {
            val queryParts = mutableListOf<String>()
            
            // If both category and query exist, include category as topic keyword
            if (category != null && category != GoogleNewsCategory.TOP_STORIES && cleanQuery.isEmpty()) {
                queryParts.add(category.displayName)
            } else if (cleanQuery.isNotEmpty()) {
                queryParts.add(cleanQuery)
            }

            if (recencyOp.isNotEmpty()) {
                queryParts.add(recencyOp)
            }

            val finalQ = queryParts.joinToString(" ")
            val encodedQ = URLEncoder.encode(finalQ, StandardCharsets.UTF_8.name())
            return "https://news.google.com/rss/search?q=$encodedQ&$localeParams"
        }

        // Pure topic feeds: /rss/headlines/section/topic/{SLUG}?hl=...
        return when {
            category == null || category == GoogleNewsCategory.TOP_STORIES -> {
                "https://news.google.com/rss?$localeParams"
            }
            else -> {
                "https://news.google.com/rss/headlines/section/topic/${category.slug}?$localeParams"
            }
        }
    }

    /**
     * Builds a human-readable title for the configured Google News feed.
     */
    fun buildTitle(
        category: GoogleNewsCategory? = null,
        query: String? = null,
        recency: GoogleNewsRecency? = null,
        language: GoogleNewsLanguage = GoogleNewsLanguage.US
    ): String {
        val cleanQuery = query?.trim().orEmpty()
        val parts = mutableListOf<String>()

        if (cleanQuery.isNotEmpty()) {
            parts.add("\"$cleanQuery\"")
        } else if (category != null && category != GoogleNewsCategory.TOP_STORIES) {
            parts.add(category.displayName)
        } else {
            parts.add("Top Stories")
        }

        if (recency != null && recency != GoogleNewsRecency.ANY) {
            parts.add("(${recency.displayName})")
        }

        if (language != GoogleNewsLanguage.US) {
            parts.add("[${language.gl}]")
        }

        return "Google News: " + parts.joinToString(" ")
    }
}
