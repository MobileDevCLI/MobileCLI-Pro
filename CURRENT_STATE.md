# MobileCLI Pro - Current State (For AI Context Recovery)

**Date:** January 23, 2026
**Status:** TESTING - Browser OAuth implementation
**Version:** 2.0.7-BROWSER-OAUTH

---

## LATEST APK

**File:** `/sdcard/Download/MobileCLI-Pro-v2.0.7-BROWSER-OAUTH.apk`

This APK includes all previous fixes plus:
- **NEW: Browser-based Google OAuth with PKCE**
- Fixed Google login not working (was failing silently with Credential Manager)
- Now opens Chrome Custom Tab for Google authentication
- Manual PKCE code verifier/challenge generation for reliable OAuth
- Works on ALL devices (not dependent on Credential Manager support)

---

## APK VERSION HISTORY (For Revert)

| Version | File | Changes |
|---------|------|---------|
| **v2.0.7-BROWSER-OAUTH** | `MobileCLI-Pro-v2.0.7-BROWSER-OAUTH.apk` | **LATEST** - Browser-based Google OAuth with PKCE |
| v2.0.6-STABLE | `MobileCLI-Pro-v2.0.6-STABLE.apk` | Crash loop fix, stable |
| v2.0.5-FIXED | `MobileCLI-Pro-v2.0.5-FIXED.apk` | LoginActivity onResume fix |
| v2.0.4-GOOGLE-RESTORED | `MobileCLI-Pro-v2.0.4-GOOGLE-RESTORED.apk` | Restored SDK Google OAuth |
| v2.0.3-OAUTH-FIX | `MobileCLI-Pro-v2.0.3-OAUTH-FIX.apk` | Browser-based OAuth attempt |
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

## KNOWN ISSUES (To Fix)

### PayPal custom_id Not Working
- **Problem:** PayPal subscription URLs don't support `custom_id` as URL parameter
- **Impact:** Webhook can't find user unless PayPal email matches Google login email
- **Solution Needed:** Use PayPal JavaScript SDK to pass custom_id properly
- **Workaround:** User must use same email for Google login and PayPal

### Subscription Verification
- User must click "Restore Purchase" manually after payment
- Auto-verification removed to prevent crash loops
- Webhook logs should be checked in Supabase dashboard

---

## FEATURES COMPLETED

### Authentication & Payments
- Google OAuth + Email/Password login
- PayPal subscription ($15/month recurring)
- Webhook handles all subscription events
- Multi-device login support
- Payment success deep link handler

### Account Management (Industry Standard)
- Account screen with profile display
- Logout button with confirmation
- Manage Subscription (opens PayPal)
- Restore Purchase functionality
- Delete Account option

### Bug Fixes Applied (v2.0.7)
- Fixed: Account screen transparent background → Added #121212
- Fixed: Deprecated onBackPressed (Android 13+) → OnBackPressedCallback
- Fixed: Webhook field mismatch → Changed `current_period_end` to `expires_at`
- Fixed: PayPal 404 on return → Added deep link + website success page
- Fixed: Restore Purchase button not responding → Added clickable/focusable attributes
- Fixed: Google OAuth error handling → Better error messages
- Fixed: Crash loop → Removed auto-verification in onResume()
- Fixed: "Immediately kicks away" → LoginActivity no longer auto-redirects
- **Fixed: Google OAuth not working** → Browser-based OAuth with manual PKCE (v2.0.7)

---

## GIT HISTORY (Latest)

```
d5e17bd Implement browser-based Google OAuth with PKCE (v2.0.7-BROWSER-OAUTH)
c7ff93e Fix crash loops in LoginActivity and PaywallActivity (v2.0.6-STABLE)
f3a62e6 Update documentation for v2.0.3-OAUTH-FIX
bd22955 Fix Google OAuth crash - use browser-based flow
5400f26 Fix Google OAuth crash - use browser-based flow
4ae869e Fix Restore Purchase button not responding
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
| `MobileCLI-Pro-v2.0.7-BROWSER-OAUTH.apk` | **LATEST - Use for testing** |
| `MobileCLI-Pro-v2.0.6-STABLE.apk` | Previous (crash loop fix) |
| `MobileCLI-Pro-v2.0.5-FIXED.apk` | Previous (LoginActivity fix) |
| `MobileCLI-Pro-v2.0.4-GOOGLE-RESTORED.apk` | Previous (SDK OAuth) |
| `MobileCLI-Pro-v2.0.3-OAUTH-FIX.apk` | Previous (browser OAuth) |
| `MobileCLI-Pro-v2.0.2-RESTORE-FIX.apk` | Previous (restore button) |
| `MobileCLI-Pro-v2.0.1-PAYMENT-FIX.apk` | Previous (payment deep link) |
| `MobileCLI-Pro-v2.0.0-FINAL.apk` | Original release |

---

## TEST FLOW (Updated)

1. Install APK on test phone
2. Login with Google (use same email as PayPal!)
3. Click Subscribe → Opens PayPal subscription
4. Complete $15 payment
5. Return to app
6. Click "Restore Purchase" to verify subscription
7. If Pro status → Terminal opens
8. If not → Check Supabase webhook logs

---

## IMPORTANT FILES

| File | Purpose |
|------|---------|
| `CURRENT_STATE.md` | Quick AI context recovery |
| `docs/ROADMAP_AND_STATUS.md` | Full documentation |
| `CLAUDE.md` | AI environment guide |
| `app/src/main/java/com/termux/auth/LoginActivity.kt` | Login + Google OAuth |
| `app/src/main/java/com/termux/auth/PaywallActivity.kt` | PayPal + subscription |
| `app/src/main/java/com/termux/auth/LicenseManager.kt` | Subscription verification |
| `supabase/functions/paypal-webhook/index.ts` | Webhook code |
| `supabase/setup_subscriptions.sql` | Database setup SQL |

---

## DEEP LINK CONFIGURATION

App handles these deep links:
- `com.termux://login-callback` (Google OAuth)
- `com.termux://payment-success` (PayPal return)
- `https://www.mobilecli.com/success` (HTTPS App Link)

---

## DO NOT MODIFY

- BootstrapInstaller.kt
- SetupWizard.kt
- MainActivity.kt (except drawer setup)
- gradle.properties

---

## PAYMENT FLOW (Current State)

```
User clicks Subscribe
    ↓
PayPal checkout (custom_id via URL - MAY NOT WORK)
    ↓
User completes payment
    ↓
PayPal webhook → tries to find user by custom_id or email
    ↓
If found → status='active' in database
If not found → subscription not recorded (BUG)
    ↓
User returns to app
    ↓
User clicks "Restore Purchase"
    ↓
App queries database → shows Pro status if found
```

---

*Last updated: January 23, 2026 - v2.0.7-BROWSER-OAUTH*
