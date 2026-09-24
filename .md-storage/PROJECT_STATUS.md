# Alloy Project Status Report

## Build Status: ✅ HEALTHY

**Last Updated:** 2026-09-24  
**Build:** `./gradlew assembleDebug testDebugUnitTest` - **SUCCESS**  
**APK Size:** ~83MB (debug build)

---

## Completed Work (Phase 1 MVP)

### ✅ Core Infrastructure
- [x] Gradle build system configured (AGP 9.4.1, Kotlin 2.0.21, KSP 2.0.21-1.0.28)
- [x] Android SDK 37.0 installed and configured
- [x] Hilt dependency injection across all modules
- [x] Room database with encrypted SQLCipher support
- [x] DataStore for preferences and module state management
- [x] Core design system with Material 3 and dynamic color
- [x] Desktop UX primitives (hover states, custom scrollbars)

### ✅ Module Implementation Status

| Module | Status | Features Implemented |
|--------|--------|---------------------|
| **StatsPill** | ✅ Complete | Widget, Overlay Service, /proc telemetry, CPU/RAM monitoring |
| **Scenes** | ✅ Complete | Scene launcher, launch bounds geometry, placement hints |
| **Clip** | ✅ Complete | Clipboard history, search, transformations, Room storage |
| **Scratch** | ✅ Complete | Multi-instance notes, timer, Room storage |

### ✅ Testing
- Unit tests passing for all modules
- ViewModel tests with Turbine for Flow testing
- Fake DAOs for isolated testing

---

## Known Issues & Technical Debt

### ⚠️ Build Configuration
1. **Kotlin/AGP Warnings:** Experimental `android.disallowKotlinSourceSets=false` flag required for KSP compatibility
2. **SDK Version:** Using Android 37.0 (canary) - may need adjustment for stable release

### ⚠️ Missing Features (Per PRD)
1. **ImKeys IME** - Not implemented (Phase 1 scope)
2. **Downloads Manager** - Not implemented (Phase 2)
3. **Treemap** - Not implemented (Phase 2)
4. **Phone Studio** - Not implemented (Phase 4)
5. **Backup** - Not implemented (Phase 4)
6. **DeskTerm (SSH/SFTP)** - Not implemented (Phase 3 DFM)
7. **GitDesk** - Not implemented (Phase 3 DFM)
8. **Model Manager** - Not implemented (Phase 4 DFM)

### ⚠️ Permissions Not Yet Requested Lazily
- `SYSTEM_ALERT_WINDOW` - Stats overlay requires opt-in
- `POST_NOTIFICATIONS` - For alerts
- `MANAGE_EXTERNAL_STORAGE` - For Treemap/Backup (Phase 2/4)

---

## Architecture Review

### Module Structure
```
alloy/
├── app/                          # Main shell, navigation, settings
├── core/
│   ├── common/                   # DI, events, registry, logging
│   ├── design/                   # Material 3 theme, desktop UX
│   ├── datastore/                # Room + DataStore + encryption
│   ├── proc/                     # /proc system telemetry
│   └── netlocal/                 # Ktor loopback server (for Model Manager)
├── modules/
│   ├── statspill/                # ✅ System telemetry
│   ├── scenes/                   # ✅ Workspace launcher
│   ├── clip/                     # ✅ Clipboard workbench
│   └── scratch/                  # ✅ Pinned scratchpad
└── tooling/
    └── probe/                    # Platform validation apps
```

### Design Patterns
- **Unidirectional Data Flow:** All ViewModels use `UiState`/`UiAction`/`UiEffect` pattern
- **Dependency Injection:** Hilt `@Singleton` and `@ViewModelScoped`
- **Reactive State:** Kotlin `StateFlow` with `collectAsState` in Compose
- **Modular Architecture:** Each module is a self-contained library module

---

## Next Steps (Recommended Priority)

### P0 - Stabilization
1. ✅ Fix KSP compatibility (DONE - Room 2.7.0)
2. ✅ Fix manifest merge conflicts (DONE)
3. ✅ Add proper permissions to manifest (DONE)
4. [ ] Add ProGuard/R8 rules for production builds
5. [ ] Configure crash reporting (local-only for v1)

### P1 - Phase 2 Features
1. [ ] Downloads Manager module
2. [ ] Treemap module
3. [ ] ImKeys IME companion (text expansion, macros)

### P2 - Desktop Quality
1. [ ] Add keyboard shortcuts throughout
2. [ ] Implement drag-and-drop support
3. [ ] Add multi-window support declarations
4. [ ] Desktop quality self-assessment checklist

---

## Build Commands

```bash
# Clean build with tests
./gradlew clean assembleDebug testDebugUnitTest

# Run specific module tests
./gradlew :modules:clip:testDebugUnitTest

# Build release (when ready)
./gradlew assembleRelease
```

---

## Environment Requirements

- **Java:** 17+ (JDK 17 tested)
- **Android SDK:** 37.0 (canary)
- **Android Studio:** Canary (for desktop emulator)
- **Gradle:** 9.7.1

---

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Platform API changes (Android 17) | Medium | Module isolation allows graceful degradation |
| IME trust friction | Medium | Transparency card + optional paths for features |
| Scope creep (11 modules) | High | Strict phase gates, DFM for heavy modules |
| KSP/Kotlin version compatibility | Low | Locked versions in `libs.versions.toml` |

---

## Success Metrics (Phase 1)

- [x] Build compiles without errors
- [x] All unit tests pass
- [x] 4 modules implemented (Stats, Scenes, Clip, Scratch)
- [ ] Dashboard navigation functional
- [ ] Module enable/disable works
- [ ] APK installs on Android 17 device/emulator

---

*Generated by automated project analysis*
