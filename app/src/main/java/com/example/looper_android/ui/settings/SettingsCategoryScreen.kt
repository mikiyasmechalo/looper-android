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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Clock
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
import com.composables.icons.lucide.PaintBucket
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Droplet
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Baseline
import com.composables.icons.lucide.Type
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

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
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
            } else if (categoryId == "theme") {
                item { ThemeSettingsSection(accentColor = accentColor) }
            } else if (categoryId == "playback") {
                item { LanguageAndServicesSection(accentColor = accentColor) }
            } else if (categoryId == "audio_playback") {
                item { AudioPlaybackSettingsSection(accentColor = accentColor) }
            } else if (categoryId == "library") {
                item { LibrarySettingsSection(accentColor = accentColor) }
            } else {
                item {
                    PremiumSection(
                        cornerRadius = 24.dp,
                        backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Column {
                            when (categoryId) {
                                "dashboard" -> DashboardSettingsSection(accentColor = accentColor)
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

// 1. Theme
@Composable
private fun ThemeSettingsSection(
    viewModel: SettingsViewModel = hiltViewModel(),
    accentColor: Color
) {
    val dynamicTheming by viewModel.dynamicTheming.collectAsState()
    val disableBlur by viewModel.disableBlur.collectAsState()
    val dynamicAccentColor by viewModel.dynamicAccentColor.collectAsState()
    val darkTheme by viewModel.darkTheme.collectAsState()
    val customAccentColorInt by viewModel.customAccentColor.collectAsState()
    val dynamicLyrics by viewModel.dynamicLyrics.collectAsState()
    val blurredArtworkForLyrics by viewModel.blurredArtworkForLyrics.collectAsState()
    val dynamicColorActiveLyrics by viewModel.dynamicColorActiveLyrics.collectAsState()
    val disableLyricFontScaling by viewModel.disableLyricFontScaling.collectAsState()
    val lyricsAlignment by viewModel.lyricsAlignment.collectAsState()
    val lyricsScreenDarkness by viewModel.lyricsScreenDarkness.collectAsState()
    val primaryLyricsProvider by viewModel.primaryLyricsProvider.collectAsState()
    val disableSquiggle by viewModel.disableSquiggle.collectAsState()
    val disableAnimatedDuration by viewModel.disableAnimatedDuration.collectAsState()
    val showQualityBadge by viewModel.showQualityBadge.collectAsState()
    val enablePlayerGradient by viewModel.enablePlayerGradient.collectAsState()
    val keepBackgroundGradient by viewModel.keepBackgroundGradient.collectAsState()

    val selectedPresetColor = remember(customAccentColorInt) { Color(customAccentColorInt) }

    Column {
        // --- 1. COLOR SUB-SECTION ---
        SettingsSubHeader(title = "Color", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Column {
                SettingsSwitchTile(
                    title = "Dynamic Theming",
                    subtitle = "Adapt app colors to album artwork",
                    icon = Lucide.Palette,
                    checked = dynamicTheming,
                    onCheckedChange = { viewModel.setDynamicTheming(it) },
                    accentColor = accentColor
                )
                
                if (dynamicTheming) {
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Disable Blur Effects",
                        subtitle = "Turn off heavy blurs to optimize performance",
                        icon = LucideIcons.Eye,
                        checked = disableBlur,
                        onCheckedChange = { viewModel.setDisableBlur(it) },
                        accentColor = accentColor
                    )
                } else {
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Dynamic Accent Color",
                        subtitle = "Update only the accent color dynamically from the artwork",
                        icon = Lucide.PaintBucket,
                        checked = dynamicAccentColor,
                        onCheckedChange = { viewModel.setDynamicAccentColor(it) },
                        accentColor = accentColor
                    )

                    if (!dynamicAccentColor) {
                        AboutDivider()
                        SettingsColorPaletteTile(
                            title = "Accent Color",
                            selectedColor = selectedPresetColor,
                            colors = listOf(Color(0xFF41C25E), Color(0xFFF7EAA6), Color(0xFF448AFF), Color(0xFFFF5252), Color(0xFFAB47BC)),
                            onColorSelected = { viewModel.setCustomAccentColor(it.toArgb()) }
                        )
                    }

                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Pure Black (OLED)",
                        subtitle = "Use absolute black (#000000) for backgrounds",
                        icon = Lucide.Moon,
                        checked = darkTheme,
                        onCheckedChange = { viewModel.setDarkTheme(it) },
                        accentColor = accentColor
                    )
                    
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Music Screen Gradient",
                        subtitle = "Enable the radial accent gradient background on the now playing screen",
                        icon = Lucide.Droplet,
                        checked = enablePlayerGradient,
                        onCheckedChange = { viewModel.setEnablePlayerGradient(it) },
                        accentColor = accentColor
                    )

                    if (enablePlayerGradient) {
                        AboutDivider()
                        SettingsSwitchTile(
                            title = "Keep Background Gradient",
                            subtitle = "Keep the background gradient across all application screens",
                            icon = Lucide.Layers,
                            checked = keepBackgroundGradient,
                            onCheckedChange = { viewModel.setKeepBackgroundGradient(it) },
                            accentColor = accentColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. LYRICS SUB-SECTION ---
        SettingsSubHeader(title = "Lyrics", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Column {
                SettingsSwitchTile(
                    title = "Dynamic Lyrics BG",
                    subtitle = "Apply album-art blur to lyrics screen",
                    icon = Lucide.Music,
                    checked = dynamicLyrics,
                    onCheckedChange = { viewModel.setDynamicLyrics(it) },
                    accentColor = accentColor
                )

                if (dynamicLyrics) {
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Use blurred album art for lyrics background",
                        subtitle = "Apply the dynamic theme style blurred artwork to the lyrics screen",
                        icon = Lucide.Sparkles,
                        checked = blurredArtworkForLyrics,
                        onCheckedChange = { viewModel.setBlurredArtworkForLyrics(it) },
                        accentColor = accentColor
                    )
                }

                if (dynamicTheming || dynamicLyrics) {
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Dynamic Color Active Line",
                        subtitle = "Use extracted artwork colors for the currently playing lyrics line",
                        icon = Lucide.Baseline,
                        checked = dynamicColorActiveLyrics,
                        onCheckedChange = { viewModel.setDynamicColorActiveLyrics(it) },
                        accentColor = accentColor
                    )
                }

                AboutDivider()
                SettingsSwitchTile(
                    title = "Static Lyric Font Size",
                    subtitle = "Keep active and inactive lyric lines at the same font size",
                    icon = Lucide.Type,
                    checked = disableLyricFontScaling,
                    onCheckedChange = { viewModel.setDisableLyricFontScaling(it) },
                    accentColor = accentColor
                )

                AboutDivider()
                SettingsSegmentedTile(
                    title = "Lyrics Alignment",
                    subtitle = "Align text position for scrolling lyrics",
                    options = listOf("Left" to "left", "Center" to "center", "Right" to "right"),
                    selectedOption = lyricsAlignment,
                    onOptionSelected = { viewModel.setLyricsAlignment(it) },
                    accentColor = accentColor
                )

                AboutDivider()
                SettingsSliderTile(
                    title = "Lyric Screen Darkness",
                    subtitle = "Darkness overlay opacity for lyrics background artwork",
                    value = lyricsScreenDarkness,
                    valueRange = 0f..1f,
                    steps = 19,
                    valueFormatter = { "${(it * 100).toInt()}%" },
                    icon = LucideIcons.Eye,
                    onValueChange = { viewModel.setLyricsScreenDarkness(it) },
                    accentColor = accentColor
                )

                AboutDivider()
                SettingsDropdownTile(
                    title = "Online Lyrics Provider",
                    subtitle = "Choose online service for fetching synchronized lyrics",
                    icon = Lucide.Globe,
                    options = listOf("LRCLIB" to "lrclib", "Genius" to "genius", "Musixmatch" to "musixmatch", "Netease" to "netease"),
                    selectedOption = primaryLyricsProvider,
                    onOptionSelected = { viewModel.setPrimaryLyricsProvider(it) },
                    accentColor = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. PLAYER SUB-SECTION ---
        SettingsSubHeader(title = "Player", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Column {
                SettingsSwitchTile(
                    title = "Flat Progress Bar",
                    subtitle = "Disable squiggly wave progress bar animation",
                    icon = LucideIcons.Sliders,
                    checked = disableSquiggle,
                    onCheckedChange = { viewModel.setDisableSquiggle(it) },
                    accentColor = accentColor
                )

                AboutDivider()
                SettingsSwitchTile(
                    title = "Plain Timestamps",
                    subtitle = "Disable animated rolling duration numbers",
                    icon = Lucide.Clock,
                    checked = disableAnimatedDuration,
                    onCheckedChange = { viewModel.setDisableAnimatedDuration(it) },
                    accentColor = accentColor
                )

                AboutDivider()
                SettingsSwitchTile(
                    title = "Show Quality Badge",
                    subtitle = "Display audio quality information badge on the now playing screen",
                    icon = Lucide.Info,
                    checked = showQualityBadge,
                    onCheckedChange = { viewModel.setShowQualityBadge(it) },
                    accentColor = accentColor
                )
            }
        }
    }
}

// 2. Dashboard Settings
@Composable
private fun DashboardSettingsSection(
    viewModel: SettingsViewModel = hiltViewModel(),
    accentColor: Color
) {
    val defaultScreen by viewModel.defaultScreen.collectAsState()
    val showHomeArtists by viewModel.showHomeArtists.collectAsState()
    val showHomeAlbums by viewModel.showHomeAlbums.collectAsState()
    val showHomeGenres by viewModel.showHomeGenres.collectAsState()
    val homeSectionsOrder by viewModel.homeSectionsOrder.collectAsState()
    var showReorderDialog by remember { mutableStateOf(false) }

    SettingsOptionTile(
        title = "Default Startup Screen",
        subtitle = "Select which tab opens when launching Looper",
        selectedValue = if (defaultScreen == "songs") "All Songs" else "Home",
        icon = if (defaultScreen == "songs") Lucide.Music else Lucide.List,
        onClick = {
            val newScreen = if (defaultScreen == "songs") "home" else "songs"
            viewModel.setDefaultScreen(newScreen)
        },
        accentColor = accentColor
    )
    AboutDivider()
    SettingsSwitchTile(
        title = "Show Artists Row",
        subtitle = "Display a horizontal list of artists on your Home screen",
        icon = Lucide.User,
        checked = showHomeArtists,
        onCheckedChange = { viewModel.setShowHomeArtists(it) },
        accentColor = accentColor
    )
    AboutDivider()
    SettingsSwitchTile(
        title = "Show Albums Row",
        subtitle = "Display a horizontal list of albums on your Home screen",
        icon = LucideIcons.Disc,
        checked = showHomeAlbums,
        onCheckedChange = { viewModel.setShowHomeAlbums(it) },
        accentColor = accentColor
    )
    AboutDivider()
    SettingsSwitchTile(
        title = "Show Genres Row",
        subtitle = "Display a horizontal list of genres on your Home screen",
        icon = Lucide.Music,
        checked = showHomeGenres,
        onCheckedChange = { viewModel.setShowHomeGenres(it) },
        accentColor = accentColor
    )
    AboutDivider()
    SettingsActionTile(
        title = "Reorder Dashboard Sections",
        subtitle = "Customize the display order of sections on your Home dashboard",
        icon = Lucide.List,
        onClick = { showReorderDialog = true },
        accentColor = accentColor
    )

    if (showReorderDialog) {
        val sectionTitles = mapOf(
            "quick_picks" to "Quick Picks",
            "songs" to "Top Songs",
            "artists" to "Top Artists",
            "albums" to "Top Albums",
            "genres" to "Top Genres"
        )
        val sectionIcons = mapOf(
            "quick_picks" to Lucide.Sparkles,
            "songs" to Lucide.Music,
            "artists" to Lucide.User,
            "albums" to LucideIcons.Disc,
            "genres" to Lucide.Layers
        )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showReorderDialog = false },
            title = {
                Text(
                    text = "Reorder Sections",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Use the buttons to arrange sections in your preferred order:",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontFamily = DmSansFontFamily,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    homeSectionsOrder.forEachIndexed { index, sectionKey ->
                        val title = sectionTitles[sectionKey] ?: sectionKey
                        val icon = sectionIcons[sectionKey] ?: Lucide.List
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${index + 1}. $title",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = DmSansFontFamily,
                                modifier = Modifier.weight(1f)
                            )
                            // Move Up
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (index > 0) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable(enabled = index > 0) {
                                        viewModel.moveHomeSection(index, index - 1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("▲", color = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            // Move Down
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (index < homeSectionsOrder.size - 1) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable(enabled = index < homeSectionsOrder.size - 1) {
                                        viewModel.moveHomeSection(index, index + 1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("▼", color = if (index < homeSectionsOrder.size - 1) Color.White else Color.White.copy(alpha = 0.2f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showReorderDialog = false }) {
                    Text("Done", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.resetHomeSectionsOrder()
                    }
                ) {
                    Text("Reset Default", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// 3. Language & Services Settings
@Composable
private fun LanguageAndServicesSection(
    viewModel: SettingsViewModel = hiltViewModel(),
    accentColor: Color
) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val enableSlideGesture by viewModel.enableSlideGesture.collectAsState()
    val stopOnTaskRemoved by viewModel.stopOnTaskRemoved.collectAsState()
    val enableInternet by viewModel.enableInternet.collectAsState()
    val downloadArtwork by viewModel.downloadArtwork.collectAsState()

    val languages = listOf("System Default", "English", "Español", "Français", "Deutsch", "Português")

    Column {
        // --- 1. LANGUAGE ---
        SettingsSubHeader(title = "Language", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            SettingsOptionTile(
                title = "Language",
                subtitle = "Select application interface language",
                selectedValue = appLanguage,
                icon = Lucide.Globe,
                onClick = {
                    val nextIdx = (languages.indexOf(appLanguage) + 1) % languages.size
                    viewModel.setAppLanguage(languages[nextIdx])
                },
                accentColor = accentColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. SERVICES & GESTURES ---
        SettingsSubHeader(title = "Services & Gestures", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Column {
                SettingsSwitchTile(
                    title = "Vertical Motion Effect Player",
                    subtitle = "Swipe down on the expanded player to smoothly dismiss it",
                    icon = Lucide.Play,
                    checked = enableSlideGesture,
                    onCheckedChange = { viewModel.setEnableSlideGesture(it) },
                    accentColor = accentColor
                )
                AboutDivider()
                SettingsSwitchTile(
                    title = "Stop Service on App Dismissal",
                    subtitle = "Stop playback service and close app when swiped away from recent apps",
                    icon = LucideIcons.Power,
                    checked = stopOnTaskRemoved,
                    onCheckedChange = { viewModel.setStopOnTaskRemoved(it) },
                    accentColor = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. NETWORK SERVICES ---
        SettingsSubHeader(title = "Network Services", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Column {
                SettingsSwitchTile(
                    title = "Internet Mode",
                    subtitle = "Allow network access for online lyrics and high-res cover art",
                    icon = Lucide.Globe,
                    checked = enableInternet,
                    onCheckedChange = { viewModel.setEnableInternet(it) },
                    accentColor = accentColor
                )
                if (enableInternet) {
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Download Missing Artwork",
                        subtitle = "Automatically download high-resolution cover artwork for songs from iTunes",
                        icon = Lucide.Sparkles,
                        checked = downloadArtwork,
                        onCheckedChange = { viewModel.setDownloadArtwork(it) },
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

// 4. Audio & Playback Settings
@Composable
private fun AudioPlaybackSettingsSection(
    viewModel: SettingsViewModel = hiltViewModel(),
    accentColor: Color
) {
    val fadePlayPauseStop by viewModel.fadePlayPauseStop.collectAsState()
    val fadeLengthMs by viewModel.fadeLengthMs.collectAsState()
    val pauseOnDisconnect by viewModel.pauseOnDisconnect.collectAsState()
    val resumeOnlyIfPausedByDisconnect by viewModel.resumeOnlyIfPausedByDisconnect.collectAsState()
    val resumeOnBluetoothConnect by viewModel.resumeOnBluetoothConnect.collectAsState()
    val resumeOnWiredConnect by viewModel.resumeOnWiredConnect.collectAsState()
    val audioFocus by viewModel.audioFocus.collectAsState()
    val audioFocusRequestOnPlay by viewModel.audioFocusRequestOnPlay.collectAsState()
    val audioFocusReleaseOnPause by viewModel.audioFocusReleaseOnPause.collectAsState()
    val audioFocusStopOnOtherSession by viewModel.audioFocusStopOnOtherSession.collectAsState()
    val audioFocusRestartOnGain by viewModel.audioFocusRestartOnGain.collectAsState()
    val resumeOnStart by viewModel.resumeOnStart.collectAsState()
    val permanentAudioFocusChange by viewModel.permanentAudioFocusChange.collectAsState()
    val persistQueue by viewModel.persistQueue.collectAsState()

    Column {
        // --- 1. VOLUME & TRANSITIONS ---
        SettingsSubHeader(title = "Volume & Transitions", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Column {
                SettingsSwitchTile(
                    title = "Fade Play/Pause/Stop",
                    subtitle = "Smoothly ramp volume up when playback starts and down when paused",
                    icon = Lucide.Music,
                    checked = fadePlayPauseStop,
                    onCheckedChange = { viewModel.setFadePlayPauseStop(it) },
                    accentColor = accentColor
                )
                if (fadePlayPauseStop) {
                    AboutDivider()
                    SettingsSliderTile(
                        title = "Fade Duration",
                        subtitle = "Length of the volume fade transition",
                        value = fadeLengthMs,
                        valueRange = 10f..1000f,
                        steps = 0,
                        valueFormatter = { "${it.toInt()} ms" },
                        icon = LucideIcons.Sliders,
                        onValueChange = { viewModel.setFadeLengthMs((it / 10).roundToInt() * 10f) },
                        accentColor = accentColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. HEADPHONE ACTIONS ---
        SettingsSubHeader(title = "Headphone Actions", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Column {
                SettingsSwitchTile(
                    title = "Pause on Disconnect",
                    subtitle = "Pause playback when wired or Bluetooth headphones are disconnected",
                    icon = Lucide.Pause,
                    checked = pauseOnDisconnect,
                    onCheckedChange = { viewModel.setPauseOnDisconnect(it) },
                    accentColor = accentColor
                )
                AboutDivider()
                SettingsSwitchTile(
                    title = "Resume Only If Paused by Disconnection",
                    subtitle = "Only auto-resume if music was previously paused by disconnection",
                    icon = Lucide.RefreshCw,
                    checked = resumeOnlyIfPausedByDisconnect,
                    onCheckedChange = { viewModel.setResumeOnlyIfPausedByDisconnect(it) },
                    accentColor = accentColor
                )
                AboutDivider()
                SettingsSwitchTile(
                    title = "Resume on Bluetooth Connect",
                    subtitle = "Automatically resume playback when Bluetooth headphones or speaker connect",
                    icon = Lucide.Play,
                    checked = resumeOnBluetoothConnect,
                    onCheckedChange = { viewModel.setResumeOnBluetoothConnect(it) },
                    accentColor = accentColor
                )
                AboutDivider()
                SettingsSwitchTile(
                    title = "Resume on Wired Earphones Connect",
                    subtitle = "Automatically resume playback when wired earphones are plugged in",
                    icon = Lucide.Play,
                    checked = resumeOnWiredConnect,
                    onCheckedChange = { viewModel.setResumeOnWiredConnect(it) },
                    accentColor = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. AUDIO FOCUS & SESSIONS ---
        SettingsSubHeader(title = "Audio Focus & Sessions", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Column {
                SettingsSwitchTile(
                    title = "Manage Audio Focus",
                    subtitle = "Respond properly to system audio focus and external player requests",
                    icon = Lucide.Info,
                    checked = audioFocus,
                    onCheckedChange = { viewModel.setAudioFocus(it) },
                    accentColor = accentColor
                )
                if (audioFocus) {
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Request Focus On Play",
                        subtitle = "Request audio focus from the system when starting playback",
                        icon = Lucide.Play,
                        checked = audioFocusRequestOnPlay,
                        onCheckedChange = { viewModel.setAudioFocusRequestOnPlay(it) },
                        accentColor = accentColor
                    )
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Release Focus On Pause",
                        subtitle = "Release audio focus back to the system when playback is paused",
                        icon = Lucide.Pause,
                        checked = audioFocusReleaseOnPause,
                        onCheckedChange = { viewModel.setAudioFocusReleaseOnPause(it) },
                        accentColor = accentColor
                    )
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Stop On Other Session",
                        subtitle = "Stop playback service when another media app starts playing",
                        icon = Lucide.Square,
                        checked = audioFocusStopOnOtherSession,
                        onCheckedChange = { viewModel.setAudioFocusStopOnOtherSession(it) },
                        accentColor = accentColor
                    )
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Restart On Gain",
                        subtitle = "Automatically resume playback when audio focus is regained",
                        icon = Lucide.RefreshCw,
                        checked = audioFocusRestartOnGain,
                        onCheckedChange = { viewModel.setAudioFocusRestartOnGain(it) },
                        accentColor = accentColor
                    )
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Resume on App Launch",
                        subtitle = "Automatically resume playback of your last track when opening app",
                        icon = Lucide.Power,
                        checked = resumeOnStart,
                        onCheckedChange = { viewModel.setResumeOnStart(it) },
                        accentColor = accentColor
                    )
                    AboutDivider()
                    SettingsSwitchTile(
                        title = "Permanent Focus Change Pause",
                        subtitle = "Pause playback when permanent audio focus loss occurs",
                        icon = Lucide.Info,
                        checked = permanentAudioFocusChange,
                        onCheckedChange = { viewModel.setPermanentAudioFocusChange(it) },
                        accentColor = accentColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4. PLAYBACK STATE ---
        SettingsSubHeader(title = "Playback State", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            SettingsSwitchTile(
                title = "Persist Queue & Timeline",
                subtitle = "Save and restore the current queue and position across app restarts",
                icon = Lucide.Save,
                checked = persistQueue,
                onCheckedChange = { viewModel.setPersistQueue(it) },
                accentColor = accentColor
            )
        }
    }
}

// 5. Library Settings
@Composable
private fun LibrarySettingsSection(
    viewModel: SettingsViewModel = hiltViewModel(),
    accentColor: Color
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val useAlbumArtist by viewModel.useAlbumArtist.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Column {
        // --- 1. ORGANIZATION & TAGS ---
        SettingsSubHeader(title = "Organization & Tags", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            SettingsSwitchTile(
                title = "Use Album Artist for Artists",
                subtitle = "Group and identify artists on collections using the Album Artist tag field",
                icon = Lucide.User,
                checked = useAlbumArtist,
                onCheckedChange = { viewModel.setUseAlbumArtist(it) },
                accentColor = accentColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. OFFLINE DATA ---
        SettingsSubHeader(title = "Offline Data", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            SettingsActionTile(
                title = "Sync Lyrics (Offline)",
                subtitle = "Download and cache missing synchronized lyrics for offline playback",
                icon = Lucide.Droplet,
                onClick = {
                    viewModel.syncLyricsOffline()
                    android.widget.Toast.makeText(context, "Downloading lyrics for offline...", android.widget.Toast.LENGTH_SHORT).show()
                },
                accentColor = accentColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. MAINTENANCE ---
        SettingsSubHeader(title = "Maintenance", accentColor = accentColor)
        PremiumSection(
            cornerRadius = 24.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Column {
                SettingsActionTile(
                    title = "Rescan Library",
                    subtitle = "Scan device storage and update indexed music files",
                    icon = Lucide.RefreshCw,
                    onClick = {
                        viewModel.rescanLibrary()
                        android.widget.Toast.makeText(context, "Scanning library...", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    accentColor = accentColor
                )
                AboutDivider()
                SettingsActionTile(
                    title = "Reset & Rescan",
                    subtitle = "Clear database and rebuild index (preserves downloaded artist images)",
                    icon = LucideIcons.Trash2,
                    onClick = { showResetDialog = true },
                    accentColor = accentColor,
                    destructive = true
                )
            }
        }
    }

    if (showResetDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Library?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily
                )
            },
            text = {
                Text(
                    text = "This will clear all indexed music data and rebuild your library. Your downloaded artist artwork will be safely preserved.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = DmSansFontFamily
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetLibrary()
                        viewModel.rescanLibrary()
                        android.widget.Toast.makeText(context, "Library reset and scanning...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// 6. About & Maintainers (EXACT 1-TO-1 FLUTTER PORT)
@Composable
private fun AboutSettingsSection(accentColor: Color) {
    Column {
        // Top Section Card
        PremiumSection(
            cornerRadius = 14.dp,
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
            backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
