# Graph Report - agent-a69daf4cca1e41d1e  (2026-08-25)

## Corpus Check
- 276 files · ~142,599 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3822 nodes · 8408 edges · 194 communities (150 shown, 44 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 553 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `95703c7b`
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
- PlaceSearchEvidenceTest
- Trips
- V1ScenarioExecutableFactory
- TripListContent
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
- MapUiModel
- LocationPermissionCoordinator
- Easy Trip v1.0 Pencil 设计落地方案
- TripListViewModel
- Trips
- PlaceCandidate
- Easy Trip v2 交互与视觉升级设计
- Easy Trip UI 设计公式
- Dp
- MemoryPreferences
- 高德 Android SDK 集成决策
- Trips
- Easy Trip 工作台导航重构设计
- RecordingTripRepository
- DeleteImpactDao
- TripWorkspaceRoute.kt
- 2026-08-23-easy-trip-v1-full-ui-implementation.md
- Legs
- WorkspaceSearchReturnTestActivity
- View
- ItineraryScope
- DayItineraryContent
- Batch 2 Gate Report
- FakeRepository
- Legs
- AppNavigation.kt
- UndoAddedItemsUseCase
- WorkspaceOverlay
- Easy Trip 搜索与收藏交互设计
- FakeRepository
- TripService
- PlacePoolViewModel
- FakeTrips
- Batch 2 Room Timeout Diagnosis
- CompactSecondaryButton
- PlaceDao
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
- ItineraryRepository
- FakeTripRepository
- Converters
- Easy Trip Full UI Task 7 状态预研
- MapPoiUi
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest
- ItineraryUiModels.kt
- AmapPlaceDataSource.kt
- .Content
- Legs
- TransportMode
- RoomRouteLegRepository
- OneShotCallback
- CLAUDE.md
- Trips
- Task 5 报告：地点池、详情与地图控件
- AddPlacesRoomIntegrationTest
- FeedbackState
- WorkspaceSection
- ConfirmationDialog
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
- AmapComposeMap
- ItineraryTransactionTest
- PlacePoolContent
- RoomItineraryRepository
- MutableItineraries
- Legs
- formatOccurrenceBadge
- TestSavedPlaces
- 行程项
- EasyTripDatabase
- AddPlacesRequest
- .setContent
- TripDay
- TripWorkspaceNavigationStateTest
- Batch 2 地点池视觉修复报告
- AMap 模拟器 Smoke
- reduceMapInteraction
- Task 6 接口预研：批量加入与撤销
- CreateTripAction
- guard-adb-install.sh
- PlacePoolUiState
- AmapConsentToken
- Batch 2 AMap/EGL 环境诊断
- SDD ledger — plan: docs/superpowers/plans/2026-08-23-easy-trip-v1-full-ui-implementation.md
- SharedPreferences
- WorkspaceSheetLevel
- run-amap-smoke.sh
- V1ScenarioExecutable.kt
- amap-emulator-gate-test.sh
- run-amap-smoke-test.sh
- EasyTripTokens.kt
- Trips
- InsertSide
- Itineraries
- decideCollectionToggle
- TripWorkspaceRoute
- TripWorkspaceAction
- Itineraries
- Places
- Task 7 Phase 2 Report
- CreateTripUiState
- AddPlacesOutcome
- AmapComposeMapTest.kt
- TripListAction
- V1 Full UI Scenario Matrix
- MapView
- amap-emulator-gate.sh
- ItineraryScopeRail
- WholeTripItineraryContent
- DateRangeDeletionCounts
- Shared Fixture
- PermissionExplanationContent
- TravelMode
- DateRangeApply
- LocationPermissionCoordinator.kt
- CreateTripContent
- MapLayer
- AmapServiceException
- AmapSmokeTest
- EditingPlaces
- EasyTripApplication
- Task 6 报告：批量加入与撤销领域用例
- Task 7 第一阶段报告：加入行程状态
- LocationPermissionSourceTest
- Batch 2 搜索受控状态证据契约
- parsePlaces
- AmapMapHost
- AmapMapHost
- RoutePlannerTest
- AmapMapHost
- AmapMapHost
- V1ComposeRule
- MainActivity
- MapPreferences
- TargetDayNotFoundException
- SearchSurface.kt
- .applyDateRange
- .searchCollectionAndMapFlowUsesProductionNavigationState
- .createTrip
- .createTrip
- MapFacade.kt
- .deleteDay

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
- `FakeTrips` --calls--> `TripDay`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `FakeTrips` --calls--> `TripWithDays`  [INFERRED]
  app/src/androidTest/java/com/yangchengwei/easytrip/itinerary/ui/ItineraryEditingTest.kt → app/src/main/java/com/yangchengwei/easytrip/trip/domain/TripRepository.kt
- `EasyTripApplication` --calls--> `AppContainer`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/EasyTripApplication.kt → app/src/main/java/com/yangchengwei/easytrip/AppContainer.kt
- `RoutePlannerTest` --calls--> `GeoPoint`  [INFERRED]
  app/src/test/java/com/yangchengwei/easytrip/route/domain/RoutePlannerTest.kt → app/src/main/java/com/yangchengwei/easytrip/core/model/GeoPoint.kt
- `CompactPrimaryButton()` --calls--> `EasyTripPrimaryButton()`  [INFERRED]
  app/src/main/java/com/yangchengwei/easytrip/core/ui/component/CompactActionButton.kt → app/src/main/java/com/yangchengwei/easytrip/core/ui/component/EasyTripButton.kt

## Import Cycles
- None detected.

## Communities (194 total, 44 thin omitted)

### Community 0 - "Easy Trip v1.0 全量 UI 与交互落地设计"
Cohesion: 0.04
Nodes (46): 10.1 批次 1：旅行入口与创建, 10.2 批次 2：搜索、连续收藏与地点池, 10.3 批次 3：从地点池加入行程与旅行日, 10.4 批次 4：单日编辑、交通路段与全程, 10.5 批次 5：设置、日期、权限与完整回归, 10. 五个实施批次, 11.1 本地优先, 11.2 收藏 (+38 more)

### Community 1 - "MapUiModelMapperTest"
Cohesion: 0.20
Nodes (3): CorruptRoute, mapViewportPoints(), MapUiModelMapperTest

### Community 2 - "FakeRepository"
Cohesion: 0.09
Nodes (21): connected(), hasValidatedInternet(), StateFlow, NetworkMonitor, onAvailable(), onCapabilitiesChanged(), onLost(), Active (+13 more)

### Community 3 - "FakeTripRepository"
Cohesion: 0.19
Nodes (4): FakeTripRepository, CreateTrip, Flow, TripServiceTest

### Community 4 - "awaitSdkCallback"
Cohesion: 0.10
Nodes (16): awaitSdkCallback(), BridgeState, CANCELLED, COMPLETED, READY, STARTED, STARTING, CallbackBoundary (+8 more)

### Community 5 - "TripSettingsViewModel"
Cohesion: 0.11
Nodes (15): TripSettingsContentTest, DateRangeChangeUiState, TripSettingsContent(), DayUi, Factory, androidx, com, Job (+7 more)

### Community 6 - "Visual Fix Agent Stall 诊断"
Cohesion: 0.06
Nodes (30): 1. Executive summary, 1. Harness 在 tool-result 边界自动结束，最终答复回合缺失, 2. 两次 run timeline, 2. 超大单任务上下文与高频工具链导致空终局/自动收尾概率升高, 3. 停止信号检查, 3. 存在未记录的内部运行时/turn/tool 预算, 4. adb/monkey 失败直接导致第一次停止, 4. 行为模式与次数 (+22 more)

### Community 7 - "PlaceSearchEvidenceTest"
Cohesion: 0.05
Nodes (38): gitBytes(), sha256(), PlaceSearchContentTest, EvidenceCase, ExpectedEvidence, PlaceSearchEvidenceTest, BackIcon(), BookmarkIcon() (+30 more)

### Community 8 - "Trips"
Cohesion: 0.06
Nodes (13): FailingMapPreferences, Itineraries, com, CreateTrip, Flow, ItineraryRepository, StateFlow, Trips (+5 more)

### Community 9 - "V1ScenarioExecutableFactory"
Cohesion: 0.19
Nodes (8): ComposeScenario, com, ScenarioFixture, ScenarioPath, V1ScenarioExecutable, V1ScenarioExecutableFactory, WorkspaceSheetScenarioSpec, DayItineraryUiState

### Community 10 - "TripListContent"
Cohesion: 0.13
Nodes (19): TripListContentTest, InlineStatus(), Modifier, CompactTripRow(), EmptyTrips(), Modifier, LuggageIllustration(), TripCard() (+11 more)

### Community 11 - "RealAmapMapHost"
Cohesion: 0.08
Nodes (11): AmapMapHost, Bounds, android, Modifier, View, MapZoomButton(), RealAmapMapHost, SinglePoint (+3 more)

### Community 12 - "DayItineraryViewModel"
Cohesion: 0.12
Nodes (6): DayItineraryViewModel, Factory, StateFlow, T, ViewModel, ViewModelProvider

### Community 13 - "RoomSavedPlaceRepository"
Cohesion: 0.10
Nodes (9): PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, AppContainer, CoroutineScope, Flow, RoomSavedPlaceRepository (+1 more)

### Community 14 - "RouteLegRepository"
Cohesion: 0.07
Nodes (13): Flow, Flow, Flow, Flow, RouteLegRepository, RouteLegWithEndpoints, Failure, plan() (+5 more)

### Community 15 - "DayItineraryViewModelTest"
Cohesion: 0.06
Nodes (12): Coordinator, DayItineraryViewModelTest, Itineraries, com, CompletableDeferred, CreateTrip, ItineraryRepository, kotlinx (+4 more)

### Community 16 - "GeoPoint"
Cohesion: 0.11
Nodes (10): GeoPoint, Flow, DayItinerary, ItineraryItem, ItineraryPlace, ItineraryRepository, Flow, Result (+2 more)

### Community 17 - "AddToItineraryViewModel"
Cohesion: 0.13
Nodes (11): AddToItineraryViewModel, Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider (+3 more)

### Community 18 - "CreateTrip"
Cohesion: 0.07
Nodes (9): RoomSavedPlaceRepositoryTest, IdFactory, T, MutableClock, RoomTripRepositoryTest, Flow, RoomTripRepository, CreateTrip (+1 more)

### Community 19 - "V1ScenarioMetadataTest"
Cohesion: 0.06
Nodes (19): V1FullUiAcceptanceTest, V1ScenarioCatalogTest, V1ScenarioMetadataTest, BlockerCategory, BASIC_ACCESSIBILITY, CRASH_FREE, DATA_CONSISTENCY, FUNCTIONAL_STATE (+11 more)

### Community 21 - "Easy Trip 一期设计"
Cohesion: 0.17
Nodes (12): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 离线路线等待与联网续算, 同一地点可重复安排行程 (+4 more)

### Community 22 - "TripDao"
Cohesion: 0.12
Nodes (6): CreateTrip, Flow, TripDao, TripEntityWithDays, TripDayEntity, TripEntity

### Community 23 - "MapUiModel"
Cohesion: 0.15
Nodes (4): AmapMapHost, AmapMapHost, RecordingHost, MapUiModel

### Community 24 - "LocationPermissionCoordinator"
Cohesion: 0.21
Nodes (4): T, LocationPermissionCoordinator, LocationPermissionSnapshot, LocationPermissionCoordinatorTest

### Community 25 - "Easy Trip v1.0 Pencil 设计落地方案"
Cohesion: 0.05
Nodes (39): 10.1 JVM, 10.2 Compose instrumentation, 10.3 数据与导航集成, 10.4 验证命令, 10. 测试策略, 11. 视觉验收, 12. 验收标准, 13. 协作边界 (+31 more)

### Community 26 - "TripListViewModel"
Cohesion: 0.15
Nodes (11): Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider, OpenSettings (+3 more)

### Community 27 - "Trips"
Cohesion: 0.06
Nodes (10): DayItinerarySelectionTest, Itineraries, com, CompletableDeferred, CreateTrip, Flow, ItineraryRepository, Trips (+2 more)

### Community 28 - "PlaceCandidate"
Cohesion: 0.15
Nodes (10): PlaceCandidate, AlreadySaved, Flow, PlaceTag, Saved, SavedPlace, SavedPlaceRepository, SavePlaceResult (+2 more)

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.17
Nodes (15): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+7 more)

