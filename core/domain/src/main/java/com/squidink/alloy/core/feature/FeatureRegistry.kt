package com.squidink.alloy.core.feature

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for the feature registry.
 *
 * Provides centralized feature discovery, registration, and state management.
 */
interface IFeatureRegistry {
    /**
     * Flow of all registered features.
     */
    val features: StateFlow<Map<String, FeatureDefinition>>
    
    /**
     * Flow of feature enabled states.
     */
    val featureStates: StateFlow<Map<String, Boolean>>
    
    /**
     * Get all registered features.
     */
    fun getFeatures(): List<FeatureDefinition>
    
    /**
     * Get a feature by ID.
     */
    fun getFeature(id: String): FeatureDefinition?
    
    /**
     * Get all features in a category, sorted by sortOrder.
     */
    fun getFeaturesByCategory(category: String): List<FeatureDefinition>
    
    /**
     * Get all enabled features.
     */
    fun getEnabledFeatures(): List<FeatureDefinition>
    
    /**
     * Get all enabled features, sorted by category and sortOrder.
     */
    fun getEnabledFeaturesSorted(): List<FeatureDefinition>
    
    /**
     * Check if a feature is enabled.
     */
    fun isFeatureEnabled(id: String): Boolean
    
    /**
     * Set a feature's enabled state.
     */
    suspend fun setFeatureEnabled(id: String, enabled: Boolean)
    
    /**
     * Get the categories that have enabled features.
     */
    fun getEnabledCategories(): List<String>
}

/**
 * Registry for managing all features in the app.
 *
 * Provides centralized feature discovery, registration, and lifecycle management.
 */
class FeatureRegistry private constructor() : IFeatureRegistry {
    
    override val features = kotlinx.coroutines.flow.MutableStateFlow<Map<String, FeatureDefinition>>(emptyMap())
    override val featureStates = kotlinx.coroutines.flow.MutableStateFlow<Map<String, Boolean>>(emptyMap())
    
    private val _actionProviders = mutableMapOf<String, () -> IFeatureActions>()
    
    companion object {
        @Volatile
        private var instance: FeatureRegistry? = null
        
        fun getInstance(): FeatureRegistry {
            return instance ?: synchronized(this) {
                instance ?: FeatureRegistry().also { instance = it }
            }
        }
    }
    
    /**
     * Register a feature with its definition.
     */
    fun registerFeature(featureDef: FeatureDefinition) {
        val current = features.value.toMutableMap()
        current[featureDef.id] = featureDef
        features.value = current
        
        // Initialize enabled state to true if not set
        val states = featureStates.value.toMutableMap()
        if (featureDef.id !in states) {
            states[featureDef.id] = true
            featureStates.value = states
        }
    }
    
    /**
     * Unregister a feature.
     */
    fun unregisterFeature(featureId: String) {
        val current = features.value.toMutableMap()
        current.remove(featureId)
        features.value = current
    }
    
    /**
     * Register an action provider for a feature.
     */
    fun registerActionProvider(featureId: String, provider: () -> IFeatureActions) {
        _actionProviders[featureId] = provider
    }
    
    /**
     * Get actions for a feature.
     */
    fun <T : IFeatureActions> getActions(featureId: String): T? {
        return _actionProviders[featureId]?.invoke() as? T
    }
    
    override fun getFeatures(): List<FeatureDefinition> {
        return features.value.values.toList()
    }
    
    override fun getFeature(id: String): FeatureDefinition? {
        return features.value[id]
    }
    
    override fun getFeaturesByCategory(category: String): List<FeatureDefinition> {
        return features.value.values
            .filter { it.category == category }
            .sortedBy { it.sortOrder }
    }
    
    override fun getEnabledFeatures(): List<FeatureDefinition> {
        return features.value.values.filter { feature ->
            featureStates.value[feature.id] != false
        }
    }
    
    override fun getEnabledFeaturesSorted(): List<FeatureDefinition> {
        return getEnabledFeatures()
            .sortedWith(compareBy({ it.category }, { it.sortOrder }))
    }
    
    override fun isFeatureEnabled(id: String): Boolean {
        return featureStates.value[id] != false
    }
    
    override suspend fun setFeatureEnabled(id: String, enabled: Boolean) {
        val states = featureStates.value.toMutableMap()
        states[id] = enabled
        featureStates.value = states
    }
    
    override fun getEnabledCategories(): List<String> {
        return getEnabledFeatures()
            .map { it.category }
            .distinct()
    }
    
    /**
     * Clear all features (for testing).
     */
    fun clear() {
        features.value = emptyMap()
        featureStates.value = emptyMap()
        _actionProviders.clear()
    }
}

/**
 * Extension function to get the singleton instance.
 */
fun featureRegistry(): FeatureRegistry = FeatureRegistry.getInstance()

/**
 * Extension function to get all features organized by category.
 */
fun Map<String, FeatureDefinition>.groupByCategory(): Map<String, List<FeatureDefinition>> {
    return values.groupBy { it.category }
}

/**
 * Extension function to get features sorted by category and sort order.
 */
fun List<FeatureDefinition>.sortedByCategoryAndOrder(): List<FeatureDefinition> {
    return sortedWith(
        compareBy(
            { it.category },
            { it.sortOrder }
        )
    )
}
