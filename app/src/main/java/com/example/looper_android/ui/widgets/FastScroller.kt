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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.launch

@Composable
fun FastScroller(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thumbColor: Color = LocalAccentColor.current,
    minItemsForScroller: Int = 10
) {
    val coroutineScope = rememberCoroutineScope()
    var containerHeightPx by remember { mutableStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }
    var showScrollbar by remember { mutableStateOf(false) }

    val isScrollable by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val visible = layoutInfo.visibleItemsInfo.size
            total >= minItemsForScroller && (total - visible) >= 2 && (listState.canScrollForward || listState.canScrollBackward)
        }
    }

    LaunchedEffect(listState.isScrollInProgress, isDragging, isScrollable) {
        if ((listState.isScrollInProgress || isDragging) && isScrollable) {
            showScrollbar = true
        } else {
            delay(1200)
            showScrollbar = false
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (showScrollbar && isScrollable) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "fastScrollAlpha"
    )

    if (animatedAlpha > 0f || (isDragging && isScrollable)) {
        val thumbHeightDp = 48.dp
        val density = LocalDensity.current
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(32.dp)
                .onGloballyPositioned { coordinates ->
                    containerHeightPx = coordinates.size.height
                }
                .pointerInput(minItemsForScroller) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            if (containerHeightPx <= 0) return@detectVerticalDragGestures
                            val layoutInfo = listState.layoutInfo
                            val totalCount = layoutInfo.totalItemsCount
                            val visibleCount = layoutInfo.visibleItemsInfo.size
                            if (totalCount < minItemsForScroller || (totalCount - visibleCount) < 2) return@detectVerticalDragGestures

                            isDragging = true
                            dragProgress = (offset.y / containerHeightPx).coerceIn(0f, 1f)
                            val maxVisible = (totalCount - visibleCount).coerceAtLeast(1)
                            val exact = dragProgress * maxVisible
                            val idx = exact.toInt()
                            val firstSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                            val offsetPx = ((exact - idx) * firstSize).toInt()
                            coroutineScope.launch {
                                listState.scrollToItem(idx, offsetPx)
                            }
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            if (containerHeightPx <= 0) return@detectVerticalDragGestures
                            val layoutInfo = listState.layoutInfo
                            val totalCount = layoutInfo.totalItemsCount
                            val visibleCount = layoutInfo.visibleItemsInfo.size
                            if (totalCount < minItemsForScroller || (totalCount - visibleCount) < 2) return@detectVerticalDragGestures

                            dragProgress = (change.position.y / containerHeightPx).coerceIn(0f, 1f)
                            val maxVisible = (totalCount - visibleCount).coerceAtLeast(1)
                            val exact = dragProgress * maxVisible
                            val idx = exact.toInt()
                            val firstSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                            val offsetPx = ((exact - idx) * firstSize).toInt()
                            coroutineScope.launch {
                                listState.scrollToItem(idx, offsetPx)
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        val layoutInfo = listState.layoutInfo
                        val totalCount = layoutInfo.totalItemsCount
                        val visibleCount = layoutInfo.visibleItemsInfo.size
                        if (totalCount < minItemsForScroller || (totalCount - visibleCount) < 2) {
                            return@offset IntOffset(0, 0)
                        }

                        val progress = if (isDragging) {
                            dragProgress
                        } else {
                            val maxScrollable = (totalCount - visibleCount).toFloat().coerceAtLeast(1f)
                            val firstVisible = listState.firstVisibleItemIndex
                            val firstItemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 1f
                            val offsetFraction = if (firstItemSize > 0f) {
                                (listState.firstVisibleItemScrollOffset.toFloat() / firstItemSize).coerceIn(0f, 1f)
                            } else 0f
                            ((firstVisible.toFloat() + offsetFraction) / maxScrollable).coerceIn(0f, 1f)
                        }

                        val maxOffset = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                        IntOffset(0, (progress * maxOffset).toInt())
                    }
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

@Composable
fun FastScroller(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    thumbColor: Color = LocalAccentColor.current,
    minItemsForScroller: Int = 10
) {
    val coroutineScope = rememberCoroutineScope()
    var containerHeightPx by remember { mutableStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }
    var showScrollbar by remember { mutableStateOf(false) }

    val isScrollable by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val visible = layoutInfo.visibleItemsInfo.size
            total >= minItemsForScroller && (total - visible) >= 2 && (gridState.canScrollForward || gridState.canScrollBackward)
        }
    }

    LaunchedEffect(gridState.isScrollInProgress, isDragging, isScrollable) {
        if ((gridState.isScrollInProgress || isDragging) && isScrollable) {
            showScrollbar = true
        } else {
            delay(1200)
            showScrollbar = false
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (showScrollbar && isScrollable) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "fastScrollGridAlpha"
    )

    if (animatedAlpha > 0f || (isDragging && isScrollable)) {
        val thumbHeightDp = 48.dp
        val density = LocalDensity.current
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(32.dp)
                .onGloballyPositioned { coordinates ->
                    containerHeightPx = coordinates.size.height
                }
                .pointerInput(minItemsForScroller) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            if (containerHeightPx <= 0) return@detectVerticalDragGestures
                            val layoutInfo = gridState.layoutInfo
                            val totalCount = layoutInfo.totalItemsCount
                            val visibleCount = layoutInfo.visibleItemsInfo.size
                            if (totalCount < minItemsForScroller || (totalCount - visibleCount) < 2) return@detectVerticalDragGestures

                            isDragging = true
                            dragProgress = (offset.y / containerHeightPx).coerceIn(0f, 1f)
                            val maxVisible = (totalCount - visibleCount).coerceAtLeast(1)
                            val exact = dragProgress * maxVisible
                            val idx = exact.toInt()
                            val firstSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 0
                            val offsetPx = ((exact - idx) * firstSize).toInt()
                            coroutineScope.launch {
                                gridState.scrollToItem(idx, offsetPx)
                            }
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            if (containerHeightPx <= 0) return@detectVerticalDragGestures
                            val layoutInfo = gridState.layoutInfo
                            val totalCount = layoutInfo.totalItemsCount
                            val visibleCount = layoutInfo.visibleItemsInfo.size
                            if (totalCount < minItemsForScroller || (totalCount - visibleCount) < 2) return@detectVerticalDragGestures

                            dragProgress = (change.position.y / containerHeightPx).coerceIn(0f, 1f)
                            val maxVisible = (totalCount - visibleCount).coerceAtLeast(1)
                            val exact = dragProgress * maxVisible
                            val idx = exact.toInt()
                            val firstSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 0
                            val offsetPx = ((exact - idx) * firstSize).toInt()
                            coroutineScope.launch {
                                gridState.scrollToItem(idx, offsetPx)
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        val layoutInfo = gridState.layoutInfo
                        val totalCount = layoutInfo.totalItemsCount
                        val visibleCount = layoutInfo.visibleItemsInfo.size
                        if (totalCount < minItemsForScroller || (totalCount - visibleCount) < 2) {
                            return@offset IntOffset(0, 0)
                        }

                        val progress = if (isDragging) {
                            dragProgress
                        } else {
                            val maxScrollable = (totalCount - visibleCount).toFloat().coerceAtLeast(1f)
                            val firstVisible = gridState.firstVisibleItemIndex
                            val firstItemHeight = layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height?.toFloat() ?: 1f
                            val offsetFraction = if (firstItemHeight > 0f) {
                                (gridState.firstVisibleItemScrollOffset.toFloat() / firstItemHeight).coerceIn(0f, 1f)
                            } else 0f
                            ((firstVisible.toFloat() + offsetFraction) / maxScrollable).coerceIn(0f, 1f)
                        }

                        val maxOffset = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                        IntOffset(0, (progress * maxOffset).toInt())
                    }
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

