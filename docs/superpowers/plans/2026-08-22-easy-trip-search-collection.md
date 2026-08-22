# Easy Trip Search Collection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将工作台搜索迁移到独立页面，统一搜索结果与地图 POI 的收藏/取消收藏规则，并补齐收藏 marker 身份、重复编号、高亮和地点池只读滚动条。

**Architecture:** 保留单 `app` module 和现有 Room/Repository/ViewModel 分层。`AppNavigation` 通过 back stack `SavedStateHandle` 传递一次性搜索选择，`PlacePoolViewModel` 统一执行收藏切换与影响确认，`TripWorkspaceViewModel` 只管理工作台和地图瞬时状态；`MapUiModelMapper` 产出显式 marker kind/badge/focus，AMap host 只负责 SDK 事件转换和绘制。复用当前 `deletePlaceAndReferences` 事务，不修改 Room schema。

**Tech Stack:** Kotlin、Jetpack Compose、Material 3、Navigation Compose、ViewModel、SavedStateHandle、StateFlow、Room、高德 Android Map SDK、JUnit 4、kotlinx-coroutines-test、Compose UI Test。

**Spec:** `docs/superpowers/specs/2026-08-22-easy-trip-search-collection-design.md`

## Global Constraints

- 基于当前工作树继续实现；当前存在大量未提交视觉改动，禁止回退、覆盖或要求恢复这些改动。
- 抽屉删除 SEARCH Tab，只保留地点池和每日行程。
- 工作台搜索入口只读，位于地图底部右侧，宽屏宽度 `1/5`、可见高度 `40dp`、白底、仅放大镜、无文字。
- 选择搜索结果后清空搜索状态、返回工作台并聚焦高亮；导航 selection 只消费一次。
- `usageCount == 0` 直接取消收藏；`usageCount > 0` 先显示影响确认，确认后调用现有 `deletePlaceAndReferences`。
- 点击高德底图 POI 必须先显示地点卡片，再由用户收藏或取消；不得点击即修改数据。
- 不修改 Room entity、DAO schema、数据库版本或 `app/schemas/**`。
- 采用严格 TDD：每个任务先 RED，再写最小实现，再 GREEN。
- 所有 instrumentation 只能在 AVD 名严格为 `easy_trip_p60pro` 时运行；先用 `adb -s "$ANDROID_SERIAL" emu avd name` 校验精确输出，否则停止。
- 不把 API key、keystore、密码、token、设备序列号写入 tracked 文件；不得打印或复制 `local.properties` 中的 key。
- 不 commit、不 push；忽略 writing-plans 模板中的常规 commit 步骤，以本约束为准。

---

### Task 1: 收敛工作台 Tab 并建立独立搜索路由

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt:27-28,35-46,59-63,132-151,190-198`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt:48-60,93-108,194-200`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt:46-49,64-76,109-132`
- Create: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchScreen.kt`
- Create: `app/src/test/java/com/yangchengwei/easytrip/workspace/WorkspaceTabRestorationTest.kt`
- Replace: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceSearchTabsTest.kt`

**Interfaces:**
- Consumes: current `WorkspaceSheetLevel`, `PlaceSearchField`, `PlaceSearchResults`, `PlacePoolViewModel.Factory`.
- Produces:

```kotlin
const val TRIP_SEARCH_ROUTE = "trips/{tripId}/search"
fun tripSearchRoute(tripId: String): String = "trips/$tripId/search"

enum class WorkspaceTab { PLACES, ITINERARY }

internal fun restoreWorkspaceTab(raw: String?): WorkspaceTab =
    WorkspaceTab.entries.firstOrNull { it.name == raw } ?: WorkspaceTab.PLACES

@Composable
fun WorkspaceSearchLauncher(onClick: () -> Unit, modifier: Modifier = Modifier)

