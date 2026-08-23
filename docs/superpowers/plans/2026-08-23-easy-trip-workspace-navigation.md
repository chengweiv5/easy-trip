# Easy Trip Workspace Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将旅行工作台重构为“地点池 / 行程”两层导航，并在行程中提供左侧范围栏、可编辑单日内容及按天分组的只读全程内容，同时保持地图范围与内容始终一致。

**Architecture:** `TripWorkspaceViewModel` 成为工作台导航的唯一状态源，持有 `WorkspaceSection` 与 `ItineraryScope`，并派生 `MapScope`、`selectedDayId`、地图模型和全程只读模型。生产 UI 由底导航、`ItineraryScopeRail` 和右侧单日/全程内容组成；`DayItineraryViewModel` 继续负责单日编辑，但不再负责日期选择。现有 repository、Room schema、地图 mapper 与 viewport controller 保持不变，仅复用它们的按日 flow 和完整路线坐标。

**Tech Stack:** Kotlin、Java 17、Jetpack Compose、Material 3、Navigation Compose、ViewModel、SavedStateHandle、StateFlow、Room、高德 Android Map SDK、JUnit 4、kotlinx-coroutines-test、Compose UI Test。

**Spec:** `docs/superpowers/specs/2026-08-23-easy-trip-workspace-navigation-design.md`

## Global Constraints

- 不修改 Room entity、DAO schema、数据库版本或 `app/schemas/**`。
- 不改变地点搜索、收藏/取消收藏、地图 POI 卡片、路线规划、失败重试、图层、缩放、顶部栏、悬浮搜索入口和抽屉三态的既有行为。
- 地图下方只显示“地点池 / 行程”；抽屉内部不得重复显示内容 Tab。
- 行程左栏固定 `88dp`，独立纵向滚动；右栏使用剩余宽度并独立滚动。
- 全程只读并按 `TripDay.index` 从第一天到最后一天分组；单日保留添加、排序、跨日移动、时间、删除、交通方式和重试能力。
- `WorkspaceSection` 与 `ItineraryScope` 是唯一可写导航状态；`MapScope` 与 `selectedDayId` 只能派生，禁止保留独立 setter。
- 地图自动视野继续包含当前范围的 marker 和完整 polyline；普通重组、抽屉拖动和列表滚动不得重置地图。
- 采用严格 TDD：每项行为先写测试并观察预期失败，再写最小实现并观察通过。
- instrumentation 只能在 AVD 名称严格等于 `easy_trip_p60pro` 的模拟器上运行；每次执行前必须校验 `adb -s "$ANDROID_SERIAL" emu avd name` 的精确输出，否则停止。
- 不向其他模拟器或物理真机运行 Gradle instrumentation；本轮完成自动化与专用模拟器交互验证后，再由用户决定是否开始真机验收。
- 不把 API key、keystore、密码、token 或设备序列号写入 tracked 文件；不得读取、打印或复制 `local.properties` 中的高德 Key。
- 当前 `app/src/debug/**` 是已批准的临时原型，只作为视觉参考；生产实现验收后必须删除。
- 不 commit、不 push；忽略计划模板中的常规提交步骤，以本约束为准。

---

## File Structure

- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceNavigation.kt` — 纯导航模型、派生规则、旧状态迁移和日期失效回退。
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryUiModels.kt` — 单日与全程共同使用的地点、路线和按日分组 UI 模型及 mapper。
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryScopeRail.kt` — 固定 `88dp` 的范围栏。
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContent.kt` — 只读全程分组时间线。
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WorkspaceItineraryContent.kt` — 左栏与右栏双栏组合。
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt` — 统一状态源、持久化、迁移、全程模型和地图派生。
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt` — 两项底导航及抽屉内容直出。
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt` — 组装地点池、单日和全程内容。
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModel.kt` — 消费外部选日并支持清空，不再拥有日期选择权。
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItinerarySheet.kt` — 删除横向 `DaySelector`，保留编辑时间线与 dialogs。
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryItemRow.kt` — 抽出共享只读卡片内容，编辑 wrapper 保持原交互。
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/RouteLegRow.kt` — 抽出共享路线展示，编辑 wrapper 保持方式和重试入口。
- Delete after production verification: `app/src/debug/java/com/yangchengwei/easytrip/WorkspaceNavigationPreviewActivity.kt` and `app/src/debug/AndroidManifest.xml`.

