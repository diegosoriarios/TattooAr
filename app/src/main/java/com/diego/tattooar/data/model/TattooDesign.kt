package com.diego.tattooar.data.model

import android.net.Uri

sealed class TattooDesignSource {
    data class Sample(
        val id: String,
        val name: String,
        val category: String,
        val assetPath: String
    ) : TattooDesignSource()

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
                source = TattooDesignSource.Sample(
                    id = "dragon_1",
                    name = "Tribal Dragon",
                    category = "Tribal",
                    assetPath = "tattoos/tattoo_tribal_dragon.png"
                )
            ),
            TattooDesign(
                id = "rose_1",
                title = "Tribal Rose",
                category = "Floral",
                source = TattooDesignSource.Sample(
                    id = "rose_1",
                    name = "Tribal Rose",
                    category = "Floral",
                    assetPath = "tattoos/tattoo_tribal_rose.png"
                )
            ),
            TattooDesign(
                id = "compass_1",
                title = "Compass Rose",
                category = "Geometric",
                source = TattooDesignSource.Sample(
                    id = "compass_1",
                    name = "Compass Rose",
                    category = "Geometric",
                    assetPath = "tattoos/tattoo_compass_rose.png"
                )
            ),
            TattooDesign(
                id = "wolf_1",
                title = "Tribal Wolf",
                category = "Tribal",
                source = TattooDesignSource.Sample(
                    id = "wolf_1",
                    name = "Tribal Wolf",
                    category = "Tribal",
                    assetPath = "tattoos/tattoo_tribal_wolf.png"
                )
            ),
            TattooDesign(
                id = "floral_1",
                title = "Floral Flourish",
                category = "Floral",
                source = TattooDesignSource.Sample(
                    id = "floral_1",
                    name = "Floral Flourish",
                    category = "Floral",
                    assetPath = "tattoos/tattoo_floral_flourish.png"
                )
            )
        )
    }
}