### Community 30 - "Easy Trip UI 设计公式"
Cohesion: 0.05
Nodes (43): 10.1 无网, 10.2 路线失败, 10.3 地图加载中或失败, 10.4 部分成功, 10.5 提交中与目标日失效, 10.6 保存失败, 10. 本地优先与状态公式, 11.1 地图服务授权 (+35 more)

### Community 31 - "Dp"
Cohesion: 0.09
Nodes (18): SelectablePillTest, EasyTripIconButton(), Modifier, EmptyState(), Modifier, Modifier, Role, SelectablePill() (+10 more)

### Community 32 - "MemoryPreferences"
Cohesion: 0.17
Nodes (3): SharedPreferencesMapPreferences, MapPreferencesTest, MemoryPreferences

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.14
Nodes (14): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+6 more)

### Community 34 - "Trips"
Cohesion: 0.14
Nodes (3): com, CreateTrip, Trips

### Community 35 - "Easy Trip 工作台导航重构设计"
Cohesion: 0.06
Nodes (33): 10. 数据模型, 11. 空态与错误处理, 12. 可访问性, 13. 测试策略, 14. 验收标准, 1. 目标, 2. 非目标, 3. 信息架构 (+25 more)

### Community 36 - "RecordingTripRepository"
Cohesion: 0.14
Nodes (4): com, CreateTrip, Flow, RecordingTripRepository

