package com.squidink.alloy.modules.statspill.data

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.core.data.datasource.BatteryDataSource
import com.squidink.alloy.core.data.datasource.DiskDataSource
import com.squidink.alloy.core.data.datasource.NetworkDataSource
import com.squidink.alloy.core.data.datasource.SystemStatsDataSource
import com.squidink.alloy.core.data.datasource.ThermalDataSource
import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.StatType
import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [IStatsRepository] using modular data sources.
 *
 * This repository coordinates multiple specialized data sources:
 * - [SystemStatsDataSource] for CPU and memory statistics
 * - [BatteryDataSource] for battery information
 * - [NetworkDataSource] for network I/O statistics
 * - [DiskDataSource] for storage statistics
 * - [ThermalDataSource] for temperature statistics
 *
 * The repository handles:
 * - Data source coordination
 * - Thread dispatching
 * - Error handling
 * - Type-safe stat access
 */
@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val systemStatsDataSource: SystemStatsDataSource,
    private val batteryDataSource: BatteryDataSource,
    private val networkDataSource: NetworkDataSource,
    private val diskDataSource: DiskDataSource,
    private val thermalDataSource: ThermalDataSource
) : IStatsRepository {

    private val tag = "StatsRepositoryImpl"

    /**
     * Observe all statistics as a map by category.
     */
    override fun observeAllStats(): Flow<Map<StatCategory, StatType>> {
        return combine(
            systemStatsDataSource.observe(),
            batteryDataSource.observe(),
            networkDataSource.observe(),
            diskDataSource.observe(),
            thermalDataSource.observe()
        ) { system, battery, network, disk, thermal ->
            mapOf(
                StatCategory.SYSTEM to system,
                StatCategory.POWER to battery,
                StatCategory.NETWORK to network,
                StatCategory.STORAGE to disk,
                StatCategory.THERMAL to thermal
            )
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Observe statistics for a specific category.
     */
    override fun observeStats(category: StatCategory): Flow<StatType> {
        return when (category) {
            StatCategory.SYSTEM -> systemStatsDataSource.observe()
            StatCategory.POWER -> batteryDataSource.observe()
            StatCategory.NETWORK -> networkDataSource.observe()
            StatCategory.STORAGE -> diskDataSource.observe()
            StatCategory.THERMAL -> thermalDataSource.observe()
            StatCategory.CUSTOM -> emptyFlow() // Custom stats not implemented yet
        }
    }

    /**
     * Poll all statistics immediately.
     */
    override suspend fun pollAllStats(): Map<StatCategory, StatType> {
        return withContext(Dispatchers.IO) {
            try {
                mapOf(
                    StatCategory.SYSTEM to systemStatsDataSource.read(),
                    StatCategory.POWER to batteryDataSource.read(),
                    StatCategory.NETWORK to networkDataSource.read(),
                    StatCategory.STORAGE to diskDataSource.read(),
                    StatCategory.THERMAL to thermalDataSource.read()
                )
            } catch (e: Exception) {
                Logger.e(tag, "Error polling all stats", e)
                emptyMap()
            }
        }
    }

    /**
     * Poll statistics for a specific category.
     */
    override suspend fun pollStats(category: StatCategory): StatType {
        return withContext(Dispatchers.IO) {
            try {
                when (category) {
                    StatCategory.SYSTEM -> systemStatsDataSource.read()
                    StatCategory.POWER -> batteryDataSource.read()
                    StatCategory.NETWORK -> networkDataSource.read()
                    StatCategory.STORAGE -> diskDataSource.read()
                    StatCategory.THERMAL -> thermalDataSource.read()
                    StatCategory.CUSTOM -> throw UnsupportedOperationException("Custom stats not implemented")
                }
            } catch (e: Exception) {
                Logger.e(tag, "Error polling stats for category: $category", e)
                createDefaultStat(category)
            }
        }
    }

    /**
     * Observe system statistics (CPU, memory) as a Flow.
     */
    override fun observeSystemStats(): Flow<SystemStats> {
        return systemStatsDataSource.observe()
    }

    /**
     * Poll system statistics once.
     */
    override suspend fun pollSystemStats(): SystemStats {
        return withContext(Dispatchers.IO) {
            try {
                systemStatsDataSource.read()
            } catch (e: Exception) {
                Logger.e(tag, "Error polling system stats", e)
                SystemStats(
                    timestamp = System.currentTimeMillis(),
                    memoryUsedBytes = 0,
                    memoryTotalBytes = 0,
                    memoryPercent = 0f,
                    cpuPercent = 0f
                )
            }
        }
    }

    /**
     * Get memory usage percentage.
     */
    override suspend fun getMemoryPercent(): Float {
        return withContext(Dispatchers.IO) {
            try {
                systemStatsDataSource.read().memoryPercent
            } catch (e: Exception) {
                Logger.e(tag, "Error getting memory percent", e)
                0f
            }
        }
    }

    /**
     * Get CPU usage percentage.
     */
    override suspend fun getCpuPercent(): Float {
        return withContext(Dispatchers.IO) {
            try {
                systemStatsDataSource.read().cpuPercent
            } catch (e: Exception) {
                Logger.e(tag, "Error getting CPU percent", e)
                0f
            }
        }
    }

    /**
     * Observe battery information.
     */
    override fun observeBatteryInfo(): Flow<BatteryInfo> {
        return batteryDataSource.observe()
    }

    /**
     * Observe network statistics.
     */
    override fun observeNetworkStats(): Flow<NetworkStats> {
        return networkDataSource.observe()
    }

    /**
     * Observe disk/storage statistics.
     */
    override fun observeDiskStats(): Flow<DiskStats> {
        return diskDataSource.observe()
    }

    /**
     * Poll disk/storage statistics once.
     */
    override suspend fun pollDiskStats(): DiskStats {
        return withContext(Dispatchers.IO) {
            try {
                diskDataSource.read()
            } catch (e: Exception) {
                Logger.e(tag, "Error polling disk stats", e)
                DiskStats(
                    timestamp = System.currentTimeMillis(),
                    totalBytes = 0,
                    usedBytes = 0,
                    freeBytes = 0,
                    percentUsed = 0f
                )
            }
        }
    }

    /**
     * Observe thermal/temperature statistics.
     */
    override fun observeThermalStats(): Flow<ThermalStats> {
        return thermalDataSource.observe()
    }

    /**
     * Poll thermal/temperature statistics once.
     */
    override suspend fun pollThermalStats(): ThermalStats {
        return withContext(Dispatchers.IO) {
            try {
                thermalDataSource.read()
            } catch (e: Exception) {
                Logger.e(tag, "Error polling thermal stats", e)
                ThermalStats(
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * Create a default stat value for the given category.
     */
    private fun createDefaultStat(category: StatCategory): StatType {
        return when (category) {
            StatCategory.SYSTEM -> SystemStats(
                timestamp = System.currentTimeMillis(),
                memoryUsedBytes = 0,
                memoryTotalBytes = 0,
                memoryPercent = 0f,
                cpuPercent = 0f
            )
            StatCategory.POWER -> BatteryInfo(
                timestamp = System.currentTimeMillis()
            )
            StatCategory.NETWORK -> NetworkStats(
                timestamp = System.currentTimeMillis()
            )
            StatCategory.STORAGE -> DiskStats(
                timestamp = System.currentTimeMillis()
            )
            StatCategory.THERMAL -> ThermalStats(
                timestamp = System.currentTimeMillis()
            )
            StatCategory.CUSTOM -> throw UnsupportedOperationException("Custom stats not implemented")
        }
    }
}