---

### Task 1: 建立统一导航模型与兼容迁移

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceNavigation.kt`
- Replace: `app/src/test/java/com/yangchengwei/easytrip/workspace/WorkspaceTabRestorationTest.kt`

**Interfaces:**
- Consumes: existing `MapScope`, `TripDay(id, index)` and legacy strings from `workspace.tab`, `workspace.scope`, `workspace.selectedDay`.
- Produces:

```kotlin
enum class WorkspaceSection { PLACE_POOL, ITINERARY }

sealed interface ItineraryScope {
    data object WholeTrip : ItineraryScope
    data class Day(val dayId: String) : ItineraryScope
}

data class RestoredWorkspaceNavigation(
    val section: WorkspaceSection,
    val itineraryScope: ItineraryScope?,
)

fun WorkspaceSection.toMapScope(itineraryScope: ItineraryScope): MapScope
fun ItineraryScope.selectedDayId(): String?
internal fun encodeItineraryScope(scope: ItineraryScope): String
internal fun decodeItineraryScope(raw: String?): ItineraryScope?
internal fun restoreWorkspaceNavigation(
    sectionRaw: String?,
    itineraryScopeRaw: String?,
    legacyTabRaw: String?,
    legacyScopeRaw: String?,
    legacySelectedDayId: String?,
): RestoredWorkspaceNavigation
internal fun reconcileItineraryScope(
    current: ItineraryScope?,
    previousDays: List<TripDay>,
    currentDays: List<TripDay>,
): ItineraryScope
```

- [ ] **Step 1: Write failing pure JVM tests**

Replace the old restoration test with `WorkspaceNavigationTest` cases that assert these exact behaviors:

```kotlin
@Test fun `place pool derives place pool map scope`() {
    assertEquals(MapScope.PLACE_POOL, WorkspaceSection.PLACE_POOL.toMapScope(ItineraryScope.WholeTrip))
}

@Test fun `itinerary scopes derive whole trip and single day map scopes`() {
    assertEquals(MapScope.WHOLE_TRIP, WorkspaceSection.ITINERARY.toMapScope(ItineraryScope.WholeTrip))
    assertEquals(MapScope.SINGLE_DAY, WorkspaceSection.ITINERARY.toMapScope(ItineraryScope.Day("day-2")))
}

@Test fun `new state wins over legacy keys`() {
    assertEquals(
        RestoredWorkspaceNavigation(WorkspaceSection.ITINERARY, ItineraryScope.WholeTrip),
        restoreWorkspaceNavigation("ITINERARY", "WHOLE_TRIP", "PLACES", "PLACE_POOL", "day-1"),
    )
}

@Test fun `legacy states migrate without allowing invalid combinations`() {
    assertEquals(
        RestoredWorkspaceNavigation(WorkspaceSection.PLACE_POOL, null),
        restoreWorkspaceNavigation(null, null, "SEARCH", "PLACE_POOL", null),
    )
    assertEquals(
        RestoredWorkspaceNavigation(WorkspaceSection.ITINERARY, ItineraryScope.Day("day-2")),
        restoreWorkspaceNavigation(null, null, "ITINERARY", "SINGLE_DAY", "day-2"),
    )
    assertEquals(
        RestoredWorkspaceNavigation(WorkspaceSection.ITINERARY, ItineraryScope.WholeTrip),
        restoreWorkspaceNavigation(null, null, "PLACES", "WHOLE_TRIP", null),
    )
}

@Test fun `first itinerary entry defaults to first day and no days defaults whole trip`() {
    assertEquals(ItineraryScope.Day("day-1"), reconcileItineraryScope(null, emptyList(), days("day-1", "day-2")))
    assertEquals(ItineraryScope.WholeTrip, reconcileItineraryScope(null, emptyList(), emptyList()))
}

