package com.squidink.alloy.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
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
    data object RssReader : Screens("rss_reader")
    data object Scratch : Screens("scratch")
    data object Lists : Screens("lists")
    data object Scenes : Screens("scenes")
    data object Settings : Screens("settings")
    data object LlmHost : Screens("llm_host")
    
    // Detail routes (if needed in the future)
    data class ScratchDetail(val scratchId: String) : Screens("scratch_detail/$scratchId")
    
    companion object {
        // Default start destination
        const val START_DESTINATION = "stats_pill"
        
        // Navigation argument names
        const val ARG_SCRATCH_ID = "scratchId"
        
        // Helper functions for route construction
        fun scratchDetailRoute(scratchId: String) = "scratch_detail/$scratchId"
        
        /**
         * Get all main feature screens (excluding detail routes).
         */
        fun getMainScreens(): List<Screens> {
            return listOf(StatsPill, RssReader, Scratch, Lists, Scenes, Settings, LlmHost)
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
    object NavigateToRssReader : NavActions()
    object NavigateToScratch : NavActions()
    object NavigateToLists : NavActions()
    object NavigateToScenes : NavActions()
    object NavigateToSettings : NavActions()
    object NavigateToLlmHost : NavActions()
    
    data class NavigateToScratchDetail(val scratchId: String) : NavActions()
    
    companion object {
        fun getRoute(action: NavActions): String {
            return when (action) {
                is NavigateToStatsPill -> Screens.StatsPill.route
                is NavigateToRssReader -> Screens.RssReader.route
                is NavigateToScratch -> Screens.Scratch.route
                is NavigateToLists -> Screens.Lists.route
                is NavigateToScenes -> Screens.Scenes.route
                is NavigateToSettings -> Screens.Settings.route
                is NavigateToLlmHost -> Screens.LlmHost.route
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
        Screens.RssReader.route -> featureRegistry.getFeature(FeatureIds.RSS_READER)?.name ?: "RSS Reader"
        Screens.Scratch.route -> featureRegistry.getFeature(FeatureIds.SCRATCH)?.name ?: "Scratchpad"
        Screens.Lists.route -> featureRegistry.getFeature(FeatureIds.LISTS)?.name ?: "Lists"
        Screens.Scenes.route -> featureRegistry.getFeature(FeatureIds.SCENES)?.name ?: "Scenes"
        Screens.Settings.route -> featureRegistry.getFeature(FeatureIds.SETTINGS)?.name ?: "Settings"
        Screens.LlmHost.route -> featureRegistry.getFeature(FeatureIds.LLM_HOST)?.name ?: "LLM Host"
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
        is Screens.RssReader -> featureRegistry.getFeature(FeatureIds.RSS_READER)?.icon 
            ?: Icons.Default.RssFeed
        is Screens.Scratch -> featureRegistry.getFeature(FeatureIds.SCRATCH)?.icon 
            ?: Icons.Default.EditNote
        is Screens.Lists -> featureRegistry.getFeature(FeatureIds.LISTS)?.icon 
            ?: Icons.Default.Checklist
        is Screens.Scenes -> featureRegistry.getFeature(FeatureIds.SCENES)?.icon 
            ?: Icons.Default.EmojiEvents
        is Screens.Settings -> featureRegistry.getFeature(FeatureIds.SETTINGS)?.icon
            ?: Icons.Default.Settings
        is Screens.LlmHost -> featureRegistry.getFeature(FeatureIds.LLM_HOST)?.icon
            ?: Icons.Default.Memory
        is Screens.ScratchDetail -> Icons.Default.Dashboard
    }
}

/**
 * Extension function to get feature ID from a screen.
 */
fun Screens.getFeatureId(): String {
    return when (this) {
        is Screens.StatsPill -> FeatureIds.STATS_PILL
        is Screens.RssReader -> FeatureIds.RSS_READER
        is Screens.Scratch -> FeatureIds.SCRATCH
        is Screens.Lists -> FeatureIds.LISTS
        is Screens.Scenes -> FeatureIds.SCENES
        is Screens.Settings -> FeatureIds.SETTINGS
        is Screens.LlmHost -> FeatureIds.LLM_HOST
        is Screens.ScratchDetail -> ""
    }
}

