package com.diego.tattooar.domain.tracking

import kotlin.math.PI
import kotlin.math.abs

/**
 * 1€ (One Euro) Filter for smooth, low-latency noise reduction and jitter suppression
 * on 2D tracking coordinates (x, y, scale, rotation).
 */
class OneEuroFilter(
    private val minCutoff: Float = 1.0f,
    private val beta: Float = 0.007f,
    private val dCutoff: Float = 1.0f
) {
    private var xPrev: Float? = null
    private var dxPrev: Float = 0.0f
    private var tPrev: Long? = null

    fun filter(value: Float, timestampMs: Long): Float {
        val lastT = tPrev
        val lastX = xPrev

        if (lastT == null || lastX == null) {
            xPrev = value
            dxPrev = 0.0f
            tPrev = timestampMs
            return value
        }

        val dt = (timestampMs - lastT) / 1000.0f
        if (dt <= 0.0f) return lastX

        // Estimate derivative (velocity)
        val dx = (value - lastX) / dt
        val dxHat = alpha(dt, dCutoff) * dx + (1.0f - alpha(dt, dCutoff)) * dxPrev

        // Dynamic cutoff frequency
        val cutoff = minCutoff + beta * abs(dxHat)
        val a = alpha(dt, cutoff)

        // Filtered value
        val xHat = a * value + (1.0f - a) * lastX

        xPrev = xHat
        dxPrev = dxHat
        tPrev = timestampMs

        return xHat
    }

    private fun alpha(dt: Float, cutoff: Float): Float {
        val tau = 1.0f / (2.0f * PI.toFloat() * cutoff)
        return 1.0f / (1.0f + tau / dt)
    }

    fun reset() {
        xPrev = null
        dxPrev = 0.0f
        tPrev = null
    }
}

/**
 * 2D Point Filter using two OneEuroFilters for smooth (X, Y) tracking
 */
class OneEuroFilter2D(
    minCutoff: Float = 1.0f,
    beta: Float = 0.007f,
    dCutoff: Float = 1.0f
) {
    private val filterX = OneEuroFilter(minCutoff, beta, dCutoff)
    private val filterY = OneEuroFilter(minCutoff, beta, dCutoff)

    fun filter(x: Float, y: Float, timestampMs: Long): Pair<Float, Float> {
        val smoothX = filterX.filter(x, timestampMs)
        val smoothY = filterY.filter(y, timestampMs)
        return Pair(smoothX, smoothY)
    }

    fun reset() {
        filterX.reset()
        filterY.reset()
    }
}
