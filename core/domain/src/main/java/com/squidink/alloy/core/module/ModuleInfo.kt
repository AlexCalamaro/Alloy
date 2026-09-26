package com.squidink.alloy.core.module

/**
 * Data class containing metadata about a module.
 *
 * Provides information about a module for the module registry and discovery.
 * 
 * @deprecated Use [FeatureDefinition] instead. This class is kept for backward
 * compatibility during migration.
 */
@Deprecated("Use FeatureDefinition instead", ReplaceWith("FeatureDefinition", "com.squidink.alloy.core.feature.FeatureDefinition"))
data class ModuleInfo(
    /**
     * Unique identifier for the module.
     */
    val id: String,
    
    /**
     * Display name for the module.
     */
    val name: String,
    
    /**
     * Description of the module's functionality.
     */
    val description: String,
    
    /**
     * The screen route for navigation.
     */
    val screenRoute: String,
    
    /**
     * Icon resource ID for the module.
     */
    val iconResId: Int = 0,
    
    /**
     * Whether the module is enabled.
     */
    val isEnabled: Boolean = true,
    
    /**
     * Module version.
     */
    val version: String = "1.0.0",
    
    /**
     * Category for grouping modules.
     */
    val category: String = "General",
    
    /**
     * Sort order within the category.
     */
    val sortOrder: Int = 0
)

/**
 * Companion object with predefined module IDs.
 * 
 * @deprecated Use [FeatureIds] instead.
 */
@Deprecated("Use FeatureIds instead")
object ModuleIds {
    const val STATS_PILL = "stats_pill"
    const val CLIP = "clip"
    const val SCRATCH = "scratch"
    const val SCENES = "scenes"
}

/**
 * Companion object with predefined module info instances.
 * 
 * @deprecated Use [FeatureDefinition] and [FeatureIds] instead.
 */
@Deprecated("Use FeatureDefinition instead")
object ModuleInfos {
    val STATS_PILL = ModuleInfo(
        id = ModuleIds.STATS_PILL,
        name = "System Stats",
        description = "Monitor CPU and memory usage in real-time",
        screenRoute = "stats_pill",
        category = "System",
        sortOrder = 1
    )
    
    val CLIP = ModuleInfo(
        id = ModuleIds.CLIP,
        name = "Clipboard",
        description = "Manage and organize clipboard entries",
        screenRoute = "clip",
        category = "Productivity",
        sortOrder = 1
    )
    
    val SCRATCH = ModuleInfo(
        id = ModuleIds.SCRATCH,
        name = "Scratchpad",
        description = "Quick notes and scratchpad functionality",
        screenRoute = "scratch",
        category = "Productivity",
        sortOrder = 2
    )
    
    val SCENES = ModuleInfo(
        id = ModuleIds.SCENES,
        name = "Scenes",
        description = "Scene management and automation",
        screenRoute = "scenes",
        category = "Desktop",
        sortOrder = 1
    )
}
