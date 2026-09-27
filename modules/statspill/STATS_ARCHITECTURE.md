# StatsPill Modular Architecture

This document describes the modular, extensible architecture for the StatsPill module, following DRY and MVI principles.

## Overview

The stats system is organized into four distinct layers:

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI LAYER (MVI)                           │
│  StatsScreen → StatsUiState ←→ StatsUiAction → StatsViewModel  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                     DOMAIN LAYER (Use Cases)                    │
│  IStatsRepository (interface)  ←  StatsRepositoryImpl          │
│  Domain Models: SystemStats, BatteryInfo, NetworkStats, etc.   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    DATA LAYER (Data Sources)                    │
│  SystemStatsDataSource, BatteryDataSource, NetworkDataSource   │
│  DiskDataSource, ThermalDataSource                             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  INFRASTRUCTURE (Android APIs)                  │
│  SystemStatsReader, BatteryManager, TrafficStats, etc.         │
└─────────────────────────────────────────────────────────────────┘
```

## Core Principles

### 1. DRY (Don't Repeat Yourself)

- **Generic `StatDataSource<T>` interface**: All data sources implement the same interface
- **Unified `StatType` interface**: All stat types share common properties
- **Polymorphic UI components**: `StatCard` renders different stat types consistently

### 2. MVI (Model-View-Intent)

- **`StatsUiState`**: Immutable state containing all UI data
- **`StatsUiAction`**: Sealed interface for user interactions
- **`StatsUiEffect`**: One-shot side effects (toasts, navigation)

### 3. Single Responsibility

Each data source handles exactly one type of stat:
- `SystemStatsDataSource`: CPU and memory
- `BatteryDataSource`: Battery state
- `NetworkDataSource`: Network I/O
- `DiskDataSource`: Storage usage
- `ThermalDataSource`: Temperature readings

### 4. Open/Closed Principle

Add new stat types by:
1. Creating a new domain model implementing `StatType`
2. Creating a new data source implementing `StatDataSource<T>`
3. Registering in the DI module

**No modifications needed to existing code.**

## Stat Types

### Built-in Stat Categories

| Category | Data Source | Domain Model | Description |
|----------|-------------|--------------|-------------|
| `SYSTEM` | `SystemStatsDataSource` | `SystemStats` | CPU, memory usage |
| `POWER` | `BatteryDataSource` | `BatteryInfo` | Battery level, charging state |
| `NETWORK` | `NetworkDataSource` | `NetworkStats` | RX/TX bytes, throughput |
| `STORAGE` | `DiskDataSource` | `DiskStats` | Disk space usage |
| `THERMAL` | `ThermalDataSource` | `ThermalStats` | Battery temperature (system temps require hidden APIs) |
| `CUSTOM` | TBD | TBD | User-defined stats |

### Domain Models

All stat types implement the `StatType` interface:

```kotlin
interface StatType {
    val id: String
    val name: String
    val category: StatCategory
    val timestamp: Long
}
```

Example models:

```kotlin
data class SystemStats(
    override val id: String = "system",
    override val name: String = "System",
    override val category: StatCategory = StatCategory.SYSTEM,
    override val timestamp: Long,
    val memoryUsedBytes: Long,
    val memoryTotalBytes: Long,
    val memoryPercent: Float,
    val cpuPercent: Float
) : StatType

data class DiskStats(
    override val id: String = "disk",
    override val name: String = "Storage",
    override val category: StatCategory = StatCategory.STORAGE,
    override val timestamp: Long,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val percentUsed: Float
) : StatType
```

## Repository Interface

The `IStatsRepository` provides both generic and type-safe access:

```kotlin
interface IStatsRepository {
    // Generic category-based access
    fun observeStats(category: StatCategory): Flow<StatType>
    fun observeAllStats(): Flow<Map<StatCategory, StatType>>
    suspend fun pollStats(category: StatCategory): StatType
    suspend fun pollAllStats(): Map<StatCategory, StatType>
    
