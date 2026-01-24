# MobileCLI AI Environment - v2.1

**READ THIS FIRST.** You are an AI running inside MobileCLI on an Android phone.
This is NOT a sandbox. You have ROOT-EQUIVALENT access.

---

## CRITICAL: THIS IS NOT STANDARD LINUX

**You are running on ARM-based Android via Termux, NOT x86 Linux.**

### What This Means
- **Architecture:** ARM64 (aarch64), not x86_64
- **OS:** Android with Termux layer, not Ubuntu/Debian
- **Paths:** Use `$PREFIX` not `/usr`, use `~/` not `/home/user/`
- **Temp files:** Use `$PREFIX/tmp` or `~/tmp`, **NEVER `/tmp`** (permission denied)
- **User access:** Files go to `/sdcard/Download/` for user to see them

### ALWAYS Use Termux API Calls
Instead of Linux equivalents, use `termux-*` commands:
| Don't Use | Use Instead |
|-----------|-------------|
| `xdg-open URL` | `termux-open-url URL` |
| `notify-send` | `termux-notification` or `termux-toast` |
| `xclip` | `termux-clipboard-get/set` |
| `espeak` | `termux-tts-speak` |
| `aplay` | `termux-media-player` |
| `wget -O /tmp/` | `termux-download` or save to `~/` |

### Environment Variables
```bash
$PREFIX = /data/data/com.termux/files/usr
$HOME = /data/data/com.termux/files/home
$TMPDIR = $PREFIX/tmp  # Use this, NOT /tmp
```

---

## THE GREATEST AI DEV ENVIRONMENT

MobileCLI combines:
- **79 Android Permissions** - Camera, SMS, GPS, Bluetooth, NFC, IR, Sensors, everything
- **Full Dev Tools** - Java, Python, Node.js, Gradle, Android SDK
- **AI Integration** - Claude Code running natively
- **Bootstrap System** - Automatic environment setup
- **Setup Wizard** - Permission handling and configuration
- **Self-Modification** - AI can rebuild its own container app

This is a complete mobile development environment that fits in your pocket.

---

## AVAILABLE DEVELOPER TOOLS

After running `install-dev-tools` or via `pkg install`:

### Languages & Runtimes
| Tool | Command | Use For |
|------|---------|---------|
| **Python 3** | `python3`, `pip` | Scripts, automation, AI/ML |
| **Node.js** | `node`, `npm`, `npx` | JavaScript, web tools |
| **Java 17** | `java`, `javac` | Android development |
| **Kotlin** | Via Gradle | Android apps |
| **Ruby** | `ruby`, `gem` | Scripts, automation |
| **Go** | `go` | Systems programming |
| **Rust** | `rustc`, `cargo` | Systems programming |

### Android Development
| Tool | Purpose |
|------|---------|
| **Gradle** | Build automation |
| **aapt2** | Android asset packaging |
| **d8/dx** | DEX compilation |
| **apksigner** | APK signing |
| **Android SDK** | Full SDK in `~/android-sdk/` |

### Utilities
| Tool | Purpose |
|------|---------|
| **git** | Version control |
| **gh** | GitHub CLI |
| **curl/wget** | HTTP requests |
| **jq** | JSON processing |
| **sqlite3** | Database |
| **ffmpeg** | Media processing |
| **imagemagick** | Image processing |

### Install More
```bash
pkg search <name>    # Find packages
pkg install <name>   # Install package
pip install <name>   # Python packages
npm install -g <name> # Node.js packages
```

---

## BOOTSTRAP & SETUP WIZARD

MobileCLI includes automatic setup:

### BootstrapInstaller
- Extracts Termux environment on first run
- Sets up shell (bash/zsh)
- Configures PATH and environment
- Installs core packages

### SetupWizard
- Requests all 79 permissions
- Configures storage access
- Sets up Termux:API integration
- Initializes development environment

### First Run Flow
```
App Launch → Bootstrap Check → Permission Wizard → Environment Ready
```

---

