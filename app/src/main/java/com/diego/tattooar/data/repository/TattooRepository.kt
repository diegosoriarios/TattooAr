package com.diego.tattooar.data.repository

import android.content.Context
import com.diego.tattooar.data.local.AppDatabase
import com.diego.tattooar.data.local.SavedPreviewEntity
import com.diego.tattooar.data.local.TattooEntity
import com.diego.tattooar.data.model.TattooDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TattooRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).tattooDao()

    val allTattoos: Flow<List<TattooEntity>> = dao.getAllTattoos()
    val favoriteTattoos: Flow<List<TattooEntity>> = dao.getFavoriteTattoos()
    val savedPreviews: Flow<List<SavedPreviewEntity>> = dao.getSavedPreviews()

    suspend fun seedInitialData() {
        withContext(Dispatchers.IO) {
            val initialEntities = TattooDesign.SAMPLES.map { design ->
                TattooEntity(
                    id = design.id,
                    title = design.title,
                    category = design.category,
                    isCustom = false
                )
            }
            dao.insertTattoos(initialEntities)
        }
    }

    fun searchTattoos(query: String): Flow<List<TattooEntity>> {
        return dao.searchTattoos(query)
    }

    suspend fun toggleFavorite(tattoo: TattooEntity) {
        withContext(Dispatchers.IO) {
            val updated = tattoo.copy(isFavorite = !tattoo.isFavorite)
            dao.updateTattoo(updated)
        }
    }

    suspend fun saveCustomTattoo(id: String, title: String, category: String, uri: String) {
        withContext(Dispatchers.IO) {
            val entity = TattooEntity(
                id = id,
                title = title,
                category = category,
                imageUri = uri,
                isCustom = true
            )
            dao.insertTattoo(entity)
        }
    }

    suspend fun savePreviewSnapshot(previewUri: String, designTitle: String) {
        withContext(Dispatchers.IO) {
            val entity = SavedPreviewEntity(
                id = "preview_${System.currentTimeMillis()}",
                designId = "custom",
                previewImageUri = previewUri,
                title = designTitle
            )
            dao.insertSavedPreview(entity)
        }
    }
}
