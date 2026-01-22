# MobileCLI Pro - Authentication Setup Guide

This document explains how the authentication system works and how to configure it.

## Overview

MobileCLI Pro uses **Supabase** for authentication with two login methods:
- Email + Password
- Google OAuth

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   App       │────▶│   Google    │────▶│  Supabase   │
│             │◀────│   OAuth     │◀────│  Callback   │
└─────────────┘     └─────────────┘     └─────────────┘
      │                                        │
      │         com.termux://login-callback    │
      └────────────────────────────────────────┘
```

### OAuth Flow
1. User taps "Continue with Google" in app
2. App opens Google sign-in page (via Supabase)
3. User authenticates with Google
4. Google redirects to Supabase callback URL (HTTPS)
5. Supabase redirects to app using custom scheme
6. App receives authorization code and exchanges it for session

## Service Configuration

### Supabase Project

- **Project URL:** `https://mwxlguqukyfberyhtkmg.supabase.co`
- **Dashboard:** https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg

#### Supabase Settings

1. **Authentication → Providers → Google**
   - Enabled: Yes
   - Client ID: (from Google Cloud Console)
   - Client Secret: (from Google Cloud Console)

2. **Authentication → URL Configuration**
   - Site URL: `com.termux://login-callback`
   - Redirect URLs: `com.termux://login-callback`

### Google Cloud Console

- **Console:** https://console.cloud.google.com/apis/credentials
- **Project:** MobileCLI Pro (or your project name)

#### OAuth 2.0 Client Setup

1. Create OAuth 2.0 Client ID (Type: **Web application**)
2. Authorized redirect URIs:
   - `https://mwxlguqukyfberyhtkmg.supabase.co/auth/v1/callback`

**IMPORTANT:** Do NOT add custom schemes (com.termux://) to Google - Google only accepts HTTPS URLs.

## App Configuration

### AndroidManifest.xml

The LoginActivity must handle deep links:

```xml
<activity
    android:name=".auth.LoginActivity"
    android:exported="true"
    android:launchMode="singleTask">

    <!-- Deep link for OAuth callback -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="com.termux"
            android:host="login-callback" />
    </intent-filter>
</activity>
```

### SupabaseClient.kt

Configure Auth with PKCE flow and custom scheme:

```kotlin
install(Auth) {
    flowType = FlowType.PKCE
    scheme = "com.termux"
    host = "login-callback"
}
```

Handle deep link callback:

```kotlin
suspend fun handleDeepLink(uri: Uri): Boolean {
    val code = uri.getQueryParameter("code")
    if (code != null) {
        auth.exchangeCodeForSession(code)
        return isLoggedIn()
    }
    return false
}
```

### LoginActivity.kt

Handle the OAuth redirect in `onNewIntent`:

```kotlin
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.data?.let { uri ->
        lifecycleScope.launch {
            val success = SupabaseClient.handleDeepLink(uri)
            if (success) {
                onLoginSuccess()
            }
        }
    }
}
```

## Credentials Location

| Credential | Location |
|------------|----------|
| Supabase URL | `SupabaseClient.kt` (line 24) |
| Supabase Anon Key | `SupabaseClient.kt` (line 25) |
| Google Client ID | Supabase Dashboard → Auth → Providers → Google |
| Google Client Secret | Supabase Dashboard → Auth → Providers → Google |

## Troubleshooting

### "Site can't be reached" after Google sign-in
- Check that Supabase has `com.termux://login-callback` in Redirect URLs
- Check that Google has the Supabase callback URL (not custom scheme)

### "Invalid Redirect" error in Google Console
- Google only accepts `http://` or `https://` schemes
- Custom schemes go in Supabase, not Google

### App doesn't receive callback
- Verify AndroidManifest has the intent-filter with correct scheme/host
- Verify LoginActivity has `android:launchMode="singleTask"`
- Verify LoginActivity has `android:exported="true"`

### Login succeeds but app doesn't proceed
- Check `onNewIntent` is calling `handleDeepLink`
- Check `exchangeCodeForSession` is being called with the code
- Check logs for errors: `adb logcat | grep LoginActivity`

## Testing

### Skip Login (Demo Mode)
For testing without authentication, tap "Skip for now (Demo mode)" on the login screen.

### Test Accounts
Create test accounts at: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/auth/users

---

## User Management - Where to Find Users

### Viewing All Users (Signups & Logins)

**Supabase Dashboard → Authentication → Users**
https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/auth/users

This shows:
- All registered users (email, Google OAuth, etc.)
- Sign up date/time
- Last sign in date/time
- Email confirmation status
- User ID (UUID)

### User Data You Can See

| Column | Description |
|--------|-------------|
| **Email** | User's email address |
| **Provider** | `email` or `google` |
| **Created** | When they signed up |
| **Last Sign In** | Most recent login |
| **User UID** | Unique identifier for database queries |

### Exporting User Data

1. Go to Supabase Dashboard → Authentication → Users
2. Users can be viewed in the table
3. For export, use the Supabase API or SQL Editor

### SQL Query to List Users

In Supabase SQL Editor (https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg/sql):

```sql
-- List all users with sign up and last login times
SELECT
  id,
  email,
  raw_user_meta_data->>'provider' as provider,
  created_at as signed_up,
  last_sign_in_at as last_login
FROM auth.users
ORDER BY created_at DESC;
```

### User Activity Monitoring

To track user activity over time, consider creating a custom `user_activity` table:

```sql
-- Create activity tracking table
CREATE TABLE user_activity (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id),
  action TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS
ALTER TABLE user_activity ENABLE ROW LEVEL SECURITY;
```

---

## Security Notes

- The Supabase anon key is safe to include in client code (protected by Row Level Security)
- Never commit the Supabase service_role key to the repository
- Google Client Secret is stored only in Supabase Dashboard (not in app code)

## Related Files

- `app/src/main/java/com/termux/auth/LoginActivity.kt` - Login UI and OAuth handling
- `app/src/main/java/com/termux/auth/SupabaseClient.kt` - Supabase client singleton
- `app/src/main/java/com/termux/auth/LicenseManager.kt` - License verification
- `app/src/main/java/com/termux/auth/PaywallActivity.kt` - Subscription paywall
- `app/src/main/AndroidManifest.xml` - Deep link configuration
- `app/src/main/res/layout/activity_login.xml` - Login screen layout
