# Task 6 接口预研：批量加入与撤销

## 范围与依据

本预研只分析 Task 6，不实现代码、不改 schema、不运行设备。依据：

- `docs/superpowers/plans/2026-08-23-easy-trip-v1-full-ui-implementation.md:494-545`
- `.superpowers/sdd/2026-08-23-easy-trip-v1-full-ui-implementation/task-6-preflight/task-brief.md`
- `docs/superpowers/specs/2026-08-23-easy-trip-v1-full-ui-implementation-design.md:488-526`
- 当前 `ItineraryRepository`、`RoomItineraryRepository`、`ItineraryDao`、`RouteLegDao`、Room 实体及现有事务测试。

计划中的 `Long` 仅是示意，与当前代码的 ID 类型不一致；Task 6 应沿用现有 `String` ID，不做全局 ID 迁移。

## 现有可复用接口签名

### Domain Repository

```kotlin
interface ItineraryRepository {
    fun observeDay(dayId: String): Flow<DayItinerary>
    suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int): String
    suspend fun moveItem(itemId: String, targetDayId: String, targetIndex: Int)
    suspend fun deleteItem(itemId: String)
    suspend fun updateTiming(itemId: String, arrivalTime: LocalTime?, stayMinutes: Int?)
    suspend fun removePlaceOccurrences(placeId: String)
}
```

可直接复用：

- `observeDay(dayId)`：取得当前末尾索引，也能核验目标日存在及其 `tripId`。
- `addItem(dayId, savedPlaceId, targetIndex)`：返回真实创建 ID；允许同一 `savedPlaceId` 重复生成不同 Item；每次写入均在 Room 事务中重排并同步日内相邻 RouteLeg。
- `deleteItem(itemId)`：只删 ItineraryItem，并在事务内修复受影响 RouteLeg；不会删除 SavedPlace。

### Room/DAO

```kotlin
suspend fun ItineraryDao.dayAndItems(dayId: String): DayItems?
suspend fun ItineraryDao.items(dayId: String): List<ItineraryItemEntity>
suspend fun ItineraryDao.tripIdForDay(dayId: String): String?
suspend fun ItineraryDao.savedPlace(placeId: String): SavedPlaceEntity?
suspend fun ItineraryDao.insertItem(item: ItineraryItemEntity)
suspend fun ItineraryDao.deleteRow(itemId: String): Int
suspend fun RouteLegDao.legs(dayId: String): List<RouteLegEntity>
suspend fun RouteLegDao.insert(leg: RouteLegEntity)
suspend fun RouteLegDao.deleteEdge(dayId: String, fromItemId: String, toItemId: String): Int
```

`RoomItineraryRepository.addItem/deleteItem/moveItem/removePlaceOccurrences` 已用 `database.withTransaction` 包裹。`ItineraryItemEntity` 的复合外键保证 Item 的 day、trip、place 同属一个旅行；`RouteLegEntity` 的复合外键保证一条 leg 两端属于同一天。现有 `ItineraryTransactionTest` 已提供 in-memory Room、确定性 ID factory、顺序/边断言、失败回滚与并发测试模式。

## 接口缺口与建议边界

### 缺口 1：当前接口无法把“整批加入”作为一个 Repository 原子操作

逐次调用 `observeDay + addItem` 可以实现按顺序末尾追加和真实 ID 返回，但每个 `addItem` 是独立事务，因此批次中途失败会留下已创建项。这恰好能表达 `PartialSuccess`，却不能保证以下复合条件在一个临界区内完成：

- 目标日在开始提交后不被删除；
- 所有追加索引基于同一份末尾快照；
- 并发写入时本批条目保持连续且严格按选择顺序；
- 批量撤销不与并发编辑交错。

Task 6 的产品模型明确包含 `PartialSuccess`，所以不应把业务结果强行设计成“全批成功或全批回滚”。建议把“原子”拆成两层：每个 place 的 Item + RouteLeg 更新必须原子；整批由 use case 顺序执行并记录成功/失败。若产品要求本批连续不可穿插，再新增最小批量 Repository 方法，而不是暴露 DAO/Room 给领域层。

