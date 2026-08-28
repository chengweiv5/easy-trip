# Easy Trip 旅行设置与日期正确性收敛实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 加固现有旅行设置日期流程，使未提交草稿、影响预览、单次写入、Room 事实确认、同步恢复和设置导航在并发条件下保持一致。

**Architecture:** 保留现有 `TripSettingsRoute` / `TripSettingsContent` / `TripSettingsViewModel` / `TripDateRangeService` / `RoomTripRepository` 边界。ViewModel 分离 Room 基线与用户结束日期草稿，以带身份的不可变请求驱动 preview、confirmation、apply 和 Room 确认；Room 事务继续作为写入与乐观并发校验权威。

**Tech Stack:** Kotlin、Android ViewModel、StateFlow、Coroutines、Jetpack Compose、Navigation Compose、Room、JUnit4、kotlinx-coroutines-test、AndroidX Compose UI Test、Android Instrumentation。

**Spec:** `docs/superpowers/specs/2026-08-27-easy-trip-trip-settings-date-correctness-convergence-design.md`

## Global Constraints

- 开始日期在本批固定；设置页只允许修改已有日期旅行的结束日期。
- 无日期旅行保持无日期；本批不增加从无日期切换为有日期或反向切换的入口。
- 增长只从尾部追加连续旅行日，缩短只从尾部删除旅行日。
- 缩短日期和删除单日必须先展示精确影响，再由 Room 事务复核。
- SavedPlace 在日期缩短和旅行日删除中必须保留。
- 同一请求最多执行一次 `applyDateRange`；同步恢复不得重复写入。
- 用户于 2026-08-28 选择统一 30 天上限；创建、日期预览与 Room 写入共享 `MAX_TRIP_DAYS = 30`，31 天及极端日期必须在昂贵查询/写入前拒绝。
- Room 完成采用最新事实语义；每条 eligible fresh emission 都更新匹配状态，历史 match 不得在后续 fresh mismatch 后继续生效。
- “更多”继续直接进入当前旅行设置，不新增组合菜单。
- 不修改地图服务授权、定位权限、Sheet 锚点或其他页面族。
- 设备门禁仅因功能/状态错误、数据不一致、崩溃、流程不可达、严重裁切或关键交互失效而阻断。
- connected instrumentation 全局互斥并串行执行；基础设施失败最多重试一次。
- 每个代码任务完成后运行 `graphify update .`。
- 未获得明确授权前不执行 `git commit` 或 `git push`；各任务只保留可审查工作区 checkpoint。

---

## 文件结构

### 新增文件

- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/DateRangeChangeRequest.kt`
  - 定义一次日期变更的不可变请求、阶段和提交身份。
- `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripSettingsNavigationTest.kt`
  - 使用真实 `AppNavigation` 验证工作台设置入口和返回上下文。
- `docs/testing/trip-settings-date-correctness-acceptance.md`
  - 记录 RED/GREEN、候选门禁和 Mate 60 Pro 验收。

### 修改文件

- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/DateRangeChangeUiState.kt`
  - 保存 baseline、draft、dirty、阶段、确认影响和错误。
- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModel.kt`
  - 负责请求 generation、单飞、Room 确认和只读同步恢复。
- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsRoute.kt`
  - 消费一次性导航 effect，并将 busy 状态纳入返回处理。
- `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt`
  - 开始日期只读；结束日期为唯一日期输入；使用共享确认层并锁定 busy 交互。
- `app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripDateRangeService.kt`
  - 以不可变请求生成影响，并把完整预览快照提交给仓储。
- `app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt`
  - 保持 `String` ID；`DateRangeApply` 增加基线开始日期校验字段。
- `app/src/main/java/com/yangchengwei/easytrip/trip/data/RoomTripRepository.kt`
  - 在事务内核对基线开始日期、日 ID、尾部集合和影响计数。
