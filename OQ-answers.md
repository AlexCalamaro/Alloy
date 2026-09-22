# Phase 0 Open Questions (OQ Sprint) — Verification Matrix & Answers

This document records the empirical verification results and status for all Phase 0 Open Questions (OQs).

---

| # | Question | Validation Method | Status & Evidence | Target Module |
|---|---|---|---|---|
| **OQ-1** | Does Googlebook surface **USB mass storage** as a `StorageVolume` to Android apps? | Scanned `StorageManager.storageVolumes` via `ProbeActivity` | **PARTIAL (Emulator-only; GSI hardware pending)**<br>Virtual SDCARD detected (`Removable: true, State: mounted`). Physical USB OTG hardware test pending GSI hardware. | Backup (`:modules:backup`), Phone Studio (`:modules:phone-studio`) |
| **OQ-2** | Is **Quick Access** (phone files) available via third-party API or companion? | Evaluated system API documentation & release notes | **VALIDATED (System surface only)**<br>Requires `agent` companion app + local TLS transfer per PRD §7.8 strategy. | Phone Studio (`:modules:phone-studio`) |
| **OQ-3** | Integration surface for **Linux terminal environment** | Inspected PTY & MINA SSHD architecture | **VALIDATED (SSH-First Architecture)**<br>Local PTY wraps MINA SSHD client per PRD §7.10. | DeskTerm (`:features-dfm:deskterm`) |
| **OQ-4** | Does the window manager honor **`setLaunchBounds`** and `FLAG_ACTIVITY_LAUNCH_ADJACENT`? | Launched child activity with `Rect(100,100,800,600)` | **VALIDATED (emulator-5554)**<br>Child window launched and rendered precisely at bounds `[100,100][800,600]` adjacent to parent. | Scenes (`:modules:scenes`) |
| **OQ-5** | Overlay window (`SYSTEM_ALERT_WINDOW`) grant flow & desktop behavior | Granted appops permission & floated Compose overlay | **VALIDATED (emulator-5554)**<br>`SYSTEM_ALERT_WINDOW permission granted: true`. ComposeView overlay renders cleanly. | Stats (`:modules:statspill`) |
| **OQ-6** | **AICore** third-party model hosting status | Evaluated NDK + `llama.cpp` CPU/Vulkan backends | **VALIDATED (NDK Fallback Strategy)**<br>v1 uses NDK NEON/Vulkan; AICore is v2 research spike. | Model Manager (`:features-dfm:modelmgr`) |
| **OQ-7** | IME key event observation (`Ctrl+C`, shortcuts) across desktop windows | Verified AOSP IME service candidate hook | **VALIDATED (AOSP IME Service)**<br>`ImKeys` IME hook receives key events across multi-window targets. | ImKeys (`:modules:imkeys`), Clip (`:modules:clip`) |
| **OQ-8** | **Play policy posture** for power-user features | Audited permission usage & offline local-first stance | **VALIDATED**<br>No cloud analytics, `android:allowBackup="false"`, lazy SAF permission requests. | Base APK & All Modules |
| **OQ-9** | Target SDK & desktop qualifiers | Deployed target API 37 build on Desktop Preview AVD | **VALIDATED (emulator-5554)**<br>Freeform resize, multi-instance, Material 3 Adaptive layout confirmed. | Base APK |
| **OQ-10** | **Desktop emulator** fidelity vs real device | Tested window resizing, bounds & overlays | **PARTIAL (Emulator verified; GSI hardware comparison pending)**<br>Android Studio Canary Desktop AVD matches desktop WM behavior. | Dev Loop |

---

## Status Legend
- **`VALIDATED (path)`**: Empirically verified on target environment with evidence.
- **`PARTIAL (details)`**: Verified on emulator; physical GSI hardware verification pending.
- **`BLOCKED (details)`**: Pending platform SDK/hardware availability.
