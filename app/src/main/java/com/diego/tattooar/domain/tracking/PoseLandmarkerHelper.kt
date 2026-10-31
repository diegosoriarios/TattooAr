package com.diego.tattooar.domain.tracking

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
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
    val normalizedX: Float,
    val normalizedY: Float,
    val normalizedLength: Float,
    val rotationAngle: Float,
    val isTracked: Boolean
)

class PoseLandmarkerHelper(
    private val context: Context,
    private val onResult: (PoseLandmarkerResult?, Bitmap) -> Unit,
    private val onError: (String) -> Unit = {},
    private val onReady: () -> Unit = {}
) {
    @Volatile
    private var poseLandmarker: PoseLandmarker? = null

    private val setupExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        setupPoseLandmarker()
    }

    private fun setupPoseLandmarker() {
        setupExecutor.execute {
            try {
                val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(
                        BaseOptions.builder()
                            .setModelAssetPath(MODEL_ASSET)
                            .build()
                    )
                    .setMinPoseDetectionConfidence(0.5f)
                    .setMinPosePresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setRunningMode(RunningMode.VIDEO)
                    .setErrorListener { error ->
                        onError("Body tracking error: ${error.message ?: "unknown"}")
                    }
                    .build()

                poseLandmarker = PoseLandmarker.createFromOptions(context, options)
                Log.i(TAG, "PoseLandmarker ready")
                onReady()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to initialize PoseLandmarker", t)
                onError("Body tracking unavailable: ${t.message ?: t.javaClass.simpleName}")
            }
        }
    }

    /**
     * Runs pose detection synchronously. Must be called from a single background thread.
     * The [bitmap] is recycled before this method returns.
     */
    fun detect(bitmap: Bitmap, timestampMs: Long) {
        try {
            val landmarker = poseLandmarker
            if (landmarker == null) {
                onResult(null, bitmap)
                return
            }

            val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detectForVideo(mpImage, timestampMs)
            onResult(result, bitmap)
        } catch (t: Throwable) {
            Log.e(TAG, "Pose detection failed", t)
            onResult(null, bitmap)
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    fun computeBodyAnchor(
        result: PoseLandmarkerResult?,
        targetPart: TargetBodyPart,
        imageWidth: Float,
        imageHeight: Float
    ): PoseAnchorTransform {
        val landmarks = result?.landmarks()?.firstOrNull()
        if (landmarks.isNullOrEmpty() || landmarks.size < 33) {
            return PoseAnchorTransform(0f, 0f, 0f, 0f, false)
        }

        val (startIndex, endIndex) = when (targetPart) {
            TargetBodyPart.UPPER_ARM -> Pair(11, 13) // Shoulder to Elbow
            TargetBodyPart.CHEST_TORSO -> Pair(11, 12) // Shoulder to Shoulder
            TargetBodyPart.LEG_THIGH -> Pair(23, 25) // Hip to Knee
            else -> Pair(11, 13)
        }

        val startLM = landmarks[startIndex]
        val endLM = landmarks[endIndex]

        val startX = startLM.x() * imageWidth
        val startY = startLM.y() * imageHeight
        val endX = endLM.x() * imageWidth
        val endY = endLM.y() * imageHeight

        val midX = (startX + endX) / 2f
        val midY = (startY + endY) / 2f

        val dx = endX - startX
        val dy = endY - startY
        val length = hypot(dx, dy)

        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        return PoseAnchorTransform(
            normalizedX = (midX / imageWidth).coerceIn(-0.5f, 1.5f),
            normalizedY = (midY / imageHeight).coerceIn(-0.5f, 1.5f),
            normalizedLength = length / imageWidth,
            rotationAngle = angle,
            isTracked = true
        )
    }

    fun close() {
        setupExecutor.execute {
            poseLandmarker?.close()
            poseLandmarker = null
        }
        setupExecutor.shutdown()
    }

    companion object {
        private const val TAG = "PoseLandmarkerHelper"
        private const val MODEL_ASSET = "pose_landmarker.task"
    }
}
