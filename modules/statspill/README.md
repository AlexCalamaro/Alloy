# StatsPill Module

System telemetry dashboard with live overlay pill display.

## Overview

The StatsPill module provides real-time system telemetry monitoring including CPU usage, memory utilization, and battery status. It features both a detailed dashboard view and a compact floating overlay ("pill") that displays live vitals over any desktop window.

## Capabilities

### Core Features

- **System Telemetry**
  - CPU usage percentage monitoring
  - Memory (RAM) usage tracking
  - Total, used, and available memory display

- **Battery Monitoring**
  - Battery level percentage
  - Charging status
  - Battery health (good, overheat, dead, etc.)
  - Temperature readings
  - Voltage information

- **Live Overlay Pill**
  - `SYSTEM_ALERT_WINDOW` floating overlay
  - Compact CPU/RAM/network display
  - Always-on-top visibility
  - Non-interactive pill design
  - Permission check before activation
  - Permission dialog with rationale
  - Foreground service for reliability

- **Dashboard UI**
  - Detailed telemetry cards
  - Real-time 1Hz polling
  - Manual refresh capability
  - Toggle live overlay activation

- **Detail Pane Integration**
  - `StatsPillFeatureDetail` provides settings panel
  - Live overlay toggle in detail pane
  - Polling frequency configuration
  - Integrated with three-pane layout

## Architecture

```
statspill/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/squidink/alloy/modules/statspill/
│   │       ├── di/
│   │       │   └── StatsModule.kt             # Hilt DI module
│   │       ├── data/
│   │       │   └── StatsRepositoryImpl.kt     # Repository implementation
│   │       ├── StatsGlanceWidget.kt           # Android Glance widget
│   │       ├── StatsPillOverlayService.kt     # Overlay service
│   │       ├── StatsPillFeatureDetail.kt      # Detail pane settings
│   │       ├── StatsViewModel.kt              # MVI ViewModel
│   │       └── ui/
│   │           └── StatsScreen.kt             # Jetpack Compose UI
│   └── test/
│       └── java/com/squidink/alloy/modules/statspill/
│           └── StatsViewModelTest.kt
└── README.md
```

### Layer Breakdown

#### Domain Layer (via core:domain)

- `IStatsRepository`: Interface for stats operations
- System stats domain models

#### Data Layer

- `StatsRepositoryImpl`: Repository implementation for:
  - Reading `/proc/meminfo` for memory stats
  - CPU usage calculation via `/proc/stat`
  - Battery info via `BroadcastReceiver`

#### Core Components

**BatteryInfo Model**

```kotlin
data class BatteryInfo(
    val level: Int = 0,
    val scale: Int = 100,
    val percentage: Int = 0,
    val health: Int = BatteryManager.BATTERY_HEALTH_UNKNOWN,
    val status: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
    val temperature: Int = 0,
    val voltage: Int = 0,
    val isCharging: Boolean = false,
)
```

**StatsUiState**

```kotlin
data class StatsUiState(
    val memInfo: MemInfo = MemInfo(),
    val cpuUsagePercent: Float? = null,
    val netStats: NetStats = NetStats(),
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val isLiveOverlayActive: Boolean = false,
    val isPolling: Boolean = false,
    val overlayServiceIntent: Intent? = null,
)
```

#### Presentation Layer

- `StatsViewModel`: MVI pattern with:
  - 1Hz polling loop via coroutines
  - Battery broadcast receiver registration
  - Live overlay toggle management with permission check
  - Toast notifications for permission warnings

- `StatsScreen`: Dashboard UI with:
  - CPU utilization card
  - RAM usage card
  - Network speed card
  - Battery status card
  - Live overlay toggle with snackbar feedback
  - Manual refresh button

- `StatsPillOverlayService`: `LifecycleService` for:
  - `SYSTEM_ALERT_WINDOW` overlay management
  - Compose-based pill rendering
  - Foreground notification for service persistence
  - Real-time CPU/RAM/network telemetry updates

## Dependencies

