package com.squidink.alloy.core.data.datasource

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.core.proc.SystemStatsReader
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for system statistics (CPU, memory).
 * Uses [SystemStatsReader] to read from Android system APIs.
 */
@Singleton
class SystemStatsDataSource @Inject constructor(
    private val systemStatsReader: SystemStatsReader
) : StatDataSource<SystemStats> {

    private val tag = "SystemStatsDataSource"

    /**
     * Read current system statistics.
     */
    override suspend fun read(): SystemStats = withContext(Dispatchers.IO) {
        try {
            val memInfo = systemStatsReader.readMemInfo()
            val cpuPercent = systemStatsReader.readCpuUsagePercent() ?: 0f

            val totalBytes = memInfo.totalMemKb * 1024
            val availableBytes = memInfo.availableMemKb * 1024
            val usedBytes = totalBytes - availableBytes
            val memPercent = if (totalBytes > 0) {
                (usedBytes.toFloat() / totalBytes.toFloat()) * 100f
            } else 0f

            SystemStats(
                timestamp = System.currentTimeMillis(),
                memoryUsedBytes = usedBytes,
                memoryTotalBytes = totalBytes,
                memoryPercent = memPercent,
                cpuPercent = cpuPercent
            )
        } catch (e: Exception) {
            Logger.e(tag, "Error reading system stats", e)
            SystemStats(
                timestamp = System.currentTimeMillis(),
                memoryUsedBytes = 0,
                memoryTotalBytes = 0,
                memoryPercent = 0f,
                cpuPercent = 0f
            )
        }
    }

    /**
     * Observe system statistics as a continuous flow.
     * Polls at 1Hz for real-time telemetry.
     */
    override fun observe(): Flow<SystemStats> = flow {
        while (true) {
            emit(read())
            kotlinx.coroutines.delay(1000)
        }
    }
}
