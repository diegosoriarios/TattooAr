package com.diego.tattooar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tattoos")
data class TattooEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val imageUri: String? = null,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_previews")
data class SavedPreviewEntity(
    @PrimaryKey val id: String,
    val designId: String,
    val previewImageUri: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)
