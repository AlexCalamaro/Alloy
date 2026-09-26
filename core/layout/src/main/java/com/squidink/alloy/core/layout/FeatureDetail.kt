package com.squidink.alloy.core.layout

import androidx.compose.runtime.Composable

/**
 * Interface for features that provide detail pane content.
 * Features can opt-out of showing a detail pane by returning false.
 *
 * Example implementation:
 * ```kotlin
 * class ClipFeature : FeatureDetail {
 *     override val showsDetailPane: Boolean = true
 *     
 *     @Composable
 *     override fun DetailContent() {
 *         ClipSettings()
 *     }
 * }
 * ```
 */
interface FeatureDetail {
    /**
     * Whether this feature should show a detail pane.
     * Set to false to opt-out of detail pane display.
     */
    val showsDetailPane: Boolean
    
    /**
     * The content to display in the detail pane.
     * Only called if [showsDetailPane] is true.
     * 
     * Default implementation provides empty content for features that don't need it.
     */
    @Composable
    fun DetailContent() {
        // Empty default implementation
    }
}

/**
 * Empty implementation of FeatureDetail for features that don't need detail panes.
 */
object NoDetailFeature : FeatureDetail {
    override val showsDetailPane: Boolean = false
    
    @Composable
    override fun DetailContent() {
        // No content
    }
}
