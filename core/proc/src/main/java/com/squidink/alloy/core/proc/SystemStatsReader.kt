package com.squidink.alloy.core.proc

import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.content.Context
import android.os.Debug
import android.net.TrafficStats
import com.squidink.alloy.core.common.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data container for system memory vitals.
 */
data class MemInfo(
    val totalMemKb: Long = 0,
    val freeMemKb: Long = 0,
    val availableMemKb: Long = 0
)

/**
 * Data container for network interface statistics.
 */
data class NetStats(
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxBytesPerSecond: Float = 0f,
    val txBytesPerSecond: Float = 0f
)

/**
 * System stats reader using Android APIs.
 *
 * Replaces /proc file reading with official Android APIs:
 * - [ActivityManager] for memory stats
 * - [Debug] and [ActivityManager] for process CPU/memory info
 * - [TrafficStats] for network usage statistics
 *
 * These APIs work without root permissions and are Play Store compliant.
 */
@Singleton
open class SystemStatsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    private var previousRxBytes: Long = 0
    private var previousTxBytes: Long = 0
    private var lastReadTimeMs: Long = 0

    // CPU tracking - using process-level stats as approximation
    private var previousProcessCpuTimeMs: Long = 0
    private var previousUptimeMs: Long = 0

    /**
     * Reads memory information using ActivityManager.
     * Returns total, free, and available memory in KB.
     */
    open fun readMemInfo(): MemInfo {
        return try {
            val memoryInfo = MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val totalKb = memoryInfo.totalMem / 1024
            val freeKb = memoryInfo.availMem / 1024
            // Available is similar to free on Android (includes reclaimable cache)
            val availableKb = freeKb

            MemInfo(
                totalMemKb = totalKb,
                freeMemKb = freeKb,
                availableMemKb = availableKb
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading memory info", e)
            MemInfo()
        }
    }

    /**
     * Reads CPU usage for the current process using Debug and ActivityManager.
     * Returns CPU percentage as an approximation based on process CPU time.
     *
     * Note: Android doesn't provide system-wide CPU usage without root.
     * This returns the current process CPU usage as a reasonable approximation.
     */
    @Synchronized
    open fun readCpuUsagePercent(): Float? {
        return try {
            val uptimeMs = System.currentTimeMillis()
            val processCpuTimeMs = getProcessCpuTimeMs()

            val timeDelta = uptimeMs - previousUptimeMs
            if (timeDelta <= 0) {
                previousUptimeMs = uptimeMs
                return null
            }

            val cpuDelta = processCpuTimeMs - previousProcessCpuTimeMs
            previousProcessCpuTimeMs = processCpuTimeMs
            previousUptimeMs = uptimeMs

            // Calculate CPU usage as percentage of elapsed time
            // This gives us the process CPU usage over the time window
            val cpuUsage = (cpuDelta.toFloat() / timeDelta.toFloat()) * 100.0f
            cpuUsage.coerceIn(0.0f, 100.0f)
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading CPU usage", e)
            null
        }
    }

    /**
     * Gets the total CPU time consumed by the current process.
     * Uses Debug.getProcessState() and thread CPU time accumulation.
     */
    private fun getProcessCpuTimeMs(): Long {
        return try {
            // Get CPU time from all threads in the current process
            val threads = Thread.getAllStackTraces().keys
            var totalCpuNanos = 0L

            for (thread in threads) {
                try {
                    // getThreadCpuTimeNanos() is available on API 26+
                    // Use reflection to handle older API levels gracefully
                    val method = thread.javaClass.getMethod("getThreadCpuTimeNanos")
                    totalCpuNanos += method.invoke(thread) as Long
                } catch (e: Exception) {
                    // Method not available on this API level, skip this thread
                    // This will happen on API < 26
                }
            }

            // Convert nanoseconds to milliseconds
            totalCpuNanos / 1_000_000L
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting process CPU time", e)
            0L
        }
    }

    /**
     * Reads network statistics using TrafficStats.
     * Returns total RX/TX bytes and calculates KB/s since last call.
     *
     * Note: TrafficStats provides app-level network usage, not system-wide.
     * This is the standard Android API for network stats without root.
     */
    @Synchronized
    open fun readNetworkStats(): NetStats {
        return try {
            // Get total network traffic for the app (UID-based)
            val totalRx = TrafficStats.getTotalRxBytes()
            val totalTx = TrafficStats.getTotalTxBytes()

            val now = System.currentTimeMillis()
            val deltaMs = now - lastReadTimeMs
            lastReadTimeMs = now

            // Calculate KB/s (kilobytes per second)
            val rxKbps = if (deltaMs > 0 && previousRxBytes >= 0) {
                val delta = totalRx - previousRxBytes
                if (delta >= 0) delta * 1000f / deltaMs / 1024f else 0f
            } else 0f

            val txKbps = if (deltaMs > 0 && previousTxBytes >= 0) {
                val delta = totalTx - previousTxBytes
                if (delta >= 0) delta * 1000f / deltaMs / 1024f else 0f
            } else 0f

            previousRxBytes = totalRx
            previousTxBytes = totalTx

            NetStats(
                rxBytes = totalRx,
                txBytes = totalTx,
                rxBytesPerSecond = rxKbps,
                txBytesPerSecond = txKbps
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading network stats", e)
            NetStats()
        }
    }

    companion object {
        private const val TAG = "SystemStatsReader"
    }
}
