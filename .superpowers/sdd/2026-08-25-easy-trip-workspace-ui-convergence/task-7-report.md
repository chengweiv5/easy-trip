# Task 7 Report

## 状态

代码与 AVD 自动化验收完成；Mate 60 Pro 真机验收为 `PENDING/BLOCKED`。执行期间仅连接 `emulator-5554`，没有连接可独占的 Mate 60 Pro 真机。

## Fix round 1/5

### 系统返回 RED / 根因 / GREEN

- RED：真实 `BackHandler` 已注册且 `OnBackPressedDispatcher` 存在 enabled callback；打开 `LayerMenu` 后调用 Espresso `pressBack()`，却直接触发 `onBack` 离开工作台。
- 根因：Activity 已把事件正确分发给 Compose；`leaveOrCloseOverlay()` 与 `closeOverlay()` 读取的是 `collectAsStateWithLifecycle()` 产生的组合快照 `ready?.overlay`。事件发生在组合快照更新前时，它仍为 `None`，与 `viewModel.state.value.overlay` 中已打开的 `LayerMenu` 不一致。
- 修复：两个返回决策点读取 `viewModel.state.value.overlay`；不改变 reducer、SavedState 或导航契约。系统返回和顶部返回继续共用 `leaveOrCloseOverlay()`，保持“先关 overlay，再离开工作台”的优先级。
- GREEN：测试确认 callback 已启用；第一次系统返回关闭 overlay 且不离开，第二次系统返回离开；顶部返回重复同一优先级路径。

### RouteLeg RED / 根因 / GREEN

- RED：滚动到 `leg-leg-2` 后，`no route` 在 merged 与 unmerged semantics tree 中均不存在。
- 诊断：`leg-leg-2`、错误说明和 retry 的 bounds 均完整位于 LazyColumn viewport 内；实际文本是“路线规划失败”。因此不是滚动目标、语义合并、行高或永久裁剪问题。
- 根因：fixture 的失败路线使用 legacy `errorCode = "no route"` 且 `errorKind = null`，而 `RouteLegEntity.toRouteLegUi()` 只映射 typed `errorKind`，丢弃了 `errorCode`。
- 修复：UI mapper 以 typed error summary 为优先，在 typed error 缺失时 fallback 到 `errorCode`；新增 JVM 回归测试。未改变 Repository、路线 reducer 或布局。
- GREEN：测试先 `performScrollTo()` 到 RouteLeg，再同时断言 `no route` 与 retry 可见并真实点击 retry；完整 `ItineraryEditingTest` 7/7 PASS。

### 其他契约补强

- 长行程名测试不仅检查 back/more/search 的 44dp 物理点击区域，还真实点击三者并验证对应回调各触发一次。
- `waitingForNetworkKeepsAllPlaceActions` 保持真实“上移/下移” custom actions，以及 timing/move/delete 操作语义；没有把整卡改成 clickable。

## 修改文件

- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceRoute.kt`
  - 返回决策使用 ViewModel 当前 overlay，消除组合快照时序差。
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryUiModels.kt`
  - typed route error 缺失时保留 legacy `errorCode`。
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`
  - 恢复真实系统返回与顶部返回双路径覆盖；长标题操作改为真实点击并验证回调。
- `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt`
  - 滚动 RouteLeg 后同时验证 `no route` 与 retry。
- `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModelTest.kt`
  - 增加 legacy route error 映射回归。

未修改业务 reducer、SavedState、Repository、地图生命周期或权限流程。

## 最终命令结果

- `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
  - 331 JVM tests，331 PASS，0 skipped，0 failed；lint PASS；app APK 与 androidTest APK assemble PASS。
- `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest`
  - 7 tests，7 PASS，0 skipped，0 failed。
- 工作台设备套件（`TripWorkspaceContentTest`、`WorkspaceChromeTest`、`WorkspaceSearchTabsTest`、`WorkspaceFlowTest`、`WorkspacePlacePoolLayoutTest`、`ItineraryScopeRailTest`、`WholeTripItineraryContentTest`）
  - 53 tests，53 PASS，0 skipped，0 failed。
- `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest`
  - 47 tests，47 PASS，0 skipped，0 failed。
- `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1FullUiAcceptanceTest`
  - 47 tests，47 PASS，0 skipped，0 failed。

所有 connected tests 串行执行，均获得非零完整结果。

## 设备

- 自动化设备：`emulator-5554`，产品 `sdk_phone64_arm64`，型号 `Android_SDK_built_for_arm64`，测试报告显示 `easy_trip_p60pro(AVD) - 12`。
- 真机：否。
- Mate 60 Pro：`PENDING/BLOCKED`；未连接，未执行正常竖屏真机抽查，不冒充通过。

## 未执行项

- Mate 60 Pro 真实设备上的正常竖屏物理点击与视觉抽查。
- 极端小窗、横屏、分屏、2× 字体属于既有 deferred 范围，不在本任务门禁中。

## Graphify

- 源码读取前已执行规定查询：`graphify query "WorkspaceFlow V1ScenarioCatalogTest V1FullUiAcceptanceTest ItineraryEditingTest device checklist"`。
- 完成修改后执行 `graphify update .`。

## Diff check

- `git diff --check`：PASS，无输出。
- `.superpowers/brainstorm/` 保持未跟踪，不纳入提交。

## 关注点

- Mate 60 Pro 真机验收仍是唯一未完成门禁。
- AVD 结果只证明自动化功能与状态契约，不替代真实设备物理验收。
