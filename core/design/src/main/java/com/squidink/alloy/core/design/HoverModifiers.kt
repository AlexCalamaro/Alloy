package com.squidink.alloy.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color

/**
 * Extension modifier providing visual pointer hover state and highlight for desktop quality tier compliance.
 */
fun Modifier.desktopHover(
    hoverColor: Color? = null,
    onHoverChanged: (Boolean) -> Unit = {}
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    LaunchedEffect(isHovered) {
        onHoverChanged(isHovered)
    }

    val tint = hoverColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val modifier = if (isHovered) this.background(tint) else this

    modifier.hoverable(interactionSource)
}
