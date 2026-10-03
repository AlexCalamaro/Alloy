package com.squidink.alloy.modules.llmhost.data.datasource.engine

import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.InferenceResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation of [ILlmEngine] used for unit testing and CI environments.
 */
@Singleton
class MockLlmEngine @Inject constructor() : ILlmEngine {

    private var initialized: Boolean = false
    private var modelPath: String? = null
    var simulatedLatencyMs: Long = DEFAULT_SIMULATED_LATENCY_MS
    var mockResponse: String = "This is a simulated on-device completion from the LiteRT engine."

    override val isInitialized: Boolean
        get() = initialized

    override val loadedModelPath: String?
        get() = modelPath

    override suspend fun initialize(modelPath: String, backend: EngineBackend): Result<Unit> {
        this.modelPath = modelPath
        this.initialized = true
        return Result.success(Unit)
    }

    override suspend fun generate(
        prompt: String,
        maxTokens: Int,
        temperature: Float
    ): Result<InferenceResult> {
        if (!initialized) {
            return Result.failure(IllegalStateException("Mock engine not initialized"))
        }

        delay(simulatedLatencyMs)
        val response = if (prompt.contains("haiku", ignoreCase = true)) {
            "Silent laptop hums,\nTokens flow on local cores,\nKnowledge without cloud."
        } else {
            mockResponse
        }

        val tokens = response.split(" ").size
        val tokensPerSec = (tokens.toFloat() / (simulatedLatencyMs.toFloat() / MILLIS_PER_SECOND)).coerceAtLeast(1f)

        return Result.success(
            InferenceResult(
                promptText = prompt,
                responseText = response,
                latencyMs = simulatedLatencyMs,
                tokensPerSecond = tokensPerSec,
                tokenCount = tokens
            )
        )
    }

    override fun generateStreaming(
        prompt: String,
        maxTokens: Int,
        temperature: Float
    ): Flow<String> = flow {
        if (!initialized) error("Mock engine not initialized")
        val words = mockResponse.split(" ")
        for (word in words) {
            delay(STREAMING_DELAY_MS)
            emit("$word ")
        }
    }

    override suspend fun close() {
        initialized = false
        modelPath = null
    }

    companion object {
        const val DEFAULT_SIMULATED_LATENCY_MS = 50L
        private const val MILLIS_PER_SECOND = 1000f
        private const val STREAMING_DELAY_MS = 20L
    }
}
