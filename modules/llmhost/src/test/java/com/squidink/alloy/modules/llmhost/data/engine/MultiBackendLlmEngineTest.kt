package com.squidink.alloy.modules.llmhost.data.engine

import com.squidink.alloy.modules.llmhost.data.datasource.engine.LiteRtLlmEngine
import com.squidink.alloy.modules.llmhost.data.datasource.engine.LlamaCppLlmEngine
import com.squidink.alloy.modules.llmhost.data.datasource.engine.MultiBackendLlmEngine
import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.InferenceResult
import com.squidink.alloy.modules.llmhost.domain.model.ModelFormat
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MultiBackendLlmEngineTest {

    private val liteRtEngine: LiteRtLlmEngine = mockk(relaxed = true)
    private val llamaCppEngine: LlamaCppLlmEngine = mockk(relaxed = true)
    private lateinit var multiEngine: MultiBackendLlmEngine

    @Before
    fun setUp() {
        coEvery { liteRtEngine.initialize(any(), any(), any()) } returns Result.success(Unit)
        coEvery { llamaCppEngine.initialize(any(), any(), any()) } returns Result.success(Unit)
        every { liteRtEngine.isInitialized } returns true
        every { llamaCppEngine.isInitialized } returns true
        every { liteRtEngine.loadedModelPath } returns "/models/model.litertlm"
        every { llamaCppEngine.loadedModelPath } returns "/models/model.gguf"

        multiEngine = MultiBackendLlmEngine(
            liteRtEngine = liteRtEngine,
            llamaCppEngine = llamaCppEngine
        )
    }

    @Test
    fun `initialize with litertlm file routes to LiteRtLlmEngine`() = runBlocking {
        val result = multiEngine.initialize("/data/models/gemma.litertlm", EngineBackend.GPU)

        assertTrue(result.isSuccess)
        assertEquals(ModelFormat.LITERT, multiEngine.currentFormat)
        assertTrue(multiEngine.isInitialized)
        coVerify { liteRtEngine.initialize("/data/models/gemma.litertlm", EngineBackend.GPU, any()) }
        coVerify(exactly = 0) { llamaCppEngine.initialize("/data/models/gemma.litertlm", any(), any()) }
    }

    @Test
    fun `initialize with gguf file routes to LlamaCppLlmEngine`() = runBlocking {
        val tuning = ModelTuningConfig(temperature = 0.5f, maxTokens = 512)
        val result = multiEngine.initialize("/data/models/qwen.gguf", EngineBackend.CPU, tuning)

        assertTrue(result.isSuccess)
        assertEquals(ModelFormat.GGUF, multiEngine.currentFormat)
        assertTrue(multiEngine.isInitialized)
        coVerify { llamaCppEngine.initialize("/data/models/qwen.gguf", EngineBackend.CPU, tuning) }
        coVerify(exactly = 0) { liteRtEngine.initialize("/data/models/qwen.gguf", any(), any()) }
    }

    @Test
    fun `generate delegates to the currently active engine`() = runBlocking {
        multiEngine.initialize("/data/models/qwen.gguf", EngineBackend.CPU)

        val expectedResult = InferenceResult(
            promptText = "Hello",
            responseText = "World",
            latencyMs = 50L,
            tokensPerSecond = 25f,
            tokenCount = 5
        )
        coEvery { llamaCppEngine.generate("Hello", 512, 0.7f) } returns Result.success(expectedResult)

        val result = multiEngine.generate("Hello", 512, 0.7f)

        assertTrue(result.isSuccess)
        assertEquals("World", result.getOrThrow().responseText)
        coVerify { llamaCppEngine.generate("Hello", 512, 0.7f) }
    }

    @Test
    fun `generateStreaming delegates to the currently active engine`() = runBlocking {
        multiEngine.initialize("/data/models/gemma.litertlm", EngineBackend.GPU)

        every { liteRtEngine.generateStreaming("Stream test", 512, 0.7f) } returns flowOf("Token 1", "Token 2")

        val tokens = multiEngine.generateStreaming("Stream test", 512, 0.7f).toList()

        assertEquals(listOf("Token 1", "Token 2"), tokens)
        coVerify { liteRtEngine.generateStreaming("Stream test", 512, 0.7f) }
    }

    @Test
    fun `close shuts down both underlying engines and resets state`() = runBlocking {
        multiEngine.initialize("/data/models/gemma.litertlm", EngineBackend.GPU)
        multiEngine.close()

        coVerify { liteRtEngine.close() }
        coVerify { llamaCppEngine.close() }
        assertFalse(multiEngine.isInitialized)
        assertEquals(ModelFormat.UNKNOWN, multiEngine.currentFormat)
    }
}
