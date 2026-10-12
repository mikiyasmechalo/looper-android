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

class FullAlbumArtWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val state by WidgetStateRepository.state.collectAsState()
            val config = WidgetSettingsManager.getConfig(context, "FULL_ART")

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(ColorProvider(state.cardBackgroundColor))
                    .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java)))
            ) {
                // 1. Full-bleed Artwork Background (CROP FILL) with Dark Ambient Gradient
                if (state.artFullWithGradient != null) {
                    Image(
                        provider = ImageProvider(state.artFullWithGradient!!),
                        contentDescription = "Album Artwork",
                        modifier = GlanceModifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(ImageProvider(R.drawable.widget_glass_bg)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_music),
                            contentDescription = "Music",
                            modifier = GlanceModifier.size(64.dp)
                        )
                    }
                }

                // 2. Top Right Star (Like / Favorite) Button in Frosted Glass Pill (Respects showFavorite)
                if (config.showFavorite) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .background(ImageProvider(R.drawable.widget_star_pill_bg))
                                .padding(8.dp)
                                .clickable(actionRunCallback<ToggleFavoriteActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(
                                    if (state.isFavorite) R.drawable.ic_widget_star_filled
                                    else R.drawable.ic_widget_star_border
                                ),
                                contentDescription = "Favorite",
                                modifier = GlanceModifier.size(20.dp)
                            )
                        }
                    }
                }

                // 3. Bottom Metadata & Enlarged Padded Controls Wrapper
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Song Title & Artist
                    Text(
                        text = state.title,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 6.dp)
                    )

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    Text(
                        text = state.artist,
                        style = TextStyle(
                            color = ColorProvider(Color(0xB3FFFFFF)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 6.dp)
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Padded Controls Dock with Dynamic Accent Color
                    Row(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_controls_pill_bg))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous
                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable(actionRunCallback<PreviousActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_prev),
                                contentDescription = "Previous",
                                modifier = GlanceModifier.size(24.dp)
                            )
                        }

                        // Play/Pause Dynamic Palette Accent Button
                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 10.dp)
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

                        // Next
                        Box(
                            modifier = GlanceModifier
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable(actionRunCallback<NextActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_next),
                                contentDescription = "Next",
                                modifier = GlanceModifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

class FullAlbumArtWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FullAlbumArtWidget()
}
