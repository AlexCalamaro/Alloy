package com.squidink.alloy.core.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Central manager for handling app permissions.
 * Provides unified permission checking, requesting, and state management.
 */
class PermissionsManager {
    
    /**
     * Check if a permission is granted.
     * For SYSTEM_ALERT_WINDOW, uses special check.
     */
    fun isPermissionGranted(context: Context, permission: AppPermission): Boolean {
        return when (permission) {
            is AppPermission.SystemOverlay -> {
                Settings.canDrawOverlays(context)
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    permission.manifestName
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    /**
     * Check if we should show rationale for the permission.
     */
    fun shouldShowRationale(activity: Activity, permission: AppPermission): Boolean {
        return when (permission) {
            is AppPermission.SystemOverlay -> {
                // SYSTEM_ALERT_WINDOW doesn't have shouldShowRequestPermissionRationale
                true
            }
            else -> {
                activity.shouldShowRequestPermissionRationale(permission.manifestName)
            }
        }
    }
    
    /**
     * Get the current permission state.
     */
    fun getPermissionState(context: Context, activity: Activity, permission: AppPermission): PermissionUiState {
        val isGranted = isPermissionGranted(context, permission)
        val canShowRationale = shouldShowRationale(activity, permission)
        
        val state = when {
            isGranted -> PermissionState.Granted
            canShowRationale -> PermissionState.Denied
            else -> PermissionState.PermanentlyDenied
        }
        
        return PermissionUiState(
            permission = permission,
            state = state,
            canShowRationale = canShowRationale,
            requiresSpecialHandling = permission.isSpecialPermission
        )
    }
    
    /**
     * Request a permission using an ActivityResultLauncher.
     * For SYSTEM_ALERT_WINDOW, launches settings intent.
     */
    fun requestPermission(
        permission: AppPermission,
        launcher: ActivityResultLauncher<String>,
        callbacks: PermissionCallbacks,
        activity: Activity
    ) {
        when (permission) {
            is AppPermission.SystemOverlay -> {
                // Special handling for SYSTEM_ALERT_WINDOW
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$activity")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
                callbacks.onShowRationale()
            }
            else -> {
                launcher.launch(permission.manifestName)
            }
        }
    }
    
    /**
     * Check or request permission with callbacks.
     * This is the main entry point for feature modules.
     */
    fun checkOrRequest(
        context: Context,
        activity: Activity,
        permission: AppPermission,
        launcher: ActivityResultLauncher<String>,
        onGranted: () -> Unit,
        onDenied: () -> Unit = {},
        onShowRationale: () -> Unit = {}
    ) {
        if (isPermissionGranted(context, permission)) {
            onGranted()
        } else {
            val canShowRationale = shouldShowRationale(activity, permission)
            if (canShowRationale) {
                onShowRationale()
            } else {
                onDenied()
            }
        }
    }
    
    /**
     * Open app settings for permission management.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Check multiple permissions at once.
     * Returns list of granted and denied permissions.
     */
    fun checkPermissions(context: Context, permissions: List<AppPermission>): PermissionCheckResult {
        val granted = mutableListOf<AppPermission>()
        val denied = mutableListOf<AppPermission>()
        
        permissions.forEach { permission ->
            if (isPermissionGranted(context, permission)) {
                granted.add(permission)
            } else {
                denied.add(permission)
            }
        }
        
        return PermissionCheckResult(granted, denied)
    }
}

/**
 * Result of checking multiple permissions.
 */
data class PermissionCheckResult(
    val granted: List<AppPermission>,
    val denied: List<AppPermission>,
    val allGranted: Boolean = denied.isEmpty()
)
