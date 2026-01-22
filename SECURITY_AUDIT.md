# MobileCLI Security Audit Checklist

**Last Updated:** January 19, 2026
**Status:** MUST COMPLETE BEFORE PUBLIC RELEASE

---

## CRITICAL: Why This Matters

MobileCLI is intentionally built with extensive permissions (79 total). This makes it powerful but also means:

1. **Users trust us with significant device access**
2. **Any embedded personal data could be exposed**
3. **The app could be reverse-engineered**
4. **APK contents are not secret**

---

## Pre-Release Security Checklist

### 1. Personal Data Removal

#### 1.1 Search for Personal Information
```bash
# Run these searches in the MobileCLI-CLEAN directory
grep -r "your-username" .
grep -r "@gmail.com" .
grep -r "@outlook.com" .
grep -r "C:\\Users\\" .
grep -r "/Users/" .
grep -r "phone number" .
grep -r "address" .
```

#### 1.2 Check for Hardcoded Paths
- [ ] No absolute Windows paths (`C:\Users\USERNAME\...`)
- [ ] No absolute Unix paths with usernames
- [ ] All paths use relative references or environment variables

#### 1.3 Remove Debug Information
- [ ] No personal debug logs
- [ ] No test phone numbers
- [ ] No test email addresses
- [ ] No personal API keys

### 2. Conversation/Chat History

#### 2.1 AI Conversation Logs
- [ ] No Claude conversation history in code
- [ ] No Gemini conversation history
- [ ] No ChatGPT/Codex conversation history
- [ ] No conversation references in comments

#### 2.2 Development Notes
- [ ] No personal TODOs with identifying info
- [ ] No "Rob" or personal name references
- [ ] No references to personal projects

### 3. API Keys & Secrets

#### 3.1 What SHOULD Be Public (OK to commit)
- Supabase Anon Key (designed to be public)
- Stripe Publishable Key (designed to be public)
- Firebase config (non-sensitive parts)

#### 3.2 What MUST NOT Be Public (NEVER commit)
- [ ] Supabase Service Key
- [ ] Stripe Secret Key
- [ ] Stripe Webhook Signing Secret
- [ ] OAuth Client Secrets
- [ ] Signing Keystore Password
- [ ] Any database passwords

#### 3.3 Check .gitignore
Ensure these are ignored:
```
*.keystore
*.jks
local.properties
.env
.env.local
secrets.json
service-account.json
```

### 4. Git History Audit

#### 4.1 Check for Sensitive Commits
```bash
# Search git history for sensitive patterns
git log --all --full-history -p | grep -i "password"
git log --all --full-history -p | grep -i "secret"
git log --all --full-history -p | grep -i "api_key"
git log --all --full-history -p | grep -i "@gmail"
```

#### 4.2 Options if Sensitive Data Found
1. **Clean history** (complex):
   ```bash
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch PATH_TO_FILE" \
     --prune-empty --tag-name-filter cat -- --all
   ```

2. **Start fresh** (simpler):
   - Create new repo
   - Copy current clean files
   - Push as initial commit

### 5. APK Security

#### 5.1 What's Visible in APK
Anyone can extract and view:
- All Kotlin/Java code (decompiled)
- All resource files
- All string values
- AndroidManifest.xml
- All assets

#### 5.2 ProGuard/R8 Obfuscation
- [ ] Enable minification for release builds
- [ ] Configure ProGuard rules
- [ ] Test release APK works after obfuscation

#### 5.3 Release Build Config
```gradle
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

### 6. Network Security

#### 6.1 HTTPS Only
- [ ] All API calls use HTTPS
- [ ] No HTTP fallbacks
- [ ] Certificate pinning (optional but recommended)

#### 6.2 Data Transmission
- [ ] Document what data is sent where
- [ ] AI queries go to respective providers (Anthropic, Google, OpenAI)
- [ ] License verification goes to Supabase
- [ ] No other data transmission to MobileCLI servers

### 7. Permission Justification

Document why each dangerous permission is needed:

| Permission | Justification |
|------------|---------------|
| CAMERA | termux-camera-photo command |
| RECORD_AUDIO | termux-microphone-record command |
| READ_SMS | termux-sms-list command |
| SEND_SMS | termux-sms-send command |
| READ_CONTACTS | termux-contact-list command |
| ACCESS_FINE_LOCATION | termux-location command |
| READ_CALL_LOG | termux-call-log command |
| CALL_PHONE | termux-telephony-call command |
| ... | ... |

### 8. Third-Party Dependencies

#### 8.1 Audit Dependencies
- [ ] Review all Gradle dependencies
- [ ] Check for known vulnerabilities
- [ ] Update outdated dependencies

#### 8.2 Terminal Libraries
- terminal-view: Apache 2.0 ✓
- terminal-emulator: Apache 2.0 ✓
- No known vulnerabilities

### 9. User Data Protection

#### 9.1 Local Storage
- [ ] Sensitive data encrypted at rest
- [ ] License keys stored securely
- [ ] API tokens use Android Keystore

#### 9.2 Clear Data on Uninstall
- [ ] No residual files in external storage
- [ ] User notified about ~/home directory contents

---

## Security Disclosure

### Recommended Security Page Content

Add to website (`security.html`):

```
MobileCLI Security

MobileCLI is designed with extensive device access to provide
root-equivalent functionality. Here's what you should know:

PERMISSIONS
MobileCLI requests 79 permissions to enable full terminal access.
All permissions are used only when you invoke specific commands.
No data is collected or transmitted without your explicit action.

DATA HANDLING
- All data stays on your device
- AI queries are sent to respective providers
- We don't operate data-collecting servers
- License verification uses Supabase (privacy policy applies)

REPORTING VULNERABILITIES
Email: security@mobilecli.com
We take security seriously and will respond within 48 hours.
```

---

## Sign-Off

Before public release, this checklist must be completed:

- [ ] All personal data removed
- [ ] Git history clean
- [ ] API secrets not exposed
- [ ] ProGuard enabled
- [ ] Security documentation published
- [ ] Privacy policy accurate

**Audited by:** ________________
**Date:** ________________

---

*This document should be reviewed before every public release.*
