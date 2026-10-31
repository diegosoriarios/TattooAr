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
 */
@OptIn(ExperimentalGetImage::class)
fun ImageProxy.toUprightBitmap(): Bitmap {
    val source = toBitmap()
    val rotationDegrees = imageInfo.rotationDegrees
    if (rotationDegrees == 0) return source

    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    if (rotated !== source) {
        source.recycle()
    }
    return rotated
}
