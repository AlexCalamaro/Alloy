package com.squidink.alloy.modules.statspill.data.datasource

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.statspill.domain.model.DiskStats
import com.squidink.alloy.modules.statspill.domain.model.StorageVolumeInfo
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
 * Uses Android's [StorageManager] and [StatFs] to observe file system utilization
 * across all connected storage volumes (internal storage, SD card, USB OTG).
 *
 * Polled at 0.1Hz (every 10 seconds) to prevent battery drain.
 */
@Singleton
class DiskDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : StatDataSource<DiskStats> {

    /**
     * Read current disk statistics for internal and all secondary storage volumes.
     */
    override suspend fun read(): DiskStats = withContext(Dispatchers.IO) {
        try {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            val volumes = mutableListOf<StorageVolumeInfo>()

            if (storageManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                for (volume in storageManager.storageVolumes) {
                    val state = volume.state
                    if (state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY) {
                        val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            volume.directory?.absolutePath
                        } else {
                            null
                        } ?: if (volume.isPrimary) {
                            Environment.getExternalStorageDirectory().absolutePath
                        } else null

                        if (path != null) {
                            try {
                                val statFs = StatFs(path)
                                val total = statFs.totalBytes
                                val available = statFs.availableBytes
                                val used = (total - available).coerceAtLeast(0L)
                                val pct = if (total > 0L) {
                                    ((used.toFloat() / total.toFloat()) * 100f).coerceIn(0f, 100f)
                                } else 0f
                                val name = volume.getDescription(context)

                                volumes.add(
                                    StorageVolumeInfo(
                                        name = name,
                                        isPrimary = volume.isPrimary,
                                        isRemovable = volume.isRemovable,
                                        totalBytes = total,
                                        usedBytes = used,
                                        freeBytes = available,
                                        percentUsed = pct
                                    )
                                )
                            } catch (e: Exception) {
                                Logger.w(TAG, "Failed reading StatFs for volume: ${volume.getDescription(context)}", e)
                            }
                        }
                    }
                }
            }

            // Fallback for internal storage if volume enumeration yielded nothing
            if (volumes.isEmpty()) {
                val statFs = getStatFs()
                val totalBytes = statFs.totalBytes
                val availableBytes = statFs.availableBytes
                val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
                val percentUsed = if (totalBytes > 0L) {
                    ((usedBytes.toFloat() / totalBytes.toFloat()) * 100f).coerceIn(0f, 100f)
                } else 0f

                volumes.add(
                    StorageVolumeInfo(
                        name = "Internal Storage",
                        isPrimary = true,
                        isRemovable = false,
                        totalBytes = totalBytes,
                        usedBytes = usedBytes,
                        freeBytes = availableBytes,
                        percentUsed = percentUsed
                    )
                )
            }

            val primaryVolume = volumes.firstOrNull { it.isPrimary } ?: volumes.first()

            DiskStats(
                timestamp = System.currentTimeMillis(),
                totalBytes = primaryVolume.totalBytes,
                usedBytes = primaryVolume.usedBytes,
                freeBytes = primaryVolume.freeBytes,
                percentUsed = primaryVolume.percentUsed,
                volumes = volumes
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Error reading disk stats", e)
            DiskStats(
                timestamp = System.currentTimeMillis(),
                totalBytes = 0L,
                usedBytes = 0L,
                freeBytes = 0L,
                percentUsed = 0f,
                volumes = emptyList()
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
