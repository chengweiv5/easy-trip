# Task 6 Report

## 完成内容

- 新增只读 `PlaceDeletionImpact(itineraryItemCount, routeLegCount)` 仓储契约。
- Room 在单个 `withTransaction` 快照中读取行程项数和受影响路线段数。
- 路线段统计使用 `COUNT(DISTINCT l.id)`，同一路线段两端均命中时仅计一次。
- 保持 `deletePlaceAndReferences(placeId)` 原子写路径语义不变。
- Search 与 Place Pool 删除决策统一改用精确影响；`usageCount` 仅保留给列表安排次数。
- 零影响直接删除；非零影响展示行程项和路线段精确数量。
- 同步全部测试 fake repository 的新接口。

## TDD 与验证

RED：
- Room instrumentation 首次因 `PlaceDeletionImpact` / `deletionImpact` 尚不存在而编译失败。
- ViewModel 单测首次因 UI state 尚无 `deletionImpact` 而编译失败。

GREEN：
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest`：12/12 通过。
- `./gradlew :app:testDebugUnitTest --tests com.yangchengwei.easytrip.place.ui.PlacePoolViewModelTest --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest`：通过。
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest`：19/19 通过。
- `git diff --check`：通过。
- `graphify update .`：完成；工具报告两个既有 Kotlin 文件存在部分 AST 解析告警。

## 覆盖点

- Room：零影响、多 occurrence、distinct incident legs、同一 leg 双端命中不重复、查询只读。
- ViewModel：零影响直接删除、非零精确确认、取消无副作用、重复确认仅一次、stale impact 不回写、删除失败保留确认。

## Fix round 1

### 审查问题与修复

- Search 与 Place Pool 的 impact 查询增加目标与 generation 双重校验；不可取消的旧请求完成后不会覆盖新目标。
- Place Pool 的显式删除和取消收藏确认在异步删除期间保留目标与确认上下文，禁止 dismiss、重复确认和冲突操作；完成或失败仅能更新对应目标与 generation。
- Workspace 与 Place Pool Sheet 同步 busy/back/dismiss 门禁，确认后不再立即关闭弹窗，操作完成后再关闭；工作区确认按钮在进行中禁用。
- 捕获 impact 查询失败和零影响直接删除失败，保留地点、影响与错误上下文供重试，避免异常逃逸到 Main dispatcher。
- 收紧 ViewModel 测试 fake，删除 nullable impact 的隐式零路线兜底。

### TDD 证据

- RED：新增不可取消跨目标 impact 测试、删除中 dismiss/替换测试、查询失败与零影响删除失败测试后，首次因缺少 busy/error 状态和 generation 门禁而编译或断言失败。
- RED：新增 Place Pool collection 删除中冲突操作测试后，观察到 `collectionBusyPoiIds` 同时包含旧、新 POI，证明新目标仍可启动。
- GREEN：增加目标 + generation 校验和 confirmed-removal busy 门禁后，新增测试通过。

### 验证

- `./gradlew :app:testDebugUnitTest`：通过。
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest`：16/16 通过。
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest`：19/19 通过。
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest`：12/12 通过。
- `git diff --check`：通过。
- `graphify update .`：完成；仍报告两个既有 Kotlin 文件的部分 AST 解析告警。
- `PlacePoolFlowTest#deletingFromEditClosesDetailAndDoesNotRestoreItAfterConfirmation`：通过；测试已显式创建行程引用以进入非零影响确认路径。
- `PlacePoolFlowTest` 全类：仍有 3 个既有 UI 基线失败（旧“标签（逗号分隔）”控件断言 2 个、窄屏长列表 1 个），随后 instrumentation process crash；这些断言对应当前分步标签 UI，且不属于本轮删除状态机改动。
- `WorkspaceFlowTest#backCannotDismissPlaceConfirmationWhileDeleteRuns` 与 `#deleteConfirmationWaitsForReadyTargetAndConfirmsOnce`：分别单独复跑通过；全类一次运行首测后设备进程崩溃。
