package com.example.looper_android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
