# MobileCLI - Complete Roadmap & Status

**Last Updated:** January 20, 2026
**Purpose:** Master reference for developers, lawyers, and future Claude sessions

---

## CRITICAL: READ THIS FIRST

This document contains everything needed to continue development of MobileCLI. If you're a new developer or AI assistant picking up this project, read this entire document before making any changes.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Current Status](#2-current-status)
3. [What's 100% Complete](#3-whats-100-complete)
4. [What Needs To Be Done](#4-what-needs-to-be-done)
5. [Stripe Setup Instructions](#5-stripe-setup-instructions)
6. [Security Concerns](#6-security-concerns)
7. [Legal & IP Status](#7-legal--ip-status)
8. [Repository Structure](#8-repository-structure)
9. [Key Credentials & Accounts](#9-key-credentials--accounts)
10. [Future Plans](#10-future-plans)

---

## 1. Project Overview

**MobileCLI** is an AI-powered Android terminal application that provides root-equivalent access through 79 Android permissions. It integrates Claude Code, Gemini CLI, and Codex CLI.

### Business Model
- **Free Tier:** 7-day trial
- **Pro:** $10/month or $100/year
- **Team:** $20/month or $200/year per user

### Ultimate Goal
Sell to the public, gain millions of users, then sell the company to Anthropic, Google, or similar tech giant.

---

## 2. Current Status

**Last Updated:** January 20, 2026 @ 5:20 PM

### Overall Progress: ~95% Complete

| Component | Status | Notes |
|-----------|--------|-------|
| Android App | ✅ 100% | v2.0.0-beta.26, Auto Mode + context menu fixed |
| Website | ✅ 100% | Live at mobilecli.com, APK download added |
| Supabase Backend | ✅ 95% | Schema ready, needs table verification |
| Stripe Integration | ⚠️ 80% | Code complete, needs API keys |
| Legal Documentation | ✅ 100% | All docs complete |
| Play Store Account | ✅ PAID | $25 paid, identity verification in progress |
| Play Store Listing | ⏳ 0% | Waiting for identity verification |
| iOS Version | ❌ 0% | Future - needs iPad for development |

### Account IDs & Credentials
| Service | Status | ID/URL |
|---------|--------|--------|
| Play Console | ✅ Created | Account ID: 4824773357272454397 |
| Play Console Email | ✅ | [your-private-email] |
| Supabase Project | ✅ | mwxlguqukyfberyhtkmg.supabase.co |
| Website | ✅ Live | https://mobilecli.com |
| GitHub Org | ✅ | https://github.com/MobileDevCLI |
| APK Download | ✅ Live | https://mobilecli.com/downloads/MobileCLI-latest.apk |

---

## 3. What's 100% Complete

### Android App (MobileCLI-CLEAN repo)
- [x] 3-stage Setup Wizard (Legal, Permissions, Download, AI Selection)
- [x] 75+ API commands (NOT using Termux:API - original implementation)
- [x] Terminal rendering with session persistence
- [x] Proprietary Activity Manager (replaces GPL am.apk)
- [x] URL opening system with overlay permission
- [x] Wake lock and Power Mode
- [x] Claude/Gemini/Codex integration via official CLI tools
- [x] Browser OAuth authentication for AI tools
- [x] Background setup (continues when app minimized)
- [x] Text selection with long-press
- [x] Navigation drawer with all controls

### Website (website repo)
- [x] Landing page with Matrix rain design
- [x] Login page with Supabase auth
- [x] Dashboard page
- [x] Pricing page with tier comparison
- [x] Privacy policy page
- [x] Terms of service page
- [x] DMCA page
- [x] 30+ additional pages (demos, labs, games, etc.)
- [x] Vercel deployment configured

### Supabase Backend
- [x] Project created: `mwxlguqukyfberyhtkmg.supabase.co`
- [x] SQL schema written (supabase-setup.sql)
- [x] Tables: waitlist, profiles, subscriptions, app_licenses, usage_events
- [x] Functions: verify_license, generate_license_key, register_device
- [x] Edge Functions: create-checkout, customer-portal, stripe-webhook
- [x] Auth configured with OAuth (Google, GitHub)
- [x] RLS policies configured

### Legal Documentation
- [x] LICENSE (Proprietary)
- [x] THIRD_PARTY_LICENSES.md (Apache 2.0 attribution)
- [x] LEGAL_SUMMARY.md (Executive summary for lawyers)
- [x] IP.md (Intellectual property documentation)
- [x] COMPLETE_SYSTEM_DOCUMENTATION.md (Technical reference)
- [x] Privacy policy in app
- [x] Terms of service in Setup Wizard

---

## 4. What Needs To Be Done

### COMPLETED TODAY (January 20, 2026)
- [x] ✅ **Vercel CLI drawer button** - One-tap install + OAuth login in new terminal tab
- [x] ✅ **GitHub CLI drawer button** - One-tap install + OAuth login in new terminal tab
- [x] ✅ Claude can now use `vercel` and `gh` commands immediately after setup
- [x] ✅ **Fixed context menu** - "More" button now works, shows Copy All/Paste/Select All/New Session/Kill Session/Reset Terminal
- [x] ✅ **Renamed Power Mode → Auto Mode** - Clearer name for users
- [x] ✅ **Auto Mode actually works** - Added bash function that passes `--dangerously-skip-permissions` to Claude

### COMPLETED (January 19, 2026)
- [x] ✅ Play Store Account - $25 PAID
- [x] ✅ APK hosted on website - https://mobilecli.com/downloads/MobileCLI-latest.apk
- [x] ✅ pro-preview.html updated with v2.0.0 download button
- [x] ✅ GitHub release created (v2.0.0-beta.20)

### IN PROGRESS NOW
- [ ] ⏳ Play Console Identity Verification (uploading ID document)

### AFTER IDENTITY VERIFICATION (What Happens Next)

Once Google verifies your identity (usually 1-48 hours), you can:

#### Step 1: Create App Listing in Play Console
```
1. Go to Play Console → Create app
2. Fill in:
   - App name: MobileCLI
   - Default language: English
   - App or game: App
   - Free or paid: Free (with in-app purchases)
3. Accept policies
```

#### Step 2: Store Listing (What Users See)
```
Required:
- Short description (80 chars): "AI-powered terminal with Claude, Gemini & Codex"
- Full description (4000 chars): [We'll write this together]
- App icon: 512x512 PNG
- Feature graphic: 1024x500 PNG
- Screenshots: At least 2 phone screenshots
- Privacy policy URL: https://mobilecli.com/privacy.html
```

#### Step 3: Upload APK/App Bundle
```
1. Go to Release → Production → Create new release
2. Upload signed APK or AAB (Android App Bundle)
3. Add release notes
4. Review and rollout
```

#### Step 4: Content Rating
```
1. Fill out questionnaire about app content
2. Topics: violence, language, mature content, etc.
3. Get IARC rating (usually "Everyone" or "Teen")
```

#### Step 5: Submit for Review
```
- Google reviews the app (usually 1-7 days for new apps)
- They check for policy violations
- Once approved → LIVE on Play Store
```

---

### REMAINING TASKS (Can Do While Waiting)

#### 4.1 Stripe Setup (15 minutes)
```
Status: Code complete, needs API credentials

Steps:
1. Go to https://stripe.com and sign in (or create account)
2. Go to Dashboard → Products → Add Product
3. Create these products:
   - "MobileCLI Pro Monthly" - $10/month recurring
   - "MobileCLI Pro Yearly" - $100/year recurring
   - "MobileCLI Team Monthly" - $20/month recurring
   - "MobileCLI Team Yearly" - $200/year recurring
4. Copy each Price ID (starts with "price_")
5. Go to Developers → API Keys
6. Copy Publishable Key (starts with "pk_test_" or "pk_live_")
7. Update website/js/stripe-config.js with these values
```

#### 4.2 Verify Supabase Tables (5 minutes)
```
Status: SQL file ready, verify tables exist

Steps:
1. Go to https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg
2. Click "Table Editor" in left sidebar
3. Check if these tables exist:
   - waitlist
   - profiles
   - subscriptions
   - app_licenses
   - usage_events
4. If missing, go to SQL Editor and run website/supabase-setup.sql
```

#### 4.3 Generate Release Signing Key (5 minutes)
```
Status: Not started - REQUIRED for Play Store

Steps:
1. Open terminal in MobileCLI-CLEAN directory
2. Run: keytool -genkey -v -keystore mobilecli-release.keystore -alias mobilecli -keyalg RSA -keysize 2048 -validity 10000
3. Save password securely (DO NOT COMMIT TO GIT)
4. Update app/build.gradle with signing config
5. Build release APK: ./gradlew assembleRelease

IMPORTANT: Keep this keystore file FOREVER. If you lose it, you cannot
update your app on Play Store - you'd have to publish as a new app.
```

#### 4.4 Security Audit - Remove Personal Data
```
Status: CRITICAL - Do before public Play Store release

BEFORE PUBLIC RELEASE:
1. Search entire codebase for personal info
2. Remove any hardcoded emails, names, paths
3. Remove any conversation logs or AI chat history
4. Check for any API keys that shouldn't be public
5. Review all comments for personal references
6. Check git history for sensitive commits
```

#### 4.7 Push Privacy Policy to Live Site
```
Status: Page exists, verify it's accessible

Steps:
1. Ensure privacy.html is deployed to Vercel
2. Test: https://mobilecli.com/privacy.html
3. Update Play Store listing with this URL
```

### MEDIUM TERM (This Month)

#### 4.8 Play Store Submission
```
Requirements:
- Release APK (signed)
- App icon (512x512)
- Feature graphic (1024x500)
- Screenshots (phone and tablet)
- Short description (80 chars)
- Full description (4000 chars)
- Privacy policy URL
- Content rating questionnaire
```

#### 4.9 Marketing Launch
```
Channels:
- Twitter/X announcement
- Hacker News "Show HN" post
- Reddit (r/androiddev, r/programming, r/artificial)
- YouTube demo video
- Product Hunt launch
```

### LONG TERM (Future)

#### 4.10 iOS Version
```
Status: Not started
Requires: iPad for development
Tech: Swift/Kotlin Multiplatform
Timeline: After Android success
```

#### 4.11 Additional Features
```
- Crash reporting (Firebase Crashlytics)
- Analytics (Mixpanel/Amplitude)
- Push notifications
- Team collaboration features
- Custom AI model integration
```

---

## 5. Stripe Setup Instructions

### File to Update
`website/js/stripe-config.js`

### Current State (needs updating):
```javascript
const STRIPE_PUBLIC_KEY = 'pk_test_YOUR_STRIPE_PUBLISHABLE_KEY';  // CHANGE THIS

const STRIPE_PRICES = {
    pro_monthly: 'price_YOUR_PRO_MONTHLY_PRICE_ID',      // CHANGE THIS
    pro_yearly: 'price_YOUR_PRO_YEARLY_PRICE_ID',        // CHANGE THIS
    team_monthly: 'price_YOUR_TEAM_MONTHLY_PRICE_ID',    // CHANGE THIS
    team_yearly: 'price_YOUR_TEAM_YEARLY_PRICE_ID'       // CHANGE THIS
};
```

### After You Create Stripe Products:
Replace the placeholder values with your actual keys from Stripe Dashboard.

### Stripe Webhook Setup
1. In Stripe Dashboard → Developers → Webhooks
2. Add endpoint: `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/stripe-webhook`
3. Select events:
   - `customer.subscription.created`
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
   - `invoice.paid`
   - `invoice.payment_failed`
4. Copy webhook signing secret
5. Add to Supabase Edge Function secrets

---

## 6. Security Concerns

### CRITICAL: Personal Data Removal

Before public release, audit for:

1. **Conversation History**
   - Any AI chat logs embedded in code
   - Comments referencing personal conversations
   - Debug logs with personal info

2. **Personal Identifiers**
   - Email addresses
   - Phone numbers
   - Physical addresses
   - Social media handles
   - Real names

3. **File Paths**
   - `C:\Users\USERNAME\...` → Should be relative paths
   - Any absolute paths with usernames

4. **API Keys & Secrets**
   - Supabase service key (NOT anon key - anon is public)
   - Stripe secret key
   - Any OAuth client secrets

5. **Git History**
   - May contain sensitive data from past commits
   - Consider: `git filter-branch` to clean history
   - Or: Start fresh repo for public release

### App Security Model

MobileCLI is intentionally powerful (79 permissions). This is a feature, not a bug. But users must understand:

1. **User Consent:** Setup Wizard requires explicit agreement
2. **Local Processing:** No data sent to MobileCLI servers
3. **AI Provider Data:** Queries go to Anthropic/Google/OpenAI
4. **Permission Scope:** Users grant permissions knowingly

### Recommendations

1. Add security disclosure page to website
2. Document all permissions and why they're needed
3. Consider penetration testing before major launch
4. Set up bug bounty program after launch

---

## 7. Legal & IP Status

### Ownership Summary

| Component | Lines | Owner | License |
|-----------|-------|-------|---------|
| MobileCLI App Code | ~9,000+ | MobileCLI Team | Proprietary |
| terminal-view | ~2,500 | Termux/Jack Palevich | Apache 2.0 |
| terminal-emulator | ~2,500 | Termux/Jack Palevich | Apache 2.0 |
| Website Code | ~50,000+ | MobileCLI Team | Proprietary |
| Supabase Schema | ~500 | MobileCLI Team | Proprietary |

### GPL Status: NONE

- No GPL code bundled in APK
- Proprietary Activity Manager replaces GPL am.apk
- Runtime downloads (bash, etc.) are user-initiated

### Required Attribution (Apache 2.0)

The Open Source Licenses screen in the app MUST remain with:
- Jack Palevich credited as original author
- Fredrik Fornwall credited for adaptation
- Apache 2.0 license referenced

### Legal Documents Location

```
MobileCLI-CLEAN/
├── LICENSE                          # Proprietary license
├── THIRD_PARTY_LICENSES.md          # Apache 2.0 components
├── LEGAL_SUMMARY.md                 # Executive summary
├── IP.md                            # IP documentation
└── COMPLETE_SYSTEM_DOCUMENTATION.md # Technical reference
```

### Commercial Rights

- ✅ Can be sold commercially
- ✅ Can be distributed closed-source
- ✅ No royalty obligations
- ✅ Can be acquired by third party
- ✅ All original code owned by MobileCLI Team

---

## 8. Repository Structure

### GitHub Organization: MobileDevCLI

| Repository | Purpose | Status |
|------------|---------|--------|
| MobileCLI-Alpha | Main Android app source | Active |
| website | mobilecli.com source | Active |
| MobileCLI-Lab | Experimental features | Archive |
| MobileCLI-Production | Production builds | Archive |
| MobileCLI-Source | Legacy source | Archive |

### Local Directories

```
C:\Users\USERNAME\Downloads\
├── Phone Link\
│   └── MobileCLI-CLEAN\          # Main app development
├── MobileCLI-APKs\               # Built APKs + docs
└── MobileCLI-Website\            # Website source (cloned)
```

### Key Files

**Android App:**
- `app/src/main/java/com/termux/MainActivity.kt` - Terminal UI
- `app/src/main/java/com/termux/SetupWizard.kt` - Setup flow
- `app/src/main/java/com/termux/BootstrapInstaller.kt` - Bootstrap
- `app/src/main/java/com/termux/TermuxApiReceiver.kt` - API commands

**Website:**
- `js/supabase-config.js` - Supabase credentials (CONFIGURED)
- `js/stripe-config.js` - Stripe credentials (NEEDS UPDATE)
- `supabase-setup.sql` - Database schema
- `supabase/functions/` - Edge functions

---

## 9. Key Credentials & Accounts

### Supabase (CONFIGURED)
```
Project URL: https://mwxlguqukyfberyhtkmg.supabase.co
Anon Key: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Dashboard: https://supabase.com/dashboard/project/mwxlguqukyfberyhtkmg
```

### Stripe (NEEDS SETUP)
```
Dashboard: https://dashboard.stripe.com
Status: Account needed or keys needed
```

### Vercel (CONFIGURED)
```
Project: website-neon-kappa-34.vercel.app
Domain: mobilecli.com
```

### GitHub
```
Organization: https://github.com/MobileDevCLI
Main Repo: https://github.com/MobileDevCLI/MobileCLI-Alpha
```

### Play Store (NEEDS SETUP)
```
Console: https://play.google.com/console
Status: $25 account needed
```

---

## 10. Future Plans

### Phase 1: Launch (Current)
1. Complete Stripe integration
2. Submit to Play Store
3. Soft launch to early adopters

### Phase 2: Growth (1-3 months)
1. Marketing push (Twitter, HN, Reddit, YouTube)
2. Gather user feedback
3. Bug fixes and polish
4. Build user community

### Phase 3: Scale (3-6 months)
1. iOS version development
2. Team features
3. Enterprise sales
4. Partnership discussions

### Phase 4: Exit (6-12 months)
1. Demonstrate traction metrics
2. Revenue growth
3. Acquisition discussions with:
   - Anthropic (Claude integration)
   - Google (Gemini integration)
   - Microsoft/OpenAI (Codex integration)
   - Other strategic acquirers

---

## Quick Reference Commands

### Build Debug APK
```bash
cd MobileCLI-CLEAN
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK
```bash
cd MobileCLI-CLEAN
./gradlew assembleRelease
# Requires signing key configured
```

### Push to GitHub
```bash
git add .
git commit -m "Description of changes"
git push origin master
```

### Deploy Website
```bash
# Automatic via Vercel on git push
git push origin main
```

---

## Contact & Support

**Website:** https://mobilecli.com
**GitHub:** https://github.com/MobileDevCLI
**Email:** Contact through website

---

**This document should be updated whenever significant changes are made to the project.**

*Last Updated: January 20, 2026 by Claude Opus 4.5*
