#!/usr/bin/env bash
set -euo pipefail

serial=
command_line=
evidence=
host_arch=
image_package=
emulator_version=
serial_count=0
command_line_count=0
evidence_count=0
host_arch_count=0
image_package_count=0
emulator_version_count=0
while (($#)); do
  (($# >= 2)) || { printf 'missing value for %s\n' "$1" >&2; exit 64; }
  case "$1" in
    --serial) serial=$2; ((serial_count += 1)) ;;
    --command-line) command_line=$2; ((command_line_count += 1)) ;;
    --evidence) evidence=$2; ((evidence_count += 1)) ;;
    --host-arch) host_arch=$2; ((host_arch_count += 1)) ;;
    --image-package) image_package=$2; ((image_package_count += 1)) ;;
    --emulator-version) emulator_version=$2; ((emulator_version_count += 1)) ;;
    *) printf 'unknown argument: %s\n' "$1" >&2; exit 64 ;;
  esac
  shift 2
done
[[ $serial_count -eq 1 && $command_line_count -eq 1 && $evidence_count -eq 1 && $host_arch_count -eq 1 && $image_package_count -eq 1 && $emulator_version_count -eq 1 && -n "$serial" && -n "$command_line" && -n "$evidence" && -n "$host_arch" && -n "$image_package" && -n "$emulator_version" ]] || { printf 'serial, command-line, evidence, host-arch, image-package and emulator-version are each required exactly once\n' >&2; exit 64; }
[[ "$host_arch" == arm64 ]] || { printf 'host architecture mismatch: %s\n' "$host_arch" >&2; exit 3; }
[[ "$image_package" == 'system-images;android-36;google_apis;arm64-v8a' ]] || { printf 'AVD image package mismatch: %s\n' "$image_package" >&2; exit 3; }

read -r -a command_args <<<"$command_line"
[[ ${command_args[0]:-} == emulator ]] || { printf 'invalid emulator executable\n' >&2; exit 3; }
avd_arg= gpu_arg= port_arg=
avd_count=0 gpu_count=0 port_count=0 snapshot_load_count=0 snapshot_save_count=0
for ((i=1; i<${#command_args[@]}; i++)); do
  case "${command_args[i]}" in
    -avd|-gpu|-port)
      ((i + 1 < ${#command_args[@]})) || { printf 'missing command value\n' >&2; exit 3; }
      value=${command_args[++i]}
      case "${command_args[i-1]}" in
        -avd) avd_arg=$value; ((avd_count += 1)) ;;
        -gpu) gpu_arg=$value; ((gpu_count += 1)) ;;
        -port) port_arg=$value; ((port_count += 1)) ;;
      esac
      ;;
    -no-snapshot-load) ((snapshot_load_count += 1)) ;;
    -no-snapshot-save) ((snapshot_save_count += 1)) ;;
    -snapshot|-snapshot-load|-snapshot-save) printf 'conflicting snapshot argument: %s\n' "${command_args[i]}" >&2; exit 3 ;;
    *) printf 'unexpected emulator argument: %s\n' "${command_args[i]}" >&2; exit 3 ;;
  esac
done
[[ $avd_count -eq 1 && $gpu_count -eq 1 && $port_count -eq 1 && $snapshot_load_count -eq 1 && $snapshot_save_count -eq 1 ]] || { printf 'emulator arguments must occur exactly once\n' >&2; exit 3; }
[[ "$avd_arg" == trail_map_api36 && "$gpu_arg" == swiftshader ]] || { printf 'emulator command mismatch\n' >&2; exit 3; }
[[ "$serial" =~ ^emulator-([0-9]+)$ && "$port_arg" == "${BASH_REMATCH[1]}" ]] || { printf 'serial/port mismatch\n' >&2; exit 3; }

devices=$(adb devices | grep -E '^emulator-[0-9]+[[:space:]]+device$' | cut -f1 || true)
[[ $(printf '%s\n' "$devices" | grep -c .) -eq 1 && "$devices" == "$serial" ]] || { printf 'expected exactly one emulator serial: %s\n' "$serial" >&2; exit 2; }
api=$(adb -s "$serial" shell getprop ro.build.version.sdk | tr -d '\r')
abi=$(adb -s "$serial" shell getprop ro.product.cpu.abi | tr -d '\r')
avd=$(adb -s "$serial" shell getprop ro.boot.qemu.avd_name | tr -d '\r')
boot=$(adb -s "$serial" shell getprop sys.boot_completed | tr -d '\r')
gles=$(adb -s "$serial" shell dumpsys SurfaceFlinger | grep -i 'GLES' || true)
printf '%s' "$gles" | grep -qi 'swiftshader' || { printf 'renderer mismatch: %s\n' "$gles" >&2; exit 3; }
[[ "$avd" == trail_map_api36 ]] || { printf 'AVD mismatch: %s\n' "$avd" >&2; exit 3; }
[[ "$api" == 36 ]] || { printf 'API mismatch: %s\n' "$api" >&2; exit 3; }
[[ "$abi" == arm64-v8a ]] || { printf 'ABI mismatch: %s\n' "$abi" >&2; exit 3; }
[[ "$boot" == 1 ]] || { printf 'device not booted\n' >&2; exit 3; }

mkdir -p "$(dirname "$evidence")"
python3 - "$evidence" "$avd" "$api" "$abi" "$serial" "$command_line" "$gles" "$host_arch" "$image_package" "$emulator_version" <<'PY'
import json, sys
path, avd, api, abi, serial, command_line, gles, host_arch, image_package, emulator_version = sys.argv[1:]
with open(path, "w", encoding="utf-8") as output:
    json.dump({"avd": avd, "api": int(api), "abi": abi, "serial": serial, "command_line": command_line, "renderer": "swiftshader", "gles": gles, "cold_boot": True, "host_arch": host_arch, "image_package": image_package, "emulator_version": emulator_version}, output, ensure_ascii=False)
    output.write("\n")
PY
printf 'AMAP_GATE_OK evidence=%s\n' "$evidence"
