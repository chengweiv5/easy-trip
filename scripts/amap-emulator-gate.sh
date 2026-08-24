#!/usr/bin/env bash
set -euo pipefail

serial=
command_line=
evidence=
while (($#)); do
  case "$1" in
    --serial) serial=$2; shift 2 ;;
    --command-line) command_line=$2; shift 2 ;;
    --evidence) evidence=$2; shift 2 ;;
    *) printf 'unknown argument: %s\n' "$1" >&2; exit 64 ;;
  esac
done

[[ -n "$serial" && -n "$command_line" && -n "$evidence" ]] || { printf 'serial, command-line and evidence are required\n' >&2; exit 64; }
mapfile_cmd=$(command -v mapfile || true)
devices=$(adb devices | grep -E '^emulator-[0-9]+[[:space:]]+device$' | cut -f1)
[[ $(printf '%s\n' "$devices" | grep -c .) -eq 1 && "$devices" == "$serial" ]] || { printf 'expected exactly one emulator serial: %s\n' "$serial" >&2; exit 2; }

api=$(adb -s "$serial" shell getprop ro.build.version.sdk | tr -d '\r')
abi=$(adb -s "$serial" shell getprop ro.product.cpu.abi | tr -d '\r')
avd=$(adb -s "$serial" shell getprop ro.boot.qemu.avd_name | tr -d '\r')
boot=$(adb -s "$serial" shell getprop sys.boot_completed | tr -d '\r')
gles=$(adb -s "$serial" shell dumpsys SurfaceFlinger | grep -i -m1 'GLES' || true)
renderer=unknown
printf '%s' "$gles" | grep -qi 'swiftshader' && renderer=swiftshader

[[ "$avd" == trail_map_api36 ]] || { printf 'AVD mismatch: %s\n' "$avd" >&2; exit 3; }
[[ "$api" == 36 ]] || { printf 'API mismatch: %s\n' "$api" >&2; exit 3; }
[[ "$abi" == x86_64 ]] || { printf 'ABI mismatch: %s\n' "$abi" >&2; exit 3; }
[[ "$boot" == 1 ]] || { printf 'device not booted\n' >&2; exit 3; }
[[ "$renderer" == swiftshader ]] || { printf 'renderer mismatch: %s\n' "$gles" >&2; exit 3; }
[[ "$command_line" == *'-avd trail_map_api36'* && "$command_line" == *'-gpu swiftshader'* && "$command_line" == *'-no-snapshot-load'* && "$command_line" == *'-no-snapshot-save'* ]] || { printf 'emulator command mismatch\n' >&2; exit 3; }

mkdir -p "$(dirname "$evidence")"
printf '{"avd":"%s","api":%s,"abi":"%s","serial":"%s","command_line":"%s","renderer":"%s","gles":"%s","cold_boot":true}\n' \
  "$avd" "$api" "$abi" "$serial" "${command_line//\"/\\\"}" "$renderer" "${gles//\"/\\\"}" >"$evidence"
printf 'AMAP_GATE_OK evidence=%s\n' "$evidence"
