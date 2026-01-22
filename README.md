# MobileCLI Alpha

**AI-Powered Mobile Terminal with Root-Equivalent Access**

MobileCLI transforms your Android phone into a powerful development environment with AI assistants that can control your device, access sensors, send messages, and build apps - all without requiring root.

---

## Features

### AI Integration
- **Claude Code** (Anthropic) - Most capable AI for coding, recommended
- **Gemini CLI** (Google) - Multimodal AI for research and development
- **Codex CLI** (OpenAI) - Code-focused model for completion and generation

### Root-Equivalent Access (79 Permissions)
MobileCLI provides nearly complete device access through Android's permission system:

| Category | Capabilities |
|----------|-------------|
| **Storage** | Full filesystem access, media files, documents |
| **Camera** | Take photos, record video, access all cameras |
| **Microphone** | Audio recording, speech recognition |
| **Location** | GPS, network location, background tracking |
| **Telephony** | Make calls, read call logs, device info |
| **SMS** | Send/receive messages, read inbox |
| **Contacts** | Full contact list access |
| **Sensors** | Accelerometer, gyroscope, proximity, all sensors |
| **Bluetooth** | Scan, connect, transfer files |
| **NFC** | Read/write NFC tags |
| **Infrared** | IR blaster control (remote control) |
| **Biometrics** | Fingerprint authentication |
| **System** | Notifications, wake locks, boot receiver |

### Development Environment
- **Node.js** - JavaScript runtime for AI tools
- **Python** - Scripting and automation
- **Java 17** - Android development
- **Gradle** - Build automation
- **Android SDK Tools** - aapt, aapt2, d8, apksigner

### 75+ API Commands (Original Implementation)
Full terminal access to device hardware - built from scratch, not using Termux:API app:
```bash
termux-camera-photo      # Take photos
termux-sms-send          # Send SMS
termux-location          # Get GPS coordinates
termux-bluetooth-scan    # Scan Bluetooth devices
termux-notification      # Send notifications
termux-tts-speak         # Text to speech
termux-sensor            # Access all sensors
termux-torch             # Flashlight control
termux-vibrate           # Vibration control
termux-clipboard-get     # Clipboard access
# ... and 65+ more
```

---

## Installation

### Requirements
- Android 7.0+ (API 24+)
- ~100MB free storage for initial setup
- Internet connection for bootstrap download

### Steps
1. Download the APK from Releases
2. Enable "Install from unknown sources" if prompted
3. Install and open MobileCLI
4. Accept the Terms of Service
5. Grant requested permissions (including "Display over other apps" for AI browser auth)
6. Wait for setup to complete (~5-10 minutes)
   - Setup continues in background if you leave the app
   - Setup continues if your screen turns off
7. Choose your AI assistant and authenticate via browser

---

## Usage

### First Launch
After installation, you'll see the **Choose Your AI** screen:
- **Claude** (Recommended) - Best for coding tasks
- **Gemini** - Good for research and multimodal tasks
- **Codex** - Focused on code completion
- **Basic Terminal** - Skip AI, use terminal directly

### Running AI Assistants
From the terminal, simply type:
```bash
claude    # Start Claude Code
gemini    # Start Gemini CLI
codex     # Start Codex CLI
```

### Power Mode
Enable **Power Mode** from the drawer menu to allow AI assistants to:
- Execute commands without confirmation prompts
- Run overnight tasks autonomously
- Perform system-level operations

### Wake Lock
Enable **Wake Lock** to prevent the device from sleeping during:
- Long-running builds
- AI conversations
- Background tasks

---

## Architecture

```
MobileCLI-Alpha/
├── app/src/main/java/com/termux/
│   ├── MainActivity.kt          # Terminal UI, drawer, AI launching
│   ├── SetupWizard.kt           # 4-stage setup (legal, permissions, download, AI)
│   ├── BootstrapInstaller.kt    # Bootstrap, API scripts, CLAUDE.md
│   ├── TermuxApiReceiver.kt     # 75+ API command handlers
│   ├── TermuxService.kt         # Background terminal service
│   └── AmSocketServer.kt        # Fast activity manager commands
├── app/src/main/AndroidManifest.xml  # 79 permissions
├── app/src/main/res/
│   ├── layout/                  # UI layouts
│   └── values/                  # Themes, strings
└── docs/
    ├── ARCHITECTURE.md          # Technical deep-dive
    ├── API_REFERENCE.md         # All termux-* commands
    └── ROADMAP.md               # Development roadmap
```

### Key Components

| Component | Purpose |
|-----------|---------|
| **SetupWizard** | Legal agreement, permissions, bootstrap download, AI selection |
| **BootstrapInstaller** | Downloads Termux packages, creates 75+ API scripts, generates CLAUDE.md |
| **MainActivity** | Terminal emulator, drawer navigation, AI launching, wake/power controls |
| **TermuxApiReceiver** | Handles all termux-* command broadcasts |
| **TermuxService** | Maintains terminal sessions in background |

---

## API Reference

### Clipboard
```bash
termux-clipboard-get              # Get clipboard contents
termux-clipboard-set "text"       # Set clipboard contents
```

### Notifications
```bash
termux-toast "message"            # Show toast
termux-notification -t "Title" -c "Content"  # Send notification
termux-vibrate -d 500             # Vibrate for 500ms
```

### Camera & Media
```bash
termux-camera-info                # List cameras
termux-camera-photo -c 0 -o photo.jpg  # Take photo
termux-media-scan photo.jpg       # Add to media library
termux-microphone-record -f audio.wav  # Record audio
```

