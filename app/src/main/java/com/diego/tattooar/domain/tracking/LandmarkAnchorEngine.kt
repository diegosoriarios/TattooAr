package com.diego.tattooar.domain.tracking

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Anchor expressed in normalized upright-image coordinates. Callers map it to screen
 * space with [PreviewCoordinateMapper] so the value is independent of camera resolution.
 */
data class AnchorTransform(
    val normalizedX: Float,
    val normalizedY: Float,
    val normalizedLength: Float,
    val rotationAngle: Float,
    val confidence: Float,
    val isTracked: Boolean
)

class LandmarkAnchorEngine {

    private val xFilter = OneEuroFilter(minCutoff = 1.2f, beta = 0.01f)
    private val yFilter = OneEuroFilter(minCutoff = 1.2f, beta = 0.01f)
    private val lengthFilter = OneEuroFilter(minCutoff = 0.8f, beta = 0.005f)
    private val rotationFilter = OneEuroFilter(minCutoff = 1.0f, beta = 0.008f)

    private var lastTrackedTimestamp: Long = 0L

    fun computeAnchor(
        landmarks: List<NormalizedLandmark>?,
        imageWidth: Float,
        imageHeight: Float,
        timestampMs: Long
    ): AnchorTransform {
        if (landmarks.isNullOrEmpty() || landmarks.size < 21 || imageWidth <= 0f || imageHeight <= 0f) {
            return AnchorTransform(
                normalizedX = 0f,
                normalizedY = 0f,
                normalizedLength = 0f,
                rotationAngle = 0f,
                confidence = 0f,
                isTracked = false
            )
        }

        // Landmark 0 = Wrist
        val wrist = landmarks[0]
        // Landmark 9 = Middle Finger MCP
        val middleMcp = landmarks[9]

        // Map normalized coordinates to image pixel coordinates
        val wristX = wrist.x() * imageWidth
        val wristY = wrist.y() * imageHeight

        val mcpX = middleMcp.x() * imageWidth
        val mcpY = middleMcp.y() * imageHeight

        // Vector extending along forearm (pointing from Middle Finger MCP down towards Wrist)
        val dirX = wristX - mcpX
        val dirY = wristY - mcpY

        // Distance between Wrist and Middle MCP determines natural limb scaling
        val handLengthInPixels = hypot(dirX, dirY).coerceAtLeast(1f)

        // Orientation angle in degrees relative to vertical axis
        val rawAngle = Math.toDegrees(atan2(dirY.toDouble(), dirX.toDouble())).toFloat() + 90f

        // Position tattoo slightly below wrist onto the forearm
        val forearmOffsetDistance = handLengthInPixels * 0.6f
        val normX = dirX / handLengthInPixels
        val normY = dirY / handLengthInPixels

        val targetX = wristX + normX * forearmOffsetDistance
        val targetY = wristY + normY * forearmOffsetDistance

        // Apply One Euro filter jitter reduction in pixel space
        val smoothX = xFilter.filter(targetX, timestampMs)
        val smoothY = yFilter.filter(targetY, timestampMs)
        val smoothLength = lengthFilter.filter(handLengthInPixels, timestampMs)
        val smoothRotation = rotationFilter.filter(rawAngle, timestampMs)

        lastTrackedTimestamp = timestampMs

        return AnchorTransform(
            normalizedX = (smoothX / imageWidth).coerceIn(-0.5f, 1.5f),
            normalizedY = (smoothY / imageHeight).coerceIn(-0.5f, 1.5f),
            normalizedLength = smoothLength / imageWidth,
            rotationAngle = smoothRotation,
            confidence = 0.95f,
            isTracked = true
        )
    }

    fun reset() {
        xFilter.reset()
        yFilter.reset()
        lengthFilter.reset()
        rotationFilter.reset()
    }
}
