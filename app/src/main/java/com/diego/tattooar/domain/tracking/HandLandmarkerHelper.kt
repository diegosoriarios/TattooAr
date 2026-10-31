package com.diego.tattooar.domain.tracking

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HandLandmarkerHelper(
    private val context: Context,
    private val onResult: (HandLandmarkerResult?, Bitmap) -> Unit,
    private val onError: (String) -> Unit = {},
    private val onReady: () -> Unit = {}
) {
    @Volatile
    private var handLandmarker: HandLandmarker? = null

    private val setupExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        setupHandLandmarker()
    }

    private fun setupHandLandmarker() {
        setupExecutor.execute {
            try {
                val options = HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(
                        BaseOptions.builder()
                            .setModelAssetPath(MODEL_ASSET)
                            .build()
                    )
                    .setMinHandDetectionConfidence(0.5f)
                    .setMinHandPresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setNumHands(2)
                    .setRunningMode(RunningMode.VIDEO)
                    .setErrorListener { error ->
                        onError("Hand tracking error: ${error.message ?: "unknown"}")
                    }
                    .build()

                handLandmarker = HandLandmarker.createFromOptions(context, options)
                Log.i(TAG, "HandLandmarker ready")
                onReady()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to initialize HandLandmarker", t)
                onError("Hand tracking unavailable: ${t.message ?: t.javaClass.simpleName}")
            }
        }
    }

    /**
     * Runs hand detection synchronously. Must be called from a single background thread.
     * The [bitmap] is recycled before this method returns.
     */
    fun detect(bitmap: Bitmap, timestampMs: Long) {
        try {
            val landmarker = handLandmarker
            if (landmarker == null) {
                onResult(null, bitmap)
                return
            }

            val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detectForVideo(mpImage, timestampMs)
            onResult(result, bitmap)
        } catch (t: Throwable) {
            Log.e(TAG, "Hand detection failed", t)
            onResult(null, bitmap)
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    fun close() {
        setupExecutor.execute {
            handLandmarker?.close()
            handLandmarker = null
        }
        setupExecutor.shutdown()
    }

    companion object {
        private const val TAG = "HandLandmarkerHelper"
        private const val MODEL_ASSET = "hand_landmarker.task"
    }
}