@Test fun `deleted day selects successor then predecessor and keeps valid id across reorder`() {
    val old = days("day-1", "day-2", "day-3")
    assertEquals(ItineraryScope.Day("day-3"), reconcileItineraryScope(ItineraryScope.Day("day-2"), old, days("day-1", "day-3")))
    assertEquals(ItineraryScope.Day("day-1"), reconcileItineraryScope(ItineraryScope.Day("day-3"), old, days("day-1", "day-2")))
    assertEquals(ItineraryScope.Day("day-2"), reconcileItineraryScope(ItineraryScope.Day("day-2"), old, listOf(TripDay("day-2", 0), TripDay("day-1", 1))))
}
```

The test helper must create `TripDay(id, index)` in argument order.

- [ ] **Step 2: Run RED**

```bash
./gradlew testDebugUnitTest --tests '*WorkspaceNavigationTest'
```

Expected: compilation fails because the new types and functions do not exist.

- [ ] **Step 3: Implement the minimal pure navigation model**

Implement exact encodings:

```kotlin
private const val WHOLE_TRIP_VALUE = "WHOLE_TRIP"
private const val DAY_PREFIX = "DAY:"
```

Rules:
- `encodeItineraryScope(WholeTrip) == "WHOLE_TRIP"`.
- `encodeItineraryScope(Day("day-2")) == "DAY:day-2"`.
- Unknown/blank values decode to `null`.
- New keys have priority whenever `sectionRaw` parses successfully; only absent/invalid new keys consult legacy values.
- Legacy `SEARCH` maps to `PLACE_POOL`.
- Legacy `WHOLE_TRIP` maps to `ITINERARY + WholeTrip`; legacy `SINGLE_DAY` with an ID maps to `ITINERARY + Day(id)`; otherwise legacy tab determines the section.
- `reconcileItineraryScope` retains `WholeTrip`, retains a valid day ID across reorder, and uses the removed day’s prior position to choose the current item at that position (successor) or the last item (predecessor).

- [ ] **Step 4: Run GREEN**

```bash
./gradlew testDebugUnitTest --tests '*WorkspaceNavigationTest'
```

Expected: all tests pass.

---

### Task 2: 建立全程只读 UI 模型

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryUiModels.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModel.kt:30-31,154-173`
- Create: `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryMapperTest.kt`

**Interfaces:**
- Consumes: `TripDay`, `DayMapSnapshot`, `ItineraryItem`, `RouteLegEntity`, `RouteStatus`, `TransportMode`, current route error copy.
- Produces:

```kotlin
data class ItineraryItemUi(
    val id: String,
    val name: String,
    val address: String,
    val arrivalTime: LocalTime?,
    val stayMinutes: Int?,
)

data class RouteLegUi(
    val id: String,
    val fromItemId: String,
    val toItemId: String,
    val mode: TransportMode,
    val status: RouteStatus,
    val distanceMeters: Int?,
    val durationSeconds: Int?,
    val error: String?,
)

data class WholeTripDayUi(
    val dayId: String,
    val dayNumber: Int,
    val items: List<ItineraryItemUi>,
    val legs: List<RouteLegUi>,
)

internal fun mapWholeTripDays(
    days: List<TripDay>,
    snapshots: List<DayMapSnapshot>,
): List<WholeTripDayUi>
```

- [ ] **Step 1: Write failing mapper tests**

Cover in one focused test class:
- input days in unsorted order must output increasing `TripDay.index` with day numbers `index + 1`;
- a day with no snapshot remains present with empty `items` and `legs`;
- itinerary item order is preserved;
- only a leg whose `fromItemId` and `toItemId` match adjacent items is retained;
- selected transport mode wins over recommended mode;
- status, distance, duration and existing Chinese error summaries match `DayItineraryViewModel` behavior.

Use concrete fixtures with day 2 listed before day 1, three items, one valid adjacent leg and one stale/non-adjacent leg. Assert the complete returned `WholeTripDayUi` values, not only sizes.

- [ ] **Step 2: Run RED**

```bash
./gradlew testDebugUnitTest --tests '*WholeTripItineraryMapperTest'
```

Expected: compilation fails because `WholeTripDayUi` and `mapWholeTripDays` do not exist.

- [ ] **Step 3: Move shared models and implement mapper**

Move `ItineraryItemUi` and `RouteLegUi` unchanged out of `DayItineraryViewModel.kt` into `ItineraryUiModels.kt`. Add internal conversion functions there so both the day ViewModel and whole-trip mapper use one mapping path. Build each day’s legs by adjacent item pairs:

```kotlin
val adjacentPairs = items.zipWithNext { from, to -> from.id to to.id }.toSet()
val visibleLegs = snapshot.legs
    .filter { it.fromItemId to it.toItemId in adjacentPairs }
    .map(RouteLegEntity::toRouteLegUi)
```

