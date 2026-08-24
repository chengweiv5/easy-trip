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
    fun everyScenarioHasTypedFixtureAndRealFocusedTest() {
        V1ScenarioFixtures.scenarios.forEach { scenario ->
            assertTrue("scenario ${scenario.number} frame", scenario.frameId.isNotBlank())
            assertTrue("scenario ${scenario.number} path", scenario.launch.reachablePath.isNotBlank())
            assertTrue("scenario ${scenario.number} assertions", scenario.assertions.isNotEmpty())
            assertEquals(PhysicalDeviceUiStatus.PENDING, scenario.physicalDeviceUiStatus)

            val automation = scenario.launch.automation
            val method = Class.forName(automation.className).declaredMethods.singleOrNull {
                it.name == automation.methodName && it.parameterCount == 0
            }
            assertTrue("scenario ${scenario.number} test ${automation.qualifiedName} does not exist", method != null)
            assertTrue(
                "scenario ${scenario.number} test ${automation.qualifiedName} is not a JUnit test",
                method?.getAnnotation(Test::class.java) != null,
            )
            assertFalse(
                "scenario ${scenario.number} uses catalog self-check as automation",
                automation.className == javaClass.name,
            )
        }
    }

    @Test
    fun criticalReviewScenariosUseFocusedBehaviorTests() {
        val critical = setOf(8, 10, 13, 25, 29, 30, 32, 34, 35)
        V1ScenarioFixtures.scenarios.filter { it.number in critical }.forEach { scenario ->
            assertTrue(
                "scenario ${scenario.number} must use a focused scenario test",
                scenario.launch.automation.className != javaClass.name,
            )
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

}
