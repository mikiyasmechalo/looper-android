package com.example.looper_android.ui.widgets

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.ui.theme.JostFontFamily

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = JostFontFamily,
    letterSpacing: TextUnit = 0.3.sp
) {
    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = constraints.maxWidth
        val textMeasurer = rememberTextMeasurer()
        val textStyle = remember(color, fontSize, fontWeight, fontFamily, letterSpacing) {
            TextStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing
            )
        }
        val layoutResult = remember(text, textStyle) {
            textMeasurer.measure(text = text, style = textStyle, maxLines = 1)
        }
        val isOverflowing = layoutResult.size.width > maxWidthPx

        Text(
            text = text,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isOverflowing) {
                        Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            velocity = 40.dp
                        )
                    } else {
                        Modifier
                    }
                )
        )
    }
}
