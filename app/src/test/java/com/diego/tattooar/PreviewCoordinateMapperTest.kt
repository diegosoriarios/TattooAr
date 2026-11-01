package com.diego.tattooar

import com.diego.tattooar.domain.tracking.PreviewCoordinateMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewCoordinateMapperTest {

    @Test
    fun centerOfImageMapsToCenterOfView() {
        val imageWidth = 480
        val imageHeight = 640
        val viewWidth = 1080f
        val viewHeight = 2400f
        val mapping = PreviewCoordinateMapper.fillCenterMapping(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight
        )

        val (x, y) = PreviewCoordinateMapper.toViewCenter(
            normalizedX = 0.5f,
            normalizedY = 0.5f,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            mapping = mapping
        )

        assertEquals(0f, x, 0.001f)
        assertEquals(0f, y, 0.001f)
    }

    @Test
    fun croppedImageEdgeMapsToViewEdge() {
        val imageWidth = 480
        val imageHeight = 640
        val viewWidth = 1080f
        val viewHeight = 2400f
        val mapping = PreviewCoordinateMapper.fillCenterMapping(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight
        )

        // The image is wider than the view after FILL_CENTER, so the visible right edge is
        // at normalized x = 0.8 rather than 1.0.
        val (rightX, _) = PreviewCoordinateMapper.toViewCenter(
            normalizedX = 0.8f,
            normalizedY = 0.5f,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            mapping = mapping
        )
        val (_, topY) = PreviewCoordinateMapper.toViewCenter(
            normalizedX = 0.5f,
            normalizedY = 0f,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            mapping = mapping
        )

        assertEquals(viewWidth / 2f, rightX, 0.001f)
        assertEquals(-viewHeight / 2f, topY, 0.001f)
    }

    @Test
    fun matchingAspectRatioProducesNoCropOffset() {
        val imageWidth = 480
        val imageHeight = 640
        val viewWidth = 1080f
        val viewHeight = 1440f
        val mapping = PreviewCoordinateMapper.fillCenterMapping(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight
        )

        assertEquals(0f, mapping.offsetX, 0.001f)
        assertEquals(0f, mapping.offsetY, 0.001f)

        val (rightX, bottomY) = PreviewCoordinateMapper.toViewCenter(
            normalizedX = 1f,
            normalizedY = 1f,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            mapping = mapping
        )

        assertEquals(viewWidth / 2f, rightX, 0.001f)
        assertEquals(viewHeight / 2f, bottomY, 0.001f)
    }

    @Test
    fun invalidDimensionsFallBackToIdentityMapping() {
        val mapping = PreviewCoordinateMapper.fillCenterMapping(
            imageWidth = 0,
            imageHeight = 0,
            viewWidth = 0f,
            viewHeight = 0f
        )

        assertEquals(1f, mapping.scale, 0.001f)
        assertEquals(0f, mapping.offsetX, 0.001f)
        assertEquals(0f, mapping.offsetY, 0.001f)
    }
}
