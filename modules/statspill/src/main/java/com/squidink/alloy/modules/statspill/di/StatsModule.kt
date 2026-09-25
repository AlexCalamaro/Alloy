package com.squidink.alloy.modules.statspill.di

import com.squidink.alloy.core.data.datasource.BatteryDataSource
import com.squidink.alloy.core.data.datasource.SystemStatsDataSource
import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.modules.statspill.data.StatsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI module for Stats module dependencies.
 *
 * Provides bindings for:
 * - Stats repository interfaces and implementations
 * - Data sources for system and battery statistics
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StatsModule {

    /**
     * Bind IStatsRepository to StatsRepositoryImpl.
     */
    @Binds
    @Singleton
    abstract fun bindStatsRepository(statsRepositoryImpl: StatsRepositoryImpl): IStatsRepository
}

/**
 * Provides data source instances for stats module.
 */
@Module
@InstallIn(SingletonComponent::class)
object StatsDataSourceModule {

    /**
     * Provides SystemStatsDataSource instance.
     */
    @Provides
    @Singleton
    fun provideSystemStatsDataSource(
        systemStatsReader: com.squidink.alloy.core.proc.SystemStatsReader,
        cache: com.squidink.alloy.core.data.cache.MemoryCache<String, Any>
    ): SystemStatsDataSource {
        return SystemStatsDataSource(systemStatsReader, cache)
    }

    /**
     * Provides BatteryDataSource instance.
     */
    @Provides
    @Singleton
    fun provideBatteryDataSource(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): BatteryDataSource {
        return BatteryDataSource(context)
    }
}
