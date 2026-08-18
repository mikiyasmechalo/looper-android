package com.example.looper_android.ui.library

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.composables.icons.lucide.ListOrdered
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.Type
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.AddToPlaylistBottomSheet
import com.example.looper_android.ui.components.ArtistCollage
import com.example.looper_android.ui.components.LocalSelectionBottomBar
import com.example.looper_android.ui.components.MultiSelectBottomBar
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.components.PremiumLoadingView
import com.example.looper_android.ui.components.SelectionTopBar
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.player.SongOptionsSheet
import com.example.looper_android.ui.player.TechnicalInfoBottomSheet
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.theme.WhiteAlpha15
import com.example.looper_android.ui.widgets.FastScroller
import com.example.looper_android.ui.widgets.PremiumSection

enum class SongSortOption { TITLE, ALBUM, TRACK_NUMBER, DURATION, DATE_ADDED, YEAR }

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
    viewModel: CollectionDetailViewModel = hiltViewModel()
) {
    val isDbLoaded by viewModel.isDbLoaded.collectAsState()
    val collectionSongs by viewModel.collectionSongs.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()
    
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val currentSong = playbackState.currentSong

    val currentTitle by viewModel.currentTitle.collectAsState()
    val effectiveTitle = currentTitle.ifBlank { title }

    val playlistsViewModel: PlaylistsViewModel = hiltViewModel()

    val progressState by viewModel.progressState.collectAsState()
    val pendingBatchDelete by viewModel.pendingBatchDeletePermission.collectAsState()
    var showSortSheet by remember { mutableStateOf(false) }
    var showCollectionOptionsSheet by remember { mutableStateOf(false) }
    var showEditAlbumSheet by remember { mutableStateOf(false) }
    var showDeleteAlbumDialog by remember { mutableStateOf(false) }
    var showDeleteArtistDialog by remember { mutableStateOf(false) }
    var showSafRequiredDialog by remember { mutableStateOf(false) }
    var pendingSafAction by remember { mutableStateOf<String?>(null) }
    var selectedSongForOptions by remember { mutableStateOf<SongEntity?>(null) }

    // Multi-Selection State for Songs inside Collection
    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val selectionBottomBar = LocalSelectionBottomBar.current
    val isAllSelected = selectedSongIds.isNotEmpty() && selectedSongIds.size == collectionSongs.size

    var showAddToPlaylistForSelection by remember { mutableStateOf(false) }
    var showDeleteSelectedSongsDialog by remember { mutableStateOf(false) }

    val pendingDeleteFromAllSongs by allSongsViewModel.pendingDeletePermission.collectAsState()
    val pendingBatchDeleteFromAllSongs by allSongsViewModel.pendingBatchDeletePermission.collectAsState()

    val singleDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onDeletePermissionGranted()
        } else {
            allSongsViewModel.onDeletePermissionDenied()
        }
    }

    val allSongsBatchDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            allSongsViewModel.onBatchDeletePermissionGranted()
        } else {
            allSongsViewModel.onBatchDeletePermissionDenied()
        }
    }

    LaunchedEffect(pendingDeleteFromAllSongs) {
        pendingDeleteFromAllSongs?.let {
            singleDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    LaunchedEffect(pendingBatchDeleteFromAllSongs) {
        pendingBatchDeleteFromAllSongs?.let {
            allSongsBatchDeleteLauncher.launch(
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

    LaunchedEffect(isSelectionMode, selectedSongIds, collectionSongs, isAllSelected, playbackState.accentColor) {
        if (isSelectionMode) {
            selectionBottomBar.value = {
                MultiSelectBottomBar(
                    isVisible = true,
                    accentColor = playbackState.accentColor,
                    onPlay = {
                        val selectedSongs = collectionSongs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.setPlaylist(selectedSongs, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Songs")
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onShuffle = {
                        val selectedSongs = collectionSongs.filter { selectedSongIds.contains(it.id) }.shuffled()
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.setPlaylist(selectedSongs, initialIndex = 0, disableShuffleMode = true, queueName = "Selected Songs")
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onPlayNext = {
                        val selectedSongs = collectionSongs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.addNext(selectedSongs)
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onAddToQueue = {
                        val selectedSongs = collectionSongs.filter { selectedSongIds.contains(it.id) }
                        if (selectedSongs.isNotEmpty()) {
                            playbackViewModel.addToQueue(selectedSongs)
                        }
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    onAddToPlaylist = {
                        showAddToPlaylistForSelection = true
                    },
                    showFavorite = !isAllSelected,
                    onToggleFavorite = {
                        val selectedSongs = collectionSongs.filter { selectedSongIds.contains(it.id) }
                        val anyUnfav = selectedSongs.any { !it.isFavorite }
                        playbackViewModel.toggleFavorites(selectedSongs, targetState = anyUnfav)
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    showDelete = !isAllSelected,
                    onDelete = {
                        showDeleteSelectedSongsDialog = true
                    }
                )
            }
        } else {
            selectionBottomBar.value = null
        }
    }

    val batchDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onBatchDeletePermissionGranted(
                collectionName = effectiveTitle,
                isAlbum = type.equals("album", ignoreCase = true)
            )
            onBackClick()
        } else {
            viewModel.onBatchDeletePermissionDenied()
        }
    }

    LaunchedEffect(pendingBatchDelete) {
        pendingBatchDelete?.let {
            batchDeleteLauncher.launch(
                androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()
            )
        }
    }

    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            viewModel.saveSafPermission(treeUri)
            if (viewModel.isCollectionSafCovered(collectionSongs)) {
                when (pendingSafAction) {
                    "edit" -> showEditAlbumSheet = true
                    "delete" -> showDeleteAlbumDialog = true
                    "delete_artist" -> showDeleteArtistDialog = true
                }
            }
        }
        pendingSafAction = null
    }

    LaunchedEffect(title, type) {
        viewModel.initCollection(title, type)
    }

    val artists by viewModel.artists.collectAsState()
    val isAlbumType = type.equals("album", ignoreCase = true)
    val isArtistType = type.equals("artist", ignoreCase = true)

    val artistEntity = if (isArtistType) {
        artists.find { it.name.equals(effectiveTitle, ignoreCase = true) }
    } else null

    if (isArtistType && (artistEntity == null || artistEntity.artistImageUrl.isNullOrEmpty())) {
        LaunchedEffect(effectiveTitle) {
            viewModel.fetchArtistImage(effectiveTitle)
        }
    }

    val displayArt = if (isArtistType) {
        artistEntity?.artistImageUrl ?: artistEntity?.artPath ?: artPath
    } else {
        artPath ?: collectionSongs.firstOrNull { !it.artPath.isNullOrEmpty() }?.artPath
    }
    val bottomPadding = if (currentSong != null) 200.dp else 140.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedSongIds.size,
                    totalCount = collectionSongs.size,
                    onClearSelection = {
                        selectedSongIds = emptySet()
                        isSelectionMode = false
                    },
                    onToggleSelectAll = {
                        if (selectedSongIds.size == collectionSongs.size) {
                            selectedSongIds = emptySet()
                            isSelectionMode = false
                        } else {
                            selectedSongIds = collectionSongs.map { it.id }.toSet()
                        }
                    },
                    accentColor = playbackState.accentColor,
                    hasStatusBarsPadding = true
                )
            } else {
                // Top Back Button Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                }
            }

            if (!isDbLoaded && collectionSongs.isEmpty()) {
                PremiumLoadingView(message = "LOADING COLLECTION")
            } else {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 4.dp, bottom = bottomPadding),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Hero Header (Cover image centered, text & buttons left-aligned)
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                // Centered artwork image container
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (type.equals("artist", ignoreCase = true)) {
                                        ArtistCollage(
                                            artistImageUrl = displayArt,
                                            modifier = Modifier.size(140.dp),
                                            shape = CircleShape
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                        ) {
                                            OptimizedImage(
                                                url = displayArt,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = effectiveTitle,
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = DmSansFontFamily,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${collectionSongs.size} ${if (collectionSongs.size == 1) "track" else "tracks"}",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 16.sp,
                                    fontFamily = DmSansFontFamily
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Action Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play All Button
                                    PremiumSection(
                                        height = 44.dp,
                                        backgroundColor = Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(12.dp),
                                        onClick = {
                                            if (collectionSongs.isNotEmpty()) {
                                                playbackViewModel.setPlaylist(collectionSongs, initialIndex = 0, queueName = effectiveTitle)
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
                                            playbackViewModel.shuffleAll(collectionSongs, queueName = effectiveTitle)
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
                                        playbackViewModel.setPlaylist(collectionSongs, initialIndex = index, disableShuffleMode = true, queueName = effectiveTitle)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedSongIds = setOf(song.id)
                                    }
                                },
                                onOptionsClick = { selectedSongForOptions = song },
                                showAlbumInSubtitle = (type != "album")
                            )
                        }
                    }

                    FastScroller(
                        listState = listState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(bottom = bottomPadding)
                    )
                }
            }
        }

        // Mass Operation Progress Overlay
        if (progressState.isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.70f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(32.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 32.dp, vertical = 28.dp)
                ) {
                    CircularProgressIndicator(
                        color = playbackState.accentColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = progressState.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily
                    )
                }
            }
        }
    }

    // Selection Add to Playlist Bottom Sheet
    if (showAddToPlaylistForSelection) {
        AddToPlaylistBottomSheet(
            onDismiss = { showAddToPlaylistForSelection = false },
            onPlaylistSelected = { playlist ->
                val selectedSongs = collectionSongs.filter { selectedSongIds.contains(it.id) }
                playlistsViewModel.addSongsToPlaylist(playlist, selectedSongs.map { it.path })
                isSelectionMode = false
                selectedSongIds = emptySet()
            },
            accentColor = playbackState.accentColor
        )
    }

    // Selection Delete Confirmation Dialog
    if (showDeleteSelectedSongsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedSongsDialog = false },
            title = { Text("Delete ${selectedSongIds.size} songs?", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily) },
            text = { Text("This will permanently delete the selected songs from your device.", fontFamily = DmSansFontFamily) },
            confirmButton = {
                Button(
                    onClick = {
                        val songsToDelete = collectionSongs.filter { selectedSongIds.contains(it.id) }
                        allSongsViewModel.deleteSongs(songsToDelete)
                        showDeleteSelectedSongsDialog = false
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedSongsDialog = false }) {
                    Text("Cancel", color = Color.White, fontFamily = DmSansFontFamily)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        val songSortOptions = mutableListOf(
            CategorySortOptionItem(SongSortOption.TITLE, "Title", Lucide.Type),
            CategorySortOptionItem(SongSortOption.TRACK_NUMBER, "Track Number", Lucide.ListOrdered)
        )
        if (!type.equals("album", ignoreCase = true)) {
            songSortOptions.add(CategorySortOptionItem(SongSortOption.ALBUM, "Album", Lucide.Disc))
        }
        songSortOptions.addAll(
            listOf(
                CategorySortOptionItem(SongSortOption.DURATION, "Duration", Lucide.Clock),
                CategorySortOptionItem(SongSortOption.DATE_ADDED, "Date Added", Lucide.CalendarPlus),
                CategorySortOptionItem(SongSortOption.YEAR, "Year", Lucide.Calendar)
            )
        )

        CategorySortBottomSheet(
            options = songSortOptions,
            selectedOption = sortOption,
            isAscending = isAscending,
            onOptionSelected = { viewModel.setSortOption(it) },
            onToggleAscending = { viewModel.toggleAscending() },
            onDismiss = { showSortSheet = false },
            accentColor = playbackState.accentColor
        )
    }

    // Collection 3-Dot Overflow Options Sheet
    if (showCollectionOptionsSheet) {
        CollectionOptionsMenuSheet(
            collectionTitle = effectiveTitle,
            songs = collectionSongs,
            isAlbum = isAlbumType,
            isArtist = isArtistType,
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
            onEditAlbumTagsClick = {
                showEditAlbumSheet = true
            },
            onDeleteAlbumClick = {
                showDeleteAlbumDialog = true
            },
            onDeleteArtistClick = {
                showDeleteArtistDialog = true
            },
            accentColor = playbackState.accentColor
        )
    }

    // Mass Edit Album Tags Bottom Sheet
    if (showEditAlbumSheet) {
        EditAlbumBottomSheet(
            albumName = effectiveTitle,
            songs = collectionSongs,
            isSaving = progressState.isRunning,
            accentColor = playbackState.accentColor,
            onDismiss = { showEditAlbumSheet = false },
            onSave = { newAlbumName, newAlbumArtist, newYear, newGenre, pickedArtUri, removeArtwork ->
                viewModel.massEditAlbum(
                    oldAlbumName = effectiveTitle,
                    songs = collectionSongs,
                    newAlbumName = newAlbumName,
                    newAlbumArtist = newAlbumArtist,
                    newYear = newYear,
                    newGenre = newGenre,
                    pickedArtUri = pickedArtUri,
                    removeArtwork = removeArtwork,
                    onComplete = { _, _ ->
                        showEditAlbumSheet = false
                    }
                )
            }
        )
    }

    // Delete Album Confirmation Dialog
    if (showDeleteAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAlbumDialog = false },
            title = {
                Text("Delete Album", color = Color.White, fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"$effectiveTitle\" and all ${collectionSongs.size} songs from your device?",
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = DmSansFontFamily
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAlbumDialog = false
                        viewModel.massDeleteAlbum(
                            albumName = effectiveTitle,
                            songs = collectionSongs
                        ) {
                            onBackClick()
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAlbumDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontFamily = DmSansFontFamily)
                }
            },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Artist Confirmation Dialog
    if (showDeleteArtistDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteArtistDialog = false },
            title = {
                Text("Delete Artist", color = Color.White, fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete all ${collectionSongs.size} songs by \"$effectiveTitle\" from your device?",
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = DmSansFontFamily
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteArtistDialog = false
                        viewModel.massDeleteArtist(
                            artistName = effectiveTitle,
                            songs = collectionSongs
                        ) {
                            onBackClick()
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteArtistDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontFamily = DmSansFontFamily)
                }
            },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // SAF Storage Permission Required Dialog
    if (showSafRequiredDialog) {
        AlertDialog(
            onDismissRequest = {
                showSafRequiredDialog = false
                pendingSafAction = null
            },
            title = {
                Text("Storage Permission Required", color = Color.White, fontFamily = DmSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    text = "Android requires Storage Access Framework (SAF) permission to modify tags or delete files in this folder.\n\nPlease grant permission to the folder containing these songs to proceed.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = DmSansFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSafRequiredDialog = false
                        safPickerLauncher.launch(null)
                    }
                ) {
                    Text("Grant Folder Access", color = playbackState.accentColor, fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSafRequiredDialog = false
                        pendingSafAction = null
                    }
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontFamily = DmSansFontFamily)
                }
            },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Individual Track 3-Dot Options Sheet
    selectedSongForOptions?.let { song ->
        SongOptionsSheet(
            song = song,
            accentColor = playbackState.accentColor,
            onDismiss = { selectedSongForOptions = null },
            showGoToArtist = (type != "artist"),
            showGoToAlbum = (type != "album"),
            onArtistClick = onNavigateToArtist,
            onAlbumClick = onNavigateToAlbum
        )
    }
}
