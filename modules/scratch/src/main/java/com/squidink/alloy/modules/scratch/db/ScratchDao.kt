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

    @Query("SELECT * FROM scratch_notes WHERE id = :id")
    fun getNoteById(id: String): Flow<ScratchEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ScratchEntity)

    @Query("UPDATE scratch_notes SET content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNote(
        content: String,
        updatedAt: Long,
        id: String
    )

    @Query("DELETE FROM scratch_notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("DELETE FROM scratch_notes")
    suspend fun deleteAllNotes()
}
