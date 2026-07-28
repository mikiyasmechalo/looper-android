package com.example.looper_android.ui.widgets

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onSeekStart: (() -> Unit)? = null,
    onSeekEnd: (() -> Unit)? = null,
    activeColor: Color = Color(0xFF55DF69),
    inactiveColor: Color = Color.White.copy(alpha = 0.10f),
    showTimestamps: Boolean = true,
    modifier: Modifier = Modifier
) {
    val totalDurationMs = durationMs.coerceAtLeast(1L)

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    var lastSeekProgress by remember { mutableStateOf<Float?>(null) }

    var smoothPositionMs by remember { mutableLongStateOf(positionMs) }

    LaunchedEffect(positionMs, isPlaying) {
        smoothPositionMs = positionMs
        if (isPlaying) {
            var lastRealtime = SystemClock.elapsedRealtime()
            while (isActive) {
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

    LaunchedEffect(positionMs) {
        lastSeekProgress?.let { seek ->
            val currentProgress = (positionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            if (kotlin.math.abs(currentProgress - seek) < 0.05f) {
                lastSeekProgress = null
            }
        }
    }

    val activePositionMs = if (isDragging || lastSeekProgress != null) positionMs else smoothPositionMs
    val progress = (activePositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

    val displayProgress = dragProgress ?: lastSeekProgress ?: progress
    val displayPositionMs = (displayProgress * totalDurationMs).toLong()

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
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            onSeekStart?.invoke()
                            val clickedFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            dragProgress = clickedFraction
                            val targetMs = (clickedFraction * totalDurationMs).toLong()
                            onSeek(targetMs)
                            tryAwaitRelease()
                            isDragging = false
                            dragProgress = null
                            onSeekEnd?.invoke()
                        }
                    )
                }
                .pointerInput(totalDurationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            onSeekStart?.invoke()
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            dragProgress = fraction
                        },
                        onDragEnd = {
                            dragProgress?.let { fraction ->
                                lastSeekProgress = fraction
                                onSeek((fraction * totalDurationMs).toLong())
                            }
                            isDragging = false
                            dragProgress = null
                            onSeekEnd?.invoke()
                        },
                        onDragCancel = {
                            isDragging = false
                            dragProgress = null
                            onSeekEnd?.invoke()
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            dragProgress = fraction
                            onSeek((fraction * totalDurationMs).toLong())
                        }
                    )
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

