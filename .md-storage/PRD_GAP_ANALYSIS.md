# PRD Gap Analysis: Phase 1 Modules

## Date: 2026-09-24

---

## Executive Summary

**Overall Completion:** ~60% of Phase 1 features implemented

| Module | Completion | Critical Gaps |
|--------|------------|---------------|
| StatsPill | ~50% | Missing battery/thermal/alerts, widget not live |
| Scenes | ~60% | Missing CRUD UI, JSON import/export |
| Clip | ~55% | Missing pinning, image support, transformations |
| Scratch | ~50% | Missing checklist, stopwatch, widget |

---

## 7.1 Scenes (Workspace Manager) - 60% Complete

### ✅ Implemented
- Scene data model with PlacementHint enum
- SceneLauncher with `setLaunchBounds()` and `FLAG_ACTIVITY_LAUNCH_ADJACENT`
- Basic UI listing scenes with "Fire" button
- Default "Coding Stack" scene
- 6 placement hints (LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, CENTER, FULLSCREEN)

### ❌ Missing (Priority)

#### P1 - Critical
1. **Scene CRUD UI** - No way to create/edit/delete scenes from UI
   - File: `../modules/scenes/src/main/java/com/squidink/alloy/modules/scenes/ui/ScenesScreen.kt`
   - Need: Dialog/Screen for adding scenes with step editor
   - PRD: §7.1 "Create a 3-app scene"

2. **JSON Export/Import** - No file picker integration
   - File: N/A (feature not started)
   - Need: Export scenes to JSON, import from file picker
   - PRD: §7.1 "Export/import scenes as JSON (file picker in/out)"

3. **Persistence** - Scenes not persisted (only in ViewModel memory)
   - File: `../modules/scenes/src/main/java/com/squidink/alloy/modules/scenes/ScenesViewModel.kt`
   - Need: Room database or DataStore for scene storage
   - PRD: §7.1 "repeatable 10× without stacking artifacts"

#### P2 - Important
4. **Scene limit enforcement** - No "up to 10 scenes" validation
   - PRD: §7.1 "Up to 10 scenes"

5. **Deep-link support** - No intent URI handling
   - PRD: §7.1 "Deep-link scenes (e.g., open a specific repo in Git client)"

6. **Keyboard shortcuts** - Ctrl+Alt+1…9 (requires ImKeys)
   - PRD: §7.1 "keyboard: Ctrl+Alt+1…9 to fire"

---

## 7.3 Stats (System Telemetry) - 50% Complete

### ✅ Implemented
- `/proc/stat` CPU usage calculation
- `/proc/meminfo` RAM usage
- StatsPillOverlayService with SYSTEM_ALERT_WINDOW
- Basic widget (StatsGlanceWidget)
- Live overlay toggle in UI

### ❌ Missing (Priority)

#### P1 - Critical
1. **Battery telemetry** - No BatteryManager integration
   - File: `../modules/statspill/src/main/java/com/squidink/alloy/modules/statspill/StatsViewModel.kt`
   - Need: Register BatteryReceiver, expose level/health/charge state/temp
   - PRD: §7.3 "BatteryManager (level, health, charge state, temperature)"

2. **Thermal telemetry** - No ThermalManager integration
   - File: `../modules/statspill/src/main/java/com/squidink/alloy/modules/statspill/StatsViewModel.kt`
   - Need: Use android.os.ThermalManager (API 30+)
   - PRD: §7.3 "ThermalManager"

3. **Widget live data** - Widget shows static text, not live telemetry
   - File: `../modules/statspill/src/main/java/com/squidink/alloy/modules/statspill/StatsGlanceWidget.kt`
   - Need: Update widget with CPU/RAM/battery every 30s
   - PRD: §7.3 "Desktop widget 4×1 or 2×2 — CPU/RAM/battery/therm/net with sparklines"

4. **Alerts system** - No thermal/RAM/charge alerts
   - File: N/A (feature not started)
   - Need: Notification when thermal ≥ THRESHOLD, RAM > 90%, charge low
   - PRD: §7.3 "Alerts (opt-in): thermal ≥ THRESHOLD, RAM > 90%, charge low"

