package com.example.looper_android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.theme.JostFontFamily
import com.example.looper_android.ui.widgets.QueueSwitcher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<SongEntity>,
    currentSong: SongEntity?,
    onDismiss: () -> Unit,
    onSongSelect: (SongEntity) -> Unit,
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    val allQueues by playbackViewModel.allQueues.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QueueSwitcher(
                    queues = allQueues,
                    activeQueueId = playbackState.activeQueueId,
                    activeQueueName = playbackState.activeQueueName,
                    onQueueSelected = { queueId ->
                        playbackViewModel.switchToQueue(queueId)
                    },
                    onDeleteQueue = { queueId ->
                        playbackViewModel.deleteQueue(queueId)
                    },
                    accentColor = playbackState.accentColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Queue is empty",
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = JostFontFamily,
                        fontSize = 16.sp
                    )
                }
            } else {
                val listState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()
                
                LaunchedEffect(Unit) {
                    val index = queue.indexOfFirst { it.id == currentSong?.id }
                    if (index >= 0) {
                        listState.scrollToItem(index)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 72.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(queue, key = { index, song -> "${index}_${song.id}" }) { index, song ->
                            val isPlayingThis = song.id == currentSong?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isPlayingThis) playbackState.accentColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        playbackViewModel.skipToQueueItem(index)
                                        onSongSelect(song)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    OptimizedImage(
                                        url = song.artPath,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isPlayingThis) playbackState.accentColor else Color.White,
                                        fontFamily = JostFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val queueArtistName = song.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
                                    val queueAlbumName = song.album?.trim()?.ifBlank { null }
                                    val queueSubtitleText = if (!queueAlbumName.isNullOrEmpty() && !queueAlbumName.equals("Unknown Album", ignoreCase = true)) {
                                        "$queueArtistName • $queueAlbumName"
                                    } else {
                                        queueArtistName
                                    }

                                    Text(
                                        text = queueSubtitleText,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontFamily = JostFontFamily,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Floating Current Song / Song Count Pill at bottom center (clickable to scroll to active song)
                    val currentIdx = queue.indexOfFirst { it.id == currentSong?.id }
                    val currentPosition = if (currentIdx >= 0) currentIdx + 1 else 1
                    val totalSongs = queue.size

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF222222).copy(alpha = 0.95f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .clickable {
                                if (currentIdx >= 0) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(currentIdx)
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(playbackState.accentColor)
                            )
                            Text(
                                text = "$currentPosition of $totalSongs",
                                color = Color.White,
                                fontFamily = JostFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
