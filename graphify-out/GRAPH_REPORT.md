# Graph Report - easy-trip-v1-full-ui-run  (2026-08-24)

## Corpus Check
- 224 files · ~109,047 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2957 nodes · 6222 edges · 159 communities (116 shown, 43 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 415 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `cc84f413`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Easy Trip v1.0 全量 UI 与交互落地设计
- MapUiModelMapperTest
- FakeRepository
- FakeTripRepository
- awaitSdkCallback
- TripSettingsViewModel
- Visual Fix Agent Stall 诊断
- Legs
- Trips
- PlaceSearchReducer
- FakeItineraries
- AmapComposeMap.kt
- DayItineraryViewModel
- TripListViewModel
- ItineraryScope
- TripRepository
- RouteStatus
- ItineraryItemEntity
- CreateTrip
- OneShotCallback
- TripListContent.kt
- Easy Trip 一期设计
- TripDao
- PlacePoolViewModel
- PlaceCandidate
- Easy Trip v1.0 Pencil 设计落地方案
- PlaceSearchViewModel
- Legs
- RouteLegEntity
- Easy Trip v2 交互与视觉升级设计
- Easy Trip UI 设计公式
- AmapServiceException
- MemoryPreferences
- 高德 Android SDK 集成决策
- SelectablePill
- Easy Trip 工作台导航重构设计
- SchemaTest
- ItineraryTransactionTest
- AmapComposeMap
- 2026-08-23-easy-trip-v1-full-ui-implementation.md
- Legs
- AppNavigation.kt
- View
- WholeTripItineraryContentTest
- DayItineraryContent
- Batch 2 Gate Report
- PlaceDao
- EasyTripDatabase
- EasyTripTokens.kt
- TripWorkspaceScreen
- WorkspaceOverlay
- Easy Trip 搜索与收藏交互设计
- FakeRepository
- RouteLegRepository
- FeedbackState
- Legs
- Batch 2 Room Timeout Diagnosis
- CompactSecondaryButton
- Converters
- TripDay
- AmapComposeMapTest.kt
- RouteFormattingTest
- FakeSavedPlaces
- TripWorkspaceViewModel
- CreateTripViewModel
- WholeTripItineraryContent
- MapLifecycleController
- ItineraryDao
- TripSummary
- TripWorkspaceNavigationStateTest
- ItineraryService
- MapLayerRenderingPolicyTest
- Task 4 报告：重做搜索状态机与连续收藏
- .setContent
- MapFacade.kt
- FakeTripRepository
- CreateTripAction
- FakeLegs
- DelayedDeletePlaces
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest
- parsePoiSearchResponse
- PlaceSearchContentTest.kt
- MapPoiUi
- TripService
- reduceMapInteraction
- SavedPlace
- CLAUDE.md
- TripWorkspaceAction
- Task 5 报告：地点池、详情与地图控件
- FakeTrips
- Legs
- Trips
- RoomTripRepository
- LazyScrollbar.kt
- settledWorkspaceSheetLevel
- CreateTripUiState
- File Structure
- File Structure
- MapViewportControllerTest
- Global Constraints
- Easy Trip v1.0 Pencil Reference
- Batch 1 visual fix report
- TravelMode
- AmapRouteDataSource.kt
- Dp
- CreateTripContent
- AmapSmokeTest
- .searchCollectionMapAndRestorationFlow
- PlacePoolContent
- RoomItineraryRepository
- MapLayer
- ItineraryRepository
- EditingPlaces
- MapUiModel
- 行程项
- RoomRouteLegRepository
- TransportMode
- Trips
- DayItineraryViewModel.kt
- RoomTripRepositoryTest.kt
- Batch 2 地点池视觉修复报告
- AmapMapHost
- Legs
- AmapMapHost
- SavedPlaceRepository
- guard-adb-install.sh
- GeoPoint
- AmapConsentToken
- Batch 2 AMap/EGL 环境诊断
- SDD ledger — plan: docs/superpowers/plans/2026-08-23-easy-trip-v1-full-ui-implementation.md
- AmapRouteDataSource
- Trips
- SavedPlaceEntity
- TestMapHost
- RoutePlannerTest
- CascadeCountDao
- Places
- RoomSavedPlaceRepositoryTest
- PlaceSearchAction
- Places
- RoomRouteLegRepositoryTest
- AmapMapHost
- MapControls
- BridgeState
- MapLayerFlowTest
- SearchSurface.kt
- Itineraries
- MutableItineraries
- RouteLegRow.kt
- MapLegend
- MainActivity
- WorkspaceSection
- .createTrip

## God Nodes (most connected - your core abstractions)
1. `GeoPoint` - 131 edges
2. `PlaceCandidate` - 90 edges
3. `SavedPlace` - 67 edges
4. `TravelMode` - 63 edges
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

## Communities (159 total, 43 thin omitted)

### Community 0 - "Easy Trip v1.0 全量 UI 与交互落地设计"
Cohesion: 0.04
Nodes (46): 10.1 批次 1：旅行入口与创建, 10.2 批次 2：搜索、连续收藏与地点池, 10.3 批次 3：从地点池加入行程与旅行日, 10.4 批次 4：单日编辑、交通路段与全程, 10.5 批次 5：设置、日期、权限与完整回归, 10. 五个实施批次, 11.1 本地优先, 11.2 收藏 (+38 more)

### Community 2 - "FakeRepository"
Cohesion: 0.09
Nodes (22): connected(), hasValidatedInternet(), StateFlow, NetworkMonitor, onAvailable(), onCapabilitiesChanged(), onLost(), Active (+14 more)

### Community 3 - "FakeTripRepository"
Cohesion: 0.21
Nodes (4): FakeTripRepository, CreateTrip, Flow, TripServiceTest

### Community 4 - "awaitSdkCallback"
Cohesion: 0.13
Nodes (10): awaitSdkCallback(), CallbackBoundary, cleanupBoundary(), T, FakeBoundary, CallbackBoundary, Result, T (+2 more)

### Community 5 - "TripSettingsViewModel"
Cohesion: 0.10
Nodes (11): DayDeleteImpact, DayUi, Factory, androidx, StateFlow, T, ViewModel, ViewModelProvider (+3 more)

### Community 6 - "Visual Fix Agent Stall 诊断"
Cohesion: 0.06
Nodes (30): 1. Executive summary, 1. Harness 在 tool-result 边界自动结束，最终答复回合缺失, 2. 两次 run timeline, 2. 超大单任务上下文与高频工具链导致空终局/自动收尾概率升高, 3. 停止信号检查, 3. 存在未记录的内部运行时/turn/tool 预算, 4. adb/monkey 失败直接导致第一次停止, 4. 行为模式与次数 (+22 more)

### Community 8 - "Trips"
Cohesion: 0.06
Nodes (12): FailingMapPreferences, Itineraries, com, CreateTrip, Flow, StateFlow, Trips, Legs (+4 more)

### Community 9 - "PlaceSearchReducer"
Cohesion: 0.08
Nodes (31): PlaceSearchContentTest, BackIcon(), BookmarkIcon(), CloseIcon(), Color, Modifier, LocationIcon(), PlaceSearchContent() (+23 more)

### Community 10 - "FakeItineraries"
Cohesion: 0.17
Nodes (7): Add, FakeCoordinator, FakeItineraries, ItineraryEditingTest, kotlinx, Move, Timing

### Community 11 - "AmapComposeMap.kt"
Cohesion: 0.08
Nodes (12): AmapMapHost, Bounds, android, Modifier, View, MapZoomButton(), RealAmapMapHost, SinglePoint (+4 more)

### Community 13 - "TripListViewModel"
Cohesion: 0.15
Nodes (11): Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider, OpenSettings (+3 more)

### Community 14 - "ItineraryScope"
Cohesion: 0.15
Nodes (10): Day, decodeItineraryScope(), encodeItineraryScope(), ItineraryScope, reconcileItineraryScope(), RestoredWorkspaceNavigation, restoreWorkspaceNavigation(), toMapScope() (+2 more)

### Community 15 - "TripRepository"
Cohesion: 0.07
Nodes (8): CreateTrip, Flow, RecordingTripRepository, InsertSide, AFTER, BEFORE, Flow, TripRepository

### Community 16 - "RouteStatus"
Cohesion: 0.11
Nodes (9): RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK, Flow, RouteLegDao (+1 more)

### Community 17 - "ItineraryItemEntity"
Cohesion: 0.14
Nodes (5): OfflineRecoveryTest, RoomDeleteImpactProviderTest, ItineraryItemEntity, TripDayEntity, TripEntity

### Community 18 - "CreateTrip"
Cohesion: 0.19
Nodes (3): T, RoomTripRepositoryTest, CreateTrip

### Community 20 - "TripListContent.kt"
Cohesion: 0.05
Nodes (48): ConfirmationDialogTest, EasyTripButtonTest, TripListContentTest, ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton() (+40 more)

### Community 21 - "Easy Trip 一期设计"
Cohesion: 0.17
Nodes (12): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 离线路线等待与联网续算, 同一地点可重复安排行程 (+4 more)

### Community 22 - "TripDao"
Cohesion: 0.11
Nodes (6): TimeMode, DATED, DRAFT, Flow, TripDao, TripEntityWithDays

### Community 23 - "PlacePoolViewModel"
Cohesion: 0.05
Nodes (15): Factory, Job, kotlinx, PlaceSearchDataSource, StateFlow, T, ViewModel, ViewModelProvider (+7 more)

### Community 24 - "PlaceCandidate"
Cohesion: 0.10
Nodes (12): AppIconResourceTest, V1AcceptanceTest, AmapPrivacyGate, PlaceCandidate, PlaceSearchDataSource, Flow, RoomSavedPlaceRepository, AlreadySaved (+4 more)

### Community 25 - "Easy Trip v1.0 Pencil 设计落地方案"
Cohesion: 0.05
Nodes (39): 10.1 JVM, 10.2 Compose instrumentation, 10.3 数据与导航集成, 10.4 验证命令, 10. 测试策略, 11. 视觉验收, 12. 验收标准, 13. 协作边界 (+31 more)

### Community 26 - "PlaceSearchViewModel"
Cohesion: 0.22
Nodes (6): Factory, StateFlow, T, ViewModel, ViewModelProvider, PlaceSearchViewModel

### Community 27 - "Legs"
Cohesion: 0.06
Nodes (7): DayItinerarySelectionTest, Itineraries, CreateTrip, Flow, Trips, Legs, Trips

### Community 28 - "RouteLegEntity"
Cohesion: 0.22
Nodes (5): DayItinerary, ItineraryItem, ItineraryPlace, Flow, RouteLegEntity

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.17
Nodes (15): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+7 more)

