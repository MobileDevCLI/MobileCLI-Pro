# MobileCLI Pro - Current State (For AI Context Recovery)

**Date:** January 22, 2026
**Status:** READY FOR RELEASE
**Version:** 2.0.0-FINAL

---

## LATEST APK

**File:** `/sdcard/Download/MobileCLI-Pro-v2.0.0-FINAL.apk`

This is the release-ready APK with all features and bug fixes.

---

## KEY IDS

| Item | Value |
|------|-------|
| PayPal Plan ID | `P-3RH33892X5467024SNFZON2Y` |
| Supabase Project | `mwxlguqukyfberyhtkmg` |
| Webhook URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook` |

---

## FEATURES COMPLETED

### Authentication & Payments
- Google OAuth + Email/Password login
- PayPal subscription ($15/month recurring)
- User ID matching (any PayPal account works)
- Webhook handles all subscription events
- Multi-device login support

### Account Management (Industry Standard)
- Account screen with profile display
- Logout button with confirmation
- Manage Subscription (opens PayPal)
- Restore Purchase functionality
- Delete Account option

### Bug Fixes Applied
- Fixed: Account screen transparent background → Added #121212
- Fixed: Deprecated onBackPressed (Android 13+) → OnBackPressedCallback

---

## GIT HISTORY (Latest)

```
1e12297 Fix bugs: Add background color, fix deprecated onBackPressed
13136ff Add Account screen with logout, subscription management
0b72018 Add CURRENT_STATE.md for AI context recovery
99f3c6f Add user_id matching for PayPal webhook
6fe3c2e Update webhook to REST API format
```

---

## FILES IN /sdcard/Download/

| File | Purpose |
|------|---------|
| `MobileCLI-Pro-v2.0.0-FINAL.apk` | **USE THIS FOR WEBSITE** |
| `paypal-webhook-v3-with-userid.txt` | Webhook code (already deployed) |

---

## TEST FLOW

1. Install APK on test phone
2. Login with Google
3. Click Subscribe → Opens PayPal subscription
4. Complete $15 payment (can use any PayPal account)
5. Press back → App detects Pro status
6. Terminal opens (not paywall)
7. Open drawer → Click "Account" → See profile, logout option

---

## IMPORTANT FILES

| File | Purpose |
|------|---------|
| `CURRENT_STATE.md` | Quick AI context recovery |
| `docs/ROADMAP_AND_STATUS.md` | Full documentation |
| `CLAUDE.md` | AI environment guide |
| `app/src/main/java/com/termux/auth/AccountActivity.kt` | Account screen |
| `app/src/main/java/com/termux/auth/PaywallActivity.kt` | PayPal integration |
| `supabase/functions/paypal-webhook/index.ts` | Webhook code |

---

## DO NOT MODIFY

- BootstrapInstaller.kt
- SetupWizard.kt
- MainActivity.kt (except drawer setup)
- AndroidManifest.xml
- gradle.properties

---

## WEBSITE DEPLOYMENT

Use `/sdcard/Download/MobileCLI-Pro-v2.0.0-FINAL.apk` for:
- Direct download link
- GitHub Releases
- Any file hosting

---

*Last updated: January 22, 2026*
