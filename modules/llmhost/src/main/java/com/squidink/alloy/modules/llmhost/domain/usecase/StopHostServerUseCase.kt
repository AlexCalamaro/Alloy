package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import javax.inject.Inject

/**
 * Use case to stop the localhost HTTP server and unload the model.
 */
class StopHostServerUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    suspend operator fun invoke() {
        repository.stopServer()
    }
}
