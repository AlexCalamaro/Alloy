# Scratch Module

Pinned scratchpad for quick notes, checklists, and time tracking.

## Overview

The Scratch module provides a persistent, always-available workspace for quick notes, task checklists, and time tracking. It features auto-save functionality and multiple workspace panes for different productivity modes.

## Capabilities

### Core Features

- **Text Editor Pane**
  - Markdown-compatible text editing
  - Auto-save with 500ms debounce
  - Persistent storage via Room database
  - Full-screen editing capability

- **Checklist Pane**
  - Add, edit, delete checklist items
  - Toggle completion status with strikethrough
  - Persistent item state
  - Empty state guidance

- **Stopwatch Pane**
  - Start/stop/reset functionality
  - Lap recording with split times
  - Visual lap history display
  - Split time calculations between laps

- **Timer Widget**
  - Always-visible countdown timer
  - Start/pause/reset controls
  - Integrated in header for quick access

## Architecture

```
scratch/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/squidink/alloy/modules/scratch/
│   │       ├── di/
│   │       │   └── ScratchModule.kt         # Hilt DI module
│   │       ├── data/
│   │       │   └── ScratchRepositoryImpl.kt # Repository implementation
│   │       ├── db/
│   │       │   ├── ScratchDatabase.kt       # Room database
│   │       │   ├── ScratchDao.kt            # Data access object
│   │       │   └── ScratchEntity.kt         # Room entity
│   │       ├── ScratchViewModel.kt          # MVI ViewModel
│   │       └── ui/
│   │           └── ScratchScreen.kt         # Jetpack Compose UI
│   └── test/
│       └── java/com/squidink/alloy/modules/scratch/
│           └── ScratchViewModelTest.kt
└── README.md
```

### Layer Breakdown

#### Domain Layer (via core:domain)
- `IScratchRepository`: Interface defining scratch operations
- `Scratch`: Domain model for scratchpad entries

#### Data Layer
- `ScratchRepositoryImpl`: Repository implementation handling:
  - Database operations via Room DAO
  - Thread dispatching using coroutines
  - Mapping between domain models and Room entities

- `ScratchDatabase`: Room database for persistent note storage
- `ScratchDao`: Async data access with Flow support

#### Presentation Layer

**ViewModel (MVI Pattern)**

```kotlin
data class ScratchUiState(
    val noteContent: String = "",
    val checklistItems: List<ChecklistItem> = emptyList(),
    val activePane: ScratchPane = ScratchPane.TEXT,
    val isTimerRunning: Boolean = false,
    val timerSeconds: Int = 0,
    val isStopwatchRunning: Boolean = false,
    val stopwatchSeconds: Int = 0,
    val stopwatchLaps: List<Int> = emptyList(),
)
```

**Workspace Panes**

- `ScratchPane.TEXT`: Text editor with auto-save
- `ScratchPane.CHECKLIST`: Task management
- `ScratchPane.STOPWATCH`: Time tracking

**UI Components**

- `ScratchScreen`: Main container with tab row
- `TextEditorPane`: Markdown text editing
- `ChecklistPane`: Task list management
- `StopwatchPane`: Lap-based time tracking

### Auto-Save Mechanism

The scratchpad implements a debounce-based auto-save:

```kotlin
private fun scheduleAutoSave(content: String) {
    autoSaveJob?.cancel()
    pendingContent = content
    
    autoSaveJob = viewModelScope.launch {
        delay(500) // 500ms debounce
        savePendingContent()
    }
}
```

## Dependencies

```kotlin
implementation(project(":core:common"))
implementation(project(":core:design"))
implementation(project(":core:datastore"))
implementation(project(":core:domain"))

// Room for persistence
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)

// Hilt for DI
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
```

## Usage

### Accessing the Scratch Screen

```kotlin
@Composable
fun Scratchpad() {
    val viewModel: ScratchViewModel = hiltViewModel()
    ScratchScreen(viewModel = viewModel)
}
```

### Switching Panes

```kotlin
// Switch to checklist pane
viewModel.onAction(ScratchUiAction.SetPane(ScratchPane.CHECKLIST))

// Switch to stopwatch pane
viewModel.onAction(ScratchUiAction.SetPane(ScratchPane.STOPWATCH))
```

### Managing Checklist Items

```kotlin
// Add item
viewModel.onAction(ScratchUiAction.AddChecklistItem("New task"))

// Toggle completion
viewModel.onAction(ScratchUiAction.ToggleChecklistItem(itemId))

// Delete item
viewModel.onAction(ScratchUiAction.DeleteChecklistItem(itemId))
```

### Stopwatch Controls

```kotlin
// Start/stop/reset
viewModel.onAction(ScratchUiAction.StartStopwatch)
viewModel.onAction(ScratchUiAction.StopStopwatch)
viewModel.onAction(ScratchUiAction.ResetStopwatch)

// Record lap
viewModel.onAction(ScratchUiAction.AddLap)
```

## Testing

```bash
# Run unit tests
./gradlew :modules:scratch:test

# Test auto-save debounce
./gradlew :modules:scratch:testDebugUnitTest
```

## Technical Details

### Coroutine Lifecycle Management

The ViewModel properly manages coroutines:

```kotlin
override fun onCleared() {
    super.onCleared()
    stopTimer()
    stopStopwatch()
    autoSaveJob?.cancel()
    savePendingContent() // Ensure final save
}
```

### Lap Time Calculation

Lap splits are calculated relative to the previous lap:

```kotlin
val split = lapSeconds - prevLap
```

## Future Enhancements

- [ ] Rich text formatting support
- [ ] Multiple scratchpad tabs
- [ ] Search within notes
- [ ] Export to file
- [ ] Sync across devices
- [ ] Template snippets
- [ ] Timer presets
