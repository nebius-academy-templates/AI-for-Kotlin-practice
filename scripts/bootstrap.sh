#!/usr/bin/env bash
# Environment doctor for macOS and Linux. Windows uses bootstrap.ps1.
set -u

cd "$(dirname "$0")/.." || exit 1
unset APPIUM_HOME

ok=0
fail=0

check() {
    local name="$1"
    shift
    if "$@" >/dev/null 2>&1; then
        echo "[OK]   $name"
        ok=$((ok + 1))
    else
        echo "[FAIL] $name"
        fail=$((fail + 1))
    fi
}

echo "AI QA Sandbox environment check"

java_version="$(java -version 2>&1 | head -n 1 || true)"
case "$java_version" in
    *'version "17.'*|*'version "17"'*) check "JDK 17" true ;;
    *) check "JDK 17" false ;;
esac

node_major="$(node --version 2>/dev/null | sed -n 's/^v\([0-9][0-9]*\).*/\1/p' || true)"
if [ -n "$node_major" ] && [ "$node_major" -ge 20 ]; then
    check "Node.js 20+" true
else
    check "Node.js 20+" false
fi

if [ -n "${ANDROID_HOME:-}" ]; then
    SDK_ROOT="$ANDROID_HOME"
elif [ -n "${ANDROID_SDK_ROOT:-}" ]; then
    SDK_ROOT="$ANDROID_SDK_ROOT"
elif [ "$(uname -s)" = "Darwin" ]; then
    SDK_ROOT="$HOME/Library/Android/sdk"
else
    SDK_ROOT="$HOME/Android/Sdk"
fi

check "Android SDK root" test -d "$SDK_ROOT"
check "Android platform-tools (adb)" test -x "$SDK_ROOT/platform-tools/adb"
check "Android Emulator" test -x "$SDK_ROOT/emulator/emulator"
check "Emulator hardware acceleration" sh -c \
    '[ -x "$1" ] && "$1" -accel-check >/dev/null 2>&1' sh "$SDK_ROOT/emulator/emulator"
check "Android API 36 platform" test -f "$SDK_ROOT/platforms/android-36/android.jar"
check "Android build-tools 36.0.0" test -d "$SDK_ROOT/build-tools/36.0.0"

case "$(uname -m)" in
    arm64|aarch64) ABI="arm64-v8a" ;;
    *) ABI="x86_64" ;;
esac
AVD_NAME="${AVD_NAME:-Pixel_6}"
AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.android/avd}"
AVD_CONFIG="$AVD_HOME/$AVD_NAME.avd/config.ini"
check "Practice AVD $AVD_NAME" sh -c '
    config="$1"; abi="$2"
    [ -f "$config" ] || exit 1
    grep -Eq "^abi.type[[:space:]]*=[[:space:]]*$abi[[:space:]]*$" "$config" || exit 1
    grep -Eq "^image.sysdir.1[[:space:]]*=.*android-36.*google_apis.*$abi" "$config" || exit 1
    ram="$(sed -n "s/^hw.ramSize[[:space:]]*=[[:space:]]*//p" "$config" | tail -n 1)"
    number="$(printf "%s" "$ram" | tr -cd "0-9")"
    case "$ram" in *G) number=$((number * 1024)) ;; esac
    [ -n "$number" ] && [ "$number" -ge 1536 ] && [ "$number" -le 4096 ]
' sh "$AVD_CONFIG" "$ABI"
check "Booted practice emulator" sh -c '
    adb="$1"; expected="$2"
    serials="$("$adb" devices | grep -E "^emulator-[0-9]+[[:space:]]+device$" | cut -f1 || true)"
    for serial in $serials; do
        name="$("$adb" -s "$serial" emu avd name 2>/dev/null | head -n 1 | tr -d "\r")"
        booted="$("$adb" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d "\r")"
        [ "$name" = "$expected" ] && [ "$booted" = "1" ] && exit 0
    done
    exit 1
' sh "$SDK_ROOT/platform-tools/adb" "$AVD_NAME"

check "Pinned npm toolchain installed" sh -c \
    '[ -x node_modules/.bin/appium ] && [ -x node_modules/.bin/allure ]'
check "Appium 2.16.2" sh -c \
    '[ -x node_modules/.bin/appium ] && [ "$(node_modules/.bin/appium --version)" = "2.16.2" ]'
check "UiAutomator2 driver 3.9.8" sh -c \
    '[ -x node_modules/.bin/appium ] && node_modules/.bin/appium driver list --installed --json 2>/dev/null | node -e '\''let s=""; process.stdin.on("data", c => s += c); process.stdin.on("end", () => process.exit(JSON.parse(s).uiautomator2?.version === "3.9.8" ? 0 : 1));'\'''
check "Allure CLI 2.43.0" sh -c \
    '[ -x node_modules/.bin/allure ] && [ "$(node_modules/.bin/allure --version)" = "2.43.0" ]'

echo "Environment check summary"
echo "Passed: $ok, failed: $fail"
if [ "$fail" -gt 0 ]; then
    echo "Fix the failed items above."
    echo "Use the remediation printed by this check and rerun the doctor."
    echo "For the AVD run ./scripts/setup-emulator.sh. For Appium and UiAutomator2 run npm ci from the repository root."
    exit 1
fi
echo "Environment is ready. Next: start fake-api and ./scripts/start-appium.sh, then run ./scripts/run-suite.sh"