建议最小扩展（二选一，以失败测试证明需要后再加）：

```kotlin
suspend fun appendItems(dayId: String, savedPlaceIds: List<String>): List<String>
suspend fun deleteItems(itemIds: List<String>)
```

其中 `appendItems` 若采用单事务，则无法自然返回“部分成功”；除非先完整校验所有地点，失败即零写入。这与计划的 `PartialSuccess` 不一致。更贴合需求的是保留单项 `addItem`，只补一个原子的 `deleteItems(itemIds)` 用于撤销；批量添加的部分成功由 use case 编排。

### 缺口 2：无法无歧义地区分目标日失效与其他失败

`observeDay` 对未知日抛 `IllegalArgumentException("Unknown day: ...")`；`addItem` 对未知日、未知地点、跨旅行、索引错误也都使用参数异常。用例若靠异常消息分类会脆弱。

最小方案：用例提交前读取目标日并验证 `tripId`；调用期间若日被删除，Repository 仍会抛异常。若必须稳定映射为 `TargetDayMissing`，应增加类型化结果/异常或一个明确查询，例如 `suspend fun day(dayId: String): DayItinerary?`。不要解析错误字符串。

### 缺口 3：`tripId` 校验只能间接完成

`AddPlacesRequest.tripId` 应为 `String`。`observeDay(dayId)` 可验证 day 的 trip；每次 `addItem` 也会检查 SavedPlace 与 day 同 trip。没有必要再依赖 `SavedPlaceRepository`；但若要把跨旅行地点列入 `failedPlaceIds` 而不是抛出，use case 需要逐项捕获失败。

### 缺口 4：批量撤销当前不是整体原子

循环 `deleteItem` 能正确保留 SavedPlace、逐步修复 RouteLeg，并且已删除/失效 item 可逐项失败；但不是单事务。如果撤销语义要求全有或全无，应补 `deleteItems(itemIds)`，在一个 Room 事务中按天计算最终序列、删指定 ID、重排并一次性同步每个受影响日的 RouteLeg。

## 最小领域模型

沿用字符串 ID，并让撤销只接受本次返回的 Item ID：

```kotlin
data class AddPlacesRequest(
    val tripId: String,
    val dayId: String,
    val savedPlaceIds: List<String>,
)

sealed interface AddPlacesOutcome {
    val dayId: String
    val createdItemIds: List<String>

    data class Success(
        override val dayId: String,
        override val createdItemIds: List<String>,
    ) : AddPlacesOutcome

    data class PartialSuccess(
        override val dayId: String,
        override val createdItemIds: List<String>,
        val failedPlaceIds: List<String>,
    ) : AddPlacesOutcome

    data class TargetDayMissing(
        val retainedPlaceIds: List<String>,
    ) : AddPlacesOutcome {
        override val dayId: String = ""
        override val createdItemIds: List<String> = emptyList()
    }
}

data class UndoAddedItemsRequest(val createdItemIds: List<String>)
```

更简洁的实现可完全照计划保留三个互不继承公共属性的 outcome data class；关键约束是：

1. `savedPlaceIds` 是有序 `List`，不得转 `Set`。
2. 重复 ID 保留重复次数。
3. `createdItemIds` 与成功输入一一对应且同序。
4. Undo 只消费 `createdItemIds`，绝不能按 `savedPlaceId` 删除。
5. 空列表应定义为成功且无写入，避免制造特殊错误。

## 行为算法建议

### AddPlacesToDayUseCase

1. 读取目标日；不存在或 `tripId` 不匹配时返回 `TargetDayMissing(savedPlaceIds)`，不改投其他日。
2. 记录初始末尾索引。
3. 按 `savedPlaceIds` 原顺序逐项调用 `addItem(dayId, placeId, currentEnd)`；每成功一次递增末尾索引并保存真实 Item ID。
4. 单项失败记录原始 `placeId`，后续项是否继续必须由测试固定。为使 `PartialSuccess.failedPlaceIds` 有意义，建议继续尝试后续项，而不是首错即停。
5. 全成功返回 `Success`；至少一项失败且至少一项成功返回 `PartialSuccess`。若全部地点失败，现有模型没有专门 outcome，建议仍用 `PartialSuccess(createdItemIds = emptyList(), failedPlaceIds = all)`，不要新增未要求的状态。

