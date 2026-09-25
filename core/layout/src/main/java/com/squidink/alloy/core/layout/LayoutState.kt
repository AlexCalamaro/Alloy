package com.squidink.alloy.core.layout

/**
 * Represents the current state of the three-pane layout.
 * Tracks visibility of navigation, content, and detail panes.
 *
 * @param navigationPaneOpen Whether the navigation pane is currently visible
 * @param detailPaneOpen Whether the detail pane is currently visible
 * @param currentFeature The identifier of the currently selected feature
 * @param detailFeature The identifier of the feature whose detail is shown (if any)
 */
data class AppLayoutState(
    val navigationPaneOpen: Boolean = false,
    val detailPaneOpen: Boolean = false,
    val currentFeature: String = "",
    val detailFeature: String? = null
) {
    companion object {
        /** Default state for a fresh layout */
        val DEFAULT = AppLayoutState(
            navigationPaneOpen = false,
            detailPaneOpen = false,
            currentFeature = "",
            detailFeature = null
        )
    }
}

/**
 * Events for modifying the layout state.
 * Used by LayoutController to process state transitions.
 */
sealed interface LayoutEvent {
    /** Open the navigation pane */
    data object OpenNavigation : LayoutEvent
    
    /** Close the navigation pane */
    data object CloseNavigation : LayoutEvent
    
    /** Toggle the navigation pane visibility */
    data object ToggleNavigation : LayoutEvent
    
    /** Open the detail pane for a specific feature */
    data class OpenDetail(val feature: String) : LayoutEvent
    
    /** Close the detail pane */
    data object CloseDetail : LayoutEvent
    
    /** Toggle the detail pane visibility */
    data object ToggleDetail : LayoutEvent
    
    /** Select a new feature (updates current feature, may close navigation on compact) */
    data class SelectFeature(val feature: String) : LayoutEvent
    
    /** Reset to default state */
    data object Reset : LayoutEvent
}
