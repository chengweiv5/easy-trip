# Graph Report - easy-trip  (2026-08-23)

## Corpus Check
- 198 files · ~91,015 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2479 nodes · 5175 edges · 120 communities (97 shown, 23 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 346 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `397018fa`
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
- RealAmapMapHost
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
- ItineraryItemEntity
- Legs
- GeoPoint
- Easy Trip v2 交互与视觉升级设计
- Easy Trip UI 设计公式
- RouteRequest
- MemoryPreferences
- 高德 Android SDK 集成决策
- SelectablePill
- Easy Trip 工作台导航重构设计
- SchemaTest
- AmapRouteDataSource.kt
- MapUiModel
- 2026-08-23-easy-trip-v1-full-ui-implementation.md
- Legs
- AppNavigation.kt
- View
- ItineraryScope
- CallbackBoundary
- Trips
- PlaceDao
- EasyTripDatabase
- Dp
- WorkspaceReadyContent
- AmapSmokeTest
- Easy Trip 搜索与收藏交互设计
- FakeRepository
- DeleteImpactDao
- WorkspaceItineraryContent
- TripWorkspaceScreen
- ItineraryUiModels.kt
- EasyTripPrimaryButton
- AmapComposeMap
- MapFacade.kt
- TripWorkspaceViewModel.kt
- RouteFormattingTest
- AmapServiceException
- TripWorkspaceViewModel
- CreateTripUiState
- RoomItineraryRepository
- MapLifecycleController
- Legs
- TestTripRepository
- TripWorkspaceNavigationStateTest
- ItineraryService
- MapLayerRenderingPolicyTest
- RoutePlannerTest
- WorkspaceMapState
- SharedPreferences
- FakeTripRepository
- .independentSearchSelectionClearsPageAndIsConsumedOnce
- FakeLegs
- Trips
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest
- PoiSearch
- .setContent
- PlacePoolScrollbarTest.kt
- Context
- reduceMapInteraction
- SavedPlace
- CLAUDE.md
- Legs
- FeedbackState
- FakeTrips
- TripDeleteImpact
- ViewportReason
- TripService
- LazyScrollbar.kt
- WorkspaceSheetLevel
- Trips
- File Structure
- File Structure
- Global Constraints
- Easy Trip v1.0 Pencil Reference
- TripWorkspaceAction
- CallbackBoundary
- Places
- Itineraries
- MutableItineraries
- CompactSecondaryButton
- CreateTripAction
- MapLayer
- 行程项
- .searchCollectionMapAndRestorationFlow
- EasyTripTokens.kt
- RoomRouteLegRepositoryTest
- RouteResult
- guard-adb-install.sh

## God Nodes (most connected - your core abstractions)
1. `GeoPoint` - 112 edges
2. `PlaceCandidate` - 67 edges
3. `TravelMode` - 62 edges
4. `TripDay` - 54 edges
5. `TripWorkspaceViewModel` - 52 edges
6. `CreateTrip` - 51 edges
7. `TransportMode` - 49 edges
8. `SavedPlace` - 43 edges
9. `TripRepository` - 41 edges
10. `TripService` - 38 edges

## Surprising Connections (you probably didn't know these)
- `TripWorkspaceViewModel` --calls--> `MapViewportController`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceViewModel.kt → app/src/main/java/com/yangchengwei/easytrip/workspace/MapViewportController.kt
- `FakeTrips` --calls--> `TripDay`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `FakeTrips` --calls--> `TripWithDays`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `RoutePlannerTest` --calls--> `RouteLegWithEndpoints`  [INFERRED]
  app/src/test/java/com/yangchengwei/easytrip/route/domain/RoutePlannerTest.kt → app/src/main/java/com/yangchengwei/easytrip/route/domain/RouteLegRepository.kt
- `PlacePoolViewModel` --calls--> `PlaceSearchReducer`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolViewModel.kt → app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchReducer.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **旅行编排数据模型** — docs_superpowers_specs_2026_08_21_saved_place, docs_superpowers_specs_2026_08_21_itinerary_item, docs_superpowers_specs_2026_08_21_route_leg [EXTRACTED 1.00]

## Communities (120 total, 23 thin omitted)

### Community 0 - "Easy Trip v1.0 全量 UI 与交互落地设计"
Cohesion: 0.04
Nodes (46): 10.1 批次 1：旅行入口与创建, 10.2 批次 2：搜索、连续收藏与地点池, 10.3 批次 3：从地点池加入行程与旅行日, 10.4 批次 4：单日编辑、交通路段与全程, 10.5 批次 5：设置、日期、权限与完整回归, 10. 五个实施批次, 11.1 本地优先, 11.2 收藏 (+38 more)

### Community 2 - "FakeRepository"
Cohesion: 0.09
Nodes (21): connected(), hasValidatedInternet(), StateFlow, NetworkMonitor, onAvailable(), onCapabilitiesChanged(), onLost(), Active (+13 more)

### Community 3 - "FakeTripRepository"
Cohesion: 0.21
Nodes (4): FakeTripRepository, CreateTrip, Flow, TripServiceTest

### Community 4 - "awaitSdkCallback"
Cohesion: 0.16
Nodes (8): awaitSdkCallback(), T, FakeBoundary, CallbackBoundary, Result, T, SdkCallbackBridgeTest, R

### Community 5 - "TripSettingsViewModel"
Cohesion: 0.10
Nodes (11): androidx, DayDeleteImpact, DayUi, Factory, StateFlow, T, ViewModel, ViewModelProvider (+3 more)

### Community 6 - "RoomRouteLegRepository"
Cohesion: 0.05
Nodes (15): AmapConsentToken, AmapPrivacyGate, AmapPrivacyStateMachine, ConsentRegistry, ConsentSnapshot, StateFlow, TestConsentGate, AppContainer (+7 more)

### Community 7 - "Legs"
Cohesion: 0.05
Nodes (9): FakeMapPreferences, Itineraries, com, CreateTrip, Role, Legs, MapLayerFlowTest, Places (+1 more)

### Community 8 - "Trips"
Cohesion: 0.06
Nodes (10): Itineraries, com, CreateTrip, Flow, Trips, Legs, Places, Trips (+2 more)

### Community 9 - "PlaceSearchReducer"
Cohesion: 0.20
Nodes (8): Job, PlaceSearchDataSource, StateFlow, PlaceSearchReducer, PlaceSearchState, PlaceSearchDataSource, PlaceSearchReducerTest, SearchSource

### Community 10 - ".duplicateDragCrossDayTimingOverrideAndRetry"
Cohesion: 0.18
Nodes (6): Add, FakeCoordinator, ItineraryEditingTest, kotlinx, Move, Timing

### Community 11 - "RealAmapMapHost"
Cohesion: 0.11
Nodes (5): AmapMapHost, android, View, RealAmapMapHost, toMapPoiUi()

### Community 12 - "DayItineraryViewModel"
Cohesion: 0.10
Nodes (7): DayItineraryUiState, DayItineraryViewModel, Factory, StateFlow, T, ViewModel, ViewModelProvider

### Community 13 - "TripListViewModel"
Cohesion: 0.10
Nodes (15): CreateTimeMode, DATED, DRAFT, Factory, CreationExtras, Job, StateFlow, T (+7 more)

### Community 14 - "MapViewportControllerTest"
Cohesion: 0.12
Nodes (4): MapViewportRequest, MapViewportController, MapViewportControllerTest, MapViewportRenderingPolicyTest

### Community 15 - "TripDay"
Cohesion: 0.08
Nodes (12): Flow, Flow, InsertSide, AFTER, BEFORE, Flow, TripDay, TripRepository (+4 more)

### Community 16 - "RouteStatus"
Cohesion: 0.07
Nodes (11): OfflineRecoveryTest, RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK, Flow (+3 more)

### Community 17 - "TravelMode"
Cohesion: 0.10
Nodes (15): TransportMode, DRIVE, TAXI, TRANSIT, WALK, TravelMode, FLEXIBLE, SELF_DRIVE (+7 more)

### Community 18 - "CreateTrip"
Cohesion: 0.07
Nodes (10): CascadeDeleteTest, RoomSavedPlaceRepositoryTest, IdFactory, T, MutableClock, RoomTripRepositoryTest, V1PencilFlowTest, CreateTrip (+2 more)

### Community 19 - "RouteLegRepository"
Cohesion: 0.09
Nodes (12): Flow, Flow, Flow, Flow, RouteLegRepository, RouteLegWithEndpoints, Failure, plan() (+4 more)

### Community 20 - "TripDao"
Cohesion: 0.13
Nodes (5): Flow, TripDao, TripEntityWithDays, TripDayEntity, TripEntity

### Community 21 - "Easy Trip 一期设计"
Cohesion: 0.17
Nodes (12): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 离线路线等待与联网续算, 同一地点可重复安排行程 (+4 more)

### Community 22 - "Converters"
Cohesion: 0.08
Nodes (9): Converters, TimeMode, DATED, DRAFT, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT (+1 more)

### Community 23 - "PlacePoolViewModel"
Cohesion: 0.07
Nodes (17): PlaceSearchDataSource, PlacePoolFlowTest, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, PendingCollectionRemoval, Modifier, PlacePoolSheet() (+9 more)

### Community 24 - "PlaceCandidate"
Cohesion: 0.09
Nodes (11): PlaceCandidate, PlaceSearchDataSource, Flow, RoomSavedPlaceRepository, AlreadySaved, Flow, PlaceTag, Saved (+3 more)

### Community 25 - "Easy Trip v1.0 Pencil 设计落地方案"
Cohesion: 0.05
Nodes (39): 10.1 JVM, 10.2 Compose instrumentation, 10.3 数据与导航集成, 10.4 验证命令, 10. 测试策略, 11. 视觉验收, 12. 验收标准, 13. 协作边界 (+31 more)

### Community 26 - "ItineraryItemEntity"
Cohesion: 0.13
Nodes (5): DayItems, DayItineraryRow, ItineraryDao, Flow, ItineraryItemEntity

### Community 27 - "Legs"
Cohesion: 0.06
Nodes (7): DayItinerarySelectionTest, Itineraries, CreateTrip, Flow, Trips, Legs, Trips

### Community 28 - "GeoPoint"
Cohesion: 0.12
Nodes (10): FakeItineraries, GeoPoint, DayItinerary, ItineraryItem, ItineraryPlace, ItineraryRepository, Flow, Result (+2 more)

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.17
Nodes (15): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+7 more)