## ROOT-EQUIVALENT ACCESS (79 Permissions)

MobileCLI has **79 Android permissions** - nearly every capability available:

### What You Can Access
| Category | Capabilities |
|----------|-------------|
| **Storage** | All files, photos, videos, documents, downloads |
| **Camera** | Take photos, record video |
| **Microphone** | Record audio |
| **Phone** | Make/answer calls, read call log, voicemail |
| **SMS** | Send/receive SMS, MMS |
| **Contacts** | Read/write contacts |
| **Calendar** | Read/write calendar events |
| **Location** | GPS, network location, background location |
| **Bluetooth** | Scan, connect, advertise |
| **WiFi** | Scan networks, connection info |
| **NFC** | Read/write NFC tags |
| **Sensors** | Accelerometer, gyro, proximity, light, all sensors |
| **Biometrics** | Fingerprint, face authentication |
| **IR Blaster** | Control TVs/devices via infrared |
| **System** | Notifications, alarms, install apps, battery |

### Linux System Access
| Path | What It Is |
|------|------------|
| `/proc/` | Process info, CPU, memory stats |
| `/sys/` | Kernel info, device settings |
| `/system/` | Android system files (read) |
| `/data/data/com.termux/` | Full app data access |

### Filesystem Access
| Path | Access | Use For |
|------|--------|---------|
| `~/` | Full R/W | Projects, data, anything |
| `/sdcard/Download/` | Full R/W | **FILES USER CAN ACCESS** |
| `/sdcard/DCIM/` | Full R/W | Photos, camera output |
| `/sdcard/` | Full R/W | All user storage |

**CRITICAL:** Put files in `/sdcard/Download/` for user to access from file manager.

### Build Tools (Available after `install-dev-tools`)
You can build Android apps directly on this phone:
- **Java 17** (openjdk-17)
- **Gradle** (build automation)
- **aapt/aapt2** (Android asset packaging)
- **d8/dx** (DEX compilation)
- **apksigner** (APK signing)

### Self-Modification Loop
You can rebuild this very app:
```bash
# Install build tools (one-time)
install-dev-tools

# Clone source
git clone https://github.com/MobileDevCLI/MobileCLI-v2.git ~/MobileCLI-v2

# Make changes, then build
cd ~/MobileCLI-v2 && ./gradlew assembleDebug

# Copy APK to user-accessible location
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/MobileCLI-new.apk
```

### 75+ Termux API Commands
| Command | What It Does |
|---------|--------------|
| `termux-clipboard-get/set` | Read/write clipboard |
| `termux-toast "msg"` | Show toast notification |
| `termux-notification` | Send system notification |
| `termux-open-url URL` | Open URL in browser |
| `termux-vibrate` | Vibrate the phone |
| `termux-camera-photo` | Take a photo |
| `termux-camera-info` | Get camera details |
| `termux-brightness` | Get/set screen brightness |
| `termux-volume` | Get/set volume levels |
| `termux-battery-status` | Battery info (JSON) |
| `termux-wifi-connectioninfo` | WiFi connection info |
| `termux-wifi-scaninfo` | Scan WiFi networks |
| `termux-tts-speak "text"` | Text to speech |
| `termux-sms-send` | Send SMS message |
| `termux-sms-list` | List SMS messages |
| `termux-contact-list` | List contacts |
| `termux-call-log` | Get call history |
| `termux-telephony-deviceinfo` | Phone device info |
| `termux-telephony-cellinfo` | Cell tower info |
| `termux-location` | Get GPS location |
| `termux-sensor` | Read device sensors |
| `termux-fingerprint` | Fingerprint authentication |
| `termux-torch` | Toggle flashlight |
| `termux-infrared-transmit` | Send IR signals |
| `termux-nfc` | Read NFC tags |
| `termux-usb` | USB device access |
| `termux-audio-info` | Audio device info |
| `termux-media-player` | Play audio files |
| `termux-media-scan` | Scan media files |
| `termux-wallpaper` | Set wallpaper |
| `termux-wake-lock/unlock` | Keep CPU awake |
| `termux-dialog` | Show input dialogs |
| `termux-download` | Download files |
| `termux-share` | Share content |
| `termux-storage-get` | Pick file from storage |

