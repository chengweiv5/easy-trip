# Task 3 报告：排序仅由拖动手柄发起

## 状态

完成。

## 实现

- 将长按拖动手势从整个行程项移动到独立的 40dp 拖动手柄。
- 使用 Canvas 绘制固定 22dp 手柄图标，不再使用字体 glyph。
- 将排序步长定义为 120dp，并通过当前 `LocalDensity` 转换为 px。
- 未满完整步长不跨项，达到完整步长后按步数移动并限制在有效索引内。
- 取消拖动时恢复初始 preview，不提交排序。
- 保留整行“上移”和“下移”无障碍 custom actions。
- 迁移编辑流程中的排序测试，使其从 `drag-handle-*` 发起。

## TDD 证据

RED 阶段确认以下行为在旧实现失败：

- 正文长按拖动仍会触发排序。
- 更多菜单长按拖动仍会触发排序。
- 2× density 下旧的裸 120px 阈值会提前跨项。

GREEN 阶段覆盖：

- 正文长按拖动不 preview、不 commit。
- 更多菜单长按拖动不 preview、不 commit。
- handle 长按拖动可提交排序。
- 2× density 下少于 120dp 不跨项，超过 120dp 跨一项。
- 中间项同时保留“上移”和“下移”，且调用正确目标索引。

## 验证

- `./gradlew testDebugUnitTest lintDebug`：通过。
- `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest`：20/20 通过，设备 `easy_trip_p60pro(AVD) - 12`。
- `git diff --check`：通过。
- `graphify update .`：完成，图谱更新为 4176 nodes / 8903 edges。

## 关注点

- 更多菜单仍使用字体 glyph；该组件位于独立文件，按任务边界未扩大改动范围。
- Graphify 更新报告两个既有 Kotlin 文件存在部分语法提取警告：`NetworkMonitor.kt`、`RoutePlanner.kt`；与本任务无关。
- 未进行物理真机验收；按批次约定留到候选批次统一执行。

## Fix round 1

- 使用 `rememberUpdatedState` 持有 `onPreview`、`onCommit`、`onDraggingChange` 的最新值；`pointerInput` keys 保持为 `itemId`、`count`、`reorderStepPx`，父重组替换 callback 不会重启活动手势。
- 新增活动拖动期间替换 callback 的行为测试。旧实现明确 RED：拖动后续事件仍调用旧 callback；修复后 preview 与 commit 均调用新 callback。
- 新增取消拖动回归测试：先跨过一步产生 preview，再取消，确认 preview 恢复 `startIndex` 且不 commit。旧实现已通过该测试。
- `ItineraryTimelineContentTest` 与 `ItineraryEditingTest`：22/22 通过，设备 `easy_trip_p60pro(AVD) - 12`。
- `./gradlew testDebugUnitTest lintDebug`：通过。
- 未处理独立文件中的 more 字体 glyph，保持本轮修复范围最小。
