# MobileCLI Pro - Authentication & Payment System Documentation

**Version:** 2.0.0
**Last Updated:** January 23, 2026
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

MobileCLI Pro uses a **Supabase + PayPal** stack for authentication and payments:

- **Supabase**: User authentication, database, license storage
- **PayPal**: Payment processing, subscription management
- **Local Storage**: Encrypted license cache for offline use

### Key Principles

1. **Login Required**: App won't work without authentication
2. **Offline Support**: Once licensed, app works offline for 30 days
3. **Subscription Model**: $9.99/month recurring via PayPal
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
│  │ - Email/Pass │    │ - profiles   │    │ - paypal-    │              │
│  │ - Google OAuth│    │ - subscript- │    │   webhook    │              │
│  │ - Sessions   │    │   ions       │    │              │              │
│  │              │    │ - payment_   │    │              │              │
│  │              │    │   history    │    │              │              │
│  │              │    │ - webhook_   │    │              │              │
│  │              │    │   logs       │    │              │              │
│  └──────────────┘    └──────────────┘    └──────┬───────┘              │
│                                                  │                       │
└──────────────────────────────────────────────────┼───────────────────────┘
                                                   │
                                                   │ HTTPS (Webhooks)
                                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            PAYPAL                                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐              │
│  │   Checkout   │    │ Subscriptions│    │   Webhooks   │              │
│  │              │    │              │    │              │              │
│  │ - Hosted page│    │ - $9.99/month│    │ - BILLING.   │              │
│  │ - PayPal     │    │ - Recurring  │    │   SUBSCRIPTION│             │
│  │   account    │    │ - Cancel     │    │   .*         │              │
│  │ - Card       │    │   anytime    │    │ - PAYMENT.   │              │
│  │              │    │              │    │   SALE.*     │              │
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
| **Edge Functions** | Supabase Deno Functions | PayPal webhook handler |
| **Payments** | PayPal Subscriptions | Secure payment processing |
| **Subscriptions** | PayPal Billing | Recurring payments |
| **Local Storage** | EncryptedSharedPreferences | Secure license cache |
| **HTTP Client** | Ktor (Android) | API calls |

### Supabase Project Details

- **Project URL**: `https://mwxlguqukyfberyhtkmg.supabase.co`
- **Anon Key**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im13eGxndXF1a3lmYmVyeWh0a21nIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njc0OTg5ODgsImV4cCI6MjA4MzA3NDk4OH0.VdpU9WzYpTyLeVX9RaXKBP3dNNNf0t9YkQfVf7x_TA8`
- **Dashboard**: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg

### PayPal Configuration

- **Plan ID**: `P-3RH33892X5467024SNFZON2Y`
- **Webhook URL**: `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook`
- **Price**: $9.99/month recurring

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
       ├─▶ TRUE: Check subscription status
       │   └─▶ Query subscriptions table
       │       ├─▶ Active subscription: PROCEED TO APP
       │       └─▶ No subscription: GO TO PAYWALL
       │
       └─▶ FALSE: GO TO LOGIN

4. LOGIN FLOW (LoginActivity)
   └─▶ User enters email + password
       └─▶ SupabaseClient.auth.signInWith(Email)
           ├─▶ SUCCESS: Check subscription, route accordingly
           └─▶ FAILURE: Show error message

5. SIGNUP FLOW (LoginActivity)
   └─▶ User enters email + password
       └─▶ SupabaseClient.auth.signUpWith(Email)
           └─▶ SUCCESS: Email confirmation sent
               └─▶ User confirms email, then logs in

6. PAYWALL FLOW (PaywallActivity)
   ├─▶ START TRIAL: Create trial subscription, PROCEED TO APP
   └─▶ SUBSCRIBE: Open PayPal subscription page
       └─▶ User pays → PayPal webhook → Supabase updates subscription
           └─▶ User returns to app → verifyLicense() → PROCEED TO APP
```

### Code Entry Points

