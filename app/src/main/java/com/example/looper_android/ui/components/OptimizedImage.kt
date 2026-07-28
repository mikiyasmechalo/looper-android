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
                .crossfade(200)
            if (sizePx != null && sizePx > 0) {
                builder.size(sizePx, sizePx)
            }
            builder.build()
        }
    }

    if (imageRequest == null) {
        Box(
            modifier = modifier.background(Color(0xFF222222)),
            contentAlignment = Alignment.Center
        ) {}
    } else {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
