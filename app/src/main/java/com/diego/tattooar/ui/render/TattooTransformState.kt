package com.diego.tattooar.ui.render

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.diego.tattooar.domain.tracking.TargetBodyPart

class TattooTransformState {
    // Manual Transform Controls
    var manualTranslationX by mutableFloatStateOf(0f)
    var manualTranslationY by mutableFloatStateOf(0f)
    var manualScale by mutableFloatStateOf(1.0f)
    var manualRotationAngle by mutableFloatStateOf(0f)

    // Tracking Controls & Target Body Part
    var isAutoTrackingEnabled by mutableStateOf(true)
    var targetBodyPart by mutableStateOf(TargetBodyPart.HAND_FOREARM)
    var isTrackingActive by mutableStateOf(false)
    var trackingAlpha by mutableFloatStateOf(1.0f)

    var trackedTranslationX by mutableFloatStateOf(0f)
    var trackedTranslationY by mutableFloatStateOf(0f)
    var trackedScale by mutableFloatStateOf(1.0f)
    var trackedRotationAngle by mutableFloatStateOf(0f)

    // Phase 3 Realism Controls
    var isMeshWrappingEnabled by mutableStateOf(true)
    var meshCurvatureFactor by mutableFloatStateOf(0.7f)
    var isOcclusionEnabled by mutableStateOf(true)

    // General Controls
    var isMirrored by mutableStateOf(false)
    var opacity by mutableFloatStateOf(0.9f)
    var inkFade by mutableFloatStateOf(0.15f)
    var skinLuminance by mutableFloatStateOf(0.85f)

    val effectiveTranslationX: Float
        get() = if (isAutoTrackingEnabled && isTrackingActive) trackedTranslationX + manualTranslationX else manualTranslationX

    val effectiveTranslationY: Float
        get() = if (isAutoTrackingEnabled && isTrackingActive) trackedTranslationY + manualTranslationY else manualTranslationY

    val effectiveScale: Float
        get() = if (isAutoTrackingEnabled && isTrackingActive) trackedScale * manualScale else manualScale

    val effectiveRotationAngle: Float
        get() = if (isAutoTrackingEnabled && isTrackingActive) trackedRotationAngle + manualRotationAngle else manualRotationAngle

    fun resetTransform() {
        manualTranslationX = 0f
        manualTranslationY = 0f
        manualScale = 1.0f
        manualRotationAngle = 0f
        isMirrored = false
    }

    fun toggleMirror() {
        isMirrored = !isMirrored
    }

    fun toggleAutoTracking() {
        isAutoTrackingEnabled = !isAutoTrackingEnabled
    }

    fun toggleMeshWrapping() {
        isMeshWrappingEnabled = !isMeshWrappingEnabled
    }

    fun toggleOcclusion() {
        isOcclusionEnabled = !isOcclusionEnabled
    }
}
