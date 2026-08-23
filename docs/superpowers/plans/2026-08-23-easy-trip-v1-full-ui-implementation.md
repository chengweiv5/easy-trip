# Easy Trip v1.0 Full UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在保留现有数据层与地图能力的前提下，按五个可运行用户闭环完成 `easy-trip-v1.0.pen` 在 `01–48` 编号范围内除已废弃 `05` 外的 47 个已采纳画面、相关状态变体、7 条用户旅程和 6 组状态矩阵。

**Architecture:** 采用垂直闭环增量迁移。独立页面使用 Navigation Compose 路由，工作台抽屉、弹层、权限说明和异步状态由统一状态协调；Route 连接 ViewModel、导航与系统能力，Content 只渲染不可变状态并发送 Action。每批通过自动化、真机和 Pencil 截图门禁后才进入下一批。

**Tech Stack:** Kotlin、Jetpack Compose、Material 3、Navigation Compose、ViewModel、StateFlow、SavedStateHandle、Room、高德 Android SDK、JUnit、kotlinx-coroutines-test、Compose UI Test。

**Spec:** `docs/superpowers/specs/2026-08-23-easy-trip-v1-full-ui-implementation-design.md`

## Global Constraints

- 产品与交互以 `design/easy-trip-design-formula.md` 为最高优先级。
- 视觉只以 `design/easy-trip-v1.0.pen` 为基线；忽略 `design/easy-trip-v1.1.pen`。
- 不修改 Room schema，不引入新 DI，不重写 Repository、Service 或高德地图适配层。
- Room 是稳定事实来源；网络和地图只做渐进增强。
- `SavedPlace` 与 `ItineraryItem` 是不同实体；同一地点允许重复加入行程。
- RouteLeg 只连接同一 TripDay 内相邻 ItineraryItem，跨天不生成 RouteLeg。
- 搜索结果只收藏；地点详情只管理收藏、备注和标签；加入行程只能从地点池发起。
- 新增旅行日只能从左侧天导航末尾“添加”发起；设置页不得提供“添加一天”。
- 不绘制 Android 系统状态栏、导航栏和系统权限弹窗；使用 WindowInsets。
- 每个生产行为先写失败测试，确认 RED 后再写最小实现。
- 每批必须通过 JVM、Compose、lint、构建、真机流程与逐屏截图门禁。
- 真机不可用时不得声称批次完成，也不得进入下一批。
- 每批完成后运行 `graphify update .`，只提交正式图谱产物，不提交缓存。
- 不主动提交或推送，除非用户明确要求。

---

## Target Navigation

```kotlin
const val TRIP_LIST_ROUTE = "trips"
const val CREATE_TRIP_ROUTE = "trips/create"
const val TRIP_WORKSPACE_ROUTE = "trips/{tripId}/workspace"
const val TRIP_SEARCH_ROUTE = "trips/{tripId}/search"
const val TRIP_SETTINGS_ROUTE = "trips/{tripId}/settings"

fun tripWorkspaceRoute(tripId: Long) = "trips/$tripId/workspace"
fun tripSearchRoute(tripId: Long) = "trips/$tripId/search"
fun tripSettingsRoute(tripId: Long) = "trips/$tripId/settings"
```

迁移时可为旧 `trips/{tripId}` 保留内部重定向；闭环回归通过后删除旧路径。

---

# Batch 1：旅行入口、创建与删除

## Task 1：将创建旅行迁移为独立页面

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripRoute.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripContent.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListScreen.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripUiState.kt`
- Delete after green: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/CreateTripDialog.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/CreateTripViewModelTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/CreateTripValidatorTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/CreateTripContentTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripFlowTest.kt`

**Interfaces:**

```kotlin
sealed interface CreateTripAction {
    data object Back : CreateTripAction
    data class NameChanged(val value: String) : CreateTripAction
    data class DayCountChanged(val value: String) : CreateTripAction
    data class TimeModeChanged(val value: CreateTimeMode) : CreateTripAction
    data class StartDateChanged(val value: LocalDate?) : CreateTripAction
    data class TravelModeChanged(val value: TravelMode) : CreateTripAction
    data object Submit : CreateTripAction
}

sealed interface CreateTripEffect {
    data object NavigateBack : CreateTripEffect
    data class OpenWorkspace(val tripId: Long) : CreateTripEffect
}

@Composable
fun CreateTripRoute(
    onBack: () -> Unit,
    onOpenWorkspace: (Long) -> Unit,
    viewModel: CreateTripViewModel,
)

@Composable
fun CreateTripContent(
    state: CreateTripUiState,
    onAction: (CreateTripAction) -> Unit,
    modifier: Modifier = Modifier,
)
```

**Pencil:** `dzhkC`、`yIGiQ`、旅程 A `RqVLv`。

- [ ] **Step 1: 写创建状态机失败测试**

```kotlin
@Test fun blankNameDoesNotCreateTrip()
@Test fun repeatedSubmitWhileSavingCreatesOnlyOnce()
@Test fun failureKeepsDraftAndAllowsRetry()
@Test fun successEmitsOpenWorkspaceOnlyOnce()
@Test fun savedStateRestoresDraftFields()
```

