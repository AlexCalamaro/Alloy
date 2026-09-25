package com.squidink.alloy.core.data.repository

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.squidink.alloy.core.datastore.DataStoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized settings repository that wraps DataStoreManager.
 *
 * Provides type-safe access to all application settings with:
 * - Strongly-typed preference keys
 * - Default values
 * - Flow-based observation
 * - Centralized key management
 *
 * All settings persistence logic is delegated to this repository.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStoreManager: DataStoreManager
) {
    // Module settings keys
    private val KEY_MODULE_ENABLED = "module_enabled_"
    
    // Stats module settings keys
    private val KEY_STATS_SHOW_PILL = "stats_show_pill"
    private val KEY_STATS_USE_PERCENTAGES = "stats_use_percentages"
    private val KEY_STATS_CORNER_POSITION = "stats_corner_position"
    
    // UI settings keys
    private val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
    
    // Network settings keys
    private val KEY_NETWORK_REFRESH_INTERVAL = "network_refresh_interval"
    
    // Cache settings keys
    private val KEY_CACHE_MAX_SIZE = "cache_max_size"
    private val KEY_CACHE_ENABLED = "cache_enabled"

    // ==================== Module Settings ====================

    /**
     * Check if a module is enabled.
     *
     * @param moduleId The unique module identifier
     * @return Flow emitting the enabled state
     */
    fun isModuleEnabled(moduleId: String): Flow<Boolean> {
        return dataStoreManager.isModuleEnabled(moduleId)
    }

    /**
     * Enable or disable a module.
     *
     * @param moduleId The unique module identifier
     * @param enabled The enabled state
     */
    suspend fun setModuleEnabled(moduleId: String, enabled: Boolean) {
        dataStoreManager.setModuleEnabled(moduleId, enabled)
    }

    // ==================== Stats Module Settings ====================

    /**
     * Observe whether the stats pill is visible.
     *
     * @return Flow emitting true if pill should be shown
     */
    fun observeShowPill(): Flow<Boolean> {
        return dataStoreManager.getBooleanFlow(booleanPreferencesKey(KEY_STATS_SHOW_PILL))
            .map { it ?: true }
    }

    /**
     * Set whether the stats pill should be visible.
     *
     * @param show The visibility state
     */
    suspend fun setShowPill(show: Boolean) {
        dataStoreManager.setBoolean(booleanPreferencesKey(KEY_STATS_SHOW_PILL), show)
    }

    /**
     * Observe whether to display values as percentages.
     *
     * @return Flow emitting true if percentages should be used
     */
    fun observeUsePercentages(): Flow<Boolean> {
        return dataStoreManager.getBooleanFlow(booleanPreferencesKey(KEY_STATS_USE_PERCENTAGES))
            .map { it ?: true }
    }

    /**
     * Set whether to display values as percentages.
     *
     * @param usePercentages The percentage display state
     */
    suspend fun setUsePercentages(usePercentages: Boolean) {
        dataStoreManager.setBoolean(booleanPreferencesKey(KEY_STATS_USE_PERCENTAGES), usePercentages)
    }

    /**
     * Observe the corner position for the stats pill.
     *
     * @return Flow emitting the corner position name
     */
    fun observeCornerPosition(): Flow<String> {
        return dataStoreManager.getStringFlow(KEY_STATS_CORNER_POSITION)
            .map { it ?: "TOP_RIGHT" }
    }

    /**
     * Set the corner position for the stats pill.
     *
     * @param position The corner position name (e.g., "TOP_RIGHT", "BOTTOM_LEFT")
     */
    suspend fun setCornerPosition(position: String) {
        dataStoreManager.setString(KEY_STATS_CORNER_POSITION, position)
    }

    // ==================== UI Settings ====================

    /**
     * Observe the dynamic color (Material You) preference.
     *
     * @return Flow emitting true if dynamic color is enabled
     */
    fun observeDynamicColor(): Flow<Boolean> {
        return dataStoreManager.getDynamicColor()
    }

    /**
     * Set the dynamic color (Material You) preference.
     *
     * @param enabled Whether dynamic color should be enabled
     */
    suspend fun setDynamicColor(enabled: Boolean) {
        dataStoreManager.setDynamicColor(enabled)
    }

    // ==================== Network Settings ====================

    /**
     * Observe the network refresh interval in milliseconds.
     *
     * @return Flow emitting the refresh interval
     */
    fun observeNetworkRefreshInterval(): Flow<Long> {
        return dataStoreManager.getStringFlow(KEY_NETWORK_REFRESH_INTERVAL)
            .map { it?.toLongOrNull() ?: 1000L }
    }

    /**
     * Set the network refresh interval in milliseconds.
     *
     * @param intervalMs The refresh interval in milliseconds
     */
    suspend fun setNetworkRefreshInterval(intervalMs: Long) {
        dataStoreManager.setString(KEY_NETWORK_REFRESH_INTERVAL, intervalMs.toString())
    }

    // ==================== Cache Settings ====================

    /**
     * Observe whether caching is enabled.
     *
     * @return Flow emitting true if caching is enabled
     */
    fun observeCacheEnabled(): Flow<Boolean> {
        return dataStoreManager.getBooleanFlow(booleanPreferencesKey(KEY_CACHE_ENABLED))
            .map { it ?: true }
    }

    /**
     * Set whether caching is enabled.
     *
     * @param enabled Whether caching should be enabled
     */
    suspend fun setCacheEnabled(enabled: Boolean) {
        dataStoreManager.setBoolean(booleanPreferencesKey(KEY_CACHE_ENABLED), enabled)
    }

    /**
     * Observe the maximum cache size.
     *
     * @return Flow emitting the maximum cache size
     */
    fun observeCacheMaxSize(): Flow<Int> {
        return dataStoreManager.getStringFlow(KEY_CACHE_MAX_SIZE)
            .map { it?.toIntOrNull() ?: 100 }
    }

    /**
     * Set the maximum cache size.
     *
     * @param maxSize The maximum number of items to cache
     */
    suspend fun setCacheMaxSize(maxSize: Int) {
        dataStoreManager.setString(KEY_CACHE_MAX_SIZE, maxSize.toString())
    }

    // ==================== Generic Settings ====================

    /**
     * Observe a string setting.
     *
     * @param key The preference key
     * @param defaultValue The default value if not set
     * @return Flow emitting the string value
     */
    fun observeString(key: String, defaultValue: String = ""): Flow<String> {
        return dataStoreManager.getStringFlow(key).map { it ?: defaultValue }
    }

    /**
     * Set a string setting.
     *
     * @param key The preference key
     * @param value The string value
     */
    suspend fun setString(key: String, value: String) {
        dataStoreManager.setString(key, value)
    }

    /**
     * Observe an int setting.
     *
     * @param key The preference key
     * @param defaultValue The default value if not set
     * @return Flow emitting the int value
     */
    fun observeInt(key: String, defaultValue: Int = 0): Flow<Int> {
        return dataStoreManager.getStringFlow(key)
            .map { it?.toIntOrNull() ?: defaultValue }
    }

    /**
     * Set an int setting.
     *
     * @param key The preference key
     * @param value The int value
     */
    suspend fun setInt(key: String, value: Int) {
        dataStoreManager.setString(key, value.toString())
    }

    /**
     * Observe a long setting.
     *
     * @param key The preference key
     * @param defaultValue The default value if not set
     * @return Flow emitting the long value
     */
    fun observeLong(key: String, defaultValue: Long = 0L): Flow<Long> {
        return dataStoreManager.getStringFlow(key)
            .map { it?.toLongOrNull() ?: defaultValue }
    }

    /**
     * Set a long setting.
     *
     * @param key The preference key
     * @param value The long value
     */
    suspend fun setLong(key: String, value: Long) {
        dataStoreManager.setString(key, value.toString())
    }

    /**
     * Observe a float setting.
     *
     * @param key The preference key
     * @param defaultValue The default value if not set
     * @return Flow emitting the float value
     */
    fun observeFloat(key: String, defaultValue: Float = 0f): Flow<Float> {
        return dataStoreManager.getStringFlow(key)
            .map { it?.toFloatOrNull() ?: defaultValue }
    }

    /**
     * Set a float setting.
     *
     * @param key The preference key
     * @param value The float value
     */
    suspend fun setFloat(key: String, value: Float) {
        dataStoreManager.setString(key, value.toString())
    }

    /**
     * Observe a boolean setting.
     *
     * @param key The preference key
     * @param defaultValue The default value if not set
     * @return Flow emitting the boolean value
     */
    fun observeBoolean(key: String, defaultValue: Boolean = false): Flow<Boolean> {
        return dataStoreManager.getStringFlow(key)
            .map { it?.toBoolean() ?: defaultValue }
    }

    /**
     * Set a boolean setting.
     *
     * @param key The preference key
     * @param value The boolean value
     */
    suspend fun setBoolean(key: String, value: Boolean) {
        dataStoreManager.setString(key, value.toString())
    }
}
