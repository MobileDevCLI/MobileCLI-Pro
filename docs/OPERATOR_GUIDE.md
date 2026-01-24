# MobileCLI Pro - Operator Guide

**Version:** 2.0.13
**Last Updated:** January 23, 2026
**Author:** Claude Opus 4.5

---

## Quick Start

### Admin Access

| Credential | Value |
|------------|-------|
| **Admin Email** | admin@mobilecli.com |
| **Admin Password** | EliteDev |

### Key URLs

| Service | URL |
|---------|-----|
| **Supabase Dashboard** | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg |
| **Supabase SQL Editor** | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql/new |
| **Supabase Auth Users** | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/auth/users |
| **Supabase Edge Functions** | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions |
| **PayPal Dashboard** | https://developer.paypal.com/dashboard/applications |
| **GitHub Repository** | https://github.com/MobileDevCLI/MobileCLI-Pro |

---

## 1. System Overview

MobileCLI Pro is a subscription-based Android terminal app with:

- **Supabase** for authentication and database
- **PayPal** for payment processing ($9.99/month subscription)
- **7-day free trial** before paywall

### How It Works

```
User Downloads App → Login/Signup → 7-Day Trial → PayPal Subscription → Pro Access

PayPal Webhook → Supabase Edge Function → Database Updated → App Verifies License
```

### Subscription Price

| Plan | Price | Billing |
|------|-------|---------|
| **MobileCLI Pro** | $9.99/month | Recurring via PayPal |

---

## 2. Supabase Dashboard

### Accessing Supabase

1. Go to https://supabase.com/dashboard
2. Login with your Supabase account
3. Select project: **mwxlguqukyfberyhtkmg**

### Key Sections

| Section | What It Does | URL |
|---------|-------------|-----|
| **Auth → Users** | View/manage all registered users | .../auth/users |
| **Table Editor** | Browse database tables | .../editor |
| **SQL Editor** | Run SQL queries directly | .../sql/new |
| **Edge Functions** | View webhook function logs | .../functions |
| **Logs** | View real-time logs | .../logs |

### Database Tables

| Table | Purpose |
|-------|---------|
| `subscriptions` | User subscription status (active, trial, cancelled) |
| `payment_history` | All payment records |
| `webhook_logs` | PayPal webhook audit trail |
| `email_logs` | Email notifications sent |
| `admin_users` | Admin dashboard access |
| `user_profiles` | Extended user info |

---

## 3. PayPal Configuration

### PayPal Details

| Setting | Value |
|---------|-------|
| **Subscription Plan ID** | P-3RH33892X5467024SNFZON2Y |
| **Webhook URL** | https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook |
| **Price** | $9.99/month |

### Checking PayPal Dashboard

1. Go to https://developer.paypal.com/dashboard
2. Login with your PayPal developer account
3. Go to Applications → REST API apps
4. Check webhook delivery status

### PayPal Events We Handle

| Event | What Happens |
|-------|--------------|
| `BILLING.SUBSCRIPTION.ACTIVATED` | User subscription activated |
| `BILLING.SUBSCRIPTION.CANCELLED` | User cancelled subscription |
| `BILLING.SUBSCRIPTION.SUSPENDED` | Payment issue - suspended |
| `BILLING.SUBSCRIPTION.EXPIRED` | Subscription expired |
| `PAYMENT.SALE.COMPLETED` | Monthly renewal payment |
| `BILLING.SUBSCRIPTION.PAYMENT.FAILED` | Payment failed |

---

## 4. Common Operations

### View All Users With Subscription Status

```sql
SELECT
  u.email,
  u.created_at as signup_date,
  s.status,
  s.expires_at,
  s.paypal_subscription_id,
  s.last_payment_at
FROM auth.users u
LEFT JOIN subscriptions s ON u.id = s.user_id
ORDER BY u.created_at DESC
LIMIT 100;
```

### Find User By Email

```sql
SELECT
  u.id,
  u.email,
  u.created_at,
  s.status,
  s.expires_at,
  s.paypal_subscription_id
FROM auth.users u
LEFT JOIN subscriptions s ON u.id = s.user_id
WHERE u.email ILIKE '%user@example.com%';
```

### Manually Activate a User

When a user paid but webhook didn't work:

```sql
-- First find the user
SELECT id, email FROM auth.users WHERE email = 'user@example.com';

-- Then activate (replace USER_UUID with actual ID)
INSERT INTO subscriptions (user_id, status, expires_at, last_payment_at, admin_notes)
VALUES (
    'USER_UUID_HERE',
    'active',
    NOW() + INTERVAL '30 days',
    NOW(),
    'Manually activated by admin - ' || NOW()::DATE
)
ON CONFLICT (user_id)
DO UPDATE SET
    status = 'active',
    expires_at = NOW() + INTERVAL '30 days',
    last_payment_at = NOW(),
    admin_notes = 'Manually activated by admin - ' || NOW()::DATE,
    updated_at = NOW();
```

### Extend a Subscription

```sql
UPDATE subscriptions
SET
    expires_at = GREATEST(expires_at, NOW()) + INTERVAL '30 days',
    admin_notes = COALESCE(admin_notes || E'\n', '') || 'Extended 30 days - ' || NOW()::DATE,
    updated_at = NOW()
WHERE user_id = 'USER_UUID_HERE';
```

### Cancel a Subscription

```sql
UPDATE subscriptions
SET
    status = 'cancelled',
    cancelled_at = NOW(),
    cancel_reason = 'Admin cancelled',
    updated_at = NOW()
WHERE user_id = 'USER_UUID_HERE';
```

### View Payment History

```sql
SELECT
    p.created_at,
    u.email,
    p.amount,
    p.currency,
    p.payment_type,
    p.status,
    p.paypal_transaction_id
FROM payment_history p
JOIN auth.users u ON u.id = p.user_id
ORDER BY p.created_at DESC
LIMIT 50;
```

### View Webhook Logs

```sql
SELECT
    w.created_at,
    w.event_type,
    w.user_email,
    w.processed,
    w.processing_result,
    w.error_message
FROM webhook_logs w
ORDER BY w.created_at DESC
LIMIT 50;
```

### View Failed Webhooks

```sql
SELECT
    w.created_at,
    w.event_type,
    w.user_email,
    w.processing_result,
    w.error_message,
    w.payload
FROM webhook_logs w
WHERE w.processing_result NOT IN ('success', 'skipped')
ORDER BY w.created_at DESC
LIMIT 20;
```

---

## 5. Admin User Management

### Add Yourself as Admin

```sql
INSERT INTO admin_users (user_id, role, notes)
SELECT id, 'super_admin', 'Initial admin setup'
FROM auth.users
WHERE email = 'your-email@example.com';
```

### Add Another Admin

```sql
INSERT INTO admin_users (user_id, role, notes)
SELECT id, 'admin', 'Support team member'
FROM auth.users
WHERE email = 'new-admin@example.com';
```

### List All Admins

```sql
SELECT
    u.email,
    a.role,
    a.created_at
FROM admin_users a
JOIN auth.users u ON u.id = a.user_id
ORDER BY a.created_at;
```

### Remove an Admin

```sql
DELETE FROM admin_users
WHERE user_id = (SELECT id FROM auth.users WHERE email = 'admin@example.com');
```

---

## 6. Metrics & Reporting

### Subscription Counts by Status

```sql
SELECT
    status,
    COUNT(*) as count
FROM subscriptions
GROUP BY status
ORDER BY count DESC;
```

### Revenue by Month

```sql
SELECT
    DATE_TRUNC('month', created_at) as month,
    COUNT(*) as payments,
    SUM(amount) as revenue
FROM payment_history
WHERE status = 'completed'
AND created_at > NOW() - INTERVAL '6 months'
GROUP BY month
ORDER BY month DESC;
```

### Total Revenue

```sql
SELECT
    SUM(amount) as total_revenue,
    COUNT(*) as total_payments
FROM payment_history
WHERE status = 'completed';
```

### Trial to Paid Conversion Rate

```sql
SELECT
    COUNT(*) FILTER (WHERE status = 'trial') as in_trial,
    COUNT(*) FILTER (WHERE status = 'active') as paid,
    ROUND(
        COUNT(*) FILTER (WHERE status = 'active')::DECIMAL /
        NULLIF(COUNT(*), 0) * 100, 1
    ) as conversion_rate_percent
FROM subscriptions;
```

### Users with Expiring Trials (next 7 days)