注意：每次成功后应用当前末尾索引，不能用 `mapIndexed { initialSize + index }`，否则前一项失败会使下一项索引越界。

### UndoAddedItemsUseCase

- 仅删除调用方提供的本次 `createdItemIds`。
- 不查询、不删除 SavedPlace。
- 同一地点之前存在的 Item 不受影响。
- 推荐 Repository 原子批删；若 Task 6 暂不扩接口，则按 ID 调用 `deleteItem`，并明确这是“逐项原子、整批非原子”的已知限制。

## 测试矩阵

| 场景 | 层级 | 初始数据 | 操作 | 必须断言 |
|---|---|---|---|---|
| 重复 `placeId` | JVM use case + Room | day 空；输入 `[hotel, hotel, museum]` | 批量加入 | 创建 3 个互异 Item ID；place 顺序保持；重复项不去重；日内产生 2 条相邻 leg |
| 末尾追加 | JVM + Room | day 已有 `[a,b]` | 加入 `[c,d]` | 最终 `[a,b,c,d]`；原有 `[a,b]` 次序不变；返回 ID 对应 `c,d`；位置规范为步长 1000 |
| 部分成功 | JVM fake 必测，Room 可补 | `[ok, missing, ok2]` | 批量加入 | outcome 为 `PartialSuccess`；真实 created IDs 按成功顺序；failed 为 `[missing]`；第二个成功项紧接前一个成功项，不因失败留下索引空洞 |
| 目标日提交前失效 | JVM fake/flow | 请求仍持有已删除 day | 提交 | `TargetDayMissing(retainedPlaceIds = 原选择)`；零 `addItem` 调用；不选择首日或其他日 |
| 目标日在批次中失效 | JVM fake | 首项成功后模拟 day 删除 | 继续加入 | 不把后续项写入其他日；明确映射结果。现有 outcome 难同时表达“已创建项 + day missing”，这是需产品/接口确认的风险 |
| 撤销保留 SavedPlace | Room | 本批创建若干 Item | Undo | Item 与受影响 leg 删除/修复；`saved_places` 行仍存在；备注、标签不变 |
| 撤销不删先前重复项 | JVM + Room | 同一 place 先有 old item，本批再建 new item | Undo(new IDs) | old item 保留；只删除 new IDs；不能调用 `removePlaceOccurrences(placeId)` |
| 跨天无 RouteLeg | Room | day-1、day-2 各有 item | 分别批量追加 | 每条 leg 的 `tripDayId` 与两端 item day 相同；不存在 day-1 尾项到 day-2 首项的 leg |
| 空输入 | JVM | 任意有效 day | 加入 `[]` | Success + 空 IDs；零 Repository 写调用 |
| trip/day 不匹配 | JVM + Room | request.tripId 与 day.tripId 不同 | 提交 | 零写入；不能仅依赖 place 校验后才发现 |
| 全部失败 | JVM | 所有 place 无效 | 批量加入 | `PartialSuccess(emptyList(), allIds)` 或经确认的新失败类型；选择保留 |
| 并发追加 | Room | 同 day 有并发写 | 两批追加 | 位置唯一、合法；每批内部顺序是否必须连续需先定契约；最终 legs 恰为 `items - 1` |
| 撤销中途失败 | JVM fake + Room 故障注入 | 多个 created IDs | Undo | 若采用批删，全部回滚；若循环删除，文档与 UI 不得声称整批原子 |

Room 集成测试沿用 `ItineraryTransactionTest` 模式：in-memory `EasyTripDatabase`、固定 Clock、序列 ID factory，直接断言 `itineraryEditingDao().items(dayId)` 与 `routeLegDao().legs(dayId)`。不要通过 UI 间接验证数据库不变量。

## 建议文件列表

Task 6 最小实现文件：

