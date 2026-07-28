package com.example.looper_android.ui.player

import android.media.MediaExtractor
import android.media.MediaFormat
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

    fun analyze(path: String?): AudioAnalysis? {
        if (path.isNullOrEmpty()) return null
        cache[path]?.let { return it }

        return try {
            val file = File(path)
            if (!file.exists()) return getFallbackAnalysis(path)

            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)

            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime != null && mime.startsWith("audio/")) {
                    audioFormat = format
                    break
                }
            }
            extractor.release()

            if (audioFormat == null) return getFallbackAnalysis(path)

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
            val codec = parseCodecName(mime, path)
            val sampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val bitrate = if (audioFormat.containsKey(MediaFormat.KEY_BIT_RATE)) audioFormat.getInteger(MediaFormat.KEY_BIT_RATE) else 0
            val channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2

            val analysis = AudioAnalysis(
                codec = codec,
                sampleRate = sampleRate,
                bitrate = bitrate,
                bitsPerSample = 0,
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
            else -> path.substringAfterLast('.').uppercase()
        }
    }

    private fun getFallbackAnalysis(path: String): AudioAnalysis {
        val ext = path.substringAfterLast('.').uppercase()
        val isLossless = listOf("FLAC", "WAV", "ALAC", "APE").contains(ext)
        return AudioAnalysis(
            codec = if (ext.length in 3..4) ext else "AUDIO",
            sampleRate = 44100,
            bitrate = if (isLossless) 1411000 else 320000,
            bitsPerSample = 0,
            channelCount = 2
        )
    }

    fun getQualityString(path: String?): String {
        if (path == null) return "High Quality • Audio"
        val analysis = cache[path] ?: analyze(path) ?: getFallbackAnalysis(path)

        val codecStr = analysis.codec.uppercase()
        val isLossless = listOf("FLAC", "WAV", "ALAC", "APE").contains(codecStr)
        val qualityPrefix = if (isLossless) "Lossless" else if (analysis.bitrate in 1..191999) "Standard Quality" else "High Quality"

        val sampleRateStr = "%.1f kHz".format(analysis.sampleRate / 1000.0)
        val parts = mutableListOf<String>()
        parts.add(qualityPrefix)
        parts.add(codecStr)
        parts.add(sampleRateStr)
        if (!isLossless && analysis.bitrate > 0) {
            parts.add("${(analysis.bitrate / 1000.0).toInt()} kbps")
        }
        return parts.joinToString(" • ")
    }
}
