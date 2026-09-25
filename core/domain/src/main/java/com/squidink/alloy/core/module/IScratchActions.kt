package com.squidink.alloy.core.module

import com.squidink.alloy.core.domain.repository.Scratch

/**
 * Interface for Scratch module actions.
 *
 * Defines actions that can be triggered on the Scratch module from other parts of the app.
 */
interface IScratchActions : IModuleActions {
    /**
     * Navigate to a specific scratchpad.
     */
    fun navigateToScratch(scratchId: String)
    
    /**
     * Create a new scratchpad.
     */
    fun createScratchpad(): Scratch
    
    /**
     * Save a scratchpad.
     */
    fun saveScratchpad(scratch: Scratch)
    
    /**
     * Delete a scratchpad.
     */
    fun deleteScratchpad(scratchId: String)
    
    /**
     * Get all scratchpads.
     */
    fun getScratchpads(): List<Scratch>
}