    // Type-safe convenience methods
    fun observeSystemStats(): Flow<SystemStats>
    fun observeBatteryInfo(): Flow<BatteryInfo>
    fun observeNetworkStats(): Flow<NetworkStats>
    fun observeDiskStats(): Flow<DiskStats>
    fun observeThermalStats(): Flow<ThermalStats>
}
```

## Adding a New Stat Type

### Step 1: Define Domain Model

Create a new data class in `../../core/domain/src/main/java/com/squidink/alloy/core/domain/common/repository`:

```kotlin
data class CustomStats(
    override val id: String = "custom",
    override val name: String = "Custom",
    override val category: StatCategory = StatCategory.CUSTOM,
    override val timestamp: Long = System.currentTimeMillis(),
    val customValue: Float = 0f,
    // ... your custom fields
) : StatType
```

### Step 2: Create Data Source

Create a new data source in `core/data/src/main/java/com/squidink/alloy/core/data/datasource/`:

```kotlin
@Singleton
class CustomDataSource @Inject constructor(
    // Inject dependencies
) : StatDataSource<CustomStats> {
    
    override suspend fun read(): CustomStats {
        // Read current value
        return CustomStats(...)
    }
    
    override fun observe(): Flow<CustomStats> = flow {
        while (true) {
            emit(read())
            delay(1000) // Adjust polling interval
        }
    }
}
```

**Note:** The `observe()` method should NOT use `.flowOn(Dispatchers.IO)` on SharedFlow as it has no effect due to operator fusion.

### Step 3: Register in DI Module

Update `modules/statspill/src/main/java/com/squidink/alloy/modules/statspill/di/StatsModule.kt`:

```kotlin
@Provides
@Singleton
fun provideCustomDataSource(...): CustomDataSource {
    return CustomDataSource(...)
}

@Provides
@StatDataSources
fun provideStatDataSourcesMap(
    // ... existing sources
    custom: CustomDataSource
): Map<StatCategory, @JvmSuppressWildcards StatDataSource<out StatType>> {
    return mapOf(
        // ... existing mappings
        StatCategory.CUSTOM to custom
    )
}
```

### Step 4: Update Repository

Add convenience methods to `IStatsRepository` and implement in `StatsRepositoryImpl`:

```kotlin
// In IStatsRepository
fun observeCustomStats(): Flow<CustomStats>
suspend fun pollCustomStats(): CustomStats

// In StatsRepositoryImpl
override fun observeCustomStats(): Flow<CustomStats> {
    return customDataSource.observe().flowOn(Dispatchers.IO)
}

override suspend fun pollCustomStats(): CustomStats {
    return withContext(Dispatchers.IO) {
        customDataSource.read()
    }
}
```

### Step 5: Update UI

Add to `StatsUiState`:

```kotlin
data class StatsUiState(
    // ... existing fields
    val customStats: CustomStats? = null
)
```

Update `StatsDataObserver`:

```kotlin
private fun observeCustomStats(scope: CoroutineScope) {
    scope.launch {
        statsRepository.observeCustomStats().collectLatest { customStats ->
            stateUpdater { currentState ->
                currentState.copy(customStats = customStats)
            }
        }
    }
}
```

Add UI card in `StatsGrid.kt`:

```kotlin
@Composable
fun CustomStatCard(stats: CustomStats, modifier: Modifier) {
    // Your custom card implementation
}
```

## UI Components

### StatsGrid

The main container that displays all stat cards:

```kotlin
@Composable
fun StatsGrid(
    stats: ResourceStats,
    settings: StatsUiState,
    modifier: Modifier = Modifier
)
```

### Stat Cards

Each stat type has its own card composable with consistent styling:

- Color-coded based on usage percentage (green → yellow → red)
- Standard padding and elevation
- Responsive layout

## Testing

### Unit Tests

Test each data source independently:

```kotlin
class SystemStatsDataSourceTest {
    @Test
    fun `read returns valid SystemStats`() = runTest {
        val dataSource = SystemStatsDataSource(mockReader)
        val result = dataSource.read()
        
        assertThat(result.cpuPercent).isGreaterThan(0f)
        assertThat(result.memoryTotalBytes).isGreaterThan(0L)
    }
}
```

### ViewModel Tests

Test MVI state transitions:

```kotlin
class StatsViewModelTest {
    @Test
    fun `TogglePolling updates isPolling state`() = runTest {
        val viewModel = StatsViewModel(mockRepository)
        
        viewModel.onAction(StatsUiAction.TogglePolling)
        
        assertThat(viewModel.uiState.value.isPolling).isTrue()
    }
}
```

## Migration Notes

### Legacy Models

The following legacy models are deprecated but still available for backward compatibility:

- `core.proc.MemInfo` → Use `SystemStats.memoryTotalBytes` / `SystemStats.memoryUsedBytes`
- `core.proc.NetStats` → Use `NetworkStats`
- `core.data.datasource.BatteryInfo` → Use `core.domain.repository.BatteryInfo`

### Old Data Sources

The old `SystemStatsDataSource` that returned `SystemStatsData` has been replaced. Update imports:

```kotlin
// Old
import com.squidink.alloy.core.data.datasource.SystemStatsData

