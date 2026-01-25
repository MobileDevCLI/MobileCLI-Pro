# MobileCLI Pro Payment System

## Overview

MobileCLI Pro uses PayPal Subscriptions + Supabase for payment processing.

**Price:** $4.99/month

## Architecture

```
User clicks Subscribe → PayPal Checkout → User Pays → PayPal sends Webhook → Supabase Edge Function → Database Updated → User gets Pro access
```

## Components

### 1. PayPal
- **Subscription Plan ID:** `P-3RH33892X5467024SNFZON2Y`
- **Webhook URL:** `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook`
- **Events tracked:**
  - `BILLING.SUBSCRIPTION.ACTIVATED`
  - `BILLING.SUBSCRIPTION.CANCELLED`
  - `BILLING.SUBSCRIPTION.SUSPENDED`
  - `BILLING.SUBSCRIPTION.EXPIRED`
  - `PAYMENT.SALE.COMPLETED`

### 2. Supabase
- **Project:** `mwxlguqukyfberyhtkmg`
- **Edge Function:** `paypal-webhook`
- **Database Table:** `subscriptions`

### 3. Database Schema

```sql
CREATE TABLE subscriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id),
  status TEXT DEFAULT 'trial',  -- trial, active, cancelled, suspended, expired
  paypal_subscription_id TEXT,
  current_period_end TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

## How It Works

### User Subscribes
1. User clicks "Subscribe" in app
2. App opens PayPal checkout with `custom_id` = user's Supabase UUID
3. User completes payment
4. PayPal sends `BILLING.SUBSCRIPTION.ACTIVATED` webhook
5. Webhook updates `subscriptions.status` to `active`
6. App checks status and unlocks Pro features

### User Cancels
1. User cancels in PayPal
2. PayPal sends `BILLING.SUBSCRIPTION.CANCELLED` webhook
3. Webhook updates status to `cancelled`
4. User keeps access until `current_period_end`

### Monthly Renewal
1. PayPal charges user automatically
2. PayPal sends `PAYMENT.SALE.COMPLETED` webhook
3. Webhook extends `current_period_end` by 30 days

## Webhook Code Location

**Local:** `supabase/functions/paypal-webhook/index.ts`
**Deployed:** Supabase Dashboard → Edge Functions → paypal-webhook

## Deploying Webhook Updates

### Via Supabase Dashboard (Recommended)
1. Go to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook
2. Click "Code" tab
3. Paste new code
4. Click "Deploy updates"

### Via Supabase CLI
```bash
supabase functions deploy paypal-webhook --project-ref mwxlguqukyfberyhtkmg
```

## Testing

### Test Webhook Manually
```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{"event_type":"BILLING.SUBSCRIPTION.ACTIVATED","resource":{"id":"I-TEST123","custom_id":"USER_UUID_HERE"}}'
```

### Check Logs
Supabase Dashboard → Edge Functions → paypal-webhook → Logs

### Verify Database
```sql
SELECT status, paypal_subscription_id, updated_at FROM subscriptions;
```

## Troubleshooting

### Webhook returns 500
- Check Logs tab for error details
- Common issue: Column doesn't exist (check schema matches code)

### Status not updating
- Verify `custom_id` matches a user_id in subscriptions table
- Check that user has a row in subscriptions table

### PayPal not sending webhooks
- Verify webhook URL in PayPal Developer Dashboard
- Check webhook is for correct environment (Sandbox vs Live)

## Important URLs

- **PayPal Developer Dashboard:** https://developer.paypal.com/dashboard/applications
- **Supabase Dashboard:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg
- **Webhook Function:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook
- **SQL Editor:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql/new

## Security Notes

- Never commit PayPal Client Secret to Git
- Supabase service role key is stored as Edge Function secret
- Webhook should validate PayPal signature in production (TODO)
