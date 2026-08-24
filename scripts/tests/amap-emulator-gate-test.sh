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
  devices*) printf 'List of devices attached\n%s' "${DEVICES:-emulator-5554	device\\n}" | printf '%b' "\$(cat)" ;;
  *getprop\\ ro.build.version.sdk*) printf '%s\n' "${API:-36}" ;;
  *getprop\\ ro.product.cpu.abi*) printf '%s\n' "${ABI:-x86_64}" ;;
  *getprop\\ ro.boot.qemu.avd_name*) printf '%s\n' "${AVD:-trail_map_api36}" ;;
  *getprop\\ sys.boot_completed*) printf '%s\n' "${BOOT:-1}" ;;
  *dumpsys\\ SurfaceFlinger*) printf 'GLES: %s\n' "${GLES:-Google SwiftShader}" ;;
  *) exit 2 ;;
esac
EOF
  chmod +x "$TMP/adb"
}

expect_rejected() {
  local name=$1 expected=$2; shift 2
  set +e
  PATH="$TMP:$PATH" "$GATE" "$@" >/dev/null 2>&1
  local status=$?
  set -e
  [[ $status -eq $expected ]] || fail "$name returned $status, expected $expected"
}

args=(--serial emulator-5554 --command-line "emulator -avd trail_map_api36 -port 5554 -gpu swiftshader -no-snapshot-load -no-snapshot-save" --evidence "$TMP/good.json")
write_adb
PATH="$TMP:$PATH" "$GATE" "${args[@]}" >/dev/null || fail "approved environment rejected"
python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert d["cold_boot"] is True and d["renderer"] == "swiftshader"' "$TMP/good.json" || fail "invalid JSON evidence"

API=35 write_adb; expect_rejected "wrong API" 3 "${args[@]}"
API=36 ABI=arm64-v8a write_adb; expect_rejected "wrong ABI" 3 "${args[@]}"
ABI=x86_64 AVD=trail_map_api360 write_adb; expect_rejected "wrong AVD" 3 "${args[@]}"
AVD=trail_map_api36 BOOT=0 write_adb; expect_rejected "incomplete boot" 3 "${args[@]}"
BOOT=1 GLES='ANGLE (NVIDIA)' write_adb; expect_rejected "wrong renderer" 3 "${args[@]}"
GLES='Google SwiftShader' DEVICES='emulator-5554\tdevice\nemulator-5556\tdevice\n' write_adb; expect_rejected "multiple serials" 2 "${args[@]}"
DEVICES='emulator-5556\tdevice\n' write_adb; expect_rejected "wrong serial" 2 "${args[@]}"
DEVICES='emulator-5554\tdevice\n' write_adb
expect_rejected "AVD command prefix collision" 3 --serial emulator-5554 --command-line "emulator -avd trail_map_api360 -port 5554 -gpu swiftshader -no-snapshot-load -no-snapshot-save" --evidence "$TMP/bad.json"
expect_rejected "renderer command prefix collision" 3 --serial emulator-5554 --command-line "emulator -avd trail_map_api36 -port 5554 -gpu swiftshader_indirect -no-snapshot-load -no-snapshot-save" --evidence "$TMP/bad.json"
expect_rejected "snapshot load omission" 3 --serial emulator-5554 --command-line "emulator -avd trail_map_api36 -port 5554 -gpu swiftshader -no-snapshot-save" --evidence "$TMP/bad.json"
expect_rejected "snapshot save omission" 3 --serial emulator-5554 --command-line "emulator -avd trail_map_api36 -port 5554 -gpu swiftshader -no-snapshot-load" --evidence "$TMP/bad.json"
expect_rejected "duplicate AVD" 3 --serial emulator-5554 --command-line "emulator -avd trail_map_api36 -avd trail_map_api36 -port 5554 -gpu swiftshader -no-snapshot-load -no-snapshot-save" --evidence "$TMP/bad.json"
expect_rejected "duplicate renderer" 3 --serial emulator-5554 --command-line "emulator -avd trail_map_api36 -port 5554 -gpu swiftshader -gpu swiftshader -no-snapshot-load -no-snapshot-save" --evidence "$TMP/bad.json"
expect_rejected "duplicate port" 3 --serial emulator-5554 --command-line "emulator -avd trail_map_api36 -port 5554 -port 5554 -gpu swiftshader -no-snapshot-load -no-snapshot-save" --evidence "$TMP/bad.json"
expect_rejected "duplicate snapshot flag" 3 --serial emulator-5554 --command-line "emulator -avd trail_map_api36 -port 5554 -gpu swiftshader -no-snapshot-load -no-snapshot-load -no-snapshot-save" --evidence "$TMP/bad.json"
expect_rejected "conflicting snapshot flag" 3 --serial emulator-5554 --command-line "emulator -avd trail_map_api36 -port 5554 -gpu swiftshader -snapshot-load -no-snapshot-load -no-snapshot-save" --evidence "$TMP/bad.json"
expect_rejected "unknown parameter" 64 "${args[@]}" --surprise value

printf 'PASS: amap emulator gate fixtures\n'
