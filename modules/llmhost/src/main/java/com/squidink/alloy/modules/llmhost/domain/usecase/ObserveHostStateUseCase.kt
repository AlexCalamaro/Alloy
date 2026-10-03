package com.squidink.alloy.modules.llmhost.domain.usecase

import com.squidink.alloy.modules.llmhost.domain.model.DownloadProgress
import com.squidink.alloy.modules.llmhost.domain.model.HostStateSnapshot
import com.squidink.alloy.modules.llmhost.domain.model.HostStatus
import com.squidink.alloy.modules.llmhost.domain.model.InstalledModel
import com.squidink.alloy.modules.llmhost.domain.model.StorageUsage
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case to observe combined host state.
 */
class ObserveHostStateUseCase @Inject constructor(
    private val repository: ILlmHostRepository
) {
    operator fun invoke(): Flow<HostStateSnapshot> {
        return combine(
            repository.observeHostStatus(),
            repository.observeInstalledModels(),
            repository.observeInstalledModel(),
            repository.observeDownloadProgress(),
            repository.observeStorageUsage()
        ) { status, installedModels, activeModel, downloadProgress, storageUsage ->
            HostStateSnapshot(
                status = status,
                installedModels = installedModels,
                activeModel = activeModel,
                downloadProgress = downloadProgress,
                storageUsage = storageUsage
            )
        }
    }
}
