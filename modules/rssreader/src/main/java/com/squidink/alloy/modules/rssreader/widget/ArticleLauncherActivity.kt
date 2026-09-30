package com.squidink.alloy.modules.rssreader.widget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.squidink.alloy.modules.rssreader.db.RssFeedDatabase
import com.squidink.alloy.modules.rssreader.ui.openInNativeBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Trampoline activity for RSS widget card clicks.
 *
 * Runs with a transparent theme to avoid UI flickering.
 * Marks the article as read in Room DB, triggers a widget update,
 * launches the article in the user's native web browser, and immediately finishes.
 */
class ArticleLauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val articleId = intent.getStringExtra(EXTRA_ARTICLE_ID)
        val articleUrl = intent.getStringExtra(EXTRA_ARTICLE_URL)

        if (!articleId.isNullOrBlank()) {
            val appContext = applicationContext
            val scope = runCatching {
                dagger.hilt.android.EntryPointAccessors.fromApplication(
                    appContext,
                    com.squidink.alloy.core.common.di.DispatchersEntryPoint::class.java
                ).applicationScope()
            }.getOrDefault(CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO))

            scope.launch(Dispatchers.IO) {
                try {
                    val db = RssFeedDatabase.getInstance(appContext)
                    db.rssFeedDao().markItemAsRead(articleId)
                    RssReaderGlanceWidget().updateAll(appContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mark article as read from widget", e)
                }
            }
        }

        if (!articleUrl.isNullOrBlank()) {
            openInNativeBrowser(this, articleUrl)
        }

        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val TAG = "ArticleLauncher"
        const val EXTRA_ARTICLE_ID = "com.squidink.alloy.modules.rssreader.widget.ARTICLE_ID"
        const val EXTRA_ARTICLE_URL = "com.squidink.alloy.modules.rssreader.widget.ARTICLE_URL"

        /**
         * Creates an Intent to launch this activity with the specified article info.
         */
        fun createIntent(context: Context, articleId: String, articleUrl: String?): Intent {
            return Intent(context, ArticleLauncherActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_ARTICLE_ID, articleId)
                putExtra(EXTRA_ARTICLE_URL, articleUrl)
            }
        }
    }
}
