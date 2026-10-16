package com.example.looper_android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

/**
 * Renders a blurred artwork background that persists the previous song's image while a new track
 * is loading or decoding, preventing black flashes and stalls during song transitions.
 */
@Composable
fun PersistentBlurredArtworkBackground(
    artPath: String?,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 24.dp,
    darknessAlpha: Float = 0.65f
) {
    val context = LocalContext.current
    var lastValidPath by remember { mutableStateOf(artPath) }
    if (!artPath.isNullOrEmpty()) {
        lastValidPath = artPath
    }
    val targetPath = artPath ?: lastValidPath

    // displayedArt is the artwork currently confirmed and displayed in the base layer.
    var displayedArt by remember { mutableStateOf<String?>(null) }
    var incomingArt by remember { mutableStateOf<String?>(targetPath) }
    var isIncomingLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(targetPath) {
        if (!targetPath.isNullOrEmpty() && targetPath != incomingArt) {
            incomingArt = targetPath
            isIncomingLoaded = false
        }
    }

    LaunchedEffect(isIncomingLoaded, incomingArt) {
        if (isIncomingLoaded && incomingArt != null) {
            delay(350L)
            displayedArt = incomingArt
        }
    }

    val crossfadeAlpha by animateFloatAsState(
        targetValue = if (isIncomingLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "blurCrossfade"
    )

    Box(modifier = modifier) {
        // 1. Base persistent layer: displays previously loaded artwork so the background NEVER drops to black
        val persistentArt = displayedArt
        if (!persistentArt.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(persistentArt)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurRadius > 0.dp) Modifier.blur(radius = blurRadius) else Modifier)
            )
        }

        // 2. Top incoming layer: loads target artwork and fades in smoothly once ready
        val activeTarget = incomingArt ?: targetPath
        if (!activeTarget.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(activeTarget)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = {
                    isIncomingLoaded = true
                    if (displayedArt == null) {
                        displayedArt = activeTarget
                    }
                },
                onError = {
                    isIncomingLoaded = false
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurRadius > 0.dp) Modifier.blur(radius = blurRadius) else Modifier)
                    .graphicsLayer {
                        // On cold start / initial load, show immediately; on track skips, animate smoothly
                        alpha = if (displayedArt == null || displayedArt == activeTarget) 1f else crossfadeAlpha
                    }
            )
        }

        // 3. Darkness / smoked acrylic overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = darknessAlpha))
        )
    }
}
