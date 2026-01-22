# MobileCLI API Reference

Complete reference for all 75+ termux-* commands and MobileCLI utilities.

---

## Table of Contents

1. [Clipboard](#clipboard)
2. [Notifications](#notifications)
3. [Device Info](#device-info)
4. [Network & WiFi](#network--wifi)
5. [Location](#location)
6. [Camera](#camera)
7. [Audio & Media](#audio--media)
8. [Text-to-Speech](#text-to-speech)
9. [Telephony](#telephony)
10. [SMS](#sms)
11. [Contacts](#contacts)
12. [Call Log](#call-log)
13. [Sensors](#sensors)
14. [Biometrics](#biometrics)
15. [Bluetooth](#bluetooth)
16. [Infrared](#infrared)
17. [NFC](#nfc)
18. [USB](#usb)
19. [System Utilities](#system-utilities)
20. [File Operations](#file-operations)
21. [Keystore](#keystore)
22. [Wake Lock](#wake-lock)
23. [MobileCLI Utilities](#mobilecli-utilities)

---

## Clipboard

### termux-clipboard-get
Get the current clipboard contents.

```bash
termux-clipboard-get
```

**Output:** Text content of clipboard

---

### termux-clipboard-set
Set the clipboard contents.

```bash
termux-clipboard-set "text to copy"
# or pipe input
echo "text to copy" | termux-clipboard-set
```

**Arguments:**
- Text to set as clipboard content

---

## Notifications

### termux-toast
Show a toast message.

```bash
termux-toast "Hello World"
termux-toast -g top "Message at top"
termux-toast -b white -c black "Custom colors"
```

**Options:**
| Option | Description |
|--------|-------------|
| `-g` | Gravity: top, middle, bottom |
| `-b` | Background color |
| `-c` | Text color |
| `-s` | Short duration (default) |

---

### termux-notification
Send a notification.

```bash
termux-notification -t "Title" -c "Content"
termux-notification -t "Title" -c "Content" -i mynotif
termux-notification --ongoing -t "Running" -c "Task in progress"
```

**Options:**
| Option | Description |
|--------|-------------|
| `-t, --title` | Notification title |
| `-c, --content` | Notification content |
| `-i, --id` | Notification ID (for updates/removal) |
| `--ongoing` | Make notification persistent |
| `--alert-once` | Only alert once |
| `--priority` | high, low, min, max, default |

---

### termux-notification-remove
Remove a notification by ID.

```bash
termux-notification-remove mynotif
```

---

### termux-notification-list
List active notifications.

```bash
termux-notification-list
```

**Output:** JSON array of active notifications

---

## Device Info

### termux-battery-status
Get battery status.

```bash
termux-battery-status
```

**Output:**
```json
{
  "health": "GOOD",
  "percentage": 85,
  "plugged": "UNPLUGGED",
  "status": "DISCHARGING",
  "temperature": 25.0
}
```

---

### termux-vibrate
Vibrate the device.

```bash
termux-vibrate                 # Default 1000ms
termux-vibrate -d 500          # 500ms
termux-vibrate -f              # Force even in silent mode
```

**Options:**
| Option | Description |
|--------|-------------|
| `-d` | Duration in milliseconds |
| `-f` | Force vibration in silent mode |

---

### termux-brightness
Get or set screen brightness.

```bash
termux-brightness              # Get current
termux-brightness 255          # Set to max
termux-brightness auto         # Auto brightness
```

**Values:** 0-255, or "auto"

---

### termux-torch
Control flashlight.

```bash
termux-torch on
termux-torch off
```

---

### termux-volume
Get or set volume levels.

```bash
termux-volume                  # Get all volumes
termux-volume music 10         # Set music to 10
```

**Streams:** music, ring, alarm, notification, system

---

### termux-audio-info
Get audio device information.

```bash
termux-audio-info
```

---

## Network & WiFi

### termux-wifi-connectioninfo
Get current WiFi connection info.

```bash
termux-wifi-connectioninfo
```

**Output:**
```json
{
  "bssid": "00:11:22:33:44:55",
  "frequency_mhz": 2437,
  "ip": "192.168.1.100",
  "link_speed_mbps": 72,
  "mac_address": "AA:BB:CC:DD:EE:FF",
  "network_id": 0,
  "rssi": -50,
  "ssid": "MyNetwork",
  "supplicant_state": "COMPLETED"
}
```

---

### termux-wifi-scaninfo
Scan for nearby WiFi networks.

```bash
termux-wifi-scaninfo
```

**Output:** JSON array of nearby networks

---

### termux-wifi-enable
Enable or disable WiFi.

```bash
termux-wifi-enable true
termux-wifi-enable false
```

---

## Location

### termux-location
Get current location.

```bash
termux-location
termux-location -p gps         # GPS only
termux-location -p network     # Network only
termux-location -r once        # Single reading
```

**Options:**
| Option | Description |
|--------|-------------|
| `-p` | Provider: gps, network, passive |
| `-r` | Request: once, last, updates |

**Output:**
```json
{
  "latitude": 37.7749,
  "longitude": -122.4194,
  "altitude": 10.0,
  "accuracy": 20.0,
  "bearing": 0.0,
  "speed": 0.0,
  "provider": "gps"
}
```

---

## Camera

### termux-camera-info
Get information about available cameras.

```bash
termux-camera-info
```

**Output:**
```json
[
  {
    "id": "0",
    "facing": "back",
    "jpeg_output_sizes": [...],
    "focal_lengths": [4.25],
    "auto_exposure_modes": [...]
  }
]
```

---

### termux-camera-photo
Take a photo.

```bash
termux-camera-photo -o photo.jpg
termux-camera-photo -c 1 -o selfie.jpg    # Front camera
```

**Options:**
| Option | Description |
|--------|-------------|
| `-o` | Output file path (required) |
| `-c` | Camera ID (default: 0) |

---

## Audio & Media

### termux-media-scan
Scan a file to add it to media library.

```bash
termux-media-scan photo.jpg
termux-media-scan ~/Music/song.mp3
```

---

### termux-media-player
Control media playback.

```bash
termux-media-player play /path/to/audio.mp3
termux-media-player pause
termux-media-player stop
termux-media-player info
```

**Actions:** play, pause, stop, info

---

### termux-microphone-record
Record audio from microphone.

```bash
termux-microphone-record -f recording.wav
termux-microphone-record -f rec.wav -l 10    # 10 second limit
termux-microphone-record -q                   # Stop recording
```

**Options:**
| Option | Description |
|--------|-------------|
| `-f` | Output file path |
| `-l` | Limit in seconds |
| `-q` | Quit/stop recording |
| `-d` | Default settings |

---

## Text-to-Speech

### termux-tts-engines
List available TTS engines.

```bash
termux-tts-engines
```

---

### termux-tts-speak
Speak text aloud.

```bash
termux-tts-speak "Hello, world!"
echo "Speak this" | termux-tts-speak
termux-tts-speak -r 0.5 "Slow speech"
termux-tts-speak -p 1.5 "High pitch"
```

**Options:**
| Option | Description |
|--------|-------------|
| `-r` | Speech rate (0.5-2.0) |
| `-p` | Pitch (0.5-2.0) |
| `-l` | Language (e.g., en-US) |
| `-e` | Engine name |

---

## Telephony

### termux-telephony-deviceinfo
Get device telephony information.

```bash
termux-telephony-deviceinfo
```

**Output:**
```json
{
  "data_state": "connected",
  "device_id": "...",
  "device_software_version": "...",
  "network_country_iso": "us",
  "network_operator": "310260",
  "network_operator_name": "T-Mobile",
  "network_type": "LTE",
  "phone_type": "gsm",
  "sim_country_iso": "us",
  "sim_operator": "310260",
  "sim_operator_name": "T-Mobile",
  "sim_state": "ready"
}
```

---

### termux-telephony-cellinfo
Get cell tower information.

```bash
termux-telephony-cellinfo
```

---

### termux-telephony-call
Make a phone call.

```bash
termux-telephony-call +15551234567
```

---

## SMS

### termux-sms-list
List SMS messages.

```bash
termux-sms-list
termux-sms-list -l 20          # Last 20 messages
termux-sms-list -t inbox       # Inbox only
termux-sms-list -t sent        # Sent only
```

**Options:**
| Option | Description |
|--------|-------------|
| `-l, -n` | Limit number of messages |
| `-t` | Type: inbox, sent, draft, all |
| `-o` | Offset (skip first N) |

**Output:**
```json
[
  {
    "threadid": 1,
    "type": "inbox",
    "read": true,
    "number": "+15551234567",
    "received": "2026-01-19 10:30:00",
    "body": "Hello!"
  }
]
```

---

### termux-sms-send
Send an SMS message.

```bash
termux-sms-send -n +15551234567 "Hello from MobileCLI!"
```

**Options:**
| Option | Description |
|--------|-------------|
| `-n` | Recipient phone number (required) |

---

## Contacts

### termux-contact-list
List all contacts.

```bash
termux-contact-list
```

**Output:**
```json
[
  {
    "name": "John Doe",
    "number": "+15551234567"
  }
]
```

---

## Call Log

### termux-call-log
Get call history.

```bash
termux-call-log
termux-call-log -l 50          # Last 50 calls
```

**Options:**
| Option | Description |
|--------|-------------|
| `-l, -n` | Limit number of entries |
| `-o` | Offset |

**Output:**
```json
[
  {
    "name": "John Doe",
    "number": "+15551234567",
    "type": "INCOMING",
    "date": "2026-01-19 09:15:00",
    "duration": "120"
  }
]
```

---

## Sensors

### termux-sensor
Access device sensors.

```bash
termux-sensor -l               # List all sensors
termux-sensor -s accelerometer # Read accelerometer
termux-sensor -s gyroscope -n 5 # 5 readings from gyroscope
```

**Options:**
| Option | Description |
|--------|-------------|
| `-l` | List available sensors |
| `-s` | Sensor name to read |
| `-n` | Number of readings |
| `-d` | Delay between readings (ms) |

**Common Sensors:**
- accelerometer
- gyroscope
- light
- proximity
- pressure
- magnetic_field
- gravity
- rotation_vector

---

## Biometrics

### termux-fingerprint
Authenticate using fingerprint.

```bash
termux-fingerprint
```

**Output:**
```json
{
  "auth_result": "AUTH_RESULT_SUCCESS"
}
```

---

## Bluetooth

### termux-bluetooth-info
Get Bluetooth adapter information.

```bash
termux-bluetooth-info
```

**Output:**
```json
{
  "enabled": true,
  "address": "AA:BB:CC:DD:EE:FF",
  "name": "My Phone"
}
```

---

### termux-bluetooth-enable
Enable or disable Bluetooth.

```bash
termux-bluetooth-enable on
termux-bluetooth-enable off
```

---

### termux-bluetooth-paired
List paired Bluetooth devices.

```bash
termux-bluetooth-paired
```

**Output:**
```json
[
  {
    "name": "My Headphones",
    "address": "11:22:33:44:55:66",
    "type": "AUDIO"
  }
]
```

---

### termux-bluetooth-scaninfo
Scan for nearby Bluetooth devices.

```bash
termux-bluetooth-scaninfo
```

---

### termux-bluetooth-connect
Connect to a Bluetooth device.

```bash
termux-bluetooth-connect 11:22:33:44:55:66
```

---

## Infrared

### termux-infrared-frequencies
Get supported IR frequencies.

```bash
termux-infrared-frequencies
```

---

### termux-infrared-transmit
Transmit IR pattern.

```bash
termux-infrared-transmit -f 38000 100 50 100 50
```

**Options:**
| Option | Description |
|--------|-------------|
| `-f` | Carrier frequency in Hz |

**Arguments:** Pattern of on/off times in microseconds

---

## NFC

### termux-nfc
Interact with NFC tags.

```bash
termux-nfc
```

---

## USB

### termux-usb
List USB devices.

```bash
termux-usb
```

---

## System Utilities

### termux-wallpaper
Set device wallpaper.

```bash
termux-wallpaper image.jpg
termux-wallpaper -u "https://example.com/image.jpg"
termux-wallpaper -l            # Lock screen only
```

**Options:**
| Option | Description |
|--------|-------------|
| `-u` | URL of image |
| `-l` | Lock screen wallpaper |

---

### termux-open-url
Open a URL in the default browser.

```bash
termux-open-url "https://google.com"
```

---

### termux-open
Open a file or URL.

```bash
termux-open document.pdf
termux-open "https://example.com"
termux-open --send file.txt    # Share file
termux-open --chooser image.jpg # Show app chooser
```

**Options:**
| Option | Description |
|--------|-------------|
| `--send` | Share for sending |
| `--view` | Share for viewing (default) |
| `--chooser` | Always show app chooser |
| `--content-type` | MIME type |

---

### termux-share
Share content with other apps.

```bash
termux-share file.txt
echo "Share this text" | termux-share
termux-share -a send file.txt
```

---

### termux-download
Download a file using system download manager.

```bash
termux-download "https://example.com/file.zip"
termux-download -t "My Download" "https://example.com/file.zip"
```

**Options:**
| Option | Description |
|--------|-------------|
| `-t` | Title for download notification |
| `-d` | Description |

---

### termux-dialog
Show a dialog and get user input.

```bash
termux-dialog
termux-dialog -t "Enter name" -i "Name"
```

**Options:**
| Option | Description |
|--------|-------------|
| `-t` | Title |
| `-i` | Input hint |

---

### termux-storage-get
Get a file from shared storage.

```bash
termux-storage-get output.txt
```

---

### termux-job-scheduler
Schedule background jobs.

```bash
termux-job-scheduler --script ~/script.sh --period-ms 3600000
```

---

### termux-setup-storage
Set up storage symlinks.

```bash
termux-setup-storage
```

Creates symlinks in ~/storage/:
- shared → /sdcard
- dcim → /sdcard/DCIM
- downloads → /sdcard/Download
- pictures → /sdcard/Pictures
- music → /sdcard/Music
- movies → /sdcard/Movies

---

### termux-reload-settings
Reload terminal settings.

```bash
termux-reload-settings
```

---

### termux-info
Show MobileCLI information.

```bash
termux-info
```

---

## Keystore

### termux-keystore-list
List keystore entries.

```bash
termux-keystore-list
```

---

### termux-keystore
Manage keystore.

```bash
termux-keystore list
termux-keystore generate -a mykey -g RSA -s 2048
termux-keystore delete mykey
termux-keystore sign -a mykey -d "data to sign"
```

---

## Wake Lock

### termux-wake-lock
Acquire wake lock (prevent sleep).

```bash
termux-wake-lock
```

---

### termux-wake-unlock
Release wake lock (allow sleep).

```bash
termux-wake-unlock
```

---

## MobileCLI Utilities

### mobilecli-caps
Show all MobileCLI capabilities.

```bash
mobilecli-caps
```

---

### mobilecli-memory
Manage AI memory system.

```bash
mobilecli-memory status        # Show status
mobilecli-memory history       # Evolution history
mobilecli-memory problems      # Solved problems
mobilecli-memory caps          # Capabilities
mobilecli-memory goals         # Current goals
mobilecli-memory log "message" # Add log entry
```

---

### mobilecli-rebuild
Rebuild MobileCLI from source.

```bash
mobilecli-rebuild
```

Requires dev tools installed (`install-dev-tools`).

---

### mobilecli-share
Share files via Bluetooth.

```bash
mobilecli-share /path/to/file.apk
mobilecli-share --latest-apk   # Share most recent APK
mobilecli-share --clipboard    # Share clipboard as file
```

---

### mobilecli-dev-mode
Toggle developer mode.

```bash
mobilecli-dev-mode on
mobilecli-dev-mode off
mobilecli-dev-mode status
```

---

### install-dev-tools
Install development tools.

```bash
install-dev-tools
```

Installs: Java 17, Gradle, aapt, aapt2, d8, apksigner

---

### setup-github
Configure GitHub credentials.

```bash
setup-github YOUR_GITHUB_TOKEN
```

---

### extract-source
Extract bundled source code.

```bash
extract-source
```

---

### selfmod
Self-modification wizard.

```bash
selfmod
```

Interactive menu for modifying MobileCLI.

---

## Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | General error |
| 2 | Invalid arguments |
| 126 | Permission denied |
| 127 | Command not found |

---

## JSON Output

Most commands output JSON for easy parsing:

```bash
# Parse with jq
termux-battery-status | jq '.percentage'

# Parse with Python
termux-location | python -c "import json,sys; print(json.load(sys.stdin)['latitude'])"
```

---

## Environment Variables

| Variable | Description |
|----------|-------------|
| `HOME` | User home directory |
| `PREFIX` | Termux prefix (/data/data/com.termux/files/usr) |
| `PATH` | Executable search path |
| `TERM` | Terminal type (xterm-256color) |
| `SHELL` | Current shell |
| `TERMUX_VERSION` | Termux version |

---

## Troubleshooting

### Command not found
```bash
# Ensure PATH is correct
echo $PATH
# Should include /data/data/com.termux/files/usr/bin
```

### Permission denied
```bash
# Check if permission was granted in Android settings
# Re-run the command after granting permission
```

### No output
```bash
# Some commands require a brief delay
termux-location
sleep 2
```

### API timeout
```bash
# Increase wait time in script
# Or check if the required hardware is available
```
