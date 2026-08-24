# Graph Report - easy-trip-v1-full-ui-run  (2026-08-24)

## Corpus Check
- 218 files · ~104,282 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2823 nodes · 6024 edges · 142 communities (103 shown, 39 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 403 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `20fae8ce`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Easy Trip v1.0 全量 UI 与交互落地设计
- MapUiModelMapperTest
- FakeRepository
- FakeTripRepository
- awaitSdkCallback
- TripSettingsViewModel
- RoomRouteLegRepositoryTest
- Legs
- Trips
- PlaceSearchReducer
- FakeItineraries
- RealAmapMapHost
- DayItineraryViewModel
- TripListViewModel
- MapViewportControllerTest
- TripDay
- TransportMode
- SavedPlaceEntity
- CreateTrip
- RouteLegRepository
- TripListContent.kt
- Easy Trip 一期设计
- TripDao
- PlacePoolViewModel
- PlaceCandidate
- Easy Trip v1.0 Pencil 设计落地方案
- PlaceSearchViewModel
- DayItinerarySelectionTest.kt
- GeoPoint
- Easy Trip v2 交互与视觉升级设计
- Easy Trip UI 设计公式
- AmapRouteDataSource.kt
- MemoryPreferences
- 高德 Android SDK 集成决策
- SelectablePill
- Easy Trip 工作台导航重构设计
- SchemaTest
- RoomItineraryRepository
- AmapComposeMap
- 2026-08-23-easy-trip-v1-full-ui-implementation.md
- Legs
- AppNavigation.kt
- View
- ItineraryScope
- DayItineraryContent
- Trips
- PlaceDao
- EasyTripDatabase
- EasyTripTokens.kt
- TripWorkspaceContent.kt
- WorkspaceOverlay
- Easy Trip 搜索与收藏交互设计
- FakeRepository
- SavedPlace
- FeedbackState
- Legs
- PlacePoolSheet
- CompactSecondaryButton
- Converters
- MapFacade.kt
- MapUiModel
- RouteFormattingTest
- Legs
- TripWorkspaceViewModel
- CreateTripViewModel
- ItineraryRepository
- MapLifecycleController
- ItineraryItemEntity
- TestTripRepository
- TripWorkspaceNavigationStateTest
- ItineraryService
- MapLayerRenderingPolicyTest
- Task 4 报告：重做搜索状态机与连续收藏
- TripWorkspacePageState
- Trips
- FakeTripRepository
- CreateTripAction
- FakeLegs
- Places
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest
- AmapPlaceDataSource.kt
- TripWorkspaceViewModel.kt
- MapPoiUi
- TripService
- reduceMapInteraction
- PlacePoolScrollbarTest.kt
- CLAUDE.md
- TripWorkspaceAction
- Task 5 报告：地点池、详情与地图控件
- FakeTrips
- AmapConsentToken
- Trips
- TripRepository
- LazyScrollbar.kt
- settledWorkspaceSheetLevel
- CreateTripUiState
- File Structure
- File Structure
- TripWorkspaceScreen
- Global Constraints
- Easy Trip v1.0 Pencil Reference
- Batch 1 visual fix report
- TravelMode
- Trips
- Dp
- CreateTripContent
- RoutePlanOutcome
- RecordingHost
- PlacePoolContent
- Itineraries
- RecordingHost
- SavedPlaces
- EditingPlaces
- MapLayer
- 行程项
- RoomRouteLegRepository
- .setContent
- SchemaPlaceDao
- Factory
- RoomDeleteImpactProviderTest.kt
- Places
- AmapMapHost
- Legs
- AmapMapHost
- formatOccurrenceBadge
- guard-adb-install.sh
- Itineraries
- RoomDeleteImpactProviderTest
- MutableItineraries
- SDD ledger — plan: docs/superpowers/plans/2026-08-23-easy-trip-v1-full-ui-implementation.md
- AmapMapHost
- MapControls
- SearchMapFocusTest
- AmapMapHost
- MapMarkerKind
- .createTrip

## God Nodes (most connected - your core abstractions)
1. `GeoPoint` - 125 edges
2. `PlaceCandidate` - 87 edges
3. `TravelMode` - 63 edges
4. `SavedPlace` - 60 edges
5. `TripWorkspaceViewModel` - 60 edges
6. `TripDay` - 54 edges
7. `CreateTrip` - 53 edges
8. `TransportMode` - 50 edges
9. `PlacePoolViewModel` - 47 edges
10. `TripService` - 46 edges

## Surprising Connections (you probably didn't know these)
- `PlacePoolContent()` --calls--> `PlaceSearchField()`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/place/ui/PlacePoolSheet.kt → app/src/main/java/com/yangchengwei/easytrip/place/ui/PlaceSearchField.kt
- `FakeTrips` --calls--> `TripDay`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `FakeTrips` --calls--> `TripWithDays`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `RoutePlannerTest` --calls--> `GeoPoint`  [INFERRED]
  app/src/test/java/com/yangchengwei/easytrip/route/domain/RoutePlannerTest.kt → app/src/main/java/com/yangchengwei/easytrip/core/model/GeoPoint.kt
- `CompactPrimaryButton()` --calls--> `EasyTripPrimaryButton()`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/core/ui/component/CompactActionButton.kt → app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EasyTripButton.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **旅行编排数据模型** — docs_superpowers_specs_2026_08_21_saved_place, docs_superpowers_specs_2026_08_21_itinerary_item, docs_superpowers_specs_2026_08_21_route_leg [EXTRACTED 1.00]

## Communities (142 total, 39 thin omitted)

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
Cohesion: 0.06
Nodes (7): FakeMapPreferences, Itineraries, com, Role, Legs, MapLayerFlowTest, Places

### Community 8 - "Trips"
Cohesion: 0.06
Nodes (10): Itineraries, com, CreateTrip, Flow, Trips, Legs, Places, Trips (+2 more)

### Community 9 - "PlaceSearchReducer"
Cohesion: 0.08
Nodes (31): PlaceSearchContentTest, BackIcon(), BookmarkIcon(), CloseIcon(), Color, Modifier, LocationIcon(), PlaceSearchContent() (+23 more)

### Community 10 - "FakeItineraries"
Cohesion: 0.18
Nodes (7): Add, FakeCoordinator, FakeItineraries, ItineraryEditingTest, kotlinx, Move, Timing

### Community 11 - "RealAmapMapHost"
Cohesion: 0.11
Nodes (3): AmapMapHost, android, RealAmapMapHost

### Community 12 - "DayItineraryViewModel"
Cohesion: 0.19
Nodes (3): DayItineraryViewModel, StateFlow, ViewModel

### Community 13 - "TripListViewModel"
Cohesion: 0.11
Nodes (17): CreateTrip, OpenSettings, OpenTrip, RequestDelete, Retry, TripListAction, Factory, CreationExtras (+9 more)

### Community 14 - "MapViewportControllerTest"
Cohesion: 0.09
Nodes (10): MapViewportRequest, ViewportReason, INITIAL, PLACE_SET_CHANGED, SCOPE_CHANGED, SEARCH_FOCUS, VISIBLE_SET_CHANGED, MapViewportController (+2 more)

### Community 15 - "TripDay"
Cohesion: 0.10
Nodes (11): Flow, Flow, InsertSide, AFTER, BEFORE, TripDay, TripSummary, TripWithDays (+3 more)

### Community 16 - "TransportMode"
Cohesion: 0.07
Nodes (15): RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK, TransportMode, DRIVE (+7 more)

### Community 17 - "SavedPlaceEntity"
Cohesion: 0.27
Nodes (4): OfflineRecoveryTest, defaultRecommendMode(), haversineMeters(), SavedPlaceEntity

### Community 18 - "CreateTrip"
Cohesion: 0.07
Nodes (10): CascadeDeleteTest, RoomSavedPlaceRepositoryTest, IdFactory, T, MutableClock, RoomTripRepositoryTest, CreateTrip, RoomTripRepository (+2 more)

### Community 20 - "TripListContent.kt"
Cohesion: 0.07
Nodes (41): EasyTripButtonTest, TripListContentTest, ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton(), CompactDangerButton() (+33 more)

### Community 21 - "Easy Trip 一期设计"
Cohesion: 0.17
Nodes (12): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 离线路线等待与联网续算, 同一地点可重复安排行程 (+4 more)

### Community 22 - "TripDao"
Cohesion: 0.16
Nodes (3): TripDao, TripDayEntity, TripEntity

### Community 23 - "PlacePoolViewModel"
Cohesion: 0.05
Nodes (14): Factory, Job, kotlinx, PlaceSearchDataSource, StateFlow, T, ViewModel, ViewModelProvider (+6 more)

### Community 24 - "PlaceCandidate"
Cohesion: 0.06
Nodes (12): AppIconResourceTest, V1AcceptanceTest, Places, PlaceCandidate, PlaceSearchDataSource, Flow, RoomSavedPlaceRepository, MapScope (+4 more)

### Community 25 - "Easy Trip v1.0 Pencil 设计落地方案"
Cohesion: 0.05
Nodes (39): 10.1 JVM, 10.2 Compose instrumentation, 10.3 数据与导航集成, 10.4 验证命令, 10. 测试策略, 11. 视觉验收, 12. 验收标准, 13. 协作边界 (+31 more)

### Community 26 - "PlaceSearchViewModel"
Cohesion: 0.05
Nodes (28): CollectionDecision, Confirm, decideCollectionToggle(), PendingCollectionRemoval, RemoveNow, Save, Back, ConfirmRemoval (+20 more)

### Community 27 - "DayItinerarySelectionTest.kt"
Cohesion: 0.07
Nodes (6): DayItinerarySelectionTest, CreateTrip, Flow, Trips, Legs, Trips

### Community 28 - "GeoPoint"
Cohesion: 0.17
Nodes (9): GeoPoint, Flow, DayItinerary, ItineraryItem, ItineraryPlace, Flow, Result, PolylineCodec (+1 more)

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.17
Nodes (15): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+7 more)

