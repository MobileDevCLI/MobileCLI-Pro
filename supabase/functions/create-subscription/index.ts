// MobileCLI Pro - Server-Side PayPal Subscription Creation
// This function creates a PayPal subscription with custom_id properly embedded
// so the webhook can always match the user, regardless of email differences.

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Content-Type": "application/json"
}

// PayPal API endpoints
const PAYPAL_API_BASE = "https://api-m.paypal.com" // Production
// const PAYPAL_API_BASE = "https://api-m.sandbox.paypal.com" // Sandbox

// MobileCLI Pro subscription plan
const PLAN_ID = "P-3RH33892X5467024SNFZON2Y"

interface CreateSubscriptionRequest {
  user_id: string
  return_url?: string
  cancel_url?: string
}

/**
 * Get PayPal access token using client credentials
 */
async function getPayPalAccessToken(): Promise<string> {
  const clientId = Deno.env.get("PAYPAL_CLIENT_ID")
  const clientSecret = Deno.env.get("PAYPAL_CLIENT_SECRET")

  if (!clientId || !clientSecret) {
    throw new Error("PayPal credentials not configured")
  }

  const auth = btoa(`${clientId}:${clientSecret}`)

  const response = await fetch(`${PAYPAL_API_BASE}/v1/oauth2/token`, {
    method: "POST",
    headers: {
      "Authorization": `Basic ${auth}`,
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: "grant_type=client_credentials"
  })

  if (!response.ok) {
    const error = await response.text()
    console.error("PayPal auth failed:", error)
    throw new Error(`PayPal authentication failed: ${response.status}`)
  }

  const data = await response.json()
  return data.access_token
}

/**
 * Create a PayPal subscription with custom_id embedded
 */
async function createPayPalSubscription(
  accessToken: string,
  userId: string,
  returnUrl: string,
  cancelUrl: string
): Promise<{ approvalUrl: string; subscriptionId: string }> {

  const subscriptionData = {
    plan_id: PLAN_ID,
    custom_id: userId, // THIS IS THE KEY - embeds user_id in subscription
    application_context: {
      brand_name: "MobileCLI Pro",
      locale: "en-US",
      shipping_preference: "NO_SHIPPING",
      user_action: "SUBSCRIBE_NOW",
      return_url: returnUrl,
      cancel_url: cancelUrl
    }
  }

  console.log("Creating PayPal subscription with custom_id:", userId)

  const response = await fetch(`${PAYPAL_API_BASE}/v1/billing/subscriptions`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${accessToken}`,
      "Content-Type": "application/json",
      "PayPal-Request-Id": `mobilecli-${userId}-${Date.now()}` // Idempotency key
    },
    body: JSON.stringify(subscriptionData)
  })

  if (!response.ok) {
    const error = await response.text()
    console.error("PayPal subscription creation failed:", error)
    throw new Error(`Failed to create subscription: ${response.status} - ${error}`)
  }

  const subscription = await response.json()
  console.log("PayPal subscription created:", subscription.id)

  // Find the approval URL in the links array
  const approvalLink = subscription.links?.find(
    (link: any) => link.rel === "approve"
  )

  if (!approvalLink) {
    throw new Error("No approval URL in PayPal response")
  }

  return {
    approvalUrl: approvalLink.href,
    subscriptionId: subscription.id
  }
}

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    // Only allow POST
    if (req.method !== "POST") {
      return new Response(
        JSON.stringify({ error: "Method not allowed" }),
        { status: 405, headers: corsHeaders }
      )
    }

    // Parse request body
    const body: CreateSubscriptionRequest = await req.json()
    const { user_id, return_url, cancel_url } = body

    // Validate user_id
    if (!user_id) {
      return new Response(
        JSON.stringify({ error: "user_id is required" }),
        { status: 400, headers: corsHeaders }
      )
    }

    // Validate UUID format
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
    if (!uuidRegex.test(user_id)) {
      return new Response(
        JSON.stringify({ error: "Invalid user_id format" }),
        { status: 400, headers: corsHeaders }
      )
    }

    // Default URLs if not provided
    const finalReturnUrl = return_url || "https://www.mobilecli.com/success.html"
    const finalCancelUrl = cancel_url || "https://www.mobilecli.com/pricing.html"

    console.log("=== Creating Subscription ===")
    console.log("User ID:", user_id)
    console.log("Return URL:", finalReturnUrl)

    // Get PayPal access token
    const accessToken = await getPayPalAccessToken()

    // Create the subscription
    const { approvalUrl, subscriptionId } = await createPayPalSubscription(
      accessToken,
      user_id,
      finalReturnUrl,
      finalCancelUrl
    )

    console.log("=== Subscription Created ===")
    console.log("Subscription ID:", subscriptionId)
    console.log("Approval URL:", approvalUrl)

    // Return the approval URL for the client to redirect to
    return new Response(
      JSON.stringify({
        success: true,
        approval_url: approvalUrl,
        subscription_id: subscriptionId,
        message: "Redirect user to approval_url to complete payment"
      }),
      { status: 200, headers: corsHeaders }
    )

  } catch (err: any) {
    console.error("Error creating subscription:", err.message || err)
    return new Response(
      JSON.stringify({
        error: "Failed to create subscription",
        details: err.message
      }),
      { status: 500, headers: corsHeaders }
    )
  }
})
