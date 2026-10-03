package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import javax.inject.Inject

/**
 * Use case to cancel an ongoing model download.
 */
class CancelDownloadUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    suspend operator fun invoke() {
        repository.cancelDownload()
    }
}
