# StatsPill Module

System telemetry dashboard with live overlay pill display.

## Overview

The StatsPill module provides real-time system telemetry monitoring including CPU usage, memory utilization, network statistics, and battery status. It features both a detailed dashboard view and a compact floating overlay ("pill") that displays live vitals over any desktop window.

## Capabilities

### Core Features

- **System Telemetry**
  - CPU usage percentage monitoring
  - Memory (RAM) usage tracking
  - Total, used, and available memory display
  - Network upload/download speed monitoring

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
  - Detailed telemetry cards with color-coded usage indicators
  - Real-time 1Hz polling
  - Manual refresh capability
  - Toggle live overlay activation
  - Configurable display preferences

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
│   │       │   ├── StatsModule.kt                 # Repository bindings
│   │       │   └── StatsDataSourceModule.kt       # Data source providers
│   │       ├── data/
│   │       │   └── StatsRepositoryImpl.kt         # Repository implementation
│   │       ├── StatsGlanceWidget.kt               # Android Glance widget
│   │       ├── StatsPillOverlayService.kt         # Overlay service
│   │       ├── StatsPillFeatureDetail.kt          # Detail pane settings
│   │       ├── StatsViewModel.kt                  # MVI ViewModel
│   │       └── ui/
│   │           ├── StatsScreen.kt                 # Main dashboard screen
│   │           ├── StatsGrid.kt                   # Stats grid layout
│   │           ├── StatPill.kt                    # Overlay pill component
│   │           ├── SettingsPanel.kt               # Settings UI panel
│   │           └── StatsColorUtils.kt             # Color calculation utilities
│   └── test/
│       └── java/com/squidink/alloy/modules/statspill/
│           ├── StatsViewModelTest.kt              # ViewModel tests
│           └── FakeStatsRepository.kt             # Test doubles
└── README.md
```

### Layer Breakdown

#### Domain Layer (via core:domain)

- `IStatsRepository`: Interface for stats operations
- `SystemStats`: Domain model for system statistics

#### Data Layer (via core:data)

- `StatsRepositoryImpl`: Repository implementation coordinating:
  - `SystemStatsDataSource`: CPU, memory, network statistics
  - `BatteryDataSource`: Battery information from system
- Data models:
  - `SystemStatsData`: Raw system statistics
  - `BatteryInfo`: Battery state information

#### Core Components

**StatsUiState (MVI State)**

```kotlin
data class StatsUiState(
    val memInfo: MemInfo = MemInfo(),
    val cpuUsagePercent: Float? = null,
    val netStats: NetStats = NetStats(),
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val isLiveOverlayActive: Boolean = false,
    val isPolling: Boolean = false,
    val overlayServiceIntent: Intent? = null,
    val showPill: Boolean = true,
    val usePercentages: Boolean = true,
    val cornerPosition: CornerPosition = CornerPosition.TOP_RIGHT,
    val isLiveOverlayPermissionGranted: Boolean = false
)
```

**StatsUiAction (MVI Actions)**

```kotlin
sealed interface StatsUiAction {
    data object TogglePolling : StatsUiAction
    data object ToggleLiveOverlay : StatsUiAction
    data object RefreshNow : StatsUiAction
    data object OpenOverlayPermissionSettings : StatsUiAction
    data object DismissPermissionDialog : StatsUiAction
    data class UpdateShowPill(val show: Boolean) : StatsUiAction
    data class UpdateUsePercentages(val usePercentages: Boolean) : StatsUiAction
    data class UpdateCornerPosition(val position: CornerPosition) : StatsUiAction
}
```

**StatsUiEffect (MVI Effects)**

```kotlin
sealed interface StatsUiEffect {
    data class ShowToast(val message: String) : StatsUiEffect
    data object OpenOverlayPermissionSettings : StatsUiEffect
}
```

#### Presentation Layer

**StatsViewModel**

MVI-compliant ViewModel handling:
- State observation from repositories
- User action dispatching
- Side effect generation
- Lifecycle management

Key responsibilities:
- `observeSystemStats()`: Subscribes to system stats flow
- `observeBatteryInfo()`: Subscribes to battery info flow
- `observeNetworkStats()`: Subscribes to network stats flow
- `loadSettings()`: Loads user preferences from SettingsRepository
- `updateSettings()`: Updates settings via SettingsRepository
- `onAction()`: Handles all UI actions

**StatsScreen**

Main dashboard Composable featuring:
- CPU utilization card with color-coded usage
- RAM usage card with color-coded usage
- Network speed card (neutral color)
- Live overlay toggle control
- Manual refresh button
- Permission status display

**StatsGrid**

Grid layout for stats display:
- `ResourceStats`: Data wrapper for UI display
- Color-coded cards based on usage percentage
- Toggle between percentage and raw value display

**StatPill**

Overlay pill component:
- Compact stats display
- Configurable corner position
- Color-coded CPU and RAM usage
- Neutral network display

**SettingsPanel**

Settings configuration panel:
- Show/hide pill toggle
- Percentage/raw value toggle
- Corner position selection
- Real-time settings updates

**StatsColorUtils**

Color calculation utilities:
- `calculatePercentageColor()`: Green-to-red gradient based on usage
- `calculateValueColor()`: Generic value-to-color mapping

**StatsPillOverlayService**

`LifecycleService` for:
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
implementation(project(":core:data"))  // New: Data layer infrastructure

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

// Coroutines test
testImplementation(libs.kotlinx.coroutines.test)
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

### Running Tests

```bash
# Run all unit tests
./gradlew :modules:statspill:test