### Community 30 - "Easy Trip UI 设计公式"
Cohesion: 0.05
Nodes (43): 10.1 无网, 10.2 路线失败, 10.3 地图加载中或失败, 10.4 部分成功, 10.5 提交中与目标日失效, 10.6 保存失败, 10. 本地优先与状态公式, 11.1 地图服务授权 (+35 more)

### Community 31 - "RouteRequest"
Cohesion: 0.16
Nodes (12): AmapRouteDataSource, RouteDataSource, parseRouteResult(), RoutePathData, selectUsablePath(), validateRouteRequest(), RouteMode, DRIVE (+4 more)

### Community 32 - "MemoryPreferences"
Cohesion: 0.17
Nodes (3): SharedPreferencesMapPreferences, MapPreferencesTest, MemoryPreferences

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.14
Nodes (14): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+6 more)

### Community 34 - "SelectablePill"
Cohesion: 0.19
Nodes (9): SelectablePillTest, Modifier, Role, SelectablePill(), SelectablePillStyle, DaySelector(), LazyListState, Modifier (+1 more)

### Community 35 - "Easy Trip 工作台导航重构设计"
Cohesion: 0.06
Nodes (33): 10. 数据模型, 11. 空态与错误处理, 12. 可访问性, 13. 测试策略, 14. 验收标准, 1. 目标, 2. 非目标, 3. 信息架构 (+25 more)

