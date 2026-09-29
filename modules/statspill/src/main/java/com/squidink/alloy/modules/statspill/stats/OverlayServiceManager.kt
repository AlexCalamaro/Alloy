package com.squidink.alloy.modules.statspill.stats

import android.content.Context
import android.content.Intent
import com.squidink.alloy.core.permissions.AppPermission
import com.squidink.alloy.core.permissions.PermissionsManager
import com.squidink.alloy.modules.statspill.StatsPillOverlayService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the floating overlay service lifecycle.
 * Decoupled from ViewModel lifecycle so overlays persist across app navigation.
 */
@Singleton
class OverlayServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionsManager: PermissionsManager
) {

    /**
     * Check if overlay permission (SYSTEM_ALERT_WINDOW) is granted.
     */
    fun isPermissionGranted(): Boolean {
        return permissionsManager.isPermissionGranted(context, AppPermission.SystemOverlay)
    }

    /**
     * Start the overlay foreground service.
     * Returns true if launched, false if permission denied.
     */
    fun startOverlay(): Boolean {
        if (!isPermissionGranted()) {
            return false
        }
        val intent = Intent(context, StatsPillOverlayService::class.java)
        context.startForegroundService(intent)
        return true
    }

    /**
     * Stop the overlay foreground service.
     */
    fun stopOverlay() {
        val intent = Intent(context, StatsPillOverlayService::class.java)
        context.stopService(intent)
    }

    /**
     * Open system settings for granting overlay permission.
     */
    fun openPermissionSettings() {
        permissionsManager.openPermissionSettings(context, AppPermission.SystemOverlay)
    }
}