### Community 30 - "Easy Trip UI 设计公式"
Cohesion: 0.05
Nodes (43): 10.1 无网, 10.2 路线失败, 10.3 地图加载中或失败, 10.4 部分成功, 10.5 提交中与目标日失效, 10.6 保存失败, 10. 本地优先与状态公式, 11.1 地图服务授权 (+35 more)

### Community 31 - "AmapRouteDataSource.kt"
Cohesion: 0.07
Nodes (27): AmapSmokeTest, AmapServiceException, AmapRouteDataSource, CallbackBoundary, RouteSearch, CallbackBoundary, RouteDataSource, parseRouteResult() (+19 more)

### Community 32 - "MemoryPreferences"
Cohesion: 0.13
Nodes (5): SharedPreferencesMapPreferences, Editor, MapPreferencesTest, MemoryPreferences, SharedPreferences

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.14
Nodes (14): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+6 more)

### Community 34 - "SelectablePill"
Cohesion: 0.19
Nodes (9): SelectablePillTest, Modifier, Role, SelectablePill(), SelectablePillStyle, DaySelector(), LazyListState, Modifier (+1 more)

### Community 35 - "Easy Trip 工作台导航重构设计"
Cohesion: 0.06
Nodes (33): 10. 数据模型, 11. 空态与错误处理, 12. 可访问性, 13. 测试策略, 14. 验收标准, 1. 目标, 2. 非目标, 3. 信息架构 (+25 more)

