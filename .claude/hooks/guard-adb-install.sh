#!/bin/sh
set -eu

command=$(jq -r '.tool_input.command // ""' | tr '[:upper:]' '[:lower:]')

case "$command" in
  *"adb"*" install "*)
    case "$command" in
      *"-s emulator-"*|*"android_serial=emulator-"*) exit 0 ;;
    esac
    if [ -f .claude/device-install-approved ]; then
      rm .claude/device-install-approved
      exit 0
    fi
    printf '%s\n' '{"continue":false,"stopReason":"真机安装默认禁止。请先询问用户是否有其他项目在并行使用该真实设备；仅在用户明确确认后，创建 .claude/device-install-approved 一次性标记再重试。模拟器安装请显式指定 -s emulator-<serial>。"}'
    ;;
esac
