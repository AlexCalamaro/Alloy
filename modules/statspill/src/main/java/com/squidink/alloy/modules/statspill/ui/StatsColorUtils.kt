package com.squidink.alloy.modules.statspill.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Calculates the background color based on usage percentage in the 0.0f..100.0f range.
 *
 * @param percentage The usage percentage (0.0 to 100.0)
 * @return Material Theme color matching usage level
 */
@Composable
fun calculatePercentageColor(percentage: Float): Color {
    val normalized = if (percentage in 0f..1f && percentage > 0f) percentage * 100f else percentage
    return when {
        normalized < 60f -> MaterialTheme.colorScheme.primaryContainer
        normalized < 85f -> Color(0xFFFFA726) // Orange Warning
        else -> MaterialTheme.colorScheme.error
    }
}

/**
 * Calculates the background color for battery based on level (0 to 100).
 */
@Composable
fun calculateBatteryColor(percentage: Int): Color {
    return when {
        percentage < 20 -> MaterialTheme.colorScheme.error
        percentage < 50 -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
}

/**
 * Calculates the background color for temperature readings in Celsius.
 */
@Composable
fun calculateTemperatureColor(maxTemp: Float?): Color {
    return when {
        maxTemp == null -> MaterialTheme.colorScheme.surfaceVariant
        maxTemp > 45f -> MaterialTheme.colorScheme.error
        maxTemp > 37f -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

/**
 * Calculate a color based on value and maximum using a green-to-red gradient.
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
