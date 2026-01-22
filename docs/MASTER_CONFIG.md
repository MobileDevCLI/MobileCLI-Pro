# MobileCLI Pro - Master Configuration & Security Guide

**IMPORTANT: This file contains sensitive configuration references. Keep it secure.**

Last Updated: January 22, 2026

---

## Quick Reference - All Your Dashboards

| Service | Dashboard URL |
|---------|---------------|
| **Supabase** | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg |
| **PayPal Business** | https://www.paypal.com/businesshub/ |
| **PayPal Subscriptions** | https://www.paypal.com/billing/subscriptions |
| **PayPal Buttons** | https://www.paypal.com/buttons/ |
| **Google Cloud Console** | https://console.cloud.google.com/apis/credentials |
| **GitHub Repo** | https://github.com/MobileDevCLI/MobileCLI-Pro |
| **GitHub Releases** | https://github.com/MobileDevCLI/MobileCLI-Pro/releases |

---

## Credentials & Keys

### Supabase (Authentication & Database)

| Item | Value |
|------|-------|
| **Project ID** | `mwxlguqukyfberyhtkmg` |
| **Project URL** | `https://mwxlguqukyfberyhtkmg.supabase.co` |
| **Anon Key** | `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im13eGxndXF1a3lmYmVyeWh0a21nIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njc0OTg5ODgsImV4cCI6MjA4MzA3NDk4OH0.VdpU9WzYpTyLeVX9RaXKBP3dNNNf0t9YkQfVf7x_TA8` |

**Note:** The anon key is safe to include in client code - it's protected by Row Level Security (RLS).

**Service Role Key:** Stored only in Supabase dashboard (NEVER put in app code)

### Google OAuth

| Item | Where to Find |
|------|---------------|
| **Client ID** | Google Cloud Console → APIs & Credentials → OAuth 2.0 Client IDs |
| **Client Secret** | Google Cloud Console (also stored in Supabase Auth settings) |
| **Authorized Redirect URI** | `https://mwxlguqukyfberyhtkmg.supabase.co/auth/v1/callback` |

### PayPal

| Item | Value |
|------|-------|
| **Subscription Button ID** | `DHCKPWE3PJ684` |
| **Subscription Price** | $15.00 USD / month |
| **Payment Link** | `https://www.paypal.com/ncp/payment/DHCKPWE3PJ684` |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        MobileCLI Pro App                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐        │
│  │   Login     │───▶│   Paywall   │───▶│  Main App   │        │
│  │  Activity   │    │  Activity   │    │  Activity   │        │
│  └──────┬──────┘    └──────┬──────┘    └─────────────┘        │
│         │                  │                                    │
│         ▼                  ▼                                    │
│  ┌─────────────┐    ┌─────────────┐                            │
│  │  Supabase   │    │   PayPal    │                            │
│  │    Auth     │    │  Checkout   │                            │
│  └─────────────┘    └─────────────┘                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      External Services                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐        │
│  │  Supabase   │    │   Google    │    │   PayPal    │        │
│  │  Database   │    │   OAuth     │    │  Payments   │        │
│  └─────────────┘    └─────────────┘    └─────────────┘        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Authentication Flow

### Email/Password Login
```
User enters email + password
        │
        ▼
SupabaseClient.auth.signInWith(Email)
        │
        ▼
Supabase validates credentials
        │
        ▼
Returns session token
        │
        ▼
App stores session, proceeds to Paywall/Main
```

### Google OAuth Login
```
User taps "Continue with Google"
        │
        ▼
App calls SupabaseClient.auth.signInWith(Google)
        │
        ▼
Opens Google sign-in page
        │
        ▼
User authenticates with Google
        │
        ▼
Google redirects to Supabase callback:
https://mwxlguqukyfberyhtkmg.supabase.co/auth/v1/callback
        │
        ▼
Supabase redirects to app:
com.termux://login-callback?code=XXX
        │
        ▼
App receives deep link in onNewIntent()
        │
        ▼
App calls auth.exchangeCodeForSession(code)
        │
        ▼
Session established, proceed to Paywall/Main
```

---

## Payment Flow

```
User on Paywall screen
        │
        ▼
Taps "Subscribe - $15/month"
        │
        ▼
App opens PayPal URL in Custom Tab:
https://www.paypal.com/ncp/payment/DHCKPWE3PJ684
        │
        ▼
User logs into PayPal
        │
        ▼
User confirms $15/month subscription
        │
        ▼
PayPal processes payment
        │
        ▼
User returns to app
        │
        ▼
App checks license status (TODO: implement webhook)
```

---

## Key Files in Codebase

