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
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.atan2
import kotlin.math.hypot

enum class TargetBodyPart {
    HAND_FOREARM,
    UPPER_ARM,
    CHEST_TORSO,
    LEG_THIGH,
    FACE_NECK
}

data class PoseAnchorTransform(
    val anchorX: Float,
    val anchorY: Float,
    val scale: Float,
    val rotationAngle: Float,
    val isTracked: Boolean
)

class PoseLandmarkerHelper(
    private val context: Context,
    private val onResult: (PoseLandmarkerResult?, Bitmap) -> Unit,
    private val onError: (String) -> Unit = {}
) {
    private var poseLandmarker: PoseLandmarker? = null
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val bitmapQueue = ConcurrentLinkedQueue<Bitmap>()

    init {
        setupPoseLandmarker()
    }

    private fun setupPoseLandmarker() {
        backgroundExecutor.execute {
            try {
                val baseOptionsBuilder = BaseOptions.builder()
                try {
                    baseOptionsBuilder.setModelAssetPath("pose_landmarker.task")
                } catch (_: Exception) {}

                val baseOptions = baseOptionsBuilder.build()
                val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinPoseDetectionConfidence(0.5f)
                    .setMinPosePresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener { result, _ ->
                        val bitmap = bitmapQueue.poll() ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                        onResult(result, bitmap)
                    }
                    .setErrorListener { error ->
                        onError(error.message ?: "PoseLandmarker error")
                    }
                    .build()

                poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            } catch (t: Throwable) {
                onError("MediaPipe PoseLandmarker initialization: ${t.localizedMessage}")
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
            val landmarker = poseLandmarker
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

    fun computeBodyAnchor(
        result: PoseLandmarkerResult?,
        targetPart: TargetBodyPart,
        screenWidth: Float,
        screenHeight: Float
    ): PoseAnchorTransform {
        val landmarks = result?.landmarks()?.firstOrNull()
        if (landmarks.isNullOrEmpty() || landmarks.size < 33) {
            return PoseAnchorTransform(0f, 0f, 1.0f, 0f, false)
        }

        val (startIndex, endIndex) = when (targetPart) {
            TargetBodyPart.UPPER_ARM -> Pair(11, 13) // Shoulder to Elbow
            TargetBodyPart.CHEST_TORSO -> Pair(11, 12) // Shoulder to Shoulder
            TargetBodyPart.LEG_THIGH -> Pair(23, 25) // Hip to Knee
            else -> Pair(11, 13)
        }

        val startLM = landmarks[startIndex]
        val endLM = landmarks[endIndex]

        val startX = startLM.x() * screenWidth
        val startY = startLM.y() * screenHeight
        val endX = endLM.x() * screenWidth
        val endY = endLM.y() * screenHeight

        val midX = (startX + endX) / 2f - (screenWidth / 2f)
        val midY = (startY + endY) / 2f - (screenHeight / 2f)

        val dx = endX - startX
        val dy = endY - startY
        val length = hypot(dx, dy)

        val scale = (length / (screenWidth * 0.3f)).coerceIn(0.3f, 4.0f)
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        return PoseAnchorTransform(
            anchorX = midX,
            anchorY = midY,
            scale = scale,
            rotationAngle = angle,
            isTracked = true
        )
    }

    fun close() {
        backgroundExecutor.execute {
            poseLandmarker?.close()
            poseLandmarker = null
            bitmapQueue.clear()
        }
        backgroundExecutor.shutdown()
    }
}
