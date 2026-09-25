package com.squidink.alloy.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.core.data.cache.MemoryCache
import com.squidink.alloy.core.data.datasource.LocalDataSource
import com.squidink.alloy.core.data.datasource.RemoteDataSource
import com.squidink.alloy.core.datastore.DataStoreManager
import com.squidink.alloy.core.proc.SystemStatsReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "alloy_preferences")

/**
 * Hilt DI module for data layer dependencies.
 *
 * Provides:
 * - DataStoreManager for preferences management
 * - SystemStatsReader for system statistics
 * - Base data source implementations
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private const val TAG = "DataModule"

    /**
     * Provides DataStoreManager instance.
     * Wraps Android DataStore for type-safe preferences access.
     */
    @Provides
    @Singleton
    fun provideDataStoreManager(
        @ApplicationContext context: Context,
        cryptoManager: com.squidink.alloy.core.datastore.CryptoManager
    ): DataStoreManager {
        return DataStoreManager(context, cryptoManager)
    }

    /**
     * Provides SystemStatsReader instance.
     * Reads system statistics using Android APIs (ActivityManager, Debug, TrafficStats).
     */
    @Provides
    @Singleton
    fun provideSystemStatsReader(
        @ApplicationContext context: Context
    ): SystemStatsReader {
        return SystemStatsReader(context)
    }

    /**
     * Provides a generic local data source for preferences-based data.
     * Use this for simple key-value preferences that need Flow observation.
     */
    @Provides
    @Singleton
    fun providePreferencesLocalDataSource(
        dataStoreManager: DataStoreManager
    ): PreferencesLocalDataSource {
        return PreferencesLocalDataSource(dataStoreManager)
    }

    /**
     * Provides MemoryCache instance with default size of 100 entries.
     * Can be customized via settings repository.
     */
    @Provides
    @Singleton
    fun provideMemoryCache(): MemoryCache<String, Any> {
        return MemoryCache(maxSize = 100)
    }
}

/**
 * Local data source implementation for DataStore preferences.
 *
 * Wraps DataStoreManager to provide LocalDataSource interface for
 * key-value preferences with Flow observation support.
 *
 * @param T The type of value stored
 */
class PreferencesLocalDataSource(
    private val dataStoreManager: DataStoreManager,
    private val key: String = "generic_preference"
) : LocalDataSource<String, String> {

    override suspend fun getById(id: String): String? {
        return dataStoreManager.getStringFlow(id).first()
    }

    override suspend fun getAll(): List<String> {
        // Preferences don't support list retrieval directly
        // This would need a custom implementation
        return emptyList()
    }

    override suspend fun insert(item: String): String {
        // For preferences, item is the key-value pair in JSON format
        dataStoreManager.setString(key, item)
        return key
    }

    override suspend fun update(item: String) {
        dataStoreManager.setString(key, item)
    }

    override suspend fun delete(id: String) {
        // Preferences don't have a direct delete, would need to set to null
        dataStoreManager.setString(id, "")
    }

    override fun observeById(id: String): Flow<String?> {
        return dataStoreManager.getStringFlow(id)
    }

    override fun observeAll(): Flow<List<String>> {
        // Preferences don't support list observation directly
        return flow { }
    }
}
