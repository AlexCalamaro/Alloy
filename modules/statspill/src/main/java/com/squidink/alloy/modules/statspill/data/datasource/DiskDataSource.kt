package com.squidink.alloy.core.data.datasource

import android.os.Environment
import android.os.StatFs
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for disk/storage statistics.
 * Uses Android's StatFs to read storage information.
 */
@Singleton
class DiskDataSource @Inject constructor(
    private val context: android.content.Context
) : StatDataSource<DiskStats> {

    private val tag = "DiskDataSource"

    /**
     * Read current disk statistics for the primary storage.
     */
    override suspend fun read(): DiskStats = withContext(Dispatchers.IO) {
        try {
            val statFs = getStatFs()
            
            val totalBytes = statFs.totalBytes
            val availableBytes = statFs.availableBytes
            val usedBytes = totalBytes - availableBytes
            val percentUsed = if (totalBytes > 0) {
                (usedBytes.toFloat() / totalBytes.toFloat()) * 100f
            } else 0f

            DiskStats(
                timestamp = System.currentTimeMillis(),
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = availableBytes,
                percentUsed = percentUsed
            )
        } catch (e: Exception) {
            Logger.e(tag, "Error reading disk stats", e)
            DiskStats(
                timestamp = System.currentTimeMillis(),
                totalBytes = 0,
                usedBytes = 0,
                freeBytes = 0,
                percentUsed = 0f
            )
        }
    }

    /**
     * Observe disk statistics as a continuous flow.
     * Polls at 10Hz since disk usage changes less frequently.
     */
    override fun observe(): Flow<DiskStats> = flow {
        while (true) {
            emit(read())
            kotlinx.coroutines.delay(100) // 10Hz - disk stats change less frequently
        }
    }

    /**
     * Get StatFs for the primary external storage.
     */
    private fun getStatFs(): StatFs {
        val externalPath = Environment.getExternalStorageDirectory().absolutePath
        return StatFs(externalPath)
    }
}

/**
 * Extension properties for StatFs to get byte values.
 */
private val StatFs.totalBytes: Long
    get() = blockSizeLong * blockCountLong

private val StatFs.availableBytes: Long
    get() = blockSizeLong * availableBlocksLong

private val StatFs.freeBytes: Long
    get() = blockSizeLong * freeBlocksLong
