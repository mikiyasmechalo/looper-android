package com.example.looper_android.ui.library

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.ArrowUpDown
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Shuffle
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

@Composable
fun AllSongsScreen(
    viewModel: AllSongsViewModel = hiltViewModel(),
    songEditViewModel: SongEditViewModel = hiltViewModel(),
    onShufflePlayClick: () -> Unit
) {
    val songs by viewModel.sortedSongs.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDbLoaded by viewModel.isDbLoaded.collectAsState()

    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val currentSong = playbackState.currentSong

    var showSortSheet by remember { mutableStateOf(false) }
    var selectedSongForOptions by remember { mutableStateOf<SongEntity?>(null) }
    var selectedSongForDetails by remember { mutableStateOf<SongEntity?>(null) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadSongs()
        }
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.loadSongs()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF11110E))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row (Matches Flutter android_songs_tab.dart 1-to-1)
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
                    onClick = {}
                ) {
                    Icon(Lucide.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = "All Songs",
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
                    onClick = {}
                ) {
                    Icon(Lucide.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Thin divider below header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .padding(horizontal = 16.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            // Songs Header Bar (Matches Flutter songs_list.dart 1-to-1: Song Count + Sort Button)
            if (isDbLoaded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${songs.size} Songs",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontFamily = DmSansFontFamily
                    )
                    IconButton(
                        onClick = { showSortSheet = true }
                    ) {
                        Icon(
                            imageVector = Lucide.ArrowUpDown,
                            contentDescription = "Sort By",
                            tint = Color(0xFF55DF69),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }

            // Song List
            if (isLoading && songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF55DF69))
                }
            } else if (songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No songs found.", color = Color.Gray)
                }
            } else {
                val bottomPadding = if (currentSong != null) 260.dp else 180.dp
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = bottomPadding)
                ) {
                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                        val isCurrent = currentSong?.path == song.path
                        val isPlaying = isCurrent && playbackState.isPlaying
                        SongTile(
                            song = song,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying,
                            onClick = { viewModel.playAll(startIndex = index, shuffled = false) },
                            onOptionsClick = { selectedSongForOptions = song }
                        )
                    }
                }
            }
        }

        // Floating Shuffle Action Button (Matches Flutter android_songs_tab.dart 1-to-1: 40dp right, bottom padded)
        val fabBottomPadding by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (currentSong != null) 200.dp else 120.dp,
            animationSpec = androidx.compose.animation.core.tween(250),
            label = "fabPadding"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = fabBottomPadding, end = 40.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            PremiumSection(
                width = 48.dp,
                height = 48.dp,
                shape = RoundedCornerShape(48.dp),
                backgroundColor = Color(0xFF1E1E1E),
                onClick = {
                    viewModel.shuffleAll()
                }
            ) {
                Icon(
                    imageVector = Lucide.Shuffle,
                    contentDescription = "Shuffle",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Modal Sheets
        if (showSortSheet) {
            SortBottomSheet(
                currentSort = sortOption,
                isAscending = isAscending,
                onSortSelected = { option -> viewModel.setSortOption(option) },
                onToggleAscending = { viewModel.toggleAscending() },
                onDismiss = { showSortSheet = false }
            )
        }

        var selectedSongForEdit by remember { mutableStateOf<SongEntity?>(null) }
        var selectedSongForDelete by remember { mutableStateOf<SongEntity?>(null) }

        selectedSongForOptions?.let { song ->
            SongOptionsSheet(
                song = song,
                onDismiss = { selectedSongForOptions = null },
                accentColor = playbackState.accentColor,
                isFavorite = song.isFavorite,
                onPlayNextClick = {
                    playbackViewModel.addNext(song)
                },
                onAddToQueueClick = {
                    playbackViewModel.addToQueue(song)
                },
                onAddToPlaylistClick = {
                    // Add to Playlist action
                },
                onFavoriteClick = {
                    viewModel.updateSong(song.copy(isFavorite = !song.isFavorite))
                },
                onEditSongClick = {
                    selectedSongForEdit = song
                },
                onSongDetailsClick = {
                    selectedSongForDetails = song
                },
                onDeleteClick = {
                    selectedSongForDelete = song
                }
            )
        }

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

        selectedSongForDelete?.let { song ->
            DeleteSongDialog(
                songTitle = song.title,
                onDismiss = { selectedSongForDelete = null },
                onConfirm = {
                    viewModel.deleteSong(song)
                    selectedSongForDelete = null
                }
            )
        }

        var selectedSongForInfo by remember { mutableStateOf<SongEntity?>(null) }

        selectedSongForDetails?.let { song ->
            SongDetailsBottomSheet(
                song = song,
                onDismiss = { selectedSongForDetails = null },
                accentColor = playbackState.accentColor,
                onArtistClick = { artist ->
                    // Navigate to artist
                },
                onAlbumClick = { album ->
                    // Navigate to album
                },
                onTechnicalInfoClick = {
                    selectedSongForInfo = song
                }
            )
        }

        selectedSongForInfo?.let { song ->
            TechnicalInfoBottomSheet(
                song = song,
                onDismiss = { selectedSongForInfo = null },
                accentColor = playbackState.accentColor
            )
        }
    }
}

@Composable
fun SongTile(
    song: SongEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() }
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            OptimizedImage(
                url = song.artPath,
                modifier = Modifier.fillMaxSize()
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(visible = isPlaying) {
                        val context = LocalContext.current
                        val gifRequest = remember(context) {
                            coil.request.ImageRequest.Builder(context)
                                .data("file:///android_asset/android_icons/Playing.gif")
                                .build()
                        }
                        coil.compose.AsyncImage(
                            model = gifRequest,
                            contentDescription = "Playing Indicator",
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist ?: "Unknown Artist",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontFamily = DmSansFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = onOptionsClick) {
            Icon(Lucide.EllipsisVertical, contentDescription = "Options", tint = Color.Gray)
        }
    }
}
