package com.squidink.alloy.modules.llmhost.data.datasource.server

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.llmhost.data.datasource.engine.ILlmEngine
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Localhost HTTP server bound strictly to 127.0.0.1, hosting OpenAI-compatible endpoints
 * backed by the on-device LiteRT engine.
 */
@Singleton
class LlmHttpServer @Inject constructor(
    private val llmEngine: ILlmEngine
) {

    private var server: EmbeddedServer<*, *>? = null
    private var currentPort: Int = DEFAULT_PORT
    private var authToken: String = generateSecureToken()
    private var activeModelName: String = "litert-model"
    private var currentTuning: com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig =
        com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig()

    val isRunning: Boolean
        get() = server != null

    val port: Int
        get() = currentPort

    fun getAuthToken(): String = authToken

    fun setAuthToken(token: String) {
        this.authToken = token
    }

    fun setModelName(name: String) {
        this.activeModelName = name
    }

    fun setTuningConfig(tuning: com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig) {
        this.currentTuning = tuning
    }

    private fun generateSecureToken(): String {
        val randomBytes = ByteArray(TOKEN_BYTE_COUNT)
        SecureRandom().nextBytes(randomBytes)
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }

    private fun isAuthorized(authHeader: String?): Boolean {
        if (authToken.isBlank()) return true
        val expected = "Bearer $authToken".toByteArray(Charsets.UTF_8)
        val actual = (authHeader ?: "").toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(expected, actual)
    }

    /**
     * Starts the embedded Ktor CIO server.
     */
    suspend fun start(port: Int = DEFAULT_PORT): Result<Unit> = withContext(Dispatchers.IO) {
        if (server != null) return@withContext Result.success(Unit)
        currentPort = port

        try {
            val newServer = embeddedServer(
                factory = CIO,
                port = port,
                host = "127.0.0.1",
                parentCoroutineContext = Dispatchers.IO + SupervisorJob()
            ) {
                routing {
                    // Health check endpoint
                    get("/v1/health") {
                        val isGguf = activeModelName.endsWith(".gguf", ignoreCase = true)
                        val responseJson = JSONObject().apply {
                            put("status", "ok")
                            put("model", activeModelName)
                            put("port", currentPort)
                            put("engine_initialized", llmEngine.isInitialized)
                            put("format", if (isGguf) "gguf" else "litertlm")
                            put("backend", if (isGguf) "llama.cpp" else "litert-lm")
                        }
                        call.respondText(responseJson.toString(), ContentType.Application.Json)
                    }

                    // Models listing endpoint
                    get("/v1/models") {
                        val authHeader = call.request.header("Authorization")
                        if (!isAuthorized(authHeader)) {
                            call.respondText(
                                "{\"error\": \"Unauthorized - Valid Bearer token required\"}",
                                ContentType.Application.Json,
                                HttpStatusCode.Unauthorized
                            )
                            return@get
                        }

                        val isGguf = activeModelName.endsWith(".gguf", ignoreCase = true)
                        val responseJson = JSONObject().apply {
                            put("object", "list")
                            put("data", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("id", activeModelName)
                                    put("object", "model")
                                    put("created", System.currentTimeMillis() / MILLIS_PER_SECOND)
                                    put("owned_by", if (isGguf) "alloy-llamacpp-host" else "alloy-litert-host")
                                })
                            })
                        }
                        call.respondText(responseJson.toString(), ContentType.Application.Json)
                    }

                    // OpenAI Chat Completions endpoint
                    post("/v1/chat/completions") {
                        val authHeader = call.request.header("Authorization")
                        if (!isAuthorized(authHeader)) {
                            call.respondText(
                                "{\"error\": \"Unauthorized\"}",
                                ContentType.Application.Json,
                                HttpStatusCode.Unauthorized
                            )
                            return@post
                        }

                        if (!llmEngine.isInitialized) {
                            call.respondText(
                                "{\"error\": \"No LiteRT model is currently loaded in host.\"}",
                                ContentType.Application.Json,
                                HttpStatusCode.ServiceUnavailable
                            )
                            return@post
                        }

                        val bodyText = call.receiveText()
                        val json = try {
                            JSONObject(bodyText)
                        } catch (e: Exception) {
                            call.respondText(
                                "{\"error\": \"Malformed JSON payload\"}",
                                ContentType.Application.Json,
                                HttpStatusCode.BadRequest
                            )
                            return@post
                        }

                        val requestDto = json.toChatCompletionRequest(currentTuning.maxTokens, currentTuning.temperature)
                        val prompt = requestDto.toFormattedPrompt()
                        val result = llmEngine.generate(prompt, requestDto.maxTokens, requestDto.temperature)

                        result.fold(
                            onSuccess = { inferenceResult ->
                                val responseJson = JSONObject().apply {
                                    put("id", "chatcmpl-${UUID.randomUUID()}")
                                    put("object", "chat.completion")
                                    put("created", System.currentTimeMillis() / MILLIS_PER_SECOND)
                                    put("model", activeModelName)
                                    put("choices", JSONArray().apply {
                                        put(JSONObject().apply {
                                            put("index", 0)
                                            put("message", JSONObject().apply {
                                                put("role", "assistant")
                                                put("content", inferenceResult.responseText)
                                            })
                                            put("finish_reason", "stop")
                                        })
                                    })
                                    put("usage", JSONObject().apply {
                                        val promptTokens = (prompt.length / CHARS_PER_TOKEN).coerceAtLeast(1)
                                        put("prompt_tokens", promptTokens)
                                        put("completion_tokens", inferenceResult.tokenCount)
                                        put("total_tokens", promptTokens + inferenceResult.tokenCount)
                                    })
                                }
                                call.respondText(responseJson.toString(), ContentType.Application.Json)
                            },
                            onFailure = { error ->
                                val errorJson = JSONObject().apply {
                                    put("error", "Inference failed: ${error.localizedMessage ?: "Unknown error"}")
                                }
                                call.respondText(
                                    errorJson.toString(),
                                    ContentType.Application.Json,
                                    HttpStatusCode.InternalServerError
                                )
                            }
                        )
                    }

                    // OpenAI Legacy Prompt Completions endpoint
                    post("/v1/completions") {
                        val authHeader = call.request.header("Authorization")
                        if (!isAuthorized(authHeader)) {
                            call.respondText(
                                "{\"error\": \"Unauthorized\"}",
                                ContentType.Application.Json,
                                HttpStatusCode.Unauthorized
                            )
                            return@post
                        }

                        if (!llmEngine.isInitialized) {
                            call.respondText(
                                "{\"error\": \"No model loaded\"}",
                                ContentType.Application.Json,
                                HttpStatusCode.ServiceUnavailable
                            )
                            return@post
                        }

                        val bodyText = call.receiveText()
                        val json = try {
                            JSONObject(bodyText)
                        } catch (e: Exception) {
                            call.respondText(
                                "{\"error\": \"Malformed JSON\"}",
                                ContentType.Application.Json,
                                HttpStatusCode.BadRequest
                            )
                            return@post
                        }

                        val requestDto = json.toCompletionRequest(currentTuning.maxTokens, currentTuning.temperature)
                        val result = llmEngine.generate(requestDto.prompt, requestDto.maxTokens, requestDto.temperature)
                        result.fold(
                            onSuccess = { inferenceResult ->
                                val responseJson = JSONObject().apply {
                                    put("id", "cmpl-${UUID.randomUUID()}")
                                    put("object", "text_completion")
                                    put("created", System.currentTimeMillis() / MILLIS_PER_SECOND)
                                    put("model", activeModelName)
                                    put("choices", JSONArray().apply {
                                        put(JSONObject().apply {
                                            put("text", inferenceResult.responseText)
                                            put("index", 0)
                                            put("finish_reason", "stop")
                                        })
                                    })
                                    put("usage", JSONObject().apply {
                                        val promptTokens = (requestDto.prompt.length / CHARS_PER_TOKEN).coerceAtLeast(1)
                                        put("prompt_tokens", promptTokens)
                                        put("completion_tokens", inferenceResult.tokenCount)
                                        put("total_tokens", promptTokens + inferenceResult.tokenCount)
                                    })
                                }
                                call.respondText(responseJson.toString(), ContentType.Application.Json)
                            },
                            onFailure = { error ->
                                val errorJson = JSONObject().apply {
                                    put("error", "Inference failed: ${error.localizedMessage ?: "Unknown error"}")
                                }
                                call.respondText(
                                    errorJson.toString(),
                                    ContentType.Application.Json,
                                    HttpStatusCode.InternalServerError
                                )
                            }
                        )
                    }
                }
            }

            newServer.start(wait = false)
            server = newServer
            Logger.i(TAG, "LlmHttpServer started on 127.0.0.1:$port (Model: $activeModelName)")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to start LlmHttpServer on port $port", e)
            server = null
            Result.failure(e)
        }
    }

    /**
     * Stops the Ktor embedded server.
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        try {
            server?.stop(STOP_GRACE_PERIOD_MS, STOP_TIMEOUT_MS)
            server = null
            Logger.i(TAG, "LlmHttpServer stopped")
        } catch (e: Exception) {
            Logger.w(TAG, "Error stopping LlmHttpServer", e)
        }
    }

    companion object {
        private const val TAG = "LlmHttpServer"
        const val DEFAULT_PORT = 8787
        private const val TOKEN_BYTE_COUNT = 24
        private const val MILLIS_PER_SECOND = 1000L
        private const val DEFAULT_MAX_TOKENS = 512
        private const val DEFAULT_TEMPERATURE = 0.7f
        private const val CHARS_PER_TOKEN = 4
        private const val STOP_GRACE_PERIOD_MS = 100L
        private const val STOP_TIMEOUT_MS = 500L
    }
}
