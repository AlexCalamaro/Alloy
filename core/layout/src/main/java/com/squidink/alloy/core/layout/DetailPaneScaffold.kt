package com.squidink.alloy.core.layout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val DetailPaneWidth = 360.dp

/**
 * Reusable detail pane scaffold for features.
 * Displays an overlay panel that slides in from the right with:
 * - A header bar with title and close button
 * - Content area for feature-specific settings/detail views
 * 
 * This is designed to be embedded within feature content, not orchestrated from the top level.
 * 
 * @param isOpen Whether the detail pane should be visible
 * @param onDismiss Callback to close the detail pane
 * @param title Title displayed in the header bar
 * @param modifier Modifier to apply to the root container
 * @param content The content to display inside the detail pane
 */
@Composable
fun DetailPaneScaffold(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val paneOffsetPx = remember(density) { with(density) { DetailPaneWidth.roundToPx() } }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { paneOffsetPx }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { paneOffsetPx }) + fadeOut()
    ) {
        // BackHandler to prevent accidental dismissal
        BackHandler {
            onDismiss()
        }
        
        // Positioning container - anchors the detail pane to the right edge
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // Detail pane content - fixed width panel aligned to the right
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(DetailPaneWidth)
                    .background(MaterialTheme.colorScheme.surface)
                    .align(Alignment.CenterEnd)
            ) {
                // Header bar with title and close button
                Surface(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 16.dp)
                        )
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }
                
                // Content area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}