### Community 37 - "DeleteImpactDao"
Cohesion: 0.16
Nodes (4): DeleteImpactDao, RoomDeleteImpactProvider, DeleteImpactProvider, TripDeleteImpact

### Community 38 - "TripWorkspaceRoute.kt"
Cohesion: 0.18
Nodes (13): ConfirmationUiModel, CrossDayMoveDraft, ItineraryDeleteConfirmation, ItineraryEditDraft, RouteModeEditDraft, addOverlayToPresent(), canDismissAddOverlay(), canDismissWorkspaceOverlay() (+5 more)

### Community 39 - "2026-08-23-easy-trip-v1-full-ui-implementation.md"
Cohesion: 0.07
Nodes (27): Batch 1 Gate, Batch 1：旅行入口、创建与删除, Batch 2 Gate, Batch 2：搜索、连续收藏与地点池, Batch 3 Gate, Batch 3：从地点池加入行程与旅行日, Batch 4 Gate, Batch 4：单日编辑、RouteLeg、全程与抽屉 (+19 more)

### Community 40 - "Legs"
Cohesion: 0.05
Nodes (8): Itineraries, com, CreateTrip, ItineraryRepository, Legs, Places, Trips, WorkspaceSearchTabsTest

### Community 41 - "WorkspaceSearchReturnTestActivity"
Cohesion: 0.16
Nodes (8): ClearedProbe, ViewModel, WorkspaceSearchReturnNavEntryTest, Bundle, ComponentActivity, WorkspaceSearchReturnTestActivity, NavBackStackEntry, NavHostController

