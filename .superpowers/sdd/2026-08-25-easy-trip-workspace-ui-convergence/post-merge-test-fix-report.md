# Post-merge connected test fix report

## 状态

普通 `connectedDebugAndroidTest` 合并后已知四项回归已修复。聚焦方法、相关测试类、V1 普通验收套件、JVM/lint/assemble 均通过。完整 319 项普通设备套件执行到 195 项时仍为 0 failed、2 skipped，但命令超过 10 分钟限制后被停止，因此不声明完整套件全绿。AMap 真机地图 attach smoke 未在本轮专用环境重跑。

## 根因与修复

### V1PencilFlowTest

- RED：`createBackAndReopenUsesRoomAndNavigatesExactlyOncePerAction` 找不到文本 `返回`。
- 根因：工作台返回按钮已改成图标，仅保留 `workspace-back` test tag 和 `返回` content description；测试仍使用旧文本 selector。
- 修复：改用生产稳定语义 `workspace-back` 点击。Room 持久化和每次动作恰好一次导航的断言保持不变。

### V1ScenarioMetadataTest

- RED：`mapFailureSetupCanBeRepeatedOnTheSameExecutable` 第二轮 `render` 抛出 Activity 已 setContent。
- 根因：测试要验证的是同一 executable 的 `setup()` 可重复重置，但循环同时重复调用 Compose rule 的单次 `setContent` API。
- 修复：同一 executable 连续调用两次 `setup()`，随后只 render/actions/assertions 一次。仍验证重复 setup 后场景可正常执行，不把场景断言删掉。

### AmapComposeMapTest

- 已知 suite 失败：`disposedHostCallbacksAreIgnoredAfterLifecycleOwnerReplacement` 在 `secondRender` 后立即读取 `readyCount`，出现 expected 2 / actual 1。
- 根因：`secondRender` latch 在 host `render()` 内先释放，而 `onMapReady` 在 render 返回后才执行；断言跨线程抢跑。单方法与完整类在本机可通过，符合时序竞态。
- 修复：新增仅由第二次 `onMapReady` 释放的 latch，在断言计数前等待该行为完成。旧 host 的迟到 callback 仍必须保持 0 次 error，行为断言未削弱。

### AmapMapViewAttachSmokeTest

- RED：普通套件没有 `amapRunToken` 时硬失败。
- 根因：该测试属于 `scripts/run-amap-smoke.sh` 的专用 runner；脚本负责受控 AVD、API key、token 和证据采集，普通 connected suite 不具备这些前置条件。
- 修复：仅在 token 缺失时使用 JUnit assumption 跳过；token 非空但格式非法仍硬失败，专用脚本下所有地图加载、存活、截图、生命周期和证据断言保持不变。

## 验证

- 四个聚焦方法：4/4 PASS；其中 smoke 在无 token 普通入口为 skipped。
- 四个相关测试类：22/22 PASS；其中 smoke 为 skipped。
- `V1ScenarioCatalogTest`：47/47 PASS。
- `V1FullUiAcceptanceTest`：47/47 PASS。
- `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`：PASS。
- `git diff --check`：PASS。
- `graphify update .`：完成；报告了两个既存 Kotlin 部分解析 warning。
- 完整 `./gradlew connectedDebugAndroidTest`：启动 319 项，执行到 195 项时 0 failed、2 skipped，超过 600 秒后停止；不视作完整通过。

## AMap smoke 状态

未运行 `scripts/run-amap-smoke.sh`。当前连接着普通 API 31 `easy_trip_p60pro` AVD，而文档要求独占 `trail_map_api36`、API 36、arm64、SwiftShader、cold boot，并要求没有其他连接设备；专用运行成本较高且会与当前普通回归设备冲突。既有诊断证据表明 API 31/lavapipe 会在 AMap EGL context 创建失败，API 36/SwiftShader 才是批准环境。

## 遗留

- 完整 319 项普通 connected suite 尚缺一次无超时的完整终态结果。
- AMap attach smoke 尚缺本轮 `scripts/run-amap-smoke.sh` 专用入口结果。
- Mate 60 Pro 真机验收仍未执行。
- 未跟踪的 `diagrams/` 与 `docs/analysis/2026-08-26-ui-next-actions.md` 不属于本修复，不纳入提交。