Do not add a trip-wide repository API or query Room again.

- [ ] **Step 4: Run GREEN and existing day tests**

```bash
./gradlew testDebugUnitTest --tests '*WholeTripItineraryMapperTest' --tests '*DayItineraryViewModelTest'
```

Expected: all selected tests pass; if no class matches `DayItineraryViewModelTest`, run `./gradlew testDebugUnitTest` and confirm the suite passes.

---

### Task 3: Make TripWorkspaceViewModel the single navigation source

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt:28-33,67-80,93-96,132-211,290-305`
- Create: `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceNavigationStateTest.kt`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/workspace/MapViewportControllerTest.kt`

**Interfaces:**
- Consumes: Task 1 navigation functions and Task 2 `mapWholeTripDays`.
- Produces:

```kotlin
data class TripWorkspaceUiState(
    // existing fields retained
    val section: WorkspaceSection = WorkspaceSection.PLACE_POOL,
    val itineraryScope: ItineraryScope = ItineraryScope.WholeTrip,
    val mapScope: MapScope = MapScope.PLACE_POOL,
    val selectedDayId: String? = null,
    val wholeTripDays: List<WholeTripDayUi> = emptyList(),
)

val selectedDayId: StateFlow<String?>
fun selectSection(section: WorkspaceSection)
fun selectItineraryScope(scope: ItineraryScope)
```

- Removes public navigation methods `selectTab`, `selectScope`, and `selectDay` after all production/test callers are migrated in this task.

- [ ] **Step 1: Write failing ViewModel navigation tests**

Use `SavedStateHandle`, mutable fake trip/day/route flows and `runTest`. Assert:
1. first state is `PLACE_POOL`, but remembered itinerary scope is first day;
2. `selectSection(ITINERARY)` publishes `SINGLE_DAY` and that day ID;
3. selecting `WholeTrip` publishes `WHOLE_TRIP`, `selectedDayId == null`, and all grouped days;
4. switching to place pool and back retains the last itinerary scope;
5. new keys are written as `workspace.section` and `workspace.itineraryScope`;
6. legacy keys migrate once and later new selections are not overridden by them;
7. deleting selected day chooses successor, then predecessor, and no dates yields `WholeTrip`;
8. each real section/scope change increments viewport request once, while a repeated selection does not.

- [ ] **Step 2: Run RED**

```bash
./gradlew testDebugUnitTest --tests '*TripWorkspaceNavigationStateTest'
```

Expected: compilation fails because the ViewModel still exposes independent tab/scope/day state.

- [ ] **Step 3: Replace independent state with unified state**

Implementation rules:
- Read `workspace.section` and `workspace.itineraryScope` first, then call Task 1 migration with legacy keys only for missing new values.
- Persist immediately to the new keys after restoration, so legacy keys cannot reapply.
- Keep a private previous-days snapshot to reconcile deleted dates by prior index.
- `PLACE_POOL` changes only `section`; it never clears the remembered itinerary scope.
- `selectItineraryScope(Day(id))` ignores an ID absent from current `state.days`.
- `mapScope` and `selectedDayId` are computed from `section + itineraryScope`; they are never read from independent state flows.
- `MapUiModelMapper.map`, `mapViewportPoints`, and `MapViewportController.update` receive the derived values.
- Populate `wholeTripDays = mapWholeTripDays(currentTrip.days, currentSnapshots)`.
- Retain existing search focus, selected marker/POI, route repair, map layer and sheet state code unchanged.

Expose `selectedDayId` as a derived `StateFlow<String?>` suitable for `DayItineraryViewModel.Factory`. It must emit `null` for `WholeTrip` and `PLACE_POOL` only when no concrete day is active; switching temporarily to place pool must not destroy the remembered day in navigation state.

- [ ] **Step 4: Run GREEN and workspace regression tests**

```bash
./gradlew testDebugUnitTest \
  --tests '*WorkspaceNavigationTest' \
  --tests '*TripWorkspaceNavigationStateTest' \
  --tests '*MapUiModelMapperTest' \
  --tests '*MapViewportControllerTest' \
  --tests '*SearchSelectionConsumptionTest'
```

Expected: all selected tests pass, including marker + full-polyline viewport coverage.

---

