package com.squidink.alloy.modules.llmhost.data

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.llmhost.data.datasource.downloader.ModelDownloader
import com.squidink.alloy.modules.llmhost.data.datasource.downloader.ModelStorageManager
import com.squidink.alloy.modules.llmhost.data.datasource.engine.ILlmEngine
import com.squidink.alloy.modules.llmhost.data.datasource.server.LlmHttpServer
import com.squidink.alloy.modules.llmhost.domain.model.DownloadProgress
import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.HostStatus
import com.squidink.alloy.modules.llmhost.domain.model.InferenceResult
import com.squidink.alloy.modules.llmhost.domain.model.InstalledModel
import com.squidink.alloy.modules.llmhost.domain.model.LlmHostError
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import com.squidink.alloy.modules.llmhost.domain.model.RecommendedModel
import com.squidink.alloy.modules.llmhost.domain.model.ServerConfig
import com.squidink.alloy.modules.llmhost.domain.model.StorageUsage
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import com.squidink.alloy.modules.llmhost.service.LlmHostServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [ILlmHostRepository] supporting multi-model libraries
 * and dynamic engine backend dispatching (.litertlm and .gguf).
 */
@Singleton
class LlmHostRepositoryImpl @Inject constructor(
    private val engine: ILlmEngine,
    private val modelDownloader: ModelDownloader,
    private val storageManager: ModelStorageManager,
    private val httpServer: LlmHttpServer,
    private val serviceManager: LlmHostServiceManager
) : ILlmHostRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _hostStatus = MutableStateFlow(HostStatus.STOPPED)
    private val _installedModel = MutableStateFlow<InstalledModel?>(null)
    private val _installedModels = MutableStateFlow<List<InstalledModel>>(emptyList())
    private val _storageUsage = MutableStateFlow(StorageUsage())
    private val _errors = MutableSharedFlow<LlmHostError>(replay = 0)

    private var currentConfig = ServerConfig(
        host = "127.0.0.1",
        port = 8787,
        authToken = httpServer.getAuthToken(),
        backend = EngineBackend.GPU
    )

    init {
        refreshStorageAndModel()
    }

    override fun observeHostStatus(): Flow<HostStatus> = _hostStatus.asStateFlow()

    override fun getHostStatus(): HostStatus = _hostStatus.value

    override fun observeInstalledModel(): Flow<InstalledModel?> = _installedModel.asStateFlow()

    override fun observeInstalledModels(): Flow<List<InstalledModel>> = _installedModels.asStateFlow()

    override fun observeDownloadProgress(): Flow<DownloadProgress> = modelDownloader.progress

    override fun observeStorageUsage(): Flow<StorageUsage> = _storageUsage.asStateFlow()

    override fun observeErrors(): Flow<LlmHostError> = _errors.asSharedFlow()

    override fun getServerConfig(): ServerConfig = currentConfig

    override suspend fun updateServerConfig(port: Int, authToken: String, backend: EngineBackend) {
        currentConfig = currentConfig.copy(
            port = port,
            authToken = authToken,
            backend = backend
        )
        httpServer.setAuthToken(authToken)
    }

    override suspend fun updateTuningConfig(tuning: ModelTuningConfig) {
        currentConfig = currentConfig.copy(tuning = tuning)
        httpServer.setTuningConfig(tuning)
    }

    override suspend fun selectActiveModel(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        Logger.i(TAG, "Selecting active model: $filePath")
        currentConfig = currentConfig.copy(activeModelPath = filePath)

        val wasRunning = _hostStatus.value == HostStatus.RUNNING
        if (wasRunning) {
            stopServer()
        }

        refreshStorageAndModel()

        if (wasRunning) {
            startServer()
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun deleteModel(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (_installedModel.value?.filePath == filePath && _hostStatus.value == HostStatus.RUNNING) {
            stopServer()
        }

        val result = storageManager.deleteModel(filePath)
        if (currentConfig.activeModelPath == filePath) {
            currentConfig = currentConfig.copy(activeModelPath = null)
        }

        refreshStorageAndModel()
        result
    }

    override suspend fun startServer(): Result<Unit> = withContext(Dispatchers.IO) {
        _hostStatus.value = HostStatus.STARTING
        val model = storageManager.getInstalledModel(currentConfig.activeModelPath)
        if (model == null) {
            val error = LlmHostError.EngineError("No model installed. Please download a model from Hugging Face first.")
            _hostStatus.value = HostStatus.ERROR
            _errors.emit(error)
            return@withContext Result.failure(IllegalStateException(error.message))
        }

        // Initialize engine (MultiBackendLlmEngine dynamically routes based on model format)
        val initResult = engine.initialize(model.filePath, currentConfig.backend, currentConfig.tuning)
        if (initResult.isFailure) {
            val ex = initResult.exceptionOrNull()
            val error = LlmHostError.EngineError("Failed to initialize engine for ${model.fileName}: ${ex?.message}", ex)
            _hostStatus.value = HostStatus.ERROR
            _errors.emit(error)
            return@withContext Result.failure(ex ?: IllegalStateException("Engine init failed"))
        }

        // Configure server and start
        httpServer.setModelName(model.fileName)
        httpServer.setTuningConfig(currentConfig.tuning)
        val serverResult = httpServer.start(currentConfig.port)
        if (serverResult.isFailure) {
            val ex = serverResult.exceptionOrNull()
            val error = LlmHostError.ServerError("Failed to bind localhost server: ${ex?.message}", ex)
            engine.close()
            _hostStatus.value = HostStatus.ERROR
            _errors.emit(error)
            return@withContext Result.failure(ex ?: IllegalStateException("Server bind failed"))
        }

        serviceManager.startService()
        _hostStatus.value = HostStatus.RUNNING
        _installedModel.value = model.copy(isLoaded = true, isActive = true)
        Logger.i(TAG, "Host server started successfully on 127.0.0.1:${currentConfig.port} (Model: ${model.fileName}, Format: ${model.format})")
        Result.success(Unit)
    }

    override suspend fun stopServer() = withContext(Dispatchers.IO) {
        httpServer.stop()
        engine.close()
        serviceManager.stopService()
        _hostStatus.value = HostStatus.STOPPED
        _installedModel.value = _installedModel.value?.copy(isLoaded = false)
        Logger.i(TAG, "Host server stopped successfully")
    }

    override suspend fun startDownload(url: String, hfToken: String?): Result<Unit> = withContext(Dispatchers.IO) {
        if (url.isBlank()) {
            val error = LlmHostError.DownloadError("Model URL cannot be empty")
            _errors.emit(error)
            return@withContext Result.failure(IllegalArgumentException(error.message))
        }

        val fileName = parseFileNameFromUrl(url)
        val destinationFile = File(storageManager.modelsDir, fileName)

        val result = modelDownloader.download(url, hfToken, destinationFile)
        if (result.isSuccess) {
            currentConfig = currentConfig.copy(activeModelPath = destinationFile.absolutePath)
        }
        refreshStorageAndModel()

        if (result.isFailure) {
            val ex = result.exceptionOrNull()
            val error = LlmHostError.DownloadError("Download failed: ${ex?.message}", ex)
            _errors.emit(error)
        }

        result
    }

    override suspend fun cancelDownload() {
        modelDownloader.cancel()
        refreshStorageAndModel()
    }

    override suspend fun deleteInstalledModel(): Result<Unit> = withContext(Dispatchers.IO) {
        if (_hostStatus.value == HostStatus.RUNNING) {
            stopServer()
        }

        val result = storageManager.deleteModelFiles()
        currentConfig = currentConfig.copy(activeModelPath = null)
        refreshStorageAndModel()
        result
    }

    override suspend fun runTestPrompt(prompt: String): Result<InferenceResult> = withContext(Dispatchers.IO) {
        val model = storageManager.getInstalledModel(currentConfig.activeModelPath)
            ?: return@withContext Result.failure(IllegalStateException("No model installed for testing"))

        if (!engine.isInitialized || engine.loadedModelPath != model.filePath) {
            val initResult = engine.initialize(model.filePath, currentConfig.backend, currentConfig.tuning)
            if (initResult.isFailure) {
                val ex = initResult.exceptionOrNull()
                val error = LlmHostError.EngineError("Failed to initialize engine for test: ${ex?.message}", ex)
                _errors.emit(error)
                return@withContext Result.failure(ex ?: IllegalStateException("Engine init failed"))
            }
        }

        val result = engine.generate(prompt, currentConfig.tuning)
        if (result.isFailure) {
            val ex = result.exceptionOrNull()
            val error = LlmHostError.InferenceError("Inference failed: ${ex?.message}", ex)
            _errors.emit(error)
        }
        result
    }

    override fun getRecommendations(): List<RecommendedModel> {
        return RecommendedModel.CURATED_MODELS
    }

    private fun refreshStorageAndModel() {
        repositoryScope.launch {
            val allModels = storageManager.getInstalledModels(currentConfig.activeModelPath)
            val active = storageManager.getInstalledModel(currentConfig.activeModelPath)

            _installedModels.value = allModels.map { model ->
                model.copy(
                    isLoaded = engine.isInitialized && engine.loadedModelPath == model.filePath,
                    isActive = active?.filePath == model.filePath
                )
            }
            _installedModel.value = active?.copy(
                isLoaded = engine.isInitialized && engine.loadedModelPath == active.filePath,
                isActive = true
            )
            _storageUsage.value = storageManager.getStorageUsage(currentConfig.activeModelPath)
        }
    }

    private fun parseFileNameFromUrl(url: String): String {
        val cleanUrl = url.substringBefore("?").substringBefore("#")
        val lastSegment = cleanUrl.substringAfterLast("/")
        return if (lastSegment.isNotBlank() && lastSegment.contains(".")) {
            lastSegment
        } else {
            "model.gguf"
        }
    }

    companion object {
        private const val TAG = "LlmHostRepositoryImpl"
    }
}