### Community 30 - "Easy Trip UI 设计公式"
Cohesion: 0.05
Nodes (43): 10.1 无网, 10.2 路线失败, 10.3 地图加载中或失败, 10.4 部分成功, 10.5 提交中与目标日失效, 10.6 保存失败, 10. 本地优先与状态公式, 11.1 地图服务授权 (+35 more)

### Community 31 - "AmapServiceException"
Cohesion: 0.17
Nodes (12): AmapServiceException, parseRouteResult(), RoutePathData, selectUsablePath(), validateRouteRequest(), RouteMode, DRIVE, TRANSIT (+4 more)

### Community 32 - "MemoryPreferences"
Cohesion: 0.11
Nodes (8): InMemoryMapPreferences, StateFlow, MapPreferences, SharedPreferencesMapPreferences, Editor, MapPreferencesTest, MemoryPreferences, SharedPreferences

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.14
Nodes (14): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+6 more)

### Community 34 - "SelectablePill"
Cohesion: 0.19
Nodes (9): SelectablePillTest, Modifier, Role, SelectablePill(), SelectablePillStyle, DaySelector(), LazyListState, Modifier (+1 more)

### Community 35 - "Easy Trip 工作台导航重构设计"
Cohesion: 0.06
Nodes (33): 10. 数据模型, 11. 空态与错误处理, 12. 可访问性, 13. 测试策略, 14. 验收标准, 1. 目标, 2. 非目标, 3. 信息架构 (+25 more)

