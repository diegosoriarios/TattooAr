package com.diego.tattooar.domain.image

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.annotation.WorkerThread
import java.io.IOException
import kotlin.math.max

object BackgroundRemovalHelper {

    /**
     * Converts white/light background areas of a tattoo image into clean transparent pixels
     * using a smooth luminance thresholding algorithm with edge feathering.
     */
    @WorkerThread
    fun removeWhiteBackground(
        source: Bitmap,
        threshold: Int = 210,
        featherRange: Int = 45
    ): Bitmap {
        val width = source.width
        val height = source.height
        val mutableBitmap = source.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)

        mutableBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val minFeather = (threshold - featherRange).coerceAtLeast(0)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val currentAlpha = (color shr 24) and 0xFF

            if (currentAlpha == 0) continue

            val luminance = (r * 299 + g * 587 + b * 114) / 1000

            when {
                luminance >= threshold -> {
                    // Fully transparent background
                    pixels[i] = 0
                }
                luminance > minFeather -> {
                    // Soft alpha transition for clean anti-aliased ink edges
                    val alphaFactor = 1.0f - ((luminance - minFeather).toFloat() / featherRange.toFloat())
                    val newAlpha = (currentAlpha * alphaFactor).toInt().coerceIn(0, 255)
                    pixels[i] = (newAlpha shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }

        mutableBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return mutableBitmap
    }

    /**
     * Loads a bundled CC0 sample tattoo from the app assets folder. Falls back to
     * procedurally generated art if the asset is missing or cannot be decoded.
     */
    @WorkerThread
    fun loadSampleBitmap(
        assets: AssetManager,
        assetPath: String,
        sampleId: String,
        inSampleSize: Int = 1
    ): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
            this.inSampleSize = inSampleSize.coerceAtLeast(1)
        }
        return try {
            assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: createSampleDesignBitmap(sampleId, 500)
        } catch (e: IOException) {
            createSampleDesignBitmap(sampleId, 500)
        }
    }

    /**
     * Generates a high-quality vector-rendered bitmap tattoo art for sample selection.
     */
    fun createSampleDesignBitmap(sampleId: String, size: Int = 500): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = size * 0.025f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val fillPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#121212")
            style = Paint.Style.FILL
        }

        val center = size / 2f
        val radius = size * 0.38f

        when (sampleId) {
            "dragon_1" -> {
                // Tribal Dragon curves
                val path = Path().apply {
                    moveTo(center, center - radius * 0.8f)
                    cubicTo(center + radius, center - radius, center + radius * 0.5f, center + radius * 0.5f, center, center + radius * 0.8f)
                    cubicTo(center - radius * 0.5f, center + radius * 0.5f, center - radius, center - radius, center, center - radius * 0.8f)
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, paint)

                // Outer tribal spikes
                for (i in 0 until 8) {
                    val angle = Math.toRadians(i * 45.0)
                    val sx = center + (radius * 0.7f * kotlin.math.cos(angle)).toFloat()
                    val sy = center + (radius * 0.7f * kotlin.math.sin(angle)).toFloat()
                    val ex = center + (radius * 1.1f * kotlin.math.cos(angle)).toFloat()
                    val ey = center + (radius * 1.1f * kotlin.math.sin(angle)).toFloat()
                    canvas.drawLine(sx, sy, ex, ey, paint)
                }
            }
            "rose_1" -> {
                // Traditional Rose petals
                canvas.drawCircle(center, center, radius * 0.4f, paint)
                canvas.drawCircle(center - radius * 0.2f, center, radius * 0.35f, paint)
                canvas.drawCircle(center + radius * 0.2f, center, radius * 0.35f, paint)
                canvas.drawCircle(center, center - radius * 0.2f, radius * 0.35f, paint)

                // Stem & Leaves
                val leafPath = Path().apply {
                    moveTo(center, center + radius * 0.4f)
                    quadTo(center + radius * 0.5f, center + radius * 0.6f, center + radius * 0.8f, center + radius * 0.4f)
                    quadTo(center + radius * 0.5f, center + radius * 0.8f, center, center + radius * 0.9f)
                }
                canvas.drawPath(leafPath, fillPaint)
                canvas.drawPath(leafPath, paint)
            }
            "compass_1" -> {
                // Geometric Compass
                canvas.drawCircle(center, center, radius, paint)
                canvas.drawCircle(center, center, radius * 0.85f, paint)

                // North/South/East/West Star
                val starPath = Path().apply {
                    moveTo(center, center - radius)
                    lineTo(center + radius * 0.2f, center - radius * 0.2f)
                    lineTo(center + radius, center)
                    lineTo(center + radius * 0.2f, center + radius * 0.2f)
                    lineTo(center, center + radius)
                    lineTo(center - radius * 0.2f, center + radius * 0.2f)
                    lineTo(center - radius, center)
                    lineTo(center - radius * 0.2f, center - radius * 0.2f)
                    close()
                }
                canvas.drawPath(starPath, fillPaint)
                canvas.drawPath(starPath, paint)
            }
            "skull_1" -> {
                // Gothic Skull silhouette
                val skullPath = Path().apply {
                    addOval(RectF(center - radius * 0.6f, center - radius * 0.7f, center + radius * 0.6f, center + radius * 0.3f), Path.Direction.CW)
                    addRect(RectF(center - radius * 0.35f, center + radius * 0.2f, center + radius * 0.35f, center + radius * 0.7f), Path.Direction.CW)
                }
                canvas.drawPath(skullPath, paint)
                // Eye sockets
                canvas.drawCircle(center - radius * 0.25f, center - radius * 0.15f, radius * 0.18f, fillPaint)
                canvas.drawCircle(center + radius * 0.25f, center - radius * 0.15f, radius * 0.18f, fillPaint)
            }
            else -> {
                // Crescent Moon & Stars
                val moonPath = Path().apply {
                    addCircle(center, center, radius * 0.75f, Path.Direction.CW)
                    op(Path().apply { addCircle(center + radius * 0.35f, center - radius * 0.2f, radius * 0.7f, Path.Direction.CW) }, Path.Op.DIFFERENCE)
                }
                canvas.drawPath(moonPath, fillPaint)
                canvas.drawPath(moonPath, paint)
            }
        }

        return bitmap
    }
}
