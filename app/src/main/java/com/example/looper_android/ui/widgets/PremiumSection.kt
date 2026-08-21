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
import androidx.compose.runtime.compositionLocalOf
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

val LocalHazeState = compositionLocalOf<HazeState?> { null }
val LocalIsBlurEnabled = compositionLocalOf { false }

@Composable
fun PremiumSection(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    cornerRadius: Dp? = null,
    backgroundColor: Color = Color.White.copy(alpha = 0.06f),
    width: Dp? = null,
    height: Dp? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    showLeftBorder: Boolean = true,
    showRightBorder: Boolean = true,
    enableBlur: Boolean = false,
    hazeState: HazeState? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val finalShape = when {
        shape != null -> shape
        cornerRadius != null -> RoundedCornerShape(cornerRadius)
        else -> RoundedCornerShape(12.dp)
    }

    val actualBgColor = if (enableBlur) {
        if (backgroundColor == Color(0xFF2A2A2A) || backgroundColor == Color(0xFF1A1A1A) || backgroundColor == Color(0xFF1E1E1E) || backgroundColor == Color(0xFF0A0A0A)) {
            Color.White.copy(alpha = 0.08f)
        } else if (backgroundColor == androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant || backgroundColor == Color.White.copy(alpha = 0.05f)) {
            Color.White.copy(alpha = 0.05f) // Matches Flutter app's 5% white glass tint
        } else {
            backgroundColor
        }
    } else {
        backgroundColor
    }

    var boxModifier = modifier
        .clip(finalShape)
    
    if (enableBlur && hazeState != null) {
        boxModifier = boxModifier.hazeChild(
            state = hazeState,
            style = dev.chrisbanes.haze.HazeStyle(
                backgroundColor = Color.Transparent,
                tints = emptyList(),
                blurRadius = 16.dp
            )
        )
    }
    
    boxModifier = boxModifier
        .background(actualBgColor)
        .drawWithCache {
            val desiredWidth = 1.2.dp.toPx()
            val strokeWidth = desiredWidth * 2 
            val strokeColor = Color.White.copy(alpha = 0.05f) // Matches Flutter app exactly
            val outline = finalShape.createOutline(size, layoutDirection, this)
            onDrawWithContent {
                drawContent()
                clipRect(
                    left = if (showLeftBorder) -100f else desiredWidth,
                    top = -100f,
                    right = size.width + (if (showRightBorder) 100f else -desiredWidth),
                    bottom = size.height + 100f
                ) {
                    drawOutline(outline, strokeColor, style = Stroke(strokeWidth))
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
