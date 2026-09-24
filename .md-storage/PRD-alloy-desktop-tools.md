# PRD — Alloy (working name)
## "Power Tools for Googlebook" — a modular utility suite for the Android desktop

| | |
|---|---|
| **Status** | Draft v0.1 — for engineering kickoff |
| **Date** | 2026-09-21 |
| **Platform** | Googlebook OS (codename "Aluminium OS", Android 17-based), ARM64 + x86-64 |
| **Distribution** | Google Play (single app + Dynamic Feature Modules + one small phone companion) |
| **Working name** | Alloy (rationale: Aluminium-family nod; rename before launch is cheap, rename in Play is not — decide by v0.9) |

---

## 1. Executive summary

Alloy is a single installable utility suite — structured like Microsoft PowerToys — giving Googlebook power users and developers a set of independently toggleable desktop tools that have no native equivalent on Android: scene/workspace launching, system telemetry, clipboard power features, a pinned scratchpad, a real download manager, a disk-usage treemap, phone-storage cleanup, local backup to external drives, a terminal/SSH/SFTP workbench, a keyboard-first Git client, and a lightweight local LLM host.

**Why now (platform facts, verified as of 2026-09-21):**
- Googlebook OS is Android 17 rebuilt as a desktop OS: native Play Store apps, custom window manager, taskbar, virtual desktops, desktop surface that holds apps/widgets/files. Launch: pre-orders open 2026-09-21; first devices ship from ~2026-10-05; OS updates on a **quarterly QPR cadence**.
- Hardware floor: 12 GB RAM (16/32 GB typical), NPU with Gemini Nano v3, 16:10 displays (2.8K OLED flagships), fingerprint key, dedicated Google key, Quick Insert key (Caps Lock position — system-owned, do not claim).
- Developer tooling exists today: **Android Studio Canary ships a desktop emulator** explicitly for validating window resizing, multi-instance, and keyboard/mouse behavior; the desktop emulator is our primary dev target.
- Google publishes **desktop app quality tiers** ("Tier 1 — Adaptive Differentiated: Desktop") with testable guidelines (hover parity, keyboard parity, multi-window, multi-instance, drag & drop, file picker/handlers, custom cursors, cross-device handoff, offline support). Play Store now displays **"Made for Desktop" / "Optimized for Desktop" / "Better on the Website"** tags. Earning "Optimized for Desktop" is a distribution goal, not a vanity goal.
- Gap: first-party Google apps are choosing web wrappers (TWAs) for several flagship apps; the "desktop-class native utility" layer is effectively unowned by anyone, including Google.

**One-sentence pitch:** *PowerToys for Googlebook — one app, eleven toggles, zero cloud required.*

---

## 2. Goals / Non-goals

### Goals
1. Be the default "second install" for Googlebook developers and power users by the first QPR update (~Q1 2027).
2. Earn the "Optimized for Desktop" Play tag for the base app and each DFM.
3. Be fully local-first: no account, no cloud, no telemetry by default. Privacy is the brand.
4. Ship a public beta by launch day (2026-10-05) with at least 4 of the 11 modules (Phase 1 set, §7).
5. Establish the brand as *the* power-tool suite so each new module lands with an audience.

