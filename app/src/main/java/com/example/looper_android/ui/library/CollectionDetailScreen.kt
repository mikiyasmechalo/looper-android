package com.example.looper_android.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowUpDown
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.CalendarPlus
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.Type
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.player.SongOptionsSheet
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

enum class SongSortOption { TITLE, TRACK_NUMBER, DURATION, DATE_ADDED, YEAR }

@Composable
fun CollectionDetailScreen(
    title: String,
    subtitle: String = "Collection",
    artPath: String? = null,
    type: String, // "album", "artist", "playlist", "genre", "folder"
    onBackClick: () -> Unit,
    allSongsViewModel: AllSongsViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    val allSongs by allSongsViewModel.songs.collectAsState()
    val isDbLoaded by allSongsViewModel.isDbLoaded.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val currentSong = playbackState.currentSong

    var sortOption by remember { mutableStateOf(SongSortOption.TITLE) }
    var isAscending by remember { mutableStateOf(true) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showCollectionOptionsSheet by remember { mutableStateOf(false) }
    var selectedSongForOptions by remember { mutableStateOf<SongEntity?>(null) }

    val collectionSongs = remember(allSongs, title, type, sortOption, isAscending) {
        val filtered = when (type.lowercase()) {
            "album" -> allSongs.filter { it.album.equals(title, ignoreCase = true) }
            "artist" -> allSongs.filter {
                val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                effectiveArtist.equals(title, ignoreCase = true) || (it.artist?.equals(title, ignoreCase = true) == true)
            }
            "genre" -> allSongs.filter { (it.genre?.trim()?.ifBlank { "Unknown" }).equals(title, ignoreCase = true) }
            "folder" -> allSongs.filter {
                val parentName = java.io.File(it.path).parentFile?.name ?: ""
                parentName.equals(title, ignoreCase = true) || (java.io.File(it.path).parent ?: "").equals(title, ignoreCase = true)
            }
            else -> allSongs
        }

        val mutable = filtered.toMutableList()
        when (sortOption) {
            SongSortOption.TITLE -> mutable.sortBy { it.title.lowercase() }
            SongSortOption.TRACK_NUMBER -> mutable.sortBy { it.trackNumber ?: 0 }
            SongSortOption.DURATION -> mutable.sortByDescending { it.duration }
            SongSortOption.DATE_ADDED -> mutable.sortByDescending { it.dateAdded }
            SongSortOption.YEAR -> mutable.sortByDescending { it.year ?: 0 }
        }
        if (!isAscending) mutable.reverse()
        mutable
    }

    val coverArt = artPath ?: collectionSongs.firstOrNull { !it.artPath.isNullOrEmpty() }?.artPath
    val bottomPadding = if (currentSong != null) 200.dp else 140.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Top Back Button Bar (Matches Flutter 1-to-1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumSection(
                width = 44.dp,
                height = 44.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                onClick = onBackClick
            ) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (!isDbLoaded && collectionSongs.isEmpty()) {
            com.example.looper_android.ui.components.PremiumLoadingView()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = bottomPadding),
                modifier = Modifier.fillMaxSize()
            ) {
                // Hero Header (Matches Flutter 140x140 artwork & button row 1-to-1)
                item {
                    val albumArtist = remember(collectionSongs) {
                        collectionSongs.firstOrNull { !it.albumArtist.isNullOrBlank() }?.albumArtist
                            ?: collectionSongs.firstOrNull { !it.artist.isNullOrBlank() }?.artist
                    }
                    val subtitleText = if (type.equals("album", ignoreCase = true) && !albumArtist.isNullOrBlank()) {
                        "$albumArtist • ${collectionSongs.size} ${if (collectionSongs.size == 1) "track" else "tracks"}"
                    } else {
                        "${collectionSongs.size} ${if (collectionSongs.size == 1) "track" else "tracks"}"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                OptimizedImage(
                                    url = coverArt,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = subtitleText,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                            fontFamily = DmSansFontFamily
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Bar (Matches Flutter 1-to-1: Play All, Shuffle, Sort, 3-Dot Overflow)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play All Button (Flutter PremiumSection with rounded rectangle 12dp)
                            PremiumSection(
                                height = 44.dp,
                                backgroundColor = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    if (collectionSongs.isNotEmpty()) {
                                        playbackViewModel.setPlaylist(collectionSongs, initialIndex = 0)
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Lucide.Play, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "Play All",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = DmSansFontFamily
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Shuffle Button
                            PremiumSection(
                                width = 44.dp,
                                height = 44.dp,
                                backgroundColor = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    if (collectionSongs.isNotEmpty()) {
                                        playbackViewModel.setPlaylist(collectionSongs.shuffled(), initialIndex = 0)
                                    }
                                }
                            ) {
                                Icon(Lucide.Shuffle, contentDescription = "Shuffle", tint = Color.White, modifier = Modifier.size(16.dp))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Sort Button (Lucide.ArrowUpDown)
                            PremiumSection(
                                width = 44.dp,
                                height = 44.dp,
                                backgroundColor = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                onClick = { showSortSheet = true }
                            ) {
                                Icon(Lucide.ArrowUpDown, contentDescription = "Sort", tint = Color.White, modifier = Modifier.size(16.dp))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // 3-Dot Overflow Collection Options Menu Button
                            PremiumSection(
                                width = 44.dp,
                                height = 44.dp,
                                backgroundColor = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                onClick = { showCollectionOptionsSheet = true }
                            ) {
                                Icon(Lucide.EllipsisVertical, contentDescription = "Collection Options", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Track List
                itemsIndexed(collectionSongs, key = { _, song -> song.id }) { index, song ->
                    val isCurrent = currentSong?.path == song.path
                    SongTile(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && playbackState.isPlaying,
                        onClick = { playbackViewModel.setPlaylist(collectionSongs, initialIndex = index) },
                        onOptionsClick = { selectedSongForOptions = song }
                    )
                }
            }
        }
    }

    // Sort Bottom Sheet (Matches AllSongsScreen SortBottomSheet 100%)
    if (showSortSheet) {
        val songSortOptions = listOf(
            CategorySortOptionItem(SongSortOption.TITLE, "Title", Lucide.Type),
            CategorySortOptionItem(SongSortOption.TRACK_NUMBER, "Track Number", Lucide.Type),
            CategorySortOptionItem(SongSortOption.DURATION, "Duration", Lucide.Clock),
            CategorySortOptionItem(SongSortOption.DATE_ADDED, "Date Added", Lucide.CalendarPlus),
            CategorySortOptionItem(SongSortOption.YEAR, "Year", Lucide.Calendar)
        )
        CategorySortBottomSheet(
            options = songSortOptions,
            selectedOption = sortOption,
            isAscending = isAscending,
            onOptionSelected = { sortOption = it },
            onToggleAscending = { isAscending = !isAscending },
            onDismiss = { showSortSheet = false },
            accentColor = playbackState.accentColor
        )
    }

    // Collection 3-Dot Overflow Options Sheet
    if (showCollectionOptionsSheet) {
        CollectionOptionsMenuSheet(
            collectionTitle = title,
            songs = collectionSongs,
            onDismiss = { showCollectionOptionsSheet = false },
            onPlayNextClick = {
                collectionSongs.reversed().forEach { playbackViewModel.addNext(it) }
            },
            onAddToQueueClick = {
                collectionSongs.forEach { playbackViewModel.addToQueue(it) }
            },
            onAddToPlaylistClick = {},
            onFavoriteAllClick = {
                collectionSongs.forEach { allSongsViewModel.updateSong(it.copy(isFavorite = true)) }
            },
            accentColor = playbackState.accentColor
        )
    }

    // Individual Track 3-Dot Options Sheet
    selectedSongForOptions?.let { song ->
        SongOptionsSheet(
            song = song,
            accentColor = playbackState.accentColor,
            onDismiss = { selectedSongForOptions = null },
            isFavorite = song.isFavorite,
            onPlayNextClick = { playbackViewModel.addNext(song) },
            onAddToQueueClick = { playbackViewModel.addToQueue(song) },
            onAddToPlaylistClick = {},
            onFavoriteClick = { allSongsViewModel.updateSong(song.copy(isFavorite = !song.isFavorite)) }
        )
    }
}
