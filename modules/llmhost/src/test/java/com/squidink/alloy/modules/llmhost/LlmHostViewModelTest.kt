package com.squidink.alloy.modules.llmhost

import app.cash.turbine.test
import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.HostStatus
import com.squidink.alloy.modules.llmhost.domain.model.InstalledModel
import com.squidink.alloy.modules.llmhost.domain.model.LlmHostError
import com.squidink.alloy.modules.llmhost.domain.model.ModelFormat
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import com.squidink.alloy.modules.llmhost.domain.model.RecommendedModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LlmHostViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeLlmHostRepository
    private lateinit var viewModel: LlmHostViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeLlmHostRepository()
        viewModel = createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LlmHostViewModel {
        return LlmHostViewModel(
            observeHostStateUseCase = ObserveHostStateUseCase(fakeRepository),
            startHostServerUseCase = StartHostServerUseCase(fakeRepository),
            stopHostServerUseCase = StopHostServerUseCase(fakeRepository),
            downloadModelUseCase = DownloadModelUseCase(fakeRepository),
            cancelDownloadUseCase = CancelDownloadUseCase(fakeRepository),
            deleteModelUseCase = DeleteModelUseCase(fakeRepository),
            selectActiveModelUseCase = SelectActiveModelUseCase(fakeRepository),
            deleteSingleModelUseCase = DeleteSingleModelUseCase(fakeRepository),
            updateTuningConfigUseCase = UpdateTuningConfigUseCase(fakeRepository),
            runTestInferenceUseCase = RunTestInferenceUseCase(fakeRepository),
            getRecommendedModelsUseCase = GetRecommendedModelsUseCase(fakeRepository),
            observeErrorsUseCase = ObserveErrorsUseCase(fakeRepository),
            repository = fakeRepository
        )
    }

    @Test
    fun `initialization loads recommended models and server config`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(HostStatus.STOPPED, state.hostStatus)
        assertNotNull(state.installedModel)
        assertEquals("gemma-2-2b-it-gpu.litertlm", state.installedModel?.fileName)
        assertTrue(state.installedModels.isNotEmpty())
        assertTrue(state.recommendedModels.isNotEmpty())
        assertEquals(8787, state.serverConfig.port)
    }

    @Test
    fun `ToggleHost true starts server and emits toast`() = runTest {
        viewModel.effect.test {
            viewModel.onAction(LlmHostUiAction.ToggleHost(true))

            assertEquals(HostStatus.RUNNING, viewModel.uiState.value.hostStatus)
            assertTrue(viewModel.uiState.value.isHostActive)
            val effect = awaitItem()
            assertTrue(effect is LlmHostUiEffect.ShowToast)
        }
    }

    @Test
    fun `ToggleHost false stops server and emits toast`() = runTest {
        viewModel.effect.test {
            viewModel.onAction(LlmHostUiAction.ToggleHost(true))
            val startToast = awaitItem()
            assertTrue(startToast is LlmHostUiEffect.ShowToast)

            viewModel.onAction(LlmHostUiAction.ToggleHost(false))
            assertEquals(HostStatus.STOPPED, viewModel.uiState.value.hostStatus)
            assertFalse(viewModel.uiState.value.isHostActive)
            val stopToast = awaitItem()
            assertTrue(stopToast is LlmHostUiEffect.ShowToast)
        }
    }

    @Test
    fun `UpdateUrlInput and UpdateTokenInput update state fields`() = runTest {
        viewModel.onAction(LlmHostUiAction.UpdateUrlInput("https://huggingface.co/custom/model.gguf"))
        viewModel.onAction(LlmHostUiAction.UpdateTokenInput("hf_custom_secret"))

        assertEquals("https://huggingface.co/custom/model.gguf", viewModel.uiState.value.selectedDownloadUrl)
        assertEquals("hf_custom_secret", viewModel.uiState.value.hfTokenInput)
    }

    @Test
    fun `StartDownload triggers download and emits completion toast`() = runTest {
        viewModel.effect.test {
            viewModel.onAction(
                LlmHostUiAction.StartDownload(
                    url = "https://huggingface.co/test/model.litertlm",
                    hfToken = null
                )
            )

            assertEquals(50f, viewModel.uiState.value.downloadProgress.progressPercent)
            val effect = awaitItem()
            assertTrue(effect is LlmHostUiEffect.ShowToast)
        }
    }

    @Test
    fun `CancelDownload resets download progress and emits toast`() = runTest {
        viewModel.effect.test {
            viewModel.onAction(LlmHostUiAction.CancelDownload)
            val effect = awaitItem()
            assertTrue(effect is LlmHostUiEffect.ShowToast)
        }
    }

    @Test
    fun `DeleteModel deletes model and clears installedModel state`() = runTest {
        assertNotNull(viewModel.uiState.value.installedModel)

        viewModel.effect.test {
            viewModel.onAction(LlmHostUiAction.DeleteModel)

            assertNull(viewModel.uiState.value.installedModel)
            val effect = awaitItem()
            assertTrue(effect is LlmHostUiEffect.ShowToast)
        }
    }

    @Test
    fun `SelectActiveModel switches active model and emits toast`() = runTest {
        val ggufModel = InstalledModel(
            fileName = "qwen2.5-7b-instruct-q4_k_m.gguf",
            filePath = "/fake/models/qwen2.5-7b-instruct-q4_k_m.gguf",
            sizeBytes = 4_680_000_000L,
            lastModified = 2000L,
            format = ModelFormat.GGUF,
            isActive = false
        )
        fakeRepository.installedModelsFlow.value = fakeRepository.installedModelsFlow.value + ggufModel

        viewModel.effect.test {
            viewModel.onAction(LlmHostUiAction.SelectActiveModel(ggufModel))

            assertEquals("qwen2.5-7b-instruct-q4_k_m.gguf", viewModel.uiState.value.activeModel?.fileName)
            val effect = awaitItem()
            assertTrue(effect is LlmHostUiEffect.ShowToast)
        }
    }

    @Test
    fun `DeleteSpecificModel deletes model and emits toast`() = runTest {
        val ggufModel = InstalledModel(
            fileName = "smollm2-1.7b-instruct-q4_k_m.gguf",
            filePath = "/fake/models/smollm2-1.7b-instruct-q4_k_m.gguf",
            sizeBytes = 1_060_000_000L,
            lastModified = 3000L,
            format = ModelFormat.GGUF,
            isActive = false
        )
        fakeRepository.installedModelsFlow.value = fakeRepository.installedModelsFlow.value + ggufModel

        viewModel.effect.test {
            viewModel.onAction(LlmHostUiAction.DeleteSpecificModel(ggufModel))

            val effect = awaitItem()
            assertTrue(effect is LlmHostUiEffect.ShowToast)
            assertTrue(viewModel.uiState.value.installedModels.none { it.filePath == ggufModel.filePath })
        }
    }

    @Test
    fun `UpdateTuningConfig updates state and emits toast`() = runTest {
        val newTuning = ModelTuningConfig(
            temperature = 0.2f,
            maxTokens = 2048,
            contextSize = 4096,
            topP = 0.95f,
            threadCount = 4
        )

        viewModel.effect.test {
            viewModel.onAction(LlmHostUiAction.UpdateTuningConfig(newTuning))

            assertEquals(0.2f, viewModel.uiState.value.tuningConfig.temperature)
            assertEquals(2048, viewModel.uiState.value.tuningConfig.maxTokens)
            assertEquals(4096, viewModel.uiState.value.tuningConfig.contextSize)
            assertEquals(0.95f, viewModel.uiState.value.tuningConfig.topP)
            assertEquals(4, viewModel.uiState.value.tuningConfig.threadCount)

            val effect = awaitItem()
            assertTrue(effect is LlmHostUiEffect.ShowToast)
        }
    }

    @Test
    fun `RunTestPrompt updates testInference state with latency and result`() = runTest {
        viewModel.onAction(LlmHostUiAction.RunTestPrompt("Explain operating systems"))

        val testInference = viewModel.uiState.value.testInference
        assertFalse(testInference.isRunning)
        assertNotNull(testInference.result)
        assertEquals("Explain operating systems", testInference.result?.promptText)
        assertEquals(150L, testInference.result?.latencyMs)
        assertTrue((testInference.result?.tokensPerSecond ?: 0f) > 0f)
    }

    @Test
    fun `UpdateServerConfig updates state and repository config`() = runTest {
        viewModel.effect.test {
            viewModel.onAction(
                LlmHostUiAction.UpdateServerConfig(
                    port = 9090,
                    authToken = "new-token",
                    backend = EngineBackend.CPU
                )
            )

            assertEquals(9090, viewModel.uiState.value.serverConfig.port)
            assertEquals("new-token", viewModel.uiState.value.serverConfig.authToken)
            assertEquals(EngineBackend.CPU, viewModel.uiState.value.serverConfig.backend)
            val effect = awaitItem()
            assertTrue(effect is LlmHostUiEffect.ShowToast)
        }
    }

    @Test
    fun `SelectRecommendedModel populates download URL input`() = runTest {
        val model = RecommendedModel(
            id = "qwen-2.5-7b-gguf",
            name = "Qwen 2.5 7B Instruct (GGUF)",
            description = "Test model",
            modelSizeDisplay = "4.68 GB",
            downloadUrl = "https://huggingface.co/test/qwen.gguf",
            quantization = "Q4_K_M",
            hardwareRecommendation = "All devices",
            requiresHfToken = false,
            format = ModelFormat.GGUF
        )

        viewModel.effect.test {
            viewModel.onAction(LlmHostUiAction.SelectRecommendedModel(model))

            assertEquals("https://huggingface.co/test/qwen.gguf", viewModel.uiState.value.selectedDownloadUrl)
            val effect = awaitItem()
            assertTrue(effect is LlmHostUiEffect.ShowToast)
        }
    }

    @Test
    fun `observeErrors receives error, sets errorMessage, and DismissError clears it`() = runTest {
        fakeRepository.emitError(LlmHostError.EngineError("LiteRT Out of Memory"))

        assertEquals("LiteRT Out of Memory", viewModel.uiState.value.errorMessage)

        viewModel.onAction(LlmHostUiAction.DismissError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `OpenSettings and DismissSettings toggle isSettingsOpen`() = runTest {
        assertFalse(viewModel.uiState.value.isSettingsOpen)

        viewModel.onAction(LlmHostUiAction.OpenSettings)
        assertTrue(viewModel.uiState.value.isSettingsOpen)

        viewModel.onAction(LlmHostUiAction.DismissSettings)
        assertFalse(viewModel.uiState.value.isSettingsOpen)
    }
}
