# Graph Report - easy-trip-v1-pencil  (2026-08-23)

## Corpus Check
- 192 files · ~72,908 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2360 nodes · 5059 edges · 132 communities (93 shown, 39 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 346 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `1182d024`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- TagEntity
- MapUiModelMapperTest
- FakeRepository
- FakeTripRepository
- awaitSdkCallback
- TripSettingsViewModel
- AmapConsentToken
- Legs
- Trips
- PlaceSearchReducer
- .duplicateDragCrossDayTimingOverrideAndRetry
- RealAmapMapHost
- DayItineraryViewModel
- TripListViewModel
- GeoPoint
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
- ItineraryRepository
- Easy Trip v2 交互与视觉升级设计
- TripDayEntity
- RouteRequest
- MemoryPreferences
- 高德 Android SDK 集成决策
- Dp
- Easy Trip 工作台导航重构设计
- SchemaTest
- AmapRouteDataSource.kt
- MapUiModel
- RoomRouteLegRepository
- Legs
- MapLayer
- View
- ItineraryScope
- PlaceService
- Legs
- TimeMode
- CascadeCountDao
- AppNavigation.kt
- settledWorkspaceSheetLevel
- AmapSmokeTest
- Easy Trip 搜索与收藏交互设计
- FakeRepository
- ItineraryUiModels.kt
- PlaceDao
- TripWorkspaceScreen
- MapViewportRenderingPolicyTest
- EasyTripPrimaryButton
- AmapComposeMap
- MapFacade.kt
- TripWorkspaceViewModel.kt
- RouteFormattingTest
- AmapServiceException
- TripWorkspaceViewModel
- CreateTripUiState
- ItineraryTransactionTest
- MapLifecycleController
- RoomSavedPlaceRepositoryTest
- TestTripRepository
- TripWorkspaceNavigationStateTest
- ItineraryService
- MapLayerRenderingPolicyTest
- RoutePlannerTest
- WorkspaceMapState
- AmapMapHost
- AppNavigation
- MainActivity
- FakeLegs
- DeleteImpactDao
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest
- AmapPlaceDataSource.kt
- .setContent
- WorkspaceReadyContent
- RoomDeleteImpactProviderTest
- V1AcceptanceTest
- SavedPlace
- CLAUDE.md
- Legs
- FeedbackState
- FakeTrips
- EasyTripDatabase
- RoomTripRepository
- TripService
- LazyScrollbar.kt
- reduceMapInteraction
- Trips
- File Structure
- File Structure
- RoomItineraryRepository
- Global Constraints
- Easy Trip v1.0 Pencil Reference
- PlacePoolScrollbarTest.kt
- TripWorkspaceAction
- OneShotCallback
- Places
- formatOccurrenceBadge
- Itineraries
- MutableItineraries
- DayItinerarySheet
- Edge
- CreateTripAction
- .switchingFromSelfDriveToFlexibleRecommendsByDistanceAndClearsCache
- AmapMapHost
- TestMapHost
- 行程项
- RecordingHost
- EasyTripTokens.kt
- AmapMapHost
- AmapMapHost
- BridgeState
- TripListAction
- ViewportReason
- AppIconResourceTest
- MapMarkerKind
- WorkspaceMapFallback
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
- `FakeTrips` --calls--> `TripDay`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `FakeTrips` --calls--> `TripWithDays`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `RoutePlannerTest` --calls--> `GeoPoint`  [INFERRED]
  app/src/test/java/com/yangchengwei/easytrip/route/domain/RoutePlannerTest.kt → app/src/main/java/com/yangchengwei/easytrip/core/model/GeoPoint.kt
- `CompactPrimaryButton()` --calls--> `EasyTripPrimaryButton()`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/core/ui/component/CompactActionButton.kt → app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EasyTripButton.kt
- `CompactSecondaryButton()` --calls--> `EasyTripSecondaryButton()`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/core/ui/component/CompactActionButton.kt → app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EasyTripButton.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **旅行编排数据模型** — docs_superpowers_specs_2026_08_21_saved_place, docs_superpowers_specs_2026_08_21_itinerary_item, docs_superpowers_specs_2026_08_21_route_leg [EXTRACTED 1.00]

## Communities (132 total, 39 thin omitted)

### Community 0 - "TagEntity"
Cohesion: 0.13
Nodes (3): SchemaPlaceDao, SavedPlaceTagCrossRef, TagEntity

### Community 2 - "FakeRepository"
Cohesion: 0.09
Nodes (21): connected(), hasValidatedInternet(), StateFlow, NetworkMonitor, onAvailable(), onCapabilitiesChanged(), onLost(), Active (+13 more)

### Community 3 - "FakeTripRepository"
Cohesion: 0.21
Nodes (4): FakeTripRepository, CreateTrip, Flow, TripServiceTest

### Community 4 - "awaitSdkCallback"
Cohesion: 0.13
Nodes (10): awaitSdkCallback(), CallbackBoundary, cleanupBoundary(), T, FakeBoundary, CallbackBoundary, Result, T (+2 more)

### Community 5 - "TripSettingsViewModel"
Cohesion: 0.08
Nodes (14): androidx, DayDeleteImpact, DayUi, DeleteImpactProvider, Factory, StateFlow, T, ViewModel (+6 more)

### Community 6 - "AmapConsentToken"
Cohesion: 0.09
Nodes (13): AmapConsentToken, AmapPrivacyGate, AmapPrivacyStateMachine, ConsentRegistry, ConsentSnapshot, StateFlow, TestConsentGate, AppContainer (+5 more)

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
Nodes (3): AmapMapHost, android, RealAmapMapHost

### Community 12 - "DayItineraryViewModel"
Cohesion: 0.10
Nodes (7): DayItineraryUiState, DayItineraryViewModel, Factory, StateFlow, T, ViewModel, ViewModelProvider

### Community 13 - "TripListViewModel"
Cohesion: 0.12
Nodes (11): CreateTimeMode, DATED, DRAFT, Factory, CreationExtras, Job, StateFlow, T (+3 more)

### Community 14 - "GeoPoint"
Cohesion: 0.11
Nodes (7): GeoPoint, Result, PolylineCodec, MapViewportRequest, MapViewportController, PolylineCodecTest, MapViewportControllerTest

### Community 15 - "TripDay"
Cohesion: 0.10
Nodes (11): Flow, InsertSide, AFTER, BEFORE, Flow, TripDay, TripRepository, TripSummary (+3 more)

### Community 16 - "RouteStatus"
Cohesion: 0.08
Nodes (11): RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK, Flow, RouteLegDao (+3 more)

### Community 17 - "TravelMode"
Cohesion: 0.10
Nodes (14): TransportMode, DRIVE, TAXI, TRANSIT, WALK, TravelMode, FLEXIBLE, SELF_DRIVE (+6 more)

### Community 18 - "CreateTrip"
Cohesion: 0.20
Nodes (3): T, RoomTripRepositoryTest, CreateTrip

### Community 19 - "RouteLegRepository"
Cohesion: 0.08
Nodes (13): Flow, Flow, Flow, Flow, RouteLegRepository, RouteLegWithEndpoints, RouteResult, Failure (+5 more)

### Community 21 - "Easy Trip 一期设计"
Cohesion: 0.17
Nodes (12): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 离线路线等待与联网续算, 同一地点可重复安排行程 (+4 more)

### Community 22 - "Converters"
Cohesion: 0.10
Nodes (6): Converters, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT, UNSUPPORTED_TRANSIT

### Community 23 - "PlacePoolViewModel"
Cohesion: 0.07
Nodes (17): PlaceSearchDataSource, PlacePoolFlowTest, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, PendingCollectionRemoval, Modifier, PlacePoolSheet() (+9 more)

### Community 24 - "PlaceCandidate"
Cohesion: 0.11
Nodes (10): PlaceCandidate, PlaceSearchDataSource, Flow, RoomSavedPlaceRepository, AlreadySaved, Flow, PlaceTag, Saved (+2 more)

### Community 25 - "Easy Trip v1.0 Pencil 设计落地方案"
Cohesion: 0.05
Nodes (39): 10.1 JVM, 10.2 Compose instrumentation, 10.3 数据与导航集成, 10.4 验证命令, 10. 测试策略, 11. 视觉验收, 12. 验收标准, 13. 协作边界 (+31 more)

### Community 26 - "ItineraryItemEntity"
Cohesion: 0.12
Nodes (7): DayItems, DayItineraryRow, ItineraryDao, Flow, ItineraryItemEntity, Modifier, PlaceSearchResults()

### Community 27 - "Legs"
Cohesion: 0.06
Nodes (7): DayItinerarySelectionTest, Itineraries, CreateTrip, Flow, Trips, Legs, Trips

### Community 28 - "ItineraryRepository"
Cohesion: 0.14
Nodes (10): FakeItineraries, DayItinerary, ItineraryItem, ItineraryPlace, ItineraryRepository, Flow, mapWholeTripDays(), DayMapSnapshot (+2 more)

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.17
Nodes (15): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+7 more)

