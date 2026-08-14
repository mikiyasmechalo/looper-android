package com.example.looper_android.ui.widgets

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.ui.theme.DmSansFontFamily
import kotlinx.coroutines.isActive

@Composable
fun FlatSlider(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onSeekStart: (() -> Unit)? = null,
    onSeekEnd: (() -> Unit)? = null,
    activeColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current,
    inactiveColor: Color = Color.White.copy(alpha = 0.10f),
    showTimestamps: Boolean = true,
    disableAnimatedDuration: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalDurationMs = durationMs.coerceAtLeast(1L)

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    var lastSeekProgress by remember { mutableStateOf<Float?>(null) }

    var smoothPositionMs by remember { mutableLongStateOf(positionMs) }

    LaunchedEffect(durationMs) {
        lastSeekProgress = null
        dragProgress = null
        smoothPositionMs = 0L
    }

    LaunchedEffect(positionMs, isPlaying) {
        smoothPositionMs = if (positionMs > totalDurationMs) 0L else positionMs
        if (isPlaying) {
            val startPos = smoothPositionMs
            val startTime = SystemClock.elapsedRealtime()
            while (isActive && isPlaying) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                val nextPos = (startPos + elapsed).coerceAtMost(totalDurationMs)
                smoothPositionMs = nextPos
                kotlinx.coroutines.delay(16)
            }
        }
    }

    val displayPositionMs = when {
        isDragging && dragProgress != null -> (dragProgress!! * totalDurationMs).toLong()
        lastSeekProgress != null -> (lastSeekProgress!! * totalDurationMs).toLong()
        else -> smoothPositionMs
    }

    val actualProgress = (displayPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(totalDurationMs) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val targetMs = (fraction * totalDurationMs).toLong()
                        lastSeekProgress = fraction
                        onSeek(targetMs)
                    }
                }
                .pointerInput(totalDurationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            dragProgress = fraction
                            onSeekStart?.invoke()
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
                val thumbGapPx = thumbWidthPx * 0.75f

                val thumbCenterX = (actualProgress * width).coerceIn(0f, width)
                val activeEnd = (thumbCenterX - thumbGapPx).coerceIn(0f, width)
                val inactiveStart = (thumbCenterX + thumbGapPx).coerceIn(0f, width)

                // 1. Flat Active Track
                if (activeEnd > 0f) {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(0f, centerY - radiusPx),
                        size = Size(activeEnd, trackHeightPx),
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                }

                // 2. Flat Inactive Track
                if (inactiveStart < width) {
                    val inactiveWidth = width - inactiveStart
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(inactiveStart, centerY - radiusPx),
                        size = Size(inactiveWidth, trackHeightPx),
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                }

                // 3. Thumb Pill
                val thumbHeightPx = 16.dp.toPx()
                val thumbTopY = centerY - thumbHeightPx / 2f
                val thumbLeftX = thumbCenterX - thumbWidthPx / 2f
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(thumbLeftX, thumbTopY),
                    size = Size(thumbWidthPx, thumbHeightPx),
                    cornerRadius = CornerRadius(thumbWidthPx / 2f, thumbWidthPx / 2f)
                )
            }
        }

        if (showTimestamps) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlatDurationText(durationMs = displayPositionMs, disableAnimatedDuration = disableAnimatedDuration)
                FlatDurationText(durationMs = totalDurationMs, disableAnimatedDuration = disableAnimatedDuration)
            }
        }
    }
}

@Composable
fun FlatDurationText(
    durationMs: Long,
    disableAnimatedDuration: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationStr = formatFlatDuration(durationMs)

    if (disableAnimatedDuration) {
        Text(
            text = durationStr,
            color = Color.White.copy(alpha = 0.70f),
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            durationStr.forEachIndexed { _, char ->
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        (slideInVertically { height -> height / 2 } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut())
                    },
                    label = "digitAnimFlat"
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
}

private fun formatFlatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
