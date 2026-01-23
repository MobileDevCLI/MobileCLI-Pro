# MobileCLI Pro - Complete Roadmap & Status

**Last Updated:** January 22, 2026
**Current Version:** 2.0.0-rc.2 (Build 136)
**Status:** Webhook Needs Redeployment

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

## USER FLOW (How It Works)

### Sign Up + Subscribe Flow
```
1. User opens app
2. SplashActivity → Not logged in → LoginActivity
3. User signs in with Google (or Email)
4. Supabase creates auth user
5. Trigger creates trial subscription (7 days, status='trialing')
6. PaywallActivity shows options:
   - "Start Free Trial" (if trial available)
   - "Subscribe $15/month"
   - "Restore Purchase"
7. User clicks "Subscribe"
8. App opens PayPal subscription page (Custom Tab / Browser)
9. User completes PayPal subscription
10. PayPal sends webhook → BILLING.SUBSCRIPTION.ACTIVATED
11. Webhook updates database → status='active', plan='pro'
12. User returns to app (back button or "Return to merchant")
13. App's onResume() calls LicenseManager.verifyLicense()
14. Server returns Pro status → Proceed to MainActivity (terminal)
```

### Recurring Payment Flow (Monthly)
```
1. PayPal auto-charges user $15
2. PayPal sends webhook → BILLING.SUBSCRIPTION.ACTIVATED or PAYMENT event
3. Webhook extends subscription → current_period_end + 30 days
4. User opens app → Still Pro, no interruption
```

### Payment Failed / Cancelled Flow
```
1. PayPal payment fails or user cancels
2. PayPal sends webhook → BILLING.SUBSCRIPTION.CANCELLED or SUSPENDED
3. Webhook updates database → status='cancelled' or 'suspended'
4. User still has access until current_period_end
5. After expiry → PaywallActivity shows again
```

### Restore Purchase Flow
```
1. User clicks "Restore Purchase"
2. LicenseManager.verifyLicense() checks Supabase
3. If active subscription found → Proceed to app
4. If not found → "No active subscription"
```

---

## WHAT'S CONFIGURED

### PayPal Subscription (Created Jan 22, 2026)
- ✅ Subscription Plan: "Monthly Pro"
- ✅ Plan ID: `P-3RH33892X5467024SNFZON2Y`
- ✅ Price: $15.00 USD / month (recurring)
- ✅ Webhook events: All BILLING.SUBSCRIPTION.* events
- ✅ App uses subscription URL (not one-time payment)

### Supabase
- ✅ Auth: Google OAuth + Email/Password
- ✅ Database: subscriptions table with RLS
- ✅ Trigger: Auto-create trial on signup
- ✅ Edge Function: paypal-webhook (deployed, needs code update)

### App
- ✅ PaywallActivity with Subscribe/Trial/Restore buttons
- ✅ LicenseManager checks server on resume
- ✅ Encrypted local cache for offline
- ✅ 30-day offline grace period

---

## WHAT NEEDS TO BE DONE

### ⚠️ CRITICAL: Redeploy Webhook

The webhook code was updated to handle PayPal REST events (not IPN).

**Steps:**
1. Open: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook
2. Open file: `/sdcard/Download/paypal-webhook-v2.txt`
3. Copy ALL the code
4. Paste into Supabase editor (replace existing)
5. Click "Deploy function"
6. Verify "JWT verification" is OFF

**Why this matters:** The old code expected IPN format (`txn_type=subscr_signup`), but PayPal REST sends JSON (`event_type: BILLING.SUBSCRIPTION.ACTIVATED`).

---

## FILES THAT SHOULD NOT BE MODIFIED

These files are critical - don't change unless absolutely necessary:

| File | Why |
|------|-----|
| `BootstrapInstaller.kt` | Termux environment setup |
| `SetupWizard.kt` | 79 permissions configuration |
| `MainActivity.kt` | Terminal core |
| `AndroidManifest.xml` | Permissions |
| `gradle.properties` | ARM aapt2 fix |
| `build.gradle` | Dependencies |

**Safe to modify:**
- `PaywallActivity.kt` - Subscription UI
- `LicenseManager.kt` - License checking
- `LoginActivity.kt` - Auth UI
- `docs/*` - Documentation

---

## APK VERSIONS

| Version | File | Description |
|---------|------|-------------|
| rc.2 | `MobileCLI-Pro-v2.0.0-SUBSCRIPTION.apk` | Uses subscription plan |
| rc.1 | `MobileCLI-Pro-v2.0.0-rc1-FINAL.apk` | Used one-time payment |

**Current:** Use `MobileCLI-Pro-v2.0.0-SUBSCRIPTION.apk`

---

## TESTING CHECKLIST

### Pre-Test
- [ ] Webhook redeployed with new code
- [ ] JWT verification OFF

### Test Cases
- [ ] Fresh install → Bootstrap works
- [ ] Setup Wizard → Permissions granted
- [ ] Login with Google → User created in Supabase
- [ ] Trial shows → 7 days remaining
- [ ] Subscribe button → Opens PayPal subscription (not one-time)
- [ ] Complete payment → Webhook fires (check logs)
- [ ] Return to app → Pro status detected
- [ ] Restore Purchase → Works for existing subscriber

---

## WEBHOOK EVENTS HANDLED

| PayPal Event | Action |
|--------------|--------|
| `BILLING.SUBSCRIPTION.ACTIVATED` | Set status='active', plan='pro' |
| `BILLING.SUBSCRIPTION.CANCELLED` | Set status='cancelled' |
| `BILLING.SUBSCRIPTION.SUSPENDED` | Set status='suspended' |
| `BILLING.SUBSCRIPTION.EXPIRED` | Set status='expired' |
| `BILLING.SUBSCRIPTION.RE-ACTIVATED` | Set status='active' |
| `BILLING.SUBSCRIPTION.PAYMENT.FAILED` | Log only (PayPal retries) |

---

## LINKS

### Dashboards
- Supabase: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg
- PayPal Developer: https://developer.paypal.com/dashboard
- PayPal Subscriptions: https://www.paypal.com/billing/subscriptions
- GitHub Actions: https://github.com/MobileDevCLI/MobileCLI-Pro/actions

### Quick Open Commands
```bash
# Supabase Dashboard
termux-open-url "https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg"

# Supabase Function Logs
termux-open-url "https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs"

# PayPal Subscriptions
termux-open-url "https://www.paypal.com/billing/subscriptions"

# GitHub
termux-open-url "https://github.com/MobileDevCLI/MobileCLI-Pro"
```

---

## NOTES FOR AI

When working on this project:
1. This is ARM Android (Termux), not Linux - use `termux-*` commands
2. Never use `/tmp` - use `~/tmp` or `$PREFIX/tmp`
3. Save files to `/sdcard/Download/` for user access
4. Don't modify bootstrap/permissions files
5. Commit changes to GitHub, use Actions for builds
6. PayPal Plan ID: `P-3RH33892X5467024SNFZON2Y`

---

*Last updated: January 22, 2026 by Claude*
