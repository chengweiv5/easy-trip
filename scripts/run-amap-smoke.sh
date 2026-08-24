#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
AVD=trail_map_api36
SERIAL=emulator-5588
EVIDENCE_DIR=${AMAP_EVIDENCE_DIR:-"$ROOT/build/amap-smoke"}
LOCK_DIR=${TMPDIR:-/tmp}/easy-trip-amap-emulator.lock
COMMAND="emulator -avd $AVD -port 5588 -gpu swiftshader -no-snapshot-load -no-snapshot-save"
owned_emulator=false

cleanup() {
  if [[ "$owned_emulator" == true ]]; then adb -s "$SERIAL" emu kill >/dev/null 2>&1 || true; fi
  rmdir "$LOCK_DIR" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

mkdir "$LOCK_DIR" 2>/dev/null || { printf 'AMap emulator is globally locked: %s\n' "$LOCK_DIR" >&2; exit 10; }
command -v adb >/dev/null
command -v emulator >/dev/null
emulator -list-avds | grep -Fxq "$AVD" || { printf 'approved AVD is missing: %s\n' "$AVD" >&2; exit 11; }

attached=$(adb devices | grep -E '^(emulator-|[[:alnum:]])[^[:space:]]*[[:space:]]+device$' || true)
[[ -z "$attached" ]] || { printf 'refusing to operate while another device is attached:\n%s\n' "$attached" >&2; exit 12; }

mkdir -p "$EVIDENCE_DIR"
read -r -a emulator_args <<<"$COMMAND"
"${emulator_args[@]}" >"$EVIDENCE_DIR/emulator.log" 2>&1 &
owned_emulator=true
adb -s "$SERIAL" wait-for-device
for _ in $(seq 1 180); do
  [[ "$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]] && break
  sleep 1
done
[[ "$(adb -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r')" == 1 ]] || { printf 'emulator boot timed out\n' >&2; exit 13; }

"$ROOT/scripts/amap-emulator-gate.sh" --serial "$SERIAL" --command-line "$COMMAND" --evidence "$EVIDENCE_DIR/environment.json"
"$ROOT/gradlew" :app:installDebug :app:installDebugAndroidTest
adb -s "$SERIAL" shell am instrument -w -e class com.yangchengwei.easytrip.amap.AmapMapViewAttachSmokeTest com.yangchengwei.easytrip.test/androidx.test.runner.AndroidJUnitRunner | tee "$EVIDENCE_DIR/instrumentation.txt"
