package com.example.looper_android.ui.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.ListFilter
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Shuffle
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.AddToPlaylistBottomSheet
import com.example.looper_android.ui.components.LocalSelectionBottomBar
import com.example.looper_android.ui.components.MultiSelectBottomBar
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.components.SelectionTopBar
import com.example.looper_android.ui.library.PlaylistsViewModel
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.player.SongOptionsSheet
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.FastScroller
import com.example.looper_android.ui.widgets.PremiumSection

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AllSongsScreen(
    viewModel: AllSongsViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {}
) {
    val songs by viewModel.sortedSongs.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()
    val isDbLoaded by viewModel.isDbLoaded.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val pendingDelete by viewModel.pendingDeletePermission.collectAsState()
    val pendingBatchDelete by viewModel.pendingBatchDeletePermission.collectAsState()

    val playbackState by playbackViewModel.playbackState.collectAsState()
    val currentSong = playbackState.currentSong
    val isMiniPlayerVisible = playbackState.isSongReady && currentSong != null

    val playlistsViewModel: PlaylistsViewModel = hiltViewModel()

    var showSortSheet by remember { mutableStateOf(false) }
    var selectedSongForOptions by remember { mutableStateOf<SongEntity?>(null) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    var selectedSongIds by remember { mutableStateOf(setOf<Long>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val selectionBottomBar = LocalSelectionBottomBar.current
    val isAllSelected = selectedSongIds.isNotEmpty() && selectedSongIds.size == songs.size

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

    val hazeState = com.example.looper_android.ui.widgets.LocalHazeState.current
    val isBlurEnabled = com.example.looper_android.ui.widgets.LocalIsBlurEnabled.current
    val isDarkTheme = androidx.compose.material3.MaterialTheme.colorScheme.background == Color.Black

    LaunchedEffect(isSelectionMode, selectedSongIds, songs, isAllSelected, playbackState.accentColor, isBlurEnabled, hazeState, isDarkTheme) {
        if (isSelectionMode) {
            selectionBottomBar.value = {
                MultiSelectBottomBar(
                    isVisible = true,
                    accentColor = playbackState.accentColor,
                    enableBlur = isBlurEnabled,
                    hazeState = hazeState,
                    darkTheme = isDarkTheme,
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
                    showFavorite = !isAllSelected,
                    onToggleFavorite = {
                        val selectedSongs = songs.filter { selectedSongIds.contains(it.id) }
                        val anyUnfav = selectedSongs.any { !it.isFavorite }
                        playbackViewModel.toggleFavorites(selectedSongs, targetState = anyUnfav)
                        isSelectionMode = false
                        selectedSongIds = emptySet()
                    },
                    showDelete = !isAllSelected,
                    onDelete = {
                        showDeleteConfirmationDialog = true
                    }
                )
            }
        } else {
            selectionBottomBar.value = null
        }
    }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadSongs()
        }
    }

    val singleDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onDeletePermissionGranted()
        } else {
            viewModel.onDeletePermissionDenied()
        }
    }

    val batchDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onBatchDeletePermissionGranted()
        } else {
            viewModel.onBatchDeletePermissionDenied()
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
            .background(Color.Transparent)
    ) {
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
                    accentColor = playbackState.accentColor
                )
            } else {
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

            if (isDbLoaded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${songs.size} Songs",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontFamily = DmSansFontFamily
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isSelectionMode) { showSortSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.ListFilter,
                            contentDescription = "Sort By",
                            tint = if (isSelectionMode) Color.White.copy(alpha = 0.3f) else playbackState.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }

            val isRefreshing by viewModel.isRefreshing.collectAsState()
            val pullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()

            var wasRefreshing by remember { mutableStateOf(false) }
            LaunchedEffect(isRefreshing) {
                if (isRefreshing) {
                    wasRefreshing = true
                } else if (wasRefreshing) {
                    kotlinx.coroutines.delay(150)
                    wasRefreshing = false
                }
            }

            val isActivelyPulling = pullToRefreshState.distanceFraction > 0f && !isRefreshing && !wasRefreshing
            val targetOffset = if (isActivelyPulling) pullToRefreshState.distanceFraction * 140f else 0f

            val animatedOffsetY by androidx.compose.animation.core.animateFloatAsState(
                targetValue = targetOffset,
                animationSpec = if (isActivelyPulling) {
                    androidx.compose.animation.core.snap()
                } else {
                    androidx.compose.animation.core.tween(
                        durationMillis = 280,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                },
                label = "pullOffset"
            )

            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { 
                    if (!isScanning) {
                        viewModel.rescanLibrary() 
                    }
                },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        color = com.example.looper_android.ui.theme.LocalAccentColor.current
                    )
                }
            ) {
                if (songs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isLoading || isScanning || !isDbLoaded) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = playbackState.accentColor,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Text("No songs found.", color = Color.Gray)
                        }
                    }
                } else {
                    val bottomInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val targetBottomPadding = if (isMiniPlayerVisible) (190.dp + bottomInsets) else (120.dp + bottomInsets)
                    val bottomPadding = targetBottomPadding
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = animatedOffsetY
                                },
                            contentPadding = PaddingValues(top = 2.dp, bottom = bottomPadding)
                        ) {
                            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                                val isCurrent = currentSong?.path == song.path
                                val isPlaying = isCurrent && playbackState.isPlaying
                                val isSelected = selectedSongIds.contains(song.id)
                                SongTile(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isPlaying,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    accentColor = playbackState.accentColor,
                                    onClick = {
                                        if (isSelectionMode) {
                                            val next = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                                            selectedSongIds = next
                                            if (next.isEmpty()) isSelectionMode = false
                                        } else {
                                            viewModel.playAll(startIndex = index, shuffled = false)
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
                        
                        FastScroller(
                            listState = listState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(bottom = bottomPadding)
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
                            viewModel.deleteSongs(songsToDelete)
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

        if (showSortSheet) {
            SortBottomSheet(
                currentSort = sortOption,
                isAscending = isAscending,
                onSortSelected = { option -> viewModel.setSortOption(option) },
                onToggleAscending = { viewModel.toggleAscending() },
                onDismiss = { showSortSheet = false }
            )
        }

        selectedSongForOptions?.let { song ->
            SongOptionsSheet(
                song = song,
                onDismiss = { selectedSongForOptions = null },
                accentColor = playbackState.accentColor,
                onArtistClick = onNavigateToArtist,
                onAlbumClick = onNavigateToAlbum
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongTile(
    song: SongEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    accentColor: Color = Color.White,
    onLongClick: () -> Unit = {},
    showAlbumInSubtitle: Boolean = true,
    showTrackNumber: Boolean = false,
    trackIndex: Int = 0
) {
    val artistName = song.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
    val albumName = song.album?.trim()?.ifBlank { null }
    val subtitleText = remember(song.artist, song.album, showAlbumInSubtitle) {
        if (showAlbumInSubtitle && !albumName.isNullOrEmpty() && !albumName.equals("Unknown Album", ignoreCase = true)) {
            "$artistName • $albumName"
        } else {
            artistName
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showTrackNumber) {
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 52.dp),
                contentAlignment = Alignment.Center
            ) {
                val tNum = song.trackNumber
                val displayNum = if (tNum != null && tNum > 0) String.format("%02d", tNum % 1000) else String.format("%02d", trackIndex + 1)
                Text(
                    text = displayNum,
                    color = if (isPlaying) accentColor else Color.White.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            OptimizedImage(
                url = song.artPath,
                modifier = Modifier.fillMaxSize(),
                sizePx = 160
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
                text = subtitleText,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontFamily = DmSansFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            IconButton(onClick = onOptionsClick) {
                Icon(Lucide.EllipsisVertical, contentDescription = "Options", tint = Color.Gray)
            }
        }
    }
}
