package com.example.looper_android.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.RefreshCw
import com.example.looper_android.data.models.LyricLine
import com.example.looper_android.ui.theme.DmSansFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdvancedLyricRenderer(
    lines: List<LyricLine>,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    timingOffsetMs: Long = 0L,
    onAdjustOffset: (Long) -> Unit = {},
    showControls: Boolean = true,
    isLandscape: Boolean = false,
    disableFontScaling: Boolean = true,
    alignment: String = "center",
    activeColor: Color = Color.White,
    onUserScroll: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var fontScale by remember { mutableFloatStateOf(1.0f) }
    var activeIndex by remember { mutableIntStateOf(-1) }
    var userScrolledAway by remember { mutableStateOf(false) }
    var lastUserScrollTime by remember { mutableStateOf(0L) }

    // Detect user manual dragging on the lyrics list
    LaunchedEffect(listState.interactionSource) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                userScrolledAway = true
                lastUserScrollTime = System.currentTimeMillis()
                onUserScroll()
            }
        }
    }

    // Auto-dismiss floating re-sync bar after 7s of inactivity
    LaunchedEffect(userScrolledAway, lastUserScrollTime) {
        if (userScrolledAway) {
            delay(7000L)
            userScrolledAway = false
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val centerOffset = -((viewportHeightPx / 2.8f)).toInt()

        val effectivePositionMs = (currentPositionMs + timingOffsetMs).coerceAtLeast(0L)

        LaunchedEffect(effectivePositionMs, lines) {
            if (lines.isEmpty()) return@LaunchedEffect

            var newIndex = lines.indexOfLast { effectivePositionMs >= it.startTimeMs }
            if (newIndex == -1) newIndex = 0

            if (newIndex != activeIndex) {
                activeIndex = newIndex
                if (!userScrolledAway && !listState.isScrollInProgress) {
                    listState.animateScrollToItem(
                        index = activeIndex,
                        scrollOffset = centerOffset
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        fontScale = (fontScale * zoom).coerceIn(0.6f, 2.5f)
                    }
                },
            contentPadding = PaddingValues(
                top = 32.dp,
                bottom = 150.dp,
                start = 24.dp,
                end = 24.dp
            )
        ) {
            itemsIndexed(lines) { index, line ->
                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                    AdvancedLyricLine(
                        line = line,
                        isActive = index == activeIndex,
                        fontScale = fontScale,
                        disableFontScaling = disableFontScaling,
                        alignment = alignment,
                        activeColor = activeColor,
                        onTap = { onSeek(line.startTimeMs) }
                    )
                }
            }
        }

        val targetBottomPadding = if (showControls) {
            if (isLandscape) 140.dp else 204.dp
        } else {
            if (isLandscape) 16.dp else 36.dp
        }
        val animatedBottomPadding by animateDpAsState(
            targetValue = targetBottomPadding,
            animationSpec = tween(durationMillis = 300),
            label = "resyncBottomPadding"
        )

        // Floating Re-sync and Timing Offset Adjust Bar
        AnimatedVisibility(
            visible = userScrolledAway,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
            exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = animatedBottomPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(30.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // -0.25s Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            lastUserScrollTime = System.currentTimeMillis()
                            onAdjustOffset(-250L)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "-0.25s",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily
                    )
                }

                // Center Re-sync Button with real-time offset indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(activeColor)
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(activeIndex, centerOffset)
                            }
                            userScrolledAway = false
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.RefreshCw,
                            contentDescription = "Re-sync",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        val offsetLabel = if (timingOffsetMs != 0L) {
                            val totalSecs = timingOffsetMs / 1000f
                            val formatted = if (timingOffsetMs > 0) String.format(java.util.Locale.US, "+%.2fs", totalSecs) else String.format(java.util.Locale.US, "%.2fs", totalSecs)
                            "Re-sync ($formatted)"
                        } else {
                            "Re-sync"
                        }
                        Text(
                            text = offsetLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }

                // +0.25s Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            lastUserScrollTime = System.currentTimeMillis()
                            onAdjustOffset(250L)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+0.25s",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily
                    )
                }
            }
        }
    }
}
