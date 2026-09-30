package com.squidink.alloy.modules.scratch.util

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility for exporting document contents to a given Uri via Storage Access Framework.
 */
object DocumentExporter {

    /**
     * Writes [content] to the specified [uri] using [contentResolver].
     *
     * @param contentResolver The Android [ContentResolver] instance.
     * @param uri The destination [Uri] returned by the system document picker.
     * @param content The text content to write.
     * @return [Result.success] if written successfully, or [Result.failure] with the underlying error.
     */
    suspend fun exportToUri(
        contentResolver: ContentResolver,
        uri: Uri,
        content: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
                output.flush()
            } ?: throw IllegalStateException("Unable to open output stream for URI: $uri")
        }
    }
}
