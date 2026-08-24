#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
fail() { printf 'FAIL: %s\n' "$1" >&2; exit 1; }

make_fixture() {
  local mode=$1 dir="$TMP/$1"
  mkdir -p "$dir/bin" "$dir/repo/scripts" "$dir/tmp" "$dir/evidence" "$dir/avd/trail_map_api36.avd"
  printf 'target=android-36\nimage.sysdir.1=system-images/android-36/google_apis/arm64-v8a/\nabi.type=arm64-v8a\n' >"$dir/avd/trail_map_api36.avd/config.ini"
  printf 'stale' >"$dir/evidence/map-loaded.png"
  cp "$ROOT/scripts/run-amap-smoke.sh" "$dir/repo/scripts/"
  cp "$ROOT/scripts/amap-emulator-gate.sh" "$dir/repo/scripts/"
  if [[ "$mode" == gate-fail || "$mode" == identity-transition || "$mode" == pid-reuse ]]; then
    cat >"$dir/repo/scripts/amap-emulator-gate.sh" <<EOF
#!/usr/bin/env bash
[[ "$mode" == pid-reuse ]] && touch "$dir/pid-reused"
exit 23
EOF
  fi
  printf 'AMAP_API_KEY=test-key\n' >"$dir/repo/local.properties"
  cat >"$dir/repo/gradlew" <<EOF
#!/usr/bin/env bash
[[ "$mode" == gradle-fail ]] && exit 24
exit 0
EOF
  cat >"$dir/bin/emulator" <<EOF
#!/usr/bin/env bash
if [[ "\${1:-}" == -list-avds ]]; then printf 'trail_map_api36\n'; exit 0; fi
if [[ "\${1:-}" == -version ]]; then printf 'Android emulator version 36.1.9.0\n'; exit 0; fi
printf 'started pid=%s args=%s\n' "\$\$" "\$*" >"$dir/emulator.started"
[[ "$mode" == startup-fail ]] && exit 7
trap 'exit 0' TERM INT
while true; do sleep 1; done
EOF
  cat >"$dir/bin/fake-kill" <<EOF
#!/usr/bin/env bash
printf '%s\n' "\$*" >>"$dir/fake-kill.calls"
if [[ "$mode" == boot-exit && "\${1:-}" == -0 && -f "$dir/boot-process-exited" ]]; then exit 1; fi
exec kill "\$@"
EOF
  cat >"$dir/bin/fake-ps" <<EOF
#!/usr/bin/env bash
count=0
[[ -f "$dir/identity-count" ]] && count=\$(cat "$dir/identity-count")
count=\$((count + 1))
printf '%s' "\$count" >"$dir/identity-count"
if [[ "$mode" == pid-reuse && -f "$dir/pid-reused" ]]; then
  printf 'Wed Jan  1 00:00:01 2025 /usr/bin/unrelated-process --serve\n'
elif [[ "$mode" == identity-transition && \$count -ge 2 ]]; then
  printf 'Wed Jan 1 00:00:00 2025 /opt/android/emulator/qemu/darwin-x86_64/qemu-system-x86_64 -avd trail_map_api36 -port 5588 -gpu swiftshader\n'
else
  printf 'Wed Jan  1 00:00:00 2025 /opt/android/emulator/emulator -avd trail_map_api36 -port 5588 -gpu swiftshader\n'
fi
EOF
  cat >"$dir/bin/adb" <<EOF
#!/usr/bin/env bash
printf '%s\n' "\$*" >>"$dir/adb.calls"
case "\$*" in
  devices*) printf 'List of devices attached\n'; [[ -f "$dir/emulator.started" ]] && printf 'emulator-5588\tdevice\n' ;;
  *get-state*) [[ "$mode" == timeout ]] && exit 1; printf 'device\n' ;;
  *ro.boot.qemu.avd_name*) printf 'trail_map_api36\n' ;;
  *sys.boot_completed*)
    if [[ "$mode" == boot-exit ]]; then
      touch "$dir/boot-process-exited"
      printf '0\n'
    else
      printf '1\n'
    fi
    ;;
  *ro.build.version.sdk*) printf '36\n' ;;
  *ro.product.cpu.abi*) printf 'arm64-v8a\n' ;;
  *dumpsys\\ SurfaceFlinger*) printf 'GLES: Google SwiftShader\n' ;;
  *logcat\\ -d*) printf 'high-volume unrelated diagnostic noise without smoke markers\n' ;;
  *exec-out\\ run-as*\\.png*)
    if [[ "$mode" == corrupt-png ]]; then printf 'not-a-png'; else printf '\211PNG\r\n\032\nfixture'; fi
    ;;
  *exec-out\\ run-as*\\.json*)
    if [[ "$mode" != marker-missing ]]; then
      token=\$(printf '%s' "\$*" | grep -Eo 'amap-[A-Za-z0-9._-]+')
      printf '{"run_token":"%s","map_loaded_at":1000,"stable_alive_at":6000,"screenshot_ready":true,"lifecycle_cleanup":true}\n' "\$token"
    fi
    ;;
  *am\\ instrument*) [[ "$mode" == instrumentation-fail ]] && exit 25; printf 'OK (1 test)\n' ;;
  *) exit 0 ;;
