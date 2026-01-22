# MobileCLI - Intellectual Property Documentation

**Owner:** MobileCLI Team
**Created:** January 2026
**Status:** Proprietary - Not Open Source

---

## Core Innovation

**MobileCLI is the first Android application that:**
1. Runs Claude Code AI on a mobile phone
2. Can rebuild itself from within (self-modification)
3. Was built ON an Android phone, not a PC
4. Creates a closed loop: AI builds app that runs AI

---

## Patent-Worthy Inventions

### 1. Self-Modification Loop
The AI running inside the app can modify the app's source code and compile a new APK.

```
Claude Code → modifies source → ./gradlew build → new APK → runs Claude Code → ∞
```

**Technical Implementation:**
- Dev tools installed at runtime (Java 17, Gradle, Android SDK)
- Source code accessible at `~/MobileCLI/`
- APK output to `/sdcard/Download/`

### 2. File-Based URL Opener (Android 10+ Bypass)
Android 10+ blocks background activity launches. Our solution:

```
Shell writes URL to: ~/.termux/url_to_open
MainActivity polls file, reads URL, opens browser with Activity context
File is deleted after opening
```

**Why it works:** Activity context has permission to launch other activities.

### 3. Persistent AI Memory System
AI tracks its own evolution across sessions:

```
~/.mobilecli/memory/
├── evolution_history.json  - Version history
├── problems_solved.json    - Bugs fixed with solutions
├── capabilities.json       - What AI has learned
└── goals.json              - Current objectives
```

### 4. Two-Claude Workflow
Development uses two AI instances:
- **BUILD CLAUDE:** In Termux, builds APK
- **TEST CLAUDE:** Inside MobileCLI, tests features

They communicate via GitHub bridge repository.

### 5. 7-Tap Developer Mode
Like Android's hidden developer options:
- Tap version number 7 times
- Shows countdown: "3 taps away from developer mode"
- Reveals developer options in menu

### 6. Full Termux API Without Termux:API
50+ API commands built directly into the app:
- Camera, microphone, sensors
- SMS, contacts, call log
- Clipboard, notifications
- Location, WiFi, battery

No need for separate F-Droid Termux:API app.

---

## Trade Secrets

### Bootstrap Customization
Our bootstrap includes pre-configured:
- npm settings for Termux compatibility
- AI tool configurations
- Dev environment paths
- Custom shell scripts

### Setup Wizard Psychology
- Hide technical complexity from users
- Show progress percentage during long downloads
- Error invisibility (retry silently)
- Final choice gives user agency

### Performance Optimizations
- Debounced terminal updates (prevent race conditions)
- Keyboard visibility tracking (prevent lock-up bug)
- Session persistence across activity lifecycle

---

## Competitive Moat

1. **First mover:** First working implementation
2. **Complexity:** Months of iteration to solve all edge cases
3. **Integration:** 50+ API commands, dev tools, AI tools unified
4. **Self-improvement:** App can enhance itself

---

## Licensing

This codebase is PROPRIETARY.
- Not MIT
- Not Apache
- Not GPL
- All rights reserved

### Original Code (~9,000+ lines)
All code in `/app/src/main/java/com/termux/` is 100% original:
- BootstrapInstaller.kt (~2,900 lines) - Bootstrap download/extraction system
- MainActivity.kt (~1,400 lines) - Terminal UI, drawer, gestures
- TermuxApiReceiver.kt (~2,000 lines) - 75+ API commands (NOT using Termux:API)
- TermuxService.kt (~600 lines) - Background service, wake locks
- SetupWizard.kt (~800 lines) - 3-stage setup flow
- AmSocketServer.kt (~440 lines) - Proprietary Activity Manager (replaced GPL am.apk)

### Third-Party Libraries (Apache 2.0 - Attribution Required)
- terminal-view: VT100 terminal rendering view
- terminal-emulator: VT100 escape code parser

These are small rendering libraries, NOT the Termux app. All application logic is original.

### Runtime Downloads (User Downloads, Not Bundled)
- Termux bootstrap packages (bash, node, python) - Downloaded by user at runtime
- These are NOT bundled with MobileCLI

---

## Contact

Website: https://mobilecli.com
GitHub: https://github.com/MobileDevCLI
Owner: MobileCLI Team