@Composable
fun PlaceSearchScreen(
    state: PlacePoolUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSelect: (PlaceCandidate) -> Unit,
    onToggleCollection: (PlaceCandidate) -> Unit,
)
```

- [ ] **Step 1: Write the failing tab restoration test**

```kotlin
class WorkspaceTabRestorationTest {
    @Test fun `legacy search tab restores as places`() {
        assertEquals(WorkspaceTab.PLACES, restoreWorkspaceTab("SEARCH"))
    }

    @Test fun `known itinerary tab is retained`() {
        assertEquals(WorkspaceTab.ITINERARY, restoreWorkspaceTab("ITINERARY"))
    }
}
```

- [ ] **Step 2: Rewrite the existing drawer UI test for the new contract**

In `WorkspaceSearchTabsTest`, render `TripWorkspaceScreen` with fake contents and assert:

```kotlin
compose.onNodeWithTag("tab-PLACES").assertExists()
compose.onNodeWithTag("tab-ITINERARY").assertExists()
compose.onNodeWithTag("tab-SEARCH").assertDoesNotExist()
compose.onNodeWithTag("workspace-search-launcher").assertHasClickAction()
compose.onNodeWithTag("workspace-search-launcher").performClick()
assertEquals(1, searchLaunches)
```

Also assert no editable text node exists inside `workspace-search-launcher` and its content description is `搜索地点`.

- [ ] **Step 3: Run RED**

Run JVM first:

```bash
./gradlew testDebugUnitTest --tests '*WorkspaceTabRestorationTest'
```

Expected: FAIL because `restoreWorkspaceTab` does not exist and `WorkspaceTab.SEARCH` still exists.

Before UI RED, require:

```bash
test "$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceSearchTabsTest
```

Expected: FAIL because the drawer still renders `tab-SEARCH` and the launcher is still an editable `PlaceSearchField`.

- [ ] **Step 4: Implement the minimal workspace structure**

Change `WorkspaceTab` to two values and replace direct `valueOf` restoration with `restoreWorkspaceTab`. Remove `searchContent`, `searchQuery`, and `onSearchQueryChange` from `TripWorkspaceScreen`. Add `onOpenSearch: () -> Unit` and render:

```kotlin
Box(
    Modifier.fillMaxWidth().wrapContentHeight(),
    contentAlignment = Alignment.BottomEnd,
) {
    WorkspaceSearchLauncher(
        onClick = onOpenSearch,
        modifier = Modifier.fillMaxWidth(0.2f).height(40.dp),
    )
}
```

`WorkspaceSearchLauncher` uses a white `Surface`, a whole-surface `clickable(role = Role.Button)`, tag `workspace-search-launcher`, and a drawn/vector search icon with `contentDescription = "搜索地点"`; it must not contain `TextField` or `Text`.

Add `TRIP_SEARCH_ROUTE` and a `composable` destination. At this task, `PlaceSearchScreen` may wire the existing field/results UI; selection plumbing is completed in Task 2. Preserve the current top bar, layer menu, map scopes, sheet behavior and all visual changes unrelated to search.

- [ ] **Step 5: Run GREEN**

```bash
./gradlew testDebugUnitTest --tests '*WorkspaceTabRestorationTest' lintDebug assembleDebug
test "$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceSearchTabsTest
```

Expected: all commands PASS; the drawer has exactly two Tabs and the launcher navigates without exposing an editable field.

---

### Task 2: 完成搜索页状态清理与一次性返回聚焦

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducer.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt:28-64`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchScreen.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt:30-33,63-76,92-112,148-169`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducerTest.kt`
- Create: `app/src/test/java/com/yangchengwei/easytrip/workspace/SearchSelectionConsumptionTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/SearchMapFocusTest.kt`

**Interfaces:**
- Consumes: `TripWorkspaceViewModel.focusSearchResult(PlaceCandidate)`, `MapViewportController.focusSearchResult`.
- Produces:

