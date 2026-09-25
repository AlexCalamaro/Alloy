package com.squidink.alloy.core.layout

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Enum class defining screen size classes for responsive layouts.
 * Follows Material Design 3 window size class guidelines.
 */
enum class WindowSizeClass {
    /** Compact screens (phones, narrow views) - typically < 600dp */
    COMPACT,
    
    /** Medium screens (small tablets, large phones in landscape) - typically 600-840dp */
    MEDIUM,
    
    /** Expanded screens (tablets, desktops) - typically > 840dp */
    EXPANDED
}

/**
 * Determines the window size class based on the current configuration.
 * Uses screen width in dp to classify the layout tier.
 *
 * @return The appropriate WindowSizeClass for the current screen
 */
@Composable
@ReadOnlyComposable
fun deriveWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    return when {
        screenWidthDp < 600 -> WindowSizeClass.COMPACT
        screenWidthDp < 840 -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }
}

/**
 * Extension property to check if the current window size is compact.
 * Useful for conditional UI logic.
 */
@Composable
@ReadOnlyComposable
fun WindowSizeClass.isCompact(): Boolean = this == WindowSizeClass.COMPACT

/**
 * Extension property to check if the current window size is medium or larger.
 * Useful for showing/hiding navigation elements.
 */
@Composable
@ReadOnlyComposable
fun WindowSizeClass.isMediumOrExpanded(): Boolean = this == WindowSizeClass.MEDIUM || this == WindowSizeClass.EXPANDED

/**
 * Extension property to check if the current window size is expanded.
 * Useful for showing full three-pane layouts.
 */
@Composable
@ReadOnlyComposable
fun WindowSizeClass.isExpanded(): Boolean = this == WindowSizeClass.EXPANDED
