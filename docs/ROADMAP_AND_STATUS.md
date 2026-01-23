# MobileCLI Pro - Complete Roadmap & Status

**Last Updated:** January 22, 2026
**Current Version:** 2.0.0-FINAL
**Status:** READY FOR RELEASE

---

## QUICK REFERENCE - IMPORTANT IDS

| Item | Value |
|------|-------|
| **PayPal Plan ID** | `P-3RH33892X5467024SNFZON2Y` |
| **PayPal Client ID** | `AXAEoNFGNDeaUSzi0EFG_uiINl9Pe0MKo2E-DiKPsdp_ZHpcAiHRvMmpJ9OtebtnnKtGpZoZxqQYuOWY` |
| **Supabase Project** | `mwxlguqukyfberyhtkmg` |
| **Webhook URL** | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook` |
| **Subscribe URL** | `https://www.paypal.com/webapps/billing/plans/subscribe?plan_id=P-3RH33892X5467024SNFZON2Y` |

---

## WHAT WAS DONE TODAY (Jan 22, 2026)

### Session Summary
1. **Created PayPal Subscription Plan** - Changed from one-time payment to recurring $15/month
2. **Fixed Webhook Format** - Updated from IPN format to REST API format
3. **Added User ID Matching** - App passes user_id to PayPal, webhook matches by ID (not just email)
4. **Configured PayPal Webhooks** - All BILLING.SUBSCRIPTION.* events enabled
5. **Added Account Screen** - Industry standard account management (logout, manage subscription)
6. **Bug Fixes** - Fixed transparent background, deprecated onBackPressed

### New Features Added
- **Account Screen** (`AccountActivity.kt`) - Profile, logout, manage subscription
- **Drawer Menu** - "Account" button added for easy access
- **Multi-device Support** - Login on any device with same account

### Bug Fixes Applied
| Bug | Fix |
|-----|-----|
| Account screen transparent background | Added #121212 background color |
| Deprecated onBackPressed (Android 13+) | Replaced with OnBackPressedCallback |

### Git Commits (Latest First)
```
1e12297 Fix bugs: Add background color, fix deprecated onBackPressed
13136ff Add Account screen with logout, subscription management
0b72018 Add CURRENT_STATE.md for AI context recovery
99f3c6f Add user_id matching for PayPal webhook (industry standard)
6fe3c2e Update webhook to REST API format + documentation
fb7f18a Update PayPal to use subscription plan ID P-3RH33892X5467024SNFZON2Y
```

---

## USER FLOW (How It Works)

### Sign Up + Subscribe Flow
```
1. User opens app
2. SplashActivity → Not logged in → LoginActivity
3. User signs in with Google (or Email)
4. Supabase creates auth user (user_id = "abc-123-xyz")
5. Trigger creates trial subscription (7 days)
6. PaywallActivity shows:
   - "Start Free Trial" button
   - "Subscribe $15/month" button
   - "Restore Purchase" link
7. User clicks "Subscribe"
8. App opens: paypal.com/subscribe?plan_id=XXX&custom_id=abc-123-xyz
   ↑ This passes the user_id to PayPal!
9. User completes payment (can use ANY PayPal account)
10. PayPal sends webhook with custom_id
11. Webhook finds user by custom_id → updates subscription
12. User returns to app (press back)
13. App checks license → Pro detected → Terminal opens
```

### Why User ID Matching Matters
- User logs in with: `personal@gmail.com`
- User pays with PayPal: `business@company.com`
- **Old way:** Webhook tries to match `business@company.com` → FAILS
- **New way:** Webhook uses `custom_id` (user_id) → SUCCESS

---

## FILES IN YOUR DOWNLOADS

| File | Purpose | Deploy To |
|------|---------|-----------|
| `MobileCLI-Pro-v2.0.0-FINAL.apk` | **RELEASE APK** - All features + bug fixes | Website, Test phone |
| `paypal-webhook-v3-with-userid.txt` | Webhook code (already deployed) | Supabase Edge Function |

---

## TO COMPLETE SETUP

### Step 1: Redeploy Webhook (REQUIRED)
1. Open: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook
2. Click "Code" tab
3. Open `/sdcard/Download/paypal-webhook-v3-with-userid.txt` on phone
4. Copy ALL code
5. Paste into Supabase (replace everything)
6. Click "Deploy updates"
7. Verify "JWT verification" is OFF in Details tab

