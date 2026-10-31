package com.diego.tattooar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.diego.tattooar.domain.tracking.TargetBodyPart
import com.diego.tattooar.ui.render.TattooTransformState

@Composable
fun TattooControlsOverlay(
    transformState: TattooTransformState,
    onOpenPicker: () -> Unit,
    onTakeSnapshot: () -> Unit,
    trackingError: String? = null,
    isTrackingReady: Boolean = false,
    onRetryTracking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSliders by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Top Action Bar & Body Part Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { transformState.resetTransform() }) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Position"
                            )
                        }

                        IconButton(onClick = { transformState.toggleMirror() }) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "Mirror Flip",
                                tint = if (transformState.isMirrored) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        IconButton(onClick = { transformState.toggleAutoTracking() }) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = "Toggle Tracking",
                                tint = if (transformState.isAutoTrackingEnabled) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        IconButton(onClick = { transformState.toggleMeshWrapping() }) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = "3D Curved Mesh Wrapping",
                                tint = if (transformState.isMeshWrappingEnabled) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        IconButton(onClick = { transformState.toggleOcclusion() }) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Occlusion Filtering",
                                tint = if (transformState.isOcclusionEnabled) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        IconButton(onClick = { showSliders = !showSliders }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Adjust Opacity, Fade & Curve",
                                tint = if (showSliders) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                }

                // Tracking Status Badge
                val trackingStatusText = when {
                    trackingError != null -> "Tracking unavailable (tap to retry)"
                    !isTrackingReady -> "Initializing..."
                    !transformState.isAutoTrackingEnabled -> "Manual 2D"
                    transformState.isTrackingActive -> "Anchored"
                    else -> "Searching..."
                }
                val trackingStatusColor = when {
                    trackingError != null -> Color(0xFFB71C1C).copy(alpha = 0.9f)
                    !isTrackingReady -> Color.Black.copy(alpha = 0.6f)
                    !transformState.isAutoTrackingEnabled -> Color.Black.copy(alpha = 0.6f)
                    transformState.isTrackingActive -> Color(0xFF1B5E20).copy(alpha = 0.85f)
                    else -> Color(0xFFE65100).copy(alpha = 0.85f)
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = trackingStatusColor,
                    contentColor = Color.White,
                    modifier = if (trackingError != null) {
                        Modifier.clickable { onRetryTracking() }
                    } else {
                        Modifier
                    }
                ) {
                    Text(
                        text = trackingStatusText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Part Target Selection Chips
            AnimatedVisibility(visible = transformState.isAutoTrackingEnabled) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TargetBodyPart.entries) { bodyPart ->
                        val isSelected = transformState.targetBodyPart == bodyPart
                        FilterChip(
                            selected = isSelected,
                            onClick = { transformState.targetBodyPart = bodyPart },
                            label = {
                                Text(
                                    text = when (bodyPart) {
                                        TargetBodyPart.HAND_FOREARM -> "Hand / Forearm"
                                        TargetBodyPart.UPPER_ARM -> "Upper Arm"
                                        TargetBodyPart.CHEST_TORSO -> "Chest / Torso"
                                        TargetBodyPart.LEG_THIGH -> "Leg / Thigh"
                                        TargetBodyPart.FACE_NECK -> "Face / Neck"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        }

        // Sliders Adjustment Card
        AnimatedVisibility(
            visible = showSliders,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.82f),
                contentColor = Color.White,
                modifier = Modifier.width(230.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Opacity Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Opacity: ${(transformState.opacity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Slider(
                        value = transformState.opacity,
                        onValueChange = { transformState.opacity = it },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ink Fade / Aging Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ink Fade: ${(transformState.inkFade * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Slider(
                        value = transformState.inkFade,
                        onValueChange = { transformState.inkFade = it },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3D Limb Curve Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "3D Limb Curvature: ${(transformState.meshCurvatureFactor * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Slider(
                        value = transformState.meshCurvatureFactor,
                        onValueChange = { transformState.meshCurvatureFactor = it },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Bottom Controls Bar (Picker & Capture Snapshot)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = onOpenPicker,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Pick Tattoo Design"
                )
            }

            FloatingActionButton(
                onClick = onTakeSnapshot,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "Capture Snapshot",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
