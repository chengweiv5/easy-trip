package com.yangchengwei.easytrip.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class MapLocateRequestBaselineTrackerTest {
    @Test fun firstMountPreservesInitialBaselineForRequestRaisedBeforeHostExists() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)

        assertEquals(0, tracker.baselineForMount(attemptId = 0, currentRequest = 1))
    }

    @Test fun sameAttemptRemountUsesOnlyRecordedConsumedBaseline() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.baselineForMount(attemptId = 0, currentRequest = 0)

        assertEquals(0, tracker.baselineForMount(attemptId = 0, currentRequest = 1))
        tracker.recordConsumedRequest(attemptId = 0, request = 1)
        assertEquals(1, tracker.baselineForMount(attemptId = 0, currentRequest = 1))
    }

    @Test fun readyDoesNotInferThatCurrentRequestWasConsumed() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.baselineForMount(attemptId = 0, currentRequest = 0)

        tracker.finishReadyAttempt(attemptId = 0)

        assertEquals(0, tracker.baselineForMount(attemptId = 0, currentRequest = 1))
    }

    @Test fun immediateRetryCanReadLatestFailedRequestWithoutWaitingForUiEffect() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.baselineForMount(attemptId = 0, currentRequest = 0)
        tracker.finishFailedAttempt(attemptId = 0, currentRequest = 0)

        assertEquals(1, tracker.pendingRequestWhileFailed(currentRequest = 1))
    }

    @Test fun replacementMountBaselinesCurrentRequestByDefault() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.baselineForMount(attemptId = 0, currentRequest = 1)

        assertEquals(2, tracker.baselineForMount(attemptId = 1, currentRequest = 2))
    }

    @Test fun replacementMountDoesNotReplayTheInitialRequest() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.baselineForMount(attemptId = 0, currentRequest = 1)

        assertEquals(1, tracker.baselineForMount(attemptId = 1, currentRequest = 1))
    }

    @Test fun loadingRequestRemainsPendingWhenAttemptFailsBeforeConsumingIt() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.baselineForMount(attemptId = 0, currentRequest = 0)

        assertEquals(1, tracker.finishFailedAttempt(attemptId = 0, currentRequest = 1))
        tracker.forwardPendingRequest(attemptId = 1, request = 1)
        assertEquals(0, tracker.baselineForMount(attemptId = 1, currentRequest = 1))
    }

    @Test fun consumedRequestIsNotReplayedWhenAttemptLaterFails() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.baselineForMount(attemptId = 0, currentRequest = 0)
        tracker.recordConsumedRequest(attemptId = 0, request = 1)

        assertEquals(null, tracker.finishFailedAttempt(attemptId = 0, currentRequest = 1))
    }

    @Test fun requestRaisedAfterFailureCanBeForwardedByExplicitRetry() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.baselineForMount(attemptId = 0, currentRequest = 0)
        tracker.finishFailedAttempt(attemptId = 0, currentRequest = 0)

        assertEquals(1, tracker.pendingRequestWhileFailed(currentRequest = 1))
        tracker.forwardPendingRequest(attemptId = 1, request = 1)
        assertEquals(0, tracker.baselineForMount(attemptId = 1, currentRequest = 1))
    }

    @Test fun successfulForwardedAttemptClearsReplayBaselineBeforeSameAttemptRemount() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.baselineForMount(attemptId = 0, currentRequest = 0)
        tracker.finishFailedAttempt(attemptId = 0, currentRequest = 1)
        tracker.forwardPendingRequest(attemptId = 1, request = 1)
        assertEquals(0, tracker.baselineForMount(attemptId = 1, currentRequest = 1))

        tracker.recordConsumedRequest(attemptId = 1, request = 1)
        tracker.finishReadyAttempt(attemptId = 1)

        assertEquals(1, tracker.baselineForMount(attemptId = 1, currentRequest = 1))
    }

    @Test fun failedForwardedAttemptCanForwardStillUnconsumedRequestAgainWithoutSameAttemptReplay() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.baselineForMount(attemptId = 0, currentRequest = 0)
        tracker.finishFailedAttempt(attemptId = 0, currentRequest = 1)
        tracker.forwardPendingRequest(attemptId = 1, request = 1)
        assertEquals(0, tracker.baselineForMount(attemptId = 1, currentRequest = 1))

        assertEquals(1, tracker.finishFailedAttempt(attemptId = 1, currentRequest = 1))
        assertEquals(1, tracker.baselineForMount(attemptId = 1, currentRequest = 1))
        tracker.forwardPendingRequest(attemptId = 2, request = 1)
        assertEquals(0, tracker.baselineForMount(attemptId = 2, currentRequest = 1))
    }

    @Test fun duplicateRetryCannotReplaceForwardedRequestForSameAttempt() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.forwardPendingRequest(attemptId = 1, request = 1)

        assertEquals(false, tracker.forwardPendingRequest(attemptId = 1, request = 2))
        assertEquals(0, tracker.baselineForMount(attemptId = 1, currentRequest = 2))
    }

    @Test fun consentReplacementUsesCurrentRequestAsConsumedBaseline() {
        val tracker = MapLocateRequestBaselineTracker(initialRequest = 0)
        tracker.baselineForMount(attemptId = "first-consent", currentRequest = 0)

        assertEquals(1, tracker.baselineForMount(attemptId = "replacement-consent", currentRequest = 1))
    }
}
