# PayPal Integration Setup Guide

**Complete step-by-step instructions to set up PayPal subscriptions for MobileCLI from scratch.**

---

## Prerequisites

- PayPal Business Account (free upgrade from personal)
- Supabase Project
- Access to Supabase Dashboard

---

## Step 1: PayPal Business Account

### If You Have a Personal Account

1. Go to: https://www.paypal.com/bizsignup/
2. Select "Individual/Sole Proprietor"
3. Business name: `MobileCLI`
4. Complete verification (usually instant)

### Why Business Account?
- Personal accounts cannot accept recurring subscription payments
- Business accounts are free to create/upgrade
- Required for subscription buttons

---

## Step 2: Create PayPal Subscription Plan

### Option A: PayPal Subscriptions API (Recommended)

1. Go to: https://developer.paypal.com/dashboard/
2. Create an app (or use existing)
3. Get Client ID and Secret
4. Use API to create subscription plan:

```bash
# Get access token
curl -v -X POST "https://api-m.paypal.com/v1/oauth2/token" \
  -u "CLIENT_ID:SECRET" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials"

# Create plan
curl -v -X POST "https://api-m.paypal.com/v1/billing/plans" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ACCESS_TOKEN" \
  -d '{
    "product_id": "PROD-XXX",
    "name": "MobileCLI Pro",
    "billing_cycles": [{
      "frequency": {"interval_unit": "MONTH", "interval_count": 1},
      "tenure_type": "REGULAR",
      "sequence": 1,
      "total_cycles": 0,
      "pricing_scheme": {"fixed_price": {"value": "4.99", "currency_code": "USD"}}
    }],
    "payment_preferences": {
      "auto_bill_outstanding": true,
      "payment_failure_threshold": 3
    }
  }'
```

### Option B: PayPal Button (Simpler)

1. Go to: https://www.paypal.com/buttons/
2. Click "Create Button"
3. Select "Subscribe"
4. Set price: $4.99/month
5. Copy the Button ID

**Current Button ID:** `DHCKPWE3PJ684`
**Current Plan ID:** `P-3RH33892X5467024SNFZON2Y`

---

## Step 3: Set Up Supabase Database

### Run the SQL

Go to Supabase Dashboard -> SQL Editor and run:

```sql
-- Create subscriptions table
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    status TEXT NOT NULL DEFAULT 'trial',
    paypal_subscription_id TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    expires_at TIMESTAMPTZ,
    CONSTRAINT unique_user_subscription UNIQUE (user_id)
);

-- Enable RLS
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;

-- Users can view own subscription
CREATE POLICY "Users can view own subscription"
    ON public.subscriptions FOR SELECT
    USING (auth.uid() = user_id);

-- Index for performance
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id
    ON public.subscriptions(user_id);
```

**Full schema:** See `DATABASE_SCHEMA.md`

---

## Step 4: Deploy Webhook Edge Function

### Via Supabase Dashboard (Easiest)

1. Go to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions
2. Click "Create a new function"
3. Name it: `paypal-webhook`
4. Paste the code from `WEBHOOK_CODE.md`
5. Click "Deploy"

### Via Supabase CLI

```bash
# Install CLI
npm install -g supabase

# Login
supabase login

# Link project
supabase link --project-ref mwxlguqukyfberyhtkmg

# Deploy
supabase functions deploy paypal-webhook
```

**Webhook URL after deployment:**
```
https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook
```

---

## Step 5: Configure PayPal Webhooks

### REST API Webhooks (Recommended)

1. Go to: https://developer.paypal.com/dashboard/applications
2. Select your app
3. Scroll to "Webhooks"
4. Click "Add Webhook"
5. Enter URL: `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook`
6. Select events:
   - `BILLING.SUBSCRIPTION.ACTIVATED`
   - `BILLING.SUBSCRIPTION.CANCELLED`
   - `BILLING.SUBSCRIPTION.SUSPENDED`
   - `BILLING.SUBSCRIPTION.EXPIRED`
   - `PAYMENT.SALE.COMPLETED`

### Legacy IPN (Alternative)

1. Go to: https://www.paypal.com/ipn
2. Click "Choose IPN Settings"
3. Enter the webhook URL
4. Enable IPN messages

---

## Step 6: Update Android App

### PaywallActivity.kt

Add the PayPal subscription URL:

```kotlin
companion object {
    private const val PAYPAL_PLAN_ID = "P-3RH33892X5467024SNFZON2Y"
}

private fun openPayPalSubscription() {
    val userId = supabase.auth.currentUser?.id ?: return

    // Include custom_id for webhook matching
    val url = "https://www.paypal.com/webapps/billing/plans/subscribe?" +
        "plan_id=$PAYPAL_PLAN_ID" +
        "&custom_id=$userId"

    val intent = CustomTabsIntent.Builder().build()
    intent.launchUrl(this, Uri.parse(url))
}
```

---

## Step 7: Test the Integration

### Test Webhook Manually

```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "BILLING.SUBSCRIPTION.ACTIVATED",
    "resource": {
      "id": "I-TEST123456789",
      "custom_id": "YOUR-USER-UUID-HERE"
    }
  }'
```

### Test Full Flow

1. Install app
2. Sign up/login
3. Click Subscribe
4. Complete PayPal payment (use sandbox for testing)
5. Return to app
6. Click "Restore Purchase"
7. Verify Pro status

### Check Logs

- **Supabase Edge Function Logs:** Dashboard -> Functions -> paypal-webhook -> Logs
- **PayPal Webhook History:** Developer Dashboard -> Webhooks -> Event log

---

## Step 8: Go Live

### Sandbox to Production

1. In PayPal Developer Dashboard, switch from Sandbox to Live
2. Create production webhook with same URL
3. Update app to use production PayPal API
4. Test with real payment (can refund immediately)

---

## Configuration Summary

| Item | Value |
|------|-------|
| PayPal Plan ID | `P-3RH33892X5467024SNFZON2Y` |
| PayPal Button ID | `DHCKPWE3PJ684` |
| Supabase Project | `mwxlguqukyfberyhtkmg` |
| Webhook URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook` |
| Price | $4.99/month |

---

## Important URLs

| Purpose | URL |
|---------|-----|
| PayPal Developer Dashboard | https://developer.paypal.com/dashboard/ |
| PayPal Button Management | https://www.paypal.com/buttons/ |
| Supabase Dashboard | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg |
| Supabase Functions | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions |

---

## Troubleshooting

See `TROUBLESHOOTING.md` for common issues and solutions.

---

*Last Updated: January 25, 2026*
