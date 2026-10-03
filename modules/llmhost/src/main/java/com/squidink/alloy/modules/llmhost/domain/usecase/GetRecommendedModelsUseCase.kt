package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.model.RecommendedModel
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import javax.inject.Inject

/**
 * Use case to retrieve curated model recommendations for Googlebook OS.
 */
class GetRecommendedModelsUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    operator fun invoke(): List<RecommendedModel> {
        return repository.getRecommendations()
    }
}
