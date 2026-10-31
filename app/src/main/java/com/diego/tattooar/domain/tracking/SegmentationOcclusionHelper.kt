package com.diego.tattooar.domain.tracking

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SegmentationOcclusionHelper(
    private val context: Context,
    private val onMaskGenerated: (Bitmap) -> Unit,
    private val onError: (String) -> Unit = {}
) {
    private var imageSegmenter: ImageSegmenter? = null
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        setupSegmenter()
    }

    private fun setupSegmenter() {
        backgroundExecutor.execute {
            try {
                val baseOptionsBuilder = BaseOptions.builder()
                try {
                    baseOptionsBuilder.setModelAssetPath("selfie_segmenter.task")
                } catch (_: Exception) {}

                val baseOptions = baseOptionsBuilder.build()
                val options = ImageSegmenter.ImageSegmenterOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setOutputConfidenceMasks(true)
                    .setResultListener { result: ImageSegmenterResult, _: MPImage ->
                        val confidenceMasksOpt = result.confidenceMasks()
                        if (confidenceMasksOpt.isPresent) {
                            val maskList = confidenceMasksOpt.get()
                            if (maskList.isNotEmpty()) {
                                val maskImage = maskList[0]
                                val maskBitmap = Bitmap.createBitmap(maskImage.width, maskImage.height, Bitmap.Config.ARGB_8888)
                                onMaskGenerated(maskBitmap)
                            }
                        }
                    }
                    .setErrorListener { error ->
                        onError(error.message ?: "ImageSegmenter error")
                    }
                    .build()

                imageSegmenter = ImageSegmenter.createFromOptions(context, options)
            } catch (e: Exception) {
                onError("MediaPipe ImageSegmenter initialization: ${e.localizedMessage}")
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun detectAsync(imageProxy: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis()
        val bitmap = imageProxy.toBitmap()
        imageProxy.close()

        backgroundExecutor.execute {
            val segmenter = imageSegmenter
            if (segmenter != null) {
                try {
                    val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
                    segmenter.segmentAsync(mpImage, frameTime)
                } catch (_: Exception) {
                    onMaskGenerated(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                }
            }
        }
    }

    fun close() {
        backgroundExecutor.execute {
            imageSegmenter?.close()
            imageSegmenter = null
        }
        backgroundExecutor.shutdown()
    }
}
