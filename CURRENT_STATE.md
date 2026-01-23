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
- **CRITICAL:** Webhook field mismatch → Changed `current_period_end` to `expires_at`

---

## GIT HISTORY (Latest)

```
d882f05 Update documentation with critical webhook fix discovered in code audit
be0d9ef Fix critical webhook bug: field name mismatch
b6817f4 Update documentation with bug fixes, account management
1e12297 Fix bugs: Add background color, fix deprecated onBackPressed
13136ff Add Account screen with logout, subscription management
```

---

## FILES IN /sdcard/Download/

| File | Purpose |
|------|---------|
| `MobileCLI-Pro-v2.0.0-FINAL.apk` | **USE THIS FOR WEBSITE** |
| `paypal-webhook-v4-FIXED.txt` | Webhook code (backup) |

**STATUS:** Webhook redeployed to Supabase on Jan 22, 2026. All systems ready.

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
