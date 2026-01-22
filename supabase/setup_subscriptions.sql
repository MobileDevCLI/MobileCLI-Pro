-- MobileCLI Pro - Subscriptions Table Setup
-- Run this in Supabase SQL Editor: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql
--
-- PRIVACY-FOCUSED: Only stores minimal data needed for license verification
-- NO personal data, NO payment details, NO sensitive information

-- ============================================================================
-- 1. CREATE SUBSCRIPTIONS TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,

    -- Link to auth.users (the only identifier we need)
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,

    -- Subscription status: 'active', 'cancelled', 'expired', 'trial'
    status TEXT NOT NULL DEFAULT 'trial',

    -- PayPal subscription ID (for cancellation/management, NOT payment details)
    paypal_subscription_id TEXT,

    -- Dates
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    expires_at TIMESTAMPTZ,

    -- Ensure one subscription per user
    CONSTRAINT unique_user_subscription UNIQUE (user_id)
);

-- ============================================================================
-- 2. ROW LEVEL SECURITY (RLS) - Users can only see their own subscription
-- ============================================================================

-- Enable RLS
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only SELECT their own subscription
CREATE POLICY "Users can view own subscription"
    ON public.subscriptions
    FOR SELECT
    USING (auth.uid() = user_id);

-- Policy: Users can INSERT their own subscription (for trial creation)
CREATE POLICY "Users can create own subscription"
    ON public.subscriptions
    FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Policy: Only service role can UPDATE subscriptions (for webhook)
-- Users cannot modify their own subscription status
CREATE POLICY "Service role can update subscriptions"
    ON public.subscriptions
    FOR UPDATE
    USING (auth.role() = 'service_role');

-- ============================================================================
-- 3. AUTO-UPDATE updated_at TIMESTAMP
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_subscriptions_updated_at
    BEFORE UPDATE ON public.subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 4. FUNCTION: Create trial subscription for new users
-- ============================================================================

CREATE OR REPLACE FUNCTION create_trial_subscription()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.subscriptions (user_id, status, expires_at)
    VALUES (NEW.id, 'trial', NOW() + INTERVAL '7 days')
    ON CONFLICT (user_id) DO NOTHING;
    RETURN NEW;
END;
$$ language 'plpgsql' SECURITY DEFINER;

-- Trigger: Auto-create trial when user signs up
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION create_trial_subscription();

-- ============================================================================
-- 5. FUNCTION: Check if subscription is valid (for app to call)
-- ============================================================================

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
$$ language 'plpgsql' SECURITY DEFINER;

-- ============================================================================
-- 6. INDEX FOR PERFORMANCE
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON public.subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON public.subscriptions(status);

-- ============================================================================
-- 7. GRANT PERMISSIONS
-- ============================================================================

-- Allow authenticated users to use the function
GRANT EXECUTE ON FUNCTION is_subscription_valid(UUID) TO authenticated;

-- ============================================================================
-- VERIFICATION QUERIES (run after setup to verify)
-- ============================================================================

-- Check table exists:
-- SELECT * FROM public.subscriptions LIMIT 1;

-- Check RLS is enabled:
-- SELECT tablename, rowsecurity FROM pg_tables WHERE tablename = 'subscriptions';

-- Test the function (replace with actual user_id):
-- SELECT is_subscription_valid('your-user-uuid-here');
