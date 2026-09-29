# StatsPill Modular Architecture

This document describes the modular, extensible Clean Architecture and MVI implementation for the StatsPill module, serving as the architectural reference pattern for Alloy modules.

## Overview

The stats system is organized into distinct, unidirectional architectural layers:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        UI LAYER (MVI)                                   │
│  StatsScreen → StatsUiState ←→ StatsUiAction → StatsViewModel          │
│  StatCard / StatsGrid (Responsive & Accessible Material 3)              │
└─────────────────────────────────────────────────────────────────────────┘
                                      ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                     DOMAIN LAYER (Use Cases & Contracts)                │
│  ObserveTelemetryUseCase, PollTelemetryUseCase, ObserveErrorsUseCase    │
│  IStatsRepository (Contract Interface)                                  │
│  Domain Models: CombinedTelemetry, SystemStats, BatteryInfo, DiskStats...│
└─────────────────────────────────────────────────────────────────────────┘
                                      ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    DATA LAYER (Reactive Data Sources)                   │
│  StatsRepositoryImpl (Coordinates specialized data sources)             │
│  SystemStatsDataSource, BatteryDataSource, NetworkDataSource            │
│  DiskDataSource, ThermalDataSource                                      │
└─────────────────────────────────────────────────────────────────────────┘
                                      ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                  INFRASTRUCTURE & SYSTEM (Android APIs)                 │
│  SystemStatsReader (core:proc), Battery Broadcasts, StatFs              │
│  StatsPillOverlayService (SYSTEM_ALERT_WINDOW floating desktop pill)    │
└─────────────────────────────────────────────────────────────────────────┘
```

## Core Principles

### 1. Unidirectional Data Flow (MVI)
- **`StatsUiState`**: Immutable pure data state representing everything the UI needs. Free from Android framework leaks (`Intent`, `Context`).
- **`StatsUiAction`**: Sealed interface describing all user interactions.
- **`StatsUiEffect`**: One-shot channel-backed side effects collected via `LaunchedEffect(Unit)`.

### 2. Clean Architecture & Focused Use Cases
- Business logic and stream coordination reside in the Domain layer (`ObserveTelemetryUseCase`, `PollTelemetryUseCase`, etc.).
- UI components and ViewModels depend on Use Cases rather than coupling directly to concrete data sources or low-level proc readers.
- Background services (`StatsPillOverlayService`) consume domain telemetry streams (`ObserveTelemetryUseCase`) to ensure consistent calculations and formatting across the app and desktop overlay.

### 3. Reactive Data Sources & Lifecycle Safety
- **Generic `StatDataSource<T>`**: Consistent contract (`suspend fun read(): T`, `fun observe(): Flow<T>`).
- **Lifecycle-aware broadcast flows**: `BatteryDataSource` uses `callbackFlow` with automatic `awaitClose { unregisterReceiver(...) }` to eliminate memory leaks and avoid stale uncollected states.
- **Sensible polling rates**:
  - `SystemStatsDataSource`: 1Hz (rapid CPU/RAM telemetry).
  - `NetworkDataSource`: 1Hz (traffic throughput).
  - `DiskDataSource`: 0.1Hz / 10s (storage changes slowly).
  - `ThermalDataSource`: 0.2Hz / 5s (thermal readings from sticky battery broadcast).

### 4. Normalized Packages & Isolation
All files belonging to the Stats module are located under the root namespace:
`com.squidink.alloy.modules.statspill.*`

---

## Performance Considerations

| Telemetry Category | Polling Rate / Event | Mechanism |
|---|---|---|
| `SYSTEM` | 1Hz (1,000ms) | `/proc` via `SystemStatsReader` |
| `POWER` | Event-driven + Sticky | `callbackFlow` on `ACTION_BATTERY_CHANGED` |
| `NETWORK` | 1Hz (1,000ms) | `TrafficStats` via `SystemStatsReader` |
| `STORAGE` | 0.1Hz (10,000ms) | `StatFs` on app files / external storage |
| `THERMAL` | 0.2Hz (5,000ms) | Sticky battery broadcast temperature |

---

## Directory Structure

```
modules/statspill/
├── src/main/java/com/squidink/alloy/modules/statspill/
│   ├── StatsViewModel.kt                # MVI ViewModel
│   ├── StatsActionsImpl.kt              # Cross-module action provider (IStatsActions)
│   ├── StatsPillFeatureDetail.kt        # Detail pane integration
│   ├── StatsPillOverlayService.kt       # Persistent SYSTEM_ALERT_WINDOW floating pill
│   ├── StatsGlanceWidget.kt             # Jetpack Glance desktop widget
│   ├── data/
│   │   ├── StatsRepositoryImpl.kt       # IStatsRepository implementation
│   │   └── datasource/
│   │       ├── StatDataSource.kt        # Generic reactive data source interface
│   │       ├── SystemStatsDataSource.kt # CPU & memory data source (1Hz)
│   │       ├── BatteryDataSource.kt     # Reactive battery broadcast flow
│   │       ├── NetworkDataSource.kt     # Network throughput data source (1Hz)
│   │       ├── DiskDataSource.kt        # Storage data source (0.1Hz)
│   │       └── ThermalDataSource.kt     # Temperature data source (0.2Hz)
│   ├── domain/
│   │   ├── model/
│   │   │   ├── StatType.kt              # Base interfaces and StatCategory
│   │   │   ├── DomainModels.kt          # SystemStats, BatteryInfo, CombinedTelemetry...
│   │   │   └── StatError.kt             # Typed error model
│   │   ├── repository/
│   │   │   └── IStatsRepository.kt      # Domain repository contract
│   │   └── usecase/
│   │       ├── ObserveTelemetryUseCase.kt
│   │       ├── PollTelemetryUseCase.kt
│   │       ├── ObserveErrorsUseCase.kt
│   │       ├── ObserveSystemStatsUseCase.kt
│   │       └── CalculateSystemStatsUseCase.kt
│   ├── di/
│   │   └── StatsModule.kt               # Hilt interface bindings
│   └── ui/
│       ├── StatsScreen.kt               # Main dashboard UI
│       ├── StatsGrid.kt                 # Responsive, adaptive stats grid
│       ├── StatCard.kt                  # Material 3 accessible telemetry card
│       ├── SettingsPanel.kt             # Display & overlay settings
│       └── StatsColorUtils.kt           # Color and percentage utilities (0..100)
└── src/test/java/com/squidink/alloy/modules/statspill/
    ├── StatsViewModelTest.kt            # Comprehensive MVI ViewModel tests
    ├── FakeStatsRepository.kt           # Test double for repository
    ├── data/
    │   ├── StatsRepositoryImplTest.kt   # Repository coordination tests
    │   └── datasource/
    │       └── BatteryDataSourceTest.kt # Intent parsing & reactive tests
    ├── domain/usecase/
    │   └── DomainUseCasesTest.kt        # Domain use case tests
    └── ui/
        └── StatsColorUtilsTest.kt       # Color calculations test
```
