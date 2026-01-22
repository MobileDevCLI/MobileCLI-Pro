# MobileCLI - Roadmap to Release

**Created:** January 2026
**Updated:** January 19, 2026 (Beta 14)
**Status:** Beta - All Core Features Complete
**Goal:** App Store Ready
**Repository:** https://github.com/MobileDevCLI/MobileCLI-Alpha

---

## What MobileCLI IS (Honest Positioning)

> **ROOT WITHOUT ROOT.** 79 Android permissions give you root-equivalent access to everything: sensors, cameras, SMS, calls, contacts, GPS, Bluetooth, NFC, IR blaster, biometrics, /proc, /sys, and more. Full Linux dev stack. Build Android apps on Android. Self-modifying AI environment.

**Unique Value:**
- 79 Android permissions (nearly everything)
- 75+ device API commands (termux-*)
- Full dev stack: Node.js, Python, Java 17, Gradle
- Android build tools: aapt2, apksigner, d8
- Self-modification: AI can rebuild the app from within
- 3 AI assistants: Claude, Gemini, Codex
- Root-equivalent access without rooting

**NOT claiming:** "Better than PC" - different capability profile

---

## COMPLETED: Security Fixes

### 1. Keystore Password - FIXED
- Moved from `build.gradle.kts` to `local.properties`
- `build.gradle.kts` now reads from properties file
- Never committed to git

### 2. Username Leak - FIXED
- `local.properties` added to `.gitignore`
- Created `local.properties.template` for developers

### 3. Owner Name - FIXED
- Changed from "Samblamz" to "MobileCLI Team" in `IP.md`

### 4. .gitignore - CREATED
- Excludes `local.properties`, `*.keystore`, build artifacts, screenshots

---

## COMPLETED: Gemini CLI Fix

**Problem:** Native module compilation failed on Android/Termux
**Solution:** Create `~/.gyp/include.gypi` before npm install

```bash
mkdir -p ~/.gyp
printf "{\n  'variables': {\n    'android_ndk_path': ''\n  }\n}\n" > ~/.gyp/include.gypi
```

**Implemented in:**
- `SetupWizard.kt` - First-time setup
- `MainActivity.kt` - Reinstall AI tools
- `BootstrapInstaller.kt` - Bootstrap install

---

## COMPLETED: 79 Permissions (Root-Equivalent)

**Runtime permissions requested:** 30+ dangerous permissions
**Total manifest permissions:** 79

| Category | Permissions |
|----------|-------------|
| Storage | READ/WRITE_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE, READ_MEDIA_* |
| Camera/Audio | CAMERA, RECORD_AUDIO, MODIFY_AUDIO_SETTINGS |
| Location | ACCESS_FINE/COARSE/BACKGROUND_LOCATION |
| Bluetooth | BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_CONNECT/SCAN/ADVERTISE |
| Phone | READ_PHONE_STATE, CALL_PHONE, ANSWER_PHONE_CALLS, READ_CALL_LOG |
| SMS | SEND/READ/RECEIVE_SMS, RECEIVE_MMS |
| Contacts | READ/WRITE_CONTACTS, GET_ACCOUNTS |
| Calendar | READ/WRITE_CALENDAR |
| Sensors | BODY_SENSORS, ACTIVITY_RECOGNITION, HIGH_SAMPLING_RATE_SENSORS |
| Biometrics | USE_BIOMETRIC, USE_FINGERPRINT |
| NFC | NFC, NFC_TRANSACTION_EVENT |
| IR | TRANSMIT_IR |
| System | POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW, REQUEST_INSTALL_PACKAGES |

**Excluded (2):** Cosmetic/UI-changing permissions only

---

## COMPLETED: CLAUDE.md Updated

**Location:** `~/CLAUDE.md` (created during bootstrap)

Now documents:
- ROOT-EQUIVALENT ACCESS header
- 79 permissions with categories
- Linux system access (/proc, /sys, /system)
- 75+ termux-* commands (full list)
- Self-modification capabilities
- All sensor/hardware access
- Rules for AI behavior

---

## DECIDED: No XML Over-Engineering

Originally planned:
- ~~mobilecli-info command with XML output~~
- ~~XML sections in CLAUDE.md~~
- ~~MOBILECLI_* environment variables~~

**Decision:** Keep it simple. CLAUDE.md in plain markdown is sufficient. AI assistants read it well.

---

## COMPLETED: Legal Compliance

### First Launch Agreement
- Users must accept Terms of Service before using app
- Checkbox + "Accept & Continue" button required
- "Decline" exits the app
- Acceptance saved in SharedPreferences

