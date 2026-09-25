package com.squidink.alloy.modules.statspill.di

import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.modules.statspill.data.StatsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI module for Stats module dependencies.
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
