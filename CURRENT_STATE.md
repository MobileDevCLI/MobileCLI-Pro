# MobileCLI Pro - Current State (For AI Context Recovery)

**Date:** January 23, 2026
**Status:** READY FOR RELEASE
**Version:** 2.0.3-OAUTH-FIX

---

## LATEST APK

**File:** `/sdcard/Download/MobileCLI-Pro-v2.0.3-OAUTH-FIX.apk`

This APK includes:
- Google OAuth browser-based flow (fixes crash)
- PayPal payment success deep link handler
- Restore Purchase button fix

---

## APK VERSION HISTORY (For Revert)

| Version | File | Changes |
|---------|------|---------|
| **v2.0.3-OAUTH-FIX** | `MobileCLI-Pro-v2.0.3-OAUTH-FIX.apk` | **LATEST** - Google OAuth crash fix |
| v2.0.2-RESTORE-FIX | `MobileCLI-Pro-v2.0.2-RESTORE-FIX.apk` | Restore Purchase button clickability |
| v2.0.1-PAYMENT-FIX | `MobileCLI-Pro-v2.0.1-PAYMENT-FIX.apk` | PayPal deep link handler |
| v2.0.0-FINAL | `MobileCLI-Pro-v2.0.0-FINAL.apk` | Original release |

All APKs stored in `/sdcard/Download/` for easy revert.

---

## KEY IDS

| Item | Value |
|------|-------|
| PayPal Plan ID | `P-3RH33892X5467024SNFZON2Y` |
| Supabase Project | `mwxlguqukyfberyhtkmg` |
| Webhook URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook` |
| Website | `https://www.mobilecli.com` |
| Success Page | `https://www.mobilecli.com/success` |

---

## FEATURES COMPLETED

### Authentication & Payments
- Google OAuth + Email/Password login
- PayPal subscription ($15/month recurring)
- User ID matching via `custom_id` (any PayPal account works)
- Webhook handles all subscription events
- Multi-device login support
- **NEW:** Payment success deep link handler

### Account Management (Industry Standard)
- Account screen with profile display
- Logout button with confirmation
- Manage Subscription (opens PayPal)
- Restore Purchase functionality
- Delete Account option

### Bug Fixes Applied
- Fixed: Account screen transparent background → Added #121212
- Fixed: Deprecated onBackPressed (Android 13+) → OnBackPressedCallback
- Fixed: Webhook field mismatch → Changed `current_period_end` to `expires_at`
- Fixed: PayPal 404 on return → Added deep link + website success page
- Fixed: Restore Purchase button not responding → Added clickable/focusable attributes
- **NEW:** Fixed: Google OAuth crash → Switched to browser-based PKCE flow

---

## GIT HISTORY (Latest)

```
5400f26 Fix Google OAuth crash - use browser-based flow
4ae869e Fix Restore Purchase button not responding
2af1fbc Update documentation for v2.0.1-PAYMENT-FIX
7728d70 Add payment success deep link handler
4f93451 Update documentation: webhook deployed, all systems ready
d882f05 Update documentation with critical webhook fix discovered in code audit
be0d9ef Fix critical webhook bug: field name mismatch
```

---

## WEBSITE PAGES (Deployed)

| URL | Purpose |
|-----|---------|
| `mobilecli.com/success` | PayPal payment success → opens app |
| `mobilecli.com/cancel` | PayPal payment cancelled |

Vercel auto-deploys from `MobileDevCLI/website` repo.

---

## FILES IN /sdcard/Download/

| File | Purpose |
|------|---------|
| `MobileCLI-Pro-v2.0.3-OAUTH-FIX.apk` | **LATEST - Use for testing** |
| `MobileCLI-Pro-v2.0.2-RESTORE-FIX.apk` | Previous (restore button fix) |
| `MobileCLI-Pro-v2.0.1-PAYMENT-FIX.apk` | Previous (payment deep link) |
| `MobileCLI-Pro-v2.0.0-FINAL.apk` | Original release |
| `FULL_IMPLEMENTATION_PLAN.md` | Complete payment flow documentation |
| `COMPREHENSIVE_AUDIT_v2.txt` | Full code audit report |

---

## TEST FLOW (Updated)

1. Install APK on test phone
2. Login with Google
3. Click Subscribe → Opens PayPal subscription
4. Complete $15 payment (any PayPal account)
5. **PayPal redirects to mobilecli.com/success**
6. **Success page auto-opens app via deep link**
7. App verifies subscription → "Welcome to Pro!"
8. Terminal opens (not paywall)
9. Open drawer → Click "Account" → See profile, Pro status

---

## IMPORTANT FILES

| File | Purpose |
|------|---------|
| `CURRENT_STATE.md` | Quick AI context recovery |
| `docs/ROADMAP_AND_STATUS.md` | Full documentation |
| `CLAUDE.md` | AI environment guide |
| `app/src/main/java/com/termux/auth/PaywallActivity.kt` | PayPal + deep link handler |
| `app/src/main/java/com/termux/auth/AccountActivity.kt` | Account screen |
| `supabase/functions/paypal-webhook/index.ts` | Webhook code |
| `AndroidManifest.xml` | Deep link intent filters |

---

## DEEP LINK CONFIGURATION

App handles these deep links for payment return:
- `com.termux://payment-success` (custom scheme)
- `https://www.mobilecli.com/success` (HTTPS App Link)

Website `vercel.json` has rewrites:
- `/success` → `/success.html`
- `/cancel` → `/cancel.html`

---

## DO NOT MODIFY

- BootstrapInstaller.kt
- SetupWizard.kt
- MainActivity.kt (except drawer setup)
- gradle.properties

---

## PAYMENT FLOW (Industry Standard)

```
User clicks Subscribe
    ↓
PayPal checkout (with custom_id=user_id)
    ↓
User completes payment
    ↓
PayPal webhook → Supabase → status='active'
    ↓
PayPal redirects → mobilecli.com/success
    ↓
Success page → deep link → app opens
    ↓
App verifies → "Welcome to Pro!"
```

---

*Last updated: January 23, 2026*
