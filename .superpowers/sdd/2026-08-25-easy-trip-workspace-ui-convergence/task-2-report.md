# Task 2 Report

## 状态

已完成 BottomSheet 锚点、dp 手势阈值与拖动边界实现。

## 修改文件

- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceBottomSheet.kt`
  - 新增 `WorkspaceSheetAnchors` 与按档位索引。
  - 新增 `workspaceSheetAnchors(...)`，保持正常窗口三档高度与搜索返回抬升。
  - 新增 `resolveWorkspaceSheetDrag(...)`，保持一步一档。
  - 新增 `clampWorkspaceSheetDragOffsetPx(...)`。
  - Compose 手势阈值由 `24.dp` 通过 `LocalDensity` 转 px，拖动 offset 限制在 collapsed/expanded 锚点范围。
- `app/src/test/java/com/yangchengwei/easytrip/workspace/WorkspaceSheetSyncTest.kt`
  - 将高度断言迁移为锚点断言。
  - 新增 px 阈值、一步一档、拖动边界测试。
  - 极端窗口只断言三档有序且不超过可用高度。
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentTest.kt`
  - 新增折叠态 handle 可见、业务内容隐藏测试。
  - 新增三档 Sheet 不越出工作台根节点测试。
  - 修复同一测试重复调用 `setContent`，改用 Compose state 驱动档位重组。
- `graphify-out/*`
  - 执行 `graphify update .` 更新图谱。

## RED

命令：

`./gradlew testDebugUnitTest --tests 'com.yangchengwei.easytrip.workspace.WorkspaceSheetSyncTest'`

结果：按预期失败；编译报告 `WorkspaceSheetAnchors`、`workspaceSheetAnchors`、`resolveWorkspaceSheetDrag`、`clampWorkspaceSheetDragOffsetPx` 均未定义。

## GREEN / 回归

通过：

- `./gradlew testDebugUnitTest --tests 'com.yangchengwei.easytrip.workspace.WorkspaceSheetSyncTest'`
- 新增/直接相关 Compose 测试共 3 项：
  - `collapsedSheetKeepsHandleAndHidesBusinessContent`
  - `allSheetLevelsKeepMapSubtreeAndUseDistinctConstrainedHeights`
  - `sheetAlwaysStaysInsideWorkspaceRoot`

首次完整 `TripWorkspaceContentTest` 共 13 项，9 项通过、4 项失败。修复测试自身重复 `setContent` 后，直接相关 3 项均通过；固定高度断言迁移前仍有 4 项失败，见关注点。

## Fix round 1/5

### 测试迁移

- `halfSheetMatchesDesignProportionAndStaysBelowTopSafeArea` 不再写死 395–397dp，改为根据当前 root 高度计算 `workspaceSheetAnchors(...).half`，并保留 Sheet 与顶部安全区的边界断言。
- `searchReturnSheetIsRaisedAndHighlightsRecentCollections` 在同一 Compose 树中先测普通 HALF，再注入搜索返回状态；断言搜索返回 HALF 高于普通 HALF，且等于当前 root 对应的 search-return anchor。
- 未修改生产实现。

### RED 证据

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest
```

迁移前结果：13 项中 4 项失败。两个本轮目标失败分别为：

- 普通 HALF 实际约 455dp，不满足旧 395–397dp 固定范围。
- 搜索返回 HALF 实际约 471dp，不满足旧 411–413dp 固定范围。

另有 Tab indicator 与地点池底部 inset 两项既有失败。

### GREEN / 完整回归

```bash
./gradlew testDebugUnitTest \
  --tests 'com.yangchengwei.easytrip.workspace.WorkspaceSheetSyncTest'
```

结果：`BUILD SUCCESSFUL`。

```bash
./gradlew compileDebugAndroidTestKotlin
```

结果：`BUILD SUCCESSFUL`。

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.TripWorkspaceContentTest
```

最终结果：13 项中 11 项通过、2 项失败。本轮迁移的普通 HALF 和搜索返回 HALF 测试均通过。

剩余失败严格限于后续任务范围：

1. `workspaceTabsUseIndicatorAndTabSemantics`：Task 4 承接 Tab indicator。
2. `placePoolListScrollsAndKeepsSecondCardAboveBottomInset`：Task 5 承接 place pool bottom inset。

## 自审

- 保留 `COLLAPSED / HALF / EXPANDED` 三档和默认 `HALF` 恢复语义。
- 拖动无论位移多大都只移动一个相邻档位。
- 生产代码没有为 280dp / 2× 字体新增专门分支。
- 未修改 ViewModel、Repository、导航、权限或地图生命周期。
- 未纳入已有未跟踪目录 `.superpowers/brainstorm/`。

## Commit

实现提交：`a5d5a50`。

## 关注点

最终完整 Compose 类回归仍有 2 个后续任务已明确承接的失败：

1. `placePoolListScrollsAndKeepsSecondCardAboveBottomInset`：末项距 root 底部约 16dp，旧断言要求 24dp，由 Task 5 承接。
2. `workspaceTabsUseIndicatorAndTabSemantics`：3dp 指示条在当前模拟器上 `assertIsDisplayed` 失败，由 Task 4 承接。

普通 HALF 与搜索返回 HALF 的固定高度断言已迁移为当前 root 与 anchor 的关系断言，不再要求设备相关固定 dp 值。