package com.squidink.alloy.modules.llmhost.domain.model

/**
 * Status of the local HTTP LLM host server.
 */
enum class HostStatus(val displayName: String) {
    STOPPED("Stopped"),
    STARTING("Starting..."),
    RUNNING("Running"),
    ERROR("Error")
}

/**
 * Hardware execution backend for LiteRT inference.
 */
enum class EngineBackend(val displayName: String) {
    GPU("GPU (OpenCL / Vulkan)"),
    CPU("CPU (ARM NEON / SIMD)")
}

/**
 * Supported on-device model file formats.
 */
enum class ModelFormat(val extension: String, val displayName: String) {
    LITERT("litertlm", "LiteRT-LM (.litertlm)"),
    GGUF("gguf", "Llama.cpp (.gguf)"),
    UNKNOWN("", "Unknown");

    companion object {
        fun fromFileName(fileName: String): ModelFormat {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "litertlm", "bin", "tflite" -> LITERT
                "gguf" -> GGUF
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Represents an on-device model file downloaded and installed for inference.
 */
data class InstalledModel(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val format: ModelFormat = ModelFormat.fromFileName(fileName),
    val isLoaded: Boolean = false,
    val isActive: Boolean = false
)

/**
 * Status states for background model downloads from Hugging Face.
 */
enum class DownloadStatus {
    IDLE,
    CONNECTING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Reactive progress of a model file download.
 */
data class DownloadProgress(
    val status: DownloadStatus = DownloadStatus.IDLE,
    val bytesRead: Long = 0L,
    val totalBytes: Long = 0L,
    val progressPercent: Float = 0f,
    val speedBytesPerSec: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Storage metrics for model files and device capacity.
 */
data class StorageUsage(
    val currentModelSizeBytes: Long = 0L,
    val totalModelsSizeBytes: Long = 0L,
    val availableStorageBytes: Long = 0L
)

/**
 * Advanced model execution and generation tuning parameters.
 */
data class ModelTuningConfig(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 512,
    val contextSize: Int = 2048,
    val topP: Float = 0.95f,
    val threadCount: Int = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
)

/**
 * Combined host state stream for UI state mapping.
 */
data class HostStateSnapshot(
    val status: HostStatus,
    val installedModels: List<InstalledModel> = emptyList(),
    val activeModel: InstalledModel? = null,
    val downloadProgress: DownloadProgress = DownloadProgress(),
    val storageUsage: StorageUsage = StorageUsage()
) {
    /**
     * Backward-compatible accessor for existing single-model references.
     */
    val installedModel: InstalledModel? get() = activeModel
}

/**
 * Configuration parameters for the localhost loopback server.
 */
data class ServerConfig(
    val host: String = "127.0.0.1",
    val port: Int = 8787,
    val authToken: String = "",
    val backend: EngineBackend = EngineBackend.GPU,
    val activeModelPath: String? = null,
    val tuning: ModelTuningConfig = ModelTuningConfig()
)

/**
 * State of diagnostic prompt execution.
 */
data class TestInferenceState(
    val isRunning: Boolean = false,
    val result: InferenceResult? = null,
    val errorMessage: String? = null
)

private const val BYTES_PER_UNIT = 1024.0

/**
 * Formats bytes into a human-readable string (e.g. "1.46 GB", "520 MB").
 */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val kb = bytes / BYTES_PER_UNIT
    val mb = kb / BYTES_PER_UNIT
    val gb = mb / BYTES_PER_UNIT

    return if (gb >= 1.0) {
        String.format(java.util.Locale.US, "%.2f GB", gb)
    } else {
        String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}

