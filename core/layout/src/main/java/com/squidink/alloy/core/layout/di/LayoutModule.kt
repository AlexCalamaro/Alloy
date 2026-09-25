package com.squidink.alloy.core.layout.di

import com.squidink.alloy.core.layout.LayoutStateRepository
import com.squidink.alloy.core.datastore.DataStoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing layout-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object LayoutModule {
    
    /**
     * Provides the LayoutStateRepository singleton.
     */
    @Provides
    @Singleton
    fun provideLayoutStateRepository(dataStoreManager: DataStoreManager): LayoutStateRepository {
        return LayoutStateRepository(dataStoreManager)
    }
}
