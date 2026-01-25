# PayPal Webhook Test Payloads

**Test commands and payloads for verifying webhook functionality.**

---

## Quick Test

```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{"event_type":"BILLING.SUBSCRIPTION.ACTIVATED","resource":{"id":"I-TEST123","custom_id":"YOUR-USER-UUID"}}'
```

Replace `YOUR-USER-UUID` with an actual user ID from your database.

---

## Test Payloads by Event Type

### BILLING.SUBSCRIPTION.ACTIVATED

This is the main event when a user completes payment.

```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "BILLING.SUBSCRIPTION.ACTIVATED",
    "resource": {
      "id": "I-TEST-SUBSCRIPTION-ID",
      "custom_id": "USER-UUID-FROM-DATABASE",
      "status": "ACTIVE",
      "subscriber": {
        "email_address": "user@example.com"
      }
    }
  }'
```

**Expected result:**
- Response: `{"received":true,"event":"BILLING.SUBSCRIPTION.ACTIVATED"}`
- Database: User's subscription status = "active"

---

### BILLING.SUBSCRIPTION.CANCELLED

When user cancels their subscription.

```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "BILLING.SUBSCRIPTION.CANCELLED",
    "resource": {
      "id": "I-TEST-SUBSCRIPTION-ID",
      "custom_id": "USER-UUID-FROM-DATABASE",
      "status": "CANCELLED"
    }
  }'
```

**Expected result:**
- Database: User's subscription status = "cancelled"

---

### BILLING.SUBSCRIPTION.SUSPENDED

When payment fails and subscription is paused.

```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "BILLING.SUBSCRIPTION.SUSPENDED",
    "resource": {
      "id": "I-TEST-SUBSCRIPTION-ID",
      "custom_id": "USER-UUID-FROM-DATABASE",
      "status": "SUSPENDED"
    }
  }'
```

**Expected result:**
- Database: User's subscription status = "suspended"

---

### BILLING.SUBSCRIPTION.EXPIRED

When subscription period ends.

```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "BILLING.SUBSCRIPTION.EXPIRED",
    "resource": {
      "id": "I-TEST-SUBSCRIPTION-ID",
      "custom_id": "USER-UUID-FROM-DATABASE",
      "status": "EXPIRED"
    }
  }'
```

**Expected result:**
- Database: User's subscription status = "expired"

---

### PAYMENT.SALE.COMPLETED

Monthly renewal payment received.

```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "PAYMENT.SALE.COMPLETED",
    "resource": {
      "id": "SALE-ID-123",
      "custom_id": "USER-UUID-FROM-DATABASE",
      "amount": {
        "total": "4.99",
        "currency": "USD"
      }
    }
  }'
```

**Expected result:**
- Database: User's subscription status = "active"

---

## Full PayPal Webhook Payload (Reference)

This is what a real PayPal webhook looks like:

```json
{
  "id": "WH-2WR32451HC0233532-6XXXXXXXX",
  "event_version": "1.0",
  "create_time": "2026-01-25T12:00:00.000Z",
  "resource_type": "subscription",
  "event_type": "BILLING.SUBSCRIPTION.ACTIVATED",
  "summary": "Subscription activated",
  "resource": {
    "id": "I-BW452GLLEP1G",
    "plan_id": "P-3RH33892X5467024SNFZON2Y",
    "start_time": "2026-01-25T12:00:00Z",
    "quantity": "1",
    "shipping_amount": {
      "currency_code": "USD",
      "value": "0.0"
    },
    "subscriber": {
      "email_address": "user@example.com",
      "payer_id": "XXXXXXXXXX",
      "name": {
        "given_name": "John",
        "surname": "Doe"
      }
    },
    "billing_info": {
      "outstanding_balance": {
        "currency_code": "USD",
        "value": "0.0"
      },
      "cycle_executions": [
        {
          "tenure_type": "REGULAR",
          "sequence": 1,
          "cycles_completed": 1,
          "cycles_remaining": 0,
          "current_pricing_scheme_version": 1
        }
      ],
      "last_payment": {
        "amount": {
          "currency_code": "USD",
          "value": "4.99"
        },
        "time": "2026-01-25T12:00:00Z"
      },
      "next_billing_time": "2026-02-25T10:00:00Z"
    },
    "create_time": "2026-01-25T12:00:00Z",
    "update_time": "2026-01-25T12:00:00Z",
    "custom_id": "USER-UUID-HERE",
    "links": [
      {
        "href": "https://api.paypal.com/v1/billing/subscriptions/I-BW452GLLEP1G",
        "rel": "self",
        "method": "GET"
      }
    ],
    "status": "ACTIVE",
    "status_update_time": "2026-01-25T12:00:00Z"
  },
  "links": [
    {
      "href": "https://api.paypal.com/v1/notifications/webhooks-events/WH-2WR32451HC0233532-6XXXXXXXX",
      "rel": "self",
      "method": "GET"
    }
  ]
}
```

---

## Testing Without custom_id

If custom_id is not available, test email matching:

```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "BILLING.SUBSCRIPTION.ACTIVATED",
    "resource": {
      "id": "I-TEST-SUB",
      "subscriber": {
        "email_address": "user-email-in-database@example.com"
      }
    }
  }'
```

---

## Verifying Results

### Check Database After Test

```sql
-- In Supabase SQL Editor
SELECT user_id, status, paypal_subscription_id, updated_at
FROM subscriptions
WHERE user_id = 'USER-UUID-YOU-TESTED';
```

### Check Edge Function Logs

Go to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs

Look for:
```
Event: BILLING.SUBSCRIPTION.ACTIVATED User: USER-UUID SubID: I-TEST123
DB result: {"data":[{"id":"...","user_id":"...","status":"active"...}],"error":null}
```

---

## Common Issues

### Response: `{"err":"..."}` (500 error)

Check:
1. JSON syntax is valid
2. Supabase credentials are configured
3. Edge function is deployed

### Response: 200 but no database change

Check:
1. `custom_id` matches a real user in database
2. Webhook code uses `.upsert()` not `.update()`
3. `subscriptions` table has UNIQUE constraint on `user_id`

---

## Batch Testing Script

Test all event types:

```bash
#!/bin/bash
WEBHOOK="https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook"
USER="YOUR-USER-UUID"

echo "Testing ACTIVATED..."
curl -s -X POST "$WEBHOOK" \
  -H "Content-Type: application/json" \
  -d "{\"event_type\":\"BILLING.SUBSCRIPTION.ACTIVATED\",\"resource\":{\"id\":\"I-TEST\",\"custom_id\":\"$USER\"}}"
echo ""

echo "Testing CANCELLED..."
curl -s -X POST "$WEBHOOK" \
  -H "Content-Type: application/json" \
  -d "{\"event_type\":\"BILLING.SUBSCRIPTION.CANCELLED\",\"resource\":{\"id\":\"I-TEST\",\"custom_id\":\"$USER\"}}"
echo ""

echo "Testing ACTIVATED again (restore)..."
curl -s -X POST "$WEBHOOK" \
  -H "Content-Type: application/json" \
  -d "{\"event_type\":\"BILLING.SUBSCRIPTION.ACTIVATED\",\"resource\":{\"id\":\"I-TEST\",\"custom_id\":\"$USER\"}}"
echo ""
```

---

*Last Updated: January 25, 2026*
