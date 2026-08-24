package com.yangchengwei.easytrip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V1FullUiAcceptanceTest {
    @Test
    fun containsEveryExistingNumberedFrameWithoutInventingFive() {
        val numbers = V1ScenarioFixtures.scenarios.map { it.number }.toSet()

        assertEquals((1..48).filterNot { it == 5 }.toSet(), numbers)
        assertFalse(5 in numbers)
        assertTrue(V1ScenarioFixtures.scenarios.first { it.number == 1 }.variants.any { it.frameId == "d1sTtb" })
    }

    @Test
    fun containsExactlyFortySevenAdoptedNumberedScenarios() {
        assertEquals(47, V1ScenarioFixtures.scenarios.size)
        assertEquals(47, V1ScenarioFixtures.scenarios.map { it.frameId }.toSet().size)
    }

    @Test
    fun coversSevenJourneysAndSixMatrices() {
        assertEquals(V1ScenarioFixtures.journeyIds, V1ScenarioFixtures.scenarios.mapNotNull { it.journey }.toSet())
        assertEquals(V1ScenarioFixtures.matrixIds, V1ScenarioFixtures.scenarios.mapNotNull { it.matrix }.toSet())
    }

    @Test
    fun everyScenarioHasFrameFixtureDevicePathAndAssertions() {
        V1ScenarioFixtures.scenarios.forEach { scenario ->
            assertTrue("scenario ${scenario.number} frame", scenario.frameId.isNotBlank())
            assertTrue("scenario ${scenario.number} fixture", scenario.launch.fixture.isNotBlank())
            assertTrue("scenario ${scenario.number} automation", scenario.launch.automation.isNotBlank())
            assertTrue("scenario ${scenario.number} path", scenario.launch.reachablePath.isNotBlank())
            assertTrue("scenario ${scenario.number} assertions", scenario.assertions.isNotEmpty())
            assertEquals(PhysicalDeviceUiStatus.PENDING, scenario.physicalDeviceUiStatus)
        }
    }

    @Test
    fun blockerAssertionsCoverTheAcceptanceGate() {
        assertEquals(
            setOf(
                BlockerCategory.FUNCTIONAL_STATE,
                BlockerCategory.DATA_CONSISTENCY,
                BlockerCategory.CRASH_FREE,
                BlockerCategory.REACHABILITY,
                BlockerCategory.SEVERE_CLIPPING,
                BlockerCategory.KEY_INTERACTION,
                BlockerCategory.BASIC_ACCESSIBILITY,
            ),
            V1ScenarioFixtures.scenarios.flatMap { scenario -> scenario.assertions.map { it.category } }.toSet(),
        )
    }

    @Test
    fun requiredProductSemanticsRemainCatalogued() {
        assertExpected(3, "搜索页不显示加入行程入口")
        assertExpected(10, "地点详情不显示加入行程入口")
        assertExpected(8, "设置页不显示添加一天入口")
        assertExpected(6, "全程视图不显示编辑或拖动入口")
        assertExpected(30, "系统权限前先展示用途说明")
        assertExpected(34, "系统权限前先展示定位用途说明")
    }

    @Test
    fun dangerousActionsDescribeTheirImpact() {
        assertExpected(13, "确认文案说明级联删除影响")
        assertExpected(25, "确认文案说明地点和路段影响")
        assertExpected(32, "确认文案说明相邻路线重算影响")
    }

    private fun assertExpected(number: Int, expected: String) {
        val assertions = V1ScenarioFixtures.scenarios.first { it.number == number }.assertions
        assertTrue("scenario $number missing: $expected", assertions.any { it.expected == expected })
    }
}