```sql
SELECT
    u.email,
    s.expires_at,
    EXTRACT(DAY FROM s.expires_at - NOW())::INT as days_left
FROM subscriptions s
JOIN auth.users u ON u.id = s.user_id
WHERE s.status = 'trial'
AND s.expires_at BETWEEN NOW() AND NOW() + INTERVAL '7 days'
ORDER BY s.expires_at;
```

---

## 7. Troubleshooting

### "User paid but can't access Pro"

1. **Check webhook logs:**
```sql
SELECT * FROM webhook_logs
WHERE user_email ILIKE '%their-email%'
ORDER BY created_at DESC;
```

2. **If `user_not_found`:** User's PayPal email differs from app login email. Manually activate.

3. **If no webhook at all:** Check PayPal dashboard for webhook delivery. Manually activate.

4. **Manually activate:**
```sql
UPDATE subscriptions
SET status = 'active', expires_at = NOW() + INTERVAL '30 days'
WHERE user_id = (SELECT id FROM auth.users WHERE email = 'their-app-email@example.com');
```

### "User can't log in"

1. **Check if user exists:**
```sql
SELECT * FROM auth.users WHERE email = 'user@example.com';
```

2. **Reset password:** Go to Auth → Users → Find user → Actions → Send password reset email

### "Webhook processing failed"

1. **Find the failed webhook:**
```sql
SELECT * FROM webhook_logs
WHERE processing_result = 'db_error'
ORDER BY created_at DESC LIMIT 5;
```

2. **Check error_message column** for details

3. **Manually process** by reading the payload and updating subscription

---

## 8. Emergency Procedures

### Emergency: Mass Subscription Issue

If many users report access issues:

1. **Check if Supabase is up:** https://status.supabase.com
2. **Check webhook function logs** in Supabase dashboard
3. **Check PayPal status:** https://status.paypal.com

### Emergency: Manually Grant Access to All Trial Users

If there's a system issue affecting trials:

```sql
-- Give all trial users 7 more days
UPDATE subscriptions
SET expires_at = expires_at + INTERVAL '7 days'
WHERE status = 'trial';
```

### Emergency: Disable Paywall (EXTREME ONLY)

This would require an app update. The app checks subscription status from Supabase.

**Better approach:** Grant individual users manual subscriptions.

---

## 9. Technical Reference

### Supabase Project Details

| Setting | Value |
|---------|-------|
| **Project ID** | mwxlguqukyfberyhtkmg |
| **Project URL** | https://mwxlguqukyfberyhtkmg.supabase.co |
| **Anon Key** | eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (in app code) |

### Webhook Function

The PayPal webhook is handled by a Supabase Edge Function:

- **Function name:** `paypal-webhook`
- **Location:** `supabase/functions/paypal-webhook/index.ts`
- **Logs:** Supabase Dashboard → Edge Functions → paypal-webhook → Logs

### User Matching Logic

The webhook tries to match users in order:

1. **custom_id** - Supabase user_id passed from app (most reliable)
2. **subscriber.email_address** - PayPal subscriber email (fallback)

If matching fails, check if user's PayPal email differs from app login email.

---

## 10. Files & Source Code

### Key Files

| File | Purpose |
|------|---------|
| `app/src/main/java/com/termux/auth/LoginActivity.kt` | Login screen |
| `app/src/main/java/com/termux/auth/PaywallActivity.kt` | Paywall screen |
| `app/src/main/java/com/termux/auth/LicenseManager.kt` | License verification |
| `app/src/main/java/com/termux/auth/SupabaseClient.kt` | Supabase SDK config |
| `supabase/functions/paypal-webhook/index.ts` | Webhook handler |
| `supabase/migrations/002_professional_subscription.sql` | Database schema |

### Documentation

| File | Purpose |
|------|---------|
| `docs/DATABASE_SCHEMA.md` | Full database schema |
| `docs/ADMIN_OPERATIONS.md` | SQL queries for admin tasks |
| `docs/AUTHENTICATION_AND_PAYMENTS.md` | Auth & payment flow |
| `docs/QUICK_REFERENCE.md` | One-page cheat sheet |

---

## Contact

For issues with this system:

1. **Check this documentation first**
2. **Check Supabase logs** for errors
3. **Check webhook_logs table** for failed events
4. **Check PayPal dashboard** for webhook delivery

---

*This document is designed to help operate the MobileCLI Pro subscription system. Keep it updated when the system changes.*
