package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import javax.inject.Inject

/**
 * Use case to start the localhost HTTP server hosting the LiteRT model.
 */
class StartHostServerUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.startServer()
    }
}
