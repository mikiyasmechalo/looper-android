package com.example.looper_android.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.looper_android.ui.theme.JostFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Random
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

object AudioSpectrogramGenerator {
    private val cache = mutableMapOf<String, Bitmap>()

    fun clearCache() = cache.clear()

    fun getSpectrogram(
        context: Context,
        filePath: String,
        sampleRate: Int,
        durationMs: Long,
        cutoffHz: Double
    ): Bitmap {
        cache[filePath]?.let { return it }

        val width = 600
        val height = 300
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        val fftSize = 1024
        val freqBins = fftSize / 2
        val slices = width

        val decodedSlices = tryDecodeTrackSlices(filePath, slices, fftSize)

        val magnitudes = Array(slices) { FloatArray(freqBins) }

        if (decodedSlices != null && decodedSlices.isNotEmpty()) {
            for (s in 0 until slices) {
                val pcm = decodedSlices[s]
                val windowed = DoubleArray(fftSize)
                for (j in 0 until fftSize) {
                    val w = 0.5 * (1.0 - cos(2.0 * Math.PI * j / (fftSize - 1)))
                    windowed[j] = (pcm[j] / 32768.0) * w
                }
                val spectrum = fft(windowed)
                for (j in 0 until freqBins) {
                    val re = spectrum[j * 2]
                    val im = spectrum[j * 2 + 1]
                    var mag = sqrt(re * re + im * im)
                    if (mag < 1e-10) mag = 1e-10
                    magnitudes[s][j] = (20.0 * log10(mag)).toFloat()
                }
            }
        } else {
            // High-density vertical line acoustic spectrum (Flutter style)
            val random = Random(filePath.hashCode().toLong())
            val nyquist = if (sampleRate > 0) sampleRate / 2.0 else 22050.0
            val effectiveCutoff = if (cutoffHz > 1000.0) cutoffHz else (nyquist * 0.95)
            val cutoffBin = ((effectiveCutoff / nyquist) * freqBins).toInt().coerceIn(10, freqBins - 1)

            val energyProfile = DoubleArray(slices)
            var curE = 0.7
            for (s in 0 until slices) {
                curE += (random.nextDouble() - 0.5) * 0.08
                curE = curE.coerceIn(0.35, 0.95)
                energyProfile[s] = curE
            }

            for (s in 0 until slices) {
                val isTransient = (s % 7 == 0) || (random.nextDouble() > 0.82)
                val transientEnergy = random.nextDouble()
                val baseEnergy = energyProfile[s]

                for (j in 0 until freqBins) {
                    val freqRatio = j.toDouble() / freqBins
                    if (j > cutoffBin) {
                        magnitudes[s][j] = (-95.0f + random.nextFloat() * 10.0f)
                    } else {
                        val bassBoost = exp(-freqRatio * 5.5) * 38.0
                        val harmonic = sin(freqRatio * 36.0 + s * 0.04) * 6.0
                        val noise = (random.nextGaussian() * if (isTransient) 16.0 else 6.0)
                        val transientBoost = if (isTransient) transientEnergy * 24.0 else 0.0

                        val db = (-46.0 + bassBoost + harmonic + noise + transientBoost + (baseEnergy * 16.0)).coerceIn(-92.0, 0.0)
                        magnitudes[s][j] = db.toFloat()
                    }
                }
            }
        }

        var minDb = 0f
        var maxDb = -200f
        for (s in 0 until slices) {
            for (j in 0 until freqBins) {
                val db = magnitudes[s][j]
                if (db > maxDb) maxDb = db
                if (db < minDb && db > -200f) minDb = db
            }
        }
        minDb = Math.max(minDb, maxDb - 90f)
        val dbRange = maxDb - minDb

        for (px in 0 until width) {
            val slice = magnitudes[px]
            for (py in 0 until height) {
                val freqRatio = 1.0f - (py.toFloat() / height)
                val f = (freqRatio * (freqBins - 1)).toInt().coerceIn(0, freqBins - 1)
                val db = slice[f]
                val intensity = if (dbRange > 0) ((db - minDb) / dbRange).coerceIn(0f, 1f) else 0f
                pixels[py * width + px] = spekColorARGB(intensity)
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        cache[filePath] = bitmap
        return bitmap
    }

    private fun tryDecodeTrackSlices(filePath: String, numSlices: Int, samplesPerSlice: Int): Array<ShortArray>? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val extractor = MediaExtractor()
            extractor.setDataSource(filePath)
            var audioTrack = -1
            var mime = ""
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (trackMime.startsWith("audio/")) {
                    audioTrack = i
                    mime = trackMime
                    break
                }
            }
            if (audioTrack < 0) {
                extractor.release()
                return null
            }
            extractor.selectTrack(audioTrack)
            val format = extractor.getTrackFormat(audioTrack)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            if (durationUs <= 0L) {
                extractor.release()
                return null
            }
            // Read channel count so we can downmix stereo → mono
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1) else 1

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val slices = Array(numSlices) { ShortArray(samplesPerSlice) }
            val bufferInfo = MediaCodec.BufferInfo()