```kotlin
const val SEARCH_SELECTION_RESULT = "workspace.searchSelection"

data class SearchSelectionPayload(
    val poiId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

fun PlaceSearchReducer.clear()
fun PlacePoolViewModel.clearSearch()

internal fun consumeSearchSelection(
    handle: SavedStateHandle,
    onSelection: (PlaceCandidate) -> Unit,
): Boolean
```

- [ ] **Step 1: Add failing reducer tests**

Extend `PlaceSearchReducerTest` with a controllable suspended source. Set a query, emit results/error, call `clear()`, then assert:

```kotlin
assertEquals(PlaceSearchState(), reducer.state.value)
assertTrue(activeSearchJob.isCancelled)
```

Advance virtual time after clearing and assert a stale response cannot repopulate results.

- [ ] **Step 2: Add failing one-shot consumption test**

```kotlin
@Test fun `selection is consumed once and removed from saved state`() {
    val handle = SavedStateHandle(mapOf(SEARCH_SELECTION_RESULT to payload))
    val selected = mutableListOf<PlaceCandidate>()
    assertTrue(consumeSearchSelection(handle, selected::add))
    assertFalse(consumeSearchSelection(handle, selected::add))
    assertEquals(listOf("poi-1"), selected.map { it.poiId })
    assertNull(handle.get<SearchSelectionPayload>(SEARCH_SELECTION_RESULT))
}
```

- [ ] **Step 3: Run RED**

```bash
./gradlew testDebugUnitTest \
  --tests '*PlaceSearchReducerTest' \
  --tests '*SearchSelectionConsumptionTest'
```

Expected: FAIL because `clear`, payload and consume helper are absent.

- [ ] **Step 4: Implement minimal clearing and navigation result transfer**

`PlaceSearchReducer.clear()` increments the existing generation, cancels the active job and assigns `PlaceSearchState()`. `PlacePoolViewModel.clearSearch()` delegates to it.

On result selection in the search destination:

```kotlin
navController.previousBackStackEntry
    ?.savedStateHandle
    ?.set(SEARCH_SELECTION_RESULT, candidate.toPayload())
placeModel.clearSearch()
navController.popBackStack()
```

In the workspace destination, observe/consume `SEARCH_SELECTION_RESULT`, call `workspaceModel.focusSearchResult(payload.toCandidate())`, then remove the key before returning. Do not restore the removed SEARCH Tab. Update focus reconciliation so a selected point remains focused when it matches a saved place even after search results are cleared.

- [ ] **Step 5: Update focus instrumentation flow**

`SearchMapFocusTest` must navigate by clicking `workspace-search-launcher`, type on the independent page, click a result, and assert:

- search destination disappears;
- its query/result state is empty;
- workspace receives exactly one `SEARCH_FOCUS` viewport request;
- the marker is highlighted;
- recomposition and re-entering the page do not replay the old selection.

- [ ] **Step 6: Run GREEN**

```bash
./gradlew testDebugUnitTest \
  --tests '*PlaceSearchReducerTest' \
  --tests '*SearchSelectionConsumptionTest' \
  --tests '*MapInteractionReducerTest'
test "$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.SearchMapFocusTest
```

Expected: PASS; selection clears search and moves the camera exactly once.

---

### Task 3: 统一搜索结果与地点卡片的收藏切换策略

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/place/ui/CollectionTogglePolicy.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt:18-26,28-64`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchResults.kt:16-49`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchScreen.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt:44-53`
- Create: `app/src/test/java/com/yangchengwei/easytrip/place/ui/CollectionTogglePolicyTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlacePoolFlowTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/CascadeDeleteTest.kt`

**Interfaces:**
- Consumes: `SavedPlaceRepository.save`, `usageCount`, `deletePlaceAndReferences`, and `PlaceService` wrappers.
- Produces:

```kotlin
data class PendingCollectionRemoval(
    val candidate: PlaceCandidate,
    val place: SavedPlace,
    val usageCount: Int,
)