### Community 37 - "RoomItineraryRepository"
Cohesion: 0.11
Nodes (6): ItineraryTransactionTest, SequenceIds, RoomItineraryRepository, AdjacencyDiff, Edge, AdjacencyPlannerTest

### Community 38 - "AmapComposeMap"
Cohesion: 0.14
Nodes (10): AmapComposeMap(), Bounds, Modifier, View, MapHostCallbackGuard, MapZoomButton(), SinglePoint, ViewportCommand (+2 more)

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
Cohesion: 0.08
Nodes (7): AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, View

### Community 43 - "ItineraryScope"
Cohesion: 0.06
Nodes (29): ItineraryScopeRailTest, WholeTripItineraryContentTest, ItineraryScopeRail(), Modifier, ScopeItem(), RouteLegUi, WholeTripDayUi, Modifier (+21 more)

### Community 44 - "DayItineraryContent"
Cohesion: 0.20
Nodes (17): AddPlace, CommitMove, ConfirmDelete, DayItineraryAction, DayItineraryContent(), DayItinerarySheet(), DismissDialogs, Modifier (+9 more)

### Community 46 - "PlaceDao"
Cohesion: 0.10
Nodes (6): Flow, PlaceDao, PlaceSnapshotRow, PlaceUsageRow, SavedPlaceTagCrossRef, TagEntity

