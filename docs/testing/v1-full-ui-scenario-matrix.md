# V1 Full UI Scenario Matrix

## Gate policy

The automated gate blocks functional/state errors, data inconsistency, crashes, unreachable flows, severe clipping, broken key interactions, and basic accessibility failures. Physical-device UI review may remain `PENDING`; it is not an automated blocker. Per-screen screenshots, pixel comparison, and Pencil diffs are not required.

## Coverage model

- Numbered scenarios: 47 (`01..48`, excluding retired `05`).
- Variants: `d1sTtb` belongs to scenario `01`; `jQhXs` to `02`; `XsGon` to `10`; Batch 5 的 `nAdK8`、`mz2IS`、`eHTX3` 均为 parent `04` 的 typed variants。它们不增加 parent scenario；`S0psO` 是 parent 27，其 controlled-state render 仅是证据方式。Batch 4 的八个 frame 仍映射到既有 parent `09/15/19/31/33/39/42/43`，不新增编号 parent。
- Journeys: `RqVLv`, `IpuKg`, `V7cr3b`, `o4Wcz`, `q08to1`, `xP91E`, `y3rP1`.
- Matrices: `hVIMZ`, `xENWi`, `eHTbI`, `CW0vn`, `a5GvBo`, `fIkSG`.
- `V1ScenarioFixtures` 为 47 个 parent 与全部 variants 绑定不可对调的 number/frame/fixture/screen/factory identity，并提供 typed `V1ScenarioExecutable`。其中 `declaredPath` 只是声明的场景终点 metadata，不证明实际导航可达。`V1FullUiAcceptanceTest` 参数化执行 47 个 parent；`V1ScenarioMetadataTest` 另行遍历全部 variants。`U06l7P`、`oW9mK`、`d1sTtb` 的 production AppNavigation + in-memory Room 导航/级联删除证据统一来自 `TripSettingsNavigationTest`；`p4G1tS`、`XsGon` 的边界是 production workspace Compose + controlled UiState + deterministic/recording fake map，不是 Room 触发导航，也不是真实 AMap。
- 证据轴写法：`Host` 表示 production AppNavigation / production Compose host / controlled content；`state source` 表示 Room、受控 repository 或 controlled UiState；`map surface` 明确 RealAmap、recording/deterministic fake 或无地图；`permission surface` 明确 AMap consent、AndroidSystem 或未涉及；`source` 记录具体 AVD、Huawei 或构建任务。`PASS` 只覆盖该行明确写出的轴。

## Scenario directory

