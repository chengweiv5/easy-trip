#!/usr/bin/env bash
set -euo pipefail

serial=
command_line=
evidence=
while (($#)); do
  (($# >= 2)) || { printf 'missing value for %s\n' "$1" >&2; exit 64; }
  case "$1" in
    --serial) serial=$2 ;;
    --command-line) command_line=$2 ;;
    --evidence) evidence=$2 ;;
    *) printf 'unknown argument: %s\n' "$1" >&2; exit 64 ;;
  esac
  shift 2
done
[[ -n "$serial" && -n "$command_line" && -n "$evidence" ]] || { printf 'serial, command-line and evidence are required\n' >&2; exit 64; }

read -r -a command_args <<<"$command_line"
[[ ${command_args[0]:-} == emulator ]] || { printf 'invalid emulator executable\n' >&2; exit 3; }
avd_arg= gpu_arg= port_arg= snapshot_load=false snapshot_save=false
for ((i=1; i<${#command_args[@]}; i++)); do
  case "${command_args[i]}" in
    -avd|-gpu|-port)
      ((i + 1 < ${#command_args[@]})) || { printf 'missing command value\n' >&2; exit 3; }
      value=${command_args[++i]}
      case "${command_args[i-1]}" in -avd) avd_arg=$value ;; -gpu) gpu_arg=$value ;; -port) port_arg=$value ;; esac
      ;;
    -no-snapshot-load) snapshot_load=true ;;
    -no-snapshot-save) snapshot_save=true ;;
    *) printf 'unexpected emulator argument: %s\n' "${command_args[i]}" >&2; exit 3 ;;
  esac
done
[[ "$avd_arg" == trail_map_api36 && "$gpu_arg" == swiftshader && "$snapshot_load" == true && "$snapshot_save" == true ]] || { printf 'emulator command mismatch\n' >&2; exit 3; }
[[ "$serial" =~ ^emulator-([0-9]+)$ && "$port_arg" == "${BASH_REMATCH[1]}" ]] || { printf 'serial/port mismatch\n' >&2; exit 3; }

devices=$(adb devices | grep -E '^emulator-[0-9]+[[:space:]]+device$' | cut -f1 || true)
[[ $(printf '%s\n' "$devices" | grep -c .) -eq 1 && "$devices" == "$serial" ]] || { printf 'expected exactly one emulator serial: %s\n' "$serial" >&2; exit 2; }
api=$(adb -s "$serial" shell getprop ro.build.version.sdk | tr -d '\r')
abi=$(adb -s "$serial" shell getprop ro.product.cpu.abi | tr -d '\r')
avd=$(adb -s "$serial" shell getprop ro.boot.qemu.avd_name | tr -d '\r')
boot=$(adb -s "$serial" shell getprop sys.boot_completed | tr -d '\r')
gles=$(adb -s "$serial" shell dumpsys SurfaceFlinger | grep -i -m1 'GLES' || true)
printf '%s' "$gles" | grep -qi 'swiftshader' || { printf 'renderer mismatch: %s\n' "$gles" >&2; exit 3; }
[[ "$avd" == trail_map_api36 ]] || { printf 'AVD mismatch: %s\n' "$avd" >&2; exit 3; }
[[ "$api" == 36 ]] || { printf 'API mismatch: %s\n' "$api" >&2; exit 3; }
[[ "$abi" == x86_64 ]] || { printf 'ABI mismatch: %s\n' "$abi" >&2; exit 3; }
[[ "$boot" == 1 ]] || { printf 'device not booted\n' >&2; exit 3; }

mkdir -p "$(dirname "$evidence")"
python3 - "$evidence" "$avd" "$api" "$abi" "$serial" "$command_line" "$gles" <<'PY'
import json, sys
path, avd, api, abi, serial, command_line, gles = sys.argv[1:]
with open(path, "w", encoding="utf-8") as output:
    json.dump({"avd": avd, "api": int(api), "abi": abi, "serial": serial, "command_line": command_line, "renderer": "swiftshader", "gles": gles, "cold_boot": True}, output, ensure_ascii=False)
    output.write("\n")
PY
printf 'AMAP_GATE_OK evidence=%s\n' "$evidence"
