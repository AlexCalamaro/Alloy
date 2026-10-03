package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import javax.inject.Inject

/**
 * Use case to delete a specific model file from local storage.
 */
class DeleteSingleModelUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    suspend operator fun invoke(filePath: String): Result<Unit> {
        return repository.deleteModel(filePath)
    }
}
