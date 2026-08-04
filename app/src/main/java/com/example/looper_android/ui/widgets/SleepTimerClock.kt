package com.example.looper_android.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.ui.theme.JostFontFamily

@Composable
fun SleepTimerClock(
    progress: Float, // 0.0f to 1.0f
    label: String,
    color: Color = com.example.looper_android.ui.theme.LocalAccentColor.current,
    modifier: Modifier = Modifier
) {
    val sizeDp = 28.dp

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val strokeWidth = 2.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width - strokeWidth) / 2f

            // Track circle
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Active arc
            if (progress > 0f) {
                val sweepAngle = 360f * progress.coerceIn(0f, 1f)
                drawArc(
                    color = color,
                    startAngle = -90f, // Start from top
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Text(
            text = label,
            color = Color.White,
            fontFamily = JostFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 8.5.sp
        )
    }
}
