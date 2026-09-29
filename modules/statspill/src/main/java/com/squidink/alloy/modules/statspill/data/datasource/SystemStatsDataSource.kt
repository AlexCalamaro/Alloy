package com.squidink.alloy.modules.statspill.data.datasource

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.core.proc.SystemStatsReader
import com.squidink.alloy.modules.statspill.domain.model.SystemStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for system statistics (CPU, memory).
 * Uses [SystemStatsReader] to read from Android system APIs.
 *
 * Ensures all percentages are consistently formatted in the 0.0f..100.0f range.
 */
@Singleton
class SystemStatsDataSource @Inject constructor(
    private val systemStatsReader: SystemStatsReader
) : StatDataSource<SystemStats> {

    /**
     * Read current system statistics.
     */
    override suspend fun read(): SystemStats = withContext(Dispatchers.IO) {
        try {
            val memInfo = systemStatsReader.readMemInfo()
            val cpuPercent = systemStatsReader.readCpuUsagePercent() ?: 0f

            val totalBytes = memInfo.totalMemKb * 1024L
            val availableBytes = memInfo.availableMemKb * 1024L
            val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
            val memPercent = if (totalBytes > 0) {
                ((usedBytes.toFloat() / totalBytes.toFloat()) * 100f).coerceIn(0f, 100f)
            } else 0f

            SystemStats(
                timestamp = System.currentTimeMillis(),
                memoryUsedBytes = usedBytes,
                memoryTotalBytes = totalBytes,
                memoryPercent = memPercent,
                cpuPercent = cpuPercent.coerceIn(0f, 100f)
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading system stats", e)
            SystemStats(
                timestamp = System.currentTimeMillis(),
                memoryUsedBytes = 0L,
                memoryTotalBytes = 0L,
                memoryPercent = 0f,
                cpuPercent = 0f
            )
        }
    }

    /**
     * Observe system statistics as a continuous flow at 1Hz.
     */
    override fun observe(): Flow<SystemStats> = flow {
        while (true) {
            emit(read())
            delay(POLL_INTERVAL_MS)
        }
    }

    companion object {
        private const val TAG = "SystemStatsDataSource"
        const val POLL_INTERVAL_MS = 1000L
    }
}
