package com.example.looper_android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailsBottomSheet(
    song: SongEntity,
    onDismiss: () -> Unit,
    showGoToArtist: Boolean = true,
    showGoToAlbum: Boolean = true,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onTechnicalInfoClick: () -> Unit,
    accentColor: Color = Color(0xFF55DF69)
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    OptimizedImage(url = song.artPath, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Song Details",
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Rows
            // 1. Artist (Clickable with diagonal up-right arrow icon)
            val artistName = song.artist ?: "Unknown Artist"
            DetailRow(
                label = "Artist",
                value = artistName,
                isClickable = showGoToArtist && song.artist != null,
                onClick = {
                    song.artist?.let {
                        onArtistClick(it)
                        onDismiss()
                    }
                },
                accentColor = accentColor
            )

            // 2. Album Artist (Clickable with diagonal up-right arrow icon)
            val albumArtistName = song.albumArtist ?: song.artist ?: "Unknown Album Artist"
            DetailRow(
                label = "Album Artist",
                value = albumArtistName,
                isClickable = showGoToArtist && (song.albumArtist != null || song.artist != null),
                onClick = {
                    val target = song.albumArtist ?: song.artist
                    target?.let {
                        onArtistClick(it)
                        onDismiss()
                    }
                },
                accentColor = accentColor
            )

            // 3. Album (Clickable with diagonal up-right arrow icon)
            val albumName = song.album ?: "Unknown Album"
            DetailRow(
                label = "Album",
                value = albumName,
                isClickable = showGoToAlbum && song.album != null,
                onClick = {
                    song.album?.let {
                        onAlbumClick(it)
                        onDismiss()
                    }
                },
                accentColor = accentColor
            )

            // 4. Duration
            val dur = song.duration ?: 0
            val minutes = (dur / 1000) / 60
            val seconds = (dur / 1000) % 60
            DetailRow(
                label = "Duration",
                value = String.format("%d:%02d", minutes, seconds),
                isClickable = false
            )

            // 5. Year
            song.year?.let { y ->
                if (y > 0) {
                    DetailRow(
                        label = "Year",
                        value = y.toString(),
                        isClickable = false
                    )
                }
            }

            // 6. Path
            DetailRow(
                label = "File Location",
                value = song.path,
                isClickable = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Technical & Frequency Info Action Button
            PremiumSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    onTechnicalInfoClick()
                    onDismiss()
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Equalizer,
                        contentDescription = "Technical Info",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Technical & Frequency Info",
                        color = Color.White,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isClickable: Boolean,
    onClick: (() -> Unit)? = null,
    accentColor: Color = Color(0xFF55DF69)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (isClickable && onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.45f),
            fontFamily = DmSansFontFamily,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = if (isClickable) accentColor else Color.White,
                fontFamily = DmSansFontFamily,
                fontWeight = if (isClickable) FontWeight.Medium else FontWeight.Normal,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isClickable) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Clickable",
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