| Action | File | Method |
|--------|------|--------|
| App starts | `SplashActivity.kt` | `checkAuthStatus()` |
| User logs in | `LoginActivity.kt` | `login()` |
| User signs up | `LoginActivity.kt` | `signup()` |
| License verified | `LicenseManager.kt` | `verifyLicense()` |
| Checkout opened | `PaywallActivity.kt` | `openPayPalSubscription()` |

---

## License Management

### License Storage

Licenses are stored in **EncryptedSharedPreferences** (AES-256-GCM encryption):

```
Location: /data/data/com.termux/shared_prefs/mobilecli_license.xml (encrypted)

Keys stored:
- user_id: UUID from Supabase
- user_email: User's email
- status: "trial" | "active" | "expired" | "cancelled"
- expires_at: Unix timestamp (milliseconds)
- last_verified: Unix timestamp (milliseconds)
- paypal_subscription_id: PayPal subscription ID (if subscribed)
```

### Verification Schedule

| Scenario | Action |
|----------|--------|
| First login | Query `subscriptions` table |
| App launch (subscription valid, <30 days) | Use local cache, no network |
| App launch (subscription valid, >30 days) | Re-verify with Supabase |
| App launch (subscription expired) | Show paywall |
| Network unavailable | Use local cache if not expired |

---

## Payment Flow

### Subscription Flow

```
1. User taps "Subscribe - $9.99/month" in PaywallActivity

2. App opens PayPal subscription URL in browser:
   https://www.paypal.com/webapps/billing/plans/subscribe?plan_id=P-3RH33892X5467024SNFZON2Y&custom_id=USER_UUID

3. PayPal page loads
   - User logs into PayPal (or uses card)
   - User approves subscription
   - PayPal redirects to success URL

4. PayPal sends webhook to:
   https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook

5. Edge Function receives webhook:
   - Event: BILLING.SUBSCRIPTION.ACTIVATED
   - Finds user by custom_id (Supabase user_id)
   - Updates `subscriptions` table
   - Sets status = "active", expires_at = +30 days

6. User returns to app

7. App queries subscription status
   - Supabase returns status = "active"
   - App stores locally and proceeds
```

### PayPal Webhook Events Handled

| Event | Action |
|-------|--------|
| `BILLING.SUBSCRIPTION.ACTIVATED` | Set status = active, record payment |
| `BILLING.SUBSCRIPTION.CANCELLED` | Set status = cancelled |
| `BILLING.SUBSCRIPTION.SUSPENDED` | Set status = suspended |
| `BILLING.SUBSCRIPTION.EXPIRED` | Set status = expired |
| `BILLING.SUBSCRIPTION.RE-ACTIVATED` | Set status = active |
| `BILLING.SUBSCRIPTION.PAYMENT.FAILED` | Record failed payment |
| `PAYMENT.SALE.COMPLETED` | Record payment, extend subscription |

### User Matching Logic

The webhook matches users in this order:

1. **custom_id** - The Supabase user_id passed from the app (most reliable)
2. **subscriber.email_address** - PayPal email (fallback if custom_id missing)

If matching fails (e.g., PayPal email differs from app email), the webhook logs `user_not_found` and admin must manually activate.

---

## Database Schema

### Tables

#### `subscriptions`
```sql
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE REFERENCES auth.users(id),
    status TEXT DEFAULT 'trial' CHECK (status IN ('trial', 'active', 'cancelled', 'suspended', 'expired')),
    paypal_subscription_id TEXT,
    paypal_payer_id TEXT,
    expires_at TIMESTAMPTZ,
    trial_started_at TIMESTAMPTZ,
    last_payment_at TIMESTAMPTZ,
    payment_failed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancel_reason TEXT,
    admin_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### `payment_history`
```sql
CREATE TABLE payment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id),
    amount DECIMAL(10,2),
    currency TEXT DEFAULT 'USD',
    payment_type TEXT,  -- 'subscription_initial', 'subscription_renewal', 'refund'
    provider TEXT DEFAULT 'paypal',
    paypal_transaction_id TEXT,
    paypal_subscription_id TEXT,
    status TEXT,  -- 'completed', 'pending', 'failed', 'refunded'
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### `webhook_logs`
```sql
CREATE TABLE webhook_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type TEXT,
    event_id TEXT,
    provider TEXT DEFAULT 'paypal',
    payload JSONB,
    user_id UUID,
    user_email TEXT,
    processed BOOLEAN DEFAULT false,
    processing_result TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);
```