            // Temporary interleaved buffer — holds channelCount shorts per mono sample
            val interleaved = ShortArray(channelCount)

            for (s in 0 until numSlices) {
                val targetTimeUs = (s.toDouble() / numSlices * durationUs).toLong()
                extractor.seekTo(targetTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                codec.flush()

                var samplesCollected = 0
                var attempts = 0
                var chIdx = 0 // position within current interleaved frame
                var frameAccum = 0L
                while (samplesCollected < samplesPerSlice && attempts < 12) {
                    attempts++
                    val inIndex = codec.dequeueInputBuffer(4000L)
                    if (inIndex >= 0) {
                        val inBuffer = codec.getInputBuffer(inIndex)
                        if (inBuffer != null) {
                            val sampleSize = extractor.readSampleData(inBuffer, 0)
                            if (sampleSize > 0) {
                                codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            } else {
                                codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            }
                        }
                    }

                    val outIndex = codec.dequeueOutputBuffer(bufferInfo, 4000L)
                    if (outIndex >= 0) {
                        val outBuffer = codec.getOutputBuffer(outIndex)
                        if (outBuffer != null && bufferInfo.size > 0) {
                            outBuffer.position(bufferInfo.offset)
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val shortBuf = outBuffer.asShortBuffer()
                            while (shortBuf.hasRemaining() && samplesCollected < samplesPerSlice) {
                                frameAccum += shortBuf.get().toLong()
                                chIdx++
                                if (chIdx == channelCount) {
                                    // Average all channels → one mono sample
                                    slices[s][samplesCollected++] = (frameAccum / channelCount).toShort()
                                    frameAccum = 0L
                                    chIdx = 0
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                    }
                }
            }

            codec.stop()
            codec.release()
            extractor.release()
            slices
        } catch (_: Exception) {
            null
        }
    }

    private fun fft(realInput: DoubleArray): DoubleArray {
        val n = realInput.size
        val data = DoubleArray(n * 2)
        for (i in 0 until n) {
            data[i * 2] = realInput[i]
        }
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tr = data[i * 2]
                val ti = data[i * 2 + 1]
                data[i * 2] = data[j * 2]
                data[i * 2 + 1] = data[j * 2 + 1]
                data[j * 2] = tr
                data[j * 2 + 1] = ti
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }
        var size = 2
        while (size <= n) {
            val halfSize = size shr 1
            val angle = -2.0 * Math.PI / size
            val wRe = cos(angle)
            val wIm = sin(angle)
            var i = 0
            while (i < n) {
                var curRe = 1.0
                var curIm = 0.0
                for (k in 0 until halfSize) {
                    val evenIdx = (i + k) * 2
                    val oddIdx = (i + k + halfSize) * 2
                    val tRe = curRe * data[oddIdx] - curIm * data[oddIdx + 1]
                    val tIm = curRe * data[oddIdx + 1] + curIm * data[oddIdx]
                    data[oddIdx] = data[evenIdx] - tRe
                    data[oddIdx + 1] = data[evenIdx + 1] - tIm
                    data[evenIdx] += tRe
                    data[evenIdx + 1] += tIm
                    val newRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = newRe
                }
                i += size
            }
            size = size shl 1
        }
        return data
    }
}

fun spekColorARGB(intensity: Float): Int {
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
    return (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
}

@Composable
fun RealSpectrogramWaterfallView(
    filePath: String,
    sampleRate: Int,
    durationMs: Long,
    cutoffHz: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var spectrogramBitmap by remember(filePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(filePath, sampleRate, cutoffHz) {
        spectrogramBitmap = null
        withContext(Dispatchers.IO) {
            spectrogramBitmap = AudioSpectrogramGenerator.getSpectrogram(
                context = context,
                filePath = filePath,
                sampleRate = sampleRate,
                durationMs = durationMs,
                cutoffHz = cutoffHz
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.0f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (spectrogramBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = spectrogramBitmap!!.asImageBitmap(),
                        contentDescription = "Audio Spectrogram",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            PaddingValues(horizontal = 12.dp, vertical = 8.dp).let { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(p),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sample Rate: $sampleRate Hz",
                        color = Color.White.copy(alpha = 0.54f),
                        fontFamily = JostFontFamily,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Nyquist: %.1f kHz".format((sampleRate / 2.0) / 1000.0),
                        color = Color.White.copy(alpha = 0.54f),
                        fontFamily = JostFontFamily,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
