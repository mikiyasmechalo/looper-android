package com.example.looper_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.example.looper_android.data.db.SongEntity


@Composable
fun PlaylistArtworkCollage(
    songPaths: List<String>,
    allSongs: List<SongEntity>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    accentColor: Color = Color.White,
    fallbackIconSize: Dp = 24.dp
) {
    val artUrls = remember(songPaths, allSongs) {
        if (songPaths.isEmpty() || allSongs.isEmpty()) {
            emptyList()
        } else {
            val pathSet = songPaths.toSet()
            allSongs.asSequence()
                .filter { pathSet.contains(it.path) }
                .mapNotNull { it.artPath?.ifBlank { null } }
                .distinct()
                .take(4)
                .toList()
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center
    ) {
        when {
            artUrls.size >= 4 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        OptimizedImage(
                            url = artUrls[0],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        OptimizedImage(
                            url = artUrls[1],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        OptimizedImage(
                            url = artUrls[2],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        OptimizedImage(
                            url = artUrls[3],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            artUrls.isNotEmpty() -> {
                OptimizedImage(
                    url = artUrls.first(),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.ListMusic,
                        contentDescription = "Playlist",
                        tint = accentColor,
                        modifier = Modifier.size(fallbackIconSize)
                    )
                }
            }
        }
    }
}
