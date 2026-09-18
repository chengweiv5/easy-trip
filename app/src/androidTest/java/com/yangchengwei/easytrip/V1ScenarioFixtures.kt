package com.yangchengwei.easytrip

data class V1ScenarioIdentity(
    val parentNumber: Int,
    val frameId: String,
    val fixtureId: String,
    val screen: ScenarioScreen,
    val factoryIdentity: String,
) {
    val signature: String
        get() = "$parentNumber|$frameId|$fixtureId|${screen.name}|$factoryIdentity"
}

data class V1Scenario(
    val number: Int,
    val name: String,
    val frameId: String,
    val journey: String?,
    val matrix: String?,
    val declaredIdentity: V1ScenarioIdentity,
    val assertions: List<ScenarioAssertion>,
    val variants: List<V1ScenarioVariant> = emptyList(),
    val physicalDeviceUiStatus: PhysicalDeviceUiStatus = PhysicalDeviceUiStatus.PENDING,
) {
    fun createExecutable(): V1ScenarioExecutable = V1ScenarioExecutableFactory.create(declaredIdentity)
}

data class V1ScenarioVariant(
    val parentNumber: Int,
    val name: String,
    val frameId: String,
    val declaredIdentity: V1ScenarioIdentity,
) {
    fun createExecutable(): V1ScenarioExecutable = V1ScenarioExecutableFactory.create(declaredIdentity)
}

data class ScenarioAssertion(
    val category: BlockerCategory,
    val expected: String,
)

enum class Batch5FrameCheckpoint(val frameId: String) {
    ItineraryPage("nAdK8"),
    ItemEditor("K336N"),
    ItemEditComplete("mz2IS"),
    ItemDelete("l2xCsM"),
    RouteEditor("T7aESo"),
    SingleDayMap("eHTX3"),
    WholeTrip("FTIOF"),
    EmptyDay("Bcf6A"),
    AppendDay("zvO9Z"),
    DeleteDay("J7PZ7u"),
}

sealed interface Batch5ExecutableEvidence {
    val requiredCheckpoints: Set<Batch5FrameCheckpoint>
    fun execute(test: V2AcceptanceTest): Set<Batch5FrameCheckpoint>

    fun verifyCheckpoints(test: V2AcceptanceTest): Set<Batch5FrameCheckpoint> =
        execute(test).also { observed ->
            check(observed == requiredCheckpoints) {
                "Batch 5 evidence mismatch: missing=${requiredCheckpoints - observed}, unexpected=${observed - requiredCheckpoints}"
            }
        }

    data object ProductionNavigationMainFlow : Batch5ExecutableEvidence {
        override val requiredCheckpoints: Set<Batch5FrameCheckpoint> = Batch5FrameCheckpoint.entries.toSet()
        override fun execute(test: V2AcceptanceTest): Set<Batch5FrameCheckpoint> =
            test.executeBatch5ProductionNavigationRoomMainFlow()
    }
}

enum class EvidenceHost { ProductionCompose, ProductionAppNavigation, ProductionRepository, PhysicalDevice }
enum class EvidenceStateSource { ControlledUiState, InMemoryRoomNavigation, FileBackedRoomReopen, HandwrittenRepositoryFakes, InstalledAppRestart }
enum class EvidenceMapSurface { None, DeterministicFakeSurface, RecordingFakeMapHost, NonRecordingFakeMapHost, FailureInjectingFakeMapHost, RealAmap }
enum class EvidencePermissionSurface { None, ControlledSnapshot, ActivityResultContract, AndroidSystem }

enum class Batch6FrameCheckpoint(val frameId: String) {
    WaitingForNetwork("P7k0M"),
    FailedRoute("E3EhSv"),
    MapConsentExplanation("EHOHC"),
    LocationExplanation("JFhZ7"),
    LocationSettingsRecovery("HYCsZ"),
    MapLoading("GoxB6"),
    MapFailure("U8R5i"),
    EditSaveFailure("OOEsk"),
}