### Community 38 - "AmapComposeMap"
Cohesion: 0.22
Nodes (3): AmapComposeMap(), MapHostCallbackGuard, MapHostCallbackGuardTest

### Community 39 - "2026-08-23-easy-trip-v1-full-ui-implementation.md"
Cohesion: 0.07
Nodes (27): Batch 1 Gate, Batch 1：旅行入口、创建与删除, Batch 2 Gate, Batch 2：搜索、连续收藏与地点池, Batch 3 Gate, Batch 3：从地点池加入行程与旅行日, Batch 4 Gate, Batch 4：单日编辑、RouteLeg、全程与抽屉 (+19 more)

### Community 40 - "Legs"
Cohesion: 0.05
Nodes (7): Itineraries, com, CreateTrip, Legs, Places, Trips, WorkspaceSearchTabsTest

### Community 41 - "AppNavigation.kt"
Cohesion: 0.16
Nodes (14): AppNavigation(), AppNavigationDependencies, AppNavigationObserver, consumeWorkspaceSearchReturn(), android, androidx, com, publishWorkspaceSearchReturn() (+6 more)

### Community 42 - "View"
Cohesion: 0.09
Nodes (6): AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, View

### Community 44 - "DayItineraryContent"
Cohesion: 0.20
Nodes (17): AddPlace, CommitMove, ConfirmDelete, DayItineraryAction, DayItineraryContent(), DayItinerarySheet(), DismissDialogs, Modifier (+9 more)

