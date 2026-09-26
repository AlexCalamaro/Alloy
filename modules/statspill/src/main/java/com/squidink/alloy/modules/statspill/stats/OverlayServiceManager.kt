package com.squidink.alloy.modules.statspill.stats

import android.content.Context
import android.content.Intent
import com.squidink.alloy.core.permissions.AppPermission
import com.squidink.alloy.core.permissions.PermissionsManager
import com.squidink.alloy.modules.statspill.StatsUiEffect
import com.squidink.alloy.modules.statspill.StatsUiState
import com.squidink.alloy.modules.statspill.StatsPillOverlayService

/**
 * Manages the overlay service lifecycle.
 *
 * Handles:
 * - Starting/stopping the foreground service
 * - Checking overlay permissions
 * - Emitting permission-related effects
 */
class OverlayServiceManager(
    private val context: Context,
    private val permissionsManager: PermissionsManager,
    private val effectEmitter: (StatsUiEffect) -> Unit,
    private val stateUpdater: ((StatsUiState) -> StatsUiState) -> Unit
) {
    private var currentServiceIntent: Intent? = null

    /**
     * Check if overlay permission is granted.
     */
    fun checkPermission(): Boolean {
        val isGranted = permissionsManager.isPermissionGranted(
            context,
            AppPermission.SystemOverlay
        )
        stateUpdater { currentState ->
            currentState.copy(isLiveOverlayPermissionGranted = isGranted)
        }
        return isGranted
    }

    /**
     * Start the overlay service.
     *
     * Returns true if started successfully, false if permission denied.
     */
    fun startOverlay(): Boolean {
        if (!permissionsManager.isPermissionGranted(context, AppPermission.SystemOverlay)) {
            effectEmitter(StatsUiEffect.OpenOverlayPermissionSettings)
            stateUpdater { currentState ->
                currentState.copy(isLiveOverlayActive = false)
            }
            return false
        }

        val intent = Intent(context, StatsPillOverlayService::class.java)
        context.startForegroundService(intent)
        currentServiceIntent = intent
        stateUpdater { currentState ->
            currentState.copy(isLiveOverlayActive = true, overlayServiceIntent = intent)
        }
        return true
    }

    /**
     * Stop the overlay service.
     */
    fun stopOverlay() {
        val intent = currentServiceIntent ?: Intent(context, StatsPillOverlayService::class.java)
        context.stopService(intent)
        currentServiceIntent = null
        stateUpdater { currentState ->
            currentState.copy(isLiveOverlayActive = false, overlayServiceIntent = null)
        }
    }

    /**
     * Open permission settings screen.
     */
    fun openPermissionSettings() {
        permissionsManager.openPermissionSettings(context, AppPermission.SystemOverlay)
    }
}
