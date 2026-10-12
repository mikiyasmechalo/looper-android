package com.example.looper_android.ui.widgets

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import androidx.glance.unit.ColorProvider
import com.example.looper_android.MainActivity
import com.example.looper_android.R

class LyricsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state by WidgetStateRepository.state.collectAsState()
            val config = WidgetSettingsManager.getConfig(context, "LYRICS")
            val accent = state.accentColor

            val cardBg = WidgetSettingsManager.getEffectiveCardColor(context, config, state.cardBackgroundColor)
            val textCol = WidgetSettingsManager.getEffectiveTextColor(context, config)
            val secTextCol = WidgetSettingsManager.getEffectiveSecondaryTextColor(context, config)

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(22.dp)
                    .background(ColorProvider(cardBg))
                    .padding(12.dp)
                    .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java)))
            ) {
                // 1. Top Mini Player Bar (With Star Button at Top Right like Full Art widget)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.artBitmap != null) {
                        Image(
                            provider = ImageProvider(state.artBitmap!!),
                            contentDescription = "Artwork",
                            modifier = GlanceModifier.size(38.dp).cornerRadius(10.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = GlanceModifier.size(38.dp).cornerRadius(10.dp).background(ColorProvider(Color(0xFF262626))),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(provider = ImageProvider(R.drawable.ic_widget_music), contentDescription = null, modifier = GlanceModifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(10.dp))

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = state.title,
                            style = TextStyle(color = ColorProvider(textCol), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = state.artist,
                            style = TextStyle(color = ColorProvider(secTextCol), fontSize = 11.sp),
                            maxLines = 1
                        )
                    }

                    // Mini Controls Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = GlanceModifier.padding(horizontal = 3.dp).clickable(actionRunCallback<PreviousActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(provider = ImageProvider(R.drawable.ic_widget_prev), contentDescription = "Prev", modifier = GlanceModifier.size(20.dp))
                        }

                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 4.dp)
                                .size(32.dp)
                                .cornerRadius(16.dp)
                                .background(ColorProvider(state.accentColor))
                                .clickable(actionRunCallback<PlayPauseActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play),
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                modifier = GlanceModifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = GlanceModifier.padding(horizontal = 3.dp).clickable(actionRunCallback<NextActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(provider = ImageProvider(R.drawable.ic_widget_next), contentDescription = "Next", modifier = GlanceModifier.size(20.dp))
                        }

                        // Top Right Star Pill Wrapper (Matches Full Album Art Widget)
                        if (config.showFavorite) {
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            Box(
                                modifier = GlanceModifier
                                    .background(ImageProvider(R.drawable.widget_star_pill_bg))
                                    .padding(6.dp)
                                    .clickable(actionRunCallback<ToggleFavoriteActionCallback>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(
                                        if (state.isFavorite) R.drawable.ic_widget_star_filled
                                        else R.drawable.ic_widget_star_border
                                    ),
                                    contentDescription = "Favorite",
                                    modifier = GlanceModifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // 2. Multi-Line Lyrics (3 to 5 Lines Window)
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val lines: List<String> = if (state.lyricsLines.isNotEmpty()) state.lyricsLines else listOf("No lyrics available for this song")
                    val activeIdx = state.activeLyricIndex

                    for (idx in lines.indices) {
                        val lineText = lines[idx]
                        val isActive = idx == activeIdx
                        Text(
                            text = lineText,
                            style = TextStyle(
                                color = ColorProvider(
                                    if (isActive) accent
                                    else secTextCol
                                ),
                                fontSize = if (isActive) 14.sp else 12.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = if (isActive) 3.dp else 2.dp)
                        )
                    }
                }
            }
        }
    }
}

class LyricsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LyricsWidget()
}