- [ ] **Step 2: 运行测试并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.trip.ui.CreateTripViewModelTest"
```

预期：`CreateTripViewModel` 或对应 Effect 尚不存在，或旧列表 ViewModel 仍持有创建状态。

- [ ] **Step 3: 最小迁移状态与 Effect**

将现有校验、防重、失败保留、SavedStateHandle 和创建调用迁移到独立 ViewModel；不修改 TripService/Repository 语义。

- [ ] **Step 4: 写创建页 Compose 失败测试**

```kotlin
@Test fun showsFullPageTitleFieldsPlanningTipAndSubmit()
@Test fun validationErrorsRemainVisibleUntilCorrected()
@Test fun keyboardDoesNotHideSubmitAction()
```

- [ ] **Step 5: 运行 Compose 测试并确认 RED**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.CreateTripContentTest
```

- [ ] **Step 6: 实现全页 CreateTripContent 和新路由**

按 `dzhkC` 信息顺序实现标题、步骤提示、名称、日期、方式、Planning Tip 和主按钮；使用 `safeDrawingPadding()` 与 `imePadding()`，不绘制状态栏。

- [ ] **Step 7: 删除旧 Dialog 路径并跑绿灯**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.trip.ui.CreateTrip*"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.CreateTripContentTest
```

- [ ] **Step 8: 提交边界**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt \
  app/src/main/java/com/yangchengwei/easytrip/trip/ui \
  app/src/test/java/com/yangchengwei/easytrip/trip/ui \
  app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui
git commit -m "Move trip creation to a full-page flow"
```

## Task 2：校正旅行列表和删除影响

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EmptyState.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/InlineStatus.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/core/ui/component/ConfirmationDialog.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListScreen.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripListViewModel.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripListViewModelTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripListContentTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripFlowTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/RoomDeleteImpactProviderTest.kt`

**Interfaces:**

```kotlin
data class ConfirmationUiModel(
    val title: String,
    val message: String,
    val deletedItems: List<String>,
    val retainedItems: List<String>,
    val confirmLabel: String,
    val dismissLabel: String,
    val destructive: Boolean,
    val reversible: Boolean,
)

sealed interface TripListEffect {
    data object OpenCreateTrip : TripListEffect
    data class OpenWorkspace(val tripId: Long) : TripListEffect
    data class OpenSettings(val tripId: Long) : TripListEffect
}
```

**Pencil:** `K9h3r`、`zIbEu`、`oW9mK`、`d1sTtb`、状态组 A `hVIMZ`。

- [ ] **Step 1: 写列表状态与删除失败测试**

```kotlin
@Test fun emptyListShowsSingleCreateAction()
@Test fun contentMapsPrimaryAndOtherTripsWithoutStaleCards()
@Test fun deletePreviewListsAffectedAndRetainedData()
@Test fun cancellingDeleteDoesNotCallRepository()
@Test fun confirmingDeleteCallsRepositoryOnlyOnce()
```

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.trip.ui.TripList*"
```

- [ ] **Step 3: 最小实现状态映射和删除预览**

复用现有 DeleteImpactProvider，不新增数据库查询；删除后完全依赖 Room Flow 刷新。

- [ ] **Step 4: 写视觉结构 Compose 测试并确认 RED**

```kotlin
@Test fun titleUsesDisplaySizeAndTripCardsExposeActions()
@Test fun destructiveDialogExplainsDeletionAndIrreversibility()
@Test fun longTripNameAndLargeFontRemainScrollable()
```

- [ ] **Step 5: 校正列表视觉并实现共享状态组件**

标题使用 32sp 对应 typography；主旅行卡与其他旅行按 `.pen` 分层；删除确认包含旅行日、收藏、行程项和路段影响。

- [ ] **Step 6: 跑绿灯并提交**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.trip.ui.TripList*"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripListContentTest
git add app/src/main/java/com/yangchengwei/easytrip/core/ui/component \
  app/src/main/java/com/yangchengwei/easytrip/trip/ui \
  app/src/test/java/com/yangchengwei/easytrip/trip/ui \
  app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui
git commit -m "Align trip list and deletion states with v1"
```

## Batch 1 Gate

- [ ] 运行：

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripFlowTest
```

- [ ] 真机走通：空列表 → 创建 → 校验 → 创建成功 → 工作台 → 返回 → 再次进入 → 删除。
- [ ] 对比 `K9h3r`、`dzhkC`、`yIGiQ`、`oW9mK`、`zIbEu`、`d1sTtb`。
- [ ] 记录截图与差异；未通过前停止。
- [ ] 运行 `graphify update .`，提交本批代码与正式图谱产物。

---

# Batch 2：搜索、连续收藏与地点池

