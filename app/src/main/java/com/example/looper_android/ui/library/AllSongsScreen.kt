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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.player.SongOptionsSheet
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AllSongsScreen(
    viewModel: AllSongsViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {}
) {
    val songs by viewModel.sortedSongs.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDbLoaded by viewModel.isDbLoaded.collectAsState()

    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val currentSong = playbackState.currentSong
    val isMiniPlayerVisible = playbackState.isSongReady && currentSong != null

    var showSortSheet by remember { mutableStateOf(false) }
    var selectedSongForOptions by remember { mutableStateOf<SongEntity?>(null) }

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

            // Songs Header Bar (Song Count + Compact Sort Button)
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
                            .clickable { showSortSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.ArrowUpDown,
                            contentDescription = "Sort By",
                            tint = playbackState.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Song List with PullToRefresh swipe-down gesture (120fps butter-smooth drag, 280ms rebound, zero jerk)
            val isRefreshing by viewModel.isLoading.collectAsState()
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
                onRefresh = { viewModel.rescanLibrary() },
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
                if (songs.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No songs found.", color = Color.Gray)
                    }
                } else {
                    val bottomInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val targetBottomPadding = if (isMiniPlayerVisible) (190.dp + bottomInsets) else (120.dp + bottomInsets)
                    val bottomPadding = targetBottomPadding
                    LazyColumn(
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
        }

        // Floating Shuffle Action Button (Compact padding)
        val bottomInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val fabBottomPadding = if (currentSong != null) (185.dp + bottomInsets) else (120.dp + bottomInsets)

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

@Composable
fun SongTile(
    song: SongEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    showAlbumInSubtitle: Boolean = true
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
                text = subtitleText,
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
