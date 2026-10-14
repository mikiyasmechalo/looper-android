package com.example.looper_android.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.example.looper_android.data.models.LyricLine
import com.example.looper_android.data.models.LyricsSyncMode
import com.example.looper_android.ui.theme.LocalLyricsFontFamily

@Composable
fun AdvancedLyricLine(
    line: LyricLine,
    isActive: Boolean,
    fontScale: Float,
    disableFontScaling: Boolean = true,
    alignment: String = "center",
    activeColor: Color = Color.White,
    positionMs: Long = 0L,
    isPlaying: Boolean = false,
    syncMode: LyricsSyncMode = LyricsSyncMode.CHAR,
    onTap: () -> Unit
) {
    val unsungColor = Color.White.copy(alpha = 0.38f)
    val animatedColor by animateColorAsState(
        targetValue = if (isActive) activeColor else unsungColor,
        animationSpec = tween(durationMillis = 280),
        label = "lyricTextColor"
    )

    val fontSize = if (disableFontScaling) {
        24f * fontScale
    } else {
        if (isActive) 28f * fontScale else 22f * fontScale
    }
    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold

    // Duet partner lines sit on the opposite edge
    val effectiveAlignment = when {
        line.isOppositeSide && alignment.equals("left", ignoreCase = true) -> "right"
        line.isOppositeSide && alignment.equals("right", ignoreCase = true) -> "left"
        else -> alignment
    }

    val textAlignment = when (effectiveAlignment.lowercase()) {
        "left" -> TextAlign.Left
        "right" -> TextAlign.Right
        else -> TextAlign.Center
    }

    val boxAlignment = when (effectiveAlignment.lowercase()) {
        "left" -> Alignment.CenterStart
        "right" -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    val columnAlignment = when (effectiveAlignment.lowercase()) {
        "left" -> Alignment.Start
        "right" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    val isInstrumental = line.text.isEmpty() ||
            line.text == "..." ||
            line.text.contains("instrumental", ignoreCase = true) ||
            line.text.contains("[music]", ignoreCase = true) ||
            line.text.contains("♪")

    val usesWordTiming = line.hasWordTiming && syncMode != LyricsSyncMode.LINE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        contentAlignment = boxAlignment
    ) {
        if (isInstrumental) {
            if (line.text == "...") {
                Text(
                    text = "...",
                    fontFamily = LocalLyricsFontFamily.current,
                    color = animatedColor,
                    fontSize = fontSize.sp,
                    fontWeight = fontWeight,
                    textAlign = textAlignment,
                    lineHeight = (fontSize * 1.5f).sp
                )
            } else {
                val infiniteTransition = rememberInfiniteTransition(label = "pulseInstrumental")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.92f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Icon(
                    imageVector = Lucide.Music,
                    contentDescription = "Instrumental",
                    tint = animatedColor,
                    modifier = Modifier
                        .size((fontSize * 1.25f).dp)
                        .scale(if (isActive) pulseScale else 1f)
                )
            }
        } else if (usesWordTiming) {
            WordSyncedLyricText(
                line = line,
                positionMs = positionMs,
                isPlaying = isPlaying,
                activeColor = activeColor,
                unsungColor = unsungColor,
                fontSize = fontSize.sp,
                fontWeight = fontWeight,
                alignment = effectiveAlignment,
                mode = syncMode,
                isActive = isActive
            )
        } else {
            Column(horizontalAlignment = columnAlignment) {
                Text(
                    text = line.text,
                    fontFamily = LocalLyricsFontFamily.current,
                    color = animatedColor,
                    fontSize = fontSize.sp,
                    fontWeight = fontWeight,
                    textAlign = textAlignment,
                    lineHeight = (fontSize * 1.4f).sp,
                    style = TextStyle(
                        shadow = if (isActive) Shadow(
                            color = activeColor.copy(alpha = 0.55f),
                            blurRadius = 18f,
                            offset = Offset(0f, 2f)
                        ) else null
                    )
                )

                if (line.effectiveBackgroundText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = line.effectiveBackgroundText,
                        fontFamily = LocalLyricsFontFamily.current,
                        color = animatedColor.copy(alpha = animatedColor.alpha * 0.7f),
                        fontSize = (fontSize * 0.62f).sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = textAlignment,
                        lineHeight = (fontSize * 0.9f).sp
                    )
                }
            }
        }
    }
}
