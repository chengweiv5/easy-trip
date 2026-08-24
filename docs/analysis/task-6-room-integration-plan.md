# Task 6 Room Integration Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用真实 in-memory Room 固定 Task 6 批量加入与撤销的持久化契约，覆盖恢复选择、错误分类、相邻 RouteLeg 和撤销隔离。

**Architecture:** 新增一个 Android instrumentation 测试类，真实构建 `EasyTripDatabase`、`RoomItineraryRepository`、`AddPlacesToDayUseCase` 与 `UndoAddedItemsUseCase`，通过 DAO 直接布置和断言数据库状态。测试不经过 UI，不新增 schema 或生产 API；若实现尚未满足审查裁决，测试应保持失败并交回生产实现阶段修复。

**Tech Stack:** Kotlin、JUnit4、AndroidX Test、Room in-memory database、kotlinx-coroutines-test。

**Spec:** `docs/analysis/task-6-interface-preflight.md`（当前分支未包含时读取 commit `ef7429a`）；主计划 `docs/superpowers/plans/2026-08-23-easy-trip-v1-full-ui-implementation.md:494-545`；待测实现 commit `b26bda6`。

## Global Constraints

- 只创建 `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt`；不修改生产代码、现有测试、Room schema 或 migration。
- 所有 ID 使用现有 `String` 类型；请求中的 `savedPlaceIds` 是有序 `List`，不得去重或排序。
- 真实调用 `RoomItineraryRepository`，不得用 fake repository 替代 Room 集成边界。
- 审查裁决优先于 `b26bda6` 当前行为：中途目标日失效时 `retainedPlaceIds` 必须恢复**完整原始请求**，同时 `createdItemIds` 仍准确返回失效前已创建项。
- 只有类型化“单地点可恢复错误”可进入 `PartialSuccess.failedPlaceIds` 并继续后续地点；未知基础设施异常必须原样传播，不能被归类为地点失败。
- `TargetDayNotFoundException` 继续专用于目标日失效；测试不得靠异常消息判断错误类型。
- Undo 只按 `createdItemIds` 删除本批 occurrence；不得删除 `SavedPlace`、备注、标签或同地点的历史 occurrence。
- RouteLeg 只连接同一天相邻 Item；任何跨天边都必须不存在。
- 本计划的 connected 测试命令需要 Android 设备或 emulator；计划阶段不运行设备。

---

## File Structure

- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt`
  - 统一持有真实 Room fixture、确定性 Item/Leg ID factory、seed helpers 和全部 Task 6 Room cases。
- Read only: `app/src/main/java/com/yangchengwei/easytrip/itinerary/domain/AddPlacesToDayUseCase.kt`（来自 `b26bda6`）
  - 接受 `AddPlacesRequest`，返回 `Success`、`PartialSuccess` 或 `TargetDayMissing`。
- Read only: `app/src/main/java/com/yangchengwei/easytrip/itinerary/domain/UndoAddedItemsUseCase.kt`（来自 `b26bda6`）
  - 逐个调用 `ItineraryRepository.deleteItem(createdItemId)`。
- Read only: `app/src/main/java/com/yangchengwei/easytrip/itinerary/data/RoomItineraryRepository.kt`
  - 每次 add/delete 在 Room transaction 内重排 Item 并同步日内 RouteLeg。
- Read only: `app/src/main/java/com/yangchengwei/easytrip/itinerary/data/ItineraryDao.kt`、`app/src/main/java/com/yangchengwei/easytrip/route/data/RouteLegDao.kt`
  - 测试通过 `items(dayId)`、`item(itemId)`、`savedPlace(placeId)`、`legs(dayId)` 读取真实持久化结果。

## Shared Fixture

测试类沿用 `ItineraryTransactionTest` 的 fixture，但把 Task 6 用例与 DAO 验证集中在一个文件：

```kotlin
@RunWith(AndroidJUnit4::class)
class AddPlacesRoomIntegrationTest {
    private lateinit var database: EasyTripDatabase
    private lateinit var repository: RoomItineraryRepository
    private lateinit var addPlaces: AddPlacesToDayUseCase
    private lateinit var undoAddedItems: UndoAddedItemsUseCase