#### P2 - Important
5. **Network telemetry** - No TrafficStats integration
   - PRD: §7.3 "TrafficStats (rx/tx totals + delta)"

6. **System report share card** - No device/build/temps summary
   - PRD: §7.3 "System report share card (screenshot-able summary)"

7. **Sparklines in widget** - No historical data visualization
   - PRD: §7.3 "with sparklines"

---

## 7.4 Clip (Clipboard Power Tool) - 55% Complete

### ✅ Implemented
- ClipEntity with textContent, sourceApp, timestamp
- ClipDao with CRUD operations
- ClipViewModel with search, selection, transformations
- ClipScreen with 2-pane layout
- 3 transformations: UPPERCASE, LOWERCASE, TRIM
- Room database (ClipDatabase)

### ❌ Missing (Priority)

#### P1 - Critical
1. **Pinning/Favorites** - No pinning mechanism
   - File: `../modules/clip/src/main/java/com/squidink/alloy/modules/clip/db/ClipEntity.kt`
   - Need: Add `isPinned` field (already exists in schema but not in UI/actions)
   - PRD: §7.4 "Pinning, favorites"

2. **More transformations** - Only 3 of 8 transformations
   - File: `../modules/clip/src/main/java/com/squidink/alloy/modules/clip/ClipTransformations.kt`
   - Missing: sort lines, dedupe, regex find/replace, JSON pretty, base64, URL-encode
   - PRD: §7.4 "Transformations pipeline: case, trim, sort lines, dedupe, regex find/replace, JSON pretty, base64, URL-encode"

3. **500-item/30-day cap** - No cleanup policy
   - File: `../modules/clip/src/main/java/com/squidink/alloy/modules/clip/ClipViewModel.kt`
   - Need: Auto-delete old/unpinned clips
   - PRD: §7.4 "30-day or 500-item cap, user-settable"

4. **Image support** - No image clipboard handling
   - File: `../modules/clip/src/main/java/com/squidink/alloy/modules/clip/db/ClipEntity.kt`
   - Need: Add bitmap/mimeType support, ML Kit OCR
   - PRD: §7.4 "images (bitmap, ≤ 16 MP)", "Image OCR"

#### P2 - Important
5. **Biometric lock** - No history view lock
   - PRD: §7.4 "optional biometric lock for the history view"

6. **Per-app filtering** - No "show only Chrome entries"
   - PRD: §7.4 "Per-app clipboards: tag each entry with the active app"

7. **Paste targets** - No "Send to Scratch/Git/Model"
   - PRD: §7.4 "Paste targets: Open in, Send to Scratch, Send to Git, Send to Model Manager"

---

## 7.5 Scratch (Pinned Scratchpad) - 50% Complete

### ✅ Implemented
- ScratchEntity with content, updatedAt
- ScratchDao with CRUD
- ScratchViewModel with content editing, timer
- ScratchScreen UI
- ScratchActivity with multi-instance support
- Timer functionality (start/stop/reset)

### ❌ Missing (Priority)

#### P1 - Critical
1. **Checklist pane** - No checklist UI/logic
   - File: `../modules/scratch/src/main/java/com/squidink/alloy/modules/scratch/ui/ScratchScreen.kt`
   - Need: Add checklist data model, UI, toggle items
   - PRD: §7.5 "Panes (user-configurable): plain text, checklist, stopwatch/timer"

2. **Stopwatch** - Timer exists but not stopwatch (count-up with lap)
   - File: `../modules/scratch/src/main/java/com/squidink/alloy/modules/scratch/ScratchViewModel.kt`
   - Need: Add lap functionality, pause/resume
   - PRD: §7.5 "stopwatch/timer with optional notification on completion"

3. **Keyboard shortcuts** - Ctrl+Enter, Ctrl+T
   - File: `../modules/scratch/src/main/java/com/squidink/alloy/modules/scratch/ui/ScratchScreen.kt`
   - Need: FocusManager, onKeyEvent handlers
   - PRD: §7.5 "Keyboard: Ctrl+Enter = new checklist item; Ctrl+T = toggle timer"

