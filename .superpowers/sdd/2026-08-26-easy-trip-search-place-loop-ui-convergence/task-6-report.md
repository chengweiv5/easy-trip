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
