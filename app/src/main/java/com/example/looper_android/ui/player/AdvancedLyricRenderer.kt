package com.example.looper_android.ui.player

 import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.looper_android.data.models.LyricLine

@Composable
fun AdvancedLyricRenderer(
    lines: List<LyricLine>,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onUserScroll: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    var fontScale by remember { mutableFloatStateOf(1.0f) }
    var activeIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(listState.interactionSource) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                onUserScroll()
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val centerOffset = -((viewportHeightPx / 2.8f)).toInt()

        LaunchedEffect(currentPositionMs, lines) {
            if (lines.isEmpty()) return@LaunchedEffect

            var newIndex = lines.indexOfLast { currentPositionMs >= it.startTimeMs }
            if (newIndex == -1) newIndex = 0

            if (newIndex != activeIndex) {
                activeIndex = newIndex
                if (!listState.isScrollInProgress) {
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
                bottom = 140.dp,
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
                        onTap = { onSeek(line.startTimeMs) }
                    )
                }
            }
        }
    }
}
