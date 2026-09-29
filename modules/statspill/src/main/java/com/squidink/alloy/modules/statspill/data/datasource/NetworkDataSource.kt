package com.squidink.alloy.modules.statspill.data.datasource

import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.core.proc.NetStats
import com.squidink.alloy.core.proc.SystemStatsReader
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for network statistics.
 * Uses [SystemStatsReader] to read network I/O and calculate transfer rates.
 */
@Singleton
class NetworkDataSource @Inject constructor(
    private val systemStatsReader: SystemStatsReader
) : StatDataSource<NetworkStats> {

    @Volatile
    private var lastStats: NetStats? = null

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
            Logger.e(TAG, "Error reading network stats", e)
            val fallback = lastStats
            NetworkStats(
                timestamp = System.currentTimeMillis(),
                rxBytes = fallback?.rxBytes ?: 0L,
                txBytes = fallback?.txBytes ?: 0L,
                rxBytesPerSecond = 0f,
                txBytesPerSecond = 0f
            )
        }
    }

    /**
     * Observe network statistics as a continuous flow at 1Hz.
     */
    override fun observe(): Flow<NetworkStats> = flow {
        while (true) {
            emit(read())
            delay(POLL_INTERVAL_MS)
        }
    }

    companion object {
        private const val TAG = "NetworkDataSource"
        const val POLL_INTERVAL_MS = 1000L
    }
}
