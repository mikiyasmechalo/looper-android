package com.example.looper_android.ui.widgets

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.text.FontWeight

class LyricsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.parseColor("#1A1A1A")))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mock multiple lines (3-5 lines) of synced lyrics
                Text(
                    text = "Previous lyric line that passed...",
                    style = TextStyle(color = ColorProvider(Color.GRAY)),
                    modifier = GlanceModifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Current glowing synced lyric!",
                    style = TextStyle(
                        color = ColorProvider(Color.parseColor("#55DF69")), // App brand color
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "The next lyric coming up...",
                    style = TextStyle(color = ColorProvider(Color.GRAY)),
                    modifier = GlanceModifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

class LyricsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LyricsWidget()
}
