package com.squidink.alloy.core.layout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val DetailPaneWidth = 360.dp

/**
 * Reusable detail pane scaffold for features.
 * Displays an overlay panel that slides in from the right with:
 * - A dimmed background scrim with tap-to-dismiss behavior
 * - A header bar with title, divider, and close button
 * - Distinct visual contrast using surfaceContainerHigh, elevation, and border
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
            // Backdrop scrim to visually isolate the sidebar from the underlying content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            // Detail pane content - elevated card aligned to the right edge
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(DetailPaneWidth)
                    .align(Alignment.CenterEnd),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header bar with title and close button
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
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
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // Content area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
