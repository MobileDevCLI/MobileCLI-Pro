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
//
// LOGGING:
// - All webhooks logged to webhook_logs table
// - All payments logged to payment_history table

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
  let webhookLogId: string | null = null

  try {
    // PayPal REST webhooks send JSON (not form-urlencoded like IPN)
    const payload = await req.json()

    const eventType = payload.event_type
    const eventId = payload.id
    const resource = payload.resource

    // Extract user email for logging
    const userEmail = resource?.subscriber?.email_address ||
                      resource?.payer?.email_address ||
                      null

    console.log('PayPal Webhook received:', {
      event_type: eventType,
      event_id: eventId,
      resource_id: resource?.id,
      custom_id: resource?.custom_id,
      subscriber_email: userEmail,
      status: resource?.status
    })

    // Log webhook to database immediately
    webhookLogId = await logWebhook(supabase, {
      eventType,
      eventId,
      payload,
      userEmail,
      processed: false,
      processingResult: null,
      errorMessage: null
    })

    // Handle different PayPal REST webhook event types
    let processingResult = 'success'
    let userId: string | null = null

    switch (eventType) {
      case 'BILLING.SUBSCRIPTION.ACTIVATED':
        console.log('Subscription ACTIVATED')
        userId = await handleSubscriptionActivated(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.CREATED':
        console.log('Subscription CREATED')
        // Don't activate yet - wait for ACTIVATED event
        processingResult = 'skipped'
        break

      case 'BILLING.SUBSCRIPTION.UPDATED':
        console.log('Subscription UPDATED')
        userId = await handleSubscriptionUpdated(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.CANCELLED':
        console.log('Subscription CANCELLED')
        userId = await handleSubscriptionCancelled(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.SUSPENDED':
        console.log('Subscription SUSPENDED')
        userId = await handleSubscriptionSuspended(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.EXPIRED':
        console.log('Subscription EXPIRED')
        userId = await handleSubscriptionExpired(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.RE-ACTIVATED':
        console.log('Subscription RE-ACTIVATED')
        userId = await handleSubscriptionActivated(supabase, resource)
        break

      case 'BILLING.SUBSCRIPTION.PAYMENT.FAILED':
        console.log('Subscription PAYMENT FAILED')
        userId = await handlePaymentFailed(supabase, resource)
        processingResult = 'success'
        break

      case 'PAYMENT.SALE.COMPLETED':
        console.log('Payment SALE COMPLETED')
        userId = await handlePaymentCompleted(supabase, resource)
        break

      default:
        console.log('Unhandled event type:', eventType)
        processingResult = 'skipped'
    }

    // Update webhook log with success
    await updateWebhookLog(supabase, webhookLogId, {
      userId,
      processed: true,
      processingResult,
      errorMessage: null
    })

    return new Response(JSON.stringify({ received: true, log_id: webhookLogId }), {
      status: 200,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })

  } catch (error) {
    console.error('Webhook error:', error)

    // Update webhook log with error
    if (webhookLogId) {
      await updateWebhookLog(supabase, webhookLogId, {
        userId: null,
        processed: true,
        processingResult: 'db_error',
        errorMessage: error.message
      })
    }

    return new Response(JSON.stringify({ error: 'Internal error' }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }
})

/**
 * Log webhook to database
 */
async function logWebhook(supabase: any, data: {
  eventType: string,
  eventId: string,
  payload: any,
  userEmail: string | null,
  processed: boolean,
  processingResult: string | null,
  errorMessage: string | null
}): Promise<string> {
  try {
    const { data: log, error } = await supabase
      .from('webhook_logs')
      .insert({
        event_type: data.eventType,
        event_id: data.eventId,
        provider: 'paypal',
        payload: data.payload,
        user_email: data.userEmail,
        processed: data.processed,
        processing_result: data.processingResult,
        error_message: data.errorMessage
      })
      .select('id')
      .single()

    if (error) {
      console.error('Error logging webhook:', error)
      return ''
    }

    return log.id
  } catch (err) {
    console.error('Exception logging webhook:', err)
    return ''
  }
}

/**
 * Update webhook log after processing
 */
async function updateWebhookLog(supabase: any, logId: string, data: {
  userId: string | null,
  processed: boolean,
  processingResult: string,
  errorMessage: string | null
}): Promise<void> {
  if (!logId) return

  try {
    await supabase
      .from('webhook_logs')
      .update({
        user_id: data.userId,
        processed: data.processed,
        processing_result: data.processingResult,
        error_message: data.errorMessage,
        processed_at: new Date().toISOString()
      })
      .eq('id', logId)
  } catch (err) {
    console.error('Error updating webhook log:', err)
  }
}

/**
 * Record payment to payment_history table
 */
async function recordPayment(supabase: any, data: {
  userId: string,
  amount: number,
  currency: string,
  paymentType: string,
  transactionId: string | null,
  subscriptionId: string | null,
  status: string,
  description: string | null
}): Promise<void> {
  try {
    await supabase
      .from('payment_history')
      .insert({
        user_id: data.userId,
        amount: data.amount,
        currency: data.currency,
        payment_type: data.paymentType,
        provider: 'paypal',
        paypal_transaction_id: data.transactionId,
        paypal_subscription_id: data.subscriptionId,
        status: data.status,
        description: data.description
      })

    console.log('Payment recorded:', data.transactionId || data.subscriptionId)
  } catch (err) {
    console.error('Error recording payment:', err)
  }
}

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
async function handleSubscriptionActivated(supabase: any, resource: any): Promise<string | null> {
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
    return null
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
      last_payment_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    }, {
      onConflict: 'user_id'
    })

  if (error) {
    console.error('Error updating subscription:', error)
    return null
  }

  console.log('✅ Subscription ACTIVATED for user:', user.id, user.email)

  // Record initial payment (Pro is $9.99)
  await recordPayment(supabase, {
    userId: user.id,
    amount: 9.99,
    currency: 'USD',
    paymentType: 'subscription_initial',
    transactionId: null,
    subscriptionId: subscriptionId,
    status: 'completed',
    description: 'MobileCLI Pro subscription activated'
  })

  return user.id
}

/**
 * Handle subscription updated
 */
async function handleSubscriptionUpdated(supabase: any, resource: any): Promise<string | null> {
  const customId = resource?.custom_id
  const subscriberEmail = resource?.subscriber?.email_address
  const subscriptionId = resource?.id
  const status = resource?.status

  const user = await findUser(supabase, customId, subscriberEmail)
  if (!user) return null

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
    return null
  }

  console.log('Subscription updated for user:', user.id)
  return user.id
}

/**
 * Handle subscription cancelled
 */
async function handleSubscriptionCancelled(supabase: any, resource: any): Promise<string | null> {
  const customId = resource?.custom_id
  const subscriberEmail = resource?.subscriber?.email_address

  const user = await findUser(supabase, customId, subscriberEmail)
  if (!user) return null

  const { error } = await supabase
    .from('subscriptions')
    .update({
      status: 'cancelled',
      cancelled_at: new Date().toISOString(),
      cancel_reason: 'User cancelled via PayPal',
      updated_at: new Date().toISOString()
    })
    .eq('user_id', user.id)

  if (error) {
    console.error('Error cancelling subscription:', error)
    return null
  }

  console.log('Subscription cancelled for user:', user.id)
  return user.id
}

/**
 * Handle subscription suspended
 */
async function handleSubscriptionSuspended(supabase: any, resource: any): Promise<string | null> {
  const customId = resource?.custom_id
  const subscriberEmail = resource?.subscriber?.email_address

  const user = await findUser(supabase, customId, subscriberEmail)
  if (!user) return null

  const { error } = await supabase
    .from('subscriptions')
    .update({
      status: 'suspended',
      updated_at: new Date().toISOString()
    })
    .eq('user_id', user.id)

  if (error) {
    console.error('Error suspending subscription:', error)
    return null
  }

  console.log('Subscription suspended for user:', user.id)
  return user.id
}

/**
 * Handle subscription expired
 */
async function handleSubscriptionExpired(supabase: any, resource: any): Promise<string | null> {
  const customId = resource?.custom_id
  const subscriberEmail = resource?.subscriber?.email_address

  const user = await findUser(supabase, customId, subscriberEmail)
  if (!user) return null

  const { error } = await supabase
    .from('subscriptions')
    .update({
      status: 'expired',
      updated_at: new Date().toISOString()
    })
    .eq('user_id', user.id)

  if (error) {
    console.error('Error expiring subscription:', error)
    return null
  }

  console.log('Subscription expired for user:', user.id)
  return user.id
}

/**
 * Handle payment failed
 */
async function handlePaymentFailed(supabase: any, resource: any): Promise<string | null> {
  const customId = resource?.custom_id
  const subscriberEmail = resource?.subscriber?.email_address
  const subscriptionId = resource?.id

  const user = await findUser(supabase, customId, subscriberEmail)
  if (!user) return null

  // Update subscription with payment failed timestamp
  const { error } = await supabase
    .from('subscriptions')
    .update({
      payment_failed_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    })
    .eq('user_id', user.id)

  if (error) {
    console.error('Error updating payment failed:', error)
    return null
  }

  // Record failed payment
  await recordPayment(supabase, {
    userId: user.id,
    amount: 9.99,
    currency: 'USD',
    paymentType: 'subscription_renewal',
    transactionId: null,
    subscriptionId: subscriptionId,
    status: 'failed',
    description: 'Payment failed - user needs to update payment method'
  })

  console.log('Payment failed recorded for user:', user.id)
  return user.id
}

/**
 * Handle one-time payment completed (subscription renewal)
 */
async function handlePaymentCompleted(supabase: any, resource: any): Promise<string | null> {
  const customId = resource?.custom_id
  const payerEmail = resource?.payer?.email_address
  const transactionId = resource?.id
  const billingAgreementId = resource?.billing_agreement_id

  // Get amount from resource
  const amount = parseFloat(resource?.amount?.total || '9.99')
  const currency = resource?.amount?.currency || 'USD'

  const user = await findUser(supabase, customId, payerEmail)
  if (!user) return null

  const expiresAt = new Date()
  expiresAt.setDate(expiresAt.getDate() + 30)

  const { error } = await supabase
    .from('subscriptions')
    .upsert({
      user_id: user.id,
      status: 'active',
      paypal_subscription_id: billingAgreementId,
      expires_at: expiresAt.toISOString(),
      last_payment_at: new Date().toISOString(),
      payment_failed_at: null, // Clear any previous failure
      updated_at: new Date().toISOString()
    }, {
      onConflict: 'user_id'
    })

  if (error) {
    console.error('Error activating from payment:', error)
    return null
  }

  console.log('Subscription activated from payment for user:', user.id)

  // Record the payment
  await recordPayment(supabase, {
    userId: user.id,
    amount: amount,
    currency: currency,
    paymentType: 'subscription_renewal',
    transactionId: transactionId,
    subscriptionId: billingAgreementId,
    status: 'completed',
    description: 'MobileCLI Pro subscription renewal'
  })

  return user.id
}