- `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
  - 仅在真实导航测试暴露缺陷时修正设置返回路径；不重构路由。
- `app/src/test/java/com/yangchengwei/easytrip/trip/domain/TripDateRangeServiceTest.kt`
  - 覆盖请求快照、固定开始日期和过期基线。
- `app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModelTest.kt`
  - 覆盖 dirty、generation、apply/Room 双阶段和同步恢复。
- `app/src/androidTest/java/com/yangchengwei/easytrip/trip/data/TripDateRangeRoomTest.kt`
  - 覆盖乐观并发、级联清理、保留收藏和连续编号。
- `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContentTest.kt`
  - 覆盖只读开始日期、结构化影响和 busy 锁定。
- `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioExecutable.kt`
  - 迁移设置场景到稳定语义节点，不改变 47 个场景数量。

### 最终修复补充（2026-08-28）

- TDD 增加真实 ViewModel 竞态：apply 挂起，matching emission 后跟 fresh wrong-ID mismatch，最后 service 返回；预期保持 dirty/等待同步。
- TDD 增加创建 30/31 天边界，`TripDateRangeService` 30/31/`9999-12-31` 边界与“拒绝前不调用 deletionCounts/apply”，以及 Room create/apply 31 天防守测试。
- 最小实现：`roomConfirmed` 对每条 eligible fresh emission 赋值为当前 `matches`；新增领域常量并由 Validator、TripService、TripDateRangeService、RoomTripRepository 共用。
- 设置页沿用既有错误展示，不新增选择器控件；增加 focused Content 测试证明明确中文错误可见。
- 本轮修改使此前完整门禁和物理真机结果 stale；仅运行用户指定的定向验证，完整门禁由 controller 后续统一执行。
- Boundary follow-up：`TripService.insertDay/appendTripDay` 在调用 repository 前读取当前旅行并拒绝 30→31；Room 插入事务在 park/insert/touch 前再次拒绝，29→30 保持成功。
- 将误命名的“same-size”增长测试改为真实三日→三日 matching seam 用例，并增加缩短 retained-ID wrong/reordered 的公开流程反例，保证 growth/shrink/same-size 三分支可靠覆盖。
- Date representability residual：以领域 helper 验证 dated create/insert/setStartDate 的 `startDate + dayCount - 1` 可表示；Validator 显示明确错误，Service 与 Room 在写前防守。Room setStartDate 在单事务读取 trip/day count 后校验。测试覆盖 `LocalDate.MAX` + 1 日通过、+ 2 日拒绝且 Room 不变。

---

### Task 1：锁定日期草稿与阶段状态的 RED 基线

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/DateRangeChangeRequest.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/DateRangeChangeUiState.kt:1-12`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModelTest.kt:22-261`

**Interfaces:**
- Consumes: `TripWithDays(id, name, startDate, travelMode, days)`、现有 `TripRepository.observeTrip(tripId)`。
- Produces:

```kotlin
data class DateRangeChangeRequest(
    val generation: Long,
    val tripId: String,
    val baselineStartDate: LocalDate,
    val baselineDayIds: List<String>,
    val targetEndDate: LocalDate,
)

sealed interface DateRangeChangePhase {
    data object Idle : DateRangeChangePhase
    data class Previewing(val request: DateRangeChangeRequest) : DateRangeChangePhase
    data class AwaitingConfirmation(
        val request: DateRangeChangeRequest,
        val impact: DateRangeChangeImpact,
    ) : DateRangeChangePhase
    data class Applying(
        val request: DateRangeChangeRequest,
        val impact: DateRangeChangeImpact,
    ) : DateRangeChangePhase
    data class AwaitingRoom(
        val request: DateRangeChangeRequest,
        val impact: DateRangeChangeImpact,
    ) : DateRangeChangePhase
    data class SyncFailed(
        val request: DateRangeChangeRequest,
        val impact: DateRangeChangeImpact,
    ) : DateRangeChangePhase
}
```

`DateRangeChangeUiState` 的目标签名：

```kotlin
data class DateRangeChangeUiState(
    val startDate: LocalDate? = null,
    val baselineEndDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isDirty: Boolean = false,
    val phase: DateRangeChangePhase = DateRangeChangePhase.Idle,
    val error: String? = null,
) {
    val confirmation: DateRangeChangeImpact?
        get() = when (val value = phase) {
            is DateRangeChangePhase.AwaitingConfirmation -> value.impact
            is DateRangeChangePhase.Applying -> value.impact
            is DateRangeChangePhase.AwaitingRoom -> value.impact
            is DateRangeChangePhase.SyncFailed -> value.impact
            else -> null
        }

    val submitting: Boolean
        get() = phase is DateRangeChangePhase.Applying || phase is DateRangeChangePhase.AwaitingRoom
}
```

- [ ] **Step 1：先为当前测试补齐精确 fake 控制点**

在 `FakeRepository` 增加：

```kotlin
var lastApply: DateRangeApply? = null
var autoEmitApply = true
var observeFailure: Throwable? = null

fun emit(value: TripWithDays?) {
    trip.value = value
}

