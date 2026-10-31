package com.diego.tattooar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diego.tattooar.ui.camera.CameraPreview
import com.diego.tattooar.ui.components.ArtistBookingDialog
import com.diego.tattooar.ui.components.DisclaimerDialog
import com.diego.tattooar.ui.components.TattooCatalogBottomSheet
import com.diego.tattooar.ui.components.TattooControlsOverlay
import com.diego.tattooar.ui.gestures.TransformableGestureBox
import com.diego.tattooar.ui.snapshot.SnapshotHelper

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onBackToGallery: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current

    val selectedDesign by viewModel.selectedDesign.collectAsState()
    val processedBitmap by viewModel.processedTattooBitmap.collectAsState()
    val showDisclaimer by viewModel.showDisclaimer.collectAsState()
    val showPickerBottomSheet by viewModel.showPickerBottomSheet.collectAsState()
    val showArtistBookingDialog by viewModel.showArtistBookingDialog.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required for TattooPreview", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // CameraX Live Stream & Analysis
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onFrameAnalyzed = { imageProxy ->
                viewModel.processFrameForTracking(imageProxy)
            }
        )

        // Interactive Tattoo Overlay Box
        TransformableGestureBox(
            transformState = viewModel.transformState
        ) {
            processedBitmap?.let { bitmap ->
                TattooRenderCanvas(
                    bitmap = bitmap,
                    translationX = viewModel.transformState.effectiveTranslationX,
                    translationY = viewModel.transformState.effectiveTranslationY,
                    scale = viewModel.transformState.effectiveScale,
                    rotationAngle = viewModel.transformState.effectiveRotationAngle,
                    isMirrored = viewModel.transformState.isMirrored,
                    opacity = viewModel.transformState.opacity,
                    inkFade = viewModel.transformState.inkFade,
                    trackingAlpha = viewModel.transformState.trackingAlpha,
                    skinLuminance = viewModel.transformState.skinLuminance,
                    isMeshWrappingEnabled = viewModel.transformState.isMeshWrappingEnabled,
                    meshCurvatureFactor = viewModel.transformState.meshCurvatureFactor
                )
            }
        }

        // Top Navigation Bar (Back to Gallery)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.65f),
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToGallery) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Gallery"
                    )
                }
                Text(
                    text = selectedDesign?.title ?: "AR Preview",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }

        // Controls Bar & Sliders
        TattooControlsOverlay(
            transformState = viewModel.transformState,
            onOpenPicker = { viewModel.openPicker() },
            onTakeSnapshot = {
                val capturedBitmap = SnapshotHelper.captureBitmapFromView(view)
                val savedUri = SnapshotHelper.saveImageToGallery(context, capturedBitmap)
                if (savedUri != null) {
                    viewModel.savePreviewSnapshotToRoom(savedUri.toString())
                    Toast.makeText(context, "Snapshot saved to gallery!", Toast.LENGTH_SHORT).show()
                    SnapshotHelper.shareImageUri(context, savedUri)
                } else {
                    Toast.makeText(context, "Failed to save snapshot", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // First-launch Disclaimer Dialog
        if (showDisclaimer) {
            DisclaimerDialog(
                onDismiss = { viewModel.dismissDisclaimer() }
            )
        }

        // Tattoo Catalog Bottom Sheet
        if (showPickerBottomSheet) {
            TattooCatalogBottomSheet(
                selectedDesign = selectedDesign,
                onSelectDesign = { viewModel.selectDesign(it) },
                onPickCustomImage = { viewModel.loadCustomImageFromGallery(it) },
                onOpenArtistBooking = { viewModel.openArtistBooking() },
                onDismiss = { viewModel.dismissPicker() }
            )
        }

        // Artist Directory & Consultation Booking Dialog
        if (showArtistBookingDialog) {
            ArtistBookingDialog(
                onDismiss = { viewModel.dismissArtistBooking() }
            )
        }
    }
}

@Composable
fun TattooRenderCanvas(
    bitmap: Bitmap,
    translationX: Float,
    translationY: Float,
    scale: Float,
    rotationAngle: Float,
    isMirrored: Boolean,
    opacity: Float,
    inkFade: Float,
    trackingAlpha: Float,
    skinLuminance: Float,
    isMeshWrappingEnabled: Boolean,
    meshCurvatureFactor: Float,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    // Color Matrix for Ink Fade / Aging & Dynamic Skin Tone Light Adjustment
    val fadeMatrix = remember(inkFade, skinLuminance) {
        val contrast = 1.0f - (inkFade * 0.3f)
        val shift = inkFade * 35f + (skinLuminance - 0.5f) * 15f
        ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, shift,
                0f, contrast, 0f, 0f, shift,
                0f, 0f, contrast, 0f, shift,
                0f, 0f, 0f, 1.0f, 0f
            )
        )
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        withTransform({
            translate(left = translationX, top = translationY)
            rotate(degrees = rotationAngle, pivot = androidx.compose.ui.geometry.Offset(centerX, centerY))

            val effectiveScaleX = if (isMirrored) -scale else scale
            val cylindricalCurveScaleY = if (isMeshWrappingEnabled) scale * (1.0f - 0.15f * meshCurvatureFactor) else scale

            scale(
                scaleX = effectiveScaleX,
                scaleY = cylindricalCurveScaleY,
                pivot = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }) {
            val drawX = centerX - (imageBitmap.width / 2f)
            val drawY = centerY - (imageBitmap.height / 2f)

            val effectiveAlpha = opacity * (1.0f - 0.25f * inkFade) * trackingAlpha

            if (effectiveAlpha > 0.01f) {
                drawImage(
                    image = imageBitmap,
                    topLeft = androidx.compose.ui.geometry.Offset(drawX, drawY),
                    alpha = effectiveAlpha,
                    blendMode = BlendMode.Multiply,
                    colorFilter = ColorFilter.colorMatrix(fadeMatrix)
                )
            }
        }
    }
}
