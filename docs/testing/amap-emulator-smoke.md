# AMap 模拟器 Smoke

## 固定环境

仅允许 Apple Silicon (`arm64`) host 上的 `trail_map_api36`、API 36、`system-images;android-36;default;arm64-v8a` 镜像和 `arm64-v8a` guest ABI，启动参数固定为 `-gpu swiftshader -no-snapshot-load -no-snapshot-save`。脚本使用独立 serial `emulator-5588`，不清除或修改 AVD 用户数据。

```bash
AMAP_EVIDENCE_DIR="$PWD/build/amap-smoke" scripts/run-amap-smoke.sh
```

运行前在 `local.properties` 配置非空 `AMAP_API_KEY`；脚本会在启动模拟器前校验，instrumentation 也会硬失败而不会 skip。国内网络建议直连高德服务，避免会改写 DNS、证书或出口地区的代理。

`src/debug/AndroidManifest.xml` 只追加 smoke Activity。与主线集成时应保留已有的 `WorkspaceSearchReturnTestActivity` 等 debug 声明，在同一 `<application>` 中合并，不要覆盖整个 debug manifest。

## 门禁与互斥

脚本通过 `${TMPDIR:-/tmp}/easy-trip-android-device-emulator-5588.lock` 实施跨项目设备全局互斥，锁覆盖启动、安装和 instrumentation；owner metadata 记录 PID、时间与 serial。检测到锁时一律 fail-fast，不自动回收或删除，即使记录的 owner PID 已不存在也如此；确认没有任务仍持有设备后，才可人工删除 stale lock。发现任何已连接设备时立即停止，避免操作共享模拟器。启动后、安装 APK 前，门禁记录并校验：

- host architecture、AVD target/image package、API、guest ABI、唯一 serial
- emulator version
- 完整 emulator 命令行
- renderer 与 GLES 信息
- `sys.boot_completed=1`
- 禁用 snapshot load/save 的 cold boot

任一组合不符会 fail-fast，不安装 APK。环境证据写入合法 JSON `build/amap-smoke/environment.json`；`emulator.log`、`instrumentation.txt`、`logcat.txt` 和 `map-loaded.png` 位于同目录。Gradle 安装与 instrumentation 都显式绑定 `emulator-5588`。清理只终止脚本保存的 emulator PID，PID 已退出时不会通过 serial 杀设备。

## Smoke 行为

测试显式调用隐私展示与同意接口，在 Activity window 中附着真实 `MapView`。map-loaded 必须在 20 秒内成功；成功后继续稳定存活至少 5 秒并确认 PID 存在，随后执行 `onPause`、`onDestroy` 并移除 View。测试通过本身覆盖完整断言，另外通过 logcat 的 `AMAP_SMOKE` tag 记录 map-loaded 与 lifecycle cleanup marker。

## 停止与恢复

`Ctrl-C` 会停止本脚本启动的模拟器并释放锁；脚本不会停止任何既有设备。超时、renderer/API/ABI 不符、多个 serial、进程死亡或 instrumentation 失败时立即停止。修正环境后重新运行完整命令；不要复用不确定的快照或手工绕过门禁。异常退出遗留锁时，先确认没有相关运行任务，再删除锁目录。
