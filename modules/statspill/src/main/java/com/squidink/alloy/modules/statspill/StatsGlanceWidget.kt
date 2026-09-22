package com.squidink.alloy.modules.statspill

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * 100% Jetpack Glance Compose AppWidget for desktop telemetry (zero layout XML).
 */
class StatsGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceWidgetContent()
        }
    }

    @Composable
    private fun GlanceWidgetContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            Text(
                text = "Alloy System Vitals",
                style = TextStyle(fontSize = 16.sp)
            )
            Text(
                text = "RAM: Monitoring 1Hz",
                style = TextStyle(fontSize = 14.sp),
                modifier = GlanceModifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * AppWidgetReceiver linking Glance Widget to the Android system.
 */
class StatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatsGlanceWidget()
}
