package com.diego.tattooar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TattooDao {

    @Query("SELECT * FROM tattoos ORDER BY createdAt DESC")
    fun getAllTattoos(): Flow<List<TattooEntity>>

    @Query("SELECT * FROM tattoos WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteTattoos(): Flow<List<TattooEntity>>

    @Query("SELECT * FROM tattoos WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchTattoos(query: String): Flow<List<TattooEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTattoos(tattoos: List<TattooEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTattoo(tattoo: TattooEntity)

    @Update
    suspend fun updateTattoo(tattoo: TattooEntity)

    @Query("SELECT * FROM saved_previews ORDER BY createdAt DESC")
    fun getSavedPreviews(): Flow<List<SavedPreviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPreview(preview: SavedPreviewEntity)
}
