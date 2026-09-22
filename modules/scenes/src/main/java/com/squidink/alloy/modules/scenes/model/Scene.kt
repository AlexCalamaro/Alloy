package com.squidink.alloy.modules.scenes.model

/**
 * Window placement hint for desktop window positioning.
 */
enum class PlacementHint {
    LEFT,
    RIGHT,
    TOP_LEFT,
    TOP_RIGHT,
    CENTER,
    FULLSCREEN
}

/**
 * Single step within a Scene stack representing an application launch configuration.
 */
data class SceneStep(
    val id: String,
    val packageName: String,
    val activityName: String? = null,
    val placement: PlacementHint = PlacementHint.CENTER
)

/**
 * Named workspace Scene stacking multiple app windows in defined desktop bounds.
 */
data class Scene(
    val id: String,
    val name: String,
    val description: String = "",
    val shortcutKey: String? = null,
    val steps: List<SceneStep> = emptyList()
)