### Community 45 - "Batch 2 Gate Report"
Cohesion: 0.07
Nodes (28): 1. Graphify 定向查询, 2. 全套 JVM、lint、assemble, 3. 设备检查, 4. Task 4/5 目标 Compose 与相关 Room 测试, adb/uiautomator 断言, API 36 恢复环境生产旅程 B（2026-08-24）, Batch 2 Gate Report, Concerns (+20 more)

### Community 46 - "PlaceDao"
Cohesion: 0.08
Nodes (7): SchemaPlaceDao, Flow, PlaceDao, PlaceSnapshotRow, PlaceUsageRow, SavedPlaceTagCrossRef, TagEntity

### Community 47 - "EasyTripDatabase"
Cohesion: 0.10
Nodes (6): CascadeDeleteTest, EasyTripDatabase, com, SchemaItineraryDao, SchemaRouteDao, RoomDatabase

### Community 48 - "EasyTripTokens.kt"
Cohesion: 0.52
Nodes (4): EasyTripElevation, EasyTripSizes, EasyTripSpacing, EasyTripTheme

### Community 49 - "TripWorkspaceScreen"
Cohesion: 0.13
Nodes (27): ConfirmationUiModel, DayItineraryUiState, PlacePoolUiState, Composable, Modifier, toSheetValue(), TripWorkspaceContent(), WorkspacePageMessage() (+19 more)

### Community 50 - "WorkspaceOverlay"
Cohesion: 0.12
Nodes (16): AddTripDay, Confirmation, EditItineraryItem, EditRouteLeg, Feedback, FeedbackUiModel, LayerMenu, None (+8 more)

### Community 51 - "Easy Trip 搜索与收藏交互设计"
Cohesion: 0.07
Nodes (27): 10. 边界与错误处理, 11. 测试策略, 12. 验收标准, 1. 背景与目标, 2. 非目标, 3. 当前实现基线, 4.1 工作台, 4.2 独立搜索页 (+19 more)

### Community 52 - "FakeRepository"
Cohesion: 0.16
Nodes (5): CreateTripViewModelTest, FakeRepository, Flow, SavedStateHandle, FakeRepository

