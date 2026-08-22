package com.yangchengwei.easytrip.route.domain

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.network.NetworkMonitor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteRefreshCoordinatorTest {
    @Test fun `stale version one response cannot overwrite completed version two`() = runTest {
        val repository = FakeRepository(leg(version = 1))
        val first = CompletableDeferred<RoutePlanOutcome>()
        val planner = FakePlanner { if (it.version == 1L) first.await() else RoutePlanOutcome.Success(result(2)) }
        val coordinator = DefaultRouteRefreshCoordinator(repository, planner, FakeNetworkMonitor(true))
        coordinator.start(backgroundScope); runCurrent()
        coordinator.overrideMode("leg", TransportMode.WALK); runCurrent()
        assertEquals(2L, repository.current.version); assertEquals(2, repository.current.distanceMeters)
        first.complete(RoutePlanOutcome.Success(result(1))); runCurrent()
        assertEquals(2L, repository.current.version); assertEquals(2, repository.current.distanceMeters)
    }

    @Test fun `start recovers calculating work left by process death`() = runTest {
        val repository = FakeRepository(leg(status = RouteStatus.CALCULATING))
        val planner = FakePlanner { RoutePlanOutcome.Success(result(11)) }
        DefaultRouteRefreshCoordinator(repository, planner, FakeNetworkMonitor(true)).start(backgroundScope)
        runCurrent()
        assertEquals(RouteStatus.SUCCESS, repository.current.status)
        assertEquals(1, planner.calls)
    }

    @Test fun `disconnecting in flight request cancels planner and waits for network`() = runTest {
        val repository = FakeRepository(leg())
        val gate = CompletableDeferred<RoutePlanOutcome>()
        val network = FakeNetworkMonitor(true)
        DefaultRouteRefreshCoordinator(repository, FakePlanner { gate.await() }, network).start(backgroundScope)
        runCurrent()
        assertEquals(RouteStatus.CALCULATING, repository.current.status)
        network.online.value = false; runCurrent()
        assertEquals(RouteStatus.WAITING_NETWORK, repository.current.status)
    }

    @Test fun `cancelling in flight request releases calculating claim`() = runTest {
        val repository = FakeRepository(leg())
        val gate = CompletableDeferred<RoutePlanOutcome>()
        val scopeJob = kotlinx.coroutines.Job()
        val scope = kotlinx.coroutines.CoroutineScope(StandardTestDispatcher(testScheduler) + scopeJob)
        DefaultRouteRefreshCoordinator(repository, FakePlanner { gate.await() }, FakeNetworkMonitor(true)).start(scope)
        runCurrent()
        assertEquals(RouteStatus.CALCULATING, repository.current.status)
        scopeJob.cancel(); runCurrent()
        assertEquals(RouteStatus.PENDING, repository.current.status)
    }

    @Test fun `offline pending waits without source call and resumes online`() = runTest {
        val repository = FakeRepository(leg())
        val network = FakeNetworkMonitor(false)
        val planner = FakePlanner { RoutePlanOutcome.Success(result(3)) }
        val coordinator = DefaultRouteRefreshCoordinator(repository, planner, network)
        coordinator.start(backgroundScope); runCurrent()
        assertEquals(RouteStatus.WAITING_NETWORK, repository.current.status); assertEquals(0, planner.calls)
        network.online.value = true; runCurrent()
        assertEquals(RouteStatus.SUCCESS, repository.current.status); assertEquals(1, planner.calls)
    }

    @Test fun `selected override is used instead of recommendation`() = runTest {
        val repository = FakeRepository(leg(recommended = TransportMode.TRANSIT))
        val planner = FakePlanner { RoutePlanOutcome.Success(result(4)) }
        val coordinator = DefaultRouteRefreshCoordinator(repository, planner, FakeNetworkMonitor(true))
        coordinator.overrideMode("leg", TransportMode.WALK); coordinator.start(backgroundScope); runCurrent()
        assertEquals(TransportMode.WALK, planner.requests.single().actualMode)
        assertEquals(TransportMode.WALK, repository.current.selectedMode)
    }

    @Test fun `network recovery retries transient failure but not business failure`() = runTest {
        val transient = FakeRepository(leg(status = RouteStatus.FAILED, errorKind = RouteErrorKind.TRANSIENT))
        val business = FakeRepository(leg(id = "business", status = RouteStatus.FAILED, errorKind = RouteErrorKind.NO_ROUTE))
        val network = FakeNetworkMonitor(false)
        val transientPlanner = FakePlanner { RoutePlanOutcome.Success(result(5)) }
        val businessPlanner = FakePlanner { RoutePlanOutcome.Success(result(6)) }
        DefaultRouteRefreshCoordinator(transient, transientPlanner, network).start(backgroundScope)
        DefaultRouteRefreshCoordinator(business, businessPlanner, network).start(backgroundScope)
        runCurrent(); network.online.value = true; runCurrent()
        assertEquals(RouteStatus.SUCCESS, transient.current.status); assertEquals(1, transientPlanner.calls)
        assertEquals(RouteStatus.FAILED, business.current.status); assertEquals(0, businessPlanner.calls)
    }

    @Test fun `start is idempotent and repository claim permits one request`() = runTest {
        val repository = FakeRepository(leg())
        val gate = CompletableDeferred<RoutePlanOutcome>()
        val planner = FakePlanner { gate.await() }
        val coordinator = DefaultRouteRefreshCoordinator(repository, planner, FakeNetworkMonitor(true))
        coordinator.start(backgroundScope); coordinator.start(backgroundScope); runCurrent()
        assertEquals(1, planner.calls); assertFalse(repository.claimIfVersionMatches("leg", 1))
        gate.complete(RoutePlanOutcome.Success(result(7))); runCurrent()
        assertEquals(RouteStatus.SUCCESS, repository.current.status)
    }




    @Test fun `repository refresh failure is reported without cancelling owner scope`() = runTest {
        val repository = FakeRepository(leg(), refreshFailure = IllegalStateException("db"))
        val errors = mutableListOf<Throwable>()
        val scopeJob = kotlinx.coroutines.Job()
        val scope = kotlinx.coroutines.CoroutineScope(StandardTestDispatcher(testScheduler) + scopeJob)
        DefaultRouteRefreshCoordinator(repository, FakePlanner { RoutePlanOutcome.Success(result(1)) }, FakeNetworkMonitor(true), errors::add).start(scope)
        runCurrent()
        assertEquals(listOf("db"), errors.map(Throwable::message))
        assertTrue(scopeJob.isActive)
        scopeJob.cancel()
    }

    @Test fun `requeue actor reports failure and continues with next rising edge`() = runTest {
        val repository = FakeRepository(leg(status = RouteStatus.FAILED, errorKind = RouteErrorKind.TRANSIENT), requeueFailure = IllegalStateException("db"))
        val network = FakeNetworkMonitor(false)
        val errors = mutableListOf<Throwable>()
        val scopeJob = kotlinx.coroutines.Job()
        val scope = kotlinx.coroutines.CoroutineScope(StandardTestDispatcher(testScheduler) + scopeJob)
        DefaultRouteRefreshCoordinator(repository, FakePlanner { RoutePlanOutcome.Success(result(1)) }, network, { error -> errors.add(error) }).start(scope)
        runCurrent(); network.online.value = true; runCurrent()
        assertEquals(1, repository.requeueCalls); assertEquals(1, errors.size); assertTrue(scopeJob.isActive)
        network.online.value = false; runCurrent(); network.online.value = true; runCurrent()
        assertEquals(2, repository.requeueCalls); assertEquals(1, errors.size); assertTrue(scopeJob.isActive)
        scopeJob.cancel()
    }

    @Test fun `network collector queues rising edges while requeue is suspended`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeRepository(leg(status = RouteStatus.FAILED, errorKind = RouteErrorKind.TRANSIENT), gate)
        val network = FakeNetworkMonitor(false)
        val coordinator = DefaultRouteRefreshCoordinator(repository, FakePlanner { RoutePlanOutcome.Success(result(8)) }, network)
        coordinator.start(backgroundScope); runCurrent()
        network.online.value = true; runCurrent()
        assertEquals(1, repository.requeueCalls)
        network.online.value = false; runCurrent(); network.online.value = true; runCurrent()
        assertEquals(1, repository.requeueCalls)
        gate.complete(Unit); runCurrent()
        assertEquals(2, repository.requeueCalls)
    }

    @Test fun `scope cancellation cancels suspended requeue jobs`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeRepository(leg(status = RouteStatus.FAILED, errorKind = RouteErrorKind.TRANSIENT), gate)
        val network = FakeNetworkMonitor(false)
        val scopeJob = kotlinx.coroutines.Job()
        val scope = kotlinx.coroutines.CoroutineScope(StandardTestDispatcher(testScheduler) + scopeJob)
        DefaultRouteRefreshCoordinator(repository, FakePlanner { RoutePlanOutcome.Success(result(1)) }, network).start(scope)
        runCurrent(); network.online.value = true; runCurrent(); runCurrent()
        assertEquals(1, repository.requeueCalls)
        scopeJob.cancel(); runCurrent()
        assertEquals(0, scopeJob.children.count())
    }

    @Test fun `initial online resumes transient failures once`() = runTest {
        val repository = FakeRepository(leg(status = RouteStatus.FAILED, errorKind = RouteErrorKind.TRANSIENT))
        val network = FakeNetworkMonitor(true)
        val coordinator = DefaultRouteRefreshCoordinator(repository, FakePlanner { RoutePlanOutcome.Success(result(1)) }, network)
        coordinator.start(backgroundScope); runCurrent()
        assertEquals(1, repository.requeueCalls)
        assertEquals(RouteStatus.SUCCESS, repository.current.status)
        network.online.value = true; runCurrent()
        assertEquals(1, repository.requeueCalls)
    }

    @Test fun `offline to online edge requeues after initial startup scan`() = runTest {
        val repository = FakeRepository(leg(status = RouteStatus.FAILED, errorKind = RouteErrorKind.TRANSIENT))
        val network = FakeNetworkMonitor(false)
        val coordinator = DefaultRouteRefreshCoordinator(repository, FakePlanner { RoutePlanOutcome.Success(result(9)) }, network)
        coordinator.start(backgroundScope); runCurrent()
        assertEquals(0, repository.requeueCalls)
        network.online.value = true; runCurrent()
        assertEquals(1, repository.requeueCalls)
        assertEquals(RouteStatus.SUCCESS, repository.current.status)
    }

    private fun leg(id: String = "leg", version: Long = 1, status: RouteStatus = RouteStatus.PENDING, recommended: TransportMode = TransportMode.TAXI, errorKind: RouteErrorKind? = null) =
        RouteLegWithEndpoints(id, version, status, GeoPoint(39.9,116.3), GeoPoint(39.91,116.31), "北京", "上海", recommended, null, errorKind = errorKind)
    private fun result(distance: Int) = RouteResult(distance, 10, listOf(GeoPoint(1.0,2.0), GeoPoint(3.0,4.0)))
}

