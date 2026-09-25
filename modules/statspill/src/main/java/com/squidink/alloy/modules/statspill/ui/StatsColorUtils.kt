package com.squidink.alloy.modules.statspill.ui

import androidx.compose.ui.graphics.Color

/**
 * Calculate a color based on value and maximum using the formula:
 * R = (value/maximum) * 255
 * G = (1 - (value/maximum)) * 255
 * B = 100
 *
 * This creates a conventional gradient from:
 * - Low values: Green-dominant (low R, high G) - safe
 * - High values: Red-dominant (high R, low G) - warning
 * - Blue stays constant at 100
 *
 * @param value The current value
 * @param maximum The maximum value for normalization
 * @return Color with RGB calculated based on the ratio
 */
fun calculateValueColor(value: Int, maximum: Int): Color {
    val ratio = if (maximum > 0) {
        (value.toFloat() / maximum).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    val r = (ratio * 255).toInt().coerceIn(0, 255)
    val g = ((1f - ratio) * 255).toInt().coerceIn(0, 255)
    val b = 100
    
    return Color(r / 255f, g / 255f, b / 255f)
}

/**
 * Calculate a color based on percentage value and maximum percentage.
 * Convenience wrapper for Float-based percentages.
 *
 * @param value The current percentage value (0.0 to 1.0)
 * @param maximum The maximum percentage value (typically 1.0)
 * @return Color with RGB calculated based on the ratio
 */
fun calculatePercentageColor(value: Float, maximum: Float = 1f): Color {
    val ratio = if (maximum > 0) {
        (value / maximum).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    val r = (ratio * 255).toInt().coerceIn(0, 255)
    val g = ((1f - ratio) * 255).toInt().coerceIn(0, 255)
    val b = 100
    
    return Color(r / 255f, g / 255f, b / 255f)
}
