# PayPal Payment Integration for MobileCLI

**Status:** WORKING (January 25, 2026)
**Price:** $4.99/month subscription
**Can be reactivated anytime**

---

## Quick Summary

This folder contains everything needed to run PayPal subscriptions:
- Supabase Edge Function webhook
- Database schema
- Setup instructions
- Troubleshooting guide

**If you're reading this in the future and want to set up PayPal again, read `SETUP_GUIDE.md`.**

---

## Architecture

```
User clicks Subscribe
    |
    v
PayPal Checkout (with custom_id = user_id)
    |
    v
User Pays $4.99/month
    |
    v
PayPal sends Webhook to Supabase Edge Function
    |
    v
Edge Function uses UPSERT to update/create subscription row
    |
    v
User has Pro access (status = 'active')
```

---

## Key IDs (Not Secrets)

| Item | Value |
|------|-------|
| PayPal Plan ID | `P-3RH33892X5467024SNFZON2Y` |
| PayPal Button ID | `DHCKPWE3PJ684` |
| Supabase Project | `mwxlguqukyfberyhtkmg` |
| Webhook URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook` |

---

## Files in This Directory

| File | Purpose |
|------|---------|
| `README.md` | This overview |
| `STORY.md` | The full story of building this integration |
| `SETUP_GUIDE.md` | Step-by-step setup from scratch |
| `WEBHOOK_CODE.md` | The working webhook with explanations |
| `DATABASE_SCHEMA.md` | All SQL needed |
| `TROUBLESHOOTING.md` | Problems encountered and solutions |
| `TEST_PAYLOADS.md` | How to test webhooks |

---

## To Reactivate PayPal

1. Read `SETUP_GUIDE.md` for full instructions
2. Deploy webhook code from `WEBHOOK_CODE.md`
3. Run SQL from `DATABASE_SCHEMA.md`
4. Configure PayPal webhook URL in PayPal Dashboard
5. Test with payloads from `TEST_PAYLOADS.md`

---

## The Critical Fix (January 25, 2026)

**Problem:** Webhooks were receiving events but database wasn't updating.

**Root Cause:** Using `.update()` which returns empty array (not error) when no row matches.

**Solution:** Changed to `.upsert()` which creates the row if it doesn't exist.

See `STORY.md` for the full debugging journey.

---

## Quick Test

```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{"event_type":"BILLING.SUBSCRIPTION.ACTIVATED","resource":{"id":"I-TEST123","custom_id":"YOUR-USER-UUID"}}'
```

---

*Last Updated: January 25, 2026*
