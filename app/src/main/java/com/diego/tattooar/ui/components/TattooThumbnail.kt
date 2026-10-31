package com.diego.tattooar.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.diego.tattooar.data.model.TattooDesign
import com.diego.tattooar.data.model.TattooDesignSource
import com.diego.tattooar.domain.image.BackgroundRemovalHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TattooThumbnail(
    design: TattooDesign,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, design.id) {
        value = withContext(Dispatchers.IO) {
            when (val source = design.source) {
                is TattooDesignSource.Sample -> BackgroundRemovalHelper.loadSampleBitmap(
                    assets = context.assets,
                    assetPath = source.assetPath,
                    sampleId = source.id,
                    inSampleSize = 4
                )
                is TattooDesignSource.LocalUri -> null
            }
        }
    }

    val imageBitmap = bitmap?.asImageBitmap() ?: return

    Image(
        bitmap = imageBitmap,
        contentDescription = design.title,
        modifier = modifier,
        contentScale = contentScale
    )
}
