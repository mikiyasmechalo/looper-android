package com.example.looper_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import org.json.JSONArray

@Composable
fun ArtistCollage(
    artistImageUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    gap: Dp = 2.dp,
    fallbackIcon: ImageVector = Lucide.User,
    backgroundColor: Color = Color.White.copy(alpha = 0.05f)
) {
    val images = remember(artistImageUrl) {
        if (artistImageUrl.isNullOrBlank()) {
            emptyList()
        } else if (artistImageUrl.startsWith("[") && artistImageUrl.endsWith("]")) {
            try {
                val json = JSONArray(artistImageUrl)
                val list = mutableListOf<String>()
                for (i in 0 until json.length()) {
                    val str = json.optString(i)
                    if (str.isNotBlank()) list.add(str)
                }
                list
            } catch (_: Exception) {
                listOf(artistImageUrl)
            }
        } else {
            listOf(artistImageUrl)
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when (images.size) {
            0 -> {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.38f),
                    modifier = Modifier.size(24.dp)
                )
            }
            1 -> {
                OptimizedImage(
                    url = images[0],
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            2 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(end = gap / 2)
                    ) {
                        OptimizedImage(
                            url = images[0],
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = gap / 2)
                    ) {
                        OptimizedImage(
                            url = images[1],
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            3 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(end = gap / 2)
                    ) {
                        OptimizedImage(
                            url = images[0],
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = gap / 2)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = gap / 2)
                        ) {
                            OptimizedImage(
                                url = images[1],
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = gap / 2)
                        ) {
                            OptimizedImage(
                                url = images[2],
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            else -> {
                // 4 or more images
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(bottom = gap / 2)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(end = gap / 2)
                        ) {
                            OptimizedImage(
                                url = images[0],
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = gap / 2)
                        ) {
                            OptimizedImage(
                                url = images[1],
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = gap / 2)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(end = gap / 2)
                        ) {
                            OptimizedImage(
                                url = images[2],
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = gap / 2)
                        ) {
                            OptimizedImage(
                                url = images[3],
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
