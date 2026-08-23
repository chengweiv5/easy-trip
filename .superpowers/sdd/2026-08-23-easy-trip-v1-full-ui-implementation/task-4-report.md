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

生产环境结果态、空态和连续收藏未能通过真实高德数据手工验证：同意隐私后 AMap `GLSurfaceView` 在该模拟器以 `java.lang.RuntimeException: createContext failed: EGL_SUCCESS` 崩溃。对应状态与连续收藏已由 Compose/JVM/Room 自动化测试覆盖。

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

## Graphify

已运行 `graphify update .`。`graphify-out` 生成物未纳入 Task 4 提交。

## 自审与关注点

- 搜索路由不再生产 `SEARCH_SELECTION_RESULT`，工作台旧消费路径已从 `AppNavigation` 移除；领域转换辅助函数和历史测试保留，避免越界重构。
- 旧 `PlaceSearchScreen` 仅作为 deprecated 兼容包装器保留，`onSelect` 不再执行。
- 真实结果态和连续收藏的模拟器手工证据受 AMap/EGL 环境阻塞；自动化覆盖通过，但这是本批次剩余关注点。

## 提交

提交信息：`Support continuous collection from place search`。最终 SHA 以本报告所在本地提交为准。
