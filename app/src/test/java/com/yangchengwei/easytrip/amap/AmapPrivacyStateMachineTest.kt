package com.yangchengwei.easytrip.amap

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class AmapPrivacyStateMachineTest {
    @Test fun cannotDecideBeforePrivacyWasShown() {
        val state = AmapPrivacyStateMachine()
        assertThrows(IllegalStateException::class.java) { state.decide(true) }
    }

    @Test fun rejectionDoesNotIssueConsent() {
        val state = AmapPrivacyStateMachine()
        state.markShown()
        assertFalse(state.decide(false))
    }

    @Test fun shownThenAcceptedIssuesOpaqueConsent() {
        val state = AmapPrivacyStateMachine()
        state.markShown()
        assertNotNull(state.decide(true))
    }
}
