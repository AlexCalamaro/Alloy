package com.squidink.alloy.core.permissions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Utility functions for permission handling.
 */

/**
 * Check if a standard runtime permission is granted.
 */
fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Check if multiple permissions are granted.
 */
fun Context.hasPermissions(permissions: Array<String>): Boolean {
    return permissions.all { hasPermission(it) }
}

/**
 * Get a list of permissions that are not yet granted.
 */
fun Context.getDeniedPermissions(permissions: Array<String>): List<String> {
    return permissions.filter { !hasPermission(it) }
}

/**
 * Extension to check if an AppPermission is granted.
 */
fun Context.isPermissionGranted(permission: AppPermission): Boolean {
    return when (permission) {
        is AppPermission.SystemOverlay -> {
            android.provider.Settings.canDrawOverlays(this)
        }
        else -> {
            hasPermission(permission.manifestName)
        }
    }
}

/**
 * Check if all permissions in a list are granted.
 */
fun Context.areAllPermissionsGranted(permissions: List<AppPermission>): Boolean {
    return permissions.all { isPermissionGranted(it) }
}

/**
 * Get the list of denied permissions from a list.
 */
fun Context.getDeniedPermissions(permissions: List<AppPermission>): List<AppPermission> {
    return permissions.filter { !isPermissionGranted(it) }
}

/**
 * Check if we should show rationale for a permission request.
 */
fun android.app.Activity.shouldShowPermissionRationale(permission: AppPermission): Boolean {
    return when (permission) {
        is AppPermission.SystemOverlay -> {
            // SYSTEM_ALERT_WINDOW doesn't have rationale, always show our custom dialog
            true
        }
        else -> {
            shouldShowRequestPermissionRationale(permission.manifestName)
        }
    }
}
