# MobileCLI Pro - Current State (For AI Context Recovery)

**Date:** January 25, 2026
**Status:** Auth/Payment Flow Production Ready
**Version:** 2.0.0-rc.3 (Build 15)

---

## LATEST WORK: Auth/Payment Flow Fixes

**Completed January 25, 2026**

### Changes Made
1. **Fixed support email** - Changed `support@mobilecli.com` to `mobiledevcli@gmail.com`
2. **Added webhook logging** - All PayPal events logged to `webhook_logs` table
3. **Added payment history** - All payments recorded to `payment_history` table
4. **Processing tracking** - Webhook marks events as processed with result status

### New Database Tables Required
Run in Supabase SQL Editor:
```sql
CREATE TABLE IF NOT EXISTS webhook_logs (...);
CREATE TABLE IF NOT EXISTS payment_history (...);
```
See full SQL in docs or commit message.

---

## PREVIOUS WORK: PayPal Documentation Archive

**Completed January 25, 2026**

Created complete PayPal integration documentation in `docs/paypal/`:

| File | Purpose |
|------|---------|
| `README.md` | Overview and quick start |
| `STORY.md` | Full development history |
| `SETUP_GUIDE.md` | Step-by-step setup from scratch |
| `WEBHOOK_CODE.md` | Working webhook with explanations |
| `DATABASE_SCHEMA.md` | All SQL needed |
| `TROUBLESHOOTING.md` | Common problems and solutions |
| `TEST_PAYLOADS.md` | How to test webhooks |

**Key Fix Documented:** Changed `.update()` to `.upsert()` in webhook to fix silent failures.

---

## LATEST APK

**File:** `/sdcard/Download/MobileCLI-Pro-v2.0.0-rc.3.apk`

This APK includes all previous fixes plus:
- **Fixed AccountActivity delete email** - Now uses correct mobiledevcli@gmail.com
- All support emails now correct
- Webhook logging and payment history
- Browser-based Google OAuth
- Back button navigation fix

---

## APK VERSION HISTORY (For Revert)

| Version | File | Changes |
|---------|------|---------|
| **v2.0.0-rc.3** | `MobileCLI-Pro-v2.0.0-rc.3.apk` | **LATEST** - All email fixes complete |
| v2.0.0-rc.2 | `MobileCLI-Pro-v2.0.0-rc.2.apk` | Support email fix, webhook logging |
| v2.0.8-BACKFIX | `MobileCLI-Pro-v2.0.8-BACKFIX.apk` | Back button navigation fix |
| v2.0.7-BROWSER-OAUTH | `MobileCLI-Pro-v2.0.7-BROWSER-OAUTH.apk` | Browser-based Google OAuth with PKCE |
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
| PayPal Button ID | `DHCKPWE3PJ684` |
| Supabase Project | `mwxlguqukyfberyhtkmg` |
| Webhook URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook` |
| Website | `https://www.mobilecli.com` |
| Success Page | `https://www.mobilecli.com/success` |

---

## PAYPAL STATUS

**Status:** WORKING and DOCUMENTED

**The Fix (January 25, 2026):**
- Problem: Webhook using `.update()` returned empty array when no row matched
- Solution: Changed to `.upsert()` with `onConflict: "user_id"`
- Now creates subscription row if missing, updates if exists

**Full Documentation:** See `docs/paypal/` directory

---

## NEXT STEPS: Stripe Migration

The PayPal integration is now fully documented. To switch to Stripe:

1. PayPal documentation preserved in `docs/paypal/`
2. Can revert to PayPal anytime using the documentation
3. Stripe integration would follow similar pattern with different webhook events

---

## KNOWN ISSUES (To Fix)

### PayPal custom_id Reliability
- **Problem:** PayPal subscription URLs don't reliably pass `custom_id` as URL parameter
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
- PayPal subscription ($4.99/month recurring)
- Webhook handles all subscription events (with UPSERT fix)
- Multi-device login support
- Payment success deep link handler

### Account Management (Industry Standard)
- Account screen with profile display
- Logout button with confirmation
- Manage Subscription (opens PayPal)
- Restore Purchase functionality
- Delete Account option

### Bug Fixes Applied
- Fixed: Account screen transparent background
- Fixed: Deprecated onBackPressed (Android 13+)
- Fixed: Webhook field mismatch
- Fixed: PayPal 404 on return
- Fixed: Restore Purchase button not responding
- Fixed: Google OAuth error handling
- Fixed: Crash loop
- Fixed: "Immediately kicks away" issue
- Fixed: Google OAuth not working (browser-based OAuth)
- **Fixed: Webhook silent failure (UPSERT)**

---

## GIT TAGS

| Tag | Description |
|-----|-------------|
| `paypal-working-jan25` | Complete working PayPal integration |

To recover PayPal:
```bash
git checkout paypal-working-jan25
# Read docs/paypal/SETUP_GUIDE.md
```

---

## IMPORTANT FILES

| File | Purpose |
|------|---------|
| `CURRENT_STATE.md` | Quick AI context recovery |
| `docs/paypal/` | **Complete PayPal archive** |
| `CLAUDE.md` | AI environment guide |
| `app/src/main/java/com/termux/auth/LoginActivity.kt` | Login + Google OAuth |
| `app/src/main/java/com/termux/auth/PaywallActivity.kt` | PayPal + subscription |
| `app/src/main/java/com/termux/auth/LicenseManager.kt` | Subscription verification |
| `supabase/functions/paypal-webhook/index.ts` | Webhook code (UPSERT version) |
| `supabase/setup_subscriptions.sql` | Database setup SQL |

---

## DO NOT MODIFY

- BootstrapInstaller.kt
- SetupWizard.kt
- MainActivity.kt (except drawer setup)
- gradle.properties

---

## PAYMENT FLOW (Fixed)

```
User clicks Subscribe
    |
    v
PayPal checkout (with custom_id = user_id)
    |
    v
User completes payment
    |
    v
PayPal webhook -> UPSERT (creates row if needed)
    |
    v
status = 'active' in database
    |
    v
User returns to app
    |
    v
User clicks "Restore Purchase" -> Pro access granted
```

---

*Last updated: January 25, 2026 - All email fixes complete (rc.3)*
