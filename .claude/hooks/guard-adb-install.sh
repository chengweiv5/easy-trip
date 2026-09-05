#!/bin/sh
set -eu

command=$(jq -r '.tool_input.command // ""' | tr '[:upper:]' '[:lower:]')

case "$command" in
  *"adb"*" install "*)
    case "$command" in
      *"-s emulator-"*|*"android_serial=emulator-"*) exit 0 ;;
      *" install -r "*) exit 0 ;;
    esac
    printf '%s\n' '{"continue":false,"stopReason":"Easy Trip 真机仅允许通过 adb install -r 覆盖安装；禁止卸载、清数据或非覆盖安装。模拟器安装请显式指定 -s emulator-<serial>。"}'
    ;;
esac
