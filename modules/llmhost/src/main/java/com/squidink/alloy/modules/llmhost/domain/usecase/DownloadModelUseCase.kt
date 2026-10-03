package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import javax.inject.Inject

/**
 * Use case to download a LiteRT model from Hugging Face.
 */
class DownloadModelUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    suspend operator fun invoke(url: String, hfToken: String?): Result<Unit> {
        return repository.startDownload(url, hfToken)
    }
}