### Community 53 - "RouteLegRepository"
Cohesion: 0.06
Nodes (20): Flow, Flow, Lifecycle, LifecycleOwner, TestOwner, Flow, Flow, Flow (+12 more)

### Community 54 - "FeedbackState"
Cohesion: 0.24
Nodes (11): FeedbackStateTest, EmptyFeedbackState(), ErrorFeedbackState(), FeedbackContent(), FeedbackKind, EMPTY, ERROR, LOADING (+3 more)

### Community 56 - "Batch 2 Room Timeout Diagnosis"
Cohesion: 0.11
Nodes (18): Batch 2 Room Timeout Diagnosis, GREEN 修复与验证, Phase 1：失败与复现, Phase 2：模式对比, Phase 3：单一假设验证, 仓库内 working Room Flow 模式, 假设, 判定 (+10 more)

### Community 57 - "CompactSecondaryButton"
Cohesion: 0.24
Nodes (13): CompactPrimaryButton(), CompactSecondaryButton(), EditTimingDialog(), Modifier, PlaceDetailContent(), Modifier, SavedPlaceRow(), Modifier (+5 more)

### Community 58 - "Converters"
Cohesion: 0.10
Nodes (6): Converters, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT, UNSUPPORTED_TRANSIT

### Community 59 - "TripDay"
Cohesion: 0.30
Nodes (10): ItineraryItemUi, mapWholeTripDays(), RouteLegUi, toItineraryItemUi(), toRouteErrorSummary(), toRouteLegUi(), WholeTripDayUi, TripDay (+2 more)

### Community 60 - "AmapComposeMapTest.kt"
Cohesion: 0.23
Nodes (8): AmapComposeMapTest, Lifecycle, LifecycleOwner, TestOwner, MapMarkerKind, SAVED_ITINERARY, SAVED_PLACE_POOL, UNSAVED_SEARCH

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 62 - "FakeSavedPlaces"
Cohesion: 0.13
Nodes (7): FakeSavedPlaces, IgnoringCancellationSearchSource, ImmediateSearchSource, Flow, PlaceSearchDataSource, PlaceSearchViewModelTest, RecordingSearchSource

### Community 63 - "TripWorkspaceViewModel"
Cohesion: 0.09
Nodes (20): Factory, CreationExtras, Flow, Job, StateFlow, T, ViewModel, ViewModelProvider (+12 more)

### Community 64 - "CreateTripViewModel"
Cohesion: 0.23
Nodes (8): CreateTripViewModel, Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider

### Community 65 - "WholeTripItineraryContent"
Cohesion: 0.14
Nodes (11): ItineraryScopeRailTest, ItineraryScopeRail(), Modifier, ScopeItem(), dayHeading(), Modifier, WholeTripItineraryContent(), Modifier (+3 more)

### Community 67 - "ItineraryDao"
Cohesion: 0.15
Nodes (4): DayItems, DayItineraryRow, ItineraryDao, Flow

### Community 68 - "TripSummary"
Cohesion: 0.11
Nodes (8): TripSummary, TripDeleteImpact, TripListUiModelsTest, CreateTrip, Flow, TestImpacts, TestTripRepository, TripListViewModelTest

### Community 69 - "TripWorkspaceNavigationStateTest"
Cohesion: 0.25
Nodes (3): SavedStateHandle, Trips, TripWorkspaceNavigationStateTest

### Community 72 - "Task 4 报告：重做搜索状态机与连续收藏"
Cohesion: 0.13
Nodes (14): Device gate review fix round 1, Fix round 1, Fix round 2, Fix round 3, Fix round 4, Graphify, Pencil 对照, Task 4 报告：重做搜索状态机与连续收藏 (+6 more)

### Community 73 - ".setContent"
Cohesion: 0.37
Nodes (3): com, TripWorkspaceContentTest, SavedPlaceRowUi

