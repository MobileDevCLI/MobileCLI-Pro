# MobileCLI Pro - Admin Operations Guide

**Supabase Dashboard:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg

---

## Quick Reference

### Dashboard Links
- **SQL Editor:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql/new
- **Table Editor:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/editor
- **Auth Users:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/auth/users
- **Edge Functions:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions

---

## Common Admin Tasks

### 1. Manually Activate a User's Subscription

When a user paid but webhook didn't process correctly:

```sql
-- Find user by email
SELECT id, email FROM auth.users WHERE email = 'user@example.com';

-- Activate their subscription (30 days from now)
UPDATE subscriptions
SET
    status = 'active',
    expires_at = NOW() + INTERVAL '30 days',
    last_payment_at = NOW(),
    admin_notes = 'Manually activated by admin on ' || NOW(),
    updated_at = NOW()
WHERE user_id = 'USER_UUID_HERE';

-- Or use UPSERT if no subscription exists
INSERT INTO subscriptions (user_id, status, expires_at, last_payment_at, admin_notes)
VALUES (
    'USER_UUID_HERE',
    'active',
    NOW() + INTERVAL '30 days',
    NOW(),
    'Manually activated by admin'
)
ON CONFLICT (user_id)
DO UPDATE SET
    status = 'active',
    expires_at = NOW() + INTERVAL '30 days',
    last_payment_at = NOW(),
    admin_notes = 'Manually activated by admin on ' || NOW(),
    updated_at = NOW();
```

### 2. Extend a User's Subscription

```sql
-- Extend by 30 days from current expiry (or now if expired)
UPDATE subscriptions
SET
    expires_at = GREATEST(expires_at, NOW()) + INTERVAL '30 days',
    admin_notes = COALESCE(admin_notes || E'\n', '') || 'Extended 30 days by admin on ' || NOW(),
    updated_at = NOW()
WHERE user_id = 'USER_UUID_HERE';
```

### 3. Cancel a User's Subscription

```sql
UPDATE subscriptions
SET
    status = 'cancelled',
    cancelled_at = NOW(),
    cancel_reason = 'Admin cancelled',
    admin_notes = COALESCE(admin_notes || E'\n', '') || 'Cancelled by admin on ' || NOW(),
    updated_at = NOW()
WHERE user_id = 'USER_UUID_HERE';
```

### 4. View User's Subscription Status

```sql
-- Full subscription info
SELECT
    u.email,
    s.status,
    s.expires_at,
    s.paypal_subscription_id,
    s.last_payment_at,
    s.cancelled_at,
    s.cancel_reason,
    s.admin_notes,
    s.created_at
FROM subscriptions s
JOIN auth.users u ON u.id = s.user_id
WHERE u.email = 'user@example.com';
```

### 5. View User's Payment History

```sql
SELECT
    p.created_at,
    p.amount,
    p.currency,
    p.payment_type,
    p.status,
    p.paypal_transaction_id,
    p.description
FROM payment_history p
JOIN auth.users u ON u.id = p.user_id
WHERE u.email = 'user@example.com'
ORDER BY p.created_at DESC;
```

### 6. View User's Webhook Events

```sql
SELECT
    w.created_at,
    w.event_type,
    w.processed,
    w.processing_result,
    w.error_message
FROM webhook_logs w
WHERE w.user_email = 'user@example.com'
ORDER BY w.created_at DESC;
```

---

## Admin User Management

### Add Yourself as Super Admin

```sql
INSERT INTO admin_users (user_id, role, notes)
SELECT id, 'super_admin', 'Initial admin setup'
FROM auth.users
WHERE email = 'your-email@example.com';
```

### Add Another Admin

```sql
INSERT INTO admin_users (user_id, role, created_by, notes)
SELECT
    new_admin.id,
    'admin',
    creator.id,
    'Added for support operations'
FROM auth.users new_admin
CROSS JOIN auth.users creator
WHERE new_admin.email = 'new-admin@example.com'
AND creator.email = 'your-email@example.com';
```

### List All Admins

```sql
SELECT
    u.email,
    a.role,
    a.permissions,
    a.created_at
FROM admin_users a
JOIN auth.users u ON u.id = a.user_id
ORDER BY a.created_at;
```

### Remove Admin

```sql
DELETE FROM admin_users
WHERE user_id = (SELECT id FROM auth.users WHERE email = 'admin@example.com');
```

---

## Troubleshooting

### User Says "I Paid But It Didn't Work"

