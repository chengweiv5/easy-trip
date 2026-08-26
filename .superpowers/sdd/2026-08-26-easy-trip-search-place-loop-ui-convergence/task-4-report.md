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

## 关注点

- 额外运行 `PlacePoolFlowTest` 时，旧用例修正后仍在 `searchSaveEditAndFilterThroughPlacePool` 出现 instrumentation 进程崩溃；该非 brief focused 套件未通过。brief 指定的 Compose focused 测试、两个 ViewModel JVM 测试和必要编译均通过。
- 未修改 `diagrams/`，未实现 Task 8 的 PLACE_POOL 单地点加入行程。
