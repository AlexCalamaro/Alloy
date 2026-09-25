# Clip Module

Clipboard workbench for managing, transforming, and organizing clipboard history.

## Overview

The Clip module provides a powerful clipboard management system with real-time clip history, text transformations, and persistent storage. It serves as a productivity tool for developers who frequently copy and manipulate text data.

## Capabilities

### Core Features

- **Clipboard History Management**
  - Persistent storage of clipboard entries using Room database
  - Automatic clip capture from system clipboard
  - Timestamp tracking for clip entries

- **Text Transformations**
  - Case conversions (UPPER, lower)
  - Whitespace trimming
  - Line sorting (ascending/descending)
  - Line deduplication
  - JSON formatting (pretty/minify)
  - Base64 encoding/decoding
  - URL encoding/decoding

- **Clip Organization**
  - Pin important clips for quick access
  - Filter by pinned clips only
  - Search clips by content or source app
  - Delete unwanted clips

- **Source Tracking**
  - Track which application each clip originated from
  - Display source app metadata with each clip

- **Permission Integration**
  - Integrated with centralized PermissionsManager system
  - Clipboard operations include permission checks for consistency
  - Toast notifications via SnackbarHost for user feedback

- **Detail Pane Integration**
  - `ClipFeatureDetail` provides settings panel
  - History size configuration display
  - Pinned clips information
  - Integrated with three-pane layout

## Architecture

```
clip/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/squidink/alloy/modules/clip/
│   │       ├── di/
│   │       │   └── ClipModule.kt          # Hilt DI module
│   │       ├── data/
│   │       │   └── ClipRepositoryImpl.kt  # Repository implementation
│   │       ├── db/
│   │       │   ├── ClipDatabase.kt        # Room database
│   │       │   ├── ClipDao.kt             # Data access object
│   │       │   └── ClipEntity.kt          # Room entity
│   │       ├── ClipFeatureDetail.kt       # Detail pane settings
│   │       ├── ClipViewModel.kt           # MVI ViewModel
│   │       ├── ClipTransformations.kt     # Text transformation utilities
│   │       └── ui/
│   │           └── ClipScreen.kt          # Jetpack Compose UI
│   └── test/
│       └── java/com/squidink/alloy/modules/clip/
│           └── ClipViewModelTest.kt
└── README.md
```

### Layer Breakdown

#### Domain Layer (via core:domain)
- `IClipRepository`: Interface defining clip operations
- `Clip`: Domain model for clipboard entries

#### Data Layer
- `ClipRepositoryImpl`: Repository implementation handling:
  - Database operations via Room DAO
  - Thread dispatching using coroutines
  - Mapping between domain models and Room entities

- `ClipDatabase`: Room database with encrypted storage support
- `ClipDao`: Async data access with Flow support

#### Presentation Layer
- `ClipViewModel`: MVI pattern implementation with:
  - `ClipUiState`: Reactive UI state
  - `ClipUiAction`: User interactions
  - `ClipUiEffect`: One-time effects (toasts, clipboard ops)
  - `PermissionsManager` integration for permission checks
  - `copyToClipboardWithPermission()` for safe clipboard operations

- `ClipScreen`: Composable UI with:
  - Dual-pane layout (clips list + transformations)
  - Real-time search and filtering
  - Pin/unpin functionality
  - SnackbarHost for toast notifications
  - Edit dialog for clip content modification

## Dependencies

```kotlin
implementation(project(":core:common"))
implementation(project(":core:design"))
implementation(project(":core:datastore"))
implementation(project(":core:domain"))
implementation(project(":core:layout"))
implementation(project(":core:permissions"))

// Room for persistence
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)

// Hilt for DI
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)

// Navigation Compose for hiltViewModel
implementation(libs.androidx.hilt.navigation.compose)
```

## Usage

### Accessing the Clip Screen

```kotlin
// Navigate to clip workbench
navController.navigate("clip")
```

### Using the ViewModel

```kotlin
@Composable
fun ClipWorkbench() {
    val viewModel: ClipViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    ClipScreen(viewModel = viewModel)
}
```

### Applying Transformations

```kotlin
// Apply transformation to selected clip
viewModel.onAction(ClipUiAction.ApplyTransformation(TransformationType.JSON_PRETTY))
```

### Detail Pane Settings

The `ClipFeatureDetail` class implements `FeatureDetail` to provide
settings content in the three-pane layout's detail pane:

```kotlin
class ClipFeatureDetail : FeatureDetail {
    override val showsDetailPane: Boolean = true
    
    @Composable
    override fun DetailContent() {
        // Display clipboard settings info
    }
}
```

When the Clip feature is selected, the detail pane automatically shows
the `ClipFeatureDetail` content.

## Testing

```bash
# Run unit tests
./gradlew :modules:clip:test

# Run with coverage
./gradlew :modules:clip:testDebugUnitTest
```

## Future Enhancements

See [FEATURE_ENHANCEMENTS.md](../../FEATURE_ENHANCEMENTS.md#clip-module-clipboard-workbench) for the complete roadmap.

### High Priority
- [ ] **Tags & folders** for clip organization
- [ ] **Biometric safe box** for sensitive clips
- [ ] **Full-text search** across history
- [x] **Edit clips** before pasting (dialog-based editor added)

### Medium Priority
- [ ] **Combine multiple clips** before pasting
- [ ] **QR code generation** from URL clips
- [ ] **Text expansion snippets** for reusable content
- [ ] **Custom cleanup rules** for auto-deletion
- [ ] **Image clipboard support**

### Nice-to-Have
- [ ] Dynamic values (date/time/random)
- [ ] Web interface for access
- [ ] Custom scripting commands