sealed interface CollectionDecision {
    data object Save : CollectionDecision
    data class RemoveNow(val placeId: String) : CollectionDecision
    data class Confirm(val placeId: String, val usageCount: Int) : CollectionDecision
}

fun decideCollectionToggle(
    candidate: PlaceCandidate,
    savedPlace: SavedPlace?,
    usageCount: Int?,
): CollectionDecision

fun PlacePoolViewModel.toggleCollection(candidate: PlaceCandidate)
fun PlacePoolViewModel.confirmCollectionRemoval()
fun PlacePoolViewModel.dismissCollectionRemoval()
```

`PlacePoolUiState` additionally exposes `pendingCollectionRemoval`, `collectionBusyPoiIds: Set<String>` and `collectionError: String?`.

- [ ] **Step 1: Write failing pure policy tests**

```kotlin
@Test fun `unsaved candidate saves`() =
    assertEquals(CollectionDecision.Save, decideCollectionToggle(candidate, null, null))

@Test fun `unused saved place removes immediately`() =
    assertEquals(CollectionDecision.RemoveNow("place-1"), decideCollectionToggle(candidate, saved, 0))

@Test fun `used saved place requires impact confirmation`() =
    assertEquals(CollectionDecision.Confirm("place-1", 4), decideCollectionToggle(candidate, saved, 4))
