package com.squidink.alloy.modules.llmhost

import com.squidink.alloy.core.common.di.ApplicationScope
import com.squidink.alloy.core.feature.FeatureIds
import com.squidink.alloy.core.module.ILlmHostActions
import com.squidink.alloy.core.navigation.Screens
import com.squidink.alloy.modules.llmhost.domain.model.HostStatus
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ILlmHostActions] enabling inter-module LLM access across Alloy.
 */
@Singleton
class LlmHostActionsImpl @Inject constructor(
    private val repository: ILlmHostRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ILlmHostActions {

    override val moduleId: String = FeatureIds.LLM_HOST
    override val displayName: String = "LLM Host"
    override val screenRoute: String = Screens.LlmHost.route

    override fun startHost() {
        applicationScope.launch {
            repository.startServer()
        }
    }

    override fun stopHost() {
        applicationScope.launch {
            repository.stopServer()
        }
    }

    override fun isHostRunning(): Boolean = repository.getHostStatus() == HostStatus.RUNNING


    override suspend fun query(prompt: String): String {
        return repository.runTestPrompt(prompt).getOrThrow().responseText
    }
}
