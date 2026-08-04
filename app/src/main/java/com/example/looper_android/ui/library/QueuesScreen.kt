package com.example.looper_android.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.theme.JostFontFamily
import com.example.looper_android.ui.widgets.QueueSwitcher

@Composable
fun QueuesScreen(
    onBackClick: () -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val allQueues by viewModel.allQueues.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val queue = playbackState.queue
    val currentSong = playbackState.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            QueueSwitcher(
                queues = allQueues,
                activeQueueId = playbackState.activeQueueId,
                onQueueSelected = { queueId ->
                    viewModel.switchToQueue(queueId)
                },
                onDeleteQueue = { queueId ->
                    viewModel.deleteQueue(queueId)
                },
                accentColor = playbackState.accentColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp),
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
                                // TODO: play song at index
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
        }
    }
}
