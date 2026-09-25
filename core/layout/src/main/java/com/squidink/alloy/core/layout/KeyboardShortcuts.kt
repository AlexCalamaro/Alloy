package com.squidink.alloy.core.layout

/**
 * Keyboard shortcuts for the three-pane layout.
 * Provides documentation for available keyboard commands.
 *
 * ## Available Shortcuts
 *
 * | Shortcut | Action |
 * |----------|--------|
 * | `M` | Toggle navigation pane |
 * | `D` | Toggle detail pane |
 * | `Arrow Right` | Navigate to next feature |
 * | `Arrow Left` | Navigate to previous feature |
 *
 * ## Usage
 *
 * Keyboard shortcuts are handled automatically in the DashboardScreen.
 * Focus is requested on app launch to enable keyboard navigation.
 *
 * ## Implementation Notes
 *
 * - Shortcuts use hardware key codes for compatibility
 * - Debouncing is applied to prevent rapid-fire triggers
 * - Focus management ensures shortcuts work when expected
 */
object KeyboardShortcuts {
    /**
     * Debounce time for keyboard shortcuts (ms)
     */
    const val DEBOUNCE_MS = 300
    
    /**
     * Key codes for layout commands
     */
    const val TOGGLE_NAVIGATION_KEY = android.view.KeyEvent.KEYCODE_M
    const val TOGGLE_DETAIL_KEY = android.view.KeyEvent.KEYCODE_D
    const val NEXT_FEATURE_KEY = android.view.KeyEvent.KEYCODE_DPAD_RIGHT
    const val PREVIOUS_FEATURE_KEY = android.view.KeyEvent.KEYCODE_DPAD_LEFT
}