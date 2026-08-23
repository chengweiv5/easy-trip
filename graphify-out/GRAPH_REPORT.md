# Graph Report - easy-trip-v1  (2026-08-23)

## Corpus Check
- 163 files · ~58,734 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1922 nodes · 4194 edges · 121 communities (80 shown, 41 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 272 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `52eff215`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- TagEntity
- PlaceCandidate
- FakeRepository
- FakeTripRepository
- awaitSdkCallback
- TripSettingsViewModel
- AmapConsentToken
- Places
- Legs
- PlaceSearchReducer
- FakeItineraries
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
- SavedPlaceRepository
- Trips
- RoomItineraryRepository
- Legs
- ItineraryRepository
- Easy Trip v2 交互与视觉升级设计
- RouteLegEntity
- RouteRequest
- MemoryPreferences
- 高德 Android SDK 集成决策
- SelectablePill
- Easy Trip 工作台导航重构设计
- SchemaTest
- AmapRouteDataSource.kt
- AmapComposeMapTest.kt
- RoomRouteLegRepository
- Legs
- MapLayer
- SharedPreferences
- ItineraryScope
- RoomTripRepository
- Legs
- TimeMode
- EasyTripDatabase
- Dp
- TripWorkspaceScreen
- AmapSmokeTest
- Easy Trip 搜索与收藏交互设计
- AppNavigation.kt
- DayItinerarySheet
- PlaceDao
- MapPoiUi
- GeoPoint
- RecordingHost
- SavedPlacesContent
- MapFacade.kt
- TripWorkspaceViewModel.kt
- RouteFormattingTest
- AmapServiceException
- .searchCollectionMapAndRestorationFlow
- .independentSearchSelectionClearsPageAndIsConsumedOnce
- TripWorkspaceViewModel
- MapLifecycleController
- RoomSavedPlaceRepositoryTest
- RoomRouteLegRepositoryTest
- TripWorkspaceNavigationStateTest
- ItineraryService
- MapLayerRenderingPolicyTest
- RoutePlannerTest
- V2AcceptanceTest.kt
- AmapMapHost
- FakeTripRepository
- MainActivity.kt
- FakeLegs
- DeleteImpactDao
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest
- Context
- CallbackBoundary
- Legs
- RoomDeleteImpactProviderTest
- RouteResult
- SavedPlace
- CLAUDE.md
- Legs
- WholeTripItineraryContent
- FakeTrips
- WorkspaceItineraryContent
- ItineraryUiModels.kt
- TripService
- LazyScrollbar.kt
- reduceMapInteraction
- Trips
- File Structure
- Trips
- Trips
- Global Constraints
- SavedPlaces
- PlacePoolScrollbarTest.kt
- Itineraries
- Places
- Places
- formatOccurrenceBadge
- Itineraries
- MutableItineraries
- ItineraryItemRow.kt
- TripEntity
- TripFlowTest.kt
- SearchMapFocusTest
- TripDao.kt
- WorkspaceSheetLevel
- 行程项

## God Nodes (most connected - your core abstractions)
1. `GeoPoint` - 112 edges
2. `PlaceCandidate` - 64 edges
3. `CreateTrip` - 60 edges
4. `TravelMode` - 52 edges
5. `TripDay` - 51 edges
6. `TransportMode` - 49 edges
7. `TripWorkspaceViewModel` - 42 edges
8. `SavedPlace` - 41 edges
9. `RouteStatus` - 35 edges
10. `TripRepository` - 35 edges

## Surprising Connections (you probably didn't know these)
- `FakeTrips` --calls--> `TripDay`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `FakeTrips` --calls--> `TripWithDays`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `RoutePlannerTest` --calls--> `GeoPoint`  [INFERRED]
  app/src/test/java/com/yangchengwei/easytrip/route/domain/RoutePlannerTest.kt → app/src/main/java/com/yangchengwei/easytrip/core/model/GeoPoint.kt
- `SelectablePill()` --calls--> `label()`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/core/ui/component/SelectablePill.kt → app/src/main/java/com/yangchengwei/easytrip/workspace/TripWorkspaceScreen.kt
- `DayItinerarySheet()` --calls--> `EditTimingDialog()`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/DayItinerarySheet.kt → app/src/main/java/com/yangchengwei/easytrip/itinerary/ui/EditTimingDialog.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **旅行编排数据模型** — docs_superpowers_specs_2026_08_21_saved_place, docs_superpowers_specs_2026_08_21_itinerary_item, docs_superpowers_specs_2026_08_21_route_leg [EXTRACTED 1.00]

## Communities (121 total, 41 thin omitted)

### Community 0 - "TagEntity"
Cohesion: 0.13
Nodes (3): SchemaPlaceDao, SavedPlaceTagCrossRef, TagEntity

### Community 2 - "FakeRepository"
Cohesion: 0.09
Nodes (21): connected(), hasValidatedInternet(), StateFlow, NetworkMonitor, onAvailable(), onCapabilitiesChanged(), onLost(), Active (+13 more)

### Community 3 - "FakeTripRepository"
Cohesion: 0.19
Nodes (3): FakeTripRepository, Flow, TripServiceTest

### Community 4 - "awaitSdkCallback"
Cohesion: 0.08
Nodes (18): OneShotCallback, awaitSdkCallback(), BridgeState, CANCELLED, COMPLETED, READY, STARTED, STARTING (+10 more)

### Community 5 - "TripSettingsViewModel"
Cohesion: 0.11
Nodes (11): androidx, DayDeleteImpact, DayUi, Factory, StateFlow, T, ViewModel, ViewModelProvider (+3 more)

### Community 6 - "AmapConsentToken"
Cohesion: 0.09
Nodes (12): AmapConsentToken, AmapPrivacyStateMachine, ConsentRegistry, ConsentSnapshot, StateFlow, TestConsentGate, AppContainer, CoroutineScope (+4 more)

### Community 7 - "Places"
Cohesion: 0.11
Nodes (5): FakeMapPreferences, Itineraries, Role, MapLayerFlowTest, Places

### Community 9 - "PlaceSearchReducer"
Cohesion: 0.20
Nodes (8): Job, PlaceSearchDataSource, StateFlow, PlaceSearchReducer, PlaceSearchState, PlaceSearchDataSource, PlaceSearchReducerTest, SearchSource

### Community 10 - "FakeItineraries"
Cohesion: 0.16
Nodes (7): Add, FakeCoordinator, FakeItineraries, ItineraryEditingTest, kotlinx, Move, Timing

### Community 11 - "AmapComposeMap.kt"
Cohesion: 0.09
Nodes (12): android, AmapComposeMap(), AmapMapHost, Bounds, Modifier, View, MapZoomButton(), RealAmapMapHost (+4 more)

### Community 12 - "DayItineraryViewModel"
Cohesion: 0.10
Nodes (7): DayItineraryUiState, DayItineraryViewModel, Factory, StateFlow, T, ViewModel, ViewModelProvider

### Community 13 - "TripListViewModel"
Cohesion: 0.10
Nodes (15): CreateTripDialog(), TripListScreen(), CreateTimeMode, DATED, DRAFT, Factory, StateFlow, T (+7 more)

### Community 14 - "MapViewportControllerTest"
Cohesion: 0.16
Nodes (3): MapViewportRequest, MapViewportController, MapViewportControllerTest

### Community 15 - "TripDay"
Cohesion: 0.11
Nodes (10): Flow, InsertSide, AFTER, BEFORE, Flow, TripDay, TripRepository, TripSummary (+2 more)

### Community 16 - "RouteStatus"
Cohesion: 0.11
Nodes (9): RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK, Flow, RouteLegDao (+1 more)

### Community 17 - "TravelMode"
Cohesion: 0.12
Nodes (15): TransportMode, DRIVE, TAXI, TRANSIT, WALK, TravelMode, FLEXIBLE, SELF_DRIVE (+7 more)

### Community 18 - "CreateTrip"
Cohesion: 0.15
Nodes (5): IdFactory, T, MutableClock, RoomTripRepositoryTest, CreateTrip

### Community 19 - "RouteLegRepository"
Cohesion: 0.07
Nodes (13): Flow, Flow, Flow, Flow, Flow, RouteLegRepository, RouteLegWithEndpoints, Failure (+5 more)

### Community 21 - "Easy Trip 一期设计"
Cohesion: 0.17
Nodes (12): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 离线路线等待与联网续算, 同一地点可重复安排行程 (+4 more)

### Community 22 - "Converters"
Cohesion: 0.10
Nodes (6): Converters, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT, UNSUPPORTED_TRANSIT

### Community 23 - "PlacePoolViewModel"
Cohesion: 0.10
Nodes (10): PendingCollectionRemoval, Factory, kotlinx, PlaceSearchDataSource, StateFlow, T, ViewModel, ViewModelProvider (+2 more)

### Community 24 - "SavedPlaceRepository"
Cohesion: 0.15
Nodes (7): Flow, AlreadySaved, Flow, PlaceTag, Saved, SavedPlaceRepository, SavePlaceResult

### Community 26 - "RoomItineraryRepository"
Cohesion: 0.06
Nodes (11): ItineraryTransactionTest, SequenceIds, DayItems, DayItineraryRow, ItineraryDao, Flow, ItineraryItemEntity, RoomItineraryRepository (+3 more)

### Community 27 - "Legs"
Cohesion: 0.07
Nodes (6): DayItinerarySelectionTest, Itineraries, Flow, Trips, Legs, Trips

### Community 28 - "ItineraryRepository"
Cohesion: 0.21
Nodes (5): DayItinerary, ItineraryItem, ItineraryPlace, ItineraryRepository, Flow

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.17
Nodes (15): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+7 more)

