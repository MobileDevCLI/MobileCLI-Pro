-- ============================================
-- MobileCLI Professional Subscription System
-- Migration: 002_professional_subscription.sql
-- ============================================
--
-- This migration adds:
-- 1. Enhanced columns to subscriptions table
-- 2. payment_history table (track every payment)
-- 3. webhook_logs table (audit trail)
-- 4. email_logs table (track notifications)
-- 5. admin_users table (dashboard access)
-- 6. user_profiles table (extended user info)
--
-- SAFE TO RUN MULTIPLE TIMES (idempotent with IF NOT EXISTS)
-- ============================================


-- ============================================
-- 1. ENHANCED SUBSCRIPTIONS TABLE
-- Add new columns for professional tracking
-- ============================================

-- Trial tracking
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS trial_started_at TIMESTAMPTZ;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS trial_reminder_sent BOOLEAN DEFAULT false;

-- Payment tracking
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS payment_failed_at TIMESTAMPTZ;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS last_payment_at TIMESTAMPTZ;

-- Cancellation tracking
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS cancel_reason TEXT;

-- Admin notes
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS admin_notes TEXT;

-- PayPal fields (if migrating from Stripe or supporting both)
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS paypal_subscription_id TEXT;
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS paypal_payer_id TEXT;

-- Add index for PayPal lookups
CREATE INDEX IF NOT EXISTS idx_subscriptions_paypal_sub ON subscriptions(paypal_subscription_id);


-- ============================================
-- 2. PAYMENT HISTORY TABLE
-- Track every payment for financial records
-- ============================================

CREATE TABLE IF NOT EXISTS payment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,

    -- Amount info
    amount DECIMAL(10,2) NOT NULL,
    currency TEXT DEFAULT 'USD',

    -- Payment type
    payment_type TEXT NOT NULL,  -- 'subscription_initial', 'subscription_renewal', 'one_time', 'refund'

    -- Payment provider info (supports Stripe and PayPal)
    provider TEXT DEFAULT 'paypal',  -- 'paypal', 'stripe'
    paypal_transaction_id TEXT,
    paypal_subscription_id TEXT,
    stripe_payment_intent_id TEXT,
    stripe_invoice_id TEXT,

    -- Status
    status TEXT NOT NULL DEFAULT 'completed',  -- 'completed', 'pending', 'failed', 'refunded'

    -- Details
    description TEXT,
    metadata JSONB DEFAULT '{}',

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    -- Constraints
    CONSTRAINT valid_payment_status CHECK (status IN ('completed', 'pending', 'failed', 'refunded')),
    CONSTRAINT valid_payment_type CHECK (payment_type IN ('subscription_initial', 'subscription_renewal', 'one_time', 'refund')),
    CONSTRAINT valid_provider CHECK (provider IN ('paypal', 'stripe', 'manual'))
);

