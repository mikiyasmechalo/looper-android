package com.example.looper_android.playback.equalizer.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.tanh

class CustomEqualizerAudioProcessor : BaseAudioProcessor() {

    companion object {
        val BAND_FREQUENCIES_HZ = floatArrayOf(
            31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
        )
    }

    private val filters = Array(BAND_FREQUENCIES_HZ.size) { i ->
        val type = when (i) {
            0 -> BiquadType.LOW_SHELF
            BAND_FREQUENCIES_HZ.size - 1 -> BiquadType.HIGH_SHELF
            else -> BiquadType.PEAKING
        }
        BiquadFilter(type = type, centerFreqHz = BAND_FREQUENCIES_HZ[i], qFactor = 1.414f)
    }

    private val bassBoostFilter = BiquadFilter(type = BiquadType.LOW_SHELF, centerFreqHz = 80f, qFactor = 1.2f)

    @Volatile
    var isMasterEnabled: Boolean = false
        private set

    private var targetGainsDb = FloatArray(BAND_FREQUENCIES_HZ.size) { 0f }
    private var targetPreampGainDb: Float = 0f
    private var targetBassBoostPercent: Int = 0
    private var targetVirtualizerPercent: Int = 0

    private var preampMultiplier: Float = 1.0f
    private var stereoWidthMultiplier: Float = 1.0f

    private var activeSampleRate: Int = 44100

    fun updateParameters(
        enabled: Boolean,
        bandGains: List<Float>,
        preampDb: Float,
        bassBoostPercent: Int,
        virtualizerPercent: Int
    ) {
        isMasterEnabled = enabled
        targetPreampGainDb = preampDb.coerceIn(-15f, 15f)
        targetBassBoostPercent = bassBoostPercent.coerceIn(0, 100)
        targetVirtualizerPercent = virtualizerPercent.coerceIn(0, 100)

        preampMultiplier = 10f.pow(targetPreampGainDb / 20f)
        // Stereo widening: 0% = normal (1.0x Side), 100% = expanded (2.2x Side)
        stereoWidthMultiplier = 1.0f + (targetVirtualizerPercent / 100f) * 1.2f

        for (i in filters.indices) {
            if (i < bandGains.size) {
                targetGainsDb[i] = bandGains[i].coerceIn(-15f, 15f)
            }
        }

        recomputeCoefficients()
    }

    private fun recomputeCoefficients() {
        val sampleRate = activeSampleRate
        for (i in filters.indices) {
            filters[i].updateCoefficients(sampleRate, if (isMasterEnabled) targetGainsDb[i] else 0f)
        }

        val bassGainDb = if (isMasterEnabled) (targetBassBoostPercent / 100f) * 10f else 0f
        bassBoostFilter.updateCoefficients(sampleRate, bassGainDb)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        activeSampleRate = inputAudioFormat.sampleRate
        recomputeCoefficients()

        // We output 16-bit PCM for consistent AudioTrack playback
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_16BIT
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val channelCount = inputAudioFormat.channelCount
        val is16Bit = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT
        val bytesPerSample = if (is16Bit) 2 else 4
        val frameCount = inputBuffer.remaining() / (bytesPerSample * channelCount)

        if (frameCount == 0) return

        val outputByteCount = frameCount * 2 * channelCount // 16-bit PCM output
        val outputBuffer = replaceOutputBuffer(outputByteCount).order(ByteOrder.LITTLE_ENDIAN)

        val enabled = isMasterEnabled
        val pMultiplier = preampMultiplier
        val widthMult = stereoWidthMultiplier
        val hasVirtualizer = enabled && targetVirtualizerPercent > 0 && channelCount >= 2

        if (channelCount == 1) {
            // Mono stream processing
            for (f in 0 until frameCount) {
                var s = if (is16Bit) {
                    inputBuffer.short.toFloat() / 32768f
                } else {
                    inputBuffer.float
                }

                if (enabled) {
                    s *= pMultiplier
                    for (i in filters.indices) {
                        s = filters[i].processLeft(s)
                    }
                    s = bassBoostFilter.processLeft(s)
                    s = softLimit(s)
                }

                val outShort = (s.coerceIn(-1.0f, 1.0f) * 32767f).toInt().toShort()
                outputBuffer.putShort(outShort)
            }
        } else {
            // Stereo stream processing
            for (f in 0 until frameCount) {
                var left = if (is16Bit) {
                    inputBuffer.short.toFloat() / 32768f
                } else {
                    inputBuffer.float
                }
                var right = if (is16Bit) {
                    inputBuffer.short.toFloat() / 32768f
                } else {
                    inputBuffer.float
                }

                if (enabled) {
                    left *= pMultiplier
                    right *= pMultiplier

                    // 1. 10-Band Biquad Cascade
                    for (i in filters.indices) {
                        left = filters[i].processLeft(left)
                        right = filters[i].processRight(right)
                    }

                    // 2. Bass Boost Filter
                    left = bassBoostFilter.processLeft(left)
                    right = bassBoostFilter.processRight(right)

                    // 3. Stereo Virtualizer Matrix (Mid-Side Widening)
                    if (hasVirtualizer) {
                        val mid = (left + right) * 0.5f
                        val side = (left - right) * 0.5f * widthMult
                        left = mid + side
                        right = mid - side
                    }

                    // 4. Soft Saturation / Limiter
                    left = softLimit(left)
                    right = softLimit(right)
                }

                val outLeft = (left.coerceIn(-1.0f, 1.0f) * 32767f).toInt().toShort()
                val outRight = (right.coerceIn(-1.0f, 1.0f) * 32767f).toInt().toShort()

                outputBuffer.putShort(outLeft)
                outputBuffer.putShort(outRight)
            }
        }

        outputBuffer.flip()
    }

    private fun softLimit(x: Float): Float {
        val absX = kotlin.math.abs(x)
        return if (absX <= 0.85f) {
            x
        } else {
            val sign = if (x >= 0f) 1f else -1f
            sign * (0.85f + 0.15f * tanh(((absX - 0.85f) / 0.15f).toDouble()).toFloat())
        }
    }

    override fun onFlush() {
        for (f in filters) f.resetState()
        bassBoostFilter.resetState()
    }

    override fun onReset() {
        for (f in filters) f.resetState()
        bassBoostFilter.resetState()
    }
}