## Task 3：建立统一工作台状态与 Overlay

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceOverlay.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceRoute.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceNavigationStateTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentStateTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`

**Interfaces:**

```kotlin
sealed interface WorkspaceOverlay {
    data object None : WorkspaceOverlay
    data class PlaceDetail(val placeId: Long) : WorkspaceOverlay
    data class SelectPlacesForDay(val dayId: Long) : WorkspaceOverlay
    data class SelectTargetDay(val placeIds: Set<Long>) : WorkspaceOverlay
    data object AddTripDay : WorkspaceOverlay
    data class EditItineraryItem(val itemId: Long) : WorkspaceOverlay
    data class EditRouteLeg(val legId: Long) : WorkspaceOverlay
    data object LayerMenu : WorkspaceOverlay
    data class Confirmation(val model: ConfirmationUiModel) : WorkspaceOverlay
    data class PermissionExplanation(val kind: PermissionKind) : WorkspaceOverlay
    data class Feedback(val model: FeedbackUiModel) : WorkspaceOverlay
}
```

- [ ] **Step 1: 写 Overlay 排他、Back 和恢复测试**

```kotlin
@Test fun openingOverlayReplacesCurrentOverlay()
@Test fun backClosesOverlayBeforeLeavingWorkspace()
@Test fun tabDayScopeAndSheetLevelRestoreFromSavedState()
```

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.workspace.TripWorkspaceNavigationStateTest"
```

- [ ] **Step 3: 最小实现协调状态和 Route/Content 边界**

工作台 Route 组合现有子 ViewModel 状态；Content 不直接订阅 Flow，不持有 NavController。

- [ ] **Step 4: 迁移新 workspace 路由并跑绿灯**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.workspace.TripWorkspace*"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt \
  app/src/main/java/com/yangchengwei/easytrip/workspace \
  app/src/test/java/com/yangchengwei/easytrip/workspace \
  app/src/androidTest/java/com/yangchengwei/easytrip/workspace
git commit -m "Centralize workspace navigation and overlay state"
```

## Task 4：重做搜索状态机与连续收藏

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchRoute.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchScreen.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducer.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducerTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/place/ui/CollectionTogglePolicyTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlaceSearchViewModelTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlaceSearchContentTest.kt`

**Interfaces:**

```kotlin
sealed interface PlaceSearchAction {
    data object Back : PlaceSearchAction
    data class QueryChanged(val value: String) : PlaceSearchAction
    data object Submit : PlaceSearchAction
    data object Retry : PlaceSearchAction
    data class ToggleCollection(val poiId: String) : PlaceSearchAction
    data object DismissRemovalConfirmation : PlaceSearchAction
    data object ConfirmRemoval : PlaceSearchAction
}
```

**Pencil:** `ofdn5`、`S0psO`、`GJo79`、`s1OvvX`、`I62qd5`、状态组 B `xENWi`、旅程 B `IpuKg`。

- [ ] **Step 1: 写搜索状态失败测试**

```kotlin
@Test fun loadingResultsEmptyAndNetworkFailureAreExclusive()
@Test fun collectingKeepsUserOnSearchScreen()
@Test fun multipleResultsCanBeCollectedSequentially()
@Test fun searchActionsNeverCreateItineraryItems()
@Test fun queryRestoresFromSavedState()
```

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.place.ui.PlaceSearch*"
```

- [ ] **Step 3: 最小实现 reducer、状态和收藏行为**

收藏写入 Room 后由 Flow 更新；移除旧的自动返回或直接加入行程路径。

- [ ] **Step 4: 写并运行 Compose 失败测试**

```kotlin
@Test fun resultRowsExposeBookmarkButNoScheduleAction()
@Test fun loadingEmptyAndFailureMatchTheirActions()
@Test fun activeSearchSurfaceUsesOpaqueWhiteBackground()
```

- [ ] **Step 5: 实现 Content 并跑绿灯**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.place.ui.PlaceSearch*"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlaceSearchContentTest
```

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/place/ui \
  app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt \
  app/src/test/java/com/yangchengwei/easytrip/place/ui \
  app/src/androidTest/java/com/yangchengwei/easytrip/place/ui
git commit -m "Support continuous collection from place search"
```

## Task 5：地点池、详情与地图控件

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/place/ui/SavedPlaceRow.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceDetailContent.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/MapLegend.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/MapControls.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/SearchSurface.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModelTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlacePoolFlowTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/MapLayerFlowTest.kt`

**Interfaces:**

```kotlin
data class SavedPlaceRowUi(
    val id: Long,
    val name: String,
    val address: String,
    val note: String?,
    val tags: List<String>,
    val itineraryOccurrenceCount: Int,
    val selected: Boolean,
)
```

**Pencil:** `A9EKX`、`p4G1tS`、`shoPV`、`lsr1I`、`jQhXs`。

- [ ] **Step 1: 写地点池和详情失败测试**

```kotlin
@Test fun placePoolDistinguishesSavedOnlyFromScheduled()
@Test fun placeDetailAllowsCollectionNoteAndTagsOnly()
@Test fun emptyPoolProvidesSearchAction()
@Test fun failedDetailSaveKeepsDraft()
```

