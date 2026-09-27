package com.squidink.alloy.core.data.datasource

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.core.proc.SystemStatsReader
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for network statistics.
 * Uses [SystemStatsReader] to read network I/O statistics.
 */
@Singleton
class NetworkDataSource @Inject constructor(
    private val systemStatsReader: SystemStatsReader
) : StatDataSource<NetworkStats> {

    private val tag = "NetworkDataSource"
    private var lastStats: com.squidink.alloy.core.proc.NetStats? = null

    /**
     * Read current network statistics.
     */
    override suspend fun read(): NetworkStats = withContext(Dispatchers.IO) {
        try {
            val stats = systemStatsReader.readNetworkStats()
            lastStats = stats

            NetworkStats(
                timestamp = System.currentTimeMillis(),
                rxBytes = stats.rxBytes,
                txBytes = stats.txBytes,
                rxBytesPerSecond = stats.rxBytesPerSecond,
                txBytesPerSecond = stats.txBytesPerSecond
            )
        } catch (e: Exception) {
            Logger.e(tag, "Error reading network stats", e)
            NetworkStats(
                timestamp = System.currentTimeMillis(),
                rxBytes = lastStats?.rxBytes ?: 0,
                txBytes = lastStats?.txBytes ?: 0,
                rxBytesPerSecond = 0f,
                txBytesPerSecond = 0f
            )
        }
    }

    /**
     * Observe network statistics as a continuous flow.
     * Polls at 1Hz for real-time telemetry.
     */
    override fun observe(): Flow<NetworkStats> = flow {
        while (true) {
            emit(read())
            kotlinx.coroutines.delay(1000)
        }
    }
}
