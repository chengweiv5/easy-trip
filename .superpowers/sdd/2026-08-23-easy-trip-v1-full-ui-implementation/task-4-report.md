# Task 4 报告：重做搜索状态机与连续收藏

## 实现结果

- 搜索状态改为互斥的 `Initial`、`Loading`、`Results`、`Empty`、`NetworkFailure`。
- 新增独立 `PlaceSearchViewModel`、`PlaceSearchRoute`、`PlaceSearchContent`，保持 Route 收集状态和消费返回 effect，Content 仅接收不可变状态与 action。
- query 由 `SavedStateHandle` 恢复并持续保存。
- 收藏和取消收藏只写 `SavedPlaceRepository`，收藏事实由 Room Flow 回推 UI。
- 收藏后保持在搜索页，支持连续收藏；搜索 action 不创建 `ItineraryItem`。
- 移除搜索结果点击、saved-state 结果回传和自动返回路径；搜索结果仅暴露收藏按钮。
- 保留取消收藏影响确认流程。

## Pencil 对照

通过 Pencil MCP 读取 `ofdn5`、`S0psO`、`GJo79`、`s1OvvX`、`I62qd5`、`xENWi`、`IpuKg`，未直接读取 `.pen` 文件。

- `ofdn5`：390×844 页面、`#F5F3EE` 背景、20dp 横向留白、44dp 返回触控区、48dp 搜索框、绿色 2dp 描边、结果标题、46dp 地点图标框和 40dp 收藏按钮。
- `S0psO`：无结果标题、说明和“清空搜索”。
- `GJo79`：网络失败标题、说明和“重新搜索”。
- `s1OvvX`：加载标题、当前 query 文案和 42dp 进度指示器。
- `I62qd5`、`xENWi`、`IpuKg`：搜索仅负责连续收藏，返回地点池后由收藏数据展示；四种活跃状态互斥。

## TDD 证据

RED 阶段实际运行：

- JVM 初始失败于缺少 `PlaceSearchPhase`、新 `PlaceSearchViewModel` 和 `PlaceSearchAction`。
- Compose 初始失败于缺少 `PlaceSearchContent`；新增 IME 行为测试后先失败于 IME action 为 `Default`。

GREEN 阶段最终验证：

- `./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.place.ui.PlaceSearch*" --tests "com.yangchengwei.easytrip.place.ui.CollectionTogglePolicyTest"`：成功。
- `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest`：5/5 成功。
- `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest,com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest`：7/7 成功。
- `./gradlew testDebugUnitTest lintDebug assembleDebug`：成功。

覆盖内容包括状态互斥、SavedStateHandle 恢复、连续收藏、不创建行程项、无加入行程动作、IME Search、空态清空、失败重试、白色不透明内容面、280dp 窄屏与 2× 字体。

## 模拟器验证

设备：`emulator-5554`。

已安装并启动 debug APK，创建 2 天旅行 `Task4Trip`，拒绝高德隐私授权后进入工作台与生产搜索路由，并验证：

- 默认态可见，返回按钮和搜索框可操作。
- 输入并提交后进入失败态，展示“网络连接失败”“请先阅读并同意高德隐私政策”和“重新搜索”。
- 返回操作回到地点池/工作台，不存在结果点击自动返回路径。

证据：

- `task-4-evidence/launch.png`
- `task-4-evidence/default.png`
- `task-4-evidence/loading.png`
- `task-4-evidence/failure.png`

后续真实设备门禁已通过：同意高德隐私政策后，真实 API 搜索结果可见，可连续收藏多个地点，返回地点池后收藏结果均可见。真实 API 的空结果回调可能同时携带 `suggestions`；该响应已验证为正常空态，不再误报网络失败。早先模拟器上的 AMap `GLSurfaceView` / EGL 崩溃仅保留为模拟器环境记录，不再阻塞 Task 4 设备验收。

## 修改文件

