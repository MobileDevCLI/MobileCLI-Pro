# MobileCLI - Internal Development Notes

**Last Updated:** January 2026
**Status:** Active Development
**Version:** 2.0.0

---

## Quick Context (For AI Sessions)

MobileCLI is a Termux-compatible Android terminal that runs AI coding assistants (Claude, Gemini, Codex). The breakthrough feature is **self-modification** - the AI running inside can rebuild the app itself.

**Critical Constraints:**
- Package name MUST be `com.termux` (hardcoded in Termux binaries)
- targetSdk MUST be 28 (Android 10+ blocks exec() from app data)
- HOME path: `/data/data/com.termux/files/home`

---

## Architecture Overview

```
User opens app
    │
    ▼
SetupWizard.kt (if first run)
    │
    ├── Stage 1: Bootstrap download (~50MB)
    │   └── BootstrapInstaller.kt handles this
    │
    ├── Stage 2: AI tools installation
    │   └── Node.js, Claude Code, Gemini, Codex
    │
    └── Stage 3: AI selection
        └── User picks which AI to launch
    │
    ▼
MainActivity.kt (Terminal UI)
    │
    ├── TerminalView (from termux-app library)
    ├── Navigation Drawer (swipe left)
    ├── Extra Keys (ESC, CTRL, arrows, etc.)
    └── Service binding (TermuxService)
    │
    ▼
TermuxService.kt (Background)
    │
    ├── Wake lock (CPU stays awake)
    ├── WiFi lock (network stays connected)
    ├── AmSocketServer (fast am commands)
    └── Session persistence
```

---

## Key Files

| File | Lines | Purpose |
|------|-------|---------|
| `MainActivity.kt` | ~1000 | Terminal UI, drawer, gestures, developer mode |
| `BootstrapInstaller.kt` | ~2966 | **IP** - Apache-based bootstrap download/extraction |
| `SetupWizard.kt` | ~600 | 3-stage setup flow with progress UI |
| `TermuxService.kt` | ~550 | Background service, wake locks, am commands |
| `TermuxApiReceiver.kt` | ~2000 | 78 Termux API commands |
| `AmSocketServer.kt` | ~440 | Unix socket for fast am command execution |

---

## What Loads During Setup

### Stage 1: Bootstrap (0-50%)
- Downloads from Apache mirror (~50MB compressed)
- Extracts to `/data/data/com.termux/files/usr/`
- Sets up: bash, coreutils, apt, pkg, etc.
- **Time:** 2-5 minutes depending on connection

### Stage 2: AI Tools (50-90%)
- `pkg update && pkg install nodejs-lts`
- `npm install -g @anthropic-ai/claude-code`
- `mkdir -p ~/.node-gyp && echo '9' > ~/.node-gyp/installVersion` (NDK workaround)
- `npm install -g @google/gemini-cli`
- `npm install -g @openai/codex`
- **Time:** 3-5 minutes

### Stage 3: AI Selection (90-100%)
- User picks Claude/Gemini/Codex/None
- Saves preference
- Launches MainActivity with selected AI

---

## Features Status

### Working
- [x] Terminal display and input
- [x] Multi-session support (tabs)
- [x] Extra keys (ESC, CTRL, ALT, arrows, symbols)
- [x] Navigation drawer
- [x] Text size adjustment (persisted)
- [x] Toggle keyboard
- [x] Wake lock toggle
- [x] Power mode toggle
- [x] 7-tap developer mode activation
- [x] Long-press context menu (Copy/Paste/More)
- [x] Settings dialog (text size, wake lock on startup, default AI, reset app)
- [x] Help dialog
- [x] About dialog
- [x] URL opening (file-based + socket)

### Needs Work
- [ ] Install AI Tools button (verify functionality)

### Planned (Developer Mode)
- [ ] Reinstall Bootstrap button
- [ ] Reinstall AI Tools button
- [ ] Clear Cache button
- [ ] Live debug log viewer

---

## Developer Mode

Activated by tapping version number 7 times (like Android Developer Options).

**Current dev options:**
- Developer Mode: ON/OFF toggle
- Install Dev Tools button

**Planned additions:**
- Reinstall Bootstrap
- Reinstall AI Tools
- Clear Cache
- View Logs/Debug

---

## Intellectual Property