### Location
```bash
termux-location                   # Get GPS coordinates (JSON)
```

### Telephony & SMS
```bash
termux-telephony-deviceinfo       # Device info
termux-telephony-cellinfo         # Cell tower info
termux-sms-list -l 10             # List last 10 SMS
termux-sms-send -n "+1234567890" "Hello"  # Send SMS
termux-call-log -l 10             # Call history
```

### Contacts
```bash
termux-contact-list               # List all contacts (JSON)
```

### Sensors
```bash
termux-sensor -l                  # List available sensors
termux-sensor -s "accelerometer"  # Read accelerometer
termux-battery-status             # Battery info
termux-brightness 255             # Set brightness
termux-torch on                   # Flashlight on
```

### Bluetooth
```bash
termux-bluetooth-info             # Adapter info
termux-bluetooth-paired           # List paired devices
termux-bluetooth-scaninfo         # Scan for devices
termux-bluetooth-connect "AA:BB:CC:DD:EE:FF"  # Connect
```

### System
```bash
termux-wake-lock                  # Prevent sleep
termux-wake-unlock                # Allow sleep
termux-wallpaper image.jpg        # Set wallpaper
termux-open-url "https://..."     # Open URL in browser
termux-share file.txt             # Share file
termux-tts-speak "Hello"          # Text to speech
```

### MobileCLI Custom Commands
```bash
mobilecli-caps                    # Show all capabilities
mobilecli-memory                  # AI memory system
mobilecli-rebuild                 # Rebuild app from source
mobilecli-share file.apk          # Bluetooth file sharing
install-dev-tools                 # Install Java, Gradle, SDK
```

---

## Legal & Licensing

**MobileCLI is proprietary software.** See documentation below for complete details.

### Legal Documentation

| Document | Purpose |
|----------|---------|
| [LICENSE](LICENSE) | Proprietary license for MobileCLI code |
| [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) | Third-party component documentation |
| [LEGAL_SUMMARY.md](LEGAL_SUMMARY.md) | One-page executive summary for lawyers/investors |
| [IP.md](IP.md) | Intellectual property documentation |

### Ownership Summary

- **Original Code:** ~9,000+ lines (100% MobileCLI Team)
- **Third-Party:** terminal-view & terminal-emulator (Apache 2.0 - VT100 rendering only)
- **GPL Code:** NONE bundled

### Terms of Service
By using MobileCLI, you agree to:
- Assume all risk for AI actions and device operations
- Accept that MobileCLI is provided "AS IS" without warranty
- Indemnify the MobileCLI Team from any claims

### Privacy Policy
- MobileCLI does NOT collect or transmit personal data to our servers
- All data remains on your device
- AI queries are sent to their respective providers (Anthropic, Google, OpenAI)

### Open Source Attribution
MobileCLI is built with open source technology:
- **terminal-view & terminal-emulator libraries** - Apache License 2.0
- Based on Android Terminal Emulator by Jack Palevich
- Adapted by Fredrik Fornwall and contributors

All application code (~9,000+ lines), UI, setup system, API commands, and integrations are original work by the MobileCLI Team.

---

## Security

### Credential Storage
- Keystore passwords stored in `local.properties` (git-ignored)
- GitHub tokens stored in `~/.termux/github_token`
- Never commit credentials to the repository

### Permissions
MobileCLI requests extensive permissions to provide root-equivalent functionality. All permission usage is local - no data is sent to MobileCLI servers.

---

## Development

### Building from Source
```bash
# Clone the repository
git clone https://github.com/MobileDevCLI/MobileCLI-Alpha.git
cd MobileCLI-Alpha

# Create local.properties with your keystore (for release builds)
cp local.properties.template local.properties
# Edit local.properties with your credentials

# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Self-Modification
MobileCLI can rebuild itself from within the app:
```bash
install-dev-tools     # Install Java, Gradle, Android SDK
mobilecli-rebuild     # Build new APK
```

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test on a real device
5. Submit a pull request

---

## Roadmap

See [ROADMAP.md](ROADMAP.md) for the full development roadmap.

### Completed
- [x] 79 Android permissions
- [x] Claude, Gemini, Codex integration
- [x] 75+ Termux API commands
- [x] Legal agreement on first launch
- [x] Privacy Policy, Terms of Service
- [x] Clean bootstrap (no terminal output during setup)
- [x] Wake Lock and Power Mode
- [x] GitHub backup

### Recently Added
- [x] **Vercel CLI drawer button** - One-tap install + OAuth login in new terminal tab
- [x] **GitHub CLI drawer button** - One-tap install gh + git + OAuth in new terminal tab
- [x] Background setup - continues even if you leave the app or screen turns off
- [x] Browser authentication for all AI tools (Claude, Gemini, Codex)
- [x] "Display over other apps" permission for browser launching
- [x] Text selection with long-press context menu
- [x] Python installed before AI tools (fixes node-gyp compilation)

### Upcoming
- [ ] Multi-device testing
- [ ] Release build with signing
- [ ] App store submission
- [ ] Performance optimization

---

## Support

For issues and feature requests, please use the GitHub Issues page.

---

## License

**MobileCLI is proprietary software.**

- [LICENSE](LICENSE) - Proprietary license
- [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) - Third-party components
- [LEGAL_SUMMARY.md](LEGAL_SUMMARY.md) - Executive summary for due diligence
- [IP.md](IP.md) - Intellectual property documentation

---

**Copyright (c) 2026 MobileCLI Team. All Rights Reserved.**