### Community 47 - "EasyTripDatabase"
Cohesion: 0.08
Nodes (6): CascadeCountDao, EasyTripDatabase, com, SchemaItineraryDao, SchemaRouteDao, RoomDatabase

### Community 48 - "EasyTripTokens.kt"
Cohesion: 0.52
Nodes (4): EasyTripElevation, EasyTripSizes, EasyTripSpacing, EasyTripTheme

### Community 49 - "TripWorkspaceContent.kt"
Cohesion: 0.25
Nodes (10): Modifier, SearchSurface(), Composable, Modifier, toSheetValue(), TripWorkspaceContent(), WorkspacePageMessage(), WorkspaceReadyContent() (+2 more)

### Community 50 - "WorkspaceOverlay"
Cohesion: 0.12
Nodes (16): AddTripDay, Confirmation, EditItineraryItem, EditRouteLeg, Feedback, FeedbackUiModel, LayerMenu, None (+8 more)

### Community 51 - "Easy Trip 搜索与收藏交互设计"
Cohesion: 0.07
Nodes (27): 10. 边界与错误处理, 11. 测试策略, 12. 验收标准, 1. 背景与目标, 2. 非目标, 3. 当前实现基线, 4.1 工作台, 4.2 独立搜索页 (+19 more)

### Community 52 - "FakeRepository"
Cohesion: 0.17
Nodes (4): CreateTripViewModelTest, FakeRepository, SavedStateHandle, FakeRepository

### Community 53 - "SavedPlace"
Cohesion: 0.13
Nodes (15): Flow, Flow, Flow, Flow, AlreadySaved, Flow, PlaceTag, Saved (+7 more)

### Community 54 - "FeedbackState"
Cohesion: 0.24
Nodes (11): FeedbackStateTest, EmptyFeedbackState(), ErrorFeedbackState(), FeedbackContent(), FeedbackKind, EMPTY, ERROR, LOADING (+3 more)

### Community 56 - "PlacePoolSheet"
Cohesion: 0.22
Nodes (6): PlaceSearchDataSource, PlacePoolFlowTest, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, PlacePoolSheet()

### Community 57 - "CompactSecondaryButton"
Cohesion: 0.20
Nodes (15): CompactPrimaryButton(), CompactSecondaryButton(), EditTimingDialog(), Modifier, PlaceDetailContent(), PlaceDetailDraft, SavedPlaceRowUi, Modifier (+7 more)

### Community 58 - "Converters"
Cohesion: 0.09
Nodes (6): Converters, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT, UNSUPPORTED_TRANSIT

### Community 59 - "MapFacade.kt"
Cohesion: 0.16
Nodes (14): ItineraryItemUi, mapWholeTripDays(), toItineraryItemUi(), toRouteErrorSummary(), toRouteLegUi(), DayMapSnapshot, MapMarkerUi, MapPolylineUi (+6 more)

