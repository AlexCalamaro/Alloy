package com.squidink.alloy.core.module

/**
 * Interface for Scenes module actions.
 *
 * Defines actions that can be triggered on the Scenes module from other parts of the app.
 */
interface IScenesActions : IModuleActions {
    /**
     * Navigate to a specific scene.
     */
    fun navigateToScene(sceneId: String)
    
    /**
     * Create a new scene.
     */
    fun createScene(): String
    
    /**
     * Save a scene.
     */
    fun saveScene(sceneId: String, sceneData: String)
    
    /**
     * Delete a scene.
     */
    fun deleteScene(sceneId: String)
    
    /**
     * Get all scenes.
     */
    fun getScenes(): List<String>
}