### Community 31 - "RouteRequest"
Cohesion: 0.22
Nodes (8): AmapRouteDataSource, RouteDataSource, parseRouteResult(), RoutePathData, selectUsablePath(), validateRouteRequest(), RouteRequest, RouteContractsTest

### Community 32 - "MemoryPreferences"
Cohesion: 0.17
Nodes (3): SharedPreferencesMapPreferences, MapPreferencesTest, MemoryPreferences

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.14
Nodes (14): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+6 more)

### Community 34 - "SelectablePill"
Cohesion: 0.24
Nodes (6): SelectablePillTest, Modifier, Role, SelectablePill(), SelectablePillStyle, DaySelector()

### Community 35 - "Easy Trip 工作台导航重构设计"
Cohesion: 0.06
Nodes (33): 10. 数据模型, 11. 空态与错误处理, 12. 可访问性, 13. 测试策略, 14. 验收标准, 1. 目标, 2. 非目标, 3. 信息架构 (+25 more)

### Community 37 - "AmapRouteDataSource.kt"
Cohesion: 0.33
Nodes (6): RouteSearch, RouteCallback, BusRouteResult, DriveRouteResult, RideRouteResult, WalkRouteResult

### Community 38 - "AmapComposeMapTest.kt"
Cohesion: 0.14
Nodes (14): AmapComposeMapTest, Lifecycle, LifecycleOwner, TestOwner, MapMarkerKind, SAVED_ITINERARY, SAVED_PLACE_POOL, UNSAVED_SEARCH (+6 more)

