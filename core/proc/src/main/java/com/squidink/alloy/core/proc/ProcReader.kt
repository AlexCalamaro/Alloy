package com.squidink.alloy.core.proc

import com.squidink.alloy.core.common.Logger
import java.io.File
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
 * Low-overhead reader parsing /proc system telemetry metrics.
 */
@Singleton
open class ProcReader @Inject constructor() {

    private var previousIdleTicks: Long = 0
    private var previousTotalTicks: Long = 0

    open fun readMemInfo(): MemInfo {
        return try {
            val file = File("/proc/meminfo")
            if (!file.exists()) return MemInfo()

            var total = 0L
            var free = 0L
            var available = 0L

            file.useLines { lines ->
                lines.forEach { line ->
                    when {
                        line.startsWith("MemTotal:") -> total = parseKb(line)
                        line.startsWith("MemFree:") -> free = parseKb(line)
                        line.startsWith("MemAvailable:") -> available = parseKb(line)
                    }
                }
            }
            MemInfo(totalMemKb = total, freeMemKb = free, availableMemKb = available)
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading /proc/meminfo", e)
            MemInfo()
        }
    }

    /**
     * Reads /proc/stat and calculates CPU delta percentage since last call.
     */
    open fun readCpuUsagePercent(): Float? {
        return try {
            val file = File("/proc/stat")
            if (!file.exists()) return null

            val cpuLine = file.useLines { lines -> lines.firstOrNull { it.startsWith("cpu ") } } ?: return null
            val parts = cpuLine.trim().split("\\s+".toRegex())
            if (parts.size < 8) return null

            val user = parts[1].toLongOrNull() ?: 0L
            val nice = parts[2].toLongOrNull() ?: 0L
            val system = parts[3].toLongOrNull() ?: 0L
            val idle = parts[4].toLongOrNull() ?: 0L
            val iowait = parts[5].toLongOrNull() ?: 0L
            val irq = parts[6].toLongOrNull() ?: 0L
            val softirq = parts[7].toLongOrNull() ?: 0L

            val currentIdle = idle + iowait
            val currentTotal = user + nice + system + idle + iowait + irq + softirq

            val totalDelta = currentTotal - previousTotalTicks
            val idleDelta = currentIdle - previousIdleTicks

            previousIdleTicks = currentIdle
            previousTotalTicks = currentTotal

            if (totalDelta <= 0) return null

            val cpuUsage = ((totalDelta - idleDelta).toFloat() / totalDelta.toFloat()) * 100.0f
            cpuUsage.coerceIn(0.0f, 100.0f)
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading /proc/stat", e)
            null
        }
    }

    private fun parseKb(line: String): Long {
        return line.substringAfter(":").trim().substringBefore(" ").trim().toLongOrNull() ?: 0L
    }

    companion object {
        private const val TAG = "ProcReader"
    }
}
