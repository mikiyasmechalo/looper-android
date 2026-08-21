package com.example.looper_android.ui.library
import com.example.looper_android.ui.components.PlaylistArtworkCollage
import com.composables.icons.lucide.FolderInput
import com.composables.icons.lucide.Download
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.looper_android.ui.components.PremiumLoadingView
import com.composables.icons.lucide.GitMerge

import android.content.Context
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.looper_android.ui.components.ArtistCollage
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.CalendarPlus
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Grid3x3
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.List
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Type
import com.composables.icons.lucide.User
import com.example.looper_android.data.db.AlbumEntity
import com.example.looper_android.data.db.ArtistEntity
import com.example.looper_android.data.db.PlaylistEntity
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.player.SongOptionsSheet
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection
import com.example.looper_android.ui.widgets.FastScroller
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Play
import com.example.looper_android.ui.components.SelectionTopBar
import com.example.looper_android.ui.components.MultiSelectBottomBar
import com.example.looper_android.ui.components.AddToPlaylistBottomSheet
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import kotlin.math.abs

// --- SHARED CATEGORY TOP BAR MATCHING FLUTTER CategoryDetailWrapper 1-TO-1 ---
@Composable
fun CategoryTopBar(
    title: String,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actions()
        }
    }
}

// 1. ALBUMS SCREEN (Supports View Mode: Grid 2, Grid 3, List + Shuffle, Sort & Multi-Selection)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit = {},
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val albums by viewModel.albums.collectAsState()
    val albumSongCounts by viewModel.albumSongCounts.collectAsState()
    val isDbLoaded by viewModel.isDbLoaded.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val allSongsViewModel: AllSongsViewModel = hiltViewModel()
    val allSongs by allSongsViewModel.songs.collectAsState()
    val pendingDelete by allSongsViewModel.pendingDeletePermission.collectAsState()
    val pendingBatchDelete by allSongsViewModel.pendingBatchDeletePermission.collectAsState()
    val playlistsViewModel: PlaylistsViewModel = hiltViewModel()
    val settingsViewModel: com.example.looper_android.ui.settings.SettingsViewModel = hiltViewModel()
    val showCardPlayButton by settingsViewModel.showCardPlayButton.collectAsState()
    val showCardOptionsButton by settingsViewModel.showCardOptionsButton.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("library_prefs", Context.MODE_PRIVATE) }
    var cardStyle by remember {
        mutableStateOf(
            try {
                CardStyle.valueOf(prefs.getString("album_card_style", "NORMAL") ?: "NORMAL")
            } catch (_: Exception) {
                CardStyle.NORMAL
            }
        )
    }
    var showSortSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    val collectionDetailViewModel: CollectionDetailViewModel = hiltViewModel()
    var selectedAlbumForOptions by remember { mutableStateOf<AlbumEntity?>(null) }

    var selectedAlbums by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val selectionBottomBar = com.example.looper_android.ui.components.LocalSelectionBottomBar.current
    val isAllSelected = selectedAlbums.isNotEmpty() && selectedAlbums.size == albums.size
    val hazeState = com.example.looper_android.ui.widgets.LocalHazeState.current
    val isBlurEnabled = com.example.looper_android.ui.widgets.LocalIsBlurEnabled.current
    val isDarkTheme = androidx.compose.material3.MaterialTheme.colorScheme.background == Color.Black

    val singleDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onDeletePermissionGranted()
        } else {
            allSongsViewModel.onDeletePermissionDenied()
        }
    }

    val batchDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onBatchDeletePermissionGranted()
        } else {
            allSongsViewModel.onBatchDeletePermissionDenied()
        }
    }

    LaunchedEffect(pendingDelete) {
        pendingDelete?.let {
            singleDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    LaunchedEffect(pendingBatchDelete) {
        pendingBatchDelete?.let {
            batchDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedAlbums = emptySet()
    }

    LaunchedEffect(isSelectionMode) {
        playbackViewModel.setSelectionActive(isSelectionMode)
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackViewModel.setSelectionActive(false)
            selectionBottomBar.value = null
        }
    }

    LaunchedEffect(isSelectionMode, selectedAlbums, allSongs, isAllSelected, playbackState.accentColor) {
        if (isSelectionMode) {
            selectionBottomBar.value = {
                MultiSelectBottomBar(
                    isVisible = true,
                    accentColor = playbackState.accentColor,

                    onPlay = {
                        val songsToPlay = allSongs.filter { it.album != null && selectedAlbums.contains(it.album) }
                        if (songsToPlay.isNotEmpty()) {
                            playbackViewModel.setPlaylist(songsToPlay, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Albums")
                        }
                        isSelectionMode = false
                        selectedAlbums = emptySet()
                    },
                    onShuffle = {
                        val songsToPlay = allSongs.filter { it.album != null && selectedAlbums.contains(it.album) }.shuffled()
                        if (songsToPlay.isNotEmpty()) {
                            playbackViewModel.setPlaylist(songsToPlay, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Albums")
                        }
                        isSelectionMode = false
                        selectedAlbums = emptySet()
                    },
                    onPlayNext = {
                        val songsToQueue = allSongs.filter { it.album != null && selectedAlbums.contains(it.album) }
                        if (songsToQueue.isNotEmpty()) {
                            playbackViewModel.addNext(songsToQueue)
                        }
                        isSelectionMode = false
                        selectedAlbums = emptySet()
                    },
                    onAddToQueue = {
                        val songsToQueue = allSongs.filter { it.album != null && selectedAlbums.contains(it.album) }
                        if (songsToQueue.isNotEmpty()) {
                            playbackViewModel.addToQueue(songsToQueue)
                        }
                        isSelectionMode = false
                        selectedAlbums = emptySet()
                    },
                    onAddToPlaylist = {
                        showAddToPlaylistSheet = true
                    },
                    showFavorite = true,
                    onToggleFavorite = {
                        val songsToFav = allSongs.filter { it.album != null && selectedAlbums.contains(it.album) }
                        if (songsToFav.isNotEmpty()) {
                            val anyUnfav = songsToFav.any { !it.isFavorite }
                            playbackViewModel.toggleFavorites(songsToFav, targetState = anyUnfav)
                        }
                        isSelectionMode = false
                        selectedAlbums = emptySet()
                    },
                    showDelete = true,
                    onDelete = {
                        showDeleteConfirmationDialog = true
                    }
                )
            }
        } else {
            selectionBottomBar.value = null
        }
    }

    val bottomPadding = if (playbackState.currentSong != null) 200.dp else 140.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedAlbums.size,
                    totalCount = albums.size,
                    onClearSelection = {
                        selectedAlbums = emptySet()
                        isSelectionMode = false
                    },
                    onToggleSelectAll = {
                        if (selectedAlbums.size == albums.size) {
                            selectedAlbums = emptySet()
                            isSelectionMode = false
                        } else {
                            selectedAlbums = albums.map { it.name }.toSet()
                        }
                    },
                    accentColor = playbackState.accentColor,
                    hasStatusBarsPadding = true
                )
            } else {
                CategoryTopBar(
                    title = "Albums",
                    onBackClick = onBackClick,
                    actions = {
                        PremiumSection(
                            width = 48.dp,
                            height = 48.dp,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            onClick = onNavigateToSearch
                        ) {
                            Icon(Lucide.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Shuffle Button
                        PremiumSection(
                            width = 48.dp,
                            height = 48.dp,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            shape = CircleShape,
                            onClick = {
                                if (albums.isNotEmpty() && allSongs.isNotEmpty()) {
                                    scope.launch(Dispatchers.Default) {
                                        val albumMap = mutableMapOf<String, MutableList<SongEntity>>()
                                        for (song in allSongs) {
                                            val key = (song.album ?: "Unknown Album").lowercase()
                                            albumMap.getOrPut(key) { mutableListOf() }.add(song)
                                        }
                                        val shuffledAlbums = albums.shuffled()
                                        val songsInAlbumOrder = ArrayList<SongEntity>(allSongs.size)
                                        for (album in shuffledAlbums) {
                                            val songsForAlbum = albumMap[album.name.lowercase()]
                                            if (songsForAlbum != null) {
                                                songsInAlbumOrder.addAll(songsForAlbum.sortedBy { it.trackNumber ?: 0 })
                                            }
                                        }
                                        withContext(Dispatchers.Main) {
                                            if (songsInAlbumOrder.isNotEmpty()) {
                                                playbackViewModel.setPlaylist(songsInAlbumOrder, initialIndex = 0, disableShuffleMode = true, queueName = "Albums Shuffle")
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Lucide.Shuffle, contentDescription = "Shuffle", tint = Color.White, modifier = Modifier.size(16.dp))
                        }

                        // Sort & Layout Button (Lucide.ArrowUpDown)
                        PremiumSection(
                            width = 48.dp,
                            height = 48.dp,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            shape = CircleShape,
                            onClick = { showSortSheet = true }
                        ) {
                            Icon(Lucide.SlidersHorizontal, contentDescription = "Sort & Layout", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }

            if (!isDbLoaded && albums.isEmpty()) {
                com.example.looper_android.ui.components.PremiumLoadingView(message = "LOADING ALBUMS")
            } else if (albums.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No albums found", color = Color.Gray, fontFamily = DmSansFontFamily)
                }
            } else if (viewMode == ViewMode.LIST) {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(albums, key = { it.id }) { album ->
                            val count = albumSongCounts[album.name] ?: 0
                            val isSelected = selectedAlbums.contains(album.name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) playbackState.accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) {
                                                val next = if (isSelected) selectedAlbums - album.name else selectedAlbums + album.name
                                                selectedAlbums = next
                                                if (next.isEmpty()) isSelectionMode = false
                                            } else {
                                                onAlbumClick(album.name)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                isSelectionMode = true
                                                selectedAlbums = setOf(album.name)
                                            }
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    OptimizedImage(
                                        url = album.artPath,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(playbackState.accentColor.copy(alpha = 0.7f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = album.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = DmSansFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${album.artist ?: "Unknown Artist"} • $count ${if (count == 1) "song" else "songs"}",
                                        color = Color.White.copy(alpha = 0.38f),
                                        fontSize = 12.sp,
                                        fontFamily = DmSansFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!isSelectionMode && showCardPlayButton) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(playbackState.accentColor.copy(alpha = 0.18f))
                                            .clickable {
                                                val albumSongs = allSongs
                                                    .filter { (it.album ?: "Unknown Album").equals(album.name, ignoreCase = true) }
                                                    .sortedBy { it.trackNumber ?: 0 }
                                                if (albumSongs.isNotEmpty()) {
                                                    playbackViewModel.setPlaylist(albumSongs, initialIndex = 0, disableShuffleMode = true, queueName = album.name)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Play, contentDescription = "Play Album", tint = playbackState.accentColor, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                if (!isSelectionMode && showCardOptionsButton) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .clickable { selectedAlbumForOptions = album },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.EllipsisVertical, contentDescription = "Album Options", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(Lucide.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.38f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    FastScroller(
                        listState = listState,
                        thumbColor = playbackState.accentColor,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            } else {
                val gridState = rememberLazyGridState()
                Box(modifier = Modifier.fillMaxSize()) {
                    val columnsCount = if (viewMode == ViewMode.GRID_3) 3 else 2
                    val cardPadding = when {
                        cardStyle == CardStyle.IMAGE_GAPLESS -> 0.dp
                        viewMode == ViewMode.GRID_3 -> 12.dp
                        else -> 16.dp
                    }
                    val cornerRadius = when {
                        cardStyle == CardStyle.IMAGE_GAPLESS -> 0.dp
                        viewMode == ViewMode.GRID_3 -> 14.dp
                        else -> 20.dp
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columnsCount),
                        contentPadding = PaddingValues(start = cardPadding, end = cardPadding, top = if (cardStyle == CardStyle.IMAGE_GAPLESS) 0.dp else 16.dp, bottom = bottomPadding),
                        horizontalArrangement = Arrangement.spacedBy(cardPadding),
                        verticalArrangement = Arrangement.spacedBy(cardPadding),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(albums, key = { it.id }) { album ->
                            val isSelected = selectedAlbums.contains(album.name)
                            val itemModifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            val next = if (isSelected) selectedAlbums - album.name else selectedAlbums + album.name
                                            selectedAlbums = next
                                            if (next.isEmpty()) isSelectionMode = false
                                        } else {
                                            onAlbumClick(album.name)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                            selectedAlbums = setOf(album.name)
                                        }
                                    }
                                )

                            if (cardStyle == CardStyle.IMAGE || cardStyle == CardStyle.IMAGE_GAPLESS) {
                                Box(
                                    modifier = itemModifier
                                        .aspectRatio(1f)
                                        .clip(if (cardStyle == CardStyle.IMAGE_GAPLESS) RoundedCornerShape(0.dp) else RoundedCornerShape(cornerRadius))
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    OptimizedImage(
                                        url = album.artPath,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Gradient overlay with text
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                )
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = album.name,
                                                color = Color.White,
                                                fontSize = if (viewMode == ViewMode.GRID_3) 13.sp else 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = DmSansFontFamily,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = album.artist ?: "Unknown Artist",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 11.sp,
                                                fontFamily = DmSansFontFamily,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (!isSelectionMode && showCardPlayButton) {
                                        Box(
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .size(32.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(playbackState.accentColor)
                                                .clickable {
                                                    val albumSongs = allSongs
                                                        .filter { (it.album ?: "Unknown Album").equals(album.name, ignoreCase = true) }
                                                        .sortedBy { it.trackNumber ?: 0 }
                                                    if (albumSongs.isNotEmpty()) {
                                                        playbackViewModel.setPlaylist(albumSongs, initialIndex = 0, disableShuffleMode = true, queueName = album.name)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.Play, contentDescription = "Play Album", tint = Color.Black, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    if (!isSelectionMode && showCardOptionsButton) {
                                        Box(
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .size(28.dp)
                                                .align(Alignment.TopEnd)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.55f))
                                                .clickable { selectedAlbumForOptions = album },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.EllipsisVertical, contentDescription = "Album Options", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(playbackState.accentColor.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(playbackState.accentColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Lucide.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                val cardShape = if (cardStyle == CardStyle.CIRCLE) CircleShape else RoundedCornerShape(cornerRadius)
                                val containerModifier = if (cardStyle == CardStyle.CARD) {
                                    itemModifier
                                        .clip(RoundedCornerShape(cornerRadius + 4.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(8.dp)
                                } else {
                                    itemModifier
                                }

                                Column(
                                    modifier = containerModifier,
                                    horizontalAlignment = if (cardStyle == CardStyle.CIRCLE) Alignment.CenterHorizontally else Alignment.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                    ) {
                                        OptimizedImage(
                                            url = album.artPath,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(cardShape)
                                                .background(Color.White.copy(alpha = 0.05f)),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (!isSelectionMode && showCardPlayButton) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .size(34.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .clip(CircleShape)
                                                    .background(playbackState.accentColor)
                                                    .clickable {
                                                        val albumSongs = allSongs
                                                            .filter { (it.album ?: "Unknown Album").equals(album.name, ignoreCase = true) }
                                                            .sortedBy { it.trackNumber ?: 0 }
                                                        if (albumSongs.isNotEmpty()) {
                                                            playbackViewModel.setPlaylist(albumSongs, initialIndex = 0, disableShuffleMode = true, queueName = album.name)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Lucide.Play, contentDescription = "Play Album", tint = Color.Black, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        if (!isSelectionMode && showCardOptionsButton) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .size(28.dp)
                                                    .align(Alignment.TopEnd)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.55f))
                                                    .clickable { selectedAlbumForOptions = album },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Lucide.EllipsisVertical, contentDescription = "Album Options", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(cardShape)
                                                    .background(playbackState.accentColor.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(playbackState.accentColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Lucide.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = album.name,
                                        color = Color.White,
                                        fontSize = if (viewMode == ViewMode.GRID_3) 13.sp else 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = DmSansFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = if (cardStyle == CardStyle.CIRCLE) TextAlign.Center else TextAlign.Start,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Text(
                                        text = album.artist ?: "Unknown Artist",
                                        color = Color.White.copy(alpha = 0.38f),
                                        fontSize = 12.sp,
                                        fontFamily = DmSansFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = if (cardStyle == CardStyle.CIRCLE) TextAlign.Center else TextAlign.Start,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    FastScroller(
                        gridState = gridState,
                        thumbColor = playbackState.accentColor,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }

    if (showAddToPlaylistSheet) {
        AddToPlaylistBottomSheet(
            onDismiss = { showAddToPlaylistSheet = false },
            onPlaylistSelected = { playlist ->
                val songsToAdd = allSongs.filter { it.album != null && selectedAlbums.contains(it.album) }
                playlistsViewModel.addSongsToPlaylist(playlist, songsToAdd.map { it.path })
                isSelectionMode = false
                selectedAlbums = emptySet()
            },
            accentColor = playbackState.accentColor
        )
    }

    if (showDeleteConfirmationDialog) {
        val songsToDelete = allSongs.filter { it.album != null && selectedAlbums.contains(it.album) }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Delete songs in ${selectedAlbums.size} albums?", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily) },
            text = { Text("This will permanently delete ${songsToDelete.size} songs from your device.", fontFamily = DmSansFontFamily) },
            confirmButton = {
                Button(
                    onClick = {
                        allSongsViewModel.deleteSongs(songsToDelete)
                        showDeleteConfirmationDialog = false
                        isSelectionMode = false
                        selectedAlbums = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White, fontFamily = DmSansFontFamily)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }

    if (showSortSheet) {
        val sortOptions = listOf(
            CategorySortOptionItem(AlbumSortOption.NAME, "Name", Lucide.Type),
            CategorySortOptionItem(AlbumSortOption.ARTIST, "Artist", Lucide.User),
            CategorySortOptionItem(AlbumSortOption.SONG_COUNT, "Number of Songs", Lucide.ListMusic),
            CategorySortOptionItem(AlbumSortOption.DATE_ADDED, "Date Added", Lucide.CalendarPlus),
            CategorySortOptionItem(AlbumSortOption.YEAR, "Year", Lucide.Calendar)
        )
        CategorySortBottomSheet(
            options = sortOptions,
            selectedOption = sortOption,
            isAscending = isAscending,
            onOptionSelected = { viewModel.setSortOption(it) },
            onToggleAscending = { viewModel.toggleAscending() },
            viewMode = viewMode,
            onViewModeChange = { viewModel.setViewMode(it) },
            cardStyle = cardStyle,
            onCardStyleChange = {
                cardStyle = it
                prefs.edit().putString("album_card_style", it.name).apply()
            },
            onDismiss = { showSortSheet = false },
            accentColor = playbackState.accentColor
        )
    }

    selectedAlbumForOptions?.let { album ->
        val albumSongs = remember(album.name, allSongs) {
            allSongs.filter { (it.album ?: "Unknown Album").equals(album.name, ignoreCase = true) }.sortedBy { it.trackNumber ?: 0 }
        }
        AlbumOptionsBottomSheet(
            album = album,
            songs = albumSongs,
            accentColor = playbackState.accentColor,
            onDismiss = { selectedAlbumForOptions = null },
            onPlay = {
                if (albumSongs.isNotEmpty()) {
                    playbackViewModel.setPlaylist(albumSongs, initialIndex = 0, disableShuffleMode = true, queueName = album.name)
                }
                selectedAlbumForOptions = null
            },
            onPlayNext = {
                if (albumSongs.isNotEmpty()) playbackViewModel.addNext(albumSongs)
                selectedAlbumForOptions = null
            },
            onAddToQueue = {
                if (albumSongs.isNotEmpty()) playbackViewModel.addToQueue(albumSongs)
                selectedAlbumForOptions = null
            },
            onAddToPlaylist = {
                selectedAlbums = setOf(album.name)
                showAddToPlaylistSheet = true
                selectedAlbumForOptions = null
            },
            onToggleFavorite = {
                val anyUnfav = albumSongs.any { !it.isFavorite }
                playbackViewModel.toggleFavorites(albumSongs, targetState = anyUnfav)
                selectedAlbumForOptions = null
            },
            onDelete = {
                selectedAlbums = setOf(album.name)
                showDeleteConfirmationDialog = true
                selectedAlbumForOptions = null
            }
        )
    }
}

// 2. ARTISTS SCREEN (Supports View Mode: Grid 2, Grid 3, List + Shuffle, Sort & Multi-Selection)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistsScreen(
    viewModel: ArtistsViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit = {},
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    val artists by viewModel.artists.collectAsState()
    val songCounts by viewModel.artistSongCounts.collectAsState()
    val isDbLoaded by viewModel.isDbLoaded.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val allSongsViewModel: AllSongsViewModel = hiltViewModel()
    val allSongs by allSongsViewModel.songs.collectAsState()
    val pendingDelete by allSongsViewModel.pendingDeletePermission.collectAsState()
    val pendingBatchDelete by allSongsViewModel.pendingBatchDeletePermission.collectAsState()
    val playlistsViewModel: PlaylistsViewModel = hiltViewModel()
    val settingsViewModel: com.example.looper_android.ui.settings.SettingsViewModel = hiltViewModel()
    val showCardPlayButton by settingsViewModel.showCardPlayButton.collectAsState()
    val showCardOptionsButton by settingsViewModel.showCardOptionsButton.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("library_prefs", Context.MODE_PRIVATE) }
    var cardStyle by remember {
        mutableStateOf(
            try {
                CardStyle.valueOf(prefs.getString("artist_card_style", "CIRCLE") ?: "CIRCLE")
            } catch (_: Exception) {
                CardStyle.CIRCLE
            }
        )
    }
    var showSortSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    val collectionDetailViewModel: CollectionDetailViewModel = hiltViewModel()
    var selectedArtistForOptions by remember { mutableStateOf<ArtistEntity?>(null) }

    var selectedArtists by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val selectionBottomBar = com.example.looper_android.ui.components.LocalSelectionBottomBar.current
    val isAllSelected = selectedArtists.isNotEmpty() && selectedArtists.size == artists.size

    val singleDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onDeletePermissionGranted()
        } else {
            allSongsViewModel.onDeletePermissionDenied()
        }
    }

    val batchDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onBatchDeletePermissionGranted()
        } else {
            allSongsViewModel.onBatchDeletePermissionDenied()
        }
    }

    LaunchedEffect(pendingDelete) {
        pendingDelete?.let {
            singleDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    LaunchedEffect(pendingBatchDelete) {
        pendingBatchDelete?.let {
            batchDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedArtists = emptySet()
    }

    LaunchedEffect(isSelectionMode) {
        playbackViewModel.setSelectionActive(isSelectionMode)
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackViewModel.setSelectionActive(false)
            selectionBottomBar.value = null
        }
    }

    LaunchedEffect(isSelectionMode, selectedArtists, allSongs, isAllSelected, playbackState.accentColor) {
        if (isSelectionMode) {
            selectionBottomBar.value = {
                MultiSelectBottomBar(
                    isVisible = true,
                    accentColor = playbackState.accentColor,

                    onPlay = {
                        val songsToPlay = allSongs.filter {
                            val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                            selectedArtists.any { sel -> sel.equals(effectiveArtist, ignoreCase = true) || (it.artist != null && sel.equals(it.artist, ignoreCase = true)) }
                        }
                        if (songsToPlay.isNotEmpty()) {
                            playbackViewModel.setPlaylist(songsToPlay, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Artists")
                        }
                        isSelectionMode = false
                        selectedArtists = emptySet()
                    },
                    onShuffle = {
                        val songsToPlay = allSongs.filter {
                            val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                            selectedArtists.any { sel -> sel.equals(effectiveArtist, ignoreCase = true) || (it.artist != null && sel.equals(it.artist, ignoreCase = true)) }
                        }.shuffled()
                        if (songsToPlay.isNotEmpty()) {
                            playbackViewModel.setPlaylist(songsToPlay, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Artists")
                        }
                        isSelectionMode = false
                        selectedArtists = emptySet()
                    },
                    onPlayNext = {
                        val songsToQueue = allSongs.filter {
                            val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                            selectedArtists.any { sel -> sel.equals(effectiveArtist, ignoreCase = true) || (it.artist != null && sel.equals(it.artist, ignoreCase = true)) }
                        }
                        if (songsToQueue.isNotEmpty()) {
                            playbackViewModel.addNext(songsToQueue)
                        }
                        isSelectionMode = false
                        selectedArtists = emptySet()
                    },
                    onAddToQueue = {
                        val songsToQueue = allSongs.filter {
                            val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                            selectedArtists.any { sel -> sel.equals(effectiveArtist, ignoreCase = true) || (it.artist != null && sel.equals(it.artist, ignoreCase = true)) }
                        }
                        if (songsToQueue.isNotEmpty()) {
                            playbackViewModel.addToQueue(songsToQueue)
                        }
                        isSelectionMode = false
                        selectedArtists = emptySet()
                    },
                    onAddToPlaylist = {
                        showAddToPlaylistSheet = true
                    },
                    showMerge = selectedArtists.size == 2,
                    onMerge = {
                        showMergeDialog = true
                    },
                    showFavorite = true,
                    onToggleFavorite = {
                        val songsToFav = allSongs.filter {
                            val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                            selectedArtists.any { sel -> sel.equals(effectiveArtist, ignoreCase = true) || (it.artist != null && sel.equals(it.artist, ignoreCase = true)) }
                        }
                        if (songsToFav.isNotEmpty()) {
                            val anyUnfav = songsToFav.any { !it.isFavorite }
                            playbackViewModel.toggleFavorites(songsToFav, targetState = anyUnfav)
                        }
                        isSelectionMode = false
                        selectedArtists = emptySet()
                    },
                    showDelete = true,
                    onDelete = {
                        showDeleteConfirmationDialog = true
                    }
                )
            }
        } else {
            selectionBottomBar.value = null
        }
    }

    val bottomPadding = if (playbackState.currentSong != null) 200.dp else 140.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedArtists.size,
                    totalCount = artists.size,
                    onClearSelection = {
                        selectedArtists = emptySet()
                        isSelectionMode = false
                    },
                    onToggleSelectAll = {
                        if (selectedArtists.size == artists.size) {
                            selectedArtists = emptySet()
                            isSelectionMode = false
                        } else {
                            selectedArtists = artists.map { it.name }.toSet()
                        }
                    },
                    accentColor = playbackState.accentColor,
                    hasStatusBarsPadding = true
                )
            } else {
                CategoryTopBar(
                    title = "Artists",
                    onBackClick = onBackClick,
                    actions = {
                        PremiumSection(
                            width = 48.dp,
                            height = 48.dp,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            onClick = onNavigateToSearch
                        ) {
                            Icon(Lucide.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Shuffle Button
                        PremiumSection(
                            width = 48.dp,
                            height = 48.dp,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            shape = CircleShape,
                            onClick = {
                                if (artists.isNotEmpty() && allSongs.isNotEmpty()) {
                                    scope.launch(Dispatchers.Default) {
                                        val artistMap = mutableMapOf<String, MutableList<SongEntity>>()
                                        for (song in allSongs) {
                                            val effectiveArtist = song.albumArtist?.trim()?.ifBlank { null } ?: song.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                                            artistMap.getOrPut(effectiveArtist.lowercase()) { mutableListOf() }.add(song)
                                            val rawArtist = song.artist?.trim()?.ifBlank { null }
                                            if (rawArtist != null && !rawArtist.equals(effectiveArtist, ignoreCase = true)) {
                                                artistMap.getOrPut(rawArtist.lowercase()) { mutableListOf() }.add(song)
                                            }
                                        }
                                        val shuffledArtists = artists.shuffled()
                                        val songsInArtistOrder = ArrayList<SongEntity>(allSongs.size)
                                        val seenSongIds = HashSet<Long>()
                                        for (artist in shuffledArtists) {
                                            val songsForArtist = artistMap[artist.name.lowercase()]
                                            if (songsForArtist != null) {
                                                val sortedSongs = songsForArtist.sortedBy { it.album ?: "" }
                                                for (s in sortedSongs) {
                                                    if (seenSongIds.add(s.id)) {
                                                        songsInArtistOrder.add(s)
                                                    }
                                                }
                                            }
                                        }
                                        withContext(Dispatchers.Main) {
                                            if (songsInArtistOrder.isNotEmpty()) {
                                                playbackViewModel.setPlaylist(songsInArtistOrder, initialIndex = 0, disableShuffleMode = true, queueName = "Artists Shuffle")
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Lucide.Shuffle, contentDescription = "Shuffle", tint = Color.White, modifier = Modifier.size(16.dp))
                        }

                        // Sort & Layout Button (Lucide.ArrowUpDown)
                        PremiumSection(
                            width = 48.dp,
                            height = 48.dp,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            shape = CircleShape,
                            onClick = { showSortSheet = true }
                        ) {
                            Icon(Lucide.SlidersHorizontal, contentDescription = "Sort & Layout", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }

            if (!isDbLoaded && artists.isEmpty()) {
                com.example.looper_android.ui.components.PremiumLoadingView(message = "LOADING ARTISTS")
            } else if (artists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No artists found", color = Color.Gray, fontFamily = DmSansFontFamily)
                }
            } else if (viewMode == ViewMode.LIST) {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(artists, key = { it.name }) { artist ->
                            if (artist.artistImageUrl.isNullOrEmpty()) {
                                LaunchedEffect(artist.name) {
                                    viewModel.fetchArtistImage(artist.name)
                                }
                            }
                            val count = songCounts[artist.name] ?: 0
                            val isSelected = selectedArtists.contains(artist.name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) playbackState.accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) {
                                                val next = if (isSelected) selectedArtists - artist.name else selectedArtists + artist.name
                                                selectedArtists = next
                                                if (next.isEmpty()) isSelectionMode = false
                                            } else {
                                                onArtistClick(artist.name)
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                isSelectionMode = true
                                                selectedArtists = setOf(artist.name)
                                            }
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(52.dp)) {
                                    ArtistCollage(
                                        artistImageUrl = artist.artistImageUrl ?: artist.artPath,
                                        modifier = Modifier.fillMaxSize(),
                                        shape = CircleShape
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(playbackState.accentColor.copy(alpha = 0.7f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artist.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = DmSansFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$count ${if (count == 1) "song" else "songs"}",
                                        color = Color.White.copy(alpha = 0.38f),
                                        fontSize = 12.sp,
                                        fontFamily = DmSansFontFamily
                                    )
                                }
                                if (!isSelectionMode && showCardPlayButton) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(playbackState.accentColor.copy(alpha = 0.18f))
                                            .clickable {
                                                val artistSongs = allSongs
                                                    .filter {
                                                        val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                                                        effectiveArtist.equals(artist.name, ignoreCase = true) || (it.artist?.equals(artist.name, ignoreCase = true) == true)
                                                    }
                                                    .sortedBy { it.album ?: "" }
                                                if (artistSongs.isNotEmpty()) {
                                                    playbackViewModel.setPlaylist(artistSongs, initialIndex = 0, disableShuffleMode = true, queueName = artist.name)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.Play, contentDescription = "Play Artist", tint = playbackState.accentColor, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                if (!isSelectionMode && showCardOptionsButton) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .clickable { selectedArtistForOptions = artist },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Lucide.EllipsisVertical, contentDescription = "Artist Options", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(Lucide.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.38f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    FastScroller(
                        listState = listState,
                        thumbColor = playbackState.accentColor,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            } else {
                val gridState = rememberLazyGridState()
                Box(modifier = Modifier.fillMaxSize()) {
                    val columnsCount = if (viewMode == ViewMode.GRID_3) 3 else 2
                    val cardPadding = when {
                        cardStyle == CardStyle.IMAGE_GAPLESS -> 0.dp
                        viewMode == ViewMode.GRID_3 -> 12.dp
                        else -> 16.dp
                    }
                    val cornerRadius = when {
                        cardStyle == CardStyle.IMAGE_GAPLESS -> 0.dp
                        viewMode == ViewMode.GRID_3 -> 14.dp
                        else -> 20.dp
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columnsCount),
                        contentPadding = PaddingValues(start = cardPadding, end = cardPadding, top = if (cardStyle == CardStyle.IMAGE_GAPLESS) 0.dp else 16.dp, bottom = bottomPadding),
                        horizontalArrangement = Arrangement.spacedBy(cardPadding),
                        verticalArrangement = Arrangement.spacedBy(cardPadding),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(artists, key = { it.name }) { artist ->
                            if (artist.artistImageUrl.isNullOrEmpty()) {
                                LaunchedEffect(artist.name) {
                                    viewModel.fetchArtistImage(artist.name)
                                }
                            }
                            val count = songCounts[artist.name] ?: 0
                            val isSelected = selectedArtists.contains(artist.name)
                            val itemModifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            val next = if (isSelected) selectedArtists - artist.name else selectedArtists + artist.name
                                            selectedArtists = next
                                            if (next.isEmpty()) isSelectionMode = false
                                        } else {
                                            onArtistClick(artist.name)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                            selectedArtists = setOf(artist.name)
                                        }
                                    }
                                )

                            if (cardStyle == CardStyle.IMAGE || cardStyle == CardStyle.IMAGE_GAPLESS) {
                                Box(
                                    modifier = itemModifier
                                        .aspectRatio(1f)
                                        .clip(if (cardStyle == CardStyle.IMAGE_GAPLESS) RoundedCornerShape(0.dp) else RoundedCornerShape(cornerRadius))
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    ArtistCollage(
                                        artistImageUrl = artist.artistImageUrl ?: artist.artPath,
                                        modifier = Modifier.fillMaxSize(),
                                        shape = if (cardStyle == CardStyle.IMAGE_GAPLESS) RoundedCornerShape(0.dp) else RoundedCornerShape(cornerRadius)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                )
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = artist.name,
                                                color = Color.White,
                                                fontSize = if (viewMode == ViewMode.GRID_3) 13.sp else 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = DmSansFontFamily,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "$count ${if (count == 1) "song" else "songs"}",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 11.sp,
                                                fontFamily = DmSansFontFamily
                                            )
                                        }
                                    }
                                    if (!isSelectionMode && showCardPlayButton) {
                                        Box(
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .size(32.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(playbackState.accentColor)
                                                .clickable {
                                                    val artistSongs = allSongs
                                                        .filter {
                                                            val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                                                            effectiveArtist.equals(artist.name, ignoreCase = true) || (it.artist?.equals(artist.name, ignoreCase = true) == true)
                                                        }
                                                        .sortedBy { it.album ?: "" }
                                                    if (artistSongs.isNotEmpty()) {
                                                        playbackViewModel.setPlaylist(artistSongs, initialIndex = 0, disableShuffleMode = true, queueName = artist.name)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.Play, contentDescription = "Play Artist", tint = Color.Black, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    if (!isSelectionMode && showCardOptionsButton) {
                                        Box(
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .size(28.dp)
                                                .align(Alignment.TopEnd)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.55f))
                                                .clickable { selectedArtistForOptions = artist },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Lucide.EllipsisVertical, contentDescription = "Artist Options", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(playbackState.accentColor.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(playbackState.accentColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Lucide.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                val cardShape = if (cardStyle == CardStyle.CIRCLE) CircleShape else RoundedCornerShape(cornerRadius)
                                val containerModifier = if (cardStyle == CardStyle.CARD) {
                                    itemModifier
                                        .clip(RoundedCornerShape(cornerRadius + 4.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(8.dp)
                                } else {
                                    itemModifier
                                }

                                Column(
                                    modifier = containerModifier,
                                    horizontalAlignment = if (cardStyle == CardStyle.CIRCLE) Alignment.CenterHorizontally else Alignment.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                    ) {
                                        ArtistCollage(
                                            artistImageUrl = artist.artistImageUrl ?: artist.artPath,
                                            modifier = Modifier.fillMaxSize(),
                                            shape = cardShape
                                        )
                                        if (!isSelectionMode && showCardPlayButton) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(6.dp)
                                                    .size(34.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .clip(CircleShape)
                                                    .background(playbackState.accentColor)
                                                    .clickable {
                                                        val artistSongs = allSongs
                                                            .filter {
                                                                val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                                                                effectiveArtist.equals(artist.name, ignoreCase = true) || (it.artist?.equals(artist.name, ignoreCase = true) == true)
                                                            }
                                                            .sortedBy { it.album ?: "" }
                                                        if (artistSongs.isNotEmpty()) {
                                                            playbackViewModel.setPlaylist(artistSongs, initialIndex = 0, disableShuffleMode = true, queueName = artist.name)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Lucide.Play, contentDescription = "Play Artist", tint = Color.Black, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        if (!isSelectionMode && showCardOptionsButton) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(6.dp)
                                                    .size(28.dp)
                                                    .align(Alignment.TopEnd)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.55f))
                                                    .clickable { selectedArtistForOptions = artist },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Lucide.EllipsisVertical, contentDescription = "Artist Options", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(cardShape)
                                                    .background(playbackState.accentColor.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(playbackState.accentColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Lucide.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = artist.name,
                                        color = Color.White,
                                        fontSize = if (viewMode == ViewMode.GRID_3) 13.sp else 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = DmSansFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = if (cardStyle == CardStyle.CIRCLE) TextAlign.Center else TextAlign.Start,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Text(
                                        text = "$count ${if (count == 1) "song" else "songs"}",
                                        color = Color.White.copy(alpha = 0.38f),
                                        fontSize = 12.sp,
                                        fontFamily = DmSansFontFamily,
                                        textAlign = if (cardStyle == CardStyle.CIRCLE) TextAlign.Center else TextAlign.Start,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    FastScroller(
                        gridState = gridState,
                        thumbColor = playbackState.accentColor,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }

    if (showAddToPlaylistSheet) {
        AddToPlaylistBottomSheet(
            onDismiss = { showAddToPlaylistSheet = false },
            onPlaylistSelected = { playlist ->
                val songsToAdd = allSongs.filter {
                    val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                    selectedArtists.any { sel -> sel.equals(effectiveArtist, ignoreCase = true) || (it.artist != null && sel.equals(it.artist, ignoreCase = true)) }
                }
                playlistsViewModel.addSongsToPlaylist(playlist, songsToAdd.map { it.path })
                isSelectionMode = false
                selectedArtists = emptySet()
            },
            accentColor = playbackState.accentColor
        )
    }

    if (showMergeDialog && selectedArtists.size == 2) {
        val artistList = selectedArtists.toList()
        var targetArtist by remember { mutableStateOf(artistList[0]) }
        val sourceArtist = if (targetArtist == artistList[0]) artistList[1] else artistList[0]

        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Text(
                    text = "Merge Artists",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose the primary artist name to keep. All songs by '$sourceArtist' will be updated to '$targetArtist':",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                        fontFamily = DmSansFontFamily
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    artistList.forEach { artistName ->
                        val isSelected = targetArtist == artistName
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) playbackState.accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                .clickable { targetArtist = artistName }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = isSelected,
                                onClick = { targetArtist = artistName },
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                    selectedColor = playbackState.accentColor,
                                    unselectedColor = Color.White.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = artistName,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = DmSansFontFamily,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMergeDialog = false
                        collectionDetailViewModel.mergeArtists(
                            sourceArtistName = sourceArtist,
                            targetArtistName = targetArtist
                        ) {
                            isSelectionMode = false
                            selectedArtists = emptySet()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = playbackState.accentColor)
                ) {
                    Text("Merge", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

    if (showDeleteConfirmationDialog) {
        val songsToDelete = allSongs.filter {
            val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
            selectedArtists.any { sel -> sel.equals(effectiveArtist, ignoreCase = true) || (it.artist != null && sel.equals(it.artist, ignoreCase = true)) }
        }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Delete songs in ${selectedArtists.size} artists?", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily) },
            text = { Text("This will permanently delete ${songsToDelete.size} songs from your device.", fontFamily = DmSansFontFamily) },
            confirmButton = {
                Button(
                    onClick = {
                        allSongsViewModel.deleteSongs(songsToDelete)
                        showDeleteConfirmationDialog = false
                        isSelectionMode = false
                        selectedArtists = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White, fontFamily = DmSansFontFamily)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }

    if (showSortSheet) {
        val sortOptions = listOf(
            CategorySortOptionItem(ArtistSortOption.NAME, "Name", Lucide.Type),
            CategorySortOptionItem(ArtistSortOption.SONG_COUNT, "Number of Songs", Lucide.ListMusic)
        )
        CategorySortBottomSheet(
            options = sortOptions,
            selectedOption = sortOption,
            isAscending = isAscending,
            onOptionSelected = { viewModel.setSortOption(it) },
            onToggleAscending = { viewModel.toggleAscending() },
            viewMode = viewMode,
            onViewModeChange = { viewModel.setViewMode(it) },
            cardStyle = cardStyle,
            onCardStyleChange = {
                cardStyle = it
                prefs.edit().putString("artist_card_style", it.name).apply()
            },
            onDismiss = { showSortSheet = false },
            accentColor = playbackState.accentColor
        )
    }

    selectedArtistForOptions?.let { artist ->
        val artistSongs = remember(artist.name, allSongs) {
            allSongs.filter {
                val effectiveArtist = it.albumArtist?.trim()?.ifBlank { null } ?: it.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                effectiveArtist.equals(artist.name, ignoreCase = true) || (it.artist?.equals(artist.name, ignoreCase = true) == true)
            }.sortedBy { it.album ?: "" }
        }
        ArtistOptionsBottomSheet(
            artist = artist,
            songs = artistSongs,
            accentColor = playbackState.accentColor,
            onDismiss = { selectedArtistForOptions = null },
            onPlay = {
                if (artistSongs.isNotEmpty()) {
                    playbackViewModel.setPlaylist(artistSongs, initialIndex = 0, disableShuffleMode = true, queueName = artist.name)
                }
                selectedArtistForOptions = null
            },
            onPlayNext = {
                if (artistSongs.isNotEmpty()) playbackViewModel.addNext(artistSongs)
                selectedArtistForOptions = null
            },
            onAddToQueue = {
                if (artistSongs.isNotEmpty()) playbackViewModel.addToQueue(artistSongs)
                selectedArtistForOptions = null
            },
            onAddToPlaylist = {
                selectedArtists = setOf(artist.name)
                showAddToPlaylistSheet = true
                selectedArtistForOptions = null
            },
            onToggleFavorite = {
                val anyUnfav = artistSongs.any { !it.isFavorite }
                playbackViewModel.toggleFavorites(artistSongs, targetState = anyUnfav)
                selectedArtistForOptions = null
            },
            onDelete = {
                selectedArtists = setOf(artist.name)
                showDeleteConfirmationDialog = true
                selectedArtistForOptions = null
            }
        )
    }
}

// 3. PLAYLISTS SCREEN
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel = hiltViewModel(),
    allSongsViewModel: AllSongsViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playlists by viewModel.playlists.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<com.example.looper_android.data.db.PlaylistEntity?>(null) }
    var playlistToRename by remember { mutableStateOf<com.example.looper_android.data.db.PlaylistEntity?>(null) }
    var selectedPlaylistForOptions by remember { mutableStateOf<com.example.looper_android.data.db.PlaylistEntity?>(null) }
    var playlistToExportM3u by remember { mutableStateOf<com.example.looper_android.data.db.PlaylistEntity?>(null) }
    var playlistToExportJson by remember { mutableStateOf<com.example.looper_android.data.db.PlaylistEntity?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val res = viewModel.importPlaylist(uri)
                res.onSuccess {
                    Toast.makeText(context, "Imported '${it.playlistName}' (${it.matchedTracks} songs)", Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(context, "Import failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val exportM3uLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        val p = playlistToExportM3u
        if (uri != null && p != null) {
            coroutineScope.launch {
                val res = viewModel.exportPlaylistToM3u(uri, p)
                res.onSuccess {
                    Toast.makeText(context, "Exported '${p.name}.m3u8' ($it songs)", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Export failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        playlistToExportM3u = null
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val p = playlistToExportJson
        if (uri != null && p != null) {
            coroutineScope.launch {
                val res = viewModel.exportPlaylistToJson(uri, p)
                res.onSuccess {
                    Toast.makeText(context, "Exported '${p.name}.json' ($it songs)", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Export failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        playlistToExportJson = null
    }

    val bottomPadding = if (playbackState.currentSong != null) 200.dp else 140.dp

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryTopBar(
            title = "Playlists",
            onBackClick = onBackClick,
            actions = {
                PremiumSection(
                    width = 44.dp,
                    height = 44.dp,
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { importLauncher.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "application/json", "*/*")) }
                ) {
                    Icon(Lucide.FolderInput, contentDescription = "Import Playlist", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                PremiumSection(
                    width = 44.dp,
                    height = 44.dp,
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    shape = CircleShape,
                    onClick = { showCreateDialog = true }
                ) {
                    Icon(Lucide.Plus, contentDescription = "Create Playlist", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { showCreateDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(playbackState.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Plus, contentDescription = null, tint = playbackState.accentColor, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Playlist",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily
                        )
                        Text(
                            text = "Add a new playlist to your library",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }

            items(playlists, key = { it.id }) { playlist ->
                val songCount = remember(playlist.songPaths) {
                    try {
                        org.json.JSONArray(playlist.songPaths).length()
                    } catch (_: Exception) { 0 }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .clickable { onPlaylistClick(playlist.name) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val parsedSongPaths = remember(playlist.songPaths) {
                        try {
                            val arr = org.json.JSONArray(playlist.songPaths.ifBlank { "[]" })
                            (0 until arr.length()).map { arr.getString(it) }
                        } catch (_: Exception) {
                            playlist.songPaths.split(",").map { it.trim() }
                        }
                    }
                    val allLibrarySongs by allSongsViewModel.songs.collectAsState()
                    PlaylistArtworkCollage(
                        songPaths = parsedSongPaths,
                        allSongs = allLibrarySongs,
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        accentColor = playbackState.accentColor,
                        fallbackIconSize = 24.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$songCount ${if (songCount == 1) "song" else "songs"}",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontFamily = DmSansFontFamily
                        )
                    }
                    IconButton(onClick = { selectedPlaylistForOptions = playlist }) {
                        Icon(Lucide.EllipsisVertical, contentDescription = "Playlist Options", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Delete Playlist", color = Color.White, fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${playlist.name}\"?", color = Color.White.copy(alpha = 0.7f), fontFamily = DmSansFontFamily) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        playlistToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontFamily = DmSansFontFamily)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(20.dp)
        )
    }


    if (selectedPlaylistForOptions != null) {
        val playlist = selectedPlaylistForOptions!!
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { selectedPlaylistForOptions = null },
            containerColor = Color(0xFF1E1E1E),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = playlist.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily
                )
                Spacer(modifier = Modifier.height(16.dp))

                CategoryOptionActionRow(
                    icon = Lucide.Download,
                    title = "Export as M3U8 Playlist (.m3u8)",
                    onClick = {
                        selectedPlaylistForOptions = null
                        playlistToExportM3u = playlist
                        exportM3uLauncher.launch("${playlist.name}.m3u8")
                    },
                    accentColor = playbackState.accentColor
                )

                CategoryOptionActionRow(
                    icon = Lucide.Download,
                    title = "Export as JSON (.json)",
                    onClick = {
                        selectedPlaylistForOptions = null
                        playlistToExportJson = playlist
                        exportJsonLauncher.launch("${playlist.name}.json")
                    },
                    accentColor = playbackState.accentColor
                )

                CategoryOptionActionRow(
                    icon = Lucide.SlidersHorizontal,
                    title = "Rename Playlist",
                    onClick = {
                        val p = playlist
                        selectedPlaylistForOptions = null
                        playlistToRename = p
                    },
                    accentColor = playbackState.accentColor
                )

                CategoryOptionActionRow(
                    icon = Lucide.Trash2,
                    title = "Delete Playlist",
                    onClick = {
                        val p = playlist
                        selectedPlaylistForOptions = null
                        playlistToDelete = p
                    },
                    accentColor = Color(0xFFFF5252),
                    textColor = Color(0xFFFF5252)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (playlistToRename != null) {
        val p = playlistToRename!!
        var newNameText by remember { mutableStateOf(p.name) }
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = { Text("Rename Playlist", color = Color.White, fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newNameText,
                    onValueChange = { newNameText = it },
                    singleLine = true,
                    label = { Text("Playlist Name", fontFamily = DmSansFontFamily) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newNameText.isNotBlank()) {
                            viewModel.renamePlaylist(p, newNameText.trim())
                        }
                        playlistToRename = null
                    }
                ) {
                    Text("Save", color = playbackState.accentColor, fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontFamily = DmSansFontFamily)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showCreateDialog) {
        var newPlaylistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", color = Color.White, fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist Name", color = Color.White.copy(alpha = 0.3f), fontFamily = DmSansFontFamily) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = playbackState.accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim())
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create", color = playbackState.accentColor, fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontFamily = DmSansFontFamily)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// 4. FOLDERS SCREEN
@Composable
fun FoldersScreen(
    viewModel: FoldersViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onFolderClick: (String) -> Unit
) {
    val folderNodes by viewModel.folderNodes.collectAsState()
    val isDbLoaded by viewModel.isDbLoaded.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()

    val bottomPadding = if (playbackState.currentSong != null) 200.dp else 140.dp

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryTopBar(title = "Folders", onBackClick = onBackClick)

        if (!isDbLoaded && folderNodes.isEmpty()) {
            com.example.looper_android.ui.components.PremiumLoadingView(message = "LOADING FOLDERS")
        } else if (folderNodes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No folders found", color = Color.Gray, fontFamily = DmSansFontFamily)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(folderNodes, key = { it.path }) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .clickable { onFolderClick(folder.name) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Folder, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DmSansFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${folder.songs.size} ${if (folder.songs.size == 1) "song" else "songs"}",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                fontFamily = DmSansFontFamily
                            )
                        }
                        Icon(Lucide.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// 5. GENRES SCREEN
@Composable
fun GenresScreen(
    viewModel: GenresViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit = {},
    onBackClick: () -> Unit,
    onGenreClick: (String) -> Unit
) {
    val genres by viewModel.genres.collectAsState()
    val isDbLoaded by viewModel.isDbLoaded.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    var showSortSheet by remember { mutableStateOf(false) }

    val bottomPadding = if (playbackState.currentSong != null) 200.dp else 140.dp

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryTopBar(
            title = "Genres",
            onBackClick = onBackClick,
            actions = {
                PremiumSection(
                    width = 48.dp,
                    height = 48.dp,
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    onClick = onNavigateToSearch
                ) {
                    Icon(Lucide.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                PremiumSection(
                    width = 48.dp,
                    height = 48.dp,
                    backgroundColor = Color.White.copy(alpha = 0.08f),
                    shape = CircleShape,
                    onClick = { showSortSheet = true }
                ) {
                    Icon(Lucide.SlidersHorizontal, contentDescription = "Sort", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        )

        if (!isDbLoaded && genres.isEmpty()) {
            com.example.looper_android.ui.components.PremiumLoadingView(message = "LOADING GENRES")
        } else if (genres.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No genres found", color = Color.Gray, fontFamily = DmSansFontFamily)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(genres, key = { it.name }) { genre ->
                    val hash = abs(genre.name.hashCode())
                    val hue = (hash % 360).toFloat()
                    val startColor = Color.hsl(hue, 0.70f, 0.35f)
                    val endColor = Color.hsl((hue + 45f) % 360f, 0.80f, 0.18f)
                    val firstSongWithArt = genre.songs.firstOrNull { it.artPath != null } ?: genre.songs.firstOrNull()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(startColor, endColor)))
                            .clickable { onGenreClick(genre.name) }
                            .padding(14.dp)
                    ) {
                        firstSongWithArt?.artPath?.let { art ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 10.dp, y = 10.dp)
                                    .rotate(15f)
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                OptimizedImage(
                                    url = art,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.align(Alignment.TopStart),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = genre.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DmSansFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
    }

    if (showSortSheet) {
        val sortOptions = listOf(
            CategorySortOptionItem(GenreSortOption.NAME, "Name", Lucide.Type),
            CategorySortOptionItem(GenreSortOption.SONG_COUNT, "Number of Songs", Lucide.ListMusic)
        )
        CategorySortBottomSheet(
            options = sortOptions,
            selectedOption = sortOption,
            isAscending = isAscending,
            onOptionSelected = { viewModel.setSortOption(it) },
            onToggleAscending = { viewModel.toggleAscending() },
            onDismiss = { showSortSheet = false },
            accentColor = playbackState.accentColor
        )
    }
}

// 6. HISTORY SCREEN
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    allSongsViewModel: AllSongsViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    playlistsViewModel: PlaylistsViewModel = hiltViewModel(),
    songEditViewModel: com.example.looper_android.ui.player.SongEditViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {}
) {
    val historySongs by viewModel.historySongs.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()

    var selectedSongForOptions by remember { mutableStateOf<SongEntity?>(null) }
    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val selectionBottomBar = com.example.looper_android.ui.components.LocalSelectionBottomBar.current
    val isAllSelected = selectedSongIds.isNotEmpty() && selectedSongIds.size == historySongs.size

    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    val pendingDelete by allSongsViewModel.pendingDeletePermission.collectAsState()
    val pendingBatchDelete by allSongsViewModel.pendingBatchDeletePermission.collectAsState()

    val singleDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onDeletePermissionGranted()
        } else {
            allSongsViewModel.onDeletePermissionDenied()
        }
    }

    val batchDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onBatchDeletePermissionGranted()
        } else {
            allSongsViewModel.onBatchDeletePermissionDenied()
        }
    }

    LaunchedEffect(pendingDelete) {
        pendingDelete?.let {
            singleDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    LaunchedEffect(pendingBatchDelete) {
        pendingBatchDelete?.let {
            batchDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedSongIds = emptySet()
    }

    LaunchedEffect(isSelectionMode) {
        playbackViewModel.setSelectionActive(isSelectionMode)
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackViewModel.setSelectionActive(false)
            selectionBottomBar.value = null
        }
    }

    LaunchedEffect(isSelectionMode, selectedSongIds, historySongs, isAllSelected, playbackState.accentColor) {
        if (isSelectionMode) {
            selectionBottomBar.value = {
                MultiSelectBottomBar(
                    isVisible = true,
                    accentColor = playbackState.accentColor,

                    onPlay = {
                        val selectedSongs = historySongs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.setPlaylist(selectedSongs, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Songs")
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onShuffle = {
                        val selectedSongs = historySongs.filter { selectedSongIds.contains(it.id) }.shuffled()
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.setPlaylist(selectedSongs, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Songs")
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onPlayNext = {
                        val selectedSongs = historySongs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.addNext(selectedSongs)
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onAddToQueue = {
                        val selectedSongs = historySongs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.addToQueue(selectedSongs)
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onAddToPlaylist = {
                        showAddToPlaylistSheet = true
                    },
                    showFavorite = true,
                    onToggleFavorite = {
                        val selectedSongs = historySongs.filter { selectedSongIds.contains(it.id) }
                        val anyUnfav = selectedSongs.any { !it.isFavorite }
                        playbackViewModel.toggleFavorites(selectedSongs, targetState = anyUnfav)
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    showDelete = true,
                    onDelete = {
                        showDeleteConfirmationDialog = true
                    }
                )
            }
        } else {
            selectionBottomBar.value = null
        }
    }

    val bottomPadding = if (playbackState.currentSong != null) 200.dp else 140.dp

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionTopBar(
                selectedCount = selectedSongIds.size,
                totalCount = historySongs.size,
                onClearSelection = {
                    selectedSongIds = emptySet()
                    isSelectionMode = false
                },
                onToggleSelectAll = {
                    if (selectedSongIds.size == historySongs.size) {
                        selectedSongIds = emptySet()
                        isSelectionMode = false
                    } else {
                        selectedSongIds = historySongs.map { it.id }.toSet()
                    }
                },
                accentColor = playbackState.accentColor,
                hasStatusBarsPadding = true
            )
        } else {
            CategoryTopBar(
                title = "History",
                onBackClick = onBackClick,
                actions = {
                    if (historySongs.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear", color = com.example.looper_android.ui.theme.LocalAccentColor.current, fontFamily = DmSansFontFamily)
                        }
                    }
                }
            )
        }

        if (historySongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No listening history", color = Color.Gray, fontFamily = DmSansFontFamily)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(historySongs, key = { _, song -> song.id }) { index, song ->
                    val isCurrent = playbackState.currentSong?.path == song.path
                    val isSelected = selectedSongIds.contains(song.id)
                    SongTile(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && playbackState.isPlaying,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        accentColor = playbackState.accentColor,
                        onClick = {
                            if (isSelectionMode) {
                                val next = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                                selectedSongIds = next
                                if (next.isEmpty()) isSelectionMode = false
                            } else {
                                playbackViewModel.setPlaylist(historySongs, index, queueName = "Recently Played")
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedSongIds = setOf(song.id)
                            }
                        },
                        onOptionsClick = { selectedSongForOptions = song }
                    )
                }
            }
        }
    }

    if (showAddToPlaylistSheet) {
        AddToPlaylistBottomSheet(
            onDismiss = { showAddToPlaylistSheet = false },
            onPlaylistSelected = { playlist ->
                val selectedSongs = historySongs.filter { selectedSongIds.contains(it.id) }
                playlistsViewModel.addSongsToPlaylist(playlist, selectedSongs.map { it.path })
                isSelectionMode = false
                selectedSongIds = emptySet()
            },
            accentColor = playbackState.accentColor
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Delete ${selectedSongIds.size} songs?", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily) },
            text = { Text("This will permanently delete the selected songs from your device.", fontFamily = DmSansFontFamily) },
            confirmButton = {
                Button(
                    onClick = {
                        val songsToDelete = historySongs.filter { selectedSongIds.contains(it.id) }
                        allSongsViewModel.deleteSongs(songsToDelete)
                        showDeleteConfirmationDialog = false
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White, fontFamily = DmSansFontFamily)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }

    selectedSongForOptions?.let { song ->
        SongOptionsSheet(
            song = song,
            accentColor = playbackState.accentColor,
            onDismiss = { selectedSongForOptions = null },
            onArtistClick = onNavigateToArtist,
            onAlbumClick = onNavigateToAlbum
        )
    }
}

// 7. FAVORITES SCREEN
@Composable
fun FavoritesScreen(
    onBackClick: () -> Unit,
    allSongsViewModel: AllSongsViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    playlistsViewModel: PlaylistsViewModel = hiltViewModel(),
    songEditViewModel: com.example.looper_android.ui.player.SongEditViewModel = hiltViewModel(),
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {}
) {
    val songs by allSongsViewModel.songs.collectAsState()
    val favoriteSongs = remember(songs) { songs.filter { it.isFavorite } }
    val playbackState by playbackViewModel.playbackState.collectAsState()

    var selectedSongForOptions by remember { mutableStateOf<SongEntity?>(null) }
    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val selectionBottomBar = com.example.looper_android.ui.components.LocalSelectionBottomBar.current
    val isAllSelected = selectedSongIds.isNotEmpty() && selectedSongIds.size == favoriteSongs.size

    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    val pendingDelete by allSongsViewModel.pendingDeletePermission.collectAsState()
    val pendingBatchDelete by allSongsViewModel.pendingBatchDeletePermission.collectAsState()

    val singleDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onDeletePermissionGranted()
        } else {
            allSongsViewModel.onDeletePermissionDenied()
        }
    }

    val batchDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onBatchDeletePermissionGranted()
        } else {
            allSongsViewModel.onBatchDeletePermissionDenied()
        }
    }

    LaunchedEffect(pendingDelete) {
        pendingDelete?.let {
            singleDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    LaunchedEffect(pendingBatchDelete) {
        pendingBatchDelete?.let {
            batchDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedSongIds = emptySet()
    }

    LaunchedEffect(isSelectionMode) {
        playbackViewModel.setSelectionActive(isSelectionMode)
    }

    DisposableEffect(Unit) {
        onDispose {
            playbackViewModel.setSelectionActive(false)
            selectionBottomBar.value = null
        }
    }

    LaunchedEffect(isSelectionMode, selectedSongIds, favoriteSongs, isAllSelected, playbackState.accentColor) {
        if (isSelectionMode) {
            selectionBottomBar.value = {
                MultiSelectBottomBar(
                    isVisible = true,
                    accentColor = playbackState.accentColor,

                    onPlay = {
                        val selectedSongs = favoriteSongs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.setPlaylist(selectedSongs, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Songs")
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onShuffle = {
                        val selectedSongs = favoriteSongs.filter { selectedSongIds.contains(it.id) }.shuffled()
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.setPlaylist(selectedSongs, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Songs")
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onPlayNext = {
                        val selectedSongs = favoriteSongs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.addNext(selectedSongs)
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onAddToQueue = {
                        val selectedSongs = favoriteSongs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.addToQueue(selectedSongs)
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onAddToPlaylist = {
                        showAddToPlaylistSheet = true
                    },
                    showFavorite = true,
                    onToggleFavorite = {
                        val selectedSongs = favoriteSongs.filter { selectedSongIds.contains(it.id) }
                        val anyUnfav = selectedSongs.any { !it.isFavorite }
                        playbackViewModel.toggleFavorites(selectedSongs, targetState = anyUnfav)
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    showDelete = true,
                    onDelete = {
                        showDeleteConfirmationDialog = true
                    }
                )
            }
        } else {
            selectionBottomBar.value = null
        }
    }

    val bottomPadding = if (playbackState.currentSong != null) 200.dp else 140.dp

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionTopBar(
                selectedCount = selectedSongIds.size,
                totalCount = favoriteSongs.size,
                onClearSelection = {
                    selectedSongIds = emptySet()
                    isSelectionMode = false
                },
                onToggleSelectAll = {
                    if (selectedSongIds.size == favoriteSongs.size) {
                        selectedSongIds = emptySet()
                        isSelectionMode = false
                    } else {
                        selectedSongIds = favoriteSongs.map { it.id }.toSet()
                    }
                },
                accentColor = playbackState.accentColor,
                hasStatusBarsPadding = true
            )
        } else {
            CategoryTopBar(title = "Favorites", onBackClick = onBackClick)
        }

        if (favoriteSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No favorite songs yet", color = Color.Gray, fontFamily = DmSansFontFamily)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(favoriteSongs, key = { _, song -> song.id }) { index, song ->
                    val isCurrent = playbackState.currentSong?.path == song.path
                    val isSelected = selectedSongIds.contains(song.id)
                    SongTile(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && playbackState.isPlaying,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        accentColor = playbackState.accentColor,
                        onClick = {
                            if (isSelectionMode) {
                                val next = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                                selectedSongIds = next
                                if (next.isEmpty()) isSelectionMode = false
                            } else {
                                playbackViewModel.setPlaylist(favoriteSongs, index, queueName = "Favorites")
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedSongIds = setOf(song.id)
                            }
                        },
                        onOptionsClick = { selectedSongForOptions = song }
                    )
                }
            }
        }
    }

    if (showAddToPlaylistSheet) {
        AddToPlaylistBottomSheet(
            onDismiss = { showAddToPlaylistSheet = false },
            onPlaylistSelected = { playlist ->
                val selectedSongs = favoriteSongs.filter { selectedSongIds.contains(it.id) }
                playlistsViewModel.addSongsToPlaylist(playlist, selectedSongs.map { it.path })
                isSelectionMode = false
                selectedSongIds = emptySet()
            },
            accentColor = playbackState.accentColor
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Delete ${selectedSongIds.size} songs?", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily) },
            text = { Text("This will permanently delete the selected songs from your device.", fontFamily = DmSansFontFamily) },
            confirmButton = {
                Button(
                    onClick = {
                        val songsToDelete = favoriteSongs.filter { selectedSongIds.contains(it.id) }
                        allSongsViewModel.deleteSongs(songsToDelete)
                        showDeleteConfirmationDialog = false
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White, fontFamily = DmSansFontFamily)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }

    selectedSongForOptions?.let { song ->
        SongOptionsSheet(
            song = song,
            accentColor = playbackState.accentColor,
            onDismiss = { selectedSongForOptions = null },
            onArtistClick = onNavigateToArtist,
            onAlbumClick = onNavigateToAlbum
        )
    }
}

// --- ALBUM & ARTIST OPTIONS BOTTOM SHEETS ---

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AlbumOptionsBottomSheet(
    album: AlbumEntity,
    songs: List<SongEntity>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val anyUnfavorite = remember(songs) { songs.any { !it.isFavorite } }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.24f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    OptimizedImage(
                        url = album.artPath,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${album.artist ?: "Unknown Artist"} • ${songs.size} ${if (songs.size == 1) "song" else "songs"}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontFamily = DmSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            CategoryOptionActionRow(icon = Lucide.Play, title = "Play", onClick = onPlay, accentColor = accentColor)
            CategoryOptionActionRow(icon = Lucide.ListPlus, title = "Play Next", onClick = onPlayNext, accentColor = accentColor)
            CategoryOptionActionRow(icon = Lucide.ListMusic, title = "Add to Queue", onClick = onAddToQueue, accentColor = accentColor)
            CategoryOptionActionRow(icon = Lucide.Plus, title = "Add to Playlist", onClick = onAddToPlaylist, accentColor = accentColor)
            CategoryOptionActionRow(
                icon = Lucide.Heart,
                title = if (anyUnfavorite) "Favorite All Songs" else "Unfavorite All Songs",
                onClick = onToggleFavorite,
                accentColor = accentColor
            )
            CategoryOptionActionRow(
                icon = Lucide.Trash2,
                title = "Delete from Device",
                onClick = onDelete,
                accentColor = Color(0xFFFF5252),
                textColor = Color(0xFFFF5252)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ArtistOptionsBottomSheet(
    artist: ArtistEntity,
    songs: List<SongEntity>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val anyUnfavorite = remember(songs) { songs.any { !it.isFavorite } }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.24f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    ArtistCollage(
                        artistImageUrl = artist.artistImageUrl ?: artist.artPath,
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${songs.size} ${if (songs.size == 1) "song" else "songs"}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontFamily = DmSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            CategoryOptionActionRow(icon = Lucide.Play, title = "Play", onClick = onPlay, accentColor = accentColor)
            CategoryOptionActionRow(icon = Lucide.ListPlus, title = "Play Next", onClick = onPlayNext, accentColor = accentColor)
            CategoryOptionActionRow(icon = Lucide.ListMusic, title = "Add to Queue", onClick = onAddToQueue, accentColor = accentColor)
            CategoryOptionActionRow(icon = Lucide.Plus, title = "Add to Playlist", onClick = onAddToPlaylist, accentColor = accentColor)
            CategoryOptionActionRow(
                icon = Lucide.Heart,
                title = if (anyUnfavorite) "Favorite All Songs" else "Unfavorite All Songs",
                onClick = onToggleFavorite,
                accentColor = accentColor
            )
            CategoryOptionActionRow(
                icon = Lucide.Trash2,
                title = "Delete from Device",
                onClick = onDelete,
                accentColor = Color(0xFFFF5252),
                textColor = Color(0xFFFF5252)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategoryOptionActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    accentColor: Color,
    textColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (textColor == Color(0xFFFF5252)) textColor else accentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DmSansFontFamily
        )
    }
}

// ==============================================================================

// ==============================================================================
// DISCOVERY CATEGORY SCREENS (Recently Added, Not Played, In Library)
// ==============================================================================

@Composable
fun RecentlyAddedScreen(
    onBackClick: () -> Unit,
    allSongsViewModel: AllSongsViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    playlistsViewModel: PlaylistsViewModel = hiltViewModel(),
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {}
) {
    val allSongs by allSongsViewModel.libraryCacheManager.songs.collectAsState()
    val isDbLoaded by allSongsViewModel.libraryCacheManager.isDbLoaded.collectAsState()
    val recentlyAdded = remember(allSongs) {
        allSongs.sortedByDescending { it.dateAdded }
    }
    GenericSongCategoryScreen(
        title = "Recently Added",
        songs = recentlyAdded,
        isLoading = !isDbLoaded,
        onBackClick = onBackClick,
        allSongsViewModel = allSongsViewModel,
        playbackViewModel = playbackViewModel,
        playlistsViewModel = playlistsViewModel,
        onNavigateToArtist = onNavigateToArtist,
        onNavigateToAlbum = onNavigateToAlbum
    )
}

@Composable
fun NotPlayedScreen(
    onBackClick: () -> Unit,
    allSongsViewModel: AllSongsViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    playlistsViewModel: PlaylistsViewModel = hiltViewModel(),
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {}
) {
    val allSongs by allSongsViewModel.libraryCacheManager.songs.collectAsState()
    val isDbLoaded by allSongsViewModel.libraryCacheManager.isDbLoaded.collectAsState()
    val notPlayed = remember(allSongs) {
        allSongs.filter { (it.playCount ?: 0) == 0 }
    }
    GenericSongCategoryScreen(
        title = "Not Played",
        songs = notPlayed,
        isLoading = !isDbLoaded,
        onBackClick = onBackClick,
        allSongsViewModel = allSongsViewModel,
        playbackViewModel = playbackViewModel,
        playlistsViewModel = playlistsViewModel,
        onNavigateToArtist = onNavigateToArtist,
        onNavigateToAlbum = onNavigateToAlbum
    )
}



@Composable
private fun GenericSongCategoryScreen(
    title: String,
    songs: List<SongEntity>,
    isLoading: Boolean = false,
    onBackClick: () -> Unit,
    allSongsViewModel: AllSongsViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistsViewModel: PlaylistsViewModel,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit
) {
    val playbackState by playbackViewModel.playbackState.collectAsState()

    var selectedSongForOptions by remember { mutableStateOf<SongEntity?>(null) }
    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val selectionBottomBar = com.example.looper_android.ui.components.LocalSelectionBottomBar.current
    val isAllSelected = selectedSongIds.isNotEmpty() && selectedSongIds.size == songs.size

    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    val pendingDelete by allSongsViewModel.pendingDeletePermission.collectAsState()
    val pendingBatchDelete by allSongsViewModel.pendingBatchDeletePermission.collectAsState()

    val singleDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onDeletePermissionGranted()
        } else {
            allSongsViewModel.onDeletePermissionDenied()
        }
    }

    val batchDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onBatchDeletePermissionGranted()
        } else {
            allSongsViewModel.onBatchDeletePermissionDenied()
        }
    }

    LaunchedEffect(pendingDelete) {
        pendingDelete?.let {
            singleDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    LaunchedEffect(pendingBatchDelete) {
        pendingBatchDelete?.let {
            batchDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    LaunchedEffect(isSelectionMode, selectedSongIds, songs, isAllSelected, playbackState.accentColor) {
        if (isSelectionMode && selectedSongIds.isNotEmpty()) {
            selectionBottomBar.value = {
                MultiSelectBottomBar(
                    isVisible = true,
                    accentColor = playbackState.accentColor,
                    onPlay = {
                        val selectedSongs = songs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.setPlaylist(selectedSongs, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Songs")
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onShuffle = {
                        val selectedSongs = songs.filter { selectedSongIds.contains(it.id) }.shuffled()
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.setPlaylist(selectedSongs, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Songs")
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onPlayNext = {
                        val selectedSongs = songs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.addNext(selectedSongs)
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onAddToQueue = {
                        val selectedSongs = songs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.addToQueue(selectedSongs)
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onAddToPlaylist = {
                        showAddToPlaylistSheet = true
                    },
                    showFavorite = true,
                    onToggleFavorite = {
                        val selectedSongs = songs.filter { selectedSongIds.contains(it.id) }
                        val anyUnfav = selectedSongs.any { !it.isFavorite }
                        playbackViewModel.toggleFavorites(selectedSongs, targetState = anyUnfav)
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    showDelete = true,
                    onDelete = {
                        showDeleteConfirmationDialog = true
                    }
                )
            }
        } else {
            selectionBottomBar.value = null
        }
    }

    val bottomPadding = if (playbackState.currentSong != null) 200.dp else 140.dp

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionTopBar(
                selectedCount = selectedSongIds.size,
                totalCount = songs.size,
                onClearSelection = {
                    selectedSongIds = emptySet()
                    isSelectionMode = false
                },
                onToggleSelectAll = {
                    if (selectedSongIds.size == songs.size) {
                        selectedSongIds = emptySet()
                        isSelectionMode = false
                    } else {
                        selectedSongIds = songs.map { it.id }.toSet()
                    }
                },
                accentColor = playbackState.accentColor,
                hasStatusBarsPadding = true
            )
        } else {
            CategoryTopBar(
                title = title,
                onBackClick = onBackClick,
                actions = {
                    if (songs.isNotEmpty()) {
                        PremiumSection(
                            width = 44.dp,
                            height = 44.dp,
                            backgroundColor = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                playbackViewModel.setPlaylist(
                                    songs.shuffled(),
                                    initialIndex = 0,
                                    disableShuffleMode = false,
                                    queueName = "$title (Shuffle)"
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Lucide.Shuffle,
                                contentDescription = "Shuffle",
                                tint = playbackState.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        }

        if (isLoading && songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PremiumLoadingView(message = "LOADING SONGS")
            }
        } else if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs found", color = Color.Gray, fontFamily = DmSansFontFamily)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "header_play_shuffle_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(playbackState.accentColor)
                                .clickable {
                                    playbackViewModel.setPlaylist(songs, initialIndex = 0, disableShuffleMode = true, queueName = title)
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.Play, contentDescription = null, tint = Color.Black, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily, fontSize = 14.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    playbackViewModel.setPlaylist(songs.shuffled(), initialIndex = 0, disableShuffleMode = false, queueName = "$title (Shuffle)")
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Lucide.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily, fontSize = 14.sp)
                            }
                        }
                    }
                }

                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    val isCurrent = playbackState.currentSong?.path == song.path
                    val isSelected = selectedSongIds.contains(song.id)
                    SongTile(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && playbackState.isPlaying,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        accentColor = playbackState.accentColor,
                        onClick = {
                            if (isSelectionMode) {
                                val next = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                                selectedSongIds = next
                                if (next.isEmpty()) isSelectionMode = false
                            } else {
                                playbackViewModel.setPlaylist(songs, index, queueName = title)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedSongIds = setOf(song.id)
                            }
                        },
                        onOptionsClick = { selectedSongForOptions = song }
                    )
                }
            }
        }
    }

    if (showAddToPlaylistSheet) {
        AddToPlaylistBottomSheet(
            onDismiss = { showAddToPlaylistSheet = false },
            onPlaylistSelected = { playlist ->
                val selectedSongs = songs.filter { selectedSongIds.contains(it.id) }
                playlistsViewModel.addSongsToPlaylist(playlist, selectedSongs.map { it.path })
                isSelectionMode = false
                selectedSongIds = emptySet()
            },
            accentColor = playbackState.accentColor
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Delete ${selectedSongIds.size} songs?", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily) },
            text = { Text("This will permanently delete the selected songs from your device.", fontFamily = DmSansFontFamily) },
            confirmButton = {
                Button(
                    onClick = {
                        val songsToDelete = songs.filter { selectedSongIds.contains(it.id) }
                        allSongsViewModel.deleteSongs(songsToDelete)
                        showDeleteConfirmationDialog = false
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White, fontFamily = DmSansFontFamily)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }

    selectedSongForOptions?.let { song ->
        SongOptionsSheet(
            song = song,
            accentColor = playbackState.accentColor,
            onDismiss = { selectedSongForOptions = null },
            onArtistClick = onNavigateToArtist,
            onAlbumClick = onNavigateToAlbum
        )
    }
}
