# Task 3 Report

## 修改文件

- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceScaffold.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceBottomSheet.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- `app/src/test/java/com/yangchengwei/easytrip/workspace/WorkspaceLayoutMetricsTest.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt`
- `graphify-out/` 生成图谱文件

## RED

`./gradlew testDebugUnitTest --tests 'com.yangchengwei.easytrip.workspace.WorkspaceLayoutMetricsTest'`

按预期在测试编译期失败：`workspaceLayoutMetrics` 未定义。

## GREEN

新增 `WorkspaceLayoutMetrics`、`workspaceLayoutMetrics` 与 `WorkspaceScaffold`。Scaffold 统一应用 `WindowInsets.safeDrawing` 和 `imePadding()`，消费既有 `WorkspaceSheetAnchors`，并向 map/topOverlay slots 暴露同一组 Sheet 几何指标。`WorkspaceBottomSheet` 改为接收壳层计算的 anchors，避免重复计算布局边界。

`TripWorkspaceContent` 已迁移地图、fallback、顶部栏、地图控件、图例、搜索和 Sheet 到统一壳层；搜索、图例、地图控件和失败重试均按当前 Sheet 边界避让。删除了该文件中不再使用的 Material BottomSheet 状态/API 与 `toSheetValue()`。

## 完整相关测试结果

- `WorkspaceLayoutMetricsTest` + `WorkspaceSheetSyncTest`：通过，BUILD SUCCESSFUL。
- 完整 `testDebugUnitTest`：通过，BUILD SUCCESSFUL。
- `TripWorkspaceContentTest`：16 项中 14 项通过；Task 3 新增的 3 项空间关系测试全部通过。
- 两项已知非 Task 3 失败保持不变：
  - `workspaceTabsUseIndicatorAndTabSemantics`：由 Task 4 承接。
  - `placePoolListScrollsAndKeepsSecondCardAboveBottomInset`：由 Task 5 承接。
- 首次 instrumentation 执行因无连接设备失败；启动 `easy_trip_p60pro` 后完成上述 16 项执行。

## 自审

- 未改 ViewModel、Repository、导航、权限或地图生命周期。
- `safeDrawing` 与 `imePadding` 仅在 `WorkspaceScaffold` 工作台根容器应用一次。
- Sheet anchors 由壳层单次计算，并由 metrics 与 `WorkspaceBottomSheet` 共享。
- 无固定 Sheet bottom 值；overlay 基于 `metrics.overlayBottomInset`，地图控件/图例额外避让搜索栏高度。
- `git diff --check` 通过。
- 已运行 `graphify update .`；工具报告两个既存 Kotlin 文件解析警告，与本任务无关。

## SHA

`991ed9a` — Coordinate workspace overlays with sheet.

## 关注点

完整 Compose 类仍因 Task 4/5 已知测试各失败一项而返回非零；Task 3 新增和直接相关断言均通过。未修改用户已有未跟踪目录 `.superpowers/brainstorm/`。
