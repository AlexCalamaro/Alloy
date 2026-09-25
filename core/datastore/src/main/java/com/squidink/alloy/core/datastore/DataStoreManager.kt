package com.squidink.alloy.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alloy_preferences")

/**
 * Encrypted preferences DataStore manager handling module enable states and settings.
 */
@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {

    fun isModuleEnabled(moduleId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("module_enabled_$moduleId")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: true
        }
    }

    suspend fun setModuleEnabled(moduleId: String, enabled: Boolean) {
        val key = booleanPreferencesKey("module_enabled_$moduleId")
        context.dataStore.edit { preferences ->
            preferences[key] = enabled
        }
    }

    /**
     * Get the dynamic color (Material You) preference.
     * Defaults to true if not set.
     */
    fun getDynamicColor(): Flow<Boolean> {
        val key = booleanPreferencesKey("dynamic_color_enabled")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: true
        }
    }

    /**
     * Set the dynamic color (Material You) preference.
     */
    suspend fun setDynamicColor(enabled: Boolean) {
        val key = booleanPreferencesKey("dynamic_color_enabled")
        context.dataStore.edit { preferences ->
            preferences[key] = enabled
        }
    }

    fun getStringFlow(key: String): Flow<String?> {
        val preferencesKey = stringPreferencesKey(key)
        return context.dataStore.data.map { preferences ->
            preferences[preferencesKey]
        }
    }

    suspend fun setString(key: String, value: String) {
        val preferencesKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[preferencesKey] = value
        }
    }
}
