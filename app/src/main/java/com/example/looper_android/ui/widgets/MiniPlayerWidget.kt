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

class MiniPlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetStateRepository.ensureInitialized(context)
        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) { -1 }

        provideContent {
            val state by WidgetStateRepository.state.collectAsState()
            val configVersion by WidgetSettingsManager.configVersion.collectAsState()
            val config = WidgetSettingsManager.getConfig(context, "MINI_PLAYER", appWidgetId)

            val cardBg = WidgetSettingsManager.getEffectiveCardColor(context, config, state.cardBackgroundColor)
            val textCol = WidgetSettingsManager.getEffectiveTextColor(context, config)
            val secTextCol = WidgetSettingsManager.getEffectiveSecondaryTextColor(context, config)

            val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val (leftBgRes, rightBgRes) = if (config.showOutline) {
                when {
                    config.themeMode == WidgetThemeMode.LIGHT ||
                        (config.themeMode == WidgetThemeMode.SYSTEM && !isSystemDark) -> {
                        R.drawable.widget_mini_player_left_light to R.drawable.widget_mini_player_right_light
                    }
                    config.transparency >= 0.6f -> {
                        R.drawable.widget_mini_player_left_trans to R.drawable.widget_mini_player_right_trans
                    }
                    config.transparency <= 0.05f -> {
                        R.drawable.widget_mini_player_left_solid to R.drawable.widget_mini_player_right_solid
                    }
                    else -> {
                        R.drawable.widget_mini_player_left to R.drawable.widget_mini_player_right
                    }
                }
            } else {
                when {
                    config.themeMode == WidgetThemeMode.LIGHT ||
                        (config.themeMode == WidgetThemeMode.SYSTEM && !isSystemDark) -> {
                        R.drawable.widget_mini_player_left_light_no_border to R.drawable.widget_mini_player_right_light_no_border
                    }
                    config.transparency >= 0.6f -> {
                        R.drawable.widget_mini_player_left_trans_no_border to R.drawable.widget_mini_player_right_trans_no_border
                    }
                    config.transparency <= 0.05f -> {
                        R.drawable.widget_mini_player_left_solid_no_border to R.drawable.widget_mini_player_right_solid_no_border
                    }
                    else -> {
                        R.drawable.widget_mini_player_left_no_border to R.drawable.widget_mini_player_right_no_border
                    }
                }
            }

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Reverted to true Mini Player 2-segment shape (36dp outer curves, 12dp inner facing curves)
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(80.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Section: Song Info (Asymmetric corners: 36dp start, 12dp end facing button)
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(80.dp)
                            .background(ImageProvider(leftBgRes))
                            .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java)))
                            .padding(start = 10.dp, end = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Album Art (Enlarged to 62dp, 31dp corner radius -> perfect circle)
                            if (state.artBitmap != null) {
                                Image(
                                    provider = ImageProvider(state.artBitmap!!),
                                    contentDescription = "Artwork",
                                    modifier = GlanceModifier
                                        .size(62.dp)
                                        .cornerRadius(31.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = GlanceModifier
                                        .size(62.dp)
                                        .cornerRadius(31.dp)
                                        .background(ColorProvider(Color(0xFF262626))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        provider = ImageProvider(R.drawable.ic_widget_music),
                                        contentDescription = null,
                                        modifier = GlanceModifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = GlanceModifier.width(10.dp))

                            // Metadata Column: Title & Artist (Slightly larger font sizes)
                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.title,
                                    style = TextStyle(
                                        color = ColorProvider(textCol),
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = state.artist,
                                    style = TextStyle(
                                        color = ColorProvider(secTextCol),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Right Section: Play/Pause Button (Asymmetric corners: 12dp start facing song info, 36dp end)
                    Box(
                        modifier = GlanceModifier
                            .width(76.dp)
                            .height(80.dp)
                            .background(ImageProvider(rightBgRes))
                            .clickable(actionRunCallback<PlayPauseActionCallback>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                if (state.isPlaying) R.drawable.ic_widget_pause
                                else R.drawable.ic_widget_play
                            ),
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = GlanceModifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

class MiniPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MiniPlayerWidget()
}