### Community 37 - "AmapRouteDataSource.kt"
Cohesion: 0.33
Nodes (6): RouteSearch, RouteCallback, BusRouteResult, DriveRouteResult, RideRouteResult, WalkRouteResult

### Community 38 - "MapUiModel"
Cohesion: 0.14
Nodes (6): AmapComposeMapTest, AmapMapHost, Lifecycle, LifecycleOwner, TestOwner, MapUiModel

### Community 39 - "2026-08-23-easy-trip-v1-full-ui-implementation.md"
Cohesion: 0.07
Nodes (27): Batch 1 Gate, Batch 1：旅行入口、创建与删除, Batch 2 Gate, Batch 2：搜索、连续收藏与地点池, Batch 3 Gate, Batch 3：从地点池加入行程与旅行日, Batch 4 Gate, Batch 4：单日编辑、RouteLeg、全程与抽屉 (+19 more)

### Community 40 - "Legs"
Cohesion: 0.05
Nodes (7): Itineraries, com, CreateTrip, Legs, Places, Trips, WorkspaceSearchTabsTest

### Community 41 - "AppNavigation.kt"
Cohesion: 0.21
Nodes (11): AppNavigation(), AppNavigationDependencies, AppNavigationObserver, android, com, tripSearchRoute(), MainActivity, AmapPlaceDataSource (+3 more)

