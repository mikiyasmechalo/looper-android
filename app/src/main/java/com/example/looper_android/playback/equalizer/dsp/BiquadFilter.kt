package com.example.looper_android.playback.equalizer.dsp

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class BiquadType {
    PEAKING,
    LOW_SHELF,
    HIGH_SHELF
}

class BiquadFilter(
    val type: BiquadType = BiquadType.PEAKING,
    var centerFreqHz: Float = 1000f,
    var qFactor: Float = 1.414f
) {
    private var b0: Double = 1.0
    private var b1: Double = 0.0
    private var b2: Double = 0.0
    private var a1: Double = 0.0
    private var a2: Double = 0.0

    // Direct Form II Transposed state for Left and Right stereo channels
    private var d1Left: Double = 0.0
    private var d2Left: Double = 0.0
    private var d1Right: Double = 0.0
    private var d2Right: Double = 0.0

    private var currentSampleRate: Int = 44100
    private var currentGainDb: Float = 0f
    var isBypassed: Boolean = true
        private set

    fun updateCoefficients(sampleRate: Int, gainDb: Float) {
        currentSampleRate = sampleRate.coerceAtLeast(8000)
        currentGainDb = gainDb

        if (gainDb in -0.05f..0.05f) {
            isBypassed = true
            b0 = 1.0
            b1 = 0.0
            b2 = 0.0
            a1 = 0.0
            a2 = 0.0
            return
        }

        isBypassed = false
        val clampedFreq = centerFreqHz.coerceIn(10f, (currentSampleRate / 2f) - 100f)
        val omega = 2.0 * Math.PI * clampedFreq / currentSampleRate
        val sn = sin(omega)
        val cs = cos(omega)
        val q = qFactor.coerceAtLeast(0.1f).toDouble()
        val alpha = sn / (2.0 * q)
        val aVal = 10.0.pow(gainDb.toDouble() / 40.0) // sqrt(A) in linear power

        var rawB0 = 1.0
        var rawB1 = 0.0
        var rawB2 = 0.0
        var rawA0 = 1.0
        var rawA1 = 0.0
        var rawA2 = 0.0

        when (type) {
            BiquadType.PEAKING -> {
                rawB0 = 1.0 + alpha * aVal
                rawB1 = -2.0 * cs
                rawB2 = 1.0 - alpha * aVal
                rawA0 = 1.0 + alpha / aVal
                rawA1 = -2.0 * cs
                rawA2 = 1.0 - alpha / aVal
            }
            BiquadType.LOW_SHELF -> {
                val sqrtA = sqrt(aVal)
                rawB0 = aVal * ((aVal + 1.0) - (aVal - 1.0) * cs + 2.0 * sqrtA * alpha)
                rawB1 = 2.0 * aVal * ((aVal - 1.0) - (aVal + 1.0) * cs)
                rawB2 = aVal * ((aVal + 1.0) - (aVal - 1.0) * cs - 2.0 * sqrtA * alpha)
                rawA0 = (aVal + 1.0) + (aVal - 1.0) * cs + 2.0 * sqrtA * alpha
                rawA1 = -2.0 * ((aVal - 1.0) + (aVal + 1.0) * cs)
                rawA2 = (aVal + 1.0) + (aVal - 1.0) * cs - 2.0 * sqrtA * alpha
            }
            BiquadType.HIGH_SHELF -> {
                val sqrtA = sqrt(aVal)
                rawB0 = aVal * ((aVal + 1.0) + (aVal - 1.0) * cs + 2.0 * sqrtA * alpha)
                rawB1 = -2.0 * aVal * ((aVal - 1.0) + (aVal + 1.0) * cs)
                rawB2 = aVal * ((aVal + 1.0) + (aVal - 1.0) * cs - 2.0 * sqrtA * alpha)
                rawA0 = (aVal + 1.0) - (aVal - 1.0) * cs + 2.0 * sqrtA * alpha
                rawA1 = 2.0 * ((aVal - 1.0) - (aVal + 1.0) * cs)
                rawA2 = (aVal + 1.0) - (aVal - 1.0) * cs - 2.0 * sqrtA * alpha
            }
        }

        b0 = rawB0 / rawA0
        b1 = rawB1 / rawA0
        b2 = rawB2 / rawA0
        a1 = rawA1 / rawA0
        a2 = rawA2 / rawA0
    }

    fun processLeft(sample: Float): Float {
        if (isBypassed) return sample
        val x = sample.toDouble()
        val y = b0 * x + d1Left
        d1Left = b1 * x - a1 * y + d2Left
        d2Left = b2 * x - a2 * y
        return y.toFloat()
    }

    fun processRight(sample: Float): Float {
        if (isBypassed) return sample
        val x = sample.toDouble()
        val y = b0 * x + d1Right
        d1Right = b1 * x - a1 * y + d2Right
        d2Right = b2 * x - a2 * y
        return y.toFloat()
    }

    fun resetState() {
        d1Left = 0.0
        d2Left = 0.0
        d1Right = 0.0
        d2Right = 0.0
    }
}
