package com.squidink.alloy.modules.statspill.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DiskStatsTest {

    @Test
    fun `DiskStats has correct default id and name`() {
        val stats = DiskStats()

        assertEquals("disk", stats.id)
        assertEquals("Storage", stats.name)
        assertEquals(StatCategory.STORAGE, stats.category)
    }

    @Test
    fun `DiskStats implements StatType interface`() {
        val stats: StatType = DiskStats(
            timestamp = 1234567890L,
            totalBytes = 1000L,
            usedBytes = 500L,
            freeBytes = 500L,
            percentUsed = 50f
        )

        assertNotNull(stats)
        assertEquals(StatCategory.STORAGE, stats.category)
    }

    @Test
    fun `DiskStats stores values correctly`() {
        val stats = DiskStats(
            timestamp = 1234567890L,
            totalBytes = 100_000_000_000L,
            usedBytes = 60_000_000_000L,
            freeBytes = 40_000_000_000L,
            percentUsed = 60f
        )

        assertEquals(100_000_000_000L, stats.totalBytes)
        assertEquals(60_000_000_000L, stats.usedBytes)
        assertEquals(40_000_000_000L, stats.freeBytes)
        assertEquals(60f, stats.percentUsed)
    }

    @Test
    fun `percentFree calculates correctly`() {
        val stats = DiskStats(
            totalBytes = 100L,
            usedBytes = 60L,
            freeBytes = 40L,
            percentUsed = 60f
        )

        assertEquals(40f, stats.percentFree, 0.01f)
    }

    @Test
    fun `percentFree handles zero total bytes`() {
        val stats = DiskStats(
            totalBytes = 0L,
            usedBytes = 0L,
            freeBytes = 0L,
            percentUsed = 0f
        )

        assertEquals(0f, stats.percentFree, 0.01f)
    }

    @Test
    fun `formatUsedBytes formats GB correctly`() {
        val stats = DiskStats(
            totalBytes = 100_000_000_000L,
            usedBytes = 50_000_000_000L,
            freeBytes = 50_000_000_000L,
            percentUsed = 50f
        )

        assertEquals("46.6 GB", stats.formatUsedBytes())
    }

    @Test
    fun `formatUsedBytes formats MB correctly`() {
        val stats = DiskStats(
            totalBytes = 100_000_000L,
            usedBytes = 50_000_000L,
            freeBytes = 50_000_000L,
            percentUsed = 50f
        )

        assertEquals("47.7 MB", stats.formatUsedBytes())
    }

    @Test
    fun `formatUsedBytes formats KB correctly`() {
        val stats = DiskStats(
            totalBytes = 100_000L,
            usedBytes = 50_000L,
            freeBytes = 50_000L,
            percentUsed = 50f
        )

        assertEquals("48.8 KB", stats.formatUsedBytes())
    }

    @Test
    fun `formatUsedBytes formats bytes correctly`() {
        val stats = DiskStats(
            totalBytes = 500L,
            usedBytes = 250L,
            freeBytes = 250L,
            percentUsed = 50f
        )

        assertEquals("250 B", stats.formatUsedBytes())
    }

    @Test
    fun `formatTotalBytes formats correctly`() {
        val stats = DiskStats(
            totalBytes = 100_000_000_000L,
            usedBytes = 0L,
            freeBytes = 100_000_000_000L,
            percentUsed = 0f
        )

        assertEquals("93.1 GB", stats.formatTotalBytes())
    }

    @Test
    fun `DiskStats default values are correct`() {
        val stats = DiskStats()

        assertEquals(0L, stats.totalBytes)
        assertEquals(0L, stats.usedBytes)
        assertEquals(0L, stats.freeBytes)
        assertEquals(0f, stats.percentUsed)
        assertEquals(0f, stats.percentFree)
    }
}
