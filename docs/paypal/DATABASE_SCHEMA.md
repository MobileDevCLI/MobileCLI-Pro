# PayPal Database Schema

**All SQL needed to set up the database for PayPal subscriptions.**

---

## Quick Setup

Run this in Supabase SQL Editor for minimal setup:

```sql
-- Minimal subscriptions table for PayPal
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    status TEXT NOT NULL DEFAULT 'trial',
    paypal_subscription_id TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    expires_at TIMESTAMPTZ,
    CONSTRAINT unique_user_subscription UNIQUE (user_id)
);

-- Enable RLS
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;

-- Users can only view their own subscription
CREATE POLICY "Users can view own subscription"
    ON public.subscriptions FOR SELECT
    USING (auth.uid() = user_id);

-- Index for performance
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id
    ON public.subscriptions(user_id);
```

---

## Full Schema (Professional)

For a complete system with payment history, webhook logs, and admin features:

### 1. Subscriptions Table (Enhanced)

```sql
-- Add extra columns for professional tracking
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS tier TEXT DEFAULT 'pro';
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS current_period_end TIMESTAMPTZ;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS trial_started_at TIMESTAMPTZ;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS trial_reminder_sent BOOLEAN DEFAULT false;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS payment_failed_at TIMESTAMPTZ;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS last_payment_at TIMESTAMPTZ;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS cancel_reason TEXT;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS admin_notes TEXT;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS paypal_payer_id TEXT;

-- Index for PayPal lookups
CREATE INDEX IF NOT EXISTS idx_subscriptions_paypal_sub
    ON subscriptions(paypal_subscription_id);
```

### 2. Payment History Table

```sql
CREATE TABLE IF NOT EXISTS payment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency TEXT DEFAULT 'USD',
    payment_type TEXT NOT NULL,  -- 'subscription_initial', 'subscription_renewal', 'one_time', 'refund'
    provider TEXT DEFAULT 'paypal',  -- 'paypal', 'stripe'
    paypal_transaction_id TEXT,
    paypal_subscription_id TEXT,
    status TEXT NOT NULL DEFAULT 'completed',  -- 'completed', 'pending', 'failed', 'refunded'
    description TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    CONSTRAINT valid_payment_status CHECK (status IN ('completed', 'pending', 'failed', 'refunded')),
    CONSTRAINT valid_payment_type CHECK (payment_type IN ('subscription_initial', 'subscription_renewal', 'one_time', 'refund')),
    CONSTRAINT valid_provider CHECK (provider IN ('paypal', 'stripe', 'manual'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_payment_history_user ON payment_history(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_history_created ON payment_history(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_history_paypal_txn ON payment_history(paypal_transaction_id);

-- RLS
ALTER TABLE payment_history ENABLE ROW LEVEL SECURITY;

-- Users can view their own payments
CREATE POLICY "Users can view own payments" ON payment_history
    FOR SELECT USING (auth.uid() = user_id);
```

### 3. Webhook Logs Table

```sql
CREATE TABLE IF NOT EXISTS webhook_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type TEXT NOT NULL,
    event_id TEXT,
    provider TEXT DEFAULT 'paypal',
    payload JSONB NOT NULL,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    user_email TEXT,
    processed BOOLEAN DEFAULT false,
    processing_result TEXT,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    last_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    processed_at TIMESTAMPTZ,

    CONSTRAINT valid_processing_result CHECK (
        processing_result IS NULL OR
        processing_result IN ('success', 'user_not_found', 'db_error', 'invalid_signature', 'duplicate', 'skipped')
    )
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_webhook_logs_created ON webhook_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_event_type ON webhook_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_processed ON webhook_logs(processed);

-- RLS (admin only via service role)
ALTER TABLE webhook_logs ENABLE ROW LEVEL SECURITY;
```

---

## Helper Functions

### Check if Subscription is Valid

```sql
CREATE OR REPLACE FUNCTION is_subscription_valid(check_user_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    sub_status TEXT;
    sub_expires TIMESTAMPTZ;
BEGIN
    SELECT status, expires_at
    INTO sub_status, sub_expires
    FROM public.subscriptions
    WHERE user_id = check_user_id;

    -- No subscription found
    IF sub_status IS NULL THEN
        RETURN FALSE;
    END IF;

    -- Active subscription (paid)
    IF sub_status = 'active' THEN
        RETURN TRUE;
    END IF;

    -- Trial - check if not expired
    IF sub_status = 'trial' AND sub_expires > NOW() THEN
        RETURN TRUE;
    END IF;

    -- Cancelled or expired
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant to authenticated users
GRANT EXECUTE ON FUNCTION is_subscription_valid(UUID) TO authenticated;
```

### Auto-create Trial on Signup

```sql
CREATE OR REPLACE FUNCTION create_trial_subscription()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.subscriptions (user_id, status, expires_at)
    VALUES (NEW.id, 'trial', NOW() + INTERVAL '7 days')
    ON CONFLICT (user_id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger on new user signup
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION create_trial_subscription();
```

### Auto-update updated_at

```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_subscriptions_updated_at
    BEFORE UPDATE ON public.subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

---

## Important Constraints

### UNIQUE on user_id

**This is REQUIRED for UPSERT to work:**

```sql
CONSTRAINT unique_user_subscription UNIQUE (user_id)
```

Without this, `upsert({ onConflict: "user_id" })` will fail.

---

## RLS Policies Summary

| Table | Policy | Access |
|-------|--------|--------|
| `subscriptions` | SELECT | Own rows only |
| `payment_history` | SELECT | Own rows only |
| `webhook_logs` | None | Service role only |

**Service Role:** Used by webhook to bypass RLS and update any user's subscription.

---

## Migration Files

The full migration is at:
```
supabase/migrations/002_professional_subscription.sql
```

Simpler version:
```
supabase/setup_subscriptions.sql
```

---

## Verification Queries

### Check Table Exists
```sql
SELECT * FROM public.subscriptions LIMIT 1;
```

### Check RLS is Enabled
```sql
SELECT tablename, rowsecurity
FROM pg_tables
WHERE tablename = 'subscriptions';
```

### Check User's Subscription
```sql
SELECT * FROM subscriptions
WHERE user_id = 'USER-UUID-HERE';
```

### Check UNIQUE Constraint Exists
```sql
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'subscriptions'
AND constraint_type = 'UNIQUE';
```

---

*Last Updated: January 25, 2026*
