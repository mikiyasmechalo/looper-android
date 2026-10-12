package com.example.looper_android.ui.widgets

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Sun

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetType = "COMPACT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        widgetType = intent.getStringExtra("widget_type") ?: "COMPACT"
        val initialConfig = WidgetSettingsManager.getConfig(this, widgetType, appWidgetId)

        setContent {
            WidgetConfigScreen(
                initialConfig = initialConfig,
                widgetType = widgetType,
                onSave = { savedConfig ->
                    WidgetSettingsManager.saveConfig(this, widgetType, appWidgetId, savedConfig)
                    WidgetStateRepository.updatePlayPauseOptimistic(this, WidgetStateRepository.state.value.isPlaying)

                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    setResult(RESULT_OK, resultValue)
                    finish()
                },
                onCancel = {
                    finish()
                }
            )
        }
    }
}

fun extractLiveWallpaperBitmap(context: Context): Bitmap? {
    try {
        val wm = WallpaperManager.getInstance(context)
        val drawable: Drawable? = try {
            wm.drawable ?: wm.peekDrawable()
        } catch (_: SecurityException) {
            wm.peekDrawable()
        }

        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            return drawable.bitmap
        }

        if (drawable != null) {
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 600
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1000
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bmp
        }
    } catch (_: Exception) {}
    return null
}

