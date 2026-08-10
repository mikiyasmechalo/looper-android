package com.example.looper_android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.List
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Power
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.User
import com.example.looper_android.ui.theme.BrandAccent
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.theme.LucideIcons
import com.example.looper_android.ui.widgets.PremiumSection

@Composable
fun SettingsCategoryScreen(
    categoryId: String,
    title: String,
    onBackClick: () -> Unit,
    accentColor: Color = BrandAccent
) {
    val playbackViewModel: com.example.looper_android.ui.player.PlaybackViewModel = hiltViewModel()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val isMiniPlayerVisible = playbackState.isSongReady && playbackState.currentSong != null

    val bottomInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = if (isMiniPlayerVisible) 200.dp + bottomInsets else 130.dp + bottomInsets

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.Transparent)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumSection(
                width = 48.dp,
                height = 48.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                onClick = onBackClick
            ) {
                Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily
            )

            Spacer(modifier = Modifier.size(48.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = bottomPadding
            ),
        ) {
            if (categoryId == "about") {
                item { AboutSettingsSection(accentColor = accentColor) }
            } else {
                item {
                    PremiumSection(
                        cornerRadius = 24.dp,
                        backgroundColor = Color(0xFF1E1E1E),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Column {
                            when (categoryId) {
                                "theme" -> ThemeSettingsSection(accentColor = accentColor)
                                "dashboard" -> DashboardSettingsSection(accentColor = accentColor)
                                "playback" -> PlaybackSettingsSection(accentColor = accentColor)
                                "audio_playback" -> AudioPlaybackSettingsSection(accentColor = accentColor)
                                "library" -> LibrarySettingsSection(accentColor = accentColor)
                                "support_us" -> SupportUsSettingsSection(accentColor = accentColor)
                                else -> ThemeSettingsSection(accentColor = accentColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 1. Theme & Appearance
@Composable
private fun ThemeSettingsSection(accentColor: Color) {
    var dynamicTheming by remember { mutableStateOf(true) }
    var disableBlur by remember { mutableStateOf(false) }
    var dynamicAccentColor by remember { mutableStateOf(false) }
    var darkTheme by remember { mutableStateOf(false) }
    var selectedPresetColor by remember { mutableStateOf(Color(0xFF41C25E)) }
    var dynamicLyrics by remember { mutableStateOf(true) }
    var blurredArtworkForLyrics by remember { mutableStateOf(false) }
    var dynamicColorActiveLyrics by remember { mutableStateOf(true) }
    var lyricsAlignment by remember { mutableStateOf("center") }
    var disableSquiggle by remember { mutableStateOf(false) }
    var disableAnimatedDuration by remember { mutableStateOf(false) }
    var showQualityBadge by remember { mutableStateOf(true) }
    var enablePlayerGradient by remember { mutableStateOf(true) }
    var keepBackgroundGradient by remember { mutableStateOf(true) }

    var useCustomFont by remember { mutableStateOf(false) }
    var selectedFontFamily by remember { mutableStateOf("DM Sans") }
    var fontWeightValue by remember { mutableFloatStateOf(400f) }

    var useCustomFontLyrics by remember { mutableStateOf(false) }
    var selectedLyricsFontFamily by remember { mutableStateOf("DM Sans") }
    var lyricsFontWeightValue by remember { mutableFloatStateOf(400f) }

    val fontFamilies = listOf("DM Sans", "Inter", "Roboto", "Jost")

    SettingsSwitchTile(title = "Dynamic Theming", subtitle = "Adapt app colors to album artwork", icon = Lucide.Palette, checked = dynamicTheming, onCheckedChange = { dynamicTheming = it }, accentColor = accentColor)
    
    if (dynamicTheming) {
        AboutDivider()
        SettingsSwitchTile(title = "Disable Blur Effects", subtitle = "Turn off heavy blurs to optimize performance", icon = LucideIcons.Eye, checked = disableBlur, onCheckedChange = { disableBlur = it }, accentColor = accentColor)
    } else {
        AboutDivider()
        SettingsSwitchTile(title = "Dynamic Accent Color", subtitle = "Update only the accent color dynamically from the artwork", icon = LucideIcons.Share2, checked = dynamicAccentColor, onCheckedChange = { dynamicAccentColor = it }, accentColor = accentColor)
        AboutDivider()
        SettingsSwitchTile(title = "Pure Black (OLED)", subtitle = "Use absolute black for backgrounds", icon = LucideIcons.Moon, checked = darkTheme, onCheckedChange = { darkTheme = it }, accentColor = accentColor)

        if (!dynamicAccentColor) {
            AboutDivider()
            SettingsColorPaletteTile(title = "Accent Color", selectedColor = selectedPresetColor, colors = listOf(Color(0xFF41C25E), Color(0xFFF7EAA6), Color(0xFF448AFF)), onColorSelected = { selectedPresetColor = it })
            AboutDivider()
            SettingsOptionTile(title = "Custom Accent Color", subtitle = "Select Custom Color", selectedValue = "#${selectedPresetColor.value.toString(16).take(6).uppercase()}", icon = LucideIcons.Activity, onClick = {}, accentColor = accentColor)
        }
    }
    
    AboutDivider()
    SettingsSwitchTile(title = "Dynamic Lyrics BG", subtitle = "Apply album-art blur to lyrics screen", icon = Lucide.Music, checked = dynamicLyrics, onCheckedChange = { dynamicLyrics = it }, accentColor = accentColor)

    if (!dynamicLyrics) {
        AboutDivider()
        SettingsSwitchTile(title = "Dynamic background only for lyrics", subtitle = "Apply dynamic background strictly to lyrics screen", icon = Lucide.Sparkles, checked = blurredArtworkForLyrics, onCheckedChange = { blurredArtworkForLyrics = it }, accentColor = accentColor)
    }

    if (dynamicTheming || dynamicLyrics) {
        AboutDivider()
        SettingsSwitchTile(title = "Dynamic Color Active Line", subtitle = "Use extracted artwork colors for the currently playing lyrics line", icon = LucideIcons.Layers, checked = dynamicColorActiveLyrics, onCheckedChange = { dynamicColorActiveLyrics = it }, accentColor = accentColor)
    }

    AboutDivider()
    SettingsOptionTile(title = "Lyrics Alignment", subtitle = "Align text positions for scrolling lyrics", selectedValue = lyricsAlignment.replaceFirstChar { it.uppercase() }, icon = Lucide.List, onClick = { lyricsAlignment = when (lyricsAlignment) { "left" -> "center" "center" -> "right" else -> "left" } }, accentColor = accentColor)
    AboutDivider()
    SettingsSwitchTile(title = "Flat Progress Bar", subtitle = "Disable squiggly wave progress bar animation", icon = LucideIcons.Sliders, checked = disableSquiggle, onCheckedChange = { disableSquiggle = it }, accentColor = accentColor)
    AboutDivider()
    SettingsSwitchTile(title = "Plain Timestamps", subtitle = "Use static text instead of rolling animation for progress duration", icon = LucideIcons.Clock, checked = disableAnimatedDuration, onCheckedChange = { disableAnimatedDuration = it }, accentColor = accentColor)
    AboutDivider()
    SettingsSwitchTile(title = "Show Quality Badge", subtitle = "Display audio quality information badge on the now playing screen", icon = Lucide.Info, checked = showQualityBadge, onCheckedChange = { showQualityBadge = it }, accentColor = accentColor)
    AboutDivider()
    SettingsSwitchTile(title = "Music Screen Gradient", subtitle = "Enable the radial accent gradient background on the now playing screen", icon = LucideIcons.Droplet, checked = enablePlayerGradient, onCheckedChange = { enablePlayerGradient = it }, accentColor = accentColor)

    if (enablePlayerGradient) {
        AboutDivider()
        SettingsSwitchTile(title = "Keep Background Gradient", subtitle = "Keep the background gradient across all application screens", icon = Lucide.Folder, checked = keepBackgroundGradient, onCheckedChange = { keepBackgroundGradient = it }, accentColor = accentColor)
    }

    AboutDivider()
    SettingsSwitchTile(title = "Use Custom Font", subtitle = "Use Jost or other custom fonts. Otherwise, DM Sans is used.", icon = Lucide.Palette, checked = useCustomFont, onCheckedChange = { useCustomFont = it }, accentColor = accentColor)
    if (useCustomFont) {
        AboutDivider()
        SettingsOptionTile(title = "Select Font Family", subtitle = "Active font: $selectedFontFamily", selectedValue = selectedFontFamily, icon = Lucide.Palette, onClick = { val nextIdx = (fontFamilies.indexOf(selectedFontFamily) + 1) % fontFamilies.size; selectedFontFamily = fontFamilies[nextIdx] }, accentColor = accentColor)
        AboutDivider()
        SettingsSliderTile(title = "Font Weight Adjustment", subtitle = "Current weight: ${fontWeightValue.toInt()}", value = fontWeightValue, valueRange = 100f..900f, steps = 7, valueFormatter = { "${it.toInt()}" }, icon = LucideIcons.Sliders, onValueChange = { fontWeightValue = it }, accentColor = accentColor)
    }

    AboutDivider()
    SettingsSwitchTile(title = "Use Custom Font for Lyrics", subtitle = "Use custom font and weight for synchronized lyrics view", icon = Lucide.Music, checked = useCustomFontLyrics, onCheckedChange = { useCustomFontLyrics = it }, accentColor = accentColor)
    if (useCustomFontLyrics) {
        AboutDivider()
        SettingsOptionTile(title = "Lyrics Font Family", subtitle = "Active lyrics font: $selectedLyricsFontFamily", selectedValue = selectedLyricsFontFamily, icon = Lucide.Music, onClick = { val nextIdx = (fontFamilies.indexOf(selectedLyricsFontFamily) + 1) % fontFamilies.size; selectedLyricsFontFamily = fontFamilies[nextIdx] }, accentColor = accentColor)
        AboutDivider()
        SettingsSliderTile(title = "Lyrics Font Weight Adjustment", subtitle = "Current weight: ${lyricsFontWeightValue.toInt()}", value = lyricsFontWeightValue, valueRange = 100f..900f, steps = 7, valueFormatter = { "${it.toInt()}" }, icon = LucideIcons.Sliders, onValueChange = { lyricsFontWeightValue = it }, accentColor = accentColor)
    }
}

// 2. Dashboard Settings
@Composable
private fun DashboardSettingsSection(accentColor: Color) {
    var showHomeArtists by remember { mutableStateOf(true) }
    var showHomeAlbums by remember { mutableStateOf(true) }
    var showHomeGenres by remember { mutableStateOf(true) }

    SettingsSwitchTile(title = "Show Artists Row", subtitle = "Display a horizontal list of artists on your Home screen", icon = Lucide.User, checked = showHomeArtists, onCheckedChange = { showHomeArtists = it }, accentColor = accentColor)
    AboutDivider()
    SettingsSwitchTile(title = "Show Albums Row", subtitle = "Display a horizontal list of albums on your Home screen", icon = LucideIcons.Disc, checked = showHomeAlbums, onCheckedChange = { showHomeAlbums = it }, accentColor = accentColor)
    AboutDivider()
    SettingsSwitchTile(title = "Show Genres Row", subtitle = "Display a horizontal list of genres on your Home screen", icon = Lucide.Music, checked = showHomeGenres, onCheckedChange = { showHomeGenres = it }, accentColor = accentColor)
    AboutDivider()
    SettingsActionTile(title = "Reorder Dashboard Sections", subtitle = "Drag and drop to set preferred dashboard order", icon = Lucide.List, onClick = {}, accentColor = accentColor)
}

// 3. Playback Settings
@Composable
private fun PlaybackSettingsSection(accentColor: Color) {
    var language by remember { mutableStateOf("System Default") }
    var enableSlideGesture by remember { mutableStateOf(true) }
    var stopOnTaskRemoved by remember { mutableStateOf(false) }
    var enableInternet by remember { mutableStateOf(true) }
    var downloadArtwork by remember { mutableStateOf(true) }
    val languages = listOf("System Default", "English", "Español", "Français", "Deutsch", "Português")

    SettingsOptionTile(title = "Language", subtitle = "Select application language", selectedValue = language, icon = Lucide.Globe, onClick = { language = languages[(languages.indexOf(language) + 1) % languages.size] }, accentColor = accentColor)
    AboutDivider()
    SettingsSwitchTile(title = "Vertical Motion Effect Player", subtitle = "Swipe down on the expanded player to dismiss it", icon = Lucide.Play, checked = enableSlideGesture, onCheckedChange = { enableSlideGesture = it }, accentColor = accentColor)
    AboutDivider()
    SettingsSwitchTile(title = "Stop Service on App Dismissal", subtitle = "Stop background service and close app when dismissed", icon = LucideIcons.Power, checked = stopOnTaskRemoved, onCheckedChange = { stopOnTaskRemoved = it }, accentColor = accentColor)
    AboutDivider()
    SettingsSwitchTile(title = "Internet Mode", subtitle = "Enable network use for online lyrics & artist art", icon = Lucide.Globe, checked = enableInternet, onCheckedChange = { enableInternet = it }, accentColor = accentColor)
    if (enableInternet) {
        AboutDivider()
        SettingsSwitchTile(title = "Download Missing Artwork", subtitle = "Automatically download high-resolution cover artwork for songs from iTunes", icon = Lucide.Sparkles, checked = downloadArtwork, onCheckedChange = { downloadArtwork = it }, accentColor = accentColor)
    }
}

// 4. Audio Playback Settings
@Composable
private fun AudioPlaybackSettingsSection(accentColor: Color) {
    var fadePlayPauseStop by remember { mutableStateOf(true) }
    var fadeLengthMs by remember { mutableFloatStateOf(300f) }
    var audioFocus by remember { mutableStateOf(true) }
    var shuffle by remember { mutableStateOf(false) }
    var persistQueue by remember { mutableStateOf(true) }
    var audioFocusRequestOnPlay by remember { mutableStateOf(true) }
    var audioFocusReleaseOnPause by remember { mutableStateOf(true) }
    var audioFocusStopOnOtherSession by remember { mutableStateOf(false) }
    var audioFocusRestartOnGain by remember { mutableStateOf(true) }
    var resumeOnStart by remember { mutableStateOf(false) }
    var permanentAudioFocusChange by remember { mutableStateOf(false) }

    SettingsSwitchTile(title = "Fade Play/Pause/Stop", subtitle = "Smoothly ramp volume up when playback starts...", icon = Lucide.Music, checked = fadePlayPauseStop, onCheckedChange = { fadePlayPauseStop = it }, accentColor = accentColor)
    if (fadePlayPauseStop) {
        AboutDivider()
        SettingsSliderTile(title = "Fade Duration", subtitle = "How long the fade takes for play, pause, and stop actions.", value = fadeLengthMs, valueRange = 10f..1000f, steps = 99, valueFormatter = { "${it.toInt()} ms" }, icon = LucideIcons.Sliders, onValueChange = { fadeLengthMs = it }, accentColor = accentColor)
    }
    AboutDivider()
    SettingsSwitchTile(title = "Shuffle", subtitle = "Randomize queue track order", icon = LucideIcons.Shuffle, checked = shuffle, onCheckedChange = { shuffle = it }, accentColor = accentColor)
    AboutDivider()
    SettingsSwitchTile(title = "Persist Queue", subtitle = "Save and restore the current queue and position across app restarts.", icon = Lucide.Save, checked = persistQueue, onCheckedChange = { persistQueue = it }, accentColor = accentColor)
    AboutDivider()
    SettingsSwitchTile(title = "Manage Audio Focus", subtitle = "Respond properly to system audio focus changes.", icon = Lucide.Info, checked = audioFocus, onCheckedChange = { audioFocus = it }, accentColor = accentColor)
    if (audioFocus) {
        AboutDivider()
        SettingsSwitchTile(title = "Request Focus On Play", subtitle = "Request audio focus when playback starts.", icon = Lucide.Play, checked = audioFocusRequestOnPlay, onCheckedChange = { audioFocusRequestOnPlay = it }, accentColor = accentColor)
        AboutDivider()
        SettingsSwitchTile(title = "Release Focus On Pause", subtitle = "Release audio focus when playback pauses.", icon = Lucide.Pause, checked = audioFocusReleaseOnPause, onCheckedChange = { audioFocusReleaseOnPause = it }, accentColor = accentColor)
        AboutDivider()
        SettingsSwitchTile(title = "Stop On Other Session", subtitle = "Stop playback when another app requests audio focus.", icon = Lucide.Square, checked = audioFocusStopOnOtherSession, onCheckedChange = { audioFocusStopOnOtherSession = it }, accentColor = accentColor)
        AboutDivider()
        SettingsSwitchTile(title = "Restart On Gain", subtitle = "Resume playback when audio focus is regained.", icon = Lucide.RefreshCw, checked = audioFocusRestartOnGain, onCheckedChange = { audioFocusRestartOnGain = it }, accentColor = accentColor)
        AboutDivider()
        SettingsSwitchTile(title = "Resume On Start", subtitle = "Automatically resume playback when the app starts.", icon = Lucide.Power, checked = resumeOnStart, onCheckedChange = { resumeOnStart = it }, accentColor = accentColor)
        AboutDivider()
        SettingsSwitchTile(title = "Permanent Focus Change", subtitle = "Pause playback when permanent focus loss occurs.", icon = Lucide.Info, checked = permanentAudioFocusChange, onCheckedChange = { permanentAudioFocusChange = it }, accentColor = accentColor)
    }
}

// 5. Library Settings
@Composable
private fun LibrarySettingsSection(accentColor: Color) {
    SettingsActionTile(title = "Add Folder", subtitle = "Select folder to index music files", icon = Lucide.Plus, onClick = {}, accentColor = accentColor)
    AboutDivider()
    SettingsActionTile(title = "Rescan Library", subtitle = "Update library files indexing", icon = Lucide.RefreshCw, onClick = {}, accentColor = accentColor)
    AboutDivider()
    SettingsActionTile(title = "Reset & Rescan", subtitle = "Remove all songs from your indexed library", icon = LucideIcons.Trash2, onClick = {}, accentColor = accentColor, destructive = true)
}

// 6. About & Maintainers (EXACT 1-TO-1 FLUTTER PORT)
@Composable
private fun AboutSettingsSection(accentColor: Color) {
    Column {
        // Top Section Card
        PremiumSection(
            cornerRadius = 14.dp,
            backgroundColor = Color(0xFF1E1E1E),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val svgImageLoader = remember {
                    coil.ImageLoader.Builder(context)
                        .components {
                            add(coil.decode.SvgDecoder.Factory())
                        }
                        .build()
                }
                coil.compose.AsyncImage(
                    model = "file:///android_asset/main_logo_transparent.svg",
                    contentDescription = null,
                    imageLoader = svgImageLoader,
                    modifier = Modifier.height(36.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(
                            Color.Transparent,
                            RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 10.dp, bottomEnd = 10.dp)
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 10.dp, bottomEnd = 10.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Version 2.0.00",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DmSansFontFamily
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = LucideIcons.GithubCustom, contentDescription = "GitHub", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Give Star on GitHub (No Card background, exactly like Flutter!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = LucideIcons.Heart, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Give Star on GitHub", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = DmSansFontFamily)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Support the project and show some love!", color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp, fontFamily = DmSansFontFamily)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // FLUTTER ANDROID Header
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = "Maintainers",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Looper Android",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                fontFamily = DmSansFontFamily
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Flutter Android Card
        PremiumSection(
            cornerRadius = 14.dp,
            backgroundColor = Color(0xFF1E1E1E),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                AboutMaintainerRow(initial = "J", name = "Jhon Doe", role = "Creator & Android Lead")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = "Original Flutter app",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                fontFamily = DmSansFontFamily
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Original Flutter Maintainers Card
        PremiumSection(
            cornerRadius = 14.dp,
            backgroundColor = Color(0xFF1E1E1E),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                AboutMaintainerRow(initial = "N", name = "Nilesh Suthar", role = "Creator and Maintainer")
                AboutDivider()
                AboutMaintainerRow(initial = "K", name = "Krn.", role = "Designer and Maintainer")
                AboutDivider()
                AboutMaintainerRow(initial = "M", name = "Madan Suthar", role = "Active Contributor")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // App Info & Privacy Card
        PremiumSection(
            cornerRadius = 14.dp,
            backgroundColor = Color(0xFF1E1E1E),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "APP INFO & PRIVACY",
                    color = accentColor.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                AboutInfoSubTile(icon = Lucide.Music, title = "Core Purpose", description = "Looper Player is an offline-first, high-fidelity audio player designed for music enthusiasts who want absolute control over their local library, gapless playback, and fluid, synchronized lyrics scrolling.")
                Spacer(modifier = Modifier.height(16.dp))
                AboutInfoSubTile(icon = LucideIcons.UserCheck, title = "Why Permissions are Used", description = "• Storage / Media Access: Required to discover, read, and index local audio tracks stored on your device.\n• Notifications: Required to display active playback control widgets in your status bar and system drawer.")
                Spacer(modifier = Modifier.height(16.dp))
                AboutInfoSubTile(icon = Lucide.Globe, title = "Why Internet is Used", description = "• Dynamic Lyrics Syncing: Used solely to securely fetch and download synchronized lyrics (LRC formats) from online databases. No personal data, settings, or media files are ever uploaded or shared.")
                Spacer(modifier = Modifier.height(16.dp))
                AboutInfoSubTile(icon = Lucide.Lock, title = "Privacy & Safety", description = "100% private and offline-first. Your tracks, playback history, favorites, and configuration stay strictly inside a secure local database on your device. We do not track, collect, or share your usage data or preferences.")
            }
        }
    }
}

// 7. Support Us
@Composable
private fun SupportUsSettingsSection(accentColor: Color) {
    SettingsActionTile(title = "Support Development", subtitle = "Help keep Looper Player alive & open-source", icon = Lucide.Coffee, onClick = {}, accentColor = accentColor)
}

// --- EXACT FLUTTER ABOUT COMPONENTS ---

@Composable
private fun AboutMaintainerRow(
    initial: String,
    name: String,
    role: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = DmSansFontFamily)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = role, color = Color.White.copy(alpha = 0.54f), fontSize = 12.sp, fontFamily = DmSansFontFamily)
        }
        
        Icon(imageVector = LucideIcons.GithubCustom, contentDescription = "GitHub", tint = Color.White.copy(alpha = 0.54f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Icon(imageVector = Lucide.Send, contentDescription = "Telegram", tint = Color.White.copy(alpha = 0.54f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AboutInfoSubTile(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 2.dp).size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = DmSansFontFamily)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, lineHeight = 18.sp, fontFamily = DmSansFontFamily)
        }
    }
}

@Composable
fun AboutDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 72.dp)
            .background(Color.White.copy(alpha = 0.1f))
    )
}
