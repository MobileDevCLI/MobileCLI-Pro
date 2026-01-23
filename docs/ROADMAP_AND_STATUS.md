# MobileCLI Pro - Complete Roadmap & Status

**Last Updated:** January 22, 2026
**Current Version:** 2.0.0-rc.1 (Build 135)
**Status:** Release Candidate - Needs Testing & Webhook Setup

---

## TABLE OF CONTENTS

1. [Project Overview](#project-overview)
2. [What's Been Completed](#whats-been-completed)
3. [What's In Progress](#whats-in-progress)
4. [What Still Needs To Be Done](#what-still-needs-to-be-done)
5. [Step-by-Step Setup Guide](#step-by-step-setup-guide)
6. [Testing Checklist](#testing-checklist)
7. [Important Files & Locations](#important-files--locations)
8. [URLs & Credentials](#urls--credentials)
9. [Known Issues](#known-issues)
10. [Future Improvements](#future-improvements)

---

## PROJECT OVERVIEW

MobileCLI Pro is a commercial Android terminal app with:
- **Subscription model:** $15/month via PayPal
- **Authentication:** Supabase (Google OAuth + Email/Password)
- **Trial:** 7 days free for new users
- **Backend:** Supabase (PostgreSQL + Auth + Edge Functions)

### Business Model
```
User Signs Up → 7-Day Trial → Paywall → PayPal Payment → Pro Access
```

### Technical Stack
| Component | Technology |
|-----------|------------|
| App | Kotlin/Android |
| Auth | Supabase Auth (PKCE OAuth) |
| Database | Supabase PostgreSQL |
| Payments | PayPal Subscriptions |
| Webhooks | Supabase Edge Functions (Deno) |

---

## WHAT'S BEEN COMPLETED

### ✅ Authentication System
- [x] Supabase project created and configured
- [x] Google OAuth configured with PKCE flow
- [x] Email/password authentication
- [x] Deep link handling for OAuth callbacks
- [x] SupabaseClient.kt singleton
- [x] LoginActivity.kt with Google + Email login
- [x] SplashActivity.kt for auth checking

### ✅ Subscription System (Code)
- [x] `subscriptions` table SQL created (`supabase/setup_subscriptions.sql`)
- [x] Row Level Security (RLS) policies
- [x] Auto-create trial on signup (PostgreSQL trigger)
- [x] LicenseManager.kt for subscription checking
- [x] PaywallActivity.kt for subscription prompts
- [x] Encrypted local caching (EncryptedSharedPreferences)
- [x] 30-day offline grace period

### ✅ PayPal Integration (Code)
- [x] PayPal Business account configured
- [x] Subscription button created (ID: `DHCKPWE3PJ684`)
- [x] Payment URL: `https://www.paypal.com/ncp/payment/DHCKPWE3PJ684`
- [x] PaywallActivity opens PayPal checkout
- [x] Webhook code written (`supabase/functions/paypal-webhook/index.ts`)

### ✅ Security
- [x] Removed "Skip Login" bypass (was security risk)
- [x] RLS on subscriptions table
- [x] Service role key only used in webhook (not in app)
- [x] Encrypted local storage for license cache

### ✅ Legal Documents
- [x] Terms of Service (`docs/TERMS_OF_SERVICE.md`)
- [x] Privacy Policy (`docs/PRIVACY_POLICY.md`)
- [x] Legal Disclaimers (`docs/LEGAL_DISCLAIMERS.md`)

### ✅ Documentation
- [x] CLAUDE.md - AI environment guide (ARM/Termux specifics)
- [x] AUTH_SETUP.md - Authentication configuration
- [x] PAYMENTS_SETUP.md - PayPal configuration
- [x] SUBSCRIPTION_SETUP.md - Database setup
- [x] WEBHOOK_SETUP.md - Edge Function deployment
- [x] CHANGELOG.md - Version history

### ✅ Version Control
- [x] All code committed to GitHub
- [x] GitHub Actions CI/CD for APK builds
- [x] Latest commit: `2ee5cb7` (CLAUDE.md update)

---

## WHAT'S IN PROGRESS

### 🔄 PayPal Webhook Deployment
**Status:** Code written, NOT YET DEPLOYED to Supabase

The webhook code exists at:
```
supabase/functions/paypal-webhook/index.ts
```

It needs to be deployed to Supabase Edge Functions via the dashboard.

### 🔄 PayPal IPN Configuration
**Status:** NOT YET CONFIGURED

PayPal needs to be told where to send payment notifications.

---

## WHAT STILL NEEDS TO BE DONE

### 1. Deploy PayPal Webhook to Supabase
**Priority:** HIGH
**Time:** 5 minutes
**Difficulty:** Easy (copy-paste)

### 2. Configure PayPal IPN URL
**Priority:** HIGH
**Time:** 2 minutes
**Difficulty:** Easy

### 3. Test the APK
**Priority:** HIGH
**Time:** 15 minutes
**What to test:**
- Fresh install
- Bootstrap runs correctly
- Setup wizard works
- Google login works
- Email login works
- Trial period shows correctly
- PayPal checkout opens
- Subscription check works

### 4. Activate Existing Subscriber (Manual)
**Priority:** MEDIUM
**Time:** 2 minutes
**Note:** Until webhook is live, manually activate via SQL

### 5. End-to-End Payment Test
**Priority:** HIGH
**Time:** 10 minutes
**Test:** Complete a real $15 payment and verify auto-activation

---

## STEP-BY-STEP SETUP GUIDE

### STEP 1: Deploy Webhook to Supabase

1. Open: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions
2. Click "Create a new function"
3. Name: `paypal-webhook`
4. Open file: `/sdcard/Download/paypal-webhook-code.txt` (or `supabase/functions/paypal-webhook/index.ts`)
5. Copy ALL the code
6. Paste into Supabase editor (replace any template code)
7. Click "Deploy function"
8. Wait for deployment to complete
9. Note the URL: `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook`

### STEP 2: Configure PayPal IPN

1. Open: https://www.paypal.com/ipn
2. Click "Choose IPN Settings"
3. Notification URL: `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook`
4. IPN messages: Select "Receive IPN messages (Enabled)"
5. Click "Save"

### STEP 3: Test Webhook (Optional)

```bash
curl -X POST https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "txn_type=subscr_signup&payer_email=test@example.com"
```

Should return: `OK`

### STEP 4: Activate Existing Subscriber

1. Open: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql/new
2. Find user ID:
```sql
SELECT id, email, created_at FROM auth.users ORDER BY created_at DESC;
```
3. Activate them:
```sql
UPDATE subscriptions
SET status = 'active',
    expires_at = NOW() + INTERVAL '30 days',
    updated_at = NOW()
WHERE user_id = 'USER_ID_HERE';
```

### STEP 5: Install & Test APK

APK Location: `/sdcard/Download/MobileCLI-Pro-v2.0.0-rc1.apk`

Test flow:
1. Uninstall old version
2. Install RC1
3. Open app → Bootstrap should run
4. Setup wizard → Grant permissions
5. Login screen → Try Google login
6. Should see paywall OR main app (depending on subscription)

---

## TESTING CHECKLIST

### App Installation
- [ ] APK installs without error
- [ ] App icon appears in launcher
- [ ] App opens without crash

### Bootstrap
- [ ] Bootstrap detects first run
- [ ] Bootstrap extracts files correctly
- [ ] Shell environment works after bootstrap

### Setup Wizard
- [ ] Wizard appears on first run
- [ ] All permissions can be granted
- [ ] Wizard completes successfully

### Authentication
- [ ] Login screen appears
- [ ] Google login button works
- [ ] Google OAuth flow completes
- [ ] Email signup works
- [ ] Email login works
- [ ] Session persists after app restart

### Subscription Flow
- [ ] New user gets 7-day trial
- [ ] Trial expiry date is correct
- [ ] Paywall appears for trial users
- [ ] PayPal checkout opens from paywall
- [ ] "Restore Purchase" button works

### Terminal
- [ ] Terminal displays after auth
- [ ] Commands execute correctly
- [ ] Extra keys work (Ctrl, Alt, etc.)
- [ ] Multiple sessions work

### Offline Mode
- [ ] App works without internet (cached license)
- [ ] Shows appropriate message when offline

---

## IMPORTANT FILES & LOCATIONS

### Source Code (App)
| File | Purpose |
|------|---------|
| `app/src/main/java/com/termux/auth/SupabaseClient.kt` | Supabase connection |
| `app/src/main/java/com/termux/auth/LicenseManager.kt` | Subscription checking |
| `app/src/main/java/com/termux/auth/LoginActivity.kt` | Login UI |
| `app/src/main/java/com/termux/auth/PaywallActivity.kt` | Paywall/checkout |
| `app/src/main/java/com/termux/auth/SplashActivity.kt` | Entry point, auth check |
| `app/src/main/java/com/termux/SetupWizard.kt` | Permission wizard |
| `app/src/main/java/com/termux/BootstrapInstaller.kt` | Environment setup |
| `app/src/main/java/com/termux/MainActivity.kt` | Terminal UI |

### Supabase (Backend)
| File | Purpose |
|------|---------|
| `supabase/setup_subscriptions.sql` | Database table + RLS + triggers |
| `supabase/functions/paypal-webhook/index.ts` | PayPal IPN handler |

### Documentation
| File | Purpose |
|------|---------|
| `CLAUDE.md` | AI assistant guide (Termux/ARM specifics) |
| `CHANGELOG.md` | Version history |
| `docs/ROADMAP_AND_STATUS.md` | This file - complete status |
| `docs/AUTH_SETUP.md` | Auth configuration |
| `docs/PAYMENTS_SETUP.md` | PayPal setup |
| `docs/SUBSCRIPTION_SETUP.md` | Database setup |
| `docs/WEBHOOK_SETUP.md` | Edge function deployment |
| `docs/TERMS_OF_SERVICE.md` | Legal - ToS |
| `docs/PRIVACY_POLICY.md` | Legal - Privacy |
| `docs/LEGAL_DISCLAIMERS.md` | Legal - Disclaimers |

### Build Output
| Location | Description |
|----------|-------------|
| `/sdcard/Download/MobileCLI-Pro-v2.0.0-rc1.apk` | Latest APK for testing |
| `/sdcard/Download/paypal-webhook-code.txt` | Webhook code for easy copying |

---

## URLS & CREDENTIALS

### Supabase
| Item | Value |
|------|-------|
| Project URL | `https://mwxlguqukyfberyhtkmg.supabase.co` |
| Project ID | `mwxlguqukyfberyhtkmg` |
| Dashboard | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg |
| SQL Editor | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql/new |
| Edge Functions | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions |
| Anon Key | In `SupabaseClient.kt` (safe for client) |
| Service Key | In Supabase dashboard only (NEVER in app code) |

### PayPal
| Item | Value |
|------|-------|
| Button ID | `DHCKPWE3PJ684` |
| Payment URL | `https://www.paypal.com/ncp/payment/DHCKPWE3PJ684` |
| Price | $15.00 USD/month |
| IPN Settings | https://www.paypal.com/ipn |
| IPN History | https://www.paypal.com/ipn/history |
| Subscriptions | https://www.paypal.com/billing/subscriptions |

### GitHub
| Item | Value |
|------|-------|
| Repository | https://github.com/MobileDevCLI/MobileCLI-Pro |
| Actions (Builds) | https://github.com/MobileDevCLI/MobileCLI-Pro/actions |
| Releases | https://github.com/MobileDevCLI/MobileCLI-Pro/releases |

### Webhook URL (after deployment)
```
https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook
```

---

## KNOWN ISSUES

### 1. /tmp Permission Denied (Claude Code on ARM)
**Issue:** Claude Code tries to use `/tmp` which isn't accessible on Termux
**Workaround:** Use `~/tmp` or `$PREFIX/tmp` instead
**Status:** Documented in CLAUDE.md

### 2. Supabase CLI Won't Install via npm
**Issue:** `npm install -g supabase` fails on ARM
**Workaround:** Use Supabase dashboard instead
**Status:** Use web dashboard for Edge Function deployment

### 3. GitHub Artifact Download Issues
**Issue:** `gh run download` sometimes fails
**Workaround:** Use background `curl` or `wget`
**Status:** Documented in CLAUDE.md

---

## FUTURE IMPROVEMENTS

### Short Term (Before Sale)
- [ ] Complete webhook deployment
- [ ] Full end-to-end testing
- [ ] Test on multiple devices

### Medium Term (After Launch)
- [ ] Add PayPal IPN verification (post back to PayPal)
- [ ] Add email notifications for subscription events
- [ ] Add in-app subscription management
- [ ] Add receipt/invoice generation

### Long Term
- [ ] Add alternative payment methods (Stripe, Google Play)
- [ ] Add team/enterprise subscriptions
- [ ] Add usage analytics
- [ ] Add referral system

---

## QUICK COMMANDS FOR FUTURE AI SESSIONS

### Check Git Status
```bash
cd ~/MobileCLI-Pro && git status && git log --oneline -5
```

### Build & Download APK
```bash
cd ~/MobileCLI-Pro && git push origin master
# Wait for GitHub Actions, then:
gh run list --limit 1
# Download when complete
```

### Copy Webhook Code to Clipboard
```bash
cat ~/MobileCLI-Pro/supabase/functions/paypal-webhook/index.ts | termux-clipboard-set
```

### Open Supabase Dashboard
```bash
termux-open-url "https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg"
```

### Open PayPal IPN Settings
```bash
termux-open-url "https://www.paypal.com/ipn"
```

---

## SUMMARY

**What works:** Authentication, subscription checking, PayPal checkout, trial system, offline caching

**What needs setup:** Deploy webhook to Supabase, configure PayPal IPN

**What needs testing:** Full end-to-end flow on fresh install

**Ready to sell?** Almost - complete the webhook setup and testing first.

---

*This document should be read by any AI continuing work on this project.*
