package com.squidink.alloy.core.feature

/**
 * Base interface for all feature actions.
 *
 * Defines the contract for feature-specific actions that can be triggered
 * from other parts of the app.
 */
interface IFeatureActions {
    /**
     * Get the unique identifier for this feature.
     */
    val featureId: String
    
    /**
     * Get the display name for this feature.
     */
    val displayName: String
    
    /**
     * Get the screen route for this feature.
     */
    val screenRoute: String
}