4. **Auto-save debounce** - No 500ms idle debounce
   - File: `../modules/scratch/src/main/java/com/squidink/alloy/modules/scratch/ScratchViewModel.kt`
   - Need: Debounce content changes before DB write
   - PRD: §7.5 "text autosave < 500 ms after idle"

#### P2 - Important
5. **Widget variant** - No 2×4 AppWidget
   - PRD: §7.5 "AppWidget variant (2×4) for widget-only users"

6. **Export to .md** - No file export
   - PRD: §7.5 "plain .md export"

7. **Clock/timezone** - No clock pane
   - PRD: §7.5 "clock/timezone mini"

8. **Window chrome** - No opacity slider, borderless toggle
   - PRD: §7.5 "Window chrome: borderless toggle, opacity slider"

---

## Cross-Cutting Gaps (All Modules)

### P1 - Critical
1. **Module enable/disable persistence** - ModuleRegistryImpl uses in-memory StateFlow
   - File: `../app/src/main/java/com/squidink/alloy/registry/ModuleRegistryImpl.kt`
   - Need: Read initial state from DataStore on construction
   - PRD: §6 "toggling a module off stops all its services within 5 s"

2. **Keyboard shortcut map** - No global shortcut registry
   - File: `../core/common/src/main/java/com/squidink/alloy/core/common/ShortcutManager.kt`
   - Need: Implement conflict checking, persistence
   - PRD: §9 "all rebindable", "Conflict policy: on first launch, scan for OS-level conflicts"

3. **Desktop hover states** - Partial (only on some elements)
   - Files: All UI screens
   - Need: Ensure hover on all interactive elements
   - PRD: §4.3 "Hover states (tooltips, flyouts)"

4. **Right-click context menus** - Not implemented
   - Files: All UI screens
   - Need: ContextMenu for lists, canvases
   - PRD: §4.3 "context menus (right-click) on all lists/canvases"

---

## Recommended Implementation Order (No Permissions Required)

### Sprint 1 (Today)
1. ✅ Add battery/thermal to StatsViewModel
2. ✅ Add pinning to Clip (UI + actions)
3. ✅ Add 5 more transformations to Clip
4. ✅ Add checklist pane to Scratch
5. ✅ Fix ModuleRegistryImpl to read from DataStore

### Sprint 2 (Next)
1. Add scene CRUD UI + Room persistence
2. Add JSON export/import for scenes
3. Add stopwatch to Scratch
4. Add widget live data updates

### Sprint 3 (Later)
1. Add image support to Clip
2. Add alerts to StatsPill
3. Add keyboard shortcuts throughout
4. Add right-click context menus

---

## Files Requiring Changes

### High Priority (Sprint 1)
- `modules/statspill/src/main/java/.../StatsViewModel.kt`
- `modules/clip/src/main/java/.../ClipViewModel.kt`
- `modules/clip/src/main/java/.../ClipTransformations.kt`
- `modules/scratch/src/main/java/.../ScratchViewModel.kt`
- `modules/scratch/src/main/java/.../ui/ScratchScreen.kt`
- `app/src/main/java/.../registry/ModuleRegistryImpl.kt`

### Medium Priority (Sprint 2)
- `modules/scenes/src/main/java/.../ScenesViewModel.kt`
- `modules/scenes/src/main/java/.../ui/ScenesScreen.kt`
- `modules/scenes/src/main/java/.../db/` (new)
- `modules/statspill/src/main/java/.../StatsGlanceWidget.kt`

---

## Notes

- **No permissions needed** for Sprint 1 items (all existing permissions cover these)
- **Image support** requires `READ_MEDIA_IMAGES` (already in manifest)
- **Keyboard shortcuts** work within app (no special permissions)
- **JSON import/export** uses system file picker (SAF, no special permissions)

---

*Analysis generated from PRD v0.1 and current codebase state*
