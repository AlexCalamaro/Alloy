package com.squidink.alloy.modules.statspill

import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.domain.repository.SystemStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation of IStatsRepository for testing.
 */
open class FakeStatsRepository : IStatsRepository {
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
}
