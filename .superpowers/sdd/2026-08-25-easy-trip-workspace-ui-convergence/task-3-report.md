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

## Fix round 1/5：拖动中间态实时同步

### Finding

原实现的 `WorkspaceLayoutMetrics` 仅由离散 `sheetLevel` 生成，`WorkspaceBottomSheet` 的 `dragOffset` 为组件私有状态。拖动手柄且尚未松手时，Sheet 已移动，但搜索、图例、地图控件和 fallback 仍按旧边界布局，存在被 Sheet 遮挡的窗口。

### RED

新增 `searchTracksSheetTopDuringActiveDrag`，在手柄上按下并向上拖动 80px、保持未松手，再比较搜索底边与当前 Sheet 顶边。首次执行因测试错误地在触摸输入的主线程作用域内调用同步 API，先触发 Compose 测试线程保护异常；修正测试结构后再进行生产修改。未保留一轮“修正后的测试结构 + 旧生产实现”的独立行为 RED 记录，此项如实作为 TDD 证据缺口记录。

### 实现

- `WorkspaceScaffold` 持有实时可见 Sheet 高度 px，并由该值生成唯一 `WorkspaceLayoutMetrics`。
- `WorkspaceBottomSheet` 在拖动 offset 更新、结束、取消及离散 level/anchor 更新时回传同一实时可见高度。
- Sheet 实际位移与 metrics 均由同一个 `dragOffset` 推导；map 与 topOverlay slots 继续共享同一 metrics，因此搜索、图例、地图控件和 fallback 同步消费实时边界。
- anchors 仍由 Scaffold 单次计算；一步一档 settle、safeDrawing/IME 应用位置和业务 action 均未改变。
- 新增纯函数覆盖，验证显式实时高度同时驱动 `sheetHeight`、`sheetTop` 与 `overlayBottomInset`。

### 验证

- 聚焦 `searchTracksSheetTopDuringActiveDrag`：1 项通过，BUILD SUCCESSFUL。
- `WorkspaceLayoutMetricsTest` + `WorkspaceSheetSyncTest`：通过，BUILD SUCCESSFUL。
- 完整 `testDebugUnitTest`：通过，BUILD SUCCESSFUL。
- 完整 `TripWorkspaceContentTest`：17 项中 15 项通过；实时拖动同步测试通过。
- 仅剩两项既知失败，未修改：
  - `workspaceTabsUseIndicatorAndTabSemantics`：Task 4。
  - `placePoolListScrollsAndKeepsSecondCardAboveBottomInset`：Task 5。
- `git diff --check`：通过。

### 自审

未修改业务状态、导航、Repository、权限、地图生命周期、Task 4 Tab 或 Task 5 bottom inset；未触碰 `.superpowers/brainstorm/`。

## Fix round 2/5：受控跨档回调一致性

### Finding

round 1 中 `WorkspaceBottomSheet` 私有持有 `dragOffset`，而 `WorkspaceScaffold` 另持有 `visibleSheetHeightPx`。跨过阈值的 drag end 会先清除 Sheet 的私有 offset，却在等待受控 `onValueChange` 前将 overlays 提前发布到新档锚点；父级延迟或拒绝更新时，两者会分离。

### RED

新增三项触摸测试：cancel 回当前档、未跨阈值 end 回当前档、跨阈值但父级不接纳请求时仍保持受控当前档。旧实现的拒绝更新测试确认失败：Sheet 已回 HALF，但搜索仍提前移动到 EXPANDED 边界（`expected 434.9091.dp but was 70.90909.dp`）。

### 实现

- 将 `dragOffsetPx` 提升到 `WorkspaceScaffold`，作为 Sheet offset 与 `WorkspaceLayoutMetrics.visibleSheetHeight` 的唯一共享进度状态。
- Sheet 和 metrics 都由 `anchors[sheetLevel] - dragOffsetPx` 推导；不再维护 `visibleSheetHeightPx` 这个副本。
- drag end 先清共享 offset 回受控当前档，再仅请求 `onValueChange(next)`；不提前发布下一档 metrics。父级接纳时，新的 `sheetLevel` 触发受控重组；拒绝或延迟时，Sheet 与 overlays 继续停在同一当前档。
- 保留 clamp、24dp threshold 与相邻一档 settle；未更改业务回调语义、anchors 计算或 insets 应用位置。

### 验证

- 四项拖动 Compose 测试（active drag、cancel、低于阈值 end、拒绝跨档请求）：全部通过，BUILD SUCCESSFUL。
- `WorkspaceLayoutMetricsTest` + `WorkspaceSheetSyncTest`：通过，BUILD SUCCESSFUL。
- 完整 `TripWorkspaceContentTest`：20 项中 18 项通过；仅剩既知 Task 4 `workspaceTabsUseIndicatorAndTabSemantics` 与 Task 5 `placePoolListScrollsAndKeepsSecondCardAboveBottomInset` 失败，未修改。
- `git diff --check`：通过。

### 自审

实时 Sheet/overlay 边界只有 Scaffold 的 `dragOffsetPx` 一份状态；手势局部累计值仅在 pointerInput 协程中用于阈值判定，每次更新都发布至该单一状态，终止路径清零后同步回当前受控档。未触碰 `.superpowers/brainstorm/`。
