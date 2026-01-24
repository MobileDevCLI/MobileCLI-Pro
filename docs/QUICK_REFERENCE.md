# MobileCLI Pro - Quick Reference

**One-page cheat sheet for operating the subscription system.**

---

## URLs

| Service | URL |
|---------|-----|
| Supabase Dashboard | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg |
| SQL Editor | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql/new |
| Auth Users | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/auth/users |
| Edge Functions | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions |
| PayPal Developer | https://developer.paypal.com/dashboard |
| GitHub Repo | https://github.com/MobileDevCLI/MobileCLI-Pro |

---

## Credentials

| Account | Email | Password |
|---------|-------|----------|
| Admin User | admin@mobilecli.com | EliteDev |

---

## PayPal

| Setting | Value |
|---------|-------|
| Plan ID | P-3RH33892X5467024SNFZON2Y |
| Webhook URL | https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook |
| Price | $9.99/month |

---

## Database Tables

| Table | Purpose |
|-------|---------|
| `subscriptions` | User subscription status |
| `payment_history` | All payments |
| `webhook_logs` | PayPal webhook events |
| `admin_users` | Admin access |

---

## Quick SQL Queries

### View all users
```sql
SELECT u.email, s.status, s.expires_at
FROM auth.users u
LEFT JOIN subscriptions s ON u.id = s.user_id
ORDER BY u.created_at DESC;
```

### Find user by email
```sql
SELECT * FROM auth.users WHERE email ILIKE '%search@email.com%';
```

### Manually activate user
```sql
INSERT INTO subscriptions (user_id, status, expires_at, admin_notes)
VALUES ('USER_UUID', 'active', NOW() + INTERVAL '30 days', 'Manual activation')
ON CONFLICT (user_id) DO UPDATE SET status = 'active', expires_at = NOW() + INTERVAL '30 days';
```

### View recent payments
```sql
SELECT * FROM payment_history ORDER BY created_at DESC LIMIT 20;
```

### View webhook logs
```sql
SELECT created_at, event_type, user_email, processing_result
FROM webhook_logs ORDER BY created_at DESC LIMIT 20;
```

### View failed webhooks
```sql
SELECT * FROM webhook_logs
WHERE processing_result NOT IN ('success', 'skipped')
ORDER BY created_at DESC;
```

### Extend subscription 30 days
```sql
UPDATE subscriptions
SET expires_at = GREATEST(expires_at, NOW()) + INTERVAL '30 days'
WHERE user_id = 'USER_UUID';
```

---

## Troubleshooting

| Problem | Check | Fix |
|---------|-------|-----|
| User paid but no access | `webhook_logs` for their email | Manually activate |
| Can't log in | `auth.users` if they exist | Password reset via dashboard |
| Webhook not processing | Edge Functions logs | Check error, manually activate |
| PayPal email differs | `user_not_found` in webhook_logs | Match by app email, manually activate |

---

## Key Files

| File | What |
|------|------|
| `supabase/functions/paypal-webhook/index.ts` | Webhook handler |
| `supabase/migrations/002_professional_subscription.sql` | DB schema |
| `docs/OPERATOR_GUIDE.md` | Full operations guide |
| `docs/DATABASE_SCHEMA.md` | Database documentation |

---

*For full details, see OPERATOR_GUIDE.md*
