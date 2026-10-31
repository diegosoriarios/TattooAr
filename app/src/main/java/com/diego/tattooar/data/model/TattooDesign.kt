package com.diego.tattooar.data.model

import android.net.Uri

sealed class TattooDesignSource {
    data class Sample(val id: String, val name: String, val category: String) : TattooDesignSource()
    data class LocalUri(val uri: Uri, val name: String) : TattooDesignSource()
}

data class TattooDesign(
    val id: String,
    val title: String,
    val category: String,
    val source: TattooDesignSource,
    val isCustom: Boolean = false
) {
    companion object {
        val SAMPLES = listOf(
            TattooDesign(
                id = "dragon_1",
                title = "Tribal Dragon",
                category = "Tribal",
                source = TattooDesignSource.Sample("dragon_1", "Tribal Dragon", "Tribal")
            ),
            TattooDesign(
                id = "rose_1",
                title = "Traditional Rose",
                category = "Floral",
                source = TattooDesignSource.Sample("rose_1", "Traditional Rose", "Floral")
            ),
            TattooDesign(
                id = "compass_1",
                title = "Geometric Compass",
                category = "Geometric",
                source = TattooDesignSource.Sample("compass_1", "Geometric Compass", "Geometric")
            ),
            TattooDesign(
                id = "skull_1",
                title = "Gothic Skull",
                category = "Gothic",
                source = TattooDesignSource.Sample("skull_1", "Gothic Skull", "Gothic")
            ),
            TattooDesign(
                id = "moon_1",
                title = "Minimalist Moon",
                category = "Minimalist",
                source = TattooDesignSource.Sample("moon_1", "Minimalist Moon", "Minimalist")
            )
        )
    }
}
