package com.squidink.alloy.modules.statspill.domain.model

/**
 * Represents an error that occurred while fetching or processing statistics.
 *
 * @property category The stat category where the error occurred
 * @property type The type of error
 * @property message Human-readable error message
 * @property timestamp When the error occurred
 */
data class StatError(
    val category: StatCategory,
    val type: ErrorType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Types of errors that can occur in the stats system.
 */
enum class ErrorType {
    /** Data source returned no valid data */
    DATA_UNAVAILABLE,

    /** Error reading from data source (exception occurred) */
    READ_ERROR,

    /** Required permission not granted */
    PERMISSION_DENIED,

    /** Request timed out */
    TIMEOUT,

    /** Invalid or malformed data received */
    INVALID_DATA,

    /** General unknown error */
    UNKNOWN
}
