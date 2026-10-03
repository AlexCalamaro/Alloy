package com.squidink.alloy.modules.llmhost

import androidx.lifecycle.viewModelScope
import com.squidink.alloy.core.common.BaseViewModel
import com.squidink.alloy.core.common.UiAction
import com.squidink.alloy.core.common.UiEffect
import com.squidink.alloy.core.common.UiState
import com.squidink.alloy.modules.llmhost.domain.model.DownloadProgress
import com.squidink.alloy.modules.llmhost.domain.model.DownloadStatus
import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.HostStatus
import com.squidink.alloy.modules.llmhost.domain.model.InstalledModel
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import com.squidink.alloy.modules.llmhost.domain.model.RecommendedModel
import com.squidink.alloy.modules.llmhost.domain.model.ServerConfig
import com.squidink.alloy.modules.llmhost.domain.model.StorageUsage
import com.squidink.alloy.modules.llmhost.domain.model.TestInferenceState
import com.squidink.alloy.modules.llmhost.domain.repository.ILlmHostRepository
import com.squidink.alloy.modules.llmhost.domain.usecase.CancelDownloadUseCase
import com.squidink.alloy.modules.llmhost.domain.usecase.DeleteModelUseCase
import com.squidink.alloy.modules.llmhost.domain.usecase.DeleteSingleModelUseCase
import com.squidink.alloy.modules.llmhost.domain.usecase.DownloadModelUseCase
import com.squidink.alloy.modules.llmhost.domain.usecase.GetRecommendedModelsUseCase
import com.squidink.alloy.modules.llmhost.domain.usecase.ObserveErrorsUseCase
import com.squidink.alloy.modules.llmhost.domain.usecase.ObserveHostStateUseCase
import com.squidink.alloy.modules.llmhost.domain.usecase.RunTestInferenceUseCase
import com.squidink.alloy.modules.llmhost.domain.usecase.SelectActiveModelUseCase
import com.squidink.alloy.modules.llmhost.domain.usecase.StartHostServerUseCase
import com.squidink.alloy.modules.llmhost.domain.usecase.StopHostServerUseCase
import com.squidink.alloy.modules.llmhost.domain.usecase.UpdateTuningConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the LLM Host screen.
 */
data class LlmHostUiState(
    val hostStatus: HostStatus = HostStatus.STOPPED,
    val installedModel: InstalledModel? = null,
    val installedModels: List<InstalledModel> = emptyList(),
    val activeModel: InstalledModel? = null,
    val tuningConfig: ModelTuningConfig = ModelTuningConfig(),
    val downloadProgress: DownloadProgress = DownloadProgress(),
    val storageUsage: StorageUsage = StorageUsage(),
    val testInference: TestInferenceState = TestInferenceState(),
    val serverConfig: ServerConfig = ServerConfig(),
    val recommendedModels: List<RecommendedModel> = emptyList(),
    val selectedDownloadUrl: String = "",
    val hfTokenInput: String = "",
    val isSettingsOpen: Boolean = false,
    val errorMessage: String? = null
) : UiState {
    val isHostActive: Boolean get() = hostStatus == HostStatus.RUNNING
    val isDownloading: Boolean
        get() = downloadProgress.status == DownloadStatus.DOWNLOADING ||
            downloadProgress.status == DownloadStatus.CONNECTING
}

/**
 * UI Actions for user interactions on the LLM Host screen.
 */
sealed interface LlmHostUiAction : UiAction {
    data class ToggleHost(val enable: Boolean) : LlmHostUiAction
    data class UpdateUrlInput(val url: String) : LlmHostUiAction
    data class UpdateTokenInput(val token: String) : LlmHostUiAction
    data class StartDownload(val url: String, val hfToken: String?) : LlmHostUiAction
    data object CancelDownload : LlmHostUiAction
    data object DeleteModel : LlmHostUiAction
    data class SelectActiveModel(val model: InstalledModel) : LlmHostUiAction
    data class DeleteSpecificModel(val model: InstalledModel) : LlmHostUiAction
    data class UpdateTuningConfig(val tuning: ModelTuningConfig) : LlmHostUiAction
    data class RunTestPrompt(val prompt: String) : LlmHostUiAction
    data class UpdateServerConfig(val port: Int, val authToken: String, val backend: EngineBackend) : LlmHostUiAction
    data class SelectRecommendedModel(val model: RecommendedModel) : LlmHostUiAction
    data object OpenSettings : LlmHostUiAction
    data object DismissSettings : LlmHostUiAction
    data object DismissError : LlmHostUiAction
}