1. **Check webhook logs:**
```sql
SELECT
    w.created_at,
    w.event_type,
    w.processing_result,
    w.error_message,
    w.payload->>'resource'->>'id' as subscription_id
FROM webhook_logs w
WHERE w.user_email ILIKE '%user-email%'
   OR w.payload::text ILIKE '%user-email%'
ORDER BY w.created_at DESC
LIMIT 10;
```

2. **If webhook shows `user_not_found`:**
   - User's PayPal email differs from login email
   - Manually activate using their login email

3. **If no webhook at all:**
   - PayPal webhook may have failed
   - Check PayPal dashboard for webhook delivery status
   - Manually activate

### User Can't Log In

1. **Check if user exists:**
```sql
SELECT id, email, created_at, last_sign_in_at
FROM auth.users
WHERE email = 'user@example.com';
```

2. **Reset password (send email):**
   - Go to Auth Users in dashboard
   - Find user → Actions → Send password reset email

### Webhook Processing Failed

1. **Find failed webhooks:**
```sql
SELECT *
FROM webhook_logs
WHERE processing_result NOT IN ('success', 'skipped')
ORDER BY created_at DESC
LIMIT 20;
```

2. **Retry failed webhook manually:**
   - Read the `payload` JSON
   - Extract user email and subscription info
   - Manually update subscription

---

## Metrics & Reporting

### Active Subscribers Count

```sql
SELECT
    status,
    COUNT(*) as count
FROM subscriptions
GROUP BY status
ORDER BY count DESC;
```

### Revenue Summary

```sql
SELECT
    DATE_TRUNC('month', created_at) as month,
    COUNT(*) as payments,
    SUM(amount) as revenue,
    SUM(CASE WHEN status = 'failed' THEN 1 ELSE 0 END) as failed
FROM payment_history
WHERE created_at > NOW() - INTERVAL '6 months'
GROUP BY month
ORDER BY month DESC;
```

### Daily Signups

```sql
SELECT
    DATE(created_at) as date,
    COUNT(*) as signups
FROM auth.users
WHERE created_at > NOW() - INTERVAL '30 days'
GROUP BY date
ORDER BY date DESC;
```

### Trial to Paid Conversion

```sql
SELECT
    COUNT(*) FILTER (WHERE status = 'trial') as in_trial,
    COUNT(*) FILTER (WHERE status = 'active') as paid,
    ROUND(
        COUNT(*) FILTER (WHERE status = 'active')::DECIMAL /
        NULLIF(COUNT(*), 0) * 100, 1
    ) as conversion_rate
FROM subscriptions;
```

### Users Whose Trial Expires Soon

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

## PayPal Integration

### PayPal Subscription Plan ID
```
P-3RH33892X5467024SNFZON2Y
```

### PayPal Webhook URL
```
https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook
```

### Test Webhook Locally

```bash
curl -X POST "https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "BILLING.SUBSCRIPTION.ACTIVATED",
    "id": "TEST-EVENT-123",
    "resource": {
      "id": "I-TEST123",
      "custom_id": "USER_UUID_HERE",
      "subscriber": {
        "email_address": "test@example.com"
      }
    }
  }'
```

### View Recent PayPal Events

```sql
SELECT
    created_at,
    event_type,
    user_email,
    processing_result,
    payload->'resource'->>'id' as subscription_id
FROM webhook_logs
WHERE provider = 'paypal'
ORDER BY created_at DESC
LIMIT 50;
```

---

## Database Maintenance

### Check Table Sizes

```sql
SELECT
    relname as table,
    pg_size_pretty(pg_total_relation_size(relid)) as size
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC;
```

### Clean Up Old Webhook Logs (older than 90 days)

```sql
DELETE FROM webhook_logs
WHERE created_at < NOW() - INTERVAL '90 days';
```

### Check RLS is Enabled

```sql
SELECT
    tablename,
    rowsecurity
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;
```

---

## Emergency Procedures

### Disable Subscription Enforcement (EMERGENCY ONLY)

If there's a database issue preventing users from accessing the app, you can temporarily disable enforcement by updating the RLS policy or creating a function that always returns true.

**DO NOT do this unless absolutely necessary.**

### Restore From Backup

Supabase maintains point-in-time recovery. Contact Supabase support or use the dashboard:
1. Go to Settings → Database → Backups
2. Select a restore point
3. Restore to a new project for testing

---

## Contact

For issues with this system, contact the development team or check the documentation in `/docs/`.
