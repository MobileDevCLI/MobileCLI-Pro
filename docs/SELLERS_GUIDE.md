# First-Time App Seller's Guide: Everything You Need to Know

## Who This Is For

You're a developer who built an app (MobileCLI Pro) and you're about to sell it to potentially thousands of customers at $15.00/month. You've never done this before and you need to understand the business side, not just the code.

---

## PART 1: THE MONEY FLOW - How You Actually Get Paid

### What Happens When Someone Subscribes

```
Customer clicks "Subscribe" in your app
    |
Opens PayPal checkout page
    |
Customer logs into PayPal, confirms $15.00/month
    |
PayPal takes the money from customer
    |
PayPal sends webhook to your Supabase function
    |
Your database marks user as "active"
    |
User has Pro access
    |
PayPal deposits money to YOUR PayPal account (minus fees)
```

### PayPal Fees - What You Actually Keep

| Customer Pays | PayPal Fee | You Keep |
|---------------|------------|----------|
| $15.00 | ~$0.74 (2.9% + $0.30) | ~$14.26 |

**Per 1,000 subscribers/month:** ~$14,260 revenue

### When Do You Get The Money?

- PayPal holds money in your PayPal Business account
- You can transfer to your bank (takes 1-3 days)
- PayPal may hold funds for new sellers (first 21 days or until you build history)

---

## PART 2: YOUR DASHBOARDS - Where To See Everything

### Dashboard 1: PayPal Business Dashboard

**URL:** https://www.paypal.com/businesshub/

