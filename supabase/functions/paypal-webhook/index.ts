// MobileCLI Pro - PayPal Webhook Handler
// Deployed to: https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook
//
// This function receives PayPal webhook events and updates subscription status.
// PayPal Subscription Plan ID: P-3RH33892X5467024SNFZON2Y
//
// IMPORTANT: Uses Deno.serve() for reliability in Supabase Edge Functions

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
    const customId = resource.custom_id // This is the Supabase user_id
    const subscriptionId = resource.id
    const subscriberEmail = resource.subscriber?.email_address

    console.log("=== PayPal Webhook ===")
    console.log("Event:", eventType)
    console.log("User ID:", customId)
    console.log("Subscription ID:", subscriptionId)
    console.log("Email:", subscriberEmail)

    // Initialize Supabase client
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

    // Handle different PayPal events
    // IMPORTANT: Using UPSERT instead of UPDATE
    // Supabase .update() returns empty array (not error) when no row matches
    // UPSERT creates row if missing, updates if exists

    if (eventType === "BILLING.SUBSCRIPTION.ACTIVATED" && customId) {
      console.log("Activating subscription for user:", customId)

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

      if (error) {
        console.error("Database error:", error.message)
      } else {
        console.log("Subscription activated successfully:", data)
      }
    }

    if (eventType === "BILLING.SUBSCRIPTION.CANCELLED" && customId) {
      console.log("Cancelling subscription for user:", customId)

      const { data, error } = await supabase
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

    if (eventType === "PAYMENT.SALE.COMPLETED" && customId) {
      console.log("Payment completed for user:", customId)

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

    // Always return success to PayPal
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
