# Changelog

All notable changes to MobileCLI Pro are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