-- Indexes for payment_history
CREATE INDEX IF NOT EXISTS idx_payment_history_user ON payment_history(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_history_created ON payment_history(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_history_paypal_txn ON payment_history(paypal_transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_history_status ON payment_history(status);

-- RLS for payment_history
ALTER TABLE payment_history ENABLE ROW LEVEL SECURITY;

-- Users can view their own payment history
DROP POLICY IF EXISTS "Users can view own payments" ON payment_history;
CREATE POLICY "Users can view own payments" ON payment_history
    FOR SELECT USING (auth.uid() = user_id);

-- Only service_role can insert (from webhooks)
-- No INSERT policy = only service_role can insert


-- ============================================
-- 3. WEBHOOK LOGS TABLE
-- Complete audit trail of all webhook events
-- ============================================

CREATE TABLE IF NOT EXISTS webhook_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Event info
    event_type TEXT NOT NULL,
    event_id TEXT,  -- Provider's event ID (PayPal/Stripe)
    provider TEXT DEFAULT 'paypal',  -- 'paypal', 'stripe'

    -- Full payload for debugging
    payload JSONB NOT NULL,

    -- User tracking (even if user deleted)
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    user_email TEXT,  -- Store email for matching even if user deleted

    -- Processing status
    processed BOOLEAN DEFAULT false,
    processing_result TEXT,  -- 'success', 'user_not_found', 'db_error', 'invalid_signature', etc
    error_message TEXT,

    -- Retry info
    retry_count INTEGER DEFAULT 0,
    last_retry_at TIMESTAMPTZ,

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    processed_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT valid_processing_result CHECK (
        processing_result IS NULL OR
        processing_result IN ('success', 'user_not_found', 'db_error', 'invalid_signature', 'duplicate', 'skipped')
    )
);

-- Indexes for webhook_logs
CREATE INDEX IF NOT EXISTS idx_webhook_logs_created ON webhook_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_event_type ON webhook_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_event_id ON webhook_logs(event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_processed ON webhook_logs(processed);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_user ON webhook_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_user_email ON webhook_logs(user_email);

-- RLS for webhook_logs
ALTER TABLE webhook_logs ENABLE ROW LEVEL SECURITY;

-- Only service_role can access (no policies = service_role only)
-- Admins will access via admin dashboard with service key


-- ============================================
-- 4. EMAIL LOGS TABLE
-- Track all email notifications sent
-- ============================================

CREATE TABLE IF NOT EXISTS email_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Recipient info
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    recipient_email TEXT NOT NULL,

    -- Email details
    email_type TEXT NOT NULL,
    subject TEXT NOT NULL,
    template_data JSONB DEFAULT '{}',  -- Variables used in template

    -- Status
    status TEXT DEFAULT 'pending',  -- 'pending', 'sent', 'failed', 'bounced', 'opened'
    error_message TEXT,

    -- Provider info
    provider TEXT DEFAULT 'resend',  -- 'resend', 'sendgrid', 'manual'
    provider_message_id TEXT,

    -- Timestamps
    sent_at TIMESTAMPTZ,
    opened_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    -- Constraints
    CONSTRAINT valid_email_status CHECK (status IN ('pending', 'sent', 'failed', 'bounced', 'opened')),
    CONSTRAINT valid_email_type CHECK (email_type IN (
        'welcome',
        'trial_expiring_3day',
        'trial_expiring_1day',
        'trial_expired',
        'payment_success',
        'payment_failed',
        'subscription_renewed',
        'subscription_cancelled',
        'subscription_reactivated',
        'admin_notification',
        'password_reset',
        'email_verification'
    ))
);

-- Indexes for email_logs
CREATE INDEX IF NOT EXISTS idx_email_logs_user ON email_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_email_logs_recipient ON email_logs(recipient_email);
CREATE INDEX IF NOT EXISTS idx_email_logs_type ON email_logs(email_type);
CREATE INDEX IF NOT EXISTS idx_email_logs_status ON email_logs(status);
CREATE INDEX IF NOT EXISTS idx_email_logs_created ON email_logs(created_at DESC);

-- RLS for email_logs
ALTER TABLE email_logs ENABLE ROW LEVEL SECURITY;

-- Users can view their own email logs
DROP POLICY IF EXISTS "Users can view own emails" ON email_logs;
CREATE POLICY "Users can view own emails" ON email_logs
    FOR SELECT USING (auth.uid() = user_id);


-- ============================================
-- 5. ADMIN USERS TABLE
-- Dashboard access control
-- ============================================

CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL UNIQUE,

    -- Role
    role TEXT DEFAULT 'admin',  -- 'admin', 'super_admin', 'support'

    -- Granular permissions (optional)
    permissions JSONB DEFAULT '{
        "view_users": true,
        "view_payments": true,
        "view_webhooks": true,
        "manage_subscriptions": false,
        "manage_admins": false,
        "refund_payments": false
    }',

    -- Audit
    created_by UUID REFERENCES auth.users(id),
    notes TEXT,

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,

    -- Constraints
    CONSTRAINT valid_admin_role CHECK (role IN ('admin', 'super_admin', 'support'))
);

-- Index for quick lookup
CREATE INDEX IF NOT EXISTS idx_admin_users_user ON admin_users(user_id);

-- RLS for admin_users
ALTER TABLE admin_users ENABLE ROW LEVEL SECURITY;

-- Users can check if they are admin
DROP POLICY IF EXISTS "Users can check own admin status" ON admin_users;
CREATE POLICY "Users can check own admin status" ON admin_users
    FOR SELECT USING (auth.uid() = user_id);

-- Service role handles all other operations


-- ============================================
-- 6. USER PROFILES TABLE (Extended)
-- Additional user information beyond auth.users
-- ============================================

CREATE TABLE IF NOT EXISTS user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL UNIQUE,

    -- Display info
    display_name TEXT,
    avatar_url TEXT,

    -- Device tracking
    device_info JSONB DEFAULT '[]',  -- Array of devices logged in from

    -- Activity tracking
    last_active_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    login_count INTEGER DEFAULT 0,

    -- Financial summary (updated by triggers)
    total_payments DECIMAL(10,2) DEFAULT 0,
    lifetime_value DECIMAL(10,2) DEFAULT 0,

    -- Admin fields
    notes TEXT,  -- Admin notes about user
    flags JSONB DEFAULT '{}',  -- 'is_beta_tester', 'is_vip', 'is_flagged', etc

    -- Preferences
    preferences JSONB DEFAULT '{
        "email_notifications": true,
        "marketing_emails": false,
        "timezone": "UTC"
    }',

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_user_profiles_user ON user_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_last_active ON user_profiles(last_active_at DESC);

-- RLS for user_profiles
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;

