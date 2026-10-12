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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.looper_android.MainActivity
import com.example.looper_android.R

class CompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state by WidgetStateRepository.state.collectAsState()
            val config = WidgetSettingsManager.getConfig(context, "COMPACT")

            val cardBg = WidgetSettingsManager.getEffectiveCardColor(context, config, state.cardBackgroundColor)
            val textCol = WidgetSettingsManager.getEffectiveTextColor(context, config)
            val secTextCol = WidgetSettingsManager.getEffectiveSecondaryTextColor(context, config)

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .cornerRadius(18.dp)
                        .background(ColorProvider(cardBg))
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                        .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java))),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Album Art (58dp with comfortable vertical margin)
                    if (state.artBitmap != null) {
                        Image(
                            provider = ImageProvider(state.artBitmap!!),
                            contentDescription = "Artwork",
                            modifier = GlanceModifier
                                .size(58.dp)
                                .cornerRadius(13.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = GlanceModifier
                                .size(58.dp)
                                .cornerRadius(13.dp)
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

                    // 3. Controls (Prev, Play, Next, Star)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous
                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clickable(actionRunCallback<PreviousActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_prev),
                                contentDescription = "Previous",
                                modifier = GlanceModifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(2.dp))

                        // Play/Pause Dynamic Button (Prominent 40dp)
                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 2.dp)
                                .size(40.dp)
                                .cornerRadius(20.dp)
                                .background(ColorProvider(state.accentColor))
                                .clickable(actionRunCallback<PlayPauseActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(
                                    if (state.isPlaying) R.drawable.ic_widget_pause
                                    else R.drawable.ic_widget_play
                                ),
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                modifier = GlanceModifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(2.dp))

                        // Next
                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clickable(actionRunCallback<NextActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_next),
                                contentDescription = "Next",
                                modifier = GlanceModifier.size(22.dp)
                            )
                        }

                    // Optional Favorite Button
                    if (config.showFavorite) {
                        Spacer(modifier = GlanceModifier.width(2.dp))
                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 3.dp, vertical = 2.dp)
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
        }
    }
}
}

class CompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWidget()
}
