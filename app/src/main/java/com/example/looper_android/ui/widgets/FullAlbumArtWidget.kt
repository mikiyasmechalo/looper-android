package com.example.looper_android.ui.widgets

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class FullAlbumArtWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Root Box to hold the full background image and overlays
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.DKGRAY))
            ) {
                // Background Album Art (Mocked with color for now)
                
                // Top Right Star Button
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Star button
                    Text(
                        text = "⭐",
                        modifier = GlanceModifier.clickable { /* Toggle Favorite */ }
                    )
                }

                // Bottom Controls Wrapper
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Padded wrapper for bigger controls
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ColorProvider(Color.parseColor("#80000000"))) // Glassmorphism dark tint
                            .padding(16.dp), // Padded wrapper to make it bigger and easier to tap
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⏪", style = TextStyle(color = ColorProvider(Color.WHITE)), modifier = GlanceModifier.padding(horizontal = 16.dp))
                        Text(text = "⏸️", style = TextStyle(color = ColorProvider(Color.WHITE)), modifier = GlanceModifier.padding(horizontal = 24.dp))
                        Text(text = "⏩", style = TextStyle(color = ColorProvider(Color.WHITE)), modifier = GlanceModifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

class FullAlbumArtWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FullAlbumArtWidget()
}
