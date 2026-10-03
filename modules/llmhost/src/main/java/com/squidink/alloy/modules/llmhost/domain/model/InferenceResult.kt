package com.squidink.alloy.modules.llmhost.domain.model

/**
 * Result data class for model inference execution.
 */
data class InferenceResult(
    val promptText: String,
    val responseText: String,
    val latencyMs: Long,
    val tokensPerSecond: Float,
    val tokenCount: Int
)
