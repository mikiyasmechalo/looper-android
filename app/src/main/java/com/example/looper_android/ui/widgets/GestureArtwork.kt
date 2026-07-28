package com.example.looper_android.ui.widgets

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.theme.JostFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GestureArtwork(
    song: SongEntity,
    nextSong: SongEntity?,
    prevSong: SongEntity?,
    onTap: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val screenWidthPx = LocalConfiguration.current.screenWidthDp * 2.5f

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var feedbackType by remember { mutableStateOf<String?>(null) }

    // Animate feedback overlay
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(song.id) {
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
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        val threshold = screenWidthPx * 0.20f
                        if (dragOffset < -threshold) {
                            if (nextSong != null) {
                                triggerFeedback("next")
                                onSkipNext()
                            }
                        } else if (dragOffset > threshold) {
                            if (prevSong != null) {
                                triggerFeedback("previous")
                                onSkipPrevious()
                            }
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Carousel translation for smooth artwork swipe peek (Hero tag matching Flutter)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(dragOffset.roundToInt(), 0) }
                .sharedBoundsOrElement(
                    key = "album_art",
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    clipShape = RoundedCornerShape(24.dp)
                )
        ) {
            OptimizedImage(
                url = song.artPath,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Feedback Overlay Animation (-10s / +10s / Next / Prev)
        feedbackType?.let { type ->
            val opacity = (1f - feedbackAnimProgress.value).coerceIn(0f, 1f)
            val scale = 0.8f + (feedbackAnimProgress.value * 0.3f)
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
                        .scale(scale)
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