enum class Batch6TypedScenario(
    val number: Int,
    val frameId: String,
    val fixtureId: String,
    val screen: ScenarioScreen,
    val factoryIdentity: String,
) {
    WaitingForNetwork(28, "P7k0M", "offline-pending-routes", ScenarioScreen.ITINERARY, "batch6-waiting-for-network"),
    FailedRoute(29, "E3EhSv", "failed-route", ScenarioScreen.ITINERARY, "batch6-failed-route"),
    MapConsentExplanation(30, "EHOHC", "map-consent-required", ScenarioScreen.PERMISSION, "batch6-map-consent-explanation"),
    LocationExplanation(34, "JFhZ7", "location-rationale", ScenarioScreen.PERMISSION, "batch6-location-explanation"),
    LocationSettingsRecovery(35, "HYCsZ", "location-permanently-denied", ScenarioScreen.PERMISSION, "batch6-location-settings-recovery"),
    MapLoading(45, "GoxB6", "map-loading", ScenarioScreen.WORKSPACE, "batch6-map-loading"),
    MapFailure(46, "U8R5i", "map-load-error", ScenarioScreen.WORKSPACE, "batch6-map-failure"),
    EditSaveFailure(48, "OOEsk", "itinerary-save-error", ScenarioScreen.ITEM_EDITOR, "batch6-edit-save-failure"),
}

data class Batch6FrameEvidence(
    val scenario: V1Scenario,
    val checkpoint: Batch6FrameCheckpoint,
    val fixtureId: String,
    val host: EvidenceHost,
    val stateSource: EvidenceStateSource,
    val mapSurface: EvidenceMapSurface,
    val permissionSurface: EvidencePermissionSurface,
    val automatedEntry: String,
    val limitations: String,
) {
    fun executable(): V1ScenarioExecutable = scenario.createExecutable()
}

