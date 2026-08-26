# Easy Trip 搜索与地点闭环 UI 收敛实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有独立搜索 destination 内实现搜索结果与地图半屏详情切换，并完成统一地点详情、标签编辑、精确删除影响、地点池 `＋/···` 和单地点加入行程闭环。

**Architecture:** `PlaceSearchViewModel` 继续拥有搜索、详情模式、收藏、编辑和确认状态，`PlaceSearchReducer` 只负责查询状态；搜索页面复用现有 AMap host/lifecycle，构造只含当前候选的最小地图模型。搜索与地点池复用无业务所有权的 `PlaceDetailPanel`；删除只新增一致快照影响查询，写操作仍唯一调用现有原子删除；单地点加入只为现有 `AddToItineraryViewModel` 增加入口。

**Tech Stack:** Kotlin、Jetpack Compose、Material 3、Navigation Compose、SavedStateHandle、Room、StateFlow、高德 Android SDK、JUnit 4、Compose UI Test。

**Spec:** `docs/superpowers/specs/2026-08-26-easy-trip-search-place-loop-ui-convergence-design.md`

## Global Constraints

- 搜索地图详情属于现有 `TRIP_SEARCH_ROUTE` 内部模式，不新增导航 destination，不提升到 `TripWorkspaceViewModel`，不复用工作台三档 Sheet 状态。
- 结果整行打开详情；收藏按钮独立操作且不冒泡；收藏后停留搜索流程。
- 搜索列表和详情均不得提供加入行程；单地点加入只位于地点池 `＋`。
- 地图只显示当前候选，允许缩放和平移；其他 POI/marker 点击不得切换详情；普通重组不得重置用户相机。
- 复用现有 AMap host、lifecycle、privacy consent 和 callback guard，不复制 MapView 生命周期实现。
- 未收藏 candidate 没有 SavedPlace 记录，只能看基础信息和收藏；收藏成功后才能编辑备注/标签。
- 详情编辑、收藏、取消收藏、删除的业务状态由 ViewModel 持有；Composable 只持有 LazyListState、地图临时相机、菜单 Boolean 和输入焦点。
- 标签 trim 后校验；单地点最多 8 个；CJK 12 字、普通字符 24 个；emoji 按 Unicode code point 计 1。
- UI trim-only；Room 保留现有 NFKC + case-fold canonical identity，避免无迁移改变已有标签身份，同时在事务边界执行数量/长度防御。
- 删除影响使用同一 Room transaction 读取 item 数和唯一 incident RouteLeg 数；删除写路径仍唯一使用 `deletePlaceAndReferences(placeId)`。
- 地点池 `＋` 只选择当前 SavedPlace，并直接进入 SELECT_TARGET_DAY；批量入口继续进入 SELECT_PLACES。
- 地点池 `···` 只含编辑和删除，行内仅保存 Popup 展开 Boolean。
- 系统 Back 与顶部 Back 使用同一 ViewModel priority；提交中锁定返回/关闭；搜索返回 payload 仅在真正退出 Results 时发布一次。
- `recentlyCollectedPoiIds` 是会话提示，不要求进程死亡恢复；恢复后以 Repository 收藏状态为准。
- 不修改 SavedPlace/ItineraryItem 身份关系、工作台壳层、行程时间线、旅行设置或 Room schema。
- 不处理极端小窗、横屏、分屏和 2× 字体；真机验收统一留到候选批次并标 `DEFERRED`。
- 严格 TDD；connected tests、ADB、模拟器和真机全局互斥；基础设施失败最多重试一次。
- 修改后运行 `graphify update .`；不触碰未跟踪 `diagrams/`；未获授权不得 push。

---

## File Structure

### 新建

- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceDetailPanel.kt`：搜索和地点池复用的无状态详情面板。
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceTagValidation.kt`：标签单位、规范化和结构化校验。
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/SearchDetailMapModel.kt`：候选到最小 `MapUiModel` 的纯映射。
- `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceDetailPanelTest.kt`：来源能力、编辑态、IME、busy 和可访问性。
- `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlaceTagValidationTest.kt`：标签纯函数边界。
- `docs/testing/search-place-ui-acceptance.md`：候选自动化及统一真机清单。
- `docs/testing/search-place-ui-conflicts.md`：实施裁决和剩余差异。

### 修改

- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModel.kt`：Results/MapDetail、saved map、详情草稿、返回优先级、相机 request id。
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt`：统一 BackHandler、map host/consent 注入和一次性退出。
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContent.kt`：结果整行、地图半屏详情、列表位置恢复和状态视觉。
- `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`：向 search route 传入 consent/map host factory；保持现有 destination。
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt`：统一详情 edit state、影响查询 generation 和 panel action。
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt`：详情 panel、quick add 和菜单 action wiring。
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/SavedPlaceRow.kt`：`＋`/`···` 固定操作区。
- `app/src/main/java/com/yangchengwei/easytrip/place/domain/SavedPlaceRepository.kt`：`PlaceDeletionImpact` 只读契约及标签防御约束。
- `app/src/main/java/com/yangchengwei/easytrip/place/domain/PlaceService.kt`：统一影响查询委托。
- `app/src/main/java/com/yangchengwei/easytrip/place/data/PlaceDao.kt`：item/incident leg 统计查询。
- `app/src/main/java/com/yangchengwei/easytrip/place/data/RoomSavedPlaceRepository.kt`：事务一致快照和标签最终防御。
- `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/AddToItineraryViewModel.kt`：`startForPlace(placeId)`。
- `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt`：`StartAddSingle(placeId)`。
- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceRoute.kt`：复用单地点加入 coordinator。
- 相关 JVM/Room/Compose/Navigation/V1 scenario tests。

### 原则上不修改

- `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceScaffold.kt`
- `app/src/main/java/com/yangchengwei/easytrip/workspace/AmapComposeMap.kt`，除非 focused test 证明既有注入接口不足。
- Room entity 和 schema。
- `AddPlacesToDayUseCase`、Undo use case 和提交状态机。

---

### Task 1: 建立 Results / MapDetail 状态和统一返回优先级

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModel.kt:19-171`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt:12-55`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModelTest.kt`

**Interfaces:**
- Consumes: 现有 `PlaceSearchReducer`、`SavedStateHandle`、Repository 收藏 Flow。
- Produces: `SearchDisplayMode`、`OpenDetail`、统一 `Back` decision、可恢复 selected poiId。

- [ ] **Step 1: 写模式与返回失败测试**

```kotlin
@Test fun openDetailKeepsQueryAndResultsAndStoresPoiId()
@Test fun openDetailRejectsPoiOutsideCurrentResults()
@Test fun restoredDetailWaitsForSearchTerminalStateBeforeValidation()
@Test fun restoredMissingPoiFallsBackToResults()
@Test fun backDismissesRemovalBeforeClosingDetail()
@Test fun backCancelsEditBeforeClosingDetail()
@Test fun backFromDetailReturnsToResults()
@Test fun backFromResultsRequestsDestinationExit()
@Test fun backIsIgnoredWhileDetailMutationIsSubmitting()
```

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest
```

预期：`SearchDisplayMode`/`OpenDetail`/详情返回不存在。

- [ ] **Step 3: 定义状态和 SavedState keys**

```kotlin
sealed interface SearchDisplayMode {
    data object Results : SearchDisplayMode
    data class MapDetail(val poiId: String) : SearchDisplayMode
}
```

在 `PlaceSearchUiState` 增加：

```kotlin
val displayMode: SearchDisplayMode = SearchDisplayMode.Results,
val savedPlacesByPoiId: Map<String, SavedPlace> = emptyMap(),
val detailDraft: PlaceDetailEditState? = null,
```

SavedState 保存 mode 名和 poiId，不保存 candidate。

- [ ] **Step 4: 实现 OpenDetail 与恢复校验**

- 只接受当前 `search.results` 内 poiId。
- query/results 不变。
- 恢复 query 尚在 Initial/Loading 时不清理 detail。
- 进入 Empty/NetworkFailure/Results 终态且找不到 poiId 时退回 Results。

- [ ] **Step 5: 实现统一 Back dispatcher**

