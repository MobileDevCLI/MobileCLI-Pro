# MobileCLI Pro - Database Schema Documentation

**Supabase Project:** mwxlguqukyfberyhtkmg
**URL:** https://mwxlguqukyfberyhtkmg.supabase.co
**Dashboard:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg

---

## Overview

This document describes the complete database schema for MobileCLI Pro's subscription system.

### Tables

| Table | Purpose |
|-------|---------|
| `subscriptions` | User subscription status and PayPal info |
| `payment_history` | Record of all payments (completed, failed, refunded) |
| `webhook_logs` | Audit trail of all PayPal webhook events |
| `email_logs` | Track all emails sent to users |
| `admin_users` | Dashboard access control |
| `user_profiles` | Extended user information |

---

## 1. subscriptions

Primary table for tracking user subscription status.

### Columns

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `user_id` | UUID | Foreign key to auth.users (unique) |
| `status` | TEXT | 'active', 'trial', 'cancelled', 'suspended', 'expired' |
| `paypal_subscription_id` | TEXT | PayPal subscription ID |
| `paypal_payer_id` | TEXT | PayPal payer ID |
| `expires_at` | TIMESTAMPTZ | When subscription/trial expires |
| `trial_started_at` | TIMESTAMPTZ | When trial started |
| `trial_reminder_sent` | BOOLEAN | Whether trial reminder was sent |
| `payment_failed_at` | TIMESTAMPTZ | Last payment failure |
| `last_payment_at` | TIMESTAMPTZ | Last successful payment |
| `cancelled_at` | TIMESTAMPTZ | When cancelled |
| `cancel_reason` | TEXT | Reason for cancellation |
| `admin_notes` | TEXT | Admin notes about this user |
| `created_at` | TIMESTAMPTZ | When created |
| `updated_at` | TIMESTAMPTZ | When last updated |

### RLS Policies

- Users can view their own subscription
- Users can create their own subscription (for trial)
- Only service_role can update subscriptions

### Key Functions

```sql
-- Check if subscription is valid
SELECT is_subscription_valid('user-uuid-here');

-- Get subscription status details
SELECT * FROM get_subscription_status('user-uuid-here');
```

---

## 2. payment_history

Records every payment transaction.

### Columns

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `user_id` | UUID | Foreign key to auth.users |
| `amount` | DECIMAL(10,2) | Payment amount |
| `currency` | TEXT | Currency code (default 'USD') |
| `payment_type` | TEXT | 'subscription_initial', 'subscription_renewal', 'one_time', 'refund' |
| `provider` | TEXT | 'paypal', 'stripe', 'manual' |
| `paypal_transaction_id` | TEXT | PayPal transaction ID |
| `paypal_subscription_id` | TEXT | PayPal subscription ID |
| `stripe_payment_intent_id` | TEXT | Stripe payment intent ID |
| `stripe_invoice_id` | TEXT | Stripe invoice ID |
| `status` | TEXT | 'completed', 'pending', 'failed', 'refunded' |
| `description` | TEXT | Human-readable description |
| `metadata` | JSONB | Additional data |
| `created_at` | TIMESTAMPTZ | When recorded |

### RLS Policies

- Users can view their own payment history
- Only service_role can insert (from webhooks)

### Example Queries

```sql
-- Get user's payment history
SELECT * FROM payment_history
WHERE user_id = 'user-uuid'
ORDER BY created_at DESC;

-- Get total revenue
SELECT SUM(amount) as total
FROM payment_history
WHERE status = 'completed';

-- Get revenue by month
SELECT
    DATE_TRUNC('month', created_at) as month,
    SUM(amount) as revenue
FROM payment_history
WHERE status = 'completed'
GROUP BY month
ORDER BY month DESC;
```

---

## 3. webhook_logs

Complete audit trail of all PayPal webhook events.

### Columns

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `event_type` | TEXT | PayPal event type (e.g., 'BILLING.SUBSCRIPTION.ACTIVATED') |
| `event_id` | TEXT | PayPal's event ID |
| `provider` | TEXT | 'paypal' or 'stripe' |
| `payload` | JSONB | Full webhook payload |
| `user_id` | UUID | Matched user (if found) |
| `user_email` | TEXT | Email from webhook (for matching) |
| `processed` | BOOLEAN | Whether processing completed |
| `processing_result` | TEXT | 'success', 'user_not_found', 'db_error', etc. |
| `error_message` | TEXT | Error details if failed |
| `retry_count` | INTEGER | Number of retry attempts |
| `last_retry_at` | TIMESTAMPTZ | Last retry timestamp |
| `created_at` | TIMESTAMPTZ | When received |
| `processed_at` | TIMESTAMPTZ | When processing completed |

### RLS Policies

- Only service_role can access (admin views via service key)

### Example Queries

```sql
-- View recent webhooks
SELECT id, event_type, user_email, processed, processing_result, created_at
FROM webhook_logs
ORDER BY created_at DESC
LIMIT 50;

-- Find failed webhooks
SELECT * FROM webhook_logs
WHERE processing_result = 'user_not_found'
ORDER BY created_at DESC;

-- Find webhooks for a specific email
SELECT * FROM webhook_logs
WHERE user_email ILIKE '%example@email.com%'
ORDER BY created_at DESC;
```

---

## 4. email_logs