### Community 60 - "MapUiModel"
Cohesion: 0.23
Nodes (6): AmapComposeMapTest, Lifecycle, LifecycleOwner, TestOwner, toMapPoiUi(), MapUiModel

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 63 - "TripWorkspaceViewModel"
Cohesion: 0.22
Nodes (3): SearchResultSelection, TripWorkspaceUiState, TripWorkspaceViewModel

### Community 64 - "CreateTripViewModel"
Cohesion: 0.23
Nodes (8): CreateTripViewModel, Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider

### Community 67 - "ItineraryItemEntity"
Cohesion: 0.13
Nodes (5): DayItems, DayItineraryRow, ItineraryDao, Flow, ItineraryItemEntity

### Community 68 - "TestTripRepository"
Cohesion: 0.12
Nodes (6): TripDeleteImpact, CreateTrip, TestImpacts, TestTripRepository, TripListViewModelTest, RuntimeException

### Community 69 - "TripWorkspaceNavigationStateTest"
Cohesion: 0.25
Nodes (3): SavedStateHandle, Trips, TripWorkspaceNavigationStateTest

### Community 72 - "Task 4 报告：重做搜索状态机与连续收藏"
Cohesion: 0.13
Nodes (14): Device gate review fix round 1, Fix round 1, Fix round 2, Fix round 3, Fix round 4, Graphify, Pencil 对照, Task 4 报告：重做搜索状态机与连续收藏 (+6 more)

### Community 73 - "TripWorkspacePageState"
Cohesion: 0.19
Nodes (12): Modifier, WorkspaceMapFallback(), ConsentRequired, Error, Failed, Loading, NotFound, Ready (+4 more)

### Community 75 - "FakeTripRepository"
Cohesion: 0.17
Nodes (6): FakeImpacts, FakeTripRepository, CreateTrip, Role, MoveCall, TripFlowTest

### Community 76 - "CreateTripAction"
Cohesion: 0.13
Nodes (14): Back, CreateTimeMode, DATED, DRAFT, CreateTripAction, CreateTripEffect, DayCountChanged, NameChanged (+6 more)

### Community 79 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 80 - "Easy Trip"
Cohesion: 0.50
Nodes (4): 高德地图本地密钥配置, Easy Trip, 本地优先 Android 旅行规划, 未授权或离线本地编辑

### Community 86 - "AmapPlaceDataSource.kt"
Cohesion: 0.12
Nodes (11): AmapPlaceDataSource, CallbackBoundary, PoiSearch, CallbackBoundary, com, parsePoiSearchResponse(), parsePlaces(), RawPlace (+3 more)

### Community 87 - "TripWorkspaceViewModel.kt"
Cohesion: 0.12
Nodes (17): Factory, CreationExtras, Flow, Job, StateFlow, T, ViewModel, ViewModelProvider (+9 more)

### Community 88 - "MapPoiUi"
Cohesion: 0.16
Nodes (5): Itineraries, AmapMapHost, PoiHost, WorkspaceFlowTest, MapPoiUi

### Community 89 - "TripService"
Cohesion: 0.15
Nodes (3): com, CreateTrip, TripService

### Community 90 - "reduceMapInteraction"
Cohesion: 0.27
Nodes (6): FocusSearchResult, MapInteractionAction, MapInteractionState, ReconcileSearchResults, reduceMapInteraction(), MapInteractionReducerTest

### Community 91 - "PlacePoolScrollbarTest.kt"
Cohesion: 0.46
Nodes (3): LazyListState, Modifier, PlacePoolScrollbarTest

### Community 93 - "TripWorkspaceAction"
Cohesion: 0.17
Nodes (12): Back, CloseOverlay, OpenOverlay, OpenPrivacySettings, OpenSearch, OpenSettings, Retry, SelectItineraryScope (+4 more)

### Community 94 - "Task 5 报告：地点池、详情与地图控件"
Cohesion: 0.17
Nodes (11): Fix round 1, Fix round 2：Room 测试调度竞态, Graphify, Pencil 对照, Task 5 报告：地点池、详情与地图控件, TDD 证据, 修改文件, 关注点 (+3 more)