esac
EOF
  chmod +x "$dir/repo/gradlew" "$dir/bin/adb" "$dir/bin/emulator" "$dir/bin/fake-kill" "$dir/bin/fake-ps"
}

run_status() {
  local mode=$1 boot_timeout=1
  [[ "$mode" == boot-exit ]] && boot_timeout=5
  set +e
  TMPDIR="$TMP/$mode/tmp" PATH="$TMP/$mode/bin:$PATH" ANDROID_AVD_HOME="$TMP/$mode/avd" AMAP_HOST_ARCH=arm64 AMAP_PS_BIN="$TMP/$mode/bin/fake-ps" AMAP_KILL_BIN="$TMP/$mode/bin/fake-kill" AMAP_EVIDENCE_DIR="$TMP/$mode/evidence" AMAP_BOOT_TIMEOUT_SECONDS="$boot_timeout" "$TMP/$mode/repo/scripts/run-amap-smoke.sh" >"$TMP/$mode/output" 2>&1
  RUN_STATUS=$?
  set -e
}

make_fixture wrong-avd-image
printf 'target=android-36\nimage.sysdir.1=system-images/android-36/google_apis/x86_64/\nabi.type=x86_64\n' >"$TMP/wrong-avd-image/avd/trail_map_api36.avd/config.ini"
run_status wrong-avd-image
[[ $RUN_STATUS -eq 11 ]] || fail "wrong AVD image returned $RUN_STATUS"
[[ ! -f "$TMP/wrong-avd-image/emulator.started" ]] || fail 'emulator started before AVD image validation'
grep -q 'approved AVD image required' "$TMP/wrong-avd-image/output" || fail 'wrong AVD image lacked precise error'

make_fixture startup-fail
run_status startup-fail
[[ $RUN_STATUS -eq 13 ]] || fail "startup failure returned $RUN_STATUS"
[[ -f "$TMP/startup-fail/emulator.started" ]] || fail 'startup emulator was not invoked'
[[ ! -f "$TMP/startup-fail/adb.calls" ]] || ! grep -q 'emu kill' "$TMP/startup-fail/adb.calls" || fail 'startup failure killed external serial'

make_fixture lock
mkdir -p "$TMP/lock/tmp/easy-trip-android-device-emulator-5588.lock"
printf 'pid=999999\n' >"$TMP/lock/tmp/easy-trip-android-device-emulator-5588.lock/owner"
run_status lock
[[ $RUN_STATUS -eq 10 ]] || fail "occupied lock returned $RUN_STATUS"
[[ -d "$TMP/lock/tmp/easy-trip-android-device-emulator-5588.lock" ]] || fail 'occupied lock removed'
[[ ! -f "$TMP/lock/emulator.started" ]] || fail 'emulator started despite occupied lock'

make_fixture timeout
run_status timeout
[[ $RUN_STATUS -eq 13 ]] || fail "timeout returned $RUN_STATUS"
grep -q 'get-state' "$TMP/timeout/adb.calls" || fail 'timeout did not poll device state'

