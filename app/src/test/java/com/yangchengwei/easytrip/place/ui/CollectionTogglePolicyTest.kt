package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.SavedPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CollectionTogglePolicyTest {
    private val candidate = PlaceCandidate("poi-1", "故宫", "北京市东城区", GeoPoint(39.916, 116.397), "010")
    private val saved = SavedPlace("place-1", "trip-1", "poi-1", "故宫", "北京市东城区", GeoPoint(39.916, 116.397), "", emptyList())

    @Test fun `unsaved candidate saves`() {
        assertEquals(CollectionDecision.Save, decideCollectionToggle(candidate, null, null))
    }

    @Test fun `unused saved place removes immediately`() {
        assertEquals(CollectionDecision.RemoveNow("place-1"), decideCollectionToggle(candidate, saved, 0))
    }

    @Test fun `used saved place requires impact confirmation`() {
        assertEquals(CollectionDecision.Confirm("place-1", 4), decideCollectionToggle(candidate, saved, 4))
    }

    @Test fun `saved place rejects missing usage`() {
        assertThrows(IllegalArgumentException::class.java) {
            decideCollectionToggle(candidate, saved, null)
        }
    }

    @Test fun `saved place rejects negative usage`() {
        assertThrows(IllegalArgumentException::class.java) {
            decideCollectionToggle(candidate, saved, -1)
        }
    }
}
