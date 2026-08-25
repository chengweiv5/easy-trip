# Graph Report - easy-trip-v1-full-ui-run  (2026-08-25)

## Corpus Check
- 280 files · ~153,568 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3882 nodes · 8475 edges · 195 communities (145 shown, 50 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 558 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c04a0819`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Easy Trip v1.0 全量 UI 与交互落地设计
- MapUiModelMapperTest
- FakeRepository
- TripService
- awaitSdkCallback
- TripSettingsViewModel
- Visual Fix Agent Stall 诊断
- PlaceSearchEvidenceTest
- TripWorkspaceContentStateTest
- V1ScenarioExecutableFactory
- .setContent
- RealAmapMapHost
- DayItineraryViewModel
- RoomSavedPlaceRepository
- RouteLegRepository
- DayItineraryViewModelTest
- GeoPoint
- AddToItineraryViewModel
- CreateTrip
- V1ScenarioMetadataTest
- SchemaTest
- Easy Trip 一期设计
- TripDao
- RecordingHost
- LocationPermissionCoordinator
- Easy Trip v1.0 Pencil 设计落地方案
- TripListViewModel
- Trips
- TripWithDays
- Easy Trip v2 交互与视觉升级设计
- Easy Trip UI 设计公式
- Dp
- MapLayer
- 高德 Android SDK 集成决策
- Trips
- Easy Trip 工作台导航重构设计
- RecordingTripRepository
- DeleteImpactDao
- Easy Trip 工作台 UI 收敛设计
- 2026-08-23-easy-trip-v1-full-ui-implementation.md
- Legs
- WorkspaceSearchReturnTestActivity
- View
- TripDay
- DayItineraryContent
- Batch 2 Gate Report
- FakeRepository
- Legs
- AppNavigation.kt
- UndoAddedItemsUseCase
- V1ScenarioExecutable.kt
- Easy Trip 搜索与收藏交互设计
- FakeRepository
- Dp
- PlacePoolViewModel
- FakeItineraries
- Batch 2 Room Timeout Diagnosis
- CompactSecondaryButton
- RouteLegEntity
- FakeItineraries
- FakeRepository
- RouteFormattingTest
- PlaceSearchViewModel
- TripWorkspaceViewModel
- CreateTripViewModel
- Batch 2 搜索加载与网络失败可控设备证据方案
- MapLifecycleController
- CascadeCountDao
- TripSummary
- .model
- ItineraryService
- MapLayerRenderingPolicyTest
- Task 4 报告：重做搜索状态机与连续收藏
- FakeLegs
- .referencedSavedResultRequiresConfirmationAndDismissPreservesData
- FakeTripRepository
- Converters
- Easy Trip Full UI Task 7 状态预研
- MapPoiUi
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest
- Legs
- PlaceCandidate
- .Content
- Trips
- TransportMode
- RoomRouteLegRepository
- PlaceDao
- CLAUDE.md
- Trips
- Task 5 报告：地点池、详情与地图控件
- AddPlacesRoomIntegrationTest
- FeedbackState
- consumeWorkspaceSearchReturn
- TripListContent
- LazyScrollbar.kt
- DayDeleteImpact
- ScenarioScreen
- File Structure
- File Structure
- MapViewportControllerTest
- Global Constraints
- Easy Trip v1.0 Pencil Reference
- Batch 1 visual fix report
- Legs
- MapControls
- MapLayerFlowTest
- AmapRouteDataSource.kt
- MapUiModel
- ItineraryTransactionTest
- PlacePoolContent
- RoomItineraryRepository
- MutableItineraries
- Legs
- FakeTrips
- .routeBackPublishesCurrentSessionCollectionsThroughNavigationCallback
- 行程项
- EasyTripDatabase
- AddPlacesRequest
- .setContent
- TravelMode
- TripWorkspaceNavigationStateTest
- Batch 2 地点池视觉修复报告
- AMap 模拟器 Smoke
- reduceMapInteraction
- Task 6 接口预研：批量加入与撤销
- CreateTripAction
- guard-adb-install.sh
- DayItineraryUiState
- AmapConsentToken
- Batch 2 AMap/EGL 环境诊断
- SDD ledger — plan: docs/superpowers/plans/2026-08-23-easy-trip-v1-full-ui-implementation.md
- SharedPreferences
- WorkspaceSheetLevel
- run-amap-smoke.sh
- SavedPlace
- amap-emulator-gate-test.sh
- run-amap-smoke-test.sh
- EasyTripTokens.kt
- Trips
- .emptyScopeClearsRequestSoReplacementHostDoesNotReplayInitialViewport
- Itineraries
- Trips
- TripWorkspaceRoute
- TripWorkspaceAction
- Itineraries
- Places
- Task 7 Phase 2 Report
- CreateTripUiState
- AddPlacesOutcome
- AmapMapHost
- File Structure
- V1 Full UI Scenario Matrix
- Legs
- amap-emulator-gate.sh
- SearchMapFocusTest
- ItineraryUiModels.kt
- DateRangeDeletionCounts
- Shared Fixture
- PermissionExplanationContent
- Places
- ConfirmationDialog
- SavedPlaces
- CreateTripContent
- TestMapHost
- RouteRequest
- WorkspaceIcons.kt
- EditingPlaces
- Itineraries
- Task 6 报告：批量加入与撤销领域用例
- Task 7 第一阶段报告：加入行程状态
- LocationPermissionSourceTest
- Batch 2 搜索受控状态证据契约
- parsePlaces
- MapLayerFlowTest
- RoutePlannerTest
- AmapMapHost
- AmapMapHost
- V1ComposeRule
- MainActivity
- DayItineraryViewModel.kt
- MapViewportRenderingPolicyTest
- SearchSurface
- .applyDateRange
- RecordingHost
- AmapMapHost
- MapFacade.kt
- MapLegend
- MapMarkerKind
- PlaceSearchDataSource
- Trips

## God Nodes (most connected - your core abstractions)
1. `GeoPoint` - 144 edges
2. `PlaceCandidate` - 94 edges
3. `TripDay` - 76 edges
4. `SavedPlace` - 73 edges
5. `TravelMode` - 71 edges
6. `CreateTrip` - 58 edges
7. `TripWorkspaceViewModel` - 58 edges
8. `TransportMode` - 57 edges
9. `TripService` - 54 edges
10. `TripRepository` - 49 edges

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

## Communities (195 total, 50 thin omitted)

### Community 0 - "Easy Trip v1.0 全量 UI 与交互落地设计"
Cohesion: 0.04
Nodes (46): 10.1 批次 1：旅行入口与创建, 10.2 批次 2：搜索、连续收藏与地点池, 10.3 批次 3：从地点池加入行程与旅行日, 10.4 批次 4：单日编辑、交通路段与全程, 10.5 批次 5：设置、日期、权限与完整回归, 10. 五个实施批次, 11.1 本地优先, 11.2 收藏 (+38 more)

### Community 2 - "FakeRepository"
Cohesion: 0.09
Nodes (21): connected(), hasValidatedInternet(), StateFlow, NetworkMonitor, onAvailable(), onCapabilitiesChanged(), onLost(), Active (+13 more)

### Community 3 - "TripService"
Cohesion: 0.11
Nodes (7): com, CreateTrip, TripService, FakeTripRepository, CreateTrip, Flow, TripServiceTest

### Community 4 - "awaitSdkCallback"
Cohesion: 0.07
Nodes (19): OneShotCallback, awaitSdkCallback(), BridgeState, CANCELLED, COMPLETED, READY, STARTED, STARTING (+11 more)

### Community 5 - "TripSettingsViewModel"
Cohesion: 0.13
Nodes (9): Factory, androidx, com, Job, StateFlow, T, ViewModel, ViewModelProvider (+1 more)

### Community 6 - "Visual Fix Agent Stall 诊断"
Cohesion: 0.06
Nodes (30): 1. Executive summary, 1. Harness 在 tool-result 边界自动结束，最终答复回合缺失, 2. 两次 run timeline, 2. 超大单任务上下文与高频工具链导致空终局/自动收尾概率升高, 3. 停止信号检查, 3. 存在未记录的内部运行时/turn/tool 预算, 4. adb/monkey 失败直接导致第一次停止, 4. 行为模式与次数 (+22 more)

### Community 7 - "PlaceSearchEvidenceTest"
Cohesion: 0.05
Nodes (38): gitBytes(), sha256(), PlaceSearchContentTest, EvidenceCase, ExpectedEvidence, PlaceSearchEvidenceTest, BackIcon(), BookmarkIcon() (+30 more)

### Community 8 - "TripWorkspaceContentStateTest"
Cohesion: 0.14
Nodes (5): Itineraries, ItineraryRepository, Trips, TripWorkspaceContentStateTest, Places

### Community 9 - "V1ScenarioExecutableFactory"
Cohesion: 0.19
Nodes (6): ComposeScenario, com, ScenarioFixture, ScenarioPath, V1ScenarioExecutable, V1ScenarioExecutableFactory

### Community 10 - ".setContent"
Cohesion: 0.13
Nodes (15): TripListContentTest, Content, CreateTrip, Empty, Error, Loading, OpenSettings, OpenTrip (+7 more)

### Community 11 - "RealAmapMapHost"
Cohesion: 0.08
Nodes (10): AmapMapHost, Bounds, android, Modifier, View, MapZoomButton(), RealAmapMapHost, SinglePoint (+2 more)

### Community 12 - "DayItineraryViewModel"
Cohesion: 0.14
Nodes (3): DayItineraryViewModel, CrossDayMoveDraft, RouteModeEditDraft

### Community 13 - "RoomSavedPlaceRepository"
Cohesion: 0.11
Nodes (6): CascadeDeleteTest, AppContainer, CoroutineScope, Flow, RoomSavedPlaceRepository, PlaceService

### Community 15 - "DayItineraryViewModelTest"
Cohesion: 0.09
Nodes (9): Coordinator, DayItineraryViewModelTest, Itineraries, CompletableDeferred, ItineraryRepository, kotlinx, Legs, Timing (+1 more)

### Community 16 - "GeoPoint"
Cohesion: 0.17
Nodes (8): GeoPoint, DayItinerary, ItineraryItem, ItineraryPlace, Flow, Result, PolylineCodec, PolylineCodecTest

### Community 17 - "AddToItineraryViewModel"
Cohesion: 0.13
Nodes (11): AddToItineraryViewModel, Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider (+3 more)

### Community 18 - "CreateTrip"
Cohesion: 0.06
Nodes (10): RoomSavedPlaceRepositoryTest, IdFactory, T, MutableClock, RoomTripRepositoryTest, TripDateRangeRoomTest, Flow, RoomTripRepository (+2 more)

### Community 19 - "V1ScenarioMetadataTest"
Cohesion: 0.06
Nodes (19): V1FullUiAcceptanceTest, V1ScenarioCatalogTest, V1ScenarioMetadataTest, BlockerCategory, BASIC_ACCESSIBILITY, CRASH_FREE, DATA_CONSISTENCY, FUNCTIONAL_STATE (+11 more)

### Community 21 - "Easy Trip 一期设计"
Cohesion: 0.17
Nodes (12): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 离线路线等待与联网续算, 同一地点可重复安排行程 (+4 more)

### Community 22 - "TripDao"
Cohesion: 0.09
Nodes (7): OfflineRecoveryTest, CreateTrip, Flow, TripDao, TripEntityWithDays, TripDayEntity, TripEntity

### Community 24 - "LocationPermissionCoordinator"
Cohesion: 0.15
Nodes (10): InMemoryLocationPermissionRequestStore, StateFlow, T, LocationPermissionCoordinator, LocationPermissionRequestStore, LocationPermissionSnapshot, SavedStateLocationPermissionRequestStore, SharedPreferencesLocationPermissionRequestStore (+2 more)

### Community 25 - "Easy Trip v1.0 Pencil 设计落地方案"
Cohesion: 0.05
Nodes (39): 10.1 JVM, 10.2 Compose instrumentation, 10.3 数据与导航集成, 10.4 验证命令, 10. 测试策略, 11. 视觉验收, 12. 验收标准, 13. 协作边界 (+31 more)

### Community 26 - "TripListViewModel"
Cohesion: 0.15
Nodes (11): Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider, OpenSettings (+3 more)

### Community 27 - "Trips"
Cohesion: 0.06
Nodes (10): DayItinerarySelectionTest, Itineraries, com, CompletableDeferred, CreateTrip, Flow, ItineraryRepository, Trips (+2 more)

### Community 28 - "TripWithDays"
Cohesion: 0.09
Nodes (21): Flow, Flow, Flow, Flow, ItineraryRepository, AlreadySaved, Flow, PlaceTag (+13 more)

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.17
Nodes (15): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+7 more)

### Community 30 - "Easy Trip UI 设计公式"
Cohesion: 0.05
Nodes (43): 10.1 无网, 10.2 路线失败, 10.3 地图加载中或失败, 10.4 部分成功, 10.5 提交中与目标日失效, 10.6 保存失败, 10. 本地优先与状态公式, 11.1 地图服务授权 (+35 more)

### Community 31 - "Dp"
Cohesion: 0.19
Nodes (9): SelectablePillTest, Modifier, Role, SelectablePill(), SelectablePillStyle, DaySelector(), LazyListState, Modifier (+1 more)

### Community 32 - "MapLayer"
Cohesion: 0.11
Nodes (9): MapLayer, SATELLITE_ROAD, STANDARD, InMemoryMapPreferences, StateFlow, MapPreferences, SharedPreferencesMapPreferences, MapPreferencesTest (+1 more)

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.14
Nodes (14): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+6 more)

### Community 34 - "Trips"
Cohesion: 0.13
Nodes (3): com, CreateTrip, Trips

### Community 35 - "Easy Trip 工作台导航重构设计"
Cohesion: 0.06
Nodes (33): 10. 数据模型, 11. 空态与错误处理, 12. 可访问性, 13. 测试策略, 14. 验收标准, 1. 目标, 2. 非目标, 3. 信息架构 (+25 more)

### Community 36 - "RecordingTripRepository"
Cohesion: 0.13
Nodes (4): com, CreateTrip, Flow, RecordingTripRepository

### Community 38 - "Easy Trip 工作台 UI 收敛设计"
Cohesion: 0.06
Nodes (32): 10. 待讨论项, 11. 实施批次, 12.1 自动化, 12.2 真机抽查, 12.3 阻断条件, 12. 测试与验收, 13. 完成定义, 1. 目标 (+24 more)

### Community 39 - "2026-08-23-easy-trip-v1-full-ui-implementation.md"
Cohesion: 0.07
Nodes (27): Batch 1 Gate, Batch 1：旅行入口、创建与删除, Batch 2 Gate, Batch 2：搜索、连续收藏与地点池, Batch 3 Gate, Batch 3：从地点池加入行程与旅行日, Batch 4 Gate, Batch 4：单日编辑、RouteLeg、全程与抽屉 (+19 more)

### Community 40 - "Legs"
Cohesion: 0.05
Nodes (8): Itineraries, com, CreateTrip, ItineraryRepository, Legs, Places, Trips, WorkspaceSearchTabsTest

### Community 41 - "WorkspaceSearchReturnTestActivity"
Cohesion: 0.15
Nodes (8): ClearedProbe, ViewModel, WorkspaceSearchReturnNavEntryTest, Bundle, ComponentActivity, WorkspaceSearchReturnTestActivity, NavBackStackEntry, NavHostController

### Community 42 - "View"
Cohesion: 0.06
Nodes (8): AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, View

### Community 43 - "TripDay"
Cohesion: 0.10
Nodes (17): ItineraryScopeRailTest, ItineraryScopeRail(), Modifier, ScopeItem(), Modifier, WorkspaceItineraryContent(), TripDay, Day (+9 more)

### Community 44 - "DayItineraryContent"
Cohesion: 0.13
Nodes (22): AddPlace, AddPlaces, AppendTripDay, CommitMove, ConfirmDelete, DayItineraryAction, DayItineraryContent(), DismissDialogs (+14 more)

### Community 45 - "Batch 2 Gate Report"
Cohesion: 0.06
Nodes (34): 1. Graphify 定向查询, 2. 全套 JVM、lint、assemble, 3. 设备检查, 4. Task 4/5 目标 Compose 与相关 Room 测试, adb/uiautomator 断言, API 36 恢复环境生产旅程 B（2026-08-24）, Batch 2 Gate Report, Concerns (+26 more)

### Community 46 - "FakeRepository"
Cohesion: 0.11
Nodes (7): DateRangeApply, DateRangeChangeImpact, TripDateRangeService, FakeRepository, CreateTrip, Flow, TripDateRangeServiceTest

### Community 48 - "AppNavigation.kt"
Cohesion: 0.18
Nodes (12): AppNavigation(), AppNavigationDependencies, AppNavigationObserver, android, com, tripSearchRoute(), WorkspaceSearchReturnViewModel, PlaceSearchRoute() (+4 more)

### Community 49 - "UndoAddedItemsUseCase"
Cohesion: 0.36
Nodes (4): UndoAddedItemsOutcome, UndoAddedItemsRequest, UndoAddedItemsUseCase, UndoAddedItemsUseCaseTest

### Community 50 - "V1ScenarioExecutable.kt"
Cohesion: 0.06
Nodes (45): ConfirmationUiModel, AddToItineraryStep, COMPLETED, IDLE, SELECT_PLACES, SELECT_TARGET_DAY, AddToItineraryUiState, ItineraryDeleteConfirmation (+37 more)

### Community 51 - "Easy Trip 搜索与收藏交互设计"
Cohesion: 0.07
Nodes (27): 10. 边界与错误处理, 11. 测试策略, 12. 验收标准, 1. 背景与目标, 2. 非目标, 3. 当前实现基线, 4.1 工作台, 4.2 独立搜索页 (+19 more)

### Community 52 - "FakeRepository"
Cohesion: 0.18
Nodes (4): CreateTripViewModelTest, FakeRepository, FakeRepository, SavedStateHandle

### Community 53 - "Dp"
Cohesion: 0.11
Nodes (14): EasyTripButtonTest, EasyTripIconButton(), Modifier, EmptyState(), Modifier, ItineraryItemCard(), ItineraryItemRow(), Color (+6 more)

### Community 54 - "PlacePoolViewModel"
Cohesion: 0.05
Nodes (15): Factory, Job, kotlinx, PlaceSearchDataSource, StateFlow, T, ViewModel, ViewModelProvider (+7 more)

### Community 55 - "FakeItineraries"
Cohesion: 0.15
Nodes (9): Add, FakeCoordinator, FakeItineraries, ItineraryEditingTest, ItineraryRepository, kotlinx, Move, Timing (+1 more)

### Community 56 - "Batch 2 Room Timeout Diagnosis"
Cohesion: 0.11
Nodes (18): Batch 2 Room Timeout Diagnosis, GREEN 修复与验证, Phase 1：失败与复现, Phase 2：模式对比, Phase 3：单一假设验证, 仓库内 working Room Flow 模式, 假设, 判定 (+10 more)

### Community 57 - "CompactSecondaryButton"
Cohesion: 0.16
Nodes (16): CompactPrimaryButton(), CompactSecondaryButton(), AddTripDayContent(), EditItineraryItemContent(), Modifier, Modifier, SelectTargetDayContent(), Modifier (+8 more)

### Community 58 - "RouteLegEntity"
Cohesion: 0.09
Nodes (6): RoomDeleteImpactProviderTest, SchemaPlaceDao, SchemaRouteDao, SavedPlaceTagCrossRef, TagEntity, RouteLegEntity

### Community 59 - "FakeItineraries"
Cohesion: 0.18
Nodes (6): UndoCreatedItemsBatch, AddToItineraryStateTest, FakeItineraries, ItineraryRepository, SavedStateHandle, FakeItineraries

### Community 60 - "FakeRepository"
Cohesion: 0.12
Nodes (8): FakeImpacts, FakeRepository, com, CompletableDeferred, CreateTrip, FakeRepository, Flow, TripSettingsViewModelTest

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 62 - "PlaceSearchViewModel"
Cohesion: 0.05
Nodes (28): CollectionDecision, Confirm, decideCollectionToggle(), PendingCollectionRemoval, RemoveNow, Save, Back, ConfirmRemoval (+20 more)

### Community 63 - "TripWorkspaceViewModel"
Cohesion: 0.11
Nodes (17): Factory, CreationExtras, Flow, ItineraryRepository, Job, StateFlow, T, ViewModel (+9 more)

### Community 64 - "CreateTripViewModel"
Cohesion: 0.23
Nodes (8): CreateTripViewModel, Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider

### Community 65 - "Batch 2 搜索加载与网络失败可控设备证据方案"
Cohesion: 0.07
Nodes (26): 1. 无设备的生产状态机回归, 2. 明确锁定获准 AVD, 3. 运行专用可控证据测试, 4. 拉取并校验, 5. 视觉对照, A. 生产真实触发, B. 可控状态渲染证据, Batch 2 搜索加载与网络失败可控设备证据方案 (+18 more)

### Community 68 - "TripSummary"
Cohesion: 0.12
Nodes (9): TripSummary, TripListUiModelsTest, com, CompletableDeferred, CreateTrip, Flow, TestImpacts, TestTripRepository (+1 more)

### Community 72 - "Task 4 报告：重做搜索状态机与连续收藏"
Cohesion: 0.13
Nodes (14): Device gate review fix round 1, Fix round 1, Fix round 2, Fix round 3, Fix round 4, Graphify, Pencil 对照, Task 4 报告：重做搜索状态机与连续收藏 (+6 more)

### Community 74 - ".referencedSavedResultRequiresConfirmationAndDismissPreservesData"
Cohesion: 0.11
Nodes (8): Flow, ItineraryRepository, java, PlaceSearchDataSource, ItineraryRepository, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource

### Community 75 - "FakeTripRepository"
Cohesion: 0.15
Nodes (7): FakeImpacts, FakeTripRepository, com, CreateTrip, Role, MoveCall, TripFlowTest

### Community 76 - "Converters"
Cohesion: 0.09
Nodes (6): Converters, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT, UNSUPPORTED_TRANSIT

### Community 77 - "Easy Trip Full UI Task 7 状态预研"
Cohesion: 0.08
Nodes (25): Compose / Route, Easy Trip Full UI Task 7 状态预研, JVM 状态/reducer, partial success 与 undo 语义, SavedStateHandle 键与恢复, scheduled 与 selectedPlaceIds 分离, Task 6 依赖契约, Task 6 当前阻塞 (+17 more)

### Community 78 - "MapPoiUi"
Cohesion: 0.11
Nodes (7): Itineraries, AmapMapHost, ItineraryRepository, Places, PoiHost, WorkspaceFlowTest, MapPoiUi

### Community 79 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 80 - "Easy Trip"
Cohesion: 0.50
Nodes (4): 高德地图本地密钥配置, Easy Trip, 本地优先 Android 旅行规划, 未授权或离线本地编辑

### Community 83 - "Legs"
Cohesion: 0.11
Nodes (3): com, Flow, Legs

### Community 86 - "PlaceCandidate"
Cohesion: 0.10
Nodes (12): AmapServiceException, AmapPlaceDataSource, CallbackBoundary, PoiSearch, CallbackBoundary, com, parsePoiSearchResponse(), PlaceCandidate (+4 more)

### Community 87 - ".Content"
Cohesion: 0.46
Nodes (3): LazyListState, Modifier, PlacePoolScrollbarTest

### Community 88 - "Trips"
Cohesion: 0.13
Nodes (3): com, CreateTrip, Trips

### Community 89 - "TransportMode"
Cohesion: 0.05
Nodes (20): RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK, TransportMode, DRIVE (+12 more)

### Community 91 - "PlaceDao"
Cohesion: 0.15
Nodes (4): Flow, PlaceDao, PlaceSnapshotRow, PlaceUsageRow

### Community 93 - "Trips"
Cohesion: 0.14
Nodes (3): com, CreateTrip, Trips

### Community 94 - "Task 5 报告：地点池、详情与地图控件"
Cohesion: 0.17
Nodes (11): Fix round 1, Fix round 2：Room 测试调度竞态, Graphify, Pencil 对照, Task 5 报告：地点池、详情与地图控件, TDD 证据, 修改文件, 关注点 (+3 more)

### Community 95 - "AddPlacesRoomIntegrationTest"
Cohesion: 0.24
Nodes (4): AddPlacesRoomIntegrationTest, DeleteTargetDayAfterFirstAddRepository, ItineraryRepository, SequenceIds

### Community 96 - "FeedbackState"
Cohesion: 0.24
Nodes (11): FeedbackStateTest, EmptyFeedbackState(), ErrorFeedbackState(), FeedbackContent(), FeedbackKind, EMPTY, ERROR, LOADING (+3 more)

### Community 97 - "consumeWorkspaceSearchReturn"
Cohesion: 0.26
Nodes (6): consumeWorkspaceSearchReturn(), androidx, publishWorkspaceSearchReturn(), TestOwner, WorkspaceSearchReturnNavigationTest, ViewModelStoreOwner

### Community 98 - "TripListContent"
Cohesion: 0.14
Nodes (26): ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton(), CompactDangerButton(), Modifier, EasyTripButton() (+18 more)

### Community 99 - "LazyScrollbar.kt"
Cohesion: 0.29
Nodes (6): calculateScrollbarThumb(), LazyListState, Modifier, ReadOnlyLazyScrollbar(), ScrollbarThumb, LazyScrollbarGeometryTest

### Community 100 - "DayDeleteImpact"
Cohesion: 0.20
Nodes (7): TripSettingsContentTest, DateRangeChangeUiState, TripSettingsContent(), DayDeleteImpact, DayUi, PendingDayDeletion, TripSettingsUiState

### Community 101 - "ScenarioScreen"
Cohesion: 0.13
Nodes (15): ScenarioScreen, CREATE_TRIP, DATE_PICKER, ITEM_EDITOR, ITINERARY, PERMISSION, PLACE_DETAIL, PLACE_POOL (+7 more)

### Community 102 - "File Structure"
Cohesion: 0.18
Nodes (10): Easy Trip Workspace Navigation Implementation Plan, File Structure, Global Constraints, Task 1: 建立统一导航模型与兼容迁移, Task 2: 建立全程只读 UI 模型, Task 3: Make TripWorkspaceViewModel the single navigation source, Task 4: Separate editable day content from date selection and share presentation, Task 5: Build the 88dp itinerary scope rail and two-column content (+2 more)

### Community 103 - "File Structure"
Cohesion: 0.15
Nodes (12): Easy Trip v1.0 Pencil Implementation Plan, File Structure, Global Constraints, Task 1: 冻结 Pencil 主流程基线, Task 2: 建立 v1.0 Theme 与共享组件, Task 3: 拆分并实现“我的旅行”页面, Task 4: 实现创建旅行状态机与真实提交, Task 5: 拆分工作台并实现地图降级 (+4 more)

### Community 104 - "MapViewportControllerTest"
Cohesion: 0.11
Nodes (9): MapViewportRequest, ViewportReason, INITIAL, PLACE_SET_CHANGED, SCOPE_CHANGED, SEARCH_FOCUS, VISIBLE_SET_CHANGED, MapViewportController (+1 more)

### Community 105 - "Global Constraints"
Cohesion: 0.20
Nodes (9): Easy Trip Search Collection Implementation Plan, Global Constraints, Task 1: 收敛工作台 Tab 并建立独立搜索路由, Task 2: 完成搜索页状态清理与一次性返回聚焦, Task 3: 统一搜索结果与地点卡片的收藏切换策略, Task 4: 建立稳定的收藏 marker 身份、编号与聚焦样式, Task 5: 接入高德底图 POI 点击与统一地点卡片, Task 6: 为地点池增加只读纵向 scrollbar thumb (+1 more)

### Community 106 - "Easy Trip v1.0 Pencil Reference"
Cohesion: 0.18
Nodes (10): Easy Trip v1.0 Pencil Reference, Layout Verification, Main Flow Frames, Public Components, Responsive Notes, Screenshot Baseline, Semantic Tokens, Shape, Spacing, Size, Elevation (+2 more)

### Community 107 - "Batch 1 visual fix report"
Cohesion: 0.20
Nodes (9): Batch 1 visual fix report, Commit, Fix round 1, Fix round 1 commit, Fix round 1 最终验证, Frame 差异与修复, RED / GREEN, 修改文件 (+1 more)

### Community 109 - "MapControls"
Cohesion: 0.28
Nodes (4): WorkspaceChromeTest, description(), Modifier, MapControls()

### Community 111 - "AmapRouteDataSource.kt"
Cohesion: 0.19
Nodes (8): CallbackBoundary, RouteSearch, CallbackBoundary, RouteCallback, BusRouteResult, DriveRouteResult, RideRouteResult, WalkRouteResult

### Community 112 - "MapUiModel"
Cohesion: 0.13
Nodes (9): AmapComposeMapTest, Lifecycle, LifecycleOwner, TestOwner, AmapComposeMap(), MapHostCallbackGuard, toMapPoiUi(), MapUiModel (+1 more)

### Community 114 - "PlacePoolContent"
Cohesion: 0.12
Nodes (19): ConfirmCollectionRemoval, ConfirmDelete, Delete, DismissDialogs, Edit, Modifier, PlacePoolAction, PlacePoolContent() (+11 more)

### Community 115 - "RoomItineraryRepository"
Cohesion: 0.09
Nodes (10): DayItems, DayItineraryRow, ItineraryDao, Flow, ItineraryItemEntity, ItineraryRepository, RoomItineraryRepository, AdjacencyDiff (+2 more)

### Community 118 - "FakeTrips"
Cohesion: 0.12
Nodes (4): FakeTrips, com, CreateTrip, java

### Community 119 - ".routeBackPublishesCurrentSessionCollectionsThroughNavigationCallback"
Cohesion: 0.29
Nodes (3): com, com, com

### Community 120 - "行程项"
Cohesion: 0.67
Nodes (3): 行程项, 路线段, 收藏地点

### Community 121 - "EasyTripDatabase"
Cohesion: 0.08
Nodes (11): AppIconResourceTest, V1AcceptanceTest, V1PencilFlowTest, EasyTripDatabase, com, TimeMode, DATED, DRAFT (+3 more)

### Community 122 - "AddPlacesRequest"
Cohesion: 0.16
Nodes (10): AddPlacesRequest, AddPlacesToDayUseCase, RecoverablePlaceAddException, TargetDayNotFoundException, AddCall, AddPlacesToDayUseCaseTest, FakeItineraryRepository, Flow (+2 more)

### Community 124 - "TravelMode"
Cohesion: 0.09
Nodes (10): Flow, TravelMode, FLEXIBLE, SELF_DRIVE, InsertSide, AFTER, BEFORE, Flow (+2 more)

### Community 126 - "Batch 2 地点池视觉修复报告"
Cohesion: 0.14
Nodes (13): Batch 2 地点池视觉修复报告, Concerns, Diff 审计, Findings 修复, Fix round 1（2026-08-24）, Fix round 2（2026-08-24）, Fix round 3（2026-08-24）, GREEN (+5 more)

### Community 127 - "AMap 模拟器 Smoke"
Cohesion: 0.33
Nodes (5): AMap 模拟器 Smoke, Smoke 行为, 停止与恢复, 固定环境, 门禁与互斥

### Community 128 - "reduceMapInteraction"
Cohesion: 0.27
Nodes (6): FocusSearchResult, MapInteractionAction, MapInteractionState, ReconcileSearchResults, reduceMapInteraction(), MapInteractionReducerTest

### Community 129 - "Task 6 接口预研：批量加入与撤销"
Cohesion: 0.10
Nodes (19): AddPlacesToDayUseCase, Domain Repository, Room/DAO, Task 6 接口预研：批量加入与撤销, UndoAddedItemsUseCase, 建议文件列表, 接口缺口与建议边界, 明确禁止项 (+11 more)

### Community 130 - "CreateTripAction"
Cohesion: 0.13
Nodes (14): Back, CreateTimeMode, DATED, DRAFT, CreateTripAction, CreateTripEffect, DayCountChanged, NameChanged (+6 more)

### Community 132 - "DayItineraryUiState"
Cohesion: 0.12
Nodes (24): WorkspaceSheetScenarioSpec, DayItineraryUiState, PlacePoolUiState, Composable, Modifier, TripWorkspaceContent(), WorkspacePageMessage(), WorkspaceReadyContent() (+16 more)

### Community 133 - "AmapConsentToken"
Cohesion: 0.07
Nodes (17): AmapMapViewAttachSmokeTest, AmapAttachSmokeActivity, Bundle, ComponentActivity, AmapConsentToken, AmapPrivacyGate, AmapPrivacyStateMachine, ConsentRegistry (+9 more)

### Community 134 - "Batch 2 AMap/EGL 环境诊断"
Cohesion: 0.18
Nodes (10): Batch 2 AMap/EGL 环境诊断, 原始失败证据, 后续旅程判断, 恢复验证证据, 最小恢复操作, 根因结论, 状态, 环境对比 (+2 more)

### Community 137 - "WorkspaceSheetLevel"
Cohesion: 0.15
Nodes (12): settledWorkspaceSheetLevel(), Modifier, restoreWorkspaceSheetLevel(), WorkspaceBottomSheet(), workspaceSheetFraction(), workspaceSheetHeightDp(), WorkspaceSheetLevel, COLLAPSED (+4 more)

### Community 138 - "run-amap-smoke.sh"
Cohesion: 0.52
Nodes (5): cleanup(), handle_signal(), process_alive(), same_process_identity(), run-amap-smoke.sh script

### Community 139 - "SavedPlace"
Cohesion: 0.16
Nodes (8): PlacePoolFlowTest, Flow, TestSavedPlaces, Modifier, SelectPlacesContent(), SavedPlace, EditSavedPlaceDialog(), SavedPlaceRowUi

### Community 140 - "amap-emulator-gate-test.sh"
Cohesion: 0.80
Nodes (4): expect_rejected(), fail(), amap-emulator-gate-test.sh script, write_adb()

### Community 141 - "run-amap-smoke-test.sh"
Cohesion: 0.70
Nodes (4): fail(), make_fixture(), run_status(), run-amap-smoke-test.sh script

### Community 142 - "EasyTripTokens.kt"
Cohesion: 0.52
Nodes (4): EasyTripElevation, EasyTripSizes, EasyTripSpacing, EasyTripTheme

### Community 143 - "Trips"
Cohesion: 0.13
Nodes (3): com, CreateTrip, Trips

### Community 144 - ".emptyScopeClearsRequestSoReplacementHostDoesNotReplayInitialViewport"
Cohesion: 0.50
Nodes (3): Lifecycle, LifecycleOwner, TestOwner

### Community 146 - "Trips"
Cohesion: 0.14
Nodes (3): com, CreateTrip, Trips

### Community 148 - "TripWorkspaceAction"
Cohesion: 0.15
Nodes (13): Back, CloseOverlay, Locate, OpenOverlay, OpenPrivacySettings, OpenSearch, OpenSettings, Retry (+5 more)

### Community 151 - "Task 7 Phase 2 Report"
Cohesion: 0.29
Nodes (6): Task 7 Phase 2 Report, TDD, 实现, 未覆盖风险, 状态, 验证

### Community 152 - "CreateTripUiState"
Cohesion: 0.32
Nodes (5): CreateTripUiState, CreateTripValidation, validateCreateTrip(), ValidCreateTrip, CreateTripValidatorTest

### Community 153 - "AddPlacesOutcome"
Cohesion: 0.21
Nodes (8): AddPlacesOutcome, PartialSuccess, Success, TargetDayMissing, AddToItineraryEditingTarget, ForDay, FromPlacePool, Flow

### Community 155 - "File Structure"
Cohesion: 0.13
Nodes (14): Easy Trip 工作台 UI 收敛实施计划, Execution Notes, File Structure, Global Constraints, Task 1: 建立工作台视觉 token 与图标基础, Task 2: 修正 BottomSheet 锚点与手势几何, Task 3: 建立统一 WorkspaceScaffold 边界, Task 4: 收敛 TopBar、搜索、地图控件、图例和普通 Tab (+6 more)

### Community 156 - "V1 Full UI Scenario Matrix"
Cohesion: 0.33
Nodes (5): Coverage model, Cross-cutting journeys and matrices, Gate policy, Scenario directory, V1 Full UI Scenario Matrix

### Community 160 - "ItineraryUiModels.kt"
Cohesion: 0.11
Nodes (22): WholeTripItineraryContentTest, Failed, ItineraryItemUi, mapWholeTripDays(), Ready, RouteLegUi, RouteLegUiState, toItineraryItemUi() (+14 more)

### Community 162 - "Shared Fixture"
Cohesion: 0.15
Nodes (12): Expected RED Findings Against `b26bda6`, File Structure, Global Constraints, Self-Review, Shared Fixture, Task 1: 创建真实 Room 基线、重复地点与末尾追加测试, Task 2: 固定类型化单地点恢复与未知基础设施异常传播, Task 3: 模拟批次中途目标日删除并验证完整请求恢复 (+4 more)

### Community 163 - "PermissionExplanationContent"
Cohesion: 0.15
Nodes (11): WorkspacePermissionFlowTest, OpenApplicationSettings, RequestLocationPermission, ShowCurrentLocation, WorkspaceEffect, PermissionCopy, PermissionExplanationContent(), PermissionKind (+3 more)

### Community 164 - "Places"
Cohesion: 0.18
Nodes (3): FailingMapPreferences, StateFlow, Places

### Community 165 - "ConfirmationDialog"
Cohesion: 0.29
Nodes (6): ConfirmationDialogTest, ConfirmationDialog(), ConfirmationSection(), Color, Modifier, TripListScreen()

### Community 167 - "CreateTripContent"
Cohesion: 0.39
Nodes (4): CreateTripContentTest, CreateTripContent(), Modifier, StepNumber()

### Community 169 - "RouteRequest"
Cohesion: 0.12
Nodes (14): AmapSmokeTest, AmapRouteDataSource, RouteDataSource, parseRouteResult(), RoutePathData, selectUsablePath(), validateRouteRequest(), RouteDataSource (+6 more)

### Community 170 - "WorkspaceIcons.kt"
Cohesion: 0.42
Nodes (8): Modifier, WorkspaceBackIcon(), WorkspaceCheckIcon(), WorkspaceCloseIcon(), WorkspaceLayerIcon(), WorkspaceLocateIcon(), WorkspaceMoreIcon(), WorkspaceSearchIcon()

### Community 173 - "Task 6 报告：批量加入与撤销领域用例"
Cohesion: 0.25
Nodes (7): Fix 1, Room gate, Task 6 报告：批量加入与撤销领域用例, TDD / JVM, 实现, 最终状态, 未包含与风险

### Community 174 - "Task 7 第一阶段报告：加入行程状态"
Cohesion: 0.25
Nodes (7): Fix 1, Fix 2, Task 7 第一阶段报告：加入行程状态, TDD 与验证, 后续阶段, 实现, 范围

### Community 176 - "Batch 2 搜索受控状态证据契约"
Cohesion: 0.29
Nodes (6): Batch 2 搜索受控状态证据契约, 单设备 agent 命令, 发布与消费协议, 构建 provenance, 范围, 视觉契约

### Community 177 - "parsePlaces"
Cohesion: 0.43
Nodes (3): parsePlaces(), RawPlace, PlaceContractsTest

### Community 179 - "MapLayerFlowTest"
Cohesion: 0.38
Nodes (3): FakeMapPreferences, Role, MapLayerFlowTest

### Community 180 - "RoutePlannerTest"
Cohesion: 0.38
Nodes (3): RouteDataSource, RoutePlannerTest, RouteDataSource

### Community 184 - "MainActivity"
Cohesion: 0.60
Nodes (3): Bundle, ComponentActivity, MainActivity

### Community 185 - "DayItineraryViewModel.kt"
Cohesion: 0.29
Nodes (5): Factory, StateFlow, T, ViewModel, ViewModelProvider

### Community 189 - "RecordingHost"
Cohesion: 0.18
Nodes (4): AmapMapHost, CoroutineScope, RecordingHost, V2AcceptanceTest

### Community 192 - "MapFacade.kt"
Cohesion: 0.14
Nodes (14): CorruptRoute, formatOccurrenceBadge(), MapMarkerUi, MapPolylineUi, MapRouteLabelUi, MapScope, PLACE_POOL, SINGLE_DAY (+6 more)

### Community 194 - "MapMarkerKind"
Cohesion: 0.50
Nodes (4): MapMarkerKind, SAVED_ITINERARY, SAVED_PLACE_POOL, UNSAVED_SEARCH

## Knowledge Gaps
- **656 isolated node(s):** `guard-adb-install.sh script`, `TRIP_LIST`, `CREATE_TRIP`, `DATE_PICKER`, `WORKSPACE` (+651 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **50 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `reduceMapInteraction`, `MapUiModelMapperTest`, `FakeRepository`, `PlaceSearchEvidenceTest`, `V1ScenarioExecutableFactory`, `SavedPlace`, `RealAmapMapHost`, `RoomSavedPlaceRepository`, `RouteLegRepository`, `.emptyScopeClearsRequestSoReplacementHostDoesNotReplayInitialViewport`, `CreateTrip`, `TripWorkspaceRoute`, `RecordingHost`, `TripWithDays`, `SearchMapFocusTest`, `SavedPlaces`, `RouteRequest`, `EditingPlaces`, `AppNavigation.kt`, `parsePlaces`, `V1ScenarioExecutable.kt`, `RoutePlannerTest`, `PlacePoolViewModel`, `FakeItineraries`, `MapViewportRenderingPolicyTest`, `PlaceSearchViewModel`, `TripWorkspaceViewModel`, `MapFacade.kt`, `.model`, `.referencedSavedResultRequiresConfirmationAndDismissPreservesData`, `MapPoiUi`, `PlaceCandidate`, `.Content`, `TransportMode`, `RoomRouteLegRepository`, `MapViewportControllerTest`, `AmapRouteDataSource.kt`, `MapUiModel`, `.routeBackPublishesCurrentSessionCollectionsThroughNavigationCallback`, `EasyTripDatabase`, `AddPlacesRequest`, `.setContent`, `TripWorkspaceNavigationStateTest`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Why does `TripRepository` connect `TravelMode` to `TripService`, `TripSettingsViewModel`, `Trips`, `CreateTrip`, `Trips`, `TripListViewModel`, `Trips`, `TripWithDays`, `DateRangeDeletionCounts`, `Trips`, `RecordingTripRepository`, `Legs`, `FakeRepository`, `AppNavigation.kt`, `FakeRepository`, `DayItineraryViewModel.kt`, `FakeRepository`, `TripWorkspaceViewModel`, `TripSummary`, `Trips`, `FakeTripRepository`, `Trips`, `Trips`, `FakeTrips`?**
  _High betweenness centrality (0.070) - this node is a cross-community bridge._
- **Why does `TripDay` connect `TripDay` to `MapUiModelMapperTest`, `TripService`, `TripWorkspaceContentStateTest`, `V1ScenarioExecutableFactory`, `Trips`, `GeoPoint`, `CreateTrip`, `Trips`, `Trips`, `TripWithDays`, `Dp`, `ItineraryUiModels.kt`, `Trips`, `Legs`, `FakeRepository`, `V1ScenarioExecutable.kt`, `DayItineraryViewModel.kt`, `CompactSecondaryButton`, `FakeRepository`, `TripWorkspaceViewModel`, `MapFacade.kt`, `.model`, `FakeTripRepository`, `PlaceCandidate`, `Trips`, `FakeTrips`, `TravelMode`, `TripWorkspaceNavigationStateTest`?**
  _High betweenness centrality (0.066) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 5 inferred relationships involving `TripDay` (e.g. with `FakeTrips` and `.trip()`) actually correct?**
  _`TripDay` has 5 INFERRED edges - model-reasoned connections that need verification._
- **What connects `guard-adb-install.sh script`, `TRIP_LIST`, `CREATE_TRIP` to the rest of the system?**
  _656 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Easy Trip v1.0 全量 UI 与交互落地设计` be split into smaller, more focused modules?**
  _Cohesion score 0.0425531914893617 - nodes in this community are weakly interconnected._