package com.squidink.alloy.modules.statspill.di

import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.modules.statspill.data.StatsRepositoryImpl
import com.squidink.alloy.core.proc.ProcReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI module for Stats module dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object StatsModule {
    
    /**
     * Provide IStatsRepository implementation.
     */
    @Provides
    @Singleton
    fun provideStatsRepository(procReader: ProcReader): IStatsRepository {
        return StatsRepositoryImpl(procReader)
    }
}
