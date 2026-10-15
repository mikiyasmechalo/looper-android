package com.example.looper_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import coil.request.CachePolicy
import kotlinx.coroutines.Dispatchers

@Composable
fun OptimizedImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    sizePx: Int? = null
) {
    val context = LocalContext.current
    val imageRequest = remember(url, sizePx, context) {
        if (url.isNullOrEmpty()) null
        else {
            val builder = ImageRequest.Builder(context)
                .data(url)
                .dispatcher(Dispatchers.IO)
                .allowHardware(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
            if (sizePx != null && sizePx > 0) {
                builder.size(sizePx, sizePx)
            }
            builder.build()
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    }
}
