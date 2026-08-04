package com.example.looper_android.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PremiumMusicBar(
    viewModel: PlaybackViewModel,
    onNavigateToPlayer: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val song = playbackState.currentSong
    val haptics = rememberHapticFeedbackHelper()

    val coroutineScope = rememberCoroutineScope()
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(song?.id) {
        dragOffset = Offset.Zero
    }

    AnimatedVisibility(
        visible = song != null && playbackState.isSongReady,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(72.dp)
    ) {
        if (song != null) {
            val tiltX = (dragOffset.x / 100f).coerceIn(-0.2f, 0.2f)
            val tiltY = (dragOffset.y / 100f).coerceIn(-0.1f, 0.1f)

            val subtitleText = remember(song.artist, song.album) {
                val artist = song.artist ?: "Unknown Artist"
                if (!song.album.isNullOrEmpty() && song.album != "Unknown Album") {
                    "$artist • ${song.album}"
                } else {
                    artist
                }
            }

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
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            },
                            onDragEnd = {
                                val dx = dragOffset.x
                                val dy = dragOffset.y

                                if (dy > 70f && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                                    haptics.mediumImpact()
                                    viewModel.clearQueue()
                                    dragOffset = Offset.Zero
                                } else if (dy < -70f && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                                    haptics.lightImpact()
                                    onNavigateToPlayer()
                                    val startOffset = dragOffset
                                    coroutineScope.launch {
                                        animate(
                                            typeConverter = Offset.VectorConverter,
                                            initialValue = startOffset,
                                            targetValue = Offset(0f, startOffset.y * 2.5f),
                                            animationSpec = tween(
                                                durationMillis = 400,
                                                easing = FastOutSlowInEasing
                                            )
                                        ) { value, _ ->
                                            dragOffset = value
                                        }
                                    }
                                } else if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                    if (dx > 70f) {
                                        haptics.lightImpact()
                                        viewModel.skipToPrevious()
                                    } else if (dx < -70f) {
                                        haptics.lightImpact()
                                        viewModel.skipToNext()
                                    }
                                    dragOffset = Offset.Zero
                                } else {
                                    dragOffset = Offset.Zero
                                }
                            },
                            onDragCancel = { dragOffset = Offset.Zero }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Left Section: Song Info
                    PremiumSection(
                        modifier = Modifier
                            .weight(4f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                        backgroundColor = Color(0xFF1E1E1E),
                        onClick = {
                            haptics.lightImpact()
                            onNavigateToPlayer()
                        }
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
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (playbackState.isPlaying) {
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
                                    modifier = Modifier.sharedBoundsOrElement(
                                        key = "song_title",
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                )
                                Text(
                                    text = subtitleText,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    fontFamily = DmSansFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.sharedBoundsOrElement(
                                        key = "song_artist",
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right Section: Play/Pause Button (16dp icon size)
                    PremiumSection(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .sharedBoundsOrElement(
                                key = "player_play_pause_btn",
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                clipShape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 36.dp, bottomEnd = 36.dp)
                            ),
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 36.dp, bottomEnd = 36.dp),
                        backgroundColor = Color(0xFF1E1E1E),
                        onClick = {
                            haptics.mediumImpact()
                            viewModel.togglePlayPause()
                        }
                    ) {
                        AnimatedPlayPauseIcon(
                            isPlaying = playbackState.isPlaying,
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
                    tween(
                        durationMillis = 400,
                        easing = FastOutSlowInEasing
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
