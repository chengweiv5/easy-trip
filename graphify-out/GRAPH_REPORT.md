# Graph Report - easy-trip  (2026-08-23)

## Corpus Check
- 146 files · ~55,228 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1556 nodes · 3353 edges · 86 communities (60 shown, 26 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 188 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- RoomSavedPlaceRepository
- PlaceCandidate
- FakeRepository
- TripService
- awaitSdkCallback()
- TripSettingsViewModel
- AmapConsentToken
- Legs
- Legs
- TripWorkspaceViewModel
- FakeLegs
- RealAmapMapHost
- DayItineraryViewModel
- TripListViewModel
- GeoPoint
- TripRepository
- RouteStatus
- TransportMode
- CreateTrip
- RouteLegRepository
- TripDao
- Easy Trip v3 需求
- Converters
- PlacePoolViewModel
- MapLayerFlowTest.kt
- Itineraries
- ItineraryItemEntity
- ItineraryTransactionTest
- SavedPlace
- Easy Trip v2 交互与视觉升级设计
- RouteLegEntity
- AmapServiceException
- MemoryPreferences
- 高德 Android SDK 集成决策
- SelectablePill()
- EasyTripDatabase
- TravelMode
- AmapRouteDataSource.kt
- AmapComposeMapTest.kt
- RoomRouteLegRepository
- Legs
- MapLayer
- SharedPreferences
- RouteResult
- RoomTripRepository
- Legs
- TimeMode
- EasyTripDatabase.kt
- CompactSecondaryButton()
- TripWorkspaceScreen()
- AmapRouteDataSource
- RoomItineraryRepository
- AppNavigation.kt
- DayItinerarySheet()
- Trips
- Trips
- RoutePlanOutcome
- RecordingHost
- SavedStateHandle
- CascadeCountDao
- Edge
- RouteFormattingTest
- .search()
- SearchMapFocusTest.kt
- Places
- Places
- MapLifecycleController
- RoomSavedPlaceRepositoryTest
- RoomRouteLegRepositoryTest
- Itineraries
- ItineraryService
- MapLayerRenderingPolicyTest
- RoutePlannerTest
- MapViewportRenderingPolicyTest
- 先用一家 SDK 跑通真实场景，再用样例数据完成最终选型
- .savedResultClickFlowsThroughWorkspaceMapperAndM
- MainActivity.kt
- ViewportReason
- AppIconResourceTest
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest

## God Nodes (most connected - your core abstractions)
1. `GeoPoint` - 85 edges
2. `CreateTrip` - 53 edges
3. `TravelMode` - 48 edges
4. `PlaceCandidate` - 48 edges
5. `TransportMode` - 43 edges
6. `EasyTripDatabase` - 32 edges
7. `RouteStatus` - 32 edges
8. `SavedPlace` - 32 edges
9. `TripRepository` - 31 edges
10. `TripDao` - 30 edges

## Surprising Connections (you probably didn't know these)
- `统一加入行程入口` --conceptually_related_to--> `同一地点可重复安排行程`  [INFERRED]
  v3-todo.md → docs/superpowers/specs/2026-08-21-easy-trip-v1-design.md
- `重复授权地图空白 Bug` --conceptually_related_to--> `显式隐私授权门控`  [INFERRED]
  v2-issues-3.md → docs/adr/0001-amap-sdk-integration.md
- `统一加入行程入口` --shares_data_with--> `收藏地点`  [EXTRACTED]
  v3-todo.md → docs/superpowers/specs/2026-08-21-easy-trip-v1-design.md
- `行程项追加到目标日末尾` --conceptually_related_to--> `路线段`  [INFERRED]
  v3-todo.md → docs/superpowers/specs/2026-08-21-easy-trip-v1-design.md
- `独立搜索页面取代抽屉搜索 Tab` --conceptually_related_to--> `三个独立工作台内容 Tab`  [INFERRED]
  v2-issues-2.md → docs/superpowers/specs/2026-08-22-easy-trip-v2-design.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **旅行编排数据模型** — docs_superpowers_specs_2026_08_21_saved_place, docs_superpowers_specs_2026_08_21_itinerary_item, docs_superpowers_specs_2026_08_21_route_leg [EXTRACTED 1.00]
- **v3 加入行程流程** — v3_todo_unified_add_to_itinerary, v3_todo_lightweight_day_picker, docs_superpowers_specs_2026_08_21_saved_place, docs_superpowers_specs_2026_08_21_itinerary_item, docs_superpowers_specs_2026_08_21_route_leg [EXTRACTED 1.00]

## Communities (86 total, 26 thin omitted)

### Community 0 - "RoomSavedPlaceRepository"
Cohesion: 0.05
Nodes (12): Fixture, SchemaTest, PlaceSearchDataSource, PlacePoolFlowTest, PlaceSearchDataSource, SchemaPlaceDao, Flow, PlaceDao (+4 more)

### Community 1 - "PlaceCandidate"
Cohesion: 0.07
Nodes (22): V1AcceptanceTest, PlaceCandidate, PlaceSearchDataSource, Job, PlaceSearchDataSource, StateFlow, PlaceSearchReducer, CorruptRoute (+14 more)

### Community 2 - "FakeRepository"
Cohesion: 0.09
Nodes (21): connected(), hasValidatedInternet(), StateFlow, NetworkMonitor, onAvailable(), onCapabilitiesChanged(), onLost(), Active (+13 more)

### Community 3 - "TripService"
Cohesion: 0.07
Nodes (10): FakeImpacts, FakeTripRepository, Role, MoveCall, TripFlowTest, com, TripService, FakeTripRepository (+2 more)

### Community 4 - "awaitSdkCallback()"
Cohesion: 0.06
Nodes (23): OneShotCallback, awaitSdkCallback(), BridgeState, CANCELLED, COMPLETED, READY, STARTED, STARTING (+15 more)

### Community 5 - "TripSettingsViewModel"
Cohesion: 0.06
Nodes (15): androidx, RoomDeleteImpactProviderTest, DeleteImpactDao, RoomDeleteImpactProvider, DayDeleteImpact, DayUi, Factory, StateFlow (+7 more)

### Community 6 - "AmapConsentToken"
Cohesion: 0.08
Nodes (14): AmapConsentToken, AmapPrivacyGate, AmapPrivacyStateMachine, ConsentRegistry, ConsentSnapshot, StateFlow, TestConsentGate, AppContainer (+6 more)

### Community 7 - "Legs"
Cohesion: 0.05
Nodes (8): FakeMapPreferences, Itineraries, com, Role, Legs, MapLayerFlowTest, Places, Trips

### Community 8 - "Legs"
Cohesion: 0.06
Nodes (6): Itineraries, com, Legs, Places, Trips, WorkspaceFlowTest

### Community 9 - "TripWorkspaceViewModel"
Cohesion: 0.08
Nodes (23): FocusSearchResult, MapInteractionAction, MapInteractionState, ReconcileSearchResults, reduceMapInteraction(), Factory, StateFlow, T (+15 more)

### Community 10 - "FakeLegs"
Cohesion: 0.08
Nodes (9): Add, FakeCoordinator, FakeItineraries, FakeLegs, ItineraryEditingTest, com, kotlinx, Move (+1 more)

### Community 11 - "RealAmapMapHost"
Cohesion: 0.10
Nodes (11): android, AmapComposeMap(), AmapMapHost, Bounds, Modifier, View, MapZoomButton(), RealAmapMapHost (+3 more)

### Community 12 - "DayItineraryViewModel"
Cohesion: 0.10
Nodes (9): DayItineraryUiState, DayItineraryViewModel, Factory, ItineraryItemUi, StateFlow, T, ViewModel, ViewModelProvider (+1 more)

### Community 13 - "TripListViewModel"
Cohesion: 0.10
Nodes (14): CreateTripDialog(), CreateTimeMode, DATED, DRAFT, Factory, StateFlow, T, ViewModel (+6 more)

### Community 14 - "GeoPoint"
Cohesion: 0.13
Nodes (7): GeoPoint, Result, PolylineCodec, MapViewportRequest, MapViewportController, PolylineCodecTest, MapViewportControllerTest

### Community 15 - "TripRepository"
Cohesion: 0.14
Nodes (7): Flow, Flow, Flow, TripDay, TripRepository, TripSummary, TripWithDays

### Community 16 - "RouteStatus"
Cohesion: 0.10
Nodes (9): RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK, Flow, RouteLegDao (+1 more)

### Community 17 - "TransportMode"
Cohesion: 0.14
Nodes (11): TransportMode, DRIVE, TAXI, TRANSIT, WALK, defaultRecommendMode(), haversineMeters(), Flow (+3 more)

### Community 18 - "CreateTrip"
Cohesion: 0.15
Nodes (5): IdFactory, T, MutableClock, RoomTripRepositoryTest, CreateTrip

### Community 19 - "RouteLegRepository"
Cohesion: 0.11
Nodes (7): Flow, Flow, Flow, RouteLegRepository, RouteLegWithEndpoints, Flow, observeSnapshots()

### Community 21 - "Easy Trip v3 需求"
Cohesion: 0.10
Nodes (23): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 行程项, 离线路线等待与联网续算 (+15 more)

### Community 22 - "Converters"
Cohesion: 0.10
Nodes (6): Converters, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT, UNSUPPORTED_TRANSIT

### Community 23 - "PlacePoolViewModel"
Cohesion: 0.13
Nodes (9): Factory, kotlinx, PlaceSearchDataSource, StateFlow, T, ViewModel, ViewModelProvider, PlacePoolUiState (+1 more)

### Community 24 - "MapLayerFlowTest.kt"
Cohesion: 0.15
Nodes (8): Flow, Flow, AlreadySaved, Flow, PlaceTag, Saved, SavedPlaceRepository, SavePlaceResult

### Community 25 - "Itineraries"
Cohesion: 0.12
Nodes (6): Itineraries, Lifecycle, LifecycleOwner, SavedPlaces, SearchMapFocusTest, TestOwner

### Community 26 - "ItineraryItemEntity"
Cohesion: 0.14
Nodes (5): DayItems, DayItineraryRow, ItineraryDao, Flow, ItineraryItemEntity

### Community 28 - "SavedPlace"
Cohesion: 0.20
Nodes (7): DayItinerary, ItineraryItem, ItineraryPlace, ItineraryRepository, Flow, SavedPlace, ComponentActivity

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.13
Nodes (19): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+11 more)

