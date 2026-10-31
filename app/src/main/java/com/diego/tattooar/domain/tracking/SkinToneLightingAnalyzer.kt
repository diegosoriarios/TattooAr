package com.diego.tattooar.domain.tracking

import android.graphics.Bitmap
import androidx.annotation.WorkerThread

data class SkinLightingMetrics(
    val luminance: Float = 0.5f,     // 0.0 (dark ambient) to 1.0 (bright ambient)
    val redFactor: Float = 1.0f,     // Skin warmth
    val greenFactor: Float = 1.0f,
    val blueFactor: Float = 1.0f
)

object SkinToneLightingAnalyzer {

    /**
     * Samples pixel colors around the detected wrist/forearm region
     * to estimate real-time ambient skin tone and light exposure.
     */
    @WorkerThread
    fun analyzeSkinLighting(
        bitmap: Bitmap,
        sampleXNormalized: Float = 0.5f,
        sampleYNormalized: Float = 0.5f,
        sampleRadiusPixels: Int = 30
    ): SkinLightingMetrics {
        val width = bitmap.width
        val height = bitmap.height

        val centerX = (sampleXNormalized * width).toInt().coerceIn(0, width - 1)
        val centerY = (sampleYNormalized * height).toInt().coerceIn(0, height - 1)

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var sampleCount = 0

        val minX = (centerX - sampleRadiusPixels).coerceAtLeast(0)
        val maxX = (centerX + sampleRadiusPixels).coerceAtMost(width - 1)
        val minY = (centerY - sampleRadiusPixels).coerceAtLeast(0)
        val maxY = (centerY + sampleRadiusPixels).coerceAtMost(height - 1)

        val step = 4 // Step size for efficient background sampling

        for (y in minY..maxY step step) {
            for (x in minX..maxX step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                totalR += r
                totalG += g
                totalB += b
                sampleCount++
            }
        }

        if (sampleCount == 0) return SkinLightingMetrics()

        val avgR = (totalR / sampleCount) / 255.0f
        val avgG = (totalG / sampleCount) / 255.0f
        val avgB = (totalB / sampleCount) / 255.0f

        val luminance = (0.299f * avgR + 0.587f * avgG + 0.114f * avgB).coerceIn(0.1f, 1.0f)

        return SkinLightingMetrics(
            luminance = luminance,
            redFactor = avgR,
            greenFactor = avgG,
            blueFactor = avgB
        )
    }
}
