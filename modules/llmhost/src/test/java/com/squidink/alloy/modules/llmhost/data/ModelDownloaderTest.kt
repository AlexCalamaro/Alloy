package com.squidink.alloy.modules.llmhost.data

import com.squidink.alloy.modules.llmhost.data.datasource.downloader.ModelDownloader
import com.squidink.alloy.modules.llmhost.domain.model.DownloadStatus
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelDownloaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockServer: MockWebServer
    private lateinit var downloader: ModelDownloader

    @Before
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        downloader = ModelDownloader()
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `successful download streams bytes and creates destination file`() = runBlocking {
        val testContent = "SAMPLE_LITERTLM_MODEL_BINARY_CONTENT"
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", testContent.length)
                .setBody(testContent)
        )

        val destinationFile = File(tempFolder.root, "test-model.litertlm")
        val downloadUrl = mockServer.url("/model.litertlm").toString()

        val result = downloader.download(
            url = downloadUrl,
            hfToken = "hf_test_token_123",
            destinationFile = destinationFile
        )

        assertTrue(result.isSuccess)
        assertTrue(destinationFile.exists())
        assertEquals(testContent, destinationFile.readText())

        val request = mockServer.takeRequest()
        assertEquals("Bearer hf_test_token_123", request.getHeader("Authorization"))
        assertEquals(DownloadStatus.COMPLETED, downloader.progress.value.status)
        assertEquals(100f, downloader.progress.value.progressPercent)
    }

    @Test
    fun `download without token does not send Authorization header`() = runBlocking {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("UNAUTHENTICATED_MODEL_CONTENT")
        )

        val destinationFile = File(tempFolder.root, "open-model.litertlm")
        val downloadUrl = mockServer.url("/open-model.litertlm").toString()

        val result = downloader.download(
            url = downloadUrl,
            hfToken = null,
            destinationFile = destinationFile
        )

        assertTrue(result.isSuccess)
        val request = mockServer.takeRequest()
        assertTrue(request.getHeader("Authorization").isNullOrEmpty())
    }

    @Test
    fun `401 response sets FAILED status with unauthorized message`() = runBlocking {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("Unauthorized")
        )

        val destinationFile = File(tempFolder.root, "failed-model.litertlm")
        val downloadUrl = mockServer.url("/gated.litertlm").toString()

        val result = downloader.download(
            url = downloadUrl,
            hfToken = null,
            destinationFile = destinationFile
        )

        assertTrue(result.isFailure)
        assertFalse(destinationFile.exists())
        assertEquals(DownloadStatus.FAILED, downloader.progress.value.status)
        assertTrue(downloader.progress.value.errorMessage?.contains("Unauthorized") == true)
    }
}
