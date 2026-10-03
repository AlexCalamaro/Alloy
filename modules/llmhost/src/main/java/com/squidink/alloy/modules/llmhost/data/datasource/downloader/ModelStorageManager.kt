package com.squidink.alloy.modules.llmhost.data.datasource.downloader

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.squidink.alloy.core.common.Logger
import com.squidink.alloy.modules.llmhost.domain.model.InstalledModel
import com.squidink.alloy.modules.llmhost.domain.model.StorageUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages model files on disk, storage calculations, and StatFs queries.
 */
@Singleton
class ModelStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val modelsDir: File
        get() = File(context.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }

    /**
     * Inspects the models directory and returns all installed models in the library.
     *
     * @param activePath Optional path to the currently active model.
     */
    suspend fun getInstalledModels(activePath: String? = null): List<InstalledModel> = withContext(Dispatchers.IO) {
        val files = modelsDir.listFiles { file ->
            isSupportedModelFile(file)
        } ?: return@withContext emptyList()

        files.sortedByDescending { it.lastModified() }.map { file ->
            InstalledModel(
                fileName = file.name,
                filePath = file.absolutePath,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                isActive = activePath != null && file.absolutePath == activePath
            )
        }
    }

    /**
     * Inspects the models directory and returns the currently installed active model, if any.
     *
     * @param activePath Preferred path to the active model; defaults to most recently modified if null.
     */
    suspend fun getInstalledModel(activePath: String? = null): InstalledModel? = withContext(Dispatchers.IO) {
        val allModels = getInstalledModels(activePath)
        if (allModels.isEmpty()) return@withContext null

        if (activePath != null) {
            val matching = allModels.firstOrNull { it.filePath == activePath }
            if (matching != null) return@withContext matching.copy(isActive = true)
        }

        allModels.first().copy(isActive = true)
    }

    /**
     * Computes model storage usage and available device disk space.
     */
    suspend fun getStorageUsage(activePath: String? = null): StorageUsage = withContext(Dispatchers.IO) {
        val installed = getInstalledModel(activePath)
        val currentSize = installed?.sizeBytes ?: 0L

        val totalModelsSize = modelsDir.listFiles()?.sumOf { it.length() } ?: 0L

        val availableStorage = try {
            val statFs = StatFs(context.filesDir.absolutePath)
            statFs.availableBytes
        } catch (e: Exception) {
            Logger.w(TAG, "Failed reading StatFs", e)
            0L
        }

        StorageUsage(
            currentModelSizeBytes = currentSize,
            totalModelsSizeBytes = totalModelsSize,
            availableStorageBytes = availableStorage
        )
    }

    /**
     * Deletes a specific model file from storage.
     */
    suspend fun deleteModel(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                val deleted = file.delete()
                if (deleted) {
                    Logger.i(TAG, "Deleted model file: $filePath")
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Failed to delete file: $filePath"))
                }
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Exception deleting model file: $filePath", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes all model files and temporary download fragments.
     */
    suspend fun deleteModelFiles(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            modelsDir.listFiles()?.forEach { file ->
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to delete model files", e)
            Result.failure(e)
        }
    }

    private fun isSupportedModelFile(file: File): Boolean {
        if (!file.isFile) return false
        val ext = file.extension.lowercase(Locale.ROOT)
        return ext == "gguf" || ext == "litertlm" || ext == "bin" || ext == "tflite"
    }

    companion object {
        private const val TAG = "ModelStorageManager"

        /**
         * Formats bytes into a human-readable string (e.g. "1.46 GB", "520 MB").
         */
        fun formatBytes(bytes: Long): String =
            com.squidink.alloy.modules.llmhost.domain.model.formatBytes(bytes)
    }
}
