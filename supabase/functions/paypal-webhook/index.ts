// MobileCLI Pro - PayPal Webhook Handler
// Deployed to: https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook
//
// FIXED: Changed import to use jsr.io instead of esm.sh to fix Deno "filename" error
// PayPal Subscription Plan ID: P-3RH33892X5467024SNFZON2Y
// Price: $15/month

import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Content-Type": "application/json"
}

// Helper to validate UUID format
function isValidUUID(str: string | null | undefined): boolean {
  if (!str) return false
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
  return uuidRegex.test(str)
}

// Helper to find user by email
async function findUserByEmail(supabase: any, email: string): Promise<string | null> {
  if (!email) return null

  try {
    // Query auth.users via admin API
    const { data, error } = await supabase.auth.admin.listUsers()
    if (error || !data?.users) return null

    const user = data.users.find((u: any) =>
      u.email?.toLowerCase() === email.toLowerCase()
    )
    return user?.id || null
  } catch (e) {
    console.error("Error finding user by email:", e)
    return null
  }
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

    // Try multiple ways to get user identifier
    const customId = resource.custom_id || body.custom_id
    const subscriptionId = resource.id || resource.billing_agreement_id
    const subscriberEmail = resource.subscriber?.email_address ||
                           resource.payer?.email_address ||
                           body.payer?.email_address

    console.log("=== PayPal Webhook Received ===")
    console.log("Event Type:", eventType)
    console.log("Custom ID (user_id):", customId)
    console.log("Subscription ID:", subscriptionId)
    console.log("Subscriber Email:", subscriberEmail)
    console.log("Full payload keys:", Object.keys(body))

    // Initialize Supabase client with service role key
    const supabaseUrl = Deno.env.get("SUPABASE_URL")
    const supabaseKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")

    if (!supabaseUrl || !supabaseKey) {
      console.error("Missing Supabase credentials")
      return new Response(
        JSON.stringify({ error: "Server configuration error" }),
        { status: 500, headers: corsHeaders }
      )
    }

    const supabase = createClient(supabaseUrl, supabaseKey, {
      auth: {
        autoRefreshToken: false,
        persistSession: false
      }
    })

    // Try to get user_id from custom_id first, then by email lookup
    let userId: string | null = isValidUUID(customId) ? customId : null

    // If no custom_id, try to find user by PayPal email
    if (!userId && subscriberEmail) {
      console.log("No custom_id, trying email lookup for:", subscriberEmail)
      userId = await findUserByEmail(supabase, subscriberEmail)
      if (userId) {
        console.log("Found user by email:", userId)
      }
    }

    // Log webhook event to database FIRST (for debugging)
    const logEntry = {
      event_type: eventType,
      event_id: body.id || `manual-${Date.now()}`,
      provider: "paypal",
      payload: body,
      user_id: userId,
      user_email: subscriberEmail || null,
      processed: false,
      processing_result: userId ? "user_found" : "no_user_id"
    }

    const { error: logError } = await supabase
      .from("webhook_logs")
      .insert(logEntry)

    if (logError) {
      console.error("Failed to log webhook:", logError.message)
    } else {
      console.log("Webhook logged successfully")
    }

    // Process the event
    let processingResult = "no_action"

    // Handle subscription activation
    if ((eventType === "BILLING.SUBSCRIPTION.ACTIVATED" ||
         eventType === "BILLING.SUBSCRIPTION.CREATED") && userId) {
      console.log("Activating subscription for user:", userId)

      const { data, error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: userId,
          status: "active",
          paypal_subscription_id: subscriptionId,
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })
        .select()

      if (error) {
        console.error("Database error on activation:", error.message)
        processingResult = `error: ${error.message}`
      } else {
        console.log("Subscription activated:", data)
        processingResult = "subscription_activated"
      }
    }

    // Handle payment completed (renewal)
    if (eventType === "PAYMENT.SALE.COMPLETED" && userId) {
      console.log("Payment completed for user:", userId)

      const { error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: userId,
          status: "active",
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })

      if (error) {
        console.error("Database error on payment:", error.message)
        processingResult = `error: ${error.message}`
      } else {
        processingResult = "payment_completed"
      }

      // Record payment history
      await supabase
        .from("payment_history")
        .insert({
          user_id: userId,
          amount: parseFloat(resource.amount?.total) || 15.00,
          currency: resource.amount?.currency || "USD",
          payment_type: "subscription_renewal",
          provider: "paypal",
          paypal_transaction_id: resource.id,
          paypal_subscription_id: subscriptionId,
          status: "completed",
          description: "MobileCLI Pro subscription - $15/month"
        })
    }

    // Handle cancellation
    if (eventType === "BILLING.SUBSCRIPTION.CANCELLED" && userId) {
      console.log("Cancelling subscription for user:", userId)

      const { error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: userId,
          status: "cancelled",
          cancelled_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })

      processingResult = error ? `error: ${error.message}` : "subscription_cancelled"
    }

    // Handle suspension
    if (eventType === "BILLING.SUBSCRIPTION.SUSPENDED" && userId) {
      const { error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: userId,
          status: "suspended",
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })

      processingResult = error ? `error: ${error.message}` : "subscription_suspended"
    }

    // Handle expiration
    if (eventType === "BILLING.SUBSCRIPTION.EXPIRED" && userId) {
      const { error } = await supabase
        .from("subscriptions")
        .upsert({
          user_id: userId,
          status: "expired",
          updated_at: new Date().toISOString()
        }, {
          onConflict: "user_id"
        })

      processingResult = error ? `error: ${error.message}` : "subscription_expired"
    }

    // Update webhook log with processing result
    if (body.id) {
      await supabase
        .from("webhook_logs")
        .update({
          processed: true,
          processing_result: processingResult,
          processed_at: new Date().toISOString()
        })
        .eq("event_id", body.id)
    }

    console.log("=== Webhook Processing Complete ===")
    console.log("Result:", processingResult)

    // Always return 200 to PayPal (so they don't retry)
    return new Response(
      JSON.stringify({
        received: true,
        event: eventType,
        user_found: !!userId,
        result: processingResult
      }),
      { status: 200, headers: corsHeaders }
    )

  } catch (err) {
    console.error("Webhook error:", err.message || err)
    return new Response(
      JSON.stringify({ error: "Internal server error", details: err.message }),
      { status: 500, headers: corsHeaders }
    )
  }
})
