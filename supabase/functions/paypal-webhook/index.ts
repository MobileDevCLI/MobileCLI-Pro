// MobileCLI Pro - PayPal IPN Webhook Handler
// Deploy to: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/functions
//
// This function receives PayPal Instant Payment Notifications (IPN)
// and updates the subscription status in the database.

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
    // Get request body (PayPal sends form-urlencoded data)
    const body = await req.text()
    const params = new URLSearchParams(body)

    // PayPal IPN fields
    const txnType = params.get('txn_type')
    const payerEmail = params.get('payer_email')
    const subscriptionId = params.get('subscr_id')
    const paymentStatus = params.get('payment_status')
    const mcGross = params.get('mc_gross')
    const mcCurrency = params.get('mc_currency')

    console.log('PayPal IPN received:', {
      txn_type: txnType,
      payer_email: payerEmail,
      subscr_id: subscriptionId,
      payment_status: paymentStatus,
      amount: mcGross,
      currency: mcCurrency
    })

    // Verify this is a legitimate PayPal request (optional but recommended)
    // In production, you should verify by posting back to PayPal:
    // https://ipnpb.paypal.com/cgi-bin/webscr?cmd=_notify-validate&{original_params}

    // Create Supabase client with service role (bypasses RLS)
    const supabaseUrl = Deno.env.get('SUPABASE_URL')
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')

    if (!supabaseUrl || !supabaseServiceKey) {
      console.error('Missing Supabase environment variables')
      return new Response('Configuration error', { status: 500, headers: corsHeaders })
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    // Handle different transaction types
    switch (txnType) {
      case 'subscr_signup':
        // New subscription created
        console.log('New subscription signup:', payerEmail)
        await handleSubscriptionSignup(supabase, payerEmail, subscriptionId)
        break

      case 'subscr_payment':
        // Recurring payment received
        console.log('Subscription payment received:', payerEmail, mcGross)
        await handleSubscriptionPayment(supabase, payerEmail, subscriptionId)
        break

      case 'subscr_cancel':
        // Subscription cancelled
        console.log('Subscription cancelled:', payerEmail)
        await handleSubscriptionCancel(supabase, payerEmail, subscriptionId)
        break

      case 'subscr_eot':
        // Subscription expired (end of term)
        console.log('Subscription expired:', payerEmail)
        await handleSubscriptionExpired(supabase, payerEmail, subscriptionId)
        break

      case 'subscr_failed':
        // Payment failed
        console.log('Subscription payment failed:', payerEmail)
        // Don't immediately cancel - PayPal will retry
        break

      default:
        console.log('Unhandled transaction type:', txnType)
    }

    // PayPal expects a 200 response
    return new Response('OK', { status: 200, headers: corsHeaders })

  } catch (error) {
    console.error('Webhook error:', error)
    return new Response('Internal error', { status: 500, headers: corsHeaders })
  }
})

/**
 * Handle new subscription signup
 */
async function handleSubscriptionSignup(
  supabase: any,
  payerEmail: string | null,
  subscriptionId: string | null
) {
  if (!payerEmail) {
    console.error('No payer email provided')
    return
  }

  // Find user by email
  const { data: users, error: userError } = await supabase.auth.admin.listUsers()

  if (userError) {
    console.error('Error listing users:', userError)
    return
  }

  const user = users.users.find((u: any) =>
    u.email?.toLowerCase() === payerEmail.toLowerCase()
  )

  if (!user) {
    console.error('User not found for email:', payerEmail)
    // Store for later matching or manual activation
    return
  }

  // Update subscription to active
  const { error } = await supabase
    .from('subscriptions')
    .upsert({
      user_id: user.id,
      status: 'active',
      paypal_subscription_id: subscriptionId,
      expires_at: getNextMonthDate(),
      updated_at: new Date().toISOString()
    }, {
      onConflict: 'user_id'
    })

  if (error) {
    console.error('Error updating subscription:', error)
  } else {
    console.log('Subscription activated for user:', user.id)
  }
}

/**
 * Handle recurring subscription payment
 */
async function handleSubscriptionPayment(
  supabase: any,
  payerEmail: string | null,
  subscriptionId: string | null
) {
  if (!payerEmail) {
    console.error('No payer email provided')
    return
  }

  // Find user by email
  const { data: users, error: userError } = await supabase.auth.admin.listUsers()

  if (userError) {
    console.error('Error listing users:', userError)
    return
  }

  const user = users.users.find((u: any) =>
    u.email?.toLowerCase() === payerEmail.toLowerCase()
  )

  if (!user) {
    console.error('User not found for email:', payerEmail)
    return
  }

  // Extend subscription by 30 days
  const { error } = await supabase
    .from('subscriptions')
    .upsert({
      user_id: user.id,
      status: 'active',
      paypal_subscription_id: subscriptionId,
      expires_at: getNextMonthDate(),
      updated_at: new Date().toISOString()
    }, {
      onConflict: 'user_id'
    })

  if (error) {
    console.error('Error updating subscription:', error)
  } else {
    console.log('Subscription extended for user:', user.id)
  }
}

/**
 * Handle subscription cancellation
 */
async function handleSubscriptionCancel(
  supabase: any,
  payerEmail: string | null,
  subscriptionId: string | null
) {
  if (!payerEmail) return

  const { data: users } = await supabase.auth.admin.listUsers()
  const user = users?.users.find((u: any) =>
    u.email?.toLowerCase() === payerEmail.toLowerCase()
  )

  if (!user) return

  // Mark as cancelled (user keeps access until expires_at)
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
 * Handle subscription expiration
 */
async function handleSubscriptionExpired(
  supabase: any,
  payerEmail: string | null,
  subscriptionId: string | null
) {
  if (!payerEmail) return

  const { data: users } = await supabase.auth.admin.listUsers()
  const user = users?.users.find((u: any) =>
    u.email?.toLowerCase() === payerEmail.toLowerCase()
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
 * Get date 30 days from now
 */
function getNextMonthDate(): string {
  const date = new Date()
  date.setDate(date.getDate() + 30)
  return date.toISOString()
}
