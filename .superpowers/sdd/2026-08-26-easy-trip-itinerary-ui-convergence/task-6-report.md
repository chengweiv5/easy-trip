# Task 6 报告

## 状态

完成。

## 实现

- 全程视图继续复用只读 `ItineraryPlaceRow` 与 `RouteLegContent`，不暴露拖拽、菜单、时间、跨日、删除、交通方式或重试操作。
- 无旅行日时复用 `EmptyState` 与 `ItineraryEmptyIllustration`，提供“新增旅行日”入口。
- 全程列表增加 24dp 底部安全间距；空旅行日仍保留日期标题和紧凑空态。
- rail 与全程空态的新增旅行日入口共享同一个 `onAddDay`。
- timing、跨日移动、删除确认、交通方式 overlay 统一由既有 `editDraft`、`crossDayMove`、`deleteConfirmation`、`modeEditor` 推导。
- 菜单 Popup 自身关闭不派发 `DismissDialogs`。
- 仅 Ready RouteLeg 可建立 mode editor；Failed RouteLeg 重试不会打开 mode overlay。全程 Failed RouteLeg 只展示错误，不提供 retry。

## TDD 证据

- 无旅行日 action 首次 RED：`WholeTripItineraryContent` 不存在 `onAddDay` 参数，编译失败。
- overlay 状态扩展首次 RED：helper 尚不接受 cross-day/mode 状态，编译失败。
- Failed RouteLeg mode 首次 RED：Failed 状态仍建立 mode editor；增加 Ready 状态守卫后转 GREEN。

## 验证

- `./gradlew testDebugUnitTest --tests 'com.yangchengwei.easytrip.workspace.TripWorkspaceNavigationStateTest'`：通过。
- `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryScopeRailTest,com.yangchengwei.easytrip.workspace.WorkspaceFlowTest`：28 tests，0 failed，设备 `easy_trip_p60pro(AVD) - 12`。
- `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`：通过。
- `graphify update .`：完成；报告了两个既有文件的 AST 部分提取 warning。

## Fix round 1/5

- `DayItineraryViewModelTest` 直接观察既有 fake repository：`RequestDelete` 后明确断言 `deleteCalls` 为空，`ConfirmDelete` 后才断言仅调用一次且 item id 为 `item-beta`。
- `WorkspaceFlowTest` 记录收到的 `DayItineraryAction`，关闭菜单后明确断言未收到 `DismissDialogs`。
- rail 与全程空态的新增日入口改为两个独立 composition，各自断言回调恰好一次。
- 全程只读禁止 tag 前缀补充 `more-` 与 `drag-handle-`。
- 保留 `DayItineraryViewModel` 的 Ready mode guard 与 `TripWorkspaceRoute` 的业务状态 overlay 派生。
- JVM 定向测试通过；Task 6 设备测试 29 tests、0 failed；完整静态门禁通过。

## 范围检查

- Task 6 改动文件准确列表：
  - `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContent.kt`
  - `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WorkspaceItineraryContent.kt`
  - `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModel.kt`
  - `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceRoute.kt`
  - `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContentTest.kt`
  - `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryScopeRailTest.kt`
  - `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`
  - `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModelTest.kt`
  - `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceNavigationStateTest.kt`
- 未修改 Repository 或 route generation；修改了 workspace shell 的 `TripWorkspaceRoute.kt`，用于按业务状态派生 overlay。
- 未触碰 `diagrams/`。
- 未 push。

## 关注点

- 设备验证通过 Compose instrumentation 完成，未额外执行物理真机人工验收。
- Graphify 对 `NetworkMonitor.kt` 与 `RoutePlanner.kt` 报告既有语法解析 warning，本任务未修改这两个文件。
