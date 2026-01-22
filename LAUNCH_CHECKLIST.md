# MobileCLI Launch Checklist & Security Setup

**Created:** January 19, 2026 @ 9:00 PM
**Timeline:** 2 weeks to production
**Goal:** Launch properly with security, privacy, and industry standards

---

## COMPLETED TODAY (January 19, 2026)

- [x] Play Console account created ($25 paid)
- [x] Account ID: 4824773357272454397
- [x] Identity verification PASSED
- [x] Phone verification PASSED
- [x] App "MobileCLI" created in Play Console
- [x] APK hosted on website: https://mobilecli.com/downloads/MobileCLI-latest.apk
- [x] GitHub release created: v2.0.0-beta.21
- [x] **SECURITY AUDIT COMPLETE** - All personal info removed from codebase
- [x] Documentation sanitized (no emails, usernames, paths exposed)
- [x] Codebase safe to make public

---

## SECURITY & PRIVACY SETUP (Do First)

### Your Identity Protection Strategy

| What | Private (Hidden) | Public (Visible) |
|------|------------------|------------------|
| Google Account | [your-private-email] | NEVER shown |
| Developer Name | - | "MobileCLI" |
| Contact Email | - | Need alias (see below) |
| Phone Number | Your real number | NEVER shown |
| Physical Address | - | Required - need solution |

### Action Items for Security

#### 1. Create Business Email Alias
```
Options:
- contact@mobilecli.com (if you have domain email)
- mobilecli.contact@gmail.com (free Gmail)
- Use Protonmail for extra privacy

This email will be PUBLIC on Play Store for user inquiries.
```

#### 2. Physical Address Solution
```
Google REQUIRES a public address. Options:

A) PO Box (~$20-100/year)
   - Check if your state allows for business use
   - Some states require physical address anyway

B) Registered Agent Service (~$50-150/year)
   - Gives you a real business address
   - Recommended: Northwest Registered Agent, Incfile
   - Bonus: Can use for LLC later

C) Virtual Office (~$30-100/month)
   - Regus, WeWork, etc.
   - Real address, mail forwarding

D) LLC with Registered Agent
   - Most professional for investors
   - ~$100-500 to form depending on state
   - Includes registered agent address

RECOMMENDATION: Start with Registered Agent service ($50-150/year)
- Protects your home address
- Looks professional
- Can upgrade to LLC later
```

#### 3. GitHub Security Audit
```
Before making repo public, check for:
- [ ] No personal email addresses in code
- [ ] No hardcoded paths with username (C:\Users\USERNAME\)
- [ ] No API keys or secrets
- [ ] No personal phone numbers
- [ ] No home address references
- [ ] Review git history for sensitive commits
```

---

## 14-DAY CLOSED TESTING REQUIREMENTS

Google requires new developers to complete closed testing before production.

### Requirements:
1. Upload APK to closed testing track
2. Add at least 12 testers
3. Testers must opt-in (accept invite)
4. Run test for minimum 14 days
5. Then apply for production access

### Tester Strategy (Need 12):
```
You can use multiple email addresses you control:
1. Your personal Gmail
2. [your-private-email]
3. Any other emails you have
4-12. Ask friends/family for their emails

Testers receive email → Click link → Install from Play Store
They don't need to actively use it, just install and opt-in.
```

### Timeline:
```
Day 1 (Tomorrow): Upload APK, add testers
Day 2-14: Testers install, you gather feedback
Day 15: Apply for production access
Day 16-22: Google reviews (~1-7 days)
Day 22+: LIVE on Play Store
```

---

## STORE LISTING REQUIREMENTS

### Required Assets:

| Asset | Specification | Status |
|-------|---------------|--------|
| App Icon | 512x512 PNG, 32-bit, no alpha | NEED TO CREATE |
| Feature Graphic | 1024x500 PNG or JPG | NEED TO CREATE |
| Phone Screenshots | Min 2, 16:9 or 9:16 | NEED TO TAKE |
| Short Description | Max 80 characters | ✅ READY (see below) |
| Full Description | Max 4000 characters | ✅ READY (see below) |
| Privacy Policy URL | Must be accessible | ✅ mobilecli.com/privacy.html |
| App Category | Select from list | Tools or Productivity |
| Content Rating | Complete questionnaire | TODO |
| Contact Email | Public email | NEED ALIAS |
| Contact Address | Public address | NEED SOLUTION |

### Ready-to-Use Descriptions:

**Short Description (77 chars):**
```
AI terminal with Claude, Gemini & Codex. Root-level Android access, no root.
```

