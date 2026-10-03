package com.squidink.alloy.modules.llmhost.data.datasource.engine

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.InferenceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [ILlmEngine] using Google AI Edge's LiteRT-LM framework.
 *
 * Encapsulates native model loading, GPU/CPU delegation, KV-cache management,
 * and thread-safe sequential inference.
 */
@Singleton
class LiteRtLlmEngine @Inject constructor() : ILlmEngine {

    private val mutex = Mutex()
    private var engine: Engine? = null
    private var currentModelPath: String? = null

    override val isInitialized: Boolean
        get() = engine != null

    override val loadedModelPath: String?
        get() = currentModelPath

    override suspend fun initialize(
        modelPath: String,
        backend: EngineBackend
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                closeInternal()

                Logger.i(TAG, "Initializing LiteRT-LM engine with model: $modelPath on $backend")
                val litertBackend = when (backend) {
                    EngineBackend.GPU -> Backend.GPU()
                    EngineBackend.CPU -> Backend.CPU()
                }

                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = litertBackend
                )

                val newEngine = Engine(config)
                newEngine.initialize()

                engine = newEngine
                currentModelPath = modelPath
                Logger.i(TAG, "LiteRT-LM engine initialized successfully")
                Result.success(Unit)
            } catch (e: Throwable) {
                val detailedMsg = if (backend == EngineBackend.CPU && modelPath.contains("-gpu", ignoreCase = true)) {
                    "Model appears to be GPU-specific (-gpu.litertlm) and failed to load on CPU backend: ${e.message}"
                } else {
                    e.message ?: "Unknown initialization error"
                }
                Logger.e(TAG, "Failed to initialize LiteRT-LM engine: $detailedMsg", e)
                closeInternal()
                Result.failure(IllegalStateException(detailedMsg, e))
            }
        }
    }

    override suspend fun generate(
        prompt: String,
        maxTokens: Int,
        temperature: Float
    ): Result<InferenceResult> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val activeEngine = engine ?: return@withContext Result.failure(
                IllegalStateException("LiteRT engine is not initialized. Please load a model first.")
            )

            try {
                val startTime = System.currentTimeMillis()
                val samplerConfig = SamplerConfig(
                    topK = 40,
                    topP = 0.95,
                    temperature = temperature.toDouble(),
                    seed = 0
                )
                val conversationConfig = ConversationConfig(
                    maxOutputToken = maxTokens,
                    samplerConfig = samplerConfig
                )

                val conversation = activeEngine.createConversation(conversationConfig)
                val responseMessage = try {
                    conversation.sendMessage(prompt)
                } finally {
                    conversation.close()
                }

                val responseText = responseMessage.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }

                val latencyMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                val approxTokens = (responseText.length / CHARS_PER_TOKEN).coerceAtLeast(1)
                val tokensPerSec = (approxTokens.toFloat() / (latencyMs.toFloat() / MILLIS_PER_SECOND))

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
                Logger.e(TAG, "Inference generation failed", e)
                Result.failure(e)
            }
        }
    }

    override fun generateStreaming(
        prompt: String,
        maxTokens: Int,
        temperature: Float
    ): Flow<String> = flow {
        mutex.withLock {
            val activeEngine = engine ?: error("LiteRT engine is not initialized")
            val samplerConfig = SamplerConfig(
                topK = 40,
                topP = 0.95,
                temperature = temperature.toDouble(),
                seed = 0
            )
            val conversationConfig = ConversationConfig(
                maxOutputToken = maxTokens,
                samplerConfig = samplerConfig
            )
            val conversation = activeEngine.createConversation(conversationConfig)
            try {
                conversation.sendMessageAsync(prompt).collect { message ->
                    val text = message.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("") { it.text }
                    if (text.isNotEmpty()) {
                        emit(text)
                    }
                }
            } finally {
                conversation.close()
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeInternal()
        }
    }

    private fun closeInternal() {
        try {
            engine?.close()
        } catch (e: Exception) {
            Logger.w(TAG, "Error closing LiteRT engine", e)
        } finally {
            engine = null
            currentModelPath = null
        }
    }

    companion object {
        private const val TAG = "LiteRtLlmEngine"
        private const val CHARS_PER_TOKEN = 4
        private const val MILLIS_PER_SECOND = 1000f
    }
}
