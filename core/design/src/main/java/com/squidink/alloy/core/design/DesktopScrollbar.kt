package com.squidink.alloy.core.design

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

/**
 * Desktop scrollbar indicator for mouse/trackpad scrolling parity.
 * Calculates dynamic thumb height and vertical offset reflecting the current [ScrollState].
 */
@Composable
fun DesktopVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    trackColor: Color = Color.Transparent
) {
    if (scrollState.maxValue <= 0 || scrollState.maxValue == Int.MAX_VALUE) {
        return
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(6.dp)
            .background(trackColor)
    ) {
        val totalTrackHeight = maxHeight
        val scrollFraction = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
        
        // Dynamic thumb height proportional to content length, clamped between 32dp and total height
        val thumbHeight = (totalTrackHeight * THUMB_HEIGHT_FRACTION).coerceIn(32.dp, totalTrackHeight)
        val availableTravel = totalTrackHeight - thumbHeight
        val thumbOffset = availableTravel * scrollFraction

        Box(
            modifier = Modifier
                .offset(y = thumbOffset)
                .height(thumbHeight)
                .width(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(thumbColor)
        )
    }
}

private const val THUMB_HEIGHT_FRACTION = 0.25f

