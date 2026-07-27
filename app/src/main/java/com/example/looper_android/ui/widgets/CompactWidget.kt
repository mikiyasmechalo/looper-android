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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class CompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // 1x4 Widget Layout
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.DKGRAY))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Album Art
                Box(
                    modifier = GlanceModifier.size(48.dp).background(ColorProvider(Color.LTGRAY))
                ) {
                    // Placeholder for actual album art image
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                // 2. Title and Artist Column
                Column(
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Text(
                        text = "Song Title",
                        style = TextStyle(color = ColorProvider(Color.WHITE), fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = "Artist Name",
                        style = TextStyle(color = ColorProvider(Color.LTGRAY)),
                        maxLines = 1
                    )
                }

                // 3. Controls at the right end
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⏪", modifier = GlanceModifier.padding(horizontal = 8.dp))
                    Text(text = "⏸️", modifier = GlanceModifier.padding(horizontal = 8.dp))
                    Text(text = "⏩", modifier = GlanceModifier.padding(horizontal = 8.dp))
                }
            }
        }
    }
}

class CompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWidget()
}
