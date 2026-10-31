package com.diego.tattooar.data.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.diego.tattooar.data.model.TattooDesign
import com.diego.tattooar.data.model.TattooDesignSource
import com.diego.tattooar.domain.image.BackgroundRemovalHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local implementation of [TattooImageAdapter] loading designs from local app assets and device storage.
 */
class LocalTattooImageAdapter : TattooImageAdapter {

    override suspend fun getTattooDesigns(): List<TattooDesign> = withContext(Dispatchers.IO) {
        TattooDesign.SAMPLES
    }

    override suspend fun loadTattooBitmap(context: Context, design: TattooDesign): Bitmap? = withContext(Dispatchers.IO) {
        val rawBitmap = when (val source = design.source) {
            is TattooDesignSource.Sample -> {
                BackgroundRemovalHelper.loadSampleBitmap(
                    assets = context.assets,
                    assetPath = source.assetPath,
                    sampleId = source.id
                )
            }
            is TattooDesignSource.LocalUri -> {
                loadBitmapFromUri(context, source.uri)
            }
        }

        if (rawBitmap != null) {
            when (design.source) {
                is TattooDesignSource.Sample -> rawBitmap
                is TattooDesignSource.LocalUri -> BackgroundRemovalHelper.removeWhiteBackground(rawBitmap)
            }
        } else null
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
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
}

/*
 * Example template for future Remote/URL Adapter:
 *
 * class UrlTattooImageAdapter(private val baseUrl: String) : TattooImageAdapter {
 *     override suspend fun getTattooDesigns(): List<TattooDesign> {
 *         // Fetch JSON metadata array from remote API endpoint
 *     }
 *
 *     override suspend fun loadTattooBitmap(context: Context, design: TattooDesign): Bitmap? {
 *         // Download image bitmap from CDN/URL using Coil or Http client
 *     }
 * }
 */
