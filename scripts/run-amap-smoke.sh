#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
AVD=trail_map_api36
SERIAL=emulator-5588
EVIDENCE_DIR=${AMAP_EVIDENCE_DIR:-"$ROOT/build/amap-smoke"}
LOCK_DIR=${TMPDIR:-/tmp}/easy-trip-android-device-$SERIAL.lock
COMMAND="emulator -avd $AVD -port 5588 -gpu swiftshader -no-snapshot-load -no-snapshot-save"
BOOT_TIMEOUT_SECONDS=${AMAP_BOOT_TIMEOUT_SECONDS:-180}
owned_emulator=false
owned_lock=false

cleanup() {
  if [[ "$owned_emulator" == true ]]; then adb -s "$SERIAL" emu kill >/dev/null 2>&1 || true; fi
  if [[ "$owned_lock" == true ]]; then
    rm -f "$LOCK_DIR/owner" 2>/dev/null || true
    rmdir "$LOCK_DIR" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

api_key=$(python3 - "$ROOT/local.properties" <<'PY'
import sys
try:
    lines = open(sys.argv[1], encoding="utf-8").read().splitlines()
except FileNotFoundError:
    print("")
    raise SystemExit
values = [line.split("=", 1)[1].strip() for line in lines if line.strip().startswith("AMAP_API_KEY=")]
print(values[-1] if values else "")
PY
)
[[ -n "$api_key" ]] || { printf 'local.properties must contain a non-empty AMAP_API_KEY\n' >&2; exit 9; }

if ! mkdir "$LOCK_DIR" 2>/dev/null; then
  owner=$(test -f "$LOCK_DIR/owner" && grep -E '^pid=' "$LOCK_DIR/owner" | cut -d= -f2 || true)
  if [[ "$owner" =~ ^[0-9]+$ ]] && ! kill -0 "$owner" 2>/dev/null; then
    rm -rf "$LOCK_DIR"
    mkdir "$LOCK_DIR"
  else
    printf 'Android device is locked: %s%s\n' "$LOCK_DIR" "${owner:+ (pid $owner)}" >&2
    exit 10
  fi
fi
owned_lock=true
printf 'pid=%s\nstarted=%s\nserial=%s\n' "$$" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$SERIAL" >"$LOCK_DIR/owner"
command -v adb >/dev/null
command -v emulator >/dev/null
emulator -list-avds | grep -Fxq "$AVD" || { printf 'approved AVD is missing: %s\n' "$AVD" >&2; exit 11; }
attached=$(adb devices | grep -E '^[^[:space:]]+[[:space:]]+device$' || true)
[[ -z "$attached" ]] || { printf 'refusing to operate while another device is attached:\n%s\n' "$attached" >&2; exit 12; }

mkdir -p "$EVIDENCE_DIR"
read -r -a emulator_args <<<"$COMMAND"
"${emulator_args[@]}" >"$EVIDENCE_DIR/emulator.log" 2>&1 &
owned_emulator=true
deadline=$((SECONDS + BOOT_TIMEOUT_SECONDS))
while ((SECONDS < deadline)); do
  [[ "$(adb -s "$SERIAL" get-state 2>/dev/null || true)" == device ]] && break
  sleep 1
done
[[ "$(adb -s "$SERIAL" get-state 2>/dev/null || true)" == device ]] || { printf 'emulator registration timed out\n' >&2; exit 13; }
while ((SECONDS < deadline)); do
  [[ "$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]] && break
  sleep 1
done
[[ "$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]] || { printf 'emulator boot timed out\n' >&2; exit 13; }

"$ROOT/scripts/amap-emulator-gate.sh" --serial "$SERIAL" --command-line "$COMMAND" --evidence "$EVIDENCE_DIR/environment.json"
ANDROID_SERIAL="$SERIAL" "$ROOT/gradlew" :app:installDebug :app:installDebugAndroidTest
adb -s "$SERIAL" logcat -c
set +e
adb -s "$SERIAL" shell am instrument -w -e class com.yangchengwei.easytrip.amap.AmapMapViewAttachSmokeTest com.yangchengwei.easytrip.test/androidx.test.runner.AndroidJUnitRunner | tee "$EVIDENCE_DIR/instrumentation.txt"
status=${PIPESTATUS[0]}
set -e
adb -s "$SERIAL" logcat -d >"$EVIDENCE_DIR/logcat.txt"
adb -s "$SERIAL" exec-out screencap -p >"$EVIDENCE_DIR/final.png"
[[ $status -eq 0 ]] || exit "$status"
grep -q 'OK (1 test)' "$EVIDENCE_DIR/instrumentation.txt" || { printf 'instrumentation did not report success\n' >&2; exit 14; }
grep -q 'AMAP_SMOKE map_loaded=true' "$EVIDENCE_DIR/instrumentation.txt" || { printf 'map-loaded evidence missing\n' >&2; exit 14; }
grep -q 'AMAP_SMOKE lifecycle_cleanup=true' "$EVIDENCE_DIR/instrumentation.txt" || { printf 'lifecycle cleanup evidence missing\n' >&2; exit 14; }
