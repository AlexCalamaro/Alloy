package com.squidink.alloy.modules.llmhost.data.datasource.engine

import android.content.Context
import android.os.ParcelFileDescriptor
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.InferenceResult
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.nehuatl.llamacpp.LlamaContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [ILlmEngine] using Llama.cpp for GGUF model execution.
 */
@Singleton
class LlamaCppLlmEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : ILlmEngine {

    private val mutex = Mutex()
    private var llamaContext: LlamaContext? = null
    private var currentModelPath: String? = null
    private var currentTuning: ModelTuningConfig = ModelTuningConfig()
    private var activeStreamCallback: ((String) -> Unit)? = null

    override val isInitialized: Boolean
        get() = llamaContext != null

    override val loadedModelPath: String?
        get() = currentModelPath

    override suspend fun initialize(modelPath: String, backend: EngineBackend): Result<Unit> =
        initialize(modelPath, backend, ModelTuningConfig())

    override suspend fun initialize(
        modelPath: String,
        backend: EngineBackend,
        tuning: ModelTuningConfig
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                closeInternal()

                val modelFile = File(modelPath)
                if (!modelFile.exists() || !modelFile.isFile) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Model file does not exist: $modelPath")
                    )
                }

                Logger.i(TAG, "Initializing Llama.cpp engine with model: $modelPath on $backend (ctx: ${tuning.contextSize}, threads: ${tuning.threadCount})")

                val pfd = ParcelFileDescriptor.open(modelFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    ?: return@withContext Result.failure(
                        IllegalStateException("Failed to open file descriptor for $modelPath")
                    )
                val fd = pfd.detachFd()

                val params = mapOf(
                    "model" to modelPath,
                    "model_fd" to fd,
                    "n_ctx" to tuning.contextSize,
                    "n_threads" to tuning.threadCount,
                    "use_mmap" to true,
                    "use_mlock" to false,
                    "embedding" to false,
                    "n_batch" to 512,
                    "n_gpu_layers" to if (backend == EngineBackend.GPU) 99 else 0,
                    "vocab_only" to false,
                    "lora" to "",
                    "lora_scaled" to 1.0,
                    "rope_freq_base" to 0.0,
                    "rope_freq_scale" to 0.0
                )

                val contextId = 1
                val contextInstance = LlamaContext(contextId, params)
                contextInstance.setTokenCallback { token ->
                    activeStreamCallback?.invoke(token)
                }

                llamaContext = contextInstance
                currentModelPath = modelPath
                currentTuning = tuning
                Logger.i(TAG, "Llama.cpp engine initialized successfully")
                Result.success(Unit)
            } catch (e: Throwable) {
                Logger.e(TAG, "Failed to initialize Llama.cpp engine", e)
                closeInternal()
                Result.failure(e)
            }
        }
    }

    override suspend fun generate(prompt: String, maxTokens: Int, temperature: Float): Result<InferenceResult> =
        generate(prompt, currentTuning.copy(maxTokens = maxTokens, temperature = temperature))

    override suspend fun generate(prompt: String, tuning: ModelTuningConfig): Result<InferenceResult> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val active = llamaContext ?: return@withContext Result.failure(
                IllegalStateException("Llama.cpp engine is not initialized. Please load a GGUF model first.")
            )

            try {
                val startTime = System.currentTimeMillis()
                val params = mapOf(
                    "prompt" to prompt,
                    "n_predict" to tuning.maxTokens,
                    "temperature" to tuning.temperature.toDouble(),
                    "top_p" to tuning.topP.toDouble(),
                    "n_threads" to tuning.threadCount
                )

                val result = active.completion(params)
                val responseText = result["text"] as? String ?: ""

                val latencyMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                val approxTokens = (responseText.length / CHARS_PER_TOKEN).coerceAtLeast(1)
                val tokensPerSec = approxTokens.toFloat() / (latencyMs.toFloat() / MILLIS_PER_SECOND)

                Result.success(
                    InferenceResult(
                        promptText = prompt,
                        responseText = responseText,
                        latencyMs = latencyMs,
                        tokensPerSecond = tokensPerSec,
                        tokenCount = approxTokens
                    )
                )
            } catch (e: Throwable) {
                Logger.e(TAG, "Llama.cpp generation failed", e)
                Result.failure(e)
            }
        }
    }

    override fun generateStreaming(prompt: String, maxTokens: Int, temperature: Float): Flow<String> =
        generateStreaming(prompt, currentTuning.copy(maxTokens = maxTokens, temperature = temperature))

    override fun generateStreaming(prompt: String, tuning: ModelTuningConfig): Flow<String> = callbackFlow {
        mutex.withLock {
            val active = llamaContext ?: error("Llama.cpp engine is not initialized")
            activeStreamCallback = { token ->
                trySend(token)
            }

            try {
                val params = mapOf(
                    "prompt" to prompt,
                    "n_predict" to tuning.maxTokens,
                    "temperature" to tuning.temperature.toDouble(),
                    "top_p" to tuning.topP.toDouble(),
                    "n_threads" to tuning.threadCount
                )
                active.completion(params)
            } finally {
                activeStreamCallback = null
                channel.close()
            }
        }
        awaitClose {
            activeStreamCallback = null
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeInternal()
        }
    }

    private fun closeInternal() {
        try {
            llamaContext?.release()
        } catch (e: Exception) {
            Logger.w(TAG, "Error closing Llama.cpp context", e)
        } finally {
            llamaContext = null
            currentModelPath = null
            activeStreamCallback = null
        }
    }

    companion object {
        private const val TAG = "LlamaCppLlmEngine"
        private const val CHARS_PER_TOKEN = 4
        private const val MILLIS_PER_SECOND = 1000f
    }
}
