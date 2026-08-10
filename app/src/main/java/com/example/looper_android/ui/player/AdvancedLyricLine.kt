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
    onTap: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.4f,
        animationSpec = tween(durationMillis = 300),
        label = "alpha"
    )

    val fontSize = if (isActive) 28f * fontScale else 22f * fontScale
    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold

    val isInstrumental = line.text.isEmpty() ||
            line.text == "..." ||
            line.text.contains("instrumental", ignoreCase = true) ||
            line.text.contains("[music]", ignoreCase = true) ||
            line.text.contains("♪")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
    ) {
        if (isInstrumental) {
            if (line.text == "...") {
                Text(
                    text = "...",
                    color = Color.White,
                    fontSize = fontSize.sp,
                    fontWeight = fontWeight,
                    modifier = Modifier.alpha(alpha),
                    lineHeight = (fontSize * 1.5f).sp
                )
            } else {
                Icon(
                    imageVector = Lucide.Music,
                    contentDescription = "Instrumental",
                    tint = Color.White,
                    modifier = Modifier
                        .size((fontSize * 1.2f).dp)
                        .alpha(alpha)
                )
            }
        } else {
            Text(
                text = line.text,
                color = Color.White,
                fontSize = fontSize.sp,
                fontWeight = fontWeight,
                modifier = Modifier.alpha(alpha),
                lineHeight = (fontSize * 1.5f).sp
            )
        }
    }
}
