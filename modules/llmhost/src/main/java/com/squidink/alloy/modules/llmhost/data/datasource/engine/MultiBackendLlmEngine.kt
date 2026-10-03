package com.squidink.alloy.modules.llmhost.data.datasource.engine

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.InferenceResult
import com.squidink.alloy.modules.llmhost.domain.model.ModelFormat
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composite [ILlmEngine] that dynamically routes inference requests to either
 * [LiteRtLlmEngine] (for .litertlm models) or [LlamaCppLlmEngine] (for .gguf models).
 */
@Singleton
class MultiBackendLlmEngine @Inject constructor(
    private val liteRtEngine: LiteRtLlmEngine,
    private val llamaCppEngine: LlamaCppLlmEngine
) : ILlmEngine {

    private var activeEngine: ILlmEngine? = null
    private var activeFormat: ModelFormat = ModelFormat.UNKNOWN

    override val isInitialized: Boolean
        get() = activeEngine?.isInitialized == true

    override val loadedModelPath: String?
        get() = activeEngine?.loadedModelPath

    val currentFormat: ModelFormat
        get() = activeFormat

    override suspend fun initialize(modelPath: String, backend: EngineBackend): Result<Unit> =
        initialize(modelPath, backend, ModelTuningConfig())

    override suspend fun initialize(
        modelPath: String,
        backend: EngineBackend,
        tuning: ModelTuningConfig
    ): Result<Unit> {
        close()

        val format = ModelFormat.fromFileName(modelPath)
        Logger.i(TAG, "Routing model $modelPath to backend for format: $format")

        val targetEngine = when (format) {
            ModelFormat.LITERT -> liteRtEngine
            ModelFormat.GGUF -> llamaCppEngine
            ModelFormat.UNKNOWN -> {
                Logger.w(TAG, "Unknown format for $modelPath; defaulting to Llama.cpp engine")
                llamaCppEngine
            }
        }

        val result = targetEngine.initialize(modelPath, backend, tuning)
        if (result.isSuccess) {
            activeEngine = targetEngine
            activeFormat = format
            Logger.i(TAG, "Successfully initialized backend engine: ${targetEngine::class.simpleName}")
        } else {
            activeEngine = null
            activeFormat = ModelFormat.UNKNOWN
            Logger.e(TAG, "Failed initializing backend engine: ${targetEngine::class.simpleName}", result.exceptionOrNull())
        }
        return result
    }

    override suspend fun generate(prompt: String, maxTokens: Int, temperature: Float): Result<InferenceResult> {
        val engine = activeEngine ?: return Result.failure(
            IllegalStateException("No LLM engine initialized. Please load a model first.")
        )
        return engine.generate(prompt, maxTokens, temperature)
    }

    override suspend fun generate(prompt: String, tuning: ModelTuningConfig): Result<InferenceResult> {
        val engine = activeEngine ?: return Result.failure(
            IllegalStateException("No LLM engine initialized. Please load a model first.")
        )
        return engine.generate(prompt, tuning)
    }

    override fun generateStreaming(prompt: String, maxTokens: Int, temperature: Float): Flow<String> {
        val engine = activeEngine ?: error("No LLM engine initialized. Please load a model first.")
        return engine.generateStreaming(prompt, maxTokens, temperature)
    }

    override fun generateStreaming(prompt: String, tuning: ModelTuningConfig): Flow<String> {
        val engine = activeEngine ?: error("No LLM engine initialized. Please load a model first.")
        return engine.generateStreaming(prompt, tuning)
    }

    override suspend fun close() {
        try {
            liteRtEngine.close()
        } catch (e: Exception) {
            Logger.w(TAG, "Error closing liteRtEngine", e)
        }
        try {
            llamaCppEngine.close()
        } catch (e: Exception) {
            Logger.w(TAG, "Error closing llamaCppEngine", e)
        }
        activeEngine = null
        activeFormat = ModelFormat.UNKNOWN
    }

    companion object {
        private const val TAG = "MultiBackendLlmEngine"
    }
}