    private val now = Instant.parse("2026-08-24T00:00:00Z")
    private val itemIds = SequenceIds("item")
    private val legIds = SequenceIds("leg")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            EasyTripDatabase::class.java,
        ).build()
        repository = RoomItineraryRepository(
            database = database,
            itineraryDao = database.itineraryEditingDao(),
            routeLegDao = database.routeLegDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
            itemIdFactory = itemIds,
            legIdFactory = legIds,
            isOnline = { true },
            recommendMode = { _, _, _ -> TransportMode.WALK },
        )
        addPlaces = AddPlacesToDayUseCase(repository)
        undoAddedItems = UndoAddedItemsUseCase(repository)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun seedTrip(id: String, vararg days: String) {
        database.tripDao().insertTrip(
            TripEntity(id, id, TimeMode.DRAFT, null, TravelMode.FLEXIBLE, now, now),
        )
        days.forEachIndexed { index, dayId ->
            database.tripDao().insertDay(TripDayEntity(dayId, id, index * 1_000L))
        }
    }

    private suspend fun seedPlace(
        id: String,
        tripId: String = "trip",
        note: String? = null,
    ) {
        database.savedPlaceDao().insertPlace(
            SavedPlaceEntity(id, tripId, "poi-$id", id, "address-$id", 0.0, 0.0, note),
        )
    }

    private class SequenceIds(private val prefix: String) : () -> String {
        private val next = AtomicInteger()
        override fun invoke(): String = "$prefix-${next.getAndIncrement()}"
    }
}
```

固定断言规则：

- Item 顺序：`database.itineraryEditingDao().items(dayId)`；同时断言 `position == [0, 1000, ...]`。
- RouteLeg 顺序：`database.routeLegDao().legs(dayId)`；映射为 `fromItemId to toItemId`。
- SavedPlace：`database.itineraryEditingDao().savedPlace(placeId)`；需要时再用 `database.savedPlaceDao().place(placeId)` 验证备注。
- 结果 ID 必须与 DAO 实际 Item ID 完全相等，不能只断言数量。

---

### Task 1: 创建真实 Room 基线、重复地点与末尾追加测试

**Files:**
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt`

**Interfaces:**
- Consumes: `AddPlacesToDayUseCase.invoke(AddPlacesRequest): AddPlacesOutcome`；`RoomItineraryRepository.addItem(dayId, savedPlaceId, targetIndex): String`。
- Produces: 可复用 fixture；确定性 `item-N` / `leg-N`；批量追加后的真实 Item 和 RouteLeg 断言。

- [ ] **Step 1: 写入 shared fixture 和重复地点/末尾追加测试**

```kotlin
@Test
fun duplicatePlacesAppendAfterExistingItemsAndCreateAdjacentLegs() = runTest {
    seedTrip("trip", "day")
    listOf("a", "hotel", "museum").forEach { seedPlace(it) }
    val existing = repository.addItem("day", "a", 0)

    val outcome = addPlaces(
        AddPlacesRequest("trip", "day", listOf("hotel", "hotel", "museum")),
    )

    val success = assertIs<AddPlacesOutcome.Success>(outcome)
    val items = database.itineraryEditingDao().items("day")
    assertEquals(listOf(existing) + success.createdItemIds, items.map { it.id })
    assertEquals(listOf("a", "hotel", "hotel", "museum"), items.map { it.savedPlaceId })
    assertEquals(3, success.createdItemIds.distinct().size)
    assertEquals(listOf(0L, 1_000L, 2_000L, 3_000L), items.map { it.position })
    assertEquals(
        items.zipWithNext { from, to -> from.id to to.id },
        database.routeLegDao().legs("day").map { it.fromItemId to it.toItemId },
    )
}
```

