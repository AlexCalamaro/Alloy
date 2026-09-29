package com.squidink.alloy.modules.statspill.di

import com.squidink.alloy.core.module.IStatsActions
import com.squidink.alloy.modules.statspill.StatsActionsImpl
import com.squidink.alloy.modules.statspill.data.StatsRepositoryImpl
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for the Stats module.
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

    /**
     * Bind IStatsActions to StatsActionsImpl.
     */
    @Binds
    @Singleton
    abstract fun bindStatsActions(statsActionsImpl: StatsActionsImpl): IStatsActions
}
