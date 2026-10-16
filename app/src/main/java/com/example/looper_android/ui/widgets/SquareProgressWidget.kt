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
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
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

class SquareProgressWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetStateRepository.ensureInitialized(context)
        val appWidgetId = try {
            androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) { -1 }

        provideContent {
            val widgetSize = LocalSize.current
            val minDim = minOf(widgetSize.width, widgetSize.height)

            // Dynamic scaling: base 160.dp (standard 2x2 widget).
            // Image scales up significantly as widget scales (e.g., from 58dp up to 135dp+ on 4x4)
            val scale = (1.0f + ((minDim.value - 160f) / 160f).coerceIn(0f, 1.8f))
            val artSize = (58 * scale).dp
            val artRadius = (14 * scale).dp
            val musicIconSize = (26 * scale).dp

            val titleFontSize = (13 * (1.0f + ((minDim.value - 160f) / 160f).coerceIn(0f, 1.0f) * 0.35f)).sp
            val artistFontSize = (11 * (1.0f + ((minDim.value - 160f) / 160f).coerceIn(0f, 1.0f) * 0.35f)).sp
            val vSpacer = (6 * scale).dp.coerceAtMost(16.dp)

            val ctrlScale = (1.0f + ((minDim.value - 160f) / 160f).coerceIn(0f, 1.0f) * 0.20f)
            val ctrlWidth = (50 * ctrlScale).dp
            val ctrlHeight = (36 * ctrlScale).dp
            val ctrlPrevNextIconSize = (18 * ctrlScale).dp
            val ctrlPlayIconSize = (20 * ctrlScale).dp

            val state by WidgetStateRepository.state.collectAsState()
            val configVersion by WidgetSettingsManager.configVersion.collectAsState()
            val config = WidgetSettingsManager.getConfig(context, "SQUARE", appWidgetId)

            val cardBg = WidgetSettingsManager.getEffectiveCardColor(context, config, state.cardBackgroundColor)
            val textCol = WidgetSettingsManager.getEffectiveTextColor(context, config)
            val secTextCol = WidgetSettingsManager.getEffectiveSecondaryTextColor(context, config)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(22.dp)
                    .background(ColorProvider(cardBg))
                    .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java)))
            ) {
                // 1. Top Right Favorite (Star) Button in Frosted Glass Pill Wrapper
                if (config.showFavorite) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
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

                // 2. Main Centered Player Content (Dynamically Scaled)
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Centered Artwork (Scales with widget size)
                    if (state.artBitmap != null) {
                        Image(
                            provider = ImageProvider(state.artBitmap!!),
                            contentDescription = "Artwork",
                            modifier = GlanceModifier
                                .size(artSize)
                                .cornerRadius(artRadius),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = GlanceModifier
                                .size(artSize)
                                .cornerRadius(artRadius)
                                .background(ColorProvider(Color(0xFF282828))),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_music),
                                contentDescription = "Music",
                                modifier = GlanceModifier.size(musicIconSize)
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(vSpacer))

                    // Title & Artist
                    Text(
                        text = state.title,
                        style = TextStyle(
                            color = ColorProvider(textCol),
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth()
                    )

                    Text(
                        text = state.artist,
                        style = TextStyle(
                            color = ColorProvider(secTextCol),
                            fontSize = artistFontSize,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth()
                    )

                    Spacer(modifier = GlanceModifier.height(vSpacer + 2.dp))

                    // Three-Part Pill Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isDarkIcons = WidgetSettingsManager.isDarkControlIcons(context, config)
                        val prevPillBg = if (isDarkIcons) R.drawable.widget_ctrl_prev_pill_light else R.drawable.widget_ctrl_prev_pill
                        val nextPillBg = if (isDarkIcons) R.drawable.widget_ctrl_next_pill_light else R.drawable.widget_ctrl_next_pill
                        val playPillBg = if (isDarkIcons) R.drawable.widget_ctrl_play_pill_light else R.drawable.widget_ctrl_play_pill

                        val prevIconRes = if (isDarkIcons) R.drawable.ic_widget_prev_dark else R.drawable.ic_widget_prev
                        val nextIconRes = if (isDarkIcons) R.drawable.ic_widget_next_dark else R.drawable.ic_widget_next
                        val playPauseIconRes = if (state.isPlaying) {
                            if (isDarkIcons) R.drawable.ic_widget_pause_dark else R.drawable.ic_widget_pause
                        } else {
                            if (isDarkIcons) R.drawable.ic_widget_play_dark else R.drawable.ic_widget_play
                        }

                        // Previous Pill Cap
                        Box(
                            modifier = GlanceModifier
                                .width(ctrlWidth)
                                .height(ctrlHeight)
                                .background(ImageProvider(prevPillBg))
                                .clickable(actionRunCallback<PreviousActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(prevIconRes),
                                contentDescription = "Prev",
                                modifier = GlanceModifier.size(ctrlPrevNextIconSize)
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(4.dp))

                        // Play/Pause Center Pill
                        Box(
                            modifier = GlanceModifier
                                 .width(ctrlWidth)
                                 .height(ctrlHeight)
                                 .let {
                                     if (state.isPlaying) {
                                         it.background(ImageProvider(playPillBg))
                                     } else {
                                         it.cornerRadius(8.dp).background(ColorProvider(state.accentColor))
                                     }
                                 }
                                 .clickable(actionRunCallback<PlayPauseActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(playPauseIconRes),
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                modifier = GlanceModifier.size(ctrlPlayIconSize)
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(4.dp))

                        // Next Pill Cap
                        Box(
                            modifier = GlanceModifier
                                .width(ctrlWidth)
                                .height(ctrlHeight)
                                .background(ImageProvider(nextPillBg))
                                .clickable(actionRunCallback<NextActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(nextIconRes),
                                contentDescription = "Next",
                                modifier = GlanceModifier.size(ctrlPrevNextIconSize)
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

class SquareProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SquareProgressWidget()
}
