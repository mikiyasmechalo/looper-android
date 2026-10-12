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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.looper_android.MainActivity
import com.example.looper_android.R

class SquareProgressWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state by WidgetStateRepository.state.collectAsState()
            val config = WidgetSettingsManager.getConfig(context, "SQUARE")

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

                // 2. Main Centered Player Content
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Centered Artwork
                    if (state.artBitmap != null) {
                        Image(
                            provider = ImageProvider(state.artBitmap!!),
                            contentDescription = "Artwork",
                            modifier = GlanceModifier
                                .size(56.dp)
                                .cornerRadius(14.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = GlanceModifier
                                .size(56.dp)
                                .cornerRadius(14.dp)
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

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Title & Artist
                    Text(
                        text = state.title,
                        style = TextStyle(
                            color = ColorProvider(textCol),
                            fontSize = 13.sp,
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
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth()
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Enlarged Controls Row (Prev: 26dp, Play: 44dp, Next: 26dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 8.dp)
                                .clickable(actionRunCallback<PreviousActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_prev),
                                contentDescription = "Prev",
                                modifier = GlanceModifier.size(26.dp)
                            )
                        }

                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 8.dp)
                                .size(44.dp)
                                .cornerRadius(22.dp)
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

                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 8.dp)
                                .clickable(actionRunCallback<NextActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_next),
                                contentDescription = "Next",
                                modifier = GlanceModifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

class SquareProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SquareProgressWidget()
}
