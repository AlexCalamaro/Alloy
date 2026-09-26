package com.squidink.alloy.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * System statistics data.
 */
data class SystemStats(
    val memoryUsedBytes: Long,
    val memoryTotalBytes: Long,
    val memoryPercent: Float,
    val cpuPercent: Float,
    val timestamp: Long
)

/**
 * Repository interface for system statistics operations.
 */
interface IStatsRepository {
    /**
     * Observe system statistics as a Flow.
     *
     * @return Flow emitting system stats updates
     */
    fun observeSystemStats(): Flow<SystemStats>
    
    /**
     * Poll system statistics once.
     *
     * @return Current system statistics
     */
    suspend fun pollSystemStats(): SystemStats
    
    /**
     * Get memory usage percentage.
     *
     * @return Memory usage as percentage (0.0 to 100.0)
     */
    suspend fun getMemoryPercent(): Float
    
    /**
     * Get CPU usage percentage.
     *
     * @return CPU usage as percentage (0.0 to 100.0)
     */
    suspend fun getCpuPercent(): Float
    
    /**
     * Observe battery information.
     *
     * @return Flow emitting battery info updates
     */
    fun observeBatteryInfo(): Flow<BatteryInfo>
    
    /**
     * Observe network statistics.
     *
     * @return Flow emitting network stats updates
     */
    fun observeNetworkStats(): Flow<NetStats>
}
