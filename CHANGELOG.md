# Changelog

All notable changes to MobileCLI Pro are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0-rc.3] - 2026-01-25 (Release Candidate 3)

### Fixed
- AccountActivity delete account email changed from `support@mobilecli.com` to `mobiledevcli@gmail.com`

### Changed
- All support email references now use correct email address

---

## [2.0.0-rc.2] - 2026-01-25 (Release Candidate 2)

### Fixed
- Support email changed from `support@mobilecli.com` to `mobiledevcli@gmail.com`

### Added
- Webhook logging to `webhook_logs` table for debugging payment issues
- Payment history recording to `payment_history` table for audit trail
- Processing result tracking in webhook handler

### Changed
- PayPal webhook now logs all events before processing
- Webhook marks events as processed with result status

---

## [2.0.0-rc.1] - 2026-01-22 (Release Candidate)

### Security
- **CRITICAL FIX**: Removed "Skip Login (Demo Mode)" button that bypassed authentication
- All users must now authenticate to access the app

### Added
- Complete subscription system with Supabase backend
- PayPal IPN webhook for automatic payment activation (`supabase/functions/paypal-webhook/`)
- Webhook setup documentation (`docs/WEBHOOK_SETUP.md`)
- Subscription setup documentation (`docs/SUBSCRIPTION_SETUP.md`)
- Terms of Service (`docs/TERMS_OF_SERVICE.md`)
- Privacy Policy (`docs/PRIVACY_POLICY.md`)
- Legal Disclaimers (`docs/LEGAL_DISCLAIMERS.md`)
- Row Level Security (RLS) for subscriptions table
- Automatic trial subscription (7 days) on signup
- Encrypted local caching for offline access

### Fixed
- java.time.Instant API 26+ requirement - now works on Android 7.0+ (API 24)
- Added registerDevice() backward compatibility method
- PayPal checkout return handling with proper delay

### Changed
- LicenseManager now queries Supabase subscriptions table
- PaywallActivity onResume increased delay for webhook processing
- Version bumped to Release Candidate

---

## [2.0.0-beta.34-build13] - 2026-01-22

### Fixed
- PayPal payment URL format changed to NCP format (`/ncp/payment/`)
- PayPal subscription now fully working with Business account

### Changed
- PayPal Button ID updated to `DHCKPWE3PJ684` (Business account button)

---

## [2.0.0-beta.34-build12] - 2026-01-22

### Changed
- Updated PayPal button to Business account button
- Updated all documentation with new button ID

---

## [2.0.0-beta.34-build11] - 2026-01-22

### Added
- Master configuration documentation (`docs/MASTER_CONFIG.md`)
- Comprehensive security guidelines

---

## [2.0.0-beta.34-build9] - 2026-01-22

### Added
- PayPal subscription integration ($15/month)
- PaywallActivity with subscribe button
- Payment documentation (`docs/PAYMENTS_SETUP.md`)

### Changed
- Checkout flow now uses PayPal instead of website redirect

---

## [2.0.0-beta.34-build7] - 2026-01-22

### Fixed
- Google OAuth PKCE flow - proper code exchange
- Deep link handling with `exchangeCodeForSession()`
- Auth configuration with scheme and host parameters

### Added
- Authentication documentation (`docs/AUTH_SETUP.md`)

---

## [2.0.0-beta.34-build6] - 2026-01-22

### Added
- Deep link intent-filter for OAuth callback
- `onNewIntent()` handler in LoginActivity
- OAuth callback URL: `com.termux://login-callback`

### Changed
- LoginActivity launch mode set to `singleTask`
- LoginActivity exported set to `true`

---

## [2.0.0-beta.34-build5] - 2026-01-22

### Added
- Google OAuth login button (enabled)
- Google provider configuration in Supabase

### Fixed
- Removed "Google login requires configuration" toast

---

## [2.0.0-beta.34-build4] - 2026-01-22

### Added
- GitHub Actions workflow for automated builds
- Automatic GitHub Releases with APK artifacts

### Changed
- CI workflow removes ARM-specific aapt2 override

---

## [2.0.0-beta.34-build3] - 2026-01-22

### Added
- "Skip for now" demo mode button on login screen

---

## [2.0.0-beta.34] - 2026-01-22

### Added
- Complete authentication system
  - LoginActivity with email/password
  - SignUp functionality
  - Google OAuth (initial setup)
- SupabaseClient singleton for auth management
- LicenseManager for subscription verification
- PaywallActivity for subscription prompts
- SplashActivity for initial loading

### Technical
- Supabase SDK integration (v2.0.4)
- Ktor client for Android
- PKCE OAuth flow support
- Encrypted SharedPreferences for license storage

---

## [1.x.x] - Previous Versions

### Original MobileCLI
- Terminal emulator functionality
- Termux integration
- Basic app structure

---

## Version Categories

### Added
New features added to the project.

### Changed
Changes in existing functionality.

### Deprecated
Features that will be removed in upcoming releases.

### Removed
Features removed in this release.

### Fixed
Bug fixes.

### Security
Security vulnerability fixes.

---

## Links

- [GitHub Releases](https://github.com/MobileDevCLI/MobileCLI-Pro/releases)
- [GitHub Repository](https://github.com/MobileDevCLI/MobileCLI-Pro)