### Task 4: Separate editable day content from date selection and share presentation

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModel.kt:53-85`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItinerarySheet.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryItemRow.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/RouteLegRow.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContent.kt`
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContentTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt`

**Interfaces:**
- Consumes: `DayItineraryUiState`, Task 2 models, current editing callbacks and formatting functions.
- Produces:

```kotlin
@Composable
fun DayItinerarySheet(
    viewModel: DayItineraryViewModel,
    modifier: Modifier = Modifier,
)

@Composable
fun ItineraryItemCard(
    item: ItineraryItemUi,
    displayOrder: Int,
    modifier: Modifier = Modifier,
)

@Composable
fun RouteLegContent(
    leg: RouteLegUi,
    modifier: Modifier = Modifier,
)

@Composable
fun WholeTripItineraryContent(
    days: List<WholeTripDayUi>,
    modifier: Modifier = Modifier,
)
```

- [ ] **Step 1: Write failing Compose tests**

In `WholeTripItineraryContentTest`, assert:
- zero days shows `暂无旅行日`;
- days render in mapper order with tags `whole-trip-day-{dayId}` and headings `第一天`, `第二天`;
- an empty day shows `暂无行程` under its heading;
- item and matching leg tags appear in item/leg/item order;
- unmerged tree contains no tags prefixed `timing-`, `move-`, `delete-`, `mode-`, `retry-`;
- item cards have no reorder custom actions.

Update `ItineraryEditingTest` so it invokes the new two-argument `DayItinerarySheet(model)` and explicitly asserts no `Day 1`/`Day 2` selector is rendered while every existing edit assertion remains.

- [ ] **Step 2: Verify Compose RED on the approved AVD**

```bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
AVD_NAME="$($ADB -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')"
test "$AVD_NAME" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContentTest
```

Expected: compilation fails because `WholeTripItineraryContent` does not exist.

- [ ] **Step 3: Extract pure presentation without weakening edit behavior**

Implementation rules:
- `ItineraryItemCard` renders the card’s number, name, optional address and timing only; it has no pointer input, buttons or reorder semantics.
- `ItineraryItemRow` wraps `ItineraryItemCard` and retains the current long-press drag, visual elevation, custom accessibility actions and three edit buttons.
- `RouteLegContent` renders mode, status/detail and progress indicator only.
- `RouteLegRow` wraps it and retains transport mode and failed-route retry buttons.
- `WholeTripItineraryContent` uses one `LazyColumn`; each date heading has heading semantics and test tag, then emits item/leg/item using adjacent IDs. It invokes only `ItineraryItemCard` and `RouteLegContent`.
- Use Chinese numerals through a deterministic helper for at least 1–10, and fall back to `第 N 天` above 10. The exact visible labels for 1–10 are `第一天` through `第十天`.
- `DayItinerarySheet` removes `DaySelector` and `onSelectDay`, but retains saved-place add buttons, errors, editable timeline and all four dialog flows.
- Make `DayItineraryViewModel` consume nullable external day selections so a `null` emission clears items/legs/preview state instead of being filtered out.

- [ ] **Step 4: Run GREEN on the approved AVD**

```bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
test "$($ADB -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContentTest,com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest
```

Expected: both classes pass; single-day editing retains all prior capabilities and whole-trip has no edit actions.

---

### Task 5: Build the 88dp itinerary scope rail and two-column content

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryScopeRail.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WorkspaceItineraryContent.kt`
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryScopeRailTest.kt`

**Interfaces:**
- Consumes: `List<TripDay>`, `ItineraryScope`, `List<WholeTripDayUi>`, editable day content slot.
- Produces:

```kotlin
@Composable
fun ItineraryScopeRail(
    days: List<TripDay>,
    selected: ItineraryScope,
    onSelect: (ItineraryScope) -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
fun WorkspaceItineraryContent(
    days: List<TripDay>,
    selected: ItineraryScope,
    wholeTripDays: List<WholeTripDayUi>,
    onSelect: (ItineraryScope) -> Unit,
    dayContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
)
```

- [ ] **Step 1: Write failing scope rail tests**

Render seven days and assert:
- container tag `itinerary-scope-rail` has width `88.dp`;
- `itinerary-scope-WHOLE_TRIP` and `itinerary-scope-day-1` exist;
- visible copy is `全程`, `第一天`, `第二天`;
- selected semantics moves from `WholeTrip` to `Day("day-2")` after click;
- scrolling only the rail reveals `第七天` while a tagged right-side sentinel keeps the same root position;
- `WorkspaceItineraryContent` invokes whole-trip rendering only for `WholeTrip`, and invokes `dayContent` only for `Day`.

- [ ] **Step 2: Run RED on the approved AVD**

```bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
test "$($ADB -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryScopeRailTest
```

Expected: compilation fails because both composables do not exist.

- [ ] **Step 3: Implement the rail and composition boundary**

Use:

```kotlin
Row(modifier) {
    ItineraryScopeRail(Modifier.width(88.dp).fillMaxHeight())
    VerticalDivider()
    Box(Modifier.weight(1f).fillMaxHeight()) { /* right content */ }
}
```

Rail rules:
- a dedicated `LazyColumn` with `Modifier.selectableGroup()`;
- first item `全程`, then days sorted by `TripDay.index`;
- each item uses `SelectablePill` or equivalent `selectable(role = Role.Tab)` with explicit selected semantics and tags;
- rail selection never directly edits `MapScope`;
- right side owns its own list/scroll state (`WholeTripItineraryContent` has its `LazyColumn`; `DayItinerarySheet` retains its editable `LazyColumn`).

- [ ] **Step 4: Run GREEN**

Repeat the guarded command from Step 2. Expected: all tests pass.

---

### Task 6: Replace the production workspace navigation and wire map synchronization

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt:47-107,194-245`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt:92-137`
- Replace behavior in: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`
- Replace behavior in: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceSearchTabsTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/SearchMapFocusTest.kt`

**Interfaces:**
- Consumes: `TripWorkspaceUiState.section`, `itineraryScope`, `wholeTripDays`; Task 5 `WorkspaceItineraryContent`; existing `placeContent`, map and POI callbacks.
- Produces this screen contract:

```kotlin
@Composable
fun TripWorkspaceScreen(
    viewModel: TripWorkspaceViewModel,
    consent: AmapConsentToken?,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onPrivacySettings: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    placeContent: @Composable () -> Unit,
    dayItineraryContent: @Composable () -> Unit,
    // existing collection and map host parameters unchanged
)
```

- [ ] **Step 1: Rewrite failing workspace UI tests for the new contract**

`WorkspaceFlowTest` must assert:
- exactly two bottom navigation tags: `section-PLACE_POOL`, `section-ITINERARY`;
- no `scope-PLACE_POOL`, `scope-SINGLE_DAY`, `scope-WHOLE_TRIP`, `tab-PLACES`, or `tab-ITINERARY` node exists;
- default place content is shown directly;
- selecting `行程` shows `itinerary-scope-rail`, defaults to `第一天`, shows editable day sentinel, and state/map scope is `SINGLE_DAY`;
- selecting `全程` hides editable sentinel, shows whole-trip day groups, and state/map scope is `WHOLE_TRIP`;
- selecting `第二天` restores editable sentinel, updates `selectedDayId`, and map scope is `SINGLE_DAY`;
- switching place pool → itinerary remembers `第二天`;
- each selection changes viewport request ID once;
- top bar remains `40.dp`, search launcher remains `48.dp`, map stays above bottom navigation, handle collapse/expand remains functional.

`WorkspaceSearchTabsTest` becomes a focused bottom-navigation/search regression test: only two section choices, no drawer tab row, search launcher remains read-only and clickable.

Update `SearchMapFocusTest.tabAndSheetChangesKeepViewModelViewportRequestId` to prove repeated selection, sheet changes and list rendering do not move the camera; real section/scope changes are covered by `WorkspaceFlowTest`.

- [ ] **Step 2: Run RED on the approved AVD**

```bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
test "$($ADB -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest,com.yangchengwei.easytrip.workspace.WorkspaceSearchTabsTest,com.yangchengwei.easytrip.workspace.SearchMapFocusTest
```

Expected: assertions fail because the production screen still exposes old tabs and independent map scopes.

- [ ] **Step 3: Replace screen navigation without touching map overlays**

In `TripWorkspaceScreen`:
- remove the sheet’s `WorkspaceTab` row;
- sheet body switches directly on `state.section`;
- `PLACE_POOL` invokes `placeContent()`;
- `ITINERARY` invokes `WorkspaceItineraryContent(state.days, state.itineraryScope, state.wholeTripDays, viewModel::selectItineraryScope, dayItineraryContent)`;
- replace the `MapScope.entries` row under the map with two adjacent `SelectablePill` controls tagged `section-PLACE_POOL` and `section-ITINERARY`, wrapped by `selectableGroup()`;
- labels are exactly `地点池` and `行程`;
- preserve the existing map `Box`, top bar, layer menu, search launcher, map errors, POI/marker dialogs, sheet peek height, handle and drag synchronization unchanged.

In `AppNavigation`:
- continue creating the same three ViewModels;
- continue passing `workspaceModel.selectedDayId` into `DayItineraryViewModel.Factory`;
- pass `PlacePoolSheet(showSearch = false)` as place content;
- pass `DayItinerarySheet(itineraryModel)` as editable day content;
- do not create a separate whole-trip ViewModel or repository query.

- [ ] **Step 4: Run GREEN and map regressions**

```bash
./gradlew testDebugUnitTest --tests 'com.yangchengwei.easytrip.workspace.*'
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
test "$($ADB -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest,com.yangchengwei.easytrip.workspace.WorkspaceSearchTabsTest,com.yangchengwei.easytrip.workspace.SearchMapFocusTest,com.yangchengwei.easytrip.workspace.MapLayerFlowTest
```

Expected: all tests pass; section/scope selection and map always agree.

---

### Task 7: Complete regression coverage, simulator interaction, docs, graph, and prototype cleanup

**Files:**
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/V2AcceptanceTest.kt` only where old navigation labels/tags are asserted.
- Modify: `docs/testing/v2-device-checklist.md`
- Delete: `app/src/debug/java/com/yangchengwei/easytrip/WorkspaceNavigationPreviewActivity.kt`
- Delete: `app/src/debug/AndroidManifest.xml`
- Generated update: `graphify-out/**`

**Interfaces:**
- Consumes: final production behavior from Tasks 1–6.
- Produces: verified build, updated acceptance checklist and no debug-only preview activity.

- [ ] **Step 1: Compile all tests before device execution**

```bash
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

Expected: all commands pass with no compilation or lint errors. If an old test refers to removed `WorkspaceTab`, `selectScope`, `DaySelector`, or old tags, update that test to assert the new public behavior rather than reintroducing compatibility APIs.

- [ ] **Step 2: Run all instrumentation on only the approved AVD**

```bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
AVD_NAME="$($ADB -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')"
printf '%s\n' "$AVD_NAME"
test "$AVD_NAME" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest
```

Expected: full instrumentation suite passes. If the AVD name differs or the command is unsupported because the target is a physical device, stop without running Gradle instrumentation.

- [ ] **Step 3: Perform interactive simulator verification**

Install and launch only on the already verified `easy_trip_p60pro` serial:

```bash
./gradlew assembleDebug
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
test "$($ADB -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
$ADB -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
$ADB -s "$ANDROID_SERIAL" shell am start -n com.yangchengwei.easytrip/.MainActivity
```

Interact in the emulator and record results for:
1. “地点池 / 行程”切换且抽屉无重复 Tab；
2. 首次行程默认第一天，切回后记住最后选择；
3. 全程/每天切换地图并包含完整路线；
4. 全程按天分组、空日空态、无编辑入口；
5. 单日添加、排序、时间、交通方式、删除、跨日移动入口仍存在；
6. 7 天以上左栏独立滚动，右侧位置不随左栏移动；
7. 抽屉收起/半屏/展开、顶部栏、搜索、图层、缩放和地图 POI 收藏交互无回归。

Do not claim simulator verification for any item that was not actually clicked.

- [ ] **Step 4: Update the device checklist**

Add a new unverified physical-device section dated `2026-08-23` for this navigation refactor. Record automated and `easy_trip_p60pro` simulator results separately. Leave physical-device result as `待验收`; do not overwrite the previous V2 passed record or claim this increment passed on device.

- [ ] **Step 5: Remove the throwaway preview and rebuild**

Delete both debug prototype files listed above. Then run:

```bash
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
git diff --check
```

Expected: all pass and the installed production UI no longer depends on `WorkspaceNavigationPreviewActivity`.

- [ ] **Step 6: Refresh Graphify and review the final diff**

```bash
graphify update .
git status --short
git diff --stat
git diff --check
```

Confirm:
- no secrets or `local.properties` are tracked;
- no Room schema changed;
- no temporary preview file remains;
- no unrelated file was modified;
- no commit or push was performed.
