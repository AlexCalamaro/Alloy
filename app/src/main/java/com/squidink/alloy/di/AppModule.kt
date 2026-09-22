package com.squidink.alloy.di

import com.squidink.alloy.core.common.ModuleRegistry
import com.squidink.alloy.registry.ModuleRegistryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindModuleRegistry(impl: ModuleRegistryImpl): ModuleRegistry
}
