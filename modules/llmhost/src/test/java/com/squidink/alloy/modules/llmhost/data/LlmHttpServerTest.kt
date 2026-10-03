package com.squidink.alloy.modules.llmhost.data

import com.squidink.alloy.modules.llmhost.data.datasource.engine.MockLlmEngine
import com.squidink.alloy.modules.llmhost.data.datasource.server.LlmHttpServer
import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket

class LlmHttpServerTest {

    private lateinit var mockEngine: MockLlmEngine
    private lateinit var server: LlmHttpServer
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private var testPort: Int = 18787

    @Before
    fun setUp() = runBlocking {
        mockEngine = MockLlmEngine()
        mockEngine.initialize("/fake/model.litertlm", EngineBackend.GPU)

        server = LlmHttpServer(mockEngine)
        server.setAuthToken("secret-token-456")
        server.setModelName("gemma-2-2b-it")

        testPort = ServerSocket(0).use { it.localPort }
        val result = server.start(testPort)
        assertTrue(result.isSuccess)
    }

    @After
    fun tearDown() = runBlocking {
        server.stop()
        client.connectionPool.evictAll()
    }

    @Test
    fun `health endpoint returns status ok without authorization`() {
        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/v1/health")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val json = JSONObject(response.body!!.string())
            assertEquals("ok", json.getString("status"))
            assertEquals("gemma-2-2b-it", json.getString("model"))
            assertTrue(json.getBoolean("engine_initialized"))
        }
    }

    @Test
    fun `models endpoint requires valid bearer token`() {
        // Request without auth header -> 401
        val unauthorizedRequest = Request.Builder()
            .url("http://127.0.0.1:$testPort/v1/models")
            .get()
            .build()

        client.newCall(unauthorizedRequest).execute().use { response ->
            assertEquals(401, response.code)
        }

        // Request with valid bearer token -> 200
        val authorizedRequest = Request.Builder()
            .url("http://127.0.0.1:$testPort/v1/models")
            .header("Authorization", "Bearer secret-token-456")
            .get()
            .build()

        client.newCall(authorizedRequest).execute().use { response ->
            assertEquals(200, response.code)
            val json = JSONObject(response.body!!.string())
            assertEquals("list", json.getString("object"))
            val dataArray = json.getJSONArray("data")
            assertEquals(1, dataArray.length())
            assertEquals("gemma-2-2b-it", dataArray.getJSONObject(0).getString("id"))
        }
    }

    @Test
    fun `chat completions endpoint executes inference and returns OpenAI JSON`() {
        val payload = """
            {
                "model": "gemma-2-2b-it",
                "messages": [
                    {"role": "user", "content": "Write a computer haiku"}
                ],
                "max_tokens": 100
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/v1/chat/completions")
            .header("Authorization", "Bearer secret-token-456")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val json = JSONObject(response.body!!.string())
            assertEquals("chat.completion", json.getString("object"))
            val choices = json.getJSONArray("choices")
            assertTrue(choices.length() > 0)
            val message = choices.getJSONObject(0).getJSONObject("message")
            assertEquals("assistant", message.getString("role"))
            assertTrue(message.getString("content").contains("Silent laptop hums"))
        }
    }

    @Test
    fun `completions endpoint executes prompt completion and returns OpenAI JSON`() {
        val payload = """
            {
                "model": "gemma-2-2b-it",
                "prompt": "Write a computer haiku",
                "max_tokens": 50
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/v1/completions")
            .header("Authorization", "Bearer secret-token-456")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val json = JSONObject(response.body!!.string())
            assertEquals("text_completion", json.getString("object"))
            val choices = json.getJSONArray("choices")
            assertTrue(choices.length() > 0)
            assertTrue(choices.getJSONObject(0).getString("text").contains("Silent laptop hums"))
        }
    }
}
