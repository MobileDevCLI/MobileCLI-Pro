# AAPT2 ARM Compatibility Fix

**Problem Solved:** January 22, 2026
**Bug Duration:** ~2 days of debugging

---

## The Problem

When building Android APKs on an ARM device (phone/tablet) using Gradle, the build fails with:

```
AAPT2 aapt2-8.2.0-10154469-linux Daemon #0: Unexpected error output:
.../aapt2: 2: Syntax error: Unterminated quoted string

AAPT2 Daemon startup failed
This should not happen under normal circumstances
```

### Root Cause

1. Gradle downloads `aapt2` from Maven Central
2. Maven only has **x86_64 Linux** binaries
3. Your phone is **ARM64** - can't execute x86 binaries
4. When ARM tries to run x86 binary, it interprets it as a shell script → "Syntax error"

---

## The Fix

Add this line to `gradle.properties`:

```properties
# CRITICAL: Use local ARM aapt2 instead of x86 from Maven
android.aapt2FromMavenOverride=/data/user/0/com.termux/files/home/android-sdk/build-tools/34.0.0/aapt2
```

This tells Gradle to use the **local ARM-compiled aapt2** from your Android SDK instead of downloading the x86 version.

---

## Where the Fix Lives

| File | Location |
|------|----------|
| `gradle.properties` | Project root (`~/MobileCLI-Pro/gradle.properties`) |
| Local aapt2 | `~/android-sdk/build-tools/34.0.0/aapt2` |

---

## How to Verify the Fix

1. Check gradle.properties has the override:
   ```bash
   grep aapt2 ~/MobileCLI-Pro/gradle.properties
   ```

2. Verify local aapt2 exists and is ARM:
   ```bash
   file ~/android-sdk/build-tools/34.0.0/aapt2
   # Should show: ELF 64-bit LSB executable, ARM aarch64
   ```

3. Build should work:
   ```bash
   cd ~/MobileCLI-Pro && ./gradlew assembleDebug
   ```

---

## If It Breaks Again

### Symptoms
- Build fails at `processDebugResources` task
- Error mentions "aapt2 daemon startup failed"
- "Syntax error: Unterminated quoted string"

### Solutions

1. **Check the override is still in gradle.properties**
   ```bash
   grep aapt2 gradle.properties
   ```

2. **Check aapt2 exists at the specified path**
   ```bash
   ls -la ~/android-sdk/build-tools/34.0.0/aapt2
   ```

3. **If build-tools version changed**, update the path:
   ```bash
   ls ~/android-sdk/build-tools/
   # Use the version that exists
   ```

4. **Clear Gradle cache if corrupted**
   ```bash
   rm -rf ~/.gradle/caches/transforms-3/
   ./gradlew assembleDebug
   ```

---

## Why This Happens

| Platform | aapt2 from Maven | Result |
|----------|------------------|--------|
| Linux x86_64 | x86_64 binary | ✅ Works |
| macOS x86_64 | x86_64 binary | ✅ Works |
| macOS ARM | Rosetta translates | ✅ Works |
| **Android ARM** | x86_64 binary | ❌ **FAILS** |

Android/Termux has no x86 emulation layer, so x86 binaries simply can't run.

---

## Related Files

- `gradle.properties` - Contains the fix
- `local.properties` - Contains SDK path
- `build.gradle.kts` - Build configuration

---

## Commit History

```
209e05a - Fix aapt2 ARM compatibility for on-device builds
```

---

*This fix is permanent as long as gradle.properties isn't overwritten.*
