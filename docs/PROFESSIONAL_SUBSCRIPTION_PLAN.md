# MobileCLI Pro - Professional Subscription System Plan

**Created:** 2026-01-23
**Status:** Planning Phase
**Goal:** Industry-standard subscription management with admin control, user notifications, and reliability

---

## Phase 1: Database Foundation (Backend)

### New Tables Required

```sql
-- 1. Payment History - Track every payment
CREATE TABLE payment_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    amount DECIMAL(10,2) NOT NULL,
    currency TEXT DEFAULT 'USD',
    payment_type TEXT NOT NULL, -- 'subscription', 'one-time'
    paypal_transaction_id TEXT,
    paypal_subscription_id TEXT,
    status TEXT NOT NULL, -- 'completed', 'refunded', 'failed'
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Webhook Logs - Audit trail for all PayPal events
CREATE TABLE webhook_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type TEXT NOT NULL,
    event_id TEXT, -- PayPal event ID
    payload JSONB NOT NULL,
    user_id UUID REFERENCES auth.users(id),
    processed BOOLEAN DEFAULT false,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Email Logs - Track all emails sent
CREATE TABLE email_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id),
    email_type TEXT NOT NULL, -- 'welcome', 'payment_confirm', 'trial_expiring', etc.
    recipient_email TEXT NOT NULL,
    subject TEXT NOT NULL,
    sent_at TIMESTAMPTZ DEFAULT NOW(),
    status TEXT DEFAULT 'sent' -- 'sent', 'failed', 'bounced'
);

-- 4. Admin Users - Who can access admin dashboard
CREATE TABLE admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE UNIQUE,
    role TEXT DEFAULT 'admin', -- 'admin', 'super_admin'
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. User Profiles - Extended user info
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE UNIQUE,
    display_name TEXT,
    device_count INTEGER DEFAULT 0,
    last_active_at TIMESTAMPTZ,
    notes TEXT, -- Admin notes
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### Update Existing Subscriptions Table

```sql
-- Add fields to track subscription lifecycle
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS
    trial_started_at TIMESTAMPTZ,
    trial_reminded_at TIMESTAMPTZ, -- When we sent trial expiring email
    payment_failed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancel_reason TEXT,
    admin_notes TEXT;
```

---

## Phase 2: Email System

### Email Types Required

| Email Type | Trigger | Content |
|------------|---------|---------|
| `welcome` | User signs up | Welcome, how to use app, trial info |
| `trial_expiring_3day` | 3 days before trial ends | Reminder to subscribe |
| `trial_expiring_1day` | 1 day before trial ends | Last chance reminder |
| `trial_expired` | Trial ends | Trial over, subscribe to continue |
| `payment_success` | PayPal payment confirmed | Receipt, thank you |
| `payment_failed` | PayPal payment fails | Update payment method |
| `subscription_renewed` | Monthly renewal | Confirmation, next billing date |
| `subscription_cancelled` | User cancels | Confirmation, access until date |
| `subscription_reactivated` | User resubscribes | Welcome back |

### Implementation Options

**Option A: Supabase Edge Functions + Resend**
- Resend.com for email delivery (free tier: 3000/month)
- Edge function triggered by database changes
- Professional templates

**Option B: Supabase Edge Functions + SMTP**
- Use your own SMTP (Gmail, SendGrid, etc.)
- More control, potentially free

**Recommended: Option A (Resend)** - Most reliable, good free tier

### Email Edge Function Structure

```
supabase/functions/
├── send-email/
│   └── index.ts          # Generic email sender
├── scheduled-emails/
│   └── index.ts          # Cron job for trial reminders
└── templates/
    ├── welcome.html
    ├── payment-success.html
    ├── trial-expiring.html
    └── ...
```

---

## Phase 3: Webhook Improvements

### Current Problems
1. No signature verification (anyone could fake a webhook)
2. No logging (can't debug issues)
3. No retry logic (if processing fails, payment is lost)

### Fixes Required

```typescript
// 1. Verify PayPal webhook signature
async function verifyPayPalSignature(req: Request, body: string): Promise<boolean> {
    const transmissionId = req.headers.get('paypal-transmission-id');
    const timestamp = req.headers.get('paypal-transmission-time');
    const signature = req.headers.get('paypal-transmission-sig');
    const certUrl = req.headers.get('paypal-cert-url');
    const webhookId = Deno.env.get('PAYPAL_WEBHOOK_ID');

    // Call PayPal API to verify
    const verifyUrl = 'https://api-m.paypal.com/v1/notifications/verify-webhook-signature';
    // ... implementation
}

// 2. Log every webhook
async function logWebhook(supabase, eventType, payload, userId, processed, error) {
    await supabase.from('webhook_logs').insert({
        event_type: eventType,
        event_id: payload.id,
        payload: payload,
        user_id: userId,
        processed: processed,
        error_message: error
    });
}

