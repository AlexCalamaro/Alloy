package com.squidink.alloy.modules.llmhost.domain.repository

import com.squidink.alloy.modules.llmhost.domain.model.DownloadProgress
import com.squidink.alloy.modules.llmhost.domain.model.EngineBackend
import com.squidink.alloy.modules.llmhost.domain.model.HostStatus
import com.squidink.alloy.modules.llmhost.domain.model.InferenceResult
import com.squidink.alloy.modules.llmhost.domain.model.InstalledModel
import com.squidink.alloy.modules.llmhost.domain.model.LlmHostError
import com.squidink.alloy.modules.llmhost.domain.model.ModelTuningConfig
import com.squidink.alloy.modules.llmhost.domain.model.RecommendedModel
import com.squidink.alloy.modules.llmhost.domain.model.ServerConfig
import com.squidink.alloy.modules.llmhost.domain.model.StorageUsage
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for local LLM hosting, model lifecycle, and storage management.
 */
interface ILlmHostRepository {

    /**
     * Observes the active running status of the localhost server.
     */
    fun observeHostStatus(): Flow<HostStatus>

    /**
     * Returns the current running status of the localhost server synchronously.
     */
    fun getHostStatus(): HostStatus

    /**
     * Observes the currently installed and active model, or null if no model is present.
     */
    fun observeInstalledModel(): Flow<InstalledModel?>

    /**
     * Observes all installed models available in the local model library.
     */
    fun observeInstalledModels(): Flow<List<InstalledModel>>

    /**
     * Selects a model from the installed library as the active model for inference.
     */
    suspend fun selectActiveModel(filePath: String): Result<Unit>

    /**
     * Deletes a specific model file from the library.
     */
    suspend fun deleteModel(filePath: String): Result<Unit>

    /**
     * Updates model execution and generation tuning parameters.
     */
    suspend fun updateTuningConfig(tuning: ModelTuningConfig)

    /**
     * Observes real-time download progress for models being fetched from Hugging Face.
     */
    fun observeDownloadProgress(): Flow<DownloadProgress>

    /**
     * Observes storage usage metrics for installed models and remaining device disk space.
     */
    fun observeStorageUsage(): Flow<StorageUsage>

    /**
     * Observes domain errors emitted during server hosting, downloads, or inference.
     */
    fun observeErrors(): Flow<LlmHostError>

    /**
     * Retrieves current server configuration parameters.
     */
    fun getServerConfig(): ServerConfig

    /**
     * Updates port, authorization token, or hardware backend configuration.
     */
    suspend fun updateServerConfig(port: Int, authToken: String, backend: EngineBackend)

    /**
     * Starts the localhost HTTP server hosting the installed LiteRT model.
     */
    suspend fun startServer(): Result<Unit>

    /**
     * Stops the localhost HTTP server and releases engine resources.
     */
    suspend fun stopServer()

    /**
     * Initiates streaming download of a LiteRT model from a Hugging Face URL.
     *
     * @param url Direct download URL to a .litertlm file.
     * @param hfToken Optional Hugging Face user access token for gated models.
     */
    suspend fun startDownload(url: String, hfToken: String?): Result<Unit>

    /**
     * Cancels an ongoing download and cleans up temporary partial files.
     */
    suspend fun cancelDownload()

    /**
     * Deletes the currently installed model file to free local disk space.
     */
    suspend fun deleteInstalledModel(): Result<Unit>

    /**
     * Executes a sample test prompt against the installed model and measures performance.
     *
     * @param prompt The prompt to execute.
     */
    suspend fun runTestPrompt(prompt: String): Result<InferenceResult>

    /**
     * Returns curated model recommendations tailored for Googlebook OS compact laptops.
     */
    fun getRecommendations(): List<RecommendedModel>
}
