# Graph Report - easy-trip  (2026-08-23)

## Corpus Check
- 145 files · ~55,372 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1558 nodes · 3354 edges · 93 communities (63 shown, 30 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 188 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `cee53cf6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- RoomSavedPlaceRepository
- TripWorkspaceViewModel
- FakeRepository
- TripService
- awaitSdkCallback
- TripSettingsViewModel
- AmapConsentToken
- Legs
- Legs
- PlaceSearchReducer
- FakeLegs
- RealAmapMapHost
- DayItineraryViewModel
- TripListViewModel
- MapViewportController
- TripRepository
- RouteLegEntity
- TravelMode
- CreateTrip
- RouteLegRepository
- TripDao
- Easy Trip v3 需求
- Converters
- PlacePoolViewModel
- SavedPlace
- SearchMapFocusTest.kt
- ItineraryItemEntity
- ItineraryTransactionTest
- GeoPoint
- Easy Trip v2 交互与视觉升级设计
- TripDayEntity
- RouteRequest
- MemoryPreferences
- 高德 Android SDK 集成决策
- SelectablePill
- EasyTripDatabase
- SchemaTest
- AmapRouteDataSource.kt
- AmapComposeMapTest.kt
- RoomRouteLegRepository
- Legs
- MapLayer
- SharedPreferences
- RouteResult
- RoomTripRepository
- Legs
- RoomDeleteImpactProviderTest.kt
- EasyTripDatabase.kt
- CompactSecondaryButton
- TripWorkspaceScreen
- AmapSmokeTest
- RoomItineraryRepository
- AppNavigation.kt
- DayItinerarySheet
- PlaceDao
- Trips
- RoutePlanOutcome
- RecordingHost
- SavedStateHandle
- CascadeCountDao
- Edge
- RouteFormattingTest
- AmapServiceException
- RouteLegWithEndpoints
- Places
- Places
- MapLifecycleController
- RoomSavedPlaceRepositoryTest
- RoomRouteLegRepositoryTest
- Itineraries
- ItineraryService
- MapLayerRenderingPolicyTest
- RoutePlannerTest
- PlaceCandidate
- 先用一家 SDK 跑通真实场景，再用样例数据完成最终选型
- FakeTripRepository
- MainActivity.kt
- RouteStatus
- AppIconResourceTest
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest
- PoiSearch
- AmapRouteDataSource
- AmapComposeMap
- RoomDeleteImpactProviderTest
- .threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes
- PlacePoolSheet
- CLAUDE.md

## God Nodes (most connected - your core abstractions)
1. `GeoPoint` - 85 edges
2. `CreateTrip` - 53 edges
3. `PlaceCandidate` - 48 edges
4. `TravelMode` - 48 edges
5. `TransportMode` - 43 edges
6. `RouteStatus` - 32 edges
7. `SavedPlace` - 32 edges
8. `EasyTripDatabase` - 32 edges
9. `TripRepository` - 31 edges
10. `FakeRepository` - 30 edges

## Surprising Connections (you probably didn't know these)
- `统一加入行程入口` --conceptually_related_to--> `同一地点可重复安排行程`  [INFERRED]
  v3-todo.md → docs/superpowers/specs/2026-08-21-easy-trip-v1-design.md
- `行程项追加到目标日末尾` --conceptually_related_to--> `路线段`  [INFERRED]
  v3-todo.md → docs/superpowers/specs/2026-08-21-easy-trip-v1-design.md
- `统一加入行程入口` --shares_data_with--> `收藏地点`  [EXTRACTED]
  v3-todo.md → docs/superpowers/specs/2026-08-21-easy-trip-v1-design.md
- `独立搜索页面取代抽屉搜索 Tab` --conceptually_related_to--> `三个独立工作台内容 Tab`  [INFERRED]
  v2-issues-2.md → docs/superpowers/specs/2026-08-22-easy-trip-v2-design.md
- `重复授权地图空白 Bug` --conceptually_related_to--> `显式隐私授权门控`  [INFERRED]
  v2-issues-3.md → docs/adr/0001-amap-sdk-integration.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **旅行编排数据模型** — docs_superpowers_specs_2026_08_21_saved_place, docs_superpowers_specs_2026_08_21_itinerary_item, docs_superpowers_specs_2026_08_21_route_leg [EXTRACTED 1.00]
- **v3 加入行程流程** — v3_todo_unified_add_to_itinerary, v3_todo_lightweight_day_picker, docs_superpowers_specs_2026_08_21_saved_place, docs_superpowers_specs_2026_08_21_itinerary_item, docs_superpowers_specs_2026_08_21_route_leg [EXTRACTED 1.00]

## Communities (93 total, 30 thin omitted)

### Community 0 - "RoomSavedPlaceRepository"
Cohesion: 0.10
Nodes (5): SchemaPlaceDao, SavedPlaceTagCrossRef, TagEntity, Flow, RoomSavedPlaceRepository

### Community 1 - "TripWorkspaceViewModel"
Cohesion: 0.05
Nodes (37): CorruptRoute, DayMapSnapshot, FocusSearchResult, MapInteractionAction, MapInteractionState, MapMarkerUi, MapPolylineUi, MapRouteLabelUi (+29 more)

### Community 2 - "FakeRepository"
Cohesion: 0.09
Nodes (21): connected(), hasValidatedInternet(), StateFlow, NetworkMonitor, onAvailable(), onCapabilitiesChanged(), onLost(), Active (+13 more)

### Community 3 - "TripService"
Cohesion: 0.11
Nodes (5): com, TripService, FakeTripRepository, Flow, TripServiceTest

### Community 4 - "awaitSdkCallback"
Cohesion: 0.08
Nodes (18): OneShotCallback, awaitSdkCallback(), BridgeState, CANCELLED, COMPLETED, READY, STARTED, STARTING (+10 more)

### Community 5 - "TripSettingsViewModel"
Cohesion: 0.11
Nodes (11): androidx, DayDeleteImpact, DayUi, Factory, StateFlow, T, ViewModel, ViewModelProvider (+3 more)

### Community 6 - "AmapConsentToken"
Cohesion: 0.06
Nodes (15): AmapConsentToken, AmapPrivacyStateMachine, ConsentRegistry, ConsentSnapshot, StateFlow, TestConsentGate, AppContainer, CoroutineScope (+7 more)

### Community 7 - "Legs"
Cohesion: 0.05
Nodes (9): FakeMapPreferences, Itineraries, com, Flow, Role, Legs, MapLayerFlowTest, Places (+1 more)

### Community 8 - "Legs"
Cohesion: 0.06
Nodes (7): Itineraries, com, Flow, Legs, Places, Trips, WorkspaceFlowTest

### Community 9 - "PlaceSearchReducer"
Cohesion: 0.20
Nodes (7): Job, PlaceSearchDataSource, StateFlow, PlaceSearchReducer, PlaceSearchDataSource, PlaceSearchReducerTest, SearchSource

### Community 10 - "FakeLegs"
Cohesion: 0.06
Nodes (11): Add, FakeCoordinator, FakeItineraries, FakeLegs, FakeTrips, ItineraryEditingTest, com, kotlinx (+3 more)

### Community 11 - "RealAmapMapHost"
Cohesion: 0.12
Nodes (4): android, AmapMapHost, View, RealAmapMapHost

### Community 12 - "DayItineraryViewModel"
Cohesion: 0.10
Nodes (9): DayItineraryUiState, DayItineraryViewModel, Factory, ItineraryItemUi, StateFlow, T, ViewModel, ViewModelProvider (+1 more)

### Community 13 - "TripListViewModel"
Cohesion: 0.10
Nodes (15): CreateTripDialog(), TripListScreen(), CreateTimeMode, DATED, DRAFT, Factory, StateFlow, T (+7 more)

### Community 14 - "MapViewportController"
Cohesion: 0.15
Nodes (4): MapViewportRequest, MapViewportController, MapViewportControllerTest, MapViewportRenderingPolicyTest

### Community 15 - "TripRepository"
Cohesion: 0.11
Nodes (10): Flow, Flow, InsertSide, AFTER, BEFORE, Flow, TripDay, TripRepository (+2 more)

### Community 16 - "RouteLegEntity"
Cohesion: 0.14
Nodes (4): Flow, RouteLegDao, RouteLegEndpointRow, RouteLegEntity

### Community 17 - "TravelMode"
Cohesion: 0.11
Nodes (14): TransportMode, DRIVE, TAXI, TRANSIT, WALK, TravelMode, FLEXIBLE, SELF_DRIVE (+6 more)

### Community 18 - "CreateTrip"
Cohesion: 0.15
Nodes (5): IdFactory, T, MutableClock, RoomTripRepositoryTest, CreateTrip

### Community 21 - "Easy Trip v3 需求"
Cohesion: 0.10
Nodes (23): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 行程项, 离线路线等待与联网续算 (+15 more)

### Community 22 - "Converters"
Cohesion: 0.10
Nodes (6): Converters, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT, UNSUPPORTED_TRANSIT

### Community 23 - "PlacePoolViewModel"
Cohesion: 0.10
Nodes (12): PlaceSearchDataSource, PlacePoolFlowTest, PlaceSearchDataSource, Factory, kotlinx, PlaceSearchDataSource, StateFlow, T (+4 more)

### Community 24 - "SavedPlace"
Cohesion: 0.19
Nodes (7): Flow, Flow, PlaceTag, SavedPlace, SavedPlaceRepository, Modifier, SavedPlacesContent()

### Community 25 - "SearchMapFocusTest.kt"
Cohesion: 0.08
Nodes (7): Itineraries, Lifecycle, LifecycleOwner, SavedPlaces, SearchMapFocusTest, TestOwner, Trips

### Community 26 - "ItineraryItemEntity"
Cohesion: 0.15
Nodes (5): DayItems, DayItineraryRow, ItineraryDao, Flow, ItineraryItemEntity

### Community 28 - "GeoPoint"
Cohesion: 0.12
Nodes (10): GeoPoint, DayItinerary, ItineraryItem, ItineraryPlace, ItineraryRepository, Flow, Result, PolylineCodec (+2 more)

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.13
Nodes (19): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+11 more)

### Community 30 - "TripDayEntity"
Cohesion: 0.23
Nodes (3): OfflineRecoveryTest, TripDayEntity, TripEntity

### Community 31 - "RouteRequest"
Cohesion: 0.22
Nodes (9): RoutePathData, selectUsablePath(), validateRouteRequest(), RouteMode, DRIVE, TRANSIT, WALK, RouteRequest (+1 more)

### Community 32 - "MemoryPreferences"
Cohesion: 0.17
Nodes (3): SharedPreferencesMapPreferences, MapPreferencesTest, MemoryPreferences

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.12
Nodes (16): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+8 more)

### Community 34 - "SelectablePill"
Cohesion: 0.24
Nodes (6): SelectablePillTest, Modifier, Role, SelectablePill(), SelectablePillStyle, DaySelector()

### Community 35 - "EasyTripDatabase"
Cohesion: 0.23
Nodes (5): EasyTripDatabase, com, Clock, Context, RoomDatabase

### Community 37 - "AmapRouteDataSource.kt"
Cohesion: 0.33
Nodes (6): RouteSearch, RouteCallback, BusRouteResult, DriveRouteResult, RideRouteResult, WalkRouteResult

### Community 38 - "AmapComposeMapTest.kt"
Cohesion: 0.13
Nodes (12): AmapComposeMapTest, AmapMapHost, AmapMapHost, Lifecycle, LifecycleOwner, View, TestOwner, ViewportReason (+4 more)

### Community 41 - "MapLayer"
Cohesion: 0.26
Nodes (6): MapLayer, SATELLITE_ROAD, STANDARD, InMemoryMapPreferences, StateFlow, MapPreferences

### Community 43 - "RouteResult"
Cohesion: 0.22
Nodes (3): parseRouteResult(), RouteDataSource, RouteResult

### Community 46 - "RoomDeleteImpactProviderTest.kt"
Cohesion: 0.18
Nodes (5): TimeMode, DATED, DRAFT, Flow, TripEntityWithDays

### Community 48 - "CompactSecondaryButton"
Cohesion: 0.29
Nodes (9): ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton(), CompactDangerButton(), CompactPrimaryButton(), CompactSecondaryButton() (+1 more)

### Community 49 - "TripWorkspaceScreen"
Cohesion: 0.26
Nodes (8): PlaceSearchField(), Modifier, label(), settledWorkspaceSheetLevel(), TripWorkspaceScreen(), WorkspaceSheetHandle(), WorkspaceSheetSyncTest, SheetValue

### Community 52 - "AppNavigation.kt"
Cohesion: 0.23
Nodes (8): FakeImpacts, Role, TripFlowTest, AppNavigation(), AmapPlaceDataSource, TripSettingsScreen(), DeleteImpactProvider, TripDeleteImpact

### Community 53 - "DayItinerarySheet"
Cohesion: 0.24
Nodes (7): DayItinerarySheet(), Modifier, EditTimingDialog(), ItineraryItemRow(), semanticsActions(), modeLabel(), RouteLegRow()

### Community 54 - "PlaceDao"
Cohesion: 0.16
Nodes (3): Flow, PlaceDao, PlaceSnapshotRow

### Community 56 - "RoutePlanOutcome"
Cohesion: 0.25
Nodes (5): Failure, plan(), RoutePlanner, RoutePlanOutcome, Success

### Community 57 - "RecordingHost"
Cohesion: 0.29
Nodes (3): AmapMapHost, RecordingHost, MapUiModel

### Community 58 - "SavedStateHandle"
Cohesion: 0.33
Nodes (5): WorkspaceSearchTabsTest, PlaceSearchState, Modifier, PlaceSearchResults(), SavedStateHandle

### Community 60 - "Edge"
Cohesion: 0.42
Nodes (3): AdjacencyDiff, Edge, AdjacencyPlannerTest

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 62 - "AmapServiceException"
Cohesion: 0.30
Nodes (5): AmapServiceException, parsePlaces(), RawPlace, PlaceContractsTest, Exception

### Community 72 - "RoutePlannerTest"
Cohesion: 0.38
Nodes (3): RouteDataSource, RoutePlannerTest, RouteDataSource

### Community 73 - "PlaceCandidate"
Cohesion: 0.19
Nodes (6): AmapPrivacyGate, PlaceCandidate, PlaceSearchDataSource, AlreadySaved, Saved, SavePlaceResult

### Community 74 - "先用一家 SDK 跑通真实场景，再用样例数据完成最终选型"
Cohesion: 0.43
Nodes (7): 步骤4：高德/百度选型（体验、覆盖、稳定性、授权与成本）, 地图 SDK 首版决策路径, 步骤5：正式接入, 步骤1：需求收敛（地图展示、前台定位、地点搜索、路线预览）, 步骤3：样例验证（城市、POI、路线、弱网、粗略定位）, 步骤2：接一家 SDK, 先用一家 SDK 跑通真实场景，再用样例数据完成最终选型

### Community 76 - "MainActivity.kt"
Cohesion: 0.47
Nodes (3): EasyTripTheme(), MainActivity, Bundle

### Community 77 - "RouteStatus"
Cohesion: 0.18
Nodes (6): RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK

### Community 79 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 80 - "Easy Trip"
Cohesion: 0.50
Nodes (4): 高德地图本地密钥配置, Easy Trip, 本地优先 Android 旅行规划, 未授权或离线本地编辑

### Community 86 - "PoiSearch"
Cohesion: 0.22
Nodes (5): CallbackBoundary, PoiSearch, CallbackBoundary, com, PoiResult

### Community 87 - "AmapRouteDataSource"
Cohesion: 0.24
Nodes (4): AmapRouteDataSource, CallbackBoundary, CallbackBoundary, RouteDataSource

### Community 88 - "AmapComposeMap"
Cohesion: 0.33
Nodes (7): AmapComposeMap(), Bounds, Modifier, MapZoomButton(), SinglePoint, ViewportCommand, ViewportRendering

### Community 91 - "PlacePoolSheet"
Cohesion: 0.50
Nodes (3): EditSavedPlaceDialog(), Modifier, PlacePoolSheet()

## Knowledge Gaps
- **83 isolated node(s):** `graphify`, `AlreadySaved`, `Saved`, `FocusSearchResult`, `ReconcileSearchResults` (+78 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **30 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `RoomSavedPlaceRepository`, `TripWorkspaceViewModel`, `FakeRepository`, `PlaceSearchReducer`, `FakeLegs`, `MapViewportController`, `TravelMode`, `RouteLegRepository`, `PlacePoolViewModel`, `SavedPlace`, `SearchMapFocusTest.kt`, `RouteRequest`, `EasyTripDatabase`, `AmapRouteDataSource.kt`, `AmapComposeMapTest.kt`, `RoomRouteLegRepository`, `RouteResult`, `RoomTripRepository`, `AmapSmokeTest`, `SavedStateHandle`, `AmapServiceException`, `RoomSavedPlaceRepositoryTest`, `RoomRouteLegRepositoryTest`, `RoutePlannerTest`, `PlaceCandidate`, `AmapRouteDataSource`, `.threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes`?**
  _High betweenness centrality (0.182) - this node is a cross-community bridge._
- **Why does `TravelMode` connect `TravelMode` to `TripService`, `TripSettingsViewModel`, `Legs`, `Legs`, `FakeLegs`, `TripListViewModel`, `TripRepository`, `Converters`, `SavedPlace`, `SearchMapFocusTest.kt`, `ItineraryItemEntity`, `ItineraryTransactionTest`, `GeoPoint`, `TripDayEntity`, `EasyTripDatabase`, `RoomDeleteImpactProviderTest.kt`, `CompactSecondaryButton`, `RoomItineraryRepository`, `Trips`, `FakeTripRepository`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **Why does `TripRepository` connect `TripRepository` to `TripWorkspaceViewModel`, `TripService`, `TripSettingsViewModel`, `Legs`, `Legs`, `FakeLegs`, `FakeTripRepository`, `DayItineraryViewModel`, `RoomTripRepository`, `TripListViewModel`, `AppNavigation.kt`, `Trips`, `SavedPlace`, `SearchMapFocusTest.kt`, `GeoPoint`?**
  _High betweenness centrality (0.060) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `graphify`, `AlreadySaved`, `Saved` to the rest of the system?**
  _83 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `RoomSavedPlaceRepository` be split into smaller, more focused modules?**
  _Cohesion score 0.10333333333333333 - nodes in this community are weakly interconnected._
- **Should `TripWorkspaceViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.05017543859649123 - nodes in this community are weakly interconnected._