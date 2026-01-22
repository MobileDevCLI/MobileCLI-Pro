# MobileCLI - Third-Party Licenses

**Document Purpose:** This file documents all third-party open source components used in MobileCLI for legal compliance and due diligence purposes.

**Last Updated:** January 19, 2026

---

## Summary

MobileCLI uses **two** third-party libraries for terminal rendering. All other code (~9,000+ lines) is original proprietary work.

| Component | License | Purpose | Required? |
|-----------|---------|---------|-----------|
| terminal-view | Apache 2.0 | Android View for terminal display | Yes |
| terminal-emulator | Apache 2.0 | VT100 escape code parser | Yes |
| AndroidX | Apache 2.0 | Android support libraries | Yes |

---

## Terminal Rendering Libraries

### Component: terminal-view & terminal-emulator

**License:** Apache License 2.0

**Source:** https://github.com/termux/termux-app

**Maven Coordinates:**
```
com.github.termux.termux-app:terminal-view:v0.118.0
com.github.termux.termux-app:terminal-emulator:v0.118.0
```

**Provenance Chain:**
```
Jack Palevich (2011)
    │ Original author
    │ License: Apache 2.0
    ▼
Android Terminal Emulator
    │ Source: github.com/jackpal/Android-Terminal-Emulator
    │ License: Apache 2.0
    ▼
Fredrik Fornwall / Termux (2016+)
    │ Adapted and maintained
    │ License: Apache 2.0 (explicit exception in Termux LICENSE.md)
    ▼
MobileCLI (2026)
    │ Uses packaged libraries
    │ No modifications to library code
```

**What These Libraries Do:**
- `terminal-emulator`: Parses VT100/ANSI escape sequences, manages terminal buffer
- `terminal-view`: Android View component that renders terminal text on screen

**What These Libraries Do NOT Do:**
- They do NOT provide the shell (bash)
- They do NOT provide packages (node, python)
- They do NOT provide API commands
- They do NOT provide the setup wizard, UI, or any application logic

**Copyright Notice:**
```
Copyright (c) 2011 Jack Palevich
Copyright (c) 2016-2024 Fredrik Fornwall and contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## AndroidX Libraries

**License:** Apache License 2.0

**Components Used:**
```
androidx.core:core-ktx:1.12.0
androidx.appcompat:appcompat:1.6.1
com.google.android.material:material:1.10.0
androidx.constraintlayout:constraintlayout:2.1.4
androidx.lifecycle:lifecycle-runtime-ktx:2.6.2
```

**Copyright Notice:**
```
Copyright (c) Google LLC

Licensed under the Apache License, Version 2.0
```

---

## Kotlin & Coroutines

**License:** Apache License 2.0

**Components Used:**
```
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3
```

**Copyright Notice:**
```
Copyright (c) JetBrains s.r.o.

Licensed under the Apache License, Version 2.0
```

---

## What is NOT Third-Party (Original MobileCLI Code)

The following is 100% original code owned by MobileCLI Team:

| File | Lines | Description |
|------|-------|-------------|
| MainActivity.kt | ~1,400 | Terminal UI, navigation drawer, gestures |
| BootstrapInstaller.kt | ~2,900 | Bootstrap download, extraction, scripts |
| TermuxApiReceiver.kt | ~2,000 | 75+ API command implementations |
| SetupWizard.kt | ~800 | 3-stage setup flow |
| TermuxService.kt | ~600 | Background service, wake locks |
| AmSocketServer.kt | ~440 | Proprietary Activity Manager (IPC) |
| Other files | ~800 | Activities, receivers, utilities |
| **TOTAL** | **~9,000+** | **100% Original** |

---

## Runtime Downloads (NOT Bundled)

Users download the following at runtime from Termux package repositories. These are NOT included in the MobileCLI APK:

- bash, coreutils, apt (GPL)
- Node.js (MIT)
- Python (PSF)
- npm packages (various licenses)

MobileCLI acts as a download client, similar to a web browser. Users download these packages themselves; MobileCLI does not distribute them.

---

## Apache License 2.0 - Full Text Reference

The full Apache License 2.0 text is available at:
https://www.apache.org/licenses/LICENSE-2.0

**Key Permissions (Apache 2.0):**
- ✅ Commercial use
- ✅ Modification
- ✅ Distribution
- ✅ Private use
- ✅ Sublicensing

**Requirements:**
- Include copyright notice
- Include license text or reference
- State changes if modified (we do not modify)

**NOT Required:**
- ❌ Open source your code
- ❌ Share your source code
- ❌ Use same license for your code

---

## Compliance Statement

MobileCLI complies with Apache License 2.0 by:

1. ✅ Including copyright notices in app (Open Source Licenses screen)
2. ✅ Documenting all third-party components (this file)
3. ✅ Providing license reference in app
4. ✅ Not removing or altering copyright notices in library code

---

## Contact for Legal Inquiries

Website: https://mobilecli.com
GitHub: https://github.com/MobileDevCLI
Email: [Contact through website]

---

*Document created: January 19, 2026*
*This document is for legal compliance and due diligence purposes.*
