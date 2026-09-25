# Permissions Module

Centralized permissions management system for the Alloy app.

## Overview

The Permissions module provides a unified approach to handling all permission requests across the app, ensuring consistent UX and proper permission lifecycle management.

## Capabilities

### Core Features

- **Unified Permission API**
  - Single interface for all permission types
  - Handles both runtime and special permissions
  - Permission state management

- **Permission Types Supported**
  - Runtime permissions (CAMERA, LOCATION, etc.)
  - Special permissions (SYSTEM_ALERT_WINDOW, notifications)
  - Custom app-defined permissions

- **Smart Settings Navigation**
  - Opens specific settings pages based on permission type
  - Direct link to overlay permission settings
  - Direct link to notification settings (Android 8.0+)
  - Fallback to general app settings

### Permission Definitions

Defined in `AppPermission` sealed class:

| Permission | Type | Use Case |
|------------|------|----------|
| `SystemOverlay` | Special | StatsPill floating overlay |
| `PostNotifications` | Runtime | Alert thresholds, notifications |
| `ReadClipboard` | Runtime | Clip module clipboard management |
| `WriteClipboard` | Runtime | Clip module paste functionality |
| `ReceiveBoot` | Runtime | Auto-start features |

## Architecture

```
core/permissions/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/squidink/alloy/core/permissions/
│   │       ├── Permission.kt              # AppPermission definitions
│   │       ├── PermissionState.kt         # UI state classes
│   │       ├── PermissionsManager.kt      # Core logic
│   │       ├── PermissionUtils.kt         # Extension functions
│   │       └── di/
│   │           └── PermissionsModule.kt   # Hilt DI module
│   │       └── ui/
│   │           ├── PermissionDialog.kt    # Rationale dialogs
│   │           └── PermissionStatus.kt    # Status indicators
│   └── test/
│       └── java/com/squidink/alloy/core/permissions/
│           └── PermissionsManagerTest.kt
└── README.md
```

## Usage

### 1. Inject PermissionsManager

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val permissionsManager: PermissionsManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    // ...
}
```

### 2. Check Permission Status

```kotlin
// Simple check
val isGranted = permissionsManager.isPermissionGranted(
    context, 
    AppPermission.SystemOverlay
)

// Get full UI state
val permissionState = permissionsManager.getPermissionState(
    context,
    activity,
    AppPermission.PostNotifications
)
```

### 3. Request Permission with Rationale

```kotlin
fun requestOverlayPermission() {
    if (permissionsManager.isPermissionGranted(context, AppPermission.SystemOverlay)) {
        // Permission already granted, proceed
        startFeature()
    } else {
        // Show rationale dialog to user
        showPermissionDialog(AppPermission.SystemOverlay)
    }
}
```

### 4. Open Permission-Specific Settings

```kotlin
fun openPermissionSettings() {
    permissionsManager.openPermissionSettings(
        context = context,
        permission = AppPermission.SystemOverlay
    )
    // Directly opens: Settings > Apps > Special app access > Draw over other apps
}
```

This method intelligently routes to the appropriate settings page:
- **SystemOverlay**: Opens overlay permission settings directly
- **PostNotifications**: Opens notification settings (Android 8.0+)
- **Other permissions**: Falls back to general app settings

## UI Components

### PermissionRationaleDialog

Material You themed permission rationale dialog:

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    if (showPermissionDialog) {
        PermissionRationaleDialog(
            permission = AppPermission.SystemOverlay,
            onGrantClick = { 
                showPermissionDialog = false
                viewModel.grantPermission()
            },
            onSettingsClick = { 
                showPermissionDialog = false
                permissionsManager.openPermissionSettings(context, AppPermission.SystemOverlay)
            },
            onDismiss = { showPermissionDialog = false }
        )
    }
    
    // ... rest of your UI
}
```

### PermissionStatusIndicator

Show permission status in your UI:

```kotlin
@Composable
fun PermissionBadge(viewModel: MyViewModel) {
    val permissionState by viewModel.permissionState.collectAsState()
    
    PermissionStatusBadge(
        state = permissionState,
        onClick = { viewModel.requestPermission() }
    )
}
```

## Dependencies

```kotlin
implementation(project(":core:common"))

// AndroidX
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.activity.ktx)

// Compose
implementation(platform(libs.compose.bom))
implementation(libs.compose.ui)
implementation(libs.compose.material3)
```

## Testing

```kotlin
class TestPermissionsManager : PermissionsManager() {
    override fun isPermissionGranted(context: Context, permission: AppPermission): Boolean {
        return true // Or false for testing denied state
    }
    
    override fun getPermissionState(context: Context, activity: Activity, permission: AppPermission): PermissionUiState {
        return PermissionUiState(
            permission = permission,
            state = PermissionState.Granted
        )
    }
}
```

## Best Practices

1. **Check before acting**: Always check permission status before performing actions that require permissions

2. **Show rationale**: Explain to users why you need the permission before requesting it

3. **Use specific settings**: Call `openPermissionSettings()` instead of `openAppSettings()` for better UX

4. **Handle denial gracefully**: Provide fallback behavior when permissions are denied

5. **Test all states**: Test with granted, denied, and permanently denied permission states

## Future Enhancements

- Support for permission groups
- Permission usage analytics
- Automated permission request flows
- Custom permission templates