- [ ] **Step 2: 写地图视觉语义失败测试**

```kotlin
@Test fun mapLegendWrapsContentAndHasTextLabels()
@Test fun searchSurfaceIsFullWidthAndOpaque()
@Test fun layerMenuUsesExclusiveWorkspaceOverlay()
```

- [ ] **Step 3: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.place.ui.PlacePoolViewModelTest"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest
```

- [ ] **Step 4: 实现独立地点行、详情和地图控件**

地点详情删除所有加入行程与安排信息；地图搜索入口接近全宽；图例使用文字和形状区分状态。

- [ ] **Step 5: 跑绿灯并提交**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.place.ui.PlacePoolViewModelTest"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.MapLayerFlowTest
git add app/src/main/java/com/yangchengwei/easytrip/place/ui \
  app/src/main/java/com/yangchengwei/easytrip/workspace \
  app/src/test app/src/androidTest
git commit -m "Complete place pool details and map controls"
```

## Batch 2 Gate

- [ ] 运行全套 JVM、目标 Compose、lint 和 assemble。
- [ ] 真机走通旅程 B：搜索 → 连续收藏 → 主动返回 → 地点池 → 编辑备注和标签。
- [ ] 对比 `A9EKX`、`ofdn5`、`p4G1tS`、`shoPV`、`lsr1I`、`S0psO`、`GJo79`、`s1OvvX`、`I62qd5`。
- [ ] 修复差异后重跑并更新 graphify。

---

# Batch 3：从地点池加入行程与旅行日

## Task 6：实现批量加入和撤销用例

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/domain/AddPlacesToDayUseCase.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/domain/UndoAddedItemsUseCase.kt`
- Modify only if existing interface lacks required atomic operations: `app/src/main/java/com/yangchengwei/easytrip/itinerary/domain/ItineraryRepository.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModel.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/itinerary/domain/AddPlacesToDayUseCaseTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/itinerary/domain/UndoAddedItemsUseCaseTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/AddPlacesRoomIntegrationTest.kt`

**Interfaces:**

```kotlin
data class AddPlacesRequest(
    val tripId: Long,
    val dayId: Long,
    val savedPlaceIds: List<Long>,
)

sealed interface AddPlacesOutcome {
    data class Success(val dayId: Long, val createdItemIds: List<Long>) : AddPlacesOutcome
    data class PartialSuccess(
        val dayId: Long,
        val createdItemIds: List<Long>,
        val failedPlaceIds: List<Long>,
    ) : AddPlacesOutcome
    data class TargetDayMissing(val retainedPlaceIds: List<Long>) : AddPlacesOutcome
}
```

- [ ] **Step 1: 阅读现有 Repository 方法签名并用它们写失败测试**

不得先添加重复 API。测试覆盖：重复 placeId、末尾追加、部分成功、目标日失效、撤销保留 SavedPlace、跨天无 RouteLeg。

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.domain.AddPlacesToDayUseCaseTest"
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.domain.UndoAddedItemsUseCaseTest"
```

- [ ] **Step 3: 最小实现用例**

按用户选择顺序追加；返回真实创建 ID；只在现有接口不足时添加最小 Repository 方法。

- [ ] **Step 4: 运行 Room 集成测试**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.AddPlacesRoomIntegrationTest
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/itinerary \
  app/src/test/java/com/yangchengwei/easytrip/itinerary \
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary
git commit -m "Add recoverable batch scheduling from saved places"
```

## Task 7：实现多选地点与目标日状态

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/AddToItineraryUiState.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/SelectPlacesContent.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/SelectTargetDayContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceOverlay.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/AddToItineraryStateTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/place/ui/PlacePoolFlowTest.kt`

**Interfaces:**

```kotlin
data class AddToItineraryUiState(
    val selectedPlaceIds: Set<Long> = emptySet(),
    val targetDayId: Long? = null,
    val isSubmitting: Boolean = false,
    val result: AddPlacesOutcome? = null,
)
```

**Pencil:** `xQfD0`、`p7U8B`、`Pqdkf`、`X3rm1`、`f25l9`、`yNKT4`、`cRdBn`、`mGhKO`、`V6RALq`、`D3XZi`、`KPBBb`、状态组 C `eHTbI`。

- [ ] **Step 1: 写选择、提交和恢复失败测试**

```kotlin
@Test fun continueIsDisabledWithoutSelectedPlaces()
@Test fun longDayListScrollsIndependently()
@Test fun submittingDisablesDuplicateActions()
@Test fun missingTargetDayKeepsSelectionAndRequiresReselection()
@Test fun partialSuccessKeepsSavedPlaces()
@Test fun undoDoesNotRemoveEarlierOccurrences()
```

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.ui.AddToItineraryStateTest"
```

- [ ] **Step 3: 实现状态、Overlay 和 SavedStateHandle 保存**

保存 `selectedPlaceIds`、`targetDayId` 和当前编辑目标；目标日失效后刷新日期但保留地点选择。

- [ ] **Step 4: 实现两个 Content 并跑 Compose 绿灯**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.place.ui.PlacePoolFlowTest
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/itinerary/ui \
  app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt \
  app/src/main/java/com/yangchengwei/easytrip/workspace \
  app/src/test app/src/androidTest
git commit -m "Add multi-place target-day selection states"
```

