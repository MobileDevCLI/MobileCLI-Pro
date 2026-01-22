# MobileCLI - Project Manifest

**Version:** 2.0.0 (Clean Rebuild)
**Status:** Production Ready
**Owner:** Samblamz

---

## FOR AI ASSISTANTS: READ THIS FIRST

This is a CLEAN, consolidated version of MobileCLI. There is ONE of everything.
Do NOT create duplicate functions, classes, or files.

### Project Rules
1. ONE setup wizard - in SetupWizard.kt
2. ONE bootstrap installer - in BootstrapInstaller.kt
3. ONE main activity - in MainActivity.kt
4. NO legacy code, NO commented-out old versions
5. Ask before creating new files

---

## File Map

### Core Files (DO NOT DUPLICATE)

| File | Purpose | Lines | DO NOT |
|------|---------|-------|--------|
| `SetupWizard.kt` | 3-stage setup flow | ~550 | Duplicate setup logic |
| `MainActivity.kt` | Terminal UI, session management | ~600 | Add setup wizard code here |
| `BootstrapInstaller.kt` | Bootstrap + environment setup | ~3000 | Add UI code here |
| `TermuxApiReceiver.kt` | 50+ API handlers | ~1500 | Modify unless adding new API |
| `app/TermuxService.kt` | Background service, wake locks | ~550 | - |
| `app/TermuxOpenReceiver.kt` | URL/file opening from shell | ~185 | - |

### Supporting Classes

| File | Purpose |
|------|---------|
| `TermuxApplication.kt` | Application class, initialization |
| `TermuxUrlHandlerActivity.kt` | Opens URLs from shell (Android 10+ fix) |
| `TermuxAmDispatcherActivity.kt` | Handles `am start` from terminal |
| `activities/SettingsActivity.kt` | Settings screen (placeholder) |
| `filepicker/TermuxFileReceiverActivity.kt` | Receives shared files |
| `filepicker/TermuxDocumentsProvider.kt` | SAF file provider |
| `boot/BootReceiver.kt` | Run scripts on device boot |
| `app/RunCommandService.kt` | External command execution (Tasker) |

---

## Architecture

```
App Launch
    │
    ▼
┌─────────────────┐
│  SetupWizard    │  ← Handles all first-time setup
│  (3 stages)     │
└────────┬────────┘
         │ Setup complete
         ▼
┌─────────────────┐
│  MainActivity   │  ← Terminal UI only
│  (Terminal)     │
└────────┬────────┘
         │ API calls
         ▼
┌─────────────────┐
│ TermuxApiReceiver│  ← Handles termux-* commands
└─────────────────┘
```

---

## Setup Wizard Stages

### Stage 1: Permissions
- Request all 15 Android permissions
- Guide 7-tap developer mode activation
- No downloads yet

### Stage 2: Full Environment Download
- Download Termux bootstrap (~50MB)
- Install ALL packages: Node.js, Python, Java 17, Gradle
- Install ALL AI tools: Claude, Gemini, Codex
- Install dev tools: aapt, d8, apksigner
- Show progress bar with percentage

### Stage 3: Choose Your AI
- Modern card UI
- Claude (RECOMMENDED), Gemini, Codex, Basic Terminal
- Everything already installed - just choosing what to LAUNCH
- Selected AI opens immediately

---

## Key Paths (Termux Compatible)

| Path | Purpose |
|------|---------|
| `/data/data/com.termux/files/home` | HOME directory |
| `/data/data/com.termux/files/usr` | PREFIX (binaries, libs) |
| `/data/data/com.termux/files/usr/bin` | Executables |
| `/sdcard/Download` | User-accessible output |

---

## Build Commands

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

---

## Intellectual Property

See `IP.md` for full documentation of proprietary innovations:
1. Self-modification loop
2. File-based URL opener (Android 10+ fix)
3. Persistent AI memory system
4. Two-Claude workflow
5. 7-tap developer mode

---

## DO NOT

- Create multiple setup wizards
- Add legacy AlertDialog code
- Mix UI logic into BootstrapInstaller
- Create duplicate classes
- Modify package name (must be com.termux)
- Change targetSdk above 28

