# Settings Module

User preferences and application configuration management.

## Overview

The Settings module provides the foundation for user-configurable application preferences and system settings. Currently serving as a placeholder for future expansion, it establishes the architectural pattern for settings management within Alloy.

## Capabilities

### Current Features

- **Settings Framework**
  - MVI-based ViewModel structure
  - Refresh action support
  - Navigation integration

### Planned Features

- **User Preferences**
  - Theme selection (light/dark/system)
  - Font size adjustments
  - Behavior toggles

- **Application Settings**
  - Clipboard monitoring configuration
  - Auto-save intervals
  - Notification preferences

- **System Settings**
  - Overlay permissions management
  - Storage allocation
  - Data export/import

## Architecture

```
settings/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/squidink/alloy/modules/settings/
│   │       ├── di/
│   │       │   └── SettingsModule.kt          # Hilt DI module
│   │       ├── data/
│   │       │   ├── ISettingsRepository.kt     # Repository interface
│   │       │   └── SettingsRepositoryImpl.kt  # Repository implementation
│   │       ├── SettingsViewModel.kt           # MVI ViewModel
│   │       └── ui/
│   │           └── SettingsScreen.kt          # Jetpack Compose UI
│   └── test/
│       └── java/com/squidink/alloy/modules/settings/
│           └── SettingsViewModelTest.kt
└── README.md
```

### Layer Breakdown

#### Domain Layer (via core:domain)

- `ISettingsRepository`: Interface for settings operations
- Settings domain models (planned)

#### Data Layer

- `SettingsRepositoryImpl`: Repository implementation for:
  - DataStore-based preference persistence
  - Type-safe settings access
  - Flow-based reactive updates
  
**Repository Methods:**
```kotlin
// Observe settings
fun observeShowPill(): Flow<Boolean>
fun observeUsePercentages(): Flow<Boolean>
fun observeCornerPosition(): Flow<String>

// Update settings
suspend fun setShowPill(show: Boolean)
suspend fun setUsePercentages(usePercentages: Boolean)
suspend fun setCornerPosition(position: String)
```

#### Presentation Layer

**ViewModel (MVI Pattern)**

```kotlin
data class SettingsUiState(
    val isLoading: Boolean = false,
    // Future: val theme: Theme = Theme.SYSTEM
    // Future: val autoSaveEnabled: Boolean = true
) : UiState

sealed interface SettingsUiAction : UiAction {
    data object Refresh : SettingsUiAction
    // Future: data class SetTheme(val theme: Theme) : SettingsUiAction
}
```

**UI Components**

- `SettingsScreen`: Main settings container
- `SettingsScreenContent`: Content composable with navigation

## Dependencies

```kotlin
implementation(project(":core:common"))
implementation(project(":core:design"))
implementation(project(":core:domain"))

// Hilt for DI
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
```

## Usage

### Accessing the Settings Screen

```kotlin
@Composable
fun SettingsPage() {
    val viewModel: SettingsViewModel = hiltViewModel()
    SettingsScreen(viewModel = viewModel)
}
```

### Navigation Integration

```kotlin
// Navigate to settings
navController.navigate("settings")

// With nested routes
navController.navigate("settings/preferences")
```

### Extending Settings

```kotlin
// Add new settings action
sealed interface SettingsUiAction : UiAction {
    data object Refresh : SettingsUiAction
    data class UpdatePreference<T>(
        val key: String,
        val value: T
    ) : SettingsUiAction
}
```

## Testing

```bash
# Run unit tests
./gradlew :modules:settings:test

# Run ViewModel tests
./gradlew :modules:settings:testDebugUnitTest
```

## Implementation Guidelines

### Adding New Settings

1. Define setting in domain layer
2. Implement repository method
3. Add to ViewModel state and actions
4. Create UI component
5. Add tests

### DataStore Integration

```kotlin
// Example settings definition
object PreferencesKeys {
    val THEME = stringPreferencesKey("theme")
    val AUTO_SAVE = booleanPreferencesKey("auto_save")
}

// Example repository implementation
override fun getTheme(): Flow<String> {
    return dataStore.data.map { prefs ->
        prefs[PreferencesKeys.THEME] ?: "system"
    }
}
```

## Future Enhancements

See [FEATURE_ENHANCEMENTS.md](../../.md-storage/planning/FEATURE_ENHANCEMENTS.md#settings-module) for the complete roadmap.

### High Priority
- [ ] **Theme selection** (Light/Dark/System)
- [ ] **Module toggles** to enable/disable features
- [ ] **Data export** for clipboard/scratch data

### Medium Priority
- [ ] **Keyboard shortcut configuration**
- [ ] **Privacy controls** for data collection
- [ ] **Storage management** with one-tap cleanup

### Nice-to-Have
- [ ] Settings import from backup
- [ ] Cloud sync for preferences
- [ ] Accessibility enhancements
