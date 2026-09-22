package com.squidink.alloy.modules.statspill

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * AppWidgetProvider for 2×2 / 4×1 desktop telemetry widget.
 */
class StatsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(android.R.id.text1, "Alloy System Stats")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
