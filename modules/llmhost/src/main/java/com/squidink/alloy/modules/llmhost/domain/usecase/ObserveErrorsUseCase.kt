package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.model.LlmHostError
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe domain errors.
 */
class ObserveErrorsUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    operator fun invoke(): Flow<LlmHostError> {
        return repository.observeErrors()
    }
}