### Community 40 - "Legs"
Cohesion: 0.05
Nodes (6): Itineraries, com, Legs, Places, Trips, WorkspaceSearchTabsTest

### Community 41 - "MapLayer"
Cohesion: 0.23
Nodes (6): MapLayer, SATELLITE_ROAD, STANDARD, InMemoryMapPreferences, StateFlow, MapPreferences

### Community 43 - "ItineraryScope"
Cohesion: 0.12
Nodes (13): Day, decodeItineraryScope(), encodeItineraryScope(), ItineraryScope, reconcileItineraryScope(), RestoredWorkspaceNavigation, restoreWorkspaceNavigation(), toMapScope() (+5 more)

### Community 44 - "RoomTripRepository"
Cohesion: 0.13
Nodes (3): CascadeDeleteTest, PlaceService, RoomTripRepository

### Community 46 - "TimeMode"
Cohesion: 0.29
Nodes (3): TimeMode, DATED, DRAFT

### Community 47 - "EasyTripDatabase"
Cohesion: 0.08
Nodes (6): CascadeCountDao, EasyTripDatabase, com, SchemaItineraryDao, SchemaRouteDao, RoomDatabase

### Community 48 - "Dp"
Cohesion: 0.24
Nodes (11): ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton(), CompactDangerButton(), CompactPrimaryButton(), CompactSecondaryButton() (+3 more)

