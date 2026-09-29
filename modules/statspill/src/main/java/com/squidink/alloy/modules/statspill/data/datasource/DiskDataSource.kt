package com.squidink.alloy.modules.statspill.data.datasource

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for storage / disk statistics.
 * Uses Android's [StatFs] to observe file system utilization.
 *
 * Polled at 0.1Hz (every 10 seconds) to prevent battery drain.
 */
@Singleton
class DiskDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : StatDataSource<DiskStats> {

    /**
     * Read current disk statistics for internal and primary storage.
     */
    override suspend fun read(): DiskStats = withContext(Dispatchers.IO) {
        try {
            val statFs = getStatFs()

            val totalBytes = statFs.totalBytes
            val availableBytes = statFs.availableBytes
            val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
            val percentUsed = if (totalBytes > 0L) {
                ((usedBytes.toFloat() / totalBytes.toFloat()) * 100f).coerceIn(0f, 100f)
            } else 0f

            DiskStats(
                timestamp = System.currentTimeMillis(),
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = availableBytes,
                percentUsed = percentUsed
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading disk stats", e)
            DiskStats(
                timestamp = System.currentTimeMillis(),
                totalBytes = 0L,
                usedBytes = 0L,
                freeBytes = 0L,
                percentUsed = 0f
            )
        }
    }

    /**
     * Observe disk statistics as a continuous flow.
     * Storage changes slowly, so 0.1Hz (every 10s) provides sufficient fidelity
     * with minimal CPU and I/O footprint.
     */
    override fun observe(): Flow<DiskStats> = flow {
        while (true) {
            emit(read())
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun getStatFs(): StatFs {
        return try {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null && Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                StatFs(externalDir.absolutePath)
            } else {
                StatFs(context.filesDir.absolutePath)
            }
        } catch (e: Exception) {
            StatFs(Environment.getDataDirectory().absolutePath)
        }
    }

    companion object {
        private const val TAG = "DiskDataSource"
        const val POLL_INTERVAL_MS = 10_000L // 0.1Hz (10 seconds)
    }
}
