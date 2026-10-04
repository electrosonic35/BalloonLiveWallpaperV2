#!/usr/bin/env bash
set -euo pipefail

# Lightweight Gradle bootstrap for GitHub/Codemagic.
# If Gradle is already installed, use it. Otherwise download the pinned
# Gradle distribution into the user's Gradle cache and run it.

GRADLE_VERSION="8.10.2"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
INSTALL_DIR="$CACHE_DIR/gradle-${GRADLE_VERSION}"
ZIP_FILE="$CACHE_DIR/gradle-${GRADLE_VERSION}-bin.zip"

if [ ! -x "$INSTALL_DIR/bin/gradle" ]; then
    mkdir -p "$CACHE_DIR"
    if [ ! -f "$ZIP_FILE" ]; then
        echo "Gradle ${GRADLE_VERSION} is not installed; downloading pinned distribution..."
        if command -v curl >/dev/null 2>&1; then
            curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$ZIP_FILE"
        elif command -v wget >/dev/null 2>&1; then
            wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O "$ZIP_FILE"
        else
            echo "Neither curl nor wget is available." >&2
            exit 1
        fi
    fi
    rm -rf "$INSTALL_DIR"
    unzip -q "$ZIP_FILE" -d "$CACHE_DIR"
fi

exec "$INSTALL_DIR/bin/gradle" --project-dir "$ROOT_DIR" "$@"
