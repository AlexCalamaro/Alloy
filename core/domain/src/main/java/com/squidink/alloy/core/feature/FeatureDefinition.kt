package com.squidink.alloy.core.feature

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class containing complete metadata for a feature.
 *
 * This is the single source of truth for all feature information,
 * replacing scattered definitions across the codebase.
 */
data class FeatureDefinition(
    /**
     * Unique identifier for the feature.
     */
    val id: String,
    
    /**
     * Display name for the feature.
     */
    val name: String,
    
    /**
     * Description of the feature's functionality.
     */
    val description: String,
    
    /**
     * Navigation route for the feature screen.
     */
    val screenRoute: String,
    
    /**
     * Category for grouping features in the UI.
     */
    val category: String,
    
    /**
     * Sort order within the category (lower = earlier).
     */
    val sortOrder: Int,
    
    /**
     * The icon to display for this feature.
     */
    val icon: ImageVector,
    
    /**
     * Optional lambda to create the FeatureDetail content.
     * Deferred to avoid circular dependencies.
     * If null, no detail pane will be shown.
     */
    val detailFeatureProvider: (() -> Any)? = null  // Using Any to avoid direct dependency
)

/**
 * Companion object with predefined feature IDs.
 */
object FeatureIds {
    const val STATS_PILL = "stats_pill"
    const val CLIP = "clip"
    const val SCRATCH = "scratch"
    const val SCENES = "scenes"
    const val SETTINGS = "settings"
}

/**
 * Companion object with predefined feature categories.
 */
object FeatureCategories {
    const val SYSTEM = "System"
    const val DESKTOP = "Desktop"
    const val PRODUCTIVITY = "Productivity"
}
