package com.yangchengwei.easytrip.amap

import com.yangchengwei.easytrip.decideAmapPrivacy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AmapConsentStoreTest {
    @Test
    fun initialStateWithoutPersistedDecisionIsUndecided() {
        val store = createStore(decision = null)

        assertTrue(store.state.value.fact is AmapConsentFact.Undecided)
    }

    @Test
    fun repeatedReportShownCallsReportOnceAndExposeShownFact() = runTest {
        val reporter = ControllablePrivacyReporter()
        val store = createStore(reporter = reporter)

        store.reportShown().getOrThrow()
        store.reportShown().getOrThrow()

        assertTrue(store.shown.value)
        assertEquals(1, reporter.shownReports)
    }

    @Test
    fun concurrentReportShownCallsShareOneReporterCall() = runTest {
        val reporter = ControllablePrivacyReporter(pauseShown = true)
        val store = createStore(reporter = reporter)
        val first = async { store.reportShown() }
        reporter.awaitShownReport()
        val second = async { store.reportShown() }
        reporter.completeShownReport()

        first.await().getOrThrow()
        second.await().getOrThrow()
        assertTrue(store.shown.value)
        assertEquals(1, reporter.shownReports)
    }

    @Test
    fun failedReportShownCanRetryWithoutPublishingShown() = runTest {
        val reporter = ControllablePrivacyReporter()
        val store = createStore(reporter = reporter)
        reporter.failShownNext(IllegalStateException("show failed"))

        assertTrue(store.reportShown().isFailure)
        assertFalse(store.shown.value)
        store.reportShown().getOrThrow()

        assertTrue(store.shown.value)
        assertEquals(2, reporter.shownReports)
    }

    @Test
    fun cancelledReportShownRethrowsAndCanRetry() = runTest {
        val reporter = ControllablePrivacyReporter()
        val store = createStore(reporter = reporter)
        reporter.failShownNext(CancellationException("cancelled"))

        try {
            store.reportShown()
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            Unit
        }
        assertFalse(store.shown.value)
        store.reportShown().getOrThrow()

        assertTrue(store.shown.value)
        assertEquals(2, reporter.shownReports)
    }

    @Test
    fun failedReportShownCanRetryBeforeDecision() = runTest {
        val reporter = ControllablePrivacyReporter()
        val store = createStore(reporter = reporter)
        reporter.failShownNext(IllegalStateException("show failed"))

        assertTrue(store.reportShown().isFailure)
        assertFalse(store.shown.value)

        store.reportShown().getOrThrow()
        val decision = store.decide(false)

        assertTrue(decision.isSuccess)
        assertTrue(store.state.value.fact is AmapConsentFact.Declined)
    }


    @Test
    fun acceptedAndDeclinedDecisionsRestoreAcrossStoreInstances() = runTest {
        val persistence = MemoryConsentPersistence()

        createStore(persistence).decide(true)
        assertTrue(createStore(persistence).state.value.fact is AmapConsentFact.Accepted)

        createStore(persistence).decide(false)
        assertTrue(createStore(persistence).state.value.fact is AmapConsentFact.Declined)
    }

    @Test
    fun withdrawalInvalidatesPreviouslyIssuedToken() = runTest {
        val store = createStore()
        store.decide(true)
        val token = (store.state.value.fact as AmapConsentFact.Accepted).token

        store.decide(false)

        assertFalse(token.isActive())
        assertTrue(store.state.value.fact is AmapConsentFact.Declined)
    }

    @Test
    fun privacyApiFailureKeepsPreviousConsentFact() = runTest {
        val reporter = ControllablePrivacyReporter()
        val store = createStore(reporter = reporter)
        store.decide(true)
        val accepted = store.state.value.fact
        reporter.failNext(IllegalStateException("privacy update failed"))

        store.decide(false)

        assertEquals(accepted, store.state.value.fact)
        assertEquals("地图授权更新失败，请重试", store.state.value.error)
    }

    @Test
    fun cancellationClearsUpdatingWithoutChangingFactOrPublishingError() = runTest {
        val reporter = ControllablePrivacyReporter()
        val store = createStore(reporter = reporter)
        store.decide(true)
        val accepted = store.state.value.fact
        reporter.failNext(CancellationException("cancelled"))

        try {
            store.decide(false)
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            Unit
        }

        assertEquals(accepted, store.state.value.fact)
        assertFalse(store.state.value.updating)
        assertNull(store.state.value.error)
    }

    @Test
    fun privacyApiFailureReturnsFailureResult() = runTest {
        val reporter = ControllablePrivacyReporter()
        val store = createStore(reporter = reporter)
        reporter.failNext(IllegalStateException("privacy update failed"))

        val result = store.decide(false)

        assertTrue(result.isFailure)
    }

    @Test
    fun failedDeclineApplicationDelegateKeepsAcceptedRuntimeFact() = runTest {
        val reporter = ControllablePrivacyReporter()
        val store = createStore(reporter = reporter)
        store.decide(true)
        val accepted = store.state.value.fact
        reporter.failNext(IllegalStateException("privacy update failed"))

        decideAmapPrivacy(store, accepted = false)

        assertEquals(accepted, store.state.value.fact)
        assertEquals("地图授权更新失败，请重试", store.state.value.error)
    }

    @Test
    fun staleGenerationCompletionCannotOverwriteNewerDecision() = runTest {
        val reporter = ControllablePrivacyReporter(pauseDecisions = true)
        val store = createStore(reporter = reporter)
        val accept = async { store.decide(true) }
        reporter.awaitDecision(true)
        val decline = async { store.decide(false) }
        reporter.completeDecision(true)
        reporter.awaitDecision(false)
        reporter.completeDecision(false)
        val declineResult = decline.await()
        val acceptResult = accept.await()

        assertTrue(acceptResult.isSuccess)
        assertTrue(declineResult.isSuccess)
        assertTrue(store.state.value.fact is AmapConsentFact.Declined)
        assertNull(store.state.value.error)
    }

    @Test
    fun newerDecisionWaitsForOlderReporterAndLeavesSdkAtNewestFact() = runTest {
        val reporter = SerialDecisionReporter()
        val store = createStore(reporter = reporter)
        val accept = async { store.decide(true) }
        reporter.awaitDecision(true)
        val decline = async { store.decide(false) }
        testScheduler.runCurrent()

        assertEquals(listOf(true), reporter.decisions)
        reporter.completeCurrent()
        reporter.awaitDecision(false)
        reporter.completeCurrent()

        assertTrue(accept.await().isSuccess)
        assertTrue(decline.await().isSuccess)
        assertEquals(false, reporter.finalDecision)
        assertTrue(store.state.value.fact is AmapConsentFact.Declined)
    }

    @Test
    fun cancellingDecisionWhileWaitingForOlderReporterDoesNotSupersedeOrLeaveStoreBusy() = runTest {
        val persistence = MemoryConsentPersistence()
        val reporter = SerialDecisionReporter()
        val store = createStore(persistence = persistence, reporter = reporter)
        val accept = async { store.decide(true) }
        reporter.awaitDecision(true)
        val cancelledDecline = async { store.decide(false) }
        testScheduler.runCurrent()

        cancelledDecline.cancel()
        try {
            cancelledDecline.await()
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            Unit
        }
        assertEquals(listOf(true), reporter.decisions)

        reporter.completeCurrent()
        assertTrue(accept.await().isSuccess)
        assertEquals(true, reporter.finalDecision)
        assertEquals(true, persistence.decision)
        assertTrue(store.state.value.fact is AmapConsentFact.Accepted)
        assertFalse(store.state.value.updating)
        assertNull(store.state.value.error)

        val decline = async { store.decide(false) }
        reporter.awaitDecision(false)
        reporter.completeCurrent()

        assertTrue(decline.await().isSuccess)
        assertEquals(false, reporter.finalDecision)
        assertEquals(false, persistence.decision)
        assertTrue(store.state.value.fact is AmapConsentFact.Declined)
        assertFalse(store.state.value.updating)
        assertNull(store.state.value.error)
    }

    private fun createStore(
        persistence: MemoryConsentPersistence = MemoryConsentPersistence(),
        decision: Boolean? = null,
        reporter: AmapPrivacyReporter = ControllablePrivacyReporter(),
    ): AmapConsentStore {
        persistence.decision = decision ?: persistence.decision
        return AmapConsentStore(persistence, reporter, ConsentRegistry())
    }

    private class MemoryConsentPersistence(
        var decision: Boolean? = null,
    ) : AmapConsentPersistence {
        override fun readDecision(): Boolean? = decision

        override fun writeDecision(accepted: Boolean) {
            decision = accepted
        }
    }

    private class SerialDecisionReporter : AmapPrivacyReporter {
        val decisions = mutableListOf<Boolean>()
        private val started = Channel<Boolean>(Channel.UNLIMITED)
        private var completion = CompletableDeferred<Unit>()
        var finalDecision: Boolean? = null
            private set

        override suspend fun reportShown() = Unit

        override suspend fun reportDecision(accepted: Boolean) {
            decisions += accepted
            started.send(accepted)
            completion.await()
            finalDecision = accepted
        }

        suspend fun awaitDecision(expected: Boolean) {
            assertEquals(expected, started.receive())
        }

        fun completeCurrent() {
            completion.complete(Unit)
            completion = CompletableDeferred()
        }
    }

    private class ControllablePrivacyReporter(
        private val pauseDecisions: Boolean = false,
        private val pauseShown: Boolean = false,
    ) : AmapPrivacyReporter {
        private val decisions = Channel<Boolean>(Channel.UNLIMITED)
        private val completions = mutableMapOf<Boolean, CompletableDeferred<Unit>>()
        private val shownStarted = CompletableDeferred<Unit>()
        private val shownCompletion = CompletableDeferred<Unit>()
        private var nextFailure: Throwable? = null
        private var nextShownFailure: Throwable? = null
        var shownReports = 0
            private set

        override suspend fun reportShown() {
            shownReports++
            nextShownFailure?.let {
                nextShownFailure = null
                throw it
            }
            if (pauseShown) {
                shownStarted.complete(Unit)
                shownCompletion.await()
            }
        }

        override suspend fun reportDecision(accepted: Boolean) {
            nextFailure?.let {
                nextFailure = null
                throw it
            }
            if (pauseDecisions) {
                val completion = CompletableDeferred<Unit>()
                completions[accepted] = completion
                decisions.send(accepted)
                completion.await()
            }
        }

        suspend fun awaitShownReport() {
            shownStarted.await()
        }

        fun completeShownReport() {
            shownCompletion.complete(Unit)
        }

        fun failShownNext(error: Throwable) {
            nextShownFailure = error
        }

        fun failNext(error: Throwable) {
            nextFailure = error
        }

        suspend fun awaitDecision(expected: Boolean) {
            assertEquals(expected, decisions.receive())
        }

        fun completeDecision(accepted: Boolean) {
            completions.getValue(accepted).complete(Unit)
        }
    }
}
