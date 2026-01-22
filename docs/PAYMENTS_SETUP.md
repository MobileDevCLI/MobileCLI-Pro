# MobileCLI Pro - Payments Setup Guide

This document explains how the payment system works and how to manage it.

Last Updated: January 22, 2026

---

## Overview

MobileCLI Pro uses **PayPal Subscriptions** for recurring payments at $15/month.

**Account Type:** PayPal Business (Individual/Sole Proprietor)
**Business Name:** MobileCLI

---

## PayPal Account Requirements

### Why Business Account is Required
- Personal PayPal accounts **cannot** accept recurring subscription payments
- Business accounts are **free** to upgrade
- Required for subscription buttons to work

### How to Upgrade to Business (if needed)
1. Go to https://www.paypal.com/bizsignup/
2. Select "Individual/Sole Proprietor"
3. Enter business name: `MobileCLI`
4. Complete verification

---

## PayPal Configuration

### Subscription Button Details

| Setting | Value |
|---------|-------|
| **Button ID** | `DHCKPWE3PJ684` |
| **Price** | $15.00 USD |
| **Billing Cycle** | Monthly |
| **Stop After** | Never (until cancelled) |

### Payment Link

Direct subscription URL:
```
https://www.paypal.com/ncp/payment/DHCKPWE3PJ684
```

### Managing Subscriptions

1. **View Subscriptions:** https://www.paypal.com/billing/subscriptions
2. **Button Management:** https://www.paypal.com/buttons/
3. **Transaction History:** https://www.paypal.com/activities/

## Payment Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   App       │────▶│   PayPal    │────▶│   Payment   │
│  Paywall    │     │  Checkout   │     │  Complete   │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │   Return    │
                                        │   to App    │
                                        └─────────────┘
```

### User Journey

1. User sees paywall in app
2. Taps "Subscribe - $15/month"
3. App opens PayPal checkout page
4. User logs into PayPal and confirms subscription
5. User returns to app
6. App verifies subscription status

## App Integration

### PaywallActivity.kt

The PayPal URL is stored in:
```kotlin
companion object {
    private const val PAYPAL_SUBSCRIBE_URL = "https://www.paypal.com/ncp/payment/DHCKPWE3PJ684"
}
```

### Opening Checkout

```kotlin
private fun openCheckout() {
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()
    customTabsIntent.launchUrl(this, Uri.parse(PAYPAL_SUBSCRIBE_URL))
}
```

## Webhook Setup (Optional)

For automatic license activation after payment, set up PayPal IPN (Instant Payment Notification):

1. Go to PayPal: https://www.paypal.com/ipn
2. Set IPN URL to: `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook`
3. Create a Supabase Edge Function to handle the webhook

### Supabase Edge Function (paypal-webhook)

```typescript
// supabase/functions/paypal-webhook/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

serve(async (req) => {
  const body = await req.text()
  const params = new URLSearchParams(body)

  const txnType = params.get('txn_type')
  const payerEmail = params.get('payer_email')
  const subscriptionId = params.get('subscr_id')

  if (txnType === 'subscr_payment') {
    // Payment received - activate license
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    )

    // Update user's license to pro
    await supabase
      .from('licenses')
      .update({ tier: 'pro', paypal_subscription_id: subscriptionId })
      .eq('email', payerEmail)
  }

  return new Response('OK', { status: 200 })
})
```

## Changing Price

To change the subscription price:

1. Go to https://www.paypal.com/buttons/
2. Find your subscription button
3. Edit the price
4. Save

The button ID stays the same - no app update needed.

## Cancellations

Users can cancel their subscription at:
- https://www.paypal.com/myaccount/autopay/

You can view/manage subscriber cancellations in your PayPal dashboard.

---

## Viewing Subscribers & Payments

### Where to See Your Subscribers

**PayPal Business Dashboard:**
https://www.paypal.com/businesshub/dashboard

**Subscription Management:**
https://www.paypal.com/billing/subscriptions

**Transaction History:**
https://www.paypal.com/activities/

### Subscriber Information Available

| Data | Where to Find |
|------|---------------|
| Subscriber email | Transaction details |
| Payment date | Activities page |
| Subscription status | Billing → Subscriptions |
| Payment amount | Activities page |
| Subscription ID | Transaction details |

### Monthly Revenue Tracking

1. Go to https://www.paypal.com/activities/
2. Filter by date range
3. Filter by "Subscription payment"
4. Export to CSV if needed

---

## Important PayPal URLs

| Purpose | URL |
|---------|-----|
| Main Dashboard | https://www.paypal.com/businesshub/ |
| View Subscribers | https://www.paypal.com/billing/subscriptions |
| Manage Buttons | https://www.paypal.com/buttons/ |
| Transaction History | https://www.paypal.com/activities/ |
| IPN Settings | https://www.paypal.com/ipn |
| Developer Dashboard | https://developer.paypal.com/dashboard/ |

## Fee Structure

| Amount | PayPal Fee | You Receive |
|--------|------------|-------------|
| $15.00 | $0.93 (3.49% + $0.49) | $14.07 |

Annual revenue per subscriber: ~$168.84

## Testing

### Test the Payment Link

1. Open: https://www.paypal.com/ncp/payment/DHCKPWE3PJ684
2. Log in with a PayPal account
3. Complete the subscription
4. Verify it appears in your PayPal dashboard

### Test in App

1. Install latest APK
2. Login/signup
3. On paywall, tap "Subscribe"
4. Complete PayPal checkout
5. Return to app

## Troubleshooting

### Payment link not working
- Verify button ID is correct: `DHCKPWE3PJ684`
- Check PayPal button is active at https://www.paypal.com/buttons/

### User paid but not activated
- Manual activation: Update their license in Supabase
- Set up IPN webhook for automatic activation

### Refunds
- Process refunds directly in PayPal dashboard
- User's subscription will be cancelled automatically

## Related Files

- `app/src/main/java/com/termux/auth/PaywallActivity.kt` - Payment UI
- `app/src/main/java/com/termux/auth/LicenseManager.kt` - License verification
- `app/src/main/res/layout/activity_paywall.xml` - Paywall layout
