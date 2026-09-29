package com.squidink.alloy.modules.statspill.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatErrorTest {

    @Test
    fun `StatError stores category correctly`() {
        val error = StatError(
            category = StatCategory.SYSTEM,
            type = ErrorType.READ_ERROR,
            message = "Failed to read system stats"
        )

        assertEquals(StatCategory.SYSTEM, error.category)
    }

    @Test
    fun `StatError stores type correctly`() {
        val error = StatError(
            category = StatCategory.POWER,
            type = ErrorType.PERMISSION_DENIED,
            message = "Permission not granted"
        )

        assertEquals(ErrorType.PERMISSION_DENIED, error.type)
    }

    @Test
    fun `StatError stores message correctly`() {
        val errorMessage = "Connection timeout after 5000ms"
        val error = StatError(
            category = StatCategory.NETWORK,
            type = ErrorType.TIMEOUT,
            message = errorMessage
        )

        assertEquals(errorMessage, error.message)
    }

    @Test
    fun `StatError timestamp defaults to current time`() {
        val before = System.currentTimeMillis()
        val error = StatError(
            category = StatCategory.STORAGE,
            type = ErrorType.DATA_UNAVAILABLE,
            message = "No data available"
        )
        val after = System.currentTimeMillis()

        assertTrue(error.timestamp >= before)
        assertTrue(error.timestamp <= after)
    }

    @Test
    fun `StatError can be created with custom timestamp`() {
        val customTimestamp = 1234567890L
        val error = StatError(
            category = StatCategory.THERMAL,
            type = ErrorType.INVALID_DATA,
            message = "Invalid temperature data",
            timestamp = customTimestamp
        )

        assertEquals(customTimestamp, error.timestamp)
    }

    @Test
    fun `ErrorType enum has all expected values`() {
        assertEquals(6, ErrorType.entries.size)
        assertNotNull(ErrorType.DATA_UNAVAILABLE)
        assertNotNull(ErrorType.READ_ERROR)
        assertNotNull(ErrorType.PERMISSION_DENIED)
        assertNotNull(ErrorType.TIMEOUT)
        assertNotNull(ErrorType.INVALID_DATA)
        assertNotNull(ErrorType.UNKNOWN)
    }
}
