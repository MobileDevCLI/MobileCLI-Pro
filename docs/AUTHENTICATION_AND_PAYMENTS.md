# MobileCLI Pro - Authentication & Payment System Documentation

**Version:** 1.0.0
**Last Updated:** January 22, 2026
**Author:** Claude Opus 4.5 + Human Developer

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Technology Stack](#technology-stack)
4. [Authentication Flow](#authentication-flow)
5. [License Management](#license-management)
6. [Payment Flow](#payment-flow)
7. [Database Schema](#database-schema)
8. [API Endpoints](#api-endpoints)
9. [File Reference](#file-reference)
10. [Troubleshooting](#troubleshooting)
11. [Security Considerations](#security-considerations)

---

## System Overview

MobileCLI Pro uses a **Supabase + Stripe** stack for authentication and payments:

- **Supabase**: User authentication, database, license storage
- **Stripe**: Payment processing, subscription management
- **Local Storage**: Encrypted license cache for offline use

### Key Principles

1. **Login Required**: App won't work without authentication
2. **Offline Support**: Once licensed, app works offline for 30 days
3. **Subscription Model**: $15/month recurring via Stripe
4. **7-Day Trial**: Free users get 7 days before paywall

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           USER'S PHONE                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
│  │ SplashActivity│───▶│LoginActivity │───▶│PaywallActivity│             │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘              │
│         │                   │                    │                       │
│         │                   │                    │                       │
│         ▼                   ▼                    ▼                       │
│  ┌──────────────────────────────────────────────────────────┐           │
│  │                    LicenseManager                         │           │
│  │  - Stores license in EncryptedSharedPreferences          │           │
│  │  - Checks expiration locally                              │           │
│  │  - Verifies with server every 30 days                    │           │
│  └──────────────────────────┬───────────────────────────────┘           │
│                             │                                            │
│                             │ HTTPS                                      │
└─────────────────────────────┼────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         SUPABASE CLOUD                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
│  │  Auth (GoTrue)│    │  PostgreSQL  │    │Edge Functions│              │
│  │              │    │              │    │              │              │
│  │ - Email/Pass │    │ - profiles   │    │ - create-    │              │
│  │ - Google OAuth│    │ - subscript- │    │   checkout   │              │
│  │ - Sessions   │    │   ions       │    │ - stripe-    │              │
│  │              │    │ - app_       │    │   webhook    │              │
│  │              │    │   licenses   │    │ - customer-  │              │
│  │              │    │              │    │   portal     │              │
│  └──────────────┘    └──────────────┘    └──────┬───────┘              │
│                                                  │                       │
└──────────────────────────────────────────────────┼───────────────────────┘
                                                   │
                                                   │ HTTPS (Webhooks)
                                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            STRIPE                                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
│  │   Checkout   │    │ Subscriptions│    │   Webhooks   │              │
│  │              │    │              │    │              │              │
│  │ - Hosted page│    │ - $15/month  │    │ - payment    │              │
│  │ - Card input │    │ - Recurring  │    │   succeeded  │              │
│  │ - PayPal     │    │ - Cancel     │    │ - subscription│             │
│  │              │    │              │    │   updated    │              │
│  └──────────────┘    └──────────────┘    └──────────────┘              │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Auth** | Supabase GoTrue | Email/password + OAuth |
| **Database** | Supabase PostgreSQL | User data, licenses, subscriptions |
| **Edge Functions** | Supabase Deno Functions | Stripe integration |
| **Payments** | Stripe Checkout | Secure payment processing |
| **Subscriptions** | Stripe Billing | Recurring payments |
| **Local Storage** | EncryptedSharedPreferences | Secure license cache |
| **HTTP Client** | Ktor (Android) | API calls |

### Supabase Project Details

- **Project URL**: `https://mwxlguqukyfberyhtkmg.supabase.co`
- **Anon Key**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im13eGxndXF1a3lmYmVyeWh0a21nIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njc0OTg5ODgsImV4cCI6MjA4MzA3NDk4OH0.VdpU9WzYpTyLeVX9RaXKBP3dNNNf0t9YkQfVf7x_TA8`
- **Dashboard**: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg

---

## Authentication Flow

### Detailed Step-by-Step

```
1. APP LAUNCH
   └─▶ SplashActivity.onCreate()
       └─▶ checkAuthStatus()

2. CHECK LOCAL LICENSE
   └─▶ LicenseManager.hasValidLocalLicense()
       ├─▶ TRUE + not expired + not needs verification
       │   └─▶ PROCEED TO APP (SetupWizard or MainActivity)
       │
       ├─▶ TRUE + needs verification (>30 days since last check)
       │   └─▶ LicenseManager.verifyLicense()
       │       ├─▶ SUCCESS: Update local license, PROCEED TO APP
       │       └─▶ FAILURE (network): Allow offline if not expired
       │
       └─▶ FALSE (no license or expired)
           └─▶ Check if logged in to Supabase

3. CHECK SUPABASE AUTH
   └─▶ SupabaseClient.isLoggedIn()
       ├─▶ TRUE: Try to register device
       │   └─▶ LicenseManager.registerDevice()
       │       ├─▶ Pro tier: PROCEED TO APP
       │       └─▶ Free tier: GO TO PAYWALL
       │
       └─▶ FALSE: GO TO LOGIN

4. LOGIN FLOW (LoginActivity)
   └─▶ User enters email + password
       └─▶ SupabaseClient.auth.signInWith(Email)
           ├─▶ SUCCESS: Register device, check tier
           │   ├─▶ Pro: PROCEED TO APP
           │   └─▶ Free: GO TO PAYWALL
           └─▶ FAILURE: Show error message

5. SIGNUP FLOW (LoginActivity)
   └─▶ User enters email + password
       └─▶ SupabaseClient.auth.signUpWith(Email)
           └─▶ SUCCESS: Email confirmation sent
               └─▶ User confirms email, then logs in

6. PAYWALL FLOW (PaywallActivity)
   ├─▶ START TRIAL: Use 7-day free tier, PROCEED TO APP
   └─▶ SUBSCRIBE: Open Stripe Checkout in browser
       └─▶ User pays → Stripe webhook → Supabase updates subscription
           └─▶ User returns to app → verifyLicense() → PROCEED TO APP
```

### Code Entry Points

| Action | File | Method |
|--------|------|--------|
| App starts | `SplashActivity.kt` | `checkAuthStatus()` |
| User logs in | `LoginActivity.kt` | `login()` |
| User signs up | `LoginActivity.kt` | `signup()` |
| Device registered | `LicenseManager.kt` | `registerDevice()` |
| License verified | `LicenseManager.kt` | `verifyLicense()` |
| Checkout opened | `PaywallActivity.kt` | `openCheckout()` |

---

## License Management

### License Storage

Licenses are stored in **EncryptedSharedPreferences** (AES-256-GCM encryption):

```
Location: /data/data/com.termux/shared_prefs/mobilecli_license.xml (encrypted)

Keys stored:
- license_key: "MCLI-XXXX-XXXX-XXXX-XXXX"
- user_id: UUID from Supabase
- user_email: User's email
- tier: "free" | "pro" | "team"
- expires_at: Unix timestamp (milliseconds)
- last_verified: Unix timestamp (milliseconds)
- device_id: Android device ID
```

### License Key Format

```
MCLI-XXXX-XXXX-XXXX-XXXX

Example: MCLI-A3F2-9B4C-E7D1-8F6A
```

Generated by Supabase function `generate_license_key()`.

### Verification Schedule

| Scenario | Action |
|----------|--------|
| First login | Call `register_device()` → Get license |
| App launch (license valid, <30 days) | Use local license, no network |
| App launch (license valid, >30 days) | Call `verify_license()` to refresh |
| App launch (license expired) | Require re-login or show paywall |
| Network unavailable | Use local license if not expired |

### Device Registration

When a user logs in on a new device:

1. App calls Supabase RPC `register_device(user_id, device_id, device_name)`
2. Supabase checks subscription status
3. If subscribed: Creates license tied to subscription end date
4. If free: Creates license with 7-day expiry (trial)
5. License key returned to app and stored locally

---

## Payment Flow

### Subscription Flow

```
1. User taps "Subscribe - $15/month" in PaywallActivity

2. App opens browser to:
   https://mobilecli.com/pricing.html?email=user@example.com&user_id=xxx

3. Website creates Stripe Checkout session via Edge Function:
   POST /functions/v1/create-checkout
   Body: { priceId: "price_xxx", successUrl: "...", cancelUrl: "..." }

4. Stripe Checkout page loads (hosted by Stripe)
   - User enters card details
   - User clicks "Subscribe"

5. Stripe processes payment

6. Stripe sends webhook to:
   https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/stripe-webhook

7. Edge Function receives webhook:
   - Event: checkout.session.completed
   - Updates `subscriptions` table in Supabase
   - Sets tier = "pro", status = "active"
   - Sets period_end to subscription end date

8. User returns to app

9. App calls verifyLicense()
   - Supabase returns updated tier = "pro"
   - App stores new license locally
   - App proceeds to SetupWizard/MainActivity
```

### Stripe Webhook Events Handled

| Event | Action |
|-------|--------|
| `checkout.session.completed` | Create/update subscription, set tier |
| `customer.subscription.updated` | Update tier, status, period dates |
| `customer.subscription.deleted` | Set status = cancelled, expire licenses |
| `invoice.payment_failed` | Set status = past_due |
| `invoice.payment_succeeded` | Set status = active |

### Stripe Configuration

| Setting | Value |
|---------|-------|
| Product | MobileCLI Pro |
| Price | $15/month recurring |
| Price ID | `price_xxx` (get from Stripe Dashboard) |
| Webhook URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/stripe-webhook` |
| Webhook Events | checkout.session.completed, customer.subscription.*, invoice.* |

---

## Database Schema

### Tables

#### `profiles`
```sql
CREATE TABLE profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id),
    email TEXT,
    display_name TEXT,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

#### `subscriptions`
```sql
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id),
    stripe_customer_id TEXT,
    stripe_subscription_id TEXT UNIQUE,
    tier TEXT DEFAULT 'free' CHECK (tier IN ('free', 'pro', 'team')),
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'cancelled', 'past_due', 'trialing', 'expired')),
    current_period_start TIMESTAMP WITH TIME ZONE,
    current_period_end TIMESTAMP WITH TIME ZONE,
    cancel_at_period_end BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

#### `app_licenses`
```sql
CREATE TABLE app_licenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id),
    device_id TEXT NOT NULL,
    device_name TEXT,
    license_key TEXT UNIQUE NOT NULL,
    is_active BOOLEAN DEFAULT true,
    activated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_verified_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### Database Functions (RPCs)

#### `register_device(p_user_id, p_device_id, p_device_name)`
- Creates or returns existing license for device
- Returns: `{ success, license_key, tier, message }`

#### `verify_license(p_license_key, p_device_id)`
- Validates license and returns current status
- Updates `last_verified_at`
- Returns: `{ valid, user_id, tier, expires_at, subscription_status }`

#### `generate_license_key()`
- Generates unique `MCLI-XXXX-XXXX-XXXX-XXXX` format key
- Returns: TEXT

---

## API Endpoints

### Supabase Edge Functions

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/functions/v1/create-checkout` | POST | Create Stripe Checkout session |
| `/functions/v1/stripe-webhook` | POST | Handle Stripe webhooks |
| `/functions/v1/customer-portal` | POST | Get Stripe Customer Portal URL |

### Request/Response Examples

#### Create Checkout
```
POST /functions/v1/create-checkout
Authorization: Bearer <supabase_jwt>
Content-Type: application/json

{
  "priceId": "price_xxxxx",
  "successUrl": "https://mobilecli.com/dashboard.html?payment=success",
  "cancelUrl": "https://mobilecli.com/pricing.html?payment=cancelled"
}

Response:
{
  "sessionId": "cs_xxxxx",
  "url": "https://checkout.stripe.com/pay/cs_xxxxx"
}
```

#### Supabase RPC - Register Device
```
POST /rest/v1/rpc/register_device
Authorization: Bearer <supabase_jwt>
Content-Type: application/json

{
  "p_user_id": "uuid-xxxx",
  "p_device_id": "android_device_id",
  "p_device_name": "Samsung Galaxy S24"
}

Response:
{
  "success": true,
  "license_key": "MCLI-A3F2-9B4C-E7D1-8F6A",
  "tier": "pro",
  "message": "Device registered successfully"
}
```

---

## File Reference

### Android App Files

| File | Purpose |
|------|---------|
| `auth/SplashActivity.kt` | Entry point, checks auth status on launch |
| `auth/LoginActivity.kt` | Email/password and Google OAuth login |
| `auth/PaywallActivity.kt` | Subscription options and trial |
| `auth/LicenseManager.kt` | Local license storage and verification |
| `auth/SupabaseClient.kt` | Supabase SDK configuration and helpers |
| `res/layout/activity_splash.xml` | Splash screen UI |
| `res/layout/activity_login.xml` | Login form UI |
| `res/layout/activity_paywall.xml` | Subscription UI |

### Website Files (mobilecli.com)

| File | Purpose |
|------|---------|
| `js/supabase-config.js` | Supabase client initialization |
| `js/stripe-config.js` | Stripe client + payment helpers |
| `login.html` | Web login page |
| `dashboard.html` | User dashboard after login |
| `pricing.html` | Subscription pricing page |
| `supabase/functions/create-checkout/index.ts` | Stripe Checkout Edge Function |
| `supabase/functions/stripe-webhook/index.ts` | Stripe Webhook handler |
| `supabase/functions/customer-portal/index.ts` | Stripe Portal Edge Function |
| `supabase-setup.sql` | Database schema + functions |

---

## Troubleshooting

### Common Issues

#### User can't log in
1. Check if email is confirmed (Supabase sends confirmation email)
2. Check Supabase Dashboard → Authentication → Users
3. Try resetting password

#### License not found after payment
1. Check Stripe Dashboard → Events for webhook delivery
2. Check Supabase Edge Function logs
3. Verify `stripe_webhook` function is deployed
4. Check `subscriptions` table for user's record

#### App stuck on "Verifying..."
1. Check internet connection
2. Check Supabase status page
3. Clear app data and re-login
4. Check if Supabase anon key is correct

#### Subscription not recognized
1. Check `subscriptions` table: `SELECT * FROM subscriptions WHERE user_id = 'xxx'`
2. Check `tier` column is "pro" not "free"
3. Check `status` column is "active"
4. Verify Stripe webhook is hitting Supabase

#### License expired but user is subscribed
1. The `expires_at` in `app_licenses` should be updated by webhook
2. Check if webhook updated the license: `SELECT * FROM app_licenses WHERE user_id = 'xxx'`
3. User can "Restore Purchase" in paywall to re-verify

### Debug Queries

```sql
-- Find user by email
SELECT * FROM auth.users WHERE email = 'user@example.com';

-- Check user's subscription
SELECT * FROM subscriptions WHERE user_id = 'uuid-here';

-- Check user's licenses
SELECT * FROM app_licenses WHERE user_id = 'uuid-here';

-- Check recent webhook events (if logging enabled)
SELECT * FROM usage_events
WHERE event_type LIKE 'stripe%'
ORDER BY created_at DESC LIMIT 10;
```

### Stripe Webhook Testing

1. Install Stripe CLI: `brew install stripe/stripe-cli/stripe`
2. Login: `stripe login`
3. Forward webhooks locally:
   ```
   stripe listen --forward-to https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/stripe-webhook
   ```
4. Trigger test events:
   ```
   stripe trigger checkout.session.completed
   ```

---

## Security Considerations

### What's Safe to Expose

- Supabase URL (public)
- Supabase Anon Key (public, protected by RLS)
- Stripe Publishable Key (public)

### What Must Stay Secret

- Supabase Service Role Key (server only)
- Stripe Secret Key (server only)
- Stripe Webhook Secret (server only)

### Row Level Security (RLS)

All tables have RLS enabled:
- Users can only read their own data
- Only service role can update subscriptions (via webhooks)
- Licenses can only be created for authenticated users

### License Security

- Stored in Android EncryptedSharedPreferences (AES-256-GCM)
- Tied to device ID (can't copy license to another device)
- Verified server-side on registration
- Expires and requires re-verification

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2026-01-22 | 1.0.0 | Initial documentation |

---

## Contact

For issues with:
- **App/Android**: Check this documentation first, then debug with logs
- **Supabase**: Dashboard → Logs, or check Edge Function logs
- **Stripe**: Dashboard → Developers → Events → Webhooks

---

*This documentation should be kept up-to-date with any changes to the authentication or payment system.*
