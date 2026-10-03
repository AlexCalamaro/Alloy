package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.model.InferenceResult
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import javax.inject.Inject

/**
 * Use case to run a sample diagnostic prompt against the installed LiteRT model.
 */
class RunTestInferenceUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    suspend operator fun invoke(prompt: String): Result<InferenceResult> {
        return repository.runTestPrompt(prompt)
    }
}
