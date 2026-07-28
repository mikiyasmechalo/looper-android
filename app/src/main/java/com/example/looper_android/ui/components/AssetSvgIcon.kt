package com.example.looper_android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

@Composable
fun AssetSvgIcon(
    assetName: String,
    contentDescription: String? = null,
    tint: Color? = Color.White,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(assetName, context) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/android_icons/$assetName")
            .decoderFactory(SvgDecoder.Factory())
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = modifier
    )
}

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
        AssetSvgIcon(
            assetName = if (playing) "pause_icon.svg" else "play_icon.svg",
            contentDescription = if (playing) "Pause" else "Play",
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
    }
}
