package com.example.looper_android.ui.library

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.CirclePlay
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.SquarePen
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.User
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.ArtistCollage
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

private fun formatTotalDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0) return "0 min"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours} hr ${minutes} mins"
        hours > 0 -> "${hours} hr"
        else -> "${minutes.coerceAtLeast(1)} mins"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionOptionsMenuSheet(
    collectionTitle: String,
    collectionSubtitle: String? = null,
    artPathOrUrl: String? = null,
    songs: List<SongEntity>,
    isAlbum: Boolean = false,
    isArtist: Boolean = false,
    onDismiss: () -> Unit,
    onPlayAllClick: (() -> Unit)? = null,
    onShuffleClick: (() -> Unit)? = null,
    onPlayNextClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onFavoriteAllClick: () -> Unit,
    onSaveImageClick: (() -> Unit)? = null,
    onEditArtistClick: (() -> Unit)? = null,
    onEditAlbumTagsClick: (() -> Unit)? = null,
    onGoToArtistClick: ((String) -> Unit)? = null,
    onDeleteAlbumClick: (() -> Unit)? = null,
    onDeleteArtistClick: (() -> Unit)? = null,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current

    fun hapticLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    val totalSec = songs.sumOf { ((it.duration ?: 0) / 1000).toLong() }
    val formattedDuration = formatTotalDuration(totalSec)

    val effectiveSubtitle = collectionSubtitle ?: when {
        isAlbum -> {
            val artistName = songs.firstOrNull()?.let { it.albumArtist ?: it.artist } ?: "Unknown Artist"
            "$artistName • ${songs.size} ${if (songs.size == 1) "track" else "tracks"} • $formattedDuration"
        }
        isArtist -> "${songs.size} ${if (songs.size == 1) "track" else "tracks"} • $formattedDuration"
        else -> "${songs.size} ${if (songs.size == 1) "track" else "tracks"} • $formattedDuration"
    }

    val sheetBg = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.54f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.24f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header Card ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 12.dp, end = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(if (isArtist) CircleShape else RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isArtist) {
                        ArtistCollage(
                            artistImageUrl = artPathOrUrl,
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape
                        )
                    } else {
                        OptimizedImage(
                            url = artPathOrUrl ?: songs.firstOrNull { !it.artPath.isNullOrEmpty() }?.artPath,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collectionTitle,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = effectiveSubtitle,
                        color = Color.White.copy(alpha = 0.50f),
                        fontSize = 12.sp,
                        fontFamily = DmSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onPlayAllClick != null || onShuffleClick != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onPlayAllClick != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                                    .clickable {
                                        hapticLight()
                                        onDismiss()
                                        onPlayAllClick()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Play,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (onShuffleClick != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .clickable {
                                        hapticLight()
                                        onDismiss()
                                        onShuffleClick()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Options List ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.04f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val albumArtistName = if (isAlbum) songs.firstOrNull()?.let { it.albumArtist ?: it.artist } else null

                    // Option Row Helper
                    @Composable
                    fun OptionRow(
                        icon: ImageVector,
                        label: String,
                        iconTint: Color = accentColor,
                        onClick: () -> Unit
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 300.dp)
                                ) {
                                    hapticLight()
                                    onDismiss()
                                    onClick()
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = label,
                                color = Color.White,
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OptionRow(
                        icon = Lucide.CirclePlay,
                        label = "Play Next",
                        onClick = onPlayNextClick
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.8.dp, color = Color.White.copy(alpha = 0.04f))

                    OptionRow(
                        icon = Lucide.ListPlus,
                        label = "Add to Queue",
                        onClick = onAddToQueueClick
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.8.dp, color = Color.White.copy(alpha = 0.04f))

                    OptionRow(
                        icon = Lucide.ListMusic,
                        label = "Add to Playlist",
                        onClick = onAddToPlaylistClick
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.8.dp, color = Color.White.copy(alpha = 0.04f))

                    OptionRow(
                        icon = Lucide.Star,
                        label = "Favorite All Songs",
                        onClick = onFavoriteAllClick
                    )

                    if (onSaveImageClick != null) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.8.dp, color = Color.White.copy(alpha = 0.04f))
                        OptionRow(
                            icon = Lucide.Download,
                            label = if (isArtist) "Save Artist Image" else "Save Album Art",
                            onClick = onSaveImageClick
                        )
                    }

                    if (isArtist && onEditArtistClick != null) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.8.dp, color = Color.White.copy(alpha = 0.04f))
                        OptionRow(
                            icon = Lucide.SquarePen,
                            label = "Edit Artist",
                            onClick = onEditArtistClick
                        )
                    }

                    if (isAlbum && onEditAlbumTagsClick != null) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.8.dp, color = Color.White.copy(alpha = 0.04f))
                        OptionRow(
                            icon = Lucide.SquarePen,
                            label = "Edit Album Tags",
                            onClick = onEditAlbumTagsClick
                        )
                    }

                    if (isAlbum && !albumArtistName.isNullOrBlank() && onGoToArtistClick != null) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.8.dp, color = Color.White.copy(alpha = 0.04f))
                        OptionRow(
                            icon = Lucide.User,
                            label = "Go to Artist ($albumArtistName)",
                            onClick = { onGoToArtistClick(albumArtistName) }
                        )
                    }

                    if (isAlbum && onDeleteAlbumClick != null) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.8.dp, color = Color.White.copy(alpha = 0.04f))
                        OptionRow(
                            icon = Lucide.Trash2,
                            label = "Delete Album from Device",
                            iconTint = Color(0xFFFF4D4D),
                            onClick = onDeleteAlbumClick
                        )
                    }

                    if (isArtist && onDeleteArtistClick != null) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.8.dp, color = Color.White.copy(alpha = 0.04f))
                        OptionRow(
                            icon = Lucide.Trash2,
                            label = "Delete Artist from Device",
                            iconTint = Color(0xFFFF4D4D),
                            onClick = onDeleteArtistClick
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
