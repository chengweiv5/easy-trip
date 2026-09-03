package com.yangchengwei.easytrip

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
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
        val executable = scenario.createExecutable()
        executable.setup()
        executable.render(compose)
        compose.waitForIdle()
        executable.actions(compose)
        compose.waitForIdle()
        executable.assertions(compose)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun scenarios(): List<Array<V1Scenario>> = V1ScenarioFixtures.scenarios.map { arrayOf(it) }
    }
}

@RunWith(AndroidJUnit4::class)
class V1ScenarioMetadataTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

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
    fun workspaceAllEmptyFramesAreReachableTypedVariants() {
        val variants = V1ScenarioFixtures.scenarios
            .flatMap(V1Scenario::variants)
            .associateBy(V1ScenarioVariant::frameId)

        assertTrue(variants.containsKey("BrYVA"))
        assertTrue(variants.containsKey("WFOpg"))
        assertTrue(variants.containsKey("jQhXs"))
        assertTrue(variants.containsKey("XsGon"))
        assertEquals(2, variants.getValue("BrYVA").parentNumber)
        assertEquals(4, variants.getValue("WFOpg").parentNumber)
        assertEquals(2, variants.getValue("jQhXs").parentNumber)
        assertEquals(10, variants.getValue("XsGon").parentNumber)
    }

    @Test fun BrYVAExecutesProductionWorkspaceVariant() = executeVariant("BrYVA")

    @Test fun WFOpgExecutesProductionWorkspaceVariant() = executeVariant("WFOpg")

    @Test fun jQhXsExecutesProductionWorkspaceVariant() = executeVariant("jQhXs")

    @Test fun XsGonExecutesProductionWorkspaceVariant() = executeVariant("XsGon")

    @Test fun nAdK8ExecutesProductionWorkspaceVariant() = executeVariant("nAdK8")

    @Test fun mz2ISExecutesProductionWorkspaceVariant() = executeVariant("mz2IS")

    @Test fun batch5FramesKeepTypedMappingAndEvidenceBoundaries() {
        val frames = setOf("nAdK8", "K336N", "mz2IS", "l2xCsM", "T7aESo", "eHTX3", "FTIOF", "Bcf6A", "zvO9Z", "J7PZ7u")
        val evidence = VisualBatch0EvidenceTest.batch5Evidence

        assertEquals(frames, evidence.keys)
        assertEquals(47, V1ScenarioFixtures.scenarios.size)
        evidence.forEach { (frameId, descriptor) ->
            assertEquals(frameId, descriptor.scenario.frameId)
            assertEquals(frameId, descriptor.checkpoint.frameId)
            assertEquals(Batch5ExecutableEvidence.ProductionNavigationMainFlow, descriptor.executable)
            assertTrue(descriptor.checkpoint in descriptor.executable.requiredCheckpoints)
            assertEquals(EvidenceHost.ProductionAppNavigation, descriptor.host)
            assertEquals(EvidenceStateSource.InMemoryRoomNavigation, descriptor.stateSource)
            assertEquals(EvidenceMapSurface.RecordingFakeMapHost, descriptor.mapSurface)
        }
    }

    @Test fun eHTX3ExecutesProductionWorkspaceVariant() = executeVariant("eHTX3")

    @Test fun batch5ProductionEvidenceContractRequiresEveryTypedCheckpoint() {
        val contract = Batch5ExecutableEvidence.ProductionNavigationMainFlow

        assertEquals(Batch5FrameCheckpoint.entries.toSet(), contract.requiredCheckpoints)
        assertEquals(
            VisualBatch0EvidenceTest.batch5Evidence.keys,
            contract.requiredCheckpoints.mapTo(mutableSetOf(), Batch5FrameCheckpoint::frameId),
        )
    }

    @Test fun nAdK8DistinctExecutableRunsFullContract() = executeBatch5Variant(
        frameId = "nAdK8",
        fixtureId = "batch5-itinerary-page",
        screen = ScenarioScreen.ITINERARY,
    )

    @Test fun mz2ISDistinctExecutableRunsFullContract() = executeBatch5Variant(
        frameId = "mz2IS",
        fixtureId = "batch5-item-edit-complete",
        screen = ScenarioScreen.ITEM_EDITOR,
    )

    @Test fun eHTX3DistinctExecutableRunsFullContract() = executeBatch5Variant(
        frameId = "eHTX3",
        fixtureId = "batch5-single-day-route",
        screen = ScenarioScreen.ROUTE_EDITOR,
    )

    private fun executeBatch5Variant(frameId: String, fixtureId: String, screen: ScenarioScreen) {
        val executable = V1ScenarioFixtures.scenarios
            .flatMap(V1Scenario::variants)
            .first { it.frameId == frameId }
            .createExecutable()
        executable.setup()
        executable.render(compose)
        compose.waitForIdle()
        executable.actions(compose)
        compose.waitForIdle()
        executable.assertions(compose)
        assertEquals(fixtureId, executable.fixture.id)
        assertEquals(screen, executable.fixture.screen)
    }

    @Test fun WFOpgFixtureMatchesReachableWorkspaceState() {
        val fixture = V1ScenarioExecutableFactory.itineraryAllEmptyFixture()

        assertFalse(fixture.ready.isWorkspaceAllEmpty)
        assertTrue(fixture.ready.isItineraryAllEmpty)
        assertTrue(fixture.placeState.rows.isNotEmpty())
        assertEquals(fixture.ready.days.map { it.id }, fixture.ready.wholeTripDays.map { it.dayId })
        assertTrue(fixture.ready.wholeTripDays.all { it.items.isEmpty() })
        assertTrue(fixture.itineraryState.days.isNotEmpty())
    }

    private fun executeVariant(frameId: String) {
        val executable = V1ScenarioFixtures.scenarios
            .flatMap(V1Scenario::variants)
            .first { it.frameId == frameId }
            .createExecutable()
        executable.setup()
        executable.render(compose)
        compose.waitForIdle()
        executable.actions(compose)
        executable.assertions(compose)
    }

    @Test
    fun coversSevenJourneysAndSixMatrices() {
        assertEquals(V1ScenarioFixtures.journeyIds, V1ScenarioFixtures.scenarios.mapNotNull { it.journey }.toSet())
        assertEquals(V1ScenarioFixtures.matrixIds, V1ScenarioFixtures.scenarios.mapNotNull { it.matrix }.toSet())
    }

    @Test
    fun batch4AddToItineraryFramesUseTypedAddFixturesInsteadOfLegacyTripAndRouteFixtures() {
        val scenarios = V1ScenarioFixtures.scenarios.associateBy(V1Scenario::number)

        assertEquals("single-place-multi-day-selection", scenarios.getValue(9).createExecutable().fixture.id)
        assertEquals("no-trip-days-add-guidance", scenarios.getValue(15).createExecutable().fixture.id)
        assertEquals("selected-day-multi-place-picker", scenarios.getValue(19).createExecutable().fixture.id)
        assertEquals("add-success-result", scenarios.getValue(31).createExecutable().fixture.id)
        assertEquals("long-add-target-day-list", scenarios.getValue(33).createExecutable().fixture.id)
        assertEquals("add-partial-success-result", scenarios.getValue(39).createExecutable().fixture.id)
        assertEquals("missing-add-target-day-result", scenarios.getValue(42).createExecutable().fixture.id)
        assertEquals("add-undo-success-result", scenarios.getValue(43).createExecutable().fixture.id)
    }

    @Test
    fun batch4FramesDeclareControlledProductionWorkspaceHostEvidence() {
        val frames = setOf("xQfD0", "cRdBn", "Pqdkf", "p7U8B", "yNKT4", "mGhKO", "D3XZi", "KPBBb")
        val evidence = VisualBatch0EvidenceTest.batch4WorkspaceHostEvidence

        assertEquals(frames, evidence.keys)
        evidence.values.forEach { descriptor ->
            assertEquals("production Compose workspace host", descriptor.host)
            assertEquals("controlled UiState; not Room-triggered", descriptor.stateSource)
            assertEquals("deterministic fake map surface; not a real map host", descriptor.mapSurface)
        }
    }

    @Test
    fun everyScenarioHasTypedExecutableFixturePathAndAssertions() {
        V1ScenarioFixtures.scenarios.forEach { scenario ->
            val executable = scenario.createExecutable()
            assertTrue("scenario ${scenario.number} frame", scenario.frameId.isNotBlank())
            assertTrue("scenario ${scenario.number} fixture", executable.fixture.id.isNotBlank())
            assertTrue("scenario ${scenario.number} path", executable.reachablePath.steps.isNotEmpty())
            assertEquals(
                "scenario ${scenario.number} path target",
                executable.fixture.screen,
                executable.reachablePath.steps.last(),
            )
            assertTrue("scenario ${scenario.number} assertions", scenario.assertions.isNotEmpty())
            assertEquals(PhysicalDeviceUiStatus.PENDING, scenario.physicalDeviceUiStatus)
        }
    }

    @Test
    fun eachSuiteGetsAnIndependentExecutableFixture() {
        V1ScenarioFixtures.scenarios.forEach { scenario ->
            assertNotSame(scenario.createExecutable(), scenario.createExecutable())
        }
    }

    @Test
    fun mapFailureSetupCanBeRepeatedOnTheSameExecutable() {
        val executable = V1ScenarioFixtures.scenarios.first { it.number == 46 }.createExecutable()

        repeat(2) { executable.setup() }
        executable.render(compose)
        compose.waitForIdle()
        executable.actions(compose)
        compose.waitForIdle()
        executable.assertions(compose)
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
            9 to ScenarioScreen.TARGET_DAY,
            10 to ScenarioScreen.PLACE_DETAIL,
            11 to ScenarioScreen.ITEM_EDITOR,
            12 to ScenarioScreen.ROUTE_EDITOR,
            13 to ScenarioScreen.TRIP_LIST,
            14 to ScenarioScreen.STATUS_MATRIX,
            15 to ScenarioScreen.TARGET_DAY,
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
            31 to ScenarioScreen.WORKSPACE,
            32 to ScenarioScreen.ITINERARY,
            33 to ScenarioScreen.TARGET_DAY,
            34 to ScenarioScreen.PERMISSION,
            35 to ScenarioScreen.PERMISSION,
            36 to ScenarioScreen.TRIP_LIST,
            37 to ScenarioScreen.ITINERARY,
            38 to ScenarioScreen.SEARCH,
            39 to ScenarioScreen.WORKSPACE,
            40 to ScenarioScreen.TRIP_SETTINGS,
            41 to ScenarioScreen.TARGET_DAY,
            42 to ScenarioScreen.WORKSPACE,
            43 to ScenarioScreen.WORKSPACE,
            44 to ScenarioScreen.SEARCH,
            45 to ScenarioScreen.WORKSPACE,
            46 to ScenarioScreen.WORKSPACE,
            47 to ScenarioScreen.CREATE_TRIP,
            48 to ScenarioScreen.ITEM_EDITOR,
        )

        assertEquals(expected, V1ScenarioFixtures.scenarios.associate { it.number to it.createExecutable().fixture.screen })
    }

    @Test
    fun workspaceSheetScenariosRejectSwappedFixtureIdentities() {
        val scenarios = V1ScenarioFixtures.scenarios.associateBy(V1Scenario::number)

        listOf(22 to 23, 23 to 24, 24 to 22).forEach { (scenarioNumber, fixtureSourceNumber) ->
            val fixtureSource = scenarios.getValue(fixtureSourceNumber)

            assertThrows(IllegalArgumentException::class.java) {
                V1ScenarioExecutableFactory.create(scenarioNumber, fixtureSource.frameId, fixtureSource.createExecutable().fixture.id).setup()
            }
        }
    }

    @Test fun tripListEntryUsesProductionSelectorsAndSemantics() = assertProductionScenario(1)

    @Test fun createTripEntryUsesProductionSelectorsAndSemantics() = assertProductionScenario(7)

    @Test fun singlePlaceTargetEntryUsesProductionSelectorsAndSemantics() = assertProductionScenario(9)

    @Test fun deletionEntryUsesProductionSelectorsAndSemantics() = assertProductionScenario(13)

    @Test fun emptyTripListEntryUsesProductionSelectorsAndSemantics() = assertProductionScenario(36)

    @Test fun dateModeEntryUsesProductionSelectorsAndSemantics() = assertProductionScenario(47)

    private fun assertProductionScenario(number: Int) {
        val executable = V1ScenarioFixtures.scenarios.first { it.number == number }.createExecutable()
        executable.setup()
        executable.render(compose)
        compose.waitForIdle()
        executable.actions(compose)
        compose.waitForIdle()
        executable.assertions(compose)
    }

    @Test
    fun tripEntryScenarioCatalogCoversConfirmedFrames() {
        val frames = V1ScenarioFixtures.scenarios
            .flatMap { scenario -> listOf(scenario.frameId) + scenario.variants.map(V1ScenarioVariant::frameId) }
            .toSet()

        assertTrue(frames.containsAll(setOf("K9h3r", "d1sTtb", "dzhkC", "xQfD0", "oW9mK", "zIbEu", "yIGiQ")))
    }

    @Test
    fun tripEntryExecutablesDoNotClaimProfileNavigation() {
        val executable = V1ScenarioFixtures.scenarios.first { it.number == 1 }.createExecutable()
        executable.setup()
        executable.render(compose)
        compose.waitForIdle()

        compose.onNodeWithTag("profile-avatar", useUnmergedTree = true).assertIsDisplayed()
            .assert(SemanticsProperties.Role.keyNotDefined())
            .assert(SemanticsActions.OnClick.keyNotDefined())
        compose.onAllNodesWithContentDescription("个人中心").assertCountEquals(0)
        compose.onAllNodes(hasClickAction() and hasTestTag("profile-avatar")).assertCountEquals(0)
    }

    private fun <T> androidx.compose.ui.semantics.SemanticsPropertyKey<T>.keyNotDefined() =
        androidx.compose.ui.test.SemanticsMatcher("$name is not defined") { node ->
            node.config.getOrNull(this) == null
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
