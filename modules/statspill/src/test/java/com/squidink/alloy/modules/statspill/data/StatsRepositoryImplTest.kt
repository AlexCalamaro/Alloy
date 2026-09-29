package com.squidink.alloy.modules.statspill.data

import app.cash.turbine.test
import com.squidink.alloy.modules.statspill.data.datasource.BatteryDataSource
import com.squidink.alloy.modules.statspill.data.datasource.DiskDataSource
import com.squidink.alloy.modules.statspill.data.datasource.NetworkDataSource
import com.squidink.alloy.modules.statspill.data.datasource.SystemStatsDataSource
import com.squidink.alloy.modules.statspill.data.datasource.ThermalDataSource
import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class StatsRepositoryImplTest {

    private val systemStatsDataSource: SystemStatsDataSource = mockk()
    private val batteryDataSource: BatteryDataSource = mockk()
    private val networkDataSource: NetworkDataSource = mockk()
    private val diskDataSource: DiskDataSource = mockk()
    private val thermalDataSource: ThermalDataSource = mockk()

    private lateinit var repository: StatsRepositoryImpl

    private val systemStats = SystemStats(
        timestamp = 1000L,
        memoryUsedBytes = 2048L,
        memoryTotalBytes = 4096L,
        memoryPercent = 50.0f,
        cpuPercent = 30.0f
    )
    private val batteryInfo = BatteryInfo(level = 90, percentage = 90, isCharging = false)
    private val networkStats = NetworkStats(rxBytes = 100L, txBytes = 200L, rxBytesPerSecond = 50f, txBytesPerSecond = 20f)
    private val diskStats = DiskStats(totalBytes = 1000L, usedBytes = 400L, freeBytes = 600L, percentUsed = 40f)
    private val thermalStats = ThermalStats(batteryTemperature = 28.5f)

    @Before
    fun setUp() {
        every { systemStatsDataSource.observe() } returns flowOf(systemStats)
        every { batteryDataSource.observe() } returns flowOf(batteryInfo)
        every { networkDataSource.observe() } returns flowOf(networkStats)
        every { diskDataSource.observe() } returns flowOf(diskStats)
        every { thermalDataSource.observe() } returns flowOf(thermalStats)

        coEvery { systemStatsDataSource.read() } returns systemStats
        coEvery { batteryDataSource.read() } returns batteryInfo
        coEvery { networkDataSource.read() } returns networkStats
        coEvery { diskDataSource.read() } returns diskStats
        coEvery { thermalDataSource.read() } returns thermalStats

        repository = StatsRepositoryImpl(
            systemStatsDataSource = systemStatsDataSource,
            batteryDataSource = batteryDataSource,
            networkDataSource = networkDataSource,
            diskDataSource = diskDataSource,
            thermalDataSource = thermalDataSource
        )
    }

    @Test
    fun `observeCombinedTelemetry emits aggregated telemetry`() = runTest {
        repository.observeCombinedTelemetry().test {
            val telemetry = awaitItem()
            assertEquals(50.0f, telemetry.systemStats?.memoryPercent)
            assertEquals(30.0f, telemetry.systemStats?.cpuPercent)
            assertEquals(90, telemetry.batteryInfo.percentage)
            assertEquals(50f, telemetry.networkStats.rxBytesPerSecond)
            assertEquals(40f, telemetry.diskStats?.percentUsed)
            assertEquals(28.5f, telemetry.thermalStats?.batteryTemperature)
            awaitComplete()
        }
    }

    @Test
    fun `pollCombinedTelemetry returns latest values`() = runTest {
        val result = repository.pollCombinedTelemetry()

        assertNotNull(result.systemStats)
        assertEquals(30.0f, result.systemStats?.cpuPercent)
        assertEquals(90, result.batteryInfo.percentage)
        assertEquals(40f, result.diskStats?.percentUsed)
    }

    @Test
    fun `convenience methods return correct values`() = runTest {
        assertEquals(30.0f, repository.getCpuPercent())
        assertEquals(50.0f, repository.getMemoryPercent())
    }

    @Test
    fun `pollStats for specific category returns matching stat`() = runTest {
        val stat = repository.pollStats(StatCategory.SYSTEM)
        assertEquals(StatCategory.SYSTEM, stat.category)
        assertEquals(systemStats, stat)
    }
}
