package com.example.looper_android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.looper_android.ui.theme.JostFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoScreen(
    song: SongEntity,
    onBackClick: () -> Unit,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Technical & Audio Details",
                        color = Color.White,
                        fontFamily = JostFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141414))
            )
        },
        containerColor = Color(0xFF141414)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header: Artwork & Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                ) {
                    OptimizedImage(url = song.artPath, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontFamily = JostFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist ?: "Unknown Artist",
                        color = Color.White.copy(alpha = 0.70f),
                        fontFamily = JostFontFamily,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.album ?: "Unknown Album",
                        color = accentColor,
                        fontFamily = JostFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(18.dp))

            // ── Section 1: Audio Stream Details ──────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Lucide.AudioLines,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Audio Stream Details",
                            color = Color.White,
                            fontFamily = JostFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (currentSpecs.codec.isNotEmpty()) {
                            TechMetricChip(icon = Lucide.Cpu, label = "Codec", value = currentSpecs.codec)
                        }
                        if (currentSpecs.container.isNotEmpty()) {
                            TechMetricChip(icon = Lucide.Disc, label = "Container", value = currentSpecs.container)
                        }
                        TechMetricChip(
                            icon = Lucide.Activity,
                            label = "Sample Rate",
                            value = "%.1f kHz".format(currentSpecs.sampleRate / 1000.0)
                        )
                        if (currentSpecs.bitDepth.isNotEmpty()) {
                            TechMetricChip(icon = Lucide.AudioWaveform, label = "Bit Depth", value = currentSpecs.bitDepth)
                        }
                        if (currentSpecs.decodedSampleFormat.isNotEmpty()) {
                            TechMetricChip(icon = Lucide.SlidersHorizontal, label = "Decoded Format", value = currentSpecs.decodedSampleFormat)
                        }
                        if (currentSpecs.bitrateBps > 0) {
                            TechMetricChip(
                                icon = Lucide.Zap,
                                label = "Bitrate",
                                value = formatBitrate(currentSpecs.bitrateBps)
                            )
                        }
                        TechMetricChip(
                            icon = Lucide.Music,
                            label = "Channels",
                            value = if (currentSpecs.channels == 2) "Stereo (2 Ch)" else if (currentSpecs.channels == 1) "Mono (1 Ch)" else "${currentSpecs.channels} Ch"
                        )
                        TechMetricChip(
                            icon = Lucide.Timer,
                            label = "Duration",
                            value = formatDuration(currentSpecs.durationMs)
                        )
                        TechMetricChip(
                            icon = Lucide.TrendingUp,
                            label = "Nyquist",
                            value = "%.1f kHz".format(nyquist / 1000.0)
                        )
                        if (currentSpecs.fileSize > 0) {
                            TechMetricChip(
                                icon = Lucide.HardDrive,
                                label = "File Size",
                                value = formatFileSize(currentSpecs.fileSize)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TechMetricChip(icon = Lucide.Activity, label = "Dynamic Range", value = "%.2f dB".format(currentSpecs.dynamicRangeDb))
                        TechMetricChip(icon = Lucide.Maximize2, label = "Peak", value = "%.2f dB".format(currentSpecs.peakAmplitudeDb))
                        TechMetricChip(icon = Lucide.SlidersHorizontal, label = "RMS", value = "%.2f dB".format(currentSpecs.rmsLevelDb))
                        TechMetricChip(icon = Lucide.Volume2, label = "LUFS", value = "%.1f LUFS".format(currentSpecs.integratedLufs))
                        TechMetricChip(icon = Lucide.Info, label = "True Peak", value = "%.2f dBTP".format(currentSpecs.truePeakDb))
                        TechMetricChip(icon = Lucide.ShieldAlert, label = "Clipping", value = if (currentSpecs.clippingSamples <= 0) "None (Clean)" else "${currentSpecs.clippingSamples} samples")
                        TechMetricChip(icon = Lucide.AudioWaveform, label = "Cutoff", value = "%.1f kHz".format(currentSpecs.spectralCutoffHz / 1000.0))
                        TechMetricChip(icon = Lucide.Binary, label = "Samples", value = formatNumber(currentSpecs.totalSamples))
                    }

                    if (currentSpecs.channels >= 2) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "PER-CHANNEL METRICS",
                            color = Color.White.copy(alpha = 0.45f),
                            fontFamily = JostFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(text = "Channel 1 (Left)", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = JostFontFamily)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Peak: %.1f dB".format(currentSpecs.ch1PeakDb), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = JostFontFamily)
                                    Text(text = "RMS: %.1f dB".format(currentSpecs.ch1RmsDb), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = JostFontFamily)
                                    Text(text = "DR: %.1f dB".format(currentSpecs.ch1DrDb), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = JostFontFamily)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(text = "Channel 2 (Right)", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = JostFontFamily)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Peak: %.1f dB".format(currentSpecs.ch2PeakDb), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = JostFontFamily)
                                    Text(text = "RMS: %.1f dB".format(currentSpecs.ch2RmsDb), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = JostFontFamily)
                                    Text(text = "DR: %.1f dB".format(currentSpecs.ch2DrDb), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = JostFontFamily)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 2: Frequency & Spectrum Analysis ─────────────────────
            Text(
                text = "FREQUENCY SPECTRUM ANALYSIS",
                color = Color.White.copy(alpha = 0.50f),
                fontFamily = JostFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "20 Hz", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontFamily = JostFontFamily)
                        Text(text = "1 kHz", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontFamily = JostFontFamily)
                        Text(text = "%.1f kHz (Nyquist)".format(nyquist / 1000.0), color = accentColor.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = JostFontFamily)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val bars = 36
                        for (i in 0 until bars) {
                            val curve = sin((i.toDouble() / bars) * Math.PI)
                            val heightFactor = (curve * 0.75 + 0.20).coerceIn(0.12, 0.98).toFloat()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(heightFactor)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (i >= bars * 0.85) accentColor.copy(alpha = 0.45f)
                                        else accentColor
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 3: Complete Metadata Details ─────────────────────────
            Text(
                text = "METADATA DETAILS",
                color = Color.White.copy(alpha = 0.50f),
                fontFamily = JostFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    MetaSpecRow("Track Title", song.title)
                    MetaSpecRow("Artist", song.artist ?: "Unknown Artist")
                    MetaSpecRow("Album", song.album ?: "Unknown Album")
                    if (!currentSpecs.albumArtist.isNullOrBlank()) {
                        MetaSpecRow("Album Artist", currentSpecs.albumArtist)
                    }
                    if (!currentSpecs.composer.isNullOrBlank()) {
                        MetaSpecRow("Composer", currentSpecs.composer)
                    }
                    MetaSpecRow("Genre", song.genre ?: currentSpecs.genre ?: "Unknown Genre")
                    if (song.year != null && song.year > 0) {
                        MetaSpecRow("Release Year", song.year.toString())
                    } else if (!currentSpecs.releaseYear.isNullOrBlank()) {
                        MetaSpecRow("Release Year", currentSpecs.releaseYear)
                    }
                    if (!currentSpecs.trackNumber.isNullOrBlank()) {
                        MetaSpecRow("Track Number", currentSpecs.trackNumber)
                    }
                    MetaSpecRow("ISRC", currentSpecs.isrc ?: "N/A")
                    MetaSpecRow("Label", currentSpecs.label ?: "N/A")
                    MetaSpecRow("Copyright", currentSpecs.copyright ?: "N/A", isLong = true)
                    MetaSpecRow("Encoder", currentSpecs.encoder ?: "N/A")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 4: File Information ──────────────────────────────────
            Text(
                text = "FILE INFORMATION",
                color = Color.White.copy(alpha = 0.50f),
                fontFamily = JostFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val fileObj = File(song.path)
                    MetaSpecRow("File Name", fileObj.name)
                    MetaSpecRow("File Format", fileObj.extension.uppercase())
                    MetaSpecRow("Bitrate", formatBitrate(currentSpecs.bitrateBps))
                    MetaSpecRow("File Size", formatFileSize(currentSpecs.fileSize))
                    MetaSpecRow("Date Added", currentSpecs.dateAddedFormatted)
                    MetaSpecRow("Absolute Path", song.path, isLong = true)
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

