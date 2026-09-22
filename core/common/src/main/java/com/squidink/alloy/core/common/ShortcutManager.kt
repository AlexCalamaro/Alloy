package com.squidink.alloy.core.common

import kotlinx.coroutines.flow.StateFlow

/**
 * Keyboard shortcut definition binding a key combination to an action.
 */
data class ShortcutBinding(
    val id: String,
    val moduleId: String,
    val label: String,
    val defaultKeyCombination: String,
    val currentKeyCombination: String
)

/**
 * Central interface managing rebindable global and module-level keyboard shortcuts.
 */
interface ShortcutManager {

    /**
     * Flow of all registered shortcut bindings.
     */
    val registeredShortcuts: StateFlow<List<ShortcutBinding>>

    /**
     * Rebinds a shortcut to a new key combination.
     */
    suspend fun rebindShortcut(shortcutId: String, newKeyCombination: String)

    /**
     * Checks if a key combination conflicts with existing shortcuts or OS reserved keys.
     */
    fun checkConflict(keyCombination: String): Boolean
}
