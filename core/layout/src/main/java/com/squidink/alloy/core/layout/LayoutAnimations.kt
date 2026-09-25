package com.squidink.alloy.core.layout

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec

/**
 * Centralized animation specifications for layout transitions.
 * All animations can be easily tweaked from this single location.
 */
object LayoutAnimations {
    
    /** Duration for pane entry/exit animations (ms) */
    const val PANE_DURATION_MS = 300
    
    /** Duration for crossfade transitions (ms) */
    const val CROSSFADE_DURATION_MS = 200
    
    /** Standard animation spec for pane transitions */
    val PaneEnter: AnimationSpec<Float> = TweenSpec(
        durationMillis = PANE_DURATION_MS,
        easing = FastOutSlowInEasing
    )
    
    val PaneExit: AnimationSpec<Float> = TweenSpec(
        durationMillis = PANE_DURATION_MS,
        easing = FastOutSlowInEasing
    )
    
    /** Animation spec for crossfade transitions */
    val Crossfade: AnimationSpec<Float> = TweenSpec(
        durationMillis = CROSSFADE_DURATION_MS,
        easing = FastOutSlowInEasing
    )
}
