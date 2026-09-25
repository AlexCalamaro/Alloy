package com.squidink.alloy.core.module

import com.squidink.alloy.core.domain.repository.Clip

/**
 * Interface for Clip module actions.
 *
 * Defines actions that can be triggered on the Clip module from other parts of the app.
 */
interface IClipActions : IModuleActions {
    /**
     * Navigate to a specific clip.
     */
    fun navigateToClip(clipId: String)
    
    /**
     * Share a specific clip.
     */
    fun shareClip(clipId: String)
    
    /**
     * Delete a specific clip.
     */
    fun deleteClip(clipId: String)
    
    /**
     * Get all clips.
     */
    fun getClips(): List<Clip>
}
