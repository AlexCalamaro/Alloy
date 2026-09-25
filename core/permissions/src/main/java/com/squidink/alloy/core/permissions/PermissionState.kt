package com.squidink.alloy.core.permissions

/**
 * UI state for a permission request flow.
 */
sealed class PermissionState {
    /** Permission is granted and ready to use */
    object Granted : PermissionState()
    
    /** Permission is denied, can request again */
    object Denied : PermissionState()
    
    /** Permission is permanently denied, show settings */
    object PermanentlyDenied : PermissionState()
    
    /** Permission is being requested */
    object Pending : PermissionState()
}

/**
 * Extended state including permission details for UI display.
 */
data class PermissionUiState(
    val permission: AppPermission,
    val state: PermissionState,
    val canShowRationale: Boolean = true,
    val requiresSpecialHandling: Boolean = permission.isSpecialPermission
)

/**
 * Result of a permission request.
 */
data class PermissionResult(
    val permission: AppPermission,
    val isGranted: Boolean,
    val canShowRationale: Boolean,
    val needsSettingsRedirect: Boolean
)

/**
 * Callbacks for permission flows.
 */
data class PermissionCallbacks(
    val onGranted: () -> Unit = {},
    val onDenied: () -> Unit = {},
    val onShowRationale: () -> Unit = {},
    val onOpenSettings: () -> Unit = {}
)
