package com.example.looper_android.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.data.models.LyricLine
import com.example.looper_android.data.models.LyricWord
import com.example.looper_android.data.models.LyricsSyncMode
import com.example.looper_android.ui.theme.LocalLyricsFontFamily
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

internal data class WordGroupData(
    val words: List<LyricWord>,
    val displayText: String,
    val hasTrailingSpace: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSyncedLyricText(
    line: LyricLine,
    positionMs: Long,
    isPlaying: Boolean,
    activeColor: Color,
    unsungColor: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    alignment: String,
    mode: LyricsSyncMode,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    // Smooth frame extrapolation between player position updates (approx every 30-50ms)
    var smoothPositionMs by remember { mutableLongStateOf(positionMs) }

    LaunchedEffect(positionMs) {
        val diff = smoothPositionMs - positionMs
        if (diff < 0 || diff > 250L) {
            smoothPositionMs = positionMs
        }
    }

    LaunchedEffect(isPlaying, isActive) {
        if (!isPlaying || !isActive) {
            smoothPositionMs = positionMs
            return@LaunchedEffect
        }
        var lastFrameNanos = 0L
        while (coroutineContext.isActive) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val deltaMs = (frameNanos - lastFrameNanos) / 1_000_000L
                    smoothPositionMs += deltaMs
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    val currentClock = if (isActive) smoothPositionMs else positionMs
    val fontFamily = LocalLyricsFontFamily.current

    val textAlign = when (alignment.lowercase()) {
        "left" -> TextAlign.Left
        "right" -> TextAlign.Right
        else -> TextAlign.Center
    }

    val flowRowAlignment = when (alignment.lowercase()) {
        "left" -> Arrangement.Start
        "right" -> Arrangement.End
        else -> Arrangement.Center
    }

    val columnAlignment = when (alignment.lowercase()) {
        "left" -> Alignment.Start
        "right" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = columnAlignment
    ) {
        // Main lyric line word wrap
        WordGroupFlow(
            words = line.words,
            positionMs = currentClock,
            mode = mode,
            fontSize = fontSize,
            fontWeight = fontWeight,
            activeColor = activeColor,
            unsungColor = unsungColor,
            fontFamily = fontFamily,
            textAlign = textAlign,
            flowAlignment = flowRowAlignment
        )

        // Background vocals (if present in enhanced LRC [bg:...] or TTML role=x-bg)
        if (line.effectiveBackgroundText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            val bgFontSize = (fontSize.value * 0.62f).sp
            val bgUnsungColor = unsungColor.copy(alpha = unsungColor.alpha * 0.7f)
            val bgActiveColor = activeColor.copy(alpha = 0.85f)

            if (line.backgroundWords.isNotEmpty()) {
                WordGroupFlow(
                    words = line.backgroundWords,
                    positionMs = currentClock,
                    mode = mode,
                    fontSize = bgFontSize,
                    fontWeight = FontWeight.Normal,
                    activeColor = bgActiveColor,
                    unsungColor = bgUnsungColor,
                    fontFamily = fontFamily,
                    textAlign = textAlign,
                    flowAlignment = flowRowAlignment
                )
            } else {
                Text(
                    text = line.effectiveBackgroundText,
                    fontSize = bgFontSize,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Normal,
                    color = bgUnsungColor,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordGroupFlow(
    words: List<LyricWord>,
    positionMs: Long,
    mode: LyricsSyncMode,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    activeColor: Color,
    unsungColor: Color,
    fontFamily: FontFamily,
    textAlign: TextAlign,
    flowAlignment: Arrangement.Horizontal
) {
    val groups = remember(words) { groupWords(words) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val spaceWidthDp = remember(fontSize, fontFamily, fontWeight, density) {
        val px = textMeasurer.measure(
            text = " ",
            style = TextStyle(
                fontSize = fontSize,
                fontFamily = fontFamily,
                fontWeight = fontWeight
            )
        ).size.width
        val dpVal = with(density) { px.toDp() }
        if (dpVal > 0.dp) dpVal else (fontSize.value * 0.28f).dp
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = flowAlignment,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (group in groups) {
            val fraction = LyricLine.sungFraction(group.words, positionMs)

            Box(
                modifier = Modifier.padding(
                    end = if (group.hasTrailingSpace) spaceWidthDp else 0.dp
                )
            ) {
                SungWord(
                    text = group.displayText,
                    fraction = fraction,
                    mode = mode,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    activeColor = activeColor,
                    unsungColor = unsungColor,
                    fontFamily = fontFamily,
                    textAlign = textAlign
                )
            }
        }
    }
}

@Composable
private fun SungWord(
    text: String,
    fraction: Float,
    mode: LyricsSyncMode,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    activeColor: Color,
    unsungColor: Color,
    fontFamily: FontFamily,
    textAlign: TextAlign
) {
    var textWidthPx by remember(text, fontSize, fontFamily, fontWeight) { mutableFloatStateOf(0f) }
    val isSinging = fraction > 0f && fraction < 1f

    val fontSizePx = fontSize.value * 2.5f
    val liftPx = 4f
    val eased = FastOutSlowInEasing.transform(fraction.coerceIn(0f, 1f))
    val maxSwell = if (textWidthPx <= 0f) 0f else fontSizePx * 0.12f / textWidthPx
    val swell = if (isSinging) {
        (sin(PI * fraction).toFloat() * min(0.04f, maxSwell))
    } else 0f

    val wordModifier = Modifier.graphicsLayer {
        translationY = -liftPx * eased
        scaleX = 1f + swell
        scaleY = 1f + swell
    }

    if (mode == LyricsSyncMode.WORD) {
        // Discrete Word Pulse illumination
        val animatedColor by animateColorAsState(
            targetValue = if (fraction > 0f) activeColor else unsungColor,
            animationSpec = tween(durationMillis = 160),
            label = "wordPulseColor"
        )
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            color = animatedColor,
            textAlign = textAlign,
            style = TextStyle(
                shadow = if (fraction > 0f) {
                    Shadow(
                        color = activeColor.copy(alpha = 0.5f),
                        blurRadius = 18f,
                        offset = Offset(0f, 3f)
                    )
                } else null
            ),
            onTextLayout = { layoutResult ->
                textWidthPx = layoutResult.size.width.toFloat()
            },
            modifier = wordModifier
        )
    } else if (isSinging && textWidthPx > 0f) {
        // Character Mode (Smooth Gradient Wipe - Apple Music Style)
        val feather = (fontSizePx * 0.5f / textWidthPx).coerceIn(0.03f, 0.45f)
        val center = fraction * (1f + 2f * feather) - feather

        val brush = Brush.horizontalGradient(
            0f to activeColor,
            (center - feather).coerceIn(0f, 1f) to activeColor,
            (center + feather).coerceIn(0f, 1f) to unsungColor,
            1f to unsungColor,
            startX = 0f,
            endX = textWidthPx
        )

        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            style = TextStyle(brush = brush),
            onTextLayout = { layoutResult ->
                textWidthPx = layoutResult.size.width.toFloat()
            },
            modifier = wordModifier
        )
    } else {
        // Fully sung or unsung word
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            color = if (fraction >= 1f) activeColor else unsungColor,
            textAlign = textAlign,
            style = TextStyle(
                shadow = if (fraction >= 1f) {
                    Shadow(
                        color = activeColor.copy(alpha = 0.5f),
                        blurRadius = 18f,
                        offset = Offset(0f, 3f)
                    )
                } else null
            ),
            onTextLayout = { layoutResult ->
                textWidthPx = layoutResult.size.width.toFloat()
            },
            modifier = wordModifier
        )
    }
}

private fun groupWords(words: List<LyricWord>): List<WordGroupData> {
    if (words.isEmpty()) return emptyList()

    val rawSlices = LyricLine.splitAtPunctuation(words)
    val rawGroups = mutableListOf<List<LyricWord>>()
    var current = mutableListOf<LyricWord>()

    for (w in rawSlices) {
        if (current.isNotEmpty() && (w.text.startsWith(" ") || w.text.startsWith("\t"))) {
            rawGroups.add(current)
            current = mutableListOf()
        }
        current.add(w)
        if (LyricLine.endsWord(w.text) || w.text.endsWith(" ") || w.text.endsWith("\t")) {
            rawGroups.add(current)
            current = mutableListOf()
        }
    }
    if (current.isNotEmpty()) rawGroups.add(current)

    val result = mutableListOf<WordGroupData>()
    for (i in rawGroups.indices) {
        val group = rawGroups[i]
        val rawCombined = group.joinToString("") { it.text }
        val displayText = rawCombined.trim()
        if (displayText.isEmpty()) continue

        val hasTrailingSpace = if (i == rawGroups.size - 1) {
            false
        } else {
            val nextGroup = rawGroups[i + 1]
            val nextRaw = nextGroup.joinToString("") { it.text }
            rawCombined.endsWith(" ") || rawCombined.endsWith("\t") ||
                nextRaw.startsWith(" ") || nextRaw.startsWith("\t") ||
                (isLatinOrDigit(displayText.lastOrNull()) && isLatinOrDigit(nextRaw.trimStart().firstOrNull()))
        }

        val trimmedWords = group.mapIndexed { idx, w ->
            val newText = when {
                group.size == 1 -> w.text.trim()
                idx == 0 -> w.text.trimStart()
                idx == group.size - 1 -> w.text.trimEnd()
                else -> w.text
            }
            LyricWord(w.startTimeMs, w.endTimeMs, newText)
        }

        result.add(
            WordGroupData(
                words = trimmedWords,
                displayText = displayText,
                hasTrailingSpace = hasTrailingSpace
            )
        )
    }
    return result
}

private fun isLatinOrDigit(c: Char?): Boolean {
    if (c == null) return false
    return c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in '\u00C0'..'\u024F'
}
