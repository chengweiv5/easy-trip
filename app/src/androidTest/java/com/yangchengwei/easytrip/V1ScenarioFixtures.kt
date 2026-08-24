package com.yangchengwei.easytrip

data class V1Scenario(
    val number: Int,
    val name: String,
    val frameId: String,
    val journey: String?,
    val matrix: String?,
    val launch: ScenarioLaunch,
    val assertions: List<ScenarioAssertion>,
    val variants: List<V1ScenarioVariant> = emptyList(),
    val physicalDeviceUiStatus: PhysicalDeviceUiStatus = PhysicalDeviceUiStatus.PENDING,
)

data class V1ScenarioVariant(
    val parentNumber: Int,
    val name: String,
    val frameId: String,
)

data class ScenarioLaunch(
    val fixture: String,
    val automation: ScenarioAutomation,
    val reachablePath: String,
)

data class ScenarioAutomation(
    val className: String,
    val methodName: String,
) {
    val qualifiedName: String get() = "$className#$methodName"

    companion object {
        private val testPackages = mapOf(
            "V1AcceptanceTest" to "com.yangchengwei.easytrip",
            "V1PencilFlowTest" to "com.yangchengwei.easytrip",
            "V2AcceptanceTest" to "com.yangchengwei.easytrip",
            "OfflineRecoveryTest" to "com.yangchengwei.easytrip",
            "TripSettingsContentTest" to "com.yangchengwei.easytrip.trip.ui",
            "TripFlowTest" to "com.yangchengwei.easytrip.trip.ui",
            "PlaceSearchContentTest" to "com.yangchengwei.easytrip.place.ui",
            "PlacePoolFlowTest" to "com.yangchengwei.easytrip.place.ui",
            "ItineraryEditingTest" to "com.yangchengwei.easytrip.itinerary.ui",
            "WorkspaceFlowTest" to "com.yangchengwei.easytrip.workspace",
            "WorkspacePermissionFlowTest" to "com.yangchengwei.easytrip.workspace",
            "TripWorkspaceContentTest" to "com.yangchengwei.easytrip.workspace",
            "ConfirmationDialogTest" to "com.yangchengwei.easytrip.core.ui.component",
        )

        fun registered(reference: String): ScenarioAutomation {
            val (simpleClassName, methodName) = reference.split('#', limit = 2)
            val packageName = requireNotNull(testPackages[simpleClassName]) {
                "Unregistered scenario test class: $simpleClassName"
            }
            return ScenarioAutomation("$packageName.$simpleClassName", methodName)
        }
    }
}

data class ScenarioAssertion(
    val category: BlockerCategory,
    val expected: String,
)

enum class BlockerCategory {
    FUNCTIONAL_STATE,
    DATA_CONSISTENCY,
    CRASH_FREE,
    REACHABILITY,
    SEVERE_CLIPPING,
    KEY_INTERACTION,
    BASIC_ACCESSIBILITY,
}

enum class PhysicalDeviceUiStatus { PENDING, PASS, FAIL }

object V1ScenarioFixtures {
    val journeyIds = setOf("RqVLv", "IpuKg", "V7cr3b", "o4Wcz", "q08to1", "xP91E", "y3rP1")
    val matrixIds = setOf("hVIMZ", "xENWi", "eHTbI", "CW0vn", "a5GvBo", "fIkSG")