### Community 30 - "RouteLegEntity"
Cohesion: 0.14
Nodes (3): OfflineRecoveryTest, RouteLegEntity, TripEntity

### Community 31 - "AmapServiceException"
Cohesion: 0.25
Nodes (8): AmapServiceException, parseRouteResult(), RoutePathData, selectUsablePath(), validateRouteRequest(), RouteRequest, RouteContractsTest, Exception

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.12
Nodes (16): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+8 more)

### Community 34 - "SelectablePill()"
Cohesion: 0.18
Nodes (8): SelectablePillTest, Modifier, Role, SelectablePill(), SelectablePillStyle, DaySelector(), Modifier, SavedPlacesContent()

### Community 35 - "EasyTripDatabase"
Cohesion: 0.18
Nodes (5): EasyTripDatabase, com, Clock, Context, RoomDatabase

### Community 36 - "TravelMode"
Cohesion: 0.12
Nodes (5): FakeTrips, TravelMode, FLEXIBLE, SELF_DRIVE, java

### Community 37 - "AmapRouteDataSource.kt"
Cohesion: 0.19
Nodes (8): CallbackBoundary, RouteSearch, CallbackBoundary, RouteCallback, BusRouteResult, DriveRouteResult, RideRouteResult, WalkRouteResult

### Community 38 - "AmapComposeMapTest.kt"
Cohesion: 0.18
Nodes (7): AmapComposeMapTest, AmapMapHost, AmapMapHost, Lifecycle, LifecycleOwner, View, TestOwner