### Community 42 - "View"
Cohesion: 0.05
Nodes (10): AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost (+2 more)

### Community 43 - "ItineraryScope"
Cohesion: 0.15
Nodes (10): Day, decodeItineraryScope(), encodeItineraryScope(), ItineraryScope, reconcileItineraryScope(), RestoredWorkspaceNavigation, restoreWorkspaceNavigation(), toMapScope() (+2 more)

### Community 44 - "CallbackBoundary"
Cohesion: 0.12
Nodes (10): OneShotCallback, BridgeState, CANCELLED, COMPLETED, READY, STARTED, STARTING, CallbackBoundary (+2 more)

### Community 45 - "Trips"
Cohesion: 0.07
Nodes (10): Itineraries, AmapMapHost, CreateTrip, Lifecycle, LifecycleOwner, RecordingHost, SavedPlaces, SearchMapFocusTest (+2 more)

### Community 46 - "PlaceDao"
Cohesion: 0.16
Nodes (3): Flow, PlaceDao, PlaceSnapshotRow

### Community 47 - "EasyTripDatabase"
Cohesion: 0.06
Nodes (7): CascadeCountDao, EasyTripDatabase, com, SchemaItineraryDao, SchemaPlaceDao, SchemaRouteDao, RoomDatabase

### Community 48 - "Dp"
Cohesion: 0.19
Nodes (9): EasyTripButtonTest, EasyTripIconButton(), Modifier, ItineraryItemCard(), ItineraryItemRow(), Modifier, semanticsActions(), Color (+1 more)

### Community 49 - "WorkspaceReadyContent"
Cohesion: 0.19
Nodes (14): Modifier, label(), LayerIcon(), toSheetValue(), TripWorkspaceContent(), WorkspacePageMessage(), WorkspaceReadyContent(), WorkspaceSheetHandle() (+6 more)

### Community 51 - "Easy Trip 搜索与收藏交互设计"
Cohesion: 0.07
Nodes (27): 10. 边界与错误处理, 11. 测试策略, 12. 验收标准, 1. 背景与目标, 2. 非目标, 3. 当前实现基线, 4.1 工作台, 4.2 独立搜索页 (+19 more)

### Community 52 - "FakeRepository"
Cohesion: 0.16
Nodes (7): CreateTripViewModelTest, FakeRepository, CreateTrip, Flow, SavedStateHandle, receiveOne(), FakeRepository

### Community 54 - "WorkspaceItineraryContent"
Cohesion: 0.29
Nodes (6): ItineraryScopeRailTest, ItineraryScopeRail(), Modifier, ScopeItem(), Modifier, WorkspaceItineraryContent()

### Community 55 - "TripWorkspaceScreen"
Cohesion: 0.08
Nodes (8): Itineraries, AmapMapHost, com, Legs, Places, PoiHost, WorkspaceFlowTest, TripWorkspaceScreen()

### Community 56 - "ItineraryUiModels.kt"
Cohesion: 0.15
Nodes (14): WholeTripItineraryContentTest, ItineraryItemUi, RouteLegUi, toItineraryItemUi(), toRouteErrorSummary(), toRouteLegUi(), WholeTripDayUi, Modifier (+6 more)

### Community 57 - "EasyTripPrimaryButton"
Cohesion: 0.18
Nodes (18): ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton(), EasyTripButton(), EasyTripButtonStyle, DANGER (+10 more)

