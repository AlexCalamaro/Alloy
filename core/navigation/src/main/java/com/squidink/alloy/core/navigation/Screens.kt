package com.squidink.alloy.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.ui.graphics.vector.ImageVector
import com.squidink.alloy.core.feature.FeatureIds
import com.squidink.alloy.core.feature.featureRegistry

/**
 * Sealed class defining all navigation routes in the app.
 *
 * Provides type-safe navigation routes and prevents typos.
 * 
 * Note: Screen metadata (icons, titles, categories) is now managed by the
 * FeatureRegistry. This class primarily serves as route constants.
 */
sealed class Screens(val route: String) {
    // Main tabs
    data object StatsPill : Screens("stats_pill")
    data object Clip : Screens("clip")
    data object Scratch : Screens("scratch")
    data object Scenes : Screens("scenes")
    
    // Detail routes (if needed in the future)
    data class ClipDetail(val clipId: String) : Screens("clip_detail/$clipId")
    data class ScratchDetail(val scratchId: String) : Screens("scratch_detail/$scratchId")
    
    companion object {
        // Default start destination
        const val START_DESTINATION = "stats_pill"
        
        // Navigation argument names
        const val ARG_CLIP_ID = "clipId"
        const val ARG_SCRATCH_ID = "scratchId"
        
        // Helper functions for route construction
        fun clipDetailRoute(clipId: String) = "clip_detail/$clipId"
        fun scratchDetailRoute(scratchId: String) = "scratch_detail/$scratchId"
        
        /**
         * Get all main feature screens (excluding detail routes).
         */
        fun getMainScreens(): List<Screens> {
            return listOf(StatsPill, Clip, Scratch, Scenes)
        }
    }
}

/**
 * Sealed class defining all navigation actions.
 *
 * Provides type-safe navigation between screens.
 */
sealed class NavActions {
    object NavigateToStatsPill : NavActions()
    object NavigateToClip : NavActions()
    object NavigateToScratch : NavActions()
    object NavigateToScenes : NavActions()
    
    data class NavigateToClipDetail(val clipId: String) : NavActions()
    data class NavigateToScratchDetail(val scratchId: String) : NavActions()
    
    companion object {
        fun getRoute(action: NavActions): String {
            return when (action) {
                is NavigateToStatsPill -> Screens.StatsPill.route
                is NavigateToClip -> Screens.Clip.route
                is NavigateToScratch -> Screens.Scratch.route
                is NavigateToScenes -> Screens.Scenes.route
                is NavigateToClipDetail -> Screens.clipDetailRoute(action.clipId)
                is NavigateToScratchDetail -> Screens.scratchDetailRoute(action.scratchId)
            }
        }
    }
}

/**
 * Extension function to get screen title from route.
 * 
 * Now delegates to FeatureRegistry for the actual title.
 */
fun String.getScreenTitle(): String {
    val featureRegistry = featureRegistry()
    return when (this) {
        Screens.StatsPill.route -> featureRegistry.getFeature(FeatureIds.STATS_PILL)?.name ?: "System Stats"
        Screens.Clip.route -> featureRegistry.getFeature(FeatureIds.CLIP)?.name ?: "Clipboard"
        Screens.Scratch.route -> featureRegistry.getFeature(FeatureIds.SCRATCH)?.name ?: "Scratchpad"
        Screens.Scenes.route -> featureRegistry.getFeature(FeatureIds.SCENES)?.name ?: "Scenes"
        else -> "Alloy"
    }
}

/**
 * Extension function to get navigation icon for a screen.
 * 
 * Now delegates to FeatureRegistry for the actual icon.
 */
fun Screens.getNavigationIcon(): ImageVector {
    val featureRegistry = featureRegistry()
    return when (this) {
        is Screens.StatsPill -> featureRegistry.getFeature(FeatureIds.STATS_PILL)?.icon 
            ?: Icons.Default.Dashboard
        is Screens.Clip -> featureRegistry.getFeature(FeatureIds.CLIP)?.icon 
            ?: Icons.Default.FolderZip
        is Screens.Scratch -> featureRegistry.getFeature(FeatureIds.SCRATCH)?.icon 
            ?: Icons.Default.Campaign
        is Screens.Scenes -> featureRegistry.getFeature(FeatureIds.SCENES)?.icon 
            ?: Icons.Default.EmojiEvents
        is Screens.ClipDetail, is Screens.ScratchDetail -> Icons.Default.Dashboard
    }
}

/**
 * Extension function to get feature ID from a screen.
 */
fun Screens.getFeatureId(): String {
    return when (this) {
        is Screens.StatsPill -> FeatureIds.STATS_PILL
        is Screens.Clip -> FeatureIds.CLIP
        is Screens.Scratch -> FeatureIds.SCRATCH
        is Screens.Scenes -> FeatureIds.SCENES
        is Screens.ClipDetail, is Screens.ScratchDetail -> ""
    }
}