- `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducer.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchScreen.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContent.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt`
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModel.kt`
- `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducerTest.kt`
- `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModelTest.kt`
- `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContentTest.kt`

## Fix round 1

独立审查提出的 4 个 Important 和 2 个 Minor 已逐项处理：

- 确认取消收藏在启动协程前同步占用 busy 状态；重复确认只触发一次删除，对话框 busy 时确认、取消及外部关闭均不可重复触发。
- 删除 deprecated `PlaceSearchScreen` 及旧签名，验收测试迁移到 `PlaceSearchContent`/`PlaceSearchViewModel`。
- 删除 `SEARCH_SELECTION_RESULT`、`SearchSelectionPayload`、转换/消费 helper 和旧消费测试；保留独立实时地图搜索聚焦模型及其 Task 3 回归测试。
- 清空搜索触控区调整为 48×48dp，图标保持 19dp，搜索框保持 48dp。
- SavedState 测试证明恢复 query 会 trim 后自动搜索，并证明忽略 cancellation 的恢复旧响应不会覆盖新 query。
- Compose 测试读取真实 bounds，锁定返回 44dp、搜索框 48dp、地点图标 46dp、收藏视觉 40dp/触控 48dp、清空触控 48dp；280dp/2× 字体覆盖 Results、Empty、NetworkFailure 并检查容器内可达及无交叠。

Fix round RED 证据：

- 重复确认测试在旧实现中收到两次删除，并留下成功后的伪失败。
- bounds 测试在旧实现中找不到尺寸节点；清空按钮旧实现仅 32dp。

Fix round GREEN 验证：

- Task 4 JVM 与 `CollectionTogglePolicyTest`：成功。
- `PlaceSearchContentTest`：6/6 成功。
- `V2AcceptanceTest`：1/1 成功，验证连续收藏后仍停留搜索页。
- `SearchMapFocusTest`：5/5 成功，Task 3 实时地图聚焦保持独立可用。

## Fix round 2

补齐原 Minor #6 的真实窄屏 bounds 验收：

- 在 280dp / 2× 字体下，Results 读取结果行、地点图标、文字区域、收藏视觉与 48dp 触控节点 bounds，断言全部位于内容 Surface 内，且收藏触控不与图标或文字关键区重叠。
- Empty 为状态主体、说明和清空搜索按钮提供稳定 tag；读取真实 bounds，断言均位于 Surface 内、说明与按钮不重叠、按钮触控高度至少 48dp。
- NetworkFailure 同样覆盖主体、说明和重新搜索按钮的容器边界、不重叠及最小触控高度。
- RED：新增测试在旧生产代码中因缺少 `place-search-result-row-poi-1` 等稳定布局节点失败。
- GREEN：补充仅用于布局定位的 test tags 后，`PlaceSearchContentTest` 6/6 通过；生产布局无需尺寸调整。

## Fix round 3

补齐原 Minor #6 剩余的相邻节点与双轴触控覆盖：

- Empty 和 NetworkFailure 为图标、标题增加纯定位 test tags；未修改尺寸、间距或状态行为。
- 在 280dp / 2× 字体下读取图标、标题、说明、动作的真实 bounds，逐对断言图标—标题、标题—说明、说明—动作不相交，并断言每个节点四边均位于 Surface 内。
- 最小触控 helper 同时断言宽度和高度均至少 48dp，覆盖 Results 收藏动作、Empty 清空搜索和 NetworkFailure 重新搜索。
- RED：旧生产代码因缺少 `place-search-empty-icon` 定位节点失败。
- GREEN：增加图标与标题纯 test tags 后，`PlaceSearchContentTest` 6/6 通过；生产布局无需调整。

## Fix round 4

修复设备门禁发现的真实空结果误报网络失败：

- 根因：高德 SDK 成功码回调在 `pois` 为空时进入 `parsePlaces`；旧实现无论 `suggestions` 是否为空都抛出 `POI_EMPTY`，随后 `PlaceSearchReducer` 将该异常映射为 `NetworkFailure`，因此绕过了其已有的“空列表 → `PlaceSearchPhase.Empty`”分支。
- RED：新增最小生产边界测试 `emptyPlacesWithoutSuggestionsReturnEmptyResults`；旧实现抛出 `AmapServiceException`，聚焦测试 1/1 按预期失败。
- 初版 GREEN 仅在候选和 suggestions 都为空时返回空列表，候选为空但存在 suggestions 时仍保留 `POI_EMPTY`；真实 API 验证证明这一假设不成立，因为正常空结果也可能携带 suggestions。
- 最终修复：高德成功码回调中，只要过滤后没有有效地点就返回空列表，由 `PlaceSearchReducer` 进入 `PlaceSearchPhase.Empty`；高德非成功码仍由 `AmapPlaceDataSource` 在解析前映射为 `POI_SEARCH` 网络失败，不受影响。
- 测试覆盖：`emptyPlacesWithoutSuggestionsReturnEmptyResults` 与 `emptyPlacesWithSuggestionsReturnEmptyResults` 同时锁定两种正常空结果。
- 验证：`./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.place.amap.PlaceContractsTest" --tests "com.yangchengwei.easytrip.place.ui.PlaceSearch*" --tests "com.yangchengwei.easytrip.place.ui.CollectionTogglePolicyTest"`：成功；`ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest,com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest,com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest`：13/13 成功；`./gradlew testDebugUnitTest lintDebug assembleDebug`：成功。
- 中间提交 `4d41d7c`（`Map empty POI responses to empty state`）只覆盖无 suggestions 空结果；最终提交见“提交”章节，未 push。

## Graphify

已运行 `graphify update .`。仅纳入项目正式图谱产物；未纳入缓存。

## 自审与关注点

- 搜索生产入口及 API 已不存在结果选择、SavedState 回传、自动返回或加入行程旁路。
- 真实设备已覆盖真实结果、连续收藏、返回地点池与携带 suggestions 的空结果，Task 4 device gate 完成。
- 无剩余 Task 4 阻塞项。

## 提交

Task 4 提交链：`5d10195`、`391f871`、`faa8a85`、`d1172c7`、`4d41d7c`、`351ff08`，以及本报告所在最终收尾提交；均未 push。
