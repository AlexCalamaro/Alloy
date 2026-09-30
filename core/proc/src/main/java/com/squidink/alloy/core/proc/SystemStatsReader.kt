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

    private var previousRxBytes: Long = -1L
    private var previousTxBytes: Long = -1L
    private var lastReadTimeMs: Long = 0L

    // CPU tracking - using process-level stats as approximation
    private var previousProcessCpuTimeMs: Long = 0L
    private var previousUptimeMs: Long = 0L

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
     * Reads CPU usage for the current process using official Android Process APIs.
     * Returns CPU percentage normalized by core count (0.0f..100.0f).
     *
     * Note: Android doesn't provide system-wide CPU usage without root.
     * This returns the current process CPU usage as a reasonable approximation.
     */
    @Synchronized
    open fun readCpuUsagePercent(): Float? {
        return try {
            val uptimeMs = android.os.SystemClock.elapsedRealtime()
            val processCpuTimeMs = getProcessCpuTimeMs()

            val timeDelta = uptimeMs - previousUptimeMs
            if (timeDelta <= 0 || previousUptimeMs == 0L) {
                previousUptimeMs = uptimeMs
                previousProcessCpuTimeMs = processCpuTimeMs
                return null
            }

            val cpuDelta = processCpuTimeMs - previousProcessCpuTimeMs
            previousProcessCpuTimeMs = processCpuTimeMs
            previousUptimeMs = uptimeMs

            // Calculate CPU usage as percentage of elapsed time normalized by core count
            val cores = readCpuCores().coerceAtLeast(1)
            val cpuUsage = ((cpuDelta.toFloat() / (timeDelta.toFloat() * cores)) * 100.0f).coerceIn(0.0f, 100.0f)
            cpuUsage
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading CPU usage", e)
            null
        }
    }

    /**
     * Gets the total CPU time consumed by the current process in milliseconds.
     * Uses official Android OS Process.getElapsedCpuTime() (API 24+).
     */
    private fun getProcessCpuTimeMs(): Long {
        return try {
            android.os.Process.getElapsedCpuTime()
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting process CPU time", e)
            0L
        }
    }

    /**
     * Reads network statistics using TrafficStats and monotonic clock.
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

            val now = android.os.SystemClock.elapsedRealtime()
            val deltaMs = now - lastReadTimeMs
            lastReadTimeMs = now

            // Calculate Bytes/s (bytes per second)
            val rxBytesPerSec = if (deltaMs > 0 && previousRxBytes >= 0) {
                val delta = totalRx - previousRxBytes
                if (delta >= 0) delta * 1000f / deltaMs else 0f
            } else 0f

            val txBytesPerSec = if (deltaMs > 0 && previousTxBytes >= 0) {
                val delta = totalTx - previousTxBytes
                if (delta >= 0) delta * 1000f / deltaMs else 0f
            } else 0f

            previousRxBytes = totalRx
            previousTxBytes = totalTx

            NetStats(
                rxBytes = totalRx,
                txBytes = totalTx,
                rxBytesPerSecond = rxBytesPerSec,
                txBytesPerSecond = txBytesPerSec
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading network stats", e)
            NetStats()
        }
    }

    /**
     * Returns the number of available CPU cores.
     */
    open fun readCpuCores(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    @Volatile
    private var isLoadAvgSupported: Boolean = true

    /**
     * Reads system load averages (1m, 5m, 15m) from /proc/loadavg.
     * Returns an empty list if /proc/loadavg is inaccessible or denied by SELinux.
     * Permanently stops querying if the kernel denies access to prevent logcat AVC spam.
     */
    open fun readSystemLoadAverage(): List<Double> {
        if (!isLoadAvgSupported) return emptyList()
        return try {
            val file = java.io.File("/proc/loadavg")
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                val parts = content.split("\\s+".toRegex())
                if (parts.size >= 3) {
                    listOfNotNull(
                        parts[0].toDoubleOrNull(),
                        parts[1].toDoubleOrNull(),
                        parts[2].toDoubleOrNull()
                    )
                } else emptyList()
            } else {
                isLoadAvgSupported = false
                Logger.w(TAG, "/proc/loadavg is inaccessible or restricted by SELinux; halting loadavg polling.")
                emptyList()
            }
        } catch (e: Exception) {
            isLoadAvgSupported = false
            Logger.w(TAG, "Error accessing /proc/loadavg (${e.message}); halting loadavg polling.")
            emptyList()
        }
    }

    companion object {
        private const val TAG = "SystemStatsReader"
    }
}
