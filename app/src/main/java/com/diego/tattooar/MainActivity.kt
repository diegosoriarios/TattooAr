package com.diego.tattooar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diego.tattooar.ui.AppScreen
import com.diego.tattooar.ui.MainScreen
import com.diego.tattooar.ui.MainViewModel
import com.diego.tattooar.ui.gallery.GalleryScreen
import com.diego.tattooar.ui.theme.TattooArTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TattooArTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val viewModel: MainViewModel = viewModel()
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val availableDesigns by viewModel.availableDesigns.collectAsState()

                    when (currentScreen) {
                        is AppScreen.Gallery -> {
                            GalleryScreen(
                                designs = availableDesigns,
                                onSelectDesignForAr = { design ->
                                    viewModel.selectDesignForAr(design)
                                },
                                onPickCustomImage = { uri ->
                                    viewModel.loadCustomImageFromGallery(uri)
                                }
                            )
                        }
                        is AppScreen.ArPreview -> {
                            MainScreen(
                                viewModel = viewModel,
                                onBackToGallery = {
                                    viewModel.navigateToGallery()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
