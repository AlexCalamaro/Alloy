package com.squidink.alloy.modules.scratch.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScratchDao {

    @Query("SELECT * FROM scratch_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<ScratchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ScratchEntity)

    @Query("DELETE FROM scratch_notes WHERE id = :id")
    suspend fun deleteNote(id: String)
}
