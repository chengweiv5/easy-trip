#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
AVD=trail_map_api36
SERIAL=emulator-5588
EVIDENCE_ROOT=${AMAP_EVIDENCE_DIR:-"$ROOT/build/amap-smoke"}
RUN_ID=$(date -u +%Y%m%dT%H%M%SZ)-$$-$RANDOM
RUN_TOKEN="amap-$RUN_ID"
EVIDENCE_DIR="$EVIDENCE_ROOT/$RUN_ID"
LOCK_DIR=${TMPDIR:-/tmp}/easy-trip-android-device-$SERIAL.lock
COMMAND="emulator -avd $AVD -port 5588 -gpu swiftshader -no-snapshot-load -no-snapshot-save"
BOOT_TIMEOUT_SECONDS=${AMAP_BOOT_TIMEOUT_SECONDS:-180}
emulator_pid=
emulator_identity=
owned_lock=false
cleaned=false
lock_token=

process_identity() {
  local snapshot weekday month day time year command
  snapshot=$(LC_ALL=C "${AMAP_PS_BIN:-ps}" -p "$1" -o lstart= -o command= 2>/dev/null | tr -s ' ' | sed 's/^ //') || return 1
  read -r weekday month day time year command <<<"$snapshot"
  [[ -n "$command" && "$snapshot" == *"$AVD"* && "$snapshot" == *"-port 5588"* ]] || return 1
  printf '%s %s %s %s %s\n' "$weekday" "$month" "$day" "$time" "$year"
}

same_process_identity() {
  [[ -n "$1" && "$1" == "$2" ]]
}

cleanup() {
  [[ "$cleaned" == false ]] || return 0
  cleaned=true
  if [[ "$emulator_pid" =~ ^[0-9]+$ ]] && kill -0 "$emulator_pid" 2>/dev/null; then
    current_identity=$(process_identity "$emulator_pid" || true)
    if same_process_identity "$emulator_identity" "$current_identity"; then
      kill "$emulator_pid" 2>/dev/null || true
      for _ in 1 2 3 4 5; do
        kill -0 "$emulator_pid" 2>/dev/null || break
        sleep 0.2
      done
      if kill -0 "$emulator_pid" 2>/dev/null; then
        current_identity=$(process_identity "$emulator_pid" || true)
        if same_process_identity "$emulator_identity" "$current_identity"; then
          kill -KILL "$emulator_pid" 2>/dev/null || true
          for _ in 1 2 3 4 5; do
            kill -0 "$emulator_pid" 2>/dev/null || break
            sleep 0.2
          done
        else
          printf 'warning: emulator PID identity changed after TERM; refusing KILL pid %s\n' "$emulator_pid" >&2
        fi
      fi
    else
      printf 'warning: emulator PID identity changed; refusing to kill pid %s\n' "$emulator_pid" >&2
    fi
  fi
  if [[ "$owned_lock" == true ]]; then
    current_lock_token=$(grep -E '^token=' "$LOCK_DIR/owner" 2>/dev/null | cut -d= -f2- || true)
    if [[ -n "$lock_token" && "$current_lock_token" == "$lock_token" ]]; then
      rm -f "$LOCK_DIR/owner" 2>/dev/null || true
      rmdir "$LOCK_DIR" 2>/dev/null || true
    else
      printf 'warning: lock ownership changed; refusing to release %s\n' "$LOCK_DIR" >&2
    fi
    owned_lock=false
  fi
}

handle_signal() {
  local status=$1
  trap - EXIT INT TERM
  cleanup
  exit "$status"
}
trap cleanup EXIT
trap 'handle_signal 130' INT
trap 'handle_signal 143' TERM

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
  printf 'Android device is locked: %s%s; verify ownership and remove it manually if stale\n' "$LOCK_DIR" "${owner:+ (pid $owner)}" >&2
  exit 10
fi
owned_lock=true
lock_token="$RUN_TOKEN-$RANDOM"
printf 'token=%s\npid=%s\nstarted=%s\nserial=%s\n' "$lock_token" "$$" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$SERIAL" >"$LOCK_DIR/owner"
command -v adb >/dev/null
command -v emulator >/dev/null
emulator -list-avds | grep -Fxq "$AVD" || { printf 'approved AVD is missing: %s\n' "$AVD" >&2; exit 11; }
attached=$(adb devices | grep -E '^[^[:space:]]+[[:space:]]+device$' || true)
[[ -z "$attached" ]] || { printf 'refusing to operate while another device is attached:\n%s\n' "$attached" >&2; exit 12; }