### Community 95 - "FakeTrips"
Cohesion: 0.14
Nodes (3): FakeTrips, CreateTrip, java

### Community 96 - "AmapConsentToken"
Cohesion: 0.06
Nodes (16): AmapConsentToken, AmapPrivacyGate, AmapPrivacyStateMachine, ConsentRegistry, ConsentSnapshot, StateFlow, TestConsentGate, AppContainer (+8 more)

### Community 98 - "TripRepository"
Cohesion: 0.10
Nodes (5): CreateTrip, Flow, RecordingTripRepository, Flow, TripRepository

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

### Community 104 - "TripWorkspaceScreen"
Cohesion: 0.24
Nodes (9): ConfirmationDialogTest, ConfirmationUiModel, DayItineraryUiState, PlacePoolUiState, confirmation(), stableWorkspaceOverlayId(), TripWorkspaceRoute(), Composable (+1 more)

### Community 105 - "Global Constraints"
Cohesion: 0.20
Nodes (9): Easy Trip Search Collection Implementation Plan, Global Constraints, Task 1: 收敛工作台 Tab 并建立独立搜索路由, Task 2: 完成搜索页状态清理与一次性返回聚焦, Task 3: 统一搜索结果与地点卡片的收藏切换策略, Task 4: 建立稳定的收藏 marker 身份、编号与聚焦样式, Task 5: 接入高德底图 POI 点击与统一地点卡片, Task 6: 为地点池增加只读纵向 scrollbar thumb (+1 more)

### Community 106 - "Easy Trip v1.0 Pencil Reference"
Cohesion: 0.18
Nodes (10): Easy Trip v1.0 Pencil Reference, Layout Verification, Main Flow Frames, Public Components, Responsive Notes, Screenshot Baseline, Semantic Tokens, Shape, Spacing, Size, Elevation (+2 more)

### Community 107 - "Batch 1 visual fix report"
Cohesion: 0.20
Nodes (9): Batch 1 visual fix report, Commit, Fix round 1, Fix round 1 commit, Fix round 1 最终验证, Frame 差异与修复, RED / GREEN, 修改文件 (+1 more)

### Community 108 - "TravelMode"
Cohesion: 0.19
Nodes (5): TravelMode, FLEXIBLE, SELF_DRIVE, TransportModeRecommender, TransportModeRecommenderTest

### Community 110 - "Dp"
Cohesion: 0.13
Nodes (13): EasyTripIconButton(), Modifier, EmptyState(), Modifier, ItineraryItemCard(), ItineraryItemRow(), Color, Modifier (+5 more)

### Community 111 - "CreateTripContent"
Cohesion: 0.39
Nodes (4): CreateTripContentTest, CreateTripContent(), Modifier, StepNumber()

### Community 112 - "RoutePlanOutcome"
Cohesion: 0.25
Nodes (5): Failure, plan(), RoutePlanner, RoutePlanOutcome, Success

### Community 114 - "PlacePoolContent"
Cohesion: 0.13
Nodes (16): EditSavedPlaceDialog(), ConfirmCollectionRemoval, ConfirmDelete, Delete, DismissDialogs, Edit, Modifier, PlacePoolAction (+8 more)

### Community 119 - "MapLayer"
Cohesion: 0.11
Nodes (11): AmapMapHost, TestMapHost, V1PencilFlowTest, MapLayer, SATELLITE_ROAD, STANDARD, InMemoryMapPreferences, StateFlow (+3 more)

### Community 120 - "行程项"
Cohesion: 0.67
Nodes (3): 行程项, 路线段, 收藏地点

### Community 121 - "RoomRouteLegRepository"
Cohesion: 0.10
Nodes (4): PlaceSearchDataSource, V2AcceptanceTest, PlaceSearchDataSource, RoomRouteLegRepository

### Community 124 - "Factory"
Cohesion: 0.50
Nodes (3): Factory, T, ViewModelProvider

