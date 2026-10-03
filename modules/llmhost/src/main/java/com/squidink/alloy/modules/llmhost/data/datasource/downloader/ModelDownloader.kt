package com.squidink.alloy.modules.llmhost.data.datasource.downloader

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.llmhost.domain.model.DownloadProgress
import com.squidink.alloy.modules.llmhost.domain.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads model files from Hugging Face with progress tracking, token auth, and cancellation.
 */
@Singleton
class ModelDownloader @Inject constructor() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

    private var activeCall: Call? = null

    /**
     * Starts downloading the file at [url] to [destinationFile].
     */
    suspend fun download(
        url: String,
        hfToken: String?,
        destinationFile: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val partFile = File(destinationFile.parentFile, "${destinationFile.name}.part")

        _progress.value = DownloadProgress(status = DownloadStatus.CONNECTING)

        val requestBuilder = Request.Builder().url(url)
        if (!hfToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer ${hfToken.trim()}")
        }
        val request = requestBuilder.build()

        val call = client.newCall(request)
        activeCall = call

        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    HTTP_UNAUTHORIZED -> "Unauthorized. Gated Hugging Face model requires a valid Access Token."
                    HTTP_FORBIDDEN -> "Forbidden. Access denied or terms not accepted on Hugging Face."
                    HTTP_NOT_FOUND -> "Model file not found at specified URL."
                    else -> "Download failed with HTTP ${response.code}: ${response.message}"
                }
                _progress.value = DownloadProgress(
                    status = DownloadStatus.FAILED,
                    errorMessage = errorMsg
                )
                return@withContext Result.failure(IOException(errorMsg))
            }

            val body = response.body ?: throw IOException("Empty response body from server")
            val totalBytes = body.contentLength()

            _progress.value = DownloadProgress(
                status = DownloadStatus.DOWNLOADING,
                totalBytes = totalBytes
            )

            if (partFile.exists()) partFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead = 0L
                    var lastUpdateTime = System.currentTimeMillis()
                    var bytesSinceLastUpdate = 0L
                    var read: Int

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        bytesSinceLastUpdate += read

                        val now = System.currentTimeMillis()
                        val timeDelta = now - lastUpdateTime

                        if (timeDelta >= UPDATE_INTERVAL_MS) {
                            val speed = (bytesSinceLastUpdate.toFloat() /
                                (timeDelta.toFloat() / MILLIS_PER_SECOND)).toLong()
                            val percent = if (totalBytes > 0) {
                                ((bytesRead.toFloat() / totalBytes.toFloat()) * PERCENT_MULTIPLIER)
                                    .coerceIn(0f, PERCENT_MULTIPLIER)
                            } else {
                                0f
                            }

                            _progress.value = DownloadProgress(
                                status = DownloadStatus.DOWNLOADING,
                                bytesRead = bytesRead,
                                totalBytes = totalBytes,
                                progressPercent = percent,
                                speedBytesPerSec = speed
                            )

                            lastUpdateTime = now
                            bytesSinceLastUpdate = 0L
                        }
                    }
                }
            }

            // Rename .part to final model file
            if (destinationFile.exists()) destinationFile.delete()
            if (!partFile.renameTo(destinationFile)) {
                throw IOException("Failed to rename temporary download file to ${destinationFile.name}")
            }

            _progress.value = DownloadProgress(
                status = DownloadStatus.COMPLETED,
                bytesRead = destinationFile.length(),
                totalBytes = destinationFile.length(),
                progressPercent = 100f
            )

            Logger.i(TAG, "Download completed: ${destinationFile.absolutePath} (${destinationFile.length()} bytes)")
            Result.success(Unit)
        } catch (e: Exception) {
            if (call.isCanceled() || e is CancellationException) {
                Logger.i(TAG, "Download cancelled by user")
                if (partFile.exists()) partFile.delete()
                _progress.value = DownloadProgress(status = DownloadStatus.CANCELLED)
                Result.failure(CancellationException("Download cancelled"))
            } else {
                Logger.e(TAG, "Download failed", e)
                if (partFile.exists()) partFile.delete()
                _progress.value = DownloadProgress(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Download failed"
                )
                Result.failure(e)
            }
        } finally {
            activeCall = null
        }
    }

    /**
     * Cancels any active download and resets progress.
     */
    fun cancel() {
        activeCall?.cancel()
        activeCall = null
        _progress.value = DownloadProgress(status = DownloadStatus.CANCELLED)
    }

    companion object {
        private const val TAG = "ModelDownloader"
        private const val BUFFER_SIZE = 8192
        private const val UPDATE_INTERVAL_MS = 500L
        private const val MILLIS_PER_SECOND = 1000f
        private const val PERCENT_MULTIPLIER = 100f
        private const val CONNECT_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 60L
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
    }
}
