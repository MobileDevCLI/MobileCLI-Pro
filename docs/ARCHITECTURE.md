# MobileCLI Architecture

Technical documentation for developers and contributors.

---

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        MobileCLI App                            │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ SetupWizard │  │ MainActivity │  │    TermuxService       │  │
│  │             │  │              │  │    (Background)        │  │
│  │ - Legal     │  │ - Terminal   │  │                        │  │
│  │ - Perms     │  │ - Drawer     │  │ - Session management   │  │
│  │ - Bootstrap │  │ - AI launch  │  │ - Process lifecycle    │  │
│  │ - AI choice │  │ - Settings   │  │ - Wake lock            │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                  BootstrapInstaller                         ││
│  │  - Downloads Termux bootstrap (~50MB)                       ││
│  │  - Extracts to /data/data/com.termux/files/                 ││
│  │  - Creates 75+ termux-* API scripts                         ││
│  │  - Generates CLAUDE.md for AI context                       ││
│  │  - Sets up environment variables                            ││
│  └─────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                  TermuxApiReceiver                          ││
│  │  - Receives broadcasts from termux-* scripts                ││
│  │  - Executes API calls (camera, SMS, location, etc.)         ││
│  │  - Returns results via file IPC                             ││
│  └─────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────┤
│                     Android System                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ Camera   │ │ Location │ │ Telephony│ │ Bluetooth│  ...      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Details

### SetupWizard.kt

**Purpose:** First-time setup flow with 4 stages.

**Stages:**
1. **Stage 0 - Legal Agreement**
   - Shows Terms of Service summary
   - Requires checkbox acceptance
   - Saves acceptance to SharedPreferences

2. **Stage 1 - Permissions**
   - Requests 30+ dangerous runtime permissions
   - Handles Android 13+ specific permissions (notifications, media)
   - Graceful handling of denied permissions

3. **Stage 2 - Bootstrap Download**
   - Downloads ~50MB Termux bootstrap from GitHub
   - Shows progress bar with status messages
   - **Critical:** Runs login shell to trigger bootstrap second stage
   - Installs Node.js, Claude, Gemini, Codex, Python, Java, Gradle

4. **Stage 3 - AI Selection**
   - Card-based UI for choosing default AI
   - Options: Claude (recommended), Gemini, Codex, Basic Terminal
   - Launches MainActivity with selected AI

**Key Functions:**
```kotlin
private suspend fun runBashCommand(command: String): Boolean
private suspend fun runLoginShell(): Boolean  // Triggers bootstrap second stage
private fun requestPermissions()
```

---

### MainActivity.kt

**Purpose:** Main terminal interface with navigation drawer.

**Features:**
- Terminal emulator (TerminalView)
- Navigation drawer with settings
- AI launching
- Wake lock / Power mode toggles
- Session management (multi-tab)
- URL watching (for `termux-open-url`)

**Key Components:**

```kotlin
// Terminal session management
private var session: TerminalSession? = null
private val sessions = mutableListOf<TerminalSession>()
private fun createSession()

// AI launching
private fun launchAI(ai: String) {
    val command = when (ai) {
        AI_CLAUDE -> "claude\n"
        AI_GEMINI -> "gemini\n"
        AI_CODEX -> "codex\n"
    }
    session?.write(command.toByteArray())
}

// Drawer controls
private fun toggleWakeLock()
private fun togglePowerMode()
private fun showLicenses()
private fun showPrivacyPolicy()
private fun showTermsOfService()
```

**URL Watcher:**
```kotlin
// Polls ~/.termux/url_to_open for URLs to open
// This allows shell scripts to open URLs via the app's context
private fun setupUrlWatcher()
```

---

### BootstrapInstaller.kt

**Purpose:** Core installation and environment setup.

**Responsibilities:**
1. Download Termux bootstrap from GitHub releases
2. Extract to app's files directory
3. Create all termux-* API scripts
4. Generate CLAUDE.md for AI context
5. Set up shell configuration (.bashrc, .profile)
6. Create MobileCLI helper scripts

