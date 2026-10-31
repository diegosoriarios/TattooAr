package com.diego.tattooar.domain.tracking

import kotlin.math.max

/**
 * Describes how an analysis image is scaled and offset when displayed by a
 * `PreviewView` using `ScaleType.FILL_CENTER`.
 */
data class PreviewMapping(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)

/**
 * Maps normalized MediaPipe landmarks (in upright analysis-image space) to the
 * screen coordinates used by the Compose overlay canvas.
 */
object PreviewCoordinateMapper {

    fun fillCenterMapping(
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float
    ): PreviewMapping {
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0f || viewHeight <= 0f) {
            return PreviewMapping(scale = 1f, offsetX = 0f, offsetY = 0f)
        }

        val scale = max(viewWidth / imageWidth, viewHeight / imageHeight)
        val offsetX = (viewWidth - imageWidth * scale) / 2f
        val offsetY = (viewHeight - imageHeight * scale) / 2f
        return PreviewMapping(scale = scale, offsetX = offsetX, offsetY = offsetY)
    }

    /**
     * Returns the point in view pixels relative to the center of the view, which is the
     * coordinate space expected by [com.diego.tattooar.ui.render.TattooTransformState].
     */
    fun toViewCenter(
        normalizedX: Float,
        normalizedY: Float,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        mapping: PreviewMapping
    ): Pair<Float, Float> {
        val screenX = normalizedX * imageWidth * mapping.scale + mapping.offsetX
        val screenY = normalizedY * imageHeight * mapping.scale + mapping.offsetY
        return Pair(screenX - viewWidth / 2f, screenY - viewHeight / 2f)
    }
}