### Community 30 - "TripDayEntity"
Cohesion: 0.19
Nodes (4): OfflineRecoveryTest, CreateTrip, TripDayEntity, TripEntity

### Community 31 - "RouteRequest"
Cohesion: 0.14
Nodes (13): AmapRouteDataSource, RouteDataSource, parseRouteResult(), RoutePathData, selectUsablePath(), validateRouteRequest(), RouteDataSource, RouteMode (+5 more)

### Community 32 - "MemoryPreferences"
Cohesion: 0.12
Nodes (5): SharedPreferencesMapPreferences, Editor, MapPreferencesTest, MemoryPreferences, SharedPreferences

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.14
Nodes (14): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+6 more)

### Community 34 - "Dp"
Cohesion: 0.14
Nodes (12): SelectablePillTest, EasyTripIconButton(), Modifier, Modifier, Role, SelectablePill(), SelectablePillStyle, DaySelector() (+4 more)

### Community 35 - "Easy Trip 工作台导航重构设计"
Cohesion: 0.06
Nodes (33): 10. 数据模型, 11. 空态与错误处理, 12. 可访问性, 13. 测试策略, 14. 验收标准, 1. 目标, 2. 非目标, 3. 信息架构 (+25 more)

### Community 37 - "AmapRouteDataSource.kt"
Cohesion: 0.19
Nodes (8): CallbackBoundary, RouteSearch, CallbackBoundary, RouteCallback, BusRouteResult, DriveRouteResult, RideRouteResult, WalkRouteResult

