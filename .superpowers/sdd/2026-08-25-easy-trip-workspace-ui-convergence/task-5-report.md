# Task 5 报告

## 状态

已将地点池内容接入工作台共享 Sheet 的单层水平 inset，并修复长文案挤压操作区和列表底部留白不足。

## 修改文件

- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/SavedPlaceRow.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/WorkspacePlacePoolLayoutTest.kt`
- `graphify-out/*`

## RED / GREEN

1. 共享 Sheet 水平 inset 与长文案布局
   - RED：工作台地点池仍叠加自身 16dp padding；编辑按钮缺少稳定 tag，长名称未限制行数和溢出。
   - GREEN：两个 `PlacePoolContent` overload 增加显式 `contentPadding`，独立入口保持默认 16dp，工作台仅传底部 padding；文本区保持可收缩，名称单行、地址两行省略，操作按钮保持在卡片内。
2. 列表底部留白
   - RED：`placePoolListScrollsAndKeepsSecondCardAboveBottomInset` 实测 root bottom 为 `909.8182dp`、末卡 bottom 为 `889.8182dp`，只有 20dp 留白，未达到 24dp 契约。
   - GREEN：工作台宿主底部 padding 调整为 24dp，目标测试通过。

## 测试

- `TripWorkspaceContentTest#placePoolListScrollsAndKeepsSecondCardAboveBottomInset`：通过，1/1。
- `WorkspacePlacePoolLayoutTest`：通过，2/2。
- `WorkspacePlacePoolLayoutTest`、`PlacePoolFlowTest`、`TripWorkspaceContentTest` 完整相关设备回归：未形成有效完整结果。首次运行停在 4/38 超过 15 分钟且输出 15 分钟无更新，设备仍在线，停止残留 Gradle/UTP 任务；按约定仅重跑一次后，首个测试进程被系统以 signal 9 杀死，instrumentation 报 `Process crashed`。该测试类此前独立运行通过 2/2，目标底部 inset 测试独立运行通过 1/1。
- `./gradlew testDebugUnitTest lintDebug`：通过。

## 自审

- `PlacePoolAction`、按钮数量和事件保持不变。
- 独立地点池继续使用默认 16dp content padding；工作台左右间距仅由共享 Sheet 提供一次。
- 未修改业务状态、导航、Repository、权限或地图生命周期。
- 未扩展到 Task 6，未触碰 `.superpowers/brainstorm/`。
- 已执行 `graphify update .`；仅有 `NetworkMonitor.kt` 和 `RoutePlanner.kt` 两个既有解析警告。
- 已执行 `git diff --check`，通过。
