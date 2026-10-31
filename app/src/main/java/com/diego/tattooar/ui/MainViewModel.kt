package com.diego.tattooar.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
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
import com.diego.tattooar.domain.tracking.SegmentationOcclusionHelper
import com.diego.tattooar.domain.tracking.SkinToneLightingAnalyzer
import com.diego.tattooar.domain.tracking.TargetBodyPart
import com.diego.tattooar.ui.render.TattooTransformState
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
        handLandmarkerHelper = HandLandmarkerHelper(
            context = getApplication(),
            onResult = { result, bitmap ->
                val landmarks = result?.landmarks()?.firstOrNull()
                val timestampMs = System.currentTimeMillis()

                if (landmarks != null && landmarks.size >= 21) {
                    val anchor = landmarkAnchorEngine.computeAnchor(
                        landmarks = landmarks,
                        screenWidth = bitmap.width.toFloat(),
                        screenHeight = bitmap.height.toFloat(),
                        timestampMs = timestampMs
                    )

                    val lightingMetrics = SkinToneLightingAnalyzer.analyzeSkinLighting(bitmap)

                    transformState.trackedTranslationX = anchor.anchorX
                    transformState.trackedTranslationY = anchor.anchorY
                    transformState.trackedScale = anchor.scale
                    transformState.trackedRotationAngle = anchor.rotationAngle
                    transformState.isTrackingActive = true
                    transformState.skinLuminance = lightingMetrics.luminance

                    transformState.trackingAlpha = (transformState.trackingAlpha + 0.15f).coerceAtMost(1.0f)
                } else {
                    transformState.isTrackingActive = false
                    transformState.trackingAlpha = (transformState.trackingAlpha - 0.1f).coerceAtLeast(0.0f)
                }
            }
        )

        poseLandmarkerHelper = PoseLandmarkerHelper(
            context = getApplication(),
            onResult = { result, bitmap ->
                val bodyAnchor = poseLandmarkerHelper?.computeBodyAnchor(
                    result = result,
                    targetPart = transformState.targetBodyPart,
                    screenWidth = bitmap.width.toFloat(),
                    screenHeight = bitmap.height.toFloat()
                )

                if (bodyAnchor != null && bodyAnchor.isTracked) {
                    transformState.trackedTranslationX = bodyAnchor.anchorX
                    transformState.trackedTranslationY = bodyAnchor.anchorY
                    transformState.trackedScale = bodyAnchor.scale
                    transformState.trackedRotationAngle = bodyAnchor.rotationAngle
                    transformState.isTrackingActive = true
                    transformState.trackingAlpha = (transformState.trackingAlpha + 0.15f).coerceAtMost(1.0f)
                } else {
                    transformState.isTrackingActive = false
                    transformState.trackingAlpha = (transformState.trackingAlpha - 0.1f).coerceAtLeast(0.0f)
                }
            }
        )

        arCoreFaceTracker = ARCoreFaceTracker(getApplication()).apply {
            initializeSession()
        }

        segmentationOcclusionHelper = SegmentationOcclusionHelper(
            context = getApplication(),
            onMaskGenerated = { _ -> }
        )
    }

    fun processFrameForTracking(imageProxy: ImageProxy) {
        if (!transformState.isAutoTrackingEnabled) {
            imageProxy.close()
            return
        }

        when (transformState.targetBodyPart) {
            TargetBodyPart.HAND_FOREARM -> {
                handLandmarkerHelper?.detectAsync(imageProxy)
            }
            TargetBodyPart.UPPER_ARM, TargetBodyPart.CHEST_TORSO, TargetBodyPart.LEG_THIGH -> {
                poseLandmarkerHelper?.detectAsync(imageProxy)
            }
            TargetBodyPart.FACE_NECK -> {
                imageProxy.close()
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
}
