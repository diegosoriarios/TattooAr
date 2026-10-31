package com.diego.tattooar.domain.tracking

import android.content.Context
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException

data class FaceAnchorData(
    val centerX: Float,
    val centerY: Float,
    val scale: Float,
    val rotationAngle: Float,
    val isFaceTracked: Boolean
)

class ARCoreFaceTracker(private val context: Context) {

    private var arSession: Session? = null

    fun initializeSession(): Boolean {
        return try {
            arSession = Session(context).apply {
                val config = Config(this).apply {
                    augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
                }
                configure(config)
            }
            true
        } catch (e: UnavailableException) {
            e.printStackTrace()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun computeFaceAnchor(
        faces: Collection<AugmentedFace>,
        screenWidth: Float,
        screenHeight: Float
    ): FaceAnchorData {
        val face = faces.firstOrNull { it.trackingState == com.google.ar.core.TrackingState.TRACKING }
            ?: return FaceAnchorData(0f, 0f, 1.0f, 0f, false)

        val centerPose = face.centerPose
        val translation = centerPose.translation

        // Screen mapping approximation
        val screenX = (translation[0] * screenWidth * 0.5f)
        val screenY = (-translation[1] * screenHeight * 0.5f)

        // Rotation & distance estimation
        val rotMatrix = FloatArray(16)
        centerPose.toMatrix(rotMatrix, 0)
        val scaleFactor = (1.0f / (translation[2].coerceAtLeast(0.2f))).coerceIn(0.5f, 3.0f)

        return FaceAnchorData(
            centerX = screenX,
            centerY = screenY,
            scale = scaleFactor,
            rotationAngle = 0f,
            isFaceTracked = true
        )
    }

    fun close() {
        arSession?.close()
        arSession = null
    }
}