预期：重复 `hotel` 产生两个不同 occurrence；新项严格追加到旧项后；每对日内相邻项恰有一条 leg。

- [ ] **Step 2: 运行单 case 确认测试边界**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.AddPlacesRoomIntegrationTest#duplicatePlacesAppendAfterExistingItemsAndCreateAdjacentLegs
```

预期：若 `b26bda6` 已合入待测工作树则 PASS；否则先出现缺少 Task 6 类型的编译失败，不能在本测试任务中补生产实现。

---

### Task 2: 固定类型化单地点恢复与未知基础设施异常传播

**Files:**
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt`

**Interfaces:**
- Consumes: 审查裁决要求的类型化可恢复单地点错误（实现阶段应提供明确异常/结果，例如 `RecoverablePlaceAddException(placeId, cause)`；以最终生产符号为准，但禁止捕获通用 `Exception`）。
- Produces: 一个真实 Room partial-success case；一个故障注入 case，证明未知 RouteLeg/SQLite 异常不会被吞掉。

- [ ] **Step 1: 写可恢复单地点错误测试**

使用存在的 `first`、缺失/非法的 `missing`、存在的 `last`。通过真实 Room repository 触发生产代码定义的**类型化**地点错误，而非伪造通用 `IllegalArgumentException`：

```kotlin
@Test
fun recoverableSinglePlaceFailureContinuesWithoutPositionGap() = runTest {
    seedTrip("trip", "day")
    seedPlace("first")
    seedPlace("last")

    val outcome = addPlaces(
        AddPlacesRequest("trip", "day", listOf("first", "missing", "last")),
    )

    val partial = assertIs<AddPlacesOutcome.PartialSuccess>(outcome)
    val items = database.itineraryEditingDao().items("day")
    assertEquals(listOf("missing"), partial.failedPlaceIds)
    assertEquals(partial.createdItemIds, items.map { it.id })
    assertEquals(listOf("first", "last"), items.map { it.savedPlaceId })
    assertEquals(listOf(0L, 1_000L), items.map { it.position })
    assertEquals(
        listOf(items[0].id to items[1].id),
        database.routeLegDao().legs("day").map { it.fromItemId to it.toItemId },
    )
}
```

预期：只有明确可恢复的 `missing` 进入 `failedPlaceIds`；`last` 继续写入索引 1，不留位置空洞。若生产代码仍用 `catch (_: Exception)`，此 case 不能证明裁决，必须配合下一 case 阻止泛捕获。

- [ ] **Step 2: 用 RouteLeg ID 冲突注入未知基础设施失败**

先真实建立 `a -> b`，取已有 leg ID；随后重建 repository，使 `legIdFactory` 总是返回该已有 ID。向日末追加 `c` 会在 `RouteLegDao.insert` 触发 `SQLiteConstraintException`，且 `RoomItineraryRepository.addItem` 的事务应回滚 `c`。

```kotlin
@Test
fun unknownRouteLegInsertFailurePropagatesAndRollsBackCurrentPlace() = runTest {
    seedTrip("trip", "day")
    listOf("a", "b", "c").forEach { seedPlace(it) }
    val a = repository.addItem("day", "a", 0)
    val b = repository.addItem("day", "b", 1)
    val existingLeg = database.routeLegDao().legs("day").single()
    val beforeItems = database.itineraryEditingDao().items("day")
    val beforeLegs = database.routeLegDao().legs("day")
    repository = RoomItineraryRepository(
        database,
        database.itineraryEditingDao(),
        database.routeLegDao(),
        Clock.fixed(now, ZoneOffset.UTC),
        itemIds,
        { existingLeg.id },
        { true },
        { _, _, _ -> TransportMode.WALK },
    )
    addPlaces = AddPlacesToDayUseCase(repository)

    assertThrows(SQLiteConstraintException::class.java) {
        runBlocking {
            addPlaces(AddPlacesRequest("trip", "day", listOf("c")))
        }
    }

    assertEquals(listOf(a, b), database.itineraryEditingDao().items("day").map { it.id })
    assertEquals(beforeItems, database.itineraryEditingDao().items("day"))
    assertEquals(beforeLegs, database.routeLegDao().legs("day"))
}
```

