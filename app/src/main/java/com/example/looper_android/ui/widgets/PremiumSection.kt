package com.example.looper_android.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PremiumSection(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    cornerRadius: Dp? = null,
    backgroundColor: Color = Color(0xFF1E1E1E),
    width: Dp? = null,
    height: Dp? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    showLeftBorder: Boolean = true,
    showRightBorder: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val finalShape = when {
        shape != null -> shape
        cornerRadius != null -> RoundedCornerShape(cornerRadius)
        else -> RoundedCornerShape(12.dp)
    }

    var boxModifier = modifier
        .clip(finalShape)
        .background(backgroundColor)
        .drawWithCache {
            // Stroke draws centered on the path. Since we clip to the shape, 
            // half the stroke is outside and gets clipped. We multiply by 2 to get the desired visible inner width.
            val desiredWidth = 1.2.dp.toPx()
            val strokeWidth = desiredWidth * 2 
            val outline = finalShape.createOutline(size, layoutDirection, this)
            onDrawWithContent {
                drawContent()
                clipRect(
                    left = if (showLeftBorder) -100f else desiredWidth,
                    top = -100f,
                    right = size.width + (if (showRightBorder) 100f else -desiredWidth),
                    bottom = size.height + 100f
                ) {
                    drawOutline(outline, Color.White.copy(alpha = 0.05f), style = Stroke(strokeWidth))
                }
            }
        }

    if (width != null) boxModifier = boxModifier.width(width)
    if (height != null) boxModifier = boxModifier.height(height)
    if (onClick != null) {
        boxModifier = boxModifier.clickable { onClick() }
    }

    Box(
        modifier = boxModifier.padding(contentPadding),
        contentAlignment = Alignment.Center,
        content = content
    )
}
