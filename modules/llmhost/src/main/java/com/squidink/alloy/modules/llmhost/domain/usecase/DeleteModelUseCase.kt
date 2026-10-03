package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import javax.inject.Inject

/**
 * Use case to delete an installed model and free storage space.
 */
class DeleteModelUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.deleteInstalledModel()
    }
}