-- Users can view and update their own profile
DROP POLICY IF EXISTS "Users can view own profile" ON user_profiles;
CREATE POLICY "Users can view own profile" ON user_profiles
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own profile" ON user_profiles;
CREATE POLICY "Users can update own profile" ON user_profiles
    FOR UPDATE USING (auth.uid() = user_id);


-- ============================================
-- 7. HELPER FUNCTIONS
-- ============================================

-- Check if user is admin
CREATE OR REPLACE FUNCTION is_admin(check_user_id UUID DEFAULT auth.uid())
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM admin_users
        WHERE user_id = check_user_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- Check if user is super admin
CREATE OR REPLACE FUNCTION is_super_admin(check_user_id UUID DEFAULT auth.uid())
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM admin_users
        WHERE user_id = check_user_id
        AND role = 'super_admin'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- Get user's subscription status
CREATE OR REPLACE FUNCTION get_subscription_status(check_user_id UUID)
RETURNS TABLE(
    tier TEXT,
    status TEXT,
    is_trial BOOLEAN,
    days_remaining INTEGER,
    expires_at TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        s.tier,
        s.status,
        (s.status = 'trialing') AS is_trial,
        GREATEST(0, EXTRACT(DAY FROM s.current_period_end - NOW())::INTEGER) AS days_remaining,
        s.current_period_end AS expires_at
    FROM subscriptions s
    WHERE s.user_id = check_user_id
    AND s.status IN ('active', 'trialing', 'past_due')
    ORDER BY s.created_at DESC
    LIMIT 1;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- Update user profile totals after payment
CREATE OR REPLACE FUNCTION update_user_payment_totals()
RETURNS TRIGGER AS $$
BEGIN
    -- Update total payments for user
    UPDATE user_profiles
    SET
        total_payments = (
            SELECT COALESCE(SUM(amount), 0)
            FROM payment_history
            WHERE user_id = NEW.user_id
            AND status = 'completed'
        ),
        lifetime_value = (
            SELECT COALESCE(SUM(
                CASE
                    WHEN status = 'completed' THEN amount
                    WHEN status = 'refunded' THEN -amount
                    ELSE 0
                END
            ), 0)
            FROM payment_history
            WHERE user_id = NEW.user_id
        ),
        updated_at = NOW()
    WHERE user_id = NEW.user_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create trigger for payment totals
DROP TRIGGER IF EXISTS update_payment_totals_trigger ON payment_history;
CREATE TRIGGER update_payment_totals_trigger
    AFTER INSERT OR UPDATE ON payment_history
    FOR EACH ROW
    EXECUTE FUNCTION update_user_payment_totals();


-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to tables that have updated_at
DROP TRIGGER IF EXISTS update_admin_users_updated_at ON admin_users;
CREATE TRIGGER update_admin_users_updated_at
    BEFORE UPDATE ON admin_users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_user_profiles_updated_at ON user_profiles;
CREATE TRIGGER update_user_profiles_updated_at
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();


-- Create user_profile on signup (if not using existing profiles table)
CREATE OR REPLACE FUNCTION handle_new_user_profile()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO user_profiles (user_id, display_name, created_at)
    VALUES (
        NEW.id,
        COALESCE(
            NEW.raw_user_meta_data->>'name',
            NEW.raw_user_meta_data->>'full_name',
            split_part(NEW.email, '@', 1)
        ),
        NOW()
    )
    ON CONFLICT (user_id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger for auto-creating user_profile
DROP TRIGGER IF EXISTS on_auth_user_created_profile ON auth.users;
CREATE TRIGGER on_auth_user_created_profile
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION handle_new_user_profile();


-- ============================================
-- 8. ADMIN DASHBOARD VIEWS
-- Read-only views for admin dashboard
-- ============================================

-- User overview for admins
CREATE OR REPLACE VIEW admin_user_overview AS
SELECT
    u.id AS user_id,
    u.email,
    u.created_at AS signup_date,
    up.display_name,
    up.last_active_at,
    up.total_payments,
    up.flags,
    s.tier,
    s.status AS subscription_status,
    s.current_period_end AS subscription_ends,
    s.trial_started_at,
    (SELECT COUNT(*) FROM payment_history WHERE user_id = u.id AND status = 'completed') AS payment_count,
    CASE
        WHEN a.id IS NOT NULL THEN true
        ELSE false
    END AS is_admin
FROM auth.users u
LEFT JOIN user_profiles up ON up.user_id = u.id
LEFT JOIN subscriptions s ON s.user_id = u.id AND s.status IN ('active', 'trialing', 'past_due')
LEFT JOIN admin_users a ON a.user_id = u.id;


-- Recent payments for admins
CREATE OR REPLACE VIEW admin_recent_payments AS
SELECT
    p.id,
    p.user_id,
    u.email,
    p.amount,
    p.currency,
    p.payment_type,
    p.provider,
    p.status,
    p.paypal_transaction_id,
    p.created_at
FROM payment_history p
LEFT JOIN auth.users u ON u.id = p.user_id
ORDER BY p.created_at DESC
LIMIT 100;


-- Recent webhook events for admins
CREATE OR REPLACE VIEW admin_recent_webhooks AS
SELECT
    id,
    event_type,
    event_id,
    provider,
    user_email,
    processed,
    processing_result,
    error_message,
    retry_count,
    created_at,
    processed_at
FROM webhook_logs
ORDER BY created_at DESC
LIMIT 100;


-- Subscription metrics
CREATE OR REPLACE VIEW admin_subscription_metrics AS
SELECT
    (SELECT COUNT(*) FROM subscriptions WHERE status = 'active' AND tier = 'pro') AS active_pro,
    (SELECT COUNT(*) FROM subscriptions WHERE status = 'active' AND tier = 'team') AS active_team,
    (SELECT COUNT(*) FROM subscriptions WHERE status = 'trialing') AS trialing,
    (SELECT COUNT(*) FROM subscriptions WHERE status = 'cancelled') AS cancelled,
    (SELECT COUNT(*) FROM subscriptions WHERE status = 'past_due') AS past_due,
    (SELECT COALESCE(SUM(amount), 0) FROM payment_history WHERE status = 'completed' AND created_at > NOW() - INTERVAL '30 days') AS revenue_30_days,
    (SELECT COALESCE(SUM(amount), 0) FROM payment_history WHERE status = 'completed') AS revenue_total;


-- ============================================
-- 9. WEBHOOK PROCESSING FUNCTION
-- Used by PayPal webhook Edge Function
-- ============================================

CREATE OR REPLACE FUNCTION process_paypal_webhook(
    p_event_type TEXT,
    p_event_id TEXT,
    p_payload JSONB,
    p_user_email TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID;
    v_log_id UUID;
    v_result TEXT := 'success';
    v_error TEXT;
BEGIN
    -- Create webhook log entry first
    INSERT INTO webhook_logs (event_type, event_id, provider, payload, user_email)
    VALUES (p_event_type, p_event_id, 'paypal', p_payload, p_user_email)
    RETURNING id INTO v_log_id;

    -- Try to find user by email
    IF p_user_email IS NOT NULL THEN
        SELECT id INTO v_user_id
        FROM auth.users
        WHERE email = p_user_email
        LIMIT 1;

        IF v_user_id IS NULL THEN
            v_result := 'user_not_found';
        END IF;
    END IF;

    -- Update log with user_id if found
    UPDATE webhook_logs
    SET
        user_id = v_user_id,
        processed = true,
        processing_result = v_result,
        processed_at = NOW()
    WHERE id = v_log_id;

    RETURN jsonb_build_object(
        'success', true,
        'log_id', v_log_id,
        'user_id', v_user_id,
        'result', v_result
    );

EXCEPTION WHEN OTHERS THEN
    -- Log the error
    UPDATE webhook_logs
    SET
        processed = true,
        processing_result = 'db_error',
        error_message = SQLERRM,
        processed_at = NOW()
    WHERE id = v_log_id;

    RETURN jsonb_build_object(
        'success', false,
        'error', SQLERRM
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- Record a payment
CREATE OR REPLACE FUNCTION record_payment(
    p_user_id UUID,
    p_amount DECIMAL,
    p_currency TEXT,
    p_payment_type TEXT,
    p_provider TEXT,
    p_transaction_id TEXT,
    p_subscription_id TEXT DEFAULT NULL,
    p_status TEXT DEFAULT 'completed',
    p_description TEXT DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    v_payment_id UUID;
BEGIN
    INSERT INTO payment_history (
        user_id,
        amount,
        currency,
        payment_type,
        provider,
        paypal_transaction_id,
        paypal_subscription_id,
        status,
        description
    )
    VALUES (
        p_user_id,
        p_amount,
        p_currency,
        p_payment_type,
        p_provider,
        p_transaction_id,
        p_subscription_id,
        p_status,
        p_description
    )
    RETURNING id INTO v_payment_id;

    -- Update subscription's last_payment_at
    IF p_subscription_id IS NOT NULL THEN
        UPDATE subscriptions
        SET last_payment_at = NOW()
        WHERE paypal_subscription_id = p_subscription_id
        OR stripe_subscription_id = p_subscription_id;
    END IF;

    RETURN v_payment_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ============================================
-- DONE!
-- ============================================
--
-- Next steps:
-- 1. Run this migration in Supabase SQL Editor
-- 2. Deploy the PayPal webhook Edge Function
-- 3. Add yourself as super_admin:
--    INSERT INTO admin_users (user_id, role)
--    SELECT id, 'super_admin' FROM auth.users WHERE email = 'your@email.com';
--
-- ============================================
