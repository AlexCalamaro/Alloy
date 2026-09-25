# Alloy — Power Tools for Googlebook

> **PowerToys for Googlebook** — a modular utility suite giving Googlebook power users and developers desktop tools that have no native equivalent on Android.

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Platform](https://img.shields.io/badge/platform-Android%2017-blue)
![License](https://img.shields.io/badge/license-MIT-green)

## 🎯 Overview

Alloy is a single installable utility suite structured like Microsoft PowerToys, providing 11 toggleable modules:

| Phase | Module | Status | Description |
|-------|--------|--------|-------------|
| **1** | StatsPill | ✅ | Live system telemetry widget & overlay |
| **1** | Scenes | ✅ | Workspace scene launcher with window bounds |
| **1** | Clip | ✅ | Searchable encrypted clipboard history |
| **1** | Scratch | ✅ | Multi-instance pinned scratchpad |
| **2** | Downloads | 🚧 | Chunked download manager with resume |
| **2** | Treemap | 🚧 | Disk usage visualization (WinDirStat style) |
| **2** | ImKeys | 🚧 | Macro & hotkey IME companion |
| **3** | DeskTerm | 🔜 | Terminal / SSH / SFTP (DFM) |
| **3** | GitDesk | 🔜 | Keyboard-first Git client (DFM) |
| **4** | Phone Studio | 🔜 | Phone storage cleanup & dedupe |
| **4** | Backup | 🔜 | Time Machine-style local backups |
| **4** | Model Manager | 🔜 | Local LLM host (DFM) |

## 🏗️ Architecture

```
alloy/
├── app/                          # Main shell: nav, dashboard, settings
├── core/
│   ├── common/                   # DI, events, registry, logging
│   ├── design/                   # Material 3 theme, desktop UX primitives
│   ├── datastore/                # Room + DataStore + SQLCipher
│   ├── proc/                     # /proc system telemetry readers
│   └── netlocal/                 # Ktor loopback server (Model Manager)
├── modules/                      # Phase 1 modules (included in base APK)
│   ├── statspill/                # System telemetry
│   ├── scenes/                   # Workspace launcher
│   ├── clip/                     # Clipboard workbench
│   └── scratch/                  # Pinned scratchpad
├── features-dfm/                 # Dynamic Feature Modules (on-demand install)
│   ├── deskterm/                 # SSH/SFTP terminal
│   ├── gitdesk/                  # Git client
│   └── modelmgr/                 # Local LLM host
└── tooling/
    └── probe/                    # Platform validation utilities
```

### Tech Stack

- **Language:** Kotlin 2.0.21
- **UI:** Jetpack Compose + Material 3 (dynamic color)
- **DI:** Hilt
- **Architecture:** Unidirectional Data Flow (StateFlow + Compose)
- **Persistence:** Room + SQLCipher (encrypted) + DataStore
- **Async:** Coroutines + Flow + WorkManager
- **Build:** Gradle Kotlin DSL + Version Catalog

## 🚀 Getting Started

### Prerequisites

- **Java:** JDK 17+
- **Android Studio:** Canary build (for desktop emulator support)
- **Android SDK:** API 37 (Android 17)
- **Gradle:** 9.7.1+

### Setup

```bash
# Clone the repository
git clone https://github.com/your-org/alloy.git
cd alloy

# Install Android SDK components (if needed)
JAVA_HOME=/usr/lib/jvm/java-17-openjdk \
  /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager \
  --sdk_root=/opt/android-sdk \
  "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"

# Build the project
./gradlew clean assembleDebug testDebugUnitTest

# Install on device/emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Running Tests

```bash
# All unit tests
./gradlew testDebugUnitTest

# Specific module tests
./gradlew :modules:clip:testDebugUnitTest
./gradlew :modules:scenes:testDebugUnitTest
```

## 📱 Desktop Quality Features

Alloy targets the "Optimized for Desktop" Play Store badge with:

- ✅ **Hover Parity:** Tooltips and hover states on all interactive elements
- ✅ **Keyboard Parity:** Full keyboard navigation (Ctrl+K search, module shortcuts)
- ✅ **Multi-Window:** Scratch and future modules support multiple instances
- ✅ **Drag & Drop:** Drop folders into Treemap, URLs into Downloads
- ✅ **Custom Cursors:** I-beam for text, crosshair for treemap, etc.
- ✅ **Desktop Scrollbars:** Visible during mouse/trackpad scroll
- ✅ **Context Menus:** Right-click menus on all lists and canvases

## 🔐 Privacy & Security

- **Local-First:** No account, no cloud, no telemetry by default
- **Encrypted Storage:** SQLCipher for sensitive data (clipboard, SSH keys)
- **Keystore Integration:** Android Keystore for key management
- **Loopback-Only APIs:** Model Manager binds to 127.0.0.1 only
- **Transparent Permissions:** All permissions requested lazily with justification

## 📋 Module Quick Reference

### StatsPill
- **Widget:** 4×1 or 2×2 with CPU/RAM/battery/net sparklines
- **Live Mode:** Floating overlay (requires `SYSTEM_ALERT_WINDOW`)
- **Alerts:** Thermal, RAM, charge notifications (opt-in)
- 📖 [Full Documentation](modules/statspill/README.md)

### Scenes
- **Hotkeys:** `Ctrl+Alt+1…9` to launch scenes
- **Placement:** Uses `setLaunchBounds()` for window positioning
- **Import/Export:** Scene configurations as JSON
- 📖 [Full Documentation](modules/scenes/README.md)

### Clip
- **History:** Text (≤1MB) + images (≤16MP), 30-day/500-item cap
- **OCR:** Image text extraction via ML Kit
- **Transformations:** Case, trim, sort, dedupe, regex, JSON, base64
- 📖 [Full Documentation](modules/clip/README.md)

### Scratch
- **Multi-Instance:** Multiple independent windows
- **Panes:** Text notes, checklists, stopwatch/timer
- **Keyboard:** `Ctrl+Enter` new item, `Ctrl+T` toggle timer
- 📖 [Full Documentation](modules/scratch/README.md)

### Settings
- **Framework:** MVI-based settings management (placeholder)
- **Planned:** Theme, preferences, system configuration
- 📖 [Full Documentation](modules/settings/README.md)

## 🛠️ Development

### Adding a New Module

1. Create module directory: `modules/<name>/`
2. Create `build.gradle.kts`:
   ```kotlin
   plugins {
       alias(libs.plugins.android.library)
       alias(libs.plugins.kotlin.compose)
       alias(libs.plugins.hilt)
       alias(libs.plugins.ksp)
   }
   
   android {
       namespace = "com.squidink.alloy.modules.<name>"
       compileSdk = 37
       // ...
   }
   
   dependencies {
       implementation(project(":core:common"))
       implementation(project(":core:design"))
       // ...
   }
   ```
3. Add to `settings.gradle.kts`: `include(":modules:<name>")`
4. Add to app's `build.gradle.kts`: `implementation(project(":modules:<name>"))`
5. Create `AndroidManifest.xml` in `src/main/`
6. Implement `ViewModel`, `UI State`, `UI Screen`, and `Database` (if needed)

### Code Style

- **Naming:** PascalCase for classes, camelCase for functions/variables
- **Packages:** `com.squidink.alloy.<module>.<feature>`
- **ViewModels:** Use `UiState`/`UiAction`/`UiEffect` pattern
- **Comments:** KDoc for public APIs, inline for complex logic

## 📊 Roadmap

### Phase 1 (Current) — MVP
- [x] Dashboard shell & module registry
- [x] StatsPill widget + overlay
- [x] Scenes with window bounds
- [x] Clip with transformations
- [x] Scratch with multi-instance
- [ ] "Optimized for Desktop" self-assessment ≥90%

### Phase 2 — Power Core
- [ ] Downloads manager with chunked transfer
- [ ] Treemap disk visualization
- [ ] ImKeys IME with text expansion

### Phase 3 — Desk Tools (DFMs)
- [ ] DeskTerm (SSH/SFTP)
- [ ] GitDesk with hunk staging

### Phase 4 — Data + AI (DFMs)
- [ ] Phone Studio cleanup
- [ ] Backup to external drive
- [ ] Model Manager local LLM

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests: `./gradlew testDebugUnitTest`
5. **Update documentation:** If you modified a module's source code, update its `README.md` in the module directory
6. Commit with clear messages
7. Push and open a Pull Request

### Module Documentation Requirements

When making meaningful changes to a module, update its README to include:
- **New capabilities or features** added
- **Architecture changes** (new layers, components, or patterns)
- **API or interface modifications** (public methods, data models)
- **Usage examples** for new functionality

A pre-commit hook validates that module READMEs are updated when source files change. To bypass (for documentation-only commits or emergency fixes):
```bash
git commit --no-verify
```

## 📄 License

MIT License - see [LICENSE](LICENSE) for details

## 🙏 Acknowledgments

- **Inspired by:** Microsoft PowerToys
- **Platform:** Googlebook OS (Android 17-based)
- **Built with:** Jetpack Compose, Hilt, Room, Ktor

---

*Alloy is a codename. Final name TBD by v0.9.*

**Tagline:** *Power tools for your Googlebook.*