### Community 42 - "View"
Cohesion: 0.09
Nodes (6): AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, View

### Community 43 - "ItineraryScope"
Cohesion: 0.15
Nodes (10): Day, decodeItineraryScope(), encodeItineraryScope(), ItineraryScope, reconcileItineraryScope(), RestoredWorkspaceNavigation, restoreWorkspaceNavigation(), toMapScope() (+2 more)

### Community 44 - "DayItineraryContent"
Cohesion: 0.15
Nodes (21): AddPlace, AddPlaces, AppendTripDay, CommitMove, ConfirmDelete, DayItineraryAction, DayItineraryContent(), DismissDialogs (+13 more)

### Community 45 - "Batch 2 Gate Report"
Cohesion: 0.06
Nodes (34): 1. Graphify 定向查询, 2. 全套 JVM、lint、assemble, 3. 设备检查, 4. Task 4/5 目标 Compose 与相关 Room 测试, adb/uiautomator 断言, API 36 恢复环境生产旅程 B（2026-08-24）, Batch 2 Gate Report, Concerns (+26 more)

### Community 46 - "FakeRepository"
Cohesion: 0.15
Nodes (5): TripDateRangeService, FakeRepository, CreateTrip, Flow, TripDateRangeServiceTest

### Community 48 - "AppNavigation.kt"
Cohesion: 0.13
Nodes (15): AppNavigation(), AppNavigationDependencies, AppNavigationObserver, consumeWorkspaceSearchReturn(), android, androidx, com, publishWorkspaceSearchReturn() (+7 more)

### Community 49 - "UndoAddedItemsUseCase"
Cohesion: 0.36
Nodes (4): UndoAddedItemsOutcome, UndoAddedItemsRequest, UndoAddedItemsUseCase, UndoAddedItemsUseCaseTest

### Community 50 - "WorkspaceOverlay"
Cohesion: 0.08
Nodes (23): AppendDayCompletionDecision, CloseOverlayAndConsume, Consume, None, WorkspaceBackDecision, CloseOverlay, Ignore, LeaveWorkspace (+15 more)

### Community 51 - "Easy Trip 搜索与收藏交互设计"
Cohesion: 0.07
Nodes (27): 10. 边界与错误处理, 11. 测试策略, 12. 验收标准, 1. 背景与目标, 2. 非目标, 3. 当前实现基线, 4.1 工作台, 4.2 独立搜索页 (+19 more)

### Community 52 - "FakeRepository"
Cohesion: 0.17
Nodes (5): CreateTripViewModelTest, FakeRepository, FakeRepository, Flow, SavedStateHandle

### Community 53 - "TripService"
Cohesion: 0.15
Nodes (3): com, CreateTrip, TripService

### Community 54 - "PlacePoolViewModel"
Cohesion: 0.05
Nodes (14): Factory, Job, kotlinx, PlaceSearchDataSource, StateFlow, T, ViewModel, ViewModelProvider (+6 more)