固定优先级：确认 → 取消编辑 → detail 回 results → destination exit；mutation busy 时忽略。

`PlaceSearchRoute` 的顶部和系统返回都 dispatch `Back`，只在 state 请求真正退出时调用外部 `onBack`。

- [ ] **Step 6: 运行 GREEN 与 reducer 回归**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchReducerTest
```

- [ ] **Step 7: Graphify 和建议提交**

```bash
graphify update .
```

建议提交：`Add search detail display state`。

---

### Task 2: 搜索结果整行打开详情并恢复列表位置

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContentTest.kt`

**Interfaces:**
- Consumes: Task 1 `SearchDisplayMode`/`OpenDetail`。
- Produces: 独立整行与收藏点击、可保存 `LazyListState`、detail content slot。

- [ ] **Step 1: 写结果行和恢复失败测试**

```kotlin
@Test fun resultRowOpensDetailWhileBookmarkOnlyTogglesCollection()
@Test fun detailBackRestoresQueryResultsAndListPosition()
@Test fun candidateWithoutCoordinatesCanOpenDetailWithoutMapFocus()
@Test fun resultAndDetailExposeNoAddToItineraryAction()
@Test fun rowAndBookmarkHaveIndependentButtonSemantics()
```

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest
```

- [ ] **Step 3: 拆分 SearchResultRow 点击**

```kotlin
@Composable
private fun SearchResultRow(
    candidate: PlaceCandidate,
    saved: Boolean,
    busy: Boolean,
    onOpenDetail: () -> Unit,
    onToggleCollection: () -> Unit,
)
```

整行使用“查看 <地点> 详情”Button 语义；收藏按钮独立 48dp 并消费点击。

- [ ] **Step 4: 持有可保存列表状态**

在 route：

```kotlin
val listState = rememberSaveable(saver = LazyListState.Saver) {
    LazyListState()
}
```

传入 content；Results/MapDetail 切换不重建该 state。

- [ ] **Step 5: 增加 detail slot**

```kotlin
@Composable
fun PlaceSearchContent(
    state: PlaceSearchUiState,
    onAction: (PlaceSearchAction) -> Unit,
    modifier: Modifier = Modifier,
    resultsListState: LazyListState,
    detailContent: @Composable (PlaceCandidate) -> Unit,
)
```

无坐标 candidate 仍进入 detail slot。

- [ ] **Step 6: 运行 GREEN**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest
```

- [ ] **Step 7: 静态门禁和建议提交**

```bash
./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
graphify update .
```

建议提交：`Open search details from result rows`。

---

### Task 3: 接入搜索详情 AMap 和最小地图模型

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/place/ui/SearchDetailMapModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt:326-355`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/amap/AmapComposeMapTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContentTest.kt`

**Interfaces:**
- Consumes: `AmapComposeMap`、`AmapConsentToken`、`AmapMapHost`、Task 1 selected candidate。
- Produces: `searchDetailMapModel(candidate, requestId)`、`RecenterDetail`、搜索详情地图区域。

- [ ] **Step 1: 写纯 mapper 和相机 request 测试**

```kotlin
@Test fun selectedCandidateMapsToExactlyOneSearchMarker()
@Test fun candidateWithoutCoordinatesProducesNoMapModel()
@Test fun ordinaryDetailStateChangesKeepViewportRequestId()
@Test fun recenterIncrementsRequestId()
```

- [ ] **Step 2: 运行 JVM RED**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.SearchDetailMapModelTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest
```

- [ ] **Step 3: 实现最小 MapUiModel**

- 一个 `UNSAVED_SEARCH` marker。
- 无路线、无其他 marker。
- 一个 `ViewportReason.SEARCH_FOCUS` 请求。
- 普通重组不增加 id；`RecenterDetail` 才增加。

- [ ] **Step 4: 写 host 生命周期和 no-op 点击测试**

```kotlin
@Test fun searchDetailUsesExistingMapHostLifecycle()
@Test fun replacingHostDisposesPreviousHost()
@Test fun markerAndPoiClicksDoNotChangeSelectedDetail()
@Test fun mapFailureLeavesDetailActionsEnabled()
```

- [ ] **Step 5: 注入现有地图 host**

`PlaceSearchRoute` 增加：

```kotlin
consent: AmapConsentToken? = null,
mapHostFactory: (Context) -> AmapMapHost = ::RealAmapMapHost,
```

调用现有 `AmapComposeMap`，marker/POI click 为 no-op，详情面板继续渲染。

- [ ] **Step 6: AppNavigation 传入边界依赖**

从 application/dependencies 获取与工作台一致的 consent 和 factory，不创建第二 lifecycle controller。

- [ ] **Step 7: 运行 GREEN**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.SearchDetailMapModelTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.amap.AmapComposeMapTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest
```

