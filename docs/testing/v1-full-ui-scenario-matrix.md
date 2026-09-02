# V1 Full UI Scenario Matrix

## Gate policy

The automated gate blocks functional/state errors, data inconsistency, crashes, unreachable flows, severe clipping, broken key interactions, and basic accessibility failures. Physical-device UI review may remain `PENDING`; it is not an automated blocker. Per-screen screenshots, pixel comparison, and Pencil diffs are not required.

## Coverage model

- Numbered scenarios: 47 (`01..48`, excluding retired `05`).
- Variants: `d1sTtb` belongs to scenario `01`; `jQhXs` to `02`; `XsGon` to `10`。它们不增加 parent scenario；`S0psO` 是 parent 27，其 controlled-state render 仅是证据方式。
- Journeys: `RqVLv`, `IpuKg`, `V7cr3b`, `o4Wcz`, `q08to1`, `xP91E`, `y3rP1`.
- Matrices: `hVIMZ`, `xENWi`, `eHTbI`, `CW0vn`, `a5GvBo`, `fIkSG`.
- `V1ScenarioFixtures` binds each parent entry to a typed `V1ScenarioExecutable` with fixture, reachable path, setup, production rendering, actions, and assertions. `V1FullUiAcceptanceTest` parameterizes over the 47 parent entries and executes that contract directly; `V1ScenarioCatalogTest` verifies catalog completeness and screen bindings without reflection. Variants do not increase the 47 count and are executed separately by `V1ScenarioMetadataTest`; all Device UI statuses remain `PENDING` until physical-device acceptance.

## Scenario directory

