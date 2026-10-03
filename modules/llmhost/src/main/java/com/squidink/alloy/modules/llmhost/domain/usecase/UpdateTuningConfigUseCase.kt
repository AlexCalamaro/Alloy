package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import javax.inject.Inject

/**
 * Use case to update model execution and generation tuning parameters.
 */
class UpdateTuningConfigUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    suspend operator fun invoke(tuning: ModelTuningConfig) {
        repository.updateTuningConfig(tuning)
    }
}
