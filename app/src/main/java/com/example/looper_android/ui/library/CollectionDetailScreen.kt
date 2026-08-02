package com.example.looper_android.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
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
import com.composables.icons.lucide.Disc
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Hash
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.Type
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.player.DeleteSongDialog
import com.example.looper_android.ui.player.EditSongSheet
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.player.SongDetailsBottomSheet
import com.example.looper_android.ui.player.SongEditViewModel
import com.example.looper_android.ui.player.SongOptionsSheet
import com.example.looper_android.ui.player.TechnicalInfoBottomSheet
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

enum class SongSortOption { TITLE, TRACK_NUMBER, DURATION, DATE_ADDED, YEAR, ALBUM }

@Composable
fun CollectionDetailScreen(
    title: String,
    subtitle: String = "Collection",
    artPath: String? = null,
    type: String, // "album", "artist", "playlist", "genre", "folder"
    onBackClick: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    allSongsViewModel: AllSongsViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    songEditViewModel: SongEditViewModel = hiltViewModel()
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
    var selectedSongForEdit by remember { mutableStateOf<SongEntity?>(null) }
    var selectedSongForDetails by remember { mutableStateOf<SongEntity?>(null) }
    var selectedSongForInfo by remember { mutableStateOf<SongEntity?>(null) }
    var selectedSongForDelete by remember { mutableStateOf<SongEntity?>(null) }

    val collectionSongs = remember(allSongs, title, type, sortOption, isAscending) {
        val filtered = when (type) {
            "album" -> allSongs.filter { (it.album ?: "Unknown Album").equals(title, ignoreCase = true) }
            "artist" -> allSongs.filter { 
                val songArtist = it.artist?.trim()
                val albumArtist = it.albumArtist?.trim()
                (songArtist != null && songArtist.equals(title, ignoreCase = true)) || 
                (albumArtist != null && albumArtist.equals(title, ignoreCase = true))
            }
            "playlist" -> allSongs.filter { it.isFavorite }
            "genre" -> allSongs.filter { (it.genre ?: "Unknown Genre").equals(title, ignoreCase = true) }
            "folder" -> allSongs.filter { song ->
                val parentPath = java.io.File(song.path).parent ?: ""
                val folderName = java.io.File(parentPath).name.ifBlank { parentPath }
                folderName.equals(title, ignoreCase = true) || parentPath.equals(title, ignoreCase = true)
            }
            else -> allSongs
        }

        val sorted = filtered.toMutableList()
        when (sortOption) {
            SongSortOption.TITLE -> if (isAscending) sorted.sortBy { it.title.lowercase() } else sorted.sortByDescending { it.title.lowercase() }
            SongSortOption.TRACK_NUMBER -> if (isAscending) sorted.sortBy { it.trackNumber ?: 0 } else sorted.sortByDescending { it.trackNumber ?: 0 }
            SongSortOption.DURATION -> if (isAscending) sorted.sortBy { it.duration } else sorted.sortByDescending { it.duration }
            SongSortOption.DATE_ADDED -> if (isAscending) sorted.sortBy { it.dateAdded ?: 0L } else sorted.sortByDescending { it.dateAdded ?: 0L }
            SongSortOption.YEAR -> if (isAscending) sorted.sortBy { it.year ?: 0 } else sorted.sortByDescending { it.year ?: 0 }
            SongSortOption.ALBUM -> if (isAscending) sorted.sortBy { it.album?.lowercase() ?: "" } else sorted.sortByDescending { it.album?.lowercase() ?: "" }
        }
        sorted
    }

    val displayArt = remember(collectionSongs, artPath) {
        artPath ?: collectionSongs.firstOrNull { !it.artPath.isNullOrEmpty() }?.artPath
    }

    val albumArtistName = remember(collectionSongs) {
        collectionSongs.firstOrNull()?.albumArtist?.trim()?.ifBlank { null }
            ?: collectionSongs.firstOrNull()?.artist?.trim()?.ifBlank { null }
            ?: "Unknown Artist"
    }

    val headerSubtitleText = remember(type, albumArtistName, collectionSongs.size) {
        if (type == "album") {
            "$albumArtistName • ${collectionSongs.size} tracks"
        } else {
            "${collectionSongs.size} tracks"
        }
    }

    val bottomPadding = if (currentSong != null) 200.dp else 140.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumSection(
                width = 40.dp,
                height = 40.dp,
                backgroundColor = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                onClick = onBackClick
            ) {
                Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isDbLoaded && collectionSongs.isEmpty()) {
            com.example.looper_android.ui.components.PremiumLoadingView()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = bottomPadding),
                modifier = Modifier.fillMaxSize()
            ) {
                // Centered Hero Header Section (Flutter style)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp, top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Centered Album Artwork Card
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            OptimizedImage(
                                url = displayArt,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Collection Title
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Collection Subtitle (Artist • Track Count)
                        Text(
                            text = headerSubtitleText,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons Row (Play, Shuffle, Sort, 3-Dot)
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play All Button
                            PremiumSection(
                                width = 110.dp,
                                height = 44.dp,
                                backgroundColor = Color(0xFF55DF69),
                                shape = RoundedCornerShape(22.dp),
                                onClick = {
                                    if (collectionSongs.isNotEmpty()) {
                                        playbackViewModel.setPlaylist(collectionSongs, initialIndex = 0)
                                    }
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Lucide.Play, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
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

                            // Sort Button
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
                        onOptionsClick = { selectedSongForOptions = song },
                        showAlbumInSubtitle = (type != "album")
                    )
                }
            }
        }
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        val songSortOptions = listOf(
            CategorySortOptionItem(SongSortOption.TITLE, "Title", Lucide.Type),
            CategorySortOptionItem(SongSortOption.TRACK_NUMBER, "Track Number", Lucide.Hash),
            CategorySortOptionItem(SongSortOption.DURATION, "Duration", Lucide.Clock),
            CategorySortOptionItem(SongSortOption.DATE_ADDED, "Date Added", Lucide.CalendarPlus),
            CategorySortOptionItem(SongSortOption.YEAR, "Year", Lucide.Calendar),
            CategorySortOptionItem(SongSortOption.ALBUM, "Album", Lucide.Disc)
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

    // Permission launcher for song metadata edits
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            songEditViewModel.onPermissionGranted()
        } else {
            songEditViewModel.onPermissionDenied()
        }
    }

    val pendingWritePermission by songEditViewModel.pendingPermission.collectAsState()
    LaunchedEffect(pendingWritePermission) {
        pendingWritePermission?.let { pending ->
            val request = androidx.activity.result.IntentSenderRequest.Builder(pending.intentSender).build()
            writePermissionLauncher.launch(request)
        }
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
            onFavoriteClick = { allSongsViewModel.updateSong(song.copy(isFavorite = !song.isFavorite)) },
            onEditSongClick = { selectedSongForEdit = song },
            onSongDetailsClick = { selectedSongForDetails = song },
            onDeleteClick = { selectedSongForDelete = song }
        )
    }

    // Edit Song Sheet
    selectedSongForEdit?.let { song ->
        EditSongSheet(
            song = song,
            onDismiss = { selectedSongForEdit = null },
            accentColor = playbackState.accentColor,
            onSave = { newTitle, newArtist, newAlbum, newYear, newGenre, newLyrics, newArtPath ->
                songEditViewModel.saveEdit(
                    song = song,
                    newTitle = newTitle,
                    newArtist = newArtist,
                    newAlbum = newAlbum,
                    newYear = if (newYear > 0) newYear else null,
                    newGenre = newGenre,
                    newLyrics = newLyrics,
                    newArtPath = newArtPath
                )
                selectedSongForEdit = null
            }
        )
    }

    // Delete Song Confirmation Dialog
    selectedSongForDelete?.let { song ->
        DeleteSongDialog(
            songTitle = song.title,
            onDismiss = { selectedSongForDelete = null },
            onConfirm = {
                allSongsViewModel.deleteSong(song)
                selectedSongForDelete = null
            }
        )
    }

    // Song Details Bottom Sheet
    selectedSongForDetails?.let { song ->
        SongDetailsBottomSheet(
            song = song,
            onDismiss = { selectedSongForDetails = null },
            accentColor = playbackState.accentColor,
            onArtistClick = { artist ->
                selectedSongForDetails = null
                onNavigateToArtist(artist)
            },
            onAlbumClick = { album ->
                selectedSongForDetails = null
                onNavigateToAlbum(album)
            },
            onTechnicalInfoClick = { selectedSongForInfo = song }
        )
    }

    // Technical Audio Details Sheet
    selectedSongForInfo?.let { song ->
        TechnicalInfoBottomSheet(
            song = song,
            onDismiss = { selectedSongForInfo = null },
            accentColor = playbackState.accentColor
        )
    }
}