### Community 125 - "RoomDeleteImpactProviderTest.kt"
Cohesion: 0.18
Nodes (5): TimeMode, DATED, DRAFT, Flow, TripEntityWithDays

### Community 137 - "MapControls"
Cohesion: 0.47
Nodes (4): description(), Modifier, MapControls(), LayerIcon()

### Community 138 - "SearchMapFocusTest"
Cohesion: 0.50
Nodes (4): Lifecycle, LifecycleOwner, SearchMapFocusTest, TestOwner

### Community 140 - "MapMarkerKind"
Cohesion: 0.50
Nodes (4): MapMarkerKind, SAVED_ITINERARY, SAVED_PLACE_POOL, UNSAVED_SEARCH

## Knowledge Gaps
- **388 isolated node(s):** `guard-adb-install.sh script`, `READY`, `STARTING`, `STARTED`, `CANCELLED` (+383 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **39 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `MapUiModelMapperTest`, `FakeRepository`, `RoomRouteLegRepositoryTest`, `PlaceSearchReducer`, `FakeItineraries`, `SearchMapFocusTest`, `MapViewportControllerTest`, `TransportMode`, `CreateTrip`, `PlacePoolViewModel`, `PlaceCandidate`, `PlaceSearchViewModel`, `DayItinerarySelectionTest.kt`, `AmapRouteDataSource.kt`, `AmapComposeMap`, `SavedPlace`, `PlacePoolSheet`, `CompactSecondaryButton`, `MapFacade.kt`, `MapUiModel`, `TripWorkspaceViewModel`, `TripWorkspaceNavigationStateTest`, `Places`, `AmapPlaceDataSource.kt`, `TripWorkspaceViewModel.kt`, `MapPoiUi`, `reduceMapInteraction`, `PlacePoolScrollbarTest.kt`, `Itineraries`, `RecordingHost`, `EditingPlaces`, `RoomRouteLegRepository`?**
  _High betweenness centrality (0.116) - this node is a cross-community bridge._
- **Why does `TravelMode` connect `TravelMode` to `FakeTripRepository`, `TripSettingsViewModel`, `Trips`, `TripDay`, `SavedPlaceEntity`, `CreateTrip`, `TripListContent.kt`, `TripDao`, `PlaceCandidate`, `DayItinerarySelectionTest.kt`, `GeoPoint`, `RoomItineraryRepository`, `Legs`, `Trips`, `FakeRepository`, `SavedPlace`, `CompactSecondaryButton`, `Converters`, `CreateTripViewModel`, `ItineraryItemEntity`, `TestTripRepository`, `Trips`, `FakeTripRepository`, `CreateTripAction`, `FakeTrips`, `Trips`, `TripRepository`, `CreateTripUiState`, `Trips`, `CreateTripContent`, `RoomDeleteImpactProviderTest.kt`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **Why does `TripRepository` connect `TripRepository` to `FakeTripRepository`, `TripSettingsViewModel`, `Trips`, `TripListViewModel`, `TripDay`, `CreateTrip`, `DayItinerarySelectionTest.kt`, `Legs`, `AppNavigation.kt`, `Trips`, `FakeRepository`, `SavedPlace`, `ItineraryRepository`, `TestTripRepository`, `Trips`, `FakeTripRepository`, `TripWorkspaceViewModel.kt`, `FakeTrips`, `Trips`, `TravelMode`, `Trips`?**
  _High betweenness centrality (0.053) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 25 inferred relationships involving `TripWorkspaceViewModel` (e.g. with `.model()` and `.emptyScopeClearsRequestSoReplacementHostDoesNotReplayInitialViewport()`) actually correct?**
  _`TripWorkspaceViewModel` has 25 INFERRED edges - model-reasoned connections that need verification._
- **What connects `guard-adb-install.sh script`, `READY`, `STARTING` to the rest of the system?**
  _388 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Easy Trip v1.0 全量 UI 与交互落地设计` be split into smaller, more focused modules?**
  _Cohesion score 0.0425531914893617 - nodes in this community are weakly interconnected._