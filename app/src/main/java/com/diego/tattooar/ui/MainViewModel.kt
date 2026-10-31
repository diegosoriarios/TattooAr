package com.diego.tattooar.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diego.tattooar.data.adapter.LocalTattooImageAdapter
import com.diego.tattooar.data.adapter.TattooImageAdapter
import com.diego.tattooar.data.datastore.UserPreferencesRepository
import com.diego.tattooar.data.model.TattooDesign
import com.diego.tattooar.data.model.TattooDesignSource
import com.diego.tattooar.data.repository.TattooRepository
import com.diego.tattooar.domain.tracking.ARCoreFaceTracker
import com.diego.tattooar.domain.tracking.HandLandmarkerHelper
import com.diego.tattooar.domain.tracking.LandmarkAnchorEngine
import com.diego.tattooar.domain.tracking.PoseLandmarkerHelper
import com.diego.tattooar.domain.tracking.PreviewCoordinateMapper
import com.diego.tattooar.domain.tracking.SegmentationOcclusionHelper
import com.diego.tattooar.domain.tracking.SkinToneLightingAnalyzer
import com.diego.tattooar.domain.tracking.TargetBodyPart
import com.diego.tattooar.domain.tracking.toUprightBitmap
import com.diego.tattooar.ui.render.TattooTransformState
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AppScreen {
    object Gallery : AppScreen()
    data class ArPreview(val design: TattooDesign) : AppScreen()
}

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val imageAdapter: TattooImageAdapter = LocalTattooImageAdapter()
) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val repository = TattooRepository(application)
    private val landmarkAnchorEngine = LandmarkAnchorEngine()

    val transformState = TattooTransformState()

    private var handLandmarkerHelper: HandLandmarkerHelper? = null
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var arCoreFaceTracker: ARCoreFaceTracker? = null
    private var segmentationOcclusionHelper: SegmentationOcclusionHelper? = null

    @Volatile
    private var viewportWidth: Float = 0f

    @Volatile
    private var viewportHeight: Float = 0f

    private val _trackingError = MutableStateFlow<String?>(null)
    val trackingError: StateFlow<String?> = _trackingError.asStateFlow()

    private val _isTrackingReady = MutableStateFlow(false)
    val isTrackingReady: StateFlow<Boolean> = _isTrackingReady.asStateFlow()

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Gallery)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _availableDesigns = MutableStateFlow<List<TattooDesign>>(emptyList())
    val availableDesigns: StateFlow<List<TattooDesign>> = _availableDesigns.asStateFlow()

    private val _selectedDesign = MutableStateFlow<TattooDesign?>(TattooDesign.SAMPLES.first())
    val selectedDesign: StateFlow<TattooDesign?> = _selectedDesign.asStateFlow()

    private val _processedTattooBitmap = MutableStateFlow<Bitmap?>(null)
    val processedTattooBitmap: StateFlow<Bitmap?> = _processedTattooBitmap.asStateFlow()

    private val _showDisclaimer = MutableStateFlow(false)
    val showDisclaimer: StateFlow<Boolean> = _showDisclaimer.asStateFlow()

    private val _showPickerBottomSheet = MutableStateFlow(false)
    val showPickerBottomSheet: StateFlow<Boolean> = _showPickerBottomSheet.asStateFlow()

    private val _showArtistBookingDialog = MutableStateFlow(false)
    val showArtistBookingDialog: StateFlow<Boolean> = _showArtistBookingDialog.asStateFlow()

    init {
        checkDisclaimer()
        loadAvailableDesigns()
        initTrackingEngines()
        seedDatabase()
    }

    private fun loadAvailableDesigns() {
        viewModelScope.launch {
            val designs = imageAdapter.getTattooDesigns()
            _availableDesigns.value = designs
            if (designs.isNotEmpty() && _selectedDesign.value == null) {
                _selectedDesign.value = designs.first()
            }
        }
    }

    private fun seedDatabase() {
        viewModelScope.launch {
            repository.seedInitialData()
        }
    }

    private fun initTrackingEngines() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                handLandmarkerHelper = HandLandmarkerHelper(
                    context = getApplication(),
                    onResult = ::handleHandResult,
                    onError = ::reportTrackingError,
                    onReady = { _isTrackingReady.value = true }
                )
            } catch (t: Throwable) {
                reportTrackingError("Hand tracking unavailable: ${t.message ?: t.javaClass.simpleName}")
            }

            try {
                poseLandmarkerHelper = PoseLandmarkerHelper(
                    context = getApplication(),
                    onResult = ::handlePoseResult,
                    onError = ::reportTrackingError,
                    onReady = { _isTrackingReady.value = true }
                )
            } catch (t: Throwable) {
                reportTrackingError("Body tracking unavailable: ${t.message ?: t.javaClass.simpleName}")
            }

            try {
                arCoreFaceTracker = ARCoreFaceTracker(getApplication()).apply {
                    initializeSession()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            try {
                segmentationOcclusionHelper = SegmentationOcclusionHelper(
                    context = getApplication(),
                    onMaskGenerated = { _ -> }
                )
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    fun retryTracking() {
        handLandmarkerHelper?.close()
        poseLandmarkerHelper?.close()
        handLandmarkerHelper = null
        poseLandmarkerHelper = null
        landmarkAnchorEngine.reset()
        _trackingError.value = null
        _isTrackingReady.value = false
        initTrackingEngines()
    }

    fun setViewportSize(width: Float, height: Float) {
        viewportWidth = width
        viewportHeight = height
    }

    private fun reportTrackingError(message: String) {
        Log.e(TAG, message)
        _trackingError.value = message
    }

    private fun handleHandResult(result: HandLandmarkerResult?, bitmap: Bitmap) {
        val landmarks = result?.landmarks()?.firstOrNull()

        if (landmarks != null && landmarks.size >= 21) {
            val anchor = landmarkAnchorEngine.computeAnchor(
                landmarks = landmarks,
                imageWidth = bitmap.width.toFloat(),
                imageHeight = bitmap.height.toFloat(),
                timestampMs = SystemClock.uptimeMillis()
            )

            val lightingMetrics = SkinToneLightingAnalyzer.analyzeSkinLighting(bitmap)
            applyNormalizedAnchor(
                normalizedX = anchor.normalizedX,
                normalizedY = anchor.normalizedY,
                normalizedLength = anchor.normalizedLength,
                rotationAngle = anchor.rotationAngle,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height
            )
            transformState.skinLuminance = lightingMetrics.luminance
            transformState.isTrackingActive = true
            transformState.trackingAlpha = (transformState.trackingAlpha + 0.15f).coerceAtMost(1.0f)
            clearTrackingError()
        } else {
            transformState.isTrackingActive = false
            transformState.trackingAlpha = (transformState.trackingAlpha - 0.1f).coerceAtLeast(0.0f)
        }
    }

    private fun handlePoseResult(result: PoseLandmarkerResult?, bitmap: Bitmap) {
        val bodyAnchor = poseLandmarkerHelper?.computeBodyAnchor(
            result = result,
            targetPart = transformState.targetBodyPart,
            imageWidth = bitmap.width.toFloat(),
            imageHeight = bitmap.height.toFloat()
        )

        if (bodyAnchor != null && bodyAnchor.isTracked) {
            applyNormalizedAnchor(
                normalizedX = bodyAnchor.normalizedX,
                normalizedY = bodyAnchor.normalizedY,
                normalizedLength = bodyAnchor.normalizedLength,
                rotationAngle = bodyAnchor.rotationAngle,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height
            )
            transformState.isTrackingActive = true
            transformState.trackingAlpha = (transformState.trackingAlpha + 0.15f).coerceAtMost(1.0f)
            clearTrackingError()
        } else {
            transformState.isTrackingActive = false
            transformState.trackingAlpha = (transformState.trackingAlpha - 0.1f).coerceAtLeast(0.0f)
        }
    }

    private fun applyNormalizedAnchor(
        normalizedX: Float,
        normalizedY: Float,
        normalizedLength: Float,
        rotationAngle: Float,
        imageWidth: Int,
        imageHeight: Int
    ) {
        val viewWidth = if (viewportWidth > 0f) viewportWidth else imageWidth.toFloat()
        val viewHeight = if (viewportHeight > 0f) viewportHeight else imageHeight.toFloat()
        val mapping = PreviewCoordinateMapper.fillCenterMapping(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight
        )

        val (screenX, screenY) = PreviewCoordinateMapper.toViewCenter(
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            mapping = mapping
        )

        val screenLength = normalizedLength * imageWidth * mapping.scale
        transformState.trackedTranslationX = screenX
        transformState.trackedTranslationY = screenY
        transformState.trackedScale = (screenLength / (viewWidth * 0.25f)).coerceIn(0.3f, 4.0f)
        transformState.trackedRotationAngle = rotationAngle
    }

    private fun clearTrackingError() {
        if (_trackingError.value != null) {
            _trackingError.value = null
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun processFrameForTracking(imageProxy: ImageProxy) {
        if (!transformState.isAutoTrackingEnabled) {
            imageProxy.close()
            return
        }

        val bitmap = imageProxy.toUprightBitmap()
        imageProxy.close()
        val timestampMs = SystemClock.uptimeMillis()

        when (transformState.targetBodyPart) {
            TargetBodyPart.HAND_FOREARM -> {
                val helper = handLandmarkerHelper
                if (helper != null) {
                    helper.detect(bitmap, timestampMs)
                } else {
                    bitmap.recycle()
                }
            }
            TargetBodyPart.UPPER_ARM, TargetBodyPart.CHEST_TORSO, TargetBodyPart.LEG_THIGH -> {
                val helper = poseLandmarkerHelper
                if (helper != null) {
                    helper.detect(bitmap, timestampMs)
                } else {
                    bitmap.recycle()
                }
            }
            TargetBodyPart.FACE_NECK -> {
                bitmap.recycle()
            }
        }
    }

    private fun checkDisclaimer() {
        viewModelScope.launch {
            val accepted = userPreferencesRepository.isDisclaimerAccepted.first()
            _showDisclaimer.value = !accepted
        }
    }

    fun dismissDisclaimer() {
        viewModelScope.launch {
            userPreferencesRepository.setDisclaimerAccepted(true)
            _showDisclaimer.value = false
        }
    }

    fun openPicker() {
        _showPickerBottomSheet.value = true
    }

    fun dismissPicker() {
        _showPickerBottomSheet.value = false
    }

    fun openArtistBooking() {
        _showArtistBookingDialog.value = true
    }

    fun dismissArtistBooking() {
        _showArtistBookingDialog.value = false
    }

    fun selectDesignForAr(design: TattooDesign) {
        _selectedDesign.value = design
        _currentScreen.value = AppScreen.ArPreview(design)
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = imageAdapter.loadTattooBitmap(getApplication(), design)
            _processedTattooBitmap.value = bitmap
        }
    }

    fun selectDesign(design: TattooDesign) {
        _selectedDesign.value = design
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = imageAdapter.loadTattooBitmap(getApplication(), design)
            _processedTattooBitmap.value = bitmap
        }
    }

    fun navigateToGallery() {
        _currentScreen.value = AppScreen.Gallery
    }

    fun loadCustomImageFromGallery(uri: Uri) {
        val customDesign = TattooDesign(
            id = "custom_${System.currentTimeMillis()}",
            title = "Custom Gallery Image",
            category = "Custom",
            source = TattooDesignSource.LocalUri(uri, "Custom Image"),
            isCustom = true
        )
        selectDesignForAr(customDesign)

        viewModelScope.launch {
            repository.saveCustomTattoo(
                id = customDesign.id,
                title = customDesign.title,
                category = customDesign.category,
                uri = uri.toString()
            )
        }
    }

    fun savePreviewSnapshotToRoom(previewUri: String) {
        viewModelScope.launch {
            repository.savePreviewSnapshot(
                previewUri = previewUri,
                designTitle = selectedDesign.value?.title ?: "Tattoo Preview"
            )
        }
    }

    override fun onCleared() {
        handLandmarkerHelper?.close()
        poseLandmarkerHelper?.close()
        arCoreFaceTracker?.close()
        segmentationOcclusionHelper?.close()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