make_fixture boot-exit
run_status boot-exit
[[ $RUN_STATUS -eq 13 ]] || fail "boot process exit returned $RUN_STATUS"
grep -q 'owned emulator process exited before boot completed' "$TMP/boot-exit/output" || { grep -E 'emulator process|identity changed|boot timed out' "$TMP/boot-exit/output" >&2 || true; fail 'boot process exit lacked precise error'; }
! grep -q 'boot timed out' "$TMP/boot-exit/output" || fail 'boot process exit was misreported as timeout'

for mode in gate-fail gradle-fail instrumentation-fail marker-missing; do
  make_fixture "$mode"
  run_status "$mode"
  case "$mode" in gate-fail) expected=23 ;; gradle-fail) expected=24 ;; instrumentation-fail) expected=25 ;; marker-missing) expected=14 ;; esac
  [[ $RUN_STATUS -eq $expected ]] || fail "$mode returned $RUN_STATUS, expected $expected"
  [[ ! -f "$TMP/$mode/evidence/complete.txt" ]] || fail "$mode marked stale root evidence complete"
  [[ -z "$(find "$TMP/$mode/evidence" -mindepth 2 -name complete.txt -type f -print -quit)" ]] || fail "$mode marked failed run complete"
done
grep -q 'am instrument' "$TMP/marker-missing/adb.calls" || fail 'marker fixture never ran instrumentation'
grep -q 'logcat -d' "$TMP/marker-missing/adb.calls" || fail 'marker fixture never archived logcat'
grep -q 'exec-out run-as.*\.json' "$TMP/marker-missing/adb.calls" || fail 'marker fixture never read app status'

make_fixture corrupt-png
run_status corrupt-png
[[ $RUN_STATUS -eq 14 ]] || fail "corrupt PNG returned $RUN_STATUS"
grep -q 'app evidence invalid or incomplete' "$TMP/corrupt-png/output" || fail 'corrupt PNG did not fail validation precisely'
grep -q 'exec-out run-as' "$TMP/corrupt-png/adb.calls" || fail 'corrupt PNG fixture did not export screenshot'

make_fixture identity-transition
run_status identity-transition
[[ $RUN_STATUS -eq 23 ]] || fail "legitimate identity transition returned $RUN_STATUS"
! grep -q 'identity changed before boot completed' "$TMP/identity-transition/output" || fail 'legitimate emulator exec transition was rejected'
! grep -q 'refusing to kill' "$TMP/identity-transition/output" || fail 'cleanup rejected legitimate emulator exec transition'

make_fixture pid-reuse
run_status pid-reuse
[[ $RUN_STATUS -eq 23 ]] || fail "pid reuse returned $RUN_STATUS"
grep -q 'refusing to kill' "$TMP/pid-reuse/output" || fail 'PID reuse was not detected'

make_fixture signal
TMPDIR="$TMP/signal/tmp" PATH="$TMP/signal/bin:$PATH" ANDROID_AVD_HOME="$TMP/signal/avd" AMAP_HOST_ARCH=arm64 AMAP_PS_BIN="$TMP/signal/bin/fake-ps" AMAP_KILL_BIN="$TMP/signal/bin/fake-kill" AMAP_EVIDENCE_DIR="$TMP/signal/evidence" AMAP_BOOT_TIMEOUT_SECONDS=30 "$TMP/signal/repo/scripts/run-amap-smoke.sh" >"$TMP/signal/output" 2>&1 &
runner_pid=$!
for _ in $(seq 1 50); do [[ -f "$TMP/signal/emulator.started" ]] && break; sleep 0.02; done
kill -TERM "$runner_pid"
set +e; wait "$runner_pid"; signal_status=$?; set -e
[[ $signal_status -eq 143 ]] || fail "TERM returned $signal_status"
[[ ! -d "$TMP/signal/tmp/easy-trip-android-device-emulator-5588.lock" ]] || fail 'signal left lock behind'

printf 'PASS: amap smoke runner fixtures\n'
