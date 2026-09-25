package com.squidink.alloy.modules.scratch.data

import com.squidink.alloy.core.domain.repository.IScratchRepository
import com.squidink.alloy.core.domain.repository.Scratch
import com.squidink.alloy.modules.scratch.db.ScratchDao
import com.squidink.alloy.modules.scratch.db.ScratchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [IScratchRepository] using Room DAO.
 *
 * This is the data layer implementation that handles:
 * - Database operations
 * - Thread dispatching
 * - Mapping between domain models and Room entities
 */
class ScratchRepositoryImpl @Inject constructor(
    private val scratchDao: ScratchDao
) : IScratchRepository {
    
    override fun getScratchpads(): Flow<List<Scratch>> {
        return scratchDao.getAllNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getScratchpadById(id: String): Flow<Scratch?> {
        return scratchDao.getNoteById(id).map { it?.toDomain() }
    }
    
    override suspend fun insertScratchpad(scratch: Scratch) {
        withContext(Dispatchers.IO) {
            scratchDao.insertNote(scratch.toEntity())
        }
    }
    
    override suspend fun updateScratchpad(scratch: Scratch) {
        withContext(Dispatchers.IO) {
            scratchDao.updateNote(scratch.content, scratch.updatedAt, scratch.id)
        }
    }
    
    override suspend fun deleteScratchpad(id: String) {
        withContext(Dispatchers.IO) {
            scratchDao.deleteNote(id)
        }
    }
    
    override suspend fun deleteAllScratchpads() {
        withContext(Dispatchers.IO) {
            scratchDao.deleteAllNotes()
        }
    }
}

// Extension functions for mapping between domain and entity models
private fun ScratchEntity.toDomain(): Scratch = Scratch(
    id = id,
    content = content,
    label = "Default Note",
    createdAt = updatedAt,
    updatedAt = updatedAt
)

private fun Scratch.toEntity(): ScratchEntity = ScratchEntity(
    id = id,
    content = content,
    updatedAt = updatedAt
)
