# MobileCLI Pro - Subscription System Setup

Last Updated: January 22, 2026

---

## Overview

The subscription system verifies if users have paid for MobileCLI Pro. It's designed to be:
- **Privacy-focused**: Only stores user ID, status, and dates
- **Secure**: Uses Row Level Security (RLS)
- **Offline-capable**: Caches status locally for offline use

---

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   User Login    │────▶│    Supabase     │────▶│  subscriptions  │
│   (App)         │     │    Auth         │     │    table        │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                                                        ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   PayPal        │────▶│  PayPal IPN     │────▶│  Update status  │
│   Payment       │     │  Webhook        │     │  to 'active'    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## Step 1: Create Subscriptions Table in Supabase

### Open Supabase SQL Editor
https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql

### Run the SQL Script
Copy and paste the contents of `supabase/setup_subscriptions.sql` into the SQL editor and run it.

This creates:
- `subscriptions` table with RLS
- Auto-create trial on user signup (trigger)
- `is_subscription_valid()` function

### Verify Setup
Run this query to check:
```sql
SELECT tablename, rowsecurity FROM pg_tables WHERE tablename = 'subscriptions';
```

Should show `rowsecurity = true`.

---

## Step 2: How It Works

### New User Signs Up
1. User creates account (email or Google)
2. Trigger automatically creates subscription with `status = 'trial'`
3. Trial expires in 7 days

### User Logs In (App)
1. App calls `LicenseManager.checkSubscription()`
2. Queries `subscriptions` table for user's status
3. Caches status locally (encrypted)
4. Returns `active`, `trial`, `cancelled`, or `expired`

### User Pays via PayPal
1. User completes PayPal subscription
2. PayPal sends IPN notification to webhook
3. Webhook updates `subscriptions.status = 'active'`
4. Next time app checks, user has full access

---

## Step 3: Set Up PayPal IPN Webhook

### Create Supabase Edge Function

1. Go to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions
2. Create new function: `paypal-webhook`
3. Use this code:

```typescript
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

serve(async (req) => {
  // Get request body
  const body = await req.text()
  const params = new URLSearchParams(body)

  // PayPal IPN fields
  const txnType = params.get('txn_type')
  const payerEmail = params.get('payer_email')
  const subscriptionId = params.get('subscr_id')
  const paymentStatus = params.get('payment_status')

  console.log('PayPal IPN received:', { txnType, payerEmail, subscriptionId })

  // Only process subscription payments
  if (txnType !== 'subscr_payment' && txnType !== 'subscr_signup') {
    return new Response('OK', { status: 200 })
  }

  // Create Supabase client with service role
  const supabase = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
  )

  // Find user by email
  const { data: users } = await supabase.auth.admin.listUsers()
  const user = users.users.find(u => u.email === payerEmail)

  if (!user) {
    console.error('User not found:', payerEmail)
    return new Response('User not found', { status: 404 })
  }

  // Update subscription to active
  const { error } = await supabase
    .from('subscriptions')
    .upsert({
      user_id: user.id,
      status: 'active',
      paypal_subscription_id: subscriptionId,
      expires_at: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString()
    }, {
      onConflict: 'user_id'
    })

  if (error) {
    console.error('Failed to update subscription:', error)
    return new Response('Database error', { status: 500 })
  }

  console.log('Subscription activated for:', payerEmail)
  return new Response('OK', { status: 200 })
})
```

### Configure PayPal IPN

1. Go to: https://www.paypal.com/ipn
2. Click "Choose IPN Settings"
3. Set IPN URL to: `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook`
4. Enable IPN

---

## Data Stored

### In Supabase (subscriptions table)
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Auto-generated |
| user_id | UUID | Links to auth.users |
| status | TEXT | 'active', 'trial', 'cancelled', 'expired' |
| paypal_subscription_id | TEXT | For reference only |
| created_at | TIMESTAMP | When subscription created |
| updated_at | TIMESTAMP | Last update |
| expires_at | TIMESTAMP | When subscription/trial expires |

### On User's Device (encrypted)
| Key | Description |
|-----|-------------|
| user_id | UUID only |
| status | Subscription status |
| expires_at | Expiry timestamp |
| last_verified | Last online verification |

**NO personal data, NO payment details, NO email stored locally.**

---

## Privacy & Security

### Row Level Security (RLS)
- Users can only read their OWN subscription
- Users CANNOT modify their subscription status
- Only service role (webhook) can update subscriptions

### Data Minimization
- Only essential data stored
- No payment card info
- No transaction amounts
- PayPal handles all payment data

### Offline Support
- Status cached locally (encrypted)
- Works offline for 30 days
- Re-verifies when online

---

## Manual Operations

### Manually Activate a User
```sql
UPDATE subscriptions
SET status = 'active',
    expires_at = NOW() + INTERVAL '30 days'
WHERE user_id = 'user-uuid-here';
```

### Check User's Subscription
```sql
SELECT * FROM subscriptions
WHERE user_id = 'user-uuid-here';
```

### List All Active Subscribers
```sql
SELECT u.email, s.status, s.created_at
FROM subscriptions s
JOIN auth.users u ON s.user_id = u.id
WHERE s.status = 'active'
ORDER BY s.created_at DESC;
```

### Cancel a Subscription
```sql
UPDATE subscriptions
SET status = 'cancelled'
WHERE user_id = 'user-uuid-here';
```

---

## Troubleshooting

### User paid but shows trial
1. Check PayPal IPN history
2. Verify webhook is receiving notifications
3. Manually update subscription if needed

### Subscription check fails
1. Check internet connection
2. App will use cached status if offline
3. Verify Supabase is accessible

### Webhook not receiving IPN
1. Check PayPal IPN settings
2. Verify Edge Function is deployed
3. Check Edge Function logs in Supabase

---

## Related Files

| File | Purpose |
|------|---------|
| `supabase/setup_subscriptions.sql` | Database setup script |
| `app/.../LicenseManager.kt` | App-side subscription checking |
| `docs/PAYMENTS_SETUP.md` | PayPal configuration |
