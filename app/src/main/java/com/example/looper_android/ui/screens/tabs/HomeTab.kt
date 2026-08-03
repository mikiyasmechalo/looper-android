package com.example.looper_android.ui.screens.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.User
import com.example.looper_android.data.db.AlbumEntity
import com.example.looper_android.data.db.ArtistEntity
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.library.SongTile

import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.player.SongEditViewModel
import com.example.looper_android.ui.player.SongOptionsSheet
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection
import kotlin.math.abs

@Composable
fun HomeTab(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSongs: () -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToGenres: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val currentSong = playbackState.currentSong

    var selectedSongForOptions by remember { mutableStateOf<SongEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Header (Matches Flutter: "Home" title on left, Search & Settings on right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Home",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .padding(horizontal = 16.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )

        if (uiState.isLoading) {
            com.example.looper_android.ui.components.PremiumLoadingView(message = "LOADING MUSIC LIBRARY")
        } else if (uiState.quickPicks.isEmpty() && uiState.topSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs found", color = Color.Gray)
            }
        } else {
            // Proper bottom padding so content is never covered by bottom navigation bar or miniplayer!
            val bottomPadding = if (currentSong != null) 200.dp else 140.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)
            ) {
                // 1. QUICK PICKS SECTION
                if (uiState.quickPicks.isNotEmpty()) {
                    item {
                        QuickPicksSection(
                            quickPicks = uiState.quickPicks,
                            currentSong = currentSong,
                            isPlaying = playbackState.isPlaying,
                            onPlayShuffle = { viewModel.playQuickPicks(shuffled = true) },
                            onSongClick = { song, index -> viewModel.playSongFromQuickPicks(index) },
                            onSongLongClick = { song -> selectedSongForOptions = song }
                        )
                    }
                }

                // 2. TOP SONGS SECTION (Icon-only shuffle button + "All Songs" action)
                if (uiState.topSongs.isNotEmpty()) {
                    item {
                        SectionHeaderWithShuffle(
                            title = "Top Songs",
                            actionText = "All Songs",
                            onShuffleClick = { viewModel.playTopSongs(shuffled = true) },
                            onActionClick = onNavigateToSongs
                        )
                    }

                    itemsIndexed(uiState.topSongs, key = { _, s -> "top_${s.id}" }) { index, song ->
                        val isCurrent = currentSong?.path == song.path
                        val isPlaying = isCurrent && playbackState.isPlaying
                        SongTile(
                            song = song,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying,
                            onClick = { viewModel.playAllTopSongs(startIndex = index) },
                            onOptionsClick = { selectedSongForOptions = song }
                        )
                    }
                }

                // 3. TOP ARTISTS SECTION
                if (uiState.artists.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Top Artists",
                            actionText = "View all",
                            onActionClick = onNavigateToArtists
                        )
                        HorizontalArtistsList(artists = uiState.artists)
                    }
                }

                // 4. TOP ALBUMS SECTION
                if (uiState.albums.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Top Albums",
                            actionText = "View all",
                            onActionClick = onNavigateToAlbums
                        )
                        HorizontalAlbumsList(albums = uiState.albums)
                    }
                }

                // 5. GENRES SECTION
                if (uiState.genres.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Genres",
                            actionText = "View all",
                            onActionClick = onNavigateToGenres
                        )
                        HorizontalGenresList(genres = uiState.genres)
                    }
                }
            }
        }
    }

    // Modal Sheets
    selectedSongForOptions?.let { song ->
        SongOptionsSheet(
            song = song,
            accentColor = playbackState.accentColor,
            onDismiss = { selectedSongForOptions = null }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DmSansFontFamily
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable { onActionClick() }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = actionText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = DmSansFontFamily
                )
                Icon(
                    imageVector = Lucide.ArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderWithShuffle(
    title: String,
    actionText: String,
    onShuffleClick: () -> Unit,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DmSansFontFamily
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon-only Shuffle Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onShuffleClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Shuffle,
                    contentDescription = "Shuffle",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // All Songs Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onActionClick() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = actionText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = DmSansFontFamily
                    )
                    Icon(
                        imageVector = Lucide.ArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPicksSection(
    quickPicks: List<SongEntity>,
    currentSong: SongEntity?,
    isPlaying: Boolean,
    onPlayShuffle: () -> Unit,
    onSongClick: (SongEntity, Int) -> Unit,
    onSongLongClick: (SongEntity) -> Unit
) {
    val pageCount = (quickPicks.size + 5) / 6
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        // Quick Picks Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Quick Picks",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily
                )
                Text(
                    text = "Today's mix for you",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DmSansFontFamily
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onPlayShuffle() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Shuffle,
                        contentDescription = "Shuffle",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Play",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = DmSansFontFamily
                    )
                }
            }
        }

        // Paged 3x2 Grid
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            val startIndex = pageIndex * 6
            val pageItems = quickPicks.drop(startIndex).take(6)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (rowIndex in 0 until 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (colIndex in 0 until 3) {
                            val itemIndex = rowIndex * 3 + colIndex
                            if (itemIndex < pageItems.size) {
                                val song = pageItems[itemIndex]
                                val globalIndex = startIndex + itemIndex
                                val isCurrent = currentSong?.path == song.path

                                QuickPickGridItem(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSongClick(song, globalIndex) },
                                    onLongClick = { onSongLongClick(song) }
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Page Indicator Dots
        if (pageCount > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until pageCount) {
                    val isActive = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (isActive) 16.dp else 6.dp, height = 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isActive) Color.White else Color.White.copy(alpha = 0.24f))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickPickGridItem(
    song: SongEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        OptimizedImage(
            url = song.artPath,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        startY = 60f
                    )
                )
        )

        // Song title text
        Text(
            text = song.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DmSansFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )

        if (isCurrent && isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF55DF69),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun HorizontalArtistsList(artists: List<ArtistEntity>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(artists) { artist ->
            Column(
                modifier = Modifier
                    .width(92.dp)
                    .clickable { },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = artist.artistImageUrl ?: artist.artPath
                    if (!avatarUrl.isNullOrEmpty()) {
                        OptimizedImage(
                            url = avatarUrl,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Lucide.User,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.38f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = artist.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = DmSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HorizontalAlbumsList(albums: List<AlbumEntity>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums) { album ->
            Column(
                modifier = Modifier
                    .width(120.dp)
                    .clickable { }
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    OptimizedImage(
                        url = album.artPath,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = album.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = DmSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artist ?: "Unknown Artist",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontFamily = DmSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HorizontalGenresList(genres: List<GenreItem>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(genres) { genre ->
            val hash = abs(genre.name.hashCode())
            val hue = (hash % 360).toFloat()
            val startColor = Color.hsl(hue, 0.70f, 0.35f)
            val endColor = Color.hsl((hue + 45f) % 360f, 0.80f, 0.18f)

            val firstSongWithArt = genre.songs.firstOrNull { it.artPath != null } ?: genre.songs.firstOrNull()

            Box(
                modifier = Modifier
                    .size(width = 180.dp, height = 90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(startColor, endColor)))
                    .clickable { }
                    .padding(14.dp)
            ) {
                // Rotated Album Art in bottom right corner
                firstSongWithArt?.artPath?.let { art ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 10.dp, y = 10.dp)
                            .rotate(15f)
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        OptimizedImage(
                            url = art,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } ?: run {
                    Icon(
                        imageVector = Lucide.Music,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 6.dp)
                            .rotate(15f)
                            .size(40.dp)
                    )
                }

                // Title & song count on left
                Column(
                    modifier = Modifier.align(Alignment.TopStart),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = genre.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${genre.songs.size} Songs",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DmSansFontFamily
                    )
                }
            }
        }
    }
}
