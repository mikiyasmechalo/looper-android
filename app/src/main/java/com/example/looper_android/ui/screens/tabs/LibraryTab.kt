package com.example.looper_android.ui.screens.tabs

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
    val currentSong = playbackState.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
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
                width = 44.dp,
                height = 44.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 10.dp, bottomEnd = 10.dp),
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
                width = 44.dp,
                height = 44.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 24.dp, bottomEnd = 24.dp),
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
                CircularProgressIndicator(color = Color(0xFF55DF69))
            }
        } else {
            val bottomPadding = if (currentSong != null) 200.dp else 140.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
            ) {
                // 1. RECENT PLAYED SECTION
                item {
                    SectionHeaderPill(title = "Recent Played")
                    RecentlyPlayedHorizontalSection(
                        recentlyPlayed = uiState.recentlyPlayed,
                        onSongClick = { index -> viewModel.playRecentlyPlayedSong(index) }
                    )
                }

                // 2. CATEGORIES SECTION
                item {
                    SectionHeaderPill(title = "Categories", topPadding = 24.dp)

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
                            )
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
                            )
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
    topPadding: androidx.compose.ui.unit.Dp = 8.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = 16.dp)
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
            .padding(start = 24.dp, end = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((song, globalIndex) in row1Songs) {
                RecentPill(
                    song = song,
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                OptimizedImage(
                    url = song.artPath,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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
private fun CategoryBlock(items: List<CategoryItem>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1E1E))
    ) {
        Column {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { item.onClick() }
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(22.dp)
                        )
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
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 0.9.dp,
                        color = Color.White.copy(alpha = 0.1f)
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