- [ ] **Step 8: Graphify 与建议提交**

```bash
graphify update .
```

建议提交：`Add map preview to search details`。

---

### Task 4: 建立共享 PlaceDetailPanel

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceDetailPanel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceDetailContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt`
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceDetailPanelTest.kt`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModelTest.kt`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModelTest.kt`

**Interfaces:**
- Consumes: candidate、可选 SavedPlace、可选 edit state、来源能力矩阵。
- Produces: `PlaceDetailSource`、`PlaceDetailEditState`、`PlaceDetailPanelAction`、无状态 `PlaceDetailPanel`。

- [ ] **Step 1: 写来源能力失败测试**

```kotlin
@Test fun searchUnsavedShowsCollectionButNoEditDeleteOrAdd()
@Test fun searchSavedShowsCollectionAndEditButNoAdd()
@Test fun placePoolShowsEditAndDeleteButNoCollectionOrAdd()
@Test fun readOnlyShowsAddressNoteAndTagFallbacks()
@Test fun savingDisablesInputsSaveCancelAndDismiss()
@Test fun panelTitleHasHeadingSemantics()
```

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceDetailPanelTest
```

- [ ] **Step 3: 定义 panel 模型与 action**

按规格定义 `PlaceDetailSource`、`PlaceDetailEditState`、`PlaceDetailPanelAction`。Panel 不持有 repository 或业务草稿。

- [ ] **Step 4: 实现来源能力矩阵**

- SEARCH unsaved：收藏。
- SEARCH saved：收藏/取消、编辑。
- PLACE_POOL：编辑、删除。
- 所有来源：无加入行程。
- 地址、备注、标签有明确 fallback。
- 编辑态原位替换只读内容；IME 下操作可达。

- [ ] **Step 5: 迁移旧详情组件**

将 `PlaceDetailContent`/`EditSavedPlaceDialog` 调用迁移到 panel；删除重复草稿解析，但保留兼容 wrapper 仅在仍有调用时使用，最终只有一个真实详情 UI。

- [ ] **Step 6: 补 ViewModel 草稿边界测试**

```kotlin
@Test fun editingSavedPlaceCreatesDraftBoundToPlaceId()
@Test fun unsavedCandidateCannotStartEdit()
@Test fun cancelEditDiscardsDraftWithoutRepositoryCall()
```

- [ ] **Step 7: 运行 GREEN**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceDetailPanelTest

./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.PlacePoolViewModelTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest
```

- [ ] **Step 8: Graphify 和建议提交**

```bash
graphify update .
```

建议提交：`Unify place detail panels`。

---

### Task 5: 实现标签校验与保存恢复

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceTagValidation.kt`
- Create: `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlaceTagValidationTest.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceDetailPanel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/data/RoomSavedPlaceRepository.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/place/data/RoomSavedPlaceRepositoryTest.kt`

**Interfaces:**
- Consumes: Task 4 edit state 和现有 `updateDetails`。
- Produces: tag normalization/units/validation、8 标签限制、失败保留与 stale completion 防护。

- [ ] **Step 1: 写标签纯函数失败测试**

覆盖：trim、空、重复、12/13 CJK、24/25 Latin、emoji code point、8/9 标签和满额取消。

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceTagValidationTest
```

- [ ] **Step 3: 实现纯函数**

```kotlin
internal fun normalizePlaceTagName(raw: String): String = raw.trim()
internal fun placeTagUnits(value: String): Int
internal fun validatePlaceTag(raw: String, selectedTagNames: Set<String>): PlaceTagValidation
```

