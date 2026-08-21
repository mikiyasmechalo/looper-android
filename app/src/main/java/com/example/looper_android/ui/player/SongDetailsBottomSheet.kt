package com.example.looper_android.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.composables.icons.lucide.Lucide
import com.example.looper_android.data.db.SongEntity
import com.example.looper_android.ui.components.OptimizedImage
import com.example.looper_android.ui.theme.DmSansFontFamily
import com.example.looper_android.ui.theme.JostFontFamily
import com.example.looper_android.ui.widgets.PremiumSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailsBottomSheet(
    song: SongEntity,
    onDismiss: () -> Unit,
    showGoToArtist: Boolean = true,
    showGoToAlbum: Boolean = true,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    initialShowTechnical: Boolean = false,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
) {
    var isShowingTechnical by remember { mutableStateOf(initialShowTechnical) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        AnimatedContent(
            targetState = isShowingTechnical,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "DetailsToTechTransition"
        ) { showingTech ->
            if (showingTech) {
                TechnicalDetailsContent(
                    song = song,
                    accentColor = accentColor,
                    onBackToDetails = { isShowingTechnical = false }
                )
            } else {
                CleanSongInfoContent(
                    song = song,
                    accentColor = accentColor,
                    showGoToArtist = showGoToArtist,
                    showGoToAlbum = showGoToAlbum,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    onOpenTechnical = { isShowingTechnical = true }
                )
            }
        }
    }
}

