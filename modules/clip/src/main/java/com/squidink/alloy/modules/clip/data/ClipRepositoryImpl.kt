package com.squidink.alloy.modules.clip.data

import com.squidink.alloy.core.domain.repository.Clip
import com.squidink.alloy.core.domain.repository.IClipRepository
import com.squidink.alloy.modules.clip.db.ClipDao
import com.squidink.alloy.modules.clip.db.ClipEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [IClipRepository] using Room DAO.
 *
 * This is the data layer implementation that handles:
 * - Database operations
 * - Thread dispatching
 * - Mapping between domain models and Room entities
 */
class ClipRepositoryImpl @Inject constructor(
    private val clipDao: ClipDao
) : IClipRepository {
    
    override fun getClips(): Flow<List<Clip>> {
        return clipDao.getAllClips().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getClipById(id: String): Flow<Clip?> {
        return clipDao.getClipById(id).map { it?.toDomain() }
    }
    
    override suspend fun insertClip(clip: Clip) {
        withContext(Dispatchers.IO) {
            clipDao.insertClip(clip.toEntity())
        }
    }
    
    override suspend fun updateClip(clip: Clip) {
        withContext(Dispatchers.IO) {
            clipDao.updateClip(clip.id, clip.textContent, clip.isPinned)
        }
    }
    
    override suspend fun deleteClip(id: String) {
        withContext(Dispatchers.IO) {
            clipDao.deleteClip(id)
        }
    }
    
    override suspend fun pinClip(id: String) {
        withContext(Dispatchers.IO) {
            clipDao.updateClip(id, "", true)
        }
    }
    
    override suspend fun unpinClip(id: String) {
        withContext(Dispatchers.IO) {
            clipDao.updateClip(id, "", false)
        }
    }
    
    override suspend fun deleteAllClips() {
        withContext(Dispatchers.IO) {
            clipDao.deleteAllClips()
        }
    }
}

// Extension functions for mapping between domain and entity models
private fun ClipEntity.toDomain(): Clip = Clip(
    id = id,
    textContent = textContent,
    sourceApp = sourceApp,
    isPinned = isPinned,
    createdAt = timestamp,
    updatedAt = timestamp
)

private fun Clip.toEntity(): ClipEntity = ClipEntity(
    id = id,
    textContent = textContent,
    sourceApp = sourceApp,
    isPinned = isPinned,
    timestamp = updatedAt  // Use updatedAt as timestamp
)
