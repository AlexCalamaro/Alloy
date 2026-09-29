# StatsPill Module

System telemetry dashboard with live floating overlay pill display. Serves as the architectural reference standard for Alloy modules.

## Overview

The StatsPill module provides real-time system telemetry monitoring including CPU usage, memory utilization, network throughput, battery status, storage, and thermal readings. It features both a responsive Material 3 dashboard view and a compact floating overlay pill (`SYSTEM_ALERT_WINDOW`) that displays live vitals over any desktop window or application.

## Capabilities

### Core Features

- **System Telemetry**
  - CPU usage percentage monitoring (0.0% to 100.0%)
  - Memory (RAM) utilization tracking
  - Total, used, and available memory display
  - Real-time network upload/download speeds

- **Battery Monitoring**
  - Reactive `callbackFlow` listening to `ACTION_BATTERY_CHANGED`
  - Battery level percentage, health, voltage, and charging status
  - Accurate battery temperature readings in Celsius

- **Storage & Thermals**
  - Storage space utilization (0.1Hz / 10s low-overhead polling)
  - Device and battery thermal state monitoring

- **Live Overlay Pill**
  - `SYSTEM_ALERT_WINDOW` floating overlay
  - Compact CPU/RAM/network display rendered with 100% Jetpack Compose
  - Foreground service with notification
  - Independent lifecycle: stays active over other apps until toggled off

- **Dashboard UI**
  - WCAG AA compliant Material 3 telemetry cards
  - Real-time 1Hz reactive polling with toggle control
  - Manual on-demand refresh capability
  - Configurable display format (Percentages vs Absolute Values)
  - Configurable overlay corner position

---

## Architecture

The module adheres strictly to **Clean Architecture** and **Model-View-Intent (MVI)** principles:

```
statspill/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/squidink/alloy/modules/statspill/
│   │       ├── StatsViewModel.kt                # Pure MVI ViewModel
│   │       ├── StatsActionsImpl.kt              # IStatsActions module contract
│   │       ├── StatsPillOverlayService.kt       # Persistent floating pill service
│   │       ├── StatsGlanceWidget.kt             # Jetpack Glance widget
│   │       ├── StatsPillFeatureDetail.kt        # Detail pane integration
│   │       ├── di/
│   │       │   └── StatsModule.kt               # Repository & Action Hilt bindings
│   │       ├── domain/
│   │       │   ├── model/
│   │       │   │   ├── StatType.kt              # Base interfaces and categories
│   │       │   │   ├── DomainModels.kt          # SystemStats, CombinedTelemetry...
│   │       │   │   └── StatError.kt             # Typed error domain model
│   │       │   ├── repository/
│   │       │   │   └── IStatsRepository.kt      # Domain repository contract
│   │       │   └── usecase/
│   │       │       ├── ObserveTelemetryUseCase.kt
│   │       │       ├── PollTelemetryUseCase.kt
│   │       │       ├── ObserveErrorsUseCase.kt
│   │       │       ├── ObserveSystemStatsUseCase.kt
│   │       │       └── CalculateSystemStatsUseCase.kt
│   │       ├── data/
│   │       │   ├── StatsRepositoryImpl.kt       # Repository implementation
│   │       │   └── datasource/
│   │       │       ├── StatDataSource.kt        # Generic reactive data source interface
│   │       │       ├── SystemStatsDataSource.kt # 1Hz CPU/RAM data source
│   │       │       ├── BatteryDataSource.kt     # Reactive callbackFlow battery source
│   │       │       ├── NetworkDataSource.kt     # 1Hz network throughput source
│   │       │       ├── DiskDataSource.kt        # 0.1Hz storage data source
│   │       │       └── ThermalDataSource.kt     # Temperature data source
│   │       ├── stats/
│   │       │   └── OverlayServiceManager.kt     # Overlay service lifecycle manager
│   │       └── ui/
│   │           ├── StatsScreen.kt               # Main dashboard composable
│   │           ├── StatsGrid.kt                 # Adaptive grid layout (1-col/2-col)
│   │           ├── StatCard.kt                  # Material 3 accessible card component
│   │           ├── SettingsPanel.kt             # Settings panel composable
│   │           └── StatsColorUtils.kt           # Color and percentage utilities
│   └── test/
│       └── java/com/squidink/alloy/modules/statspill/
│           ├── StatsViewModelTest.kt            # Exhaustive MVI ViewModel unit tests
│           ├── FakeStatsRepository.kt           # Test double repository
│           ├── data/
│           │   ├── StatsRepositoryImplTest.kt   # Repository coordination tests
│           │   └── datasource/
│           │       └── BatteryDataSourceTest.kt # Broadcast parsing tests
│           ├── domain/usecase/
│           │   └── DomainUseCasesTest.kt        # Domain use case tests
│           └── ui/
│               └── StatsColorUtilsTest.kt       # Color calculation tests
└── README.md
```

---

## MVI Contracts

### StatsUiState (MVI State)
Pure, immutable data state free of Android framework leaks (`Intent`, `Context`, `View`):

```kotlin
data class StatsUiState(
    val telemetry: CombinedTelemetry = CombinedTelemetry(),
    val errors: Map<StatCategory, StatError> = emptyMap(),
    val isLiveOverlayActive: Boolean = false,
    val isPolling: Boolean = true,
    val usePercentages: Boolean = true,
    val cornerPosition: CornerPosition = CornerPosition.TOP_RIGHT,
    val isLiveOverlayPermissionGranted: Boolean = false
) : UiState {
    val systemStats: SystemStats? get() = telemetry.systemStats
    val netStats: NetworkStats get() = telemetry.networkStats
    val batteryInfo: BatteryInfo get() = telemetry.batteryInfo
    val diskStats: DiskStats? get() = telemetry.diskStats
    val thermalStats: ThermalStats? get() = telemetry.thermalStats
}
```

### StatsUiAction (MVI Actions)
```kotlin
sealed interface StatsUiAction : UiAction {
    data object TogglePolling : StatsUiAction
    data class ToggleLiveOverlay(val enable: Boolean) : StatsUiAction
    data object RefreshNow : StatsUiAction
    data object OpenOverlayPermissionSettings : StatsUiAction
    data object DismissPermissionDialog : StatsUiAction
    data class UpdateUsePercentages(val usePercentages: Boolean) : StatsUiAction
    data class UpdateCornerPosition(val position: CornerPosition) : StatsUiAction
    data class ClearError(val category: StatCategory) : StatsUiAction
}
```

### StatsUiEffect (MVI Side Effects)
One-shot channel events collected with `LaunchedEffect(Unit)`:
```kotlin
sealed interface StatsUiEffect : UiEffect {
    data class ShowToast(val message: String) : StatsUiEffect
    data object OpenOverlayPermissionSettings : StatsUiEffect
}
```

---

## Testing

Run all unit tests:
```bash
./gradlew :modules:statspill:testDebugUnitTest
```

The test suite covers:
- **`StatsViewModelTest`**: MVI state reduction, polling toggle, refresh action, overlay toggling, settings changes, error handling, and one-shot effects.
- **`StatsRepositoryImplTest`**: Multi-source flow combination, fallback handling, and polling coordination.
- **`BatteryDataSourceTest`**: Broadcast intent parsing, edge-case scale handling, and status mapping.
- **`DomainUseCasesTest`**: Direct execution and emission verification for all domain use cases.
- **`StatsColorUtilsTest`**: Color ramp logic and numerical limits.
