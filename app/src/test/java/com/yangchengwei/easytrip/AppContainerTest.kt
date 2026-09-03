package com.yangchengwei.easytrip

import com.yangchengwei.easytrip.amap.AmapConsentFact
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.amap.ConsentRegistry
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.route.domain.RouteRefreshCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppContainerTest {
    @Test
    fun `same accepted generation reuses one runtime session`() {
        val factory = RecordingRuntimeSessionFactory()
        val manager = manager(factory)
        val accepted = acceptedFact(1).fact

        val first = manager.runtimeSession(accepted)
        val second = manager.runtimeSession(accepted)

        assertSame(first, second)
        assertEquals(listOf(1L), factory.createdGenerations)
    }

    @Test
    fun `stopping runtime after decline invalidates token and cancels coordinator scope`() {
        val factory = RecordingRuntimeSessionFactory()
        val manager = manager(factory)
        val accepted = acceptedFact(1)
        val first = manager.runtimeSession(accepted.fact)

        accepted.registry.decide(false)
        manager.stopRuntimeSession()

        assertFalse(first.token.isActive())
        assertFalse(factory.createdJobs.single().isActive)
    }

    @Test
    fun `accepted fact hides session from previous generation`() {
        val current = acceptedFact(2).fact
        val stale = session(acceptedFact(1).fact)

        val resolved = resolveAmapRuntimeDependencies(current, stale, AmapRuntimeDependencies())

        assertEquals(AmapRuntimeDependencies(), resolved)
    }

    @Test
    fun `accepted fact hides session with different token identity`() {
        val current = acceptedFact(1).fact
        val stale = session(acceptedFact(1).fact)

        val resolved = resolveAmapRuntimeDependencies(current, stale, AmapRuntimeDependencies())

        assertEquals(AmapRuntimeDependencies(), resolved)
    }

    @Test
    fun `accepted fact exposes only matching generation and token session`() {
        val current = acceptedFact(1).fact
        val matching = session(current)

        val resolved = resolveAmapRuntimeDependencies(current, matching, AmapRuntimeDependencies())

        assertSame(current.token, resolved.token)
        assertSame(FakePlaceSearchDataSource, resolved.placeSearchDataSource)
        assertSame(FakeRouteRefreshCoordinator, resolved.routeRefreshCoordinator)
    }

    @Test
    fun `legacy runtime remains available without consent store fact`() {
        val legacy = AmapRuntimeDependencies(
            token = acceptedFact(1).fact.token,
            placeSearchDataSource = FakePlaceSearchDataSource,
            routeRefreshCoordinator = FakeRouteRefreshCoordinator,
        )

        assertSame(legacy, resolveAmapRuntimeDependencies(null, null, legacy))
    }

    @Test
    fun `declined fact disables all legacy static runtime dependencies`() {
        val legacy = AmapRuntimeDependencies(
            token = acceptedFact(1).fact.token,
            placeSearchDataSource = FakePlaceSearchDataSource,
            routeRefreshCoordinator = FakeRouteRefreshCoordinator,
        )

        val resolved = resolveAmapRuntimeDependencies(
            consentFact = AmapConsentFact.Declined(2),
            session = null,
            legacy = legacy,
        )

        assertEquals(AmapRuntimeDependencies(), resolved)
    }

    @Test
    fun `same generation with different token identity replaces runtime session`() {
        val factory = RecordingRuntimeSessionFactory()
        val manager = manager(factory)
        val first = manager.runtimeSession(acceptedFact(1).fact)

        val second = manager.runtimeSession(acceptedFact(1).fact)

        assertNotSame(first, second)
        assertFalse(factory.createdJobs.first().isActive)
    }

    @Test
    fun `new accepted generation creates a distinct runtime session`() {
        val factory = RecordingRuntimeSessionFactory()
        val manager = manager(factory)
        val first = manager.runtimeSession(acceptedFact(1).fact)

        val second = manager.runtimeSession(acceptedFact(2).fact)

        assertNotSame(first, second)
        assertEquals(2L, second.generation)
        assertFalse(factory.createdJobs.first().isActive)
        assertTrue(factory.createdJobs.last().isActive)
    }

    private fun session(fact: AmapConsentFact.Accepted) = AmapRuntimeSession(
        fact.generation,
        fact.token,
        FakePlaceSearchDataSource,
        FakeRouteRefreshCoordinator,
    )

    private fun manager(factory: RecordingRuntimeSessionFactory) = AmapRuntimeSessionManager(
        applicationScope = CoroutineScope(StandardTestDispatcher()),
        factory = factory::create,
    )

    private fun acceptedFact(generation: Long): AcceptedFixture {
        val registry = ConsentRegistry()
        val snapshot = registry.decide(true)
        return AcceptedFixture(
            AmapConsentFact.Accepted(generation, AmapConsentToken.issue(registry, snapshot.generation)),
            registry,
        )
    }

    private data class AcceptedFixture(
        val fact: AmapConsentFact.Accepted,
        val registry: ConsentRegistry,
    )
}

private class RecordingRuntimeSessionFactory {
    val createdGenerations = mutableListOf<Long>()
    val createdJobs = mutableListOf<Job>()

    fun create(generation: Long, token: AmapConsentToken, scope: CoroutineScope): AmapRuntimeSession {
        createdGenerations += generation
        createdJobs += requireNotNull(scope.coroutineContext[Job])
        return AmapRuntimeSession(generation, token, FakePlaceSearchDataSource, FakeRouteRefreshCoordinator)
    }
}

private object FakePlaceSearchDataSource : PlaceSearchDataSource {
    override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> = emptyList()
}

private object FakeRouteRefreshCoordinator : RouteRefreshCoordinator {
    override fun start(scope: CoroutineScope) = Unit
    override suspend fun retry(legId: String) = false
    override suspend fun updateDetails(legId: String, selectedModeOverride: TransportMode?, durationOverrideSeconds: Int?, note: String?) = false
}