override suspend fun applyDateRange(command: DateRangeApply) {
    applyCalls++
    lastApply = command
    applyBlock?.await()
    applyFailure?.let { throw it }
    if (autoEmitApply) {
        val current = requireNotNull(trip.value)
        trip.value = current.copy(
            startDate = command.startDate,
            days = current.days.take(command.dayCount),
        )
    }
}
```

若需要模拟 Flow 失败，把 fake 改为可替换的 `MutableSharedFlow`/`flow { ... }`，但保持测试只通过 `TripRepository` 契约驱动，不调用 ViewModel 私有方法。

- [ ] **Step 2：写 dirty 草稿 RED 测试**

新增：

```kotlin
@Test fun roomEmissionDoesNotOverwriteDirtyEndDateDraft() = runTest(dispatcher) {
    val repository = FakeRepository()
    val model = model(repository)
    advanceUntilIdle()

    model.updateDateDraft(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-05"))
    repository.emit(repository.currentTrip().copy(name = "Renamed"))
    advanceUntilIdle()

    assertEquals(LocalDate.parse("2026-10-05"), model.state.value.dateRange.endDate)
    assertEquals(true, model.state.value.dateRange.isDirty)
    assertEquals("Renamed", model.state.value.name)
}
```

同时新增未 dirty 对照：Room 改变日数时 `baselineEndDate` 与 `endDate` 一起更新。

- [ ] **Step 3：写提交锁定与 Room 确认 RED 测试**

新增：

```kotlin
@Test fun applyReturnWaitsForMatchingRoomEmissionAndWritesOnce() = runTest(dispatcher) {
    val repository = FakeRepository().apply { autoEmitApply = false }
    val model = model(repository)
    advanceUntilIdle()

    model.updateDateDraft(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-04"))
    model.requestDateRangeChange()
    advanceUntilIdle()

    assertEquals(1, repository.applyCalls)
    assertEquals(true, model.state.value.dateRange.submitting)

    model.requestDateRangeChange()
    model.confirmDateRangeChange()
    advanceUntilIdle()
    assertEquals(1, repository.applyCalls)

    repository.emit(repository.tripWithDayCount(4))
    advanceUntilIdle()
    assertEquals(false, model.state.value.dateRange.submitting)
    assertEquals(false, model.state.value.dateRange.isDirty)
}
```

再增加“确认前旧 emission 不完成请求”和“新编辑不能改变 Applying 请求快照”测试。

- [ ] **Step 4：运行 RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.trip.ui.TripSettingsViewModelTest'
```

Expected: 新增 dirty/phase/Room 确认测试失败；失败原因必须指向当前 Room emission 覆盖草稿或 apply 返回立即结束 busy，不能是测试编译错误。

- [ ] **Step 5：实现状态类型，暂不改 ViewModel 行为**

创建上述 `DateRangeChangeRequest`、`DateRangeChangePhase`，并把 `DateRangeChangeUiState` 迁移到目标签名。仅做使生产和既有测试可编译的机械适配；不要在本步骤让新增行为测试转绿。

- [ ] **Step 6：运行编译和 RED 测试**

Run:

```bash
./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.trip.ui.TripSettingsViewModelTest'
```

Expected: 编译通过；新增行为测试仍以预期断言失败。

- [ ] **Step 7：更新图谱并记录 checkpoint**

```bash
graphify update .
git status --short
```

不提交。记录 Task 1 修改文件和 RED 失败名称，交给 reviewer 确认测试确实覆盖缺口。

---

### Task 2：实现 baseline、dirty 和请求单飞

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModel.kt:31-185`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 的 `DateRangeChangeRequest`、`DateRangeChangePhase`、`DateRangeChangeUiState`。
- Produces:
  - `fun updateDateEndDraft(endDate: LocalDate?)`
  - `fun requestDateRangeChange()`
  - `fun cancelDateRangeChange()`
  - `fun confirmDateRangeChange()`
  - `fun retryDateRangeSync()`
  - `val effects: Flow<TripSettingsEffect>`，其中：

```kotlin
sealed interface TripSettingsEffect {
    data object ReturnToTripList : TripSettingsEffect
}
```

- [ ] **Step 1：补旅行被删除和 stale completion RED 测试**

新增：

```kotlin
@Test fun deletedTripPublishesReturnToTripList() = runTest(dispatcher) {
    val repository = FakeRepository()
    val model = model(repository)
    advanceUntilIdle()

    repository.emit(null)
    advanceUntilIdle()

    assertEquals(TripSettingsEffect.ReturnToTripList, model.effects.first())
}
```

以及：

```kotlin
@Test fun stalePreviewCannotReplaceNewerDirtyDraft()
@Test fun inputAndBackActionsAreIgnoredWhileApplying()
@Test fun previewFailureKeepsDirtyDraftAndReturnsToIdle()
```

使用 Turbine 仅当项目现有依赖已包含；否则用 `backgroundScope.launch { effects.toList(...) }` 或 Channel 测试，不新增依赖。

- [ ] **Step 2：运行 RED**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.trip.ui.TripSettingsViewModelTest'
```

Expected: effect、dirty 保留和 Applying guard 测试失败。

- [ ] **Step 3：将 `observeTrip` 从 `filterNotNull()` 改为显式处理 null**

核心逻辑：

```kotlin
repository.observeTrip(tripId).collect { trip ->
    if (trip == null) {
        effectsChannel.send(TripSettingsEffect.ReturnToTripList)
        return@collect
    }
    val baselineEnd = trip.startDate?.plusDays((trip.days.size - 1).toLong())
    val current = mutableState.value
    val range = current.dateRange
    val nextRange = if (range.isDirty || range.phase !is DateRangeChangePhase.Idle) {
        range.copy(
            startDate = trip.startDate,
            baselineEndDate = baselineEnd,
        )
    } else {
        range.copy(
            startDate = trip.startDate,
            baselineEndDate = baselineEnd,
            endDate = baselineEnd,
        )
    }
    mutableState.value = current.copy(
        name = trip.name,
        startDate = trip.startDate,
        travelMode = trip.travelMode,
        days = trip.days.map { DayUi(it.id, service.displayLabel(it, trip.startDate)) },
        dateRange = nextRange,
    )
    reconcileDateRangeCompletion(trip)
}
```

`reconcileDateRangeCompletion` 只处理 `AwaitingRoom` / `SyncFailed` 的请求，不把任意 emission 当作完成。

- [ ] **Step 4：将日期编辑限制为结束日期**

实现：

```kotlin
fun updateDateEndDraft(endDate: LocalDate?) {
    val range = mutableState.value.dateRange
    if (range.submitting || range.startDate == null) return
    dateRangeGeneration++
    previewJob?.cancel()
    mutableState.value = mutableState.value.copy(
        dateRange = range.copy(
            endDate = endDate,
            isDirty = endDate != range.baselineEndDate,
            phase = DateRangeChangePhase.Idle,
            error = null,
        ),
    )
}
```

删除对外 `updateDateDraft(startDate, endDate)`；所有调用方和测试改用 `updateDateEndDraft(endDate)`。开始日期永远来自 Room baseline。

- [ ] **Step 5：实现请求创建和 phase guard**

`requestDateRangeChange()` 只在以下条件成立时工作：

```kotlin
val range = state.value.dateRange
if (!range.isDirty || range.startDate == null || range.endDate == null) return
if (range.phase !is DateRangeChangePhase.Idle) return
```

请求使用当前状态快照：

```kotlin
val request = DateRangeChangeRequest(
    generation = ++dateRangeGeneration,
    tripId = tripId,
    baselineStartDate = checkNotNull(range.startDate),
    baselineDayIds = state.value.days.map(DayUi::id),
    targetEndDate = checkNotNull(range.endDate),
)
```

preview 完成后：有删除进入 `AwaitingConfirmation`；无删除直接进入 `Applying` 并调用 Task 3 的 service API。

- [ ] **Step 6：实现确认、取消和重复动作 guard**

- `cancelDateRangeChange()` 只接受 `AwaitingConfirmation`，回到 `Idle`，保留 draft/dirty。
- `confirmDateRangeChange()` 只接受 `AwaitingConfirmation`，切到 `Applying`，同一 request 只调用一次 apply。
- `Applying` / `AwaitingRoom` 期间 `updateDateEndDraft`、request、confirm、cancel 都是 no-op。
- preview/apply 的 `CancellationException` 原样重抛。

- [ ] **Step 7：运行 ViewModel 测试**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.trip.ui.TripSettingsViewModelTest'
```

Expected: Task 1/2 新增测试全部 PASS；既有删除日测试仍 PASS。

- [ ] **Step 8：运行相关 JVM 回归并更新图谱**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.trip.ui.*' --tests 'com.yangchengwei.easytrip.trip.domain.*'
graphify update .
git status --short
```

不提交。记录 Task 2 checkpoint。

---

### Task 3：绑定 preview/apply 快照并强化 Room 事务

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripDateRangeService.kt:7-74`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt:41-53`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/data/RoomTripRepository.kt:79-127`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/trip/domain/TripDateRangeServiceTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/data/TripDateRangeRoomTest.kt:23-92`

**Interfaces:**
- Consumes: `DateRangeChangeRequest`。
- Produces:

```kotlin
data class DateRangeChangeImpact(
    val request: DateRangeChangeRequest,
    val retainedDayIds: List<String>,
    val deletedDayIds: List<String>,
    val deletedItineraryItems: Int,
    val deletedRouteLegs: Int,
    val retainedSavedPlaces: Int,
)

data class DateRangeApply(
    val tripId: String,
    val expectedStartDate: LocalDate,
    val startDate: LocalDate,
    val dayCount: Int,
    val expectedDayIds: List<String>,
    val expectedDeletedDayIds: List<String>,
    val expectedDeletedItineraryItems: Int,
    val expectedDeletedRouteLegs: Int,
)
```

`TripDateRangeService`：

```kotlin
suspend fun preview(request: DateRangeChangeRequest): DateRangeChangeImpact
suspend fun apply(impact: DateRangeChangeImpact)
```

- [ ] **Step 1：写 service 快照 RED 测试**

新增：

```kotlin
@Test fun previewRejectsChangedBaselineDays()
@Test fun previewKeepsFixedStartDateInImpactRequest()
@Test fun applyCopiesFullPreviewSnapshotIntoRepositoryCommand()
```

关键断言：`repository.lastApply.expectedDayIds == request.baselineDayIds`，并且 `startDate == expectedStartDate == request.baselineStartDate`。

- [ ] **Step 2：写 Room 乐观并发 RED 测试**

在 `TripDateRangeRoomTest` 新增：

```kotlin
@Test fun shrinkRejectsWhenDayIdsChangedAfterPreviewWithoutPartialWrite()
@Test fun shrinkRejectsWhenItemOrLegCountsChangedAfterPreviewWithoutPartialWrite()
@Test fun shrinkRejectsWhenStartDateChangedAfterPreviewWithoutPartialWrite()
```

每个测试：

1. 创建 3 日旅行；
2. 构造带完整 expected 字段的 `DateRangeApply`；
3. 在 apply 前改变一个基线条件；
4. 断言抛出 `IllegalArgumentException`；
5. 断言旅行开始日期、旅行日、行程项和 RouteLeg 均未发生部分写入。

- [ ] **Step 3：运行 RED**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.trip.domain.TripDateRangeServiceTest'
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.data.TripDateRangeRoomTest
```

Expected: service 签名/快照测试和 Room 开始日期基线测试失败。connected 测试必须单独运行，确认无其他 instrumentation 任务。

- [ ] **Step 4：实现 `preview(request)`**

实现顺序：

```kotlin
val trip = requireNotNull(repository.observeTrip(request.tripId).first())
require(trip.startDate == request.baselineStartDate) { "Trip start date changed after edit began" }
require(trip.days.map(TripDay::id) == request.baselineDayIds) { "Trip days changed after edit began" }
val dayCount = validateAndCount(request.baselineStartDate, request.targetEndDate)
val retained = trip.days.take(dayCount).map(TripDay::id)
val deleted = trip.days.drop(dayCount).map(TripDay::id)
val counts = repository.dateRangeDeletionCounts(request.tripId, deleted)
return DateRangeChangeImpact(request, retained, deleted, counts.itineraryItems, counts.routeLegs, counts.retainedSavedPlaces)
```

`validateAndCount` 本批只接受非空 `LocalDate`，移除双 null 日期切换分支。

- [ ] **Step 5：实现 `apply(impact)` 和 Room 开始日期复核**

`TripDateRangeService.apply` 生成完整 `DateRangeApply`。Room 事务在任何写入前增加：

```kotlin
val trip = requireNotNull(dao.trip(command.tripId)) { "Unknown trip: ${command.tripId}" }
require(trip.startDate == command.expectedStartDate) { "Trip start date changed after preview" }
require(command.startDate == command.expectedStartDate) { "Trip start date cannot change in this flow" }
```

然后执行现有日 ID、尾部集合、行程项和 RouteLeg 计数核对。所有 `require` 必须位于 `dao.setStartDate` 和删增日之前。

- [ ] **Step 6：补成功级联与连续编号集成测试**

新增：

```kotlin
@Test fun shrinkCascadesItemsAndLegsKeepsSavedPlacesAndRenumbersDays()
@Test fun dayDeleteCascadesItemsAndLegsKeepsSavedPlacesAndRenumbersDays()
```

使用真实 Room repository 创建：

- 三个旅行日；
- 被删除日上的 SavedPlace、ItineraryItem 和 RouteLeg；
- 保留日上的对照内容。

断言：删除目标内容消失、保留内容存在、SavedPlace 数量不变、剩余 `TripDay.index` 为 `0..n-1`，并且 day ID 前缀保持不变。

- [ ] **Step 7：运行 service 与 Room 测试**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.trip.domain.TripDateRangeServiceTest'
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.data.TripDateRangeRoomTest
```

Expected: 全部 PASS，0 skipped。

- [ ] **Step 8：运行相关仓储回归并更新图谱**

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.data.RoomTripRepositoryTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.CascadeDeleteTest
graphify update .
git status --short
```

Expected: 两类测试串行 PASS。不提交，记录 Task 3 checkpoint。

---

### Task 4：实现一次写入、Room 确认和同步恢复

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/DateRangeChangeUiState.kt`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModelTest.kt`

**Interfaces:**
- Consumes: Task 3 的 `TripDateRangeService.preview(request)`、`apply(impact)`。
- Produces: `retryDateRangeSync()`，只重新建立 `observeTrip` 确认，不调用 `apply`。

- [ ] **Step 1：写完成顺序 RED 测试**

新增两种顺序：

```kotlin
@Test fun roomTargetBeforeServiceReturnCompletesWhenServiceReturns()
@Test fun serviceReturnBeforeRoomTargetWaitsUntilMatchingEmission()
```

共同断言：

- busy 在两条件满足前不结束；
- `applyCalls == 1`；
- 成功后 `isDirty == false`；
- baseline 和 draft 都等于目标结束日期。

- [ ] **Step 2：写同步失败恢复 RED 测试**

新增：

```kotlin
@Test fun exhaustedRoomConfirmationShowsRetryWithoutApplyingAgain()
@Test fun repeatedRetryWhileSyncingStartsOneCollector()
@Test fun staleCollectorCannotCompleteNewGeneration()
```

`retryDateRangeSync()` 后模拟目标 Room emission，断言 `applyCalls` 始终为 1。

- [ ] **Step 3：运行 RED**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.trip.ui.TripSettingsViewModelTest'
```

Expected: 新增顺序和同步恢复测试失败。

- [ ] **Step 4：实现两条件完成协调**

为当前 active request 维护内存状态：

```kotlin
private data class DateRangeCommitProgress(
    val requestGeneration: Long,
    val serviceCompleted: Boolean = false,
    val roomConfirmed: Boolean = false,
)
```

规则：

- 进入 `Applying` 时建立 progress；
- service 成功设置 `serviceCompleted=true` 并进入 `AwaitingRoom`；
- Room emission 匹配请求目标时设置 `roomConfirmed=true`；
- 两者都为 true 才调用 `completeDateRangeChange(request)`；
- 任一事件顺序都必须工作；
- 只有 `activeRequest.generation` 能更新 progress。

Room 匹配条件：

```kotlin
private fun TripWithDays.matches(request: DateRangeChangeRequest): Boolean =
    startDate == request.baselineStartDate &&
        days.size == ChronoUnit.DAYS.between(request.baselineStartDate, request.targetEndDate).toInt() + 1
```

不要仅凭日期相等判断，因为确认前的旧 emission 可能与目标日期偶然相同；测试必须先建立 request，再只接受 request 启动后的 collector generation。

- [ ] **Step 5：实现同步失败和重试**

- `observeTrip` collector 异常时，若 active write 已成功，进入 `SyncFailed`。
- `retryDateRangeSync()` 只启动一个新 collector generation。
- retry 期间重复点击无效。
- 新 collector 收到匹配 trip 后完成请求。
- retry 不调用 `TripDateRangeService.apply`。
- collector 的 `CancellationException` 原样重抛。

若当前 `StateFlow` 收集结构不便于可重启，将其抽成：

```kotlin
private var tripObservationJob: Job? = null
private var tripObservationGeneration = 0L
private fun startTripObservation()
```

`init` 只调用 `startTripObservation()`；不要增加第二个并行 collector。

- [ ] **Step 6：实现失败分类**

- `IllegalArgumentException` 且消息表示快照变化：回到 `Idle`，保留 draft/dirty，错误为“旅行内容已变化，请重新确认”。
- apply 明确失败且 Room 未匹配：回到 `Idle`，保留 draft/dirty，错误为“保存失败，请重新检查影响”。
- apply 结果未知：先重新读取 Room；匹配则完成，不匹配才允许重新 preview。

不要依赖异常文案做长期类型判断；若当前 repository 只有 `IllegalArgumentException`，在本任务新增领域异常：

```kotlin
class DateRangeSnapshotChangedException(message: String) : IllegalStateException(message)
```

Room 的快照 `require` 改为显式抛出该类型；输入校验继续使用 `IllegalArgumentException`。

- [ ] **Step 7：运行 ViewModel 和 service 测试**

```bash
./gradlew :app:testDebugUnitTest --tests 'com.yangchengwei.easytrip.trip.ui.TripSettingsViewModelTest' --tests 'com.yangchengwei.easytrip.trip.domain.TripDateRangeServiceTest'
```

Expected: 全部 PASS，`applyCalls` 恢复前后均为 1。

- [ ] **Step 8：更新图谱并记录 checkpoint**

```bash
graphify update .
git status --short
```

不提交。记录 Task 4 状态机和恢复证据。

---

### Task 5：收敛设置 UI、危险确认和真实导航

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsRoute.kt:7-24`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt:39-148`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt:139-252,366-369` only if RED exposes a defect
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContentTest.kt:1-104`
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripSettingsNavigationTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioExecutable.kt:940-958`

**Interfaces:**
- Consumes: Task 2/4 的 `updateDateEndDraft`、`retryDateRangeSync`、`TripSettingsEffect` 和 `DateRangeChangePhase`。
- Produces: 稳定测试标签：
  - `settings-start-date`
  - `settings-end-date`
  - `settings-apply-date-range`
  - `settings-date-impact-days`
  - `settings-date-impact-items`
  - `settings-date-impact-legs`
  - `settings-date-impact-saved-places`
  - `settings-date-sync-retry`
  - `settings-back`

- [ ] **Step 1：先修复测试自身编译问题**

在 `TripSettingsContentTest` 增加：

```kotlin
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
```

把依赖整句文案的缩短确认断言改为结构化 tag + 数值断言；此步骤不改变生产代码。

- [ ] **Step 2：写 UI RED 测试**

新增：

```kotlin
@Test fun startDateIsReadOnlyAndOnlyEndDateDispatchesDraft()
@Test fun applyingLocksDateInputBackAndDialogDismiss()
@Test fun shrinkConfirmationExposesStructuredImpactCounts()
@Test fun syncFailureOffersExplicitRetry()
@Test fun undatedTripHasNoDateMutationAction()
```

`startDateIsReadOnly...` 断言开始日期节点无 `SetText` action，结束日期存在输入 action。`applyingLocks...` 断言日期按钮禁用、`settings-back` 不触发 callback、确认层 Back/外部点击不关闭。

- [ ] **Step 3：运行 UI RED**

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripSettingsContentTest
```

Expected: 新增只读、busy 和结构化 tag 测试失败；不是导入或测试装配错误。

- [ ] **Step 4：实现只读开始日期和唯一结束日期输入**

- 将开始日期 `OutlinedTextField` 替换为只读信息容器；保留 `settings-start-date` tag。
- 结束日期继续使用 `OutlinedTextField`，回调只传 `LocalDate?` 给 `onDateEndDraft`。
- 删除“无日期”动作。
- `startDate == null` 时显示“未设置日期”，隐藏或禁用结束日期修改与应用按钮。
- `DateRangeChangePhase.Applying` / `AwaitingRoom` 时禁用日期输入、应用按钮、旅行日删除和顶部返回。

`TripSettingsContent` 参数改为：

```kotlin
onDateEndDraft: (LocalDate?) -> Unit,
onRetryDateRangeSync: () -> Unit,
```

不再接受 `(LocalDate?, LocalDate?) -> Unit`。

- [ ] **Step 5：使用共享 `ConfirmationDialog` 渲染日期影响**

构造：

```kotlin
ConfirmationUiModel(
    title = "确认修改日期范围？",
    message = "缩短日期会删除超出范围的旅行内容。",
    deletedItems = listOf(
        "${impact.deletedDayIds.size} 个尾部旅行日",
        "${impact.deletedItineraryItems} 个行程项",
        "${impact.deletedRouteLegs} 个路线段",
    ),
    retainedItems = listOf("${impact.retainedSavedPlaces} 个收藏地点"),
    confirmLabel = "确认修改",
    dismissLabel = "取消",
    destructive = true,
    reversible = false,
)
```

为每项添加父级或节点 test tag；若共享组件当前不支持 item tag，给 `ConfirmationSection` 增加可选 `tagPrefix`，默认 null，确保其他调用不变。busy 使用现有 `ConfirmationDialog(busy = true)` 锁定 Back、外部点击和按钮。

- [ ] **Step 6：Route 消费 trip 删除 effect 并保护返回**

`TripSettingsRoute`：

- 使用 `rememberUpdatedState(onBack)`；
- `LaunchedEffect(viewModel)` 收集 `effects`；
- 收到 `ReturnToTripList` 调用最新 `onBack`；
- 普通顶部返回仅在日期和单日删除均不 busy 时调用；
- `SyncFailed` 显示 `settings-date-sync-retry`，触发 `retryDateRangeSync()`。

- [ ] **Step 7：写真实导航 RED 测试**

`TripSettingsNavigationTest` 使用 in-memory Room 和真实 `AppNavigation`：

```kotlin
@Test fun workspaceMoreOpensCurrentTripSettingsAndBackReturnsToSameWorkspace()
@Test fun deletedTripWhileSettingsOpenReturnsToTripList()
```

第一条：

1. 创建 trip；
2. 点击 `trip-$tripId`；
3. 选择一个稳定工作台 section；
4. 点击 `workspace-more`；
5. 断言 `settings-start-date` 显示且 observer 收到 `trips/$tripId/settings`；
6. 点击 `settings-back`；
7. 断言回到同一 `tripId` 的工作台，section 和 Room 内容仍存在。

第二条在设置页通过 repository 删除 trip，等待旅行列表节点出现。

- [ ] **Step 8：运行 UI 与导航测试**

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripSettingsContentTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripSettingsNavigationTest
```

Expected: 两类测试串行 PASS，0 skipped。

- [ ] **Step 9：迁移场景目录但保持 47 条**

更新 `settingsScenario` 调用签名和设置场景 selector。运行：

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest
```

Expected: `47 tests, 0 failures, 0 skipped`。

- [ ] **Step 10：更新图谱并记录 checkpoint**

```bash
graphify update .
git status --short
```

不提交。记录 Task 5 UI/导航证据。

---

### Task 6：候选门禁、验收记录和 Mate 60 Pro

**Files:**
- Create: `docs/testing/trip-settings-date-correctness-acceptance.md`
- Modify: `docs/testing/v1-device-checklist.md` only if the existing checklist lacks the approved settings/date paths

**Interfaces:**
- Consumes: Tasks 1–5 的完整实现和测试。
- Produces: 候选门禁结果、基础设施分类、真机验收结论和剩余非阻断项。

- [ ] **Step 1：运行全量 JVM**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: 0 failures。记录实际测试数，不沿用旧记录中的 426。

- [ ] **Step 2：串行运行 focused connected suites**

确认无其他 instrumentation 任务后依次运行：

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripSettingsContentTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripSettingsNavigationTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.data.TripDateRangeRoomTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.CascadeDeleteTest
```

Expected: 每个 suite 0 failures、0 skipped。若 runner crash、signal 9 或设备离线，只允许一次基础设施重试，并按项目规则记录 `INFRA-BLOCKED`。

- [ ] **Step 3：运行 47 场景门禁**

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1FullUiAcceptanceTest
```

Expected: 两个 suite 均为 47/47 PASS，0 skipped。

- [ ] **Step 4：运行静态和构建门禁**

```bash
./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Expected: `BUILD SUCCESSFUL`，lint 无 error。

- [ ] **Step 5：安装到指定物理设备**

先确认设备：

```bash
adb devices -l
```

选择型号 `ALN-AL00` 的 Mate 60 Pro 序列号；若仍为 `FMR0224725012307`：

```bash
ANDROID_SERIAL=FMR0224725012307 ./gradlew :app:installDebug
adb -s FMR0224725012307 shell input keyevent KEYCODE_WAKEUP
adb -s FMR0224725012307 shell wm dismiss-keyguard
adb -s FMR0224725012307 shell am start -W -n com.yangchengwei.easytrip/.MainActivity
```

若序列号变化，使用 `adb devices -l` 中实际的 `ALN-AL00` 序列号，不向模拟器安装后冒充真机结果。

- [ ] **Step 6：执行真机关键路径**

在 Mate 60 Pro 正常竖屏实际点击：

1. 工作台“更多”进入当前旅行设置；
2. 返回工作台，确认仍是原 trip 和原 section；
3. 延长结束日期，确认新增连续旅行日；
4. 缩短结束日期，核对旅行日、行程项、RouteLeg 和 SavedPlace 影响；
5. 取消确认，验证数据不变；
6. 再次提交并确认，验证尾部日期删除、内容级联和重编号；
7. 删除非最后一个旅行日并验证重编号；
8. 验证最后一个旅行日不可删除；
9. 快速重复点击确认，验证只执行一次；
10. 返回工作台，验证地点池和行程仍可用。

截图只作诊断证据。若无法人工制造并发快照失效和 Room 流异常，以自动化测试为权威，不在真机伪造 PASS。

- [ ] **Step 7：复查崩溃和前台状态**

```bash
adb -s FMR0224725012307 shell pidof com.yangchengwei.easytrip
adb -s FMR0224725012307 logcat -d -t 300 AndroidRuntime:E '*:S'
adb -s FMR0224725012307 exec-out screencap -p > /tmp/easytrip-trip-settings-final.png
```

读取 `/tmp/easytrip-trip-settings-final.png`，确认非黑屏、非桌面、无严重裁切。若设备息屏，先唤醒再重做截图，不把黑屏判为应用失败。

- [ ] **Step 8：写验收记录**

`docs/testing/trip-settings-date-correctness-acceptance.md` 必须记录：

- 日期、HEAD、工作区是否 dirty；
- RED 测试名称及预期失败；
- 每条最终命令、测试数、失败数和结果；
- 设备型号、序列号、Android 版本；
- 真机每条路径的 PASS/FAIL/NOT-RUN；
- 基础设施异常与一次重试；
- 剩余非阻断视觉项；
- 不得把未运行的全量 connected 或真机路径写为 PASS。

- [ ] **Step 9：最终更新图谱和检查工作区**

```bash
graphify update .
git diff --check
git status --short
git diff --stat
```

Expected: graphify 更新成功，`git diff --check` 无输出。检查只包含本计划文件和实现范围；不提交。

---

## 串行依赖

```text
Task 1 RED 基线
  → Task 2 ViewModel baseline/dirty/request
  → Task 3 service + Room 快照事务
  → Task 4 apply/Room 完成与同步恢复
  → Task 5 UI + 导航 + 场景迁移
  → Task 6 全门禁 + 真机验收
```

Task 2 与 Task 3 不并行修改接口：Task 2 先固定 UI 请求类型，Task 3 再让领域服务消费它。Task 4 必须在 Room 快照契约稳定后实现完成确认。所有 connected suites 严格串行。

## 审查检查点

每个 Task 完成后进行一次 scoped review，重点分别为：

1. Task 1：RED 是否证明真实缺口，而非测试装配错误。
2. Task 2：Room baseline 与 dirty draft 是否真正分离；旧 completion 是否隔离。
3. Task 3：所有快照检查是否发生在事务首个写操作之前；失败是否无部分写入。
4. Task 4：两种 service/Room 顺序是否都成立；重同步是否绝不重复 apply。
5. Task 5：busy 返回与弹层关闭是否被锁定；导航是否使用真实 `AppNavigation`。
6. Task 6：测试证据是否新鲜，真机和模拟器是否明确区分。

## 终审 Important 修复追加（2026-08-28）

- [x] RED：真实 Room `LocalDate.MAX` 单日快照直接 apply 为 2 日，证明旧实现写入不可表示状态；同时覆盖单日 no-op。
- [x] GREEN：`applyDateRange` 在事务及任何写入前复用 `isTripDateRangeRepresentable`。
- [x] RED：Idle/首次观察普通异常进入独立 page error；retry 建立新 collector，后续 emission 恢复内容并清错；CancellationException 既有测试保留。
- [x] GREEN：Route/Content 提供可达“重试”，retry 只重订阅；active date commit 的 SyncFailed 路径不变。
- [x] RED/GREEN：删除挂起期间重复 `requestDelete` 不再启动 impact；公共入口复用删除写锁。
- [x] 定向运行 ViewModel、trip UI/domain JVM、Content、Room repository、lint、diff check、Graphify update。
- [ ] 完整 JVM、完整 connected、Catalog、Full UI、APK 构建及物理设备验收均因生产代码变化而 stale，待 controller 刷新。
