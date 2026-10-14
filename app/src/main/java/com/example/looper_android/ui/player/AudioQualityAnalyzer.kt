package com.example.looper_android.ui.player

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class AudioAnalysis(
    val codec: String,
    val sampleRate: Int,
    val bitrate: Int,
    val bitsPerSample: Int,
    val channelCount: Int
)

object AudioQualityAnalyzer {
    private val cache = ConcurrentHashMap<String, AudioAnalysis>()

    fun getCachedAnalysis(path: String?): AudioAnalysis? {
        if (path == null) return null
        return cache[path]
    }

    fun analyze(path: String?, durationMs: Long? = null): AudioAnalysis? {
        if (path.isNullOrEmpty()) return null
        cache[path]?.let { return it }

        return try {
            val file = File(path)
            val fileSize = if (file.exists()) file.length() else 0L

            var sampleRate = 44100
            var bitrate = 0
            var channelCount = 2
            var mimeType = ""
            var bitsPerSample = 0
            var dur = durationMs ?: 0L

            // 1. MediaExtractor
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(path)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime != null && mime.startsWith("audio/")) {
                        mimeType = mime
                        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE)
                        }
                        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        if (format.containsKey(MediaFormat.KEY_DURATION)) {
                            val d = format.getLong(MediaFormat.KEY_DURATION) / 1000L
                            if (d > 0) dur = d
                        }
                        break
                    }
                }
                extractor.release()
            } catch (_: Exception) {}

            // 2. MediaMetadataRetriever (very reliable for MP3/AAC/FLAC bitrate)
            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(path)
                if (bitrate <= 0) {
                    val br = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
                    if (br != null && br > 0) bitrate = br
                }
                if (sampleRate == 44100) {
                    val sr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
                    if (sr != null && sr > 0) sampleRate = sr
                }
                if (dur <= 0L) {
                    val d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    if (d != null && d > 0L) dur = d
                }
                if (mimeType.isBlank()) {
                    val m = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    if (!m.isNullOrBlank()) mimeType = m
                }
                mmr.release()
            } catch (_: Exception) {}

            // 3. Fallback bitrate calculation from file size & duration
            if (bitrate <= 0 && dur > 0 && fileSize > 0) {
                bitrate = ((fileSize * 8L) / (dur / 1000.0)).toInt()
            }

            val codec = parseCodecName(mimeType, path)

            val analysis = AudioAnalysis(
                codec = codec,
                sampleRate = sampleRate,
                bitrate = bitrate,
                bitsPerSample = bitsPerSample,
                channelCount = channelCount
            )
            cache[path] = analysis
            analysis
        } catch (e: Exception) {
            val fallback = getFallbackAnalysis(path)
            cache[path] = fallback
            fallback
        }
    }

    private fun parseCodecName(mime: String, path: String): String {
        return when {
            mime.contains("flac", ignoreCase = true) -> "FLAC"
            mime.contains("mp3", ignoreCase = true) || mime.contains("mpeg", ignoreCase = true) -> "MP3"
            mime.contains("aac", ignoreCase = true) || mime.contains("mp4a", ignoreCase = true) -> "AAC"
            mime.contains("wav", ignoreCase = true) || mime.contains("raw", ignoreCase = true) -> "WAV"
            mime.contains("alac", ignoreCase = true) -> "ALAC"
            mime.contains("vorbis", ignoreCase = true) || mime.contains("ogg", ignoreCase = true) -> "OGG"
            mime.contains("opus", ignoreCase = true) -> "OPUS"
            else -> path.substringAfterLast('.').uppercase()
        }
    }

    private fun getFallbackAnalysis(path: String): AudioAnalysis {
        val ext = path.substringAfterLast('.').uppercase()
        val isLossless = listOf("FLAC", "WAV", "ALAC", "APE", "DSD", "DSF", "DFF").contains(ext)
        return AudioAnalysis(
            codec = if (ext.length in 3..4) ext else "AUDIO",
            sampleRate = 44100,
            bitrate = if (isLossless) 1411000 else 128000,
            bitsPerSample = 0,
            channelCount = 2
        )
    }

    fun getQualityString(path: String?, durationMs: Long? = null): String {
        if (path.isNullOrEmpty()) return "Audio"
        val analysis = cache[path] ?: analyze(path, durationMs) ?: getFallbackAnalysis(path)

        val codecStr = analysis.codec.uppercase()
        val isLossless = listOf("FLAC", "WAV", "ALAC", "APE", "DSD", "DSF", "DFF").contains(codecStr)

        val qualityPrefix = when {
            isLossless -> {
                if (analysis.sampleRate > 48000 || analysis.bitsPerSample >= 24) "Hi-Res Lossless"
                else "Lossless"
            }
            analysis.bitrate >= 192000 -> "High Quality"
            analysis.bitrate in 1..191999 -> "Standard Quality"
            else -> "Audio"
        }

        val sampleRateStr = "%.1f kHz".format(analysis.sampleRate / 1000.0)
        val parts = mutableListOf<String>()
        parts.add(qualityPrefix)
        parts.add(codecStr)
        parts.add(sampleRateStr)
        if (!isLossless && analysis.bitrate > 0) {
            val kbps = (analysis.bitrate / 1000.0 + 0.5).toInt()
            parts.add("$kbps kbps")
        }
        return parts.joinToString(" • ")
    }
}