Track all emails sent to users.

### Columns

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `user_id` | UUID | Foreign key to auth.users |
| `recipient_email` | TEXT | Email address sent to |
| `email_type` | TEXT | Type of email (see types below) |
| `subject` | TEXT | Email subject line |
| `template_data` | JSONB | Variables used in template |
| `status` | TEXT | 'pending', 'sent', 'failed', 'bounced', 'opened' |
| `error_message` | TEXT | Error if failed |
| `provider` | TEXT | 'resend', 'sendgrid', 'manual' |
| `provider_message_id` | TEXT | Provider's message ID |
| `sent_at` | TIMESTAMPTZ | When sent |
| `opened_at` | TIMESTAMPTZ | When opened (if tracked) |
| `created_at` | TIMESTAMPTZ | When created |

### Email Types

- `welcome` - Welcome email after signup
- `trial_expiring_3day` - 3 days before trial ends
- `trial_expiring_1day` - 1 day before trial ends
- `trial_expired` - Trial has ended
- `payment_success` - Payment received
- `payment_failed` - Payment failed
- `subscription_renewed` - Monthly renewal
- `subscription_cancelled` - User cancelled
- `subscription_reactivated` - User resubscribed

### RLS Policies

- Users can view their own email logs
- Only service_role can insert/update

---

## 5. admin_users

Controls access to admin dashboard.

### Columns

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `user_id` | UUID | Foreign key to auth.users (unique) |
| `role` | TEXT | 'admin', 'super_admin', 'support' |
| `permissions` | JSONB | Granular permissions |
| `created_by` | UUID | Who added this admin |
| `notes` | TEXT | Notes about this admin |
| `created_at` | TIMESTAMPTZ | When created |
| `updated_at` | TIMESTAMPTZ | When updated |

### Default Permissions

```json
{
  "view_users": true,
  "view_payments": true,
  "view_webhooks": true,
  "manage_subscriptions": false,
  "manage_admins": false,
  "refund_payments": false
}
```

### Helper Functions

```sql
-- Check if current user is admin
SELECT is_admin();

-- Check if current user is super admin
SELECT is_super_admin();
```

---

## 6. user_profiles

Extended user information beyond auth.users.

### Columns

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `user_id` | UUID | Foreign key to auth.users (unique) |
| `display_name` | TEXT | User's display name |
| `avatar_url` | TEXT | Profile picture URL |
| `device_info` | JSONB | Array of devices used |
| `last_active_at` | TIMESTAMPTZ | Last activity |
| `last_login_at` | TIMESTAMPTZ | Last login |
| `login_count` | INTEGER | Total login count |
| `total_payments` | DECIMAL | Sum of completed payments |
| `lifetime_value` | DECIMAL | Net value (minus refunds) |
| `notes` | TEXT | Admin notes |
| `flags` | JSONB | Flags like 'is_beta_tester', 'is_vip' |
| `preferences` | JSONB | User preferences |
| `created_at` | TIMESTAMPTZ | When created |
| `updated_at` | TIMESTAMPTZ | When updated |

### Auto-Updated Fields

- `total_payments` and `lifetime_value` are automatically updated when payments are recorded via a trigger.

---

## Admin Views

Pre-built views for the admin dashboard.

### admin_user_overview

```sql
SELECT * FROM admin_user_overview
ORDER BY signup_date DESC
LIMIT 100;
```

Returns: user_id, email, signup_date, display_name, last_active_at, total_payments, tier, subscription_status, is_admin

### admin_recent_payments

```sql
SELECT * FROM admin_recent_payments;
```

Returns the 100 most recent payments with user email.

### admin_recent_webhooks

```sql
SELECT * FROM admin_recent_webhooks;
```

Returns the 100 most recent webhook events.

### admin_subscription_metrics

```sql
SELECT * FROM admin_subscription_metrics;
```

Returns:
- active_pro: Count of active Pro subscribers
- active_team: Count of active Team subscribers
- trialing: Count of users in trial
- cancelled: Count of cancelled subscriptions
- past_due: Count of past due subscriptions
- revenue_30_days: Revenue in last 30 days
- revenue_total: Total revenue

---

## Migration

To apply the database schema:

1. Go to Supabase SQL Editor: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql

2. Copy the contents of `supabase/migrations/002_professional_subscription.sql`

3. Click "Run"

4. Verify tables exist:
```sql
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

5. Add yourself as super_admin:
```sql
INSERT INTO admin_users (user_id, role)
SELECT id, 'super_admin'
FROM auth.users
WHERE email = 'your-email@example.com';
```

---

## Troubleshooting

### User subscription not updating

1. Check webhook_logs for the event:
```sql
SELECT * FROM webhook_logs
WHERE user_email ILIKE '%user@email.com%'
ORDER BY created_at DESC;
```

2. Check if processing_result shows an error

3. Check if user exists in auth.users:
```sql
SELECT * FROM auth.users
WHERE email = 'user@email.com';
```

### Payment not recorded

1. Check webhook_logs for PAYMENT.SALE.COMPLETED event
2. Check payment_history for the user:
```sql
SELECT * FROM payment_history
WHERE user_id = 'user-uuid'
ORDER BY created_at DESC;
```

### Manual subscription activation

See ADMIN_OPERATIONS.md for instructions on manually activating subscriptions.
