package com.squidink.alloy.registry

import com.squidink.alloy.core.common.ModuleInfo
import com.squidink.alloy.core.common.ModuleRegistry
import com.squidink.alloy.core.datastore.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModuleRegistryImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ModuleRegistry {

    private val registeredModulesList = listOf(
        ModuleInfo("statspill", "Stats Vitals", "System CPU, RAM, thermals & overlay pill", "System"),
        ModuleInfo("scenes", "Workspace Scenes", "Scene launcher with launch bounds geometry", "Desktop"),
        ModuleInfo("clip", "Clipboard Workbench", "Searchable encrypted clipboard history", "Productivity"),
        ModuleInfo("scratch", "Pinned Scratchpad", "Multi-instance notes, checklists & timers", "Productivity")
    )

    private val _moduleStates = MutableStateFlow(
        registeredModulesList.associate { it.id to true }
    )
    override val moduleStates: StateFlow<Map<String, Boolean>> = _moduleStates.asStateFlow()

    override fun getRegisteredModules(): List<ModuleInfo> = registeredModulesList

    override suspend fun setModuleEnabled(moduleId: String, enabled: Boolean) {
        dataStoreManager.setModuleEnabled(moduleId, enabled)
        val updatedMap = _moduleStates.value.toMutableMap()
        updatedMap[moduleId] = enabled
        _moduleStates.value = updatedMap
    }

    override fun isModuleEnabled(moduleId: String): Boolean {
        return _moduleStates.value[moduleId] ?: true
    }
}
