package com.squidink.alloy.core.permissions

/**
 * Represents a permission required by the Alloy app.
 * Each permission includes metadata for UI display and rationale.
 */
sealed class AppPermission(
    /** Android manifest permission name */
    val manifestName: String,
    
    /** Short title for UI display */
    val title: String,
    
    /** Description of what the permission enables */
    val description: String,
    
    /** Rationale for why this permission is needed */
    val rationale: String
) {
    /**
     * System overlay permission for floating widgets (StatsPill)
     */
    object SystemOverlay : AppPermission(
        manifestName = android.Manifest.permission.SYSTEM_ALERT_WINDOW,
        title = "Display over other apps",
        description = "Show live system stats as a floating overlay",
        rationale = "Alloy needs permission to display the live stats pill overlay above other apps. This allows you to monitor system vitals while using any app."
    )
    
    /**
     * Post notifications permission (Android 13+)
     */
    object PostNotifications : AppPermission(
        manifestName = android.Manifest.permission.POST_NOTIFICATIONS,
        title = "Show notifications",
        description = "Alert you about system events and news updates",
        rationale = "Alloy can notify you about high CPU/RAM usage, breaking news feeds, and other important events. Notifications help you stay informed without opening the app."
    )
    
    /**
     * Receive boot completed (for auto-start features)
     */
    object ReceiveBoot : AppPermission(
        manifestName = android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
        title = "Run at startup",
        description = "Start automatically when device boots",
        rationale = "Alloy can start automatically when your device boots to restore your previous session and keep features running."
    )
}

/**
 * Extension to check if a permission is a special app permission
 * (requires special handling like SYSTEM_ALERT_WINDOW)
 */
val AppPermission.isSpecialPermission: Boolean
    get() = this is AppPermission.SystemOverlay
