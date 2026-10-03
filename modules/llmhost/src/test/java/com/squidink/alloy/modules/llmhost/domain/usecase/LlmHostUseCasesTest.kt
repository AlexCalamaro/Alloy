package com.squidink.alloy.modules.llmhost.domain.usecase

import app.cash.turbine.test
import com.squidink.alloy.modules.llmhost.FakeLlmHostRepository
import com.squidink.alloy.modules.llmhost.domain.model.DownloadStatus
import com.squidink.alloy.modules.llmhost.domain.model.HostStatus
import com.squidink.alloy.modules.llmhost.domain.model.InstalledModel
import com.squidink.alloy.modules.llmhost.domain.model.LlmHostError
import com.squidink.alloy.modules.llmhost.domain.model.ModelFormat
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LlmHostUseCasesTest {

    private lateinit var fakeRepository: FakeLlmHostRepository

    @Before
    fun setUp() {
        fakeRepository = FakeLlmHostRepository()
    }

    @Test
    fun `ObserveHostStateUseCase emits combined state snapshot with installedModels`() = runTest {
        val useCase = ObserveHostStateUseCase(fakeRepository)

        useCase().test {
            val snapshot = awaitItem()
            assertEquals(HostStatus.STOPPED, snapshot.status)
            assertNotNull(snapshot.installedModel)
            assertEquals("gemma-2-2b-it-gpu.litertlm", snapshot.installedModel?.fileName)
            assertEquals(1, snapshot.installedModels.size)
            assertEquals(DownloadStatus.IDLE, snapshot.downloadProgress.status)
            assertEquals(1_500_000_000L, snapshot.storageUsage.currentModelSizeBytes)
        }
    }

    @Test
    fun `StartHostServerUseCase transitions status to RUNNING`() = runTest {
        val useCase = StartHostServerUseCase(fakeRepository)
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(HostStatus.RUNNING, fakeRepository.hostStatusFlow.value)
        assertTrue(fakeRepository.installedModelFlow.value?.isLoaded == true)
    }

    @Test
    fun `StopHostServerUseCase transitions status to STOPPED`() = runTest {
        fakeRepository.startServer()
        assertEquals(HostStatus.RUNNING, fakeRepository.hostStatusFlow.value)

        val useCase = StopHostServerUseCase(fakeRepository)
        useCase()

        assertEquals(HostStatus.STOPPED, fakeRepository.hostStatusFlow.value)
        assertTrue(fakeRepository.installedModelFlow.value?.isLoaded == false)
    }

    @Test
    fun `DownloadModelUseCase starts download with progress`() = runTest {
        val useCase = DownloadModelUseCase(fakeRepository)
        val result = useCase("https://huggingface.co/test/model.litertlm", null)

        assertTrue(result.isSuccess)
        assertEquals(DownloadStatus.DOWNLOADING, fakeRepository.downloadProgressFlow.value.status)
        assertEquals(50f, fakeRepository.downloadProgressFlow.value.progressPercent)
    }

    @Test
    fun `CancelDownloadUseCase resets progress to CANCELLED`() = runTest {
        fakeRepository.startDownload("https://huggingface.co/test/model.litertlm", null)

        val useCase = CancelDownloadUseCase(fakeRepository)
        useCase()

        assertEquals(DownloadStatus.CANCELLED, fakeRepository.downloadProgressFlow.value.status)
    }

    @Test
    fun `DeleteModelUseCase clears installed model and updates storage`() = runTest {
        assertNotNull(fakeRepository.installedModelFlow.value)

        val useCase = DeleteModelUseCase(fakeRepository)
        val result = useCase()

        assertTrue(result.isSuccess)
        assertNull(fakeRepository.installedModelFlow.value)
        assertEquals(0L, fakeRepository.storageUsageFlow.value.currentModelSizeBytes)
    }

    @Test
    fun `SelectActiveModelUseCase updates active model in repository`() = runTest {
        val ggufModel = InstalledModel(
            fileName = "qwen2.5-7b.gguf",
            filePath = "/fake/models/qwen2.5-7b.gguf",
            sizeBytes = 4_000_000_000L,
            lastModified = 2000L,
            format = ModelFormat.GGUF,
            isActive = false
        )
        fakeRepository.installedModelsFlow.value = fakeRepository.installedModelsFlow.value + ggufModel

        val useCase = SelectActiveModelUseCase(fakeRepository)
        val result = useCase(ggufModel.filePath)

        assertTrue(result.isSuccess)
        assertEquals(ggufModel.filePath, fakeRepository.installedModelFlow.value?.filePath)
        assertTrue(fakeRepository.installedModelsFlow.value.first { it.filePath == ggufModel.filePath }.isActive)
    }

    @Test
    fun `DeleteSingleModelUseCase deletes model from list`() = runTest {
        val ggufModel = InstalledModel(
            fileName = "test.gguf",
            filePath = "/fake/models/test.gguf",
            sizeBytes = 500_000_000L,
            lastModified = 3000L,
            format = ModelFormat.GGUF,
            isActive = false
        )
        fakeRepository.installedModelsFlow.value = fakeRepository.installedModelsFlow.value + ggufModel

        val useCase = DeleteSingleModelUseCase(fakeRepository)
        val result = useCase(ggufModel.filePath)

        assertTrue(result.isSuccess)
        assertTrue(fakeRepository.installedModelsFlow.value.none { it.filePath == ggufModel.filePath })
    }

    @Test
    fun `UpdateTuningConfigUseCase updates tuning parameters in repository`() = runTest {
        val useCase = UpdateTuningConfigUseCase(fakeRepository)
        val newTuning = ModelTuningConfig(
            temperature = 0.5f,
            maxTokens = 2048,
            contextSize = 4096,
            topP = 0.85f,
            threadCount = 8
        )

        useCase(newTuning)

        val saved = fakeRepository.getServerConfig().tuning
        assertEquals(0.5f, saved.temperature)
        assertEquals(2048, saved.maxTokens)
        assertEquals(4096, saved.contextSize)
        assertEquals(0.85f, saved.topP)
        assertEquals(8, saved.threadCount)
    }

    @Test
    fun `RunTestInferenceUseCase executes test prompt and returns metrics`() = runTest {
        val useCase = RunTestInferenceUseCase(fakeRepository)
        val result = useCase("Hello LiteRT")

        assertTrue(result.isSuccess)
        val inference = result.getOrThrow()
        assertEquals("Hello LiteRT", inference.promptText)
        assertTrue(inference.responseText.isNotEmpty())
        assertEquals(150L, inference.latencyMs)
        assertTrue(inference.tokensPerSecond > 0f)
    }

    @Test
    fun `GetModelStorageUseCase emits current storage utilization`() = runTest {
        val useCase = GetModelStorageUseCase(fakeRepository)

        useCase().test {
            val usage = awaitItem()
            assertEquals(1_500_000_000L, usage.currentModelSizeBytes)
            assertEquals(64_000_000_000L, usage.availableStorageBytes)
        }
    }

    @Test
    fun `GetRecommendedModelsUseCase returns curated models list`() {
        val useCase = GetRecommendedModelsUseCase(fakeRepository)
        val recommendations = useCase()

        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.id == "gemma-2-2b-it" })
        assertTrue(recommendations.any { it.id == "qwen-2.5-7b-instruct-gguf" })
    }

    @Test
    fun `ObserveErrorsUseCase emits errors as they occur`() = runTest {
        val useCase = ObserveErrorsUseCase(fakeRepository)

        useCase().test {
            fakeRepository.emitError(LlmHostError.ServerError("Failed to bind port 8787"))
            val emitted = awaitItem()
            assertTrue(emitted is LlmHostError.ServerError)
            assertEquals("Failed to bind port 8787", emitted.message)
        }
    }
}
