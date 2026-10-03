package com.squidink.alloy.modules.llmhost.data.datasource.server

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data Transfer Objects conforming to OpenAI REST API schema.
 */

data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatCompletionRequestDto(
    val model: String = "litert",
    val messages: List<ChatMessageDto> = emptyList(),
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val stream: Boolean = false
) {
    fun toFormattedPrompt(): String {
        val promptBuilder = StringBuilder()
        for (msg in messages) {
            when (msg.role.lowercase()) {
                "system" -> promptBuilder.append("System: ${msg.content}\n")
                "user" -> promptBuilder.append("User: ${msg.content}\n")
                "assistant" -> promptBuilder.append("Assistant: ${msg.content}\n")
                else -> promptBuilder.append("${msg.role}: ${msg.content}\n")
            }
        }
        promptBuilder.append("Assistant: ")
        return promptBuilder.toString().trim()
    }
}

data class CompletionRequestDto(
    val model: String = "litert",
    val prompt: String = "",
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val stream: Boolean = false
)

fun JSONObject.toChatCompletionRequest(
    defaultMaxTokens: Int = 512,
    defaultTemperature: Float = 0.7f
): ChatCompletionRequestDto {
    val messagesArray = optJSONArray("messages") ?: JSONArray()
    val messages = ArrayList<ChatMessageDto>(messagesArray.length())
    for (i in 0 until messagesArray.length()) {
        val msg = messagesArray.optJSONObject(i) ?: continue
        messages.add(
            ChatMessageDto(
                role = msg.optString("role", "user"),
                content = msg.optString("content", "")
            )
        )
    }
    return ChatCompletionRequestDto(
        model = optString("model", "litert"),
        messages = messages,
        maxTokens = optInt("max_tokens", defaultMaxTokens),
        temperature = optDouble("temperature", defaultTemperature.toDouble()).toFloat(),
        stream = optBoolean("stream", false)
    )
}

fun JSONObject.toCompletionRequest(
    defaultMaxTokens: Int = 512,
    defaultTemperature: Float = 0.7f
): CompletionRequestDto {
    return CompletionRequestDto(
        model = optString("model", "litert"),
        prompt = optString("prompt", ""),
        maxTokens = optInt("max_tokens", defaultMaxTokens),
        temperature = optDouble("temperature", defaultTemperature.toDouble()).toFloat(),
        stream = optBoolean("stream", false)
    )
}