## Task 8：从天导航追加旅行日

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/AddTripDayContent.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/VerticalDayNavigation.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/DayItinerarySelectionTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryScopeRailTest.kt`

**Pencil:** `zvO9Z`；`.pen` 设置页的添加入口按设计公式删除。

- [ ] **Step 1: 写导航顺序和追加失败测试**

```kotlin
@Test fun navigationOrderIsWholeTripDaysThenAdd()
@Test fun addActionAlwaysAppendsAfterLastDay()
@Test fun settingsDoesNotExposeAddDay()
@Test fun tripWithoutDaysCanAddFromNavigation()
```

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.ui.DayItinerarySelectionTest"
```

- [ ] **Step 3: 使用现有 TripService/Repository 的末尾插入能力实现**

若现有接口只能通过 anchor/side 表达追加，封装 UI 侧的 `appendTripDay()`；不得暴露任意插入或排序。

- [ ] **Step 4: 跑绿灯并提交**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.ui.DayItinerarySelectionTest"
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryScopeRailTest
git add app/src/main/java/com/yangchengwei/easytrip/itinerary/ui \
  app/src/main/java/com/yangchengwei/easytrip/workspace \
  app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt \
  app/src/test app/src/androidTest
git commit -m "Append trip days from itinerary navigation"
```

## Batch 3 Gate

- [ ] 运行目标测试、全套 JVM、lint、assemble。
- [ ] 真机走通旅程 C 与异常分支：多选 → 目标日 → 提交 → 撤销 → 重复加入 → 目标日失效 → 新增一天。
- [ ] 对比本批 12 个编号画面并记录截图。
- [ ] 修复差异后重跑并更新 graphify。

---

# Batch 4：单日编辑、RouteLeg、全程与抽屉

## Task 9：实现行程项编辑草稿与删除

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryPlaceRow.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/EditItineraryItemContent.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditDraft.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItinerarySheet.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WorkspaceItineraryContent.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModelTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt`

**Interfaces:**

```kotlin
data class ItineraryEditDraft(
    val itemId: Long,
    val arrivalTime: LocalTime?,
    val stayMinutesText: String,
    val isSaving: Boolean = false,
    val saveError: String? = null,
)
```

**Pencil:** `LFmzR`、`K336N`、`l2xCsM`、`Bcf6A`、`OOEsk`、`mz2IS`、旅程 D `o4Wcz`。

- [ ] **Step 1: 写编辑与删除失败测试**

```kotlin
@Test fun editingTimeAndDurationKeepsPlaceIdentityAndOrder()
@Test fun saveFailureKeepsDraftAndOverlayOpen()
@Test fun retryUsesCurrentDraft()
@Test fun deletingItineraryItemKeepsSavedPlace()
@Test fun emptyDayShowsExplicitSummaryAndAddHint()
```

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModelTest"
```

- [ ] **Step 3: 实现草稿、Content 和删除确认**

只在 Repository 成功后关闭 Overlay；删除确认突出地点并说明 SavedPlace 保留。

- [ ] **Step 4: 跑绿灯并提交**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.ItineraryEditingTest
git add app/src/main/java/com/yangchengwei/easytrip/itinerary/ui \
  app/src/test/java/com/yangchengwei/easytrip/itinerary/ui \
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui
git commit -m "Preserve itinerary drafts across save failures"
```

## Task 10：校正同日排序和 RouteLeg 编辑

**Files:**
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/RouteLegRow.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/route/domain/RouteRefreshCoordinator.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WorkspaceItineraryContent.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/route/domain/RouteRefreshCoordinatorTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/DayItineraryViewModelTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt`

**Interfaces:**

```kotlin
sealed interface RouteLegUiState {
    data class Ready(
        val mode: TransportMode,
        val durationMinutes: Int?,
        val distanceMeters: Int?,
    ) : RouteLegUiState
    data object WaitingForNetwork : RouteLegUiState
    data class Failed(val message: String) : RouteLegUiState
}
```

**Pencil:** `T7aESo`、`P7k0M`、`E3EhSv`、`eHTX3`、旅程 E `q08to1`、状态组 D `CW0vn`。

- [ ] **Step 1: 写邻接路段失败测试**

```kotlin
@Test fun reorderWithinDayRebuildsOnlyAffectedAdjacentLegs()
@Test fun crossDayLegIsNeverCreated()
@Test fun waitingForNetworkKeepsAllPlaceActions()
@Test fun retryTargetsOnlyFailedLeg()
@Test fun changingTransportModeFailureKeepsEditorOpen()
```

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinatorTest"
```

- [ ] **Step 3: 最小校正现有排序与刷新协调器**

不重写 Repository；只补充被测试证明缺失的邻接计算和单 leg 重试。

