package com.example.looper_android.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.looper_android.data.models.LyricLine

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music

@Composable
fun AdvancedLyricLine(
    line: LyricLine,
    isActive: Boolean,
    fontScale: Float,
    disableFontScaling: Boolean = true,
    alignment: String = "center",
    activeColor: Color = Color.White,
    onTap: () -> Unit
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isActive) activeColor else Color.White.copy(alpha = 0.40f),
        animationSpec = tween(durationMillis = 280),
        label = "lyricTextColor"
    )

    val fontSize = if (disableFontScaling) {
        24f * fontScale
    } else {
        if (isActive) 28f * fontScale else 22f * fontScale
    }
    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold

    val textAlignment = when (alignment.lowercase()) {
        "left" -> androidx.compose.ui.text.style.TextAlign.Left
        "right" -> androidx.compose.ui.text.style.TextAlign.Right
        else -> androidx.compose.ui.text.style.TextAlign.Center
    }

    val boxAlignment = when (alignment.lowercase()) {
        "left" -> androidx.compose.ui.Alignment.CenterStart
        "right" -> androidx.compose.ui.Alignment.CenterEnd
        else -> androidx.compose.ui.Alignment.Center
    }

    val isInstrumental = line.text.isEmpty() ||
            line.text == "..." ||
            line.text.contains("instrumental", ignoreCase = true) ||
            line.text.contains("[music]", ignoreCase = true) ||
            line.text.contains("♪")

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
                    color = animatedColor,
                    fontSize = fontSize.sp,
                    fontWeight = fontWeight,
                    textAlign = textAlignment,
                    lineHeight = (fontSize * 1.5f).sp
                )
            } else {
                Icon(
                    imageVector = Lucide.Music,
                    contentDescription = "Instrumental",
                    tint = animatedColor,
                    modifier = Modifier.size((fontSize * 1.2f).dp)
                )
            }
        } else {
            Text(
                text = line.text,
                color = animatedColor,
                fontSize = fontSize.sp,
                fontWeight = fontWeight,
                textAlign = textAlignment,
                lineHeight = (fontSize * 1.5f).sp
            )
        }
    }
}
