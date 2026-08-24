# Graph Report - easy-trip-v1-full-ui-run  (2026-08-25)

## Corpus Check
- 257 files · ~129,311 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3449 nodes · 7247 edges · 168 communities (133 shown, 35 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 475 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f1e9e76c`
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
- ItineraryUiModels.kt
- Trips
- ItineraryTransactionTest
- FakeItineraries
- RealAmapMapHost
- DayItineraryViewModel
- Legs
- AmapComposeMap
- Legs
- RouteStatus
- AddToItineraryViewModel
- CreateTrip
- PlacePoolUiState
- TripListContent.kt
- Easy Trip 一期设计
- TripDao
- TripDay
- TripRepository
- Easy Trip v1.0 Pencil 设计落地方案
- ItineraryRepository
- Trips
- DayItinerary
- Easy Trip v2 交互与视觉升级设计
- Easy Trip UI 设计公式
- AmapServiceException
- MemoryPreferences
- 高德 Android SDK 集成决策
- SelectablePill
- Easy Trip 工作台导航重构设计
- RoomSavedPlaceRepository
- DeleteImpactDao
- DelayedDeletePlaces
- 2026-08-23-easy-trip-v1-full-ui-implementation.md
- Legs
- AppNavigation.kt
- View
- FakeLegs
- DayItineraryAction
- Batch 2 Gate Report
- PlaceDao
- TripSummary
- AddToItineraryUiState
- TripWorkspaceContent.kt
- WorkspaceOverlay
- Easy Trip 搜索与收藏交互设计
- FakeRepository
- RouteLegRepository
- PlacePoolViewModel
- FakeTrips
- Batch 2 Room Timeout Diagnosis
- CompactSecondaryButton
- Converters
- FakeItineraries
- AmapComposeMapTest.kt
- RouteFormattingTest
- PlaceSearchViewModel
- TripWorkspaceViewModel
- CreateTripViewModel
- Batch 2 搜索加载与网络失败可控设备证据方案
- MapLifecycleController
- ItineraryDao
- TripService
- .model
- ItineraryService
- MapLayerRenderingPolicyTest
- Task 4 报告：重做搜索状态机与连续收藏
- decideCollectionToggle
- WholeTripItineraryContent
- FakeTripRepository
- CreateTripAction
- Easy Trip Full UI Task 7 状态预研
- Trips
- gradlew
- Easy Trip
- AppIconManifestTest
- SanityTest
- MapView
- parsePoiSearchResponse
- .Content
- Trips
- PlaceSearchEvidenceTest
- reduceMapInteraction
- EasyTripDatabase
- CLAUDE.md
- TripWorkspaceAction
- Task 5 报告：地点池、详情与地图控件
- AddPlacesRoomIntegrationTest
- Trips
- Trips
- TripWorkspaceNavigationStateTest
- LazyScrollbar.kt
- settledWorkspaceSheetLevel
- CreateTripUiState
- File Structure
- File Structure
- MapViewportControllerTest
- Global Constraints
- Easy Trip v1.0 Pencil Reference
- Batch 1 visual fix report
- TransportMode
- AmapRouteDataSource.kt
- Dp
- CreateTripContent
- AmapSmokeTest
- RecordingHost
- PlacePoolContent
- RoomItineraryRepository
- MapLayer
- Legs
- WorkspaceSearchReturn
- CallbackBoundary
- 行程项
- RoomRouteLegRepository
- AddPlacesRequest
- Trips
- com
- TravelMode
- Batch 2 地点池视觉修复报告
- AMap 模拟器 Smoke
- Legs
- Task 6 接口预研：批量加入与撤销
- MapFacade.kt
- guard-adb-install.sh
- Legs
- AmapConsentToken
- Batch 2 AMap/EGL 环境诊断
- SDD ledger — plan: docs/superpowers/plans/2026-08-23-easy-trip-v1-full-ui-implementation.md
- WorkspaceSearchReturnTestActivity
- CascadeCountDao
- run-amap-smoke.sh
- TestMapHost
- amap-emulator-gate-test.sh
- run-amap-smoke-test.sh
- ItineraryItemRow.kt
- .createTrip
- MapUiModel
- AmapMapHost
- MapViewportRenderingPolicyTest
- Color.kt
- RoutePlannerTest
- Itineraries
- Places
- Task 7 Phase 2 Report
- AppIconResourceTest
- RoomDeleteImpactProviderTest
- ItineraryScope
- MutableItineraries
- MainActivity
- RouteMode
- amap-emulator-gate.sh
- GeoPoint
- Shared Fixture
- AddPlacesOutcome
- UndoAddedItemsUseCase
- parsePlaces
- Task 6 报告：批量加入与撤销领域用例
- Task 7 第一阶段报告：加入行程状态
- Batch 2 搜索受控状态证据契约
- TripListViewModel

## God Nodes (most connected - your core abstractions)
1. `GeoPoint` - 139 edges
2. `PlaceCandidate` - 90 edges
3. `SavedPlace` - 71 edges
4. `TravelMode` - 66 edges
5. `TripDay` - 65 edges
6. `TripWorkspaceViewModel` - 60 edges
7. `CreateTrip` - 54 edges
8. `TransportMode` - 53 edges
9. `TripService` - 52 edges
10. `PlacePoolViewModel` - 47 edges

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

## Communities (168 total, 35 thin omitted)

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
Cohesion: 0.07
Nodes (19): OneShotCallback, awaitSdkCallback(), BridgeState, CANCELLED, COMPLETED, READY, STARTED, STARTING (+11 more)

### Community 5 - "TripSettingsViewModel"
Cohesion: 0.13
Nodes (11): DayDeleteImpact, DayUi, Factory, androidx, StateFlow, T, ViewModel, ViewModelProvider (+3 more)

### Community 6 - "Visual Fix Agent Stall 诊断"
Cohesion: 0.06
Nodes (30): 1. Executive summary, 1. Harness 在 tool-result 边界自动结束，最终答复回合缺失, 2. 两次 run timeline, 2. 超大单任务上下文与高频工具链导致空终局/自动收尾概率升高, 3. 停止信号检查, 3. 存在未记录的内部运行时/turn/tool 预算, 4. adb/monkey 失败直接导致第一次停止, 4. 行为模式与次数 (+22 more)

### Community 7 - "ItineraryUiModels.kt"
Cohesion: 0.33
Nodes (8): RouteLegUi, toItineraryItemUi(), toRouteErrorSummary(), toRouteLegUi(), Modifier, modeLabel(), RouteLegContent(), RouteLegRow()

### Community 8 - "Trips"
Cohesion: 0.06
Nodes (11): Itineraries, com, CreateTrip, Flow, ItineraryRepository, Trips, Legs, Places (+3 more)

### Community 10 - "FakeItineraries"
Cohesion: 0.16
Nodes (8): Add, FakeCoordinator, FakeItineraries, ItineraryEditingTest, ItineraryRepository, kotlinx, Move, Timing

### Community 11 - "RealAmapMapHost"
Cohesion: 0.10
Nodes (6): AmapMapHost, android, RealAmapMapHost, toMapPoiUi(), MapMarkerUi, MarkerDialog()

### Community 12 - "DayItineraryViewModel"
Cohesion: 0.13
Nodes (6): DayItineraryViewModel, Factory, StateFlow, T, ViewModel, ViewModelProvider

### Community 13 - "Legs"
Cohesion: 0.09
Nodes (3): EditingPlaces, com, Legs

### Community 14 - "AmapComposeMap"
Cohesion: 0.14
Nodes (10): AmapComposeMap(), Bounds, Modifier, View, MapHostCallbackGuard, MapZoomButton(), SinglePoint, ViewportCommand (+2 more)

### Community 15 - "Legs"
Cohesion: 0.06
Nodes (9): DayItineraryViewModelTest, Itineraries, CompletableDeferred, CreateTrip, ItineraryRepository, Legs, Timing, Trips (+1 more)

### Community 16 - "RouteStatus"
Cohesion: 0.09
Nodes (9): RouteStatus, CALCULATING, FAILED, PENDING, SUCCESS, WAITING_NETWORK, Flow, RouteLegDao (+1 more)

### Community 17 - "AddToItineraryViewModel"
Cohesion: 0.13
Nodes (11): AddToItineraryViewModel, Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider (+3 more)

### Community 18 - "CreateTrip"
Cohesion: 0.08
Nodes (9): RoomSavedPlaceRepositoryTest, IdFactory, T, MutableClock, RoomTripRepositoryTest, V1PencilFlowTest, CreateTrip, RoomTripRepository (+1 more)

### Community 19 - "PlacePoolUiState"
Cohesion: 0.20
Nodes (7): PlacePoolFlowTest, com, TripWorkspaceContentTest, Modifier, SelectPlacesContent(), PlacePoolUiState, SavedPlaceRowUi

### Community 20 - "TripListContent.kt"
Cohesion: 0.06
Nodes (51): FeedbackStateTest, TripListContentTest, ActionStyle, DANGER, PRIMARY, SECONDARY, CompactActionButton(), CompactDangerButton() (+43 more)

### Community 21 - "Easy Trip 一期设计"
Cohesion: 0.17
Nodes (12): Easy Trip v1 实施计划, 地图优先旅行工作台, Room 唯一持久化事实源, 路线版本过期响应保护, 事务化邻接路段维护, Easy Trip 一期设计, 离线路线等待与联网续算, 同一地点可重复安排行程 (+4 more)

### Community 22 - "TripDao"
Cohesion: 0.07
Nodes (9): Fixture, SchemaTest, TimeMode, DATED, DRAFT, Flow, TripDao, TripEntityWithDays (+1 more)

### Community 23 - "TripDay"
Cohesion: 0.18
Nodes (9): ItineraryScopeRailTest, ItineraryScopeRail(), Modifier, ScopeItem(), Modifier, SelectTargetDayContent(), Modifier, WorkspaceItineraryContent() (+1 more)

### Community 24 - "TripRepository"
Cohesion: 0.11
Nodes (5): InsertSide, AFTER, BEFORE, Flow, TripRepository

### Community 25 - "Easy Trip v1.0 Pencil 设计落地方案"
Cohesion: 0.05
Nodes (39): 10.1 JVM, 10.2 Compose instrumentation, 10.3 数据与导航集成, 10.4 验证命令, 10. 测试策略, 11. 视觉验收, 12. 验收标准, 13. 协作边界 (+31 more)

### Community 26 - "ItineraryRepository"
Cohesion: 0.20
Nodes (4): Flow, ItineraryRepository, java, ItineraryRepository

### Community 27 - "Trips"
Cohesion: 0.06
Nodes (9): DayItinerarySelectionTest, Itineraries, CompletableDeferred, CreateTrip, Flow, ItineraryRepository, Trips, Legs (+1 more)

### Community 28 - "DayItinerary"
Cohesion: 0.22
Nodes (8): Flow, DayItinerary, ItineraryItem, ItineraryPlace, Flow, mapWholeTripDays(), DayMapSnapshot, WholeTripItineraryMapperTest

### Community 29 - "Easy Trip v2 交互与视觉升级设计"
Cohesion: 0.17
Nodes (15): 卡片式行程时间轴, 确定性地图视野适配, Easy Trip v2 实施计划, 全局地图图层偏好, 搜索结果聚焦与高亮, 统一可选择控件视觉语言, 定位点与路线 Adaptive Icon, Easy Trip v2 交互与视觉升级设计 (+7 more)

### Community 30 - "Easy Trip UI 设计公式"
Cohesion: 0.05
Nodes (43): 10.1 无网, 10.2 路线失败, 10.3 地图加载中或失败, 10.4 部分成功, 10.5 提交中与目标日失效, 10.6 保存失败, 10. 本地优先与状态公式, 11.1 地图服务授权 (+35 more)

### Community 31 - "AmapServiceException"
Cohesion: 0.20
Nodes (10): AmapServiceException, AmapRouteDataSource, RouteDataSource, parseRouteResult(), RoutePathData, selectUsablePath(), validateRouteRequest(), RouteRequest (+2 more)

### Community 32 - "MemoryPreferences"
Cohesion: 0.13
Nodes (4): Editor, MapPreferencesTest, MemoryPreferences, SharedPreferences

### Community 33 - "高德 Android SDK 集成决策"
Cohesion: 0.14
Nodes (14): 项目约定, 高德 Android SDK 集成决策, ADR 0001：高德 Android SDK 集成, 显式隐私授权门控, 路线成功结果完整性校验, 单一合并高德依赖, Domain Docs, 显式标记 ADR 冲突 (+6 more)

### Community 34 - "SelectablePill"
Cohesion: 0.17
Nodes (10): SelectablePillTest, Modifier, Role, SelectablePill(), SelectablePillStyle, DaySelector(), LazyListState, Modifier (+2 more)

### Community 35 - "Easy Trip 工作台导航重构设计"
Cohesion: 0.06
Nodes (33): 10. 数据模型, 11. 空态与错误处理, 12. 可访问性, 13. 测试策略, 14. 验收标准, 1. 目标, 2. 非目标, 3. 信息架构 (+25 more)

### Community 36 - "RoomSavedPlaceRepository"
Cohesion: 0.09
Nodes (11): PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, PlaceSearchDataSource, V2AcceptanceTest, PlaceSearchDataSource, AmapPrivacyGate (+3 more)

### Community 39 - "2026-08-23-easy-trip-v1-full-ui-implementation.md"
Cohesion: 0.07
Nodes (27): Batch 1 Gate, Batch 1：旅行入口、创建与删除, Batch 2 Gate, Batch 2：搜索、连续收藏与地点池, Batch 3 Gate, Batch 3：从地点池加入行程与旅行日, Batch 4 Gate, Batch 4：单日编辑、RouteLeg、全程与抽屉 (+19 more)

### Community 40 - "Legs"
Cohesion: 0.07
Nodes (6): Itineraries, com, ItineraryRepository, Legs, Places, WorkspaceSearchTabsTest

### Community 41 - "AppNavigation.kt"
Cohesion: 0.21
Nodes (11): AppNavigation(), AppNavigationDependencies, AppNavigationObserver, android, com, tripSearchRoute(), WorkspaceSearchReturnViewModel, AmapPlaceDataSource (+3 more)

### Community 42 - "View"
Cohesion: 0.07
Nodes (7): AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, AmapMapHost, View

### Community 44 - "DayItineraryAction"
Cohesion: 0.15
Nodes (22): AddPlace, AddPlaces, AppendTripDay, CommitMove, ConfirmDelete, DayItineraryAction, DayItineraryContent(), DayItinerarySheet() (+14 more)

### Community 45 - "Batch 2 Gate Report"
Cohesion: 0.06
Nodes (34): 1. Graphify 定向查询, 2. 全套 JVM、lint、assemble, 3. 设备检查, 4. Task 4/5 目标 Compose 与相关 Room 测试, adb/uiautomator 断言, API 36 恢复环境生产旅程 B（2026-08-24）, Batch 2 Gate Report, Concerns (+26 more)

### Community 46 - "PlaceDao"
Cohesion: 0.13
Nodes (4): Flow, PlaceDao, PlaceSnapshotRow, PlaceUsageRow

### Community 47 - "TripSummary"
Cohesion: 0.14
Nodes (7): Flow, Flow, TripSummary, TripWithDays, Flow, TripListUiModelsTest, Flow

### Community 48 - "AddToItineraryUiState"
Cohesion: 0.16
Nodes (18): ConfirmationUiModel, AddToItineraryUiState, DayItineraryUiState, ItineraryDeleteConfirmation, ItineraryEditDraft, addOverlayToPresent(), canDismissAddOverlay(), canDismissWorkspaceOverlay() (+10 more)

### Community 49 - "TripWorkspaceContent.kt"
Cohesion: 0.11
Nodes (24): description(), Modifier, MapControls(), Composable, Modifier, LayerIcon(), toSheetValue(), TripWorkspaceContent() (+16 more)

### Community 50 - "WorkspaceOverlay"
Cohesion: 0.08
Nodes (22): AppendDayCompletionDecision, CloseOverlayAndConsume, Consume, None, AddToItineraryResult, AddTripDay, Confirmation, EditItineraryItem (+14 more)

### Community 51 - "Easy Trip 搜索与收藏交互设计"
Cohesion: 0.07
Nodes (27): 10. 边界与错误处理, 11. 测试策略, 12. 验收标准, 1. 背景与目标, 2. 非目标, 3. 当前实现基线, 4.1 工作台, 4.2 独立搜索页 (+19 more)

### Community 52 - "FakeRepository"
Cohesion: 0.19
Nodes (4): CreateTripViewModelTest, FakeRepository, SavedStateHandle, FakeRepository

### Community 53 - "RouteLegRepository"
Cohesion: 0.07
Nodes (14): Flow, Flow, Flow, Flow, Flow, RouteLegRepository, RouteLegWithEndpoints, Failure (+6 more)

### Community 54 - "PlacePoolViewModel"
Cohesion: 0.05
Nodes (15): Factory, Job, kotlinx, PlaceSearchDataSource, StateFlow, T, ViewModel, ViewModelProvider (+7 more)

### Community 55 - "FakeTrips"
Cohesion: 0.18
Nodes (3): FakeTrips, CreateTrip, java

### Community 56 - "Batch 2 Room Timeout Diagnosis"
Cohesion: 0.11
Nodes (18): Batch 2 Room Timeout Diagnosis, GREEN 修复与验证, Phase 1：失败与复现, Phase 2：模式对比, Phase 3：单一假设验证, 仓库内 working Room Flow 模式, 假设, 判定 (+10 more)

### Community 57 - "CompactSecondaryButton"
Cohesion: 0.19
Nodes (15): CompactPrimaryButton(), CompactSecondaryButton(), AddTripDayContent(), EditItineraryItemContent(), Modifier, Modifier, PlaceDetailContent(), Modifier (+7 more)

### Community 58 - "Converters"
Cohesion: 0.09
Nodes (6): Converters, RouteErrorKind, NO_ROUTE, PERMANENT, TRANSIENT, UNSUPPORTED_TRANSIT

### Community 59 - "FakeItineraries"
Cohesion: 0.18
Nodes (6): UndoCreatedItemsBatch, AddToItineraryStateTest, FakeItineraries, ItineraryRepository, SavedStateHandle, FakeItineraries

### Community 60 - "AmapComposeMapTest.kt"
Cohesion: 0.17
Nodes (10): AmapComposeMapTest, Lifecycle, LifecycleOwner, TestOwner, ViewportReason, INITIAL, PLACE_SET_CHANGED, SCOPE_CHANGED (+2 more)

### Community 61 - "RouteFormattingTest"
Cohesion: 0.33
Nodes (3): formatDistance(), formatDuration(), RouteFormattingTest

### Community 62 - "PlaceSearchViewModel"
Cohesion: 0.07
Nodes (22): PendingCollectionRemoval, Back, ConfirmRemoval, DismissRemovalConfirmation, Factory, StateFlow, T, ViewModel (+14 more)

### Community 63 - "TripWorkspaceViewModel"
Cohesion: 0.09
Nodes (21): Factory, CreationExtras, Flow, ItineraryRepository, Job, StateFlow, T, ViewModel (+13 more)

### Community 64 - "CreateTripViewModel"
Cohesion: 0.23
Nodes (8): CreateTripViewModel, Factory, CreationExtras, Job, StateFlow, T, ViewModel, ViewModelProvider

### Community 65 - "Batch 2 搜索加载与网络失败可控设备证据方案"
Cohesion: 0.07
Nodes (26): 1. 无设备的生产状态机回归, 2. 明确锁定获准 AVD, 3. 运行专用可控证据测试, 4. 拉取并校验, 5. 视觉对照, A. 生产真实触发, B. 可控状态渲染证据, Batch 2 搜索加载与网络失败可控设备证据方案 (+18 more)

### Community 67 - "ItineraryDao"
Cohesion: 0.14
Nodes (4): DayItems, DayItineraryRow, ItineraryDao, Flow

### Community 68 - "TripService"
Cohesion: 0.09
Nodes (8): com, CreateTrip, TripService, CompletableDeferred, CreateTrip, TestImpacts, TestTripRepository, TripListViewModelTest

### Community 72 - "Task 4 报告：重做搜索状态机与连续收藏"
Cohesion: 0.13
Nodes (14): Device gate review fix round 1, Fix round 1, Fix round 2, Fix round 3, Fix round 4, Graphify, Pencil 对照, Task 4 报告：重做搜索状态机与连续收藏 (+6 more)

### Community 73 - "decideCollectionToggle"
Cohesion: 0.29
Nodes (6): CollectionDecision, Confirm, decideCollectionToggle(), RemoveNow, Save, CollectionTogglePolicyTest

### Community 74 - "WholeTripItineraryContent"
Cohesion: 0.27
Nodes (5): WholeTripItineraryContentTest, WholeTripDayUi, dayHeading(), Modifier, WholeTripItineraryContent()

### Community 75 - "FakeTripRepository"
Cohesion: 0.14
Nodes (7): FakeImpacts, FakeTripRepository, CreateTrip, Flow, Role, MoveCall, TripFlowTest

### Community 76 - "CreateTripAction"
Cohesion: 0.13
Nodes (14): Back, CreateTimeMode, DATED, DRAFT, CreateTripAction, CreateTripEffect, DayCountChanged, NameChanged (+6 more)

### Community 77 - "Easy Trip Full UI Task 7 状态预研"
Cohesion: 0.08
Nodes (25): Compose / Route, Easy Trip Full UI Task 7 状态预研, JVM 状态/reducer, partial success 与 undo 语义, SavedStateHandle 键与恢复, scheduled 与 selectedPlaceIds 分离, Task 6 依赖契约, Task 6 当前阻塞 (+17 more)

### Community 78 - "Trips"
Cohesion: 0.08
Nodes (9): Itineraries, AmapMapHost, CreateTrip, ItineraryRepository, Places, PoiHost, Trips, WorkspaceFlowTest (+1 more)

### Community 79 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 80 - "Easy Trip"
Cohesion: 0.50
Nodes (4): 高德地图本地密钥配置, Easy Trip, 本地优先 Android 旅行规划, 未授权或离线本地编辑

### Community 83 - "MapView"
Cohesion: 0.27
Nodes (6): AmapMapViewAttachSmokeTest, AmapAttachSmokeActivity, Bundle, ComponentActivity, FrameLayout, MapView

### Community 86 - "parsePoiSearchResponse"
Cohesion: 0.17
Nodes (7): CallbackBoundary, PoiSearch, CallbackBoundary, com, parsePoiSearchResponse(), AmapPlaceDataSourceTest, PoiResult

### Community 87 - ".Content"
Cohesion: 0.46
Nodes (3): LazyListState, Modifier, PlacePoolScrollbarTest

### Community 89 - "PlaceSearchEvidenceTest"
Cohesion: 0.05
Nodes (38): gitBytes(), sha256(), PlaceSearchContentTest, EvidenceCase, ExpectedEvidence, PlaceSearchEvidenceTest, BackIcon(), BookmarkIcon() (+30 more)

### Community 90 - "reduceMapInteraction"
Cohesion: 0.27
Nodes (6): FocusSearchResult, MapInteractionAction, MapInteractionState, ReconcileSearchResults, reduceMapInteraction(), MapInteractionReducerTest

### Community 91 - "EasyTripDatabase"
Cohesion: 0.07
Nodes (14): OfflineRecoveryTest, EasyTripDatabase, com, SchemaItineraryDao, SchemaPlaceDao, SchemaRouteDao, ItineraryItemEntity, SavedPlaceEntity (+6 more)

### Community 93 - "TripWorkspaceAction"
Cohesion: 0.17
Nodes (12): Back, CloseOverlay, OpenOverlay, OpenPrivacySettings, OpenSearch, OpenSettings, Retry, SelectItineraryScope (+4 more)

### Community 94 - "Task 5 报告：地点池、详情与地图控件"
Cohesion: 0.17
Nodes (11): Fix round 1, Fix round 2：Room 测试调度竞态, Graphify, Pencil 对照, Task 5 报告：地点池、详情与地图控件, TDD 证据, 修改文件, 关注点 (+3 more)

### Community 95 - "AddPlacesRoomIntegrationTest"
Cohesion: 0.24
Nodes (4): AddPlacesRoomIntegrationTest, DeleteTargetDayAfterFirstAddRepository, ItineraryRepository, SequenceIds

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
Cohesion: 0.16
Nodes (3): MapViewportRequest, MapViewportController, MapViewportControllerTest

### Community 105 - "Global Constraints"
Cohesion: 0.20
Nodes (9): Easy Trip Search Collection Implementation Plan, Global Constraints, Task 1: 收敛工作台 Tab 并建立独立搜索路由, Task 2: 完成搜索页状态清理与一次性返回聚焦, Task 3: 统一搜索结果与地点卡片的收藏切换策略, Task 4: 建立稳定的收藏 marker 身份、编号与聚焦样式, Task 5: 接入高德底图 POI 点击与统一地点卡片, Task 6: 为地点池增加只读纵向 scrollbar thumb (+1 more)

### Community 106 - "Easy Trip v1.0 Pencil Reference"
Cohesion: 0.18
Nodes (10): Easy Trip v1.0 Pencil Reference, Layout Verification, Main Flow Frames, Public Components, Responsive Notes, Screenshot Baseline, Semantic Tokens, Shape, Spacing, Size, Elevation (+2 more)

### Community 107 - "Batch 1 visual fix report"
Cohesion: 0.20
Nodes (9): Batch 1 visual fix report, Commit, Fix round 1, Fix round 1 commit, Fix round 1 最终验证, Frame 差异与修复, RED / GREEN, 修改文件 (+1 more)

### Community 108 - "TransportMode"
Cohesion: 0.10
Nodes (13): TransportMode, DRIVE, TAXI, TRANSIT, WALK, defaultRecommendMode(), haversineMeters(), AdjacencyDiff (+5 more)

### Community 109 - "AmapRouteDataSource.kt"
Cohesion: 0.33
Nodes (6): RouteSearch, RouteCallback, BusRouteResult, DriveRouteResult, RideRouteResult, WalkRouteResult

### Community 110 - "Dp"
Cohesion: 0.11
Nodes (11): ConfirmationDialogTest, EasyTripButtonTest, EasyTripIconButton(), Modifier, EmptyState(), Modifier, EasyTripElevation, EasyTripSizes (+3 more)

### Community 111 - "CreateTripContent"
Cohesion: 0.39
Nodes (4): CreateTripContentTest, CreateTripContent(), Modifier, StepNumber()

### Community 114 - "PlacePoolContent"
Cohesion: 0.13
Nodes (17): ConfirmCollectionRemoval, ConfirmDelete, Delete, DismissDialogs, Edit, Modifier, PlacePoolAction, PlacePoolContent() (+9 more)

### Community 115 - "RoomItineraryRepository"
Cohesion: 0.16
Nodes (4): CascadeDeleteTest, ItineraryRepository, RoomItineraryRepository, PlaceService

### Community 116 - "MapLayer"
Cohesion: 0.15
Nodes (11): AmapMapHost, RecordingHost, MapLayer, SATELLITE_ROAD, STANDARD, InMemoryMapPreferences, StateFlow, MapPreferences (+3 more)

### Community 118 - "WorkspaceSearchReturn"
Cohesion: 0.18
Nodes (8): WorkspaceSearchReturnNavEntryTest, consumeWorkspaceSearchReturn(), androidx, publishWorkspaceSearchReturn(), WorkspaceSearchReturn, TestOwner, WorkspaceSearchReturnNavigationTest, ViewModelStoreOwner

### Community 120 - "行程项"
Cohesion: 0.67
Nodes (3): 行程项, 路线段, 收藏地点

### Community 121 - "RoomRouteLegRepository"
Cohesion: 0.09
Nodes (5): RoomRouteLegRepositoryTest, V1AcceptanceTest, RoomRouteLegRepository, RouteDataSource, RouteResult

### Community 122 - "AddPlacesRequest"
Cohesion: 0.16
Nodes (10): AddPlacesRequest, AddPlacesToDayUseCase, RecoverablePlaceAddException, TargetDayNotFoundException, AddCall, AddPlacesToDayUseCaseTest, FakeItineraryRepository, Flow (+2 more)

### Community 124 - "com"
Cohesion: 0.40
Nodes (3): com, com, com

### Community 125 - "TravelMode"
Cohesion: 0.11
Nodes (5): CreateTrip, RecordingTripRepository, TravelMode, FLEXIBLE, SELF_DRIVE

### Community 126 - "Batch 2 地点池视觉修复报告"
Cohesion: 0.14
Nodes (13): Batch 2 地点池视觉修复报告, Concerns, Diff 审计, Findings 修复, Fix round 1（2026-08-24）, Fix round 2（2026-08-24）, Fix round 3（2026-08-24）, GREEN (+5 more)

### Community 127 - "AMap 模拟器 Smoke"
Cohesion: 0.33
Nodes (5): AMap 模拟器 Smoke, Smoke 行为, 停止与恢复, 固定环境, 门禁与互斥

### Community 128 - "Legs"
Cohesion: 0.06
Nodes (10): Itineraries, com, ItineraryRepository, Lifecycle, LifecycleOwner, Legs, Places, SavedPlaces (+2 more)

### Community 129 - "Task 6 接口预研：批量加入与撤销"
Cohesion: 0.10
Nodes (19): AddPlacesToDayUseCase, Domain Repository, Room/DAO, Task 6 接口预研：批量加入与撤销, UndoAddedItemsUseCase, 建议文件列表, 接口缺口与建议边界, 明确禁止项 (+11 more)

### Community 130 - "MapFacade.kt"
Cohesion: 0.12
Nodes (16): CorruptRoute, formatOccurrenceBadge(), MapMarkerKind, SAVED_ITINERARY, SAVED_PLACE_POOL, UNSAVED_SEARCH, MapPolylineUi, MapRouteLabelUi (+8 more)

### Community 132 - "Legs"
Cohesion: 0.06
Nodes (10): FakeMapPreferences, Itineraries, com, ItineraryRepository, Role, Legs, MapLayerFlowTest, Places (+2 more)

### Community 133 - "AmapConsentToken"
Cohesion: 0.09
Nodes (12): AmapConsentToken, AmapPrivacyStateMachine, ConsentRegistry, ConsentSnapshot, StateFlow, TestConsentGate, AppContainer, CoroutineScope (+4 more)

### Community 134 - "Batch 2 AMap/EGL 环境诊断"
Cohesion: 0.18
Nodes (10): Batch 2 AMap/EGL 环境诊断, 原始失败证据, 后续旅程判断, 恢复验证证据, 最小恢复操作, 根因结论, 状态, 环境对比 (+2 more)

### Community 136 - "WorkspaceSearchReturnTestActivity"
Cohesion: 0.21
Nodes (7): ClearedProbe, ViewModel, Bundle, ComponentActivity, WorkspaceSearchReturnTestActivity, NavBackStackEntry, NavHostController

### Community 138 - "run-amap-smoke.sh"
Cohesion: 0.53
Nodes (4): cleanup(), handle_signal(), same_process_identity(), run-amap-smoke.sh script

### Community 140 - "amap-emulator-gate-test.sh"
Cohesion: 0.80
Nodes (4): expect_rejected(), fail(), amap-emulator-gate-test.sh script, write_adb()

### Community 141 - "run-amap-smoke-test.sh"
Cohesion: 0.70
Nodes (4): fail(), make_fixture(), run_status(), run-amap-smoke-test.sh script

### Community 142 - "ItineraryItemRow.kt"
Cohesion: 0.39
Nodes (7): ItineraryItemCard(), ItineraryItemRow(), Color, Modifier, semanticsActions(), ItineraryPlaceRow(), ItineraryItemUi

### Community 144 - "MapUiModel"
Cohesion: 0.09
Nodes (5): AmapMapHost, AmapMapHost, AmapMapHost, MapUiModel, mapViewportPoints()

### Community 148 - "RoutePlannerTest"
Cohesion: 0.38
Nodes (3): RouteDataSource, RoutePlannerTest, RouteDataSource

### Community 151 - "Task 7 Phase 2 Report"
Cohesion: 0.29
Nodes (6): Task 7 Phase 2 Report, TDD, 实现, 未覆盖风险, 状态, 验证

### Community 154 - "ItineraryScope"
Cohesion: 0.12
Nodes (13): Day, decodeItineraryScope(), encodeItineraryScope(), ItineraryScope, reconcileItineraryScope(), RestoredWorkspaceNavigation, restoreWorkspaceNavigation(), toMapScope() (+5 more)

### Community 156 - "MainActivity"
Cohesion: 0.60
Nodes (3): Bundle, ComponentActivity, MainActivity

### Community 157 - "RouteMode"
Cohesion: 0.40
Nodes (4): RouteMode, DRIVE, TRANSIT, WALK

### Community 159 - "GeoPoint"
Cohesion: 0.07
Nodes (17): Flow, TestSavedPlaces, GeoPoint, ItineraryRepository, PlaceCandidate, PlaceSearchDataSource, AlreadySaved, Flow (+9 more)

### Community 162 - "Shared Fixture"
Cohesion: 0.15
Nodes (12): Expected RED Findings Against `b26bda6`, File Structure, Global Constraints, Self-Review, Shared Fixture, Task 1: 创建真实 Room 基线、重复地点与末尾追加测试, Task 2: 固定类型化单地点恢复与未知基础设施异常传播, Task 3: 模拟批次中途目标日删除并验证完整请求恢复 (+4 more)

### Community 169 - "AddPlacesOutcome"
Cohesion: 0.18
Nodes (12): AddPlacesOutcome, PartialSuccess, Success, TargetDayMissing, AddToItineraryEditingTarget, AddToItineraryStep, COMPLETED, IDLE (+4 more)

### Community 170 - "UndoAddedItemsUseCase"
Cohesion: 0.24
Nodes (5): UndoAddedItemsOutcome, UndoAddedItemsRequest, UndoAddedItemsUseCase, UndoAddedItemsUseCaseTest, Flow

### Community 172 - "parsePlaces"
Cohesion: 0.43
Nodes (3): parsePlaces(), RawPlace, PlaceContractsTest

### Community 173 - "Task 6 报告：批量加入与撤销领域用例"
Cohesion: 0.25
Nodes (7): Fix 1, Room gate, Task 6 报告：批量加入与撤销领域用例, TDD / JVM, 实现, 最终状态, 未包含与风险

### Community 174 - "Task 7 第一阶段报告：加入行程状态"
Cohesion: 0.25
Nodes (7): Fix 1, Fix 2, Task 7 第一阶段报告：加入行程状态, TDD 与验证, 后续阶段, 实现, 范围

### Community 176 - "Batch 2 搜索受控状态证据契约"
Cohesion: 0.29
Nodes (6): Batch 2 搜索受控状态证据契约, 单设备 agent 命令, 发布与消费协议, 构建 provenance, 范围, 视觉契约

### Community 178 - "TripListViewModel"
Cohesion: 0.11
Nodes (17): CreateTrip, OpenSettings, OpenTrip, RequestDelete, Retry, TripListAction, Factory, CreationExtras (+9 more)

## Knowledge Gaps
- **582 isolated node(s):** `guard-adb-install.sh script`, `READY`, `STARTING`, `STARTED`, `CANCELLED` (+577 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **35 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GeoPoint` connect `GeoPoint` to `Legs`, `MapUiModelMapperTest`, `MapFacade.kt`, `FakeRepository`, `FakeItineraries`, `RealAmapMapHost`, `Legs`, `AmapComposeMap`, `MapUiModel`, `CreateTrip`, `PlacePoolUiState`, `RoutePlannerTest`, `MapViewportRenderingPolicyTest`, `DayItinerary`, `RouteMode`, `AmapServiceException`, `RoomSavedPlaceRepository`, `DelayedDeletePlaces`, `parsePlaces`, `RouteLegRepository`, `PlacePoolViewModel`, `AmapComposeMapTest.kt`, `PlaceSearchViewModel`, `TripWorkspaceViewModel`, `.model`, `decideCollectionToggle`, `Trips`, `parsePoiSearchResponse`, `.Content`, `PlaceSearchEvidenceTest`, `reduceMapInteraction`, `EasyTripDatabase`, `TripWorkspaceNavigationStateTest`, `MapViewportControllerTest`, `TransportMode`, `AmapRouteDataSource.kt`, `AmapSmokeTest`, `RoomItineraryRepository`, `RoomRouteLegRepository`, `AddPlacesRequest`?**
  _High betweenness centrality (0.147) - this node is a cross-community bridge._
- **Why does `TravelMode` connect `TravelMode` to `FakeTripRepository`, `TripSettingsViewModel`, `Trips`, `ItineraryTransactionTest`, `Legs`, `TripListContent.kt`, `TripDao`, `TripRepository`, `Trips`, `DayItinerary`, `GeoPoint`, `SelectablePill`, `TripSummary`, `RouteLegRepository`, `Converters`, `CreateTripViewModel`, `ItineraryDao`, `TripService`, `FakeTripRepository`, `CreateTripAction`, `Trips`, `Trips`, `EasyTripDatabase`, `Trips`, `Trips`, `CreateTripUiState`, `TransportMode`, `CreateTripContent`, `RoomItineraryRepository`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._
- **Why does `PlaceCandidate` connect `GeoPoint` to `Legs`, `MapUiModelMapperTest`, `MapFacade.kt`, `Legs`, `Trips`, `RealAmapMapHost`, `Legs`, `CreateTrip`, `Places`, `RoomSavedPlaceRepository`, `DelayedDeletePlaces`, `Legs`, `parsePlaces`, `AddToItineraryUiState`, `PlacePoolViewModel`, `CompactSecondaryButton`, `PlaceSearchViewModel`, `TripWorkspaceViewModel`, `decideCollectionToggle`, `Trips`, `parsePoiSearchResponse`, `PlaceSearchEvidenceTest`, `reduceMapInteraction`, `EasyTripDatabase`, `PlacePoolContent`, `RoomItineraryRepository`, `RoomRouteLegRepository`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `GeoPoint` (e.g. with `.result()` and `RoutePlannerTest`) actually correct?**
  _`GeoPoint` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 3 inferred relationships involving `TripDay` (e.g. with `FakeTrips` and `.createTrip()`) actually correct?**
  _`TripDay` has 3 INFERRED edges - model-reasoned connections that need verification._
- **What connects `guard-adb-install.sh script`, `READY`, `STARTING` to the rest of the system?**
  _582 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Easy Trip v1.0 全量 UI 与交互落地设计` be split into smaller, more focused modules?**
  _Cohesion score 0.0425531914893617 - nodes in this community are weakly interconnected._