#!/usr/bin/env bash
set -uo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
GATE="$ROOT/scripts/amap-emulator-gate.sh"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

fail() { printf 'FAIL: %s\n' "$1" >&2; exit 1; }

write_adb() {
  cat >"$TMP/adb" <<EOF
#!/usr/bin/env bash
case "\$*" in
  devices*) printf 'List of devices attached\nemulator-5554\tdevice\n' ;;
  *getprop\\ ro.build.version.sdk*) printf '%s\n' "${API:-36}" ;;
  *getprop\\ ro.product.cpu.abi*) printf '%s\n' "${ABI:-x86_64}" ;;
  *getprop\\ ro.boot.qemu.avd_name*) printf '%s\n' "${AVD:-trail_map_api36}" ;;
  *getprop\\ sys.boot_completed*) printf '1\n' ;;
  *dumpsys\\ SurfaceFlinger*) printf 'GLES: %s\n' "${GLES:-Google SwiftShader}" ;;
  *shell\\ pidof*) printf '4242\n' ;;
  *) exit 2 ;;
esac
EOF
  chmod +x "$TMP/adb"
}

write_adb
PATH="$TMP:$PATH" "$GATE" --serial emulator-5554 --command-line "emulator -avd trail_map_api36 -gpu swiftshader -no-snapshot-load -no-snapshot-save" --evidence "$TMP/good.json" >/dev/null || fail "approved environment rejected"
grep -q '"cold_boot":true' "$TMP/good.json" || fail "cold boot evidence missing"
grep -q '"renderer":"swiftshader"' "$TMP/good.json" || fail "renderer evidence missing"

API=35 write_adb
if PATH="$TMP:$PATH" "$GATE" --serial emulator-5554 --command-line "emulator -avd trail_map_api36 -gpu swiftshader -no-snapshot-load -no-snapshot-save" --evidence "$TMP/bad.json" >/dev/null 2>&1; then
  fail "wrong API accepted"
fi

API=36 GLES='ANGLE (NVIDIA)' write_adb
if PATH="$TMP:$PATH" "$GATE" --serial emulator-5554 --command-line "emulator -avd trail_map_api36 -gpu swiftshader -no-snapshot-load -no-snapshot-save" --evidence "$TMP/bad-renderer.json" >/dev/null 2>&1; then
  fail "wrong renderer accepted"
fi

printf 'PASS: amap emulator gate fixtures\n'