预期：异常原样传播；绝不能返回 `PartialSuccess(failedPlaceIds = ["c"])`。当前 `b26bda6` 的通用 `catch (_: Exception)` 会使该测试 RED，这正是裁决要求暴露的生产缺口。

- [ ] **Step 3: 运行错误分类 cases**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.AddPlacesRoomIntegrationTest
```

预期：类型化可恢复错误 case PASS；未知基础设施异常传播 case PASS。任何把 `SQLiteConstraintException` 降级成 `PartialSuccess` 的实现都必须失败。

---

### Task 3: 模拟批次中途目标日删除并验证完整请求恢复

**Files:**
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt`

**Interfaces:**
- Consumes: `TargetDayNotFoundException`；`AddPlacesOutcome.TargetDayMissing(retainedPlaceIds, createdItemIds)`；真实 `TripDao.deleteDayRow(dayId)`。
- Produces: 可重复的 mid-batch race，通过代理 repository 在第一次成功后删除真实 Room day。

- [ ] **Step 1: 增加只拦截 addItem 的删除代理**

代理不是数据库 fake：除“第一次 add 成功后删除目标日”外，所有调用均委托真实 `RoomItineraryRepository`，实际 Item、级联删除与异常均由 Room 产生。

```kotlin
private class DeleteTargetDayAfterFirstAddRepository(
    private val delegate: ItineraryRepository,
    private val deleteDay: suspend () -> Unit,
) : ItineraryRepository by delegate {
    private var successfulAdds = 0

    override suspend fun addItem(dayId: String, savedPlaceId: String, targetIndex: Int): String {
        val itemId = delegate.addItem(dayId, savedPlaceId, targetIndex)
        successfulAdds += 1
        if (successfulAdds == 1) deleteDay()
        return itemId
    }
}
```

- [ ] **Step 2: 写中途删除 case**

```kotlin
@Test
fun targetDayDeletedMidBatchRestoresCompleteRequestAndStops() = runTest {
    seedTrip("trip", "target", "other")
    listOf("first", "second", "third").forEach { seedPlace(it) }
    val deletingRepository = DeleteTargetDayAfterFirstAddRepository(repository) {
        database.tripDao().deleteDayRow("target")
    }

    val outcome = AddPlacesToDayUseCase(deletingRepository)(
        AddPlacesRequest("trip", "target", listOf("first", "second", "third")),
    )

    val missing = assertIs<AddPlacesOutcome.TargetDayMissing>(outcome)
    assertEquals(listOf("first", "second", "third"), missing.retainedPlaceIds)
    assertEquals(listOf("item-0"), missing.createdItemIds)
    assertTrue(database.itineraryEditingDao().items("target").isEmpty())
    assertTrue(database.routeLegDao().legs("target").isEmpty())
    assertTrue(database.itineraryEditingDao().items("other").isEmpty())
}
```

预期：第一项的真实 ID 仍用于精确 undo/结果追踪；由于删除 day 的外键级联，数据库不再保留该 Item；UI 恢复的是完整 `[first, second, third]`，不是 `drop(index)` 后的 `[second, third]`；没有改投 `other`。当前 `b26bda6` 返回剩余后缀，因此该 case 应先 RED，直至生产修复。

- [ ] **Step 3: 运行中途失效 case**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.AddPlacesRoomIntegrationTest#targetDayDeletedMidBatchRestoresCompleteRequestAndStops
```

预期：修复后的实现 PASS；`retainedPlaceIds = request.savedPlaceIds.drop(index)` 必须 FAIL。

---

### Task 4: 验证 Undo 保留 SavedPlace 与历史 occurrence

**Files:**
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt`

