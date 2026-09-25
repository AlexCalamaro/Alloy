package com.squidink.alloy.modules.statspill.data

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.core.domain.repository.IStatsRepository
import com.squidink.alloy.core.domain.repository.SystemStats
import com.squidink.alloy.core.proc.MemInfo
import com.squidink.alloy.core.proc.SystemStatsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [IStatsRepository] using SystemStatsReader.
 *
 * This is the data layer implementation that handles:
 * - Reading system statistics using Android APIs (ActivityManager, TrafficStats, Debug)
 * - Thread dispatching
 * - Error handling
 */
class StatsRepositoryImpl @Inject constructor(
    private val systemStatsReader: SystemStatsReader
) : IStatsRepository {
    
    private val tag = "StatsRepositoryImpl"
    
    override fun observeSystemStats(): Flow<SystemStats> = flow {
        while (true) {
            val stats = pollSystemStats()
            emit(stats)
            kotlinx.coroutines.delay(1000L) // 1Hz polling
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun pollSystemStats(): SystemStats {
        return withContext(Dispatchers.IO) {
            try {
                val mem = systemStatsReader.readMemInfo()
                val cpu = systemStatsReader.readCpuUsagePercent() ?: 0f
                
                val totalBytes = mem.totalMemKb * 1024
                val availableBytes = mem.availableMemKb * 1024
                val usedBytes = totalBytes - availableBytes
                val memPercent = if (totalBytes > 0) {
                    (usedBytes.toFloat() / totalBytes.toFloat()) * 100f
                } else 0f
                
                SystemStats(
                    memoryUsedBytes = usedBytes,
                    memoryTotalBytes = totalBytes,
                    memoryPercent = memPercent,
                    cpuPercent = cpu,
                    timestamp = System.currentTimeMillis()
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
    
    override suspend fun getMemoryPercent(): Float {
        return withContext(Dispatchers.IO) {
            try {
                val mem = systemStatsReader.readMemInfo()
                val totalBytes = mem.totalMemKb * 1024
                val availableBytes = mem.availableMemKb * 1024
                val usedBytes = totalBytes - availableBytes
                
                if (totalBytes > 0) {
                    (usedBytes.toFloat() / totalBytes.toFloat()) * 100f
                } else 0f
            } catch (e: Exception) {
                Logger.e(tag, "Error getting memory percent", e)
                0f
            }
        }
    }
    
    override suspend fun getCpuPercent(): Float {
        return withContext(Dispatchers.IO) {
            try {
                systemStatsReader.readCpuUsagePercent() ?: 0f
            } catch (e: Exception) {
                Logger.e(tag, "Error getting CPU percent", e)
                0f
            }
        }
    }
}
