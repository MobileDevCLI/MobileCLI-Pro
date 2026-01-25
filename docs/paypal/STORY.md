# The PayPal Integration Story

**A complete history of building and debugging the PayPal payment system for MobileCLI Pro.**

---

## Timeline

### Week 1: Initial Setup (January 2026)

#### Goals
- Set up PayPal subscription plan ($4.99/month later changed to $15/month)
- Create Supabase Edge Function for webhooks
- Set up `subscriptions` table in database

#### What Was Built
1. **PayPal Subscription Plan**
   - Plan ID: `P-3RH33892X5467024SNFZON2Y`
   - Button ID: `DHCKPWE3PJ684`
   - Price: $4.99/month recurring

2. **Supabase Edge Function**
   - URL: `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook`
   - Handles BILLING.SUBSCRIPTION.* events

3. **Database Tables**
   - `subscriptions` - Main subscription tracking
   - `payment_history` - Financial records
   - `webhook_logs` - Audit trail

---

### Week 2: First Problems

#### Symptom
- Webhook was receiving events (logs showed "Event: BILLING.SUBSCRIPTION.ACTIVATED")
- Database wasn't updating (status stayed "trial")
- No errors in logs

#### Investigation
1. Checked PayPal webhook configuration - OK
2. Checked Supabase Edge Function logs - Events received
3. Checked database - Row existed but status not updated
4. Checked webhook code - Used `.update().eq("user_id", customId)`

---

### January 25, 2026: THE FIX

#### Root Cause Discovery

The problem was in this code:
```javascript
const { data, error } = await supabase
  .from("subscriptions")
  .update({ status: "active" })
  .eq("user_id", customId)
  .select()
```

**Key insight:** Supabase `.update()` returns an EMPTY ARRAY (not an error!) when no row matches the `eq()` condition.

**Timeline of what happens:**
1. User signs up -> `subscriptions` row may not exist yet
2. User pays via PayPal
3. PayPal sends webhook
4. Webhook does `.update().eq("user_id", ...)`
5. No row exists -> `.update()` returns `{ data: [], error: null }`
6. Looks successful! But nothing was updated

#### The Solution

Changed from `.update()` to `.upsert()`:

```javascript
const { data, error } = await supabase
  .from("subscriptions")
  .upsert({
    user_id: customId,
    status: "active",
    paypal_subscription_id: subscriptionId,
    updated_at: new Date().toISOString()
  }, { onConflict: "user_id" })
  .select()
```

**UPSERT behavior:**
- If row with `user_id` exists -> UPDATE it
- If row doesn't exist -> INSERT it

#### Verification
- Deployed fixed webhook
- Tested with manual curl
- Database showed `status=active`
- Success!

---

## What Worked

1. **PayPal subscription flow** - Users can subscribe via PayPal
2. **Webhook receiving events** - PayPal sends events to our endpoint
3. **UPSERT approach** - Handles both new and existing rows
4. **Supabase Edge Functions** - Reliable serverless execution

---

## What Didn't Work

### 1. `.update()` for webhook handlers
- Fails silently when row doesn't exist
- Supabase returns `{ data: [], error: null }`
- Looks successful but nothing happens

### 2. `custom_id` via URL parameter
- PayPal subscription URLs don't reliably pass `custom_id` as URL param
- Needed PayPal JavaScript SDK for reliable custom_id passing
- Workaround: Match by email instead

### 3. IPN vs REST Webhooks
- Initially tried legacy IPN (Instant Payment Notification)
- Switched to REST webhooks for better event structure
- REST webhooks provide more detailed payload

---

## Lessons Learned

### 1. Always use UPSERT for webhooks
Webhook handlers should never assume a row exists:
```javascript
// BAD - fails silently if no row
.update({ status: "active" }).eq("user_id", id)

// GOOD - creates row if missing
.upsert({ user_id: id, status: "active" }, { onConflict: "user_id" })
```

### 2. Check Supabase Edge Function logs for debugging
- Go to: Supabase Dashboard -> Functions -> paypal-webhook -> Logs
- Add console.log statements liberally
- Log the actual data/error objects

### 3. Test webhooks manually before relying on PayPal
```bash
curl -X POST "YOUR_WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{"event_type":"BILLING.SUBSCRIPTION.ACTIVATED","resource":{"id":"TEST","custom_id":"USER-UUID"}}'
```

### 4. Understand Supabase return values
| Method | No Match Behavior |
|--------|-------------------|
| `.select()` | Returns `{ data: [], error: null }` |
| `.update()` | Returns `{ data: [], error: null }` |
| `.delete()` | Returns `{ data: [], error: null }` |
| `.insert()` | Returns error if constraint violated |
| `.upsert()` | Creates row if missing |

### 5. Document everything
This documentation exists so any future Claude (or human) can:
- Understand what was built
- Know what problems occurred
- Fix issues without re-discovering them
- Reactivate the system if needed

---

## Timeline Summary

| Date | Event |
|------|-------|
| Early Jan 2026 | Initial PayPal setup |
| Mid Jan 2026 | Webhook receiving events but not updating DB |
| Jan 25, 2026 | Root cause found (`.update()` silent failure) |
| Jan 25, 2026 | Fixed with `.upsert()` |
| Jan 25, 2026 | Documented everything for archive |

---

## Files That Were Changed

1. **supabase/functions/paypal-webhook/index.ts**
   - Changed `.update()` to `.upsert()`
   - Added `onConflict: "user_id"` option

2. **Database schema**
   - Added `UNIQUE` constraint on `user_id` in `subscriptions` table
   - Required for upsert to work

---

## If Problems Return

1. **Check webhook logs** in Supabase Dashboard
2. **Test manually** with curl (see TEST_PAYLOADS.md)
3. **Read this document** - the fix is documented here
4. **Don't use .update()** - always use .upsert() for webhooks

---

*Written: January 25, 2026*
*Author: Claude AI working inside MobileCLI on Android*