### Community 38 - "MapUiModel"
Cohesion: 0.25
Nodes (5): AmapComposeMapTest, Lifecycle, LifecycleOwner, TestOwner, MapUiModel

### Community 40 - "Legs"
Cohesion: 0.05
Nodes (7): Itineraries, com, CreateTrip, Legs, Places, Trips, WorkspaceSearchTabsTest

### Community 41 - "MapLayer"
Cohesion: 0.14
Nodes (10): AmapMapHost, RecordingHost, MapLayer, SATELLITE_ROAD, STANDARD, InMemoryMapPreferences, StateFlow, MapPreferences (+2 more)

### Community 42 - "View"
Cohesion: 0.07
Nodes (7): AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, View

### Community 43 - "ItineraryScope"
Cohesion: 0.08
Nodes (21): ItineraryScopeRailTest, WholeTripItineraryContentTest, ItineraryScopeRail(), Modifier, ScopeItem(), WholeTripDayUi, dayHeading(), Modifier (+13 more)

### Community 45 - "Legs"
Cohesion: 0.05
Nodes (13): Itineraries, com, CreateTrip, Lifecycle, LifecycleOwner, Legs, Places, SavedPlaces (+5 more)

### Community 46 - "TimeMode"
Cohesion: 0.19
Nodes (5): TimeMode, DATED, DRAFT, Flow, TripEntityWithDays

### Community 47 - "CascadeCountDao"
Cohesion: 0.09
Nodes (3): CascadeCountDao, SchemaItineraryDao, SchemaRouteDao

