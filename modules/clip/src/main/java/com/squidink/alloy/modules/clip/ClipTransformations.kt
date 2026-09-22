package com.squidink.alloy.modules.clip

import android.util.Base64

/**
 * Pure transformations pipeline for text clipboard entries.
 */
object ClipTransformations {

    fun toUpperCase(input: String): String = input.uppercase()

    fun toLowerCase(input: String): String = input.lowercase()

    fun trimWhitespace(input: String): String = input.trim()

    fun encodeBase64(input: String): String =
        Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    fun decodeBase64(input: String): String {
        return try {
            String(Base64.decode(input, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            input
        }
    }

    fun regexReplace(input: String, pattern: String, replacement: String): String {
        return try {
            input.replace(Regex(pattern), replacement)
        } catch (e: Exception) {
            input
        }
    }
}