mkdir -p "$EVIDENCE_DIR"
read -r -a emulator_args <<<"$COMMAND"
"${emulator_args[@]}" >"$EVIDENCE_DIR/emulator.log" 2>&1 &
emulator_pid=$!
emulator_identity=$(process_identity "$emulator_pid" || true)
[[ -n "$emulator_identity" ]] || { printf 'unable to establish emulator process identity\n' >&2; exit 13; }
printf 'emulator_pid=%s\nemulator_identity=%s\n' "$emulator_pid" "$emulator_identity" >>"$LOCK_DIR/owner"
deadline=$((SECONDS + BOOT_TIMEOUT_SECONDS))
while ((SECONDS < deadline)); do
  kill -0 "$emulator_pid" 2>/dev/null || { printf 'emulator process exited during startup\n' >&2; exit 13; }
  if [[ "$(adb -s "$SERIAL" get-state 2>/dev/null || true)" == device && "$(adb -s "$SERIAL" shell getprop ro.boot.qemu.avd_name 2>/dev/null | tr -d '\r')" == "$AVD" ]]; then break; fi
  sleep 1
done
kill -0 "$emulator_pid" 2>/dev/null || { printf 'emulator process exited during startup\n' >&2; exit 13; }
[[ "$(adb -s "$SERIAL" get-state 2>/dev/null || true)" == device && "$(adb -s "$SERIAL" shell getprop ro.boot.qemu.avd_name 2>/dev/null | tr -d '\r')" == "$AVD" ]] || { printf 'owned emulator registration timed out\n' >&2; exit 13; }
while ((SECONDS < deadline)); do
  if ! kill -0 "$emulator_pid" 2>/dev/null; then
    printf 'owned emulator process exited before boot completed\n' >&2
    exit 13
  fi
  current_identity=$(process_identity "$emulator_pid" || true)
  same_process_identity "$emulator_identity" "$current_identity" || { printf 'owned emulator process identity changed before boot completed\n' >&2; exit 13; }
  [[ "$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]] && break
  sleep 1
done
if ! kill -0 "$emulator_pid" 2>/dev/null; then
  printf 'owned emulator process exited before boot completed\n' >&2
  exit 13
fi
current_identity=$(process_identity "$emulator_pid" || true)
same_process_identity "$emulator_identity" "$current_identity" || { printf 'owned emulator process identity changed before boot completed\n' >&2; exit 13; }
[[ "$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]] || { printf 'emulator boot timed out\n' >&2; exit 13; }

"$ROOT/scripts/amap-emulator-gate.sh" --serial "$SERIAL" --command-line "$COMMAND" --evidence "$EVIDENCE_DIR/environment.json"
ANDROID_SERIAL="$SERIAL" "$ROOT/gradlew" :app:installDebug :app:installDebugAndroidTest
adb -s "$SERIAL" logcat -c
set +e
adb -s "$SERIAL" shell am instrument -w -e amapRunToken "$RUN_TOKEN" -e class com.yangchengwei.easytrip.amap.AmapMapViewAttachSmokeTest com.yangchengwei.easytrip.test/androidx.test.runner.AndroidJUnitRunner | tee "$EVIDENCE_DIR/instrumentation.txt"
status=${PIPESTATUS[0]}
set -e
adb -s "$SERIAL" logcat -d >"$EVIDENCE_DIR/logcat.txt"
[[ $status -eq 0 ]] || exit "$status"
adb -s "$SERIAL" exec-out run-as com.yangchengwei.easytrip cat "files/amap-smoke-$RUN_TOKEN.png" >"$EVIDENCE_DIR/map-loaded.png"
adb -s "$SERIAL" exec-out run-as com.yangchengwei.easytrip cat "files/amap-smoke-$RUN_TOKEN.json" >"$EVIDENCE_DIR/status.json"
python3 - "$EVIDENCE_DIR/map-loaded.png" "$EVIDENCE_DIR/status.json" "$RUN_TOKEN" <<'PY' || { printf 'AMap smoke app evidence invalid or incomplete\n' >&2; exit 14; }
import json, os, sys
png_path, status_path, token = sys.argv[1:]
with open(png_path, "rb") as image:
    signature = image.read(8)
if signature != b"\x89PNG\r\n\x1a\n" or os.path.getsize(png_path) <= 8:
    raise SystemExit(1)
with open(status_path, encoding="utf-8") as source:
    status = json.load(source)
required = ("map_loaded_at", "stable_alive_at", "screenshot_ready", "lifecycle_cleanup")
if status.get("run_token") != token or not all(status.get(key) for key in required):
    raise SystemExit(1)
if status["stable_alive_at"] - status["map_loaded_at"] < 5000:
    raise SystemExit(1)
PY
grep -q 'OK (1 test)' "$EVIDENCE_DIR/instrumentation.txt" || { printf 'instrumentation did not report success\n' >&2; exit 14; }
printf 'AMAP_SMOKE_COMPLETE run=%s\n' "$EVIDENCE_DIR" | tee "$EVIDENCE_DIR/complete.txt"
