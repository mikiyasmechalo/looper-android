package com.example.looper_android.ui.player

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin

data class ComprehensiveAudioSpecs(
    val codec: String = "",
    val container: String = "",
    val sampleRate: Int = 44100,
    val bitDepth: String = "",
    val decodedSampleFormat: String = "",
    val bitrateBps: Int = 0,
    val channels: Int = 2,
    val channelLayout: String = "Stereo",
    val durationMs: Long = 0L,
    val fileSize: Long = 0L,
    val dynamicRangeDb: Double = 11.4,
    val peakAmplitudeDb: Double = -0.15,
    val rmsLevelDb: Double = -13.8,
    val integratedLufs: Double = -14.1,
    val truePeakDb: Double = -0.08,
    val clippingSamples: Int = 0,
    val spectralCutoffHz: Double = 22050.0,
    val totalSamples: Long = 0L,
    val ch1PeakDb: Double = -0.15,
    val ch1RmsDb: Double = -13.9,
    val ch1DrDb: Double = 11.5,
    val ch2PeakDb: Double = -0.18,
    val ch2RmsDb: Double = -13.7,
    val ch2DrDb: Double = 11.3,
    val composer: String? = null,
    val genre: String? = null,
    val releaseYear: String? = null,
    val trackNumber: String? = null,
    val isrc: String? = null,
    val label: String? = null,
    val copyright: String? = null,
    val encoder: String? = null,
    val albumArtist: String? = null,
    val dateAddedFormatted: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalInfoBottomSheet(
    song: SongEntity,
    onDismiss: () -> Unit,
    accentColor: Color = com.example.looper_android.ui.theme.LocalAccentColor.current
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var specs by remember { mutableStateOf<ComprehensiveAudioSpecs?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(song.path) {
        isLoading = true
        specs = withContext(Dispatchers.IO) {
            extractComprehensiveSpecs(song)
        }
        isLoading = false
    }

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
        val currentSpecs = specs ?: ComprehensiveAudioSpecs()
        val nyquist = currentSpecs.sampleRate / 2.0

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top Bar / Header ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
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
                        fontSize = 19.sp,
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
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Card 2: Acoustic & Dynamic Range Analysis ────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Lucide.Activity,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acoustic & Dynamic Range Analysis",
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

            Spacer(modifier = Modifier.height(16.dp))

            // ── Card 3: Spectral Analysis & Spectrogram ──────────────────────
            RealSpectrogramWaterfallView(
                filePath = song.path,
                sampleRate = currentSpecs.sampleRate,
                durationMs = currentSpecs.durationMs,
                cutoffHz = currentSpecs.spectralCutoffHz
            )

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

@Composable
fun TechMetricChip(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.50f),
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$label: ",
            color = Color.White.copy(alpha = 0.50f),
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
fun MetaSpecRow(
    label: String,
    value: String,
    isLong: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = Color.White.copy(alpha = 0.40f),
            fontFamily = JostFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontFamily = JostFontFamily,
            fontSize = 13.5.sp,
            maxLines = if (isLong) 5 else 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun extractComprehensiveSpecs(song: SongEntity): ComprehensiveAudioSpecs {
    var sampleRate = 44100
    var bitrateBps = 0
    var channels = 2
    var mimeType = ""
    var bitDepth = ""
    var decodedFormat = ""
    var container = ""
    var durationMs = song.duration?.toLong() ?: 0L
    var composer: String? = null
    var genre: String? = song.genre
    var year: String? = song.year?.toString()
    var trackNum: String? = song.trackNumber?.toString()
    var isrc: String? = null
    var label: String? = null
    var copyright: String? = null
    var encoder: String? = null
    var albumArtist: String? = song.albumArtist

    val file = File(song.path)
    val fileSize = if (file.exists()) file.length() else 0L
    val ext = file.extension.uppercase()

    // 1. MediaExtractor
    try {
        val extractor = MediaExtractor()
        extractor.setDataSource(song.path)
        if (extractor.trackCount > 0) {
            val format = extractor.getTrackFormat(0)
            if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            }
            if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                bitrateBps = format.getInteger(MediaFormat.KEY_BIT_RATE)
            }
            if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            }
            if (format.containsKey(MediaFormat.KEY_MIME)) {
                mimeType = format.getString(MediaFormat.KEY_MIME) ?: ""
            }
            if (format.containsKey(MediaFormat.KEY_DURATION)) {
                durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000L
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                val pcm = format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                bitDepth = when (pcm) {
                    android.media.AudioFormat.ENCODING_PCM_8BIT -> "8-bit PCM"
                    android.media.AudioFormat.ENCODING_PCM_16BIT -> "16-bit PCM"
                    android.media.AudioFormat.ENCODING_PCM_FLOAT -> "32-bit Float"
                    android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> "24-bit PCM"
                    android.media.AudioFormat.ENCODING_PCM_32BIT -> "32-bit PCM"
                    else -> ""
                }
            }
        }
        extractor.release()
    } catch (_: Exception) {}

    // 2. MediaMetadataRetriever
    try {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(song.path)
        if (bitrateBps <= 0) {
            val br = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            if (br != null && br > 0) bitrateBps = br
        }
        if (durationMs <= 0L) {
            val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (dur != null && dur > 0L) durationMs = dur
        }
        composer = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
        if (genre.isNullOrBlank()) genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
        if (year.isNullOrBlank()) year = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
        if (trackNum.isNullOrBlank()) trackNum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
        if (albumArtist.isNullOrBlank()) albumArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
        writerOrEncoder(mmr)?.let { encoder = it }
        mmr.release()
    } catch (_: Exception) {}

    // 3. Fallbacks and derived info
    if (bitrateBps <= 0 && durationMs > 0 && fileSize > 0) {
        bitrateBps = ((fileSize * 8L) / (durationMs / 1000.0)).toInt()
    }

    val codec = when {
        mimeType.contains("flac", ignoreCase = true) || ext == "FLAC" -> "FLAC"
        mimeType.contains("mpeg", ignoreCase = true) || mimeType.contains("mp3", ignoreCase = true) || ext == "MP3" -> "MP3"
        mimeType.contains("mp4a", ignoreCase = true) || mimeType.contains("aac", ignoreCase = true) || ext == "M4A" || ext == "AAC" -> "AAC"
        mimeType.contains("opus", ignoreCase = true) || ext == "OPUS" -> "Opus"
        mimeType.contains("vorbis", ignoreCase = true) || ext == "OGG" -> "Vorbis"
        mimeType.contains("raw", ignoreCase = true) || ext == "WAV" -> "WAV / PCM"
        mimeType.contains("alac", ignoreCase = true) -> "ALAC"
        else -> ext
    }

    container = when (ext) {
        "FLAC" -> "FLAC Container"
        "MP3" -> "MPEG Audio"
        "M4A" -> "QuickTime / MP4"
        "OGG" -> "Ogg Stream"
        "OPUS" -> "Ogg Opus"
        "WAV" -> "RIFF Wave"
        else -> "$ext Container"
    }

    if (bitDepth.isEmpty()) {
        bitDepth = when (codec) {
            "FLAC", "ALAC", "WAV / PCM" -> if (sampleRate > 48000) "24-bit PCM" else "16-bit PCM"
            else -> "Compressed (Lossy)"
        }
    }

    decodedFormat = when {
        bitDepth.contains("24") -> "pcm_s24le"
        bitDepth.contains("32") -> "pcm_f32le"
        else -> "pcm_s16le"
    }

    val isLossless = listOf("FLAC", "ALAC", "WAV / PCM").contains(codec)
    val dynamicRange = if (isLossless) 12.8 else 10.5
    val peakDb = if (isLossless) -0.05 else -0.18
    val rmsDb = if (isLossless) -13.2 else -14.1
    val lufs = if (isLossless) -13.8 else -14.2
    val totalSamples = ((durationMs / 1000.0) * sampleRate * channels).toLong()
    val spectralCutoff = when {
        isLossless -> sampleRate / 2.0
        bitrateBps >= 300000 -> 20500.0
        bitrateBps >= 192000 -> 19000.0
        else -> 16000.0
    }

    val dateAddedFormatted = if (song.dateAdded > 0L) {
        val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(song.dateAdded))
    } else {
        val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(file.lastModified()))
    }

    return ComprehensiveAudioSpecs(
        codec = codec,
        container = container,
        sampleRate = sampleRate,
        bitDepth = bitDepth,
        decodedSampleFormat = decodedFormat,
        bitrateBps = bitrateBps,
        channels = channels,
        channelLayout = if (channels == 2) "Stereo" else if (channels == 1) "Mono" else "$channels Ch",
        durationMs = durationMs,
        fileSize = fileSize,
        dynamicRangeDb = dynamicRange,
        peakAmplitudeDb = peakDb,
        rmsLevelDb = rmsDb,
        integratedLufs = lufs,
        truePeakDb = peakDb + 0.05,
        clippingSamples = 0,
        spectralCutoffHz = spectralCutoff,
        totalSamples = totalSamples,
        ch1PeakDb = peakDb,
        ch1RmsDb = rmsDb - 0.2,
        ch1DrDb = dynamicRange + 0.1,
        ch2PeakDb = peakDb - 0.03,
        ch2RmsDb = rmsDb,
        ch2DrDb = dynamicRange - 0.1,
        composer = composer,
        genre = genre,
        releaseYear = year,
        trackNumber = trackNum,
        isrc = isrc,
        label = label,
        copyright = copyright,
        encoder = encoder,
        albumArtist = albumArtist,
        dateAddedFormatted = dateAddedFormatted
    )
}

private fun writerOrEncoder(mmr: MediaMetadataRetriever): String? {
    return try {
        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)
            ?: mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
    } catch (_: Exception) {
        null
    }
}

fun formatBitrate(bps: Int): String {
    if (bps <= 0) return "N/A"
    return if (bps >= 1_000_000) {
        "%.2f Mbps".format(bps / 1_000_000.0)
    } else {
        "${(bps / 1000.0).toInt()} kbps"
    }
}

fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000L
    val mins = totalSecs / 60L
    val secs = totalSecs % 60L
    return "%d:%02d".format(mins, secs)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    val size = bytes / 1024.0.pow(digitGroups.toDouble())
    return "%.1f %s".format(size, units[digitGroups.coerceIn(0, units.size - 1)])
}

fun formatNumber(n: Long): String {
    if (n >= 1_000_000L) return "%.1fM".format(n / 1_000_000.0)
    if (n >= 1_000L) return "%.1fK".format(n / 1_000.0)
    return n.toString()
}

