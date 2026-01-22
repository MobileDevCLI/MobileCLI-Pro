# MobileCLI - Complete System Documentation

**Version:** 2.0.0-beta.22
**Date:** January 20, 2026
**Author:** MobileCLI Team
**Purpose:** Comprehensive technical documentation for lawyers, investors, and developers

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Application Architecture](#2-application-architecture)
3. [Setup Wizard (First Launch)](#3-setup-wizard-first-launch)
4. [Bootstrap Installation System](#4-bootstrap-installation-system)
5. [Terminal Environment](#5-terminal-environment)
6. [API Commands (75+ Commands)](#6-api-commands-75-commands)
7. [Activity Manager (Proprietary IPC)](#7-activity-manager-proprietary-ipc)
8. [URL Opening System](#8-url-opening-system)
9. [User Interface Components](#9-user-interface-components)
10. [Permission System (79 Permissions)](#10-permission-system-79-permissions)
11. [Service Architecture](#11-service-architecture)
12. [AI Integration](#12-ai-integration)
13. [File Structure](#13-file-structure)
14. [Code Ownership Summary](#14-code-ownership-summary)

---

## 1. Executive Summary

MobileCLI is a proprietary Android application that provides an AI-powered terminal environment with root-equivalent access through 79 Android permissions. The application consists of approximately **9,000+ lines of original code** built entirely from scratch by the MobileCLI Team.

### Key Innovations

1. **Proprietary Activity Manager** - Replaced GPL-licensed am.apk with custom IPC system
2. **75+ API Commands** - Original implementation (NOT using Termux:API app)
3. **3-Stage Setup Wizard** - Custom onboarding with legal agreement, permissions, and AI selection
4. **Browser Authentication System** - Overlay-based URL opening for OAuth flows
5. **Session Persistence** - Terminal sessions survive activity destruction
6. **Self-Modification Capability** - App can rebuild itself from within

### Third-Party Components (Apache 2.0 Only)

| Component | Purpose | License |
|-----------|---------|---------|
| terminal-view | Renders terminal text on screen | Apache 2.0 |
| terminal-emulator | Parses VT100 escape codes | Apache 2.0 |

**No GPL code is bundled in the APK.**

---

## 2. Application Architecture

```
MobileCLI Application Flow
==========================

[App Launch]
     │
     ▼
[SetupWizard] ─── First launch only ───┐
     │                                  │
     ├─ Stage 0: Legal Agreement        │
     ├─ Stage 1: Permissions (25+ dangerous)
     ├─ Stage 2: Environment Download   │
     └─ Stage 3: AI Selection           │
                                        │
     ┌──────────────────────────────────┘
     ▼
[MainActivity] ─── Terminal UI
     │
     ├─ TerminalView (Apache 2.0 library)
     ├─ Navigation Drawer
     ├─ Extra Keys (CTRL, ALT, etc.)
     └─ Session Tab Management
     │
     ▼
[TermuxService] ─── Background Service
     │
     ├─ Session Persistence
     ├─ Wake Lock Management
     ├─ WiFi Lock Management
     ├─ Am Socket Server (Proprietary IPC)
     └─ Notification Management
     │
     ▼
[TermuxApiReceiver] ─── 75+ API Commands
     │
     └─ Handles all termux-* shell commands
```

### Key Files and Line Counts

| File | Lines | Purpose |
|------|-------|---------|
| `BootstrapInstaller.kt` | ~2,900 | Bootstrap download, extraction, API scripts |
| `MainActivity.kt` | ~1,440 | Terminal UI, drawer, gestures, session management, Vercel/GitHub CLI |
| `TermuxApiReceiver.kt` | ~1,766 | 75+ API command implementations |
| `SetupWizard.kt` | ~893 | 3-stage setup flow |
| `TermuxService.kt` | ~628 | Background service, wake locks, IPC |
| `AmSocketServer.kt` | ~440 | Proprietary Activity Manager |
| `TermuxUrlHandlerActivity.kt` | ~47 | URL opening with Activity context |
| `TermuxAmDispatcherActivity.kt` | ~66 | Intent dispatching with foreground context |
| **TOTAL** | **~9,000+** | **100% Original Code** |

---

## 3. Setup Wizard (First Launch)

**File:** `app/src/main/java/com/termux/SetupWizard.kt` (~893 lines)

The Setup Wizard is displayed on first launch and guides users through:

### Stage 0: Legal Agreement

```kotlin
// Terms of Service presented to user
- Disclaimer of warranties ("AS IS")
- Limitation of liability
- Assumption of risk (79 permissions, AI actions)
- Indemnification
- Privacy policy
- Checkbox confirmation required
```

**User must check "I agree" and tap "Accept & Continue" to proceed.**

### Stage 1: Permissions

Requests 25+ dangerous permissions across categories:

```kotlin
val REQUIRED_PERMISSIONS = arrayOf(
    // Storage
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE,
    // Camera & Microphone
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    // Location
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    // Contacts & Calendar
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.WRITE_CONTACTS,
    // ... 17 more permissions
)
```

Also requests:
- **Android 12+ Bluetooth permissions** (BLUETOOTH_CONNECT, BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE)
- **Android 13+ permissions** (POST_NOTIFICATIONS, READ_MEDIA_IMAGES/VIDEO/AUDIO)
- **"Display over other apps"** (SYSTEM_ALERT_WINDOW) - Required for browser OAuth

### Stage 2: Full Environment Download

Downloads and installs:
1. **Bootstrap** (~50MB) - bash, coreutils, apt
2. **Node.js** - JavaScript runtime
3. **Python** - Required for native module compilation (node-gyp)
4. **Claude Code** - `@anthropic-ai/claude-code`
5. **Gemini CLI** - `@google/gemini-cli`
6. **Codex CLI** - `@openai/codex`
7. **Java 17** - For Android development
8. **Gradle** - Build automation
9. **Android SDK tools** - aapt, aapt2, d8, apksigner

**Progress shown with percentage bar. Setup continues in background if user leaves app.**

### Stage 3: AI Selection

User selects preferred AI assistant (all are installed):
- **Claude** (Recommended) - Most capable for coding
- **Gemini** - Multimodal research
- **Codex** - Code completion
- **Basic Terminal** - Skip AI launch

Selection saved to SharedPreferences, then MainActivity launched.

---

## 4. Bootstrap Installation System

**File:** `app/src/main/java/com/termux/BootstrapInstaller.kt` (~2,900 lines)

### Download Source

```kotlin
private const val BOOTSTRAP_URL =
    "https://github.com/termux/termux-packages/releases/download/bootstrap-2026.01.04-r1%2Bapt.android-7/bootstrap-aarch64.zip"
```

### Directory Structure

```
/data/data/com.termux/files/
├── usr/                    # PREFIX directory
│   ├── bin/               # Executables (bash, apt, node, python, etc.)
│   ├── lib/               # Libraries
│   ├── etc/               # Configuration files
│   │   ├── passwd         # User info (created by MobileCLI)
│   │   ├── group          # Group info (created by MobileCLI)
│   │   ├── hosts          # DNS hosts file
│   │   └── resolv.conf    # DNS resolver config
│   ├── share/             # Shared resources
│   ├── var/               # Variable data
│   └── tmp/               # Temporary files
└── home/                   # HOME directory (~/)
    ├── .bashrc            # Shell configuration
    ├── .profile           # Login profile
    ├── CLAUDE.md          # AI briefing document
    ├── .gyp/              # Native module build config
    │   └── include.gypi
    ├── .mobilecli/        # AI memory system
    │   ├── memory/
    │   │   ├── evolution_history.json
    │   │   ├── problems_solved.json
    │   │   ├── capabilities.json
    │   │   └── goals.json
    │   └── config/
    │       └── preferences.json
    └── .termux/           # Termux config
        └── termux.properties
```

### Installation Process

```kotlin
suspend fun install(): Boolean {
    // 1. Prepare directories
    prepareDirectories()

    // 2. Download bootstrap (~50MB)
    val zipFile = downloadBootstrap()

    // 3. Extract files and create symlinks
    extractBootstrap(zipFile)

    // 4. Set permissions (chmod 755)
    setPermissions()

    // 5. Install proprietary TermuxAm (replaces GPL am.apk)
    installTermuxAm()

    // 6. Install 75+ API scripts
    installApiScripts()

    // 7. Configure npm with SSL certificates
    createNpmConfig()

    // 8. Set up GitHub configuration
    createGitHubConfig()

    // 9. Initialize AI memory system
    initializePersistentMemory()

    // 10. Write version marker
    File(prefixDir, ".mobilecli_version").writeText(BOOTSTRAP_VERSION)
}
```

### API Scripts Generation

Each `termux-*` command is a generated shell script:

```bash
#!/data/data/com.termux/files/usr/bin/bash
# Generated by MobileCLI - NOT using Termux:API app

# Parse arguments and call TermuxApiReceiver via broadcast
am broadcast -a com.termux.api.API_CALL \
    --es api_method "clipboard-get" \
    --es api_args "$ARGS" \
    --es result_file "$RESULT_FILE"
```

**Key Innovation:** These scripts communicate directly with our `TermuxApiReceiver` BroadcastReceiver, NOT the separate Termux:API app.

---

## 5. Terminal Environment

### Unique Implementation Features

1. **Custom Environment Variables:**
```kotlin
fun getEnvironment(): Array<String> = arrayOf(
    "TERM=xterm-256color",
    "HOME=${homeDir.absolutePath}",
    "PREFIX=${prefixDir.absolutePath}",
    "PATH=${prefixDir.absolutePath}/bin:/system/bin",
    "LD_LIBRARY_PATH=${prefixDir.absolutePath}/lib",
    "LANG=en_US.UTF-8",
    "SHELL=${prefixDir.absolutePath}/bin/bash",
    "COLORTERM=truecolor",
    "DISPLAY=:0",  // Enables browser launching for OAuth
    "BROWSER=termux-open-url",  // Critical for AI CLI authentication
    "TERMUX_VERSION=0.118",
    "TERMUX_APK_RELEASE=MOBILECLI_ALPHA"
)
```

2. **Session Persistence:**
   - Sessions managed by `TermuxService` (not Activity)
   - Survive activity destruction and recreation
   - Reconnect when user returns to app

3. **Shell Configuration (`.bashrc`):**
```bash
# MobileCLI bashrc
export PREFIX="/data/data/com.termux/files/usr"
export HOME="/data/data/com.termux/files/home"
export DISPLAY=:0  # Enables browser launching

# Welcome message
echo "  ╔═══════════════════════════════════════╗"
echo "  ║       Welcome to MobileCLI            ║"
echo "  ╚═══════════════════════════════════════╝"
```

4. **Login Shell:**
```kotlin
val shell = File(bootstrapInstaller.prefixDir, "bin/login").absolutePath
val session = termuxService?.createSession(
    shell,
    cwd.absolutePath,
    arrayOf(shell, "-l"),
    env,
    this
)
```

---

## 6. API Commands (75+ Commands)

**File:** `app/src/main/java/com/termux/TermuxApiReceiver.kt` (~1,766 lines)

### Implementation Architecture

```
Shell Script (termux-*)
        │
        ▼
   am broadcast
        │
        ▼
TermuxApiReceiver.onReceive()
        │
        ▼
   Method dispatch
        │
        ▼
  Android APIs
        │
        ▼
  JSON response
        │
        ▼
 Result file written
```

### Complete API List

#### Clipboard
| Command | Function | Description |
|---------|----------|-------------|
| `termux-clipboard-get` | `clipboardGet()` | Read clipboard contents |
| `termux-clipboard-set` | `clipboardSet()` | Write to clipboard |

#### Notifications
| Command | Function | Description |
|---------|----------|-------------|
| `termux-toast` | `showToast()` | Show toast message |
| `termux-notification` | `showNotification()` | Send system notification |
| `termux-notification-remove` | `removeNotification()` | Cancel notification |
| `termux-notification-list` | `notificationList()` | List active notifications |

#### Device Info
| Command | Function | Description |
|---------|----------|-------------|
| `termux-battery-status` | `batteryStatus()` | Battery level, charging status |
| `termux-vibrate` | `vibrate()` | Vibrate device |
| `termux-brightness` | `getBrightness()` | Get screen brightness |
| `termux-brightness-set` | `setBrightness()` | Set screen brightness |
| `termux-torch` | `toggleTorch()` | Flashlight on/off |
| `termux-volume` | `getVolume()` | Get volume levels |
| `termux-volume-set` | `setVolume()` | Set volume level |

#### Network & WiFi
| Command | Function | Description |
|---------|----------|-------------|
| `termux-wifi-connectioninfo` | `wifiInfo()` | Current WiFi connection |
| `termux-wifi-enable` | `wifiEnable()` | Enable/disable WiFi |
| `termux-wifi-scaninfo` | `wifiScanInfo()` | Scan available networks |

#### Location
| Command | Function | Description |
|---------|----------|-------------|
| `termux-location` | `getLocation()` | GPS coordinates |

#### Camera & Media
| Command | Function | Description |
|---------|----------|-------------|
| `termux-camera-info` | `cameraInfo()` | List cameras |
| `termux-camera-photo` | `cameraPhoto()` | Take photo |
| `termux-media-scan` | `mediaScan()` | Add file to media library |
| `termux-media-player` | `mediaPlayerControl()` | Play/pause/stop audio |
| `termux-microphone-record` | `microphoneRecord()` | Record audio |
| `termux-audio-info` | `audioInfo()` | Audio device info |

#### Text-to-Speech
| Command | Function | Description |
|---------|----------|-------------|
| `termux-tts-engines` | `ttsEngines()` | List TTS engines |
| `termux-tts-speak` | `ttsSpeak()` | Speak text aloud |

#### Telephony & SMS
| Command | Function | Description |
|---------|----------|-------------|
| `termux-telephony-call` | `telephonyCall()` | Make phone call |
| `termux-telephony-cellinfo` | `telephonyCellInfo()` | Cell tower info |
| `termux-telephony-deviceinfo` | `telephonyDeviceInfo()` | Device phone info |
| `termux-sms-list` | `smsList()` | Read SMS messages |
| `termux-sms-send` | `smsSend()` | Send SMS message |

#### Contacts & Calendar
| Command | Function | Description |
|---------|----------|-------------|
| `termux-contact-list` | `contactList()` | List all contacts |
| `termux-call-log` | `callLog()` | Call history |

#### Sensors
| Command | Function | Description |
|---------|----------|-------------|
| `termux-sensor` | `sensorInfo()` | List/read sensors |

#### Biometric
| Command | Function | Description |
|---------|----------|-------------|
| `termux-fingerprint` | `fingerprintAuth()` | Fingerprint authentication |

#### Infrared
| Command | Function | Description |
|---------|----------|-------------|
| `termux-infrared-frequencies` | `infraredFrequencies()` | IR emitter frequency ranges |
| `termux-infrared-transmit` | `infraredTransmit()` | Send IR signal |

#### USB
| Command | Function | Description |
|---------|----------|-------------|
| `termux-usb` | `usbInfo()` | USB device info |

#### NFC
| Command | Function | Description |
|---------|----------|-------------|
| `termux-nfc` | `nfcInfo()` | NFC adapter status |

#### Bluetooth
| Command | Function | Description |
|---------|----------|-------------|
| `termux-bluetooth-info` | `bluetoothInfo()` | Adapter info |
| `termux-bluetooth-enable` | `bluetoothEnable()` | Enable/disable |
| `termux-bluetooth-scaninfo` | `bluetoothScanInfo()` | Scan devices |
| `termux-bluetooth-connect` | `bluetoothConnect()` | Connect to device |
| `termux-bluetooth-paired` | `bluetoothPaired()` | List paired devices |

#### System
| Command | Function | Description |
|---------|----------|-------------|
| `termux-wallpaper` | `setWallpaper()` | Set wallpaper |
| `termux-download` | `download()` | Download file |
| `termux-share` | `share()` | Share content |
| `termux-dialog` | `showDialog()` | Show dialog |
| `termux-storage-get` | `storageGet()` | File picker |
| `termux-job-scheduler` | `jobScheduler()` | Schedule jobs |
| `termux-open-url` | `openUrl()` | Open URL in browser |
| `termux-wake-lock` | `wakeLock()` | Acquire/release wake lock |

#### Android Keystore
| Command | Function | Description |
|---------|----------|-------------|
| `termux-keystore-list` | `keystoreList()` | List keystore entries |
| `termux-keystore-generate` | `keystoreGenerate()` | Generate key |
| `termux-keystore-delete` | `keystoreDelete()` | Delete key |
| `termux-keystore-sign` | `keystoreSign()` | Sign data |
| `termux-keystore-verify` | `keystoreVerify()` | Verify signature |

#### Storage Access Framework (SAF)
| Command | Function | Description |
|---------|----------|-------------|
| `termux-saf-ls` | `safList()` | List directory |
| `termux-saf-stat` | `safStat()` | File info |
| `termux-saf-read` | `safRead()` | Read file |
| `termux-saf-write` | `safWrite()` | Write file |
| `termux-saf-mkdir` | `safMkdir()` | Create directory |
| `termux-saf-rm` | `safRemove()` | Delete file |
| `termux-saf-create` | `safCreate()` | Create file |
| `termux-saf-managedir` | `safManageDir()` | Select directory |
| `termux-saf-dirs` | `safDirs()` | List SAF directories |

#### Speech Recognition
| Command | Function | Description |
|---------|----------|-------------|
| `termux-speech-to-text` | `speechToText()` | Voice recognition |

#### MobileCLI Custom Commands
| Command | Description |
|---------|-------------|
| `mobilecli-caps` | Show all capabilities |
| `mobilecli-memory` | AI memory system |
| `mobilecli-rebuild` | Rebuild app from source |
| `mobilecli-share` | Bluetooth file sharing |
| `install-dev-tools` | Install Java, Gradle, SDK |
| `setup-github` | Configure GitHub credentials |

---

## 7. Activity Manager (Proprietary IPC)

**Files:**
- `app/src/main/java/com/termux/am/AmSocketServer.kt` (~440 lines)
- `app/src/main/java/com/termux/app/TermuxService.kt` (command parsing)

### Why This Exists

The original Termux uses `am.apk` from AOSP, which is GPL-licensed. To avoid GPL contamination, MobileCLI implements a **proprietary Activity Manager** using:

1. **Socket Server** - Listens for commands on a Unix domain socket
2. **File-Based IPC** - Fallback using file polling
3. **Intent Parser** - Parses `am start` command-line arguments

### Socket Server Implementation

```kotlin
class AmSocketServer(private val context: Context) {
    companion object {
        const val SOCKET_PATH = "/data/data/com.termux/files/home/.termux/am.sock"
    }

    fun start(): Boolean {
        // Create Unix domain socket
        // Listen for "am start", "am broadcast", "am startservice" commands
        // Execute with app's permissions (not shell's limited permissions)
    }
}
```

### File-Based IPC (Fallback)

```kotlin
// In TermuxService.kt
private val commandFile: File by lazy { File(termuxDir, "am_command") }
private val resultFile: File by lazy { File(termuxDir, "am_result") }

// Polls every 200ms for commands
if (commandFile.exists()) {
    val command = commandFile.readText().trim()
    commandFile.delete()
    val result = executeAmCommand(command)
    resultFile.writeText(result)
}
```

### Command Parsing

```kotlin
private fun parseIntent(args: List<String>): Intent {
    val intent = Intent()
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-a" -> intent.action = args[++i]           // Action
            "-d" -> intent.data = Uri.parse(args[++i])  // Data URI
            "-n" -> /* parse component */               // Component
            "-e", "-es" -> /* parse string extra */     // Extras
            "-ei" -> /* parse int extra */
            "-ez" -> /* parse boolean extra */
            "-f" -> intent.flags = args[++i].toInt()    // Flags
        }
        i++
    }
    return intent
}
```

### Why This Matters for IP

**This is a KEY INNOVATION for licensing:**
- Termux's am.apk is GPL-licensed (derived from AOSP)
- Our Activity Manager is 100% original proprietary code
- No GPL contamination in MobileCLI

---

## 8. URL Opening System

**Files:**
- `app/src/main/java/com/termux/TermuxUrlHandlerActivity.kt` (~47 lines)
- `app/src/main/java/com/termux/TermuxAmDispatcherActivity.kt` (~66 lines)

### The Problem

Android 10+ blocks starting activities from background contexts. When AI CLIs need to open OAuth URLs from the terminal, they fail because the terminal runs in a background service.

### The Solution: Overlay System

MobileCLI uses the **SYSTEM_ALERT_WINDOW** permission to create a foreground Activity context:

```kotlin
// TermuxUrlHandlerActivity.kt
class TermuxUrlHandlerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        if (uri != null) {
            val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(browserIntent)
        }
        finish()
    }
}
```

### Intent Dispatcher

For generic intent dispatching:

```kotlin
// TermuxAmDispatcherActivity.kt
class TermuxAmDispatcherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetAction = intent.getStringExtra(EXTRA_TARGET_ACTION)
        val targetData = intent.getStringExtra(EXTRA_TARGET_DATA)

        val dispatchIntent = Intent(targetAction).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (targetData != null) {
                data = Uri.parse(targetData)
            }
        }
        startActivity(dispatchIntent)
        finish()
    }
}
```

### Shell Script Integration

```bash
# termux-open-url script
#!/data/data/com.termux/files/usr/bin/bash
URL="$1"
am start -n "com.termux/.TermuxUrlHandlerActivity" -d "$URL"
```

---

## 9. User Interface Components

**File:** `app/src/main/java/com/termux/MainActivity.kt` (~1,400 lines)

### Main UI Elements

1. **Terminal View** - Full-screen terminal emulator
2. **Navigation Drawer** - Left-side slide-out menu
3. **Extra Keys Row** - CTRL, ALT, TAB, etc. above keyboard
4. **Session Tabs** - Multi-session management

### Navigation Drawer Items

| Item | Function |
|------|----------|
| New Session | Create additional terminal session |
| Close Session | End current session |
| Wake Lock | Toggle CPU wake lock |
| Power Mode | Enable autonomous AI execution |
| Settings | Open settings activity |
| Claude | Launch Claude Code |
| Gemini | Launch Gemini CLI |
| Codex | Launch Codex CLI |
| Install AI Tools | Reinstall Claude/Gemini/Codex |
| Vercel CLI | One-tap install + OAuth login (opens new tab) |
| GitHub CLI | One-tap install gh + git + OAuth (opens new tab) |
| Help | Show usage help |
| Open Source Licenses | Show third-party licenses |
| About | Version info, developer mode |

### Extra Keys

```kotlin
// Extra keys displayed above keyboard
- ESC, CTRL, ALT, TAB
- -, /, |, HOME, END
- UP, DOWN, LEFT, RIGHT
```

### Gesture Support

- **Swipe right** - Open navigation drawer
- **Swipe left** - Close navigation drawer
- **Pinch zoom** - Adjust text size
- **Long press** - Text selection

### Session Management

```kotlin
// Sessions persist in TermuxService
private val sessions: List<TerminalSession>
    get() = termuxService?.getSessions() ?: emptyList()

// Reconnect when activity recreated
private fun reconnectToExistingSessions() {
    val existingSessions = sessions
    if (existingSessions.isEmpty()) {
        createSession()
        return
    }
    terminalView.attachSession(existingSessions[currentSessionIndex])
}
```

---

## 10. Permission System (79 Permissions)

**File:** `app/src/main/AndroidManifest.xml` (406 lines)

### Permission Categories

#### Basic Permissions (Auto-granted)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.VIBRATE" />
```

#### System Alert Window (Critical for URL Opening)
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

#### Storage
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
<!-- Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
```

#### Camera & Microphone
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

#### Location
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

#### Telephony & SMS
```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_NUMBERS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.WRITE_CALL_LOG" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.RECEIVE_MMS" />
```

#### Contacts & Calendar
```xml
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.WRITE_CONTACTS" />
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />
```

#### Bluetooth (Android 12+)
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
```

#### Sensors
```xml
<uses-permission android:name="android.permission.BODY_SENSORS" />
<uses-permission android:name="android.permission.BODY_SENSORS_BACKGROUND" />
<uses-permission android:name="android.permission.HIGH_SAMPLING_RATE_SENSORS" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
```

#### Biometrics
```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```

#### Network
```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

#### NFC
```xml
<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.NFC_TRANSACTION_EVENT" />
```

#### Infrared
```xml
<uses-permission android:name="android.permission.TRANSMIT_IR" />
```

#### System
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SET_WALLPAPER" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

---

## 11. Service Architecture

**File:** `app/src/main/java/com/termux/app/TermuxService.kt` (~628 lines)

### Purpose

The `TermuxService` is a foreground service that:
1. Manages terminal sessions (survives activity destruction)
2. Provides wake lock functionality
3. Runs the proprietary Activity Manager IPC
4. Shows persistent notification

### Service Lifecycle

```kotlin
class TermuxService : Service() {
    override fun onCreate() {
        instance = this
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startAmSocketServer()      // Proprietary IPC
        startCommandWatcher()      // File-based IPC fallback
    }

    override fun onDestroy() {
        stopCommandWatcher()
        stopAmSocketServer()
        releaseWakeLock()
        sessions.forEach { it.finishIfRunning() }
    }
}
```

### Wake Lock Management

```kotlin
fun acquireWakeLock() {
    // CPU wake lock
    wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "MobileCLI::TermuxWakeLock"
    ).apply { acquire() }

    // WiFi lock
    wifiLock = wifiManager.createWifiLock(
        WifiManager.WIFI_MODE_FULL_HIGH_PERF,
        "MobileCLI::TermuxWifiLock"
    ).apply { acquire() }
}
```

### Session Persistence

```kotlin
// Sessions stored in service, not activity
private val sessions = mutableListOf<TerminalSession>()

fun createSession(...): TerminalSession? {
    val session = TerminalSession(shell, cwd, args, env, transcriptRows, client)
    sessions.add(session)
    return session
}

fun hasExistingSessions(): Boolean = sessions.isNotEmpty()
```

---

## 12. AI Integration

### Supported AI Assistants

| AI | Package | Provider |
|----|---------|----------|
| Claude Code | `@anthropic-ai/claude-code` | Anthropic |
| Gemini CLI | `@google/gemini-cli` | Google |
| Codex CLI | `@openai/codex` | OpenAI |

### Launch Commands

```bash
claude    # Start Claude Code
gemini    # Start Gemini CLI
codex     # Start Codex CLI
```

### OAuth Authentication

AI CLIs require browser-based OAuth authentication. MobileCLI enables this through:

1. **BROWSER environment variable:**
```kotlin
"BROWSER=termux-open-url"
```

2. **DISPLAY environment variable:**
```kotlin
"DISPLAY=:0"  // Signals to CLIs that display is available
```

3. **Overlay permission (SYSTEM_ALERT_WINDOW):**
   - Allows opening browser from background context

### CLAUDE.md

A comprehensive AI briefing document is created at `~/CLAUDE.md` with:
- All 79 permissions documented
- Available API commands
- Filesystem access paths
- Build tools available
- Self-modification instructions
- Known workarounds

---

## 13. File Structure

```
MobileCLI-CLEAN/
├── app/
│   ├── src/main/
│   │   ├── java/com/termux/
│   │   │   ├── MainActivity.kt              # Terminal UI (~1,400 lines)
│   │   │   ├── SetupWizard.kt               # 3-stage setup (~893 lines)
│   │   │   ├── BootstrapInstaller.kt        # Bootstrap system (~2,900 lines)
│   │   │   ├── TermuxApiReceiver.kt         # 75+ APIs (~1,766 lines)
│   │   │   ├── TermuxUrlHandlerActivity.kt  # URL opener (~47 lines)
│   │   │   ├── TermuxAmDispatcherActivity.kt # Intent dispatcher (~66 lines)
│   │   │   ├── TermuxApplication.kt         # Application class
│   │   │   ├── am/
│   │   │   │   └── AmSocketServer.kt        # Proprietary AM (~440 lines)
│   │   │   ├── app/
│   │   │   │   ├── TermuxService.kt         # Background service (~628 lines)
│   │   │   │   ├── TermuxOpenReceiver.kt    # File sharing
│   │   │   │   └── RunCommandService.kt     # External command execution
│   │   │   ├── activities/
│   │   │   │   └── SettingsActivity.kt      # Settings screen
│   │   │   ├── boot/
│   │   │   │   └── BootReceiver.kt          # Boot completed receiver
│   │   │   └── filepicker/
│   │   │       ├── TermuxFileReceiverActivity.kt
│   │   │       └── TermuxDocumentsProvider.kt
│   │   ├── AndroidManifest.xml              # 79 permissions, activities, services
│   │   └── res/
│   │       ├── layout/
│   │       │   └── activity_main.xml        # Terminal layout
│   │       └── values/
│   │           ├── strings.xml
│   │           └── styles.xml
│   └── build.gradle                         # App build config
├── build.gradle                             # Project build config
├── settings.gradle                          # Project settings
│
├── LICENSE                                  # Proprietary license
├── THIRD_PARTY_LICENSES.md                  # Apache 2.0 components
├── LEGAL_SUMMARY.md                         # Executive legal summary
├── IP.md                                    # IP documentation
├── README.md                                # Project readme
└── COMPLETE_SYSTEM_DOCUMENTATION.md         # This document
```

---

## 14. Code Ownership Summary

### Original Code (100% MobileCLI Team)

| Component | Lines | Description |
|-----------|-------|-------------|
| BootstrapInstaller.kt | ~2,900 | Download, extraction, scripts, memory |
| TermuxApiReceiver.kt | ~1,766 | 75+ API command implementations |
| MainActivity.kt | ~1,400 | Terminal UI, drawer, sessions |
| SetupWizard.kt | ~893 | 3-stage onboarding flow |
| TermuxService.kt | ~628 | Background service, wake locks |
| AmSocketServer.kt | ~440 | Proprietary Activity Manager (replaces GPL) |
| TermuxAmDispatcherActivity.kt | ~66 | Intent dispatcher |
| TermuxUrlHandlerActivity.kt | ~47 | URL opener |
| Other files | ~800 | Activities, receivers, utilities |
| **TOTAL** | **~9,000+** | **100% Original** |

### Third-Party Libraries (Apache 2.0)

| Library | Source | Purpose |
|---------|--------|---------|
| terminal-view | Termux/termux-app | Android View for terminal |
| terminal-emulator | Termux/termux-app | VT100 escape code parsing |
| AndroidX | Google | Android support libraries |
| Kotlin Coroutines | JetBrains | Async programming |

**All third-party components are Apache 2.0 licensed, which permits:**
- Commercial use
- Closed-source distribution
- Modification
- Sublicensing

### GPL Components

**NONE bundled in APK.**

The GPL-licensed `am.apk` from Termux was replaced with proprietary `AmSocketServer.kt`.

Runtime downloads (bash, coreutils, etc.) are downloaded by the user from Termux repositories and are not bundled.

---

## Contact

**Website:** https://mobilecli.com
**GitHub:** https://github.com/MobileDevCLI
**Email:** Contact through website

---

**Copyright (c) 2026 MobileCLI Team. All Rights Reserved.**

*This document is confidential and proprietary. Distribution is restricted.*