### Community 41 - "MapLayer"
Cohesion: 0.26
Nodes (7): MapLayer, SATELLITE_ROAD, STANDARD, InMemoryMapPreferences, StateFlow, MapPreferences, SharedPreferencesMapPreferences

### Community 43 - "RouteResult"
Cohesion: 0.17
Nodes (6): RouteDataSource, RouteMode, DRIVE, TRANSIT, WALK, RouteResult

### Community 46 - "TimeMode"
Cohesion: 0.19
Nodes (5): TimeMode, DATED, DRAFT, Flow, TripEntityWithDays

### Community 48 - "CompactSecondaryButton()"
Cohesion: 0.29
Nodes (9): ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton(), CompactDangerButton(), CompactPrimaryButton(), CompactSecondaryButton() (+1 more)

### Community 49 - "TripWorkspaceScreen()"
Cohesion: 0.26
Nodes (8): PlaceSearchField(), Modifier, label(), settledWorkspaceSheetLevel(), TripWorkspaceScreen(), WorkspaceSheetHandle(), WorkspaceSheetSyncTest, SheetValue

### Community 50 - "AmapRouteDataSource"
Cohesion: 0.36
Nodes (3): AmapSmokeTest, AmapRouteDataSource, RouteDataSource

### Community 52 - "AppNavigation.kt"
Cohesion: 0.29
Nodes (8): AppNavigation(), AmapPlaceDataSource, EditSavedPlaceDialog(), Modifier, PlacePoolSheet(), TripListScreen(), TripSettingsScreen(), DeleteImpactProvider

### Community 53 - "DayItinerarySheet()"
Cohesion: 0.24
Nodes (7): DayItinerarySheet(), Modifier, EditTimingDialog(), ItineraryItemRow(), semanticsActions(), modeLabel(), RouteLegRow()

### Community 56 - "RoutePlanOutcome"
Cohesion: 0.29
Nodes (5): Failure, plan(), RoutePlanner, RoutePlanOutcome, Success