- [ ] **Step 4: 跑绿灯并提交**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.route.*"
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.ui.DayItineraryViewModelTest"
git add app/src/main/java/com/yangchengwei/easytrip/itinerary \
  app/src/main/java/com/yangchengwei/easytrip/route \
  app/src/test app/src/androidTest
git commit -m "Refresh adjacent route legs after itinerary edits"
```

## Task 11：全程只读视图和三档抽屉

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceBottomSheet.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceTabs.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/WorkspaceItineraryContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryMapperTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/workspace/WorkspaceSheetSyncTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/WholeTripItineraryContentTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspaceFlowTest.kt`

**Interfaces:**

```kotlin
enum class WorkspaceSheetLevel { COLLAPSED, HALF, EXPANDED }

@Composable
fun WorkspaceBottomSheet(
    value: WorkspaceSheetLevel,
    onValueChange: (WorkspaceSheetLevel) -> Unit,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
)
```

**Pencil:** `FTIOF`、`kCc5z`、`sWTB3`、`f2ieZ6`、旅程 F `xP91E`。

- [ ] **Step 1: 写抽屉与全程失败测试**

```kotlin
@Test fun sheetAcceptsOnlyThreeLevelsAndRestoresThem()
@Test fun fullTripContainsNoEditOrDragActions()
@Test fun wholeTripDoesNotConnectAdjacentDays()
@Test fun workspaceTabsUseIndicatorInsteadOfPillSelection()
```

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.workspace.WorkspaceSheetSyncTest"
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryMapperTest"
```

- [ ] **Step 3: 实现三档抽屉、普通 Tab 和只读时间轴**

地图在三个档位持续存在；抽屉内容独立滚动；全程按旅行日分色但不跨天连线。

- [ ] **Step 4: 跑绿灯并提交**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.itinerary.ui.WholeTripItineraryContentTest
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspaceFlowTest
git add app/src/main/java/com/yangchengwei/easytrip/workspace \
  app/src/main/java/com/yangchengwei/easytrip/itinerary/ui \
  app/src/test app/src/androidTest
git commit -m "Add whole-trip timeline and three workspace sheet levels"
```

## Batch 4 Gate

- [ ] 运行 itinerary、route、workspace 测试及全套 JVM、lint、assemble。
- [ ] 真机走通旅程 D/E/F。
- [ ] 对比本批行程、编辑、失败、全程和三档抽屉画面。
- [ ] 修复差异后重跑并更新 graphify。

---

# Batch 5：设置、权限、地图降级与总回归

## Task 12：整体日期范围与危险操作

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripDateRangeService.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/DateRangeChangeUiState.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsRoute.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripService.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/trip/ui/TripSettingsScreen.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/trip/domain/TripDateRangeServiceTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/trip/ui/TripSettingsViewModelTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/trip/ui/TripSettingsContentTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/CascadeDeleteTest.kt`

**Interfaces:**

```kotlin
data class DateRangeChangeImpact(
    val newStartDate: LocalDate?,
    val newEndDate: LocalDate?,
    val retainedDayIds: List<Long>,
    val deletedDayIds: List<Long>,
    val deletedItineraryItems: Int,
    val deletedRouteLegs: Int,
    val retainedSavedPlaces: Int,
)
```

**Pencil:** `U06l7P`、`J7PZ7u`、`IKTv5`、`qiFve`、旅程 G `y3rP1`；设置页“添加一天”按设计公式删除。

- [ ] **Step 1: 写日期影响和设置失败测试**

```kotlin
@Test fun endBeforeStartDoesNotSubmit()
@Test fun shrinkingRangeRequiresImpactConfirmation()
@Test fun cancelKeepsExistingDays()
@Test fun confirmedShrinkRemovesTrailingDaysAndRenumbers()
@Test fun shrinkingKeepsSavedPlaces()
@Test fun settingsExposesNoAddInsertReorderOrSingleDayDateActions()
```

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.trip.domain.TripDateRangeServiceTest"
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.trip.ui.TripSettingsViewModelTest"
```

- [ ] **Step 3: 使用现有事务能力实现 preview/apply**

增长只向末尾追加，缩短只从末尾删除；先计算影响再提交。若现有 Service 已能表达，直接复用而不新增重复 Repository 方法。

- [ ] **Step 4: 实现设置 Content 和危险确认并跑绿灯**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.trip.ui.TripSettingsContentTest
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.CascadeDeleteTest
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/yangchengwei/easytrip/trip \
  app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt \
  app/src/test/java/com/yangchengwei/easytrip/trip \
  app/src/androidTest
git commit -m "Manage continuous trip dates with impact previews"
```

## Task 13：地图授权、定位权限和地图降级

**Files:**
- Create: `app/src/main/java/com/yangchengwei/easytrip/permission/LocationPermissionCoordinator.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/permission/PermissionExplanationContent.kt`
- Create: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceMapStatusContent.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceRoute.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/WorkspaceUiModels.kt`
- Modify: `app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContent.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/permission/LocationPermissionCoordinatorTest.kt`
- Test: `app/src/test/java/com/yangchengwei/easytrip/workspace/TripWorkspaceContentStateTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/workspace/WorkspacePermissionFlowTest.kt`
- Test: `app/src/androidTest/java/com/yangchengwei/easytrip/OfflineRecoveryTest.kt`

