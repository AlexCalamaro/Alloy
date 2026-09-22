package com.squidink.alloy.tooling.probe

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.squidink.alloy.core.design.AlloyTheme

/**
 * Test bench activity for Phase 0 Open Questions (OQ) feasibility validation.
 */
class ProbeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlloyTheme {
                Scaffold { paddingValues ->
                    ProbeScreen(
                        modifier = Modifier.padding(paddingValues),
                        onTestStorage = { checkStorageVolumes() },
                        onTestLaunchBounds = { launchWithBounds() },
                        onTestOverlayPermission = { checkOverlayPermission() }
                    )
                }
            }
        }
    }

    private fun checkStorageVolumes(): String {
        val storageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val volumes = storageManager.storageVolumes
        return "Volumes detected: ${volumes.size}\n" +
                volumes.joinToString("\n") { "Volume: ${it.getDescription(this)} (Removable: ${it.isRemovable}, State: ${it.state})" }
    }

    private fun launchWithBounds() {
        val intent = Intent(this, ProbeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        }
        val options = ActivityOptions.makeBasic().apply {
            launchBounds = Rect(100, 100, 800, 600)
        }
        startActivity(intent, options.toBundle())
    }

    private fun checkOverlayPermission(): String {
        val canDraw = Settings.canDrawOverlays(this)
        return "SYSTEM_ALERT_WINDOW permission granted: $canDraw"
    }
}

@Composable
fun ProbeScreen(
    modifier: Modifier = Modifier,
    onTestStorage: () -> String,
    onTestLaunchBounds: () -> Unit,
    onTestOverlayPermission: () -> String
) {
    var storageResult by remember { mutableStateOf("Storage test not run") }
    var overlayResult by remember { mutableStateOf("Overlay test not run") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Alloy Feasibility Probe (Phase 0 OQ Sprint)", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // OQ-1 Storage Volume Test
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("OQ-1: USB Mass Storage Volumes", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { storageResult = onTestStorage() }) {
                    Text("Run Storage Volume Scan")
                }
                Text(storageResult, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // OQ-4 Launch Bounds Test
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("OQ-4: Desktop Launch Bounds (WM)", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onTestLaunchBounds) {
                    Text("Launch Adjacent Window (100,100,800,600)")
                }
            }
        }

        // OQ-5 System Alert Overlay Test
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("OQ-5: SYSTEM_ALERT_WINDOW Overlay", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { overlayResult = onTestOverlayPermission() }) {
                    Text("Check Overlay Permission")
                }
                Text(overlayResult, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
