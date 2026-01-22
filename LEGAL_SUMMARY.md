# MobileCLI - Legal Summary (Executive Overview)

**Purpose:** One-page summary for lawyers, investors, and acquirers.

**Date:** January 19, 2026

---

## Bottom Line

**MobileCLI is a proprietary Android application with ~9,000+ lines of original code. It uses two small Apache 2.0 libraries for terminal rendering. There are no GPL dependencies bundled. The software can be sold commercially without restriction.**

---

## Ownership Summary

| Category | Lines of Code | Ownership | License |
|----------|---------------|-----------|---------|
| Original MobileCLI Code | ~9,000+ | **100% MobileCLI Team** | Proprietary |
| Third-Party Libraries | ~5,000 (estimated) | Jack Palevich / Fredrik Fornwall | Apache 2.0 |

---

## What MobileCLI Team Owns (Proprietary)

All application code including:
- User interface and navigation
- Setup wizard (3-stage onboarding)
- 75+ API command implementations
- Bootstrap download/installation system
- Background service and session management
- **Proprietary Activity Manager** (replaced GPL component)

**Key Innovation:** The Activity Manager was built from scratch to replace a GPL-licensed component (am.apk), ensuring the codebase remains free of viral licensing.

---

## What MobileCLI Licenses (Apache 2.0)

Two terminal rendering libraries:
- `terminal-view` - Displays terminal text on Android
- `terminal-emulator` - Parses VT100 escape codes

**Apache 2.0 Allows:**
- ✅ Commercial use
- ✅ Closed-source distribution
- ✅ Modification
- ✅ Sublicensing

**Apache 2.0 Requires:**
- Include copyright notice (done in app)
- Reference the license (done in app)

**Apache 2.0 Does NOT Require:**
- Open sourcing your code
- Sharing source code
- Payment or royalties

---

## GPL Status

| Component | Status |
|-----------|--------|
| am.apk (Termux's GPL component) | **NOT USED** - Replaced with proprietary IPC |
| Termux app source code | **NOT USED** |
| Any GPL code bundled in APK | **NONE** |

**Verification:** The `am.apk` binary is not included in the APK. A proprietary file-based IPC system was built to replace this functionality.

---

## Runtime Downloads (Not MobileCLI's Liability)

Users download the following at runtime:
- Linux packages (bash, coreutils) - GPL
- Node.js - MIT
- Python - PSF

These are downloaded by the user from Termux package repositories. MobileCLI does not bundle or distribute these. This is analogous to a web browser downloading content.

---

## Key Legal Documents

| Document | Purpose |
|----------|---------|
| `LICENSE` | Proprietary license statement |
| `THIRD_PARTY_LICENSES.md` | Detailed third-party component documentation |
| `IP.md` | Intellectual property documentation |
| `LEGAL_SUMMARY.md` | This document |

---

## Commercial Rights

**Can MobileCLI be sold?** YES

**Can it be sold closed-source?** YES

**Are there any royalty obligations?** NO

**Are there any notification obligations?** NO (beyond in-app attribution)

**Can a buyer modify and resell?** YES (they would own it)

---

## Due Diligence Checklist

- [x] No GPL code bundled in APK
- [x] No LGPL code bundled in APK
- [x] Apache 2.0 components properly attributed
- [x] Original code ownership documented
- [x] Third-party licenses documented
- [x] Proprietary Activity Manager replaces GPL am.apk
- [x] Runtime downloads are user-initiated (not bundled)

---

## Provenance Chain (Terminal Libraries)

```
Jack Palevich (2011) ─── Apache 2.0 ───► Android Terminal Emulator
                                              │
Fredrik Fornwall (2016+) ── Apache 2.0 ──────► terminal-view
                                              │ terminal-emulator
                                              │
MobileCLI Team (2026) ───── Uses ────────────►  (no modifications)
```

---

## Contact

**Website:** https://mobilecli.com
**GitHub:** https://github.com/MobileDevCLI
**For legal inquiries:** Contact through website

---

*This summary is for informational purposes. Consult an attorney for legal advice.*
