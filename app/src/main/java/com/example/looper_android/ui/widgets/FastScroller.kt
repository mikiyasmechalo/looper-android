package com.example.looper_android.ui.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.looper_android.ui.theme.LocalAccentColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

@Composable
fun FastScroller(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = LocalAccentColor.current
) {
    val coroutineScope = rememberCoroutineScope()
    
    var containerHeight by remember { mutableStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }

    val totalItems by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }
    
    if (totalItems == 0) return

    val progress by remember(isDragging) {
        derivedStateOf {
            if (isDragging) {
                dragProgress
            } else {
                val firstVisible = listState.firstVisibleItemIndex
                val visibleItemsCount = listState.layoutInfo.visibleItemsInfo.size
                val maxVisible = (totalItems - visibleItemsCount).coerceAtLeast(1)
                
                when {
                    !listState.canScrollBackward -> 0f
                    !listState.canScrollForward -> 1f
                    else -> (firstVisible.toFloat() / maxVisible.toFloat()).coerceIn(0f, 1f)
                }
            }
        }
    }

    var showScrollbar by remember { mutableStateOf(false) }
    
    LaunchedEffect(listState.isScrollInProgress, isDragging) {
        if (listState.isScrollInProgress || isDragging) {
            showScrollbar = true
        } else {
            delay(1500) // Stay visible for 1.5 seconds after scrolling stops
            showScrollbar = false
        }
    }

    LaunchedEffect(isDragging) {
        if (isDragging) {
            snapshotFlow { targetIndex }
                .collectLatest { index ->
                    if (index != null) {
                        listState.scrollToItem(index)
                    }
                }
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (showScrollbar) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "fastScrollAlpha"
    )

    if (animatedAlpha > 0f || isDragging) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(32.dp) // wide enough for touch target
                .onGloballyPositioned { coordinates ->
                    containerHeight = coordinates.size.height
                }
                .pointerInput(totalItems) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.y / containerHeight).coerceIn(0f, 1f)
                            val visibleItemsCount = listState.layoutInfo.visibleItemsInfo.size
                            val maxVisible = (totalItems - visibleItemsCount).coerceAtLeast(1)
                            targetIndex = (dragProgress * maxVisible).toInt()
                        },
                        onDragEnd = {
                            isDragging = false
                            targetIndex = null
                        },
                        onDragCancel = {
                            isDragging = false
                            targetIndex = null
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            dragProgress = (change.position.y / containerHeight).coerceIn(0f, 1f)
                            val visibleItemsCount = listState.layoutInfo.visibleItemsInfo.size
                            val maxVisible = (totalItems - visibleItemsCount).coerceAtLeast(1)
                            targetIndex = (dragProgress * maxVisible).toInt()
                        }
                    )
                }
        ) {
            val thumbHeightDp = 48.dp
            val density = LocalDensity.current
            val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
            
            val maxOffset = (containerHeight - thumbHeightPx).coerceAtLeast(0f)
            val thumbOffset = progress * maxOffset

            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbOffset.toInt()) }
                    .padding(end = 4.dp)
                    .size(width = 6.dp, height = thumbHeightDp)
                    .alpha(animatedAlpha)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .align(Alignment.TopEnd)
            )
        }
    }
}
