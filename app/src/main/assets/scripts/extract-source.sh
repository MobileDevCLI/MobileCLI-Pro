#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI Source Extraction Script
# Extracts bundled source code for self-modification

set -e

SOURCE_DIR="$HOME/MobileCLI-source"
ASSETS_DIR="/data/data/com.termux/files/usr/share/mobilecli"
SOURCE_TAR="$ASSETS_DIR/mobilecli-source.tar.gz"

echo "=== MobileCLI Source Extraction ==="
echo ""

# Check if source already exists
if [ -d "$SOURCE_DIR" ]; then
    echo "Source directory already exists at $SOURCE_DIR"
    read -p "Replace with bundled source? (y/n): " REPLACE
    if [ "$REPLACE" = "y" ]; then
        rm -rf "$SOURCE_DIR"
    else
        echo "Keeping existing source."
        exit 0
    fi
fi

# Check for bundled source
if [ -f "$SOURCE_TAR" ]; then
    echo "Extracting bundled source..."
    mkdir -p "$SOURCE_DIR"
    tar -xzf "$SOURCE_TAR" -C "$SOURCE_DIR"
    echo "SUCCESS: Source extracted to $SOURCE_DIR"
elif [ -f "$HOME/.mobilecli/source.tar.gz" ]; then
    echo "Extracting from ~/.mobilecli..."
    mkdir -p "$SOURCE_DIR"
    tar -xzf "$HOME/.mobilecli/source.tar.gz" -C "$SOURCE_DIR"
    echo "SUCCESS: Source extracted to $SOURCE_DIR"
else
    echo "No bundled source found."
    echo ""
    echo "Options:"
    echo "  1. Clone from GitHub (requires setup-github first):"
    echo "     git clone https://github.com/MobileDevCLI/MobileCLI-v2.git ~/MobileCLI-source"
    echo ""
    echo "  2. Copy source manually to ~/MobileCLI-source/"
    exit 1
fi

echo ""
echo "To build:"
echo "  cd $SOURCE_DIR"
echo "  ./gradlew assembleDebug"
echo ""
echo "=== Extraction Complete ==="
