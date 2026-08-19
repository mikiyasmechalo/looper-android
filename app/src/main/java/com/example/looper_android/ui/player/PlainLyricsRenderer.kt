package com.example.looper_android.ui.player

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.ui.theme.DmSansFontFamily

@Composable
fun PlainLyricsRenderer(
    lines: List<String>,
    modifier: Modifier = Modifier,
    disableFontScaling: Boolean = true,
    alignment: String = "center",
    onUserScroll: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    var fontScale by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(listState.interactionSource) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                onUserScroll()
            }
        }
    }

    val textAlign = when (alignment.lowercase()) {
        "start", "left" -> TextAlign.Start
        "end", "right" -> TextAlign.End
        else -> TextAlign.Center
    }

    val effectiveFontSize = if (disableFontScaling) 18.sp else (18 * fontScale).sp
    val effectiveLineHeight = if (disableFontScaling) 28.sp else (28 * fontScale).sp

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                if (!disableFontScaling) {
                    detectTransformGestures { _, _, zoom, _ ->
                        fontScale = (fontScale * zoom).coerceIn(0.7f, 2.2f)
                    }
                }
            },
        contentPadding = PaddingValues(
            top = 32.dp,
            bottom = 140.dp,
            start = 24.dp,
            end = 24.dp
        )
    ) {
        items(lines) { line ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = line.ifBlank { " " },
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = effectiveFontSize,
                    lineHeight = effectiveLineHeight,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = com.example.looper_android.ui.theme.LocalLyricsFontFamily.current,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