### Community 74 - "MapFacade.kt"
Cohesion: 0.19
Nodes (8): CorruptRoute, formatOccurrenceBadge(), MapPolylineUi, MapRouteLabelUi, MapUiModelMapper, OccurrenceUi, routePalette(), OccurrenceBadgeFormatterTest

### Community 75 - "FakeTripRepository"
Cohesion: 0.14
Nodes (7): FakeImpacts, FakeTripRepository, CreateTrip, Flow, Role, MoveCall, TripFlowTest

### Community 76 - "CreateTripAction"
Cohesion: 0.13
Nodes (14): Back, CreateTimeMode, DATED, DRAFT, CreateTripAction, CreateTripEffect, DayCountChanged, NameChanged (+6 more)

### Community 79 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 80 - "Easy Trip"
Cohesion: 0.50
Nodes (4): 高德地图本地密钥配置, Easy Trip, 本地优先 Android 旅行规划, 未授权或离线本地编辑

### Community 86 - "parsePoiSearchResponse"
Cohesion: 0.15
Nodes (8): AmapPlaceDataSource, CallbackBoundary, PoiSearch, CallbackBoundary, com, parsePoiSearchResponse(), AmapPlaceDataSourceTest, PoiResult

### Community 87 - "PlaceSearchContentTest.kt"
Cohesion: 0.14
Nodes (5): com, Flow, com, com, TestSavedPlaces

### Community 88 - "MapPoiUi"
Cohesion: 0.12
Nodes (6): Itineraries, AmapMapHost, Places, PoiHost, WorkspaceFlowTest, MapPoiUi

### Community 89 - "TripService"
Cohesion: 0.17
Nodes (3): com, CreateTrip, TripService

### Community 90 - "reduceMapInteraction"
Cohesion: 0.27
Nodes (6): FocusSearchResult, MapInteractionAction, MapInteractionState, ReconcileSearchResults, reduceMapInteraction(), MapInteractionReducerTest

### Community 91 - "SavedPlace"
Cohesion: 0.13
Nodes (12): LazyListState, Modifier, PlacePoolScrollbarTest, SavedPlace, CollectionDecision, Confirm, decideCollectionToggle(), PendingCollectionRemoval (+4 more)

### Community 93 - "TripWorkspaceAction"
Cohesion: 0.17
Nodes (12): Back, CloseOverlay, OpenOverlay, OpenPrivacySettings, OpenSearch, OpenSettings, Retry, SelectItineraryScope (+4 more)

### Community 94 - "Task 5 报告：地点池、详情与地图控件"
Cohesion: 0.17
Nodes (11): Fix round 1, Fix round 2：Room 测试调度竞态, Graphify, Pencil 对照, Task 5 报告：地点池、详情与地图控件, TDD 证据, 修改文件, 关注点 (+3 more)

### Community 95 - "FakeTrips"
Cohesion: 0.15
Nodes (3): FakeTrips, CreateTrip, java

### Community 98 - "RoomTripRepository"
Cohesion: 0.14
Nodes (4): V1PencilFlowTest, CreateTrip, Flow, RoomTripRepository

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

### Community 104 - "MapViewportControllerTest"
Cohesion: 0.08
Nodes (14): MapScope, PLACE_POOL, SINGLE_DAY, WHOLE_TRIP, MapViewportRequest, ViewportReason, INITIAL, PLACE_SET_CHANGED (+6 more)

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
Cohesion: 0.15
Nodes (5): TravelMode, FLEXIBLE, SELF_DRIVE, TransportModeRecommender, TransportModeRecommenderTest

### Community 109 - "AmapRouteDataSource.kt"
Cohesion: 0.33
Nodes (6): RouteSearch, RouteCallback, BusRouteResult, DriveRouteResult, RideRouteResult, WalkRouteResult

### Community 110 - "Dp"
Cohesion: 0.13
Nodes (13): EasyTripIconButton(), Modifier, EmptyState(), Modifier, ItineraryItemCard(), ItineraryItemRow(), Color, Modifier (+5 more)

