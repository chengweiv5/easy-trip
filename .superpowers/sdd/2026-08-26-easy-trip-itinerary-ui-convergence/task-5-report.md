# Task 5 报告：建立单日时间线与统一空态

## 状态

完成。

## 实现

- 单日列表继续以 `previewOrder` 作为唯一展示顺序，只在当前项与下一项匹配时插入 RouteLeg。
- `LazyColumn` 增加 24dp 底部内容安全间距，长列表末项可滚动到遮挡区上方。
- 共享 `EmptyState` 新增可选 `illustration` 与 `action` slot，默认值保持旧调用兼容。
- 新增 88dp `ItineraryEmptyIllustration`，使用主题 primary、secondary、surfaceVariant，提供单一完整语义与测试 tag。
- 有选中日的当天空态展示插画、标题、说明和“从地点池添加”操作；该操作只派发 `AddPlaces`。
- 无选中日不展示隐式新增旅行日入口。
- `showDialogs` 和已有编辑、移动、删除、路线操作保持不变。
- 未触碰 `diagrams/`，未处理 carried minors（more glyph、RouteLeg 测试隔离）。

## TDD 证据

RED：新增时间线、空态、action、无选中日和底部安全区测试后，设备测试按预期失败：缺少 `itinerary-empty-illustration`，长列表末项无法满足新断言。

GREEN：实现共享 slot、插画、空态与列表 padding 后相关设备测试全部通过。

## 验证

- `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryTimelineContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest`
  - 32 tests，0 skipped，0 failed。
- `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
  - BUILD SUCCESSFUL。
- `graphify update .`
  - 完成；4214 nodes、8992 edges。

## 关注点

- `graphify update .` 仍报告既有 `NetworkMonitor.kt` 与 `RoutePlanner.kt` 解析警告；本任务未修改这两个文件。
- 本轮通过 Android AVD Compose 设备测试完成交互与滚动验证，未进行物理真机视觉验收；按批次门禁约定留待候选批次。

## Fix round 1/5

- 修复空日仍因 `savedPlaces` 非空而展示 `add-place-*` 快捷按钮的问题。
- 新增回归测试覆盖 `items`/`previewOrder` 为空但 `savedPlaces` 非空的状态；RED 明确发现 `add-place-saved-1`，GREEN 后只保留 `add-places-to-selected-day`，点击仅产生 `AddPlaces`。
- 非空行程中的 savedPlaces 快捷行保持原行为。
- 设备回归：Timeline + Editing 共 33 tests，0 skipped，0 failed。
- 静态门禁：`testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` 成功。
- `graphify update .` 完成；4222 nodes、9006 edges。
