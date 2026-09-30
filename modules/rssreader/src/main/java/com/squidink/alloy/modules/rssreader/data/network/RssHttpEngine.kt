package com.squidink.alloy.modules.rssreader.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface IRssHttpEngine {
    suspend fun fetchFeed(url: String): ParsedFeed
}

/**
 * Engine executing raw OkHttp requests for RSS feeds with proper headers and streaming response handling.
 */
@Singleton
class RssHttpEngine @Inject constructor(
    private val okHttpClient: OkHttpClient
) : IRssHttpEngine {

    /**
     * Fetches and parses an RSS or Atom feed from the specified URL.
     */
    override suspend fun fetchFeed(url: String): ParsedFeed = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Alloy-RssReader/1.0")
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}: ${response.message}")
        }
        val body = response.body ?: throw IOException("Empty response body from $url")
        body.byteStream().use { inputStream ->
            RssXmlParser.parse(inputStream, url)
        }
    }
}
