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
    onSeekRelative: (Long) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val safeQueue = if (queue.isEmpty()) listOf(song) else queue
    val safeCurrentIndex = currentSongIndex.coerceIn(0, maxOf(0, safeQueue.size - 1))

    val pagerState = rememberPagerState(
        initialPage = safeCurrentIndex,
        pageCount = { safeQueue.size }
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

    // Programmatic skip animation (e.g. Next / Previous button tap or song finishes)
    LaunchedEffect(safeCurrentIndex) {
        if (safeCurrentIndex in 0 until pagerState.pageCount && pagerState.currentPage != safeCurrentIndex) {
            wasUserDragged = false
            val distance = (pagerState.currentPage - safeCurrentIndex).absoluteValue
            if (distance == 1) {
                pagerState.animateScrollToPage(
                    page = safeCurrentIndex,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            } else {
                pagerState.scrollToPage(safeCurrentIndex)
            }
        }
    }

    // User gesture swipe detection: only when user physically dragged the pager and it settled on a new page
    LaunchedEffect(pagerState) {
        snapshotFlow { Pair(pagerState.settledPage, pagerState.isScrollInProgress) }
            .collect { (settledPage, isScrollInProgress) ->
                if (!isScrollInProgress && wasUserDragged) {
                    wasUserDragged = false
                    if (settledPage != safeCurrentIndex && settledPage in safeQueue.indices) {
                        onSkipToIndex(settledPage)
                    }
                }
            }
    }

    // Sync pager if queue list changes
    LaunchedEffect(safeQueue) {
        if (safeCurrentIndex in 0 until pagerState.pageCount && pagerState.currentPage != safeCurrentIndex) {
            pagerState.scrollToPage(safeCurrentIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageSong = safeQueue.getOrNull(page) ?: song
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            val scale = lerp(1f, 0.90f, pageOffset)
            val alpha = lerp(1f, 0.65f, pageOffset)

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
                            onTap = { onTap() },
                            onDoubleTap = { offset ->
                                val isLeft = offset.x < size.width / 2f
                                if (isLeft) {
                                    onSeekRelative(-10000L)
                                    triggerFeedback("rewind")
                                } else {
                                    onSeekRelative(10000L)
                                    triggerFeedback("forward")
                                }
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .then(
                            if (page == safeCurrentIndex) {
                                Modifier.sharedBoundsOrElement(
                                    key = "album_art",
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    clipShape = RoundedCornerShape(24.dp)
                                )
                            } else Modifier
                        )
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
