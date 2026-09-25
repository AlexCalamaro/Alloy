package com.squidink.alloy.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain model for a clipboard clip.
 * This is the domain layer representation, independent of Room entities.
 */
data class Clip(
    val id: String,
    val textContent: String,
    val sourceApp: String,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Repository interface for clipboard clip operations.
 *
 * Defines the contract for clip data access without exposing
 * implementation details (Room DAO, DataStore, etc.)
 */
interface IClipRepository {
    /**
     * Observe all clips as a Flow.
     *
     * @return Flow emitting the current list of clips and updates
     */
    fun getClips(): Flow<List<Clip>>
    
    /**
     * Get a specific clip by ID.
     *
     * @param id The clip ID
     * @return Flow emitting the clip or null if not found
     */
    fun getClipById(id: String): Flow<Clip?>
    
    /**
     * Insert a new clip.
     *
     * @param clip The clip to insert
     */
    suspend fun insertClip(clip: Clip)
    
    /**
     * Update an existing clip.
     *
     * @param clip The clip with updated data
     */
    suspend fun updateClip(clip: Clip)
    
    /**
     * Delete a clip by ID.
     *
     * @param id The clip ID to delete
     */
    suspend fun deleteClip(id: String)
    
    /**
     * Pin a clip.
     *
     * @param id The clip ID to pin
     */
    suspend fun pinClip(id: String)
    
    /**
     * Unpin a clip.
     *
     * @param id The clip ID to unpin
     */
    suspend fun unpinClip(id: String)
    
    /**
     * Delete all clips.
     */
    suspend fun deleteAllClips()
}