| No. | Frame | Scenario | Fixture | Automated coverage | Reachable path | Key gate | Device UI |
|---:|---|---|---|---|---|---|---|
| 01 | `K9h3r` | 我的旅行 | existing-trips | `V1ScenarioCatalogTest#executesProductionScenario` + `V1PencilFlowTest#createBackAndReopenUsesRoomAndNavigatesExactlyOncePerAction` | 启动应用 → 我的旅行 → 继续规划 | 唯一主入口；旅行名/日期/天数/方式 | PENDING |
| 01v | `d1sTtb` | 我的旅行 · 删除后 | variant of 01 | `V1ScenarioCatalogTest#executesProductionScenario`（场景 13 可控列表闭环） | 我的旅行 → `···` → 删除 → 确认 | 目标卡消失且保留旅行仍在 | PENDING |
| 02 | `A9EKX` | 工作台·地点池 | trip-with-saved-places | `V1ScenarioCatalogTest#executesProductionScenario` | 我的旅行 → 旅行 → 地点池 | key interaction | PENDING |
| 02v | `BrYVA` | 工作台·全空 | variant of 02 | `V1ScenarioMetadataTest#BrYVAExecutesProductionWorkspaceVariant` | 工作台 → 地点池（联合全空 UiState） | 生产 Compose host 显示联合全空，不创建新 route | PENDING |
| 02v | `jQhXs` | 地点池·短列表 | variant of 02 | `V1ScenarioMetadataTest#jQhXsExecutesProductionWorkspaceVariant` + `VisualBatch0EvidenceTest#productionComposeWorkspaceHostWithDeterministicFakeMapSurface_jQhXs` | 工作台 → 地点池（3 个收藏地点 UiState） | 生产 Compose workspace host；fake map 明确不是真实 map host | PENDING |
| 03 | `ofdn5` | 搜索地点 | search-results | `V2AcceptanceTest#searchCollectionMapAndRestorationFlow` + `PlaceSearchEvidenceTest#captureSearchDetailEvidence_noAddToItinerary` | 工作台 → 搜索地点 → 搜索详情 | 搜索详情明确无加入行程入口 | PENDING |
| 04 | `LFmzR` | 工作台·行程 | day-itinerary | `V1ScenarioCatalogTest#executesProductionScenario` | 工作台 → 行程 | reachability | PENDING |
| 04v | `WFOpg` | 工作台·行程全空 | variant of 04 | `V1ScenarioMetadataTest#WFOpgExecutesProductionWorkspaceVariant` | 工作台 → 行程（所有旅行日无行程项 UiState） | 生产 Compose host 显示全空，无 scope rail，不创建新 route | PENDING |
| 06 | `FTIOF` | 全程行程 | whole-trip-itinerary | `V2AcceptanceTest#searchCollectionMapAndRestorationFlow` | 行程 → 全程 | no edit/drag | PENDING |
| 07 | `dzhkC` | 创建旅行 | empty-trip-list | `V1ScenarioCatalogTest#executesProductionScenario` + `V1PencilFlowTest#createBackAndReopenUsesRoomAndNavigatesExactlyOncePerAction` | 我的旅行 → 创建 → `create-submit` | 名称/开始日期/天数/方式 | PENDING |
| 08 | `U06l7P` | 旅行设置 | existing-trip | focused Content test | 工作台 → 更多 → 设置 | no add-day | PENDING |
| 09 | `xQfD0` | 选择日期 | dated-trip-form | `V1ScenarioCatalogTest#executesProductionScenario` + `V1PencilFlowTest#createBackAndReopenUsesRoomAndNavigatesExactlyOncePerAction` | 创建旅行 → 指定日期 → 确定日期 | 开始与结束日期摘要 | PENDING |
| 10 | `p4G1tS` | 地点详情与编辑 | saved-place-detail | `VisualBatch0EvidenceTest#productionComposeWorkspaceHostWithDeterministicFakeMapSurface_p4G1tS` | 工作台 → 地点池 → 地点卡片 | 已安排详情显示安排摘要与加入行程；搜索详情仍无加入行程 | PENDING |
| 10v | `XsGon` | 地点详情·仅收藏 | variant of 10 | `V1ScenarioMetadataTest#XsGonExecutesProductionWorkspaceVariant` + `VisualBatch0EvidenceTest#productionComposeWorkspaceHostWithDeterministicFakeMapSurface_XsGon` | 工作台 → 地点池 → 地点详情（仅收藏 UiState） | 生产 Compose workspace host 显示空心书签、无安排摘要及加入行程；fake map 明确不是真实 map host | PENDING |
| 11 | `K336N` | 行程项编辑 | editable-itinerary-item | `V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes` | 当日行程 → 行程项 | data consistency | PENDING |
| 12 | `T7aESo` | 交通路段编辑 | editable-route-leg | `V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes` | 当日行程 → 路段 | data consistency | PENDING |
| 13 | `oW9mK` | 删除旅行确认 | trip-with-delete-impact | `V1ScenarioCatalogTest#executesProductionScenario` | 我的旅行 → `···` → 删除 → 确认删除旅行 | 精确级联删除与保留摘要 | PENDING |
| 14 | `DxZ2a` | 状态规范 | component-states | `ConfirmationDialogTest#narrowLargeFontDialogKeepsActionsReachableWithFullImpactList` | 验收目录 → 状态矩阵 | clipping | PENDING |
| 15 | `p7U8B` | 无旅行日 | trip-without-days | `ItineraryEditingTest#emptyDayShowsEmptyState` | 工作台 → 行程 | crash-free | PENDING |
| 16 | `ijpZD` | 工作台设置直达 | existing-trip | typed executable | 工作台 → 设置 | production UI has a direct settings action, not a more menu | PENDING |
| 17 | `shoPV` | 地图图层 | map-ready | `V1ScenarioCatalogTest#executesProductionScenario` + `WorkspaceChromeTest` + `VisualBatch0EvidenceTest#productionComposeWorkspaceHostWithDeterministicFakeMapSurface_shoPV` | 工作台 → 地图图层 | 三选项、选中态、遮罩、外部关闭与输入阻断 | PENDING |
| 18 | `zvO9Z` | 添加旅行日 | dated-trip | `V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes` | 行程 → 添加一天 | data consistency | PENDING |
| 19 | `Pqdkf` | 从地点池添加地点 | saved-place-and-days | V1 acceptance | 地点池 → 地点 → 加入行程 | interaction | PENDING |
| 20 | `X3rm1` | 旅程 C 选择 | multi-day-add-target | `PlacePoolFlowTest#longDayListScrollsAtNarrowLargeTextWhileSubmitStaysReachable` | 地点详情 → 选择旅行日 | reachability | PENDING |
| 21 | `f25l9` | 旅程 C 完成 | added-itinerary-item | V1 acceptance | 选择旅行日 → 确认 | data consistency | PENDING |
| 22 | `kCc5z` | 抽屉收起 | workspace-drawer-collapsed | V2 acceptance | 工作台 → 收起抽屉 | clipping | PENDING |
| 23 | `sWTB3` | 抽屉半屏 | workspace-drawer-half | V2 acceptance | 工作台 → 半屏抽屉 | clipping | PENDING |
| 24 | `f2ieZ6` | 抽屉展开 | workspace-drawer-expanded | V2 acceptance | 工作台 → 展开抽屉 | clipping | PENDING |
| 25 | `J7PZ7u` | 删除旅行日确认 | day-with-delete-impact | `TripSettingsContentTest#dayDeleteConfirmationIncludesCompleteDangerImpact` | 日期菜单 → 删除 | impact copy | PENDING |
| 26 | `lsr1I` | 地点池空状态 | empty-place-pool | V2 acceptance | 工作台 → 地点池 | crash-free | PENDING |
| 27 | `S0psO` | 搜索无结果 | empty-search-result | `V1ScenarioCatalogTest#executesProductionScenario` + `PlaceSearchEvidenceTest#captureEmptyEvidence_S0psO_controlledStateRender` | 搜索 → 无匹配关键字 | 保留关键词、引导调整/清空搜索，且无加入行程；controlled-state render 不替代 production-real-trigger 证据 | PENDING |
| 28 | `P7k0M` | 等待联网 | offline-pending-routes | `OfflineRecoveryTest#persistedRoutesRecoverInterruptedWorkWithoutTouchingSuccess` | 离线打开工作台 | data consistency | PENDING |
| 29 | `E3EhSv` | 路线失败 | failed-route | `ItineraryEditingTest#failedRouteShowsErrorAndRetryAction` | 工作台 → 失败路线 | state | PENDING |
| 30 | `EHOHC` | 地图权限说明 | map-consent-required | `WorkspacePermissionFlowTest#mapConsentExplanationDescribesPurposeBeforeConfirmation` | 首次地图能力 | pre-permission rationale | PENDING |
| 31 | `yNKT4` | 加入成功 | add-place-success | V1 acceptance | 选择旅行日 → 加入 | data consistency | PENDING |
| 32 | `l2xCsM` | 删除行程项确认 | itinerary-delete-impact | `ItineraryEditingTest#deleteConfirmationExplainsRetentionAndAdjacentRouteRecalculation` | 行程项 → 删除 | impact copy | PENDING |
| 33 | `cRdBn` | 长日期列表 | long-date-list | `PlacePoolFlowTest#longDayListScrollsAtNarrowLargeTextWhileSubmitStaysReachable` | 长旅行 → 日期列表 | clipping | PENDING |
| 34 | `JFhZ7` | 定位权限说明 | location-rationale | `WorkspacePermissionFlowTest#locationRationaleExplainsPurposeBeforePermissionRequest` | 工作台 → 定位 | pre-permission rationale | PENDING |
| 35 | `HYCsZ` | 前往设置 | location-permanently-denied | `WorkspacePermissionFlowTest#permanentDenialConfirmationOpensApplicationSettings` | 定位说明 → 永久拒绝 | reachability | PENDING |
| 36 | `zIbEu` | 我的旅行空状态 | empty-trip-list | `V1ScenarioCatalogTest#executesProductionScenario` + V1 Pencil flow | 启动无数据应用 → 创建旅行 | 新空态标题、说明与 CTA | PENDING |
| 37 | `Bcf6A` | 当天无地点 | empty-day | V1 acceptance | 工作台 → 空旅行日 | crash-free | PENDING |
| 38 | `GJo79` | 搜索网络失败 | search-network-error | `PlaceSearchContentTest#loadingEmptyAndFailureMatchTheirActions` | 搜索 → 网络失败 | state | PENDING |
| 39 | `mGhKO` | 部分成功 | partial-route-success | V1 acceptance | 混合路线行程 | data consistency | PENDING |
| 40 | `IKTv5` | 修改出行日期 | dated-trip-settings | `TripSettingsContentTest#shrinkConfirmationListsCompleteDangerImpact` | 设置 → 修改日期 | data consistency | PENDING |
| 41 | `V6RALq` | 加入提交中 | add-place-submitting | `PlacePoolFlowTest#targetDaySubmissionIsLockedWhileSubmitting` | 选择旅行日 → 提交 | interaction | PENDING |
| 42 | `D3XZi` | 目标日已删除 | stale-add-target | `PlacePoolFlowTest#missingTargetDayKeepsSelectionAndRequiresReselection` | 选择旅行日 → 删除目标 → 提交 | data consistency | PENDING |
| 43 | `KPBBb` | 撤销成功 | undo-delete-success | `WorkspaceFlowTest#resultOverlayPrioritizesUndoFailureAndExplainsMissingTargetCleanup` | 删除地点 → 撤销 | data consistency | PENDING |
| 44 | `s1OvvX` | 搜索加载中 | search-loading | V2 acceptance | 搜索 → 输入 | state | PENDING |
| 45 | `GoxB6` | 地图加载中 | map-loading | V2 acceptance | 打开工作台 | state | PENDING |
| 46 | `U8R5i` | 地图加载失败 | map-load-error | `TripWorkspaceContentTest#mapFailureShowsPersistentRetryAndKeepsContent` | 工作台 → 地图失败 | crash-free | PENDING |
| 47 | `yIGiQ` | 创建表单校验 | invalid-trip-form | `V1ScenarioCatalogTest#executesProductionScenario` + V1 Pencil flow | 创建 → 无效提交 | 名称/日期/天数错误靠近字段 | PENDING |
| 48 | `OOEsk` | 行程修改保存失败 | itinerary-save-error | `ItineraryEditingTest#itineraryEditSaveFailureRemainsVisible` | 行程项编辑 → 保存失败 | data consistency | PENDING |

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
