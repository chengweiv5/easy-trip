# Batch 2 AMap/EGL 环境诊断

## 状态

**RECOVERED**

仅调整本地 AVD 启动方式，未修改应用代码、未 commit、未派生 subagent。恢复后的环境可继续执行 Batch 2 后续生产旅程；本报告只确认 AMap/EGL 环境及真实搜索入口恢复，不等同于 Batch 2 全部功能与视觉 gate 已通过。

## 根因结论

失败实例的 Emulator EGL renderer/context 路径无法满足 AMap `GLSurfaceView` 创建 GL context：

- 失败环境为 `easy_trip_p60pro`、Android API 31，运行时使用 `lavapipe`。
- AMap 创建新 GL context 时，底层 `com.google.android.gles_jni.EGLImpl._eglCreateContext` 抛出 `IllegalArgumentException`，随后 `GLSurfaceView` 在 GLThread 抛出 `java.lang.RuntimeException: createContext failed: EGL_SUCCESS`，导致生产 Activity 退出。
- `AMapNativeGlOverlayLayer.nativeDestroy()` 的 `UnsatisfiedLinkError` 出现在旧 GL overlay 销毁阶段，是伴随现象；决定性失败点是新 EGL context 创建失败。
- 切换到已有 API 36 AVD，并显式使用 SwiftShader 后，同一 APK 能创建地图 Surface、保持进程存活并完成真实地点搜索。因此本次阻塞可归因于失败模拟器的图形环境，而不需要修改仓库代码。

该结论不扩大为“所有 API 31 必然失败”或“Amap SDK 存在已确认版本缺陷”；本次只验证了当前 API 31/lavapipe 实例失败，以及 API 36/SwiftShader 环境恢复。

## 环境对比

| 项目 | 失败环境 | 恢复环境 |
|---|---|---|
| Serial | `emulator-5554` | `emulator-5554` |
| AVD | `easy_trip_p60pro` | `trail_map_api36` |
| Android API | 31 | 36 |
| GPU 路径 | `lavapipe` | 显式 `swiftshader` |
| GLES | context 创建失败 | ANGLE + SwiftShader Vulkan，OpenGL ES 3.1 |
| 结果 | AMap GLThread 崩溃 | 地图与搜索正常，进程存活 |

恢复环境的 SurfaceFlinger renderer：

```text
Android Emulator OpenGL ES Translator (ANGLE (Google, Vulkan 1.3.0 (SwiftShader Device (LLVM 10.0.0) (0x0000C0DE)), SwiftShader driver-5.0.0)), OpenGL ES 3.1
```

## 最小恢复操作

关闭失败实例后，使用已有 API 36 AVD 冷启动，并显式选择软件 GPU：

```bash
emulator @trail_map_api36 \
  -gpu swiftshader \
  -no-snapshot-load \
  -no-snapshot-save \
  -port 5554 \
  -no-boot-anim
```

`-no-snapshot-load` / `-no-snapshot-save` 用于避免复用或写回可能携带错误图形状态的快照；没有修改或清除任一 AVD 数据。

随后通过 `adb install -r` 安装当前 APK：

```text
/Users/bytedance/Code/easy-trip/.claude/worktrees/easy-trip-v1-full-ui-run/app/build/outputs/apk/debug/app-debug.apk
```

## 生产 UI 最小复现结果

1. API 36 AVD 正常完成启动，设备在线。
2. 当前 APK 安装成功。
3. 复用 `trail_map_api36` 中已有的高德隐私同意状态和旅行 `Task4API362`。
4. 从生产 `MainActivity` 打开旅行，进入工作台。
5. 工作台存在真实 `android.view.SurfaceView`；地图初始化后应用未崩溃。
6. 点击“搜索餐厅、景点或地址”，成功进入搜索页，初始态显示“搜索想去的地方”。
7. 输入 `coffee` 并提交，真实查询返回 `20 个`结果，包括：
   - `星巴克(老佛爷百货店)`
   - `Peet's皮爷咖啡(灵境胡同店)`
   - `瑞幸咖啡(西西友谊商城店)`
   - `库迪咖啡(西单山水宾馆店)`
8. 完成查询后 `com.yangchengwei.easytrip` 进程仍存活，PID 为 `2119`。
9. 恢复验证日志中未再出现原阻塞的 `FATAL EXCEPTION` 或 `createContext failed`。

说明：本轮使用 `adb install -r` 和已有 AVD 数据，因此没有重新点击一次“同意并启用搜索”；验证覆盖的是允许的“可复用已有数据”路径，即已有同意状态下工作台地图不崩溃且真实搜索可用。

## 后续旅程判断

**可以继续。** AMap 地图初始化和真实地点搜索入口已在 `trail_map_api36` + SwiftShader 上恢复。后续 Batch 2 旅程应固定使用该 AVD 启动方式，避免回到当前已知失败的 API 31/lavapipe 实例。

本报告不改变此前自动化 Room timeout、完整生产旅程和视觉审查各自的 gate 状态；这些仍需按原计划独立验证。

## 证据路径

### 原始失败证据

- `/tmp/easy-trip-batch2-final/09-consent-logcat.txt`
- `/tmp/easy-trip-batch2-final/11-crash-logcat.txt`
- `/tmp/easy-trip-batch2-final/08-workspace.xml`
- `/tmp/easy-trip-batch2-final/11-workspace.xml`

### 恢复验证证据

- 启动后旅行列表：`/tmp/easy-trip-batch2-egl-recovery/01-launch.xml`
- 启动阶段日志：`/tmp/easy-trip-batch2-egl-recovery/01-launch-logcat.txt`
- 工作台及地图 Surface：`/tmp/easy-trip-batch2-egl-recovery/02-workspace.xml`
- 工作台日志：`/tmp/easy-trip-batch2-egl-recovery/02-workspace-logcat.txt`
- 搜索初始态：`/tmp/easy-trip-batch2-egl-recovery/03-search.xml`
- 真实搜索结果：`/tmp/easy-trip-batch2-egl-recovery/04-results.xml`
- 完整恢复日志：`/tmp/easy-trip-batch2-egl-recovery/04-results-logcat.txt`
- 过滤后的 EGL/AMap/Runtime 日志：`/tmp/easy-trip-batch2-egl-recovery/05-filtered-logcat.txt`
- 原始结果截图：`/tmp/easy-trip-batch2-egl-recovery/04-results.png`
- 已读取的压缩结果截图：`/tmp/easy-trip-batch2-egl-recovery/04-results-small.png`
