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
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetStateRepository.ensureInitialized(context)
        val appWidgetId = try {
            androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) { -1 }

        provideContent {
            val currentSize = LocalSize.current
            val semRowSpan = try {
                if (appWidgetId != -1) {
                    val manager = android.appwidget.AppWidgetManager.getInstance(context)
                    manager.getAppWidgetOptions(appWidgetId).getInt("semAppWidgetRowSpan", 0)
                } else 0
            } catch (_: Exception) { 0 }

            val isTaller = if (semRowSpan > 0) {
                semRowSpan >= 3
            } else {
                currentSize.height >= 260.dp
            }

            val state by WidgetStateRepository.state.collectAsState()
            val configVersion by WidgetSettingsManager.configVersion.collectAsState()
            val config = WidgetSettingsManager.getConfig(context, "LYRICS", appWidgetId)
            val accent = state.accentColor

            val cardBg = WidgetSettingsManager.getEffectiveCardColor(context, config, state.cardBackgroundColor)
            val textCol = WidgetSettingsManager.getEffectiveTextColor(context, config)
            val secTextCol = WidgetSettingsManager.getEffectiveSecondaryTextColor(context, config)

            Box(modifier = GlanceModifier.fillMaxSize()) {
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
                                modifier = GlanceModifier.size(48.dp).cornerRadius(12.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = GlanceModifier.size(48.dp).cornerRadius(12.dp).background(ColorProvider(Color(0xFF262626))),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(provider = ImageProvider(R.drawable.ic_widget_music), contentDescription = null, modifier = GlanceModifier.size(22.dp))
                            }
                        }

                        Spacer(modifier = GlanceModifier.width(10.dp))

                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = state.title,
                                style = TextStyle(color = ColorProvider(textCol), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = state.artist,
                                style = TextStyle(color = ColorProvider(secTextCol), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                maxLines = 1
                            )
                        }

                        // 3-Part Pill Controls (Equal 46dp width x 34dp height rectangular pill controls)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Pill Cap (46dp width, 34dp height)
                            Box(
                                modifier = GlanceModifier
                                    .width(46.dp)
                                    .height(34.dp)
                                    .background(ImageProvider(R.drawable.widget_ctrl_prev_pill))
                                    .clickable(actionRunCallback<PreviousActionCallback>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_prev),
                                    contentDescription = "Prev",
                                    modifier = GlanceModifier.size(17.dp)
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(3.dp))

                            // Play/Pause Dynamic Center Pill (46dp width, 34dp height)
                            Box(
                                modifier = GlanceModifier
                                    .width(46.dp)
                                    .height(34.dp)
                                    .let {
                                        if (state.isPlaying) {
                                            it.background(ImageProvider(R.drawable.widget_ctrl_play_pill))
                                        } else {
                                            it.cornerRadius(8.dp).background(ColorProvider(state.accentColor))
                                        }
                                    }
                                    .clickable(actionRunCallback<PlayPauseActionCallback>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(
                                        if (state.isPlaying) R.drawable.ic_widget_pause
                                        else R.drawable.ic_widget_play
                                    ),
                                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                                    modifier = GlanceModifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(3.dp))

                            // Next Pill Cap (46dp width, 34dp height)
                            Box(
                                modifier = GlanceModifier
                                    .width(46.dp)
                                    .height(34.dp)
                                    .background(ImageProvider(R.drawable.widget_ctrl_next_pill))
                                    .clickable(actionRunCallback<NextActionCallback>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_next),
                                    contentDescription = "Next",
                                    modifier = GlanceModifier.size(17.dp)
                                )
                            }

                            // Top Right Star Pill Wrapper (Matches Full Album Art Widget)
                            if (config.showFavorite) {
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .background(ImageProvider(R.drawable.widget_star_pill_bg))
                                        .padding(5.dp)
                                        .clickable(actionRunCallback<ToggleFavoriteActionCallback>()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        provider = ImageProvider(
                                            if (state.isFavorite) R.drawable.ic_widget_star_filled
                                            else R.drawable.ic_widget_star_border
                                        ),
                                        contentDescription = "Favorite",
                                        modifier = GlanceModifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // 2. Multi-Line Lyrics Area (Centered vertically in remaining space, 3 lines standard / 5 lines taller)
                    Column(
                        modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val rawLines: List<String> = if (state.lyricsLines.isNotEmpty()) state.lyricsLines else listOf("", "", "No lyrics available for this song", "", "")
                        val (displayLines, activeIdx) = if (isTaller) {
                            Pair(rawLines.take(5), 2)
                        } else {
                            if (rawLines.size >= 4) {
                                Pair(rawLines.subList(1, 4), 1)
                            } else {
                                Pair(rawLines, state.activeLyricIndex)
                            }
                        }

                        for (idx in displayLines.indices) {
                            val rawText = displayLines[idx]
                            val lineText = rawText.ifBlank { " " }
                            val isActive = idx == activeIdx
                            Text(
                                text = lineText,
                                style = TextStyle(
                                    color = ColorProvider(
                                        if (isActive) accent
                                        else secTextCol
                                    ),
                                    fontSize = if (isActive) (if (isTaller) 16.sp else 15.sp) else (if (isTaller) 13.sp else 12.sp),
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = if (isActive) (if (isTaller) 4.dp else 2.dp) else (if (isTaller) 3.dp else 1.dp))
                            )
                        }
                    }
                }

                if (config.showOutline) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(ImageProvider(R.drawable.widget_border_rounded_22))
                    ) {}
                }
            }
        }
    }
}

class LyricsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LyricsWidget()
}