# Run debug unit tests
./gradlew :modules:statspill:testDebugUnitTest

# Run with coverage
./gradlew :modules:statspill:jacocoTestReport
```

### Test Structure

**ViewModel Tests** (`StatsViewModelTest.kt`)
- Test state initialization
- Test action handling (TogglePolling, RefreshNow, etc.)
- Test settings updates
- Test effect generation

**Repository Tests** (`StatsRepositoryTest.kt`)
- Test `observeSystemStats()` flow emission
- Test `pollSystemStats()` single poll
- Test `getMemoryPercent()` calculation
- Test `getCpuPercent()` calculation

**Fake Implementations** (`FakeStatsRepository.kt`)
- Fake implementation of `IStatsRepository`
- Provides test data for ViewModel tests
- Extendable for specific test scenarios

### Test Coverage Goals

- **ViewModel**: 80%+ coverage for action handling
- **Repository**: 90%+ coverage for data operations
- **Data Sources**: 70%+ coverage for data retrieval
- **Composables**: Test critical rendering paths

### Example Test

```kotlin
@Test
fun `TogglePolling action starts polling`() = runTest {
    val viewModel = StatsViewModel(
        statsRepository = fakeRepository,
        statsRepositoryImpl = fakeRepository,
        settingsRepository = fakeSettingsRepo,
        context = mockContext(),
        permissionsManager = mockPermissionsManager()
    )
    
    viewModel.onAction(StatsUiAction.TogglePolling)
    
    assertEquals(true, viewModel.uiState.value.isPolling)
}
```

## Technical Details

### Telemetry Polling

Polling is now handled by `SystemStatsDataSource` with 1Hz frequency:

```kotlin
// In StatsRepositoryImpl
override fun observeSystemStats(): Flow<SystemStats> {
    return systemStatsDataSource.currentStats
        .map { stats ->
            SystemStats(
                memoryUsedBytes = stats.memoryUsedBytes,
                memoryTotalBytes = stats.memoryTotalBytes,
                memoryPercent = stats.memoryPercent,
                cpuPercent = stats.cpuPercent,
                timestamp = stats.timestamp
            )
        }
        .flowOn(Dispatchers.IO)
}
```

ViewModel observes the flow:

```kotlin
private fun observeSystemStats() {
    viewModelScope.launch {
        statsRepository.observeSystemStats().collectLatest { stats ->
            updateState { currentState ->
                currentState.copy(
                    memInfo = MemInfo(
                        totalMemKb = stats.memoryTotalBytes / 1024,
                        freeMemKb = (stats.memoryTotalBytes - stats.memoryUsedBytes) / 1024,
                        availableMemKb = (stats.memoryTotalBytes - stats.memoryUsedBytes) / 1024
                    ),
                    cpuUsagePercent = stats.cpuPercent
                )
            }
        }
    }
}
```

### Battery Broadcast Receiver

Handled by `BatteryDataSource`:

```kotlin
// In BatteryDataSource
fun registerBatteryReceiver(): BroadcastReceiver {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val batteryInfo = BatteryInfo(
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0),
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100),
                // ... extract other properties
            )
            _batteryInfo.value = batteryInfo
        }
    }
    context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    return receiver
}
```

### Data Source Coordination

`StatsRepositoryImpl` coordinates multiple data sources:

```kotlin
class StatsRepositoryImpl @Inject constructor(
    private val systemStatsDataSource: SystemStatsDataSource,
    private val batteryDataSource: BatteryDataSource
) : IStatsRepository {
    
    override fun observeSystemStats(): Flow<SystemStats> {
        return systemStatsDataSource.currentStats.map { /* map to domain */ }
    }
    
    fun observeBatteryInfo(): Flow<BatteryInfo> {
        return batteryDataSource.batteryInfo
    }
    
    fun observeNetworkStats(): Flow<NetStats> {
        return systemStatsDataSource.currentStats.map { it.netStats }
    }
}
```

### MVI Pattern Implementation

The ViewModel follows strict MVI pattern:

1. **State**: `StatsUiState` - Immutable, represents complete UI state
2. **Action**: `StatsUiAction` - Sealed interface for user interactions
3. **Effect**: `StatsUiEffect` - One-shot side effects (toasts, navigation)

```kotlin
class StatsViewModel @Inject constructor(
    private val statsRepositoryImpl: StatsRepositoryImpl,
    private val settingsRepository: SettingsRepository,
    // ...
) : BaseViewModel<StatsUiState, StatsUiAction, StatsUiEffect>(StatsUiState()) {
    
    // Initialize by observing data streams
    init {
        observeSystemStats()
        observeBatteryInfo()
        observeNetworkStats()
        loadSettings()
    }
    
    // Handle actions
    override fun onAction(action: StatsUiAction) {
        when (action) {
            is StatsUiAction.TogglePolling -> togglePolling()
            is StatsUiAction.UpdateShowPill -> updateSettings(showPill = action.show)
            // ...
        }
    }
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