CJK script 计 2，其他 code point 计 1；上限 24 units、8 个标签。

- [ ] **Step 4: 接入 ViewModel 草稿**

- 创建成功自动选中并清空 input。
- 满额仍允许取消。
- 保存失败保留 note、tags、new input。
- placeId + generation 防止旧 completion 关闭新详情。
- `CancellationException` 重抛。

- [ ] **Step 5: Room 最终防御**

保留 NFKC/case-fold identity；事务边界再次验证非空、24 units、最多 8 个，并继续 cross-ref 原子更新和孤儿清理。

- [ ] **Step 6: 运行 GREEN**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceTagValidationTest \
  --tests com.yangchengwei.easytrip.place.ui.PlacePoolViewModelTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest
```

- [ ] **Step 7: Graphify 与建议提交**

建议提交：`Validate and persist place tags`。

---

### Task 6: 增加精确 PlaceDeletionImpact

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/domain/SavedPlaceRepository.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/domain/PlaceService.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/data/PlaceDao.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/data/RoomSavedPlaceRepository.kt`
- Modify all fake repository implementations in tests.
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModel.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/place/data/RoomSavedPlaceRepositoryTest.kt`

**Interfaces:**
- Produces: `PlaceDeletionImpact(itemCount, routeLegCount)` 和统一确认流程。

- [ ] **Step 1: 写 Room 影响统计失败测试**

覆盖：零影响、多 occurrence、distinct incident legs、双端同 leg 不重复、只读无 mutation。

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest
```

- [ ] **Step 3: 增加契约和 DAO 查询**

```kotlin
data class PlaceDeletionImpact(val itineraryItemCount: Int, val routeLegCount: Int)
suspend fun deletionImpact(placeId: String): PlaceDeletionImpact
```

DAO 使用 item count 和 `COUNT(DISTINCT leg.id)` incident leg 查询。

- [ ] **Step 4: Room 单事务快照**

使用 `withTransaction` 同时读取两个数量，不改 schema。删除继续用现有事务。

- [ ] **Step 5: 写 ViewModel 并发/确认失败测试**

覆盖：零影响直接删除、非零显示精确数量、取消无副作用、重复确认一次、stale 影响不回写、删除失败保留确认。

- [ ] **Step 6: 迁移删除决策**

删除决策只用 `deletionImpact`；`usageCount` 保留给列表安排次数。确认文案分别显示 item/leg 数量。

- [ ] **Step 7: 运行 GREEN**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest

./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.place.ui.PlacePoolViewModelTest \
  --tests com.yangchengwei.easytrip.place.ui.PlaceSearchViewModelTest
```

- [ ] **Step 8: Graphify 与建议提交**

建议提交：`Report precise place deletion impact`。

---

### Task 7: 为单地点加入行程增加 startForPlace

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/AddToItineraryViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt`
- Modify: `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/AddToItineraryStateTest.kt`

**Interfaces:**
- Produces: `startForPlace(placeId)` 和 `PlacePoolAction.StartAddSingle(placeId)`。

- [ ] **Step 1: 写入口失败测试**

覆盖：只选当前地点、直接目标日、FromPlacePool、无效地点、清旧 result/error、无日期不创建、批量入口不变、现有 submit/undo。

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStateTest
```

- [ ] **Step 3: 实现 startForPlace**

```kotlin
fun startForPlace(placeId: String)
```

要求最新 validity 包含 placeId；状态使用 `listOf(placeId)`，targetDayId null，step SELECT_TARGET_DAY。

- [ ] **Step 4: 增加地点池 action 映射**

```kotlin
data class StartAddSingle(val placeId: String) : PlacePoolAction
```

仅发 action，不在 row 直接访问 AddToItineraryViewModel。

- [ ] **Step 5: 运行 GREEN 和回归**

```bash
./gradlew :app:testDebugUnitTest \
  --tests com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStateTest
```

- [ ] **Step 6: Graphify 与建议提交**

建议提交：`Start single-place itinerary additions`。

---

### Task 8: 地点池行改为 ＋ 和 ··· 并接入现有 coordinator

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/SavedPlaceRow.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceRoute.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlacePoolFlowTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`

