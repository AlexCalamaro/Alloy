package com.squidink.alloy.core.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.desktopHover

/**
 * Navigation enter animation spec.
 */
private val NavigationEnter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn()

/**
 * Navigation exit animation spec.
 */
private val NavigationExit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()

/**
 * Detail enter animation spec.
 */
private val DetailEnter = slideInHorizontally(initialOffsetX = { it }) + fadeIn()

/**
 * Detail exit animation spec.
 */
private val DetailExit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()

/**
 * Navigation pane width in dp.
 */
private const val NAVIGATION_PANE_WIDTH_DP = 280

/**
 * Detail pane width in dp.
 */
private const val DETAIL_PANE_WIDTH_DP = 320

/**
 * Main scaffold for three-pane responsive layout.
 * Adapts layout based on window size class:
 * - COMPACT: Shows one pane at a time (navigation overlay or content or detail)
 * - MEDIUM: Shows navigation + content or content + detail
 * - EXPANDED: Shows all three panes simultaneously
 *
 * @param windowSizeClass The detected window size class
 * @param layoutController The layout state controller
 * @param navigationContent Navigation pane content composable
 * @param contentContent Main content pane composable
 * @param detailContent Detail pane content composable (nullable - can be hidden)
 * @param modifier Modifier to apply to the root container
 */
@Composable
fun ThreePaneScaffold(
    windowSizeClass: WindowSizeClass,
    layoutController: LayoutController,
    navigationContent: @Composable () -> Unit,
    contentContent: @Composable () -> Unit,
    detailContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val layoutState by layoutController.layoutState.collectAsState()
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (windowSizeClass) {
            WindowSizeClass.COMPACT -> {
                CompactLayout(
                    layoutState = layoutState,
                    layoutController = layoutController,
                    navigationContent = navigationContent,
                    contentContent = contentContent,
                    detailContent = detailContent
                )
            }
            WindowSizeClass.MEDIUM -> {
                MediumLayout(
                    layoutState = layoutState,
                    layoutController = layoutController,
                    navigationContent = navigationContent,
                    contentContent = contentContent,
                    detailContent = detailContent
                )
            }
            WindowSizeClass.EXPANDED -> {
                ExpandedLayout(
                    layoutState = layoutState,
                    navigationContent = navigationContent,
                    contentContent = contentContent,
                    detailContent = detailContent
                )
            }
        }
    }
}

/**
 * Compact layout: Shows one pane at a time with overlay navigation.
 * Uses animated visibility for smooth transitions.
 */
@Composable
private fun CompactLayout(
    layoutState: AppLayoutState,
    layoutController: LayoutController,
    navigationContent: @Composable () -> Unit,
    contentContent: @Composable () -> Unit,
    detailContent: @Composable (() -> Unit)?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Always show content as the base layer
        contentContent()
        
        // Navigation overlay when open with slide animation
        AnimatedVisibility(
            visible = layoutState.navigationPaneOpen,
            enter = NavigationEnter,
            exit = NavigationExit
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .desktopHover()
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .width(NAVIGATION_PANE_WIDTH_DP.dp)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        navigationContent()
                    }
                    
                    // Click outside to close navigation
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.0f))
                            .clickable { layoutController.closeNavigation() }
                    )
                }
            }
        }
        
        // Detail overlay when open and available with slide animation
        detailContent?.let { content ->
            AnimatedVisibility(
                visible = layoutState.detailPaneOpen,
                enter = DetailEnter,
                exit = DetailExit
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Main content area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            contentContent()
                        }
                        
                        // Detail pane
                        Box(
                            modifier = Modifier
                                .width(DETAIL_PANE_WIDTH_DP.dp)
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Medium layout: Shows navigation + content or content + detail.
 * Uses animated visibility for smooth transitions.
 */
@Composable
private fun MediumLayout(
    layoutState: AppLayoutState,
    layoutController: LayoutController,
    navigationContent: @Composable () -> Unit,
    contentContent: @Composable () -> Unit,
    detailContent: @Composable (() -> Unit)?
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Navigation pane (collapsible) with animation
        AnimatedVisibility(
            visible = layoutState.navigationPaneOpen,
            enter = NavigationEnter,
            exit = NavigationExit
        ) {
            Box(
                modifier = Modifier
                    .width(NAVIGATION_PANE_WIDTH_DP.dp)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                navigationContent()
            }
        }
        
        // Main content
        Box(
            modifier = if (layoutState.detailPaneOpen && detailContent != null) {
                Modifier.weight(1f).fillMaxSize()
            } else {
                Modifier.fillMaxSize()
            }
        ) {
            contentContent()
        }
        
        // Detail pane (optional) with animation
        detailContent?.let { content ->
            AnimatedVisibility(
                visible = layoutState.detailPaneOpen,
                enter = DetailEnter,
                exit = DetailExit
            ) {
                Box(
                    modifier = Modifier
                        .width(DETAIL_PANE_WIDTH_DP.dp)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Expanded layout: Shows all three panes simultaneously.
 * Uses animated visibility for detail pane transitions.
 */
@Composable
private fun ExpandedLayout(
    layoutState: AppLayoutState,
    navigationContent: @Composable () -> Unit,
    contentContent: @Composable () -> Unit,
    detailContent: @Composable (() -> Unit)?
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Navigation rail (persistent)
        Box(
            modifier = Modifier
                .width(280.dp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            navigationContent()
        }
        
        // Main content
        Box(
            modifier = if (detailContent != null) {
                Modifier.weight(1f).fillMaxSize()
            } else {
                Modifier.fillMaxSize()
            }
        ) {
            contentContent()
        }
        
        // Detail pane (optional) with animation
        detailContent?.let { content ->
            AnimatedVisibility(
                visible = true,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    content()
                }
            }
        }
    }
}