    val scenarios = listOf(
        scenario(1, "我的旅行", "K9h3r", "RqVLv", "hVIMZ", "existing-trips", "V1PencilFlowTest#createBackAndReopenUsesRoomAndNavigatesExactlyOncePerAction", "启动应用 → 我的旅行", BlockerCategory.DATA_CONSISTENCY, variants = listOf(V1ScenarioVariant(1, "我的旅行 · 删除后", "d1sTtb"))),
        scenario(2, "工作台·地点池", "A9EKX", "IpuKg", "xENWi", "trip-with-saved-places", "V2AcceptanceTest#searchCollectionMapAndRestorationFlow", "我的旅行 → 打开旅行 → 地点池", BlockerCategory.KEY_INTERACTION),
        scenario(3, "搜索地点", "ofdn5", "V7cr3b", "eHTbI", "search-results", "V2AcceptanceTest#searchCollectionMapAndRestorationFlow", "工作台 → 搜索地点", BlockerCategory.BASIC_ACCESSIBILITY, "搜索页不显示加入行程入口"),
        scenario(4, "工作台·行程", "LFmzR", "o4Wcz", "CW0vn", "day-itinerary", "V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes", "工作台 → 行程", BlockerCategory.REACHABILITY),
        scenario(6, "全程行程", "FTIOF", "q08to1", "a5GvBo", "whole-trip-itinerary", "V2AcceptanceTest#searchCollectionMapAndRestorationFlow", "工作台 → 行程 → 全程", BlockerCategory.KEY_INTERACTION, "全程视图不显示编辑或拖动入口"),
        scenario(7, "创建旅行", "dzhkC", "xP91E", "fIkSG", "empty-trip-list", "V1PencilFlowTest#createBackAndReopenUsesRoomAndNavigatesExactlyOncePerAction", "我的旅行 → 创建旅行", BlockerCategory.BASIC_ACCESSIBILITY),
        scenario(8, "旅行设置", "U06l7P", "y3rP1", null, "existing-trip", "TripSettingsContentTest#settingsExposesRangeAndNoAddInsertReorderOrSingleDayDateActions", "工作台 → 更多 → 旅行设置", BlockerCategory.REACHABILITY, "设置页不显示添加一天入口"),
        scenario(9, "选择日期", "xQfD0", null, null, "dated-trip-form", "V1PencilFlowTest#createBackAndReopenUsesRoomAndNavigatesExactlyOncePerAction", "创建旅行 → 选择日期", BlockerCategory.KEY_INTERACTION),
        scenario(10, "地点详情与编辑", "p4G1tS", null, null, "saved-place-detail", "PlacePoolFlowTest#placeDetailAllowsCollectionNoteAndTagsOnly", "地点池 → 地点卡片", BlockerCategory.REACHABILITY, "地点详情不显示加入行程入口"),
        scenario(11, "行程项编辑", "K336N", null, null, "editable-itinerary-item", "V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes", "当日行程 → 行程项", BlockerCategory.DATA_CONSISTENCY),
        scenario(12, "交通路段编辑", "T7aESo", null, null, "editable-route-leg", "V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes", "当日行程 → 交通路段", BlockerCategory.DATA_CONSISTENCY),
        scenario(13, "删除旅行确认", "oW9mK", null, null, "trip-with-delete-impact", "TripFlowTest#confirmingTripDeleteShowsImpactAndDeletesExactlyOnce", "我的旅行 → 删除旅行", BlockerCategory.KEY_INTERACTION, "确认文案说明级联删除影响"),
        scenario(14, "状态规范", "DxZ2a", null, null, "component-states", "ConfirmationDialogTest#narrowLargeFontDialogKeepsActionsReachableWithFullImpactList", "验收目录 → 状态矩阵", BlockerCategory.SEVERE_CLIPPING),
        scenario(15, "无旅行日", "p7U8B", null, null, "trip-without-days", "ItineraryEditingTest#emptyDayShowsEmptyState", "工作台 → 行程", BlockerCategory.CRASH_FREE),
        scenario(16, "工作台更多菜单", "ijpZD", null, null, "existing-trip", "TripWorkspaceContentTest#readyKeepsSearchSettingsBackAndItineraryActions", "工作台 → 更多", BlockerCategory.REACHABILITY),
        scenario(17, "地图图层", "shoPV", null, null, "map-ready", "V2AcceptanceTest#searchCollectionMapAndRestorationFlow", "工作台 → 地图图层", BlockerCategory.KEY_INTERACTION),
        scenario(18, "添加旅行日", "zvO9Z", null, null, "dated-trip", "V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes", "工作台 → 行程 → 添加一天", BlockerCategory.DATA_CONSISTENCY),
        scenario(19, "从地点池添加地点", "Pqdkf", null, null, "saved-place-and-days", "V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes", "地点池 → 地点 → 加入行程", BlockerCategory.KEY_INTERACTION),
        scenario(20, "旅程 C 选择", "X3rm1", null, null, "multi-day-add-target", "PlacePoolFlowTest#longDayListScrollsAtNarrowLargeTextWhileSubmitStaysReachable", "地点详情 → 选择旅行日", BlockerCategory.REACHABILITY),
        scenario(21, "旅程 C 完成", "f25l9", null, null, "added-itinerary-item", "V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes", "选择旅行日 → 确认", BlockerCategory.DATA_CONSISTENCY),
        scenario(22, "抽屉收起", "kCc5z", null, null, "workspace-drawer-collapsed", "V2AcceptanceTest#searchCollectionMapAndRestorationFlow", "工作台 → 收起抽屉", BlockerCategory.SEVERE_CLIPPING),
        scenario(23, "抽屉半屏", "sWTB3", null, null, "workspace-drawer-half", "V2AcceptanceTest#searchCollectionMapAndRestorationFlow", "工作台 → 半屏抽屉", BlockerCategory.SEVERE_CLIPPING),
        scenario(24, "抽屉展开", "f2ieZ6", null, null, "workspace-drawer-expanded", "V2AcceptanceTest#searchCollectionMapAndRestorationFlow", "工作台 → 展开抽屉", BlockerCategory.SEVERE_CLIPPING),
        scenario(25, "删除旅行日确认", "J7PZ7u", null, null, "day-with-delete-impact", "TripSettingsContentTest#dayDeleteConfirmationIncludesCompleteDangerImpact", "工作台 → 日期菜单 → 删除", BlockerCategory.KEY_INTERACTION, "确认文案说明地点和路段影响"),
        scenario(26, "地点池空状态", "lsr1I", null, null, "empty-place-pool", "V2AcceptanceTest#searchCollectionMapAndRestorationFlow", "工作台 → 地点池", BlockerCategory.CRASH_FREE),
        scenario(27, "搜索无结果", "S0psO", null, null, "empty-search-result", "PlaceSearchContentTest#loadingEmptyAndFailureMatchTheirActions", "搜索地点 → 提交无匹配关键字", BlockerCategory.FUNCTIONAL_STATE),
        scenario(28, "等待联网", "P7k0M", null, null, "offline-pending-routes", "OfflineRecoveryTest#persistedRoutesRecoverInterruptedWorkWithoutTouchingSuccess", "离线打开含计算中路线的工作台", BlockerCategory.DATA_CONSISTENCY),
        scenario(29, "路线失败", "E3EhSv", null, null, "failed-route", "ItineraryEditingTest#failedRouteShowsErrorAndRetryAction", "工作台 → 失败路线", BlockerCategory.FUNCTIONAL_STATE),
        scenario(30, "地图权限说明", "EHOHC", null, null, "map-consent-required", "WorkspacePermissionFlowTest#mapConsentExplanationDescribesPurposeBeforeConfirmation", "首次打开地图能力", BlockerCategory.KEY_INTERACTION, "系统权限前先展示用途说明"),
        scenario(31, "加入成功", "yNKT4", null, null, "add-place-success", "V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes", "选择旅行日 → 加入", BlockerCategory.DATA_CONSISTENCY),
        scenario(32, "删除行程项确认", "l2xCsM", null, null, "itinerary-delete-impact", "ItineraryEditingTest#deleteConfirmationExplainsRetentionAndAdjacentRouteRecalculation", "行程项 → 删除", BlockerCategory.KEY_INTERACTION, "确认文案说明相邻路线重算影响"),
        scenario(33, "长日期列表", "cRdBn", null, null, "long-date-list", "PlacePoolFlowTest#longDayListScrollsAtNarrowLargeTextWhileSubmitStaysReachable", "长旅行 → 日期列表", BlockerCategory.SEVERE_CLIPPING),
        scenario(34, "定位权限说明", "JFhZ7", null, null, "location-rationale", "WorkspacePermissionFlowTest#locationRationaleExplainsPurposeBeforePermissionRequest", "工作台 → 定位", BlockerCategory.KEY_INTERACTION, "系统权限前先展示定位用途说明"),
        scenario(35, "前往设置", "HYCsZ", null, null, "location-permanently-denied", "WorkspacePermissionFlowTest#permanentDenialConfirmationOpensApplicationSettings", "定位权限说明 → 永久拒绝", BlockerCategory.REACHABILITY),
        scenario(36, "我的旅行空状态", "zIbEu", null, null, "empty-trip-list", "V1PencilFlowTest#createBackAndReopenUsesRoomAndNavigatesExactlyOncePerAction", "启动无数据应用", BlockerCategory.BASIC_ACCESSIBILITY),
        scenario(37, "当天无地点", "Bcf6A", null, null, "empty-day", "V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes", "工作台 → 空旅行日", BlockerCategory.CRASH_FREE),
        scenario(38, "搜索网络失败", "GJo79", null, null, "search-network-error", "PlaceSearchContentTest#loadingEmptyAndFailureMatchTheirActions", "搜索地点 → 网络失败", BlockerCategory.FUNCTIONAL_STATE),
        scenario(39, "部分成功", "mGhKO", null, null, "partial-route-success", "V1AcceptanceTest#threeDayTripPersistsRepeatedPlacesEdgesOverridesAndMapScopes", "工作台 → 含成功与失败路线的行程", BlockerCategory.DATA_CONSISTENCY),
        scenario(40, "修改出行日期", "IKTv5", null, null, "dated-trip-settings", "TripSettingsContentTest#shrinkConfirmationListsCompleteDangerImpact", "旅行设置 → 修改日期", BlockerCategory.DATA_CONSISTENCY),
        scenario(41, "加入提交中", "V6RALq", null, null, "add-place-submitting", "PlacePoolFlowTest#targetDaySubmissionIsLockedWhileSubmitting", "选择旅行日 → 提交", BlockerCategory.KEY_INTERACTION),
        scenario(42, "目标日已删除", "D3XZi", null, null, "stale-add-target", "PlacePoolFlowTest#missingTargetDayKeepsSelectionAndRequiresReselection", "选择旅行日 → 目标日被删除 → 提交", BlockerCategory.DATA_CONSISTENCY),
        scenario(43, "撤销成功", "KPBBb", null, null, "undo-delete-success", "WorkspaceFlowTest#resultOverlayPrioritizesUndoFailureAndExplainsMissingTargetCleanup", "删除地点 → 撤销", BlockerCategory.DATA_CONSISTENCY),
        scenario(44, "搜索加载中", "s1OvvX", null, null, "search-loading", "V2AcceptanceTest#searchCollectionMapAndRestorationFlow", "搜索地点 → 输入关键字", BlockerCategory.FUNCTIONAL_STATE),
        scenario(45, "地图加载中", "GoxB6", null, null, "map-loading", "V2AcceptanceTest#searchCollectionMapAndRestorationFlow", "打开工作台地图", BlockerCategory.FUNCTIONAL_STATE),
        scenario(46, "地图加载失败", "U8R5i", null, null, "map-load-error", "TripWorkspaceContentTest#mapFailureShowsPersistentRetryAndKeepsContent", "打开工作台 → 地图加载失败", BlockerCategory.CRASH_FREE),
        scenario(47, "创建表单校验", "yIGiQ", null, null, "invalid-trip-form", "V1PencilFlowTest#createBackAndReopenUsesRoomAndNavigatesExactlyOncePerAction", "创建旅行 → 提交无效表单", BlockerCategory.BASIC_ACCESSIBILITY),
        scenario(48, "行程修改保存失败", "OOEsk", null, null, "itinerary-save-error", "ItineraryEditingTest#itineraryEditSaveFailureRemainsVisible", "行程项编辑 → 保存失败", BlockerCategory.DATA_CONSISTENCY),
    )

    private fun scenario(
        number: Int,
        name: String,
        frameId: String,
        journey: String?,
        matrix: String?,
        fixture: String,
        automation: String,
        reachablePath: String,
        blocker: BlockerCategory,
        expected: String = "$name 的关键功能和状态与 fixture 一致",
        variants: List<V1ScenarioVariant> = emptyList(),
    ) = V1Scenario(
        number = number,
        name = name,
        frameId = frameId,
        journey = journey,
        matrix = matrix,
        launch = ScenarioLaunch(fixture, ScenarioAutomation.registered(automation), reachablePath),
        assertions = listOf(
            ScenarioAssertion(BlockerCategory.FUNCTIONAL_STATE, expected),
            ScenarioAssertion(blocker, "$name 不触发 ${blocker.name.lowercase()} blocker"),
        ).distinctBy { it.category },
        variants = variants,
    )
}
