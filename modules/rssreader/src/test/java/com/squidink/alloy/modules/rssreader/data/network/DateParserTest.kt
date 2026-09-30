package com.squidink.alloy.modules.rssreader.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateParserTest {

    @Test
    fun `parses standard RFC-822 date correctly`() {
        val dateStr = "Tue, 29 Sep 2026 19:00:00 GMT"
        val epochMs = DateParser.parseDateToEpochMs(dateStr)
        assertTrue(epochMs > 0)
    }

    @Test
    fun `parses RFC-822 with numeric offset`() {
        val dateStr = "Tue, 29 Sep 2026 19:00:00 +0000"
        val epochMs = DateParser.parseDateToEpochMs(dateStr)
        assertTrue(epochMs > 0)
    }

    @Test
    fun `parses RFC-822 without day of week`() {
        val dateStr = "29 Sep 2026 19:00:00 GMT"
        val epochMs = DateParser.parseDateToEpochMs(dateStr)
        assertTrue(epochMs > 0)
    }

    @Test
    fun `parses ISO-8601 UTC date`() {
        val dateStr = "2026-09-29T19:00:00Z"
        val epochMs = DateParser.parseDateToEpochMs(dateStr)
        assertEquals(1790708400000L, epochMs)
    }

    @Test
    fun `parses ISO-8601 with milliseconds`() {
        val dateStr = "2026-09-29T19:00:00.000Z"
        val epochMs = DateParser.parseDateToEpochMs(dateStr)
        assertEquals(1790708400000L, epochMs)
    }

    @Test
    fun `returns fallback on invalid or blank string`() {
        val fallback = 123456789L
        assertEquals(fallback, DateParser.parseDateToEpochMs(null, fallback))
        assertEquals(fallback, DateParser.parseDateToEpochMs("", fallback))
        assertEquals(fallback, DateParser.parseDateToEpochMs("not a valid date", fallback))
    }
}
