package com.squidink.alloy.modules.llmhost.data

import com.squidink.alloy.modules.llmhost.data.datasource.downloader.ModelDownloader
import com.squidink.alloy.modules.llmhost.data.datasource.downloader.ModelStorageManager
import com.squidink.alloy.modules.llmhost.data.datasource.engine.MockLlmEngine
import com.squidink.alloy.modules.llmhost.data.datasource.server.LlmHttpServer
import com.squidink.alloy.modules.llmhost.domain.model.HostStatus
import com.squidink.alloy.modules.llmhost.domain.model.InstalledModel
import com.squidink.alloy.modules.llmhost.domain.model.ModelFormat
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import com.squidink.alloy.modules.llmhost.domain.model.StorageUsage
import com.squidink.alloy.modules.llmhost.service.LlmHostServiceManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class LlmHostRepositoryImplTest {

    private val mockEngine = MockLlmEngine()
    private val modelDownloader: ModelDownloader = mockk(relaxed = true)
    private val storageManager: ModelStorageManager = mockk(relaxed = true)
    private val httpServer: LlmHttpServer = mockk(relaxed = true)
    private val serviceManager: LlmHostServiceManager = mockk(relaxed = true)

    private lateinit var repository: LlmHostRepositoryImpl

    private val sampleModel = InstalledModel(
        fileName = "gemma-2-2b-it-gpu.litertlm",
        filePath = "/data/data/com.squidink.alloy/files/models/gemma-2-2b-it-gpu.litertlm",
        sizeBytes = 1_500_000_000L,
        lastModified = 5000L,
        format = ModelFormat.LITERT,
        isActive = true
    )

    private val ggufModel = InstalledModel(
        fileName = "qwen2.5-7b-instruct.gguf",
        filePath = "/data/data/com.squidink.alloy/files/models/qwen2.5-7b-instruct.gguf",
        sizeBytes = 4_680_000_000L,
        lastModified = 6000L,
        format = ModelFormat.GGUF,
        isActive = false
    )

    @Before
    fun setUp() {
        coEvery { storageManager.getInstalledModel(any()) } returns sampleModel
        coEvery { storageManager.getInstalledModel() } returns sampleModel
        coEvery { storageManager.getInstalledModels() } returns listOf(sampleModel, ggufModel)
        coEvery { storageManager.getStorageUsage() } returns StorageUsage(
            currentModelSizeBytes = 1_500_000_000L,
            totalModelsSizeBytes = 6_180_000_000L,
            availableStorageBytes = 64_000_000_000L
        )
        every { storageManager.modelsDir } returns File("/tmp/models")
        coEvery { httpServer.start(any()) } returns Result.success(Unit)
        every { httpServer.getAuthToken() } returns "auth-token-123"

        repository = LlmHostRepositoryImpl(
            engine = mockEngine,
            modelDownloader = modelDownloader,
            storageManager = storageManager,
            httpServer = httpServer,
            serviceManager = serviceManager
        )
    }

    @Test
    fun `startServer when model is installed initializes engine, server, and service`() = runBlocking {
        val result = repository.startServer()

        assertTrue(result.isSuccess)
        assertEquals(HostStatus.RUNNING, repository.observeHostStatus().first())
        assertTrue(mockEngine.isInitialized)
        coVerify { httpServer.start(8787) }
        verify { serviceManager.startService() }
    }

    @Test
    fun `startServer when no model is installed returns failure`() = runBlocking {
        coEvery { storageManager.getInstalledModel(any()) } returns null
        coEvery { storageManager.getInstalledModel() } returns null

        val result = repository.startServer()

        assertTrue(result.isFailure)
        assertEquals(HostStatus.ERROR, repository.observeHostStatus().first())
    }

    @Test
    fun `stopServer stops server, closes engine, and stops service`() = runBlocking {
        repository.startServer()
        assertEquals(HostStatus.RUNNING, repository.observeHostStatus().first())

        repository.stopServer()

        assertEquals(HostStatus.STOPPED, repository.observeHostStatus().first())
        assertNotNull(repository.observeHostStatus().first())
        coVerify { httpServer.stop() }
        verify { serviceManager.stopService() }
    }

    @Test
    fun `deleteInstalledModel invokes storageManager and resets state`() = runBlocking {
        coEvery { storageManager.deleteModelFiles() } returns Result.success(Unit)

        val result = repository.deleteInstalledModel()

        assertTrue(result.isSuccess)
        coVerify { storageManager.deleteModelFiles() }
    }

    @Test
    fun `deleteModel deletes specific file`() = runBlocking {
        coEvery { storageManager.deleteModel(any()) } returns Result.success(Unit)

        val result = repository.deleteModel(ggufModel.filePath)

        assertTrue(result.isSuccess)
        coVerify { storageManager.deleteModel(ggufModel.filePath) }
    }

    @Test
    fun `selectActiveModel updates config and sets activeModelPath`() = runBlocking {
        coEvery { storageManager.getInstalledModel(ggufModel.filePath) } returns ggufModel

        val result = repository.selectActiveModel(ggufModel.filePath)

        assertTrue(result.isSuccess)
        assertEquals(ggufModel.filePath, repository.getServerConfig().activeModelPath)
    }

    @Test
    fun `updateTuningConfig updates configuration and httpServer`() = runBlocking {
        val tuning = ModelTuningConfig(
            temperature = 0.3f,
            maxTokens = 2048,
            contextSize = 4096,
            topP = 0.8f,
            threadCount = 6
        )

        repository.updateTuningConfig(tuning)

        assertEquals(tuning, repository.getServerConfig().tuning)
        verify { httpServer.setTuningConfig(tuning) }
    }

    @Test
    fun `runTestPrompt returns inference result from engine`() = runBlocking {
        val result = repository.runTestPrompt("Explain memory")

        assertTrue(result.isSuccess)
        val inference = result.getOrThrow()
        assertEquals("Explain memory", inference.promptText)
        assertTrue(inference.responseText.isNotEmpty())
    }
}
