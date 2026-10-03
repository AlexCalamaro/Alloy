package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import javax.inject.Inject

/**
 * Use case to select an installed model as the active model for inference.
 */
class SelectActiveModelUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    suspend operator fun invoke(filePath: String): Result<Unit> {
        return repository.selectActiveModel(filePath)
    }
}
