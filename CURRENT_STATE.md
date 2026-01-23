# MobileCLI Pro - Current State (For AI Context Recovery)

**Date:** January 22, 2026
**Status:** READY FOR TESTING

---

## WHAT TO TEST RIGHT NOW

**APK:** `/sdcard/Download/MobileCLI-Pro-v2.0.0-USERID.apk`

**Webhook:** Already deployed to Supabase (v3 with user_id matching)

**Test Flow:**
1. Install APK on test phone
2. Login with Google
3. Click Subscribe → Opens PayPal subscription
4. Complete $15 payment
5. Press back → App should detect Pro status
6. Terminal should open (not paywall)

---

## KEY IDS (MEMORIZE THESE)

| Item | Value |
|------|-------|
| PayPal Plan ID | `P-3RH33892X5467024SNFZON2Y` |
| Supabase Project | `mwxlguqukyfberyhtkmg` |
| Webhook URL | `https://mwxlguqukyfberyhtkmg.supabase.co/functions/v1/paypal-webhook` |

---

## WHAT WAS BUILT TODAY

1. **PayPal Subscription** - $15/month recurring (not one-time)
2. **Webhook v3** - Handles REST events + user_id matching
3. **App Update** - Passes user_id to PayPal for matching

---

## FILES IN /sdcard/Download/

| File | Use |
|------|-----|
| `MobileCLI-Pro-v2.0.0-USERID.apk` | **TEST THIS ONE** |
| `paypal-webhook-v3-with-userid.txt` | Webhook code (already deployed) |

---

## GIT STATUS

Branch: master (up to date)
Latest commits:
- eed4e49 Update documentation
- 99f3c6f Add user_id matching
- 6fe3c2e Update webhook to REST format
- fb7f18a Update PayPal plan ID

---

## IMPORTANT FILES

- `docs/ROADMAP_AND_STATUS.md` - Full documentation
- `CLAUDE.md` - AI environment guide (ARM/Termux)
- `app/src/main/java/com/termux/auth/PaywallActivity.kt` - PayPal integration
- `supabase/functions/paypal-webhook/index.ts` - Webhook code

---

## DO NOT MODIFY

- BootstrapInstaller.kt
- SetupWizard.kt
- MainActivity.kt
- AndroidManifest.xml
- gradle.properties

---

## NEXT STEPS AFTER TESTING

If test works:
- Remove free trial button (user wants subscription only)
- Update website with download link

If test fails:
- Check Supabase function logs for webhook errors
- Verify JWT verification is OFF
- Check if user_id is being passed in PayPal URL

---

*This file helps AI recover context after compaction*