### Community 55 - "FakeTrips"
Cohesion: 0.10
Nodes (12): Add, FakeCoordinator, FakeItineraries, FakeTrips, ItineraryEditingTest, CreateTrip, ItineraryRepository, java (+4 more)

### Community 56 - "Batch 2 Room Timeout Diagnosis"
Cohesion: 0.11
Nodes (18): Batch 2 Room Timeout Diagnosis, GREEN 修复与验证, Phase 1：失败与复现, Phase 2：模式对比, Phase 3：单一假设验证, 仓库内 working Room Flow 模式, 假设, 判定 (+10 more)

### Community 57 - "CompactSecondaryButton"
Cohesion: 0.20
Nodes (15): CompactPrimaryButton(), CompactSecondaryButton(), AddTripDayContent(), EditItineraryItemContent(), Modifier, Modifier, PlaceDetailContent(), Modifier (+7 more)

### Community 58 - "PlaceDao"
Cohesion: 0.07
Nodes (8): RoomDeleteImpactProviderTest, SchemaPlaceDao, Flow, PlaceDao, PlaceSnapshotRow, PlaceUsageRow, SavedPlaceTagCrossRef, TagEntity

### Community 59 - "FakeItineraries"
Cohesion: 0.18
Nodes (6): UndoCreatedItemsBatch, AddToItineraryStateTest, FakeItineraries, ItineraryRepository, SavedStateHandle, FakeItineraries

### Community 60 - "FakeRepository"
Cohesion: 0.17
Nodes (5): FakeRepository, CompletableDeferred, FakeRepository, Flow, TripSettingsViewModelTest

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 62 - "PlaceSearchViewModel"
Cohesion: 0.07
Nodes (22): PendingCollectionRemoval, Back, ConfirmRemoval, DismissRemovalConfirmation, Factory, StateFlow, T, ViewModel (+14 more)

### Community 63 - "TripWorkspaceViewModel"
Cohesion: 0.13
Nodes (10): Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider, SearchResultSelection (+2 more)

### Community 64 - "CreateTripViewModel"
Cohesion: 0.21
Nodes (9): CreateTripViewModel, Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider (+1 more)

### Community 65 - "Batch 2 搜索加载与网络失败可控设备证据方案"
Cohesion: 0.07
Nodes (26): 1. 无设备的生产状态机回归, 2. 明确锁定获准 AVD, 3. 运行专用可控证据测试, 4. 拉取并校验, 5. 视觉对照, A. 生产真实触发, B. 可控状态渲染证据, Batch 2 搜索加载与网络失败可控设备证据方案 (+18 more)

### Community 67 - "CascadeCountDao"
Cohesion: 0.09
Nodes (3): CascadeCountDao, SchemaItineraryDao, SchemaRouteDao

### Community 68 - "TripSummary"
Cohesion: 0.11
Nodes (9): TripSummary, TripListUiModelsTest, com, CompletableDeferred, CreateTrip, Flow, TestImpacts, TestTripRepository (+1 more)

### Community 72 - "Task 4 报告：重做搜索状态机与连续收藏"
Cohesion: 0.13
Nodes (14): Device gate review fix round 1, Fix round 1, Fix round 2, Fix round 3, Fix round 4, Graphify, Pencil 对照, Task 4 报告：重做搜索状态机与连续收藏 (+6 more)

### Community 74 - "ItineraryRepository"
Cohesion: 0.20
Nodes (4): Flow, ItineraryRepository, java, ItineraryRepository

### Community 75 - "FakeTripRepository"
Cohesion: 0.13
Nodes (8): FakeImpacts, FakeTripRepository, com, CreateTrip, Flow, Role, MoveCall, TripFlowTest

### Community 76 - "Converters"
Cohesion: 0.07
Nodes (9): Converters, TimeMode, DATED, DRAFT, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT (+1 more)

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

### Community 83 - "ItineraryUiModels.kt"
Cohesion: 0.21
Nodes (12): Failed, Ready, RouteLegUi, RouteLegUiState, toItineraryItemUi(), toRouteErrorSummary(), toRouteLegUi(), WaitingForNetwork (+4 more)

### Community 86 - "AmapPlaceDataSource.kt"
Cohesion: 0.18
Nodes (7): CallbackBoundary, PoiSearch, CallbackBoundary, com, parsePoiSearchResponse(), AmapPlaceDataSourceTest, PoiResult

### Community 87 - ".Content"
Cohesion: 0.46
Nodes (3): LazyListState, Modifier, PlacePoolScrollbarTest

