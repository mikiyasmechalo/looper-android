package com.example.looper_android.ui.player

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.theme.JostFontFamily
import com.example.looper_android.ui.widgets.QueueSwitcher
import kotlinx.coroutines.launch
import kotlin.math.abs

@Immutable
data class QueueSongItem(
    val stableKey: String,
    val song: SongEntity
)

@Composable
private fun ReorderGripIcon(tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.size(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(tint)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(tint)
        )
    }
}

@Composable
private fun QueueSongRow(
    index: Int,
    stableKey: String,
    song: SongEntity,
    isPlayingThis: Boolean,
    isPlayed: Boolean,
    showReorderHandle: Boolean,
    showOptionsButton: Boolean,
    isBeingDragged: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onReorderDragStart: () -> Unit,
    onReorderDragEnd: () -> Unit,
    onReorderDragCancel: () -> Unit,
    onReorderDragDelta: (Float) -> Unit,
    onDelete: () -> Unit
) {
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedSwipeOffsetX by animateFloatAsState(
        targetValue = swipeOffsetX,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 450f),
        label = "queue_swipe_offset_$stableKey"
    )
    val view = LocalView.current
    val density = LocalDensity.current
    val deleteThresholdPx = with(density) { 135.dp.toPx() }
    var hasArmedHaptic by remember { mutableStateOf(false) }

    fun hapticReject() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun hapticTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Red delete background shown only when actively swiped (> 2px)
        if (animatedSwipeOffsetX > 2f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Color(0xFFFF3B30).copy(
                            alpha = (animatedSwipeOffsetX / deleteThresholdPx).coerceIn(0.2f, 0.85f)
                        )
                    )
                    .padding(start = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val iconScale = (animatedSwipeOffsetX / deleteThresholdPx).coerceIn(0.6f, 1.15f)
                val iconAlpha = (animatedSwipeOffsetX / 60f).coerceIn(0.1f, 1f)
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = iconAlpha),
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }
        }

        // Main song row content (solid container background so text never bleeds through)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedSwipeOffsetX.toInt(), 0) }
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isBeingDragged) {
                        Color(0xFF282828)
                    } else if (isPlayingThis) {
                        accentColor.copy(alpha = 0.18f)
                    } else if (isPlayed) {
                        Color(0xFF181818).copy(alpha = 0.6f)
                    } else {
                        Color(0xFF1E1E1E)
                    }
                )
                .then(
                    if (isBeingDragged) {
                        Modifier.border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    } else if (isPlayingThis) {
                        Modifier.border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Left: Reordering handle (ONLY for upcoming songs) ─────────────
            if (showReorderHandle) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(stableKey) {
                            var dragDirection = 0 // 0 = undecided, 1 = vertical, 2 = horizontal
                            detectDragGestures(
                                onDragStart = {
                                    dragDirection = 0
                                    swipeOffsetX = 0f
                                    hasArmedHaptic = false
                                },
                                onDragEnd = {
                                    if (dragDirection == 2) {
                                        if (swipeOffsetX >= deleteThresholdPx) {
                                            hapticReject()
                                            swipeOffsetX = 1200f
                                            onDelete()
                                        } else {
                                            swipeOffsetX = 0f
                                        }
                                    } else if (dragDirection == 1) {
                                        onReorderDragEnd()
                                    }
                                    dragDirection = 0
                                    hasArmedHaptic = false
                                },
                                onDragCancel = {
                                    swipeOffsetX = 0f
                                    if (dragDirection == 1) {
                                        onReorderDragCancel()
                                    }
                                    dragDirection = 0
                                    hasArmedHaptic = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (dragDirection == 0) {
                                        if (abs(dragAmount.x) > abs(dragAmount.y) && dragAmount.x > 0) {
                                            dragDirection = 2
                                        } else if (abs(dragAmount.y) >= abs(dragAmount.x)) {
                                            dragDirection = 1
                                            onReorderDragStart()
                                        }
                                    }

                                    if (dragDirection == 2) {
                                        swipeOffsetX = (swipeOffsetX + dragAmount.x).coerceAtLeast(0f)
                                        if (swipeOffsetX >= deleteThresholdPx && !hasArmedHaptic) {
                                            hapticTick()
                                            hasArmedHaptic = true
                                        } else if (swipeOffsetX < deleteThresholdPx && hasArmedHaptic) {
                                            hasArmedHaptic = false
                                        }
                                    } else if (dragDirection == 1) {
                                        onReorderDragDelta(dragAmount.y)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    ReorderGripIcon(
                        tint = if (isPlayingThis) accentColor else Color.White.copy(alpha = 0.55f)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }

            // ── Song Artwork ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .alpha(if (isPlayed) 0.45f else 1f)
            ) {
                OptimizedImage(
                    url = song.artPath,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ── Title & Subtitle ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isPlayed) 0.5f else 1f)
            ) {
                Text(
                    text = song.title,
                    color = if (isPlayingThis) accentColor else Color.White,
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
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
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Right: Vertical three dots button (active & upcoming songs) ───
            if (showOptionsButton) {
                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<SongEntity>,
    currentSong: SongEntity?,
    onDismiss: () -> Unit,
    onSongSelect: (SongEntity) -> Unit,
    onArtistClick: ((String) -> Unit)? = null,
    onAlbumClick: ((String) -> Unit)? = null,
    playbackViewModel: PlaybackViewModel = hiltViewModel()
) {
    val allQueues by playbackViewModel.allQueues.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
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

            if (localQueue.isEmpty()) {
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
                val currentIdx = localQueue.indexOfFirst { it.song.id == currentSong?.id }

                LaunchedEffect(Unit) {
                    if (currentIdx >= 0) {
                        listState.scrollToItem(currentIdx)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 72.dp),
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
                                        playbackViewModel.skipToQueueItem(index)
                                        onSongSelect(item.song)
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
                                            playbackViewModel.setQueueOrder(localQueue.map { it.song }, start, end)
                                        }
                                        draggedKey = null
                                        initialDragIndex = null
                                        dragVisualOffset = 0f
                                    },
                                    onReorderDragCancel = {
                                        draggedKey = null
                                        initialDragIndex = null
                                        dragVisualOffset = 0f
                                        localQueue = queue.mapIndexed { idx, s ->
                                            QueueSongItem(stableKey = "queue_${s.id}_#$idx", song = s)
                                        }
                                    },
                                    onReorderDragDelta = { dy ->
                                        dragVisualOffset += dy
                                        val currentPos = localQueue.indexOfFirst { it.stableKey == item.stableKey }
                                        val minAllowedIdx = (currentIdx + 1).coerceAtMost(localQueue.size - 1)
                                        val threshold = itemHeightPx * 0.5f

                                        // Move down synchronously
                                        while (dragVisualOffset > threshold && currentPos < localQueue.size - 1) {
                                            val targetPos = currentPos + 1
                                            val list = localQueue.toMutableList()
                                            val moved = list.removeAt(currentPos)
                                            list.add(targetPos, moved)
                                            localQueue = list
                                            dragVisualOffset -= itemHeightPx
                                            hapticTick()
                                        }

                                        // Move up synchronously
                                        while (dragVisualOffset < -threshold && currentPos > minAllowedIdx) {
                                            val targetPos = currentPos - 1
                                            val list = localQueue.toMutableList()
                                            val moved = list.removeAt(currentPos)
                                            list.add(targetPos, moved)
                                            localQueue = list
                                            dragVisualOffset += itemHeightPx
                                            hapticTick()
                                        }
                                    },
                                    onDelete = {
                                        val deleteIdx = localQueue.indexOfFirst { it.stableKey == item.stableKey }
                                        if (deleteIdx != -1) {
                                            val list = localQueue.toMutableList()
                                            list.removeAt(deleteIdx)
                                            localQueue = list
                                            playbackViewModel.removeQueueItem(deleteIdx)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Floating Current Song / Song Count Pill at bottom center
                    val currentPosition = if (currentIdx >= 0) currentIdx + 1 else 1
                    val totalSongs = localQueue.size

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
                onArtistClick = { artist ->
                    selectedSongForOptions = null
                    selectedSongIndexForOptions = null
                    onDismiss()
                    onArtistClick?.invoke(artist)
                },
                onAlbumClick = { album ->
                    selectedSongForOptions = null
                    selectedSongIndexForOptions = null
                    onDismiss()
                    onAlbumClick?.invoke(album)
                }
            )
        }
    }
}
