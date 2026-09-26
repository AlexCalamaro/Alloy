package com.squidink.alloy.modules.rssreader.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.squidink.alloy.core.domain.repository.RssFeedItem
import com.squidink.alloy.modules.rssreader.data.RssRepositoryImpl
import com.squidink.alloy.modules.rssreader.db.RssFeedDatabase
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

/**
 * RSS Reader Glance Widget for desktop/home screen.
 * Displays recent RSS feed items with refresh functionality.
 */
class RssReaderGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val recentItems = getRecentFeedItems(context)
        
        provideContent {
            RssWidgetContent(recentItems)
        }
    }
}

/**
 * Widget content composable using Glance.
 */
@Composable
private fun RssWidgetContent(recentItems: List<RssFeedItem>) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Header
            Text(
                text = "RSS Feed",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onBackground
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )

            // Recent items
            if (recentItems.isNotEmpty()) {
                Column(
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    recentItems.take(3).forEach { item ->
                        FeedItemRow(item = item)
                    }
                }
            } else {
                Text(
                    text = "No feed items available",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            // Footer / Refresh button
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // TODO: Add refresh action when Glance supports it properly
                // actionRunCallback<RefreshWidgetAction>()
            }
        }
    }
}

/**
 * Individual feed item row in the widget.
 */
@Composable
private fun FeedItemRow(item: RssFeedItem) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = item.title,
            style = TextStyle(
                fontSize = 11.sp,
                color = GlanceTheme.colors.onBackground
            ),
            modifier = GlanceModifier.padding(horizontal = 4.dp)
        )
        Text(
            text = formatDate(item.pubDate),
            style = TextStyle(
                fontSize = 9.sp,
                color = GlanceTheme.colors.onBackground.copy(alpha = 0.6f)
            ),
            modifier = GlanceModifier.padding(horizontal = 4.dp)
        )
    }
}

/**
 * Get recent feed items from the database.
 */
private fun getRecentFeedItems(context: Context): List<RssFeedItem> {
    return runBlocking {
        try {
            val database = RssFeedDatabase.getInstance(context)
            val dao = database.rssFeedDao()
            dao.getRecentFeedItemsLimited(10)
                .first()
                .map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Format timestamp to readable date for widget.
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Convert database entity to domain model.
 */
private fun com.squidink.alloy.modules.rssreader.db.RssFeedItemEntity.toDomain(): RssFeedItem {
    return RssFeedItem(
        id = id,
        feedUrl = feedUrl,
        feedTitle = feedTitle,
        title = title,
        description = description,
        link = link,
        author = author,
        pubDate = pubDate,
        imageUrl = imageUrl,
        isRead = isRead,
        isFavorite = isFavorite,
        createdAt = createdAt
    )
}

/**
 * Widget receiver that links the Glance widget to the Android system.
 */
class RssReaderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RssReaderGlanceWidget()
}