### Community 88 - "Legs"
Cohesion: 0.05
Nodes (9): Itineraries, com, CreateTrip, ItineraryRepository, Legs, Places, SavedPlaces, SearchMapFocusTest (+1 more)

### Community 89 - "TransportMode"
Cohesion: 0.06
Nodes (16): OfflineRecoveryTest, RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK, TransportMode (+8 more)

### Community 93 - "Trips"
Cohesion: 0.17
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

### Community 97 - "WorkspaceSection"
Cohesion: 0.24
Nodes (7): shouldConsumeSearchReturn(), WorkspaceSection, ITINERARY, PLACE_POOL, TestOwner, WorkspaceSearchReturnNavigationTest, ViewModelStoreOwner

### Community 98 - "ConfirmationDialog"
Cohesion: 0.11
Nodes (23): ConfirmationDialogTest, EasyTripButtonTest, ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton(), CompactDangerButton() (+15 more)

### Community 99 - "LazyScrollbar.kt"
Cohesion: 0.29
Nodes (6): calculateScrollbarThumb(), LazyListState, Modifier, ReadOnlyLazyScrollbar(), ScrollbarThumb, LazyScrollbarGeometryTest

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

### Community 109 - "MapControls"
Cohesion: 0.38
Nodes (4): description(), Modifier, MapControls(), LayerIcon()

### Community 110 - "MapLayerFlowTest"
Cohesion: 0.16
Nodes (4): FakeMapPreferences, Role, MapLayerFlowTest, Places

### Community 111 - "AmapRouteDataSource.kt"
Cohesion: 0.19
Nodes (8): CallbackBoundary, RouteSearch, CallbackBoundary, RouteCallback, BusRouteResult, DriveRouteResult, RideRouteResult, WalkRouteResult

### Community 112 - "AmapComposeMap"
Cohesion: 0.22
Nodes (3): AmapComposeMap(), MapHostCallbackGuard, MapHostCallbackGuardTest

### Community 114 - "PlacePoolContent"
Cohesion: 0.11
Nodes (19): EditSavedPlaceDialog(), ConfirmCollectionRemoval, ConfirmDelete, Delete, DismissDialogs, Edit, Modifier, PlacePoolAction (+11 more)

### Community 115 - "RoomItineraryRepository"
Cohesion: 0.19
Nodes (6): ItineraryItemEntity, ItineraryRepository, RoomItineraryRepository, AdjacencyDiff, Edge, AdjacencyPlannerTest

### Community 119 - "TestSavedPlaces"
Cohesion: 0.16
Nodes (5): com, Flow, com, com, TestSavedPlaces

### Community 120 - "行程项"
Cohesion: 0.67
Nodes (3): 行程项, 路线段, 收藏地点

### Community 121 - "EasyTripDatabase"
Cohesion: 0.09
Nodes (10): AppIconResourceTest, CascadeDeleteTest, TripDateRangeRoomTest, V1AcceptanceTest, V1PencilFlowTest, EasyTripDatabase, com, Clock (+2 more)

### Community 122 - "AddPlacesRequest"
Cohesion: 0.20
Nodes (8): AddPlacesRequest, AddPlacesToDayUseCase, AddCall, AddPlacesToDayUseCaseTest, FakeItineraryRepository, Flow, ItineraryRepository, java

### Community 124 - "TripDay"
Cohesion: 0.10
Nodes (14): mapWholeTripDays(), Flow, TripDay, TripRepository, TripWithDays, DayMapSnapshot, Flow, ItineraryRepository (+6 more)

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

### Community 132 - "PlacePoolUiState"
Cohesion: 0.12
Nodes (23): PlacePoolUiState, Composable, Modifier, TripWorkspaceContent(), WorkspacePageMessage(), WorkspaceReadyContent(), WorkspaceSheetHandle(), WorkspaceTopBar() (+15 more)

### Community 133 - "AmapConsentToken"
Cohesion: 0.12
Nodes (9): AmapConsentToken, AmapPrivacyGate, AmapPrivacyStateMachine, ConsentRegistry, ConsentSnapshot, StateFlow, TestConsentGate, AmapPrivacyStateMachineTest (+1 more)

### Community 134 - "Batch 2 AMap/EGL 环境诊断"
Cohesion: 0.18
Nodes (10): Batch 2 AMap/EGL 环境诊断, 原始失败证据, 后续旅程判断, 恢复验证证据, 最小恢复操作, 根因结论, 状态, 环境对比 (+2 more)

### Community 137 - "WorkspaceSheetLevel"
Cohesion: 0.15
Nodes (12): settledWorkspaceSheetLevel(), Modifier, restoreWorkspaceSheetLevel(), WorkspaceBottomSheet(), workspaceSheetFraction(), workspaceSheetHeightDp(), WorkspaceSheetLevel, COLLAPSED (+4 more)