**Interfaces:**
- Consumes: `UndoAddedItemsUseCase.invoke(UndoAddedItemsRequest)`；`RoomItineraryRepository.deleteItem`。
- Produces: 只撤销本批 Item ID 的持久化证明；SavedPlace note 和历史同地点 Item 保留；RouteLeg 修复正确。

- [ ] **Step 1: 写历史 occurrence + 本批重复 occurrence fixture**

```kotlin
@Test
fun undoDeletesOnlyCreatedOccurrencesAndPreservesSavedPlaceAndHistory() = runTest {
    seedTrip("trip", "day")
    seedPlace("hotel", note = "keep-note")
    seedPlace("museum")
    val historicalHotel = repository.addItem("day", "hotel", 0)
    val historicalMuseum = repository.addItem("day", "museum", 1)

    val outcome = assertIs<AddPlacesOutcome.Success>(
        addPlaces(AddPlacesRequest("trip", "day", listOf("hotel", "hotel"))),
    )
    undoAddedItems(UndoAddedItemsRequest(outcome.createdItemIds))

    val items = database.itineraryEditingDao().items("day")
    assertEquals(listOf(historicalHotel, historicalMuseum), items.map { it.id })
    assertEquals(listOf("hotel", "museum"), items.map { it.savedPlaceId })
    assertEquals(listOf(0L, 1_000L), items.map { it.position })
    assertEquals("keep-note", database.savedPlaceDao().place("hotel")?.note)
    assertEquals(
        listOf(historicalHotel to historicalMuseum),
        database.routeLegDao().legs("day").map { it.fromItemId to it.toItemId },
    )
    outcome.createdItemIds.forEach { id ->
        assertNull(database.itineraryEditingDao().item(id))
    }
}
```

预期：本批两个 `hotel` occurrence 均删除；旧 `hotel` 和 `museum` Item、SavedPlace 与备注保留；旧相邻关系恢复为唯一 `historicalHotel -> historicalMuseum` leg。该断言能阻止 Undo 误用 `removePlaceOccurrences(placeId)` 或删除 SavedPlace。

- [ ] **Step 2: 运行 Undo case**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.AddPlacesRoomIntegrationTest#undoDeletesOnlyCreatedOccurrencesAndPreservesSavedPlaceAndHistory
```

预期：PASS；真实 Room 外键和日内 leg 同步均保持一致。

---

### Task 5: 验证跨天绝无 RouteLeg

**Files:**
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt`

**Interfaces:**
- Consumes: 两次 `AddPlacesToDayUseCase`，目标分别为 `day-1` 和 `day-2`。
- Produces: DAO 级别的 RouteLeg day/endpoint 一致性断言。

- [ ] **Step 1: 写两天批量追加测试**

```kotlin
@Test
fun batchesOnDifferentDaysNeverCreateCrossDayRouteLegs() = runTest {
    seedTrip("trip", "day-1", "day-2")
    listOf("a", "b", "c", "d").forEach { seedPlace(it) }
    addPlaces(AddPlacesRequest("trip", "day-1", listOf("a", "b")))
    addPlaces(AddPlacesRequest("trip", "day-2", listOf("c", "d")))

    val day1Items = database.itineraryEditingDao().items("day-1")
    val day2Items = database.itineraryEditingDao().items("day-2")
    val day1Ids = day1Items.mapTo(mutableSetOf()) { it.id }
    val day2Ids = day2Items.mapTo(mutableSetOf()) { it.id }
    val day1Legs = database.routeLegDao().legs("day-1")
    val day2Legs = database.routeLegDao().legs("day-2")

    assertEquals(listOf(day1Items[0].id to day1Items[1].id), day1Legs.map { it.fromItemId to it.toItemId })
    assertEquals(listOf(day2Items[0].id to day2Items[1].id), day2Legs.map { it.fromItemId to it.toItemId })
    assertTrue(day1Legs.all { it.fromItemId in day1Ids && it.toItemId in day1Ids })
    assertTrue(day2Legs.all { it.fromItemId in day2Ids && it.toItemId in day2Ids })
    assertTrue(day1Legs.none { it.fromItemId in day2Ids || it.toItemId in day2Ids })
    assertTrue(day2Legs.none { it.fromItemId in day1Ids || it.toItemId in day1Ids })
}
```

