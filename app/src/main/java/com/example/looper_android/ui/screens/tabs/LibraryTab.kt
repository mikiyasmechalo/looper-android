package com.example.looper_android.ui.screens.tabs
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Layers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Disc
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.History
import com.composables.icons.lucide.List
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Users
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

@Composable
fun LibraryTab(
    viewModel: LibraryViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val settingsViewModel: com.example.looper_android.ui.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val showLibraryRecentlyPlayed by settingsViewModel.showLibraryRecentlyPlayed.collectAsState()
    val showLibraryRecentlyAdded by settingsViewModel.showLibraryRecentlyAdded.collectAsState()
    val showLibraryNotPlayed by settingsViewModel.showLibraryNotPlayed.collectAsState()
    val recentlyAddedSongs by viewModel.libraryCacheManager.recentlyAddedSongs.collectAsState()
    val notPlayedSongs by viewModel.libraryCacheManager.notPlayedSongs.collectAsState()
    val currentSong = playbackState.currentSong
    val isMiniPlayerVisible = playbackState.isSongReady && currentSong != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Header (Matches Flutter: Search on left, "Library" in center, Settings on right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumSection(
                width = 48.dp,
                height = 48.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                onClick = onNavigateToSearch
            ) {
                Icon(Lucide.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Text(
                text = "Library",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily
            )

            PremiumSection(
                width = 48.dp,
                height = 48.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 32.dp, bottomEnd = 32.dp),
                onClick = onNavigateToSettings
            ) {
                Icon(Lucide.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .padding(horizontal = 16.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = com.example.looper_android.ui.theme.LocalAccentColor.current)
            }
        } else {
            val bottomInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val targetBottomPadding = if (isMiniPlayerVisible) (200.dp + bottomInsets) else (140.dp + bottomInsets)
            val bottomPadding = targetBottomPadding

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
            ) {
                // 1. RECENT PLAYED SECTION
                if (showLibraryRecentlyPlayed) {
                    item {
                        SectionHeaderPill(title = "Recent Played", accentColor = playbackState.accentColor)
                        RecentlyPlayedHorizontalSection(
                            recentlyPlayed = uiState.recentlyPlayed,
                            accentColor = playbackState.accentColor,
                            onSongClick = { index -> viewModel.playRecentlyPlayedSong(index) }
                        )
                    }
                }



                // 2. CATEGORIES SECTION
                item {
                    SectionHeaderPill(title = "Categories", accentColor = playbackState.accentColor, topPadding = 24.dp)

                    // Categories Block 1
                    Padding(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        CategoryBlock(
                            items = listOf(
                                CategoryItem("Favorites", Lucide.Star) { onNavigateToCategory("favorites") },
                                CategoryItem("Albums", Lucide.Disc) { onNavigateToCategory("albums") },
                                CategoryItem("Artists", Lucide.Users) { onNavigateToCategory("artists") },
                                CategoryItem("Playlists", Lucide.ListMusic) { onNavigateToCategory("playlists") }
                            ),
                            accentColor = playbackState.accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Categories Block 2
                    Padding(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        CategoryBlock(
                            items = listOf(
                                CategoryItem("Folders", Lucide.Folder) { onNavigateToCategory("folders") },
                                CategoryItem("Genres", Lucide.Music) { onNavigateToCategory("genres") },
                                CategoryItem("Queue", Lucide.List) { onNavigateToCategory("queue") },
                                CategoryItem("History", Lucide.History) { onNavigateToCategory("history") }
                            ),
                            accentColor = playbackState.accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Categories Block 3 (Discovery Pages)
                    Padding(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        CategoryBlock(
                            items = listOf(
                                CategoryItem("Recently Added", Lucide.Clock) { onNavigateToCategory("recently_added") },
                                CategoryItem("Not Played", Lucide.Sparkles) { onNavigateToCategory("not_played") }
                            ),
                            accentColor = playbackState.accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderPill(
    title: String,
    accentColor: Color,
    topPadding: androidx.compose.ui.unit.Dp = 8.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = 14.dp)
    ) {
        PremiumSection(
            shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 10.dp, bottomEnd = 10.dp),
            backgroundColor = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DmSansFontFamily,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun RecentlyPlayedHorizontalSection(
    recentlyPlayed: List<SongEntity>,
    accentColor: Color,
    onSongClick: (Int) -> Unit
) {
    if (recentlyPlayed.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recently played tracks",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 13.sp,
                fontFamily = DmSansFontFamily
            )
        }
        return
    }

    val row1Songs = mutableListOf<Pair<SongEntity, Int>>()
    val row2Songs = mutableListOf<Pair<SongEntity, Int>>()

    for ((index, song) in recentlyPlayed.withIndex()) {
        if (index % 2 == 0) {
            row1Songs.add(song to index)
        } else {
            row2Songs.add(song to index)
        }
    }

    val hasTwoRows = recentlyPlayed.size > 1
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((song, globalIndex) in row1Songs) {
                RecentPill(
                    song = song,
                    accentColor = accentColor,
                    onClick = { onSongClick(globalIndex) }
                )
            }
        }

        // Row 2 (if 2 or more tracks exist)
        if (hasTwoRows) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((song, globalIndex) in row2Songs) {
                    RecentPill(
                        song = song,
                        accentColor = accentColor,
                        onClick = { onSongClick(globalIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentPill(
    song: SongEntity,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(36.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                OptimizedImage(
                    url = song.artPath,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    sizePx = 80
                )
            }

            Text(
                text = song.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DmSansFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class CategoryItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun CategoryBlock(
    items: List<CategoryItem>,
    accentColor: Color
) {
    PremiumSection(
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color.White.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(if (index == 0 && items.size == 1) RoundedCornerShape(20.dp) else if (index == 0) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp) else if (index == items.lastIndex) RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp) else RoundedCornerShape(0.dp))
                        .clickable { item.onClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = DmSansFontFamily
                        )
                    }

                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.9.dp,
                        color = Color.White.copy(alpha = 0.06f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Padding(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}