/**
 * UI Effects for one-shot events.
 */
sealed interface LlmHostUiEffect : UiEffect {
    data class ShowToast(val message: String) : LlmHostUiEffect
    data class CopyToClipboard(val label: String, val text: String) : LlmHostUiEffect
}

/**
 * Production MVI ViewModel for LiteRT and GGUF LLM Hosting and model management.
 */
@HiltViewModel
class LlmHostViewModel @Inject constructor(
    private val observeHostStateUseCase: ObserveHostStateUseCase,
    private val startHostServerUseCase: StartHostServerUseCase,
    private val stopHostServerUseCase: StopHostServerUseCase,
    private val downloadModelUseCase: DownloadModelUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val deleteModelUseCase: DeleteModelUseCase,
    private val selectActiveModelUseCase: SelectActiveModelUseCase,
    private val deleteSingleModelUseCase: DeleteSingleModelUseCase,
    private val updateTuningConfigUseCase: UpdateTuningConfigUseCase,
    private val runTestInferenceUseCase: RunTestInferenceUseCase,
    private val getRecommendedModelsUseCase: GetRecommendedModelsUseCase,
    private val observeErrorsUseCase: ObserveErrorsUseCase,
    private val repository: ILlmHostRepository
) : BaseViewModel<LlmHostUiState, LlmHostUiAction, LlmHostUiEffect>(LlmHostUiState()) {

    init {
        loadInitialData()
        observeHostState()
        observeErrors()
    }

    override fun onAction(action: LlmHostUiAction) {
        when (action) {
            is LlmHostUiAction.ToggleHost -> toggleHost(action.enable)
            is LlmHostUiAction.UpdateUrlInput -> updateState { it.copy(selectedDownloadUrl = action.url) }
            is LlmHostUiAction.UpdateTokenInput -> updateState { it.copy(hfTokenInput = action.token) }
            is LlmHostUiAction.StartDownload -> startDownload(action.url, action.hfToken)
            LlmHostUiAction.CancelDownload -> cancelDownload()
            LlmHostUiAction.DeleteModel -> deleteModel()
            is LlmHostUiAction.SelectActiveModel -> selectActiveModel(action.model)
            is LlmHostUiAction.DeleteSpecificModel -> deleteSpecificModel(action.model)
            is LlmHostUiAction.UpdateTuningConfig -> updateTuningConfig(action.tuning)
            is LlmHostUiAction.RunTestPrompt -> runTestPrompt(action.prompt)
            is LlmHostUiAction.UpdateServerConfig -> updateServerConfig(action.port, action.authToken, action.backend)
            is LlmHostUiAction.SelectRecommendedModel -> selectRecommendedModel(action.model)
            LlmHostUiAction.OpenSettings -> updateState { it.copy(isSettingsOpen = true) }
            LlmHostUiAction.DismissSettings -> updateState { it.copy(isSettingsOpen = false) }
            LlmHostUiAction.DismissError -> updateState { it.copy(errorMessage = null) }
        }
    }

    private fun loadInitialData() {
        val config = repository.getServerConfig()
        val recommendations = getRecommendedModelsUseCase()
        val defaultUrl = recommendations.firstOrNull()?.downloadUrl ?: ""

        updateState {
            it.copy(
                serverConfig = config,
                tuningConfig = config.tuning,
                recommendedModels = recommendations,
                selectedDownloadUrl = defaultUrl
            )
        }
    }

    private fun observeHostState() {
        viewModelScope.launch {
            observeHostStateUseCase().collect { snapshot ->
                updateState {
                    it.copy(
                        hostStatus = snapshot.status,
                        installedModel = snapshot.activeModel ?: snapshot.installedModel,
                        installedModels = snapshot.installedModels,
                        activeModel = snapshot.activeModel,
                        downloadProgress = snapshot.downloadProgress,
                        storageUsage = snapshot.storageUsage
                    )
                }
            }
        }
    }

    private fun observeErrors() {
        viewModelScope.launch {
            observeErrorsUseCase().collect { error ->
                updateState { it.copy(errorMessage = error.message) }
                sendEffect(LlmHostUiEffect.ShowToast(error.message))
            }
        }
    }

    private fun toggleHost(enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                if (uiState.value.activeModel == null && uiState.value.installedModel == null) {
                    sendEffect(LlmHostUiEffect.ShowToast("Please download or select a model first"))
                    return@launch
                }
                val result = startHostServerUseCase()
                if (result.isSuccess) {
                    val port = uiState.value.serverConfig.port
                    sendEffect(LlmHostUiEffect.ShowToast("LLM Host listening on 127.0.0.1:$port"))
                }
            } else {
                stopHostServerUseCase()
                sendEffect(LlmHostUiEffect.ShowToast("LLM Host stopped"))
            }
        }
    }

    private fun startDownload(url: String, hfToken: String?) {
        viewModelScope.launch {
            val result = downloadModelUseCase(url, hfToken)
            if (result.isSuccess) {
                sendEffect(LlmHostUiEffect.ShowToast("Model download completed successfully"))
            }
        }
    }

    private fun cancelDownload() {
        viewModelScope.launch {
            cancelDownloadUseCase()
            sendEffect(LlmHostUiEffect.ShowToast("Download cancelled"))
        }
    }

    private fun deleteModel() {
        viewModelScope.launch {
            val result = deleteModelUseCase()
            if (result.isSuccess) {
                sendEffect(LlmHostUiEffect.ShowToast("Model deleted"))
            }
        }
    }

    private fun runTestPrompt(prompt: String) {
        if (prompt.isBlank()) return

        viewModelScope.launch {
            updateState {
                it.copy(
                    testInference = TestInferenceState(isRunning = true)
                )
            }

            val result = runTestInferenceUseCase(prompt)
            result.fold(
                onSuccess = { inferenceResult ->
                    updateState {
                        it.copy(
                            testInference = TestInferenceState(
                                isRunning = false,
                                result = inferenceResult
                            )
                        )
                    }
                },
                onFailure = { error ->
                    updateState {
                        it.copy(
                            testInference = TestInferenceState(
                                isRunning = false,
                                errorMessage = error.localizedMessage ?: "Test inference failed"
                            )
                        )
                    }
                }
            )
        }
    }

    private fun updateServerConfig(port: Int, authToken: String, backend: EngineBackend) {
        viewModelScope.launch {
            repository.updateServerConfig(port, authToken, backend)
            updateState {
                it.copy(
                    serverConfig = ServerConfig(
                        port = port,
                        authToken = authToken,
                        backend = backend
                    )
                )
            }
            sendEffect(LlmHostUiEffect.ShowToast("Server configuration updated"))
        }
    }

    private fun selectActiveModel(model: InstalledModel) {
        viewModelScope.launch {
            val result = selectActiveModelUseCase(model.filePath)
            if (result.isSuccess) {
                sendEffect(LlmHostUiEffect.ShowToast("Active model switched to ${model.fileName}"))
            } else {
                sendEffect(LlmHostUiEffect.ShowToast("Failed to switch active model"))
            }
        }
    }

    private fun deleteSpecificModel(model: InstalledModel) {
        viewModelScope.launch {
            val result = deleteSingleModelUseCase(model.filePath)
            if (result.isSuccess) {
                sendEffect(LlmHostUiEffect.ShowToast("Deleted ${model.fileName}"))
            } else {
                sendEffect(LlmHostUiEffect.ShowToast("Failed to delete ${model.fileName}"))
            }
        }
    }

    private fun updateTuningConfig(tuning: ModelTuningConfig) {
        viewModelScope.launch {
            updateTuningConfigUseCase(tuning)
            updateState { it.copy(tuningConfig = tuning) }
            sendEffect(LlmHostUiEffect.ShowToast("Model parameters updated"))
        }
    }

    private fun selectRecommendedModel(model: RecommendedModel) {
        updateState { it.copy(selectedDownloadUrl = model.downloadUrl) }
        sendEffect(LlmHostUiEffect.ShowToast("Selected ${model.name}"))
    }
}
