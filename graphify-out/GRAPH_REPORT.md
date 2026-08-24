# Graph Report - easy-trip-v1-full-ui-run  (2026-08-24)

## Corpus Check
- 217 files · ~103,268 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2788 nodes · 5950 edges · 142 communities (106 shown, 36 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 395 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e303fcfd`
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
- FakeItineraries
- RealAmapMapHost
- DayItineraryViewModel
- TripListViewModel
- MapViewportControllerTest
- TravelMode
- RouteStatus
- TransportMode
- CreateTrip
- RouteLegRepository
- TripListContent.kt
- Easy Trip 一期设计
- TripDao
- PlacePoolViewModel
- PlaceCandidate
- Easy Trip v1.0 Pencil 设计落地方案
- PlaceSearchViewModel
- Legs
- GeoPoint
- Easy Trip v2 交互与视觉升级设计
- Easy Trip UI 设计公式
- AmapServiceException
- MemoryPreferences
- 高德 Android SDK 集成决策
- SelectablePill
- Easy Trip 工作台导航重构设计
- TripDayEntity
- ItineraryTransactionTest
- MapUiModel
- 2026-08-23-easy-trip-v1-full-ui-implementation.md
- Legs
- AppNavigation.kt
- View
- WorkspaceNavigationTest
- DayItineraryContent
- Trips
- PlaceDao
- EasyTripDatabase
- Dp
- TripWorkspaceContent.kt
- WorkspaceOverlay
- Easy Trip 搜索与收藏交互设计
- FakeRepository
- DeleteImpactDao
- WorkspaceItineraryContent
- Legs
- WholeTripItineraryContent
- CompactSecondaryButton
- AmapComposeMap
- MapFacade.kt
- RoomTripRepository
- RouteFormattingTest
- parsePlaces
- TripWorkspaceViewModel
- CreateTripViewModel
- RoomItineraryRepository
- MapLifecycleController
- ItineraryDao
- TestTripRepository
- TripWorkspaceNavigationStateTest
- ItineraryService
- MapLayerRenderingPolicyTest
- Task 4 报告：重做搜索状态机与连续收藏
- TripWorkspaceAction
- AmapRouteDataSource.kt
- FakeTripRepository
- CreateTripAction
- FakeLegs
- Trips
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest
- AmapPlaceDataSource.kt
- TripListContent
- TripWorkspaceScreen
- TripService
- reduceMapInteraction
- PlacePoolScrollbarTest.kt
- CLAUDE.md
- ItineraryScope
- FeedbackState
- FakeTrips
- AmapConsentToken
- Legs
- RecordingTripRepository
- LazyScrollbar.kt
- settledWorkspaceSheetLevel
- CreateTripUiState
- File Structure
- File Structure
- ConfirmationDialog
- Global Constraints
- Easy Trip v1.0 Pencil Reference
- Batch 1 visual fix report
- Trips
- Trips
- ItineraryUiModels.kt
- CreateTripContent
- OneShotCallback
- AmapSmokeTest
- PlacePoolContent
- Places
- .setContent
- AmapMapHost
- Places
- MapLayer
- 行程项
- RecordingHost
- MapLayerFlowTest
- TestMapHost
- Factory
- formatOccurrenceBadge
- Itineraries
- RoutePlannerTest
- Legs
- Itineraries
- TripListAction
- guard-adb-install.sh
- RouteLegRow.kt
- MutableItineraries
- AmapMapHost
- SDD ledger — plan: docs/superpowers/plans/2026-08-23-easy-trip-v1-full-ui-implementation.md
- AmapMapHost
- BridgeState
- MapLegend
- MapMarkerKind
- .observeTrip
- .createTrip

## God Nodes (most connected - your core abstractions)
1. `GeoPoint` - 124 edges
2. `PlaceCandidate` - 86 edges
3. `TravelMode` - 63 edges
4. `TripWorkspaceViewModel` - 60 edges
5. `SavedPlace` - 57 edges
6. `TripDay` - 54 edges
7. `CreateTrip` - 52 edges
8. `TransportMode` - 50 edges
9. `TripService` - 46 edges
10. `PlacePoolViewModel` - 45 edges

## Surprising Connections (you probably didn't know these)
- `PlacePoolContent()` --calls--> `PlaceSearchField()`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt → app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchField.kt
- `FakeTrips` --calls--> `TripDay`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `FakeTrips` --calls--> `TripWithDays`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `EasyTripApplication` --calls--> `AppContainer`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/EasyTripApplication.kt → app/src/main/java/com/yangchengwei/easytrip/AppContainer.kt
- `RoutePlannerTest` --calls--> `GeoPoint`  [INFERRED]
  app/src/test/java/com/yangchengwei/easytrip/route/domain/RoutePlannerTest.kt → app/src/main/java/com/yangchengwei/easytrip/core/model/GeoPoint.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **旅行编排数据模型** — docs_superpowers_specs_2026_08_21_saved_place, docs_superpowers_specs_2026_08_21_itinerary_item, docs_superpowers_specs_2026_08_21_route_leg [EXTRACTED 1.00]

## Communities (142 total, 36 thin omitted)

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
Cohesion: 0.13
Nodes (10): awaitSdkCallback(), CallbackBoundary, cleanupBoundary(), T, FakeBoundary, CallbackBoundary, Result, T (+2 more)

### Community 5 - "TripSettingsViewModel"
Cohesion: 0.10
Nodes (11): androidx, DayDeleteImpact, DayUi, Factory, StateFlow, T, ViewModel, ViewModelProvider (+3 more)

### Community 8 - "Trips"
Cohesion: 0.06
Nodes (10): Itineraries, com, CreateTrip, Flow, Trips, Legs, Places, Trips (+2 more)

### Community 9 - "PlaceSearchReducer"
Cohesion: 0.08
Nodes (31): PlaceSearchContentTest, BackIcon(), BookmarkIcon(), CloseIcon(), Color, Modifier, LocationIcon(), PlaceSearchContent() (+23 more)

### Community 10 - "FakeItineraries"
Cohesion: 0.17
Nodes (7): Add, FakeCoordinator, FakeItineraries, ItineraryEditingTest, kotlinx, Move, Timing

### Community 11 - "RealAmapMapHost"
Cohesion: 0.11
Nodes (3): AmapMapHost, android, RealAmapMapHost

### Community 12 - "DayItineraryViewModel"
Cohesion: 0.18
Nodes (3): DayItineraryViewModel, StateFlow, ViewModel

### Community 13 - "TripListViewModel"
Cohesion: 0.15
Nodes (11): Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider, OpenSettings (+3 more)

### Community 14 - "MapViewportControllerTest"
Cohesion: 0.08
Nodes (14): MapScope, PLACE_POOL, SINGLE_DAY, WHOLE_TRIP, MapViewportRequest, ViewportReason, INITIAL, PLACE_SET_CHANGED (+6 more)

### Community 15 - "TravelMode"
Cohesion: 0.09
Nodes (16): Flow, Flow, TravelMode, FLEXIBLE, SELF_DRIVE, Flow, InsertSide, AFTER (+8 more)

### Community 16 - "RouteStatus"
Cohesion: 0.08
Nodes (10): RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK, Flow, RouteLegDao (+2 more)

### Community 17 - "TransportMode"
Cohesion: 0.12
Nodes (11): TransportMode, DRIVE, TAXI, TRANSIT, WALK, defaultRecommendMode(), haversineMeters(), Flow (+3 more)

### Community 18 - "CreateTrip"
Cohesion: 0.10
Nodes (7): RoomSavedPlaceRepositoryTest, IdFactory, T, MutableClock, RoomTripRepositoryTest, CreateTrip, Clock

### Community 19 - "RouteLegRepository"
Cohesion: 0.08
Nodes (13): Flow, Flow, Flow, RouteLegRepository, RouteLegWithEndpoints, RouteDataSource, RouteResult, Failure (+5 more)

### Community 20 - "TripListContent.kt"
Cohesion: 0.16
Nodes (21): ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton(), EasyTripButton(), EasyTripButtonStyle, DANGER (+13 more)

### Community 21 - "Easy Trip 一期设计"
Cohesion: 0.17
Nodes (12): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 离线路线等待与联网续算, 同一地点可重复安排行程 (+4 more)

### Community 22 - "TripDao"
Cohesion: 0.05
Nodes (12): Converters, TimeMode, DATED, DRAFT, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT (+4 more)

### Community 23 - "PlacePoolViewModel"
Cohesion: 0.05
Nodes (22): PlaceSearchDataSource, PlacePoolFlowTest, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, PendingCollectionRemoval, PlacePoolSheet(), Factory (+14 more)

### Community 24 - "PlaceCandidate"
Cohesion: 0.07
Nodes (17): EditingPlaces, PlaceCandidate, PlaceSearchDataSource, Flow, AlreadySaved, Flow, PlaceTag, Saved (+9 more)

### Community 25 - "Easy Trip v1.0 Pencil 设计落地方案"
Cohesion: 0.05
Nodes (39): 10.1 JVM, 10.2 Compose instrumentation, 10.3 数据与导航集成, 10.4 验证命令, 10. 测试策略, 11. 视觉验收, 12. 验收标准, 13. 协作边界 (+31 more)

### Community 26 - "PlaceSearchViewModel"
Cohesion: 0.07
Nodes (21): Back, ConfirmRemoval, DismissRemovalConfirmation, Factory, StateFlow, T, ViewModel, ViewModelProvider (+13 more)

### Community 27 - "Legs"
Cohesion: 0.06
Nodes (7): DayItinerarySelectionTest, Itineraries, CreateTrip, Flow, Trips, Legs, Trips

### Community 28 - "GeoPoint"
Cohesion: 0.11
Nodes (9): GeoPoint, DayItinerary, ItineraryItem, ItineraryPlace, ItineraryRepository, Flow, Result, PolylineCodec (+1 more)

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.17
Nodes (15): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+7 more)

### Community 30 - "Easy Trip UI 设计公式"
Cohesion: 0.05
Nodes (43): 10.1 无网, 10.2 路线失败, 10.3 地图加载中或失败, 10.4 部分成功, 10.5 提交中与目标日失效, 10.6 保存失败, 10. 本地优先与状态公式, 11.1 地图服务授权 (+35 more)

### Community 31 - "AmapServiceException"
Cohesion: 0.16
Nodes (13): AmapServiceException, AmapRouteDataSource, parseRouteResult(), RoutePathData, selectUsablePath(), validateRouteRequest(), RouteMode, DRIVE (+5 more)

### Community 32 - "MemoryPreferences"
Cohesion: 0.13
Nodes (4): Editor, MapPreferencesTest, MemoryPreferences, SharedPreferences

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.14
Nodes (14): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+6 more)

### Community 34 - "SelectablePill"
Cohesion: 0.19
Nodes (9): SelectablePillTest, Modifier, Role, SelectablePill(), SelectablePillStyle, DaySelector(), LazyListState, Modifier (+1 more)

### Community 35 - "Easy Trip 工作台导航重构设计"
Cohesion: 0.06
Nodes (33): 10. 数据模型, 11. 空态与错误处理, 12. 可访问性, 13. 测试策略, 14. 验收标准, 1. 目标, 2. 非目标, 3. 信息架构 (+25 more)

### Community 36 - "TripDayEntity"
Cohesion: 0.13
Nodes (6): Fixture, SchemaTest, OfflineRecoveryTest, CreateTrip, TripDayEntity, TripEntity

### Community 38 - "MapUiModel"
Cohesion: 0.21
Nodes (6): AmapComposeMapTest, Lifecycle, LifecycleOwner, TestOwner, toMapPoiUi(), MapUiModel

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
Cohesion: 0.06
Nodes (8): AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, View

### Community 43 - "WorkspaceNavigationTest"
Cohesion: 0.23
Nodes (4): reconcileItineraryScope(), RestoredWorkspaceNavigation, restoreWorkspaceNavigation(), WorkspaceNavigationTest

### Community 44 - "DayItineraryContent"
Cohesion: 0.20
Nodes (17): AddPlace, CommitMove, ConfirmDelete, DayItineraryAction, DayItineraryContent(), DayItinerarySheet(), DismissDialogs, Modifier (+9 more)

### Community 45 - "Trips"
Cohesion: 0.06
Nodes (11): Itineraries, AmapMapHost, CreateTrip, Lifecycle, LifecycleOwner, Places, RecordingHost, SavedPlaces (+3 more)

### Community 46 - "PlaceDao"
Cohesion: 0.07
Nodes (7): RoomDeleteImpactProviderTest, SchemaPlaceDao, Flow, PlaceDao, PlaceSnapshotRow, SavedPlaceTagCrossRef, TagEntity

### Community 47 - "EasyTripDatabase"
Cohesion: 0.06
Nodes (9): AppIconResourceTest, V2AcceptanceTest, CascadeCountDao, EasyTripDatabase, com, SchemaItineraryDao, SchemaRouteDao, Context (+1 more)

### Community 48 - "Dp"
Cohesion: 0.13
Nodes (11): EasyTripButtonTest, EasyTripIconButton(), Modifier, EmptyState(), Modifier, EasyTripElevation, EasyTripSizes, EasyTripSpacing (+3 more)

### Community 49 - "TripWorkspaceContent.kt"
Cohesion: 0.16
Nodes (16): DayItineraryUiState, PlacePoolUiState, description(), Modifier, MapControls(), Modifier, SearchSurface(), Composable (+8 more)

### Community 50 - "WorkspaceOverlay"
Cohesion: 0.12
Nodes (16): AddTripDay, Confirmation, EditItineraryItem, EditRouteLeg, Feedback, FeedbackUiModel, LayerMenu, None (+8 more)

### Community 51 - "Easy Trip 搜索与收藏交互设计"
Cohesion: 0.07
Nodes (27): 10. 边界与错误处理, 11. 测试策略, 12. 验收标准, 1. 背景与目标, 2. 非目标, 3. 当前实现基线, 4.1 工作台, 4.2 独立搜索页 (+19 more)

### Community 52 - "FakeRepository"
Cohesion: 0.17
Nodes (4): CreateTripViewModelTest, FakeRepository, SavedStateHandle, FakeRepository

### Community 54 - "WorkspaceItineraryContent"
Cohesion: 0.29
Nodes (6): ItineraryScopeRailTest, ItineraryScopeRail(), Modifier, ScopeItem(), Modifier, WorkspaceItineraryContent()

### Community 56 - "WholeTripItineraryContent"
Cohesion: 0.27
Nodes (5): WholeTripItineraryContentTest, WholeTripDayUi, dayHeading(), Modifier, WholeTripItineraryContent()

### Community 57 - "CompactSecondaryButton"
Cohesion: 0.19
Nodes (16): CompactDangerButton(), CompactPrimaryButton(), CompactSecondaryButton(), Modifier, EditTimingDialog(), Modifier, PlaceDetailContent(), Modifier (+8 more)

### Community 58 - "AmapComposeMap"
Cohesion: 0.14
Nodes (10): AmapComposeMap(), Bounds, Modifier, View, MapHostCallbackGuard, MapZoomButton(), SinglePoint, ViewportCommand (+2 more)

### Community 59 - "MapFacade.kt"
Cohesion: 0.20
Nodes (10): mapWholeTripDays(), DayMapSnapshot, MapMarkerUi, MapPolylineUi, MapRouteLabelUi, MapUiModelMapper, mapViewportPoints(), OccurrenceUi (+2 more)

### Community 60 - "RoomTripRepository"
Cohesion: 0.07
Nodes (9): CascadeDeleteTest, V1AcceptanceTest, V1PencilFlowTest, AppContainer, CoroutineScope, RoomSavedPlaceRepository, PlaceService, RoomTripRepository (+1 more)

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 62 - "parsePlaces"
Cohesion: 0.43
Nodes (3): parsePlaces(), RawPlace, PlaceContractsTest

### Community 63 - "TripWorkspaceViewModel"
Cohesion: 0.09
Nodes (20): Factory, CreationExtras, Flow, Job, StateFlow, T, ViewModel, ViewModelProvider (+12 more)

### Community 64 - "CreateTripViewModel"
Cohesion: 0.23
Nodes (8): CreateTripViewModel, Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider

### Community 65 - "RoomItineraryRepository"
Cohesion: 0.21
Nodes (5): ItineraryItemEntity, RoomItineraryRepository, AdjacencyDiff, Edge, AdjacencyPlannerTest

### Community 67 - "ItineraryDao"
Cohesion: 0.14
Nodes (4): DayItems, DayItineraryRow, ItineraryDao, Flow

### Community 68 - "TestTripRepository"
Cohesion: 0.11
Nodes (7): TripDeleteImpact, CreateTrip, Flow, TestImpacts, TestTripRepository, TripListViewModelTest, RuntimeException

### Community 69 - "TripWorkspaceNavigationStateTest"
Cohesion: 0.25
Nodes (3): SavedStateHandle, Trips, TripWorkspaceNavigationStateTest

### Community 72 - "Task 4 报告：重做搜索状态机与连续收藏"
Cohesion: 0.13
Nodes (14): Device gate review fix round 1, Fix round 1, Fix round 2, Fix round 3, Fix round 4, Graphify, Pencil 对照, Task 4 报告：重做搜索状态机与连续收藏 (+6 more)

### Community 73 - "TripWorkspaceAction"
Cohesion: 0.09
Nodes (24): Modifier, WorkspaceMapFallback(), Back, CloseOverlay, ConsentRequired, Error, Failed, Loading (+16 more)

### Community 74 - "AmapRouteDataSource.kt"
Cohesion: 0.18
Nodes (9): CallbackBoundary, RouteSearch, CallbackBoundary, RouteDataSource, RouteCallback, BusRouteResult, DriveRouteResult, RideRouteResult (+1 more)

### Community 75 - "FakeTripRepository"
Cohesion: 0.15
Nodes (7): FakeImpacts, FakeTripRepository, CreateTrip, Flow, Role, MoveCall, TripFlowTest

### Community 76 - "CreateTripAction"
Cohesion: 0.13
Nodes (14): Back, CreateTimeMode, DATED, DRAFT, CreateTripAction, CreateTripEffect, DayCountChanged, NameChanged (+6 more)

### Community 78 - "Trips"
Cohesion: 0.10
Nodes (3): DelayedDeletePlaces, CreateTrip, Trips

### Community 79 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 80 - "Easy Trip"
Cohesion: 0.50
Nodes (4): 高德地图本地密钥配置, Easy Trip, 本地优先 Android 旅行规划, 未授权或离线本地编辑

### Community 86 - "AmapPlaceDataSource.kt"
Cohesion: 0.17
Nodes (8): AmapPlaceDataSource, CallbackBoundary, PoiSearch, CallbackBoundary, com, parsePoiSearchResponse(), AmapPlaceDataSourceTest, PoiResult

### Community 87 - "TripListContent"
Cohesion: 0.17
Nodes (12): TripListContentTest, CompactTripRow(), TripListContent(), TripListHeader(), Content, Empty, Error, Loading (+4 more)

### Community 88 - "TripWorkspaceScreen"
Cohesion: 0.10
Nodes (10): PlaceSearchDataSource, PlaceSearchDataSource, Itineraries, AmapMapHost, Places, PoiHost, WorkspaceFlowTest, MapPoiUi (+2 more)

### Community 89 - "TripService"
Cohesion: 0.15
Nodes (3): com, CreateTrip, TripService

### Community 90 - "reduceMapInteraction"
Cohesion: 0.27
Nodes (6): FocusSearchResult, MapInteractionAction, MapInteractionState, ReconcileSearchResults, reduceMapInteraction(), MapInteractionReducerTest

### Community 91 - "PlacePoolScrollbarTest.kt"
Cohesion: 0.46
Nodes (3): LazyListState, Modifier, PlacePoolScrollbarTest

### Community 93 - "ItineraryScope"
Cohesion: 0.21
Nodes (9): Day, decodeItineraryScope(), encodeItineraryScope(), ItineraryScope, toMapScope(), WholeTrip, WorkspaceSection, ITINERARY (+1 more)

### Community 94 - "FeedbackState"
Cohesion: 0.24
Nodes (11): FeedbackStateTest, EmptyFeedbackState(), ErrorFeedbackState(), FeedbackContent(), FeedbackKind, EMPTY, ERROR, LOADING (+3 more)

### Community 95 - "FakeTrips"
Cohesion: 0.15
Nodes (3): FakeTrips, CreateTrip, java

### Community 96 - "AmapConsentToken"
Cohesion: 0.10
Nodes (11): AmapConsentToken, AmapPrivacyGate, AmapPrivacyStateMachine, ConsentRegistry, ConsentSnapshot, StateFlow, TestConsentGate, EasyTripApplication (+3 more)

### Community 98 - "RecordingTripRepository"
Cohesion: 0.17
Nodes (3): CreateTrip, Flow, RecordingTripRepository

### Community 99 - "LazyScrollbar.kt"
Cohesion: 0.29
Nodes (6): calculateScrollbarThumb(), LazyListState, Modifier, ReadOnlyLazyScrollbar(), ScrollbarThumb, LazyScrollbarGeometryTest

### Community 100 - "settledWorkspaceSheetLevel"
Cohesion: 0.47
Nodes (3): settledWorkspaceSheetLevel(), WorkspaceSheetSyncTest, SheetValue

### Community 101 - "CreateTripUiState"
Cohesion: 0.32
Nodes (5): CreateTripUiState, CreateTripValidation, validateCreateTrip(), ValidCreateTrip, CreateTripValidatorTest

### Community 102 - "File Structure"
Cohesion: 0.18
Nodes (10): Easy Trip Workspace Navigation Implementation Plan, File Structure, Global Constraints, Task 1: 建立统一导航模型与兼容迁移, Task 2: 建立全程只读 UI 模型, Task 3: Make TripWorkspaceViewModel the single navigation source, Task 4: Separate editable day content from date selection and share presentation, Task 5: Build the 88dp itinerary scope rail and two-column content (+2 more)

### Community 103 - "File Structure"
Cohesion: 0.15
Nodes (12): Easy Trip v1.0 Pencil Implementation Plan, File Structure, Global Constraints, Task 1: 冻结 Pencil 主流程基线, Task 2: 建立 v1.0 Theme 与共享组件, Task 3: 拆分并实现“我的旅行”页面, Task 4: 实现创建旅行状态机与真实提交, Task 5: 拆分工作台并实现地图降级 (+4 more)

### Community 104 - "ConfirmationDialog"
Cohesion: 0.19
Nodes (9): ConfirmationDialogTest, ConfirmationDialog(), ConfirmationSection(), Color, Modifier, ConfirmationUiModel, TripListScreen(), confirmation() (+1 more)

### Community 105 - "Global Constraints"
Cohesion: 0.20
Nodes (9): Easy Trip Search Collection Implementation Plan, Global Constraints, Task 1: 收敛工作台 Tab 并建立独立搜索路由, Task 2: 完成搜索页状态清理与一次性返回聚焦, Task 3: 统一搜索结果与地点卡片的收藏切换策略, Task 4: 建立稳定的收藏 marker 身份、编号与聚焦样式, Task 5: 接入高德底图 POI 点击与统一地点卡片, Task 6: 为地点池增加只读纵向 scrollbar thumb (+1 more)

### Community 106 - "Easy Trip v1.0 Pencil Reference"
Cohesion: 0.18
Nodes (10): Easy Trip v1.0 Pencil Reference, Layout Verification, Main Flow Frames, Public Components, Responsive Notes, Screenshot Baseline, Semantic Tokens, Shape, Spacing, Size, Elevation (+2 more)

### Community 107 - "Batch 1 visual fix report"
Cohesion: 0.20
Nodes (9): Batch 1 visual fix report, Commit, Fix round 1, Fix round 1 commit, Fix round 1 最终验证, Frame 差异与修复, RED / GREEN, 修改文件 (+1 more)

### Community 110 - "ItineraryUiModels.kt"
Cohesion: 0.31
Nodes (9): ItineraryItemCard(), ItineraryItemRow(), Color, Modifier, semanticsActions(), ItineraryItemUi, toItineraryItemUi(), toRouteErrorSummary() (+1 more)

### Community 111 - "CreateTripContent"
Cohesion: 0.39
Nodes (4): CreateTripContentTest, CreateTripContent(), Modifier, StepNumber()

### Community 114 - "PlacePoolContent"
Cohesion: 0.15
Nodes (16): EditSavedPlaceDialog(), ConfirmCollectionRemoval, ConfirmDelete, Delete, DismissDialogs, Edit, Modifier, PlacePoolAction (+8 more)

### Community 119 - "MapLayer"
Cohesion: 0.23
Nodes (9): MapLayer, SATELLITE_ROAD, STANDARD, InMemoryMapPreferences, StateFlow, MapPreferences, SharedPreferencesMapPreferences, FailingMapPreferences (+1 more)

### Community 120 - "行程项"
Cohesion: 0.67
Nodes (3): 行程项, 路线段, 收藏地点

### Community 122 - "MapLayerFlowTest"
Cohesion: 0.38
Nodes (3): FakeMapPreferences, Role, MapLayerFlowTest

### Community 124 - "Factory"
Cohesion: 0.50
Nodes (3): Factory, T, ViewModelProvider

### Community 127 - "RoutePlannerTest"
Cohesion: 0.38
Nodes (3): RouteDataSource, RoutePlannerTest, RouteDataSource

### Community 130 - "TripListAction"
Cohesion: 0.33
Nodes (6): CreateTrip, OpenSettings, OpenTrip, RequestDelete, Retry, TripListAction

### Community 132 - "RouteLegRow.kt"
Cohesion: 0.60
Nodes (5): RouteLegUi, Modifier, modeLabel(), RouteLegContent(), RouteLegRow()

### Community 137 - "BridgeState"
Cohesion: 0.33
Nodes (6): BridgeState, CANCELLED, COMPLETED, READY, STARTED, STARTING

### Community 139 - "MapMarkerKind"
Cohesion: 0.50
Nodes (4): MapMarkerKind, SAVED_ITINERARY, SAVED_PLACE_POOL, UNSAVED_SEARCH

## Knowledge Gaps
- **378 isolated node(s):** `guard-adb-install.sh script`, `READY`, `STARTING`, `STARTED`, `CANCELLED` (+373 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **36 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `MapUiModelMapperTest`, `FakeRepository`, `RoomRouteLegRepository`, `PlaceSearchReducer`, `FakeItineraries`, `MapViewportControllerTest`, `TravelMode`, `RouteStatus`, `TransportMode`, `CreateTrip`, `RouteLegRepository`, `PlacePoolViewModel`, `PlaceCandidate`, `PlaceSearchViewModel`, `AmapServiceException`, `MapUiModel`, `Trips`, `EasyTripDatabase`, `AmapComposeMap`, `MapFacade.kt`, `RoomTripRepository`, `parsePlaces`, `TripWorkspaceViewModel`, `TripWorkspaceNavigationStateTest`, `AmapRouteDataSource.kt`, `Trips`, `AmapPlaceDataSource.kt`, `TripWorkspaceScreen`, `reduceMapInteraction`, `PlacePoolScrollbarTest.kt`, `AmapSmokeTest`, `RoutePlannerTest`?**
  _High betweenness centrality (0.121) - this node is a cross-community bridge._
- **Why does `RouteLegRepository` connect `RouteLegRepository` to `Legs`, `Legs`, `FakeRepository`, `RoomRouteLegRepository`, `Legs`, `Legs`, `AppNavigation.kt`, `Trips`, `FakeLegs`, `TravelMode`, `Legs`, `Legs`, `GeoPoint`, `TripWorkspaceViewModel`?**
  _High betweenness centrality (0.069) - this node is a cross-community bridge._
- **Why does `TravelMode` connect `TravelMode` to `FakeTripRepository`, `TripSettingsViewModel`, `Trips`, `TransportMode`, `CreateTrip`, `RouteLegRepository`, `TripDao`, `Legs`, `GeoPoint`, `TripDayEntity`, `ItineraryTransactionTest`, `Legs`, `Trips`, `PlaceDao`, `EasyTripDatabase`, `FakeRepository`, `CompactSecondaryButton`, `CreateTripViewModel`, `RoomItineraryRepository`, `ItineraryDao`, `TestTripRepository`, `FakeTripRepository`, `CreateTripAction`, `Trips`, `TripListContent`, `FakeTrips`, `RecordingTripRepository`, `CreateTripUiState`, `Trips`, `Trips`, `CreateTripContent`?**
  _High betweenness centrality (0.068) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 25 inferred relationships involving `TripWorkspaceViewModel` (e.g. with `.model()` and `.emptyScopeClearsRequestSoReplacementHostDoesNotReplayInitialViewport()`) actually correct?**
  _`TripWorkspaceViewModel` has 25 INFERRED edges - model-reasoned connections that need verification._
- **What connects `guard-adb-install.sh script`, `READY`, `STARTING` to the rest of the system?**
  _378 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Easy Trip v1.0 全量 UI 与交互落地设计` be split into smaller, more focused modules?**
  _Cohesion score 0.0425531914893617 - nodes in this community are weakly interconnected._