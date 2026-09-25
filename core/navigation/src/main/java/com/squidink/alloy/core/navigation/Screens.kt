package com.squidink.alloy.core.navigation

/**
 * Sealed class defining all navigation routes in the app.
 *
 * Provides type-safe navigation routes and prevents typos.
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
 */
fun String.getScreenTitle(): String {
    return when (this) {
        Screens.StatsPill.route -> "System Stats"
        Screens.Clip.route -> "Clipboard"
        Screens.Scratch.route -> "Scratchpad"
        Screens.Scenes.route -> "Scenes"
        else -> "Alloy"
    }
}
