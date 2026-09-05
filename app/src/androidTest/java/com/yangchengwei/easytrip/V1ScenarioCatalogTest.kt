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
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
    fun executesDeclaredScenarioState() {
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
    fun factoryDoesNotExposeFixtureOnlyExecutableBuilders() {
        val fixtureOnlyBuilders = setOf(
            "workspaceAllEmpty",
            "itineraryAllEmpty",
            "itineraryAllEmptyFixture",
            "shortPlacePool",
            "onlyCollectedPlaceDetail",
            "workspaceItinerary",
            "workspaceItemEditComplete",
            "workspaceSingleDayRoute",
        )
        val exposed = V1ScenarioExecutableFactory::class.java.declaredMethods
            .filter { java.lang.reflect.Modifier.isPublic(it.modifiers) }
            .map { it.name }
            .toSet()
            .intersect(fixtureOnlyBuilders)

        assertTrue("Fixture-only executable builders must not be public: $exposed", exposed.isEmpty())
    }

    @Test
    fun createdExecutableRejectsCustomFactoryWithWrongFixtureDespiteMatchingScreenAndFactory() {
        val requested = V1ScenarioFixtures.scenarios.first { it.number == 1 }.declaredIdentity
        val valid = V1ScenarioExecutableFactory.create(requested)
        val wrongFixture = object : V1ScenarioExecutable by valid {
            override val declaredIdentity = requested
            override val fixture = ScenarioFixture("wrong-fixture", requested.screen)
        }

        assertThrows(IllegalArgumentException::class.java) {
            V1ScenarioExecutableFactory.create(requested) { wrongFixture }
        }
    }

    @Test
    fun createdExecutableRejectsCustomFactoryWithWrongNumberAndFrame() {
        val requested = V1ScenarioFixtures.scenarios.first { it.number == 1 }.declaredIdentity
        val valid = V1ScenarioExecutableFactory.create(requested)
        val wrongIdentity = object : V1ScenarioExecutable by valid {
            override val declaredIdentity = requested.copy(parentNumber = 2, frameId = "BrYVA")
        }

        assertThrows(IllegalArgumentException::class.java) {
            V1ScenarioExecutableFactory.create(requested) { wrongIdentity }
        }
    }

    @Test
    fun everyDeclaredVariantCreatesATypedExecutable() {
        V1ScenarioFixtures.scenarios
            .flatMap(V1Scenario::variants)
            .forEach(V1ScenarioVariant::createExecutable)
    }

    @Test
    fun d1sTtbVariantRendersDedicatedControlledFinalState() {
        val executable = V1ScenarioFixtures.scenarios
            .flatMap(V1Scenario::variants)
            .single { it.frameId == "d1sTtb" }
            .createExecutable()

        executable.setup()
        executable.render(compose)
        compose.waitForIdle()
        executable.actions(compose)
        compose.waitForIdle()
        executable.assertions(compose)

        compose.onNodeWithTag("primary-trip-trip-2").assertIsDisplayed()
        compose.onAllNodesWithTag("primary-trip-trip-1").assertCountEquals(0)
        compose.onAllNodesWithText("杭州 · 春日慢游").assertCountEquals(0)
        compose.onNodeWithText("川西小环线").assertIsDisplayed()
        compose.onNodeWithText("泉州古城散步").assertIsDisplayed()
        assertEquals("trip-list-deleted-final-state", executable.fixture.id)
        assertEquals(ScenarioScreen.TRIP_LIST, executable.fixture.screen)
        assertEquals("variant-d1sTtb-deleted-final-state", executable.factoryIdentity)
    }

    @Test
    fun parentsAndVariantsKeepExactDeclaredIdentityAndRejectEverySwappedDimension() {
        val expected = setOf(
            "1|K9h3r|existing-trips|TRIP_LIST|existing-trips",
            "2|A9EKX|trip-with-saved-places|PLACE_POOL|trip-with-saved-places",
            "3|ofdn5|search-results|SEARCH|search-results",
            "4|LFmzR|day-itinerary|ITINERARY|day-itinerary",
            "6|FTIOF|whole-trip-itinerary|ITINERARY|whole-trip-itinerary",
            "7|dzhkC|empty-trip-list|CREATE_TRIP|empty-trip-list",
            "8|U06l7P|existing-trip|TRIP_SETTINGS|existing-trip",
            "9|xQfD0|single-place-multi-day-selection|TARGET_DAY|single-place-multi-day-selection",
            "10|p4G1tS|saved-place-detail|PLACE_DETAIL|saved-place-detail",
            "11|K336N|editable-itinerary-item|ITEM_EDITOR|editable-itinerary-item",
            "12|T7aESo|editable-route-leg|ROUTE_EDITOR|editable-route-leg",
            "13|oW9mK|trip-with-delete-impact|TRIP_LIST|trip-with-delete-impact",
            "14|DxZ2a|component-states|STATUS_MATRIX|component-states",
            "15|p7U8B|no-trip-days-add-guidance|TARGET_DAY|no-trip-days-add-guidance",
            "16|ijpZD|existing-trip|WORKSPACE|existing-trip",
            "17|shoPV|map-ready|WORKSPACE|map-ready",
            "18|zvO9Z|dated-trip|TRIP_SETTINGS|dated-trip",
            "19|Pqdkf|selected-day-multi-place-picker|PLACE_POOL|selected-day-multi-place-picker",
            "20|X3rm1|multi-day-add-target|TARGET_DAY|multi-day-add-target",
            "21|f25l9|added-itinerary-item|ITINERARY|added-itinerary-item",
            "22|kCc5z|workspace-drawer-collapsed|WORKSPACE|workspace-drawer-collapsed",
            "23|sWTB3|workspace-drawer-half|WORKSPACE|workspace-drawer-half",
            "24|f2ieZ6|workspace-drawer-expanded|WORKSPACE|workspace-drawer-expanded",
            "25|J7PZ7u|day-with-delete-impact|TRIP_SETTINGS|day-with-delete-impact",
            "26|lsr1I|empty-place-pool|PLACE_POOL|empty-place-pool",
            "27|S0psO|empty-search-result|SEARCH|empty-search-result",
            "28|P7k0M|offline-pending-routes|ITINERARY|batch6-waiting-for-network",
            "29|E3EhSv|failed-route|ITINERARY|batch6-failed-route",
            "30|EHOHC|map-consent-required|PERMISSION|batch6-map-consent-explanation",
            "31|yNKT4|add-success-result|WORKSPACE|add-success-result",
            "32|l2xCsM|itinerary-delete-impact|ITINERARY|itinerary-delete-impact",
            "33|cRdBn|long-add-target-day-list|TARGET_DAY|long-add-target-day-list",
            "34|JFhZ7|location-rationale|PERMISSION|batch6-location-explanation",
            "35|HYCsZ|location-permanently-denied|PERMISSION|batch6-location-settings-recovery",
            "36|zIbEu|empty-trip-list|TRIP_LIST|empty-trip-list",
            "37|Bcf6A|empty-day|ITINERARY|empty-day",
            "38|GJo79|search-network-error|SEARCH|search-network-error",
            "39|mGhKO|add-partial-success-result|WORKSPACE|add-partial-success-result",
            "40|IKTv5|dated-trip-settings|TRIP_SETTINGS|dated-trip-settings",
            "41|V6RALq|add-place-submitting|TARGET_DAY|add-place-submitting",
            "42|D3XZi|missing-add-target-day-result|WORKSPACE|missing-add-target-day-result",
            "43|KPBBb|add-undo-success-result|WORKSPACE|add-undo-success-result",
            "44|s1OvvX|search-loading|SEARCH|search-loading",
            "45|GoxB6|map-loading|WORKSPACE|batch6-map-loading",
            "46|U8R5i|map-load-error|WORKSPACE|batch6-map-failure",
            "47|yIGiQ|invalid-trip-form|CREATE_TRIP|invalid-trip-form",
            "48|OOEsk|itinerary-save-error|ITEM_EDITOR|batch6-edit-save-failure",
            "1|d1sTtb|trip-list-deleted-final-state|TRIP_LIST|variant-d1sTtb-deleted-final-state",
            "2|BrYVA|workspace-all-empty|WORKSPACE|workspace-all-empty",
            "2|jQhXs|place-pool-short-list|WORKSPACE|place-pool-short-list",
            "4|WFOpg|itinerary-all-empty|WORKSPACE|itinerary-all-empty",
            "4|nAdK8|batch5-itinerary-page|ITINERARY|batch5-itinerary-page",
            "4|mz2IS|batch5-item-edit-complete|ITEM_EDITOR|batch5-item-edit-complete",
            "4|eHTX3|batch5-single-day-route|ROUTE_EDITOR|batch5-single-day-route",
            "10|XsGon|only-collected-place-detail|WORKSPACE|only-collected-place-detail",
        )
        val identities = V1ScenarioFixtures.allIdentities

        assertEquals(expected.size, identities.size)
        assertEquals(expected, identities.map(V1ScenarioIdentity::signature).toSet())
        assertEquals(47, V1ScenarioFixtures.parentIdentities.size)
        assertEquals(
            V1ScenarioFixtures.scenarios.map(V1Scenario::declaredIdentity).toSet(),
            V1ScenarioFixtures.parentIdentities.toSet(),
        )
        assertEquals(
            V1ScenarioFixtures.scenarios.flatMap(V1Scenario::variants).map(V1ScenarioVariant::declaredIdentity).toSet(),
            V1ScenarioFixtures.variantIdentities.toSet(),
        )
        identities.forEach { identity ->
            val executable = V1ScenarioExecutableFactory.create(identity)
            assertEquals(identity, executable.declaredIdentity)
            assertEquals(identity.fixtureId, executable.fixture.id)
            assertEquals(identity.screen, executable.fixture.screen)
            assertEquals(identity.factoryIdentity, executable.factoryIdentity)
        }

        identities.forEach { identity ->
            val differentNumber = identities.first { it.parentNumber != identity.parentNumber }
            val differentFrame = identities.first { it.frameId != identity.frameId }
            val differentFixture = identities.first { it.fixtureId != identity.fixtureId }
            val differentScreen = identities.first { it.screen != identity.screen }
            val differentFactory = identities.first { it.factoryIdentity != identity.factoryIdentity }
            listOf(
                identity.copy(parentNumber = differentNumber.parentNumber),
                identity.copy(frameId = differentFrame.frameId),
                identity.copy(fixtureId = differentFixture.fixtureId),
                identity.copy(screen = differentScreen.screen),
                identity.copy(factoryIdentity = differentFactory.factoryIdentity),
            ).forEach { swapped ->
                assertThrows(IllegalArgumentException::class.java) {
                    V1ScenarioExecutableFactory.create(swapped)
                }
            }
        }
    }

    @Test
    fun workspaceAllEmptyFramesHaveTypedDeclaredVariants() {
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

    @Test
    fun batch6FramesKeepDistinctTypedFactoryIdentityAndExactScenarioBindings() {
        val scenarios = V1ScenarioFixtures.scenarios.associateBy(V1Scenario::number)

        assertEquals(8, Batch6TypedScenario.entries.size)
        Batch6TypedScenario.entries.forEach { expected ->
            val scenario = scenarios.getValue(expected.number)
            val executable = scenario.createExecutable()

            assertEquals(expected.frameId, scenario.frameId)
            assertEquals(expected.fixtureId, executable.fixture.id)
            assertEquals(expected.screen, executable.fixture.screen)
            assertEquals(expected.factoryIdentity, executable.factoryIdentity)
        }
        assertEquals(
            Batch6TypedScenario.entries.map(Batch6TypedScenario::factoryIdentity).toSet().size,
            Batch6TypedScenario.entries.size,
        )
        assertEquals(47, V1ScenarioFixtures.scenarios.size)
    }

    @Test
    fun batch6FramesDeclareControlledEvidenceAndOnlyObservedHigherLevelBoundaries() {
        val evidence = VisualBatch0EvidenceTest.batch6Evidence

        assertEquals(Batch6FrameCheckpoint.entries.map(Batch6FrameCheckpoint::frameId).toSet(), evidence.keys)
        evidence.forEach { (frameId, records) ->
            val checkpoint = Batch6FrameCheckpoint.entries.single { it.frameId == frameId }
            val scenario = V1ScenarioFixtures.scenarios.single { it.frameId == frameId }

            assertTrue(records.any { record ->
                record.checkpoint == checkpoint &&
                    record.scenario.number == scenario.number &&
                    record.fixtureId == scenario.createExecutable().fixture.id &&
                    record.host == EvidenceHost.ProductionCompose &&
                    record.stateSource == EvidenceStateSource.ControlledUiState
            })
            records.forEach { record ->
                assertEquals(frameId, record.checkpoint.frameId)
                assertEquals(frameId, record.scenario.frameId)
                assertEquals(record.fixtureId, record.executable().fixture.id)
                assertTrue(record.automatedEntry.isExecutableTestReference())
                assertTrue(record.limitations.isNotBlank())
            }
        }
        assertTrue(evidence.getValue("P7k0M").any {
            it.host == EvidenceHost.ProductionRepository &&
                it.stateSource == EvidenceStateSource.FileBackedRoomReopen
        })
        assertTrue(evidence.getValue("EHOHC").any {
            it.host == EvidenceHost.ProductionAppNavigation &&
                it.stateSource == EvidenceStateSource.HandwrittenRepositoryFakes &&
                it.mapSurface == EvidenceMapSurface.None &&
                it.permissionSurface == EvidencePermissionSurface.ControlledSnapshot
        })
        setOf("JFhZ7", "HYCsZ").forEach { frameId ->
            assertTrue(evidence.getValue(frameId).any {
                it.host == EvidenceHost.ProductionAppNavigation &&
                    it.stateSource == EvidenceStateSource.HandwrittenRepositoryFakes &&
                    it.mapSurface == EvidenceMapSurface.RecordingFakeMapHost &&
                    it.permissionSurface == EvidencePermissionSurface.ActivityResultContract
            })
        }
        assertTrue(evidence.getValue("U8R5i").any {
            it.mapSurface == EvidenceMapSurface.FailureInjectingFakeMapHost
        })
        evidence.filterKeys { it !in setOf("EHOHC", "JFhZ7", "HYCsZ") }.values.flatten().forEach {
            assertEquals(EvidencePermissionSurface.None, it.permissionSurface)
        }
        assertTrue(evidence.getValue("GoxB6").any {
            it.mapSurface == EvidenceMapSurface.RecordingFakeMapHost
        })
        assertTrue(evidence.getValue("U8R5i").any {
            it.mapSurface == EvidenceMapSurface.FailureInjectingFakeMapHost
        })
        assertTrue(evidence.getValue("OOEsk").any {
            it.host == EvidenceHost.ProductionAppNavigation &&
                it.stateSource == EvidenceStateSource.InMemoryRoomNavigation &&
                it.mapSurface == EvidenceMapSurface.RecordingFakeMapHost
        })
        assertTrue(evidence.values.flatten().none {
            it.host == EvidenceHost.PhysicalDevice ||
                it.stateSource == EvidenceStateSource.InstalledAppRestart ||
                it.mapSurface == EvidenceMapSurface.RealAmap ||
                it.permissionSurface == EvidencePermissionSurface.AndroidSystem
        })
    }

    private fun String.isExecutableTestReference(): Boolean {
        val (simpleClassName, methodName) = split('#', limit = 2).let { parts ->
            require(parts.size == 2) { "Evidence entry must be Class#method: $this" }
            parts[0] to parts[1]
        }
        val className = simpleClassName.takeIf { it.startsWith("com.yangchengwei.easytrip.") }
            ?: "com.yangchengwei.easytrip.$simpleClassName"
        val testClass = Class.forName(className)
        return testClass.declaredMethods.any { method ->
            method.name == methodName && method.getAnnotation(Test::class.java) != null
        }
    }

    @Test fun P7k0MExecutesBatch6TypedScenario() = executeBatch6Scenario(Batch6TypedScenario.WaitingForNetwork)

    @Test fun E3EhSvExecutesBatch6TypedScenario() = executeBatch6Scenario(Batch6TypedScenario.FailedRoute)

    @Test fun EHOHCExecutesBatch6TypedScenario() = executeBatch6Scenario(Batch6TypedScenario.MapConsentExplanation)

    @Test fun JFhZ7ExecutesBatch6TypedScenario() = executeBatch6Scenario(Batch6TypedScenario.LocationExplanation)

    @Test fun HYCsZExecutesBatch6TypedScenario() = executeBatch6Scenario(Batch6TypedScenario.LocationSettingsRecovery)

    @Test fun GoxB6ExecutesBatch6TypedScenario() = executeBatch6Scenario(Batch6TypedScenario.MapLoading)

    @Test fun U8R5iExecutesBatch6TypedScenario() = executeBatch6Scenario(Batch6TypedScenario.MapFailure)

    @Test fun OOEskExecutesBatch6TypedScenario() = executeBatch6Scenario(Batch6TypedScenario.EditSaveFailure)

    private fun executeBatch6Scenario(expected: Batch6TypedScenario) {
        val scenario = V1ScenarioFixtures.scenarios.single { it.number == expected.number }
        val executable = scenario.createExecutable()
        executable.setup()
        executable.render(compose)
        compose.waitForIdle()
        executable.actions(compose)
        compose.waitForIdle()
        executable.assertions(compose)
        assertEquals(expected.frameId, scenario.frameId)
        assertEquals(expected.fixtureId, executable.fixture.id)
        assertEquals(expected.factoryIdentity, executable.factoryIdentity)
    }

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
            assertTrue("scenario ${scenario.number} declared metadata path", executable.declaredPath.steps.isNotEmpty())
            assertEquals(
                "scenario ${scenario.number} declared metadata target",
                executable.fixture.screen,
                executable.declaredPath.steps.last(),
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