### Community 58 - "AmapComposeMap"
Cohesion: 0.15
Nodes (9): AmapComposeMap(), Bounds, Modifier, MapHostCallbackGuard, MapZoomButton(), SinglePoint, ViewportCommand, ViewportRendering (+1 more)

### Community 59 - "MapFacade.kt"
Cohesion: 0.10
Nodes (21): CorruptRoute, FocusSearchResult, formatOccurrenceBadge(), MapInteractionAction, MapMarkerKind, SAVED_ITINERARY, SAVED_PLACE_POOL, UNSAVED_SEARCH (+13 more)

### Community 60 - "TripWorkspaceViewModel.kt"
Cohesion: 0.15
Nodes (16): consumeSearchSelection(), Flow, Job, SavedStateHandle, StateFlow, ViewModel, observeSnapshots(), restoreWorkspaceTab() (+8 more)

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 62 - "AmapServiceException"
Cohesion: 0.30
Nodes (5): AmapServiceException, parsePlaces(), RawPlace, PlaceContractsTest, Exception

### Community 63 - "TripWorkspaceViewModel"
Cohesion: 0.13
Nodes (11): MapPoiUi, Factory, CreationExtras, T, ViewModelProvider, SearchResultSelection, TripWorkspaceUiState, TripWorkspaceViewModel (+3 more)

### Community 64 - "CreateTripUiState"
Cohesion: 0.19
Nodes (7): CreateTripDialogTest, CreateTripDialog(), CreateTripUiState, CreateTripValidation, validateCreateTrip(), ValidCreateTrip, CreateTripValidatorTest

### Community 65 - "RoomItineraryRepository"
Cohesion: 0.11
Nodes (6): ItineraryTransactionTest, SequenceIds, RoomItineraryRepository, AdjacencyDiff, Edge, AdjacencyPlannerTest

### Community 68 - "TestTripRepository"
Cohesion: 0.16
Nodes (4): CreateTrip, TestImpacts, TestTripRepository, TripListViewModelTest

### Community 69 - "TripWorkspaceNavigationStateTest"
Cohesion: 0.30
Nodes (3): SavedStateHandle, Trips, TripWorkspaceNavigationStateTest

### Community 72 - "RoutePlannerTest"
Cohesion: 0.38
Nodes (3): RouteDataSource, RoutePlannerTest, RouteDataSource

### Community 73 - "WorkspaceMapState"
Cohesion: 0.14
Nodes (13): TripWorkspaceContentTest, Modifier, WorkspaceMapFallback(), ConsentRequired, Error, Failed, Loading, NotFound (+5 more)

### Community 75 - "FakeTripRepository"
Cohesion: 0.15
Nodes (7): FakeImpacts, FakeTripRepository, CreateTrip, Flow, Role, MoveCall, TripFlowTest

### Community 76 - ".independentSearchSelectionClearsPageAndIsConsumedOnce"
Cohesion: 0.18
Nodes (3): Places, PlacePoolUiState, PlaceSearchScreen()

### Community 79 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 80 - "Easy Trip"
Cohesion: 0.50
Nodes (4): 高德地图本地密钥配置, Easy Trip, 本地优先 Android 旅行规划, 未授权或离线本地编辑

### Community 86 - "PoiSearch"
Cohesion: 0.22
Nodes (5): CallbackBoundary, PoiSearch, CallbackBoundary, com, PoiResult

### Community 87 - ".setContent"
Cohesion: 0.13
Nodes (14): TripListContentTest, Content, CreateTrip, Empty, Error, Loading, OpenSettings, OpenTrip (+6 more)

### Community 88 - "PlacePoolScrollbarTest.kt"
Cohesion: 0.46
Nodes (3): LazyListState, Modifier, PlacePoolScrollbarTest

### Community 89 - "Context"
Cohesion: 0.13
Nodes (5): AppIconResourceTest, RoomDeleteImpactProviderTest, SavedPlaceTagCrossRef, TagEntity, Context

### Community 90 - "reduceMapInteraction"
Cohesion: 0.43
Nodes (3): MapInteractionState, reduceMapInteraction(), MapInteractionReducerTest

