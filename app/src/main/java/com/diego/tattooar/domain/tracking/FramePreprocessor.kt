package com.diego.tattooar.domain.tracking

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy

/**
 * Converts a CameraX [ImageProxy] into a bitmap oriented the way MediaPipe expects.
 *
 * CameraX analysis frames are delivered in sensor orientation; the rotation needed to make
 * them upright is exposed through [ImageProxy.getImageInfo]. MediaPipe does not apply that
 * rotation itself, so feeding the raw buffer produces sideways/upside-down limbs and degrades
 * detection significantly.
 *
 * [ImageProxy.toBitmap] ignores [ImageProxy.getCropRect] and converts the whole buffer, while
 * the preview only shows the crop rect. Cropping first keeps the normalized landmarks aligned
 * with the image the user actually sees.
 */
@OptIn(ExperimentalGetImage::class)
fun ImageProxy.toUprightBitmap(): Bitmap {
    val source = toBitmap()
    val crop = cropRect
    val cropWidth = crop.width().coerceIn(1, source.width)
    val cropHeight = crop.height().coerceIn(1, source.height)
    val cropLeft = crop.left.coerceIn(0, source.width - cropWidth)
    val cropTop = crop.top.coerceIn(0, source.height - cropHeight)

    val cropped = if (cropWidth != source.width || cropHeight != source.height ||
        cropLeft != 0 || cropTop != 0
    ) {
        Bitmap.createBitmap(source, cropLeft, cropTop, cropWidth, cropHeight)
    } else {
        source
    }
    if (cropped !== source) {
        source.recycle()
    }

    val rotationDegrees = imageInfo.rotationDegrees
    if (rotationDegrees == 0) return cropped

    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
    if (rotated !== cropped) {
        cropped.recycle()
    }
    return rotated
}