### Community 138 - "run-amap-smoke.sh"
Cohesion: 0.52
Nodes (5): cleanup(), handle_signal(), process_alive(), same_process_identity(), run-amap-smoke.sh script

### Community 139 - "V1ScenarioExecutable.kt"
Cohesion: 0.16
Nodes (13): PlacePoolFlowTest, AddToItineraryStep, COMPLETED, IDLE, SELECT_PLACES, SELECT_TARGET_DAY, AddToItineraryUiState, Modifier (+5 more)

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
Cohesion: 0.14
Nodes (3): com, CreateTrip, Trips

### Community 144 - "InsertSide"
Cohesion: 0.14
Nodes (7): Flow, Lifecycle, LifecycleOwner, TestOwner, InsertSide, AFTER, BEFORE

### Community 146 - "decideCollectionToggle"
Cohesion: 0.29
Nodes (6): CollectionDecision, Confirm, decideCollectionToggle(), RemoveNow, Save, CollectionTogglePolicyTest

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

### Community 154 - "AmapComposeMapTest.kt"
Cohesion: 0.13
Nodes (9): AmapComposeMapTest, AmapMapHost, Lifecycle, LifecycleOwner, TestOwner, MapMarkerKind, SAVED_ITINERARY, SAVED_PLACE_POOL (+1 more)

### Community 155 - "TripListAction"
Cohesion: 0.33
Nodes (6): CreateTrip, OpenSettings, OpenTrip, RequestDelete, Retry, TripListAction

### Community 156 - "V1 Full UI Scenario Matrix"
Cohesion: 0.33
Nodes (5): Coverage model, Cross-cutting journeys and matrices, Gate policy, Scenario directory, V1 Full UI Scenario Matrix

### Community 157 - "MapView"
Cohesion: 0.27
Nodes (6): AmapMapViewAttachSmokeTest, AmapAttachSmokeActivity, Bundle, ComponentActivity, FrameLayout, MapView

### Community 159 - "ItineraryScopeRail"
Cohesion: 0.21
Nodes (6): ItineraryScopeRailTest, ItineraryScopeRail(), Modifier, ScopeItem(), Modifier, WorkspaceItineraryContent()

### Community 160 - "WholeTripItineraryContent"
Cohesion: 0.14
Nodes (13): WholeTripItineraryContentTest, ItineraryItemCard(), ItineraryItemRow(), Color, Modifier, semanticsActions(), ItineraryPlaceRow(), ItineraryItemUi (+5 more)

### Community 162 - "Shared Fixture"
Cohesion: 0.15
Nodes (12): Expected RED Findings Against `b26bda6`, File Structure, Global Constraints, Self-Review, Shared Fixture, Task 1: 创建真实 Room 基线、重复地点与末尾追加测试, Task 2: 固定类型化单地点恢复与未知基础设施异常传播, Task 3: 模拟批次中途目标日删除并验证完整请求恢复 (+4 more)

### Community 163 - "PermissionExplanationContent"
Cohesion: 0.20
Nodes (7): WorkspacePermissionFlowTest, PermissionCopy, PermissionExplanationContent(), PermissionKind, DEVICE_LOCATION, DEVICE_LOCATION_SETTINGS, MAP_SERVICE_CONSENT

### Community 164 - "TravelMode"
Cohesion: 0.07
Nodes (12): TravelMode, FLEXIBLE, SELF_DRIVE, DayItems, DayItineraryRow, ItineraryDao, Flow, defaultRecommendMode() (+4 more)

### Community 166 - "LocationPermissionCoordinator.kt"
Cohesion: 0.27
Nodes (9): InMemoryLocationPermissionRequestStore, StateFlow, LocationPermissionRequestStore, OpenApplicationSettings, RequestLocationPermission, SavedStateLocationPermissionRequestStore, SharedPreferencesLocationPermissionRequestStore, ShowCurrentLocation (+1 more)

### Community 167 - "CreateTripContent"
Cohesion: 0.39
Nodes (4): CreateTripContentTest, CreateTripContent(), Modifier, StepNumber()

### Community 168 - "MapLayer"
Cohesion: 0.15
Nodes (5): AmapMapHost, TestMapHost, MapLayer, SATELLITE_ROAD, STANDARD

### Community 169 - "AmapServiceException"
Cohesion: 0.15
Nodes (14): AmapServiceException, AmapRouteDataSource, RouteDataSource, parseRouteResult(), RoutePathData, selectUsablePath(), validateRouteRequest(), RouteMode (+6 more)

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