// New
import com.squidink.alloy.core.domain.repository.SystemStats
```

## Performance Considerations

| Stat Type | Polling Interval | Notes |
|-----------|------------------|-------|
| SYSTEM | 1Hz | CPU/memory change rapidly |
| POWER | Event-driven | Battery broadcasts via BroadcastReceiver |
| NETWORK | 1Hz | Throughput calculation |
| STORAGE | 0.1Hz (100ms) | Disk changes slowly |
| THERMAL | 2Hz (500ms) | Battery temp only (system temps require hidden APIs) |

## Implementation Summary

### Files Created

| File | Purpose |
|------|---------|
| `core/domain/src/.../model/StatType.kt` | Base interface and categories |
| `core/data/src/.../datasource/StatDataSource.kt` | Generic data source interface |
| `core/data/src/.../datasource/SystemStatsDataSource.kt` | CPU/memory data source |
| `core/data/src/.../datasource/BatteryDataSource.kt` | Battery data source |
| `core/data/src/.../datasource/NetworkDataSource.kt` | Network data source |
| `core/data/src/.../datasource/DiskDataSource.kt` | Storage data source |
| `core/data/src/.../datasource/ThermalDataSource.kt` | Temperature data source |
| `modules/statspill/STATS_ARCHITECTURE.md` | This documentation |

### Files Modified

| File | Changes |
|------|---------|
| `core/domain/.../IStatsRepository.kt` | Added generic methods, new stat types |
| `core/domain/.../DomainModels.kt` | Added StatType interface, DiskStats, ThermalStats |
| `modules/statspill/.../StatsRepositoryImpl.kt` | Unified repository with all data sources |
| `modules/statspill/.../StatsDataObserver.kt` | Added disk and thermal observation |
| `modules/statspill/.../StatsViewModel.kt` | Added disk and thermal state |
| `modules/statspill/.../StatsGrid.kt` | Added disk, battery, thermal cards |
| `modules/statspill/.../StatsModule.kt` | Added data source bindings |

### Key Design Decisions

1. **ThermalDataSource limitation**: Only battery temperature is available via public APIs. Full system thermal data requires hidden APIs or root access.

2. **No flowOn on SharedFlow**: The `flowOn` operator has no effect on SharedFlow due to operator fusion.

3. **Legacy compatibility**: Old data classes in `StatsDataSources.kt` are deprecated but retained for backward compatibility.

## Future Enhancements

1. **Custom Stats**: User-defined stat sources
2. **Stat Aggregation**: Historical trends and averages
3. **Alerts**: Threshold-based notifications
4. **Export**: CSV/JSON export functionality
5. **Widgets**: Home screen widgets for each stat type
6. **Full Thermal Support**: Integration with hidden thermal APIs (requires careful consideration of Play Store compliance)