### Authentication
| File | Purpose |
|------|---------|
| `app/src/main/java/com/termux/auth/LoginActivity.kt` | Login UI, OAuth handling |
| `app/src/main/java/com/termux/auth/SupabaseClient.kt` | Supabase singleton, auth methods |
| `app/src/main/java/com/termux/auth/LicenseManager.kt` | License storage & verification |
| `app/src/main/AndroidManifest.xml` | Deep link configuration |

### Payments
| File | Purpose |
|------|---------|
| `app/src/main/java/com/termux/auth/PaywallActivity.kt` | Paywall UI, PayPal checkout |

### Layouts
| File | Purpose |
|------|---------|
| `app/src/main/res/layout/activity_login.xml` | Login screen layout |
| `app/src/main/res/layout/activity_paywall.xml` | Paywall screen layout |

### Build & CI
| File | Purpose |
|------|---------|
| `.github/workflows/build.yml` | GitHub Actions auto-build |
| `app/build.gradle.kts` | App build configuration |
| `gradle.properties` | Gradle settings (ARM aapt2 override) |

---

## How to Update Things

### Change Subscription Price
1. Go to https://www.paypal.com/buttons/
2. Find your subscription button
3. Edit the price
4. Save
5. **No app update needed** - same button ID

### Change Supabase Project
1. Update `SUPABASE_URL` and `SUPABASE_ANON_KEY` in `SupabaseClient.kt`
2. Update Google OAuth redirect URI to new Supabase callback
3. Rebuild app

### Change Google OAuth
1. Go to Google Cloud Console
2. Update OAuth client settings
3. Update Client ID/Secret in Supabase Auth settings
4. **No app update needed** - credentials are in Supabase

### Add New Login Provider (e.g., Apple, GitHub)
1. Enable provider in Supabase → Auth → Providers
2. Configure OAuth credentials in Supabase
3. Add button in `activity_login.xml`
4. Add handler in `LoginActivity.kt`

---

## Security Checklist

### ✅ Safe to Include in App Code
- Supabase URL
- Supabase Anon Key (protected by RLS)
- PayPal Button ID
- OAuth redirect URIs

### ❌ NEVER Include in App Code
- Supabase Service Role Key
- Google Client Secret (store in Supabase only)
- PayPal API credentials
- Database passwords

### Best Practices
1. **Row Level Security (RLS):** Enabled on Supabase tables
2. **HTTPS Only:** All API calls use HTTPS
3. **PKCE Flow:** OAuth uses secure PKCE exchange
4. **No Secrets in Git:** Sensitive values in Supabase dashboard only

---

## Troubleshooting Quick Reference

### Login Issues
| Problem | Solution |
|---------|----------|
| Google login "site can't be reached" | Check Supabase redirect URLs config |
| "Invalid credentials" | User entered wrong password |
| OAuth callback not received | Check AndroidManifest deep link config |

### Payment Issues
| Problem | Solution |
|---------|----------|
| PayPal page won't load | Check internet, verify button ID |
| Payment completed but not activated | Set up IPN webhook (see PAYMENTS_SETUP.md) |
| Wrong price showing | Edit button at paypal.com/buttons |

### Build Issues
| Problem | Solution |
|---------|----------|
| aapt2 error on phone | Check gradle.properties has ARM override |
| Build fails on GitHub | Remove ARM override in workflow |
| Signing error | Check keystore password in local.properties |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 2.0.0-beta.34-build13 | Jan 22, 2026 | PayPal NCP URL format fix (WORKING) |
| 2.0.0-beta.34-build12 | Jan 22, 2026 | PayPal Business button ID update |
| 2.0.0-beta.34-build11 | Jan 22, 2026 | Documentation updates |
| 2.0.0-beta.34-build9 | Jan 22, 2026 | PayPal integration |
| 2.0.0-beta.34-build7 | Jan 22, 2026 | Google OAuth PKCE flow fixed |
| 2.0.0-beta.34 | Jan 22, 2026 | Auth system added |

### Current Stable Version
**Build 13** - This is the fully working version with:
- Email/Password authentication ✅
- Google OAuth login ✅
- PayPal $15/month subscription ✅
- Skip login for testing ✅

---

## Emergency Contacts / Recovery

### Lost Access to Supabase
- Login: https://supabase.com/dashboard
- Email associated with account: (your email)

### Lost Access to PayPal
- Login: https://www.paypal.com
- Recovery: https://www.paypal.com/authflow/password-recovery

### Lost Access to Google Cloud
- Login: https://console.cloud.google.com
- Recovery: https://accounts.google.com/signin/recovery

### Lost Access to GitHub
- Login: https://github.com/login
- Recovery: https://github.com/password_reset

---

## Future Improvements (TODO)

- [ ] PayPal IPN webhook for automatic license activation
- [ ] Stripe as alternative payment method
- [ ] Apple Sign-In support
- [ ] Offline license caching
- [ ] Subscription status sync on app resume