**Interfaces:**
- Consumes: Task 7 StartAddSingle/startForPlace。
- Produces: 固定 `＋/···` row、单地点目标日 wiring、批量回归。

- [ ] **Step 1: 写行与 wiring 失败测试**

覆盖 quick add 当前 id、菜单仅 edit/delete、menu 绑定当前行、批量不变、长文案 bounds、描述、无日引导。

- [ ] **Step 2: 运行 RED**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest
```

- [ ] **Step 3: 实现 40dp `＋/···`**

- 固定图标 20–22dp。
- 名称/地址最多两行。
- 菜单 Boolean 仅行内。
- `＋` 描述和 `···` 描述包含地点名。
- 菜单仅 edit/delete，删除 danger 色。

- [ ] **Step 4: 接入 workspace coordinator**

`StartAddSingle(placeId)` 调用 `addViewModel.startForPlace(placeId)`，打开现有 target-day overlay；无效地点不打开。批量 action 继续 `startFromPool()`。

- [ ] **Step 5: 修复公开 wrapper 的 no-op**

`PlacePoolSheet(viewModel...)` 显式接收 add callbacks，或在无 coordinator 时隐藏入口；不得保留可点击无效果按钮。

- [ ] **Step 6: 运行 GREEN**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest
```

- [ ] **Step 7: Graphify 与建议提交**

建议提交：`Add quick place itinerary actions`。

---

### Task 9: 统一返回、一次性发布与候选门禁

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- Modify focused navigation tests.
- Modify only for stable selector migration: `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioExecutable.kt`
- Create: `docs/testing/search-place-ui-acceptance.md`
- Create: `docs/testing/search-place-ui-conflicts.md`

**Interfaces:**
- Consumes: Tasks 1–8 完整闭环。
- Produces: 最终返回契约、候选自动化记录与统一真机清单。

- [ ] **Step 1: 写返回/发布失败测试**

覆盖顶部和系统 Back 同优先级、edit→detail→results→workspace、detail 返回不发布、results 退出发布一次、恢复不重发旧收藏、mutation busy 锁定。

- [ ] **Step 2: 实现统一 route effect**

BackHandler 只 dispatch ViewModel Back。只有 ViewModel 请求退出 Results 时，route 调用一次外部 `onBack` 并消费 effect。不得保留 bool/effect 双轨。

- [ ] **Step 3: 运行 focused JVM 和 Compose**

```bash
./gradlew :app:testDebugUnitTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceDetailPanelTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.data.RoomSavedPlaceRepositoryTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.WorkspaceSearchReturnNavEntryTest
```

- [ ] **Step 4: 运行静态门禁**

```bash
./gradlew :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

- [ ] **Step 5: 运行 47 场景候选门禁**

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1ScenarioCatalogTest

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1FullUiAcceptanceTest
```

要求 47/47；0 tests 不算通过；selector 迁移不得删除业务断言。

- [ ] **Step 6: 写验收和冲突记录**

记录自动化精确数量、真机 DEFERRED、标签 identity 裁决、无坐标候选、双加入入口、删除影响计数等。

- [ ] **Step 7: Graphify 与最终检查**

```bash
graphify update .
git diff --check
git status --short
```

- [ ] **Step 8: 建议候选提交**

```bash
git add app/src/main app/src/test app/src/androidTest \
  docs/testing/search-place-ui-acceptance.md \
  docs/testing/search-place-ui-conflicts.md \
  graphify-out
git commit -m "Converge search and place UI" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

不加入 `diagrams/`。

---

## Execution Notes

- Tasks 1–9 顺序执行；多个任务修改 PlaceSearchViewModel/Content，不并行实现。
- Task 3 地图测试、Task 6 Room 测试和 Task 9 全量设备门禁必须独占设备。
- 每个任务使用新的实现 Agent，并经过任务级规格与质量审查。
- 若 3 次修复仍无法稳定地图详情 host，停止补丁式修复，重新评估是否需要提升搜索地图为共享 workspace 范围；未到该条件前不改变已确认架构。
- 真机人工验收继续统一延期，候选文档写 DEFERRED。
