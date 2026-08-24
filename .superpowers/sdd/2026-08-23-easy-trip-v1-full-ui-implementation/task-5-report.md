# Task 5 报告：地点池、详情与地图控件

## 实现结果

- 地点池新增空态、搜索动作和独立地点行，明确展示“仅收藏”或“已排入 N 次”。
- 地点详情仅允许取消收藏、编辑备注和标签，不提供加入行程动作。
- 详情保存失败时保留草稿、详情目标及错误信息；保存成功后由 Route 关闭详情 overlay。
- 地图新增文字与形状并用的状态图例。
- 地图搜索入口改为接近全宽、白色不透明 Surface。
- 图层菜单复用排他的 `WorkspaceOverlay.LayerMenu`，没有引入第二套 Boolean overlay 状态。
- usage count 不再阻塞地点行首帧发布；地点先展示，再逐项更新真实排入次数。

## Pencil 对照

通过 Pencil MCP 读取 Task brief 指定 frame，未直接读取加密 `.pen` 文件：`A9EKX`、`p4G1tS`、`shoPV`、`lsr1I`、`jQhXs`。

实现遵循设计中的地点池空态与列表层级、受限地点详情、地图图例、近全宽搜索入口和单一图层菜单。搜索入口保持不透明白色面，图例同时使用文字和形状，避免仅靠颜色表达状态。

## TDD 证据

RED 阶段实际运行：

- JVM 测试最初失败于缺少 `SavedPlaceRowUi`、地点 rows、详情草稿和保存错误状态。
- Compose 测试最初失败于缺少 `PlaceDetailContent`、`MapLegend`、`SearchSurface` 和 `MapControls`。
- 新增详情保存失败测试先证明旧实现会丢失草稿。
- Workspace 回归首次暴露延迟 usage fake 会阻塞 rows 发布；修复为先发布地点行，再补充 count。
- Room 多 Flow 测试暴露旧快照互相覆盖 tags 与 saved POI IDs；修复为 `combine` 加 `MutableStateFlow.update`。

GREEN 阶段验证：

- `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest,com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest,com.yangchengwei.easytrip.workspace.MapLayerFlowTest`：17/17 成功。
- `./gradlew testDebugUnitTest lintDebug assembleDebug`：成功。

覆盖：地点状态区分、空态搜索、详情允许动作边界、失败草稿保留、地图图例文本与形状、搜索 Surface 尺寸语义、排他图层菜单，以及原有 workspace 删除/返回行为。

## 模拟器与物理设备验证

- `emulator-5554` 已通过显式 activity 启动生产 debug APK；UI dump 根节点 package 为 `com.yangchengwei.easytrip`，不是 Launcher。
- Task 5 目标 Compose 套件已在该 AVD 上 17/17 通过，覆盖地点池、详情、地图图例、搜索入口和图层 overlay 行为。
- 本轮尝试从全新空数据库手工重建完整生产旅程时，创建旅行表单的天数字段自动化输入未能可靠替换既有值，因此没有把该次手工操作声明为完整旅程验收。
- 物理设备 `FMR0224725012307` 安装被设备端拒绝：`INSTALL_FAILED_ABORTED: User rejected permissions`。需要用户在设备上授权后才能完成最终物理设备验收。
- 既有 Task 4 AVD 生产流程已验证真实 API 搜索、连续收藏及返回地点池；本 Task 的新增行为由生产组件 Compose 测试覆盖。最终物理设备旅程仍待办。

## 修改文件

- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/SavedPlaceRow.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceDetailContent.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/MapLegend.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/SearchSurface.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/MapControls.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceRoute.kt`
- `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModelTest.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlacePoolFlowTest.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/MapLayerFlowTest.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`
- `.superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/task-5-report.md`
- `.superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/progress.md`
- Graphify 正式产物。

## 关注点

- 最终物理设备验收因安装授权被拒绝而待办。
- usage count 当前逐项查询；地点先以缓存值或 0 展示，随后更新真实次数，可能短暂显示“仅收藏”。
- 仅 itinerary usage 变化而地点 Flow 不发射时，地点行次数不会主动刷新；后续行程任务接入时应统一触发刷新。
- 地点行点击仍从 `search.savedPlaces` 取领域对象；极短暂的 Flow 不一致窗口理论上可能导致找不到对应项。
- 搜索 Surface 自动测试锁定最小高度与语义；不透明背景和接近全宽主要由组件实现及 Compose 布局验证保证。

## Graphify

完成代码后运行 `graphify update .`，仅提交正式图谱产物，不提交缓存。

## 提交

提交信息：`Complete place pool details and map controls`。不 push。
