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
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HandLandmarkerHelper(
    private val context: Context,
    private val onResult: (HandLandmarkerResult?, Bitmap) -> Unit,
    private val onError: (String) -> Unit = {}
) {
    private var handLandmarker: HandLandmarker? = null
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val bitmapQueue = ConcurrentLinkedQueue<Bitmap>()

    init {
        setupHandLandmarker()
    }

    private fun setupHandLandmarker() {
        backgroundExecutor.execute {
            try {
                val baseOptionsBuilder = BaseOptions.builder()
                
                try {
                    baseOptionsBuilder.setModelAssetPath("hand_landmarker.task")
                } catch (_: Exception) {
                }

                val baseOptions = baseOptionsBuilder.build()
                val options = HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinHandDetectionConfidence(0.5f)
                    .setMinHandPresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setNumHands(2)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener { result, _ ->
                        val bitmap = bitmapQueue.poll() ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                        onResult(result, bitmap)
                    }
                    .setErrorListener { error ->
                        onError(error.message ?: "HandLandmarker error")
                    }
                    .build()

                handLandmarker = HandLandmarker.createFromOptions(context, options)
            } catch (t: Throwable) {
                onError("MediaPipe HandLandmarker initialization: ${t.localizedMessage}")
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun detectAsync(imageProxy: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis()
        val bitmap = imageProxy.toBitmap()
        imageProxy.close()

        bitmapQueue.add(bitmap)

        backgroundExecutor.execute {
            val landmarker = handLandmarker
            if (landmarker != null) {
                try {
                    val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
                    landmarker.detectAsync(mpImage, frameTime)
                } catch (_: Exception) {
                    val removedBitmap = bitmapQueue.poll() ?: bitmap
                    onResult(null, removedBitmap)
                }
            } else {
                val removedBitmap = bitmapQueue.poll() ?: bitmap
                onResult(null, removedBitmap)
            }
        }
    }

    fun close() {
        backgroundExecutor.execute {
            handLandmarker?.close()
            handLandmarker = null
            bitmapQueue.clear()
        }
        backgroundExecutor.shutdown()
    }
}
