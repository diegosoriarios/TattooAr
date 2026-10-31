package com.diego.tattooar.ui.gestures

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.diego.tattooar.ui.render.TattooTransformState

@Composable
fun TransformableGestureBox(
    transformState: TattooTransformState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    transformState.manualTranslationX += pan.x
                    transformState.manualTranslationY += pan.y
                    transformState.manualScale = (transformState.manualScale * zoom).coerceIn(0.2f, 5.0f)
                    transformState.manualRotationAngle += rotation
                }
            }
    ) {
        content()
    }
}
