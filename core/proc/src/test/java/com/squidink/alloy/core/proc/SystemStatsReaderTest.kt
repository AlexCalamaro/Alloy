package com.squidink.alloy.core.proc

import android.app.ActivityManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SystemStatsReaderTest {

    private val context: Context = mockk(relaxed = true)
    private val activityManager: ActivityManager = mockk(relaxed = true)
    private lateinit var statsReader: SystemStatsReader

    @Before
    fun setUp() {
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        statsReader = SystemStatsReader(context)
    }

    @Test
    fun `readMemInfo calculates KB values from ActivityManager MemoryInfo`() {
        val memoryInfoSlot = slot<ActivityManager.MemoryInfo>()
        every { activityManager.getMemoryInfo(capture(memoryInfoSlot)) } answers {
            memoryInfoSlot.captured.totalMem = 8L * 1024 * 1024 * 1024 // 8GB
            memoryInfoSlot.captured.availMem = 4L * 1024 * 1024 * 1024 // 4GB
        }

        val memInfo = statsReader.readMemInfo()

        assertNotNull(memInfo)
        assertEquals(8L * 1024 * 1024, memInfo.totalMemKb)
        assertEquals(4L * 1024 * 1024, memInfo.freeMemKb)
        assertEquals(4L * 1024 * 1024, memInfo.availableMemKb)
    }

    @Test
    fun `readCpuCores returns at least 1 processor core`() {
        val cores = statsReader.readCpuCores()
        assertTrue("CPU cores must be at least 1", cores >= 1)
    }

    @Test
    fun `readNetworkStats returns valid NetStats structure`() {
        val netStats = statsReader.readNetworkStats()
        
        assertNotNull(netStats)
        assertTrue(netStats.rxBytes >= -1)
        assertTrue(netStats.txBytes >= -1)
        assertEquals(0f, netStats.rxBytesPerSecond, 0.001f)
        assertEquals(0f, netStats.txBytesPerSecond, 0.001f)
    }

    @Test
    fun `readCpuUsagePercent establishes baseline on first call`() {
        val firstTick = statsReader.readCpuUsagePercent()
        assertNull("First tick must return null to establish monotonic baseline", firstTick)
    }
}