**Directory Structure Created:**
```
/data/data/com.termux/files/
├── usr/
│   ├── bin/           # Executables (bash, node, python, termux-*)
│   ├── lib/           # Libraries
│   ├── etc/           # Configuration
│   └── tmp/           # Temporary files
└── home/
    ├── .bashrc        # Shell configuration
    ├── .profile       # Login profile
    ├── .termux/       # Termux config, power_mode flag
    ├── .mobilecli/    # Memory system, capabilities
    └── CLAUDE.md      # AI briefing document
```

**API Script Template:**
```kotlin
fun createApiScript(name: String, method: String) {
    val script = File(binDir, name)
    script.writeText("""#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI API: $name
RESULT_FILE="${tmpDir.absolutePath}/api_result_$$"
am broadcast -n com.termux/com.termux.TermuxApiReceiver -a com.termux.api.API_CALL \
    --es api_method "$method" \
    --es api_args "$@" \
    --es result_file "$RESULT_FILE"
sleep 0.3
if [ -f "$RESULT_FILE" ]; then
    cat "$RESULT_FILE"
    rm -f "$RESULT_FILE"
fi
""")
    script.setExecutable(true)
}
```

**Key Points:**
- Uses `-n com.termux/com.termux.TermuxApiReceiver` for explicit broadcast (required for Android 14+)
- Contains `# MobileCLI API:` comment (used for self-healing detection)
- Result file path uses app's tmp directory

**Self-Healing Mechanism (v2.0.0-beta.34+):**
```kotlin
// Called on every app startup from MainActivity.onCreate()
fun regenerateApiScriptsIfNeeded() {
    if (!areApiScriptsValid()) {
        installApiScripts()  // Regenerates all 75+ scripts
    }
}

fun areApiScriptsValid(): Boolean {
    val testScript = File(binDir, "termux-battery-status")
    return testScript.readText().contains("MobileCLI API:")
}
```

**WARNING:** Do NOT install `termux-api` package - it overwrites MobileCLI's scripts with incompatible ones. The self-healing mechanism will restore them on next app launch.

---

### TermuxApiReceiver.kt

**Purpose:** Handles all termux-* API command execution.

**Flow:**
```
Shell script (termux-camera-photo)
    ↓
am broadcast -a com.termux.api.API_CALL
    ↓
TermuxApiReceiver.onReceive()
    ↓
Execute API method (take photo)
    ↓
Write result to file
    ↓
Shell script reads result
```

**Implemented APIs:**
```kotlin
when (method) {
    // Clipboard
    "clipboard-get" -> getClipboard(context)
    "clipboard-set" -> setClipboard(context, args)

    // Notifications
    "toast" -> showToast(context, args)
    "notification" -> sendNotification(context, args)
    "vibrate" -> vibrate(context, args)

    // Camera
    "camera-info" -> getCameraInfo(context)
    "camera-photo" -> takePhoto(context, args)

    // Location
    "location" -> getLocation(context)

    // Telephony
    "telephony-deviceinfo" -> getTelephonyDeviceInfo(context)
    "telephony-cellinfo" -> getTelephonyCellInfo(context)
    "sms-list" -> listSms(context, args)
    "sms-send" -> sendSms(context, args)

    // Bluetooth
    "bluetooth-info" -> bluetoothInfo(context)
    "bluetooth-paired" -> bluetoothPaired(context)
    "bluetooth-scaninfo" -> bluetoothScanInfo(context)

    // Sensors
    "sensor" -> getSensorData(context, args)
    "battery-status" -> getBatteryStatus(context)

    // ... 60+ more APIs
}
```

---

### TermuxService.kt

**Purpose:** Background service for terminal session management.

**Responsibilities:**
- Keep terminal sessions alive when app is in background
- Handle wake lock acquisition/release
- Process `am` commands from shell
- Manage notification for foreground service

