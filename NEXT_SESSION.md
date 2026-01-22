# MobileCLI - Quick Start for Next Session

**Last Updated:** January 19, 2026 @ 9:50 PM
**Read this first when starting a new Claude Code session.**

---

## CURRENT STATUS: 95% Complete - In 14-Day Testing Phase

### What's DONE
- [x] Android app: 100% functional (v2.0.0-beta.21)
- [x] Website: Live at mobilecli.com
- [x] APK download: mobilecli.com/downloads/MobileCLI-latest.apk
- [x] Play Console: Account created, verified, app created
- [x] Supabase: Configured and ready
- [x] Legal docs: Complete

### What's IN PROGRESS
- [ ] 14-day closed testing (Google requirement for new devs)
- [ ] Security/privacy setup (alias email, business address)
- [ ] Graphics (app icon, feature graphic, screenshots)

---

## PRIORITY READING ORDER

1. **LAUNCH_CHECKLIST.md** - Complete 2-week plan with daily tasks
2. **ROADMAP_AND_STATUS.md** - Full project status
3. **SECURITY_AUDIT.md** - Pre-release security checklist
4. **RELEASE_CONTROL.md** - How to manage GitHub releases

---

## KEY ACCOUNTS

| Service | Status | Details |
|---------|--------|---------|
| Play Console | ✅ Active | Account ID: 4824773357272454397 |
| Supabase | ✅ Ready | mwxlguqukyfberyhtkmg.supabase.co |
| Website | ✅ Live | mobilecli.com |
| GitHub | ✅ Active | github.com/MobileDevCLI |
| Stripe | ❌ TODO | Need to create account |

---

## IMMEDIATE NEXT STEPS

### Security/Privacy (Do First)
1. Create alias email for public contact (contact@mobilecli.com)
2. Get business address (registered agent or PO Box)
3. Run security audit on codebase

### Closed Testing (Required by Google)
1. Generate release signing key
2. Build signed APK
3. Upload to Play Console closed testing
4. Get 12 testers to opt-in
5. Wait 14 days → Apply for production

### Stripe (For Payments)
1. Create Stripe account
2. Create 4 products ($10/mo, $100/yr, $20/mo, $200/yr)
3. Update website/js/stripe-config.js

---

## QUICK COMMANDS

```bash
# Build debug APK
cd MobileCLI-CLEAN && ./gradlew assembleDebug

# Generate signing key (ONE TIME ONLY - save password!)
keytool -genkey -v -keystore mobilecli-release.keystore -alias mobilecli -keyalg RSA -keysize 2048 -validity 10000

# Build release APK
./gradlew assembleRelease

# Security audit
grep -r "your-username" .
grep -r "@gmail.com" .
```

---

## KEY FILES

| File | Purpose |
|------|---------|
| LAUNCH_CHECKLIST.md | **START HERE** - 2-week launch plan |
| ROADMAP_AND_STATUS.md | Complete project status |
| SECURITY_AUDIT.md | Security checklist |
| RELEASE_CONTROL.md | GitHub release management |
| website/js/stripe-config.js | Needs Stripe keys |

---

## STORE LISTING (Ready to Use)

**Short Description (77 chars):**
```
AI terminal with Claude, Gemini & Codex. Root-level Android access, no root.
```

**Full Description:** See LAUNCH_CHECKLIST.md

**Privacy URL:** https://mobilecli.com/privacy.html

---

## IMPORTANT REMINDERS

1. **Keystore is FOREVER** - Back up mobilecli-release.keystore securely
2. **14 days required** - Can't skip closed testing for new accounts
3. **Address is PUBLIC** - Don't use home address on Play Store
4. **Security audit BEFORE public** - Check for personal info in code

---

**Full 2-week plan: Read LAUNCH_CHECKLIST.md**
