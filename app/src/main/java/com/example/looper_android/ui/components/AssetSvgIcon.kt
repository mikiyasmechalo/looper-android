package com.example.looper_android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.composables.icons.lucide.*

@Composable
fun AnimatedPlayPauseIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.95f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "playPauseScale"
    )

    AnimatedContent(
        targetState = isPlaying,
        transitionSpec = {
            (scaleIn(animationSpec = tween(180), initialScale = 0.65f) + fadeIn(animationSpec = tween(180)))
                .togetherWith(scaleOut(animationSpec = tween(180), targetScale = 0.65f) + fadeOut(animationSpec = tween(180)))
        },
        modifier = modifier.scale(scale),
        label = "playPauseAnim"
    ) { playing ->
        Icon(
            imageVector = if (playing) Lucide.Pause else Lucide.Play,
            contentDescription = if (playing) "Pause" else "Play",
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
    }
}
