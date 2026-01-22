# MobileCLI Pro - PayPal Webhook Setup

Last Updated: January 22, 2026

This guide walks you through setting up the PayPal IPN webhook so payments automatically activate subscriptions.

---

## Overview

When a user pays via PayPal:
1. PayPal sends an IPN (Instant Payment Notification) to your webhook
2. The webhook updates the `subscriptions` table in Supabase
3. Next time the user opens the app, they have Pro access

---

## Step 1: Deploy the Edge Function to Supabase

### Option A: Using Supabase Dashboard (Easiest)

1. Go to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions

2. Click **"Create a new function"**

3. Name it: `paypal-webhook`

4. Copy the entire contents of `supabase/functions/paypal-webhook/index.ts` into the editor

5. Click **Deploy**

### Option B: Using Supabase CLI

```bash
# Install Supabase CLI (if not installed)
npm install -g supabase

# Login to Supabase
supabase login

# Link to your project
supabase link --project-ref mwxlguqukyfberyhtkmg

# Deploy the function
supabase functions deploy paypal-webhook
```

---

## Step 2: Verify Edge Function is Working

After deployment, your webhook URL is:
```
https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook
```

Test it with curl:
```bash
curl -X POST https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "txn_type=test&payer_email=test@example.com"
```

Should return: `OK`

---

## Step 3: Configure PayPal IPN

1. **Go to PayPal IPN Settings:**
   https://www.paypal.com/ipn

2. **Click "Choose IPN Settings"**

3. **Enter Notification URL:**
   ```
   https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook
   ```

4. **Select "Receive IPN messages (Enabled)"**

5. **Click "Save"**

---

## Step 4: Test the Complete Flow

### Manual Test
1. Create a test user account in your app
2. Complete a PayPal subscription
3. Check Supabase `subscriptions` table - should show `status: 'active'`
4. Open app - should have Pro access

### Check Webhook Logs
1. Go to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs
2. Look for incoming PayPal notifications
3. Verify no errors

---

## IPN Transaction Types

The webhook handles these PayPal notifications:

| Type | Meaning | Action |
|------|---------|--------|
| `subscr_signup` | New subscription | Activate user |
| `subscr_payment` | Monthly payment | Extend 30 days |
| `subscr_cancel` | User cancelled | Mark cancelled |
| `subscr_eot` | Subscription ended | Mark expired |
| `subscr_failed` | Payment failed | No action (PayPal retries) |

---

## Troubleshooting

### User Paid But Not Activated

1. **Check PayPal IPN History:**
   https://www.paypal.com/ipn/history
   - Verify IPN was sent
   - Check delivery status

2. **Check Supabase Logs:**
   https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs
   - Look for errors

3. **Verify Email Matches:**
   - PayPal payer email must match Supabase user email
   - If using Google OAuth, the Google email must match PayPal email

4. **Manual Activation:**
   ```sql
   UPDATE subscriptions
   SET status = 'active',
       expires_at = NOW() + INTERVAL '30 days'
   WHERE user_id = 'user-uuid-here';
   ```

### IPN Not Being Received

1. Check PayPal IPN settings are enabled
2. Verify the webhook URL is correct
3. Check Supabase Edge Function is deployed
4. Look at PayPal IPN History for delivery errors

### Edge Function Errors

Check logs at:
https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs

Common issues:
- Missing environment variables (SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)
- Function not deployed
- Syntax errors in code

---

## Security Notes

1. **IPN Verification:** The webhook should verify PayPal IPNs are legitimate by posting back to PayPal. This is currently logged but not enforced.

2. **Service Role Key:** The Edge Function uses the service role key to bypass RLS. Never expose this key in client code.

3. **Email Matching:** Users must use the same email for PayPal and app login for automatic matching.

---

## Important URLs

| Resource | URL |
|----------|-----|
| Supabase Functions | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions |
| Supabase Logs | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs |
| PayPal IPN Settings | https://www.paypal.com/ipn |
| PayPal IPN History | https://www.paypal.com/ipn/history |
| PayPal Subscriptions | https://www.paypal.com/billing/subscriptions |

---

## Quick Reference

**Webhook URL:**
```
https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook
```

**Supabase Project ID:** `mwxlguqukyfberyhtkmg`

**PayPal Button ID:** `DHCKPWE3PJ684`