---

## API Endpoints

### Supabase Edge Functions

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/functions/v1/paypal-webhook` | POST | Handle PayPal webhooks |

### Supabase Direct Queries (via SDK)

| Query | Purpose |
|-------|---------|
| `SELECT * FROM subscriptions WHERE user_id = ?` | Check subscription status |
| `INSERT INTO subscriptions ...` | Create trial subscription |

---

## File Reference

### Android App Files

| File | Purpose |
|------|---------|
| `auth/SplashActivity.kt` | Entry point, checks auth status on launch |
| `auth/LoginActivity.kt` | Email/password login |
| `auth/PaywallActivity.kt` | Subscription options and trial |
| `auth/LicenseManager.kt` | Local license storage and verification |
| `auth/SupabaseClient.kt` | Supabase SDK configuration and helpers |
| `res/layout/activity_splash.xml` | Splash screen UI |
| `res/layout/activity_login.xml` | Login form UI |
| `res/layout/activity_paywall.xml` | Subscription UI |

### Supabase Files

| File | Purpose |
|------|---------|
| `supabase/functions/paypal-webhook/index.ts` | PayPal webhook handler |
| `supabase/migrations/002_professional_subscription.sql` | Database schema |

---

## Troubleshooting

### Common Issues

#### User can't log in
1. Check if email is confirmed (Supabase sends confirmation email)
2. Check Supabase Dashboard → Authentication → Users
3. Try resetting password

#### Subscription not found after payment
1. Check `webhook_logs` table for the event
2. Check `processing_result` column
3. If `user_not_found`: PayPal email differs from app email - manually activate
4. If no webhook: Check PayPal dashboard for delivery status

#### App stuck on "Verifying..."
1. Check internet connection
2. Check Supabase status page
3. Clear app data and re-login
4. Check if Supabase anon key is correct

#### Trial expired but user wants to subscribe
1. User opens paywall
2. User taps Subscribe
3. After payment, webhook updates status to "active"

### Debug Queries

```sql
-- Find user by email
SELECT * FROM auth.users WHERE email = 'user@example.com';

-- Check user's subscription
SELECT * FROM subscriptions WHERE user_id = 'uuid-here';

-- Check webhook events for user
SELECT * FROM webhook_logs
WHERE user_email ILIKE '%user@example.com%'
ORDER BY created_at DESC;

-- Check recent payments
SELECT * FROM payment_history
ORDER BY created_at DESC LIMIT 20;
```

---

## Security Considerations

### What's Safe to Expose

- Supabase URL (public)
- Supabase Anon Key (public, protected by RLS)
- PayPal Plan ID (public)

### What Must Stay Secret

- Supabase Service Role Key (server only, in Edge Functions)
- PayPal Client Secret (if using API directly)

### Row Level Security (RLS)

All tables have RLS enabled:
- Users can only read their own subscription
- Users can create their own trial subscription
- Only service role can update subscriptions (via webhooks)
- Webhook logs are service-role only

### Local Security

- License cached in Android EncryptedSharedPreferences (AES-256-GCM)
- Expires and requires re-verification every 30 days
- No sensitive keys stored on device

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2026-01-22 | 1.0.0 | Initial documentation (Stripe) |
| 2026-01-23 | 2.0.0 | Updated for PayPal integration |

---

*This documentation should be kept up-to-date with any changes to the authentication or payment system.*
