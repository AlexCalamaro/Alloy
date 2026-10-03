package com.squidink.alloy.modules.llmhost

import com.squidink.alloy.modules.llmhost.domain.model.DownloadProgress
import com.squidink.alloy.modules.llmhost.domain.model.DownloadStatus
import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.HostStatus
import com.squidink.alloy.modules.llmhost.domain.model.InferenceResult
import com.squidink.alloy.modules.llmhost.domain.model.InstalledModel
import com.squidink.alloy.modules.llmhost.domain.model.LlmHostError
import com.squidink.alloy.modules.llmhost.domain.model.ModelFormat
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import com.squidink.alloy.modules.llmhost.domain.model.RecommendedModel
import com.squidink.alloy.modules.llmhost.domain.model.ServerConfig
import com.squidink.alloy.modules.llmhost.domain.model.StorageUsage
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of [ILlmHostRepository] for unit testing.
 */
class FakeLlmHostRepository : ILlmHostRepository {

    private val defaultModel = InstalledModel(
        fileName = "gemma-2-2b-it-gpu.litertlm",
        filePath = "/fake/models/gemma-2-2b-it-gpu.litertlm",
        sizeBytes = 1_500_000_000L,
        lastModified = 1000L,
        isLoaded = false,
        format = ModelFormat.LITERT,
        isActive = true
    )

    val hostStatusFlow = MutableStateFlow(HostStatus.STOPPED)
    val installedModelFlow = MutableStateFlow<InstalledModel?>(defaultModel)
    val installedModelsFlow = MutableStateFlow<List<InstalledModel>>(listOf(defaultModel))
    val downloadProgressFlow = MutableStateFlow(DownloadProgress())
    val storageUsageFlow = MutableStateFlow(
        StorageUsage(
            currentModelSizeBytes = 1_500_000_000L,
            totalModelsSizeBytes = 1_500_000_000L,
            availableStorageBytes = 64_000_000_000L
        )
    )
    private val errorsFlow = MutableSharedFlow<LlmHostError>(replay = 0)

    var currentServerConfig = ServerConfig(
        host = "127.0.0.1",
        port = 8787,
        authToken = "test-token-123",
        backend = EngineBackend.GPU,
        activeModelPath = defaultModel.filePath,
        tuning = ModelTuningConfig()
    )

    var shouldFailStart = false
    var shouldFailDownload = false
    var shouldFailInference = false

    override fun observeHostStatus(): Flow<HostStatus> = hostStatusFlow.asStateFlow()

    override fun getHostStatus(): HostStatus = hostStatusFlow.value

    override fun observeInstalledModel(): Flow<InstalledModel?> = installedModelFlow.asStateFlow()

    override fun observeInstalledModels(): Flow<List<InstalledModel>> = installedModelsFlow.asStateFlow()

    override fun observeDownloadProgress(): Flow<DownloadProgress> = downloadProgressFlow.asStateFlow()

    override fun observeStorageUsage(): Flow<StorageUsage> = storageUsageFlow.asStateFlow()

    override fun observeErrors(): Flow<LlmHostError> = errorsFlow.asSharedFlow()

    suspend fun emitError(error: LlmHostError) {
        errorsFlow.emit(error)
    }

    override fun getServerConfig(): ServerConfig = currentServerConfig

    override suspend fun updateServerConfig(port: Int, authToken: String, backend: EngineBackend) {
        currentServerConfig = currentServerConfig.copy(port = port, authToken = authToken, backend = backend)
    }

    override suspend fun updateTuningConfig(tuning: ModelTuningConfig) {
        currentServerConfig = currentServerConfig.copy(tuning = tuning)
    }

    override suspend fun selectActiveModel(filePath: String): Result<Unit> {
        val updated = installedModelsFlow.value.map { model ->
            val active = model.filePath == filePath
            model.copy(isActive = active)
        }
        installedModelsFlow.value = updated
        val active = updated.find { it.isActive }
        installedModelFlow.value = active
        currentServerConfig = currentServerConfig.copy(activeModelPath = active?.filePath)
        return Result.success(Unit)
    }

    override suspend fun deleteModel(filePath: String): Result<Unit> {
        val updated = installedModelsFlow.value.filterNot { it.filePath == filePath }
        installedModelsFlow.value = updated
        if (installedModelFlow.value?.filePath == filePath) {
            val nextActive = updated.firstOrNull()?.copy(isActive = true)
            installedModelFlow.value = nextActive
            currentServerConfig = currentServerConfig.copy(activeModelPath = nextActive?.filePath)
        }
        val remainingBytes = updated.sumOf { it.sizeBytes }
        storageUsageFlow.value = storageUsageFlow.value.copy(
            currentModelSizeBytes = installedModelFlow.value?.sizeBytes ?: 0L,
            totalModelsSizeBytes = remainingBytes
        )
        return Result.success(Unit)
    }

    override suspend fun startServer(): Result<Unit> {
        if (shouldFailStart) {
            val error = LlmHostError.ServerError("Failed to bind port")
            errorsFlow.emit(error)
            return Result.failure(IllegalStateException(error.message))
        }
        hostStatusFlow.value = HostStatus.RUNNING
        installedModelFlow.value = installedModelFlow.value?.copy(isLoaded = true)
        return Result.success(Unit)
    }

    override suspend fun stopServer() {
        hostStatusFlow.value = HostStatus.STOPPED
        installedModelFlow.value = installedModelFlow.value?.copy(isLoaded = false)
    }

    override suspend fun startDownload(url: String, hfToken: String?): Result<Unit> {
        if (shouldFailDownload) {
            val error = LlmHostError.DownloadError("Network timeout")
            errorsFlow.emit(error)
            downloadProgressFlow.value = DownloadProgress(
                status = DownloadStatus.FAILED,
                errorMessage = error.message
            )
            return Result.failure(IllegalStateException(error.message))
        }

        downloadProgressFlow.value = DownloadProgress(
            status = DownloadStatus.DOWNLOADING,
            bytesRead = 750_000_000L,
            totalBytes = 1_500_000_000L,
            progressPercent = 50f
        )
        return Result.success(Unit)
    }

    override suspend fun cancelDownload() {
        downloadProgressFlow.value = DownloadProgress(status = DownloadStatus.CANCELLED)
    }

    override suspend fun deleteInstalledModel(): Result<Unit> {
        installedModelsFlow.value = emptyList()
        installedModelFlow.value = null
        currentServerConfig = currentServerConfig.copy(activeModelPath = null)
        storageUsageFlow.value = storageUsageFlow.value.copy(
            currentModelSizeBytes = 0L,
            totalModelsSizeBytes = 0L
        )
        return Result.success(Unit)
    }

    override suspend fun runTestPrompt(prompt: String): Result<InferenceResult> {
        if (shouldFailInference) {
            val error = LlmHostError.InferenceError("Engine out of memory")
            errorsFlow.emit(error)
            return Result.failure(IllegalStateException(error.message))
        }

        return Result.success(
            InferenceResult(
                promptText = prompt,
                responseText = "An operating system manages computer hardware and software resources.",
                latencyMs = 150L,
                tokensPerSecond = 20.5f,
                tokenCount = 12
            )
        )
    }

    override fun getRecommendations(): List<RecommendedModel> = RecommendedModel.CURATED_MODELS
}
