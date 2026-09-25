# Permissions Management Guide

## Overview

Alloy uses a centralized permissions management system located in the `core:permissions` module. This provides a unified approach to handling all permission requests across the app, ensuring consistent UX and proper permission lifecycle management.

## Available Permissions

The following permissions are defined in `AppPermission`:

| Permission | Description | Use Case |
|------------|-------------|----------|
| `SystemOverlay` | Display over other apps | StatsPill floating overlay |
| `PostNotifications` | Show notifications | Alert thresholds, clip notifications |
| `ReadClipboard` | Read clipboard | Clip module clipboard management |
| `WriteClipboard` | Write to clipboard | Clip module paste functionality |
| `ReceiveBoot` | Run at startup | Auto-start features (future) |

## Architecture

```
core/permissions/
├── Permission.kt           # AppPermission definitions
├── PermissionState.kt      # UI state classes
├── PermissionsManager.kt   # Core logic
├── PermissionUtils.kt      # Extension functions
├── di/PermissionsModule.kt # Hilt DI module
└── ui/
    ├── PermissionDialog.kt    # Rationale dialog
    └── PermissionStatus.kt    # Status indicators
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
// In your ViewModel
fun requestOverlayPermission() {
    if (permissionsManager.isPermissionGranted(context, AppPermission.SystemOverlay)) {
        // Permission already granted, proceed
        startFeature()
    } else {
        // Show rationale dialog to user
        showPermissionDialog(AppPermission.SystemOverlay)
    }
}

// After user accepts rationale
fun grantPermission() {
    permissionsManager.requestPermission(
        permission = AppPermission.SystemOverlay,
        launcher = overlayPermissionLauncher,
        activity = this,
        callbacks = PermissionCallbacks(
            onGranted = { startFeature() },
            onShowRationale = { /* Already shown */ },
            onOpenSettings = { /* User can grant in settings */ }
        )
    )
}
```

### 4. Check or Request Pattern

```kotlin
permissionsManager.checkOrRequest(
    context = context,
    activity = this,
    permission = AppPermission.ReadClipboard,
    launcher = clipboardPermissionLauncher,
    onGranted = {
        // Proceed with clipboard access
        readClipboard()
    },
    onDenied = {
        // Show error or disable feature
        showError("Clipboard permission required")
    },
    onShowRationale = {
        // Show rationale dialog
        showPermissionDialog(AppPermission.ReadClipboard)
    }
)
```

## UI Components

### PermissionRationaleDialog

Display permission rationale to users before requesting:

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
                permissionsManager.openAppSettings(context)
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
fun PermissionStatusRow(viewModel: MyViewModel) {
    val permissionState by viewModel.permissionState.collectAsState()
    
    PermissionStatusIndicator(
        permissionState = permissionState,
        onReRequestClick = { viewModel.requestPermission() }
    )
}
```

### PermissionStatusBadge

Compact status indicator for toolbars or headers:

```kotlin
@Composable
fun Toolbar(viewModel: MyViewModel) {
    TopAppBar(
        title = { Text("StatsPill") },
        actions = {
            val permissionState by viewModel.permissionState.collectAsState()
            PermissionStatusBadge(
                permissionState = permissionState,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    )
}
```

## Special Permissions

### SYSTEM_ALERT_WINDOW (SystemOverlay)

This permission requires special handling:

1. Cannot be requested via standard `ActivityResultLauncher`
2. Opens system settings screen directly
3. User must manually toggle the permission

The `PermissionsManager` handles this automatically when you use `AppPermission.SystemOverlay`.

## Best Practices

1. **Request lazily**: Only request permissions when the feature is actually needed
2. **Show rationale**: Always explain why the permission is needed before requesting
3. **Handle denial gracefully**: Provide fallback behavior when permissions are denied
4. **Use centralized system**: Always use `PermissionsManager` instead of direct permission checks
5. **Follow Material You**: Permission dialogs automatically use your app's theme

## Adding New Permissions

To add a new permission:

1. Add to `AppPermission` sealed class in `Permission.kt`:

```kotlin
object MyNewPermission : AppPermission(
    manifestName = "android.permission.MY_PERMISSION",
    title = "My Permission Title",
    description = "What this permission does",
    rationale = "Why the user should grant this"
)
```

2. Add to `AndroidManifest.xml` if it's a normal permission:

```xml
<uses-permission android:name="android.permission.MY_PERMISSION" />
```

3. Update `isSpecialPermission` extension if needed:

```kotlin
val AppPermission.isSpecialPermission: Boolean
    get() = this is AppPermission.SystemOverlay || this is AppPermission.MyNewPermission
```

## Migration Guide

### From Ad-hoc Permission Checks

**Before:**
```kotlin
// In StatsViewModel
if (!Settings.canDrawOverlays(context)) {
    Toast.makeText(context, "Permission required", Toast.LENGTH_SHORT).show()
    return
}
```

**After:**
```kotlin
// In StatsViewModel
if (!permissionsManager.isPermissionGranted(context, AppPermission.SystemOverlay)) {
    sendEffect(StatsUiEffect.ShowToast("Permission required"))
    return
}
```

## Testing

Unit test permission checks:

```kotlin
@Test
fun `permission manager returns granted state when permission is granted`() {
    val manager = PermissionsManager()
    // Mock context with granted permission
    val result = manager.isPermissionGranted(mockContext, AppPermission.PostNotifications)
    assertTrue(result)
}
```

## Related Documentation

- [Theming Guide](../THEMING_GUIDE.md) - Material You theming
- [IMPLEMENTATION_LOG.md](../IMPLEMENTATION_LOG.md) - Implementation history
