# Graph Report - easy-trip-v1-full-ui-run  (2026-08-24)

## Corpus Check
- 212 files · ~101,723 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2753 nodes · 5862 edges · 136 communities (102 shown, 34 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 381 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `4cf13e8e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Easy Trip v1.0 全量 UI 与交互落地设计
- MapUiModelMapperTest
- FakeRepository
- FakeTripRepository
- awaitSdkCallback
- TripSettingsViewModel
- RoomRouteLegRepository
- Legs
- Trips
- PlaceSearchReducer
- .duplicateDragCrossDayTimingOverrideAndRetry
- AmapComposeMap.kt
- DayItineraryViewModel
- TripListViewModel
- MapViewportControllerTest
- TripDay
- RouteStatus
- TravelMode
- CreateTrip
- RouteLegRepository
- TripDao
- Easy Trip 一期设计
- Converters
- PlacePoolViewModel
- PlaceCandidate
- Easy Trip v1.0 Pencil 设计落地方案
- FakeSavedPlaces
- Legs
- GeoPoint
- Easy Trip v2 交互与视觉升级设计
- Easy Trip UI 设计公式
- AmapRouteDataSource.kt
- MemoryPreferences
- 高德 Android SDK 集成决策
- SelectablePill
- Easy Trip 工作台导航重构设计
- SchemaTest
- PlaceSearchViewModel
- AmapComposeMapTest.kt
- 2026-08-23-easy-trip-v1-full-ui-implementation.md
- Legs
- AppNavigation.kt
- View
- ItineraryScope
- DayItineraryContent
- Trips
- SavedPlaceEntity
- EasyTripDatabase.kt
- Dp
- TripWorkspaceContent.kt
- WorkspaceOverlay
- Easy Trip 搜索与收藏交互设计
- FakeRepository
- DeleteImpactDao
- TripDay
- Legs
- WholeTripItineraryContent
- TripWorkspaceScreen.kt
- AmapComposeMap
- MapFacade.kt
- RoomTripRepository
- RouteFormattingTest
- GeoPoint
- TripWorkspaceViewModel
- CreateTripViewModel
- RoomItineraryRepository
- MapLifecycleController
- Itineraries
- TestTripRepository
- TripWorkspaceNavigationStateTest
- ItineraryService
- MapLayerRenderingPolicyTest
- Task 4 报告：重做搜索状态机与连续收藏
- .setContent
- SharedPreferences
- FakeTripRepository
- EasyTripDatabase
- FakeLegs
- Trips
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest
- AmapPlaceDataSource.kt
- .setContent
- MapPoiUi
- RoomDeleteImpactProviderTest.kt
- reduceMapInteraction
- SavedPlace
- CLAUDE.md
- AmapConsentToken
- FeedbackState
- FakeTrips
- AmapPrivacyStateMachine
- ViewportReason
- TimeMode
- LazyScrollbar.kt
- settledWorkspaceSheetLevel
- AmapPrivacyGate.kt
- File Structure
- File Structure
- ConfirmationDialog
- Global Constraints
- Easy Trip v1.0 Pencil Reference
- Batch 1 visual fix report
- TripWorkspaceAction
- EditingPlaces
- CascadeCountDao
- SchemaPlaceDao
- AmapMapHost
- RoomSavedPlaceRepositoryTest
- PlacePoolContent
- Places
- EasyTripApplication
- AmapMapHost
- AmapMapHost
- MapLayer
- 行程项
- .searchCollectionMapAndRestorationFlow
- EasyTripTokens.kt
- TestMapHost
- DayItineraryViewModel.kt
- formatOccurrenceBadge
- RoomRouteLegRepositoryTest
- MapViewportRenderingPolicyTest
- RouteResult
- AmapMapHost
- TripListAction
- guard-adb-install.sh
- RouteLegRow.kt
- .create
- AppIconResourceTest
- SDD ledger — plan: docs/superpowers/plans/2026-08-23-easy-trip-v1-full-ui-implementation.md

## God Nodes (most connected - your core abstractions)
1. `GeoPoint` - 123 edges
2. `PlaceCandidate` - 85 edges
3. `TravelMode` - 63 edges
4. `TripWorkspaceViewModel` - 60 edges
5. `TripDay` - 54 edges
6. `SavedPlace` - 53 edges
7. `CreateTrip` - 52 edges
8. `TransportMode` - 50 edges
9. `TripService` - 46 edges
10. `TripRepository` - 43 edges

## Surprising Connections (you probably didn't know these)
- `FakeTrips` --calls--> `TripDay`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `FakeTrips` --calls--> `TripWithDays`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `EasyTripApplication` --calls--> `AppContainer`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/EasyTripApplication.kt → app/src/main/java/com/yangchengwei/easytrip/AppContainer.kt
- `RoutePlannerTest` --calls--> `GeoPoint`  [INFERRED]
  app/src/test/java/com/yangchengwei/easytrip/route/domain/RoutePlannerTest.kt → app/src/main/java/com/yangchengwei/easytrip/core/model/GeoPoint.kt
- `ConfirmationDialog()` --calls--> `EasyTripDangerButton()`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/core/ui/component/ConfirmationDialog.kt → app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EasyTripButton.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **旅行编排数据模型** — docs_superpowers_specs_2026_08_21_saved_place, docs_superpowers_specs_2026_08_21_itinerary_item, docs_superpowers_specs_2026_08_21_route_leg [EXTRACTED 1.00]

## Communities (136 total, 34 thin omitted)

### Community 0 - "Easy Trip v1.0 全量 UI 与交互落地设计"
Cohesion: 0.04
Nodes (46): 10.1 批次 1：旅行入口与创建, 10.2 批次 2：搜索、连续收藏与地点池, 10.3 批次 3：从地点池加入行程与旅行日, 10.4 批次 4：单日编辑、交通路段与全程, 10.5 批次 5：设置、日期、权限与完整回归, 10. 五个实施批次, 11.1 本地优先, 11.2 收藏 (+38 more)

### Community 2 - "FakeRepository"
Cohesion: 0.10
Nodes (21): connected(), hasValidatedInternet(), StateFlow, NetworkMonitor, onAvailable(), onCapabilitiesChanged(), onLost(), Active (+13 more)

### Community 3 - "FakeTripRepository"
Cohesion: 0.21
Nodes (4): FakeTripRepository, CreateTrip, Flow, TripServiceTest

### Community 4 - "awaitSdkCallback"
Cohesion: 0.08
Nodes (18): OneShotCallback, awaitSdkCallback(), BridgeState, CANCELLED, COMPLETED, READY, STARTED, STARTING (+10 more)

### Community 5 - "TripSettingsViewModel"
Cohesion: 0.10
Nodes (11): androidx, DayDeleteImpact, DayUi, Factory, StateFlow, T, ViewModel, ViewModelProvider (+3 more)

### Community 7 - "Legs"
Cohesion: 0.05
Nodes (9): FakeMapPreferences, Itineraries, com, CreateTrip, Role, Legs, MapLayerFlowTest, Places (+1 more)

### Community 8 - "Trips"
Cohesion: 0.06
Nodes (10): Itineraries, com, CreateTrip, Flow, Trips, Legs, Places, Trips (+2 more)

### Community 9 - "PlaceSearchReducer"
Cohesion: 0.08
Nodes (31): PlaceSearchContentTest, BackIcon(), BookmarkIcon(), CloseIcon(), Color, Modifier, LocationIcon(), PlaceSearchContent() (+23 more)

### Community 10 - ".duplicateDragCrossDayTimingOverrideAndRetry"
Cohesion: 0.17
Nodes (7): Add, FakeCoordinator, FakeItineraries, ItineraryEditingTest, kotlinx, Move, Timing

### Community 11 - "AmapComposeMap.kt"
Cohesion: 0.10
Nodes (10): AmapMapHost, Bounds, android, View, MapZoomButton(), RealAmapMapHost, SinglePoint, toMapPoiUi() (+2 more)

### Community 13 - "TripListViewModel"
Cohesion: 0.15
Nodes (11): Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider, OpenSettings (+3 more)

### Community 14 - "MapViewportControllerTest"
Cohesion: 0.16
Nodes (3): MapViewportRequest, MapViewportController, MapViewportControllerTest

### Community 15 - "TripDay"
Cohesion: 0.08
Nodes (11): Flow, Flow, Flow, InsertSide, AFTER, BEFORE, Flow, TripRepository (+3 more)

### Community 16 - "RouteStatus"
Cohesion: 0.09
Nodes (10): RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK, Flow, RouteLegDao (+2 more)

### Community 17 - "TravelMode"
Cohesion: 0.10
Nodes (12): TransportMode, DRIVE, TAXI, TRANSIT, WALK, TravelMode, FLEXIBLE, SELF_DRIVE (+4 more)

### Community 18 - "CreateTrip"
Cohesion: 0.14
Nodes (5): IdFactory, T, MutableClock, RoomTripRepositoryTest, CreateTrip

### Community 19 - "RouteLegRepository"
Cohesion: 0.09
Nodes (13): Flow, Lifecycle, LifecycleOwner, TestOwner, Flow, RouteLegRepository, RouteLegWithEndpoints, Failure (+5 more)

### Community 20 - "TripDao"
Cohesion: 0.16
Nodes (4): CreateTrip, TripDao, TripDayEntity, TripEntity

### Community 21 - "Easy Trip 一期设计"
Cohesion: 0.17
Nodes (12): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 离线路线等待与联网续算, 同一地点可重复安排行程 (+4 more)

### Community 22 - "Converters"
Cohesion: 0.10
Nodes (6): Converters, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT, UNSUPPORTED_TRANSIT

### Community 23 - "PlacePoolViewModel"
Cohesion: 0.06
Nodes (18): PlaceSearchDataSource, PlacePoolFlowTest, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, PlacePoolSheet(), Factory, Job (+10 more)

### Community 24 - "PlaceCandidate"
Cohesion: 0.10
Nodes (13): AmapPrivacyGate, PlaceCandidate, PlaceSearchDataSource, Flow, RoomSavedPlaceRepository, AlreadySaved, Flow, PlaceTag (+5 more)

### Community 25 - "Easy Trip v1.0 Pencil 设计落地方案"
Cohesion: 0.05
Nodes (39): 10.1 JVM, 10.2 Compose instrumentation, 10.3 数据与导航集成, 10.4 验证命令, 10. 测试策略, 11. 视觉验收, 12. 验收标准, 13. 协作边界 (+31 more)

### Community 26 - "FakeSavedPlaces"
Cohesion: 0.14
Nodes (7): FakeSavedPlaces, IgnoringCancellationSearchSource, ImmediateSearchSource, Flow, PlaceSearchDataSource, PlaceSearchViewModelTest, RecordingSearchSource

### Community 27 - "Legs"
Cohesion: 0.06
Nodes (7): DayItinerarySelectionTest, Itineraries, CreateTrip, Flow, Trips, Legs, Trips

### Community 28 - "GeoPoint"
Cohesion: 0.18
Nodes (5): Flow, DayItinerary, ItineraryItem, ItineraryRepository, Flow

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.17
Nodes (15): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+7 more)

### Community 30 - "Easy Trip UI 设计公式"
Cohesion: 0.05
Nodes (43): 10.1 无网, 10.2 路线失败, 10.3 地图加载中或失败, 10.4 部分成功, 10.5 提交中与目标日失效, 10.6 保存失败, 10. 本地优先与状态公式, 11.1 地图服务授权 (+35 more)

### Community 31 - "AmapRouteDataSource.kt"
Cohesion: 0.07
Nodes (26): AmapSmokeTest, AmapServiceException, AmapRouteDataSource, CallbackBoundary, RouteSearch, CallbackBoundary, RouteDataSource, parseRouteResult() (+18 more)

### Community 32 - "MemoryPreferences"
Cohesion: 0.16
Nodes (3): SharedPreferencesMapPreferences, MapPreferencesTest, MemoryPreferences

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.14
Nodes (14): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+6 more)

### Community 34 - "SelectablePill"
Cohesion: 0.18
Nodes (9): SelectablePillTest, Modifier, Role, SelectablePill(), SelectablePillStyle, DaySelector(), LazyListState, Modifier (+1 more)

### Community 35 - "Easy Trip 工作台导航重构设计"
Cohesion: 0.06
Nodes (33): 10. 数据模型, 11. 空态与错误处理, 12. 可访问性, 13. 测试策略, 14. 验收标准, 1. 目标, 2. 非目标, 3. 信息架构 (+25 more)

### Community 37 - "PlaceSearchViewModel"
Cohesion: 0.13
Nodes (14): Back, ConfirmRemoval, DismissRemovalConfirmation, Factory, StateFlow, T, ViewModel, ViewModelProvider (+6 more)

### Community 38 - "AmapComposeMapTest.kt"
Cohesion: 0.21
Nodes (8): AmapComposeMapTest, Lifecycle, LifecycleOwner, TestOwner, MapMarkerKind, SAVED_ITINERARY, SAVED_PLACE_POOL, UNSAVED_SEARCH

### Community 39 - "2026-08-23-easy-trip-v1-full-ui-implementation.md"
Cohesion: 0.07
Nodes (27): Batch 1 Gate, Batch 1：旅行入口、创建与删除, Batch 2 Gate, Batch 2：搜索、连续收藏与地点池, Batch 3 Gate, Batch 3：从地点池加入行程与旅行日, Batch 4 Gate, Batch 4：单日编辑、RouteLeg、全程与抽屉 (+19 more)

### Community 40 - "Legs"
Cohesion: 0.05
Nodes (7): Itineraries, com, CreateTrip, Legs, Places, Trips, WorkspaceSearchTabsTest

### Community 41 - "AppNavigation.kt"
Cohesion: 0.17
Nodes (12): AppNavigation(), AppNavigationDependencies, AppNavigationObserver, android, com, tripSearchRoute(), MainActivity, PlaceSearchRoute() (+4 more)

### Community 42 - "View"
Cohesion: 0.07
Nodes (7): AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, View

### Community 43 - "ItineraryScope"
Cohesion: 0.12
Nodes (13): Day, decodeItineraryScope(), encodeItineraryScope(), ItineraryScope, reconcileItineraryScope(), RestoredWorkspaceNavigation, restoreWorkspaceNavigation(), toMapScope() (+5 more)

### Community 44 - "DayItineraryContent"
Cohesion: 0.20
Nodes (17): AddPlace, CommitMove, ConfirmDelete, DayItineraryAction, DayItineraryContent(), DayItinerarySheet(), DismissDialogs, Modifier (+9 more)

### Community 45 - "Trips"
Cohesion: 0.05
Nodes (8): Itineraries, com, CreateTrip, Legs, Places, SavedPlaces, SearchMapFocusTest, Trips

### Community 46 - "SavedPlaceEntity"
Cohesion: 0.14
Nodes (4): Flow, PlaceDao, PlaceSnapshotRow, SavedPlaceEntity

### Community 48 - "Dp"
Cohesion: 0.14
Nodes (13): EasyTripButtonTest, EasyTripIconButton(), Modifier, EmptyState(), Modifier, ItineraryItemCard(), ItineraryItemRow(), Color (+5 more)

### Community 49 - "TripWorkspaceContent.kt"
Cohesion: 0.22
Nodes (17): ConfirmationUiModel, DayItineraryUiState, PlacePoolUiState, Composable, Modifier, label(), LayerIcon(), toSheetValue() (+9 more)

### Community 50 - "WorkspaceOverlay"
Cohesion: 0.12
Nodes (16): AddTripDay, Confirmation, EditItineraryItem, EditRouteLeg, Feedback, FeedbackUiModel, LayerMenu, None (+8 more)

### Community 51 - "Easy Trip 搜索与收藏交互设计"
Cohesion: 0.07
Nodes (27): 10. 边界与错误处理, 11. 测试策略, 12. 验收标准, 1. 背景与目标, 2. 非目标, 3. 当前实现基线, 4.1 工作台, 4.2 独立搜索页 (+19 more)

### Community 52 - "FakeRepository"
Cohesion: 0.14
Nodes (6): CreateTripViewModelTest, FakeRepository, CreateTrip, Flow, SavedStateHandle, FakeRepository

### Community 53 - "DeleteImpactDao"
Cohesion: 0.17
Nodes (3): DeleteImpactDao, RoomDeleteImpactProvider, TripDeleteImpact

### Community 54 - "TripDay"
Cohesion: 0.13
Nodes (20): ItineraryScopeRailTest, ItineraryScopeRail(), Modifier, ScopeItem(), mapWholeTripDays(), RouteLegUi, toRouteErrorSummary(), toRouteLegUi() (+12 more)

### Community 56 - "WholeTripItineraryContent"
Cohesion: 0.27
Nodes (4): WholeTripItineraryContentTest, dayHeading(), Modifier, WholeTripItineraryContent()

### Community 57 - "TripWorkspaceScreen.kt"
Cohesion: 0.12
Nodes (27): ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton(), CompactDangerButton(), CompactPrimaryButton(), CompactSecondaryButton() (+19 more)

### Community 58 - "AmapComposeMap"
Cohesion: 0.18
Nodes (4): AmapComposeMap(), Modifier, MapHostCallbackGuard, MapHostCallbackGuardTest

### Community 59 - "MapFacade.kt"
Cohesion: 0.19
Nodes (13): CorruptRoute, MapMarkerUi, MapPolylineUi, MapRouteLabelUi, MapScope, PLACE_POOL, SINGLE_DAY, WHOLE_TRIP (+5 more)

### Community 60 - "RoomTripRepository"
Cohesion: 0.14
Nodes (3): V1PencilFlowTest, Flow, RoomTripRepository

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 62 - "GeoPoint"
Cohesion: 0.13
Nodes (7): GeoPoint, parsePlaces(), RawPlace, Result, PolylineCodec, PlaceContractsTest, PolylineCodecTest

### Community 63 - "TripWorkspaceViewModel"
Cohesion: 0.17
Nodes (6): Job, StateFlow, ViewModel, SearchResultSelection, TripWorkspaceUiState, TripWorkspaceViewModel

### Community 64 - "CreateTripViewModel"
Cohesion: 0.05
Nodes (34): CreateTripContentTest, CreateTrip, Flow, RecordingTripRepository, CreateTripContent(), Modifier, StepNumber(), Back (+26 more)

### Community 65 - "RoomItineraryRepository"
Cohesion: 0.07
Nodes (11): ItineraryTransactionTest, SequenceIds, DayItems, DayItineraryRow, ItineraryDao, Flow, ItineraryItemEntity, RoomItineraryRepository (+3 more)

### Community 68 - "TestTripRepository"
Cohesion: 0.08
Nodes (9): com, CreateTrip, TripService, CreateTrip, Flow, TestImpacts, TestTripRepository, TripListViewModelTest (+1 more)

### Community 69 - "TripWorkspaceNavigationStateTest"
Cohesion: 0.05
Nodes (10): Itineraries, com, CreateTrip, SavedStateHandle, Trips, Legs, MutableItineraries, Places (+2 more)

### Community 72 - "Task 4 报告：重做搜索状态机与连续收藏"
Cohesion: 0.13
Nodes (14): Device gate review fix round 1, Fix round 1, Fix round 2, Fix round 3, Fix round 4, Graphify, Pencil 对照, Task 4 报告：重做搜索状态机与连续收藏 (+6 more)

### Community 73 - ".setContent"
Cohesion: 0.14
Nodes (13): TripWorkspaceContentTest, Modifier, WorkspaceMapFallback(), ConsentRequired, Error, Failed, Loading, NotFound (+5 more)

### Community 75 - "FakeTripRepository"
Cohesion: 0.14
Nodes (7): FakeImpacts, FakeTripRepository, CreateTrip, Flow, Role, MoveCall, TripFlowTest

### Community 76 - "EasyTripDatabase"
Cohesion: 0.18
Nodes (5): CascadeDeleteTest, EasyTripDatabase, com, Clock, RoomDatabase

### Community 79 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 80 - "Easy Trip"
Cohesion: 0.50
Nodes (4): 高德地图本地密钥配置, Easy Trip, 本地优先 Android 旅行规划, 未授权或离线本地编辑

### Community 86 - "AmapPlaceDataSource.kt"
Cohesion: 0.16
Nodes (8): AmapPlaceDataSource, CallbackBoundary, PoiSearch, CallbackBoundary, com, parsePoiSearchResponse(), AmapPlaceDataSourceTest, PoiResult

### Community 87 - ".setContent"
Cohesion: 0.13
Nodes (19): TripListContentTest, InlineStatus(), Modifier, CompactTripRow(), EmptyTrips(), Modifier, LuggageIllustration(), TripCard() (+11 more)

### Community 88 - "MapPoiUi"
Cohesion: 0.31
Nodes (4): AmapMapHost, PoiHost, WorkspaceFlowTest, MapPoiUi

### Community 89 - "RoomDeleteImpactProviderTest.kt"
Cohesion: 0.18
Nodes (3): RoomDeleteImpactProviderTest, SavedPlaceTagCrossRef, TagEntity

### Community 90 - "reduceMapInteraction"
Cohesion: 0.27
Nodes (6): FocusSearchResult, MapInteractionAction, MapInteractionState, ReconcileSearchResults, reduceMapInteraction(), MapInteractionReducerTest

### Community 91 - "SavedPlace"
Cohesion: 0.16
Nodes (11): LazyListState, Modifier, PlacePoolScrollbarTest, SavedPlace, CollectionDecision, Confirm, decideCollectionToggle(), PendingCollectionRemoval (+3 more)

### Community 93 - "AmapConsentToken"
Cohesion: 0.27
Nodes (4): AmapConsentToken, AppContainer, CoroutineScope, PlaceService

### Community 94 - "FeedbackState"
Cohesion: 0.24
Nodes (11): FeedbackStateTest, EmptyFeedbackState(), ErrorFeedbackState(), FeedbackContent(), FeedbackKind, EMPTY, ERROR, LOADING (+3 more)

### Community 95 - "FakeTrips"
Cohesion: 0.14
Nodes (3): FakeTrips, CreateTrip, java

### Community 97 - "ViewportReason"
Cohesion: 0.33
Nodes (6): ViewportReason, INITIAL, PLACE_SET_CHANGED, SCOPE_CHANGED, SEARCH_FOCUS, VISIBLE_SET_CHANGED

### Community 98 - "TimeMode"
Cohesion: 0.19
Nodes (5): TimeMode, DATED, DRAFT, Flow, TripEntityWithDays

### Community 99 - "LazyScrollbar.kt"
Cohesion: 0.29
Nodes (6): calculateScrollbarThumb(), LazyListState, Modifier, ReadOnlyLazyScrollbar(), ScrollbarThumb, LazyScrollbarGeometryTest

### Community 100 - "settledWorkspaceSheetLevel"
Cohesion: 0.22
Nodes (7): settledWorkspaceSheetLevel(), WorkspaceSheetLevel, COLLAPSED, EXPANDED, HALF, WorkspaceSheetSyncTest, SheetValue

### Community 101 - "AmapPrivacyGate.kt"
Cohesion: 0.30
Nodes (5): ConsentRegistry, ConsentSnapshot, StateFlow, TestConsentGate, ConsentRevocationTest

### Community 102 - "File Structure"
Cohesion: 0.18
Nodes (10): Easy Trip Workspace Navigation Implementation Plan, File Structure, Global Constraints, Task 1: 建立统一导航模型与兼容迁移, Task 2: 建立全程只读 UI 模型, Task 3: Make TripWorkspaceViewModel the single navigation source, Task 4: Separate editable day content from date selection and share presentation, Task 5: Build the 88dp itinerary scope rail and two-column content (+2 more)

### Community 103 - "File Structure"
Cohesion: 0.15
Nodes (12): Easy Trip v1.0 Pencil Implementation Plan, File Structure, Global Constraints, Task 1: 冻结 Pencil 主流程基线, Task 2: 建立 v1.0 Theme 与共享组件, Task 3: 拆分并实现“我的旅行”页面, Task 4: 实现创建旅行状态机与真实提交, Task 5: 拆分工作台并实现地图降级 (+4 more)

### Community 104 - "ConfirmationDialog"
Cohesion: 0.29
Nodes (6): ConfirmationDialogTest, ConfirmationDialog(), ConfirmationSection(), Color, Modifier, TripListScreen()

### Community 105 - "Global Constraints"
Cohesion: 0.20
Nodes (9): Easy Trip Search Collection Implementation Plan, Global Constraints, Task 1: 收敛工作台 Tab 并建立独立搜索路由, Task 2: 完成搜索页状态清理与一次性返回聚焦, Task 3: 统一搜索结果与地点卡片的收藏切换策略, Task 4: 建立稳定的收藏 marker 身份、编号与聚焦样式, Task 5: 接入高德底图 POI 点击与统一地点卡片, Task 6: 为地点池增加只读纵向 scrollbar thumb (+1 more)

### Community 106 - "Easy Trip v1.0 Pencil Reference"
Cohesion: 0.18
Nodes (10): Easy Trip v1.0 Pencil Reference, Layout Verification, Main Flow Frames, Public Components, Responsive Notes, Screenshot Baseline, Semantic Tokens, Shape, Spacing, Size, Elevation (+2 more)

### Community 107 - "Batch 1 visual fix report"
Cohesion: 0.20
Nodes (9): Batch 1 visual fix report, Commit, Fix round 1, Fix round 1 commit, Fix round 1 最终验证, Frame 差异与修复, RED / GREEN, 修改文件 (+1 more)

### Community 108 - "TripWorkspaceAction"
Cohesion: 0.17
Nodes (12): Back, CloseOverlay, OpenOverlay, OpenPrivacySettings, OpenSearch, OpenSettings, Retry, SelectItineraryScope (+4 more)

### Community 114 - "PlacePoolContent"
Cohesion: 0.15
Nodes (15): ConfirmCollectionRemoval, ConfirmDelete, Delete, DismissDialogs, Edit, Modifier, PlacePoolAction, PlacePoolContent() (+7 more)

### Community 119 - "MapLayer"
Cohesion: 0.14
Nodes (10): AmapMapHost, RecordingHost, MapLayer, SATELLITE_ROAD, STANDARD, InMemoryMapPreferences, StateFlow, MapPreferences (+2 more)

### Community 120 - "行程项"
Cohesion: 0.67
Nodes (3): 行程项, 路线段, 收藏地点

### Community 121 - ".searchCollectionMapAndRestorationFlow"
Cohesion: 0.15
Nodes (5): AmapMapHost, PlaceSearchDataSource, RecordingHost, V2AcceptanceTest, PlaceSearchDataSource

### Community 122 - "EasyTripTokens.kt"
Cohesion: 0.52
Nodes (4): EasyTripElevation, EasyTripSizes, EasyTripSpacing, EasyTripTheme

### Community 124 - "DayItineraryViewModel.kt"
Cohesion: 0.29
Nodes (5): Factory, StateFlow, T, ViewModel, ViewModelProvider

### Community 128 - "RouteResult"
Cohesion: 0.20
Nodes (3): V1AcceptanceTest, RouteDataSource, RouteResult

### Community 130 - "TripListAction"
Cohesion: 0.33
Nodes (6): CreateTrip, OpenSettings, OpenTrip, RequestDelete, Retry, TripListAction

### Community 132 - "RouteLegRow.kt"
Cohesion: 0.70
Nodes (4): Modifier, modeLabel(), RouteLegContent(), RouteLegRow()

### Community 133 - ".create"
Cohesion: 0.40
Nodes (4): Factory, CreationExtras, T, ViewModelProvider

## Knowledge Gaps
- **377 isolated node(s):** `guard-adb-install.sh script`, `READY`, `STARTING`, `STARTED`, `CANCELLED` (+372 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **34 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `RouteResult`, `MapUiModelMapperTest`, `FakeRepository`, `RoomRouteLegRepository`, `PlaceSearchReducer`, `.duplicateDragCrossDayTimingOverrideAndRetry`, `AmapComposeMap.kt`, `MapViewportControllerTest`, `TripDay`, `TravelMode`, `RouteLegRepository`, `PlacePoolViewModel`, `PlaceCandidate`, `FakeSavedPlaces`, `GeoPoint`, `AmapRouteDataSource.kt`, `AmapComposeMapTest.kt`, `View`, `Trips`, `TripDay`, `MapFacade.kt`, `TripWorkspaceViewModel`, `Itineraries`, `TripWorkspaceNavigationStateTest`, `AmapPlaceDataSource.kt`, `MapPoiUi`, `reduceMapInteraction`, `SavedPlace`, `EditingPlaces`, `RoomSavedPlaceRepositoryTest`, `.searchCollectionMapAndRestorationFlow`, `RoomRouteLegRepositoryTest`, `MapViewportRenderingPolicyTest`?**
  _High betweenness centrality (0.109) - this node is a cross-community bridge._
- **Why does `TravelMode` connect `TravelMode` to `FakeTripRepository`, `TripSettingsViewModel`, `RoomRouteLegRepository`, `Legs`, `TripDay`, `RouteLegRepository`, `Converters`, `PlaceCandidate`, `Legs`, `GeoPoint`, `Legs`, `Trips`, `FakeRepository`, `TripWorkspaceScreen.kt`, `CreateTripViewModel`, `RoomItineraryRepository`, `TestTripRepository`, `FakeTripRepository`, `EasyTripDatabase`, `Trips`, `.setContent`, `RoomDeleteImpactProviderTest.kt`, `FakeTrips`, `TimeMode`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **Why does `PlaceCandidate` connect `PlaceCandidate` to `RouteResult`, `MapUiModelMapperTest`, `Legs`, `Trips`, `PlaceSearchReducer`, `TripDay`, `RouteLegRepository`, `PlacePoolViewModel`, `FakeSavedPlaces`, `PlaceSearchViewModel`, `Legs`, `Trips`, `TripWorkspaceContent.kt`, `TripDay`, `TripWorkspaceScreen.kt`, `MapFacade.kt`, `GeoPoint`, `TripWorkspaceViewModel`, `Itineraries`, `TripWorkspaceNavigationStateTest`, `AmapPlaceDataSource.kt`, `reduceMapInteraction`, `SavedPlace`, `EditingPlaces`, `RoomSavedPlaceRepositoryTest`, `PlacePoolContent`, `Places`, `.searchCollectionMapAndRestorationFlow`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 25 inferred relationships involving `TripWorkspaceViewModel` (e.g. with `.model()` and `.emptyScopeClearsRequestSoReplacementHostDoesNotReplayInitialViewport()`) actually correct?**
  _`TripWorkspaceViewModel` has 25 INFERRED edges - model-reasoned connections that need verification._
- **Are the 3 inferred relationships involving `TripDay` (e.g. with `FakeTrips` and `.createTrip()`) actually correct?**
  _`TripDay` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `guard-adb-install.sh script`, `READY`, `STARTING` to the rest of the system?**
  _377 weakly-connected nodes found - possible documentation gaps or missing edges._