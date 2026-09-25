package com.squidink.alloy.core.permissions.di

import com.squidink.alloy.core.permissions.PermissionsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing PermissionsManager dependency.
 */
@Module
@InstallIn(SingletonComponent::class)
object PermissionsModule {

    @Provides
    @Singleton
    fun providePermissionsManager(): PermissionsManager {
        return PermissionsManager()
    }
}
