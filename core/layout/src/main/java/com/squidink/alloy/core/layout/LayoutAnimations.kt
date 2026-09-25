package com.squidink.alloy.core.layout

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntOffset

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

/**
 * Composable that animates a float value with the standard pane animation.
 *
 * @param targetValue The target value to animate to
 * @return State containing the animated value
 */
@Composable
fun animatePaneValue(targetValue: Float): State<Float> {
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = LayoutAnimations.PaneEnter
    )
}

/**
 * Composable that animates a float value with the crossfade animation.
 *
 * @param targetValue The target value to animate to
 * @return State containing the animated value
 */
@Composable
fun animateCrossfade(targetValue: Float): State<Float> {
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = LayoutAnimations.Crossfade
    )
}

/**
 * Animation specs for slide-in/out effects.
 * Used for navigation and detail pane transitions on compact screens.
 */
object SlideAnimations {
    const val SLIDE_DURATION_MS = 300
    
    val SlideIn: AnimationSpec<IntOffset> = TweenSpec(
        durationMillis = SLIDE_DURATION_MS,
        easing = FastOutSlowInEasing
    )
    
    val SlideOut: AnimationSpec<IntOffset> = TweenSpec(
        durationMillis = SLIDE_DURATION_MS,
        easing = FastOutSlowInEasing
    )
}