Run `termux-` and press TAB to see all available commands.

### MobileCLI Commands
| Command | What It Does |
|---------|--------------|
| `install-dev-tools` | Install Java, Gradle, Android SDK |
| `mobilecli-rebuild` | Full rebuild from source |
| `mobilecli-memory status` | View AI memory system |
| `mobilecli-caps` | Show all capabilities |
| `selfmod` | Self-modification wizard |
| `setup-github` | Configure GitHub credentials |
| `extract-source` | Extract bundled source code |

---

## KNOWN WORKAROUNDS (BUILD ISSUES)

If you're building APKs, you need these workarounds:

### aapt2 ARM vs x86 Issue
**Problem:** Gradle downloads x86_64 aapt2 but we're on ARM.
**Solution:** Add to `gradle.properties`:
```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/home/android-sdk/build-tools/34.0.0/aapt2
```

### Java Version
**Use:** Java 17 for building
**Target:** Java 11 in build.gradle
```gradle
compileOptions {
    sourceCompatibility JavaVersion.VERSION_11
    targetCompatibility JavaVersion.VERSION_11
}
kotlinOptions {
    jvmTarget = '11'
}
```

### Android SDK Location
```properties
# local.properties
sdk.dir=/data/data/com.termux/files/home/android-sdk
```

---

## WHAT IS MOBILECLI?

MobileCLI is the most powerful AI development environment on Android.

**ROOT WITHOUT ROOT:** 79 permissions give you access to nearly everything:
- All sensors, cameras, microphones
- SMS, calls, contacts, calendar
- Bluetooth, WiFi, NFC, IR blaster
- System files via /proc and /sys
- Full Linux terminal environment

**SELF-MODIFICATION:** You can rebuild this very app from within:
```
AI runs inside MobileCLI --> modifies source --> rebuilds APK --> installs new version --> ∞
```

**The Achievement:** This app was built by AI running on an Android phone. The AI built its own container.

**Project Info:**
- Website: https://mobilecli.com
- GitHub: https://github.com/MobileDevCLI
- Version: 2.0.0

---

## MEMORY SYSTEM

Your learnings can persist across sessions:

```
~/.mobilecli/
├── memory/
│   ├── evolution_history.json  - Version history
│   ├── problems_solved.json    - Solutions discovered
│   ├── capabilities.json       - What you've learned
│   └── goals.json              - Objectives
└── config/
    └── preferences.json        - User prefs
```

Use `mobilecli-memory status` to see memory system.

---

## QUICK REFERENCE

### Create a file for user:
```bash
echo "content" > /sdcard/Download/filename.txt
```

### Open a URL:
```bash
termux-open-url "https://example.com"
```

### Build an Android app:
```bash
install-dev-tools
cd ~/my-app
./gradlew assembleDebug
cp app/build/outputs/apk/debug/*.apk /sdcard/Download/
```

### Show notification:
```bash
termux-notification -t "Done!" -c "Your task is complete"
```

### Take a photo:
```bash
termux-camera-photo /sdcard/Download/photo.jpg
```

### Get system info:
```bash
termux-battery-status
termux-wifi-connectioninfo
uname -a
```

---

## RULES

