package com.diego.tattooar.domain.tracking

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.atan2
import kotlin.math.hypot

data class AnchorTransform(
    val anchorX: Float,        // Screen coordinates X
    val anchorY: Float,        // Screen coordinates Y
    val scale: Float,          // Scale factor derived from wrist-palm distance
    val rotationAngle: Float,  // Orientation angle in degrees
    val confidence: Float,     // Tracking confidence (0.0 to 1.0)
    val isTracked: Boolean     // True if hand/forearm landmarks are currently detected
)

class LandmarkAnchorEngine {

    private val xFilter = OneEuroFilter(minCutoff = 1.2f, beta = 0.01f)
    private val yFilter = OneEuroFilter(minCutoff = 1.2f, beta = 0.01f)
    private val scaleFilter = OneEuroFilter(minCutoff = 0.8f, beta = 0.005f)
    private val rotationFilter = OneEuroFilter(minCutoff = 1.0f, beta = 0.008f)

    private var lastTrackedTimestamp: Long = 0L

    fun computeAnchor(
        landmarks: List<NormalizedLandmark>?,
        screenWidth: Float,
        screenHeight: Float,
        timestampMs: Long
    ): AnchorTransform {
        if (landmarks.isNullOrEmpty() || landmarks.size < 21) {
            return AnchorTransform(
                anchorX = 0f,
                anchorY = 0f,
                scale = 1.0f,
                rotationAngle = 0f,
                confidence = 0f,
                isTracked = false
            )
        }

        // Landmark 0 = Wrist
        val wrist = landmarks[0]
        // Landmark 9 = Middle Finger MCP
        val middleMcp = landmarks[9]

        // Map normalized coordinates [0.0, 1.0] to screen pixel coordinates
        val wristX = wrist.x() * screenWidth
        val wristY = wrist.y() * screenHeight

        val mcpX = middleMcp.x() * screenWidth
        val mcpY = middleMcp.y() * screenHeight

        // Vector extending along forearm (pointing from Middle Finger MCP down towards Wrist)
        val dirX = wristX - mcpX
        val dirY = wristY - mcpY

        // Distance between Wrist and Middle MCP determines natural limb scaling
        val handLengthInPixels = hypot(dirX, dirY)
        val baseReferenceLength = screenWidth * 0.25f
        val rawScale = (handLengthInPixels / baseReferenceLength).coerceIn(0.3f, 4.0f)

        // Orientation angle in degrees relative to vertical axis
        val rawAngle = Math.toDegrees(atan2(dirY.toDouble(), dirX.toDouble())).toFloat() + 90f

        // Position tattoo slightly below wrist onto the forearm
        val forearmOffsetDistance = handLengthInPixels * 0.6f
        val normX = dirX / (handLengthInPixels.coerceAtLeast(1f))
        val normY = dirY / (handLengthInPixels.coerceAtLeast(1f))

        val targetX = wristX + normX * forearmOffsetDistance - (screenWidth / 2f)
        val targetY = wristY + normY * forearmOffsetDistance - (screenHeight / 2f)

        // Apply One Euro filter jitter reduction
        val smoothX = xFilter.filter(targetX, timestampMs)
        val smoothY = yFilter.filter(targetY, timestampMs)
        val smoothScale = scaleFilter.filter(rawScale, timestampMs)
        val smoothRotation = rotationFilter.filter(rawAngle, timestampMs)

        lastTrackedTimestamp = timestampMs

        return AnchorTransform(
            anchorX = smoothX,
            anchorY = smoothY,
            scale = smoothScale,
            rotationAngle = smoothRotation,
            confidence = 0.95f,
            isTracked = true
        )
    }

    fun reset() {
        xFilter.reset()
        yFilter.reset()
        scaleFilter.reset()
        rotationFilter.reset()
    }
}
