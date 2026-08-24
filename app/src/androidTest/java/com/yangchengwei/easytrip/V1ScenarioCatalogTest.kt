package com.yangchengwei.easytrip

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class V1ScenarioCatalogTest(private val scenario: V1Scenario) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun executesProductionScenario() {
        scenario.executable.setup()
        scenario.executable.render(compose)
        compose.waitForIdle()
        scenario.executable.actions(compose)
        compose.waitForIdle()
        scenario.executable.assertions(compose)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun scenarios(): List<Array<V1Scenario>> = V1ScenarioFixtures.scenarios.map { arrayOf(it) }
    }
}

@RunWith(AndroidJUnit4::class)
class V1ScenarioMetadataTest {
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
    fun everyScenarioHasTypedExecutableFixturePathAndAssertions() {
        V1ScenarioFixtures.scenarios.forEach { scenario ->
            assertTrue("scenario ${scenario.number} frame", scenario.frameId.isNotBlank())
            assertTrue("scenario ${scenario.number} fixture", scenario.executable.fixture.id.isNotBlank())
            assertTrue("scenario ${scenario.number} path", scenario.executable.reachablePath.steps.isNotEmpty())
            assertEquals(
                "scenario ${scenario.number} path target",
                scenario.executable.fixture.screen,
                scenario.executable.reachablePath.steps.last(),
            )
            assertTrue("scenario ${scenario.number} assertions", scenario.assertions.isNotEmpty())
            assertEquals(PhysicalDeviceUiStatus.PENDING, scenario.physicalDeviceUiStatus)
        }
    }

    @Test
    fun everyScenarioTargetsItsDeclaredProductionScreen() {
        val expected = mapOf(
            1 to ScenarioScreen.TRIP_LIST,
            2 to ScenarioScreen.PLACE_POOL,
            3 to ScenarioScreen.SEARCH,
            4 to ScenarioScreen.ITINERARY,
            6 to ScenarioScreen.ITINERARY,
            7 to ScenarioScreen.CREATE_TRIP,
            8 to ScenarioScreen.TRIP_SETTINGS,
            9 to ScenarioScreen.DATE_PICKER,
            10 to ScenarioScreen.PLACE_DETAIL,
            11 to ScenarioScreen.ITEM_EDITOR,
            12 to ScenarioScreen.ROUTE_EDITOR,
            13 to ScenarioScreen.TRIP_LIST,
            14 to ScenarioScreen.STATUS_MATRIX,
            15 to ScenarioScreen.ITINERARY,
            16 to ScenarioScreen.WORKSPACE,
            17 to ScenarioScreen.WORKSPACE,
            18 to ScenarioScreen.TRIP_SETTINGS,
            19 to ScenarioScreen.PLACE_POOL,
            20 to ScenarioScreen.TARGET_DAY,
            21 to ScenarioScreen.ITINERARY,
            22 to ScenarioScreen.WORKSPACE,
            23 to ScenarioScreen.WORKSPACE,
            24 to ScenarioScreen.WORKSPACE,
            25 to ScenarioScreen.TRIP_SETTINGS,
            26 to ScenarioScreen.PLACE_POOL,
            27 to ScenarioScreen.SEARCH,
            28 to ScenarioScreen.ITINERARY,
            29 to ScenarioScreen.ITINERARY,
            30 to ScenarioScreen.PERMISSION,
            31 to ScenarioScreen.ITINERARY,
            32 to ScenarioScreen.ITINERARY,
            33 to ScenarioScreen.TARGET_DAY,
            34 to ScenarioScreen.PERMISSION,
            35 to ScenarioScreen.PERMISSION,
            36 to ScenarioScreen.TRIP_LIST,
            37 to ScenarioScreen.ITINERARY,
            38 to ScenarioScreen.SEARCH,
            39 to ScenarioScreen.ITINERARY,
            40 to ScenarioScreen.TRIP_SETTINGS,
            41 to ScenarioScreen.TARGET_DAY,
            42 to ScenarioScreen.TARGET_DAY,
            43 to ScenarioScreen.PLACE_POOL,
            44 to ScenarioScreen.SEARCH,
            45 to ScenarioScreen.WORKSPACE,
            46 to ScenarioScreen.WORKSPACE,
            47 to ScenarioScreen.CREATE_TRIP,
            48 to ScenarioScreen.ITEM_EDITOR,
        )

        assertEquals(expected, V1ScenarioFixtures.scenarios.associate { it.number to it.executable.fixture.screen })
    }

    @Test
    fun workspaceSheetScenariosRejectSwappedFixtureIdentities() {
        val scenarios = V1ScenarioFixtures.scenarios.associateBy(V1Scenario::number)

        listOf(22 to 23, 23 to 24, 24 to 22).forEach { (scenarioNumber, fixtureSourceNumber) ->
            val fixtureSource = scenarios.getValue(fixtureSourceNumber)

            assertThrows(IllegalArgumentException::class.java) {
                V1ScenarioExecutableFactory.create(scenarioNumber, fixtureSource.frameId, fixtureSource.executable.fixture.id).setup()
            }
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