### Community 49 - "TripWorkspaceScreen"
Cohesion: 0.30
Nodes (8): Modifier, label(), settledWorkspaceSheetLevel(), TripWorkspaceScreen(), WorkspaceSearchLauncher(), WorkspaceSheetHandle(), WorkspaceSheetSyncTest, SheetValue

### Community 51 - "Easy Trip 搜索与收藏交互设计"
Cohesion: 0.07
Nodes (27): 10. 边界与错误处理, 11. 测试策略, 12. 验收标准, 1. 背景与目标, 2. 非目标, 3. 当前实现基线, 4.1 工作台, 4.2 独立搜索页 (+19 more)

### Community 52 - "AppNavigation.kt"
Cohesion: 0.39
Nodes (6): AppNavigation(), tripSearchRoute(), PlaceSearchField(), PlaceSearchScreen(), TripSettingsScreen(), DeleteImpactProvider

### Community 53 - "DayItinerarySheet"
Cohesion: 0.27
Nodes (8): DayItinerarySheet(), Modifier, EditTimingDialog(), RouteLegUi, Modifier, modeLabel(), RouteLegContent(), RouteLegRow()

### Community 54 - "PlaceDao"
Cohesion: 0.16
Nodes (3): Flow, PlaceDao, PlaceSnapshotRow

### Community 55 - "MapPoiUi"
Cohesion: 0.17
Nodes (5): Itineraries, AmapMapHost, PoiHost, WorkspaceFlowTest, MapPoiUi

### Community 56 - "GeoPoint"
Cohesion: 0.17
Nodes (5): GeoPoint, Result, PolylineCodec, PolylineCodecTest, MapViewportRenderingPolicyTest

### Community 58 - "SavedPlacesContent"
Cohesion: 0.36
Nodes (5): Modifier, PlaceSearchResults(), LazyListState, Modifier, SavedPlacesContent()

### Community 59 - "MapFacade.kt"
Cohesion: 0.20
Nodes (13): CorruptRoute, MapMarkerUi, MapPolylineUi, MapRouteLabelUi, MapScope, PLACE_POOL, SINGLE_DAY, WHOLE_TRIP (+5 more)

### Community 60 - "TripWorkspaceViewModel.kt"
Cohesion: 0.16
Nodes (15): consumeSearchSelection(), Flow, SavedStateHandle, StateFlow, observeSnapshots(), restoreWorkspaceTab(), SearchResultSelection, SearchSelectionPayload (+7 more)

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 62 - "AmapServiceException"
Cohesion: 0.30
Nodes (5): AmapServiceException, parsePlaces(), RawPlace, PlaceContractsTest, Exception

### Community 63 - ".searchCollectionMapAndRestorationFlow"
Cohesion: 0.15
Nodes (5): AmapMapHost, PlaceSearchDataSource, RecordingHost, V2AcceptanceTest, PlaceSearchDataSource

### Community 65 - "TripWorkspaceViewModel"
Cohesion: 0.17
Nodes (7): Factory, T, ViewModel, ViewModelProvider, TripWorkspaceUiState, TripWorkspaceViewModel, CreationExtras

