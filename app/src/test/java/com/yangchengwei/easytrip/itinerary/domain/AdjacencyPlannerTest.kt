package com.yangchengwei.easytrip.itinerary.domain

import com.yangchengwei.easytrip.itinerary.data.defaultRecommendMode
import com.yangchengwei.easytrip.itinerary.data.haversineMeters
import com.yangchengwei.easytrip.place.data.SavedPlaceEntity
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.core.model.TravelMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdjacencyPlannerTest {
    @Test
    fun deletionCreatesBridgeAndDeletesInvalidEdges() {
        assertEquals(
            AdjacencyDiff(
                deleted = setOf(Edge("A", "B"), Edge("B", "C")),
                created = setOf(Edge("A", "C")),
            ),
            adjacencyDiff(old = listOf("A", "B", "C"), new = listOf("A", "C")),
        )
    }

    @Test
    fun insertionsAtHeadTailAndMiddleOnlyChangeTouchingEdges() {
        assertEquals(
            AdjacencyDiff(emptySet(), setOf(Edge("X", "A"))),
            adjacencyDiff(listOf("A", "B"), listOf("X", "A", "B")),
        )
        assertEquals(
            AdjacencyDiff(emptySet(), setOf(Edge("B", "X"))),
            adjacencyDiff(listOf("A", "B"), listOf("A", "B", "X")),
        )
        assertEquals(
            AdjacencyDiff(
                deleted = setOf(Edge("A", "B")),
                created = setOf(Edge("A", "X"), Edge("X", "B")),
            ),
            adjacencyDiff(listOf("A", "B"), listOf("A", "X", "B")),
        )
    }

    @Test
    fun inDayMoveChangesOnlyActualAdjacency() {
        assertEquals(
            AdjacencyDiff(
                deleted = setOf(Edge("A", "B"), Edge("B", "C"), Edge("C", "D")),
                created = setOf(Edge("A", "C"), Edge("C", "B"), Edge("B", "D")),
            ),
            adjacencyDiff(listOf("A", "B", "C", "D"), listOf("A", "C", "B", "D")),
        )
    }

    @Test
    fun crossDayMoveIsComputedIndependentlyForBothDays() {
        assertEquals(
            setOf(Edge("A", "C")),
            adjacencyDiff(listOf("A", "B", "C"), listOf("A", "C")).created,
        )
        assertEquals(
            setOf(Edge("X", "B"), Edge("B", "Y")),
            adjacencyDiff(listOf("X", "Y"), listOf("X", "B", "Y")).created,
        )
    }

    @Test
    fun haversineAndRecommendationHonorBoundariesAndRemainFinite() {
        val origin = SavedPlaceEntity("a", "trip", "a", "a", "a", 0.0, 0.0)
        fun placeAtMeters(id: String, meters: Double) = SavedPlaceEntity(id, "trip", id, id, id, 0.0, Math.toDegrees(meters / 6_371_000.0))

        assertEquals(TransportMode.WALK, defaultRecommendMode(origin, placeAtMeters("same", 0.0), TravelMode.FLEXIBLE))
        assertEquals(TransportMode.WALK, defaultRecommendMode(origin, placeAtMeters("one", 1_000.0), TravelMode.FLEXIBLE))
        assertEquals(TransportMode.TAXI, defaultRecommendMode(origin, placeAtMeters("over-one", 1_000.01), TravelMode.FLEXIBLE))
        assertEquals(TransportMode.TAXI, defaultRecommendMode(origin, placeAtMeters("twenty", 20_000.0), TravelMode.FLEXIBLE))
        assertEquals(TransportMode.TRANSIT, defaultRecommendMode(origin, placeAtMeters("over-twenty", 20_000.01), TravelMode.FLEXIBLE))
        assertEquals(TransportMode.DRIVE, defaultRecommendMode(origin, placeAtMeters("far", 30_000.0), TravelMode.SELF_DRIVE))
        assertTrue(haversineMeters(0.0, 0.0, 0.0, 180.0).isFinite())
    }

    @Test
    fun repeatedPlaceOccurrencesRemainDistinctBecauseEdgesUseItemIds() {
        val firstHotelItem = "hotel-item-1"
        val secondHotelItem = "hotel-item-2"
        val thirdHotelItem = "hotel-item-3"

        assertEquals(
            setOf(Edge(firstHotelItem, secondHotelItem), Edge(secondHotelItem, thirdHotelItem)),
            adjacencyDiff(emptyList(), listOf(firstHotelItem, secondHotelItem, thirdHotelItem)).created,
        )
    }
}