预期：每一天各有一条日内边，总共两条；不存在 `day-1` 尾项到 `day-2` 首项的边。`RouteLegEntity` 的 `(itemId, tripDayId)` 外键也为该契约提供数据库级保护。

- [ ] **Step 2: 运行跨天 case**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.AddPlacesRoomIntegrationTest#batchesOnDifferentDaysNeverCreateCrossDayRouteLegs
```

预期：PASS；两天的 Item/Leg 集合完全隔离。

---

### Task 6: 完整验证与提交

**Files:**
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt`

**Interfaces:**
- Consumes: 前五项的全部 fixture 与 cases。
- Produces: 单类 connected 测试结果和可审查提交。

- [ ] **Step 1: 运行 Task 6 JVM 回归**

```bash
./gradlew testDebugUnitTest \
  --tests "com.yangchengwei.easytrip.itinerary.domain.AddPlacesToDayUseCaseTest" \
  --tests "com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsUseCaseTest"
```

预期：PASS。若裁决已经要求更新原有 JVM 期望，则由生产实现任务先更新；本 Room 测试任务不得修改它们。

- [ ] **Step 2: 运行完整 Room integration class**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.AddPlacesRoomIntegrationTest
```

预期：全部 PASS；至少覆盖：重复 place、末尾追加、类型化单地点恢复、未知 RouteLeg 基础设施异常传播、中途目标日删除完整恢复、Undo 保留 SavedPlace/历史 occurrence、跨天无 leg。

- [ ] **Step 3: 若有设备，再运行邻近 Room 回归**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.data.ItineraryTransactionTest
```

预期：PASS，确认 Task 6 的测试前置和故障注入没有误解既有 Room 事务语义。

- [ ] **Step 4: 检查只增加目标测试文件**

```bash
git status --short
git diff -- app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt
```

预期：除目标测试文件外无生产/测试修改；不要提交 Gradle 产物、数据库文件或设备截图。

- [ ] **Step 5: 提交 Room integration tests**

```bash
git add app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt
git commit -m "Add Task 6 Room integration coverage"
```

## Expected RED Findings Against `b26bda6`

1. `targetDayDeletedMidBatchRestoresCompleteRequestAndStops` 应指出当前实现只返回 `request.savedPlaceIds.drop(index)`，违反“完整恢复请求”裁决。
2. `unknownRouteLegInsertFailurePropagatesAndRollsBackCurrentPlace` 应指出当前 `catch (_: Exception)` 把 `SQLiteConstraintException` 误报为 `PartialSuccess`；未知基础设施异常必须传播。
3. `recoverableSinglePlaceFailureContinuesWithoutPositionGap` 要求生产侧存在明确的类型化单地点可恢复错误；不能继续依赖所有 `Exception` 都可恢复。
4. 其余真实 Room cases 应固定当前可复用行为：重复 occurrence、真实 created IDs、末尾追加、Undo 仅删本批 Item、SavedPlace/历史 occurrence 保留、RouteLeg 日内相邻且跨天隔离。

## Self-Review

- Spec coverage：计划 Task 6 要求的重复 place、末尾追加、部分成功、目标日失效、撤销保留 SavedPlace、跨天无 RouteLeg 均有独立真实 Room case；额外覆盖审查裁决的基础设施异常传播和完整 retained 请求。
- Placeholder scan：没有把“适当错误处理”留给执行者；唯一需从最终生产实现同步的是类型化可恢复地点错误的符号名，其行为与禁止项已精确定义。
- Type consistency：全部业务 ID 为 `String`；DAO 名称与当前 `ItineraryDao` / `RouteLegDao` 一致；中途删除调用现有 `TripDao.deleteDayRow(String)`；测试使用 `TargetDayMissing(retainedPlaceIds, createdItemIds)` 的 `b26bda6` 形状。