```kotlin
implementation(project(":core:common"))
implementation(project(":core:design"))
implementation(project(":core:layout"))
implementation(project(":core:proc"))
implementation(project(":core:domain"))
implementation(project(":core:permissions"))

// Lifecycle service
implementation(libs.androidx.lifecycle.service)

// Glance widgets
implementation(libs.androidx.glance.appwidget)
implementation(libs.androidx.glance.material3)

// Hilt for DI
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)

// Navigation Compose for hiltViewModel
implementation(libs.androidx.hilt.navigation.compose)
```

## Usage

### Accessing the Stats Dashboard

```kotlin
@Composable
fun SystemTelemetry() {
    val viewModel: StatsViewModel = hiltViewModel()
    StatsScreen(viewModel = viewModel)
}
```

### Controlling Live Overlay

```kotlin
// Enable live overlay pill
viewModel.onAction(StatsUiAction.ToggleLiveOverlay(true))

// Disable live overlay pill
viewModel.onAction(StatsUiAction.ToggleLiveOverlay(false))

// Manual refresh
viewModel.onAction(StatsUiAction.RefreshNow)
```

### Starting the Overlay Service

The ViewModel handles overlay service control with permission checking:

```kotlin
// Enable live overlay (ViewModel handles permission check)
viewModel.onAction(StatsUiAction.ToggleLiveOverlay(true))

// Disable live overlay
viewModel.onAction(StatsUiAction.ToggleLiveOverlay(false))

// If permission is denied, a toast will be shown
// User can then grant permission in system settings
```

Manual service control (alternative approach):

```kotlin
// Check overlay permission first
if (Settings.canDrawOverlays(context)) {
    val intent = Intent(context, StatsPillOverlayService::class.java)
    context.startForegroundService(intent)
} else {
    // Show permission request or toast
}
```

### Detail Pane Settings

The `StatsPillFeatureDetail` class implements `FeatureDetail` to provide
settings content in the three-pane layout's detail pane:

```kotlin
class StatsPillFeatureDetail : FeatureDetail {
    override val showsDetailPane: Boolean = true
    
    @Composable
    override fun DetailContent() {
        // Display overlay and polling settings
    }
}
```

When the StatsPill feature is selected, the detail pane automatically shows
the `StatsPillFeatureDetail` content.

## Testing

```bash
# Run unit tests
./gradlew :modules:statspill:test

# Test telemetry polling
./gradlew :modules:statspill:testDebugUnitTest
```

## Technical Details

### Telemetry Polling

The ViewModel polls system stats at 1Hz:

```kotlin
private fun startPolling() {
    pollingJob = viewModelScope.launch {
        while (true) {
            pollVitals()
            delay(1000L) // 1Hz polling
        }
    }
}
```

### Battery Broadcast Receiver

```kotlin
private fun registerBatteryReceiver() {
    batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Extract battery info from intent extras
        }
    }
    context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
}
```

### Overlay Service

The `StatsPillOverlayService`:

1. Checks `SYSTEM_ALERT_WINDOW` permission
2. Creates notification channel for foreground service
3. Sets up Compose view with overlay params
4. Manages telemetry loop with coroutine scope
5. Properly cleans up on destroy

```kotlin
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
    PixelFormat.TRANSLUCENT
).apply {
    gravity = Gravity.TOP or Gravity.END
    x = 32
    y = 32
}
```

## Future Enhancements

See [FEATURE_ENHANCEMENTS.md](../../.md-storage/planning/FEATURE_ENHANCEMENTS.md#statspill-module-system-telemetry) for the complete roadmap.

### High Priority
- [x] **Network speed monitor** (upload/download)
- [ ] **Customizable overlay position** (drag to reposition)
- [ ] **Alert thresholds** for high CPU/RAM/temperature
- [ ] **Sparkline graphs** in widgets for trends

### Medium Priority
- [ ] **GPU monitoring** alongside CPU
- [ ] **Storage monitoring** for available space
- [ ] **Custom refresh interval** (1s, 5s, 30s)
- [ ] **Dark/light theme** for overlay

### Nice-to-Have
- [ ] Custom widget sizes (1×1, 2×2, 4×1)
- [ ] FPS counter for gaming
- [ ] Historical usage charts