@Composable
private fun CleanSongInfoContent(
    song: SongEntity,
    accentColor: Color,
    showGoToArtist: Boolean,
    showGoToAlbum: Boolean,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onOpenTechnical: () -> Unit
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
        // 1. Artist
        val artistName = song.artist?.trim()?.ifBlank { null } ?: "Unknown Artist"
        DetailRow(
            label = "Artist",
            value = artistName,
            isClickable = showGoToArtist && !song.artist.isNullOrBlank(),
            onClick = {
                song.artist?.trim()?.ifBlank { null }?.let { onArtistClick(it) }
            },
            accentColor = accentColor
        )

        // 2. Album Artist
        val albumArtistName = song.albumArtist?.trim()?.ifBlank { null } ?: song.artist?.trim()?.ifBlank { null } ?: "Unknown Album Artist"
        DetailRow(
            label = "Album Artist",
            value = albumArtistName,
            isClickable = showGoToArtist && (!song.albumArtist.isNullOrBlank() || !song.artist.isNullOrBlank()),
            onClick = {
                val target = song.albumArtist?.trim()?.ifBlank { null } ?: song.artist?.trim()?.ifBlank { null }
                target?.let { onArtistClick(it) }
            },
            accentColor = accentColor
        )

        // 3. Album
        val albumName = song.album ?: "Unknown Album"
        DetailRow(
            label = "Album",
            value = albumName,
            isClickable = showGoToAlbum && song.album != null,
            onClick = {
                song.album?.let { onAlbumClick(it) }
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

        Spacer(modifier = Modifier.height(20.dp))

        // Technical & Frequency Info Action Button
        PremiumSection(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            backgroundColor = Color.White.copy(alpha = 0.08f),
            onClick = onOpenTechnical
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Lucide.AudioLines,
                    contentDescription = "Technical Info",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Technical & Frequency Info",
                    color = Color.White,
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun TechnicalDetailsContent(
    song: SongEntity,
    accentColor: Color,
    onBackToDetails: () -> Unit
) {
    var specs by remember { mutableStateOf<ComprehensiveAudioSpecs?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(song.path) {
        isLoading = true
        specs = withContext(Dispatchers.IO) {
            extractComprehensiveSpecs(song)
        }
        isLoading = false
    }

    val currentSpecs = specs ?: ComprehensiveAudioSpecs()
    val nyquist = currentSpecs.sampleRate / 2.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToDetails) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Details",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Song Details",
                color = Color.White,
                fontFamily = JostFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Card (Matching Flutter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            ) {
                OptimizedImage(url = song.artPath, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist ?: "Unknown Artist",
                    color = Color.White.copy(alpha = 0.70f),
                    fontFamily = JostFontFamily,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.album ?: "Unknown Album",
                    color = accentColor,
                    fontFamily = JostFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Card 1: Metadata Details ─────────────────────────────────────
        GlassSectionCard(
            title = "Metadata Details",
            icon = Lucide.Tags
        ) {
            FlutterStyleInfoItem("Track Title", song.title)
            FlutterStyleInfoItem("Artist", song.artist ?: "Unknown Artist")
            FlutterStyleInfoItem("Album", song.album ?: "Unknown Album")
            if (!currentSpecs.albumArtist.isNullOrBlank()) {
                FlutterStyleInfoItem("Album Artist", currentSpecs.albumArtist)
            }
            if (!currentSpecs.composer.isNullOrBlank()) {
                FlutterStyleInfoItem("Composer", currentSpecs.composer)
            }
            FlutterStyleInfoItem("Genre", song.genre ?: currentSpecs.genre ?: "Unknown Genre")
            if (song.year != null && song.year > 0) {
                FlutterStyleInfoItem("Release Year", song.year.toString())
            } else if (!currentSpecs.releaseYear.isNullOrBlank()) {
                FlutterStyleInfoItem("Release Year", currentSpecs.releaseYear)
            }
            FlutterStyleInfoItem("ISRC", currentSpecs.isrc ?: "N/A")
            FlutterStyleInfoItem("Label", currentSpecs.label ?: "N/A")
            FlutterStyleInfoItem("Copyright", currentSpecs.copyright ?: "N/A", isLong = true)
            FlutterStyleInfoItem("Encoder", currentSpecs.encoder ?: "N/A")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Card 2: File Information ─────────────────────────────────────
        GlassSectionCard(
            title = "File Information",
            icon = Lucide.FileText
        ) {
            val fileObj = File(song.path)
            FlutterStyleInfoItem("File Name", fileObj.name)
            FlutterStyleInfoItem("File Format", fileObj.extension.uppercase())
            FlutterStyleInfoItem("Bitrate", formatBitrate(currentSpecs.bitrateBps))
            FlutterStyleInfoItem("File Size", formatFileSize(currentSpecs.fileSize))
            FlutterStyleInfoItem("Date Added", currentSpecs.dateAddedFormatted)
            FlutterStyleInfoItem("Absolute Path", song.path, isLong = true)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Section Title: Acoustic & Spectral Analysis ──────────────────
        Text(
            text = "ACOUSTIC & SPECTRAL ANALYSIS",
            color = Color.White.copy(alpha = 0.70f),
            fontFamily = JostFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // ── Card 3: Audio Stream Details (Flutter AudioInfoCard) ─────────
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.AudioLines,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Audio Stream Details",
                        color = Color.White,
                        fontFamily = JostFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Upper format metrics
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (currentSpecs.codec.isNotEmpty()) {
                        FlutterMetricChip(icon = Lucide.Cpu, label = "Codec", value = currentSpecs.codec)
                    }
                    if (currentSpecs.container.isNotEmpty()) {
                        FlutterMetricChip(icon = Lucide.Disc, label = "Container", value = currentSpecs.container)
                    }
                    FlutterMetricChip(
                        icon = Lucide.Activity,
                        label = "Sample Rate",
                        value = "%.1f kHz".format(currentSpecs.sampleRate / 1000.0)
                    )
                    if (currentSpecs.bitDepth.isNotEmpty()) {
                        FlutterMetricChip(icon = Lucide.AudioWaveform, label = "Bit Depth", value = currentSpecs.bitDepth)
                    }
                    if (currentSpecs.decodedSampleFormat.isNotEmpty()) {
                        FlutterMetricChip(icon = Lucide.SlidersHorizontal, label = "Decoded Format", value = currentSpecs.decodedSampleFormat)
                    }
                    if (currentSpecs.bitrateBps > 0) {
                        FlutterMetricChip(
                            icon = Lucide.Zap,
                            label = "Bitrate",
                            value = formatBitrate(currentSpecs.bitrateBps)
                        )
                    }
                    FlutterMetricChip(
                        icon = Lucide.Music,
                        label = "Channels",
                        value = if (currentSpecs.channels == 2) "Stereo" else if (currentSpecs.channels == 1) "Mono" else "${currentSpecs.channels}"
                    )
                    FlutterMetricChip(
                        icon = Lucide.Timer,
                        label = "Duration",
                        value = formatDuration(currentSpecs.durationMs)
                    )
                    FlutterMetricChip(
                        icon = Lucide.TrendingUp,
                        label = "Nyquist",
                        value = "%.1f kHz".format(nyquist / 1000.0)
                    )
                    if (currentSpecs.fileSize > 0) {
                        FlutterMetricChip(
                            icon = Lucide.HardDrive,
                            label = "File Size",
                            value = formatFileSize(currentSpecs.fileSize)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Middle acoustic metrics
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FlutterMetricChip(icon = Lucide.Activity, label = "Dynamic Range", value = "%.2f dB".format(currentSpecs.dynamicRangeDb))
                    FlutterMetricChip(icon = Lucide.Maximize2, label = "Peak", value = "%.2f dB".format(currentSpecs.peakAmplitudeDb))
                    FlutterMetricChip(icon = Lucide.SlidersHorizontal, label = "RMS", value = "%.2f dB".format(currentSpecs.rmsLevelDb))
                    FlutterMetricChip(icon = Lucide.Volume2, label = "LUFS", value = "%.1f LUFS".format(currentSpecs.integratedLufs))
                    FlutterMetricChip(icon = Lucide.Info, label = "True Peak", value = "%.2f dBTP".format(currentSpecs.truePeakDb))
                    FlutterMetricChip(icon = Lucide.ShieldAlert, label = "Clipping", value = if (currentSpecs.clippingSamples <= 0) "None (Clean)" else "${currentSpecs.clippingSamples} samples")
                    FlutterMetricChip(icon = Lucide.AudioWaveform, label = "Cutoff", value = "%.1f kHz".format(currentSpecs.spectralCutoffHz / 1000.0))
                    FlutterMetricChip(icon = Lucide.Binary, label = "Samples", value = formatNumber(currentSpecs.totalSamples))
                }

                if (currentSpecs.channels > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Per-Channel Metrics",
                        color = Color.White.copy(alpha = 0.60f),
                        fontFamily = JostFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlutterMetricChip(
                            icon = Lucide.Music,
                            label = "Ch 1",
                            value = "P %.1f / R %.1f / DR %.1f".format(currentSpecs.ch1PeakDb, currentSpecs.ch1RmsDb, currentSpecs.ch1DrDb)
                        )
                        FlutterMetricChip(
                            icon = Lucide.Music,
                            label = "Ch 2",
                            value = "P %.1f / R %.1f / DR %.1f".format(currentSpecs.ch2PeakDb, currentSpecs.ch2RmsDb, currentSpecs.ch2DrDb)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Card 4: Spectrogram Waterfall View (Flutter SpectrogramView) ─
        RealSpectrogramWaterfallView(
            filePath = song.path,
            sampleRate = currentSpecs.sampleRate,
            durationMs = currentSpecs.durationMs,
            cutoffHz = currentSpecs.spectralCutoffHz
        )

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun GlassSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(24.dp)
            )
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.01f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.50f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.90f),
                    fontFamily = JostFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
private fun FlutterStyleInfoItem(
    label: String,
    value: String,
    isLong: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = Color.White.copy(alpha = 0.40f),
            fontFamily = JostFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontFamily = JostFontFamily,
            fontSize = 14.sp,
            maxLines = if (isLong) 5 else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FlutterMetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(max = 320.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.54f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: ",
            color = Color.White.copy(alpha = 0.54f),
            fontFamily = JostFontFamily,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontFamily = JostFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}



@Composable
private fun DetailRow(
    label: String,
    value: String,
    isClickable: Boolean,
    onClick: (() -> Unit)? = null,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
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

fun spekColorRGB(intensity: Float): Color {
    val clamped = intensity.coerceIn(0f, 1f)
    val r: Int
    val g: Int
    val b: Int
    when {
        clamped < 0.08f -> {
            val t = clamped / 0.08f
            r = 0
            g = 0
            b = (t * 80).toInt()
        }
        clamped < 0.18f -> {
            val t = (clamped - 0.08f) / 0.10f
            r = (t * 50).toInt()
            g = (t * 30).toInt()
            b = (80 + t * 175).toInt()
        }
        clamped < 0.28f -> {
            val t = (clamped - 0.18f) / 0.10f
            r = (50 + t * 150).toInt()
            g = (30 - t * 30).toInt()
            b = (255 - t * 55).toInt()
        }
        clamped < 0.40f -> {
            val t = (clamped - 0.28f) / 0.12f
            r = (200 + t * 55).toInt()
            g = 0
            b = (200 - t * 200).toInt()
        }
        clamped < 0.52f -> {
            val t = (clamped - 0.40f) / 0.12f
            r = 255
            g = (t * 100).toInt()
            b = 0
        }
        clamped < 0.65f -> {
            val t = (clamped - 0.52f) / 0.13f
            r = 255
            g = (100 + t * 80).toInt()
            b = 0
        }
        clamped < 0.78f -> {
            val t = (clamped - 0.65f) / 0.13f
            r = 255
            g = (180 + t * 55).toInt()
            b = (t * 30).toInt()
        }
        clamped < 0.90f -> {
            val t = (clamped - 0.78f) / 0.12f
            r = 255
            g = (235 + t * 20).toInt()
            b = (30 + t * 100).toInt()
        }
        else -> {
            val t = (clamped - 0.90f) / 0.10f
            r = 255
            g = 255
            b = (130 + t * 125).toInt()
        }
    }
    return Color(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
}

