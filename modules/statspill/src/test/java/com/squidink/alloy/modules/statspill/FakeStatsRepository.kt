package com.squidink.alloy.modules.statspill

import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import com.squidink.alloy.modules.statspill.domain.model.CombinedTelemetry
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.StatError
import com.squidink.alloy.modules.statspill.domain.model.StatType
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation of IStatsRepository for unit testing.
 */
class FakeStatsRepository : IStatsRepository {

    private val _errorFlow = MutableSharedFlow<StatError>(replay = 0)
    override fun observeErrors(): Flow<StatError> = _errorFlow.asSharedFlow()

    suspend fun emitError(error: StatError) {
        _errorFlow.emit(error)
    }

    val defaultTelemetry = CombinedTelemetry(
        systemStats = SystemStats(
            memoryUsedBytes = 6000000L * 1024L,
            memoryTotalBytes = 16000000L * 1024L,
            memoryPercent = 37.5f,
            cpuPercent = 25.5f,
            timestamp = 1234567890L
        ),
        batteryInfo = BatteryInfo(level = 80, percentage = 80, isCharging = true),
        networkStats = NetworkStats(rxBytesPerSecond = 1024f, txBytesPerSecond = 512f),
        diskStats = DiskStats(totalBytes = 100_000_000_000L, usedBytes = 40_000_000_000L, percentUsed = 40.0f),
        thermalStats = ThermalStats(batteryTemperature = 31.5f),
        timestamp = 1234567890L
    )

    override fun observeCombinedTelemetry(): Flow<CombinedTelemetry> = flowOf(defaultTelemetry)

    override suspend fun pollCombinedTelemetry(): CombinedTelemetry = defaultTelemetry

    override fun observeAllStats(): Flow<Map<StatCategory, StatType>> = flowOf(
        mapOf(
            StatCategory.SYSTEM to defaultTelemetry.systemStats!!,
            StatCategory.POWER to defaultTelemetry.batteryInfo,
            StatCategory.NETWORK to defaultTelemetry.networkStats,
            StatCategory.STORAGE to defaultTelemetry.diskStats!!,
            StatCategory.THERMAL to defaultTelemetry.thermalStats!!
        )
    )

    override fun observeStats(category: StatCategory): Flow<StatType> = when (category) {
        StatCategory.SYSTEM -> flowOf(defaultTelemetry.systemStats!!)
        StatCategory.POWER -> flowOf(defaultTelemetry.batteryInfo)
        StatCategory.NETWORK -> flowOf(defaultTelemetry.networkStats)
        StatCategory.STORAGE -> flowOf(defaultTelemetry.diskStats!!)
        StatCategory.THERMAL -> flowOf(defaultTelemetry.thermalStats!!)
        StatCategory.CUSTOM -> flowOf(object : StatType {
            override val id: String = "custom"
            override val name: String = "Custom"
            override val category: StatCategory = StatCategory.CUSTOM
            override val timestamp: Long = System.currentTimeMillis()
        })
    }

    override suspend fun pollAllStats(): Map<StatCategory, StatType> = mapOf(
        StatCategory.SYSTEM to defaultTelemetry.systemStats!!,
        StatCategory.POWER to defaultTelemetry.batteryInfo,
        StatCategory.NETWORK to defaultTelemetry.networkStats,
        StatCategory.STORAGE to defaultTelemetry.diskStats!!,
        StatCategory.THERMAL to defaultTelemetry.thermalStats!!
    )

    override suspend fun pollStats(category: StatCategory): StatType = when (category) {
        StatCategory.SYSTEM -> defaultTelemetry.systemStats!!
        StatCategory.POWER -> defaultTelemetry.batteryInfo
        StatCategory.NETWORK -> defaultTelemetry.networkStats
        StatCategory.STORAGE -> defaultTelemetry.diskStats!!
        StatCategory.THERMAL -> defaultTelemetry.thermalStats!!
        StatCategory.CUSTOM -> throw UnsupportedOperationException("Custom stats not implemented")
    }

    override fun observeSystemStats(): Flow<SystemStats> = flowOf(defaultTelemetry.systemStats!!)

    override suspend fun pollSystemStats(): SystemStats = defaultTelemetry.systemStats!!

    override suspend fun getMemoryPercent(): Float = 37.5f
    override suspend fun getCpuPercent(): Float = 25.5f

    override fun observeBatteryInfo(): Flow<BatteryInfo> = flowOf(defaultTelemetry.batteryInfo)

    override fun observeNetworkStats(): Flow<NetworkStats> = flowOf(defaultTelemetry.networkStats)

    override fun observeDiskStats(): Flow<DiskStats> = flowOf(defaultTelemetry.diskStats!!)

    override suspend fun pollDiskStats(): DiskStats = defaultTelemetry.diskStats!!

    override fun observeThermalStats(): Flow<ThermalStats> = flowOf(defaultTelemetry.thermalStats!!)

    override suspend fun pollThermalStats(): ThermalStats = defaultTelemetry.thermalStats!!
}