// 3. Retry failed processing
// Use Supabase scheduled function to retry failed webhooks
```

---

## Phase 4: Admin Dashboard

### Features Required

1. **User List**
   - Search by email
   - Filter by status (active, trial, expired, cancelled)
   - Sort by signup date, last active
   - Export to CSV

2. **User Detail View**
   - Email, signup date, last active
   - Subscription status and history
   - Payment history
   - Device count
   - Admin notes
   - Actions: Activate, Deactivate, Extend trial, Refund

3. **Subscription Management**
   - Manually activate subscription
   - Extend subscription
   - Cancel subscription
   - Add admin notes

4. **Analytics Dashboard**
   - Total users, active subscribers, trial users
   - Signups this week/month
   - Conversion rate (trial to paid)
   - Churn rate
   - Revenue (MRR)

5. **Webhook Logs**
   - View all PayPal events
   - See failed processing
   - Retry failed webhooks

### Implementation Options

**Option A: Web Dashboard (React/Next.js)**
- Host on Vercel or your website
- Full-featured, professional
- More work to build

**Option B: Supabase Studio Custom Claims**
- Use Supabase's built-in dashboard
- Add RLS policies for admin access
- Quick but limited UI

**Option C: Admin Mode in Android App**
- Add admin screens to MobileCLI
- Only visible if user is admin
- Access from anywhere on phone

**Recommended: Option A + C**
- Web dashboard for full management
- Simple admin view in app for quick checks

### Web Dashboard Tech Stack
- Next.js 14 (React framework)
- Tailwind CSS (styling)
- Supabase JS client
- Deploy to Vercel (free tier)
- Protected by Supabase Auth (admin only)

---

## Phase 5: App Improvements

### In-App Features to Add

1. **Subscription Status Screen**
   - Current plan (Trial/Pro)
   - Expiry date
   - Payment history
   - Download receipts

2. **Better Error Handling**
   - Offline grace period (7 days)
   - Clear error messages
   - Retry buttons

3. **Push Notifications** (Optional)
   - Trial expiring
   - Payment failed
   - New features

---

## Implementation Order

### Week 1: Database & Backend
- [ ] Create new database tables
- [ ] Update subscriptions table
- [ ] Add database indexes and RLS policies
- [ ] Test migrations

### Week 2: Webhook Improvements
- [ ] Add PayPal signature verification
- [ ] Implement webhook logging
- [ ] Add retry logic for failed webhooks
- [ ] Test with PayPal sandbox

### Week 3: Email System
- [ ] Set up Resend account
- [ ] Create email templates
- [ ] Build send-email edge function
- [ ] Build scheduled-emails function (trial reminders)
- [ ] Test all email flows

### Week 4: Admin Dashboard (Web)
- [ ] Create Next.js project
- [ ] Build user list page
- [ ] Build user detail page
- [ ] Build analytics dashboard
- [ ] Deploy to Vercel
- [ ] Add your email as admin

### Week 5: App Updates
- [ ] Add subscription history screen
- [ ] Add offline grace period
- [ ] Improve error messages
- [ ] Test full flow

### Week 6: Testing & Polish
- [ ] End-to-end testing
- [ ] Security review
- [ ] Documentation
- [ ] Release

---

## Cost Estimates

| Service | Free Tier | If Exceeded |
|---------|-----------|-------------|
| Supabase | 500MB DB, 2GB bandwidth, 500K edge invocations | $25/mo Pro |
| Resend | 3,000 emails/month | $20/mo for 50K |
| Vercel | 100GB bandwidth, unlimited deploys | $20/mo Pro |
| **Total** | **$0/month** | ~$65/month at scale |

---

## Security Checklist

- [ ] PayPal webhook signature verification
- [ ] Admin dashboard requires auth
- [ ] RLS on all tables
- [ ] No sensitive data in logs
- [ ] HTTPS everywhere
- [ ] Rate limiting on APIs
- [ ] Audit logging for admin actions

---

## Files to Create/Modify

### New Files
```
supabase/
├── migrations/
│   └── 002_professional_subscription.sql
├── functions/
│   ├── send-email/index.ts
│   ├── scheduled-emails/index.ts
│   └── admin-api/index.ts

admin-dashboard/           # New Next.js project
├── app/
│   ├── page.tsx          # Dashboard home
│   ├── users/
│   │   ├── page.tsx      # User list
│   │   └── [id]/page.tsx # User detail
│   ├── analytics/
│   │   └── page.tsx
│   └── webhooks/
│       └── page.tsx
├── components/
└── lib/
    └── supabase.ts

app/src/main/java/com/termux/auth/
├── SubscriptionHistoryActivity.kt  # New
└── (existing files updated)
```

### Modified Files
```
supabase/functions/paypal-webhook/index.ts  # Add verification, logging
app/src/main/java/com/termux/auth/AccountActivity.kt  # Add history link
```

---

## Next Steps

1. Review this plan
2. Decide on any changes
3. Start with Phase 1 (Database)

Ready to proceed?