**Full Description:**
```
MobileCLI transforms your Android phone into a powerful AI-powered development environment.

POWERFUL AI INTEGRATION
• Claude Code, Gemini CLI, and Codex CLI built-in
• Browser-based OAuth authentication
• Switch between AI assistants seamlessly

ROOT-LEVEL ACCESS WITHOUT ROOTING
• 79 Android permissions give you full device control
• 75+ API commands: camera, SMS, contacts, location, sensors
• Access everything a rooted device can - safely and legally

PROFESSIONAL TERMINAL
• Full terminal emulator with session persistence
• Background execution with Wake Lock
• Text selection, copy/paste support
• Multi-session support

EASY SETUP
• 3-stage Setup Wizard guides you through everything
• Automatic bootstrap installation
• Works on Android 7.0+

BUILT BY AI, FOR AI
MobileCLI was built entirely on an Android phone using Claude Code. The app that runs the AI was built by the AI.

Perfect for:
• Mobile developers
• System administrators
• Security researchers
• AI enthusiasts
• Power users who want full device control

Download now and unlock your phone's full potential.
```

---

## REMAINING TASKS (Prioritized)

### HIGH PRIORITY (This Week)

1. **Security/Privacy Setup**
   - [ ] Create contact@mobilecli.com or alias email
   - [ ] Get registered agent or PO Box for address
   - [ ] Run security audit on codebase
   - [ ] Check git history for sensitive data

2. **Graphics/Assets**
   - [ ] Create 512x512 app icon
   - [ ] Create 1024x500 feature graphic
   - [ ] Take 4-6 screenshots of app running

3. **Closed Testing**
   - [ ] Generate release signing key
   - [ ] Build signed APK
   - [ ] Upload to Play Console closed testing
   - [ ] Collect 12 tester emails
   - [ ] Send invites, get opt-ins

### MEDIUM PRIORITY (Next Week)

4. **Stripe Setup**
   - [ ] Create Stripe account
   - [ ] Create 4 products (Pro/Team Monthly/Yearly)
   - [ ] Get API keys
   - [ ] Update website/js/stripe-config.js
   - [ ] Set up webhook

5. **Supabase Verification**
   - [ ] Verify all tables exist
   - [ ] Test authentication flow
   - [ ] Test license verification

6. **Content Rating**
   - [ ] Complete Google's questionnaire
   - [ ] Get IARC rating

### BEFORE PRODUCTION

7. **Final Security Audit**
   - [ ] Full codebase scan for personal info
   - [ ] Review all public-facing content
   - [ ] Test all user flows

8. **Documentation**
   - [ ] User guide/FAQ on website
   - [ ] Support email ready
   - [ ] Terms of service accessible

---

## KEY ACCOUNTS & CREDENTIALS

| Service | Account/URL | Status |
|---------|-------------|--------|
| Play Console | [your-private-email] | ✅ Active |
| Play Console ID | 4824773357272454397 | ✅ |
| Supabase | mwxlguqukyfberyhtkmg.supabase.co | ✅ Configured |
| GitHub Org | github.com/MobileDevCLI | ✅ Active |
| Website | mobilecli.com | ✅ Live |
| Stripe | - | ❌ Need to create |
| Contact Email | - | ❌ Need to create |
| Business Address | - | ❌ Need solution |

---

## COMMANDS REFERENCE

### Generate Release Signing Key
```bash
cd MobileCLI-CLEAN
keytool -genkey -v -keystore mobilecli-release.keystore -alias mobilecli -keyalg RSA -keysize 2048 -validity 10000
# SAVE THE PASSWORD - you need it forever
# DO NOT commit keystore to git
```

### Build Signed Release APK
```bash
cd MobileCLI-CLEAN
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Security Audit Commands
```bash
# Search for personal info
grep -r "your-username" .
grep -r "@gmail.com" .
grep -r "C:\\Users\\" .
grep -r "phone" .
grep -r "address" .

# Check git history
git log --all --full-history -p | grep -i "password"
git log --all --full-history -p | grep -i "@gmail"
```

---

## DAILY PLAN FOR NEXT 2 WEEKS

### Week 1
| Day | Tasks |
|-----|-------|
| Mon | Security setup: Create alias email, research address solutions |
| Tue | Graphics: Create app icon and feature graphic |
| Wed | Screenshots: Take professional screenshots of app |
| Thu | Signing key: Generate key, build signed APK |
| Fri | Upload: Submit to closed testing, send tester invites |
| Sat | Testers: Follow up, ensure 12 opt-ins |
| Sun | Rest / Buffer |

### Week 2
| Day | Tasks |
|-----|-------|
| Mon | Stripe: Create account, set up products |
| Tue | Stripe: Update website config, test checkout |
| Wed | Supabase: Verify tables, test flows |
| Thu | Content rating: Complete questionnaire |
| Fri | Final review: Check all requirements |
| Sat | Documentation: Update all docs |
| Sun | Day 14: Apply for production access |

---

## IMPORTANT NOTES

1. **Keystore is CRITICAL** - If you lose it, you cannot update your app on Play Store ever. Back it up in multiple secure locations.

2. **14 days starts when testers opt-in** - Not when you upload. Make sure testers actually click the link and install.

3. **Address is PUBLIC** - Whatever you use will be visible to anyone on Play Store. Use a business address, not home.

4. **Security audit before going public** - Any personal info in your code/git history can be found by determined hackers.

5. **Keep documentation updated** - Future Claude sessions will read these files to continue your work.

---

*This document should be your guide for the next 2 weeks. Update it as you complete tasks.*
