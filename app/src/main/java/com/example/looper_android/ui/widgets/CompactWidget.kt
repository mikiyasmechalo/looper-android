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
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.looper_android.MainActivity
import com.example.looper_android.R

class CompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetStateRepository.ensureInitialized(context)
        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) { -1 }

        provideContent {
            val state by WidgetStateRepository.state.collectAsState()
            val configVersion by WidgetSettingsManager.configVersion.collectAsState()
            val config = WidgetSettingsManager.getConfig(context, "COMPACT", appWidgetId)

            val cardBg = WidgetSettingsManager.getEffectiveCardColor(context, config, state.cardBackgroundColor)
            val textCol = WidgetSettingsManager.getEffectiveTextColor(context, config)
            val secTextCol = WidgetSettingsManager.getEffectiveSecondaryTextColor(context, config)

            val containerRadius = if (config.circularCapsule) 37.dp else 18.dp
            val horizontalPadding = if (config.circularCapsule) 12.dp else 9.dp
            val artRadius = if (config.circularCapsule) 29.dp else 18.dp

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(74.dp)
                ) {
                    // Strict 74dp height cap for the classic 4x1 widget
                    Row(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(containerRadius)
                            .background(ColorProvider(cardBg))
                            .padding(horizontal = horizontalPadding, vertical = 8.dp)
                            .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java))),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Album Art (Scaled to 58dp, corner radius automatically matches container: 18dp rounded, 29dp circle)
                        if (state.artBitmap != null) {
                            Image(
                                provider = ImageProvider(state.artBitmap!!),
                                contentDescription = "Artwork",
                                modifier = GlanceModifier
                                    .size(58.dp)
                                    .cornerRadius(artRadius),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = GlanceModifier
                                    .size(58.dp)
                                    .cornerRadius(artRadius)
                                    .background(ColorProvider(Color(0xFF282828))),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_music),
                                    contentDescription = "Music",
                                    modifier = GlanceModifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.width(10.dp))

                        // 2. Title & Artist Column (Center)
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.title,
                                style = TextStyle(
                                    color = ColorProvider(textCol),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = state.artist,
                                style = TextStyle(
                                    color = ColorProvider(secTextCol),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(6.dp))

                        // 3. Three-Part Pill Controls (Equal 48dp width x 36dp height, matching 8dp facing corners)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
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

                            // Previous Pill Cap (48dp width, 36dp height, 8dp facing corner, 18dp outer)
                            Box(
                                modifier = GlanceModifier
                                    .width(48.dp)
                                    .height(36.dp)
                                    .background(ImageProvider(prevPillBg))
                                    .clickable(actionRunCallback<PreviousActionCallback>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(prevIconRes),
                                    contentDescription = "Previous",
                                    modifier = GlanceModifier.size(19.dp)
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(3.dp))

                            // Play/Pause Center Pill (48dp width, 36dp height, matching 8dp corner radius)
                            Box(
                                modifier = GlanceModifier
                                    .width(48.dp)
                                    .height(36.dp)
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
                                    modifier = GlanceModifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = GlanceModifier.width(3.dp))

                            // Next Pill Cap (48dp width, 36dp height, 8dp facing corner, 18dp outer)
                            Box(
                                modifier = GlanceModifier
                                    .width(48.dp)
                                    .height(36.dp)
                                    .background(ImageProvider(nextPillBg))
                                    .clickable(actionRunCallback<NextActionCallback>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(nextIconRes),
                                    contentDescription = "Next",
                                    modifier = GlanceModifier.size(19.dp)
                                )
                            }

                            // Optional Favorite Button
                            if (config.showFavorite) {
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                val starIconRes = if (state.isFavorite) {
                                    R.drawable.ic_widget_star_filled
                                } else {
                                    if (isDarkIcons) R.drawable.ic_widget_star_border_dark else R.drawable.ic_widget_star_border
                                }
                                Box(
                                    modifier = GlanceModifier
                                        .padding(horizontal = 2.dp, vertical = 2.dp)
                                        .clickable(actionRunCallback<ToggleFavoriteActionCallback>()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        provider = ImageProvider(starIconRes),
                                        contentDescription = "Favorite",
                                        modifier = GlanceModifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (config.showOutline) {
                        val borderRes = if (config.circularCapsule) R.drawable.widget_border_capsule_37 else R.drawable.widget_border_rounded_18
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(ImageProvider(borderRes))
                        ) {}
                    }
                }
            }
        }
    }
}

class CompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWidget()
}