@Composable
fun WidgetConfigScreen(
    initialConfig: WidgetConfig,
    widgetType: String,
    onSave: (WidgetConfig) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(initialConfig.themeMode) }
    var transparency by remember { mutableFloatStateOf(initialConfig.transparency) }
    var showFavorite by remember { mutableStateOf(initialConfig.showFavorite) }
    var previewWidgetType by remember { mutableStateOf(widgetType) }

    val liveState by WidgetStateRepository.state.collectAsState()

    // 1. Fetch Real User Device Wallpaper for True Transparency Testing
    val wallpaperBitmap = remember { extractLiveWallpaperBitmap(context) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0C0C0E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Widget Customization",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Text(
                text = "Configure theme, background & transparency",
                color = Color(0x99FFFFFF),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            // Preview Type Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("COMPACT" to "1x4 Bar", "LYRICS" to "Lyrics", "SQUARE" to "2x2 Square").forEach { (type, label) ->
                    val isSelected = previewWidgetType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF55DF69) else Color(0xFF1E1E22))
                            .clickable { previewWidgetType = type }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Interactive Live Preview on Real Desktop Wallpaper
            val currentConfig = WidgetConfig(selectedTheme, transparency, showFavorite)
            val previewCardBg = WidgetSettingsManager.getEffectiveCardColor(
                context,
                currentConfig,
                liveState.cardBackgroundColor
            )
            val previewTextCol = WidgetSettingsManager.getEffectiveTextColor(context, currentConfig)
            val previewSecTextCol = WidgetSettingsManager.getEffectiveSecondaryTextColor(context, currentConfig)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (previewWidgetType == "LYRICS") 180.dp else if (previewWidgetType == "SQUARE") 190.dp else 120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Real Desktop Wallpaper Layer
                if (wallpaperBitmap != null) {
                    Image(
                        bitmap = wallpaperBitmap.asImageBitmap(),
                        contentDescription = "Desktop Wallpaper",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E3C72), Color(0xFF2A5298), Color(0xFF0F2027))
                                )
                            )
                    )
                }

                // Ambient Dark Mask
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x33000000))
                )

                // The Widget Mockup Inside Wallpaper
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (previewWidgetType) {
                        "LYRICS" -> {
                            // Lyrics Widget Mockup
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(previewCardBg)
                                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(22.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (liveState.artBitmap != null) {
                                        Image(
                                            bitmap = liveState.artBitmap!!.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF262626)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.Music, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = liveState.title.ifBlank { "Chasing Stars" }, color = previewTextCol, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(text = liveState.artist.ifBlank { "Alesso, Marshmello" }, color = previewSecTextCol, fontSize = 11.sp, maxLines = 1)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Lucide.SkipBack, null, tint = previewTextCol, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(liveState.accentColor), contentAlignment = Alignment.Center) {
                                            Icon(if (liveState.isPlaying) Lucide.Pause else Lucide.Play, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Lucide.SkipForward, null, tint = previewTextCol, modifier = Modifier.size(18.dp))
                                        if (showFavorite) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x66000000)).padding(4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Lucide.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "♪ When the night turns cold ♪", color = previewSecTextCol, fontSize = 11.sp)
                                    Text(text = "We're still chasing stars in the dark", color = liveState.accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "♪ Never letting go ♪", color = previewSecTextCol, fontSize = 11.sp)
                                }
                            }
                        }
                        "SQUARE" -> {
                            // 2x2 Square Widget Mockup (With Top-Right Star Pill)
                            Box(
                                modifier = Modifier
                                    .size(165.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(previewCardBg)
                                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(22.dp))
                            ) {
                                if (showFavorite) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0x66000000))
                                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (liveState.artBitmap != null) {
                                        Image(
                                            bitmap = liveState.artBitmap!!.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF282828)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.Music, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(text = liveState.title.ifBlank { "Chasing Stars" }, color = previewTextCol, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                                    Text(text = liveState.artist.ifBlank { "Alesso, Marshmello" }, color = previewSecTextCol, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Lucide.SkipBack, null, tint = previewTextCol, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(liveState.accentColor), contentAlignment = Alignment.Center) {
                                            Icon(if (liveState.isPlaying) Lucide.Pause else Lucide.Play, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Lucide.SkipForward, null, tint = previewTextCol, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        else -> {
                            // Compact 4x1 Bar Mockup (46dp Artwork, perfectly proportional)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(previewCardBg)
                                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (liveState.artBitmap != null) {
                                    Image(
                                        bitmap = liveState.artBitmap!!.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(13.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF282828)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Music, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = liveState.title.ifBlank { "Chasing Stars" }, color = previewTextCol, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(text = liveState.artist.ifBlank { "Alesso, Marshmello" }, color = previewSecTextCol, fontSize = 12.sp, maxLines = 1)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Lucide.SkipBack, null, tint = previewTextCol, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(liveState.accentColor), contentAlignment = Alignment.Center) {
                                        Icon(if (liveState.isPlaying) Lucide.Pause else Lucide.Play, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Lucide.SkipForward, null, tint = previewTextCol, modifier = Modifier.size(22.dp))
                                    if (showFavorite) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Lucide.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Background Theme Selector
            Text(
                text = "Background Theme",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionChip(title = "Adaptive", icon = Lucide.Palette, isSelected = selectedTheme == WidgetThemeMode.ADAPTIVE, modifier = Modifier.weight(1f), onClick = { selectedTheme = WidgetThemeMode.ADAPTIVE })
                ThemeOptionChip(title = "Dark", icon = Lucide.Moon, isSelected = selectedTheme == WidgetThemeMode.DARK, modifier = Modifier.weight(1f), onClick = { selectedTheme = WidgetThemeMode.DARK })
                ThemeOptionChip(title = "Light", icon = Lucide.Sun, isSelected = selectedTheme == WidgetThemeMode.LIGHT, modifier = Modifier.weight(1f), onClick = { selectedTheme = WidgetThemeMode.LIGHT })
                ThemeOptionChip(title = "System", icon = Lucide.Smartphone, isSelected = selectedTheme == WidgetThemeMode.SYSTEM, modifier = Modifier.weight(1f), onClick = { selectedTheme = WidgetThemeMode.SYSTEM })
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Background Transparency Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Background Transparency", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "${(transparency * 100).toInt()}%", color = Color(0xFF55DF69), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Slider(
                value = transparency,
                onValueChange = { transparency = it },
                valueRange = 0.0f..1.0f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF55DF69),
                    activeTrackColor = Color(0xFF55DF69),
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Show Favorite Button Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable { showFavorite = !showFavorite }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Show Favorite (Star) Button", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(text = "Add like button to widget controls", color = Color(0x80FFFFFF), fontSize = 12.sp)
                }
                Switch(
                    checked = showFavorite,
                    onCheckedChange = { showFavorite = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF55DF69),
                        uncheckedThumbColor = Color(0x80FFFFFF),
                        uncheckedTrackColor = Color(0xFF333333)
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 6. Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text(text = "Cancel", color = Color.White, fontSize = 15.sp)
                }

                Button(
                    onClick = {
                        onSave(WidgetConfig(selectedTheme, transparency, showFavorite))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF55DF69)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.5f).height(50.dp)
                ) {
                    Text(text = "Save & Apply", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ThemeOptionChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (isSelected) Color(0xFF55DF69) else Color(0xFF1E1E22))
    val contentCol = if (isSelected) Color.Black else Color.White

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentCol, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, color = contentCol, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}
