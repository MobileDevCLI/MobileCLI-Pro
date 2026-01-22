# APK Signing Guide - Removing "Untrusted App" Warning

## The Problem

When you install an APK directly (sideload), Android shows warnings like:
- "Blocked by Play Protect"
- "Unknown developer"
- "App not verified"

This happens because:
1. **Debug APKs** are signed with a debug key (not trusted)
2. **Release APKs** need a proper keystore
3. **Play Protect** doesn't recognize unsigned/debug apps

## Solutions

### Option 1: Sign with Release Keystore (Recommended)

1. **Create a keystore** (one-time):
   ```bash
   keytool -genkey -v -keystore mobilecli-release.keystore \
     -alias mobilecli -keyalg RSA -keysize 2048 -validity 10000 \
     -storepass YOUR_PASSWORD -keypass YOUR_PASSWORD \
     -dname "CN=MobileCLI, OU=Mobile, O=MobileDevCLI, L=City, S=State, C=US"
   ```

2. **Put keystore in project root**:
   ```
   MobileCLI-Pro/
   ├── mobilecli-release.keystore  ← Here
   ├── app/
   └── ...
   ```

3. **Update local.properties**:
   ```properties
   sdk.dir=/path/to/android-sdk
   KEYSTORE_PASSWORD=YOUR_PASSWORD
   KEY_ALIAS=mobilecli
   KEY_PASSWORD=YOUR_PASSWORD
   ```

4. **Build release APK**:
   ```bash
   ./gradlew assembleRelease
   ```

5. **APK location**:
   ```
   app/build/outputs/apk/release/app-release.apk
   ```

### Option 2: Publish on Google Play

When you publish on Google Play:
- Google signs your app with their key
- Play Protect trusts it automatically
- No sideload warnings
- Automatic updates for users

### Option 3: Disable Play Protect (Not Recommended for Users)

For testing only:
1. Open Google Play Store
2. Tap profile icon → Play Protect
3. Tap Settings (gear icon)
4. Turn off "Scan apps with Play Protect"

**Warning**: Don't tell users to do this - it's a security risk.

## Understanding the Warnings

| Warning | Cause | Fix |
|---------|-------|-----|
| "Blocked by Play Protect" | Debug APK or unsigned | Sign with release key |
| "Unknown developer" | Not on Play Store | Publish to Play Store |
| "Install anyway?" prompt | Sideloading enabled | Normal for sideload |
| "App not verified" | New/unknown app | Users must trust once |

## Build Configuration

The `build.gradle.kts` is already configured for signing:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../mobilecli-release.keystore")
        storePassword = localProperties.getProperty("KEYSTORE_PASSWORD", "")
        keyAlias = localProperties.getProperty("KEY_ALIAS", "mobilecli")
        keyPassword = localProperties.getProperty("KEY_PASSWORD", "")
    }
}

buildTypes {
    release {
        isMinifyEnabled = false
        signingConfig = signingConfigs.getByName("release")
    }
}
```

## Keystore Security

**NEVER commit these files**:
- `mobilecli-release.keystore`
- `local.properties` (with passwords)

The `.gitignore` already excludes them:
```
*.keystore
*.jks
local.properties
```

## For Google Play Submission

1. Create a **Google Play Developer account** ($25 one-time)
2. Generate **App Bundle** instead of APK:
   ```bash
   ./gradlew bundleRelease
   ```
3. Upload `.aab` file to Play Console
4. Google will sign it with Play App Signing

## Quick Commands

```bash
# Debug build (will show warnings)
./gradlew assembleDebug

# Release build (needs keystore)
./gradlew assembleRelease

# App bundle for Play Store
./gradlew bundleRelease

# Copy to Downloads
cp app/build/outputs/apk/release/app-release.apk /sdcard/Download/MobileCLI-Pro.apk
```
