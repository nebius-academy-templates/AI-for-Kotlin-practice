#!/usr/bin/env bash
# Provision the Pixel_6 practice AVD, cap it at 2 GB RAM by default, and boot it visibly.
set -eu

cd "$(dirname "$0")/.."

RAM_MB="${RAM_MB:-2048}"
CHECK_ONLY="${CHECK_ONLY:-false}"
NO_START="${NO_START:-false}"

case "$RAM_MB" in
    ''|*[!0-9]*) echo "RAM_MB must be an integer from 1536 to 4096."; exit 1 ;;
esac
[ "$RAM_MB" -ge 1536 ] && [ "$RAM_MB" -le 4096 ] || {
    echo "RAM_MB must be from 1536 to 4096."
    exit 1
}

case "$(uname -m)" in
    arm64|aarch64) ABI="arm64-v8a" ;;
    *) ABI="x86_64" ;;
esac
AVD_NAME="${AVD_NAME:-Pixel_6}"
IMAGE="system-images;android-36;google_apis;$ABI"

if [ -n "${ANDROID_HOME:-}" ]; then
    SDK_ROOT="$ANDROID_HOME"
elif [ -n "${ANDROID_SDK_ROOT:-}" ]; then
    SDK_ROOT="$ANDROID_SDK_ROOT"
elif [ "$(uname -s)" = "Darwin" ]; then
    SDK_ROOT="$HOME/Library/Android/sdk"
else
    SDK_ROOT="$HOME/Android/Sdk"
fi

find_sdk_tool() {
    tool="$1"
    for candidate in \
        "$SDK_ROOT/cmdline-tools/latest/bin/$tool" \
        "$SDK_ROOT"/cmdline-tools/*/bin/"$tool" \
        "$SDK_ROOT/tools/bin/$tool"; do
        [ -x "$candidate" ] && { printf '%s\n' "$candidate"; return 0; }
    done
    return 1
}

SDKMANAGER="$(find_sdk_tool sdkmanager || true)"
AVDMANAGER="$(find_sdk_tool avdmanager || true)"
EMULATOR="$SDK_ROOT/emulator/emulator"
ADB="$SDK_ROOT/platform-tools/adb"

[ -n "$SDKMANAGER" ] && [ -n "$AVDMANAGER" ] || {
    echo "Android SDK Command-line Tools (latest) are missing. Install them in Android Studio."
    exit 1
}
AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.android/avd}"
CONFIG="$AVD_HOME/$AVD_NAME.avd/config.ini"

ini_value() {
    key="$1"
    sed -n "s/^$key[[:space:]]*=[[:space:]]*//p" "$CONFIG" | tail -n 1
}

set_ini_value() {
    key="$1"
    value="$2"
    temp="$CONFIG.tmp"
    awk -v key="$key" -v value="$value" '
        BEGIN { written = 0 }
        $0 ~ "^" key "[[:space:]]*=" {
            if (!written) print key " = " value
            written = 1
            next
        }
        { print }
        END { if (!written) print key " = " value }
    ' "$CONFIG" >"$temp"
    mv "$temp" "$CONFIG"
}

validate_avd() {
    [ -f "$CONFIG" ] || { echo "AVD $AVD_NAME does not exist."; return 1; }
    actual_abi="$(ini_value abi.type)"
    actual_image="$(ini_value image.sysdir.1)"
    actual_ram="$(ini_value hw.ramSize)"
    [ "$actual_abi" = "$ABI" ] || {
        echo "AVD ABI is $actual_abi; expected $ABI for this host."
        return 1
    }
    case "$actual_image" in
        *android-36*google_apis*"$ABI"*) ;;
        *) echo "AVD image is $actual_image; expected API 36 google_apis $ABI."; return 1 ;;
    esac
    ram_number="$(printf '%s' "$actual_ram" | tr -cd '0-9')"
    case "$actual_ram" in *G) ram_number=$((ram_number * 1024)) ;; esac
    [ -n "$ram_number" ] && [ "$ram_number" -ge 1536 ] && [ "$ram_number" -le 4096 ] || {
        echo "AVD RAM is $actual_ram; expected 1536 to 4096 MB."
        return 1
    }
    echo "[OK] $AVD_NAME uses API 36, $ABI and $actual_ram RAM."
}

free_kb() {
    path="$1"
    while [ ! -e "$path" ]; do
        parent="$(dirname "$path")"
        [ "$parent" != "$path" ] || return 1
        path="$parent"
    done
    df -Pk "$path" | awk 'NR == 2 { print $4 }'
}

require_free_space() {
    path="$1"
    required_kb="$2"
    purpose="$3"
    available_kb="$(free_kb "$path")"
    [ -n "$available_kb" ] && [ "$available_kb" -ge "$required_kb" ] || {
        available_gb=$((available_kb / 1024 / 1024))
        required_gb=$((required_kb / 1024 / 1024))
        echo "$purpose needs at least $required_gb GB free; only about $available_gb GB is available."
        exit 1
    }
}

