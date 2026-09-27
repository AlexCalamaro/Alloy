package com.squidink.alloy.modules.statspill.di

import com.squidink.alloy.core.data.datasource.BatteryDataSource
import com.squidink.alloy.core.data.datasource.DiskDataSource
import com.squidink.alloy.core.data.datasource.NetworkDataSource
import com.squidink.alloy.core.data.datasource.StatDataSource
import com.squidink.alloy.core.data.datasource.SystemStatsDataSource
import com.squidink.alloy.core.data.datasource.ThermalDataSource
import com.squidink.alloy.core.proc.SystemStatsReader
import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.StatType
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import com.squidink.alloy.modules.statspill.data.StatsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * DI module for Stats module dependencies.
 *
 * Provides bindings for:
 * - Stats repository interfaces and implementations
 * - Data sources for system, battery, network, disk, and thermal statistics
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
        systemStatsReader: SystemStatsReader
    ): SystemStatsDataSource {
        return SystemStatsDataSource(systemStatsReader)
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

    /**
     * Provides NetworkDataSource instance.
     */
    @Provides
    @Singleton
    fun provideNetworkDataSource(
        systemStatsReader: SystemStatsReader
    ): NetworkDataSource {
        return NetworkDataSource(systemStatsReader)
    }

    /**
     * Provides DiskDataSource instance.
     */
    @Provides
    @Singleton
    fun provideDiskDataSource(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): DiskDataSource {
        return DiskDataSource(context)
    }

    /**
     * Provides ThermalDataSource instance.
     */
    @Provides
    @Singleton
    fun provideThermalDataSource(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): ThermalDataSource {
        return ThermalDataSource(context)
    }

    /**
     * Provides a map of all stat data sources by category.
     * This enables the repository to coordinate multiple data sources generically.
     */
    @Provides
    @StatDataSources
    fun provideStatDataSourcesMap(
        system: SystemStatsDataSource,
        battery: BatteryDataSource,
        network: NetworkDataSource,
        disk: DiskDataSource,
        thermal: ThermalDataSource
    ): Map<StatCategory, @JvmSuppressWildcards StatDataSource<out StatType>> {
        return mapOf(
            StatCategory.SYSTEM to system,
            StatCategory.POWER to battery,
            StatCategory.NETWORK to network,
            StatCategory.STORAGE to disk,
            StatCategory.THERMAL to thermal
        )
    }
}

/**
 * Custom qualifier for marking the stat data sources map.
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class StatDataSources