```

Also assert negative or missing usage for a saved place is rejected rather than guessed.

- [ ] **Step 2: Add failing UI/Room cases**

Update `PlacePoolFlowTest` so clicking an enabled “已收藏” action with zero usage removes the place. Add a case with one itinerary occurrence: first click opens confirmation showing `1`, dismiss leaves data unchanged, second click plus confirmation removes it.

Extend `CascadeDeleteTest` with the exact path used by `confirmCollectionRemoval`: after a place appears multiple times on multiple days, confirmation removes all occurrences and leaves the expected bridge route. Snapshot `app/schemas/**` before/after in Task 7; do not modify it here.

- [ ] **Step 3: Run RED**

```bash
./gradlew testDebugUnitTest --tests '*CollectionTogglePolicyTest'
test "$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest,com.yangchengwei.easytrip.CascadeDeleteTest
```

Expected: JVM compile/behavior FAIL because the policy is absent; instrumentation FAIL because “已收藏” is disabled and no toggle confirmation path exists.

- [ ] **Step 4: Implement the minimum shared state machine**

Keep the latest unfiltered saved-place list indexed by `amapPoiId` in `PlacePoolViewModel`. `toggleCollection` disables only the target POI while it:

```kotlin
val saved = savedByPoiId[candidate.poiId]
if (saved == null) repository.save(tripId, candidate)
else when (val count = service.deletionUsageCount(saved.id)) {
    0 -> service.deletePlaceAndReferences(saved.id)
    else -> setPending(candidate, saved, count)
}
```

Do not directly call `PlaceDao.deletePlace`. On failure retain the saved state, clear busy state and expose a retryable message. `confirmCollectionRemoval` calls `service.deletePlaceAndReferences` once and closes only on success.

Change `PlaceSearchResults` to accept:

```kotlin
onToggleCollection: (PlaceCandidate) -> Unit
collectionBusyPoiIds: Set<String>
```

The trailing control remains enabled for saved, locatable candidates and calls only `onToggleCollection`; use a separate clickable region so it does not invoke `onSelect`.

- [ ] **Step 5: Run GREEN**

```bash
./gradlew testDebugUnitTest --tests '*CollectionTogglePolicyTest' lintDebug
test "$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest,com.yangchengwei.easytrip.CascadeDeleteTest
```

Expected: PASS; zero usage removes immediately, positive usage requires confirmation, cancel preserves rows, confirm reuses existing cascade behavior.

---

### Task 4: 建立稳定的收藏 marker 身份、编号与聚焦样式

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/MapFacade.kt:13-29,85-199`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/AmapComposeMap.kt:82-219`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt:102-142`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/workspace/MapUiModelMapperTest.kt`
- Create: `app/src/test/java/com/yangchengwei/easytrip/workspace/OccurrenceBadgeFormatterTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/amap/AmapComposeMapTest.kt`

**Interfaces:**
- Consumes: `SavedPlace.amapPoiId`, `OccurrenceUi`, `MapScope`, existing viewport request.
- Produces:

```kotlin
enum class MapMarkerKind { UNSAVED_SEARCH, SAVED_PLACE_POOL, SAVED_ITINERARY }

data class MapMarkerUi(
    val key: String,
    val point: GeoPoint,
    val label: String,
    val occurrences: List<OccurrenceUi>,
    val kind: MapMarkerKind,
    val badgeText: String? = null,
    val isFocused: Boolean = false,
)

fun formatOccurrenceBadge(orders: List<Int>): String = when {
    orders.size <= 3 -> orders.joinToString("·")
    else -> "${orders.first()} +${orders.size - 1}"
}
```

- [ ] **Step 1: Write failing formatter and mapper tests**

Assert exact formatter outputs:

```kotlin
assertEquals("1", formatOccurrenceBadge(listOf(1)))
assertEquals("1·4", formatOccurrenceBadge(listOf(1, 4)))
assertEquals("1·4·7", formatOccurrenceBadge(listOf(1, 4, 7)))
assertEquals("1 +3", formatOccurrenceBadge(listOf(1, 4, 7, 9)))
```

Extend `MapUiModelMapperTest` to assert:

- PLACE_POOL saved marker is `SAVED_PLACE_POOL` with null badge;
- SINGLE_DAY and WHOLE_TRIP marker are `SAVED_ITINERARY` with correct badge;
- unsaved result is `UNSAVED_SEARCH`;
- a focused saved result keeps `place-{savedPlaceId}`, saved kind and badge, with `isFocused = true`;
- same-POI/same-coordinate collision yields one marker and preserves all occurrences.

- [ ] **Step 2: Run RED**

```bash
./gradlew testDebugUnitTest \
  --tests '*OccurrenceBadgeFormatterTest' \
  --tests '*MapUiModelMapperTest'
```

Expected: FAIL because kind/badge/focus fields and formatter do not exist; current mapper rekeys focused saved markers as search markers.

- [ ] **Step 3: Implement minimal mapping**

Build saved markers first and index them by both POI ID and coordinate. Apply search focus by copying `isFocused = true` onto the canonical marker; never change its key or kind. Only add `UNSAVED_SEARCH` when neither POI nor coordinate is already represented.

For itinerary ranges, derive visible orders before coordinate grouping, preserve the complete sorted `occurrences`, then compute `badgeText` with `formatOccurrenceBadge`. PLACE_POOL uses null badge regardless of usage.

Remove `highlightedMarkerKey` as a competing identity source or derive it only at compatibility boundaries; renderer decisions use `marker.isFocused`.

- [ ] **Step 4: Implement composed marker drawing**

Replace `highlightedMarker()`/`numberedMarker()` branching with one function:

```kotlin
private fun markerIcon(marker: MapMarkerUi): BitmapDescriptor
```

It renders:

- ordinary pin for `UNSAVED_SEARCH`;
- collection/favorite glyph without text for `SAVED_PLACE_POOL`;
- the same collection glyph with `badgeText` for `SAVED_ITINERARY`;
- an outer high-contrast focus stroke and larger padding when `isFocused`, without replacing inner glyph/text.

Keep route-label rendering separate. Update the fake host test to record the complete marker model and assert focus retains kind/badge.

- [ ] **Step 5: Run GREEN**

```bash
./gradlew testDebugUnitTest \
  --tests '*OccurrenceBadgeFormatterTest' \
  --tests '*MapUiModelMapperTest' \
  --tests '*MapInteractionReducerTest'
test "$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.amap.AmapComposeMapTest
```

Expected: PASS with stable marker identity and all exact badge strings.

---

### Task 5: 接入高德底图 POI 点击与统一地点卡片

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/AmapComposeMap.kt:79-120,221-265`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt:35-46,148-175`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt:48-60,116-127,241-253`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt:87-132`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/amap/AmapComposeMapTest.kt`
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/MapPoiCollectionFlowTest.kt`

**Interfaces:**
- Consumes: Task 3 `PlacePoolViewModel.toggleCollection`, `pendingCollectionRemoval`; Task 4 marker kind.
- Produces:

```kotlin
data class MapPoiUi(
    val poiId: String?,
    val name: String,
    val address: String,
    val point: GeoPoint,
)

interface AmapMapHost {
    fun render(
        model: MapUiModel,
        layer: MapLayer,
        onMarkerClick: (String) -> Unit,
        onMapPoiClick: (MapPoiUi) -> Unit,
        onLayerError: (Throwable, MapLayer) -> Unit = { _, _ -> },
    )
}

fun TripWorkspaceViewModel.selectMapPoi(poi: MapPoiUi)
fun TripWorkspaceViewModel.dismissPlaceCard()
```

`TripWorkspaceUiState` exposes `selectedMapPoi: MapPoiUi?`; `TripWorkspaceScreen` receives `isPoiSaved`, `onTogglePoiCollection`, busy/error state and the shared confirmation dialog state.

- [ ] **Step 1: Write failing host translation test**

In `AmapComposeMapTest`, make the fake host emit a POI equivalent to ID `B0001`, name `故宫`, snippet/address and coordinates. Assert the UI callback receives exactly:

```kotlin
MapPoiUi("B0001", "故宫", expectedAddress, GeoPoint(39.916, 116.397))
```

Also assert an SDK POI with null ID reaches the card callback but is marked non-collectable.

- [ ] **Step 2: Write failing card-before-mutation test**

`MapPoiCollectionFlowTest` uses a fake host and fake repository counters:

1. emit map POI click;
2. assert card with name/address exists and repository save/delete counters are zero;
3. click card collection action and assert exactly one toggle call;
4. for a saved referenced POI, assert impact dialog appears before delete count changes;
5. dismiss and assert no mutation; reopen and confirm to assert one delete.

- [ ] **Step 3: Run RED**

```bash
test "$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.amap.AmapComposeMapTest,com.yangchengwei.easytrip.workspace.MapPoiCollectionFlowTest
```

Expected: compile/behavior FAIL because `onMapPoiClick`, `MapPoiUi` and the collection card do not exist.

- [ ] **Step 4: Implement SDK translation and place card**

Register `mapView.map.setOnPOIClickListener` in `RealAmapMapHost`; copy values into `MapPoiUi` immediately so SDK objects do not escape. Keep `setOnMarkerClickListener` separate.

On POI click, `TripWorkspaceViewModel.selectMapPoi` stores the POI fields in `SavedStateHandle`, requests a single-point focus, and opens the card. The card uses repository-observed `savedPoiIds` to label its action. It never calls save/delete from the click callback itself. If `poiId == null`, disable the collection action with “无法收藏”. Convert collectable POI to `PlaceCandidate` only when the user presses the action and delegate to Task 3.

For self-owned marker cards, preserve existing occurrence detail and add the same collection action when the marker maps to a saved POI. Do not expose a collection action for route labels.

- [ ] **Step 5: Run GREEN**

```bash
./gradlew testDebugUnitTest --tests '*MapUiModelMapperTest' lintDebug
test "$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.amap.AmapComposeMapTest,com.yangchengwei.easytrip.workspace.MapPoiCollectionFlowTest
```

Expected: PASS; POI click only opens the card, and data mutation happens only after the explicit action/required confirmation.

---

### Task 6: 为地点池增加只读纵向 scrollbar thumb

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/place/ui/LazyScrollbar.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/SavedPlacesContent.kt:19-69`
- Create: `app/src/test/java/com/yangchengwei/easytrip/place/ui/LazyScrollbarGeometryTest.kt`
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlacePoolScrollbarTest.kt`

**Interfaces:**
- Produces:

```kotlin
data class ScrollbarThumb(
    val offsetFraction: Float,
    val sizeFraction: Float,
)

fun calculateScrollbarThumb(
    totalItems: Int,
    visibleItems: Int,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    firstVisibleItemSizePx: Int,
    minimumSizeFraction: Float = 0.08f,
): ScrollbarThumb?

@Composable
fun ReadOnlyLazyScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
)
```

- [ ] **Step 1: Write failing geometry tests**

Cover exact properties:

```kotlin
assertNull(calculateScrollbarThumb(0, 0, 0, 0, 1))
assertNull(calculateScrollbarThumb(5, 5, 0, 0, 40))
assertNull(calculateScrollbarThumb(5, 6, 0, 0, 40))
assertEquals(0f, calculateScrollbarThumb(20, 5, 0, 0, 40)!!.offsetFraction)
assertEquals(0.25f, calculateScrollbarThumb(20, 5, 0, 0, 40)!!.sizeFraction)
assertEquals(1f, calculateScrollbarThumb(20, 5, 15, 0, 40)!!.offsetFraction)
```

Add a mid-item offset case and assert both fractions stay in `0f..1f`.

- [ ] **Step 2: Run JVM RED**

```bash
./gradlew testDebugUnitTest --tests '*LazyScrollbarGeometryTest'
```

Expected: FAIL because geometry and composable do not exist.

- [ ] **Step 3: Implement the pure geometry and overlay**

`calculateScrollbarThumb` returns null when all items fit. Otherwise calculate size from `visibleItems / totalItems`, clamp to the minimum, estimate fractional item progress from offset/item size, and normalize against `totalItems - visibleItems`.

Wrap the existing `LazyColumn` in a `Box`; create/accept one `LazyListState`, pass it to the list, and align `ReadOnlyLazyScrollbar` to `Alignment.CenterEnd`. Draw a narrow rounded thumb tagged `place-pool-scrollbar-thumb`. Do not attach `clickable`, `draggable`, `pointerInput` or scroll semantics to the thumb.

- [ ] **Step 4: Write UI visibility and non-interaction test**

`PlacePoolScrollbarTest` renders 2 items in a tall viewport and asserts no thumb, then 40 items in a short viewport and asserts one thumb. Swipe the list to confirm thumb position changes. Attempt a swipe on the thumb and assert the list index does not change as a result of direct thumb dragging.

- [ ] **Step 5: Run GREEN**

```bash
./gradlew testDebugUnitTest --tests '*LazyScrollbarGeometryTest'
test "$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlacePoolScrollbarTest
```

Expected: PASS; thumb only appears for overflowing content and cannot drive list scrolling.

---

### Task 7: 集成验收、状态恢复与安全回归

**Files:**
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/V2AcceptanceTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/SearchMapFocusTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlacePoolFlowTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/CascadeDeleteTest.kt`
- Verify unchanged: `app/schemas/**`

**Interfaces:**
- Consumes: all Task 1-6 interfaces.
- Produces: one continuous acceptance flow and a verified no-schema/no-secret/no-regression result; no production API is added.

- [ ] **Step 1: Write the failing integrated acceptance flow**

Update `V2AcceptanceTest` to execute, in order:

1. open a trip and assert current top bar/layer menu/map scope visual changes still exist;
2. assert only Places/Itinerary drawer Tabs exist;
3. assert the map-right search launcher is non-editable, icon-only, white, 40dp high and approximately 20% of wide test viewport;
4. navigate to search, query, and save an unused result;
5. click “已收藏” and assert immediate removal without dialog;
6. save again, add it to an itinerary, click “已收藏”, dismiss impact confirmation and assert no rows change;
7. confirm and assert place occurrence/route changes follow existing cascade semantics;
8. select another result and assert search clears, navigation pops, workspace focuses/highlights exactly once;
9. recreate the workspace from the same `SavedStateHandle` and assert no old selection replay; legacy `workspace.tab=SEARCH` restores as Places;
10. emit a bottom-map POI click, assert card-before-mutation, then save from the card;
11. switch PLACE_POOL/SINGLE_DAY/WHOLE_TRIP and assert saved icon, exact duplicate badges and focus outline preserve identity;
12. render short/long place pools and assert scrollbar hidden/visible and non-draggable.

Use fake AMap host/repositories where SDK behavior is not under test, real in-memory Room for cascade checks, Compose idling and coroutine virtual time; do not add sleeps.

- [ ] **Step 2: Run focused RED**

```bash
./gradlew testDebugUnitTest \
  --tests '*WorkspaceTabRestorationTest' \
  --tests '*PlaceSearchReducerTest' \
  --tests '*SearchSelectionConsumptionTest' \
  --tests '*CollectionTogglePolicyTest' \
  --tests '*OccurrenceBadgeFormatterTest' \
  --tests '*MapUiModelMapperTest' \
  --tests '*LazyScrollbarGeometryTest'
test "$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V2AcceptanceTest
```

Expected before final wiring: JVM tests PASS; acceptance FAIL at the first missing integration edge, not from emulator identity or arbitrary timeout.

- [ ] **Step 3: Apply only missing integration wiring**

Connect the already-tested Task 1-6 callbacks/state at `AppNavigation`; remove obsolete `onSearchQueryChanged`/SEARCH-tab branches and update old tests that intentionally assert the superseded three-tab behavior. Do not rewrite current visual components, map layers, sheet gestures, route timeline or adaptive icon. Do not alter Room files or schemas.

- [ ] **Step 4: Run complete non-device verification**

```bash
./gradlew clean test lint assembleDebug
git diff --check
```

Expected: exit 0; debug APK exists; no whitespace errors.

- [ ] **Step 5: Prove Room schema and secrets are unchanged**

Run:

```bash
git diff --exit-code -- app/schemas app/src/main/java/com/yangchengwei/easytrip/core/database app/src/main/java/com/yangchengwei/easytrip/place/data/PlaceEntities.kt app/src/main/java/com/yangchengwei/easytrip/itinerary/data/ItineraryEntity.kt app/src/main/java/com/yangchengwei/easytrip/route/data/RouteLegEntity.kt app/src/main/java/com/yangchengwei/easytrip/trip/data/TripEntities.kt
git diff -- . ':!local.properties' | grep -E 'AMAP_API_KEY|api[_-]?key|secret|token' && exit 1 || true
```

Expected: first command exits 0; second finds no added credential value. Do not display `local.properties`.

- [ ] **Step 6: Run full instrumentation only on the required AVD**

```bash
AVD_NAME="$(adb -s "$ANDROID_SERIAL" emu avd name | tr -d '\r')"
test "$AVD_NAME" = "easy_trip_p60pro"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew connectedDebugAndroidTest
```

Expected: identity assertion succeeds exactly and all instrumentation passes. If identity differs or `ANDROID_SERIAL` is unset, stop and report instrumentation as not run; never substitute another emulator or physical device.

- [ ] **Step 7: Final working-tree review without commit**

```bash
git status --short
git diff --stat
git diff --check
```

Expected: only intended source/test/doc changes plus pre-existing visual modifications are present; no rollback, schema change, key, commit or push. Review the final diff against every acceptance criterion in the spec and report any unverified AMap visual behavior explicitly.
