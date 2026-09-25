package com.squidink.alloy.core.module

/**
 * Base interface for all module actions.
 *
 * Defines the contract for module-specific actions that can be triggered
 * from other parts of the app.
 */
interface IModuleActions {
    /**
     * Get the unique identifier for this module.
     */
    val moduleId: String
    
    /**
     * Get the display name for this module.
     */
    val displayName: String
    
    /**
     * Get the screen route for this module.
     */
    val screenRoute: String
}
