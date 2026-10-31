package com.diego.tattooar.data.adapter

import android.content.Context
import android.graphics.Bitmap
import com.diego.tattooar.data.model.TattooDesign

/**
 * Adapter interface for tattoo image data sources.
 * Abstracting image retrieval allows switching seamlessly between local assets,
 * local storage, or remote REST/URL endpoints without changing UI code.
 */
interface TattooImageAdapter {
    /**
     * Fetches available tattoo designs list.
     */
    suspend fun getTattooDesigns(): List<TattooDesign>

    /**
     * Loads the high-resolution bitmap for a specified design.
     */
    suspend fun loadTattooBitmap(context: Context, design: TattooDesign): Bitmap?
}
