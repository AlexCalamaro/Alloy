package com.squidink.alloy.modules.statspill.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SystemStatsTest {

    @Test
    fun `SystemStats has correct default id and name`() {
        val stats = SystemStats(
            timestamp = 1234567890L,
            memoryUsedBytes = 0L,
            memoryTotalBytes = 0L,
            memoryPercent = 0f,
            cpuPercent = 0f
        )

        assertEquals("system", stats.id)
        assertEquals("System", stats.name)
        assertEquals(StatCategory.SYSTEM, stats.category)
    }

    @Test
    fun `SystemStats implements StatType interface`() {
        val stats: StatType = SystemStats(
            timestamp = 1234567890L,
            memoryUsedBytes = 1000L,
            memoryTotalBytes = 2000L,
            memoryPercent = 50f,
            cpuPercent = 25f
        )

        assertNotNull(stats)
        assertEquals(StatCategory.SYSTEM, stats.category)
    }

    @Test
    fun `SystemStats stores memory and CPU values correctly`() {
        val stats = SystemStats(
            timestamp = 1234567890L,
            memoryUsedBytes = 4_000_000_000L,
            memoryTotalBytes = 8_000_000_000L,
            memoryPercent = 50f,
            cpuPercent = 75f
        )

        assertEquals(4_000_000_000L, stats.memoryUsedBytes)
        assertEquals(8_000_000_000L, stats.memoryTotalBytes)
        assertEquals(50f, stats.memoryPercent)
        assertEquals(75f, stats.cpuPercent)
    }

    @Test
    fun `SystemStats timestamp is stored correctly`() {
        val timestamp = System.currentTimeMillis()
        val stats = SystemStats(
            timestamp = timestamp,
            memoryUsedBytes = 0L,
            memoryTotalBytes = 0L,
            memoryPercent = 0f,
            cpuPercent = 0f
        )

        assertEquals(timestamp, stats.timestamp)
    }
}
