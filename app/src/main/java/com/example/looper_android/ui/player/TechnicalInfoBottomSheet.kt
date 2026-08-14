package com.example.looper_android.ui.player

import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.widgets.PremiumSection
import java.io.File
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalInfoBottomSheet(
    song: SongEntity,
    onDismiss: () -> Unit,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
) {
    val context = LocalContext.current
    var sampleRate by remember { mutableIntStateOf(44100) }
    var bitrate by remember { mutableIntStateOf(320) }
    var channels by remember { mutableIntStateOf(2) }
    var mimeType by remember { mutableStateOf("audio/mp3") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(song.path) {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(song.path)
            if (extractor.trackCount > 0) {
                val format = extractor.getTrackFormat(0)
                if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
                }
                if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                if (format.containsKey(MediaFormat.KEY_MIME)) {
                    mimeType = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp3"
                }
            }
            extractor.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
            // Header: Artwork & Title
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
                        text = "Technical & Frequency Info",
                        color = accentColor,
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Audio Technical Specs Container
            Text(
                text = "AUDIO SPECIFICATIONS",
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            PremiumSection(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f),
                onClick = null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoSpecRow("Format / Codec", mimeType.substringAfter("/").uppercase(), accentColor)
                    InfoSpecRow("Sample Rate", "$sampleRate Hz", accentColor)
                    InfoSpecRow("Bitrate", if (bitrate > 0) "$bitrate kbps" else "VBR / Standard", accentColor)
                    InfoSpecRow("Channels", if (channels == 2) "Stereo (2 Ch)" else "Mono (1 Ch)", accentColor)
                    val dur = song.duration ?: 0
                    val minutes = (dur / 1000) / 60
                    val seconds = (dur / 1000) % 60
                    InfoSpecRow("Duration", String.format("%d:%02d", minutes, seconds), accentColor)
                    InfoSpecRow("File Size", getFileSizeString(song.path), accentColor)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Frequency & Spectrum Analysis Visualizer
            Text(
                text = "FREQUENCY SPECTRUM ANALYSIS",
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            PremiumSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f),
                onClick = null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val bars = 26
                    for (i in 0 until bars) {
                        val heightFactor = (sin(i * 0.45) * 0.4 + 0.55).coerceIn(0.15, 1.0)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .fillMaxHeight(heightFactor.toFloat())
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // File Location
            Text(
                text = "FILE LOCATION",
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            PremiumSection(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f),
                onClick = null
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = song.path,
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun InfoSpecRow(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontFamily = DmSansFontFamily,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

private fun getFileSizeString(path: String): String {
    return try {
        val file = File(path)
        if (file.exists()) {
            val bytes = file.length()
            val mb = bytes / (1024.0 * 1024.0)
            String.format("%.2f MB", mb)
        } else {
            "Unknown"
        }
    } catch (e: Exception) {
        "Unknown"
    }
}