**Unique innovations (our IP):**
1. Self-modification loop - app rebuilds itself from within
2. BootstrapInstaller - Apache-based Termux bootstrap
3. File-based URL opener - bypasses Android background restrictions
4. AmSocketServer - 10x faster than app_process for am commands
5. SetupWizard UX - professional 3-stage onboarding
6. 7-tap developer mode - hidden power features

**Third-party libraries (Apache 2.0 - for VT100 rendering only):**
- terminal-view: Terminal display component
- terminal-emulator: VT100 escape code parser
- AndroidX libraries

Note: These are rendering libraries, NOT the Termux app. All application code is original (~9,000+ lines).

**Future protection:**
- ProGuard/R8 obfuscation at release time
- Consider moving critical logic to native code

---

## File Paths (Important)

| Path | Purpose |
|------|---------|
| `/data/data/com.termux/files/home` | User home directory (~) |
| `/data/data/com.termux/files/usr` | PREFIX - installed packages |
| `/data/data/com.termux/files/usr/bin` | Executables (bash, node, etc.) |
| `~/.termux/` | Config directory |
| `~/.termux/power_mode` | Power mode flag file |
| `~/.termux/url_to_open` | URL opener mechanism |
| `/sdcard/Download/` | User-accessible downloads |

---

## Session Context

**Recent changes (this session):**
- Added wake lock toggle in drawer
- Added power mode toggle in drawer
- Implemented 7-tap developer mode
- Fixed long-press context menu
- Added service binding for wake lock control
- Created Settings dialog with preferences
- Text size now persists across restarts
- Added "Wake Lock on Startup" setting
- Added "Default AI" setting
- Added "Reset App" option
- Created INTERNAL.md documentation

**Decisions made:**
- Keep changes small and incremental
- Test each change before moving on
- Maintain clean, readable codebase
- Developer features hidden behind 7-tap activation
- Document UI content locations for easy editing

---

## Next Steps (Priority Order)

1. Add Reinstall Bootstrap button (dev mode)
2. Add Reinstall AI Tools button (dev mode)
3. Add Clear Cache button (dev mode)
4. Consider live debug log viewer
5. Verify Install AI Tools functionality

---

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Install on specific device
adb -s DEVICE_ID install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## UI Content Locations (Where to Edit Text)

Quick reference for finding and editing UI text, dialogs, and labels:

### MainActivity.kt - Dialog Content

| Function | Line ~# | What It Contains |
|----------|---------|------------------|
| `showHelp()` | ~780 | Help dialog text (commands, gestures) |
| `showAbout()` | ~795 | About dialog (version, author, URL) |
| `showSettings()` | ~800 | Settings menu options |
| `showMoreOptions()` | ~720 | Long-press menu options |
| `showTextSizeDialog()` | ~760 | Text size options |
| `showAIInstallMenu()` | ~785 | AI launch menu options |
| `showDefaultAIDialog()` | ~835 | Default AI picker options |
| `confirmResetApp()` | ~855 | Reset confirmation message |
| `showContextMenu()` | ~950 | Context menu options |

### Layout Files - UI Labels

| File | What It Contains |
|------|------------------|
| `activity_main.xml` | Drawer menu items, button labels, headers |
| `activity_main.xml` (setup overlay) | Setup wizard UI (progress, AI cards) |

### Drawer Menu Items (activity_main.xml)
- Line ~200: "MobileCLI" header
- Line ~214: "Mobile AI Development" subtitle
- Line ~243: "+ New Session"
- Line ~271: "Settings"
- Line ~282: "Toggle Keyboard"
- Line ~293: "Text Size"
- Line ~305: "Wake Lock: OFF"
- Line ~317: "Power Mode: OFF"
- Line ~335: "Install AI Tools"
- Line ~346: "Help"
- Line ~357: "About"
- Line ~390: "Version 1.8.1"

### SetupWizard.kt - Setup Text
- AI card descriptions
- Progress messages
- Stage headers

### String Resources (res/values/strings.xml)
- App name and other string resources
- Currently most strings are hardcoded in Kotlin (can be moved to strings.xml for localization later)

---

## Notes for Future Sessions

When continuing development:
1. Read this file first for context
2. Check the "Features Status" section
3. Follow "Next Steps" priority order
4. Keep changes small and test individually
5. Update this file after significant changes
