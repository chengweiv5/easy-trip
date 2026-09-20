package com.yangchengwei.easytrip.route.domain

import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode

class TransportModeRecommender {
    fun recommend(travelMode: TravelMode, straightLineMeters: Double): TransportMode = when {
        travelMode == TravelMode.SELF_DRIVE -> TransportMode.DRIVE
        straightLineMeters in 0.0..1_000.0 -> TransportMode.WALK
        else -> TransportMode.TAXI
    }
}
