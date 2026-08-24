package com.yangchengwei.easytrip

data class V1Scenario(
    val number: Int,
    val name: String,
    val frameId: String,
    val journey: String?,
    val matrix: String?,
    val executable: V1ScenarioExecutable,
    val assertions: List<ScenarioAssertion>,
    val variants: List<V1ScenarioVariant> = emptyList(),
    val physicalDeviceUiStatus: PhysicalDeviceUiStatus = PhysicalDeviceUiStatus.PENDING,
)

data class V1ScenarioVariant(
    val parentNumber: Int,
    val name: String,
    val frameId: String,
)

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
        scenario(1, "我的旅行", "K9h3r", "RqVLv", "hVIMZ", "existing-trips", BlockerCategory.DATA_CONSISTENCY, variants = listOf(V1ScenarioVariant(1, "我的旅行 · 删除后", "d1sTtb"))),
        scenario(2, "工作台·地点池", "A9EKX", "IpuKg", "xENWi", "trip-with-saved-places", BlockerCategory.KEY_INTERACTION),
        scenario(3, "搜索地点", "ofdn5", "V7cr3b", "eHTbI", "search-results", BlockerCategory.BASIC_ACCESSIBILITY, "搜索页不显示加入行程入口"),
        scenario(4, "工作台·行程", "LFmzR", "o4Wcz", "CW0vn", "day-itinerary", BlockerCategory.REACHABILITY),
        scenario(6, "全程行程", "FTIOF", "q08to1", "a5GvBo", "whole-trip-itinerary", BlockerCategory.KEY_INTERACTION, "全程视图不显示编辑或拖动入口"),
        scenario(7, "创建旅行", "dzhkC", "xP91E", "fIkSG", "empty-trip-list", BlockerCategory.BASIC_ACCESSIBILITY),
        scenario(8, "旅行设置", "U06l7P", "y3rP1", null, "existing-trip", BlockerCategory.REACHABILITY, "设置页不显示添加一天入口"),
        scenario(9, "选择日期", "xQfD0", null, null, "dated-trip-form", BlockerCategory.KEY_INTERACTION),
        scenario(10, "地点详情与编辑", "p4G1tS", null, null, "saved-place-detail", BlockerCategory.REACHABILITY, "地点详情不显示加入行程入口"),
        scenario(11, "行程项编辑", "K336N", null, null, "editable-itinerary-item", BlockerCategory.DATA_CONSISTENCY),
        scenario(12, "交通路段编辑", "T7aESo", null, null, "editable-route-leg", BlockerCategory.DATA_CONSISTENCY),
        scenario(13, "删除旅行确认", "oW9mK", null, null, "trip-with-delete-impact", BlockerCategory.KEY_INTERACTION, "确认文案说明级联删除影响"),
        scenario(14, "状态规范", "DxZ2a", null, null, "component-states", BlockerCategory.SEVERE_CLIPPING),
        scenario(15, "无旅行日", "p7U8B", null, null, "trip-without-days", BlockerCategory.CRASH_FREE),
        scenario(16, "工作台设置直达", "ijpZD", null, null, "existing-trip", BlockerCategory.REACHABILITY, "当前生产工作台通过顶部设置按钮直达设置页，不声称存在更多菜单"),
        scenario(17, "地图图层", "shoPV", null, null, "map-ready", BlockerCategory.KEY_INTERACTION),
        scenario(18, "添加旅行日", "zvO9Z", null, null, "dated-trip", BlockerCategory.DATA_CONSISTENCY),
        scenario(19, "从地点池添加地点", "Pqdkf", null, null, "saved-place-and-days", BlockerCategory.KEY_INTERACTION),
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
        scenario(31, "加入成功", "yNKT4", null, null, "add-place-success", BlockerCategory.DATA_CONSISTENCY),
        scenario(32, "删除行程项确认", "l2xCsM", null, null, "itinerary-delete-impact", BlockerCategory.KEY_INTERACTION, "确认文案说明相邻路线重算影响"),
        scenario(33, "长日期列表", "cRdBn", null, null, "long-date-list", BlockerCategory.SEVERE_CLIPPING),
        scenario(34, "定位权限说明", "JFhZ7", null, null, "location-rationale", BlockerCategory.KEY_INTERACTION, "系统权限前先展示定位用途说明"),
        scenario(35, "前往设置", "HYCsZ", null, null, "location-permanently-denied", BlockerCategory.REACHABILITY),
        scenario(36, "我的旅行空状态", "zIbEu", null, null, "empty-trip-list", BlockerCategory.BASIC_ACCESSIBILITY),
        scenario(37, "当天无地点", "Bcf6A", null, null, "empty-day", BlockerCategory.CRASH_FREE),
        scenario(38, "搜索网络失败", "GJo79", null, null, "search-network-error", BlockerCategory.FUNCTIONAL_STATE),
        scenario(39, "部分成功", "mGhKO", null, null, "partial-route-success", BlockerCategory.DATA_CONSISTENCY),
        scenario(40, "修改出行日期", "IKTv5", null, null, "dated-trip-settings", BlockerCategory.DATA_CONSISTENCY),
        scenario(41, "加入提交中", "V6RALq", null, null, "add-place-submitting", BlockerCategory.KEY_INTERACTION),
        scenario(42, "目标日已删除", "D3XZi", null, null, "stale-add-target", BlockerCategory.DATA_CONSISTENCY),
        scenario(43, "撤销成功", "KPBBb", null, null, "undo-delete-success", BlockerCategory.DATA_CONSISTENCY),
        scenario(44, "搜索加载中", "s1OvvX", null, null, "search-loading", BlockerCategory.FUNCTIONAL_STATE),
        scenario(45, "地图加载中", "GoxB6", null, null, "map-loading", BlockerCategory.FUNCTIONAL_STATE),
        scenario(46, "地图加载失败", "U8R5i", null, null, "map-load-error", BlockerCategory.CRASH_FREE),
        scenario(47, "创建表单校验", "yIGiQ", null, null, "invalid-trip-form", BlockerCategory.BASIC_ACCESSIBILITY),
        scenario(48, "行程修改保存失败", "OOEsk", null, null, "itinerary-save-error", BlockerCategory.DATA_CONSISTENCY),
    )

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
        executable = V1ScenarioExecutableFactory.create(number, frameId, fixture),
        assertions = listOf(
            ScenarioAssertion(BlockerCategory.FUNCTIONAL_STATE, expected),
            ScenarioAssertion(blocker, "$name 不触发 ${blocker.name.lowercase()} blocker"),
        ).distinctBy { it.category },
        variants = variants,
    )
}