### Community 48 - "AppNavigation.kt"
Cohesion: 0.26
Nodes (9): tripSearchRoute(), CompactDangerButton(), CompactPrimaryButton(), CompactSecondaryButton(), Modifier, TripListScreen(), TripSettingsScreen(), Modifier (+1 more)

### Community 49 - "settledWorkspaceSheetLevel"
Cohesion: 0.22
Nodes (7): settledWorkspaceSheetLevel(), WorkspaceSheetLevel, COLLAPSED, EXPANDED, HALF, WorkspaceSheetSyncTest, SheetValue

### Community 51 - "Easy Trip 搜索与收藏交互设计"
Cohesion: 0.07
Nodes (27): 10. 边界与错误处理, 11. 测试策略, 12. 验收标准, 1. 背景与目标, 2. 非目标, 3. 当前实现基线, 4.1 工作台, 4.2 独立搜索页 (+19 more)

### Community 52 - "FakeRepository"
Cohesion: 0.15
Nodes (7): CreateTripViewModelTest, FakeRepository, CreateTrip, Flow, SavedStateHandle, receiveOne(), FakeRepository

### Community 53 - "ItineraryUiModels.kt"
Cohesion: 0.31
Nodes (9): ItineraryItemUi, RouteLegUi, toItineraryItemUi(), toRouteErrorSummary(), toRouteLegUi(), Modifier, modeLabel(), RouteLegContent() (+1 more)

### Community 54 - "PlaceDao"
Cohesion: 0.16
Nodes (3): Flow, PlaceDao, PlaceSnapshotRow

### Community 55 - "TripWorkspaceScreen"
Cohesion: 0.06
Nodes (10): Itineraries, AmapMapHost, com, CreateTrip, Legs, Places, PoiHost, Trips (+2 more)

### Community 57 - "EasyTripPrimaryButton"
Cohesion: 0.14
Nodes (20): EasyTripButtonTest, ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton(), EasyTripButton(), EasyTripButtonStyle (+12 more)

### Community 58 - "AmapComposeMap"
Cohesion: 0.14
Nodes (10): AmapComposeMap(), Bounds, Modifier, View, MapHostCallbackGuard, MapZoomButton(), SinglePoint, ViewportCommand (+2 more)

### Community 59 - "MapFacade.kt"
Cohesion: 0.19
Nodes (12): CorruptRoute, MapMarkerUi, MapPolylineUi, MapRouteLabelUi, MapScope, PLACE_POOL, SINGLE_DAY, WHOLE_TRIP (+4 more)

### Community 60 - "TripWorkspaceViewModel.kt"
Cohesion: 0.11
Nodes (19): consumeSearchSelection(), Factory, CreationExtras, Flow, Job, SavedStateHandle, StateFlow, T (+11 more)

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 62 - "AmapServiceException"
Cohesion: 0.30
Nodes (5): AmapServiceException, parsePlaces(), RawPlace, PlaceContractsTest, Exception

### Community 63 - "TripWorkspaceViewModel"
Cohesion: 0.10
Nodes (11): PlaceSearchDataSource, V2AcceptanceTest, PlaceSearchDataSource, MapPoiUi, toMapPoiUi(), SearchResultSelection, TripWorkspaceUiState, TripWorkspaceViewModel (+3 more)

### Community 64 - "CreateTripUiState"
Cohesion: 0.19
Nodes (7): CreateTripDialogTest, CreateTripDialog(), CreateTripUiState, CreateTripValidation, validateCreateTrip(), ValidCreateTrip, CreateTripValidatorTest

### Community 68 - "TestTripRepository"
Cohesion: 0.14
Nodes (5): CreateTrip, Flow, TestImpacts, TestTripRepository, TripListViewModelTest

### Community 69 - "TripWorkspaceNavigationStateTest"
Cohesion: 0.32
Nodes (3): SavedStateHandle, Trips, TripWorkspaceNavigationStateTest

### Community 72 - "RoutePlannerTest"
Cohesion: 0.38
Nodes (3): RouteDataSource, RoutePlannerTest, RouteDataSource

