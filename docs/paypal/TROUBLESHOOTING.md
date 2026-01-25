# PayPal Integration Troubleshooting

**Common problems and their solutions.**

---

## Problem: Webhook Returns 200 But Status Not Updated

### Symptoms
- PayPal webhook logs show successful delivery (200 OK)
- Supabase Edge Function logs show event received
- Database `subscriptions` table still shows `status = 'trial'`

### Root Cause
The webhook was using `.update()` instead of `.upsert()`. Supabase's `.update()` returns an empty array (not an error) when no row matches the WHERE clause.

### Solution
Change from:
```javascript
.update({ status: "active" }).eq("user_id", customId)
```

To:
```javascript
.upsert({ user_id: customId, status: "active" }, { onConflict: "user_id" })
```

See `WEBHOOK_CODE.md` for the full working code.

---

## Problem: Webhook Returns 500 Error

### Check 1: Supabase Credentials

```javascript
const supabaseUrl = Deno.env.get("SUPABASE_URL")
const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")
```

These should be auto-provided by Supabase. If missing, redeploy the function.

### Check 2: JSON Parse Error

Ensure PayPal is sending JSON, not form-encoded data:
```javascript
const body = await req.json()  // Expects JSON
```

PayPal REST webhooks send JSON. Legacy IPN sends form data.

### Check 3: Database Schema

Ensure `subscriptions` table exists with required columns:
```sql
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'subscriptions';
```

---

## Problem: custom_id is Null/Undefined

### Symptoms
- Webhook receives event
- `custom_id` is null in logs
- Can't identify which user to update

### Cause 1: App Not Passing custom_id

The app must include user_id when opening PayPal:
```kotlin
val url = "https://www.paypal.com/webapps/billing/plans/subscribe?" +
    "plan_id=$PAYPAL_PLAN_ID" +
    "&custom_id=$userId"  // THIS IS IMPORTANT
```

### Cause 2: PayPal Subscription Button vs API

Simple PayPal buttons (`/ncp/payment/BUTTON_ID`) don't support custom_id via URL.

**Solution:** Use PayPal Subscriptions API to create subscription with custom_id:
```javascript
// PayPal JS SDK
paypal.Buttons({
    createSubscription: (data, actions) => {
        return actions.subscription.create({
            plan_id: 'P-XXXXX',
            custom_id: 'USER-UUID-HERE'  // This works
        })
    }
}).render('#paypal-button')
```

### Workaround: Match by Email

If custom_id is unavailable, match by PayPal email:
```javascript
const email = resource.subscriber?.email_address

// Find user by email
const { data: user } = await supabase
    .from('auth.users')
    .select('id')
    .eq('email', email)
    .single()

if (user) {
    // Update using user.id
}
```

**Limitation:** User's PayPal email must match their app login email.

---

## Problem: User Paid But Shows as Trial

### Step 1: Check PayPal Webhook History

Go to: https://developer.paypal.com/dashboard/webhooksIntegration

Look for:
- Webhook delivery status
- Event type sent
- Response code from your server

### Step 2: Check Supabase Logs

Go to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs

Look for:
- Event received
- User ID logged
- Database operation result

### Step 3: Check Database Directly

```sql
SELECT *
FROM subscriptions
WHERE user_id = 'USER-UUID-HERE';
```

### Step 4: Manual Fix

If webhook failed but user paid:
```sql
UPDATE subscriptions
SET status = 'active',
    updated_at = NOW()
WHERE user_id = 'USER-UUID-HERE';
```

Or with upsert if row doesn't exist:
```sql
INSERT INTO subscriptions (user_id, status, updated_at)
VALUES ('USER-UUID-HERE', 'active', NOW())
ON CONFLICT (user_id)
DO UPDATE SET status = 'active', updated_at = NOW();
```

---

## Problem: UNIQUE Constraint Violation

### Error Message
```
duplicate key value violates unique constraint "unique_user_subscription"
```

### Cause
Trying to INSERT when row already exists.

### Solution
This is why we use UPSERT. The error means you're using INSERT instead of UPSERT.

```javascript
// Wrong - fails if row exists
.insert({ user_id, status: "active" })

// Right - handles existing rows
.upsert({ user_id, status: "active" }, { onConflict: "user_id" })
```

---

## Problem: PayPal Not Sending Webhooks

### Check 1: Webhook is Configured

1. Go to: https://developer.paypal.com/dashboard/applications
2. Select your app
3. Check "Webhooks" section
4. Verify URL is correct

### Check 2: Events are Selected

Make sure these events are enabled:
- `BILLING.SUBSCRIPTION.ACTIVATED`
- `BILLING.SUBSCRIPTION.CANCELLED`
- `BILLING.SUBSCRIPTION.SUSPENDED`
- `BILLING.SUBSCRIPTION.EXPIRED`
- `PAYMENT.SALE.COMPLETED`

### Check 3: Webhook URL is Accessible

Test manually:
```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{"event_type":"test"}'
```

Should return JSON response, not error.

### Check 4: Sandbox vs Live

- Sandbox webhooks only fire for sandbox transactions
- Live webhooks only fire for live transactions
- Make sure you're testing with the right environment

---

## Problem: Edge Function Not Deployed

### Symptoms
- Webhook URL returns 404
- "Function not found" error

### Solution

Redeploy via Dashboard:
1. Go to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions
2. Click on `paypal-webhook`
3. Go to "Code" tab
4. Paste working code from `WEBHOOK_CODE.md`
5. Click "Deploy updates"

Or via CLI:
```bash
supabase functions deploy paypal-webhook
```

---

## Problem: CORS Errors

### Symptoms
- Browser shows CORS error
- Webhook works from curl but not browser

### Note
PayPal webhooks are server-to-server, not browser-to-server. You shouldn't see CORS errors for webhooks.

If you're testing from browser, that's not how webhooks work.

### If App is Making Direct Calls

The webhook has CORS headers:
```javascript
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type"
}
```

---

## Debugging Commands

### Test Webhook Locally

```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "BILLING.SUBSCRIPTION.ACTIVATED",
    "resource": {
      "id": "I-TEST123",
      "custom_id": "YOUR-USER-UUID"
    }
  }'
```

### Check User Subscription

```sql
SELECT * FROM subscriptions
WHERE user_id = 'USER-UUID';
```

### View Recent Webhook Logs

```sql
SELECT event_type, processed, processing_result, created_at
FROM webhook_logs
ORDER BY created_at DESC
LIMIT 10;
```

### Manual Status Update

```sql
UPDATE subscriptions
SET status = 'active', updated_at = NOW()
WHERE user_id = 'USER-UUID';
```

---

## Support Resources

| Resource | URL |
|----------|-----|
| Supabase Edge Function Logs | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs |
| PayPal Webhook History | https://developer.paypal.com/dashboard/webhooksIntegration |
| PayPal IPN History | https://www.paypal.com/ipn/history |

---

*Last Updated: January 25, 2026*