### Non-goals (v1.x)
- Not a window manager / tiling engine (no platform API to move other apps' windows; scenes approximate this via launch bounds — §7.1).
- Not a system shell replacement: we do not touch taskbar, virtual desktops, Quick Settings, or the launcher.
- No Windows software compatibility, no Android app mirroring features (Cast My Apps is first-party).
- No multi-model / model-server ambitions: the Model Manager hosts exactly one GGUF model at a time (§7.11).
- No i18n beyond en in v0.1; no enterprise/MDM packaging.
- No paid tier in v1.0 (monetization decision deferred to v1.1, see §13).

---

## 3. Target users & top jobs-to-be-done

| Persona | Jobs |
|---|---|
| **Android/dev professional** (primary) | "Open my coding stack in one hotkey." "SSH into my servers from the same machine I write on." "Run a local model for summarization without sending text to the cloud." "See what's eating my disk." |
| **Creator / hybrid worker** | "Capture, annotate, file things fast." "Back up my laptop to my external drive without thinking about it." "Clean up 80 GB of phone photos from my desk." |
| **Platform early adopter** | "Make this new laptop feel like a real desk." "Discover what's possible on this OS and tell people." (They are our reviewers and distribution channel.) |

North-star behavior: **≥3 modules enabled weekly per active user** (PowerToys' own success metric is multi-module adoption — single-module users churn when the module is copied).

---

## 4. Product architecture

### 4.1 The PowerToys pattern
- **One base APK** = app shell (navigation, settings, module registry, global shortcut host, design system) + 6 small modules.
- **Dynamic Feature Modules (DFM)**, on-demand download:
  - `deskterm` — Terminal / SSH / SFTP (heavy deps: SSH libs, ANSI renderer).
  - `gitdesk` — Git client (JGit).
  - `modelmgr` — local LLM (NDK + llama.cpp libs; tens of MB).
- **One small companion app** (`agent`, separate Play listing, same developer org): runs on the Android phone, enables Phone Studio (§7.8) over local network.
- Every module: independently enable/disable (disabled ⇒ no services, no widgets, zero background cost), own storage namespace, own settings page.
- Each module exposes up to three surfaces: a page in the main app, a desktop **widget**, and/or a standalone **window activity** (multi-window/multi-instance where it matters: Terminal, Git, Scratch, Treemap).

### 4.2 Tech stack
| Layer | Choice | Notes |
|---|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 (dynamic color) | Adaptive layouts; 16:10-first; hover + focus states everywhere (quality-tier requirement) |
| minSdk / targetSdk | 37 (Android 17) / 37 | Platform is Android 17-based; do not dilute for phones in v1 |
| DI / arch | Hilt, per-module `Module` objects, unidirectional state (rememberStateFlowStore or own reducer) | Keep modules decoupled via interfaces in `core/common` |
| Async | Coroutines + Flow; WorkManager for scheduled work (backup, scans) | |
| DB | Room (per module) + DataStore for settings | |
| Crypto | Android Keystore (AES-GCM for clip history, SSH keys, backup vault) | |
| Download engine | OkHttp (range requests, chunked) + system `DownloadManager` for browser-initiated imports | |
| Terminal | Custom ANSI/VT renderer (libterminal or in-house VT100+xterm parser) over MINA SSHD client | Local PTY path depends on open question OQ-3; v1 = SSH-first |
| Git | JGit + java-diff-utils (side-by-side diff UI) | |
| LLM runtime | llama.cpp (ggml) via NDK: NEON CPU + Vulkan GPU backends, GGUF Q4/Q5 | NPU (SNPE/Qualcomm AIMET, MediaTek NeuroPilot) = v2 research spike |
| On-device OCR (clip images, model fallbacks) | ML Kit (Text, Object) — already the platform's on-device path | |
| Local HTTP (model API) | Ktor embedded, bound to 127.0.0.1, token-gated | |
| Build | Gradle (Kotlin DSL) + version catalog; CI builds for `arm64-v8a` + `x86_64` | Both ABIs ship: silicon is Intel/Qualcomm/MediaTek |

### 4.3 Global UX conventions (all modules)
- **Keyboard-first:** every module defines a shortcut map; shortcuts are user-rebindable and conflict-checked at the system level (§9). `?` anywhere = shortcut help overlay.
- **Pointer parity:** hover states (tooltips, flyouts), context menus (right-click) on all lists/canvases, scrollbars shown during mouse/trackpad scroll, custom cursors where meaningful (I-beam in terminal/scratch, crosshair in treemap, pen in future annotation).
- **Multi-window:** Terminal, Git, Treemap, Scratch declare `android:multiInstance="true"` and support free resize; layout re-composes per width breakpoint (windowed, not just phone/tablet).
- **Drag & drop:** DropHelper-based; URLs drop into Downloads; files/folders drop into Treemap (scan this), Git (add to repo dir), Scratch (append path); text drops into Scratch/Clip.
- **File system citizenship:** use the system file picker for all import/export; declare file handlers where we win (.txt/.md/.log open-in for terminal & scratch? — decide in Phase 2; avoid fighting Files).
- **Offline:** every module degrades gracefully (it's all local anyway); network-dependent bits (model download, SFTP, SMB) show explicit connection state.
- **Empty states teach:** each module's first run has a 3-step onboarding card (power tools earn trust by being legible).

---

## 5. Permissions matrix (request lazily, per module, at first use)

| Permission | Modules | Trigger |
|---|---|---|
| None (foreground UI only) | Scenes (v1), Stats pill (widget mode), Scratch, Clip (with IME) | — |
| `POST_NOTIFICATIONS` | Stats (alerts), Downloads (completion), Backup (scheduled) | First relevant action |
| `MANAGE_EXTERNAL_STORAGE` ("All files access") | Treemap (full-disk scan), Downloads (watch folder), Backup (user dir source) | "Enable full-disk scan" button → settings intent; **never at first launch**; SAF tree pick is the default path (no special permission) |
| `SYSTEM_ALERT_WINDOW` | Stats pill "Live Mode" (floating overlay) | Opt-in in module settings |
| `FOREGROUND_SERVICE` (+ `dataSync` / `specialUse` types) | Downloads (active), Backup (run), Model Manager (inference warm) | At start of run |
| `READ_MEDIA_*` (or none via SAF) | Phone Studio, Backup (MediaStore), Treemap (media dirs) | At first use |
| IME user selection | ImKeys (macro/expand/clip-capture), future dictation | User sets Alloy IME as keyboard in module setup (deep link to IME picker) |
| `USE_BIOMETRIC` | Clip (unlock history), Model Manager (model load) | First use of locked feature |
| `USB`-related / `StorageManager` volume events | Backup (ext drive detect), future NAS | Device attach |
| `REQUEST_INSTALL_PACKAGES` | — | **Not needed; we ship only via Play.** |
| Notification access (`NotificationListenerService`) | Clip (capture from any app w/o IME), future phone-mirror | Opt-in; secondary path, IME is primary |

---

## 6. Module registry (the "PowerToys" dashboard)

Main window: left rail = modules with enable switch + status (idle/running/needs-permission); top = search; details pane per module. Also: a **desktop widget "Alloy Hub"** (2×2) = module quick-toggles + 3 most-used actions. This dashboard is the brand surface and the first thing reviewers screenshot.

Acceptance: cold open → dashboard < 1.5 s on 16 GB device; toggling a module off stops all its services within 5 s (verified via `dumpsys`).

---

## 7. Functional requirements by module

Each module: purpose → key features → UX notes → acceptance criteria → risks/dependencies.

### 7.1 Scenes (Workspace / Scene Manager)
**Purpose:** One hotkey (or one click) opens a named "stack" of apps in sensible window positions — coding stack, deep work, support, streaming.
**Features (v1):**
- Scene = ordered list of launch steps: {target app (component or deep link/intent), placement hint: left|right|top-left|top-right|center|fullscreen, optional flags (launch-adjacent, new task)}.
- Placement implemented via `ActivityOptions.setLaunchBounds(Rect)` (API 35+) + `FLAG_ACTIVITY_LAUNCH_ADJACENT` fallback; percentages of work area, not pixels.
- Up to 10 scenes; keyboard: `Ctrl+Alt+1…9` to fire (via ImKeys IME hook), or widget button / notification quick actions.
- Optional per-scene: Android Focus-mode preset, media (launch music app), "deselect others" (move other windows back — **not possible** without window API; v1 = launch-only, document honestly).
- Export/import scenes as JSON (file picker in/out).
**Acceptance:**
- Create a 3-app scene → Ctrl+Alt+1 → all three windows appear within expected regions in < 2 s; repeatable 10× without stacking artifacts.
- Deep-link scenes (e.g., open a specific repo in Git client, a specific SFTP host in Terminal) work.
- Scene JSON round-trips through file picker.
**Risks:** OQ-4 (launch bounds honored by Googlebook window manager); if not honored, degrade to launch-adjacent + user drags once (we persist last geometry per scene via our own window-state callback when we're the launched app — partial).

### 7.2 ImKeys (Macro & Hotkey IME companion)
**Purpose:** The one legitimate global input hook on AOSP: a purpose-built IME that provides text expansion, key macros, app launching, and clipboard capture.
**Features (v1):**
- Text expansions: trigger → snippet, per-app scoping, variables (`{{date}}`, `{{time}}`, `{{path}}`), regex find/replace in the field before insertion.
- Key macros: while Alloy IME is active in a text field: bind → {insert text | launch app | open URL | paste from clip | run scene}.
- Clip capture: on Ctrl+C (or long-press C), snapshot system clipboard into Clip history (primary, permission-light path).
- "Alloy bar" popup (triggered by key combo, e.g., Ctrl+Space): shows recent expansions, snippets, clip history, scene list — a quick-switcher. This is the visible product face of the IME.
- Deliberately **not** a full keyboard: no touch layout (laptop has a keyboard); renders a minimal candidate strip only.
**Honest limits (state in-app):** IME input is only visible to apps that route text through the IME; system-level remapping (media keys, volume, window moves) is out of scope on AOSP. v1 positions ImKeys as *expansion + launcher + clipboard*, not Karabiner.
**Acceptance:**
- 50 expansions defined → trigger inserts with < 50 ms perceived latency; per-app scope respected.
- Ctrl+C in Chrome, Terminal, and Docs (TWA) each captured into Clip within 1 s of the gesture.
- Ctrl+Space quick-switcher: 4 tabs (snippets/clip/scenes/apps), arrow-key navigable, Enter executes.
**Risks:** IME trust friction (users wary of keyboards) → setup wizard with a "what this IME can and cannot see" transparency card; keep the IME's own footprint tiny (< 10 MB, no ads, no analytics).

### 7.3 Stats (System telemetry pill)
**Purpose:** Live machine vitals on the desktop — the "menu-bar Stats" everyone puts on a new desk.
**Features:**
- Data sources: `/proc/stat` (per-core CPU), `/proc/meminfo`, `ThermalManager`, `BatteryManager` (level, health, charge state, temperature), `TrafficStats` (rx/tx totals + delta), `UsbManager`/`StorageManager` (volumes), display refresh (when available).
- Two surfaces: (a) **Desktop widget** 4×1 or 2×2 — CPU/RAM/battery/therm/net with sparklines; 30 s refresh (AppWidget limit); (b) **Live Mode** floating pill (overlay permission) — 1 Hz, draggable, click-through, hides when a fullscreen app is focused.
- Alerts (opt-in): thermal ≥ THRESHOLD, RAM > 90%, charge low, unknown USB device.
- "System report" share card (screenshot-able summary: device, build, temps, top-RAM apps via `UsageStats` if permitted) — for support threads and Reddit benchmarks (organic distribution).
**Acceptance:** CPU% within ±2 pt of `top` over 5 min; widget idle cost < 0.5 % CPU-min; Live Mode overlay < 1 % CPU at 1 Hz; no ANRs from /proc polling (poll on a single background dispatcher, batched).

### 7.4 Clip (Clipboard power tool)
**Purpose:** The clipboard as a searchable, syncable, transformable workbench.
**Features:**
- History: text (≤ 1 MB/item), images (bitmap, ≤ 16 MP), HTML/plain dual storage; 30-day or 500-item cap, user-settable; encrypted at rest (Keystore AES-GCM); optional biometric lock for the history view.
- Capture paths: ImKeys Ctrl+C hook (primary), notification/tile "Copy here" (secondary), NotificationListener (opt-in, tertiary).
- Per-app clipboards: tag each entry with the active app (from IME focus / UsageStats); "show only Chrome entries."
- Transformations pipeline: case, trim, sort lines, dedupe, regex find/replace, JSON pretty, base64, URL-encode — applied as quick actions on any entry.
- **Image OCR:** on-device (ML Kit) "Extract text" on image entries; results are pastable.
- Snippet library (distinct from ImKeys expansions: long-form blocks with templates).
- Pinning, favorites, search (fuzzy, local).
- Paste targets: "Open in…" (system), "Send to Scratch," "Send to Git commit message," "Send to Model Manager (summarize)."
**Non-goal v1:** cross-device sync (v2, with end-to-end encrypted relay; design storage schema for it now).
**Acceptance:** 1,000-entry history → search p95 < 100 ms; image OCR on 4K screenshot < 3 s (NPU-assisted where available, else CPU); app-tag accuracy ≥ 90 % in IME-focus path.

### 7.5 Scratch (Pinned scratchpad)
**Purpose:** A small always-nearby window: notes, a stopwatch, a checklist. The "sticky note + timer" of the desk.
**Features:**
- Multi-instance (each instance = independent scratch window); user can run 3–4 pinned small windows.
- Panes (user-configurable): plain text (auto-saved, plain `.md` export), checklist, stopwatch/timer with optional notification on completion, clock/timezone mini.
- Window chrome: borderless toggle, opacity slider, "pin to desktop" (we cannot set true topmost — document as "place in a small window"; the desktop surface + multi-window makes this work in practice).
- Keyboard: Ctrl+Enter = new checklist item; Ctrl+T = toggle timer.
- AppWidget variant (2×4) for widget-only users.
**Acceptance:** 10 concurrent instances, each < 5 MB RSS; text autosave < 500 ms after idle; window survives rotation/resize without content loss.

### 7.6 Downloads (Download manager)
**Purpose:** The download manager Android never needed — until this laptop.
**Features:**
- Sources: manual URL (paste/URL drop), share-intent capture ("Save in Alloy" from Chrome/any app), **watch folder** (monitor `Download/` for new files, import into queue with metadata), "grab from clipboard" (URL detection).
- Engine: chunked parallel HTTP (default 4 streams, user-set 1–16), resume via Range, speed limit, pause/resume/cancel, retry w/ backoff, per-download path + filename override (via SAF on first use, remembered).
- UI: 3-pane (queue | in-flight | history); columns: name, size, speed, ETA, streams; row actions incl. "open containing folder," "rename," "re-run"; filters; multi-instance.
- Completion: notification + optional "move to organized folder" rule (by TLD/extension/year-month).
- System `DownloadManager` reconciliation: imports browser-initiated downloads into history read-only (no double-download).
**Non-goal v1:** BitTorrent (scope decision at v1.2 — libtorrent4j spike), browser extension companion (possible v1.2; Chrome-extension "Send to Alloy" = trivial win if policy allows, high delight).
**Acceptance:** 1 GB file, 4 streams, 500 Mbps link → ≥ 85 % of link capacity; kill app mid-transfer → resume < 10 % re-download; 1,000-item history loads < 1 s.

### 7.7 Treemap (Disk usage)
**Purpose:** "What's eating my disk" — WinDirStat energy, native and keyboard-driven.
**Features:**
- Scan roots: SAF tree pick (default, no special permission) **or** full-disk with `MANAGE_EXTERNAL_STORAGE` (clear upsell in-module).
- Scanner: NIO walk, per-directory aggregation, symlink-safe, skips `/proc`, `/sys`, AIDL-external duplication, per-user exclude patterns (e.g., `**/.git`), progress + cancel, incremental rescan of changed dirs (mtime-based, v1.1).
- View: squarified treemap (Compose canvas), hover = size + path, click = drill-in, breadcrumb; right panel = "largest files" top-N + "by extension" aggregate; search filters live.
- Actions on a node: open in Files, copy path, add exclude, "delete" (with mandatory confirm, moves to Trash/Recents per platform behavior — never hard-delete in v1), export report (JSON/CSV via file picker).
- Multi-instance; drop folder = scan that.
**Acceptance:** 500k-file volume scans to 95 % in < 60 s (16 GB/512 GB class); treemap renders 10k leaves at ≥ 45 fps during drag; zero OOMs at 4 GB heap with 1M entries (stream aggregation, capped in-memory leaf set with rollups).

### 7.8 Phone Studio (Phone storage cleanup)
**Purpose:** The phone is a junk drawer; the desk is where the sorting happens.
**Architecture (depends on OQ-2):**
- **Source adapters** (interface `PhoneSource`): (a) Quick Access API — if a third-party SDK exists at launch, implement first; (b) **`agent` companion app** on the phone (same org) + mDNS discovery + TLS local transfer — source of truth for v1; (c) USB MTP — if the OS surfaces it as a `StorageVolume` (OQ-1), read-only browse + copy-out in v1.1.
**Cleanup engine (source-agnostic):**
- Photo/video triage: perceptual dedupe (dHash on downscaled bitmap; threshold user-set), blur detection (Laplacian variance), tiny/failed-capture filter, burst groupings, > X MB outliers.
- Downloads triage: by age, size, extension; "keep latest per app."
- Output: **archive plan** = moves-to-laptop (into user-chosen folder, deduped by hash) + "delete from phone" list (always explicit per-batch confirm; per-batch undo = move-back).
- Reports: space-reclaimable summary card (share-able, Stats-style).
**Hard rules:** never auto-delete; every destructive op has a per-batch confirm + 7-day undo window (archive on laptop is the undo); all operations idempotent by content hash.
**Acceptance:** 30k-photo phone library (via agent, 1 Gbps LAN) → dedupe report < 5 min; false-positive rate on "identical" pairs < 0.1 % at default threshold (measured on a fixture library in CI).

### 7.9 Backup (Local backup to external drive)
**Purpose:** Time Machine for Googlebook — scheduled, incremental, boring.
**Scope v1:**
- Sources: (a) MediaStore (photos/videos/audio) — no special permission; (b) user-chosen SAF trees (documents, projects); (c) app data for user-installed apps via `BackupAgent` **if** the user grants "back up app data" (Android's `REQUEST_INSTALL`-free app-data backup is not generally available — verify OQ; fallback: document that v1 = user content, app data v1.1+ if platform allows).
- Destinations: external USB volume **if surfaced by the OS** (OQ-1, P0); otherwise v1.1 = SMB/NAS destination (smbj) + future "phone as backup target" (agent).
- Engine: content-addressed store (SHA-256), hardlinks for space, incremental (size+mtime fast path → hash verify), per-run manifest, verify mode (re-read + hash), prune of orphaned blocks (only after N clean runs), optional whole-vault encryption (Keystore-backed, key never leaves device; exportable key file for recovery, warned about in bold).
- UX: one-shot "Backup now" + WorkManager schedule (daily/weekly, charging-only option, Wi-Fi/Ethernet-only); run history with space stats; browse/restore any snapshot (file browser over the store, drag-out to file picker).
**Acceptance:** 500 GB source, 2 TB exFAT USB: first run completes and is resumable after power loss; 2nd run transfers < 3 % of data for a 1 % change; restore of a 4K video < 1.5 × its size in wall time.

### 7.10 DeskTerm (Terminal / SSH / SFTP) — DFM `deskterm`
**Purpose:** The terminal my laptop deserves.
**Features v1 (SSH-first):**
- SSH: MINA SSHD client; profiles (host, user, key/pass via Keystore, keepalive), known-hosts manager, port forwarding (local/remote) UI, tabs + split panes, font/size/scheme settings, xterm-256 ANSI (renderer: libterminal or in-house VT parser — decide in Phase 3 spike), mouse selection → Clip, paste confirmation for multi-line, copy as JSON (for support).
- SFTP: side pane or separate window: browse/upload/download with progress, drag from system file picker, rename/mkdir/remove, "open in terminal cd" (sets local dir context if a local shell exists, else just copies path).
- Sessions: persisted; "recent" list; session export (JSON, encrypted if it contains secrets).
- Multi-window + multi-instance (obviously).
- **Local shell:** if OQ-3 confirms a platform shell surface, wrap it as a first-class "local" session with the same UI; otherwise v1 local session = restricted console (env, ls via /proc? no — be honest: local session is v1.1 pending platform facts).
**Non-goals v1:** X11 forwarding, tmux integration UI (works transparently inside a session), Windows/serial/PPP.
**Acceptance:** 50 concurrent SSH sessions idle < 50 MB total; `htop`-style 60 fps in a 200×50 pane at 144 Hz panel; SFTP 5 GB transfer sustains ≥ 80 % of USB3/NAS throughput; zero dropped keystrokes in 1 h fuzzed input test.

### 7.11 GitDesk (Git client) — DFM `gitdesk`
**Purpose:** A keyboard-soul Git client for the desk.
**Engine:** JGit (clone/fetch/pull/push/commit/stage/unstage/branch/switch/merge/stash/tag/remote/cherry-pick; rebase = list view + "open conflict" — no interactive rebase UI in v1).
**UI:** 4-pane: Repos | Commits/Refs | Files changed | Diff (side-by-side, java-diff-utils, syntax highlight via incremental parser — highlight.js port or in-house subset).
- Keyboard: `j/k` nav, `o` open diff, `s` stage, `u` unstage, `c` commit (message editor pane, co-author templates), `b` branch menu, `g` go-to-file, `f` filter, `?` help. Everything reachable without pointer.
- Staging at file + hunk level (JGit patch application for hunk staging — spike in Phase 3; fallback = file-level only).
- Diff → Clip ("copy hunk"), diff → Model Manager ("explain this change" — local, in-app, no cloud).
- Auth: credential helper via SSH key (shared with DeskTerm profile store) + HTTPS tokens in Keystore.
- Multi-instance; drop a folder = "open as repo" (with a polite "this is not a git repo" state).
**Acceptance:** 100k-commit repo: log p95 scroll < 16 ms/frame with virtualization; commit 1k files < 3 s; merge conflict flow: detect → list → resolve in system editor or inline → complete, without data loss in the 20-case regression suite.

### 7.12 Model Manager (local LLM host) — DFM `modelmgr`
**Purpose:** One local model, hosted, usable by you and by the machine.
**Scope (deliberately narrow):** host **one** GGUF model (2–8 GB class, Q4/Q5) at a time; chat + completion + summarize/classify tasks; expose a loopback OpenAI-compatible API.
**Runtime:** llama.cpp via NDK; backends: NEON CPU (default) + Vulkan GPU (auto-detect, user-overridable); context 1–32 k (default 8 k); sampling presets (chat/creative/precise); thermal-aware auto-throttle (`ThermalManager`); model warm/keep/evict policy (evict after 5 min idle by default).
**Model store:** download from Hugging Face (curated list + custom URL), SHA-256 verify, disk usage shown, one-active-model invariant (loading a 2nd prompts swap-with-confirm).
**UIs:**
- Chat window (multi-instance; model picker locked to the hosted model; system-prompt editor; per-chat context summary).
- "Quick Ask" floating panel (overlay or small window): any text → summarize/rewrite/explain, results paste into Clip.
- **API console:** live log of requests, tokens/s, prompt/completion tokens; docs tab (curl examples).
**API:** `127.0.0.1:8787/v1/chat/completions` + `/v1/completions` + `/v1/models`; bearer token (per-install, shown once, storable in other apps' config); loopback-only bind; optional per-request log.
**In-Alloy integrations:** Clip ("summarize entry"), GitDesk ("explain diff"), Treemap/Downloads reports ("plain-English summary"), Phone Studio reports.
**Honest limits (in-app):** not a model zoo, no fine-tuning, no multi-model routing; NPU acceleration = v2 research (see OQ-6). Performance expectations set per-hardware in the docs tab.
**Acceptance:** 7 B Q4 model on 16 GB/Qualcomm X Elite class: ≥ 8 tok/s decode sustained 10 min, p99 time-to-first-token < 4 s; API survives 100 req/min load test with clean queueing; kill -9 during generation → clean model state on next start; 0 bytes of prompt leave the device (verifiable: API console shows loopback-only + network-off test).

---

## 8. Cross-cutting requirements

- **Performance budgets:** base app cold start < 2 s (16 GB class); all-modules-on resident < 200 MB; widget idle < 0.5 % CPU-min; no module may hold a wake lock without a user-visible "running" state.
- **Privacy/security:** no account, no third-party SDKs (no crashlytics in v1 — local crash log viewer in Settings instead), opt-in anonymous diagnostics (off by default, no PII, includes a "what we'd send" preview), all local data in app-namespace + Keystore-encrypted where sensitive; local API loopback + token; dependency audit in CI (OSV-Scanner).
- **Reliability:** crash-free sessions ≥ 99.5 % (internal crash log), ANR < 0.05 %; every long-running engine (scan/download/backup/inference) is cancellable and resumable.
- **Accessibility:** full TalkBack labeling, focus order per desktop-quality guidelines (initial focus on primary input, e.g., commit box in Git), contrast-compliant dark/light, font scaling to 200 %.
- **i18n:** en (v0.1); de/fr/ja/ko/zh (v1.1) — the premium buyer base is global; string resources from day 1.
- **Branding:** "Alloy" is a codename; final name decision by v0.9 (Play listing name is semi-permanent). Tagline candidate: *Power tools for your Googlebook.*
- **Support surface:** "System report" (Stats module) doubles as the support artifact; in-app "diagnostics export" (redacted).

---

## 9. Keyboard map (draft — all rebindable)

| Action | Default |
|---|---|
| Alloy quick-switcher (ImKeys) | `Ctrl+Space` |
| Fire scenes 1–9 | `Ctrl+Alt+1…9` |
| Alloy main window focus/raise | `Ctrl+Alt+A` |
| Module switcher (within main window) | `Ctrl+Tab` |
| Global search (within main window) | `Ctrl+K` |
| Clip: paste entry 1–9 / search | `Ctrl+Shift+1…9` / `Ctrl+Shift+K` |
| Scratch new instance | `Ctrl+Alt+S` |
| Stats alert mute | `Ctrl+Alt+X` |
| Model Manager: quick ask | `Ctrl+Alt+L` |
| In-module: help | `?` |

Conflict policy: on first launch, scan for OS-level conflicts (Quick Insert key features, OEM keys) and warn; never bind to the Quick Insert key or any Google-key sequence.

---

## 10. Open questions — P0 validation sprint (do BEFORE building against these)

| # | Question | Why it blocks | How to validate | Owner: you |
|---|---|---|---|---|
| OQ-1 | Does Googlebook surface **USB mass storage** as a `StorageVolume` to Android apps? | Backup destination; phone-over-USB path | GSI/early unit: plug USB drive, `dumpsys mount` + StorageManager in a probe app | Week 1 |
| OQ-2 | Is **Quick Access** (phone files) available via any third-party API/SDK, or is it system-surface-only? | Phone Studio source (a) vs (b) | Read Android 17 release notes + developer.android.com/googlebook subpages; poke the GSI; ask on Android dev channels | Week 1 |
| OQ-3 | What is the integration surface for the **Linux terminal environment** (Antigravity "interacts with" it — how? ADB? container socket? Play-published bridge)? | DeskTerm local session quality | Install Antigravity on GSI; inspect its interfaces (`adb shell`, `pm dump`, logcat); read its Play listing/permissions | Week 1–2 |
| OQ-4 | Does the Googlebook window manager honor **`setLaunchBounds`** and `FLAG_ACTIVITY_LAUNCH_ADJACENT` as on AOSP 15+? | Scenes placement | Launch probe activities with bounds on GSI + desktop emulator | Week 1 |
| OQ-5 | **Overlay window** (`SYSTEM_ALERT_WINDOW`) grant flow and any desktop-specific restrictions | Stats Live Mode | Grant + float a 1 Hz view on GSI | Week 1 |
| OQ-6 | **AICore** third-party model hosting status (can non-Google models be hosted/scheduled for NPU?) | Model Manager v2 path (NPU) | Read AICore docs + Android 17 notes; test AICore APIs on GSI | Week 2 |
| OQ-7 | IME behaviors: can our IME **observe Ctrl+C** reliably in Chrome/TWA/terminal? Is key delivery consistent across the new window manager? | ImKeys clip-capture + macro coverage | IME probe on GSI: log key events in 10 target apps | Week 2 |
| OQ-8 | **Play policy posture** for: IME + overlay + `MANAGE_EXTERNAL_STORAGE` + loopback server in a power-user tool | Distribution risk | Pre-submission via Play Console policy docs + (if available) Google's new Googlebook dev contacts/office hours | Week 2 |
| OQ-9 | Exact **target SDK / new form-factor APIs** for desktop (new config qualifiers? "desktop" UI mode? device category in Play?) | Build config, Play listing metadata | Android 17 release notes; `aapt2` dump of GSI framework; desktop emulator `dumpsys` | Week 1 |
| OQ-10 | Does the **desktop emulator** in AS Canary match real-device windowing (bounds, multi-instance) closely enough for dev loop? | Dev-loop validity | Diff probe behavior: emulator vs GSI | Week 1 |

Decision rule: any OQ with a "no" gets a documented fallback per module (§7). No module ships on an assumption that failed validation.

---

## 11. Risks & mitigations

| Risk | L×I | Mitigation |
|---|---|---|
| Google ships overlapping first-party utilities in a QPR update (clipboard, downloads, backup are classic Google acquisitions) | M×H | Ship the *suite* + brand, not single features; each module is independently valuable; local-first/privacy positioning is structurally different from Google's cloud-first habits; quarterly cadence means we have a 3-month head-start per cycle |
| Platform APIs missing for a headline module (OQ-1/2/3 failures) | M×H | Module isolation means we ship 10/11; fallbacks pre-specified in §7; "honest limits" in-app builds trust instead of hiding gaps |
| IME trust friction caps macro/clip adoption | M×M | Transparency card, tiny footprint, IME-optional paths for everything (tile/NotificationListener for clip; widget buttons for scenes) |
| Scoped-storage walls (treemap/backup full-disk) | M×M | SAF-tree default (no special permission) as the citizen path; `MANAGE_EXTERNAL_STORAGE` as explicit opt-in with in-module justification |
| NPU SDK lockout for Model Manager v2 | M×L | v1 works fine on NEON+Vulkan; NPU is upside, not dependency |
| Solo-dev scope creep (11 modules is a lot) | H×M | Strict phase gates below; DFM defers the 3 heaviest; "done" = acceptance criteria in §7, not feature lists |
| Launch-week review window is 1–2 weeks | M×H | Public beta with Phase 1 set *before* launch; prepare a 2-minute demo video per module; line up 3–5 reviewers (Chrome Unboxed / r/chromeos / dev YouTubers) with early builds |

---

## 12. Roadmap

| Phase | Window | Contents | Exit criteria |
|---|---|---|---|
| **0 — Validation** | W1–W2 | OQ-1…OQ-10 answers; repo scaffold; CI; desktop emulator + GSI dev loop; design system tokens | OQ table closed; hello-world passes desktop quality self-check |
| **1 — MVP (beta on launch day, 10/05)** | W3–W6 | Shell/dashboard + Settings + module registry; **Stats** (widget), **Scenes** (v1), **Clip** (+ ImKeys clip-capture), **Scratch** | 4 modules GA-quality on beta track; "Optimized for Desktop" self-assessment ≥ 90 %; crash-free ≥ 99 % |
| **2 — Power core** | W7–W10 | **Downloads**, **Treemap**, ImKeys full (expansion/macros/quick-switcher) | Public beta expansion; first press/guide placements |
| **3 — Desk tools DFMs** | W11–W16 | **DeskTerm** (SSH/SFTP), **GitDesk**, hunk-staging spike | DFMs installable; SSH/Git acceptance suites green |
| **4 — Data + AI DFMs** | W17–W22 | **Phone Studio** (+ agent app), **Backup** (ext-drive or SMB), **Model Manager** | Phone Studio + Backup GA; Model Manager: 7 B Q4 ≥ 8 tok/s on reference hardware |
| **v1.0** | ~Q1 2027 (2nd QPR) | Hardening, i18n (de/fr/ja/ko/zh), monetization decision, name finalization | All 11 modules stable; badge earned; ≥ 3 modules/active-user median |

Deliberate sequencing logic: Phase 1 is the *brand* (fast, visible, zero scary permissions); Phase 2 is the *daily driver*; Phase 3–4 are the *moat* (data gravity: your git repos, your backups, your hosted model).

---

## 13. Success metrics & monetization note

- **Activation:** ≥ 40 % of installs enable ≥ 2 modules in week 1.
- **Retention:** D30 ≥ 35 % (utility category benchmark); north star = ≥ 3 modules/active user.
- **Quality:** crash-free ≥ 99.5 %; "Optimized for Desktop" badge on base + all DFMs.
- **Ecosystem:** Model Manager loopback API requests/day (proxy for power-user depth); scene exports shared; press/guide mentions (target: 3 named mentions in launch-month coverage).
- **Monetization (v1.1 decision, not now):** leading candidate = one-time "Alloy Pro" unlock for the DFM extras (SFTP, scheduled backup, model context > 8 k, theme packs for the Stats pill). Avoid subscription in v1 — the audience resents it and PowerToys' credibility is free.

---

## 14. Play Store & desktop quality-tier mapping

Map each Tier-1 "Desktop" guideline to where Alloy satisfies it (use as pre-submission checklist; the guideline IDs are from developer.android.com/docs/quality-guidelines/adaptive-app-quality/experiences/desktop):

| Guideline | Where satisfied |
|---|---|
| Scrollbar_Display | All scroll containers (custom Compose scrollbar on mouse/trackpad scroll) |
| Hover_Parity | Tooltips + flyouts on every list row, treemap cell, toolbar action |
| Desktop_Menus | Right-click context menus in all lists/canvases; top menu bar in main window |
| UI_Config | 3-pane layouts with draggable dividers; list/grid toggle in Downloads/Clip/Git; dockable diff pane in GitDesk |
| Request_Fullscreen_Mode | Terminal + Model Manager chat implement `Activity#requestFullscreenMode()` |
| Keyboard_Navigation / Keyboard_Parity | §9 map; standard Ctrl-C/V/X/Z/A respected everywhere; per-module tables in `?` |
| Input_Combinations / Triple_Click | Multi-select in all lists; triple-click selects whole line in terminal/scratch/diff |
| Multitasking_PiP / Split-Screen / Attachments | PiP n/a (no media module) — document; split-screen: all windows; attachments: Clip/Git open-in |
| Multi-Instance | Terminal, Git, Scratch, Treemap, Model chat: `multiInstance="true"` |
| Drag_Drop_Support / Batch | DropHelper: URLs→Downloads, files→Treemap/Git/Scratch, text→Clip/Scratch; batch drop of multiple files |
| Printing_Support | Reports (Stats, Phone Studio, Treemap export) → print/PDF |
| File_Management_Basics / File_Picker / File_Handlers | SAF everywhere; handlers: `.log`/`.txt`/`.md` → Scratch (decide Phase 2), scene JSON import |
| Custom_Cursors / Cursor_Target_Size | PointerIcon: I-beam (text), crosshair (treemap), copy (clip), grab (panes) |
| Cross_Device_Handoff | Clip "open on phone" via agent (Phase 4); scenes export; design now for Continue On compatibility |
| Offline_Support | All modules offline-capable; network ops show explicit state |
| Web_Transition | Model Manager API docs → in-app; Git remote URLs → in-app (no forced web bounce) |

---

## 15. Repo scaffold & dev setup

```
alloy/
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── app/                          # shell: nav, dashboard, settings, module registry, design system wiring
├── core/
│   ├── common/                   # DI, events, shortcut registry, result types, logging (local)
│   ├── design/                   # M3 theme, dynamic color, desktop layouts, hover/focus primitives
│   ├── proc/                     # /proc readers, ThermalManager/BatteryManager/TrafficStats wrappers
│   ├── datastore/                # Room infra, Keystore crypto, encryption-at-rest helpers
│   └── netlocal/                 # Ktor loopback server, mDNS discovery, TLS local transport
├── modules/
│   ├── scenes/
│   ├── imkeys/                   # IME entry point (same APK, second <activity> with IME service)
│   ├── statspill/
│   ├── clip/
│   ├── scratch/
│   ├── downloads/
│   ├── treemap/
│   ├── phone-studio/
│   └── backup/
├── features-dfm/                 # on-demand install (play-dynamic-features)
│   ├── deskterm/                 # MINA SSHD, libterminal (or in-house VT), SFTP UI
│   ├── gitdesk/                  # JGit, java-diff-utils, diff renderer
│   └── modelmgr/                 # NDK + llama.cpp (externalNativeBuild), GGUF store, API console
├── agent/                        # separate Play app: phone companion (mDNS + TLS, MediaStore ops)
└── tooling/
    ├── bench/                    # tok/s harness, scan/download benchmarks, CI fixture libraries
    └── probe/                    # OQ validation apps (single-purpose throwaway instruments)
```

**Dev environment checklist (week 0):**
1. Android Studio **Canary** + **desktop emulator** enabled (this is the primary dev surface; AS Canary is required — stable may lack the desktop AVD).
2. GSI flash (ARM64, leaked build CL2B.260330.037) on spare hardware as the *verification* device; treat emulator for iteration, GSI for truth.
3. Local.properties → both ABIs; Play internal testing track from day 1 (sids for 5+ human testers).
4. CI: build + unit + instrumented (emulator) + OSV-Scanner + license check; artifact = base APK + 3 DFM bundles + agent APK.
5. Fixture data in `tooling/bench`: 30k-image synthetic phone library (for Phone Studio CI), 500k-file synthetic volume (Treemap), 100k-commit git repo (GitDesk), 7 B Q4 GGUF (Model Manager).
6. Style: ktlint + detekt; Compose compiler metrics in CI (recomposition regressions on the dashboard are a real perf risk).

---

## 16. Appendix A — first-week task list (the OQ sprint, concretely)

- [ ] Day 1–2: AS Canary installed; desktop AVD created; `probe` app: StorageManager volume dump + UsbManager attach log (OQ-1); dumpsys mount with USB drive plugged.
- [ ] Day 2–3: `probe`: launch activities with `setLaunchBounds` in 4 placements × multi-instance, screenshot diff vs docs (OQ-4, OQ-10); overlay 1 Hz float 10 min (OQ-5).
- [ ] Day 3: IME probe: minimal IME logging key events in Chrome, a TWA, and any terminal app present on GSI (OQ-7); test Ctrl+C visibility.
- [ ] Day 4: Install Antigravity on GSI; inspect how it reaches the Linux environment (adb? socket? permission list via `dumpsys package`) (OQ-3); grep AICore services (`pm list services | grep -i ai`) (OQ-6).
- [ ] Day 4–5: Read Android 17 release notes + every developer.android.com/googlebook subpage; note new APIs/qualifiers (OQ-9); Quick Access API surface search (OQ-2).
- [ ] Day 5: Play policy doc review for IME + overlay + MANAGE_EXTERNAL_STORAGE + loopback HTTP (OQ-8); open Play Console project; name check ("Alloy" and 2 fallbacks).
- [ ] EOW: OQ table filled (answer + evidence link/screenshot per row); fallback decisions recorded in §7; repo scaffold merged; CI green on hello-world with desktop quality self-check.

## Appendix B — dependency shortlist (pin in `libs.versions.toml` at Phase 0)

OkHttp · Room · DataStore · Hilt · WorkManager · kotlinx-serialization · Compose (BOM) · DropHelper (per quality-guidelines link) · libterminal (or in-house VT) · Apache MINA SSHD · JGit · java-diff-utils · smbj (v1.1) · libtorrent4j (v1.2 spike) · Ktor (server, loopback) · llama.cpp (git submodule, externalNativeBuild) · ML Kit (text) · ZXing (QR, agent) · mDNS (JmDNS or mdns in core/netlocal)

## Appendix C — names considered (decide by v0.9)

Alloy (working) · DeskForge · BookKit · ALOSTools · The Desk Suite

---

*Sources for platform facts (all accessed 2026-09-21): Google blog "Introducing Googlebook" (2026-05-12); developer.android.com/googlebook + desktop quality-guidelines tier (updated 2026-08-18); Wikipedia "Googlebook OS" (release 2026-10-05; GSI build CL2B.260330.037); 9to5Google hands-on "Googlebook OS: It's Android, stupid" (2026-09-21, incl. Linux terminal environment, Rambler/Quick Insert key, TWA choices, QPR cadence); PCMag/Verge/Android Police launch coverage (Play Store tags "Made for Desktop" / "Optimized for Desktop" / "Better on the Website"); aluminium-os.com status board (hardware requirements, OEM list, pre-order date). All OQ items are explicitly unverified — validate before relying on them.*