### Community 73 - "WorkspaceMapState"
Cohesion: 0.17
Nodes (11): TripWorkspaceContentTest, ConsentRequired, Error, Failed, Loading, NotFound, Ready, toReadyState() (+3 more)

### Community 75 - "AppNavigation"
Cohesion: 0.11
Nodes (12): FakeImpacts, FakeTripRepository, CreateTrip, Flow, Role, MoveCall, TripFlowTest, AppNavigation() (+4 more)

### Community 79 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 80 - "Easy Trip"
Cohesion: 0.50
Nodes (4): 高德地图本地密钥配置, Easy Trip, 本地优先 Android 旅行规划, 未授权或离线本地编辑

### Community 86 - "AmapPlaceDataSource.kt"
Cohesion: 0.22
Nodes (5): CallbackBoundary, PoiSearch, CallbackBoundary, com, PoiResult

### Community 87 - ".setContent"
Cohesion: 0.19
Nodes (8): TripListContentTest, Content, Empty, Error, Loading, toTripCardUiModel(), TripCardUiModel, TripListPageState

### Community 88 - "WorkspaceReadyContent"
Cohesion: 0.23
Nodes (12): OpenSettings, OpenWorkspace, TripListNavigation, Modifier, label(), LayerIcon(), toSheetValue(), TripWorkspaceContent() (+4 more)

### Community 91 - "SavedPlace"
Cohesion: 0.22
Nodes (8): SavedPlace, CollectionDecision, Confirm, decideCollectionToggle(), RemoveNow, Save, EditSavedPlaceDialog(), CollectionTogglePolicyTest

### Community 94 - "FeedbackState"
Cohesion: 0.24
Nodes (11): FeedbackStateTest, EmptyFeedbackState(), ErrorFeedbackState(), FeedbackContent(), FeedbackKind, EMPTY, ERROR, LOADING (+3 more)

### Community 95 - "FakeTrips"
Cohesion: 0.14
Nodes (3): FakeTrips, CreateTrip, java

### Community 96 - "EasyTripDatabase"
Cohesion: 0.23
Nodes (5): EasyTripDatabase, com, Clock, Context, RoomDatabase

### Community 98 - "TripService"
Cohesion: 0.15
Nodes (3): com, CreateTrip, TripService

### Community 99 - "LazyScrollbar.kt"
Cohesion: 0.29
Nodes (6): calculateScrollbarThumb(), LazyListState, Modifier, ReadOnlyLazyScrollbar(), ScrollbarThumb, LazyScrollbarGeometryTest

### Community 100 - "reduceMapInteraction"
Cohesion: 0.27
Nodes (6): FocusSearchResult, MapInteractionAction, MapInteractionState, ReconcileSearchResults, reduceMapInteraction(), MapInteractionReducerTest

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

### Community 107 - "PlacePoolScrollbarTest.kt"
Cohesion: 0.46
Nodes (3): LazyListState, Modifier, PlacePoolScrollbarTest

### Community 108 - "TripWorkspaceAction"
Cohesion: 0.20
Nodes (10): Back, OpenPrivacySettings, OpenSearch, OpenSettings, Retry, SelectItineraryScope, SelectMapLayer, SelectSection (+2 more)

### Community 114 - "DayItinerarySheet"
Cohesion: 0.24
Nodes (8): DayItinerarySheet(), Modifier, EditTimingDialog(), ItineraryItemCard(), ItineraryItemRow(), Modifier, semanticsActions(), Color

### Community 115 - "Edge"
Cohesion: 0.42
Nodes (3): AdjacencyDiff, Edge, AdjacencyPlannerTest

### Community 116 - "CreateTripAction"
Cohesion: 0.22
Nodes (8): CreateTripAction, DayCountChanged, Dismiss, NameChanged, StartDateChanged, Submit, TimeModeChanged, TravelModeChanged

### Community 120 - "行程项"
Cohesion: 0.67
Nodes (3): 行程项, 路线段, 收藏地点