**What you see here:**
- Total balance (money available to withdraw)
- Recent transactions (every payment)
- Subscription list (who's subscribed)
- Disputes/chargebacks (if any customer complains)

**Go here to:**
- See how much money you've made
- Issue refunds
- See who cancelled
- Download reports for taxes

### Dashboard 2: PayPal Subscriptions

**URL:** https://www.paypal.com/billing/subscriptions

**What you see here:**
- All active subscribers
- Subscription status (active, cancelled, suspended)
- Next billing date for each subscriber
- Payment history per subscriber

**Go here to:**
- Cancel a specific user's subscription
- See when someone will be billed next
- Troubleshoot payment issues

### Dashboard 3: Supabase Database

**URL:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/editor

**What you see here:**
- `subscriptions` table - all your users and their status
- `auth.users` table - all registered users
- Real-time data

**Go here to:**
- See how many users you have
- Debug subscription issues
- Manually fix a user's status if needed

### Dashboard 4: Supabase Edge Function Logs

**URL:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs

**What you see here:**
- Every webhook event from PayPal
- Errors if something went wrong
- Debug information

**Go here to:**
- Troubleshoot "I paid but don't have access" complaints
- See if webhooks are firing
- Debug payment issues

---

## PART 3: THINGS THAT WILL HAPPEN (And How To Handle Them)

### Scenario 1: Customer Says "I Paid But Don't Have Access"

**Check these in order:**
1. **PayPal Dashboard** - Did they actually pay? (Check transaction history)
2. **Webhook Logs** - Did PayPal send the webhook?
3. **Supabase Database** - Is their `status` set to `active`?

**Common causes:**
- Different email for PayPal vs app login (custom_id issue you know about)
- Webhook failed silently (fixed with upsert, but check)
- They paid to wrong PayPal account (unlikely but possible)

**Quick fix:**
```sql
-- In Supabase SQL Editor
UPDATE subscriptions SET status = 'active' WHERE user_id = 'their-uuid';
```

### Scenario 2: Customer Wants Refund

**How to issue:**
1. Go to PayPal Dashboard -> Activities
2. Find their transaction
3. Click "Issue refund"
4. Choose full or partial refund

**What happens:**
- Money goes back to customer
- Their subscription may be cancelled automatically
- You should also mark them as cancelled in your database

**Policy recommendation:**
- Offer refunds within 7 days, no questions asked
- After 7 days, case by case basis
- Document your policy in Terms of Service

### Scenario 3: Payment Fails (Card Expired, Insufficient Funds)

**PayPal handles this automatically:**
- Sends BILLING.SUBSCRIPTION.SUSPENDED event
- Retries payment 3 times over ~2 weeks
- If all fail, sends BILLING.SUBSCRIPTION.CANCELLED

**Your database will show:** `status = 'suspended'` then `status = 'cancelled'`

**You don't need to do anything** - your webhook already handles these events.

### Scenario 4: Customer Cancels Subscription

**Two ways they can cancel:**
1. Through PayPal: https://www.paypal.com/myaccount/autopay/
2. Through your app (if you add "Manage Subscription" button)

**What happens:**
- PayPal sends BILLING.SUBSCRIPTION.CANCELLED webhook
- Your database marks them as cancelled
- They keep access until their paid period ends (optional - your choice)

### Scenario 5: Chargeback (Customer Disputes Charge With Bank)

**This is serious:**
- Customer told their bank "I didn't authorize this"
- Bank takes money back from PayPal
- PayPal takes money from your account
- You get a $20 chargeback fee

**How to fight it:**
1. PayPal emails you about dispute
2. You provide evidence (screenshots of them using app, signup confirmation)
3. Bank decides

**Prevention:**
- Clear refund policy
- Good customer service
- Respond to refund requests promptly

---

## PART 4: TAXES - Yes, You Have To Pay Them

### What You Need To Track

- Total revenue received
- PayPal fees paid
- Refunds issued
- Net income = Revenue - Fees - Refunds

### Tools For This

**PayPal Reports:**
1. Go to https://www.paypal.com/reports/
2. Download monthly or yearly transaction reports
3. CSV/Excel format for accounting

**Recommendation:**
- Download reports monthly
- Store them somewhere safe
- At tax time, you'll have everything

### Do You Need An LLC?

**Short answer:** Not required, but recommended if you're making real money.

**Benefits of LLC:**
- Separates personal and business liability
- Looks more professional
- Tax advantages (depending on your situation)

**Cost:** $50-$500 depending on state

**For now:** You can operate as a sole proprietor (just you) until you see if this makes real money.

---

## PART 5: LEGAL DOCUMENTS YOU NEED

### 1. Terms of Service

**You have:** `docs/TERMS_OF_SERVICE.md`

**Must include:**
- What they're paying for
- Your refund policy
- What happens if they violate terms
- Limitation of liability

### 2. Privacy Policy

**You have:** `docs/PRIVACY_POLICY.md`

**Must include:**
- What data you collect (email, payment info via PayPal)
- How you use it
- Who you share it with (PayPal, Supabase)
- How they can delete their account

### 3. Refund Policy

**Added to Terms of Service:**
```
Refund Policy:
- Full refund within 7 days of first payment, no questions asked
- After 7 days: Cancel anytime, no refunds for current period
- Refunds processed within 3-5 business days
- To request: email mobiledevcli@gmail.com
```

---

## PART 6: WHAT YOU'RE MISSING RIGHT NOW

### Currently You Have:
- [x] Payment processing (PayPal)
- [x] Database (Supabase)
- [x] Webhook for subscription events
- [x] Terms of Service (updated with refund policy)
- [x] Privacy Policy (updated with contact email)

### What You Should Add:

#### 1. Customer Support Email
- **Using:** mobiledevcli@gmail.com
- Add to app Settings/About screen
- Add to website footer

#### 2. A Way To See Metrics At A Glance
Right now you have to check multiple dashboards. Consider:
- Simple spreadsheet tracking monthly subscribers/revenue
- Or build a simple admin dashboard (later)

#### 3. Email Notifications
When someone subscribes/cancels, you should know:
- PayPal can email you on every transaction
- Or use a service like Resend to send yourself alerts

#### 4. Backup Payment Method (Optional, For Later)
PayPal is good, but some users prefer:
- Credit card (Stripe)
- Google Play billing
- Having both increases conversions

---

## PART 7: YOUR DAILY/WEEKLY ROUTINE

### Daily (2 minutes)
- Check PayPal app on phone for new payments/issues
- Check email for customer support requests

### Weekly (15 minutes)
- Log into PayPal dashboard, note:
  - New subscribers this week
  - Cancellations this week
  - Revenue this week
- Check Supabase webhook logs for errors
- Respond to any support emails

### Monthly (30 minutes)
- Download PayPal monthly report
- Calculate: Revenue - Fees - Refunds = Net
- Update your tracking spreadsheet
- Check for any recurring issues

### Yearly (Tax Time)
- Collect all monthly reports
- Calculate total income
- Report on your taxes (Schedule C if sole proprietor)

---

## PART 8: WHEN YOU HIT 1,000 SUBSCRIBERS

### What Changes At Scale

| Subscribers | Monthly Revenue | Considerations |
|-------------|-----------------|----------------|
| 100 | ~$1,426 | Side project income |
| 500 | ~$7,130 | Consider LLC |
| 1,000 | ~$14,260 | Need proper bookkeeping |
| 5,000 | ~$71,300 | Consider hiring help |
| 10,000 | ~$142,600 | This is a real business |

### At 1,000+ Subscribers You Should:
- Have an LLC
- Have a separate business bank account
- Use accounting software (Wave, QuickBooks)
- Consider hiring a part-time support person
- Have automated email onboarding

---

## PART 9: COMMON MISTAKES TO AVOID

### Mistake 1: Not Tracking Revenue
**Don't:** Just let money accumulate in PayPal
**Do:** Monthly download reports, track in spreadsheet

### Mistake 2: Ignoring Customer Support
**Don't:** Take 5 days to respond to emails
**Do:** Respond within 24 hours, even if just "I'm looking into it"

### Mistake 3: No Refund Policy
**Don't:** Argue with every refund request
**Do:** Easy refunds in first 7 days = happy customers = good reviews

### Mistake 4: Not Testing Payment Flow
**Don't:** Assume it works because it worked once
**Do:** Test a real subscription yourself monthly

### Mistake 5: Ignoring Failed Payments
**Don't:** Assume PayPal handles everything
**Do:** Check for suspended subscriptions, maybe email those users

### Mistake 6: Not Backing Up Data
**Don't:** Assume Supabase never has problems
**Do:** Export subscriber list monthly (from Supabase or PayPal)

---

## PART 10: CHARGEBACKS - A Complete Education

### What is a Chargeback?

A chargeback is when a customer calls their bank/credit card and says "I want my money back for this charge." The bank then FORCES PayPal to return the money.

### Why Would Someone Do This?

1. **Legitimate reasons:**
   - They didn't recognize the charge ("What's MobileCLI?")
   - They thought they cancelled but were still charged
   - Their card was stolen and someone else paid

2. **Not-so-legitimate reasons:**
   - They used your app, don't like it, and want money back
   - They forgot they subscribed
   - They're trying to scam you (rare but happens)

### The Chargeback Process (Step by Step)

```
Day 1: Customer calls their bank
    |
Day 2-3: Bank contacts PayPal "Give the money back"
    |
Day 3: PayPal takes $15.00 from your account
    |
Day 3: PayPal charges you $20 "chargeback fee"
    |
Day 3: PayPal emails you "You have a dispute"
    |
Day 3-10: You can submit evidence to fight it
    |
Day 30-90: Bank decides who wins
```

### If You Win the Dispute:
- You get your $15.00 back
- You get the $20 fee back
- This is rare (banks usually side with customers)

### If You Lose the Dispute (Most Common):
- You lose $15.00
- You lose $20 fee
- Total loss: $35.00

### The Real Danger: Too Many Chargebacks

If more than 1% of your transactions are chargebacks:
- PayPal may freeze your account
- PayPal may hold your funds for 6 months
- PayPal may ban you permanently

### How to PREVENT Chargebacks

| Prevention | How It Helps |
|------------|--------------|
| Clear business name | Customer recognizes "MobileCLI" on their statement |
| Easy cancellation | They cancel instead of chargeback |
| Quick refunds | They get money back from you, not their bank |
| Good support | They email you first, not their bank |
| Clear description | They knew what they were buying |

### The Math: Why Refunds Are Cheaper

```
Customer unhappy and wants money back:

Option A: You say "No refunds"
  -> Customer calls bank
  -> Chargeback initiated
  -> You lose: $15.00 + $20 fee = $35.00
  -> Customer angry, leaves 1-star review

Option B: You say "Okay, here's your refund"
  -> You refund $15.00
  -> You lose: $15.00 only
  -> Customer thinks "fair enough"
  -> No bad review
```

---

## PART 11: YOUR SPECIFIC CONFIGURATION

### Support Email
**Email:** mobiledevcli@gmail.com

### Refund Policy
- Full refund within 7 days of FIRST payment, no questions asked
- After 7 days: Cancel anytime, but no refunds for current period
- Cancellation stops all future billing immediately

### Admin Tools (For Now)
Use PayPal + Supabase dashboards you already have.

| Task | Where To Do It |
|------|----------------|
| See revenue | PayPal Dashboard -> Home |
| See subscribers | PayPal -> Billing -> Subscriptions |
| Issue refund | PayPal -> Activities -> Find transaction -> Refund |
| See all users | Supabase -> Table Editor -> subscriptions |
| Fix user status | Supabase -> Table Editor -> Edit row |
| See webhook errors | Supabase -> Functions -> Logs |

---

## PART 12: YOUR ACTION ITEMS

### Before You Launch (Do These Now)

1. [ ] Add support email to app Settings/About screen: `mobiledevcli@gmail.com`
2. [ ] Add support email to website footer
3. [ ] Install PayPal app on your phone and enable notifications
4. [ ] Test the full payment flow yourself (subscribe, verify access, refund)
5. [ ] Bookmark these URLs:
   - PayPal Dashboard: https://www.paypal.com/businesshub/
   - Supabase Tables: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/editor

### When You Get Your First Real Customer
1. Celebrate!
2. Check that their status is 'active' in Supabase
3. If they email with issues, respond within 24 hours

### Weekly Routine (15 minutes)
- Check PayPal app for revenue
- Check Supabase webhook logs for errors
- Respond to any support emails

### Monthly Routine (30 minutes)
- Download PayPal monthly report (for taxes)
- Count: subscribers, revenue, refunds
- Save the report somewhere safe

---

## QUICK REFERENCE URLS

| What | URL |
|------|-----|
| PayPal Dashboard | https://www.paypal.com/businesshub/ |
| PayPal Subscriptions | https://www.paypal.com/billing/subscriptions |
| PayPal Reports | https://www.paypal.com/reports/ |
| Supabase Dashboard | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg |
| Supabase Tables | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/editor |
| Webhook Logs | https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook/logs |
| Customer Cancel Link | https://www.paypal.com/myaccount/autopay/ |

---

## SUMMARY

| Topic | Your Decision |
|-------|---------------|
| Support email | mobiledevcli@gmail.com |
| Refund policy | 7-day full refund, no questions |
| Admin dashboard | PayPal + Supabase for now |
| When to build custom tools | After 100+ subscribers |

---

## WHAT YOU LEARNED

1. **How money flows:** Customer -> PayPal -> Webhook -> Database -> Your PayPal account
2. **Chargebacks are expensive:** $15.00 refund < $35.00 chargeback loss
3. **You have dashboards:** PayPal shows revenue, Supabase shows users
4. **Only 3 support issues:** "No access", "Want refund", "How to cancel"
5. **You're not going to "get in trouble":** Just track revenue and pay taxes
6. **Start simple:** Add complexity when you have real problems to solve

---

*This guide was created January 25, 2026*
*This is not legal or tax advice. Consult professionals for your specific situation.*
