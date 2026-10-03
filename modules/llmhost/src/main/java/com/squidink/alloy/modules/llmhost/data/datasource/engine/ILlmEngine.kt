package com.squidink.alloy.modules.llmhost.data.datasource.engine

import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.InferenceResult
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import kotlinx.coroutines.flow.Flow

/**
 * Common abstraction for LiteRT Large Language Model inference execution.
 */
interface ILlmEngine {

    /**
     * Whether an active model is loaded into the engine.
     */
    val isInitialized: Boolean

    /**
     * Path to the loaded model file, or null if uninitialized.
     */
    val loadedModelPath: String?

    /**
     * Initializes the engine with a local model file.
     *
     * @param modelPath Path to the local model file (.litertlm or .gguf).
     * @param backend Hardware execution backend (GPU or CPU).
     */
    suspend fun initialize(modelPath: String, backend: EngineBackend): Result<Unit>

    /**
     * Initializes the engine with a local model file and tuning configuration.
     */
    suspend fun initialize(
        modelPath: String,
        backend: EngineBackend,
        tuning: ModelTuningConfig
    ): Result<Unit> = initialize(modelPath, backend)

    /**
     * Generates a complete text response for a given prompt synchronously.
     */
    suspend fun generate(prompt: String, maxTokens: Int = 512, temperature: Float = 0.7f): Result<InferenceResult>

    /**
     * Generates a complete text response for a given prompt using tuning parameters.
     */
    suspend fun generate(
        prompt: String,
        tuning: ModelTuningConfig
    ): Result<InferenceResult> = generate(prompt, tuning.maxTokens, tuning.temperature)

    /**
     * Streams generated tokens asynchronously.
     */
    fun generateStreaming(prompt: String, maxTokens: Int = 512, temperature: Float = 0.7f): Flow<String>

    /**
     * Streams generated tokens asynchronously using tuning parameters.
     */
    fun generateStreaming(
        prompt: String,
        tuning: ModelTuningConfig
    ): Flow<String> = generateStreaming(prompt, tuning.maxTokens, tuning.temperature)

    /**
     * Unloads model and releases native resources.
     */
    suspend fun close()
}
