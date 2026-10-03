package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.model.StorageUsage
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe storage usage of installed models and free disk space.
 */
class GetModelStorageUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    operator fun invoke(): Flow<StorageUsage> {
        return repository.observeStorageUsage()
    }
}
