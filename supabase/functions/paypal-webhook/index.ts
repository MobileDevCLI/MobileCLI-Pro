// MobileCLI Pro - PayPal REST Webhook Handler
// Deploy to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions
//
// This function receives PayPal REST Webhook events for subscriptions
// and updates the subscription status in the database.
//
// PayPal Subscription Plan ID: P-3RH33892X5467024SNFZON2Y
// Events configured: BILLING.SUBSCRIPTION.* (all subscription events)

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
        // Subscription activated (after payment)
        console.log('Subscription ACTIVATED')
        await handleSubscriptionActivated(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.CREATED':
        // Subscription created (may not be paid yet)
        console.log('Subscription CREATED')
        // Don't activate yet - wait for ACTIVATED event
        break

      case 'BILLING.SUBSCRIPTION.UPDATED':
        // Subscription updated
        console.log('Subscription UPDATED')
        await handleSubscriptionUpdated(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.CANCELLED':
        // Subscription cancelled by user
        console.log('Subscription CANCELLED')
        await handleSubscriptionCancelled(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.SUSPENDED':
        // Subscription suspended (payment failed)
        console.log('Subscription SUSPENDED')
        await handleSubscriptionSuspended(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.EXPIRED':
        // Subscription expired
        console.log('Subscription EXPIRED')
        await handleSubscriptionExpired(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.RE-ACTIVATED':
        // Subscription re-activated after suspension
        console.log('Subscription RE-ACTIVATED')
        await handleSubscriptionActivated(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.PAYMENT.FAILED':
        // Payment failed
        console.log('Subscription PAYMENT FAILED')
        // Don't immediately cancel - PayPal will retry
        break

      case 'PAYMENT.SALE.COMPLETED':
        // One-time payment completed (if using that)
        console.log('Payment SALE COMPLETED')
        await handlePaymentCompleted(supabase, resource)
        break

      default:
        console.log('Unhandled event type:', eventType)
    }

    // PayPal expects a 200 response
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
 * Handle subscription activated - this is the main success event
 */
async function handleSubscriptionActivated(supabase: any, resource: any) {
  const subscriberEmail = resource?.subscriber?.email_address
  const subscriptionId = resource?.id
  const planId = resource?.plan_id
  const status = resource?.status

  console.log('Activating subscription:', { subscriberEmail, subscriptionId, planId, status })

  if (!subscriberEmail) {
    console.error('No subscriber email in webhook payload')
    return
  }

  // Find user by email
  const { data: users, error: userError } = await supabase.auth.admin.listUsers()

  if (userError) {
    console.error('Error listing users:', userError)
    return
  }

  const user = users.users.find((u: any) =>
    u.email?.toLowerCase() === subscriberEmail.toLowerCase()
  )

  if (!user) {
    console.error('User not found for email:', subscriberEmail)
    console.log('Available users:', users.users.map((u: any) => u.email))
    // TODO: Store for later matching or send notification
    return
  }

  console.log('Found user:', user.id, user.email)

  // Calculate expiry (30 days from now for monthly)
  const expiresAt = new Date()
  expiresAt.setDate(expiresAt.getDate() + 30)

  // Update subscription to active
  const { error } = await supabase
    .from('subscriptions')
    .upsert({
      user_id: user.id,
      status: 'active',
      plan: 'pro',
      paypal_subscription_id: subscriptionId,
      current_period_end: expiresAt.toISOString(),
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
  const subscriberEmail = resource?.subscriber?.email_address
  const subscriptionId = resource?.id
  const status = resource?.status

  if (!subscriberEmail) return

  const { data: users } = await supabase.auth.admin.listUsers()
  const user = users?.users.find((u: any) =>
    u.email?.toLowerCase() === subscriberEmail.toLowerCase()
  )

  if (!user) return

  // Map PayPal status to our status
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
  const subscriberEmail = resource?.subscriber?.email_address

  if (!subscriberEmail) return

  const { data: users } = await supabase.auth.admin.listUsers()
  const user = users?.users.find((u: any) =>
    u.email?.toLowerCase() === subscriberEmail.toLowerCase()
  )

  if (!user) return

  // Mark as cancelled (user keeps access until current_period_end)
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
 * Handle subscription suspended (payment failed)
 */
async function handleSubscriptionSuspended(supabase: any, resource: any) {
  const subscriberEmail = resource?.subscriber?.email_address

  if (!subscriberEmail) return

  const { data: users } = await supabase.auth.admin.listUsers()
  const user = users?.users.find((u: any) =>
    u.email?.toLowerCase() === subscriberEmail.toLowerCase()
  )

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
  const subscriberEmail = resource?.subscriber?.email_address

  if (!subscriberEmail) return

  const { data: users } = await supabase.auth.admin.listUsers()
  const user = users?.users.find((u: any) =>
    u.email?.toLowerCase() === subscriberEmail.toLowerCase()
  )

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
 * Handle one-time payment completed (fallback)
 */
async function handlePaymentCompleted(supabase: any, resource: any) {
  // For one-time payments, extract email from custom field or billing agreement
  const payerEmail = resource?.payer?.email_address

  if (!payerEmail) {
    console.log('No payer email in payment resource')
    return
  }

  const { data: users } = await supabase.auth.admin.listUsers()
  const user = users?.users.find((u: any) =>
    u.email?.toLowerCase() === payerEmail.toLowerCase()
  )

  if (!user) {
    console.log('User not found for payment:', payerEmail)
    return
  }

  // Activate for 30 days
  const expiresAt = new Date()
  expiresAt.setDate(expiresAt.getDate() + 30)

  const { error } = await supabase
    .from('subscriptions')
    .upsert({
      user_id: user.id,
      status: 'active',
      plan: 'pro',
      current_period_end: expiresAt.toISOString(),
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
