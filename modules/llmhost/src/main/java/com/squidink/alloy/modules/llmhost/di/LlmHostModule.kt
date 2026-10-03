package com.squidink.alloy.modules.llmhost.di

import com.squidink.alloy.core.module.ILlmHostActions
import com.squidink.alloy.modules.llmhost.LlmHostActionsImpl
import com.squidink.alloy.modules.llmhost.data.LlmHostRepositoryImpl
import com.squidink.alloy.modules.llmhost.data.datasource.engine.ILlmEngine
import com.squidink.alloy.modules.llmhost.data.datasource.engine.MultiBackendLlmEngine
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for the LLM Host feature.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LlmHostModule {

    @Binds
    @Singleton
    abstract fun bindLlmHostRepository(impl: LlmHostRepositoryImpl): ILlmHostRepository

    @Binds
    @Singleton
    abstract fun bindLlmHostActions(impl: LlmHostActionsImpl): ILlmHostActions

    @Binds
    @Singleton
    abstract fun bindLlmEngine(impl: MultiBackendLlmEngine): ILlmEngine
}