| No. | Frame | Scenario | Fixture | Automated coverage | 声明路径/预期路径 | Key gate / evidence axes | Device UI |
|---:|---|---|---|---|---|---|---|
| 01 | `K9h3r` | 我的旅行 | existing-trips | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + `V1PencilFlowTest#createBackReopenDeleteUsesRoomAndNavigatesExactlyOncePerAction` | 启动应用 → 我的旅行 → 继续规划 | 唯一主入口；旅行名/日期/天数/方式 | PENDING |
| 01v | `d1sTtb` | 我的旅行 · 删除后 | trip-list-deleted-final-state | `V1ScenarioMetadataTest#d1sTtbVariantRendersDedicatedControlledFinalState` + `TripSettingsNavigationTest#settingsDeleteRemovesOwnedRowsKeepsOtherTripAndClearsWorkspaceBackStack` | 设计声明：我的旅行 → `···` → 删除 → 确认；实际 production E2E：旅行列表 → 继续规划 → 工作台更多 → 设置 → 删除这次旅行 → 确认 → 旅行列表 | Host=controlled TripListContent + production AppNavigation；state=controlled final state + in-memory Room；map=删除链使用 recording fake；permission=未涉及；source=trail_map_api36 AVD Task 4 合并 run（删除导航通过） | PENDING |
| 02 | `A9EKX` | 工作台·地点池 | trip-with-saved-places | `MapViewportControllerTest` + `MapTouchInteractionDetectorTest` + `TripWorkspaceNavigationStateTest#map gesture clears published request and suppresses navigation until place set changes` + `MapUiModelMapperTest`；`AmapComposeMapTest#gestureListenerUsesLatestComposeCallbackAndIsClearedOnDispose`、`#zoomButtonsReportViewportOperationExactlyOnceBeforeDelegatingToHost` 与 WorkspaceFlow zoom integration 已编译；Huawei production installed app 自然搜索/收藏/列表滚动 | 我的旅行 → 旅行 → 地点池 | Host=production installed workspace；state=真实 Room 8→9→10 个自然搜索收藏地点；map=RealAmap；permission=已接受 AMap consent；source=Huawei ALN-AL00 2026-09-05 + debug JVM。8 点计数/滚动、9 点 COLD start 初始 fit、真实拖动后行程→地点池保持、搜索收藏“天安门-城楼”为第 10 点后单次新 fit、5 秒稳定无二次移动，以及 10 点再次拖动/tab 往返保持均有实证。Task #84 另以真实空数据 workspace 验证两次不同方向的拖动后 tab 往返保持；`+/-` 未在该 production surface 呈现、单击后无可观察 scope fit，故不声明其真机按钮/单击证据。 | PASS |
| 02v | `BrYVA` | 工作台·全空 | variant of 02 | `V1ScenarioMetadataTest#BrYVAExecutesProductionWorkspaceVariant` | 工作台 → 地点池（联合全空 UiState） | 生产 Compose host 显示联合全空，不创建新 route | PENDING |
| 02v | `jQhXs` | 地点池·短列表 | variant of 02 | `V1ScenarioMetadataTest#jQhXsExecutesProductionWorkspaceVariant` + `VisualBatch0EvidenceTest#productionComposeWorkspaceHostWithDeterministicFakeMapSurface_jQhXs` | 工作台 → 地点池（3 个收藏地点 UiState） | 生产 Compose workspace host；fake map 明确不是真实 map host | PENDING |
| 03 | `ofdn5` | 搜索地点 | search-results | `V2AcceptanceTest#searchCollectionAndMapFlowUsesProductionNavigationState` + `PlaceSearchEvidenceTest#captureSearchDetailEvidence_noAddToItinerary` | 工作台 → 搜索地点 → 搜索详情 | 搜索详情明确无加入行程入口 | PENDING |
| 04 | `LFmzR` | 工作台·行程 | day-itinerary | `V1ScenarioCatalogTest#executesDeclaredScenarioState` | 工作台 → 行程 | reachability | PENDING |
| 04v | `WFOpg` | 工作台·行程全空 | variant of 04 | `V1ScenarioMetadataTest#WFOpgExecutesProductionWorkspaceVariant` | 工作台 → 行程（所有旅行日无行程项 UiState） | 生产 Compose host 显示全空，无 scope rail，不创建新 route | PENDING |
| 04v | `nAdK8` | 行程页 · 旅程C | batch5-itinerary-page | `V1ScenarioMetadataTest#nAdK8ExecutesProductionWorkspaceVariant` + `V2AcceptanceTest#batch5ProductionNavigationRoomMainFlow` | 工作台 → 行程 → 某日 | production AppNavigation + in-memory Room；recording fake map，不是 real AMap | PENDING |
| 04v | `mz2IS` | 行程 · 编辑完成 | batch5-item-edit-complete | `V1ScenarioMetadataTest#mz2ISExecutesProductionWorkspaceVariant` + `V2AcceptanceTest#batch5ProductionNavigationRoomMainFlow` | 行程项编辑 → 保存 → 行程页 → 重开 | production AppNavigation + in-memory Room；recording fake map，不是 real AMap | PENDING |
| 06 | `FTIOF` | 全程行程 | whole-trip-itinerary | `V2AcceptanceTest#batch5ProductionNavigationRoomMainFlow` | 行程 → 全程 | Room-backed 分组、空日、只读且无跨日 leg；recording fake map，不是 real AMap | PENDING |
| 07 | `dzhkC` | 创建旅行 | empty-trip-list | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + `V1PencilFlowTest#createBackReopenDeleteUsesRoomAndNavigatesExactlyOncePerAction` | 我的旅行 → 创建 → `create-submit` | Host=production installed app；state=真实 Room 新建旅行；map=提交后工作台 consent-declined surface，不声明 RealAmap；permission=AMap consent declined；source=Huawei ALN-AL00 2026-08-29 + automated navigation | PASS |
| 08 | `U06l7P` | 旅行设置 | existing-trip | `TripSettingsNavigationTest#workspaceMoreOpensCurrentTripSettingsAndBackReturnsToSameWorkspace` | 工作台 → 更多 → 设置（声明 metadata；该测试执行 production navigation） | Host=production AppNavigation；state=in-memory Room；map=recording fake；permission=未涉及；source=trail_map_api36 AVD Task 4 合并 run；返回同一工作台状态 | PENDING |
| 09 | `xQfD0` | 单地点 → 多日选择 | single-place-multi-day-selection | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + `V1ScenarioMetadataTest#batch4AddToItineraryFramesUseTypedAddFixturesInsteadOfLegacyTripAndRouteFixtures` | 地点池 → 单地点加入行程 → 选择多个旅行日 | 生产 `SelectTargetDayContent` 的 checkbox 多选；不复用创建旅行日期表单 | PENDING |
| 10 | `p4G1tS` | 地点详情与编辑 | saved-place-detail | `WorkspaceFlowTest#savedPlaceRowOpensBottomSheetWithoutRecreatingMapOrViewport` + `VisualBatch0EvidenceTest#productionComposeWorkspaceHostWithDeterministicFakeMapSurface_p4G1tS` | 工作台 → 地点池 → 地点卡片（声明 metadata；前者实际执行 saved-place-row flow） | Host=production workspace Compose；state=controlled repository/UiState；map=recording/deterministic fake，host/viewport 保持；permission=未涉及；source=trail_map_api36 AVD Task 4 合并 run（row flow 通过）+ 既有 visual evidence | PENDING |
| 10v | `XsGon` | 地点详情·仅收藏 | only-collected-place-detail | `V1ScenarioMetadataTest#XsGonExecutesProductionWorkspaceVariant` + `VisualBatch0EvidenceTest#productionComposeWorkspaceHostWithDeterministicFakeMapSurface_XsGon` | 工作台 → 地点池 → 地点详情（声明 metadata；未执行 production navigation） | Host=production workspace Compose；state=controlled UiState；map=deterministic fake；permission=未涉及；source=trail_map_api36 AVD metadata run + 既有 visual evidence；空心书签、无安排摘要及加入行程 | PENDING |
| 11 | `K336N` | 行程项编辑 | editable-itinerary-item | `V2AcceptanceTest#batch5ProductionNavigationRoomMainFlow` | 当日行程 → 行程项 | production UI 编辑三字段，Room 回流并重开；recording fake map，不是 real AMap | PENDING |
| 12 | `T7aESo` | 交通路段编辑 | editable-route-leg | `V2AcceptanceTest#batch5ProductionNavigationRoomMainFlow` | 当日行程 → 路段 | production UI 编辑方式、耗时、说明并从 Room 重开；recording fake map，不是 real AMap | PENDING |
| 13 | `oW9mK` | 删除旅行确认 | trip-with-delete-impact | `TripSettingsNavigationTest#settingsDeleteRemovesOwnedRowsKeepsOtherTripAndClearsWorkspaceBackStack` | 设计声明：我的旅行 → `···` → 删除 → 确认删除旅行；实际 production E2E：旅行列表 → 继续规划 → 工作台更多 → 设置 → 删除这次旅行 → 确认 → 旅行列表 | Host=production AppNavigation；state=in-memory Room 精确级联；map=recording fake；permission=未涉及；source=trail_map_api36 AVD Task 4 合并 run；保留旅行 primary、工作台回栈清除 | PENDING |
| 14 | `DxZ2a` | 状态规范 | component-states | `ConfirmationDialogTest#narrowLargeFontDialogKeepsActionsReachableWithFullImpactList` | 验收目录 → 状态矩阵 | clipping | PENDING |
| 15 | `p7U8B` | 无旅行日引导 | no-trip-days-add-guidance | `V1ScenarioCatalogTest#executesDeclaredScenarioState` | 地点池 → 单地点加入行程 → 无旅行日引导 | 生产 `SelectTargetDayContent` 明确提供“前往行程”，不自动创建旅行日 | PENDING |
| 16 | `ijpZD` | 工作台设置直达 | existing-trip | typed executable | 工作台 → 设置 | production UI has a direct settings action, not a more menu | PENDING |
| 17 | `shoPV` | 地图图层 | map-ready | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + `WorkspaceChromeTest` + `VisualBatch0EvidenceTest#productionComposeWorkspaceHostWithDeterministicFakeMapSurface_shoPV` + Huawei ALN-AL00 physical acceptance | 工作台 → 地图图层 | Host=production installed workspace；state=真实安装态工作台 + persisted layer preference；map=RealAmap 标准/卫星/卫星路网；permission=已接受 AMap consent；source=Huawei ALN-AL00 2026-09-04。AVD fake evidence 另覆盖遮罩/输入阻断 | PASS |
| 18 | `zvO9Z` | 添加旅行日 | dated-trip | `V2AcceptanceTest#batch5ProductionNavigationRoomMainFlow` | 行程 → 添加一天 | production UI 等待 Room 回流并进入新空日；recording fake map，不是 real AMap | PENDING |
| 19 | `Pqdkf` | 选定旅行日 → 多地点选择 | selected-day-multi-place-picker | `V1ScenarioCatalogTest#executesDeclaredScenarioState` | 行程 → 固定旅行日 → 选择地点 | 生产 `SelectPlacesContent` 保持固定目标日并支持多地点 | PENDING |
| 20 | `X3rm1` | 旅程 C 选择 | multi-day-add-target | `PlacePoolFlowTest#longDayListScrollsAtNarrowLargeTextWhileSubmitStaysReachable` | 地点详情 → 选择旅行日 | reachability | PENDING |
| 21 | `f25l9` | 旅程 C 完成 | added-itinerary-item | V1 acceptance | 选择旅行日 → 确认 | data consistency | PENDING |
| 22 | `kCc5z` | 抽屉收起 | workspace-drawer-collapsed | V2 acceptance | 工作台 → 收起抽屉 | clipping | PENDING |
| 23 | `sWTB3` | 抽屉半屏 | workspace-drawer-half | V2 acceptance | 工作台 → 半屏抽屉 | clipping | PENDING |
| 24 | `f2ieZ6` | 抽屉展开 | workspace-drawer-expanded | V2 acceptance | 工作台 → 展开抽屉 | clipping | PENDING |
| 25 | `J7PZ7u` | 删除旅行日确认 | day-with-delete-impact | `V2AcceptanceTest#batch5ProductionNavigationRoomMainFlow` + `TripSettingsContentTest#dayDeleteConfirmationIncludesCompleteDangerImpact` | 工作台 → 更多 → 设置 → 删除日 | production UI 取消/确认，Room 重编号并回到合法 scope；recording fake map，不是 real AMap | PENDING |
| 26 | `lsr1I` | 地点池空状态 | empty-place-pool | `PlacePoolFlowTest#emptyPoolProvidesSearchAction` + V2 acceptance | 工作台 → 地点池 | Host=production installed workspace；state=真实空地点池 Room 状态；map=AMap consent-declined fallback，不声明 RealAmap；permission=AMap consent declined；source=Huawei ALN-AL00 2026-08-29 + AVD Compose | PASS |
| 27 | `S0psO` | 搜索无结果 | empty-search-result | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + `PlaceSearchEvidenceTest#captureEmptyEvidence_S0psO_controlledStateRender` | 搜索 → 无匹配关键字 | 保留关键词、引导调整/清空搜索，且无加入行程；controlled-state render 不替代 production-real-trigger 证据 | PENDING |
| 28 | `P7k0M` | 等待联网 | offline-pending-routes | `V1ScenarioMetadataTest#P7k0MExecutesBatch6TypedScenario` + `OfflineRecoveryTest#persistedRoutesRecoverInterruptedWorkWithoutTouchingSuccess` | 离线打开工作台 | controlled production Composable 保留行程；File-backed Room reopen 只验证路线恢复，不是连续导航 E2E | PENDING |
| 29 | `E3EhSv` | 路线失败 | failed-route | `V1ScenarioMetadataTest#E3EhSvExecutesBatch6TypedScenario` + `ItineraryEditingTest#failedRouteShowsErrorAndRetryAction` | 工作台 → 失败路线 | typed production itinerary state 只允许当前失败段 retry；focused host 不替代真实网络/Room E2E | PENDING |
| 30 | `EHOHC` | 地图权限说明 | map-consent-required | `V1ScenarioMetadataTest#EHOHCExecutesBatch6TypedScenario` + `WorkspaceFlowTest#undecidedWorkspaceEntryShowsConsentExplanationUntilDecision` | 首次地图能力 | Production AppNavigation + controlled consent snapshot；不声明 AndroidSystem 或 real AMap | PENDING |
| 31 | `yNKT4` | 加入成功 | add-success-result | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + `WorkspaceFlowTest#addResultSuccessShowsDayAndNavigatesToItBeforeClosing` | 选择旅行日 → 加入 → 结果 | 生产 `AddToItineraryResultContent` 显示成功日、撤销与查看入口 | PENDING |
| 32 | `l2xCsM` | 删除行程项确认 | itinerary-delete-impact | `V2AcceptanceTest#batch5ProductionNavigationRoomMainFlow` + `ItineraryEditingTest#deleteConfirmationExplainsRetentionAndAdjacentRouteRecalculation` | 行程项 → 删除 | production UI 删除后 SavedPlace 保留并重建 bridge leg；recording fake map，不是 real AMap | PENDING |
| 33 | `cRdBn` | 长旅行日列表 | long-add-target-day-list | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + `WorkspaceFlowTest#longTargetDayListKeepsConfirmReachableAndSelectsFinalDay` + Batch 4 workspace-host controlled evidence | 单地点加入行程 → 长旅行日列表 | typed executable 滚动并选择第 30 天；host evidence证明 overlay 可呈现，`WorkspaceFlowTest` 证明真实工作台流中末日选择与固定 footer 可达 | PENDING |
| 34 | `JFhZ7` | 定位权限说明 | location-rationale | `V1ScenarioMetadataTest#JFhZ7ExecutesBatch6TypedScenario` + `WorkspacePermissionFlowTest#locationRationaleUsesJFhZ7CopyAndOnlyContinueConfirms` + `WorkspaceFlowTest#appNavigationBindsPermissionCallbackToRequestThenSettingsResumeGrantShowsLocationOnce` + Huawei ALN-AL00 AndroidSystem acceptance | 工作台 → 定位 | Host=production installed workspace；state=真实 coordinator/ActivityResult；map=RealAmap Ready；permission=AndroidSystem 定位说明→请求→拒绝；source=Huawei ALN-AL00 2026-09-04 | PASS |
| 35 | `HYCsZ` | 前往设置 | location-permanently-denied | `V1ScenarioMetadataTest#HYCsZExecutesBatch6TypedScenario` + `WorkspacePermissionFlowTest#locationSettingsUsesHYCsZCopyAndSeparatesOpenFromCancel` + `WorkspaceFlowTest#appNavigationBindsPermissionCallbackToRequestThenSettingsResumeGrantShowsLocationOnce` + Huawei ALN-AL00 AndroidSystem/settings acceptance | 定位说明 → 永久拒绝 | Host=production installed workspace；state=真实 coordinator/settings recovery；map=RealAmap Ready 并返回定位；permission=AndroidSystem 永久拒绝→系统应用设置授予；source=Huawei ALN-AL00 2026-09-04 | PASS |
| 36 | `zIbEu` | 我的旅行空状态 | empty-trip-list | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + V1 Pencil flow | 启动无数据应用 → 创建旅行 | Host=production installed trip list；state=真实空 Room 状态；map=无地图；permission=未涉及；source=Huawei ALN-AL00 2026-08-29 | PASS |
| 37 | `Bcf6A` | 当天无地点 | empty-day | `V2AcceptanceTest#batch5ProductionNavigationRoomMainFlow` + `WholeTripItineraryContentTest#singleEmptyDayKeepsScopeRailAndAddsFromCurrentDayInsteadOfWholeTripEmptyState` | 工作台 → 空旅行日 → 从地点池添加 | production UI 保留日期 rail、当前日 CTA；recording fake map，不是 real AMap | PENDING |
| 38 | `GJo79` | 搜索网络失败 | search-network-error | `PlaceSearchContentTest#loadingEmptyAndFailureMatchTheirActions` | 搜索 → 网络失败 | state | PENDING |
| 39 | `mGhKO` | 加入行程部分成功 | add-partial-success-result | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + `WorkspaceFlowTest#addResultDistinguishesPartialMissingAndUndoStates` | 多日加入 → 部分成功结果 | 生产 `AddToItineraryResultContent` 按日区分已加入与失败地点；不是路线部分成功 | PENDING |
| 40 | `IKTv5` | 修改出行日期 | dated-trip-settings | `TripSettingsContentTest#shrinkConfirmationExposesStructuredImpactCounts` | 设置 → 修改日期 | data consistency | PENDING |
| 41 | `V6RALq` | 加入提交中 | add-place-submitting | `PlacePoolFlowTest#targetDaySubmissionIsLockedWhileSubmitting` | 选择旅行日 → 提交 | interaction | PENDING |
| 42 | `D3XZi` | 加入目标日已删除 | missing-add-target-day-result | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + `WorkspaceFlowTest#addResultDistinguishesPartialMissingAndUndoStates` | 多日加入 → 目标日失效 → 结果 | 生产结果明确要求重新选择，不改投其他日期；成功项仍可撤销/查看 | PENDING |
| 43 | `KPBBb` | 加入后撤销成功 | add-undo-success-result | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + `WorkspaceFlowTest#addResultDistinguishesPartialMissingAndUndoStates` | 加入行程 → 撤销 → 结果 | 生产结果确认仅移除新增项、保留收藏，并只提供地点池入口 | PENDING |
| 44 | `s1OvvX` | 搜索加载中 | search-loading | V2 acceptance | 搜索 → 输入 | state | PENDING |
| 45 | `GoxB6` | 地图加载中 | map-loading | `V1ScenarioMetadataTest#GoxB6ExecutesBatch6TypedScenario` + `WorkspaceFlowTest#neverReadyMapShowsApprovedFallbackKeepsContentAndRetryRecreatesOnlyHost` + `AmapComposeMapTest#delayedMapReadyKeepsLoadingUntilHostSignalsReady` + Huawei ALN-AL00 real AMap acceptance | 打开工作台 | Host=production installed workspace；state=真实 Room 本地内容 + controlled Loading 自动化；map=RealAmap Loading→Ready；permission=已接受 AMap consent；source=Huawei ALN-AL00 2026-09-04 + AVD recording fake | PASS |
| 46 | `U8R5i` | 地图加载失败 | map-load-error | `V1ScenarioMetadataTest#U8R5iExecutesBatch6TypedScenario` + `WorkspaceFlowTest#mapFailureKeepsLocalTabsAndActionsReachable` | 工作台 → 地图失败 | production workspace host 保留 tab/retry；failure-injecting fake map 覆盖恢复分支。Huawei 真机真实 AMap 正常加载，未能自然触发真实 SDK 故障，不声明 RealAMap failure evidence | PENDING |
| 47 | `yIGiQ` | 创建表单校验 | invalid-trip-form | `V1ScenarioCatalogTest#executesDeclaredScenarioState` + V1 Pencil flow | 创建 → 无效提交 | Host=production installed create-trip page；state=真实表单草稿/校验；map=无地图；permission=未涉及；source=Huawei ALN-AL00 2026-08-29 | PASS |
| 48 | `OOEsk` | 行程修改保存失败 | itinerary-save-error | `V1ScenarioMetadataTest#OOEskExecutesBatch6TypedScenario` + `ItineraryEditingTest#itineraryEditSaveFailureShowsRecoveryInsteadOfBareEditorAndReturnsToSameInput` + `V2AcceptanceTest#productionNavigationSaveFailurePreservesDraftAndRetriesOnceAgainstRoom` | 行程项编辑 → 保存失败 | production AppNavigation + in-memory Room + recording fake map；不声明 installed-app restart | PENDING |

## Cross-cutting journeys and matrices

| Type | ID | Covered by |
|---|---|---|
| Journey | `RqVLv` | 01 |
| Journey | `IpuKg` | 02 |
| Journey | `V7cr3b` | 03 |
| Journey | `o4Wcz` | 04 |
| Journey | `q08to1` | 06 |
| Journey | `xP91E` | 07 |
| Journey | `y3rP1` | 08 |
| Matrix | `hVIMZ` | 01 |
| Matrix | `xENWi` | 02 |
| Matrix | `eHTbI` | 03 |
| Matrix | `CW0vn` | 04 |
| Matrix | `a5GvBo` | 06 |
| Matrix | `fIkSG` | 07 |

`BTSZj` and `voAHV` remain cross-cutting exception checks and do not increase the seven-journey count.
