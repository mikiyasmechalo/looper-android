package com.example.looper_android.ui.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.looper_android.ui.theme.LucideIcons
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.widgets.PremiumSection

@Composable
fun AllSongsScreen(
    viewModel: AllSongsViewModel = hiltViewModel(),
    onShufflePlayClick: () -> Unit
) {
    var isShuffleEnabled by remember { mutableStateOf(false) }
    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDbLoaded by viewModel.isDbLoaded.collectAsState()

    val playbackViewModel: com.example.looper_android.ui.player.PlaybackViewModel = hiltViewModel()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val currentSong = playbackState.currentSong

    val context = androidx.compose.ui.platform.LocalContext.current
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
            // Header
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
                    shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                    onClick = {}
                ) {
                    Icon(LucideIcons.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = "All Songs",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                PremiumSection(
                    width = 48.dp, 
                    height = 48.dp, 
                    shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 32.dp, bottomEnd = 32.dp),
                    onClick = {}
                ) {
                    Icon(LucideIcons.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${songs.size} Songs",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontFamily = com.example.looper_android.ui.theme.DmSansFontFamily
                    )
                    androidx.compose.material3.IconButton(onClick = { /* Sort */ }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Sort",
                            tint = Color(0xFF55DF69),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }

            // Song List
            if (isLoading || !isDbLoaded) {
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
                    contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding) // Leave space for FAB & Nav bar
                ) {
                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                        val isCurrent = currentSong?.path == song.path
                        val isPlaying = isCurrent && playbackState.isPlaying
                        SongTile(
                            song = song,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying,
                            onClick = { viewModel.playAll(index, shuffled = isShuffleEnabled) }
                        )
                    }
                }
            }
        }

        // Dynamic Floating Shuffle Button Position
        val fabBottomPadding by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (currentSong != null) 210.dp else 130.dp,
            animationSpec = androidx.compose.animation.core.tween(300),
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
                backgroundColor = if (isShuffleEnabled) Color(0xFF55DF69) else Color(0xFF1E1E1E),
                onClick = {
                    isShuffleEnabled = !isShuffleEnabled
                    if (isShuffleEnabled) {
                        viewModel.shuffleAll()
                    }
                }
            ) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data("file:///android_asset/android_icons/shuffle_button_icon.svg")
                        .build(),
                    contentDescription = "Shuffle",
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(if (isShuffleEnabled) Color.Black else Color.White),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SongTile(song: SongEntity, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
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
                        val context = androidx.compose.ui.platform.LocalContext.current
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
                fontFamily = com.example.looper_android.ui.theme.DmSansFontFamily,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist ?: "Unknown Artist",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontFamily = com.example.looper_android.ui.theme.DmSansFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        androidx.compose.material3.IconButton(onClick = { /* Options */ }) {
            Icon(LucideIcons.MoreVertical, contentDescription = "Options", tint = Color.Gray)
        }
    }
}
