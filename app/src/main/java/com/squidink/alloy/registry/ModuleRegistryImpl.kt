package com.squidink.alloy.registry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import com.squidink.alloy.core.common.ModuleInfo
import com.squidink.alloy.core.common.ModuleRegistry
import com.squidink.alloy.core.common.di.ApplicationScope
import com.squidink.alloy.core.datastore.DataStoreManager
import com.squidink.alloy.core.feature.FeatureDefinition
import com.squidink.alloy.core.feature.FeatureIds
import com.squidink.alloy.core.feature.IFeatureRegistry
import com.squidink.alloy.core.feature.featureRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
        @ApplicationScope private val applicationScope: CoroutineScope,
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

            // Register RSS Reader feature
            featureRegistry.registerFeature(
                FeatureDefinition(
                    id = FeatureIds.RSS_READER,
                    name = "RSS Reader",
                    description = "Follow news & RSS feeds with intelligent caching",
                    screenRoute = "rss_reader",
                    category = "Productivity",
                    sortOrder = 2,
                    icon = Icons.Default.RssFeed
                )
            )

            // Register Scratch feature
            featureRegistry.registerFeature(
                FeatureDefinition(
                    id = FeatureIds.SCRATCH,
                    name = "Pinned Scratchpad",
                    description = "Multi-instance notes & encrypted lockbox",
                    screenRoute = "scratch",
                    category = "Productivity",
                    sortOrder = 3,
                    icon = Icons.Default.EditNote
                )
            )

            // Register Lists feature
            featureRegistry.registerFeature(
                FeatureDefinition(
                    id = FeatureIds.LISTS,
                    name = "Lists",
                    description = "Keep-style notes, tasks & checklists",
                    screenRoute = "lists",
                    category = "Productivity",
                    sortOrder = 4,
                    icon = Icons.Default.Checklist
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
                    sortOrder = 5,
                    icon = Icons.Default.EmojiEvents
                )
            )

            // Register Settings feature
            featureRegistry.registerFeature(
                FeatureDefinition(
                    id = FeatureIds.SETTINGS,
                    name = "Settings",
                    description = "App preferences & customization",
                    screenRoute = "settings",
                    category = "System",
                    sortOrder = 6,
                    icon = Icons.Default.Settings
                )
            )

            // Asynchronously sync enabled states from DataStore
            applicationScope.launch {
                featureRegistry.getFeatures().forEach { feature ->
                    launch {
                        dataStoreManager.isModuleEnabled(feature.id).collect { enabled ->
                            featureRegistry.featureStates.update { current ->
                                current + (feature.id to enabled)
                            }
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
