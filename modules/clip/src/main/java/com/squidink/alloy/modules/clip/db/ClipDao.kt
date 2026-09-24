package com.squidink.alloy.modules.clip.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clip_entries ORDER BY isPinned DESC, timestamp DESC")
    fun getAllClips(): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clip_entries WHERE textContent LIKE '%' || :query || '%' ORDER BY isPinned DESC, timestamp DESC")
    fun searchClips(query: String): Flow<List<ClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipEntity)

    @Query("UPDATE clip_entries SET textContent = :textContent, isPinned = :isPinned WHERE id = :id")
    suspend fun updateClip(
        id: String,
        textContent: String,
        isPinned: Boolean,
    )

    @Query("DELETE FROM clip_entries WHERE id = :id")
    suspend fun deleteClip(id: String)

    @Query("DELETE FROM clip_entries WHERE isPinned = 0")
    suspend fun clearUnpinnedClips()
}
