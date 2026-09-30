package com.squidink.alloy.modules.rssreader.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.squidink.alloy.core.domain.common.model.RssFeedItem
import com.squidink.alloy.core.navigation.Screens
import com.squidink.alloy.modules.rssreader.R
import com.squidink.alloy.modules.rssreader.db.RssFeedDatabase
import com.squidink.alloy.modules.rssreader.db.RssFeedItemEntity
import com.squidink.alloy.modules.rssreader.ui.formatRelativeTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * RSS Reader Glance Widget for desktop/home screen.
 *
 * Displays the cards present in the main app's RSS feed with no additional UI
 * except a button to open the app itself in the top right corner.
 *
 * Card clicks open the article in the native browser and mark the article as read
 * in the Room database so that the main app UI reflects the updated read status.
 */
class RssReaderGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = RssFeedDatabase.getInstance(context)
        val dao = database.rssFeedDao()

        // Preload initial snapshot to prevent empty flash on cold start
        val initialItems = try {
            dao.getAllFeedItems().first().map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }

        provideContent {
            val itemsFlow = remember {
                dao.getAllFeedItems().map { list -> list.map { it.toDomain() } }
            }
            val items by itemsFlow.collectAsState(initial = initialItems)

            RssWidgetContent(
                context = context,
                items = items
            )
        }
    }

    companion object {
        private const val TAG = "RssReaderGlanceWidget"

        /**
         * Triggers a refresh for all active instances of the RSS Reader Glance widget.
         */
        fun notifyWidgetDataChanged(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RssReaderGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update Glance widget", e)
                }
            }
        }
    }
}

/**
 * Widget content composable using Glance.
 */
@Composable
private fun RssWidgetContent(
    context: Context,
    items: List<RssFeedItem>
) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
        ) {
            // Dedicated button to open the app itself in the top right corner (no other UI)
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OpenAppButton(context)
            }

            // Article Cards List or Empty State
            if (items.isNotEmpty()) {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    items(
                        items = items,
                        itemId = { it.id.hashCode().toLong() }
                    ) { item ->
                        GlanceArticleCard(
                            context = context,
                            item = item
                        )
                    }
                }
            } else {
                EmptyWidgetState()
            }
        }
    }
}

/**
 * Top-right button that launches the Alloy app directly into the RSS Reader screen.
 */
@Composable
private fun OpenAppButton(context: Context) {
    val intent = Intent().apply {
        setClassName(context.packageName, "com.squidink.alloy.ui.DashboardActivity")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("target_screen", Screens.RssReader.route)
    }

    Box(
        modifier = GlanceModifier
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.primaryContainer)
            .clickable(actionStartActivity(intent))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_open_app),
                contentDescription = "Open RSS Reader in Alloy",
                modifier = GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "Open Alloy",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * Material 3 Article card in Glance mirroring RssArticleCard in the main app.
 * Clicking the card opens the article in the browser and marks it as read.
 */
@Composable
private fun GlanceArticleCard(
    context: Context,
    item: RssFeedItem
) {
    val clickIntent = ArticleLauncherActivity.createIntent(
        context = context,
        articleId = item.id,
        articleUrl = item.link
    )

    // Outer Box provides clean vertical margins between cards
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .clickable(actionStartActivity(clickIntent))
                .padding(12.dp)
        ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            // Source badge and relative timestamp header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Feed source badge
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(6.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.feedTitle ?: "RSS",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onPrimaryContainer
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Timestamp
                Text(
                    text = formatRelativeTime(item.pubDate),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Article title
            Text(
                text = item.title,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Medium,
                    color = if (item.isRead) {
                        GlanceTheme.colors.onSurfaceVariant
                    } else {
                        GlanceTheme.colors.onSurface
                    }
                ),
                maxLines = 3
            )

            // Article description snippet
            item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = desc.trim(),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    ),
                    maxLines = 2
                )
            }

            // Author attribution
            item.author?.takeIf { it.isNotBlank() }?.let { author ->
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "By ${author.trim()}",
                    style = TextStyle(
                        fontSize = 9.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
}

/**
 * Centered empty state when no feed articles are available.
 */
@Composable
private fun EmptyWidgetState() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No articles available",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Tap 'Open Alloy' above to subscribe to feeds",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * Convert database entity to domain model.
 */
internal fun RssFeedItemEntity.toDomain(): RssFeedItem {
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
