package com.squidink.alloy.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain model for a scratchpad entry.
 */
data class Scratch(
    val id: String,
    val content: String,
    val label: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Repository interface for scratchpad operations.
 */
interface IScratchRepository {
    /**
     * Observe all scratchpads as a Flow.
     *
     * @return Flow emitting the current list of scratchpads and updates
     */
    fun getScratchpads(): Flow<List<Scratch>>
    
    /**
     * Get a specific scratchpad by ID.
     *
     * @param id The scratchpad ID
     * @return Flow emitting the scratchpad or null if not found
     */
    fun getScratchpadById(id: String): Flow<Scratch?>
    
    /**
     * Insert a new scratchpad.
     *
     * @param scratch The scratchpad to insert
     */
    suspend fun insertScratchpad(scratch: Scratch)
    
    /**
     * Update an existing scratchpad.
     *
     * @param scratch The scratchpad with updated data
     */
    suspend fun updateScratchpad(scratch: Scratch)
    
    /**
     * Delete a scratchpad by ID.
     *
     * @param id The scratchpad ID to delete
     */
    suspend fun deleteScratchpad(id: String)
    
    /**
     * Delete all scratchpads.
     */
    suspend fun deleteAllScratchpads()
}
