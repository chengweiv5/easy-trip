package com.yangchengwei.easytrip.amap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentRevocationTest {
    @Test fun issuedTokenIsRejectedAfterWithdrawal() {
        val gate = TestConsentGate()
        gate.show()
        val token = gate.decide(true)!!
        token.validateActive()
        gate.decide(false)
        assertThrows(IllegalStateException::class.java) { token.validateActive() }
    }

    @Test fun denialFromAnotherGateInvalidatesExistingTokenInSharedRegistry() {
        val registry = ConsentRegistry()
        val first = TestConsentGate(registry).apply { show() }
        val second = TestConsentGate(registry).apply { show() }
        val token = first.decide(true)!!
        second.decide(false)
        assertThrows(IllegalStateException::class.java) { token.validateActive() }
    }

    @Test fun tokenCarriesReadOnlyActiveState() {
        val registry = ConsentRegistry()
        val gate = TestConsentGate(registry).apply { show() }
        val token = gate.decide(true)!!
        assertTrue(token.active.value.active)
        gate.decide(false)
        assertFalse(token.active.value.active)
    }
}
