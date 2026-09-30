# Scratch & Code Editor Module

Lightweight tabbed code and text editor with syntax highlighting, task checklists, and secure biometric lockbox.

## Overview

The Scratch module provides a multi-document, tabbed text and code editing workspace. It features real-time syntax highlighting for 17+ languages powered by SnipMe Highlights, debounced auto-save per document, and a hardware-encrypted Secure Lockbox gated by biometric and device screen lock authentication.

## Capabilities

### Core Features

- **Multi-Document Tabbed Editor**
  - Horizontal scrollable document tab bar with active tab indicators
  - Floating Action Button (FAB) pattern for creating new documents
  - Document closing and inline renaming
  - Auto-save with 500ms debounce per document

- **Syntax Highlighting (SnipMe Highlights)**
  - Powered by `dev.snipme:highlights` pure-Kotlin engine
  - Non-intrusive Compose `VisualTransformation` with 1:1 cursor offset mapping
  - Dropdown selector supporting Kotlin, Java, Python, Rust, C++, C#, Go, JavaScript, TypeScript, Shell, Swift, PHP, Ruby, Dart, Markdown, and Plain Text
  - Dynamic light and dark syntax theme integration

- **Secure Document Lockbox**
  - Hardware-backed database encryption via SQLCipher and Android Keystore
  - Biometric (fingerprint, face) and device screen lock (PIN, pattern, password) authorization via AndroidX `BiometricPrompt`
  - Visual lock indicators: 🔒 Locked (redacted until authenticated) and 🔓 Unlocked (session active)
  - Clear user confirmation explaining encryption parameters before locking
  - Auto-lock protection upon application backgrounding (`onStop`) or app restart

- **Checklist Pane**
  - Add, edit, and delete checklist tasks
  - Strikethrough toggle and persistent task state

- **Timer Widget**
  - Integrated countdown/elapsed timer in header for productivity sprints

## Architecture

```
scratch/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/squidink/alloy/modules/scratch/
│   │       ├── auth/
│   │       │   └── BiometricPromptManager.kt      # AndroidX BiometricPrompt helper
│   │       ├── data/
│   │       │   └── ScratchRepositoryImpl.kt       # Repository implementation
│   │       ├── db/
│   │       │   ├── ScratchDatabase.kt             # Room database (v2)
│   │       │   ├── ScratchDao.kt                  # Room DAO
│   │       │   └── ScratchEntity.kt               # Entity with language & lockbox flags
│   │       ├── di/
│   │       │   └── ScratchModule.kt               # Hilt DI with SQLCipher factory
│   │       ├── model/
│   │       │   └── EditorLanguage.kt              # Type-safe language enum
│   │       ├── ScratchActivity.kt                 # Standalone ComponentActivity
│   │       ├── ScratchFeatureDetail.kt            # 3-pane settings panel
│   │       ├── ScratchViewModel.kt                # Multi-document MVI ViewModel
│   │       └── ui/
│   │           ├── ScratchScreen.kt               # Main Compose UI with tabs & FAB
│   │           └── syntax/
│   │               └── HighlightsVisualTransformation.kt # Compose syntax highlighter
│   └── test/
│       └── java/com/squidink/alloy/modules/scratch/
│           └── ScratchViewModelTest.kt            # Unit test suite
└── README.md
```

### Layer Breakdown

#### Domain Layer (`core:domain`)
- `IScratchRepository`: Interface for multi-document operations (`getScratchpads`, `insertScratchpad`, `updateScratchpad`, `deleteScratchpad`).
- `Scratch`: Domain entity modeling documents with `id`, `title`, `content`, `language`, `isLocked`, and timestamps.

#### Data Layer
- `ScratchRepositoryImpl`: Implements repository with background dispatching and mapping between domain models and Room entities.
- `ScratchDatabase`: Version 2 Room database protected by SQLCipher.
- `ScratchDao`: Reactive `Flow`-based DAO for document queries and metadata updates.

#### Presentation Layer

**ViewModel (MVI Pattern)**
```kotlin
data class ScratchUiState(
    val documents: List<Scratch> = emptyList(),
    val activeDocumentId: String? = null,
    val unlockedDocumentIds: Set<String> = emptySet(),
    val showLockConfirmationDialog: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val activePane: ScratchPane = ScratchPane.TEXT,
    val isTimerRunning: Boolean = false,
    val timerSeconds: Int = 0,
    val autoSaveDebounceMs: Long = 500,
)
```

**UI Components**
- `DocumentTabBar`: Tab strip with title, lock indicators, and close buttons
- `EditorToolbar`: Language format dropdown, lock toggle, rename button
- `CodeEditorPane`: Code editor with monospaced font and `HighlightsVisualTransformation`
- `LockboxGatekeeperPane`: Redaction and authentication CTA for locked documents
- `ChecklistPane`: Task checklist manager

## Testing

```bash
# Run unit tests
./gradlew :modules:scratch:testDebugUnitTest

# Compile verification
./gradlew :modules:scratch:compileDebugKotlin
```
