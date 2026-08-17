package com.example.looper_android.ui.library

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.player.QueueSongItem
import com.example.looper_android.ui.player.QueueSongRow
import com.example.looper_android.ui.player.SongOptionsSheet
import com.example.looper_android.ui.theme.JostFontFamily
import com.example.looper_android.ui.widgets.QueueSwitcher
import kotlinx.coroutines.launch

@Composable
fun QueuesScreen(
    onBackClick: () -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel()
) {
    val allQueues by viewModel.allQueues.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val queue = playbackState.queue
    val currentSong = playbackState.currentSong

    var selectedSongForOptions by remember { mutableStateOf<SongEntity?>(null) }
    var selectedSongIndexForOptions by remember { mutableStateOf<Int?>(null) }
    val view = LocalView.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 68.dp.toPx() }

    // Stable local list representing items with invariant stable keys
    var localQueue by remember {
        mutableStateOf(
            queue.mapIndexed { idx, song ->
                QueueSongItem(stableKey = "queue_${song.id}_#$idx", song = song)
            }
        )
    }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var initialDragIndex by remember { mutableStateOf<Int?>(null) }
    var dragVisualOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(queue) {
        if (draggedKey == null) {
            val currentIds = localQueue.map { it.song.id }
            val newIds = queue.map { it.id }
            if (currentIds != newIds) {
                val existingMap = localQueue.groupBy { it.song.id }.mapValues { it.value.toMutableList() }
                localQueue = queue.mapIndexed { idx, song ->
                    val existing = existingMap[song.id]?.removeFirstOrNull()
                    existing ?: QueueSongItem(stableKey = "queue_${song.id}_#$idx", song = song)
                }
            }
        }
    }

    fun hapticTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

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
                activeQueueName = playbackState.activeQueueName,
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

        if (localQueue.isEmpty()) {
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
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            val currentIdx = localQueue.indexOfFirst { it.song.id == currentSong?.id }

            LaunchedEffect(Unit) {
                if (currentIdx >= 0) {
                    listState.scrollToItem(currentIdx)
                }
            }

            val isMiniPlayerVisible = currentSong != null
            val bottomInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val pillBottomPadding = if (isMiniPlayerVisible) (155.dp + bottomInsets) else (95.dp + bottomInsets)
            val listBottomPadding = if (isMiniPlayerVisible) (220.dp + bottomInsets) else (160.dp + bottomInsets)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = listBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = localQueue,
                        key = { _, item -> item.stableKey }
                    ) { index, item ->
                        val isPlayingThis = item.song.id == currentSong?.id
                        val isPlayed = currentIdx >= 0 && index < currentIdx
                        val showReorderHandle = currentIdx >= 0 && index > currentIdx
                        val showOptionsButton = currentIdx < 0 || index >= currentIdx
                        val isBeingDragged = draggedKey == item.stableKey

                        Box(
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec = if (isBeingDragged) null else spring(stiffness = 500f, dampingRatio = 0.85f)
                                )
                                .zIndex(if (isBeingDragged) 10f else 1f)
                                .graphicsLayer {
                                    if (isBeingDragged) {
                                        translationY = dragVisualOffset
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                        alpha = 0.96f
                                    }
                                }
                        ) {
                            QueueSongRow(
                                index = index,
                                stableKey = item.stableKey,
                                song = item.song,
                                isPlayingThis = isPlayingThis,
                                isPlayed = isPlayed,
                                showReorderHandle = showReorderHandle,
                                showOptionsButton = showOptionsButton,
                                isBeingDragged = isBeingDragged,
                                accentColor = playbackState.accentColor,
                                onClick = {
                                    viewModel.skipToQueueItem(index)
                                },
                                onOptionsClick = {
                                    selectedSongForOptions = item.song
                                    selectedSongIndexForOptions = index
                                },
                                onReorderDragStart = {
                                    draggedKey = item.stableKey
                                    initialDragIndex = index
                                    dragVisualOffset = 0f
                                    hapticTick()
                                },
                                onReorderDragEnd = {
                                    val start = initialDragIndex
                                    val end = localQueue.indexOfFirst { it.stableKey == item.stableKey }
                                    if (start != null && end != -1 && start != end) {
                                        viewModel.setQueueOrder(localQueue.map { it.song }, start, end)
                                    }
                                    draggedKey = null
                                    initialDragIndex = null
                                    dragVisualOffset = 0f
                                },
                                onReorderDragCancel = {
                                    draggedKey = null
                                    initialDragIndex = null
                                    dragVisualOffset = 0f
                                },
                                onReorderDragDelta = { deltaY ->
                                    dragVisualOffset += deltaY
                                    val currentIndex = localQueue.indexOfFirst { it.stableKey == item.stableKey }
                                    if (currentIndex != -1) {
                                        val minIndex = (currentIdx + 1).coerceAtLeast(0)
                                        val targetShift = (dragVisualOffset / itemHeightPx).toInt()
                                        val targetIndex = (currentIndex + targetShift).coerceIn(minIndex, localQueue.size - 1)
                                        if (targetIndex != currentIndex) {
                                            val mutable = localQueue.toMutableList()
                                            val moved = mutable.removeAt(currentIndex)
                                            mutable.add(targetIndex, moved)
                                            localQueue = mutable
                                            dragVisualOffset -= (targetIndex - currentIndex) * itemHeightPx
                                            hapticTick()
                                        }
                                    }
                                },
                                onDelete = {
                                    val curIndex = localQueue.indexOfFirst { it.stableKey == item.stableKey }
                                    if (curIndex != -1) {
                                        viewModel.removeQueueItem(curIndex)
                                    }
                                }
                            )
                        }
                    }
                }

                // Floating Current Song / Song Count Pill at bottom center (floating above MiniPlayer)
                val currentPosition = if (currentIdx >= 0) currentIdx + 1 else 1
                val totalSongs = localQueue.size

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = pillBottomPadding)
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

        // Show SongOptionsSheet when 3-dot is clicked on a queue item
        if (selectedSongForOptions != null) {
            SongOptionsSheet(
                song = selectedSongForOptions!!,
                onDismiss = {
                    selectedSongForOptions = null
                    selectedSongIndexForOptions = null
                },
                isCurrentlyPlaying = (selectedSongForOptions?.id == currentSong?.id),
                isFromQueue = true,
                queueIndex = selectedSongIndexForOptions,
                showSleepTimer = true,
                accentColor = playbackState.accentColor
            )
        }
    }
}

