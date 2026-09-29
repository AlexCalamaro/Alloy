package com.squidink.alloy.modules.statspill.data.datasource

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.core.proc.NetStats
import com.squidink.alloy.core.proc.SystemStatsReader
import com.squidink.alloy.modules.statspill.domain.model.NetworkStats
import com.squidink.alloy.modules.statspill.domain.model.NetworkTransportType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for network statistics.
 * Uses [SystemStatsReader] for I/O transfer rates and [ConnectivityManager]
 * for transport medium, hardware link speeds, and metered network state.
 */
@Singleton
class NetworkDataSource @Inject constructor(
    private val systemStatsReader: SystemStatsReader,
    @ApplicationContext private val context: Context
) : StatDataSource<NetworkStats> {

    @Volatile
    private var lastStats: NetStats? = null

    /**
     * Read current network statistics.
     */
    override suspend fun read(): NetworkStats = withContext(Dispatchers.IO) {
        val (transport, linkSpeed, isMetered) = getNetworkCapabilitiesInfo()
        try {
            val stats = systemStatsReader.readNetworkStats()
            lastStats = stats

            NetworkStats(
                timestamp = System.currentTimeMillis(),
                rxBytes = stats.rxBytes,
                txBytes = stats.txBytes,
                rxBytesPerSecond = stats.rxBytesPerSecond,
                txBytesPerSecond = stats.txBytesPerSecond,
                transportType = transport,
                linkDownstreamBandwidthKbps = linkSpeed,
                isMetered = isMetered
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading network stats", e)
            val fallback = lastStats
            NetworkStats(
                timestamp = System.currentTimeMillis(),
                rxBytes = fallback?.rxBytes ?: 0L,
                txBytes = fallback?.txBytes ?: 0L,
                rxBytesPerSecond = 0f,
                txBytesPerSecond = 0f,
                transportType = transport,
                linkDownstreamBandwidthKbps = linkSpeed,
                isMetered = isMetered
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

    private fun getNetworkCapabilitiesInfo(): Triple<NetworkTransportType, Int?, Boolean?> {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return Triple(NetworkTransportType.UNKNOWN, null, null)

            val activeNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.activeNetwork
            } else {
                null
            }

            if (activeNetwork == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Triple(NetworkTransportType.NONE, null, null)
            }

            val capabilities = cm.getNetworkCapabilities(activeNetwork)
                ?: return Triple(NetworkTransportType.NONE, null, null)

            val transport = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransportType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransportType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransportType.ETHERNET
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkTransportType.VPN
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkTransportType.BLUETOOTH
                else -> NetworkTransportType.UNKNOWN
            }

            val linkSpeed = capabilities.linkDownstreamBandwidthKbps.takeIf { it > 0 }
            val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

            Triple(transport, linkSpeed, isMetered)
        } catch (e: Exception) {
            Logger.w(TAG, "Failed reading network capabilities", e)
            Triple(NetworkTransportType.UNKNOWN, null, null)
        }
    }

    companion object {
        private const val TAG = "NetworkDataSource"
        const val POLL_INTERVAL_MS = 1000L
    }
}
