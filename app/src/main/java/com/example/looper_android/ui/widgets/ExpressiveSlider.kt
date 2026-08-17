package com.example.looper_android.ui.widgets

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.ui.theme.DmSansFontFamily
import android.os.SystemClock
import kotlinx.coroutines.isActive
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ExpressiveSlider(
    positionMs: Long,
    durationMs: Long,
    songId: Long = 0L,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onSeekStart: (() -> Unit)? = null,
    onSeekEnd: (() -> Unit)? = null,
    activeColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current,
    inactiveColor: Color = Color.White.copy(alpha = 0.10f),
    showTimestamps: Boolean = true,
    modifier: Modifier = Modifier
) {
    val totalDurationMs = durationMs.coerceAtLeast(1L)

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf<Float?>(null) }

    var smoothPositionMs by remember { mutableLongStateOf(positionMs) }

    // Reset slider state whenever the song changes (keyed on songId) or the duration changes.
    // Using songId ensures a clean reset even when two consecutive songs share the same duration.
    LaunchedEffect(songId, durationMs) {
        dragProgress = null
        smoothPositionMs = 0L
    }

    LaunchedEffect(positionMs, isPlaying) {
        if (!isDragging) {
            smoothPositionMs = if (positionMs > totalDurationMs) 0L else positionMs
        }
        if (isPlaying && !isDragging && durationMs > 0L) {
            var lastRealtime = SystemClock.elapsedRealtime()
            while (isActive && isPlaying && !isDragging) {
                withFrameNanos {
                    val now = SystemClock.elapsedRealtime()
                    val delta = now - lastRealtime
                    lastRealtime = now
                    if (delta > 0) {
                        smoothPositionMs = (smoothPositionMs + delta).coerceAtMost(totalDurationMs)
                    }
                }
            }
        }
    }

    val displayPositionMs = if (isDragging && dragProgress != null) {
        (dragProgress!! * totalDurationMs).toLong()
    } else {
        smoothPositionMs
    }
    val rawProgress = (displayPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    // Force 0% when duration is not yet known — durationMs=0 means TIME_UNSET from ExoPlayer.
    // The frame loop guard above only applies at coroutine start, so this canvas-level guard
    // is the final line of defence against a briefly-wrong totalDurationMs=1 producing 100%.
    val displayProgress = when {
        durationMs <= 0L && !isDragging -> 0f
        positionMs > totalDurationMs && !isDragging -> 0f
        else -> rawProgress
    }

    val density = LocalDensity.current
    val targetAmplitudePx = with(density) { if (isPlaying && displayProgress > 0.12f) 2.dp.toPx() else 0f }
    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitudePx,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "amplitudeAnimation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wavePhase")
    val wavePhaseFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseFactor"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(totalDurationMs) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val initialFraction = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            
                            isDragging = true
                            dragProgress = initialFraction
                            onSeekStart?.invoke()
                            
                            var currentFraction = initialFraction
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.pressed) {
                                    currentFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    dragProgress = currentFraction
                                    change.consume()
                                } else {
                                    change.consume()
                                    break
                                }
                            }
                            
                            val targetMs = (currentFraction * totalDurationMs).toLong()
                            smoothPositionMs = targetMs
                            onSeek(targetMs)
                            
                            isDragging = false
                            dragProgress = null
                            onSeekEnd?.invoke()
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f

                val trackHeightPx = 8.dp.toPx()
                val radiusPx = trackHeightPx / 2f
                val thumbWidthPx = 6.dp.toPx()
                val thumbHeightPx = 16.dp.toPx()
                val thumbGapPx = thumbWidthPx * 0.75f // Exact 4.5dp Flutter gap

                val thumbCenterX = (displayProgress * width).coerceIn(0f, width)
                val activeEnd = (thumbCenterX - thumbGapPx).coerceIn(0f, width)
                val inactiveStart = (thumbCenterX + thumbGapPx).coerceIn(0f, width)

                // 1. Draw Active Track (Wave with Flutter ease envelope matching SquigglySliderTrackShape)
                if (activeEnd > 0f) {
                    val wavelengthPx = 6.dp.toPx()
                    val startX = radiusPx
                    val endX = activeEnd - radiusPx
                    val totalWaveLength = endX - startX

                    if (animatedAmplitude > 0.1f && totalWaveLength > 0f) {
                        val phase = wavePhaseFactor * 2 * Math.PI
                        val easeLength = wavelengthPx * 3f
                        val path = Path()

                        val step = 1.dp.toPx()
                        var currentX = startX
                        var first = true

                        while (currentX < endX) {
                            val x = currentX
                            val distFromStart = x - startX
                            val distFromEnd = endX - x
                            val easeFactor = when {
                                distFromStart < easeLength -> distFromStart / easeLength
                                distFromEnd < easeLength -> distFromEnd / easeLength
                                else -> 1f
                            }.coerceIn(0f, 1f)

                            val y = centerY + sin(x / wavelengthPx - phase).toFloat() * animatedAmplitude * easeFactor
                            if (first) {
                                path.moveTo(x, y)
                                first = false
                            } else {
                                path.lineTo(x, y)
                            }
                            currentX += step
                        }

                        // Always connect to exact endX with easeFactor = 0 (y = centerY)
                        if (!first) {
                            path.lineTo(endX, centerY)
                        }

                        drawPath(
                            path = path,
                            color = activeColor,
                            style = Stroke(width = trackHeightPx, cap = StrokeCap.Round)
                        )
                    } else {
                        drawRoundRect(
                            color = activeColor,
                            topLeft = Offset(0f, centerY - radiusPx),
                            size = Size(activeEnd, trackHeightPx),
                            cornerRadius = CornerRadius(radiusPx, radiusPx)
                        )
                    }
                }

                // 2. Draw Inactive Track
                if (inactiveStart < width) {
                    val inactiveWidth = width - inactiveStart
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(inactiveStart, centerY - radiusPx),
                        size = Size(inactiveWidth, trackHeightPx),
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                }

                // 3. Draw Line Thumb (Vertical capsule 6dp x 16dp)
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(thumbCenterX - thumbWidthPx / 2f, centerY - thumbHeightPx / 2f),
                    size = Size(thumbWidthPx, thumbHeightPx),
                    cornerRadius = CornerRadius(thumbWidthPx / 2f, thumbWidthPx / 2f)
                )
            }
        }

        if (showTimestamps) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedDurationText(durationMs = displayPositionMs)
                AnimatedDurationText(durationMs = totalDurationMs)
            }
        }
    }
}

@Composable
fun AnimatedDurationText(
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val durationStr = formatDuration(durationMs)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        durationStr.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    (slideInVertically { height -> height / 2 } + fadeIn())
                        .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut())
                },
                label = "digitAnim"
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    color = Color.White.copy(alpha = 0.70f),
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.width(if (targetChar == ':') 5.dp else 9.dp)
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

