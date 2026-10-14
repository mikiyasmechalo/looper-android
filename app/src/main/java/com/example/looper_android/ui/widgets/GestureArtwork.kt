package com.example.looper_android.ui.widgets

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.theme.JostFontFamily
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GestureArtwork(
    song: SongEntity,
    queue: List<SongEntity>,
    currentSongIndex: Int,
    onSkipToIndex: (Int) -> Unit,
    onTap: () -> Unit,
    onSeekRelative: ((Long) -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val safeQueue = if (queue.isEmpty()) listOf(song) else queue
    val safeCurrentIndex = currentSongIndex.coerceIn(0, maxOf(0, safeQueue.size - 1))

    val prevSong = if (safeCurrentIndex > 0) safeQueue.getOrNull(safeCurrentIndex - 1) else null
    val nextSong = if (safeCurrentIndex < safeQueue.size - 1) safeQueue.getOrNull(safeCurrentIndex + 1) else null

    val hasPrev = prevSong != null
    val hasNext = nextSong != null

    val pageCount = when {
        safeQueue.size <= 1 -> 1
        !hasPrev || !hasNext -> 2
        else -> 3
    }

    val currentSlot = when {
        !hasPrev -> 0
        else -> 1
    }

    fun getSongForPage(page: Int): SongEntity {
        return when {
            safeQueue.size <= 1 -> song
            !hasPrev -> if (page == 0) song else (nextSong ?: song)
            !hasNext -> if (page == 1) song else (prevSong ?: song)
            else -> when (page) {
                0 -> prevSong ?: song
                1 -> song
                2 -> nextSong ?: song
                else -> song
            }
        }
    }

    fun getKeyForPage(page: Int): String {
        return when {
            safeQueue.size <= 1 -> "active_${song.id}"
            !hasPrev -> if (page == 0) "active_${song.id}" else "next_${nextSong?.id ?: page}"
            !hasNext -> if (page == 1) "active_${song.id}" else "prev_${prevSong?.id ?: page}"
            else -> when (page) {
                0 -> "prev_${prevSong?.id ?: page}"
                1 -> "active_${song.id}"
                2 -> "next_${nextSong?.id ?: page}"
                else -> "slot_$page"
            }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = currentSlot,
        pageCount = { pageCount }
    )

    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    var wasUserDragged by remember { mutableStateOf(false) }

    LaunchedEffect(isDragged) {
        if (isDragged) {
            wasUserDragged = true
        }
    }

    var feedbackType by remember { mutableStateOf<String?>(null) }
    val feedbackAnimProgress = remember { Animatable(0f) }

    fun triggerFeedback(type: String) {
        feedbackType = type
        coroutineScope.launch {
            feedbackAnimProgress.snapTo(0f)
            feedbackAnimProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing)
            )
            feedbackType = null
        }
    }

    var lastSongId by remember { mutableStateOf(song.id) }

    // When song or slot changes, sync pagerState
    LaunchedEffect(song.id, currentSlot) {
        val isSongChanged = (lastSongId != song.id)
        lastSongId = song.id
        if (isSongChanged) {
            wasUserDragged = false
        }
        if (pagerState.currentPage != currentSlot && !isDragged && !wasUserDragged) {
            pagerState.scrollToPage(currentSlot)
        }
    }

    // User gesture swipe detection: only when user physically dragged the pager and it settled on a new page
    LaunchedEffect(pagerState) {
        snapshotFlow { Pair(pagerState.settledPage, pagerState.isScrollInProgress) }
            .collect { (settledPage, isScrollInProgress) ->
                if (!isScrollInProgress && wasUserDragged) {
                    wasUserDragged = false
                    if (settledPage != currentSlot) {
                        if (settledPage > currentSlot && hasNext) {
                            onSkipToIndex(safeCurrentIndex + 1)
                        } else if (settledPage < currentSlot && hasPrev) {
                            onSkipToIndex(safeCurrentIndex - 1)
                        }
                    }
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .sharedBoundsOrElement(
                key = "album_art",
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                clipShape = RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> getKeyForPage(page) },
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageSong = getSongForPage(page)
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            val isInteracting = isDragged || wasUserDragged
            val scale = if (isInteracting) lerp(1f, 0.90f, pageOffset) else 1f
            val alpha = if (isInteracting) lerp(1f, 0.65f, pageOffset) else 1f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .pointerInput(pageSong.id) {
                        detectTapGestures(
                            onTap = { onTap() }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    OptimizedImage(
                        url = pageSong.artPath,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Feedback Overlay Animation (-10s / +10s / Next / Prev)
        feedbackType?.let { type ->
            val opacity = (1f - feedbackAnimProgress.value).coerceIn(0f, 1f)
            val feedbackScale = 0.8f + (feedbackAnimProgress.value * 0.3f)
            val alignment = when (type) {
                "rewind" -> Alignment.CenterStart
                "forward" -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.35f * opacity)),
                contentAlignment = alignment
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .scale(feedbackScale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.60f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val icon = when (type) {
                            "rewind" -> Icons.Filled.FastRewind
                            "forward" -> Icons.Filled.FastForward
                            "next" -> Icons.Filled.SkipNext
                            else -> Icons.Filled.SkipPrevious
                        }
                        val label = when (type) {
                            "rewind" -> "-10s"
                            "forward" -> "+10s"
                            "next" -> "Next"
                            else -> "Previous"
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            color = Color.White,
                            fontFamily = JostFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