### In-App Legal Documents (Drawer Menu)
- **About** - App info, version, team credit
- **Open Source Licenses** - Termux Apache 2.0 attribution
- **Privacy Policy** - Data handling disclosure
- **Terms of Service** - Full liability disclaimer

### Liability Protection
- "AS IS" warranty disclaimer
- NO LIABILITY for any damages
- User assumes ALL RISK for:
  - AI actions and outputs
  - SMS/calls made through app
  - Data accessed or modified
  - Power Mode usage
- Indemnification clause

---

## COMPLETED: Clean Bootstrap Launch

### Problem
When user clicked "Claude" after setup, they saw terminal output:
```
Starting fallback run of termux bootstrap second stage
[*] Running termux bootstrap second stage
[*] Running postinst maintainer scripts
[*] Running 'coreutils' package postinst
... (many more lines)
[*] The termux bootstrap second stage completed successfully
u0_a549@mobilecli:~$ claude
```

### Root Cause
- SetupWizard used `bash -c "command"` (direct bash)
- MainActivity used `bin/login -l` (login shell)
- The `login` script triggers bootstrap second stage
- Second stage only runs on first login shell

### Solution
Added `runLoginShell()` in SetupWizard that runs `login -c exit` during setup:
```kotlin
// CRITICAL: Run login shell once to trigger bootstrap second stage
updateProgress(51, "Configuring system packages...")
runLoginShell()
```

**Result:** User clicks Claude → clean launch, no terminal spam

---

## COMPLETED: GitHub Backup

**Repository:** https://github.com/MobileDevCLI/MobileCLI-Alpha

### Committed Files (44 total)
- All Kotlin source files
- AndroidManifest.xml (79 permissions)
- Layouts and resources
- Documentation (README, ARCHITECTURE, API_REFERENCE)
- Build configuration

### Version Control
- APKs saved with version numbers: `MobileCLI-v2.0.0-beta.14-20260119.apk`
- Stored in your local APKs folder
- All commits pushed to GitHub after each change

---

## COMPLETED: Documentation

### docs/ARCHITECTURE.md
- System overview diagram
- Component details (SetupWizard, MainActivity, BootstrapInstaller, etc.)
- Data flow explanations
- Permissions architecture
- Build system documentation
- Security considerations

### docs/API_REFERENCE.md
- Complete reference for 75+ termux-* commands
- Usage examples for every command
- Options and arguments
- Output formats (JSON)
- MobileCLI utility commands

### README.md
- Project overview
- Feature list
- Installation instructions
- Usage guide
- Architecture summary
- Legal information

---

## COMPLETED: Browser Authentication for AI Tools

### Problem
Claude opened browser for authentication, but Gemini and Codex did not.

### Root Causes Found & Fixed

1. **Python Install Order** - Node-gyp needs Python for native modules
   - **Fix:** Install Python BEFORE npm AI tools in SetupWizard

2. **Missing Permission** - Browser URLs need overlay permission
   - **Fix:** Request SYSTEM_ALERT_WINDOW in SetupWizard with user dialog

3. **URL Opening with Absolute Paths** - Node.js `open` package spawns processes without $HOME
   - **Fix:** xdg-open, open, sensible-browser use absolute paths `/data/data/com.termux/files/home/.termux/url_to_open`

4. **DISPLAY Environment Variable** - Gemini/Codex check for DISPLAY before opening browser
   - **Fix:** Added `export DISPLAY=:0` to .bashrc in BootstrapInstaller

### Files Modified
- `SetupWizard.kt` - Python order, overlay permission dialog
- `BootstrapInstaller.kt` - DISPLAY env var, absolute paths in scripts

---

## COMPLETED: Background Setup with Wake Lock

### Problem
Setup would stop if user left the app or screen turned off.

### Solution
- Added `PARTIAL_WAKE_LOCK` (30 min max) during setup
- Changed from `lifecycleScope` to custom `CoroutineScope(SupervisorJob())`
- Added `FLAG_KEEP_SCREEN_ON` to window during setup
- Setup continues even when app is backgrounded

### Files Modified
- `SetupWizard.kt` - setupScope, wake lock, cleanup in finally block

---

## COMPLETED: Text Selection with Context Menu

### Problem
Long press text selection was broken - either got context menu OR selection handles, not both.

### Solution
- Show context menu on long press
- Return `false` from onLongPress to allow TerminalView's selection handles
- Both features now work together

### Files Modified
- `MainActivity.kt` - onLongPress handler, showContextMenu()

---

## UI/UX Polish

