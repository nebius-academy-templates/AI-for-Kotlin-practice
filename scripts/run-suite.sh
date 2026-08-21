#!/usr/bin/env bash
# Builds the APK and runs the Appium suite against a running emulator.
# Assumes: emulator is booted, appium server is running on 127.0.0.1:4723.
set -eu

cd "$(dirname "$0")/.."

SUITE_LOG="$PWD/suite-run.log"
exec > >(tee "$SUITE_LOG") 2>&1

FLAVOR="${FLAVOR:-stable}"
case "$FLAVOR" in
    stable | redesign) ;;
    *)
        echo "Unsupported FLAVOR '$FLAVOR'; expected stable or redesign."
        exit 2
        ;;
esac
# Capitalize first letter portably (macOS ships bash 3.2, which lacks ${x^}).
FLAVOR_CAP="$(printf '%s' "${FLAVOR%"${FLAVOR#?}"}" | tr '[:lower:]' '[:upper:]')${FLAVOR#?}"

[ -x "node_modules/.bin/appium" ] && [ -x "node_modules/.bin/allure" ] || {
    echo "The pinned local Appium/Allure toolchain is missing. Run npm ci in $PWD."
    exit 1
}

if ! curl --fail --silent --show-error --max-time 2 \
    "http://127.0.0.1:8080/swagger" >/dev/null 2>&1; then
    echo "fake-api is not ready on port 8080."
    echo "Start it in another terminal: ./gradlew :fake-api:run"
    exit 1
fi

APPIUM_STATUS="$(curl --fail --silent --show-error --max-time 2 \
    "http://127.0.0.1:4723/status" 2>/dev/null || true)"
[ -n "$APPIUM_STATUS" ] || {
    echo "Appium is not ready on port 4723."
    echo "Start it in another terminal: ./scripts/start-appium.sh"
    exit 1
}
printf '%s' "$APPIUM_STATUS" | grep -Eq '"version"[[:space:]]*:[[:space:]]*"2\.16\.2"' || {
    echo "The running Appium server is not pinned version 2.16.2."
    echo "Stop it and run ./scripts/start-appium.sh from this repository."
    exit 1
}

echo "Building $FLAVOR debug APK"
./gradlew ":app:assemble${FLAVOR_CAP}Debug"

echo "Checking connected devices"
CONNECTED_DEVICES="$(adb devices | awk '/^[^[:space:]]+[[:space:]]+device$/ && $1 != "List" { print $1 }')"
if [ -n "${DEVICE:-}" ]; then
    printf '%s\n' "$CONNECTED_DEVICES" | grep -Fxq "$DEVICE" || {
        echo "Requested device $DEVICE is not connected."
        exit 1
    }
else
    device_count="$(printf '%s\n' "$CONNECTED_DEVICES" | sed '/^$/d' | wc -l | tr -d '[:space:]')"
    [ "$device_count" -gt 0 ] || {
        echo "No booted emulator/device found. Start one and retry."
        exit 1
    }
    [ "$device_count" -eq 1 ] || {
        echo "More than one device is connected: $(printf '%s' "$CONNECTED_DEVICES" | tr '\n' ' ')"
        echo "Set DEVICE to one serial explicitly."
        exit 1
    }
    DEVICE="$CONNECTED_DEVICES"
fi
[ "$(adb -s "$DEVICE" shell getprop sys.boot_completed | tr -d '\r')" = "1" ] || {
    echo "Device $DEVICE has not completed boot."
    exit 1
}
echo "Disabling animations (UiAutomator2 can't locate elements under a live overlay animation, e.g. the nav drawer)"
for s in window_animation_scale transition_animation_scale animator_duration_scale; do
    adb -s "$DEVICE" shell settings put global "$s" 0 >/dev/null 2>&1 || true
done

echo "Resetting sandbox states"
adb -s "$DEVICE" shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver --ez reset true >/dev/null 2>&1 || true

echo "Running Appium suite against $FLAVOR APK"
# Fresh Allure results per run: the directory accumulates across runs, and a
# report generated from mixed runs misreports the run it claims to describe.
RESULTS_DIR="appium-tests/build/allure-results"
REPORT_DIR="appium-tests/build/reports/allure-report"
JUNIT_DIR="appium-tests/build/test-results/test"
rm -rf "$RESULTS_DIR" "$JUNIT_DIR"

suite_rc=0
./gradlew :appium-tests:test --rerun \
    -Dapp.apk="$PWD/app/build/outputs/apk/$FLAVOR/debug/app-$FLAVOR-debug.apk" \
    -Dui.variant="$FLAVOR" \
    -Dappium.devices="$DEVICE" "$@" || suite_rc=$?

# Static HTML report from the Allure results, also (especially) when the run
# is red. Prefers an installed allure CLI, falls back to the npm-pinned CLI. rm instead of
# --clean keeps this compatible with both Allure 2 and Allure 3.
if [ -d "$RESULTS_DIR" ]; then
    echo "Generating Allure HTML report"
    rm -rf "$REPORT_DIR"
    node_modules/.bin/allure generate "$RESULTS_DIR" -o "$REPORT_DIR" ||
        echo "Report generation failed; raw results remain in $RESULTS_DIR"
    [ -f "$REPORT_DIR/index.html" ] && echo "Allure report: $REPORT_DIR/index.html"
fi

exit "$suite_rc"
