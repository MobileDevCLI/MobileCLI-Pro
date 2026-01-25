# PayPal Webhook Code

**The working webhook code for PayPal subscription handling.**

---

## Currently Deployed Version (Working)

This is the minified version deployed to Supabase Edge Functions:

```javascript
import{createClient}from"https://esm.sh/@supabase/supabase-js@2";
Deno.serve(async(r)=>{
  if(r.method==="OPTIONS")return new Response("ok");
  try{
    const b=await r.json();
    const e=b.event_type||"";
    const c=b.resource?.custom_id;  // user_id from app
    const i=b.resource?.id;          // PayPal subscription ID
    console.log("Event:",e,"User:",c,"SubID:",i);

    if(e==="BILLING.SUBSCRIPTION.ACTIVATED"&&c){
      const s=createClient(
        Deno.env.get("SUPABASE_URL")??"",
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")??""
      );
      // UPSERT - creates row if missing, updates if exists
      const{data,error}=await s.from("subscriptions")
        .upsert({
          user_id:c,
          status:"active",
          paypal_subscription_id:i,
          updated_at:new Date().toISOString()
        },{onConflict:"user_id"})
        .select();
      console.log("DB result:",JSON.stringify({data,error}));
    }
    return new Response(JSON.stringify({ok:1,e}));
  }catch(x){
    console.log("Error:",x);
    return new Response(JSON.stringify({err:String(x)}),{status:500});
  }
});
```

---

## Full Readable Version

This is the expanded, commented version:

```typescript
// MobileCLI Pro - PayPal Webhook Handler
// Deployed to: https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook
//
// This function receives PayPal webhook events and updates subscription status.
//
// KEY FIX: Uses UPSERT instead of UPDATE to handle cases where
// the subscription row doesn't exist yet.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Content-Type": "application/json"
}

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    // Parse the incoming webhook payload
    const body = await req.json()
    const eventType = body.event_type || "UNKNOWN"
    const resource = body.resource || {}

    // custom_id contains the Supabase user_id (passed from app)
    const customId = resource.custom_id

    // PayPal's subscription ID
    const subscriptionId = resource.id

    // Email from PayPal (backup for matching)
    const subscriberEmail = resource.subscriber?.email_address

    console.log("=== PayPal Webhook ===")
    console.log("Event:", eventType)
    console.log("User ID (custom_id):", customId)
    console.log("Subscription ID:", subscriptionId)
    console.log("Email:", subscriberEmail)

    // Initialize Supabase client with service role key
    // Service role bypasses RLS for database operations
    const supabaseUrl = Deno.env.get("SUPABASE_URL")
    const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")

    if (!supabaseUrl || !supabaseKey) {
      console.error("Missing Supabase credentials")
      return new Response(
        JSON.stringify({ error: "Server configuration error" }),
        { status: 500, headers: corsHeaders }
      )
    }

    const supabase = createClient(supabaseUrl, supabaseKey)

    // ==========================================
    // SUBSCRIPTION ACTIVATED
    // User completed payment and subscription is now active
    // ==========================================
    if (eventType === "BILLING.SUBSCRIPTION.ACTIVATED" && customId) {
      console.log("Activating subscription for user:", customId)

      // IMPORTANT: Use UPSERT, not UPDATE
      // If the subscription row doesn't exist, UPDATE would return empty array
      // UPSERT creates the row if it doesn't exist
      const { data, error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: customId,
          status: "active",
          paypal_subscription_id: subscriptionId,
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"  // If user_id exists, update that row
        })
        .select()

      if (error) {
        console.error("Database error:", error.message)
      } else {
        console.log("Subscription activated successfully:", data)
      }
    }

    // ==========================================
    // SUBSCRIPTION CANCELLED
    // User cancelled their subscription
    // ==========================================
    if (eventType === "BILLING.SUBSCRIPTION.CANCELLED" && customId) {
      console.log("Cancelling subscription for user:", customId)

      const { error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: customId,
          status: "cancelled",
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })

      if (error) {
        console.error("Database error:", error.message)
      } else {
        console.log("Subscription cancelled successfully")
      }
    }

    // ==========================================
    // SUBSCRIPTION SUSPENDED
    // Payment failed, subscription paused
    // ==========================================
    if (eventType === "BILLING.SUBSCRIPTION.SUSPENDED" && customId) {
      console.log("Suspending subscription for user:", customId)

      const { error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: customId,
          status: "suspended",
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })

      if (error) {
        console.error("Database error:", error.message)
      }
    }

    // ==========================================
    // SUBSCRIPTION EXPIRED
    // Subscription period ended
    // ==========================================
    if (eventType === "BILLING.SUBSCRIPTION.EXPIRED" && customId) {
      console.log("Expiring subscription for user:", customId)

      const { error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: customId,
          status: "expired",
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })

      if (error) {
        console.error("Database error:", error.message)
      }
    }

    // ==========================================
    // PAYMENT COMPLETED
    // Monthly payment received (renewal)
    // ==========================================
    if (eventType === "PAYMENT.SALE.COMPLETED" && customId) {
      console.log("Payment completed for user:", customId)

      // Ensure status is active on payment
      const { error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: customId,
          status: "active",
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })

      if (error) {
        console.error("Database error:", error.message)
      }
    }

    // Always return 200 to PayPal
    // PayPal expects 200 OK, otherwise it will retry
    return new Response(
      JSON.stringify({ received: true, event: eventType }),
      { status: 200, headers: corsHeaders }
    )

  } catch (err) {
    console.error("Webhook error:", err)
    return new Response(
      JSON.stringify({ error: "Internal server error" }),
      { status: 500, headers: corsHeaders }
    )
  }
})
```

---

## Why UPSERT Instead of UPDATE?

### The Problem with UPDATE

```javascript
// This looks correct but fails silently!
const { data, error } = await supabase
  .from("subscriptions")
  .update({ status: "active" })
  .eq("user_id", customId)
  .select()

// If no row exists with that user_id:
// data = []  (empty array)
// error = null  (no error!)
// Nothing was updated, but it looks successful
```

### The Solution with UPSERT

```javascript
// This works whether the row exists or not
const { data, error } = await supabase
  .from("subscriptions")
  .upsert({
    user_id: customId,
    status: "active",
    paypal_subscription_id: subscriptionId,
    updated_at: new Date().toISOString()
  }, {
    onConflict: "user_id"
  })
  .select()

// If row exists: updates it
// If row doesn't exist: creates it
```

### Requirements for UPSERT

1. **UNIQUE constraint on conflict column:**
   ```sql
   CONSTRAINT unique_user_subscription UNIQUE (user_id)
   ```

2. **onConflict parameter must match constraint column:**
   ```javascript
   { onConflict: "user_id" }
   ```

---

## Deploy to Supabase

### Via Dashboard

1. Go to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions/paypal-webhook
2. Click "Code" tab
3. Paste the minified code above
4. Click "Deploy updates"

### Via CLI

```bash
# From project directory
supabase functions deploy paypal-webhook
```

---

## Environment Variables

The Edge Function needs these (auto-provided by Supabase):

| Variable | Description |
|----------|-------------|
| `SUPABASE_URL` | Your Supabase project URL |
| `SUPABASE_SERVICE_ROLE_KEY` | Service role key for bypassing RLS |

These are automatically available in Supabase Edge Functions.

---

## Testing

See `TEST_PAYLOADS.md` for test commands.

---

*Last Updated: January 25, 2026*