### Cards (Stage 3) - FIXED
- Added MATCH_PARENT + isFillViewport
- Cards now fill container consistently

### Developer Mode (7-tap activation)
Hidden features for power users:

| Button | Action | Risk Level |
|--------|--------|------------|
| Reset Terminal Environment | Reinstall bootstrap | HIGH - warn user |
| Refresh AI Tools | Reinstall npm packages | MEDIUM |
| Free Up Space | Clear caches | LOW |
| View Logs | Debug output | LOW |

### Settings Dialog
- Text size (persists)
- Wake lock on startup
- Default AI selection
- Reset app

---

## File Structure

```
MobileCLI-Alpha/
├── app/src/main/java/com/termux/
│   ├── MainActivity.kt        - Terminal UI, drawer, AI launching
│   ├── SetupWizard.kt         - 4-stage setup (legal, perms, download, AI)
│   ├── BootstrapInstaller.kt  - Bootstrap + scripts + CLAUDE.md
│   ├── TermuxApiReceiver.kt   - 75+ API command handlers
│   ├── AmSocketServer.kt      - Fast am commands
│   └── TermuxService.kt       - Background service
├── app/src/main/AndroidManifest.xml - 79 permissions
├── docs/
│   ├── ARCHITECTURE.md        - Technical deep-dive
│   └── API_REFERENCE.md       - All termux-* commands
├── README.md                  - Project overview
├── ROADMAP.md                 - This file
├── IP.md                      - Intellectual property
├── .gitignore                 - Security (excludes local.properties)
└── local.properties.template  - Template for developers
```

---

## Release Checklist

### Before Alpha - DONE
- [x] Fix keystore password exposure
- [x] Remove username from local.properties
- [x] Create .gitignore
- [x] Fix Gemini CLI installation
- [x] Update CLAUDE.md with full capabilities
- [x] Add all 79 permissions
- [x] Test AI tools installation

### Before Beta - DONE
- [x] Privacy policy (in-app dialog + first launch)
- [x] Terms of service (in-app dialog + first launch acceptance)
- [x] Apache 2.0 attributions visible in app (Open Source Licenses menu)
- [x] Legal agreement required on first launch (checkbox + accept)
- [x] Wake Lock and Power Mode controls (drawer menu)
- [x] Browser authentication working for all AI tools (Claude, Gemini, Codex)
- [x] Background setup continues when app is backgrounded or screen off
- [x] Text selection with context menu working
- [x] Display over other apps permission request
- [ ] Test on multiple devices (different Android versions)
- [ ] Performance profiling
- [ ] Battery usage optimization

### Before Release
- [ ] ProGuard/R8 obfuscation
- [ ] Signed release APK
- [ ] App store listing (screenshots, description)
- [ ] Support email/website
- [ ] Version 1.0.0 tag

---

## Commands Reference (Quick)

**MobileCLI Commands:**
- `mobilecli-caps` - Show capabilities
- `mobilecli-rebuild` - Rebuild from source
- `mobilecli-share` - Bluetooth file sharing
- `mobilecli-memory` - AI memory system
- `install-dev-tools` - Install Java/Gradle/SDK
- `selfmod` - Self-modification wizard
- `setup-github` - Configure GitHub

**Key Termux APIs (75+ total):**
- `termux-clipboard-get/set` - Clipboard
- `termux-toast` - Toast notification
- `termux-notification` - System notification
- `termux-open-url` - Open browser
- `termux-camera-photo` - Take photo
- `termux-location` - GPS
- `termux-sms-send/list` - SMS
- `termux-contact-list` - Contacts
- `termux-call-log` - Call history
- `termux-sensor` - All device sensors
- `termux-fingerprint` - Biometric auth
- `termux-bluetooth-*` - Bluetooth
- `termux-nfc` - NFC tags
- `termux-infrared-transmit` - IR blaster
- `termux-tts-speak` - Text to speech
- `termux-battery-status` - Battery info

---

## The Vision

MobileCLI = ROOT WITHOUT ROOT

**What AI can do inside MobileCLI:**
- Access 79 permissions (nearly everything)
- Read /proc, /sys, system files
- Build and install Android apps
- Send SMS, make calls
- Access all sensors
- Control devices via IR/Bluetooth/NFC
- Modify and rebuild itself

**Target Users:**
- Mobile developers
- AI enthusiasts
- Power users who want full control
- Developers without access to desktop

**Revenue Model:** (TBD)
- One-time purchase?
- Subscription?
- Free with premium features?

---

*This roadmap survives conversation compaction. Read this first in new sessions.*
