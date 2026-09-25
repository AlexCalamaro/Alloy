package com.squidink.alloy.core.module

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Registry for managing all modules in the app.
 *
 * Provides centralized module discovery, registration, and lifecycle management.
 */
class ModuleRegistry private constructor() {
    
    private val _modules = MutableStateFlow<Map<String, ModuleInfo>>(emptyMap())
    val modules: Flow<Map<String, ModuleInfo>> = _modules.asStateFlow()
    
    private val _actionProviders = mutableMapOf<String, () -> IModuleActions>()
    
    companion object {
        @Volatile
        private var instance: ModuleRegistry? = null
        
        fun getInstance(): ModuleRegistry {
            return instance ?: synchronized(this) {
                instance ?: ModuleRegistry().also { instance = it }
            }
        }
    }
    
    /**
     * Register a module with its info.
     */
    fun registerModule(moduleInfo: ModuleInfo) {
        val current = _modules.value.toMutableMap()
        current[moduleInfo.id] = moduleInfo
        _modules.value = current
    }
    
    /**
     * Unregister a module.
     */
    fun unregisterModule(moduleId: String) {
        val current = _modules.value.toMutableMap()
        current.remove(moduleId)
        _modules.value = current
    }
    
    /**
     * Register an action provider for a module.
     */
    fun registerActionProvider(moduleId: String, provider: () -> IModuleActions) {
        _actionProviders[moduleId] = provider
    }
    
    /**
     * Get module info by ID.
     */
    fun getModule(moduleId: String): ModuleInfo? {
        return _modules.value[moduleId]
    }
    
    /**
     * Get all enabled modules.
     */
    fun getEnabledModules(): List<ModuleInfo> {
        return _modules.value.values.filter { it.isEnabled }
    }
    
    /**
     * Get actions for a module.
     */
    fun <T : IModuleActions> getActions(moduleId: String): T? {
        return _actionProviders[moduleId]?.invoke() as? T
    }
    
    /**
     * Check if a module is registered.
     */
    fun isModuleRegistered(moduleId: String): Boolean {
        return _modules.value.containsKey(moduleId)
    }
    
    /**
     * Clear all modules (for testing).
     */
    fun clear() {
        _modules.value = emptyMap()
        _actionProviders.clear()
    }
}

/**
 * Extension function to get the singleton instance.
 */
fun moduleRegistry(): ModuleRegistry = ModuleRegistry.getInstance()
