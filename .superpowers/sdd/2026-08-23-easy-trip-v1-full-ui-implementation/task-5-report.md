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

## Fix round 1

- 新增 Room 聚合 Flow，同时观察 `saved_places` 与 `itinerary_items`；仅行程项变化时 usage count 也会刷新，不修改 schema。
- 地点行由地点与 usage 快照组合后原子发布；usage 尚未知时不显示“仅收藏”，避免把未知状态伪装为 0。
- `SavedPlaceRowUi` 直接携带稳定的 `SavedPlace`，Content 不再跨 `rows` 与 `search.savedPlaces` 执行 `first { id }` 拼接。
- 行程状态字段改为 `scheduled`，与 Task 7 的用户多选 `selectedPlaceIds` 保持独立语义。
- 图层菜单关闭控件扩展为至少 48×48dp；标签栏改为横向滚动，窄屏与 2× 字体下末尾标签可达。
- RED 覆盖独立 itinerary usage 变化、usage 首帧未知、异步地点列表错位、关闭控件 bounds、窄屏大字体标签可达；首轮 Compose RED 共 3 项，分别暴露 Room 双 Flow 时序、标签栏无滚动语义和关闭控件无 48dp 目标。
- GREEN：`PlacePoolViewModelTest` 6/6；Task 5 Compose/Workspace 目标套件 19/19；`RoomSavedPlaceRepositoryTest` 5/5，其中真实 Room 测试证明仅插入 `itinerary_items` 即触发 count 0→1；`testDebugUnitTest lintDebug assembleDebug` 成功。

## Fix round 2：Room 测试调度竞态

- 根据 `batch-2-room-timeout-diagnosis.md` 的既有 RED 证据，确认失败来自 `runTest` 虚拟 timeout 与 Room 真实后台 executor 的调度域不一致；未另改测试制造 RED。
- 仅将 `RoomSavedPlaceRepositoryTest.usageCountsRefreshWhenOnlyItineraryItemsChange` 的等待区切到 `Dispatchers.Default.limitedParallelism(1)`，生产 DAO/repository 未修改。
- GREEN：完整 `RoomSavedPlaceRepositoryTest` 连续 3/3 次通过（每次 5/5）；Batch 2 目标 instrumentation suite 连续 2/2 次通过（每次 22/22）；`testDebugUnitTest lintDebug assembleDebug` 成功。
- Batch 2 自动化门禁恢复为通过，但功能旅程与视觉验收尚未运行，因此 Batch 2 gate 仍为进行中。

## 关注点

- Batch 2 功能旅程与视觉验收尚未运行，不能把 Batch 2 gate 标记为 complete。
- 最终物理设备验收因安装授权被拒绝而待办。
- 搜索 Surface 自动测试锁定最小高度与语义；不透明背景和接近全宽主要由组件实现及 Compose 布局验证保证。

## Graphify

完成代码后运行 `graphify update .`，仅提交正式图谱产物，不提交缓存。

## 提交

提交信息：`Complete place pool details and map controls`。不 push。
