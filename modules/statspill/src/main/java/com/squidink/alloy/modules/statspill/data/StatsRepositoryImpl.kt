package com.squidink.alloy.modules.statspill.data

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.core.data.datasource.BatteryDataSource
import com.squidink.alloy.core.data.datasource.SystemStatsDataSource
import com.squidink.alloy.core.data.datasource.SystemStatsData
import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.domain.repository.SystemStats
import com.squidink.alloy.core.proc.NetStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [IStatsRepository] using data sources.
 *
 * This repository coordinates between:
 * - [SystemStatsDataSource] for CPU, memory, and network statistics
 * - [BatteryDataSource] for battery information
 *
 * The repository handles:
 * - Data source coordination
 * - Thread dispatching
 * - Error handling
 * - Domain model mapping
 */
@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val systemStatsDataSource: SystemStatsDataSource,
    private val batteryDataSource: BatteryDataSource
) : IStatsRepository {

    private val tag = "StatsRepositoryImpl"

    /**
     * Observe system statistics as a Flow.
     *
     * Polls system stats at 1Hz for real-time telemetry.
     *
     * @return Flow emitting system statistics
     */
    override fun observeSystemStats(): Flow<SystemStats> {
        return systemStatsDataSource.currentStats
            .map { stats ->
                SystemStats(
                    memoryUsedBytes = stats.memoryUsedBytes,
                    memoryTotalBytes = stats.memoryTotalBytes,
                    memoryPercent = stats.memoryPercent,
                    cpuPercent = stats.cpuPercent,
                    timestamp = stats.timestamp
                )
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Poll system statistics once.
     *
     * Triggers an immediate poll from the data source and returns the result.
     *
     * @return Current system statistics
     */
    override suspend fun pollSystemStats(): SystemStats {
        return withContext(Dispatchers.IO) {
            try {
                val stats = systemStatsDataSource.pollStats()
                SystemStats(
                    memoryUsedBytes = stats.memoryUsedBytes,
                    memoryTotalBytes = stats.memoryTotalBytes,
                    memoryPercent = stats.memoryPercent,
                    cpuPercent = stats.cpuPercent,
                    timestamp = stats.timestamp
                )
            } catch (e: Exception) {
                Logger.e(tag, "Error polling system stats", e)
                SystemStats(
                    memoryUsedBytes = 0,
                    memoryTotalBytes = 0,
                    memoryPercent = 0f,
                    cpuPercent = 0f,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * Get memory usage percentage.
     *
     * @return Memory usage as percentage (0.0 to 100.0)
     */
    override suspend fun getMemoryPercent(): Float {
        return withContext(Dispatchers.IO) {
            try {
                val stats = systemStatsDataSource.pollStats()
                stats.memoryPercent
            } catch (e: Exception) {
                Logger.e(tag, "Error getting memory percent", e)
                0f
            }
        }
    }

    /**
     * Get CPU usage percentage.
     *
     * @return CPU usage as percentage (0.0 to 100.0)
     */
    override suspend fun getCpuPercent(): Float {
        return withContext(Dispatchers.IO) {
            try {
                val stats = systemStatsDataSource.pollStats()
                stats.cpuPercent
            } catch (e: Exception) {
                Logger.e(tag, "Error getting CPU percent", e)
                0f
            }
        }
    }

    /**
     * Observe battery information.
     *
     * @return Flow emitting battery info updates
     */
    fun observeBatteryInfo(): Flow<com.squidink.alloy.core.data.datasource.BatteryInfo> {
        return batteryDataSource.batteryInfo
    }

    /**
     * Get current battery information.
     *
     * @return Current battery info
     */
    suspend fun getBatteryInfo(): com.squidink.alloy.core.data.datasource.BatteryInfo {
        return withContext(Dispatchers.IO) {
            batteryDataSource.fetchById(BATTERY_CACHE_KEY) 
                ?: com.squidink.alloy.core.data.datasource.BatteryInfo()
        }
    }

    /**
     * Observe network statistics.
     *
     * @return Flow emitting network stats updates
     */
    fun observeNetworkStats(): Flow<NetStats> {
        return systemStatsDataSource.currentStats.map { it.netStats }
    }

    companion object {
        private const val BATTERY_CACHE_KEY = "current_battery"
    }
}
