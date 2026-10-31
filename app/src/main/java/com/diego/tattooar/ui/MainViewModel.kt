package com.diego.tattooar.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diego.tattooar.data.datastore.UserPreferencesRepository
import com.diego.tattooar.data.model.TattooDesign
import com.diego.tattooar.data.model.TattooDesignSource
import com.diego.tattooar.data.repository.TattooRepository
import com.diego.tattooar.domain.image.BackgroundRemovalHelper
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val repository = TattooRepository(application)
    private val landmarkAnchorEngine = LandmarkAnchorEngine()

    val transformState = TattooTransformState()

    private var handLandmarkerHelper: HandLandmarkerHelper? = null
    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var arCoreFaceTracker: ARCoreFaceTracker? = null
    private var segmentationOcclusionHelper: SegmentationOcclusionHelper? = null

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
        selectDesign(TattooDesign.SAMPLES.first())
        initTrackingEngines()
        seedDatabase()
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

    fun selectDesign(design: TattooDesign) {
        _selectedDesign.value = design
        viewModelScope.launch(Dispatchers.Default) {
            val rawBitmap = when (val source = design.source) {
                is TattooDesignSource.Sample -> {
                    BackgroundRemovalHelper.createSampleDesignBitmap(source.id, 600)
                }
                is TattooDesignSource.LocalUri -> {
                    loadBitmapFromUri(source.uri)
                }
            }

            if (rawBitmap != null) {
                val cleanedBitmap = BackgroundRemovalHelper.removeWhiteBackground(rawBitmap)
                _processedTattooBitmap.value = cleanedBitmap
            }
        }
    }

    fun loadCustomImageFromGallery(uri: Uri) {
        val customDesign = TattooDesign(
            id = "custom_${System.currentTimeMillis()}",
            title = "Custom Gallery Image",
            category = "Custom",
            source = TattooDesignSource.LocalUri(uri, "Custom Image"),
            isCustom = true
        )
        selectDesign(customDesign)

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

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCleared() {
        handLandmarkerHelper?.close()
        poseLandmarkerHelper?.close()
        arCoreFaceTracker?.close()
        segmentationOcclusionHelper?.close()
    }
}
