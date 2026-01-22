# MobileCLI Release Control Guide

**Created:** January 19, 2026
**Purpose:** Quick reference for controlling APK distribution access

---

## Current Release

**URL:** https://github.com/MobileDevCLI/MobileCLI-Alpha/releases/tag/v2.0.0-beta.20

**Direct Download (only works when repo is PUBLIC):**
```
https://github.com/MobileDevCLI/MobileCLI-Alpha/releases/download/v2.0.0-beta.20/MobileCLI-v2.0.0-beta.20-bulletproof-20260119.apk
```

---

## IMPORTANT: Repo Visibility Controls Access

| Repo Status | Who Can Download |
|-------------|------------------|
| **PRIVATE** (current) | Only you and collaborators |
| **PUBLIC** | Anyone with the link |

---

## How To Make Repo PUBLIC (to share APK)

### Option 1: GitHub Website (Easiest)
1. Go to: https://github.com/MobileDevCLI/MobileCLI-Alpha/settings
2. Scroll to bottom → "Danger Zone"
3. Click "Change visibility"
4. Select "Make public"
5. Type repo name to confirm

### Option 2: Command Line
```bash
gh repo edit MobileDevCLI/MobileCLI-Alpha --visibility public
```

---

## How To Make Repo PRIVATE Again (to lock it down)

### Option 1: GitHub Website
1. Go to: https://github.com/MobileDevCLI/MobileCLI-Alpha/settings
2. Scroll to bottom → "Danger Zone"
3. Click "Change visibility"
4. Select "Make private"
5. Type repo name to confirm

### Option 2: Command Line
```bash
gh repo edit MobileDevCLI/MobileCLI-Alpha --visibility private
```

---

## How To DELETE The Release Entirely

### Option 1: GitHub Website
1. Go to: https://github.com/MobileDevCLI/MobileCLI-Alpha/releases
2. Click on "v2.0.0-beta.20"
3. Click "Delete" button (trash icon)
4. Confirm deletion

### Option 2: Command Line
```bash
gh release delete v2.0.0-beta.20 --yes
```

---

## Quick Workflow: Share With Someone Temporarily

```bash
# 1. Make public so they can download
gh repo edit MobileDevCLI/MobileCLI-Alpha --visibility public

# 2. Send them this link:
# https://github.com/MobileDevCLI/MobileCLI-Alpha/releases/download/v2.0.0-beta.20/MobileCLI-v2.0.0-beta.20-bulletproof-20260119.apk

# 3. Once they've downloaded, make private again
gh repo edit MobileDevCLI/MobileCLI-Alpha --visibility private
```

---

## All Releases Page

View/manage all releases: https://github.com/MobileDevCLI/MobileCLI-Alpha/releases

---

## Emergency: Remove All Public Access Immediately

If you need to lock everything down fast:

```bash
# Make repo private (hides everything)
gh repo edit MobileDevCLI/MobileCLI-Alpha --visibility private

# Optional: Also delete the release
gh release delete v2.0.0-beta.20 --yes
```

Or on website: https://github.com/MobileDevCLI/MobileCLI-Alpha/settings → Change visibility → Private

---

**Remember:** While repo is PRIVATE, the download link will give a 404 error to anyone who isn't a collaborator. You have full control.
