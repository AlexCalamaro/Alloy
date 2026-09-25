# Scenes Module

Workspace scene launcher for multi-app desktop configurations.

## Overview

The Scenes module enables users to define and launch predefined workspace configurations that simultaneously open multiple applications with specific window placements. It's designed for power users who work with consistent application stacks (e.g., IDE + terminal side-by-side).

## Capabilities

### Core Features

- **Scene Definitions**
  - Named workspace configurations
  - Multi-step launch sequences
  - Descriptive metadata for easy identification

- **Window Placement**
  - LEFT/RIGHT split-screen positioning
  - TOP_LEFT/TOP_RIGHT quadrant placement
  - CENTER focused window
  - FULLSCREEN mode

- **Scene Launcher**
  - Programmatic app launching via intents
  - Window bounds configuration using `FLAG_ACTIVITY_LAUNCH_ADJACENT`
  - Parallel app launch for faster setup
  - Error handling for unavailable apps

- **Default Scenes**
  - Pre-configured "Coding Stack" scene
  - Easy extension with custom scenes

## Architecture

```
scenes/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/squidink/alloy/modules/scenes/
│   │       ├── SceneLauncher.kt           # Core launcher logic
│   │       ├── ScenesViewModel.kt         # MVI ViewModel
│   │       ├── model/
│   │       │   ├── Scene.kt               # Scene data model
│   │       │   └── SceneStep.kt           # Launch step definition
│   │       └── ui/
│   │           └── ScenesScreen.kt        # Jetpack Compose UI
│   └── test/
│       └── java/com/squidink/alloy/modules/scenes/
│           └── ScenesViewModelTest.kt
└── README.md
```

### Layer Breakdown

#### Domain Models

**SceneStep**
```kotlin
data class SceneStep(
    val id: String,
    val packageName: String,
    val activityName: String? = null,
    val placement: PlacementHint = PlacementHint.CENTER
)
```

**Scene**
```kotlin
data class Scene(
    val id: String,
    val name: String,
    val description: String = "",
    val shortcutKey: String? = null,
    val steps: List<SceneStep> = emptyList()
)
```

**PlacementHint**
- `LEFT`: Left half of screen
- `RIGHT`: Right half of screen
- `TOP_LEFT`: Top-left quadrant
- `TOP_RIGHT`: Top-right quadrant
- `CENTER`: Centered window
- `FULLSCREEN`: Full screen

#### Presentation Layer

- `ScenesViewModel`: MVI pattern with:
  - `ScenesUiState`: List of available scenes
  - `ScenesUiAction`: Fire, add, delete scenes
  - `ScenesUiEffect`: Toast notifications

- `ScenesScreen`: Composable UI displaying:
  - Scene cards with metadata
  - "Fire Scene" action buttons

#### Core Logic

- `SceneLauncher`: Singleton responsible for:
  - Calculating display bounds based on placement hints
  - Creating launch intents for packages
  - Applying `FLAG_ACTIVITY_LAUNCH_ADJACENT` for multi-window
  - Setting launch bounds via `ActivityOptions`

## Dependencies

```kotlin
implementation(project(":core:common"))
implementation(project(":core:design"))

// Hilt for DI
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
```

## Usage

### Creating a Custom Scene

```kotlin
val codingScene = Scene(
    id = "coding_workspace",
    name = "Coding Workspace",
    description = "IDE + Terminal side-by-side",
    steps = listOf(
        SceneStep(
            id = "1",
            packageName = "com.squidink.alloy",
            placement = PlacementHint.LEFT
        ),
        SceneStep(
            id = "2",
            packageName = "com.android.termux",
            placement = PlacementHint.RIGHT
        )
    )
)
```

### Firing a Scene

```kotlin
val sceneLauncher: SceneLauncher = hiltViewModel().sceneLauncher
val success = sceneLauncher.launchScene(context, scene)
```

### Accessing the Scenes Screen

```kotlin
@Composable
fun ScenesManager() {
    val viewModel: ScenesViewModel = hiltViewModel()
    ScenesScreen(viewModel = viewModel)
}
```

## Testing

```bash
# Run unit tests
./gradlew :modules:scenes:test

# Test scene launcher bounds calculation
./gradlew :modules:scenes:testDebugUnitTest
```

## Technical Details

### Window Bounds Calculation

The `SceneLauncher` calculates window bounds based on display metrics:

```kotlin
// LEFT placement
DisplayBounds(0, 0, width / 2, height)

// TOP_RIGHT placement
DisplayBounds(width / 2, 0, width, height / 2)
```

### Launch Intent Handling

1. Resolves launch intent for package or specific activity
2. Adds `FLAG_ACTIVITY_NEW_TASK` and `FLAG_ACTIVITY_LAUNCH_ADJACENT`
3. Creates `ActivityOptions` with calculated bounds
4. Starts activity with options bundle

## Future Enhancements

See [FEATURE_ENHANCEMENTS.md](../../FEATURE_ENHANCEMENTS.md#scenes-module-workspace-launcher) for the complete roadmap.

### High Priority
- [x] **Keyboard shortcuts** (`Ctrl+Alt+1…9`) for quick scene launch
- [x] **Scene templates** for common workflows (5 new templates added)
- [ ] **Import/export JSON** for sharing configurations

### Medium Priority
- [ ] **Window Hopper** for cycling same-app windows
- [ ] **Scene scheduler** for timed auto-launch
- [ ] **App availability check** before firing scene

### Nice-to-Have
- [ ] Multi-monitor support
- [ ] Scene transition animations
- [ ] Voice command integration
