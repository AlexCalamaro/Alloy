package com.squidink.alloy.modules.statspill.data

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.statspill.data.datasource.BatteryDataSource
import com.squidink.alloy.modules.statspill.data.datasource.DiskDataSource
import com.squidink.alloy.modules.statspill.data.datasource.NetworkDataSource
import com.squidink.alloy.modules.statspill.data.datasource.SystemStatsDataSource
import com.squidink.alloy.modules.statspill.data.datasource.ThermalDataSource
import com.squidink.alloy.modules.statspill.domain.model.BatteryInfo
import com.squidink.alloy.modules.statspill.domain.model.CombinedTelemetry
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import com.squidink.alloy.modules.statspill.domain.model.ErrorType
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import com.squidink.alloy.modules.statspill.domain.model.StatCategory
import com.squidink.alloy.modules.statspill.domain.model.StatError
import com.squidink.alloy.modules.statspill.domain.model.StatType
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import com.squidink.alloy.modules.statspill.domain.model.ThermalStats
import com.squidink.alloy.modules.statspill.domain.repository.IStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [IStatsRepository] coordinating specialized reactive data sources.
 */
@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val systemStatsDataSource: SystemStatsDataSource,
    private val batteryDataSource: BatteryDataSource,
    private val networkDataSource: NetworkDataSource,
    private val diskDataSource: DiskDataSource,
    private val thermalDataSource: ThermalDataSource
) : IStatsRepository {

    private val _errorFlow = MutableSharedFlow<StatError>(replay = 0)
    override fun observeErrors(): Flow<StatError> = _errorFlow.asSharedFlow()

    private suspend fun emitError(category: StatCategory, type: ErrorType, message: String) {
        _errorFlow.emit(StatError(category, type, message))
    }

    /**
     * Observe combined telemetry snapshot as a reactive stream.
     */
    override fun observeCombinedTelemetry(): Flow<CombinedTelemetry> {
        return combine(
            systemStatsDataSource.observe(),
            batteryDataSource.observe(),
            networkDataSource.observe(),
            diskDataSource.observe(),
            thermalDataSource.observe()
        ) { system, battery, network, disk, thermal ->
            CombinedTelemetry(
                systemStats = system,
                batteryInfo = battery,
                networkStats = network,
                diskStats = disk,
                thermalStats = thermal,
                timestamp = System.currentTimeMillis()
            )
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Poll all telemetry immediately.
     */
    override suspend fun pollCombinedTelemetry(): CombinedTelemetry {
        return withContext(Dispatchers.IO) {
            CombinedTelemetry(
                systemStats = systemStatsDataSource.read(),
                batteryInfo = batteryDataSource.read(),
                networkStats = networkDataSource.read(),
                diskStats = diskDataSource.read(),
                thermalStats = thermalDataSource.read(),
                timestamp = System.currentTimeMillis()
            )
        }
    }

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
            StatCategory.CUSTOM -> emptyFlow()
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
                Logger.e(TAG, "Error polling all stats", e)
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
                Logger.e(TAG, "Error polling stats for category: $category", e)
                emitError(category, ErrorType.READ_ERROR, e.message ?: "Unknown error")
                createDefaultStat(category)
            }
        }
    }

    override fun observeSystemStats(): Flow<SystemStats> = systemStatsDataSource.observe()

    override suspend fun pollSystemStats(): SystemStats {
        return withContext(Dispatchers.IO) {
            try {
                systemStatsDataSource.read()
            } catch (e: Exception) {
                Logger.e(TAG, "Error polling system stats", e)
                SystemStats(
                    timestamp = System.currentTimeMillis(),
                    memoryUsedBytes = 0L,
                    memoryTotalBytes = 0L,
                    memoryPercent = 0f,
                    cpuPercent = 0f
                )
            }
        }
    }

    override suspend fun getMemoryPercent(): Float {
        return withContext(Dispatchers.IO) {
            try {
                systemStatsDataSource.read().memoryPercent
            } catch (e: Exception) {
                Logger.e(TAG, "Error getting memory percent", e)
                0f
            }
        }
    }

    override suspend fun getCpuPercent(): Float {
        return withContext(Dispatchers.IO) {
            try {
                systemStatsDataSource.read().cpuPercent
            } catch (e: Exception) {
                Logger.e(TAG, "Error getting CPU percent", e)
                0f
            }
        }
    }

    override fun observeBatteryInfo(): Flow<BatteryInfo> = batteryDataSource.observe()

    override fun observeNetworkStats(): Flow<NetworkStats> = networkDataSource.observe()

    override fun observeDiskStats(): Flow<DiskStats> = diskDataSource.observe()

    override suspend fun pollDiskStats(): DiskStats {
        return withContext(Dispatchers.IO) {
            try {
                diskDataSource.read()
            } catch (e: Exception) {
                Logger.e(TAG, "Error polling disk stats", e)
                DiskStats(
                    timestamp = System.currentTimeMillis(),
                    totalBytes = 0L,
                    usedBytes = 0L,
                    freeBytes = 0L,
                    percentUsed = 0f
                )
            }
        }
    }

    override fun observeThermalStats(): Flow<ThermalStats> = thermalDataSource.observe()

    override suspend fun pollThermalStats(): ThermalStats {
        return withContext(Dispatchers.IO) {
            try {
                thermalDataSource.read()
            } catch (e: Exception) {
                Logger.e(TAG, "Error polling thermal stats", e)
                ThermalStats(timestamp = System.currentTimeMillis())
            }
        }
    }

    private fun createDefaultStat(category: StatCategory): StatType {
        return when (category) {
            StatCategory.SYSTEM -> SystemStats(
                timestamp = System.currentTimeMillis(),
                memoryUsedBytes = 0L,
                memoryTotalBytes = 0L,
                memoryPercent = 0f,
                cpuPercent = 0f
            )
            StatCategory.POWER -> BatteryInfo(timestamp = System.currentTimeMillis())
            StatCategory.NETWORK -> NetworkStats(timestamp = System.currentTimeMillis())
            StatCategory.STORAGE -> DiskStats(timestamp = System.currentTimeMillis())
            StatCategory.THERMAL -> ThermalStats(timestamp = System.currentTimeMillis())
            StatCategory.CUSTOM -> throw UnsupportedOperationException("Custom stats not implemented")
        }
    }

    companion object {
        private const val TAG = "StatsRepositoryImpl"
    }
}
