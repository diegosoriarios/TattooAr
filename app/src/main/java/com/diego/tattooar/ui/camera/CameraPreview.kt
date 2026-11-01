package com.diego.tattooar.ui.camera

import android.view.Surface
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onFrameAnalyzed: (ImageProxy) -> Unit = {},
    onPreviewViewCreated: (PreviewView) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(cameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var isDisposed = false
        var isBound = false
        var cameraProvider: ProcessCameraProvider? = null

        // Keep preview and analysis on the same viewport/crop. Without a shared ViewPort the
        // two use cases can crop the sensor differently, which makes overlays drift toward the
        // edges of the screen.
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    onFrameAnalyzed(imageProxy)
                }
            }

        fun bindCameraIfReady() {
            if (isDisposed || isBound) return
            val provider = cameraProvider ?: return
            if (previewView.width == 0 || previewView.height == 0) return

            val targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
            val viewPort = previewView.getViewPort(targetRotation) ?: return

            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageAnalysis)
                .setViewPort(viewPort)
                .build()

            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
                isBound = true
                onPreviewViewCreated(previewView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            bindCameraIfReady()
        }
        previewView.addOnLayoutChangeListener(layoutListener)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraIfReady()
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            isDisposed = true
            previewView.removeOnLayoutChangeListener(layoutListener)
            try {
                val provider = cameraProvider ?: cameraProviderFuture.get()
                provider.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            analysisExecutor.shutdown()
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    )
}
