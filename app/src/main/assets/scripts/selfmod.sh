#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI Self-Modification Master Script
# Handles all methods: GitHub clone, bundled source, or manual setup

set -e

SOURCE_DIR="$HOME/MobileCLI-source"
SCRIPTS_DIR="$(dirname "$0")"

echo "╔════════════════════════════════════════════════════════════╗"
echo "║          MobileCLI Self-Modification System                ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check for dev tools
check_tools() {
    echo "Checking development tools..."
    MISSING=""

    which java >/dev/null 2>&1 || MISSING="$MISSING openjdk-17"
    which gradle >/dev/null 2>&1 || MISSING="$MISSING gradle"
    which git >/dev/null 2>&1 || MISSING="$MISSING git"
    which aapt >/dev/null 2>&1 || MISSING="$MISSING aapt"
    which aapt2 >/dev/null 2>&1 || MISSING="$MISSING aapt2"
    which apksigner >/dev/null 2>&1 || MISSING="$MISSING apksigner"

    if [ -n "$MISSING" ]; then
        echo "Missing tools:$MISSING"
        echo ""
        read -p "Install missing tools? (y/n): " INSTALL
        if [ "$INSTALL" = "y" ]; then
            echo "Installing..."
            pkg update -y
            pkg install -y $MISSING
            echo "Tools installed."
        else
            echo "Cannot continue without tools."
            exit 1
        fi
    else
        echo "All tools present."
    fi
    echo ""
}

# Check for Android SDK
check_sdk() {
    echo "Checking Android SDK..."

    if [ -d "$HOME/android-sdk/platforms/android-34" ]; then
        echo "Android SDK found."
        export ANDROID_HOME="$HOME/android-sdk"
    elif [ -d "$ANDROID_HOME/platforms" ]; then
        echo "Android SDK found at $ANDROID_HOME"
    else
        echo "Android SDK not found."
        read -p "Download Android SDK? (~500MB) (y/n): " DOWNLOAD
        if [ "$DOWNLOAD" = "y" ]; then
            install_sdk
        else
            echo "Cannot build without SDK."
            exit 1
        fi
    fi
    echo ""
}

# Install Android SDK
install_sdk() {
    echo "Downloading Android SDK..."
    mkdir -p ~/android-sdk/cmdline-tools
    cd ~/android-sdk/cmdline-tools

    curl -L -o cmdline-tools.zip \
        "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

    unzip -q cmdline-tools.zip
    mv cmdline-tools latest 2>/dev/null || true
    rm cmdline-tools.zip

    export ANDROID_HOME=~/android-sdk
    export JAVA_HOME=$(dirname $(dirname $(which java)))

    yes | ~/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null 2>&1 || true
    ~/android-sdk/cmdline-tools/latest/bin/sdkmanager \
        "platforms;android-34" \
        "build-tools;34.0.0" \
        "platform-tools"

    echo "SDK installed."
}

# Get source code
get_source() {
    echo "=== Source Code Setup ==="
    echo ""

    if [ -d "$SOURCE_DIR" ]; then
        echo "Source already exists at $SOURCE_DIR"
        read -p "Use existing source? (y/n): " USE_EXISTING
        if [ "$USE_EXISTING" = "y" ]; then
            return 0
        fi
        rm -rf "$SOURCE_DIR"
    fi

    echo "How do you want to get the source code?"
    echo ""
    echo "  1. Clone from GitHub (requires token)"
    echo "  2. Extract bundled source"
    echo "  3. I'll copy it manually"
    echo ""
    read -p "Choice (1/2/3): " CHOICE

    case $CHOICE in
        1)
            # GitHub clone
            if [ ! -f ~/.git-credentials ]; then
                echo "GitHub not configured. Running setup..."
                bash "$SCRIPTS_DIR/setup-github.sh"
            fi

            read -p "Repository URL (e.g., https://github.com/user/repo.git): " REPO_URL
            git clone "$REPO_URL" "$SOURCE_DIR"
            ;;
        2)
            # Extract bundled
            bash "$SCRIPTS_DIR/extract-source.sh"
            ;;
        3)
            echo "Please copy source to: $SOURCE_DIR"
            echo "Then run this script again."
            exit 0
            ;;
        *)
            echo "Invalid choice"
            exit 1
            ;;
    esac

    echo ""
}

# Build APK
build_apk() {
    echo "=== Building APK ==="
    echo ""

    cd "$SOURCE_DIR"

    # Create local.properties if missing
    if [ ! -f local.properties ]; then
        echo "sdk.dir=$ANDROID_HOME" > local.properties
    fi

    # Add aapt2 override for ARM
    if ! grep -q "aapt2FromMavenOverride" gradle.properties 2>/dev/null; then
        echo "" >> gradle.properties
        echo "# ARM aapt2 override for Termux" >> gradle.properties
        echo "android.aapt2FromMavenOverride=$(which aapt2)" >> gradle.properties
    fi

    echo "Starting build..."
    echo "This may take 5-15 minutes on first run."
    echo ""

    chmod +x gradlew
    ./gradlew assembleDebug

    # Find and copy APK
    APK_PATH=$(find . -name "*.apk" -path "*/debug/*" | head -1)
    if [ -n "$APK_PATH" ]; then
        TIMESTAMP=$(date +%Y%m%d_%H%M%S)
        OUTPUT="/sdcard/Download/MobileCLI-selfbuilt-$TIMESTAMP.apk"
        cp "$APK_PATH" "$OUTPUT"
        echo ""
        echo "╔════════════════════════════════════════════════════════════╗"
        echo "║                    BUILD SUCCESSFUL                        ║"
        echo "╚════════════════════════════════════════════════════════════╝"
        echo ""
        echo "APK: $OUTPUT"
        echo ""
        echo "To install:"
        echo "  1. Open Files app"
        echo "  2. Go to Downloads"
        echo "  3. Tap the APK"
        echo ""
    else
        echo "Build completed but APK not found."
        echo "Check: $SOURCE_DIR/app/build/outputs/apk/"
    fi
}

# Main flow
main() {
    check_tools
    check_sdk
    get_source
    build_apk
}

# Run
main
