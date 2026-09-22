package com.squidink.alloy.core.common

import kotlinx.coroutines.flow.StateFlow

/**
 * Metadata representation for an Alloy desktop module.
 */
data class ModuleInfo(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val isDynamicFeature: Boolean = false,
    val defaultEnabled: Boolean = true
)

/**
 * Central registry contract managing module lifecycles, states, and enable/disable toggles.
 */
interface ModuleRegistry {

    /**
     * Flow emitting map of Module ID to enabled status.
     */
    val moduleStates: StateFlow<Map<String, Boolean>>

    /**
     * Returns list of all registered modules.
     */
    fun getRegisteredModules(): List<ModuleInfo>

    /**
     * Toggles a module on or off. Toggling OFF immediately unregisters background workers and scope.
     */
    suspend fun setModuleEnabled(moduleId: String, enabled: Boolean)

    /**
     * Returns whether a module is currently enabled.
     */
    fun isModuleEnabled(moduleId: String): Boolean
}