### Community 180 - "RoutePlannerTest"
Cohesion: 0.38
Nodes (3): RouteDataSource, RoutePlannerTest, RouteDataSource

### Community 184 - "MainActivity"
Cohesion: 0.60
Nodes (3): Bundle, ComponentActivity, MainActivity

### Community 185 - "MapPreferences"
Cohesion: 0.53
Nodes (3): InMemoryMapPreferences, StateFlow, MapPreferences

### Community 186 - "TargetDayNotFoundException"
Cohesion: 0.60
Nodes (3): RecoverablePlaceAddException, TargetDayNotFoundException, IllegalArgumentException

### Community 189 - ".searchCollectionAndMapFlowUsesProductionNavigationState"
Cohesion: 0.15
Nodes (6): AmapMapHost, CoroutineScope, PlaceSearchDataSource, RecordingHost, V2AcceptanceTest, PlaceSearchDataSource

### Community 192 - "MapFacade.kt"
Cohesion: 0.31
Nodes (6): MapMarkerUi, MapPolylineUi, MapRouteLabelUi, MapUiModelMapper, OccurrenceUi, routePalette()

## Knowledge Gaps
- **618 isolated node(s):** `guard-adb-install.sh script`, `TRIP_LIST`, `CREATE_TRIP`, `DATE_PICKER`, `WORKSPACE` (+613 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **44 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `reduceMapInteraction`, `MapUiModelMapperTest`, `FakeRepository`, `PlaceSearchEvidenceTest`, `V1ScenarioExecutableFactory`, `V1ScenarioExecutable.kt`, `RealAmapMapHost`, `RoomSavedPlaceRepository`, `RouteLegRepository`, `InsertSide`, `CreateTrip`, `TripWorkspaceRoute`, `decideCollectionToggle`, `MapUiModel`, `AmapComposeMapTest.kt`, `PlaceCandidate`, `AmapServiceException`, `AmapSmokeTest`, `EditingPlaces`, `parsePlaces`, `RoutePlannerTest`, `PlacePoolViewModel`, `FakeTrips`, `.searchCollectionAndMapFlowUsesProductionNavigationState`, `PlaceSearchViewModel`, `TripWorkspaceViewModel`, `MapFacade.kt`, `.model`, `MapPoiUi`, `AmapPlaceDataSource.kt`, `.Content`, `Legs`, `TransportMode`, `RoomRouteLegRepository`, `MapViewportControllerTest`, `AmapRouteDataSource.kt`, `TestSavedPlaces`, `EasyTripDatabase`, `AddPlacesRequest`, `.setContent`, `TripDay`, `TripWorkspaceNavigationStateTest`?**
  _High betweenness centrality (0.120) - this node is a cross-community bridge._
- **Why does `TripRepository` connect `TripDay` to `FakeTripRepository`, `TripSettingsViewModel`, `Trips`, `DayItineraryViewModel`, `Trips`, `InsertSide`, `GeoPoint`, `CreateTrip`, `DayItineraryViewModelTest`, `TripListViewModel`, `Trips`, `PlaceCandidate`, `DateRangeDeletionCounts`, `Trips`, `RecordingTripRepository`, `DateRangeApply`, `Legs`, `FakeRepository`, `AppNavigation.kt`, `FakeRepository`, `FakeTrips`, `FakeRepository`, `TripSummary`, `FakeTripRepository`, `Legs`, `Trips`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **Why does `TripDay` connect `TripDay` to `MapUiModelMapperTest`, `FakeTripRepository`, `Trips`, `V1ScenarioExecutableFactory`, `V1ScenarioExecutable.kt`, `DayItineraryViewModel`, `InsertSide`, `GeoPoint`, `CreateTrip`, `Trips`, `PlaceCandidate`, `Dp`, `ItineraryScopeRail`, `ItineraryScope`, `FakeRepository`, `TripService`, `FakeTrips`, `FakeRepository`, `MapFacade.kt`, `.model`, `FakeTripRepository`, `ItineraryUiModels.kt`, `TripWorkspaceNavigationStateTest`?**
  _High betweenness centrality (0.063) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 5 inferred relationships involving `TripDay` (e.g. with `FakeTrips` and `.trip()`) actually correct?**
  _`TripDay` has 5 INFERRED edges - model-reasoned connections that need verification._
- **What connects `guard-adb-install.sh script`, `TRIP_LIST`, `CREATE_TRIP` to the rest of the system?**
  _618 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Easy Trip v1.0 全量 UI 与交互落地设计` be split into smaller, more focused modules?**
  _Cohesion score 0.0425531914893617 - nodes in this community are weakly interconnected._