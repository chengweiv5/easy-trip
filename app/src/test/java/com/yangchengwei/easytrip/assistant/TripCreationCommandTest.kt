package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TripCreationCommandTest {
    private val item = ImportItem("A", 0, PlaceCandidate("p1", "博物馆", "地址",
        GeoPoint(30.2, 120.1), null, "杭州"), PlaceCategory.ATTRACTION)

    @Test fun immutableCreationIncludesMetadataAndEvidenceInDigest() {
        fun command(name: String = "旅行", date: LocalDate? = null, days: Int = 1,
            sources: List<String> = listOf("作者推荐博物馆")) = TripCreationCommand.create(
            name, date, days, TravelMode.FLEXIBLE, listOf(item), sources, "create-trip:v1:op", "trip")
        val base = command()
        assertEquals(base.digest, command(name = " 旅行 ").digest)
        assertNotEquals(base.digest, command(name = "另一旅行").digest)
        assertNotEquals(base.digest, command(date = LocalDate.of(2026, 10, 24)).digest)
        assertNotEquals(base.digest, command(days = 3).digest)
        assertNotEquals(base.digest, command(sources = listOf("另一个来源")).digest)
        assertEquals(base, TripCreationCommand.decode(base.encode()))
        assertTrue(runCatching { command(name = "") }.isFailure)
        assertTrue(runCatching { command(days = 31) }.isFailure)
        assertTrue(runCatching { TripCreationCommand.create("旅行", null, 1, TravelMode.FLEXIBLE,
            listOf(item, item.copy(itemId = "B")), emptyList()) }.isFailure)
    }
}
