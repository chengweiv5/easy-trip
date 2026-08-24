# Task 6 报告：批量加入与撤销领域用例

## 实现

- 新增 `AddPlacesToDayUseCase`，全链路使用 `String` ID。
- 按输入列表顺序末尾追加，保留重复 `placeId`，返回真实 `createdItemIds`。
- 单项失败后继续后续项，结果为 `PartialSuccess`，且失败不会造成追加索引空洞。
- 提交前目标日缺失或 trip 不匹配时零写入并返回全部 `retainedPlaceIds`。
- 中途目标日失效时停止，不改投其他日；由于 Room 会级联删除该日此前创建项，恢复完整请求地点选择（含此前失败、成功和重复项），`createdItemIds` 仅供审计，UI 不得依赖其仍存在。
- 新增 `UndoAddedItemsUseCase`，只按本次 `createdItemIds` 调用 `deleteItem`，不删除 SavedPlace 或历史同地点 Item。
- 最小异常契约扩展：`TargetDayNotFoundException` 只表达目标日不存在；`RecoverablePlaceAddException` 只表达可恢复的未知地点或地点/目标日跨旅行错误。用例仅捕获这两类业务异常，基础设施、RouteLeg、ID 与不变量异常继续向上传播；未新增重复的批量 Repository API。
- RouteLeg 仍完全由现有 `addItem` / `deleteItem` 的日内同步逻辑维护，不引入跨天边。

## TDD / JVM

RED：先新增 `AddPlacesToDayUseCaseTest`、`UndoAddedItemsUseCaseTest`，聚焦命令因用例、请求、结果及类型化目标日异常不存在而编译失败。

GREEN：

- `./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.domain.AddPlacesToDayUseCaseTest"`：通过。
- `./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsUseCaseTest"`：通过。
- `./gradlew compileDebugKotlin`：通过。

覆盖：重复地点、选择顺序、末尾索引、partial success、提交前目标日缺失、trip/day 不匹配、普通失败→成功→目标日失效时完整恢复选择、未知基础设施异常传播、空输入、仅撤销本次创建项。

## Fix 1

RED：新增回归测试后，类型化可恢复地点异常尚不存在，测试编译失败；原实现还会在中途失效时只保留剩余地点，并吞掉未知基础设施异常。

GREEN：完整选择恢复和严格异常分类均通过聚焦 JVM 测试；`compileDebugKotlin` 通过。

## Room gate

- 新增真实 in-memory Room 测试 `AddPlacesRoomIntegrationTest`，覆盖重复地点与末尾顺序、类型化 partial success、未知 RouteLeg 写入异常传播和事务回滚、目标日异常映射、中途删除恢复完整选择、Undo 保留 SavedPlace/历史 occurrence，以及跨天无 RouteLeg。
- `./gradlew compileDebugAndroidTestKotlin`：通过。
- 后续独占设备运行 `AddPlacesRoomIntegrationTest`：7/7 PASS，已完成 connected Room gate。

## 最终状态

- **Task 6 complete**：静态审查/JVM gate 与 Android Room runtime 7/7 均通过。
- 实现提交：`c76e3a3126283426c0820f2065f8360e39b621b2`。
- 该提交晚于 Batch 2 功能范围，只为后续任务 ledger 连续性记录；Batch 2 gate 不依赖 Task 6。

## 未包含与风险

- 整批加入和撤销不是单事务；当前契约是逐项原子、允许部分成功。并发追加时不能保证整批连续不穿插。
- `CancellationException` 与所有未声明为可恢复的异常继续传播。