**Interfaces:**

```kotlin
enum class PermissionKind {
    MAP_SERVICE_CONSENT,
    DEVICE_LOCATION,
    DEVICE_LOCATION_SETTINGS,
}

sealed interface WorkspaceEffect {
    data object RequestLocationPermission : WorkspaceEffect
    data object OpenApplicationSettings : WorkspaceEffect
}
```

**Pencil:** `EHOHC`、`JFhZ7`、`HYCsZ`、`GoxB6`、`U8R5i`、状态组 E `a5GvBo`、横切旅程 H `voAHV`。

- [ ] **Step 1: 写权限和降级失败测试**

```kotlin
@Test fun enteringWorkspaceDoesNotRequestLocation()
@Test fun locateClickShowsExplanationBeforeSystemRequest()
@Test fun confirmationEmitsOnePermissionEffect()
@Test fun permanentDenialOffersApplicationSettings()
@Test fun mapFailureKeepsPlacePoolAndItineraryActionsEnabled()
@Test fun retryingMapDoesNotClearRoomBackedState()
```

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.permission.LocationPermissionCoordinatorTest"
./gradlew testDebugUnitTest --tests "com.yangchengwei.easytrip.workspace.TripWorkspaceContentStateTest"
```

- [ ] **Step 3: 实现用途说明、Effect 和地图状态内容**

Compose 只实现请求前说明与永久拒绝后的设置引导；系统权限框交给 Activity Result API。

- [ ] **Step 4: 跑绿灯并提交**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.workspace.WorkspacePermissionFlowTest
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.OfflineRecoveryTest
git add app/src/main/java/com/yangchengwei/easytrip/permission \
  app/src/main/java/com/yangchengwei/easytrip/workspace \
  app/src/main/java/com/yangchengwei/easytrip/AppNavigation.kt \
  app/src/test app/src/androidTest
git commit -m "Separate map consent from location permission"
```

## Task 14：建立 47 个已采纳画面的验收目录并完成总回归

**Files:**
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/V1ScenarioFixtures.kt`
- Create: `app/src/androidTest/java/com/yangchengwei/easytrip/V1FullUiAcceptanceTest.kt`
- Create: `docs/testing/v1-full-ui-scenario-matrix.md`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/V1PencilFlowTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/V1AcceptanceTest.kt`
- Modify: `app/src/androidTest/java/com/yangchengwei/easytrip/OfflineRecoveryTest.kt`

**Interfaces:**

```kotlin
data class V1Scenario(
    val number: Int,
    val name: String,
    val frameId: String,
    val journey: String?,
    val matrix: String?,
    val launch: ScenarioLaunch,
    val assertions: List<ScenarioAssertion>,
    val variants: List<V1ScenarioVariant> = emptyList(),
)

data class V1ScenarioVariant(
    val parentNumber: Int,
    val name: String,
    val frameId: String,
)
```

编号直接来自 `.pen` 名称。`05` 已废弃，不得虚构；`d1sTtb` 按原名“01 我的旅行 · 删除后”挂在 01 的 `variants` 中。完整目录必须满足：

```kotlin
val adoptedScreenNumbers = (1..48).filterNot { it == 5 }.toSet()
require(scenarios.size == 47)
require(scenarios.map { it.number }.toSet() == adoptedScreenNumbers)
require(scenarios.none { it.number == 5 })
require(scenarios.all { it.frameId.isNotBlank() })
require(scenarios.first { it.number == 1 }.variants.any { it.frameId == "d1sTtb" })
```

- [ ] **Step 1: 写场景完整性失败测试**

```kotlin
@Test fun containsEveryExistingNumberedFrameWithoutInventingFive()
@Test fun containsExactlyFortySevenAdoptedNumberedScenarios()
@Test fun coversSevenJourneysAndSixMatrices()
@Test fun everyScenarioHasFrameFixtureDevicePathAndAssertions()
```