### Community 91 - "SavedPlace"
Cohesion: 0.20
Nodes (8): SavedPlace, CollectionDecision, Confirm, decideCollectionToggle(), RemoveNow, Save, EditSavedPlaceDialog(), CollectionTogglePolicyTest

### Community 94 - "FeedbackState"
Cohesion: 0.24
Nodes (11): FeedbackStateTest, EmptyFeedbackState(), ErrorFeedbackState(), FeedbackContent(), FeedbackKind, EMPTY, ERROR, LOADING (+3 more)

### Community 95 - "FakeTrips"
Cohesion: 0.14
Nodes (3): FakeTrips, CreateTrip, java

### Community 97 - "ViewportReason"
Cohesion: 0.33
Nodes (6): ViewportReason, INITIAL, PLACE_SET_CHANGED, SCOPE_CHANGED, SEARCH_FOCUS, VISIBLE_SET_CHANGED

### Community 98 - "TripService"
Cohesion: 0.17
Nodes (3): com, CreateTrip, TripService

### Community 99 - "LazyScrollbar.kt"
Cohesion: 0.29
Nodes (6): calculateScrollbarThumb(), LazyListState, Modifier, ReadOnlyLazyScrollbar(), ScrollbarThumb, LazyScrollbarGeometryTest

### Community 100 - "WorkspaceSheetLevel"
Cohesion: 0.40
Nodes (4): WorkspaceSheetLevel, COLLAPSED, EXPANDED, HALF

### Community 102 - "File Structure"
Cohesion: 0.18
Nodes (10): Easy Trip Workspace Navigation Implementation Plan, File Structure, Global Constraints, Task 1: 建立统一导航模型与兼容迁移, Task 2: 建立全程只读 UI 模型, Task 3: Make TripWorkspaceViewModel the single navigation source, Task 4: Separate editable day content from date selection and share presentation, Task 5: Build the 88dp itinerary scope rail and two-column content (+2 more)

### Community 103 - "File Structure"
Cohesion: 0.15
Nodes (12): Easy Trip v1.0 Pencil Implementation Plan, File Structure, Global Constraints, Task 1: 冻结 Pencil 主流程基线, Task 2: 建立 v1.0 Theme 与共享组件, Task 3: 拆分并实现“我的旅行”页面, Task 4: 实现创建旅行状态机与真实提交, Task 5: 拆分工作台并实现地图降级 (+4 more)

### Community 105 - "Global Constraints"
Cohesion: 0.20
Nodes (9): Easy Trip Search Collection Implementation Plan, Global Constraints, Task 1: 收敛工作台 Tab 并建立独立搜索路由, Task 2: 完成搜索页状态清理与一次性返回聚焦, Task 3: 统一搜索结果与地点卡片的收藏切换策略, Task 4: 建立稳定的收藏 marker 身份、编号与聚焦样式, Task 5: 接入高德底图 POI 点击与统一地点卡片, Task 6: 为地点池增加只读纵向 scrollbar thumb (+1 more)

### Community 106 - "Easy Trip v1.0 Pencil Reference"
Cohesion: 0.18
Nodes (10): Easy Trip v1.0 Pencil Reference, Layout Verification, Main Flow Frames, Public Components, Responsive Notes, Screenshot Baseline, Semantic Tokens, Shape, Spacing, Size, Elevation (+2 more)

### Community 108 - "TripWorkspaceAction"
Cohesion: 0.20
Nodes (10): Back, OpenPrivacySettings, OpenSearch, OpenSettings, Retry, SelectItineraryScope, SelectMapLayer, SelectSection (+2 more)

### Community 114 - "CompactSecondaryButton"
Cohesion: 0.19
Nodes (10): CompactDangerButton(), CompactPrimaryButton(), CompactSecondaryButton(), Modifier, DayItinerarySheet(), Modifier, EditTimingDialog(), Modifier (+2 more)

### Community 116 - "CreateTripAction"
Cohesion: 0.22
Nodes (8): CreateTripAction, DayCountChanged, Dismiss, NameChanged, StartDateChanged, Submit, TimeModeChanged, TravelModeChanged

### Community 119 - "MapLayer"
Cohesion: 0.12
Nodes (10): AmapMapHost, TestMapHost, MapLayer, SATELLITE_ROAD, STANDARD, InMemoryMapPreferences, StateFlow, MapPreferences (+2 more)