### Community 57 - "RecordingHost"
Cohesion: 0.25
Nodes (3): AmapMapHost, RecordingHost, MapUiModel

### Community 60 - "Edge"
Cohesion: 0.42
Nodes (3): AdjacencyDiff, Edge, AdjacencyPlannerTest

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 62 - ".search()"
Cohesion: 0.39
Nodes (3): parsePlaces(), RawPlace, PlaceContractsTest

### Community 63 - "SearchMapFocusTest.kt"
Cohesion: 0.25
Nodes (4): Flow, InsertSide, AFTER, BEFORE

### Community 72 - "RoutePlannerTest"
Cohesion: 0.38
Nodes (3): RouteDataSource, RoutePlannerTest, RouteDataSource

### Community 74 - "先用一家 SDK 跑通真实场景，再用样例数据完成最终选型"
Cohesion: 0.43
Nodes (7): 步骤4：高德/百度选型（体验、覆盖、稳定性、授权与成本）, 地图 SDK 首版决策路径, 步骤5：正式接入, 步骤1：需求收敛（地图展示、前台定位、地点搜索、路线预览）, 步骤3：样例验证（城市、POI、路线、弱网、粗略定位）, 步骤2：接一家 SDK, 先用一家 SDK 跑通真实场景，再用样例数据完成最终选型

### Community 75 - ".savedResultClickFlowsThroughWorkspaceMapperAndM"
Cohesion: 0.53
Nodes (3): PlaceSearchState, Modifier, PlaceSearchResults()

### Community 76 - "MainActivity.kt"
Cohesion: 0.47
Nodes (3): EasyTripTheme(), MainActivity, Bundle

### Community 77 - "ViewportReason"
Cohesion: 0.40
Nodes (5): ViewportReason, INITIAL, PLACE_SET_CHANGED, SCOPE_CHANGED, SEARCH_FOCUS

### Community 79 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 80 - "Easy Trip"
Cohesion: 0.50
Nodes (4): 高德地图本地密钥配置, Easy Trip, 本地优先 Android 旅行规划, 未授权或离线本地编辑

## Knowledge Gaps
- **82 isolated node(s):** `READY`, `STARTING`, `STARTED`, `CANCELLED`, `COMPLETED` (+77 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **26 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `RoomSavedPlaceRepository`, `PlaceCandidate`, `FakeRepository`, `awaitSdkCallback()`, `Legs`, `TripWorkspaceViewModel`, `FakeLegs`, `TransportMode`, `RouteLegRepository`, `MapLayerFlowTest.kt`, `Itineraries`, `SavedPlace`, `AmapServiceException`, `EasyTripDatabase`, `AmapRouteDataSource.kt`, `AmapComposeMapTest.kt`, `RoomRouteLegRepository`, `RouteResult`, `RoomTripRepository`, `AmapRouteDataSource`, `SavedStateHandle`, `.search()`, `SearchMapFocusTest.kt`, `RoomSavedPlaceRepositoryTest`, `RoomRouteLegRepositoryTest`, `RoutePlannerTest`, `MapViewportRenderingPolicyTest`, `.savedResultClickFlowsThroughWorkspaceMapperAndM`?**
  _High betweenness centrality (0.179) - this node is a cross-community bridge._
- **Why does `EasyTripDatabase` connect `EasyTripDatabase` to `RoomSavedPlaceRepository`, `PlaceCandidate`, `RoomSavedPlaceRepositoryTest`, `RoomRouteLegRepositoryTest`, `TripSettingsViewModel`, `AmapConsentToken`, `CascadeCountDao`, `RoomTripRepository`, `EasyTripDatabase.kt`, `TripRepository`, `CreateTrip`, `MapLayerFlowTest.kt`, `ItineraryTransactionTest`, `RouteLegEntity`?**
  _High betweenness centrality (0.090) - this node is a cross-community bridge._
- **Why does `RouteLegRepository` connect `RouteLegRepository` to `FakeRepository`, `Legs`, `Legs`, `Legs`, `FakeLegs`, `RoomRouteLegRepository`, `DayItineraryViewModel`, `Legs`, `RouteResult`, `TripWorkspaceViewModel`, `MapLayerFlowTest.kt`, `RoutePlanOutcome`, `SavedPlace`, `SearchMapFocusTest.kt`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `READY`, `STARTING`, `STARTED` to the rest of the system?**
  _82 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `RoomSavedPlaceRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.052884615384615384 - nodes in this community are weakly interconnected._
- **Should `PlaceCandidate` be split into smaller, more focused modules?**
  _Cohesion score 0.06971153846153846 - nodes in this community are weakly interconnected._