1. **THIS IS ARM ANDROID, NOT LINUX** - Use termux-* commands, not Linux equivalents
2. **NEVER USE /tmp** - Use `$PREFIX/tmp` or `~/tmp` instead (permission denied on /tmp)
3. **SAVE TO /sdcard/Download/** - User can access files there from file manager
4. **USE TERMUX API** - 75+ termux-* commands for Android features
5. **NEVER HALLUCINATE** - If unsure, test it
6. **BUILD THINGS** - You can create and install real Android apps
7. **YOU HAVE ROOT-EQUIVALENT ACCESS** - 79 permissions, /proc, /sys, everything
8. **COMMIT PROPERLY** - Use git, push to GitHub, maintain version control

---

## COMMON PITFALLS (AVOID THESE)

### ❌ DON'T: Use /tmp
```bash
# WRONG - will fail with permission denied
curl -o /tmp/file.zip https://...

# RIGHT - use home directory
curl -o ~/file.zip https://...
```

### ❌ DON'T: Assume x86 binaries work
```bash
# WRONG - x86 binaries won't run
./some-x86-binary

# RIGHT - install ARM version
pkg install <package-name>
```

### ❌ DON'T: Use Linux notification tools
```bash
# WRONG
notify-send "Hello"

# RIGHT
termux-toast "Hello"
termux-notification -t "Title" -c "Content"
```

### ❌ DON'T: Forget to save to Downloads
```bash
# WRONG - user can't find this
cp output.apk ~/output.apk

# RIGHT - user can access from file manager
cp output.apk /sdcard/Download/output.apk
```

### ✅ DO: Use background tasks for long downloads
```bash
# For long-running tasks that might timeout
nohup curl -L -o ~/file.zip "https://..." > ~/download.log 2>&1 &
```

---

## ACCESSING USER'S SCREEN & FILES

### Screenshots (Samsung Galaxy)
Screenshots are saved to: `/sdcard/DCIM/Screenshots/`

```bash
# List recent screenshots
ls -lt /sdcard/DCIM/Screenshots/ | head -5

# View the most recent screenshot
Read /sdcard/DCIM/Screenshots/$(ls -t /sdcard/DCIM/Screenshots/ | head -1)
```

### Screenshots (Other Android)
Some phones save to: `/sdcard/Pictures/Screenshots/`

```bash
# Try both locations
ls -lt /sdcard/DCIM/Screenshots/ 2>/dev/null | head -3
ls -lt /sdcard/Pictures/Screenshots/ 2>/dev/null | head -3
```

### Storage Access Patterns

| Method | Works? | Notes |
|--------|--------|-------|
| `/sdcard/DCIM/Screenshots/` | ✅ YES | Direct path - always works |
| `/sdcard/Download/` | ✅ YES | User-accessible downloads |
| `~/storage/dcim/` | ❌ MAYBE | Only if `termux-setup-storage` was run |
| `find /sdcard -name "*.jpg"` | ❌ UNRELIABLE | Permission quirks |
| `/tmp/anything` | ❌ NEVER | Permission denied on Android |

### Clipboard Access
```bash
# Copy text to clipboard (user can paste anywhere)
echo "text to copy" | termux-clipboard-set

# Or copy file contents
cat ~/myfile.txt | termux-clipboard-set

# Read from clipboard
termux-clipboard-get
```

### Creating Files for User
```bash
# Always save to /sdcard/Download/ for user access
cp ~/myfile.txt /sdcard/Download/myfile.txt

# Open a file with default app
termux-open /sdcard/Download/myfile.txt
```

### Temp Files
```bash
# WRONG - permission denied
echo "data" > /tmp/myfile

# RIGHT - use home directory
echo "data" > ~/tmp/myfile
mkdir -p ~/tmp  # Create if doesn't exist

# OR use $PREFIX/tmp
echo "data" > $PREFIX/tmp/myfile
```

---

## YOU CAN:
- Build Android apps from scratch and install them
- Rebuild this very app (self-modification loop)
- Access /proc, /sys, system files (root-equivalent)
- Take photos, record audio/video
- Send/read SMS, make phone calls
- Read/write contacts and calendar
- Get GPS location, scan WiFi/Bluetooth
- Control TV with IR blaster
- Read all sensors (accelerometer, gyro, etc.)
- Use fingerprint/biometric authentication
- Read/write NFC tags
- Access USB devices
- Send notifications, set wallpaper
- Keep CPU awake for long tasks
- Install packages with `pkg install`
- Run any Linux command

**This is ROOT-EQUIVALENT access on any phone. Use it wisely.**