### Community 111 - "CreateTripContent"
Cohesion: 0.39
Nodes (4): CreateTripContentTest, CreateTripContent(), Modifier, StepNumber()

### Community 113 - ".searchCollectionMapAndRestorationFlow"
Cohesion: 0.15
Nodes (5): AmapMapHost, PlaceSearchDataSource, RecordingHost, V2AcceptanceTest, PlaceSearchDataSource

### Community 114 - "PlacePoolContent"
Cohesion: 0.16
Nodes (15): PlacePoolFlowTest, ConfirmCollectionRemoval, ConfirmDelete, Delete, DismissDialogs, Edit, Modifier, PlacePoolAction (+7 more)

### Community 116 - "MapLayer"
Cohesion: 0.17
Nodes (5): AmapMapHost, RecordingHost, MapLayer, SATELLITE_ROAD, STANDARD

### Community 119 - "MapUiModel"
Cohesion: 0.13
Nodes (4): AmapMapHost, AmapMapHost, MapUiModel, mapViewportPoints()

### Community 120 - "行程项"
Cohesion: 0.67
Nodes (3): 行程项, 路线段, 收藏地点

### Community 122 - "TransportMode"
Cohesion: 0.17
Nodes (5): TransportMode, DRIVE, TAXI, TRANSIT, WALK

### Community 124 - "DayItineraryViewModel.kt"
Cohesion: 0.29
Nodes (5): Factory, StateFlow, T, ViewModel, ViewModelProvider

### Community 125 - "RoomTripRepositoryTest.kt"
Cohesion: 0.20
Nodes (3): IdFactory, MutableClock, Clock

### Community 126 - "Batch 2 地点池视觉修复报告"
Cohesion: 0.17
Nodes (11): Batch 2 地点池视觉修复报告, Concerns, Diff 审计, Findings 修复, Fix round 1（2026-08-24）, GREEN, RED / GREEN, RED 记录 (+3 more)

### Community 128 - "Legs"
Cohesion: 0.05
Nodes (8): Itineraries, com, CreateTrip, Legs, Places, SavedPlaces, SearchMapFocusTest, Trips

### Community 132 - "GeoPoint"
Cohesion: 0.10
Nodes (11): PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, GeoPoint, parsePlaces(), RawPlace, Result (+3 more)

### Community 133 - "AmapConsentToken"
Cohesion: 0.06
Nodes (15): AmapConsentToken, AmapPrivacyStateMachine, ConsentRegistry, ConsentSnapshot, StateFlow, TestConsentGate, AppContainer, CoroutineScope (+7 more)

### Community 134 - "Batch 2 AMap/EGL 环境诊断"
Cohesion: 0.18
Nodes (10): Batch 2 AMap/EGL 环境诊断, 原始失败证据, 后续旅程判断, 恢复验证证据, 最小恢复操作, 根因结论, 状态, 环境对比 (+2 more)

### Community 136 - "AmapRouteDataSource"
Cohesion: 0.24
Nodes (4): AmapRouteDataSource, CallbackBoundary, CallbackBoundary, RouteDataSource

### Community 138 - "SavedPlaceEntity"
Cohesion: 0.20
Nodes (7): defaultRecommendMode(), haversineMeters(), Flow, AdjacencyDiff, Edge, SavedPlaceEntity, AdjacencyPlannerTest

### Community 140 - "RoutePlannerTest"
Cohesion: 0.47
Nodes (3): RouteDataSource, RoutePlannerTest, RouteDataSource

### Community 144 - "PlaceSearchAction"
Cohesion: 0.25
Nodes (8): Back, ConfirmRemoval, DismissRemovalConfirmation, PlaceSearchAction, QueryChanged, Retry, Submit, ToggleCollection

### Community 148 - "MapControls"
Cohesion: 0.47
Nodes (4): description(), Modifier, MapControls(), LayerIcon()

### Community 149 - "BridgeState"
Cohesion: 0.33
Nodes (6): BridgeState, CANCELLED, COMPLETED, READY, STARTED, STARTING

