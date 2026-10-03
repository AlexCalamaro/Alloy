package com.squidink.alloy.modules.llmhost.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.desktopHover
import com.squidink.alloy.modules.llmhost.domain.model.DownloadProgress
import com.squidink.alloy.modules.llmhost.domain.model.DownloadStatus
import com.squidink.alloy.modules.llmhost.domain.model.formatBytes
import java.util.Locale

@Composable
fun ModelManagerCard(
    urlInput: String,
    tokenInput: String,
    downloadProgress: DownloadProgress,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onStartDownload: (String, String?) -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showToken by remember { mutableStateOf(false) }
    val isDownloading = downloadProgress.status == DownloadStatus.DOWNLOADING ||
        downloadProgress.status == DownloadStatus.CONNECTING

    Card(
        modifier = modifier
            .fillMaxWidth()
            .desktopHover(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Download Hugging Face Model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter a direct download URL to a GGUF (.gguf) or LiteRT (.litertlm) model on Hugging Face.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hugging Face URL input
            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                label = { Text("Model Download URL (.gguf or .litertlm)") },
                placeholder = { Text("https://huggingface.co/.../model.gguf") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                singleLine = true,
                enabled = !isDownloading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Hugging Face API Token (for gated models like Gemma)
            OutlinedTextField(
                value = tokenInput,
                onValueChange = onTokenChange,
                label = { Text("Hugging Face Access Token (Optional)") },
                placeholder = { Text("hf_...") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(
                            if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showToken) "Hide token" else "Show token"
                        )
                    }
                },
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isDownloading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isDownloading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val statusText = if (downloadProgress.status == DownloadStatus.CONNECTING) {
                            "Connecting to server..."
                        } else {
                            "Downloading model..."
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", downloadProgress.progressPercent),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (downloadProgress.totalBytes > 0) {
                        LinearProgressIndicator(
                            progress = { downloadProgress.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val downloadedStr = formatBytes(downloadProgress.bytesRead)
                        val totalStr = formatBytes(downloadProgress.totalBytes)
                        val speedStr = formatBytes(downloadProgress.speedBytesPerSec)
                        Text(
                            text = "$downloadedStr / $totalStr ($speedStr/s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onCancelDownload) {
                            Text("Cancel")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val token = tokenInput.trim().ifBlank { null }
                            onStartDownload(urlInput.trim(), token)
                        },
                        enabled = urlInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Model")
                    }
                }
            }
        }
    }
}