data class Batch5FrameEvidence(
    val scenario: V1ScenarioVariant,
    val checkpoint: Batch5FrameCheckpoint,
    val executable: Batch5ExecutableEvidence,
    val host: EvidenceHost,
    val stateSource: EvidenceStateSource,
    val mapSurface: EvidenceMapSurface,
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

    private fun variant(
        parentNumber: Int,
        name: String,
        frameId: String,
        fixtureId: String,
        screen: ScenarioScreen,
        factoryIdentity: String = fixtureId,
    ) = V1ScenarioVariant(
        parentNumber = parentNumber,
        name = name,
        frameId = frameId,
        declaredIdentity = V1ScenarioIdentity(parentNumber, frameId, fixtureId, screen, factoryIdentity),
    )

    val scenarios = listOf(
        scenario(1, "我的旅行", "K9h3r", "RqVLv", "hVIMZ", "existing-trips", BlockerCategory.DATA_CONSISTENCY, "主卡整卡进入工作台且无独立继续规划入口，旅行名称、日期、天数和方式保持一致；主卡与其他卡的设置、删除均由各自 ··· 菜单承载", variants = listOf(variant(1, "我的旅行 · 删除后", "d1sTtb", "trip-list-deleted-final-state", ScenarioScreen.TRIP_LIST, "variant-d1sTtb-deleted-final-state"))),
        scenario(
            2,
            "工作台·地点池",
            "A9EKX",
            "IpuKg",
            "xENWi",
            "trip-with-saved-places",
            BlockerCategory.KEY_INTERACTION,
            variants = listOf(
                variant(2, "工作台·全空", "BrYVA", "workspace-all-empty", ScenarioScreen.WORKSPACE),
                variant(2, "地点池·短列表", "jQhXs", "place-pool-short-list", ScenarioScreen.WORKSPACE),
            ),
        ),
        scenario(3, "搜索地点", "ofdn5", "V7cr3b", "eHTbI", "search-results", BlockerCategory.BASIC_ACCESSIBILITY, "搜索页不显示加入行程入口"),
        scenario(
            4,
            "工作台·行程",
            "LFmzR",
            "o4Wcz",
            "CW0vn",
            "day-itinerary",
            BlockerCategory.REACHABILITY,
            variants = listOf(
                variant(4, "工作台·行程全空", "WFOpg", "itinerary-all-empty", ScenarioScreen.WORKSPACE),
                variant(4, "行程页 · 旅程C", "nAdK8", "batch5-itinerary-page", ScenarioScreen.ITINERARY),
                variant(4, "行程 · 编辑完成", "mz2IS", "batch5-item-edit-complete", ScenarioScreen.ITEM_EDITOR),
                variant(4, "单日路线", "eHTX3", "batch5-single-day-route", ScenarioScreen.ROUTE_EDITOR),
            ),
        ),
        scenario(6, "全程行程", "FTIOF", "q08to1", "a5GvBo", "whole-trip-itinerary", BlockerCategory.KEY_INTERACTION, "全程视图不显示编辑或拖动入口"),
        scenario(7, "创建旅行", "dzhkC", "xP91E", "fIkSG", "empty-trip-list", BlockerCategory.BASIC_ACCESSIBILITY, "创建保留旅行名称、开始日期、天数和出行方式，并通过生产提交入口创建旅行"),
        scenario(8, "旅行设置", "U06l7P", "y3rP1", null, "existing-trip", BlockerCategory.REACHABILITY, "设置页不显示添加一天入口"),
        scenario(9, "单地点选择旅行日", "xQfD0", null, null, "single-place-multi-day-selection", BlockerCategory.KEY_INTERACTION, "单个收藏地点可选择多个旅行日，不复用创建旅行日期表单"),
        scenario(
            10,
            "地点详情与编辑",
            "p4G1tS",
            null,
            null,
            "saved-place-detail",
            BlockerCategory.REACHABILITY,
            "地点池详情保留加入行程入口；搜索详情不显示加入行程入口",
            variants = listOf(
                variant(10, "地点详情·仅收藏", "XsGon", "only-collected-place-detail", ScenarioScreen.WORKSPACE),
            ),
        ),
        scenario(11, "行程项编辑", "K336N", null, null, "editable-itinerary-item", BlockerCategory.DATA_CONSISTENCY),
        scenario(12, "交通路段编辑", "T7aESo", null, null, "editable-route-leg", BlockerCategory.DATA_CONSISTENCY),
        scenario(13, "删除旅行确认", "oW9mK", null, null, "trip-with-delete-impact", BlockerCategory.KEY_INTERACTION, "从 ··· 菜单进入删除，确认文案精确列出旅行日、收藏地点、标签、行程项、路线段及保留内容"),
        scenario(14, "状态规范", "DxZ2a", null, null, "component-states", BlockerCategory.SEVERE_CLIPPING),
        scenario(15, "无旅行日引导", "p7U8B", null, null, "no-trip-days-add-guidance", BlockerCategory.REACHABILITY, "单地点加入行程时明确引导前往行程添加旅行日，不自动创建旅行日"),
        scenario(16, "工作台更多菜单", "ijpZD", null, null, "existing-trip", BlockerCategory.REACHABILITY, "顶部更多菜单提供旅行设置、地图授权和返回我的旅行；旅行设置保持可达"),
        scenario(17, "地图图层", "shoPV", null, null, "map-ready", BlockerCategory.KEY_INTERACTION),
        scenario(18, "添加旅行日", "zvO9Z", null, null, "dated-trip", BlockerCategory.DATA_CONSISTENCY),
        scenario(19, "选定旅行日后选择地点", "Pqdkf", null, null, "selected-day-multi-place-picker", BlockerCategory.KEY_INTERACTION, "固定旅行日入口可选择多个收藏地点并直接提交"),
        scenario(20, "旅程 C 选择", "X3rm1", null, null, "multi-day-add-target", BlockerCategory.REACHABILITY),
        scenario(21, "旅程 C 完成", "f25l9", null, null, "added-itinerary-item", BlockerCategory.DATA_CONSISTENCY),
        scenario(22, "抽屉收起", "kCc5z", null, null, "workspace-drawer-collapsed", BlockerCategory.SEVERE_CLIPPING),
        scenario(23, "抽屉半屏", "sWTB3", null, null, "workspace-drawer-half", BlockerCategory.SEVERE_CLIPPING),
        scenario(24, "抽屉展开", "f2ieZ6", null, null, "workspace-drawer-expanded", BlockerCategory.SEVERE_CLIPPING),
        scenario(25, "删除旅行日确认", "J7PZ7u", null, null, "day-with-delete-impact", BlockerCategory.KEY_INTERACTION, "确认文案说明地点和路段影响"),
        scenario(26, "地点池空状态", "lsr1I", null, null, "empty-place-pool", BlockerCategory.CRASH_FREE),
        scenario(27, "搜索无结果", "S0psO", null, null, "empty-search-result", BlockerCategory.FUNCTIONAL_STATE),
        scenario(28, "等待联网", "P7k0M", null, null, "offline-pending-routes", BlockerCategory.DATA_CONSISTENCY),
        scenario(29, "路线失败", "E3EhSv", null, null, "failed-route", BlockerCategory.FUNCTIONAL_STATE),
        scenario(30, "地图权限说明", "EHOHC", null, null, "map-consent-required", BlockerCategory.KEY_INTERACTION, "系统权限前先展示用途说明"),
        scenario(31, "加入成功", "yNKT4", null, null, "add-success-result", BlockerCategory.DATA_CONSISTENCY, "加入结果明确指向成功旅行日，并保留撤销与查看入口"),
        scenario(32, "删除行程项确认", "l2xCsM", null, null, "itinerary-delete-impact", BlockerCategory.KEY_INTERACTION, "确认文案说明相邻路线重算影响"),
        scenario(33, "长旅行日列表", "cRdBn", null, null, "long-add-target-day-list", BlockerCategory.SEVERE_CLIPPING, "长旅行日列表可滚动选择末日，固定确认操作保持可达"),
        scenario(34, "定位权限说明", "JFhZ7", null, null, "location-rationale", BlockerCategory.KEY_INTERACTION, "系统权限前先展示定位用途说明"),
        scenario(35, "前往设置", "HYCsZ", null, null, "location-permanently-denied", BlockerCategory.REACHABILITY),
        scenario(36, "我的旅行空状态", "zIbEu", null, null, "empty-trip-list", BlockerCategory.BASIC_ACCESSIBILITY, "空态展示开始规划一次旅行、用途说明和创建旅行入口"),
        scenario(37, "当天无地点", "Bcf6A", null, null, "empty-day", BlockerCategory.CRASH_FREE),
        scenario(38, "搜索网络失败", "GJo79", null, null, "search-network-error", BlockerCategory.FUNCTIONAL_STATE),
        scenario(39, "加入行程部分成功", "mGhKO", null, null, "add-partial-success-result", BlockerCategory.DATA_CONSISTENCY, "逐日区分已加入与未加入地点，收藏地点仍保留并仅重试失败项"),
        scenario(40, "修改出行日期", "IKTv5", null, null, "dated-trip-settings", BlockerCategory.DATA_CONSISTENCY),
        scenario(41, "加入提交中", "V6RALq", null, null, "add-place-submitting", BlockerCategory.KEY_INTERACTION),
        scenario(42, "加入目标日已删除", "D3XZi", null, null, "missing-add-target-day-result", BlockerCategory.DATA_CONSISTENCY, "删除的目标日明确要求重新选择，不改投其他日期，成功项仍可查看或撤销"),
        scenario(43, "加入后撤销成功", "KPBBb", null, null, "add-undo-success-result", BlockerCategory.DATA_CONSISTENCY, "撤销仅移除新增行程项，收藏地点保持，后续入口为地点池"),
        scenario(44, "搜索加载中", "s1OvvX", null, null, "search-loading", BlockerCategory.FUNCTIONAL_STATE),
        scenario(45, "地图加载中", "GoxB6", null, null, "map-loading", BlockerCategory.FUNCTIONAL_STATE),
        scenario(46, "地图加载失败", "U8R5i", null, null, "map-load-error", BlockerCategory.CRASH_FREE),
        scenario(47, "创建表单校验", "yIGiQ", null, null, "invalid-trip-form", BlockerCategory.BASIC_ACCESSIBILITY, "旅行名称、开始日期和天数错误分别显示在对应字段附近"),
        scenario(48, "行程修改保存失败", "OOEsk", null, null, "itinerary-save-error", BlockerCategory.DATA_CONSISTENCY),
    )

    val parentIdentities: List<V1ScenarioIdentity>
        get() = scenarios.map(V1Scenario::declaredIdentity)

    val variantIdentities: List<V1ScenarioIdentity>
        get() = scenarios.flatMap(V1Scenario::variants).map(V1ScenarioVariant::declaredIdentity)

    val allIdentities: List<V1ScenarioIdentity>
        get() = parentIdentities + variantIdentities

    private fun screenFor(number: Int): ScenarioScreen = when (number) {
        1, 13, 36 -> ScenarioScreen.TRIP_LIST
        2, 19, 26 -> ScenarioScreen.PLACE_POOL
        3, 27, 38, 44 -> ScenarioScreen.SEARCH
        4, 6, 21, 28, 29, 32, 37 -> ScenarioScreen.ITINERARY
        7, 47 -> ScenarioScreen.CREATE_TRIP
        8, 18, 25, 40 -> ScenarioScreen.TRIP_SETTINGS
        9, 15, 20, 33, 41 -> ScenarioScreen.TARGET_DAY
        10 -> ScenarioScreen.PLACE_DETAIL
        11, 48 -> ScenarioScreen.ITEM_EDITOR
        12 -> ScenarioScreen.ROUTE_EDITOR
        14 -> ScenarioScreen.STATUS_MATRIX
        16, 17, 22, 23, 24, 31, 39, 42, 43, 45, 46 -> ScenarioScreen.WORKSPACE
        30, 34, 35 -> ScenarioScreen.PERMISSION
        else -> error("Unsupported V1 scenario: $number")
    }

    private fun factoryIdentityFor(number: Int, fixture: String): String = when (number) {
        28 -> "batch6-waiting-for-network"
        29 -> "batch6-failed-route"
        30 -> "batch6-map-consent-explanation"
        34 -> "batch6-location-explanation"
        35 -> "batch6-location-settings-recovery"
        45 -> "batch6-map-loading"
        46 -> "batch6-map-failure"
        48 -> "batch6-edit-save-failure"
        else -> fixture
    }

    private fun scenario(
        number: Int,
        name: String,
        frameId: String,
        journey: String?,
        matrix: String?,
        fixture: String,
        blocker: BlockerCategory,
        expected: String = "$name 的关键功能和状态与 fixture 一致",
        variants: List<V1ScenarioVariant> = emptyList(),
    ) = V1Scenario(
        number = number,
        name = name,
        frameId = frameId,
        journey = journey,
        matrix = matrix,
        declaredIdentity = V1ScenarioIdentity(number, frameId, fixture, screenFor(number), factoryIdentityFor(number, fixture)),
        assertions = listOf(
            ScenarioAssertion(BlockerCategory.FUNCTIONAL_STATE, expected),
            ScenarioAssertion(blocker, "$name 不触发 ${blocker.name.lowercase()} blocker"),
        ).distinctBy { it.category },
        variants = variants,
    )
}
