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

完整 `TripWorkspaceContentTest` 共 13 项，7 项通过、6 项失败。修复测试自身重复 `setContent` 后，直接相关 3 项均通过；剩余 4 个独立既有失败可稳定复现，见关注点。

## 自审

- 保留 `COLLAPSED / HALF / EXPANDED` 三档和默认 `HALF` 恢复语义。
- 拖动无论位移多大都只移动一个相邻档位。
- 生产代码没有为 280dp / 2× 字体新增专门分支。
- 未修改 ViewModel、Repository、导航、权限或地图生命周期。
- 未纳入已有未跟踪目录 `.superpowers/brainstorm/`。

## Commit

实现提交：`a5d5a50`。

## 关注点

完整 Compose 类回归中仍有 4 个既有视觉/测试环境断言失败：

1. `halfSheetMatchesDesignProportionAndStaysBelowTopSafeArea`：模拟器安全区后的可用高度为 909.8dp，当前比例规则得到约 455dp，而测试写死 395–397dp。
2. `searchReturnSheetIsRaisedAndHighlightsRecentCollections`：同理得到约 471dp，而测试写死 411–413dp。
3. `placePoolListScrollsAndKeepsSecondCardAboveBottomInset`：末项距 root 底部约 16dp，旧断言要求 24dp。
4. `workspaceTabsUseIndicatorAndTabSemantics`：3dp 指示条在当前模拟器上 `assertIsDisplayed` 失败。

这些失败不由本任务锚点/手势修改引入；其中固定高度断言与“从当前窗口计算”的新规格冲突，未通过增加生产布局特例规避。