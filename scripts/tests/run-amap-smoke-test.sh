#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
fail() { printf 'FAIL: %s\n' "$1" >&2; exit 1; }

make_fixture() {
  local mode=$1 dir="$TMP/$1"
  mkdir -p "$dir/bin" "$dir/repo/scripts"
  cp "$ROOT/scripts/run-amap-smoke.sh" "$dir/repo/scripts/"
  cp "$ROOT/scripts/amap-emulator-gate.sh" "$dir/repo/scripts/"
  printf 'AMAP_API_KEY=test-key\n' >"$dir/repo/local.properties"
  cat >"$dir/repo/gradlew" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
  cat >"$dir/bin/emulator" <<EOF
#!/usr/bin/env bash
if [[ "\${1:-}" == -list-avds ]]; then printf 'trail_map_api36\n'; exit 0; fi
[[ "$mode" == startup-fail ]] && exit 7
trap 'exit 0' TERM INT
while true; do sleep 1; done
EOF
  cat >"$dir/bin/adb" <<EOF
#!/usr/bin/env bash
printf '%s\n' "\$*" >>"$dir/adb.calls"
case "\$*" in
  devices*) printf 'List of devices attached\n' ;;
  *get-state*) [[ "$mode" == timeout ]] && exit 1; printf 'device\n' ;;
  *ro.boot.qemu.avd_name*) printf 'trail_map_api36\n' ;;
  *sys.boot_completed*) printf '1\n' ;;
  *ro.build.version.sdk*) printf '36\n' ;;
  *ro.product.cpu.abi*) printf 'x86_64\n' ;;
  *dumpsys\\ SurfaceFlinger*) printf 'GLES: Google SwiftShader\n' ;;
  *logcat\\ -d*) [[ "$mode" == marker-missing ]] || printf 'I AMAP_SMOKE: map_loaded=true\nI AMAP_SMOKE: screenshot_ready=true\nI AMAP_SMOKE: lifecycle_cleanup=true\n' ;;
  *exec-out\\ run-as*) printf 'png' ;;
  *am\\ instrument*) printf 'OK (1 test)\n' ;;
  *) exit 0 ;;
esac
EOF
  chmod +x "$dir/repo/gradlew" "$dir/bin/adb" "$dir/bin/emulator"
}

make_fixture startup-fail
if TMPDIR="$TMP/startup-fail/tmp" PATH="$TMP/startup-fail/bin:$PATH" AMAP_BOOT_TIMEOUT_SECONDS=1 "$TMP/startup-fail/repo/scripts/run-amap-smoke.sh" >/dev/null 2>&1; then fail 'startup failure accepted'; fi
[[ ! -f "$TMP/startup-fail/adb.calls" ]] || ! grep -q 'emu kill' "$TMP/startup-fail/adb.calls" || fail 'startup failure killed external serial'

make_fixture lock
mkdir -p "$TMP/lock/tmp/easy-trip-android-device-emulator-5588.lock"
printf 'pid=999999\n' >"$TMP/lock/tmp/easy-trip-android-device-emulator-5588.lock/owner"
if TMPDIR="$TMP/lock/tmp" PATH="$TMP/lock/bin:$PATH" "$TMP/lock/repo/scripts/run-amap-smoke.sh" >/dev/null 2>&1; then fail 'occupied lock accepted'; fi
[[ -d "$TMP/lock/tmp/easy-trip-android-device-emulator-5588.lock" ]] || fail 'occupied lock removed'

make_fixture timeout
mkdir -p "$TMP/timeout/tmp"
if TMPDIR="$TMP/timeout/tmp" PATH="$TMP/timeout/bin:$PATH" AMAP_BOOT_TIMEOUT_SECONDS=1 "$TMP/timeout/repo/scripts/run-amap-smoke.sh" >/dev/null 2>&1; then fail 'timeout accepted'; fi

make_fixture marker-missing
mkdir -p "$TMP/marker-missing/tmp"
if TMPDIR="$TMP/marker-missing/tmp" PATH="$TMP/marker-missing/bin:$PATH" AMAP_BOOT_TIMEOUT_SECONDS=1 "$TMP/marker-missing/repo/scripts/run-amap-smoke.sh" >/dev/null 2>&1; then fail 'missing marker accepted'; fi

printf 'PASS: amap smoke runner fixtures\n'
