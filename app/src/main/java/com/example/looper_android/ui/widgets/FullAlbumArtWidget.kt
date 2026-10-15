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

class FullAlbumArtWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetStateRepository.ensureInitialized(context)
        val appWidgetId = try {
            androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) { -1 }

        provideContent {
            val widgetSize = LocalSize.current
            val minDim = minOf(widgetSize.width, widgetSize.height)

            // Dynamic scaling: base 160.dp (standard 2x2 widget), scaled up by at most 20% (1.20x) for 4x4
            val scale = (1.0f + ((minDim.value - 160f) / 160f).coerceIn(0f, 1f) * 0.20f)

            val ctrlBtnWidth = (50 * scale).dp
            val ctrlBtnHeight = (36 * scale).dp
            val ctrlPlayCorner = 8.dp
            val ctrlPrevNextIconSize = (18 * scale).dp
            val ctrlPlayIconSize = (20 * scale).dp
            val ctrlSpacing = 4.dp

            val starBtnSize = (38 * scale).dp
            val starIconSize = (18 * scale).dp

            val titleFontSize = (15 * scale).sp
            val artistFontSize = (12 * scale).sp
            val bottomPadding = (12 * scale).dp
            val rootRadius = (24 * scale).dp.coerceAtMost(36.dp)

            val state by WidgetStateRepository.state.collectAsState()
            val configVersion by WidgetSettingsManager.configVersion.collectAsState()
            val config = WidgetSettingsManager.getConfig(context, "FULL_ART", appWidgetId)
            val cardBg = WidgetSettingsManager.getEffectiveCardColor(context, config, state.cardBackgroundColor)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(rootRadius)
                    .background(ColorProvider(cardBg))
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
                            modifier = GlanceModifier.size((56 * scale).dp)
                        )
                    }
                }

                // 2. Top Right Star (Like / Favorite) Button in Frosted Glass Pill (Respects showFavorite)
                if (config.showFavorite) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding((12 * scale).dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(starBtnSize)
                                .background(ImageProvider(R.drawable.widget_star_pill_bg))
                                .clickable(actionRunCallback<ToggleFavoriteActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(
                                    if (state.isFavorite) R.drawable.ic_widget_star_filled
                                    else R.drawable.ic_widget_star_border
                                ),
                                contentDescription = "Favorite",
                                modifier = GlanceModifier.size(starIconSize)
                            )
                        }
                    }
                }

                // 3. Bottom Metadata & Dynamically Scaled Padded Controls Wrapper
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(bottomPadding),
                    verticalAlignment = Alignment.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Song Title & Artist
                    Text(
                        text = state.title,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 6.dp)
                    )

                    Spacer(modifier = GlanceModifier.height((2 * scale).dp))

                    Text(
                        text = state.artist,
                        style = TextStyle(
                            color = ColorProvider(Color(0xB3FFFFFF)),
                            fontSize = artistFontSize,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 6.dp)
                    )

                    Spacer(modifier = GlanceModifier.height((8 * scale).dp))

                    // Three-Part Pill Controls (Equal width buttons, matching 8dp*scale facing corner radii)
                    Row(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Pill Cap (Equal width, 7dp facing corner)
                        Box(
                            modifier = GlanceModifier
                                .width(ctrlBtnWidth)
                                .height(ctrlBtnHeight)
                                .background(ImageProvider(R.drawable.widget_ctrl_prev_pill))
                                .clickable(actionRunCallback<PreviousActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_prev),
                                contentDescription = "Previous",
                                modifier = GlanceModifier.size(ctrlPrevNextIconSize)
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(ctrlSpacing))

                        // Play/Pause Dynamic Center Pill (Equal width, matching corner radius)
                        Box(
                            modifier = GlanceModifier
                                .width(ctrlBtnWidth)
                                .height(ctrlBtnHeight)
                                .let {
                                    if (state.isPlaying) {
                                        it.background(ImageProvider(R.drawable.widget_ctrl_play_pill))
                                    } else {
                                        it.cornerRadius(ctrlPlayCorner).background(ColorProvider(state.accentColor))
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
                                modifier = GlanceModifier.size(ctrlPlayIconSize)
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(ctrlSpacing))

                        // Next Pill Cap (Equal width, 7dp facing corner)
                        Box(
                            modifier = GlanceModifier
                                .width(ctrlBtnWidth)
                                .height(ctrlBtnHeight)
                                .background(ImageProvider(R.drawable.widget_ctrl_next_pill))
                                .clickable(actionRunCallback<NextActionCallback>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_next),
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
                            .background(ImageProvider(R.drawable.widget_border_rounded_24))
                    ) {}
                }
            }
        }
    }
}

class FullAlbumArtWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FullAlbumArtWidget()
}