### Community 150 - "MapLayerFlowTest"
Cohesion: 0.38
Nodes (3): FakeMapPreferences, Role, MapLayerFlowTest

### Community 154 - "RouteLegRow.kt"
Cohesion: 0.70
Nodes (4): Modifier, modeLabel(), RouteLegContent(), RouteLegRow()

### Community 157 - "WorkspaceSection"
Cohesion: 0.50
Nodes (3): WorkspaceSection, ITINERARY, PLACE_POOL

## Knowledge Gaps
- **464 isolated node(s):** `guard-adb-install.sh script`, `READY`, `STARTING`, `STARTED`, `CANCELLED` (+459 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **43 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `Legs`, `MapUiModelMapperTest`, `SavedPlaceRepository`, `FakeRepository`, `AmapRouteDataSource`, `PlaceSearchReducer`, `FakeItineraries`, `SavedPlaceEntity`, `AmapComposeMap.kt`, `RoutePlannerTest`, `RoomSavedPlaceRepositoryTest`, `RoomRouteLegRepositoryTest`, `AmapMapHost`, `PlacePoolViewModel`, `PlaceCandidate`, `RouteLegEntity`, `AmapServiceException`, `RouteLegRepository`, `TripDay`, `AmapComposeMapTest.kt`, `FakeSavedPlaces`, `TripWorkspaceViewModel`, `TripWorkspaceNavigationStateTest`, `.setContent`, `MapFacade.kt`, `DelayedDeletePlaces`, `parsePoiSearchResponse`, `PlaceSearchContentTest.kt`, `MapPoiUi`, `reduceMapInteraction`, `SavedPlace`, `MapViewportControllerTest`, `AmapRouteDataSource.kt`, `AmapSmokeTest`, `.searchCollectionMapAndRestorationFlow`, `PlacePoolContent`, `EditingPlaces`, `MapUiModel`, `RoomRouteLegRepository`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._
- **Why does `TravelMode` connect `TravelMode` to `Legs`, `FakeTripRepository`, `TripSettingsViewModel`, `SavedPlaceEntity`, `TripRepository`, `ItineraryItemEntity`, `TripListContent.kt`, `TripDao`, `Legs`, `RouteLegEntity`, `ItineraryTransactionTest`, `Legs`, `FakeRepository`, `RouteLegRepository`, `CompactSecondaryButton`, `Converters`, `CreateTripViewModel`, `ItineraryDao`, `TripSummary`, `FakeTripRepository`, `CreateTripAction`, `FakeTrips`, `Trips`, `RoomTripRepository`, `CreateTripUiState`, `CreateTripContent`, `RoomItineraryRepository`, `Trips`, `RoomTripRepositoryTest.kt`?**
  _High betweenness centrality (0.063) - this node is a cross-community bridge._
- **Why does `TripRepository` connect `TripRepository` to `Legs`, `FakeTripRepository`, `TripSettingsViewModel`, `Trips`, `Trips`, `TripListViewModel`, `Legs`, `RouteLegEntity`, `Legs`, `AppNavigation.kt`, `FakeRepository`, `RouteLegRepository`, `TripWorkspaceViewModel`, `TripSummary`, `FakeTripRepository`, `FakeTrips`, `Trips`, `RoomTripRepository`, `Trips`, `DayItineraryViewModel.kt`?**
  _High betweenness centrality (0.059) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 25 inferred relationships involving `TripWorkspaceViewModel` (e.g. with `.model()` and `.emptyScopeClearsRequestSoReplacementHostDoesNotReplayInitialViewport()`) actually correct?**
  _`TripWorkspaceViewModel` has 25 INFERRED edges - model-reasoned connections that need verification._
- **What connects `guard-adb-install.sh script`, `READY`, `STARTING` to the rest of the system?**
  _464 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Easy Trip v1.0 全量 UI 与交互落地设计` be split into smaller, more focused modules?**
  _Cohesion score 0.0425531914893617 - nodes in this community are weakly interconnected._