**Wake Lock:**
```kotlin
private var wakeLock: PowerManager.WakeLock? = null

fun acquireWakeLock() {
    wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "MobileCLI:WakeLock"
    )
    wakeLock?.acquire()
}
```

---

## Data Flow

### Terminal Command Execution

```
User types: termux-camera-photo -o photo.jpg
    ↓
Bash executes: /data/data/com.termux/files/usr/bin/termux-camera-photo
    ↓
Script sends: am broadcast -a com.termux.api.API_CALL
    ↓
TermuxApiReceiver.onReceive() triggered
    ↓
takePhoto() called with args "-o photo.jpg"
    ↓
Camera API used to capture photo
    ↓
Result written to /tmp/api_result_12345
    ↓
Script reads result and outputs to terminal
```

### AI Launch Flow

```
User clicks "Claude" in SetupWizard Stage 3
    ↓
SetupWizard saves AI_CLAUDE to intent
    ↓
MainActivity receives intent
    ↓
launchAI(AI_CLAUDE) called
    ↓
session.write("claude\n")
    ↓
Claude CLI launches in terminal
```

---

## Permissions Architecture

### Permission Categories

**Dangerous Permissions (Runtime):**
```xml
<!-- Storage -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

<!-- Camera -->
<uses-permission android:name="android.permission.CAMERA"/>

<!-- Location -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>

<!-- Microphone -->
<uses-permission android:name="android.permission.RECORD_AUDIO"/>

<!-- Phone -->
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.CALL_PHONE"/>
<uses-permission android:name="android.permission.READ_CALL_LOG"/>

<!-- SMS -->
<uses-permission android:name="android.permission.SEND_SMS"/>
<uses-permission android:name="android.permission.READ_SMS"/>

<!-- Contacts -->
<uses-permission android:name="android.permission.READ_CONTACTS"/>

<!-- ... 70+ more -->
```

**Normal Permissions (Auto-granted):**
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.VIBRATE"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
```

---

## Security Considerations

### Sandboxing
- All code runs within Android's app sandbox
- No actual root access - uses permission system
- Cannot access other apps' private data
- Cannot modify system partition

### Credential Protection
- `local.properties` excluded from git
- Keystore passwords never hardcoded
- GitHub tokens stored in user's home directory

### AI Safety
- Power Mode requires explicit user activation
- Users warned about AI capabilities in Terms of Service
- All AI actions logged to terminal (auditable)

---

## Build System

### Gradle Configuration

```kotlin
// app/build.gradle.kts
android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.termux"
        minSdk = 24
        targetSdk = 28  // Keeps legacy storage access
    }

    signingConfigs {
        create("release") {
            storeFile = file("../mobilecli-release.keystore")
            storePassword = localProperties["KEYSTORE_PASSWORD"]
            keyAlias = localProperties["KEY_ALIAS"]
            keyPassword = localProperties["KEY_PASSWORD"]
        }
    }
}
```

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore)
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug
```

---

## Testing

### Manual Testing Checklist

- [ ] Fresh install shows legal agreement
- [ ] All permissions can be granted
- [ ] Bootstrap downloads successfully
- [ ] AI tools install without errors
- [ ] No terminal output visible during setup
- [ ] Claude/Gemini/Codex launch cleanly
- [ ] Wake lock toggle works
- [ ] Power mode toggle works
- [ ] All drawer menu items work
- [ ] termux-* commands execute correctly

### Device Compatibility

| Android Version | Status |
|----------------|--------|
| Android 7 (API 24) | Minimum supported |
| Android 8-9 | Fully tested |
| Android 10-12 | Fully tested |
| Android 13+ | Requires additional permissions |

---

## Future Architecture

### Planned Improvements

1. **Modular API System**
   - Move each API to separate class
   - Plugin architecture for new APIs

2. **Background Task Queue**
   - Queue long-running tasks
   - Progress reporting to terminal

3. **Secure Storage**
   - Android Keystore for secrets
   - Encrypted SharedPreferences

4. **Multi-Process Architecture**
   - Separate process for heavy tasks
   - Better memory management