### Community 122 - "EasyTripTokens.kt"
Cohesion: 0.52
Nodes (4): EasyTripElevation, EasyTripSizes, EasyTripSpacing, EasyTripTheme

### Community 125 - "BridgeState"
Cohesion: 0.33
Nodes (6): BridgeState, CANCELLED, COMPLETED, READY, STARTED, STARTING

### Community 126 - "TripListAction"
Cohesion: 0.33
Nodes (6): CreateTrip, OpenSettings, OpenTrip, RequestDelete, Retry, TripListAction

### Community 127 - "ViewportReason"
Cohesion: 0.33
Nodes (6): ViewportReason, INITIAL, PLACE_SET_CHANGED, SCOPE_CHANGED, SEARCH_FOCUS, VISIBLE_SET_CHANGED

### Community 129 - "MapMarkerKind"
Cohesion: 0.50
Nodes (4): MapMarkerKind, SAVED_ITINERARY, SAVED_PLACE_POOL, UNSAVED_SEARCH

## Knowledge Gaps
- **232 isolated node(s):** `guard-adb-install.sh script`, `READY`, `STARTING`, `STARTED`, `CANCELLED` (+227 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **39 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `MapUiModelMapperTest`, `FakeRepository`, `PlaceSearchReducer`, `.duplicateDragCrossDayTimingOverrideAndRetry`, `TripDay`, `RouteStatus`, `TravelMode`, `RouteLegRepository`, `PlacePoolViewModel`, `PlaceCandidate`, `ItineraryRepository`, `RouteRequest`, `AmapRouteDataSource.kt`, `MapUiModel`, `RoomRouteLegRepository`, `PlaceService`, `Legs`, `AmapSmokeTest`, `TripWorkspaceScreen`, `MapViewportRenderingPolicyTest`, `AmapComposeMap`, `MapFacade.kt`, `TripWorkspaceViewModel.kt`, `AmapServiceException`, `TripWorkspaceViewModel`, `RoomSavedPlaceRepositoryTest`, `TripWorkspaceNavigationStateTest`, `RoutePlannerTest`, `AmapPlaceDataSource.kt`, `SavedPlace`, `EasyTripDatabase`, `reduceMapInteraction`, `PlacePoolScrollbarTest.kt`?**
  _High betweenness centrality (0.161) - this node is a cross-community bridge._
- **Why does `TripRepository` connect `TripDay` to `FakeTripRepository`, `TripSettingsViewModel`, `Legs`, `Trips`, `DayItineraryViewModel`, `TripListViewModel`, `RouteLegRepository`, `Legs`, `ItineraryRepository`, `Legs`, `Legs`, `AppNavigation.kt`, `FakeRepository`, `TripWorkspaceScreen`, `TripWorkspaceViewModel.kt`, `TestTripRepository`, `AppNavigation`, `FakeTrips`, `RoomTripRepository`, `Trips`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **Why does `TripWorkspaceViewModel` connect `TripWorkspaceViewModel` to `reduceMapInteraction`, `TripWorkspaceNavigationStateTest`, `Legs`, `Legs`, `MapLayer`, `WorkspaceMapState`, `ItineraryScope`, `Trips`, `Legs`, `GeoPoint`, `AppNavigation.kt`, `settledWorkspaceSheetLevel`, `TripWorkspaceScreen`, `PlaceCandidate`, `TripWorkspaceViewModel.kt`?**
  _High betweenness centrality (0.068) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 3 inferred relationships involving `TripDay` (e.g. with `FakeTrips` and `.createTrip()`) actually correct?**
  _`TripDay` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Are the 22 inferred relationships involving `TripWorkspaceViewModel` (e.g. with `.model()` and `.emptyScopeClearsRequestSoReplacementHostDoesNotReplayInitialViewport()`) actually correct?**
  _`TripWorkspaceViewModel` has 22 INFERRED edges - model-reasoned connections that need verification._
- **What connects `guard-adb-install.sh script`, `READY`, `STARTING` to the rest of the system?**
  _232 weakly-connected nodes found - possible documentation gaps or missing edges._