### Community 120 - "行程项"
Cohesion: 0.67
Nodes (3): 行程项, 路线段, 收藏地点

### Community 121 - ".searchCollectionMapAndRestorationFlow"
Cohesion: 0.15
Nodes (5): AmapMapHost, PlaceSearchDataSource, RecordingHost, V2AcceptanceTest, PlaceSearchDataSource

### Community 122 - "EasyTripTokens.kt"
Cohesion: 0.52
Nodes (4): EasyTripElevation, EasyTripSizes, EasyTripSpacing, EasyTripTheme

### Community 128 - "RouteResult"
Cohesion: 0.18
Nodes (6): V1AcceptanceTest, mapWholeTripDays(), RouteDataSource, RouteResult, DayMapSnapshot, WholeTripItineraryMapperTest

## Knowledge Gaps
- **322 isolated node(s):** `Global Constraints`, `Target Navigation`, `Task 1：将创建旅行迁移为独立页面`, `Task 2：校正旅行列表和删除影响`, `Batch 1 Gate` (+317 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **23 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `RouteResult`, `MapUiModelMapperTest`, `FakeRepository`, `RoomRouteLegRepository`, `PlaceSearchReducer`, `.duplicateDragCrossDayTimingOverrideAndRetry`, `RealAmapMapHost`, `MapViewportControllerTest`, `TripDay`, `TravelMode`, `CreateTrip`, `RouteLegRepository`, `PlacePoolViewModel`, `PlaceCandidate`, `RouteRequest`, `AmapRouteDataSource.kt`, `MapUiModel`, `Trips`, `AmapSmokeTest`, `TripWorkspaceScreen`, `AmapComposeMap`, `MapFacade.kt`, `TripWorkspaceViewModel.kt`, `AmapServiceException`, `TripWorkspaceViewModel`, `TripWorkspaceNavigationStateTest`, `RoutePlannerTest`, `.independentSearchSelectionClearsPageAndIsConsumedOnce`, `PoiSearch`, `PlacePoolScrollbarTest.kt`, `reduceMapInteraction`, `SavedPlace`, `.searchCollectionMapAndRestorationFlow`, `RoomRouteLegRepositoryTest`?**
  _High betweenness centrality (0.138) - this node is a cross-community bridge._
- **Why does `TravelMode` connect `TravelMode` to `FakeTripRepository`, `TripSettingsViewModel`, `Legs`, `TripListViewModel`, `TripDay`, `RouteStatus`, `RouteLegRepository`, `TripDao`, `Converters`, `ItineraryItemEntity`, `Legs`, `GeoPoint`, `Legs`, `Trips`, `FakeRepository`, `CreateTripUiState`, `RoomItineraryRepository`, `TestTripRepository`, `FakeTripRepository`, `Trips`, `.setContent`, `Context`, `FakeTrips`, `CompactSecondaryButton`, `CreateTripAction`?**
  _High betweenness centrality (0.084) - this node is a cross-community bridge._
- **Why does `TripRepository` connect `TripDay` to `FakeTripRepository`, `TripSettingsViewModel`, `Legs`, `Trips`, `DayItineraryViewModel`, `TripListViewModel`, `CreateTrip`, `RouteLegRepository`, `Legs`, `GeoPoint`, `Legs`, `AppNavigation.kt`, `Trips`, `FakeRepository`, `TripWorkspaceViewModel.kt`, `TestTripRepository`, `FakeTripRepository`, `Trips`, `FakeTrips`, `Trips`?**
  _High betweenness centrality (0.083) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 3 inferred relationships involving `TripDay` (e.g. with `FakeTrips` and `.createTrip()`) actually correct?**
  _`TripDay` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Are the 22 inferred relationships involving `TripWorkspaceViewModel` (e.g. with `.model()` and `.emptyScopeClearsRequestSoReplacementHostDoesNotReplayInitialViewport()`) actually correct?**
  _`TripWorkspaceViewModel` has 22 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Global Constraints`, `Target Navigation`, `Task 1：将创建旅行迁移为独立页面` to the rest of the system?**
  _322 weakly-connected nodes found - possible documentation gaps or missing edges._