package com.example.looper_android.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Database
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.List
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import com.example.looper_android.ui.theme.BrandAccent
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.theme.LucideIcons
import com.example.looper_android.ui.widgets.PremiumSection

private data class SettingCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private data class SearchableSetting(
    val categoryId: String,
    val categoryName: String,
    val title: String,
    val subtitle: String
)

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    accentColor: Color = BrandAccent
) {
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember {
        listOf(
            SettingCategory("theme", "Theme", "Customize Colors, Theme & OLED", Lucide.Palette),
            SettingCategory("dashboard", "Home Screen", "Home Dashboard & Layout Settings", Lucide.List),
            SettingCategory("playback", "Language & Services", "Language, Gestures, Background Tasks & Network Services", Lucide.Globe),
            SettingCategory("audio_playback", "Audio & Playback", "Crossfade, Equalizer & Audio Engine", Lucide.Music),
            SettingCategory("library", "Music Library", "Library Folders, Scanning & Rescan", Lucide.Database),
            SettingCategory("about", "About & Maintainers", "App Details, Version & Creators", Lucide.Info),
            SettingCategory("support_us", "Support Us", "Buy Me a Coffee & Support Development", LucideIcons.Heart)
        )
    }

    val allSearchableItems = remember {
        listOf(
            SearchableSetting("theme", "Theme", "Dynamic Theming", "Adapt app colors and background dynamically from artwork"),
            SearchableSetting("theme", "Theme", "Disable Blur Effects", "Turn off glassmorphism blur effects to boost UI performance"),
            SearchableSetting("theme", "Theme", "Dynamic Accent Color", "Update only the primary accent color dynamically from artwork"),
            SearchableSetting("theme", "Theme", "Pure Black (OLED)", "Use absolute pitch black backgrounds (#000000)"),
            SearchableSetting("theme", "Theme", "Preset Accent Colors", "Choose quick primary brand highlight colors"),
            SearchableSetting("theme", "Theme", "Custom Accent Color", "Select custom primary brand highlight color"),
            SearchableSetting("theme", "Theme", "Dynamic Lyrics Background", "Enable dynamic artwork colors in lyrics view"),
            SearchableSetting("theme", "Theme", "Blurred Artwork for Lyrics", "Show blurred album art as background"),
            SearchableSetting("theme", "Theme", "Dynamic Color Active Lyrics", "Adapt active lyric line text color to contrast"),
            SearchableSetting("theme", "Theme", "Lyrics Alignment", "Choose text alignment for lyrics"),
            SearchableSetting("theme", "Theme", "Flat Progress Bar", "Disable squiggly animated waveform slider"),
            SearchableSetting("theme", "Theme", "Plain Timestamps", "Use static text for timestamps"),
            SearchableSetting("theme", "Theme", "Show Audio Quality Badge", "Display audio quality pill (e.g. FLAC 96kHz / 24-bit)"),
            SearchableSetting("theme", "Theme", "Enable Player Gradient", "Render smooth ambient color gradient behind artwork"),
            SearchableSetting("theme", "Theme", "Keep Background Gradient", "Keep background gradient ambient blur when minimized"),
            SearchableSetting("dashboard", "Home Screen", "Show Artists Row", "Display horizontal artist avatar list on Home"),
            SearchableSetting("dashboard", "Home Screen", "Show Albums Row", "Display horizontal album card list on Home"),
            SearchableSetting("dashboard", "Home Screen", "Show Genres Row", "Display colorful genre pills row on Home"),
            SearchableSetting("dashboard", "Home Screen", "Reorder Dashboard Sections", "Customize vertical order of Home tab cards"),
            SearchableSetting("playback", "Language & Services", "App Language", "Choose app localization language"),
            SearchableSetting("playback", "Language & Services", "Vertical Motion Effect Player", "Enable smooth vertical drag/slide gesture on Player"),
            SearchableSetting("playback", "Language & Services", "Stop Service on App Dismissal", "Terminate background playback service on app dismiss"),
            SearchableSetting("playback", "Language & Services", "Internet Mode", "Allow app to fetch lyrics and high-res artwork online"),
            SearchableSetting("playback", "Language & Services", "Download Missing Artwork", "Automatically download missing album art online"),
            SearchableSetting("audio_playback", "Audio & Playback", "Fade on Play / Pause / Stop", "Smooth volume fade when starting or changing tracks"),
            SearchableSetting("audio_playback", "Audio & Playback", "Fade Duration", "Length of volume fade transition in milliseconds"),
            SearchableSetting("audio_playback", "Audio & Playback", "Pause on Disconnect", "Pause playback when wired or Bluetooth headphones disconnect"),
            SearchableSetting("audio_playback", "Audio & Playback", "Resume Only If Paused by Disconnection", "Only resume if playback was previously paused by disconnection"),
            SearchableSetting("audio_playback", "Audio & Playback", "Resume on Bluetooth Connect", "Automatically resume playback on Bluetooth connection"),
            SearchableSetting("audio_playback", "Audio & Playback", "Resume on Wired Earphones Connect", "Automatically resume playback on wired earphones plug"),
            SearchableSetting("audio_playback", "Audio & Playback", "Manage Audio Focus", "Pause playback when another app requests focus"),
            SearchableSetting("audio_playback", "Audio & Playback", "Request Focus on Play", "Request audio focus from system on play"),
            SearchableSetting("audio_playback", "Audio & Playback", "Release Focus on Pause", "Release audio focus when paused"),
            SearchableSetting("audio_playback", "Audio & Playback", "Stop on Other Session", "Stop playback service when another media app starts"),
            SearchableSetting("audio_playback", "Audio & Playback", "Restart on Gain", "Resume playback when audio focus is regained"),
            SearchableSetting("audio_playback", "Audio & Playback", "Resume on App Launch", "Automatically resume playback on app launch"),
            SearchableSetting("audio_playback", "Audio & Playback", "Permanent Focus Change Pause", "Pause playback on permanent audio focus loss"),
            SearchableSetting("audio_playback", "Audio & Playback", "Persist Queue & Timeline", "Save active queue and playback position to database"),
            SearchableSetting("library", "Music Library", "Add Specific Folder", "Select specific storage directory to scan"),
            SearchableSetting("library", "Music Library", "Download Lyrics for Offline", "Prefetch synchronized LRC lyrics"),
            SearchableSetting("library", "Music Library", "Rescan Music Library", "Rescan saved storage folders for audio files"),
            SearchableSetting("library", "Music Library", "Use Album Artist for Artists", "Group artists using Album Artist tag field"),
            SearchableSetting("library", "Music Library", "Reset Library & Clear Database", "Clear local database and wipe cached metadata"),
            SearchableSetting("about", "About & Maintainers", "Looper Player", "Version 2.0.00 (Native Compose & ExoPlayer Port)"),
            SearchableSetting("about", "About & Maintainers", "Lyrics Provider", "lrclib.net"),
            SearchableSetting("about", "About & Maintainers", "Give a Star on GitHub", "Support project development on GitHub")
        )
    }

    val filteredSearchItems = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            allSearchableItems.filter {
                it.title.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q) ||
                it.categoryName.lowercase().contains(q)
            }
        }
    }

    val settingsViewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val darkTheme by settingsViewModel.darkTheme.collectAsState()

    val playbackViewModel: com.example.looper_android.ui.player.PlaybackViewModel = androidx.hilt.navigation.compose.hiltViewModel()
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
        // Header Row (Matches Flutter settings_view.dart)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearching) {
                // Searching Header Mode
                PremiumSection(
                    width = 48.dp,
                    height = 48.dp,
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                    onClick = {
                        isSearching = false
                        searchQuery = ""
                    }
                ) {
                    Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 32.dp, bottomEnd = 32.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = DmSansFontFamily
                            ),
                            cursorBrush = SolidColor(accentColor),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search settings...",
                                            color = Color.White.copy(alpha = 0.38f),
                                            fontSize = 16.sp,
                                            fontFamily = DmSansFontFamily
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Lucide.X, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            } else {
                // Standard Header Mode
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
                    text = "Settings",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                PremiumSection(
                    width = 48.dp,
                    height = 48.dp,
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 32.dp, bottomEnd = 32.dp),
                    onClick = { isSearching = true }
                ) {
                    Icon(Lucide.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Thin Header Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .padding(horizontal = 16.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )

        // Settings Body
        if (isSearching) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredSearchItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "Type to search settings..." else "No settings found for \"$searchQuery\"",
                                color = Color.White.copy(alpha = 0.38f),
                                fontSize = 15.sp,
                                fontFamily = DmSansFontFamily
                            )
                        }
                    }
                } else {
                    items(filteredSearchItems) { item ->
                        SettingsActionTile(
                            title = item.title,
                            subtitle = "${item.categoryName.uppercase()} • ${item.subtitle}",
                            onClick = { onNavigateToCategory(item.categoryId) },
                            accentColor = accentColor
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { cat ->
                    CategoryCard(
                        category = cat,
                        accentColor = accentColor,
                        onClick = { onNavigateToCategory(cat.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: SettingCategory,
    accentColor: Color,
    onClick: () -> Unit
) {
    PremiumSection(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = DmSansFontFamily
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = category.subtitle,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontFamily = DmSansFontFamily
                )
            }

            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