private class FakeNetworkMonitor(initial: Boolean) : NetworkMonitor {
    val online = MutableStateFlow(initial)
    override val isOnline = online
}
private class FakePlanner(private val block: suspend (RouteLegWithEndpoints) -> RoutePlanOutcome) : RoutePlanner {
    var calls = 0; val requests = mutableListOf<RouteLegWithEndpoints>()
    override suspend fun plan(leg: RouteLegWithEndpoints): RoutePlanOutcome { calls++; requests += leg; return block(leg) }
}
private class FakeRepository(initial: RouteLegWithEndpoints, private val requeueGate: CompletableDeferred<Unit>? = null, private var requeueFailure: Throwable? = null, private var refreshFailure: Throwable? = null) : RouteLegRepository {
    private val state = MutableStateFlow(listOf(initial)); val current get() = state.value.single(); var requeueCalls = 0
    override fun observeDay(dayId: String) = throw UnsupportedOperationException()
    override fun observePending(): Flow<List<RouteLegWithEndpoints>> = state
    override suspend fun requeueTransientFailures(): Int { requeueCalls++; requeueFailure?.let { requeueFailure=null; throw it }; requeueGate?.await(); var count=0; state.update { values -> values.map { if(it.status==RouteStatus.FAILED&&it.errorKind==RouteErrorKind.TRANSIENT){count++;it.copy(status=RouteStatus.PENDING,version=it.version+1)}else it } }; return count }
    override suspend fun repairCorruptPolyline(legId:String,version:Long)=false
    override suspend fun get(legId: String) = state.value.singleOrNull { it.id == legId }
    override suspend fun claimIfVersionMatches(legId: String, version: Long): Boolean {
        refreshFailure?.let { refreshFailure = null; throw it }
        return mutate(legId, version) {
            if (it.status !in setOf(RouteStatus.PENDING, RouteStatus.WAITING_NETWORK)) null else it.copy(status=RouteStatus.CALCULATING,errorKind=null,errorCode=null)
        }
    }
    override suspend fun waitForNetworkIfVersionMatches(legId: String, version: Long) = mutate(legId, version) {
        if (it.status == RouteStatus.PENDING) it.copy(status=RouteStatus.WAITING_NETWORK) else null
    }
    override suspend fun releaseClaimIfVersionMatches(legId: String, version: Long, online: Boolean) = mutate(legId, version) {
        if (it.status != RouteStatus.CALCULATING) null else it.copy(status = if (online) RouteStatus.PENDING else RouteStatus.WAITING_NETWORK)
    }
    override suspend fun completeIfVersionMatches(legId: String, version: Long, result: RouteResult) = mutate(legId, version) {
        if (it.status != RouteStatus.CALCULATING) null else it.copy(status=RouteStatus.SUCCESS,distanceMeters=result.distanceMeters,errorKind=null)
    }
    override suspend fun failIfVersionMatches(legId: String, version: Long, failure: RoutePlanOutcome.Failure) = mutate(legId, version) {
        if (it.status != RouteStatus.CALCULATING) null else it.copy(status=RouteStatus.FAILED,errorKind=failure.kind,errorCode=failure.code)
    }
    override suspend fun overrideMode(legId: String, mode: TransportMode, online: Boolean): Boolean { val item=get(legId)?:return false; return mutate(legId,item.version){it.copy(version=it.version+1,selectedMode=mode,status=if(online)RouteStatus.PENDING else RouteStatus.WAITING_NETWORK,distanceMeters=null,errorKind=null,errorCode=null)} }
    override suspend fun retry(legId: String, online: Boolean): Boolean { val item=get(legId)?:return false; return mutate(legId,item.version){it.copy(version=it.version+1,status=if(online)RouteStatus.PENDING else RouteStatus.WAITING_NETWORK,distanceMeters=null,errorKind=null,errorCode=null)} }
    override suspend fun recoverInterruptedCalculations(online: Boolean): Int {
        var count = 0
        state.update { values -> values.map { item ->
            if (item.status == RouteStatus.CALCULATING) {
                count++
                item.copy(status = if (online) RouteStatus.PENDING else RouteStatus.WAITING_NETWORK)
            } else item
        } }
        return count
    }
    private fun mutate(id:String,version:Long,change:(RouteLegWithEndpoints)->RouteLegWithEndpoints?):Boolean { var changed=false; state.update { values -> values.map { item -> if(item.id!=id||item.version!=version)item else change(item)?.also{changed=true}?:item } }; return changed }
}
