// MobileCLI Pro - PayPal REST Webhook Handler
// Deploy to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions
//
// This function receives PayPal REST Webhook events for subscriptions
// and updates the subscription status in the database.
//
// PayPal Subscription Plan ID: P-3RH33892X5467024SNFZON2Y
// Events configured: BILLING.SUBSCRIPTION.* (all subscription events)
//
// MATCHING LOGIC:
// 1. First tries to match by custom_id (Supabase user_id passed from app)
// 2. Falls back to matching by PayPal subscriber email
// This handles cases where user's PayPal email differs from login email.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // PayPal REST webhooks send JSON (not form-urlencoded like IPN)
    const payload = await req.json()

    const eventType = payload.event_type
    const resource = payload.resource

    console.log('PayPal Webhook received:', {
      event_type: eventType,
      resource_id: resource?.id,
      custom_id: resource?.custom_id,
      subscriber_email: resource?.subscriber?.email_address,
      status: resource?.status
    })

    // Create Supabase client with service role (bypasses RLS)
    const supabaseUrl = Deno.env.get('SUPABASE_URL')
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')

    if (!supabaseUrl || !supabaseServiceKey) {
      console.error('Missing Supabase environment variables')
      return new Response(JSON.stringify({ error: 'Configuration error' }), {
        status: 500,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    // Handle different PayPal REST webhook event types
    switch (eventType) {
      case 'BILLING.SUBSCRIPTION.ACTIVATED':
        console.log('Subscription ACTIVATED')
        await handleSubscriptionActivated(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.CREATED':
        console.log('Subscription CREATED')
        // Don't activate yet - wait for ACTIVATED event
        break

      case 'BILLING.SUBSCRIPTION.UPDATED':
        console.log('Subscription UPDATED')
        await handleSubscriptionUpdated(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.CANCELLED':
        console.log('Subscription CANCELLED')
        await handleSubscriptionCancelled(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.SUSPENDED':
        console.log('Subscription SUSPENDED')
        await handleSubscriptionSuspended(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.EXPIRED':
        console.log('Subscription EXPIRED')
        await handleSubscriptionExpired(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.RE-ACTIVATED':
        console.log('Subscription RE-ACTIVATED')
        await handleSubscriptionActivated(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.PAYMENT.FAILED':
        console.log('Subscription PAYMENT FAILED')
        break

      case 'PAYMENT.SALE.COMPLETED':
        console.log('Payment SALE COMPLETED')
        await handlePaymentCompleted(supabase, resource)
        break

      default:
        console.log('Unhandled event type:', eventType)
    }

    return new Response(JSON.stringify({ received: true }), {
      status: 200,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })

  } catch (error) {
    console.error('Webhook error:', error)
    return new Response(JSON.stringify({ error: 'Internal error' }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }
})

/**
 * Find user by custom_id (user_id) or email
 * Returns user object or null
 */
async function findUser(supabase: any, customId: string | null, email: string | null): Promise<any> {
  // Method 1: Direct match by custom_id (this is the Supabase user_id)
  if (customId) {
    console.log('Trying to match by custom_id (user_id):', customId)

    // custom_id IS the user_id, so we can use it directly
    const { data: user, error } = await supabase.auth.admin.getUserById(customId)

    if (!error && user) {
      console.log('✅ Found user by custom_id:', user.id, user.email)
      return user
    }
    console.log('No user found by custom_id')
  }

  // Method 2: Fall back to email matching
  if (email) {
    console.log('Trying to match by email:', email)
    const { data: users, error } = await supabase.auth.admin.listUsers()

    if (error) {
      console.error('Error listing users:', error)
      return null
    }

    const user = users.users.find((u: any) =>
      u.email?.toLowerCase() === email.toLowerCase()
    )

    if (user) {
      console.log('✅ Found user by email:', user.id, user.email)
      return user
    }
    console.log('No user found by email')
  }

  console.error('❌ Could not find user by custom_id or email')
  return null
}

/**
 * Handle subscription activated - this is the main success event
 */
async function handleSubscriptionActivated(supabase: any, resource: any) {
  const customId = resource?.custom_id  // User ID from app
  const subscriberEmail = resource?.subscriber?.email_address
  const subscriptionId = resource?.id
  const planId = resource?.plan_id

  console.log('Activating subscription:', { customId, subscriberEmail, subscriptionId, planId })

  // Find user by custom_id first, then email
  const user = await findUser(supabase, customId, subscriberEmail)

  if (!user) {
    console.error('❌ Cannot activate - user not found')
    console.log('custom_id:', customId)
    console.log('subscriber_email:', subscriberEmail)
    return
  }

  // Calculate expiry (30 days from now)
  const expiresAt = new Date()
  expiresAt.setDate(expiresAt.getDate() + 30)

  // Update subscription to active
  const { error } = await supabase
    .from('subscriptions')
    .upsert({
      user_id: user.id,
      status: 'active',
      paypal_subscription_id: subscriptionId,
      expires_at: expiresAt.toISOString(),
      updated_at: new Date().toISOString()
    }, {
      onConflict: 'user_id'
    })

  if (error) {
    console.error('Error updating subscription:', error)
  } else {
    console.log('✅ Subscription ACTIVATED for user:', user.id, user.email)
  }
}

/**
 * Handle subscription updated
 */
async function handleSubscriptionUpdated(supabase: any, resource: any) {
  const customId = resource?.custom_id
  const subscriberEmail = resource?.subscriber?.email_address
  const subscriptionId = resource?.id
  const status = resource?.status

  const user = await findUser(supabase, customId, subscriberEmail)
  if (!user) return

  let ourStatus = 'active'
  if (status === 'CANCELLED') ourStatus = 'cancelled'
  if (status === 'SUSPENDED') ourStatus = 'suspended'
  if (status === 'EXPIRED') ourStatus = 'expired'

  const { error } = await supabase
    .from('subscriptions')
    .update({
      status: ourStatus,
      paypal_subscription_id: subscriptionId,
      updated_at: new Date().toISOString()
    })
    .eq('user_id', user.id)

  if (error) {
    console.error('Error updating subscription:', error)
  } else {
    console.log('Subscription updated for user:', user.id)
  }
}

/**
 * Handle subscription cancelled
 */
async function handleSubscriptionCancelled(supabase: any, resource: any) {
  const customId = resource?.custom_id
  const subscriberEmail = resource?.subscriber?.email_address

  const user = await findUser(supabase, customId, subscriberEmail)
  if (!user) return

  const { error } = await supabase
    .from('subscriptions')
    .update({
      status: 'cancelled',
      updated_at: new Date().toISOString()
    })
    .eq('user_id', user.id)

  if (error) {
    console.error('Error cancelling subscription:', error)
  } else {
    console.log('Subscription cancelled for user:', user.id)
  }
}

/**
 * Handle subscription suspended
 */
async function handleSubscriptionSuspended(supabase: any, resource: any) {
  const customId = resource?.custom_id
  const subscriberEmail = resource?.subscriber?.email_address

  const user = await findUser(supabase, customId, subscriberEmail)
  if (!user) return

  const { error } = await supabase
    .from('subscriptions')
    .update({
      status: 'suspended',
      updated_at: new Date().toISOString()
    })
    .eq('user_id', user.id)

  if (error) {
    console.error('Error suspending subscription:', error)
  } else {
    console.log('Subscription suspended for user:', user.id)
  }
}

/**
 * Handle subscription expired
 */
async function handleSubscriptionExpired(supabase: any, resource: any) {
  const customId = resource?.custom_id
  const subscriberEmail = resource?.subscriber?.email_address

  const user = await findUser(supabase, customId, subscriberEmail)
  if (!user) return

  const { error } = await supabase
    .from('subscriptions')
    .update({
      status: 'expired',
      updated_at: new Date().toISOString()
    })
    .eq('user_id', user.id)

  if (error) {
    console.error('Error expiring subscription:', error)
  } else {
    console.log('Subscription expired for user:', user.id)
  }
}

/**
 * Handle one-time payment completed
 */
async function handlePaymentCompleted(supabase: any, resource: any) {
  const customId = resource?.custom_id
  const payerEmail = resource?.payer?.email_address

  const user = await findUser(supabase, customId, payerEmail)
  if (!user) return

  const expiresAt = new Date()
  expiresAt.setDate(expiresAt.getDate() + 30)

  const { error } = await supabase
    .from('subscriptions')
    .upsert({
      user_id: user.id,
      status: 'active',
      paypal_subscription_id: null,
      expires_at: expiresAt.toISOString(),
      updated_at: new Date().toISOString()
    }, {
      onConflict: 'user_id'
    })

  if (error) {
    console.error('Error activating from payment:', error)
  } else {
    console.log('Subscription activated from payment for user:', user.id)
  }
}