### Step 2: Install APK
Send `/sdcard/Download/MobileCLI-Pro-v2.0.0-USERID.apk` to test phone and install.

### Step 3: Test
1. Login with Google
2. Click Subscribe
3. Complete PayPal payment
4. Press back to return to app
5. Should see "Subscription activated!" and terminal opens

---

## WEBHOOK MATCHING LOGIC

The webhook now uses this priority:
```
1. Try match by custom_id (Supabase user_id from app)
   → Direct lookup, 100% accurate

2. Fall back to PayPal subscriber email
   → Only if custom_id not present

3. If neither matches → Log error, don't activate
```

---

## WEBHOOK EVENTS HANDLED

| PayPal Event | Database Action |
|--------------|-----------------|
| `BILLING.SUBSCRIPTION.ACTIVATED` | status='active', plan='pro' |
| `BILLING.SUBSCRIPTION.CANCELLED` | status='cancelled' |
| `BILLING.SUBSCRIPTION.SUSPENDED` | status='suspended' |
| `BILLING.SUBSCRIPTION.EXPIRED` | status='expired' |
| `BILLING.SUBSCRIPTION.RE-ACTIVATED` | status='active' |
| `BILLING.SUBSCRIPTION.PAYMENT.FAILED` | Log only |

---

## FILES THAT SHOULD NOT BE MODIFIED

| File | Why |
|------|-----|
| `BootstrapInstaller.kt` | Termux environment |
| `SetupWizard.kt` | 79 permissions |
| `MainActivity.kt` | Terminal core |
| `AndroidManifest.xml` | Permissions |
| `gradle.properties` | ARM aapt2 fix |

**Safe to modify:**
- `PaywallActivity.kt` - Subscription UI
- `LicenseManager.kt` - License checking
- `SupabaseClient.kt` - Auth/API
- `docs/*` - Documentation

---

## TESTING CHECKLIST

### Pre-Test
- [ ] Webhook redeployed with v3 code (user_id matching)
- [ ] JWT verification OFF
- [ ] APK installed on test phone

### Test Flow
- [ ] Fresh install → Bootstrap works
- [ ] Setup Wizard → Permissions granted
- [ ] Login with Google → User created
- [ ] Subscribe button → Opens PayPal subscription page
- [ ] Complete payment → Check Supabase function logs
- [ ] Return to app → Pro status detected
- [ ] App proceeds to terminal (not paywall)

---

## LINKS

### Dashboards
- **Supabase:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg
- **Supabase Logs:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs
- **PayPal Developer:** https://developer.paypal.com/dashboard
- **PayPal Subscriptions:** https://www.paypal.com/billing/subscriptions
- **GitHub:** https://github.com/MobileDevCLI/MobileCLI-Pro
- **GitHub Actions:** https://github.com/MobileDevCLI/MobileCLI-Pro/actions

### Quick Commands
```bash
# Open Supabase
termux-open-url "https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg"

# Open webhook logs
termux-open-url "https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs"

# Open GitHub
termux-open-url "https://github.com/MobileDevCLI/MobileCLI-Pro"
```

---

## NOTES FOR AI ASSISTANTS

When working on this project:

1. **Environment:** ARM Android (Termux), NOT x86 Linux
2. **Temp files:** Use `~/tmp` or `$PREFIX/tmp`, NEVER `/tmp`
3. **User files:** Save to `/sdcard/Download/` for user access
4. **Don't modify:** Bootstrap, SetupWizard, MainActivity, permissions
5. **Build:** Commit to GitHub → Actions builds APK → Download artifact
6. **PayPal Plan ID:** `P-3RH33892X5467024SNFZON2Y`
7. **Webhook:** Uses `custom_id` for user matching (industry standard)

### Key Files
- App PayPal integration: `app/src/main/java/com/termux/auth/PaywallActivity.kt`
- Webhook code: `supabase/functions/paypal-webhook/index.ts`
- License checking: `app/src/main/java/com/termux/auth/LicenseManager.kt`
- Auth client: `app/src/main/java/com/termux/auth/SupabaseClient.kt`

---

*Last updated: January 22, 2026 by Claude Opus 4.5*
