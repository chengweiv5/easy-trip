package com.yangchengwei.easytrip.route.domain

import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TransportModeRecommenderTest {
    private val subject = TransportModeRecommender()
    @Test fun `flexible mode uses inclusive distance boundaries`() {
        assertEquals(TransportMode.WALK, subject.recommend(TravelMode.FLEXIBLE, 0.0))
        assertEquals(TransportMode.WALK, subject.recommend(TravelMode.FLEXIBLE, 1_000.0))
        assertEquals(TransportMode.TAXI, subject.recommend(TravelMode.FLEXIBLE, 1_000.01))
        assertEquals(TransportMode.TAXI, subject.recommend(TravelMode.FLEXIBLE, 20_000.0))
        assertEquals(TransportMode.TRANSIT, subject.recommend(TravelMode.FLEXIBLE, 20_000.01))
    }
    @Test fun `self drive always recommends drive`() {
        listOf(0.0, 1_000.0, 1_000.01, 20_000.0, 20_000.01).forEach {
            assertEquals(TransportMode.DRIVE, subject.recommend(TravelMode.SELF_DRIVE, it))
        }
    }
}
