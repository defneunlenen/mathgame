#!/usr/bin/env bash
# Deploy the Math Game APK to a connected Android device.
#
#   ./deploy.sh              # auto-build + install to connected device
#   ./deploy.sh release      # build release APK + install
#   ./deploy.sh --list       # list connected devices
#   ./deploy.sh install      # only install (skip build)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

BUILD_VARIANT="${1:-debug}"

# ── Colors ───────────────────────────────────────────────
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[deploy]${NC} $*"; }
warn()  { echo -e "${YELLOW}[deploy]${NC} $*"; }
error() { echo -e "${RED}[deploy]${NC} $*"; exit 1; }

# ── Check prerequisites ─────────────────────────────────
command -v adb >/dev/null 2>&1 || error "adb not found. Install Android Platform Tools."
command -v ./android-build.sh >/dev/null 2>&1 || error "android-build.sh not found. Run from project root."

# ── List connected devices ──────────────────────────────
devices=$(adb devices 2>/dev/null | grep '\sdevice$' | awk '{print $1}')
device_count=$(echo "$devices" | grep -c '.' || true)

if [ "$device_count" -eq 0 ]; then
    error "No device connected.\n  • Enable USB Debugging on your phone/tablet\n  • Connect via USB  or  adb connect <IP>"
fi

if [ "${1:-}" = "--list" ]; then
    echo -e "${CYAN}Connected devices:${NC}"
    echo "$devices" | while read -r d; do echo "  • $d"; done
    exit 0
fi

# ── Build if APK missing or variant changed ─────────────
if [ "${1:-}" != "install" ]; then
    gradle_task="assemble${BUILD_VARIANT^}"
    info "Building ${BUILD_VARIANT} APK... (${gradle_task})"
    ./android-build.sh "$gradle_task" >/dev/null 2>&1
    info "Build complete."
fi

APK_DIR="app/build/outputs/apk/${BUILD_VARIANT}"
if [ ! -d "$APK_DIR" ]; then
    error "APK directory ${APK_DIR} not found. Build first?"
fi

# Find the APK (prefer unsigned release, fall back to first match)
APK_PATH=""
if [ "$BUILD_VARIANT" = "release" ]; then
    APK_PATH=$(find "$APK_DIR" -name 'app-release*.apk' ! -name '*unsigned*' -type f | head -1)
    [ -z "$APK_PATH" ] && APK_PATH=$(find "$APK_DIR" -name 'app-release*.apk' -type f | head -1)
fi
[ -z "$APK_PATH" ] && APK_PATH=$(find "$APK_DIR" -name "app-${BUILD_VARIANT}*.apk" -type f | head -1)
[ -z "$APK_PATH" ] && error "APK not found in ${APK_DIR}"

APK_SIZE=$(du -h "$APK_PATH" | awk '{print $1}')
info "📦 APK: $APK_PATH ($APK_SIZE)"

# ── Ask if multiple devices ──────────────────────────────
if [ "$device_count" -gt 1 ]; then
    echo -e "${CYAN}Multiple devices connected — choose one:${NC}"
    idx=0
    devices_arr=()
    while read -r d; do
        devices_arr+=("$d")
        idx=$((idx + 1))
    done <<< "$devices"

    for i in "${!devices_arr[@]}"; do
        echo -e "  ${GREEN}$((i + 1))${NC}) ${devices_arr[$i]}"
    done

    echo -n "Select device [1]: "
    read -r choice
    selected_idx=${choice:-1}
    if [ "$selected_idx" -lt 1 ] || [ "$selected_idx" -gt "$device_count" ]; then
        error "Invalid selection."
    fi
    target_serial="${devices_arr[$((selected_idx - 1))]}"
    info "Using device: $target_serial"
    install_cmd="adb -s $target_serial install -r $APK_PATH"
else
    target_serial=$(echo "$devices" | head -1)
    install_cmd="adb -s $target_serial install -r $APK_PATH"
fi

# ── Install ─────────────────────────────────────────────
info "Installing to ${target_serial}..."
if bash -c "$install_cmd"; then
    info "✅ Defne Matematik installed successfully!"
else
    error "Install failed. Check device permissions / USB debugging."
fi
