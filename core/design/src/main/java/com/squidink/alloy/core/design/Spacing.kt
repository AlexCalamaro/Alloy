package com.squidink.alloy.core.design

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.dp

/**
 * Standard spacing values for consistent UI layout.
 *
 * Provides a centralized source for spacing values used throughout the app.
 */
@Stable
object Spacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 24.dp
    val XXLarge = 32.dp
}

/**
 * Application-wide constants for numeric values.
 *
 * Provides named constants instead of magic numbers for better maintainability.
 */
object AppConstants {
    const val POLLING_INTERVAL_MS = 1000L
    const val DEFAULT_CACHE_SIZE = 100
    const val BATTERY_SCALE_DEFAULT = 100
    const val MAX_HISTORY_ITEMS = 50
}