### Community 69 - "TripWorkspaceNavigationStateTest"
Cohesion: 0.30
Nodes (3): SavedStateHandle, Trips, TripWorkspaceNavigationStateTest

### Community 72 - "RoutePlannerTest"
Cohesion: 0.38
Nodes (3): RouteDataSource, RoutePlannerTest, RouteDataSource

### Community 73 - "V2AcceptanceTest.kt"
Cohesion: 0.18
Nodes (8): PlaceSearchDataSource, PlacePoolFlowTest, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, RoomSavedPlaceRepository, PlacePoolSheet()

### Community 74 - "AmapMapHost"
Cohesion: 0.15
Nodes (4): AmapMapHost, AmapMapHost, AmapMapHost, View

### Community 75 - "FakeTripRepository"
Cohesion: 0.19
Nodes (4): FakeImpacts, FakeTripRepository, MoveCall, TripFlowTest

### Community 76 - "MainActivity.kt"
Cohesion: 0.47
Nodes (3): EasyTripTheme(), MainActivity, Bundle

### Community 79 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 80 - "Easy Trip"
Cohesion: 0.50
Nodes (4): 高德地图本地密钥配置, Easy Trip, 本地优先 Android 旅行规划, 未授权或离线本地编辑

### Community 86 - "Context"
Cohesion: 0.12
Nodes (8): AppIconResourceTest, AmapPrivacyGate, CallbackBoundary, PoiSearch, CallbackBoundary, com, Context, PoiResult

### Community 90 - "RouteResult"
Cohesion: 0.16
Nodes (7): V1AcceptanceTest, RouteDataSource, RouteMode, DRIVE, TRANSIT, WALK, RouteResult

### Community 91 - "SavedPlace"
Cohesion: 0.20
Nodes (8): SavedPlace, CollectionDecision, Confirm, decideCollectionToggle(), RemoveNow, Save, EditSavedPlaceDialog(), CollectionTogglePolicyTest

### Community 94 - "WholeTripItineraryContent"
Cohesion: 0.27
Nodes (5): WholeTripItineraryContentTest, WholeTripDayUi, dayHeading(), Modifier, WholeTripItineraryContent()

### Community 96 - "WorkspaceItineraryContent"
Cohesion: 0.29
Nodes (6): ItineraryScopeRailTest, ItineraryScopeRail(), Modifier, ScopeItem(), Modifier, WorkspaceItineraryContent()

### Community 97 - "ItineraryUiModels.kt"
Cohesion: 0.33
Nodes (7): ItineraryItemUi, mapWholeTripDays(), toItineraryItemUi(), toRouteErrorSummary(), toRouteLegUi(), DayMapSnapshot, WholeTripItineraryMapperTest

### Community 99 - "LazyScrollbar.kt"
Cohesion: 0.29
Nodes (6): calculateScrollbarThumb(), LazyListState, Modifier, ReadOnlyLazyScrollbar(), ScrollbarThumb, LazyScrollbarGeometryTest

### Community 100 - "reduceMapInteraction"
Cohesion: 0.27
Nodes (6): FocusSearchResult, MapInteractionAction, MapInteractionState, ReconcileSearchResults, reduceMapInteraction(), MapInteractionReducerTest

### Community 102 - "File Structure"
Cohesion: 0.18
Nodes (10): Easy Trip Workspace Navigation Implementation Plan, File Structure, Global Constraints, Task 1: 建立统一导航模型与兼容迁移, Task 2: 建立全程只读 UI 模型, Task 3: Make TripWorkspaceViewModel the single navigation source, Task 4: Separate editable day content from date selection and share presentation, Task 5: Build the 88dp itinerary scope rail and two-column content (+2 more)

### Community 105 - "Global Constraints"
Cohesion: 0.20
Nodes (9): Easy Trip Search Collection Implementation Plan, Global Constraints, Task 1: 收敛工作台 Tab 并建立独立搜索路由, Task 2: 完成搜索页状态清理与一次性返回聚焦, Task 3: 统一搜索结果与地点卡片的收藏切换策略, Task 4: 建立稳定的收藏 marker 身份、编号与聚焦样式, Task 5: 接入高德底图 POI 点击与统一地点卡片, Task 6: 为地点池增加只读纵向 scrollbar thumb (+1 more)

