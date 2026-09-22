package com.squidink.alloy.core.common

import android.util.Log

/**
 * Privacy-first local diagnostic logger wrapper avoiding raw Log calls and cloud transmission.
 */
object Logger {

    fun d(tag: String, message: String) {
        Log.d(tag, redact(message))
    }

    fun i(tag: String, message: String) {
        Log.i(tag, redact(message))
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, redact(message), throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, redact(message), throwable)
    }

    private fun redact(message: String): String {
        // Redact potential PII or passwords before logging
        return message.replace(Regex("(?i)(password|secret|token)=\\w+"), "$1=REDACTED")
    }
}
