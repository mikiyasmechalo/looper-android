package com.example.looper_android.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.ui.components.AnimatedPlayPauseIcon
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.player.PlaybackViewModel
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.utils.rememberHapticFeedbackHelper

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

data class MiniPlayerUiState(
    val song: com.example.looper_android.data.db.SongEntity?,
    val isPlaying: Boolean,
    val isSongReady: Boolean
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PremiumMusicBar(
    viewModel: PlaybackViewModel,
    onNavigateToPlayer: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    enableBlur: Boolean = false,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    darkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor = if (darkTheme) Color(0xFF0A0A0A) else Color(0xFF1E1E1E)
    val currentInitial = viewModel.playbackState.value
    val miniPlayerState by remember(viewModel) {
        viewModel.playbackState
            .map { MiniPlayerUiState(it.currentSong, it.isPlaying, it.isSongReady) }
            .distinctUntilChanged()
    }.collectAsState(initial = MiniPlayerUiState(currentInitial.currentSong, currentInitial.isPlaying, currentInitial.isSongReady))
    val song = miniPlayerState.song
    val haptics = rememberHapticFeedbackHelper()

    val coroutineScope = rememberCoroutineScope()
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var lastNavTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(song?.id) {
        dragOffset = Offset.Zero
    }

    val shouldBeVisible = song != null && miniPlayerState.isSongReady
    val visibleState = remember {
        MutableTransitionState(false).apply {
            targetState = shouldBeVisible
        }
    }
    LaunchedEffect(shouldBeVisible) {
        visibleState.targetState = shouldBeVisible
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = slideInVertically(
            animationSpec = spring(dampingRatio = 0.80f, stiffness = 380f),
            initialOffsetY = { fullHeight -> fullHeight * 2 }
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(
            animationSpec = tween(180),
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = tween(180)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(72.dp)
    ) {
        if (song != null) {
            val tiltX = (dragOffset.x / 100f).coerceIn(-0.2f, 0.2f)
            val tiltY = (dragOffset.y / 100f).coerceIn(-0.1f, 0.1f)

            val subtitleText = song.artist ?: "Unknown Artist"

            val localDensity = LocalDensity.current
            val upwardThresholdPx = with(localDensity) { 14.dp.toPx() }
            val swipeThresholdPx = with(localDensity) { 64.dp.toPx() }
            var isPressedPlayPause by remember { mutableStateOf(false) }

            val playPauseScale by animateFloatAsState(
                targetValue = if (isPressedPlayPause) 0.88f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "playPauseScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationX = -tiltY * 57.2958f
                        rotationY = tiltX * 57.2958f
                        translationX = dragOffset.x * 0.3f
                        translationY = dragOffset.y * 0.3f
                        cameraDistance = 12f * density
                    }
                    .pointerInput(song.id) {
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            var isDrag = false
                            val startPos = down.position
                            var currentOffset = Offset.Zero
                            var isCancelled = false

                            if (startPos.x > size.width * 0.78f) {
                                isPressedPlayPause = true
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null) {
                                    isCancelled = true
                                    break
                                }

                                if (change.changedToUp()) {
                                    change.consume()
                                    break
                                }

                                val totalDx = change.position.x - startPos.x
                                val totalDy = change.position.y - startPos.y
                                val distance = kotlin.math.hypot(totalDx, totalDy)

                                if (!isDrag && distance > touchSlop) {
                                    isDrag = true
                                    isPressedPlayPause = false
                                }

                                if (isDrag) {
                                    change.consume()
                                    currentOffset = Offset(totalDx, totalDy)
                                    dragOffset = currentOffset
                                }
                            }

                            isPressedPlayPause = false

                            if (isCancelled) {
                                coroutineScope.launch {
                                    animate(
                                        typeConverter = Offset.VectorConverter,
                                        initialValue = dragOffset,
                                        targetValue = Offset.Zero,
                                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                    ) { value, _ -> dragOffset = value }
                                }
                            } else if (!isDrag) {
                                // Tap anywhere on the mini player
                                if (startPos.x > size.width * 0.78f) {
                                    haptics.mediumImpact()
                                    viewModel.togglePlayPause()
                                } else {
                                    val now = android.os.SystemClock.elapsedRealtime()
                                    if (now - lastNavTime > 500L) {
                                        lastNavTime = now
                                        haptics.lightImpact()
                                        dragOffset = Offset.Zero
                                        onNavigateToPlayer()
                                    }
                                }
                                dragOffset = Offset.Zero
                            } else {
                                val dx = currentOffset.x
                                val dy = currentOffset.y
                                val absDx = kotlin.math.abs(dx)
                                val absDy = kotlin.math.abs(dy)

                                if (dy < -upwardThresholdPx && absDy > absDx * 0.35f) {
                                    // UPWARD SLIDE -> Continue upward from current dragged position and expand!
                                    haptics.lightImpact()
                                    onNavigateToPlayer()
                                    val startOffset = dragOffset
                                    coroutineScope.launch {
                                        animate(
                                            typeConverter = Offset.VectorConverter,
                                            initialValue = startOffset,
                                            targetValue = Offset(0f, -size.height.toFloat() * 2.5f),
                                            animationSpec = spring(
                                                stiffness = 500f,
                                                dampingRatio = 0.85f
                                            )
                                        ) { value, _ ->
                                            dragOffset = value
                                        }
                                        dragOffset = Offset.Zero
                                    }
                                } else if (dy > swipeThresholdPx && absDy > absDx) {
                                    // DOWNWARD DRAG -> Clear queue
                                    haptics.mediumImpact()
                                    viewModel.clearQueue()
                                    coroutineScope.launch {
                                        animate(
                                            typeConverter = Offset.VectorConverter,
                                            initialValue = dragOffset,
                                            targetValue = Offset.Zero,
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                        ) { value, _ -> dragOffset = value }
                                    }
                                } else if (absDx > absDy && absDx > swipeThresholdPx) {
                                    // HORIZONTAL SWIPE -> Next / Previous
                                    if (dx > 0) {
                                        haptics.lightImpact()
                                        viewModel.skipToPrevious()
                                    } else {
                                        haptics.lightImpact()
                                        viewModel.skipToNext()
                                    }
                                    coroutineScope.launch {
                                        animate(
                                            typeConverter = Offset.VectorConverter,
                                            initialValue = dragOffset,
                                            targetValue = Offset.Zero,
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                        ) { value, _ -> dragOffset = value }
                                    }
                                } else if (dy < -6.dp.toPx() && startPos.x < size.width * 0.78f) {
                                    // Micro-slide upward on Song Info -> Continue upward and expand!
                                    haptics.lightImpact()
                                    onNavigateToPlayer()
                                    val startOffset = dragOffset
                                    coroutineScope.launch {
                                        animate(
                                            typeConverter = Offset.VectorConverter,
                                            initialValue = startOffset,
                                            targetValue = Offset(0f, -size.height.toFloat() * 2.5f),
                                            animationSpec = spring(
                                                stiffness = 500f,
                                                dampingRatio = 0.85f
                                            )
                                        ) { value, _ ->
                                            dragOffset = value
                                        }
                                        dragOffset = Offset.Zero
                                    }
                                } else {
                                    coroutineScope.launch {
                                        animate(
                                            typeConverter = Offset.VectorConverter,
                                            initialValue = dragOffset,
                                            targetValue = Offset.Zero,
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                        ) { value, _ -> dragOffset = value }
                                    }
                                }
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Left Section: Song Info
                    PremiumSection(
                        modifier = Modifier
                            .weight(8f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                        backgroundColor = containerColor,
                        enableBlur = enableBlur,
                        hazeState = hazeState,
                        onClick = null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .sharedBoundsOrElement(
                                        key = "album_art",
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        clipShape = RoundedCornerShape(32.dp),
                                    )
                            ) {
                                OptimizedImage(
                                    url = song.artPath,
                                    modifier = Modifier.fillMaxSize(),
                                    sizePx = 160
                                )
                                if (miniPlayerState.isPlaying) {
                                    Box(
                                        modifier = Modifier
                                           .fillMaxSize()
                                           .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        coil.compose.AsyncImage(
                                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                .data("file:///android_asset/android_icons/Playing.gif")
                                                .build(),
                                            contentDescription = "Playing Indicator",
                                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (miniPlayerState.isPlaying) {
                                                Modifier.basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    velocity = 30.dp,
                                                    initialDelayMillis = 1200
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                                Text(
                                    text = subtitleText,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    fontFamily = DmSansFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right Section: Play/Pause Button
                    PremiumSection(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = playPauseScale
                                scaleY = playPauseScale
                            }
                            .sharedBoundsOrElement(
                                key = "player_play_pause_btn",
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                clipShape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 36.dp, bottomEnd = 36.dp)
                            ),
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 36.dp, bottomEnd = 36.dp),
                        backgroundColor = containerColor,
                        enableBlur = enableBlur,
                        hazeState = hazeState,
                        onClick = null
                    ) {
                        AnimatedPlayPauseIcon(
                            isPlaying = miniPlayerState.isPlaying,
                            modifier = Modifier.size(22.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedBoundsOrElement(
    key: Any,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    clipShape: androidx.compose.ui.graphics.Shape? = null
): Modifier {
    return if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            val contentState = rememberSharedContentState(key = key)
            val boundsModifier = this@sharedBoundsOrElement.sharedBounds(
                sharedContentState = contentState,
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    spring(
                        dampingRatio = 0.82f,
                        stiffness = 550f
                    )
                }
            )
            if (clipShape != null) {
                boundsModifier.clip(clipShape)
            } else {
                boundsModifier
            }
        }
    } else {
        this
    }
}
