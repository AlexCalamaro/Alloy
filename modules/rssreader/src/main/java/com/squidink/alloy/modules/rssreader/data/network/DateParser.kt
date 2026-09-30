package com.squidink.alloy.modules.rssreader.data.network

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

/**
 * Robust date parser supporting RFC-822, RFC-1123 (RSS 2.0) and ISO-8601 (Atom 1.0) date strings.
 */
object DateParser {

    private val rfcPatterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, d MMM yyyy HH:mm:ss z",
        "EEE, d MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss z",
        "dd MMM yyyy HH:mm:ss Z",
        "d MMM yyyy HH:mm:ss z",
        "d MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm z",
        "yyyy-MM-dd HH:mm:ss"
    )

    /**
     * Parses a date string into epoch milliseconds.
     * If the string is null, blank, or malformed, returns [fallbackMs].
     */
    fun parseDateToEpochMs(dateStr: String?, fallbackMs: Long = System.currentTimeMillis()): Long {
        if (dateStr.isNullOrBlank()) return fallbackMs
        val trimmed = dateStr.trim()

        // 1. Try ISO-8601 / Atom format via java.time
        try {
            return Instant.parse(trimmed).toEpochMilli()
        } catch (_: Exception) {
            // Not instant format (e.g. might have offset like +02:00)
        }

        try {
            return OffsetDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            // Not standard ISO offset
        }

        // 2. Try RFC-822 / RFC-1123 patterns
        for (pattern in rfcPatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = true
                }
                val date = sdf.parse(trimmed)
                if (date != null) {
                    return date.time
                }
            } catch (_: Exception) {
                // Try next pattern
            }
        }

        return fallbackMs
    }
}