echo "AI QA Sandbox emulator setup"
echo "SDK: $SDK_ROOT"
echo "Host architecture: $(uname -m)"
echo "AVD: $AVD_NAME"
echo "Image: $IMAGE"
echo "RAM: $RAM_MB MB"

if [ "$CHECK_ONLY" = "true" ]; then
    [ -x "$EMULATOR" ] && [ -x "$ADB" ] || {
        echo "Android Emulator or Platform-Tools are missing. Run without CHECK_ONLY=true."
        exit 1
    }
    validate_avd
    exit $?
fi

installed_packages="$("$SDKMANAGER" --list_installed 2>&1 | awk -F'|' '{ value=$1; gsub(/^[[:space:]]+|[[:space:]]+$/, "", value); print value }')"
missing_packages=()
for package in "platform-tools" "emulator" "platforms;android-36" "build-tools;36.0.0" "$IMAGE"; do
    printf '%s\n' "$installed_packages" | grep -Fxq "$package" || missing_packages+=("$package")
done
if [ "${#missing_packages[@]}" -gt 0 ]; then
    require_free_space "$SDK_ROOT" 8388608 "Installing Android SDK packages"
    echo "Installing missing Android SDK packages: ${missing_packages[*]}"
    "$SDKMANAGER" "${missing_packages[@]}" || {
        echo "sdkmanager failed. Review its error above; common causes are unaccepted licenses, insufficient disk space or a network failure."
        exit 1
    }
else
    echo "[OK] Required Android SDK packages are already installed."
fi
[ -x "$EMULATOR" ] && [ -x "$ADB" ] || {
    echo "sdkmanager finished but Emulator or Platform-Tools are still missing in $SDK_ROOT."
    exit 1
}

if ! "$EMULATOR" -list-avds | grep -Fxq "$AVD_NAME"; then
    require_free_space "$AVD_HOME" 8388608 "Creating the practice AVD"
    echo "Creating AVD $AVD_NAME"
    printf 'no\n' | "$AVDMANAGER" create avd --name "$AVD_NAME" --package "$IMAGE" --device pixel_6
else
    echo "Reusing existing AVD $AVD_NAME"
fi

[ -f "$CONFIG" ] || { echo "AVD config was not created at $CONFIG."; exit 1; }
actual_abi="$(ini_value abi.type)"
actual_image="$(ini_value image.sysdir.1)"
if [ "$actual_abi" != "$ABI" ]; then
    echo "Existing AVD $AVD_NAME uses ABI $actual_abi. Delete it in Device Manager and retry."
    exit 1
fi
case "$actual_image" in
    *android-36*google_apis*"$ABI"*) ;;
    *) echo "Existing AVD $AVD_NAME uses image $actual_image. Delete it in Device Manager and retry."; exit 1 ;;
esac
set_ini_value hw.ramSize "$RAM_MB"
echo "[OK] AVD $AVD_NAME is ready with $RAM_MB MB RAM."

[ "$NO_START" = "true" ] && {
    echo "Start it later in Android Studio Device Manager or rerun with NO_START=false."
    exit 0
}

"$EMULATOR" -accel-check || {
    echo "Android Emulator hardware acceleration is unavailable. Enable virtualization and retry."
    exit 1
}

find_running_serial() {
    "$ADB" devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ { print $1 }' | while read -r serial; do
        running_name="$("$ADB" -s "$serial" emu avd name 2>/dev/null | head -n 1 | tr -d '\r')"
        [ "$running_name" = "$AVD_NAME" ] && { printf '%s\n' "$serial"; break; }
    done
}

serial="$(find_running_serial || true)"
if [ -z "$serial" ]; then
    mkdir -p build/verification
    echo "Starting $AVD_NAME in a visible emulator window."
    "$EMULATOR" -avd "$AVD_NAME" >"build/verification/$AVD_NAME.out.log" 2>"build/verification/$AVD_NAME.err.log" &
fi

deadline=$(( $(date +%s) + 180 ))
while [ "$(date +%s)" -lt "$deadline" ]; do
    serial="$(find_running_serial || true)"
    if [ -n "$serial" ] && [ "$("$ADB" -s "$serial" shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; then
        for setting in window_animation_scale transition_animation_scale animator_duration_scale; do
            "$ADB" -s "$serial" shell settings put global "$setting" 0 >/dev/null
        done
        echo "[OK] $AVD_NAME booted as $serial; animations are disabled."
        echo "Next: ./scripts/bootstrap.sh"
        exit 0
    fi
    sleep 2
done

echo "AVD $AVD_NAME did not finish booting within 3 minutes."
echo "Check build/verification/$AVD_NAME.err.log."
exit 1
