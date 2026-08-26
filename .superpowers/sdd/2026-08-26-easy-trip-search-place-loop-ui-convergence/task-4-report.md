# Task 4 报告

## 状态

已完成共享 `PlaceDetailPanel`、SEARCH/PLACE_POOL 来源能力矩阵、详情编辑草稿边界和旧详情 UI 迁移。

## 实现

- 新增无状态 `PlaceDetailPanel`，输入 candidate、可选 SavedPlace、可选 edit state、来源、按 POI collection busy/error 和 action 回调。
- SEARCH unsaved 提供收藏；SEARCH saved 提供收藏/取消与编辑；PLACE_POOL 提供编辑与删除；所有来源均不提供单地点加入行程。
- 地址、备注、标签为空时显示明确 fallback。
- 编辑态原位替换只读内容，容器可滚动并使用 `imePadding`；保存时禁用输入、保存、取消和关闭。
- 标题提供 heading semantics。
- SEARCH 详情接入共享 panel，并继续按当前 POI 传递 collection busy/error。
- PLACE_POOL 编辑对话框接入共享 panel；删除无调用的旧 `EditSavedPlaceDialog`。
- `PlaceDetailContent` 保留为兼容 wrapper，但委托给共享 panel。
- PlacePool 草稿新增真实 SavedPlace `placeId`；Search 编辑只能由 `savedPlacesByPoiId` 中的 SavedPlace id 启动，不使用 PlaceCandidate poiId 充当 SavedPlace id。

## RED

- `PlaceDetailPanelTest` 首次运行在编译期按预期失败：`PlaceDetailSource`、`PlaceDetailPanel` 尚不存在。
- 两个 ViewModel focused 测试首次运行在编译期按预期失败：PlacePool 草稿缺少 `placeId`，Search 缺少 `StartEdit`/`CancelEdit`。

## GREEN

- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceDetailPanelTest`：6/6 通过。
- `./gradlew :app:testDebugUnitTest --tests com.yangchengwei.easytrip.place.ui.PlacePoolViewModelTest --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest`：通过。
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:compileDebugUnitTestKotlin`：通过。
- `PlaceSearchContentTest`：19/19 通过。
- `graphify update .`：完成，4533 nodes / 9683 edges。

## Fix round 1

### 修复

- 兼容 wrapper 将 `Dismiss` 和 `CancelEdit` 贯通到宿主关闭回调，`Delete` 使用独立删除回调，不再落入 no-op 或借用收藏语义。
- workspace 详情显式使用 `PlaceDetailSource.PlacePool`，编辑态提供取消和删除，不显示收藏操作；删除继续分发真实 `SavedPlace`。
- 抽取 `PlaceDetailDialog`，在 `detailSaving` 时同时屏蔽 `AlertDialog.onDismissRequest` 和 panel 内关闭、取消、删除，覆盖系统返回键与外部点击。
- 移除 `PlaceDetailDraft.placeId` 的空字符串默认值；所有构造点均传递真实 `SavedPlace.id`。

### RED / GREEN

- RED：新增 wrapper action 贯通、PLACE_POOL 来源能力和 saving 宿主返回键门禁测试后，因 wrapper 参数和 `PlaceDetailDialog` 尚不存在而编译失败。
- GREEN：`PlaceDetailPanelTest` 9/9 通过。
- GREEN：`PlacePoolFlowTest#placeDetailAllowsCollectionNoteAndTagsOnly` 与 `WorkspaceFlowTest#mapDetailBackThenPlaceEditRendersNewTarget` 2/2 通过；workspace 旧断言曾要求 PLACE_POOL 显示“取消收藏”，已按来源能力矩阵修正为取消且不显示收藏。
- GREEN：`PlacePoolViewModelTest` 与 `PlaceSearchViewModelTest` 通过。
- GREEN：应用、AndroidTest、UnitTest Kotlin 编译通过。
- GREEN：`graphify update .` 完成，4547 nodes / 9714 edges。

## Fix round 2

### 修复

- 将从编辑态发起删除的状态收敛到 `PlacePoolViewModel.requestDelete`：开始准备删除时立即清空 `editing`、`detailDraft` 和保存状态，同时继续异步保留删除确认目标。
- 独立 `PlacePoolSheet` 与 workspace 均通过同一个删除状态转换入口，避免 UI 各自拼接关闭编辑态；确认删除后旧编辑框不会恢复。

### RED / GREEN

- RED：新增真实 `PlacePoolViewModel` 状态转换测试后，在发起删除后的 `editing` 非空断言处按预期失败。
- GREEN：`PlacePoolViewModelTest` 全部通过。
- GREEN：新增独立 `PlacePoolSheet` 编辑删除 Compose 测试与 `PlaceDetailPanelTest` 合计 10/10 通过。
- GREEN：应用、AndroidTest、UnitTest Kotlin 编译通过。
- GREEN：`graphify update .` 完成，4552 nodes / 9729 edges。

## Fix round 3

### 回归门禁

- 新增真实 `PlacePoolViewModel` + `PlacePoolSheet` 流程测试：进入编辑态、点击删除、确认编辑态已关闭且删除确认出现、取消删除后确认 `deleting` 清空，`editing`/`detailDraft` 保持为空且详情 Dialog 不再出现。
- 该测试覆盖真实 UI wiring；若删除按钮错误地只请求删除而未关闭编辑态，或取消删除错误地恢复编辑态，断言会失败。
- 测试在 Fix round 2 的生产实现上首次运行即通过。这是审查要求补充的缺失回归门禁，因此本轮未为制造 RED 修改生产代码。

### 验证

- `PlacePoolFlowTest#cancellingDeleteFromEditDoesNotRestoreDetail`：1/1 通过。
- `PlacePoolViewModelTest`：全部通过。
- 应用、AndroidTest、UnitTest Kotlin 编译通过。
- `graphify update .` 完成，4556 nodes / 9740 edges。

## 关注点

- 先前 `PlacePoolFlowTest.searchSaveEditAndFilterThroughPlacePool` 整类运行时发生一次 instrumentation 进程崩溃；同 commit、同 AVD 串行复跑该用例 1/1 通过，runner result code 0。现无产品崩溃、选择器或 OOM 的直接证据，详见 `task-4-place-pool-flow-diagnostic.md`，未据此修改产品代码。
- 逐字标签输入与校验留给 Task 5，本轮未越界调整。
- 未修改 `diagrams/`，未实现 Task 8 的 PLACE_POOL 单地点加入行程。