- [ ] **Step 2: 运行并确认 RED**

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1FullUiAcceptanceTest
```

- [ ] **Step 3: 建立场景清单**

编号画面映射：

| 设计编号 | Frame ID | 场景 |
|---:|---|---|
| 01 | `K9h3r` | 我的旅行 |
| 02 | `A9EKX` | 工作台·地点池 |
| 03 | `ofdn5` | 搜索地点 |
| 04 | `LFmzR` | 工作台·行程 |
| 06 | `FTIOF` | 全程行程 |
| 07 | `dzhkC` | 创建旅行 |
| 08 | `U06l7P` | 旅行设置 |
| 09 | `xQfD0` | 选择日期 |
| 10 | `p4G1tS` | 地点详情与编辑 |
| 11 | `K336N` | 行程项编辑 |
| 12 | `T7aESo` | 交通路段编辑 |
| 13 | `oW9mK` | 删除旅行确认 |
| 14 | `DxZ2a` | 状态规范 |
| 15 | `p7U8B` | 无旅行日 |
| 16 | `ijpZD` | 工作台更多菜单 |
| 17 | `shoPV` | 地图图层 |
| 18 | `zvO9Z` | 添加旅行日 |
| 19 | `Pqdkf` | 从地点池添加地点 |
| 20 | `X3rm1` | 旅程 C 选择 |
| 21 | `f25l9` | 旅程 C 完成 |
| 22 | `kCc5z` | 抽屉收起 |
| 23 | `sWTB3` | 抽屉半屏 |
| 24 | `f2ieZ6` | 抽屉展开 |
| 25 | `J7PZ7u` | 删除旅行日确认 |
| 26 | `lsr1I` | 地点池空状态 |
| 27 | `S0psO` | 搜索无结果 |
| 28 | `P7k0M` | 等待联网 |
| 29 | `E3EhSv` | 路线失败 |
| 30 | `EHOHC` | 地图权限说明 |
| 31 | `yNKT4` | 加入成功 |
| 32 | `l2xCsM` | 删除行程项确认 |
| 33 | `cRdBn` | 长日期列表 |
| 34 | `JFhZ7` | 定位权限说明 |
| 35 | `HYCsZ` | 前往设置 |
| 36 | `zIbEu` | 我的旅行空状态 |
| 37 | `Bcf6A` | 当天无地点 |
| 38 | `GJo79` | 搜索网络失败 |
| 39 | `mGhKO` | 部分成功 |
| 40 | `IKTv5` | 修改出行日期 |
| 41 | `V6RALq` | 加入提交中 |
| 42 | `D3XZi` | 目标日已删除 |
| 43 | `KPBBb` | 撤销成功 |
| 44 | `s1OvvX` | 搜索加载中 |
| 45 | `GoxB6` | 地图加载中 |
| 46 | `U8R5i` | 地图加载失败 |
| 47 | `yIGiQ` | 创建表单校验 |
| 48 | `OOEsk` | 行程修改保存失败 |

01 的状态变体：`d1sTtb`，原名“01 我的旅行 · 删除后”。它不占用编号 05，也不生成 `v1-05-*` 测试或截图。

七条旅程：`RqVLv`、`IpuKg`、`V7cr3b`、`o4Wcz`、`q08to1`、`xP91E`、`y3rP1`。`BTSZj` 和 `voAHV` 是横切异常验收，不增加主旅程计数。

六组矩阵：`hVIMZ`、`xENWi`、`eHTbI`、`CW0vn`、`a5GvBo`、`fIkSG`。

- [ ] **Step 4: 实现 fixture 与关键语义断言**

必须验证：搜索无加入行程、地点详情无行程入口、设置无添加一天、全程无编辑/拖动、权限前置说明、危险操作影响文案。

- [ ] **Step 5: 运行最终自动化验证**

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1FullUiAcceptanceTest
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.V1PencilFlowTest
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.yangchengwei.easytrip.OfflineRecoveryTest
./gradlew lintDebug
./gradlew assembleDebug
```

- [ ] **Step 6: 完成真机视觉证据**

每个场景在 `docs/testing/v1-full-ui-scenario-matrix.md` 记录：frameId、fixture、测试方法、真机入口、Pencil 截图、设备截图、设备尺寸、字体缩放、允许差异、未解决差异和结论。

截图命名：

```text
v1-01-K9h3r-pencil.png
v1-01-K9h3r-device.png
v1-01-K9h3r-diff.png
```

至少验证基准设备、小屏、默认字体、放大字体、长旅行名、长地点名、长日期列表、手势导航和进程恢复。

- [ ] **Step 7: 清理被替换 UI 并跑完整回归**

删除旧 Dialog、重复 Content、旧路由和无调用兼容组件；不得删除仍服务未迁移能力的代码。

- [ ] **Step 8: 更新图谱并提交最终验收**

```bash
graphify update .
git status --short
git add app/src/androidTest/java/com/yangchengwei/easytrip \
  docs/testing/v1-full-ui-scenario-matrix.md \
  graphify-out/.graphify_labels.json \
  graphify-out/GRAPH_REPORT.md \
  graphify-out/graph.html \
  graphify-out/graph.json \
  graphify-out/manifest.json
git commit -m "Cover all Easy Trip v1 UI acceptance scenarios"
```

---

## Final Execution Order

```text
Task 1 → Task 2 → Batch 1 Gate
→ Task 3 → Task 4 → Task 5 → Batch 2 Gate
→ Task 6 → Task 7 → Task 8 → Batch 3 Gate
→ Task 9 → Task 10 → Task 11 → Batch 4 Gate
→ Task 12 → Task 13 → Task 14 → Batch 5 Gate
```

每个任务由独立实现 Agent 执行；主 Agent 在每个任务后做规格符合性审查和代码质量审查。任何批次门禁失败时停在当前批次修复，不提前开发下一批。
