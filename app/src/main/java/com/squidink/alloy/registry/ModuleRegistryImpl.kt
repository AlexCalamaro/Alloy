package com.squidink.alloy.registry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderZip
import com.squidink.alloy.core.common.ModuleInfo
import com.squidink.alloy.core.common.ModuleRegistry
import com.squidink.alloy.core.datastore.DataStoreManager
import com.squidink.alloy.core.feature.FeatureDefinition
import com.squidink.alloy.core.feature.FeatureIds
import com.squidink.alloy.core.feature.IFeatureRegistry
import com.squidink.alloy.core.feature.featureRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the feature registry that initializes all app features.
 *
 * This is the single source of truth for all feature metadata, including:
 * - Feature names, descriptions, and categories
 * - Navigation routes and icons
 * - Sort order and enabled states
 *
 * Note: ViewModel creation is handled via Hilt injection in the DashboardActivity
 * to ensure proper lifecycle management and dependency injection.
 */
@Singleton
class ModuleRegistryImpl
    @Inject
    constructor(
        private val dataStoreManager: DataStoreManager,
    ) : ModuleRegistry, IFeatureRegistry by featureRegistry() {

        init {
            initializeFeatures()
        }

        private fun initializeFeatures() {
            val featureRegistry = featureRegistry()

            // Register Stats Pill feature
            featureRegistry.registerFeature(
                FeatureDefinition(
                    id = FeatureIds.STATS_PILL,
                    name = "Stats Vitals",
                    description = "System CPU, RAM, thermals & overlay pill",
                    screenRoute = "stats_pill",
                    category = "System",
                    sortOrder = 1,
                    icon = Icons.Default.Dashboard
                )
            )

            // Register Clip feature
            featureRegistry.registerFeature(
                FeatureDefinition(
                    id = FeatureIds.CLIP,
                    name = "Clipboard Workbench",
                    description = "Searchable encrypted clipboard history",
                    screenRoute = "clip",
                    category = "Productivity",
                    sortOrder = 2,
                    icon = Icons.Default.FolderZip
                )
            )

            // Register Scratch feature
            featureRegistry.registerFeature(
                FeatureDefinition(
                    id = FeatureIds.SCRATCH,
                    name = "Pinned Scratchpad",
                    description = "Multi-instance notes, checklists & timers",
                    screenRoute = "scratch",
                    category = "Productivity",
                    sortOrder = 3,
                    icon = Icons.Default.Campaign
                )
            )

            // Register Scenes feature
            featureRegistry.registerFeature(
                FeatureDefinition(
                    id = FeatureIds.SCENES,
                    name = "Workspace Scenes",
                    description = "Scene launcher with launch bounds geometry",
                    screenRoute = "scenes",
                    category = "Desktop",
                    sortOrder = 4,
                    icon = Icons.Default.EmojiEvents
                )
            )

            // Read initial enabled states from DataStore
            runBlocking {
                val states = featureRegistry.getFeatures().associate { feature ->
                    feature.id to dataStoreManager.isModuleEnabled(feature.id).first()
                }
                // Update feature states with persisted values
                states.forEach { (id, enabled) ->
                    if (enabled != featureRegistry.isFeatureEnabled(id)) {
                        featureRegistry.featureStates.value = 
                            featureRegistry.featureStates.value.toMutableMap().apply {
                                this[id] = enabled
                            }
                    }
                }
            }
        }

        // ModuleRegistry interface implementation (legacy)
        override val moduleStates: StateFlow<Map<String, Boolean>>
            get() = featureStates

        override fun getRegisteredModules(): List<ModuleInfo> {
            return getFeatures().map { feature ->
                ModuleInfo(
                    id = feature.id,
                    name = feature.name,
                    description = feature.description,
                    category = feature.category
                )
            }
        }

        override suspend fun setModuleEnabled(moduleId: String, enabled: Boolean) {
            setFeatureEnabled(moduleId, enabled)
        }

        override fun isModuleEnabled(moduleId: String): Boolean {
            return isFeatureEnabled(moduleId)
        }
    }
