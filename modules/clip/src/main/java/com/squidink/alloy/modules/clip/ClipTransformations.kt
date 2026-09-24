package com.squidink.alloy.modules.clip

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure transformations pipeline for text clipboard entries.
 */
object ClipTransformations {
    fun toUpperCase(input: String): String = input.uppercase()

    fun toLowerCase(input: String): String = input.lowercase()

    fun trimWhitespace(input: String): String = input.trim()

    fun sortLines(
        input: String,
        descending: Boolean = false,
    ): String {
        val lines = input.split("\n").filter { it.isNotBlank() }
        return if (descending) {
            lines.sortedDescending().joinToString("\n")
        } else {
            lines.sorted().joinToString("\n")
        }
    }

    fun dedupeLines(
        input: String,
        preserveOrder: Boolean = true,
    ): String =
        if (preserveOrder) {
            input
                .split("\n")
                .filter { it.isNotBlank() }
                .toMutableList()
                .distinct()
                .joinToString("\n")
        } else {
            input
                .split("\n")
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("\n")
        }

    fun regexReplace(
        input: String,
        pattern: String,
        replacement: String,
    ): String =
        try {
            input.replace(Regex(pattern), replacement)
        } catch (e: Exception) {
            input
        }

    fun jsonPretty(input: String): String =
        try {
            // Try as JSON object first
            JSONObject(input).toString(2)
        } catch (e: Exception) {
            try {
                // Try as JSON array
                JSONArray(input).toString(2)
            } catch (e2: Exception) {
                input // Not JSON
            }
        }

    fun jsonMinify(input: String): String =
        try {
            JSONObject(input).toString(0)
        } catch (e: Exception) {
            try {
                JSONArray(input).toString(0)
            } catch (e2: Exception) {
                input
            }
        }

    fun encodeBase64(input: String): String = Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    fun decodeBase64(input: String): String =
        try {
            String(Base64.decode(input, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            input
        }

    fun urlEncode(input: String): String = android.net.Uri.encode(input)

    fun urlDecode(input: String): String =
        try {
            android.net.Uri.decode(input)
        } catch (e: Exception) {
            input
        }

    fun markdownToPlainText(input: String): String {
        // Basic markdown to plain text conversion
        return input
            .replace(Regex("#+\\s*"), "") // Remove headers
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1") // Bold
            .replace(Regex("\\*(.+?)\\*"), "$1") // Italic
            .replace(Regex("``(.+?)``"), "$1") // Inline code
            .replace(Regex("`(.+?)`"), "$1") // Inline code
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1") // Links
    }

    fun extractUrls(input: String): String {
        val urlPattern = "https?://[^\\s]+".toRegex()
        return urlPattern
            .findAll(input)
            .map { it.value }
            .distinct()
            .joinToString("\n")
    }

    fun extractEmails(input: String): String {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex()
        return emailPattern
            .findAll(input)
            .map { it.value }
            .distinct()
            .joinToString("\n")
    }
}