### Community 107 - "PlacePoolScrollbarTest.kt"
Cohesion: 0.46
Nodes (3): LazyListState, Modifier, PlacePoolScrollbarTest

### Community 114 - "ItineraryItemRow.kt"
Cohesion: 0.60
Nodes (5): ItineraryItemCard(), ItineraryItemRow(), Modifier, semanticsActions(), Color

### Community 117 - "SearchMapFocusTest"
Cohesion: 0.50
Nodes (4): Lifecycle, LifecycleOwner, SearchMapFocusTest, TestOwner

### Community 119 - "WorkspaceSheetLevel"
Cohesion: 0.40
Nodes (4): WorkspaceSheetLevel, COLLAPSED, EXPANDED, HALF

### Community 120 - "行程项"
Cohesion: 0.67
Nodes (3): 行程项, 路线段, 收藏地点

## Knowledge Gaps
- **149 isolated node(s):** `READY`, `STARTING`, `STARTED`, `CANCELLED`, `COMPLETED` (+144 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **41 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `PlaceCandidate`, `FakeRepository`, `PlaceSearchReducer`, `FakeItineraries`, `AmapComposeMap.kt`, `MapViewportControllerTest`, `TripDay`, `TravelMode`, `RouteLegRepository`, `SavedPlaceRepository`, `ItineraryRepository`, `RouteRequest`, `AmapRouteDataSource.kt`, `AmapComposeMapTest.kt`, `RoomRouteLegRepository`, `RoomTripRepository`, `AmapSmokeTest`, `MapPoiUi`, `RecordingHost`, `MapFacade.kt`, `TripWorkspaceViewModel.kt`, `AmapServiceException`, `.searchCollectionMapAndRestorationFlow`, `.independentSearchSelectionClearsPageAndIsConsumedOnce`, `TripWorkspaceViewModel`, `RoomSavedPlaceRepositoryTest`, `RoomRouteLegRepositoryTest`, `TripWorkspaceNavigationStateTest`, `RoutePlannerTest`, `V2AcceptanceTest.kt`, `Context`, `RouteResult`, `SavedPlace`, `ItineraryUiModels.kt`, `reduceMapInteraction`, `SavedPlaces`, `PlacePoolScrollbarTest.kt`, `Itineraries`, `Places`, `SearchMapFocusTest`?**
  _High betweenness centrality (0.179) - this node is a cross-community bridge._
- **Why does `RouteLegRepository` connect `RouteLegRepository` to `FakeRepository`, `RoomRouteLegRepository`, `Legs`, `Legs`, `DayItineraryViewModel`, `Legs`, `FakeLegs`, `TripDay`, `TripWorkspaceViewModel.kt`, `Legs`, `Legs`, `ItineraryRepository`, `Legs`?**
  _High betweenness centrality (0.084) - this node is a cross-community bridge._
- **Why does `TripRepository` connect `TripDay` to `FakeTripRepository`, `TripSettingsViewModel`, `Trips`, `Trips`, `Trips`, `Legs`, `FakeTripRepository`, `DayItineraryViewModel`, `RoomTripRepository`, `TripListViewModel`, `ItineraryRepository`, `TravelMode`, `TripFlowTest.kt`, `AppNavigation.kt`, `Trips`, `Legs`, `TripWorkspaceViewModel.kt`, `FakeTrips`?**
  _High betweenness centrality (0.056) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 3 inferred relationships involving `TripDay` (e.g. with `FakeTrips` and `.createTrip()`) actually correct?**
  _`TripDay` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `READY`, `STARTING`, `STARTED` to the rest of the system?**
  _149 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `TagEntity` be split into smaller, more focused modules?**
  _Cohesion score 0.13071895424836602 - nodes in this community are weakly interconnected._