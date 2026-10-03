package com.squidink.alloy.modules.llmhost.domain.model

/**
 * Domain-specific error representations for LLM Host operations.
 */
sealed class LlmHostError(open val message: String) {
    data class DownloadError(override val message: String, val cause: Throwable? = null) : LlmHostError(message)
    data class StorageError(override val message: String, val cause: Throwable? = null) : LlmHostError(message)
    data class EngineError(override val message: String, val cause: Throwable? = null) : LlmHostError(message)
    data class ServerError(override val message: String, val cause: Throwable? = null) : LlmHostError(message)
    data class InferenceError(override val message: String, val cause: Throwable? = null) : LlmHostError(message)
}