- 新增 `app/src/main/java/com/yangchengwei/easytrip/itinerary/domain/AddPlacesToDayUseCase.kt`
- 新增 `app/src/main/java/com/yangchengwei/easytrip/itinerary/domain/UndoAddedItemsUseCase.kt`
- 仅在原子撤销或类型化查询被测试证明必要时修改 `app/src/main/java/com/yangchengwei/easytrip/itinerary/domain/ItineraryRepository.kt`
- 若扩 Repository，同步修改 `app/src/main/java/com/yangchengwei/easytrip/itinerary/data/RoomItineraryRepository.kt`
- 若批删需要 DAO 支撑，最小修改 `app/src/main/java/com/yangchengwei/easytrip/itinerary/data/ItineraryDao.kt`
- 新增 `app/src/test/java/com/yangchengwei/easytrip/itinerary/domain/AddPlacesToDayUseCaseTest.kt`
- 新增 `app/src/test/java/com/yangchengwei/easytrip/itinerary/domain/UndoAddedItemsUseCaseTest.kt`
- 新增 `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt`
- `DayItineraryViewModel.kt` 只有在 Task 6 确实接线 use case 时修改；多选/目标日/结果 UI 状态属于 Task 7，不应提前实现。

如果 Repository 新增抽象方法，现有所有测试 fake 都必须同步：至少 `DayItinerarySelectionTest`、`ItineraryEditingTest`、workspace 与 acceptance 测试中的 `ItineraryRepository` fake。

## 风险

1. **计划 ID 类型漂移**：brief 使用 `Long`，生产代码全为 `String`。直接照抄会导致大面积无价值迁移。
2. **部分成功与整批原子矛盾**：单事务批量 API通常要么全成要么全退；当前产品 outcome 又要求逐项失败列表。必须先固定语义，不可同时声称两者。
3. **目标日竞态**：提交前检查不能消除检查后删除；当前异常没有类型，无法可靠分类。
4. **并发末尾追加**：`observeDay().first().items.size` 与后续 `addItem` 分离时，其他写入可使索引失效或穿插。
5. **撤销误删**：按 placeId 撤销会删除历史重复项；`removePlaceOccurrences` 明确不适用于 Undo。
6. **RouteLeg 批量重算成本**：连续调用 `addItem/deleteItem` 每次都 diff 和写边，正确但产生中间写入与额外 ID；批量 Repository 可只按最终序列同步一次。
7. **故障归类**：未知 place、跨旅行、约束冲突、RouteLeg 插入冲突都可能是 `IllegalArgumentException`/SQLite 异常；只捕获所有 Throwable 会错误吞掉取消异常，use case 必须保留协程取消。
8. **中途 day 删除的 outcome 不完备**：现有 `TargetDayMissing` 没有 `createdItemIds`；若先成功后失效，无法支持精确 Undo。应通过 Repository 原子临界区避免，或扩展结果模型。

## 明确禁止项

- 禁止修改 Room schema、实体主键类型或做 migration。
- 禁止把 `savedPlaceIds` 转为 Set、去重或排序。
- 禁止目标日失效时静默改投第一天、相邻天或新建天。
- 禁止 Undo 调用 `removePlaceOccurrences(placeId)` 或 `SavedPlaceRepository.deletePlaceAndReferences`。
- 禁止删除 SavedPlace、备注、标签或早先存在的同地点 Item。
- 禁止创建跨 TripDay RouteLeg；RouteLeg 只连接同一天相邻 Item。
- 禁止绕过 Repository 从 use case 直接调用 Room DAO/Database。
- 禁止为 Task 7 提前加入 SavedStateHandle、多选 Overlay 或 UI 状态机。
- 禁止捕获并吞掉 `CancellationException`。
- 禁止为了复用而重写现有 Repository/Service/地图适配层。

## 结论

现有接口足以先写领域用例 RED 测试，并能实现“逐项原子、允许部分成功”的最小版本；它不足以保证整批连续追加和整批撤销的事务原子性，也缺少稳定的目标日失效分类。最优先要由测试固定的是：部分成功是否继续后续项、目标日在批次中失效的结果，以及 Undo 是否要求全批原子。除非这些测试证明必要，不新增重复 API；若必须扩展，优先只加领域化的批删/批量追加方法，绝不把 Room 事务泄漏到 use case。