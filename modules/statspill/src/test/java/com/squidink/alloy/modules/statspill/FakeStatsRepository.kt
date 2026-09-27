package com.squidink.alloy.modules.statspill

import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.StatType
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation of IStatsRepository for testing.
 * Provides mock data for system statistics operations.
 */
class FakeStatsRepository : IStatsRepository {
    override fun observeAllStats(): Flow<Map<StatCategory, StatType>> = flowOf(
        mapOf(
            StatCategory.SYSTEM to SystemStats(
                memoryUsedBytes = 6000000 * 1024,
                memoryTotalBytes = 16000000 * 1024,
                memoryPercent = 37.5f,
                cpuPercent = 25.5f,
                timestamp = System.currentTimeMillis()
            ),
            StatCategory.POWER to BatteryInfo(),
            StatCategory.NETWORK to NetworkStats(),
            StatCategory.STORAGE to DiskStats(),
            StatCategory.THERMAL to ThermalStats()
        )
    )
    
    override fun observeStats(category: StatCategory): Flow<StatType> = when (category) {
        StatCategory.SYSTEM -> flowOf(SystemStats(
            memoryUsedBytes = 6000000 * 1024,
            memoryTotalBytes = 16000000 * 1024,
            memoryPercent = 37.5f,
            cpuPercent = 25.5f,
            timestamp = System.currentTimeMillis()
        ))
        StatCategory.POWER -> flowOf(BatteryInfo())
        StatCategory.NETWORK -> flowOf(NetworkStats())
        StatCategory.STORAGE -> flowOf(DiskStats())
        StatCategory.THERMAL -> flowOf(ThermalStats())
        StatCategory.CUSTOM -> flowOf(object : StatType {
            override val id: String = "custom"
            override val name: String = "Custom"
            override val category: StatCategory = StatCategory.CUSTOM
            override val timestamp: Long = System.currentTimeMillis()
        })
    }
    
    override suspend fun pollAllStats(): Map<StatCategory, StatType> = mapOf(
        StatCategory.SYSTEM to SystemStats(
            memoryUsedBytes = 6000000 * 1024,
            memoryTotalBytes = 16000000 * 1024,
            memoryPercent = 37.5f,
            cpuPercent = 25.5f,
            timestamp = System.currentTimeMillis()
        ),
        StatCategory.POWER to BatteryInfo(),
        StatCategory.NETWORK to NetworkStats(),
        StatCategory.STORAGE to DiskStats(),
        StatCategory.THERMAL to ThermalStats()
    )
    
    override suspend fun pollStats(category: StatCategory): StatType = when (category) {
        StatCategory.SYSTEM -> SystemStats(
            memoryUsedBytes = 6000000 * 1024,
            memoryTotalBytes = 16000000 * 1024,
            memoryPercent = 37.5f,
            cpuPercent = 25.5f,
            timestamp = System.currentTimeMillis()
        )
        StatCategory.POWER -> BatteryInfo()
        StatCategory.NETWORK -> NetworkStats()
        StatCategory.STORAGE -> DiskStats()
        StatCategory.THERMAL -> ThermalStats()
        StatCategory.CUSTOM -> throw UnsupportedOperationException("Custom stats not implemented")
    }
    
    override fun observeSystemStats(): Flow<SystemStats> = flowOf(
        SystemStats(
            memoryUsedBytes = 6000000 * 1024,
            memoryTotalBytes = 16000000 * 1024,
            memoryPercent = 37.5f,
            cpuPercent = 25.5f,
            timestamp = System.currentTimeMillis()
        )
    )
    
    override suspend fun pollSystemStats(): SystemStats = SystemStats(
        memoryUsedBytes = 6000000 * 1024,
        memoryTotalBytes = 16000000 * 1024,
        memoryPercent = 37.5f,
        cpuPercent = 25.5f,
        timestamp = System.currentTimeMillis()
    )
    
    override suspend fun getMemoryPercent(): Float = 37.5f
    override suspend fun getCpuPercent(): Float = 25.5f
    
    override fun observeBatteryInfo(): Flow<BatteryInfo> = flowOf(BatteryInfo())
    
    override fun observeNetworkStats(): Flow<NetworkStats> = flowOf(NetworkStats())
    
    override fun observeDiskStats(): Flow<DiskStats> = flowOf(DiskStats())
    
    override suspend fun pollDiskStats(): DiskStats = DiskStats()
    
    override fun observeThermalStats(): Flow<ThermalStats> = flowOf(ThermalStats())
    
    override suspend fun pollThermalStats(): ThermalStats = ThermalStats()
}
