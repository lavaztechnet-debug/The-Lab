package com.thelab.mediahub.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY lastModified DESC")
    fun getAllMediaItems(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE category = :category ORDER BY lastModified DESC")
    fun getMediaByCategory(category: FileCategory): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE fileName LIKE '%' || :query || '%' OR formattedLabel LIKE '%' || :query || '%'")
    fun searchMedia(query: String): Flow<List<MediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItem>)

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}
