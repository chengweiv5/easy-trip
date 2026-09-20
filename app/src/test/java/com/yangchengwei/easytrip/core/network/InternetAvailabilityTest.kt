package com.yangchengwei.easytrip.core.network

import org.junit.Assert.*
import org.junit.Test

class InternetAvailabilityTest {
    @Test fun mobileInternetCanBeUsedWithoutSystemValidation() {
        assertTrue(canAttemptInternet(hasInternet = true, captivePortal = false))
    }

    @Test fun missingInternetAndCaptivePortalsRemainOffline() {
        assertFalse(canAttemptInternet(hasInternet = false, captivePortal = false))
        assertFalse(canAttemptInternet(hasInternet = true, captivePortal = true))
        assertTrue(canAttemptInternet(hasInternet = true, captivePortal = false))
    }

    @Test fun oldWifiCallbacksDoNotDisconnectNewMobileNetwork() {
        val state = DefaultNetworkState("wifi", true)
        state.available("cellular")
        state.capabilitiesChanged("cellular", true)
        state.lost("wifi")
        state.capabilitiesChanged("wifi", false)
        state.blockedChanged("wifi", true)
        assertTrue(state.isOnline)
        state.lost("cellular")
        assertFalse(state.isOnline)
        state.available("cellular-2")
        state.capabilitiesChanged("cellular-2", true)
        assertTrue(state.isOnline)
        state.blockedChanged("cellular-2", true)
        assertFalse(state.isOnline)
        state.blockedChanged("cellular-2", false)
        assertTrue(state.isOnline)
    }
}
