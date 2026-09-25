# Feature Enhancements Roadmap

This document tracks user-requested and competitive-analysis-derived feature enhancements for Alloy modules. Items are categorized by feasibility and priority.

---

## 📋 Clip Module (Clipboard Workbench)

### High Priority (Feasible & High Impact)

| Feature | Description | Source | Status |
|---------|-------------|--------|--------|
| **Tags & Folders** | Organize clips with user-defined tags and folders for better categorization | PopSci, Android Police | |
| **Biometric Safe Box** | Secure sensitive clips (verification codes, notes) with biometric lock | Android Police | |
| **Search Across History** | Full-text search to find clips from months ago | Android Police, MakeUseOf | |
| **Edit Before Paste** | Modify clip content before pasting into target app | PopSci | ✅ Done |

### Medium Priority (Feasible, Moderate Impact)

| Feature | Description | Source |
|---------|-------------|--------|
| **Combine Multiple Clips** | Concatenate multiple selected clips before pasting | PopSci (Copy 'Em) |
| **QR Code Generation** | Convert URL clips directly to QR codes | PopSci (Clipboard Actions) |
| **Text Expansion Snippets** | Save reusable text templates (email signatures, addresses) | PopSci (Clipper+) |
| **Custom Cleanup Rules** | Auto-delete clips older than X days or exceeding size limits | PopSci |
| **Image Clipboard Support** | Capture and store images in addition to text | Alloy README |

### Nice-to-Have (Lower Priority)

| Feature | Description | Source |
|---------|-------------|--------|
| **Dynamic Values** | Insert current date/time/random values into clips | PopSci (Clipper) |
| **Web Interface** | Access clipboard history via browser | Android Police |
| **Custom Commands** | Script transformations (like CopyQ) | XDA Developers |

---

## 🎬 Scenes Module (Workspace Launcher)

### High Priority (Feasible & High Impact)

| Feature | Description | Source | Status |
|---------|-------------|--------|--------|
| **Keyboard Shortcuts** | `Ctrl+Alt+1…9` to launch scenes quickly | Alloy README, PowerToys | |
| **Scene Templates** | Pre-built templates for common workflows (coding, research, streaming) | Competitive analysis | ✅ Done |
| **Import/Export JSON** | Share scene configurations between devices | Alloy README | |

### Medium Priority (Feasible, Moderate Impact)

| Feature | Description | Source |
|---------|-------------|--------|
| **Window Hopper** | Cycle through windows of the same app (like PowerToys Window Walker) | Microsoft PowerToys |
| **Scene Scheduler** | Auto-launch scenes at specific times or events | Competitive analysis |
| **App Availability Check** | Warn if required apps aren't installed before firing scene | Competitive analysis |

### Nice-to-Have (Lower Priority)

| Feature | Description | Source |
|---------|-------------|--------|
| **Multi-Monitor Support** | Position windows across multiple displays | PowerToys FancyZones |
| **Scene Animations** | Smooth transitions between scene states | Competitive analysis |
| **Voice Commands** | Launch scenes via voice | Competitive analysis |

---

## 📊 StatsPill Module (System Telemetry)

### High Priority (Feasible & High Impact)

| Feature | Description | Source | Status |
|---------|-------------|--------|--------|
| ~~**Customizable Overlay Position**~~ | Drag to reposition floating pill | XDA Forums, Reddit | |
| **Network Speed Monitor** | Display upload/download speeds | Pocket-lint, Samsung Members | ✅ Done |
| **Alert Thresholds** | Notifications for high CPU, RAM, or temperature | Alloy README | |
| **Sparkline Graphs** | Visual history in widgets (CPU/RAM trends) | Alloy README | |

### Medium Priority (Feasible, Moderate Impact)

| Feature | Description | Source |
|---------|-------------|--------|
| **GPU Monitoring** | Add GPU usage to telemetry | XDA Forums (Profiler) |
| **Storage Monitoring** | Show available storage space | Pocket-lint |
| **Custom Refresh Interval** | Adjustable polling frequency (1s, 5s, 30s) | Competitive analysis |
| **Dark/Light Theme Overlay** | Match overlay to system theme | Competitive analysis |

### Nice-to-Have (Lower Priority)

| Feature | Description | Source |
|---------|-------------|--------|
| **Custom Widget Sizes** | 1×1, 2×2, 4×1 widget options | Alloy README |
| **FPS Counter** | For gaming performance monitoring | Reddit (retroid) |
| **Historical Charts** | View CPU/RAM usage over time | Competitive analysis |

---

## 📝 Scratch Module (Pinned Scratchpad)

### High Priority (Feasible & High Impact)

| Feature | Description | Source |
|---------|-------------|--------|
| **Full-Screen Widget** | Interactive whiteboard on home screen | The Intelligence (Ruff) |
| **Markdown Preview** | Render markdown alongside editor | Competitive analysis |
| **Multiple Scratchpads** | Independent note surfaces (like "scratch cards") | Benchpad |
| **Password Protection** | Lock private notes with biometric/PIN | Reddit |

### Medium Priority (Feasible, Moderate Impact)

| Feature | Description | Source |
|---------|-------------|--------|
| **Infinite Canvas** | Pan/zoom for large scratch space | Play Store (Scratch Pad) |
| **Horizontal/Vertical Layout** | Support landscape and portrait modes | Reddit |
| **Quick Clear Button** | One-tap clear all content | Play Store |
| **Drag & Drop Images** | Attach images to notes | Scratchpad Help |

### Nice-to-Have (Lower Priority)

| Feature | Description | Source |
|---------|-------------|--------|
| **Drawing Tools** | Freehand drawing alongside text | Competitive analysis |
| **Export to File** | Save notes as .md or .txt | Competitive analysis |
| **Sync Across Devices** | Cloud sync for scratchpads | Competitive analysis |
| **Templates** | Pre-formatted note templates (meeting notes, daily log) | Competitive analysis |

---

## 📚 Settings Module

### High Priority (Feasible & High Impact)

| Feature | Description | Source |
|---------|-------------|--------|
| **Theme Selection** | Light/Dark/System preferences | Competitive analysis |
| **Module Toggles** | Enable/disable individual modules | PowerToys pattern |
| **Data Export** | Export clipboard/scratch data | Competitive analysis |

### Medium Priority (Feasible, Moderate Impact)

| Feature | Description | Source |
|---------|-------------|--------|
| **Keyboard Shortcut Configuration** | Customize module launch keys | PowerToys |
| **Privacy Controls** | Opt-out of specific data collection | Competitive analysis |
| **Storage Management** | Clear clipboard/scratch data with one tap | Competitive analysis |

---

## 📰 RSS Feed Module (Planning Phase)

### High Priority (Feasible & High Impact)

| Feature | Description | Source | Status |
|---------|-------------|--------|--------|
| **Feed Aggregation** | Subscribe to and aggregate multiple RSS feeds | Competitive analysis | |
| **Offline Caching** | Download articles for offline reading | Industry standard | |
| **Unified Reader View** | Clean reading interface with markdown support | Industry standard | |
| **Smart Notifications** | Notify on new articles from priority feeds | Alloy pattern | |

### Medium Priority (Feasible, Moderate Impact)

| Feature | Description | Source |
|---------|-------------|--------|
| **OPML Import/Export** | Import/export subscriptions from other RSS readers | Industry standard |
| **Feed Categorization** | Organize feeds into custom folders | Industry standard |
| **Full-Text Fetching** | Auto-fetch full articles from summaries | Feedly pattern |
| **Search & Filter** | Search across all articles and filter by feed | Industry standard |

### Nice-to-Have (Lower Priority)

| Feature | Description | Source |
|---------|-------------|--------|
| **Integration with Scenes** | "News Reading" scene preset | Alloy ecosystem |
| **Text-to-Speech** | Listen to articles | Industry standard |
| **Share to Other Apps** | Send articles to Clip or Scratch | Alloy ecosystem |
| **Dark/Light Theme** | Match system theme | Industry standard |

---

## Implementation Notes

### Android Feasibility Considerations

| Feature | Feasibility | Notes |
|---------|-------------|-------|
| Cross-device sync | ✅ Medium | Requires backend; use Firebase or self-hosted |
| Biometric lock | ✅ Easy | Android Keystore + BiometricPrompt API |
| SYSTEM_ALERT_WINDOW | ✅ Easy | Already used by StatsPill |
| Floating overlay drag | ✅ Easy | WindowManager with touch handling |
| Image clipboard | ⚠️ Medium | Requires ContentResolver + image processing |
| GPU monitoring | ⚠️ Medium | May require root or limited to gaming mode |
| Multi-monitor support | ❌ Hard | Android Desktop mode limited support |
| Voice commands | ⚠️ Medium | Requires SpeechRecognizer API |

### Recommended First Pass

1. **Clip**: Tags/folders, biometric safe box, search
2. **Scenes**: Keyboard shortcuts, import/export JSON
3. **StatsPill**: Overlay position, network monitor, alerts
4. **Scratch**: Full-screen widget, markdown preview, multiple pads

---

## References

- [Android Police - Best Clipboard Manager](https://www.androidpolice.com/i-found-an-overlooked-clipboard-manager-its-my-pixels-single-best-upgrade/)
- [Popular Science - Clipboard Managers](https://www.popsci.com/diy/clipboard-managers-for-your-phone/)
- [Microsoft PowerToys Documentation](https://learn.microsoft.com/en-us/windows/powertoys/)
- [XDA Forums - System Monitors](https://xdaforums.com/t/app-4-0-profiler-cpu-gpu-ram-monitoring-in-floating-window.3759672/)
- [The Intelligence - Android Scratchpad](https://theintelligence.com/38553/android-scratchpad/)
