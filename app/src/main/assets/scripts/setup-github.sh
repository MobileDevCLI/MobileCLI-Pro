#!/data/data/com.termux/files/usr/bin/bash
# MobileCLI GitHub Setup Script
# Allows users to connect their own GitHub account for self-modification

set -e

echo "=== MobileCLI GitHub Setup ==="
echo ""
echo "This will configure git to access your GitHub repositories."
echo "You need a Personal Access Token (PAT) from GitHub."
echo ""
echo "To create a token:"
echo "  1. Go to: https://github.com/settings/tokens"
echo "  2. Click 'Generate new token (classic)'"
echo "  3. Select scopes: repo (full control)"
echo "  4. Copy the token"
echo ""

# Check if already configured
if [ -f ~/.git-credentials ]; then
    echo "Existing credentials found."
    read -p "Replace existing credentials? (y/n): " REPLACE
    if [ "$REPLACE" != "y" ]; then
        echo "Keeping existing credentials."
        exit 0
    fi
fi

# Get username
read -p "GitHub username: " GH_USERNAME
if [ -z "$GH_USERNAME" ]; then
    echo "Error: Username required"
    exit 1
fi

# Get token
read -s -p "GitHub token (hidden): " GH_TOKEN
echo ""
if [ -z "$GH_TOKEN" ]; then
    echo "Error: Token required"
    exit 1
fi

# Configure git
echo "Configuring git..."
git config --global user.name "$GH_USERNAME"
git config --global credential.helper store

# Store credentials
echo "https://${GH_USERNAME}:${GH_TOKEN}@github.com" > ~/.git-credentials
chmod 600 ~/.git-credentials

# Test connection
echo ""
echo "Testing connection..."
if curl -s -H "Authorization: token $GH_TOKEN" https://api.github.com/user | grep -q "login"; then
    echo "SUCCESS: GitHub connected as $GH_USERNAME"
    echo ""
    echo "You can now clone private repositories:"
    echo "  git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git"
else
    echo "WARNING: Could not verify token. It may still work."
fi

echo ""
echo "=== Setup Complete ==="
