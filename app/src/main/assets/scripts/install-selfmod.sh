#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI Self-Modification Installer
# Run during bootstrap to set up self-modification capability

set -e

SCRIPTS_DIR="$HOME/.mobilecli/scripts"
ASSETS="/data/data/com.termux/files/usr/share/mobilecli/scripts"

echo "=== Installing Self-Modification Scripts ==="

# Create directories
mkdir -p "$SCRIPTS_DIR"
mkdir -p "$HOME/.mobilecli/source"

# Copy scripts from assets (if available)
if [ -d "$ASSETS" ]; then
    cp "$ASSETS"/*.sh "$SCRIPTS_DIR/" 2>/dev/null || true
fi

# Make executable
chmod +x "$SCRIPTS_DIR"/*.sh 2>/dev/null || true

# Create symlinks in PATH
mkdir -p "$PREFIX/bin"

# selfmod command
cat > "$PREFIX/bin/selfmod" << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
exec "$HOME/.mobilecli/scripts/selfmod.sh" "$@"
EOF
chmod +x "$PREFIX/bin/selfmod"

# setup-github command
cat > "$PREFIX/bin/setup-github" << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
exec "$HOME/.mobilecli/scripts/setup-github.sh" "$@"
EOF
chmod +x "$PREFIX/bin/setup-github"

# extract-source command
cat > "$PREFIX/bin/extract-source" << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
exec "$HOME/.mobilecli/scripts/extract-source.sh" "$@"
EOF
chmod +x "$PREFIX/bin/extract-source"

echo ""
echo "Self-modification commands installed:"
echo "  selfmod       - Full self-modification wizard"
echo "  setup-github  - Configure GitHub access"
echo "  extract-source - Extract bundled source code"
echo ""
echo "=== Installation Complete ==="
