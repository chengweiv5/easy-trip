package com.yangchengwei.easytrip.itinerary.ui

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.core.model.RouteStatus
import com.yangchengwei.easytrip.core.model.TransportMode
import com.yangchengwei.easytrip.itinerary.domain.DayItinerary
import com.yangchengwei.easytrip.itinerary.domain.ItineraryItem
import com.yangchengwei.easytrip.itinerary.domain.ItineraryPlace
import com.yangchengwei.easytrip.route.data.RouteLegEntity
import com.yangchengwei.easytrip.route.domain.RouteErrorKind
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.DayMapSnapshot
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WholeTripItineraryMapperTest {
    @Test
    fun `maps sorted days including empty days and only adjacent route legs`() {
        val breakfast = item("item-1", "早餐店", "东街 1 号", LocalTime.of(8, 30), 45)
        val museum = item("item-2", "博物馆", "西街 2 号", LocalTime.of(10, 0), 120)
        val park = item("item-3", "公园", "南街 3 号", null, null)
        val validLeg = leg(
            id = "leg-valid",
            fromItemId = breakfast.id,
            toItemId = museum.id,
            recommendedMode = TransportMode.WALK,
            selectedMode = TransportMode.TAXI,
            status = RouteStatus.FAILED,
            distanceMeters = 3200,
            durationSeconds = 900,
            errorKind = RouteErrorKind.NO_ROUTE,
        )
        val staleLeg = leg(
            id = "leg-stale",
            fromItemId = breakfast.id,
            toItemId = park.id,
            recommendedMode = TransportMode.DRIVE,
            status = RouteStatus.SUCCESS,
        )

        val actual = mapWholeTripDays(
            days = listOf(TripDay("day-2", 1), TripDay("day-1", 0)),
            snapshots = listOf(
                DayMapSnapshot(
                    itinerary = DayItinerary("day-1", "trip-1", listOf(breakfast, museum, park)),
                    legs = listOf(staleLeg, validLeg),
                ),
            ),
        )

        assertEquals(
            listOf(
                WholeTripDayUi(
                    dayId = "day-1",
                    dayNumber = 1,
                    items = listOf(
                        ItineraryItemUi("item-1", "早餐店", "东街 1 号", LocalTime.of(8, 30), 45, placeId = "place-item-1"),
                        ItineraryItemUi("item-2", "博物馆", "西街 2 号", LocalTime.of(10, 0), 120, placeId = "place-item-2"),
                        ItineraryItemUi("item-3", "公园", "南街 3 号", null, null, placeId = "place-item-3"),
                    ),
                    legs = listOf(
                        RouteLegUi(
                            id = "leg-valid",
                            fromItemId = "item-1",
                            toItemId = "item-2",
                            mode = TransportMode.TAXI,
                            status = RouteStatus.FAILED,
                            distanceMeters = 3200,
                            durationSeconds = 900,
                            error = "未找到可用路线",
                            selectedModeOverride = TransportMode.TAXI,
                        ),
                    ),
                ),
                WholeTripDayUi("day-2", 2, emptyList(), emptyList()),
            ),
            actual,
        )
    }

    @Test
    fun `day color index is stable and cycles independently of list position`() {
        assertEquals(0, wholeTripDayColorIndex(1))
        assertEquals(1, wholeTripDayColorIndex(2))
        assertEquals(0, wholeTripDayColorIndex(6))
        assertEquals(1, wholeTripDayColorIndex(7))
    }

    @Test
    fun `failed prefers typed summary and falls back to legacy code`() {
        val places = listOf(
            item("item-0", "地点 0", "地址 0", null, null),
            item("item-1", "地点 1", "地址 1", null, null),
            item("item-2", "地点 2", "地址 2", null, null),
        )
        val legs = listOf(
            leg(
                id = "typed",
                fromItemId = "item-0",
                toItemId = "item-1",
                recommendedMode = TransportMode.WALK,
                status = RouteStatus.FAILED,
                errorKind = RouteErrorKind.NO_ROUTE,
                errorCode = "legacy error",
            ),
            leg(
                id = "legacy",
                fromItemId = "item-1",
                toItemId = "item-2",
                recommendedMode = TransportMode.WALK,
                status = RouteStatus.FAILED,
                errorCode = "legacy error",
            ),
        )

        val actual = mapWholeTripDays(
            days = listOf(TripDay("day-1", 0)),
            snapshots = listOf(DayMapSnapshot(DayItinerary("day-1", "trip-1", places), legs)),
        ).single().legs.map(RouteLegUi::state)

        assertEquals(
            listOf(RouteLegUiState.Failed("未找到可用路线"), RouteLegUiState.Failed("legacy error")),
            actual,
        )
    }

    @Test
    fun `uses existing Chinese summaries for every route error kind`() {
        val items = (0..4).map { index -> item("item-$index", "地点 $index", "地址 $index", null, null) }
        val kinds = listOf(
            RouteErrorKind.TRANSIENT,
            RouteErrorKind.NO_ROUTE,
            RouteErrorKind.UNSUPPORTED_TRANSIT,
            RouteErrorKind.PERMANENT,
        )
        val legs = kinds.mapIndexed { index, kind ->
            leg(
                id = "leg-$index",
                fromItemId = items[index].id,
                toItemId = items[index + 1].id,
                recommendedMode = TransportMode.WALK,
                status = RouteStatus.FAILED,
                errorKind = kind,
            )
        }

        val actual = mapWholeTripDays(
            days = listOf(TripDay("day-1", 0)),
            snapshots = listOf(DayMapSnapshot(DayItinerary("day-1", "trip-1", items), legs)),
        )

        assertEquals(
            listOf("网络异常，请重试", "未找到可用路线", "城市信息暂未获取，请重试或更改方式", "路线规划失败"),
            actual.single().legs.map(RouteLegUi::error),
        )
    }

    private fun item(
        id: String,
        name: String,
        address: String,
        arrivalTime: LocalTime?,
        stayMinutes: Int?,
    ) = ItineraryItem(id, ItineraryPlace("place-$id", name, address, GeoPoint(39.9, 116.4)), arrivalTime, stayMinutes)

    private fun leg(
        id: String,
        fromItemId: String,
        toItemId: String,
        recommendedMode: TransportMode,
        selectedMode: TransportMode? = null,
        status: RouteStatus,
        distanceMeters: Int? = null,
        durationSeconds: Int? = null,
        errorKind: RouteErrorKind? = null,
        errorCode: String? = null,
    ) = RouteLegEntity(
        id = id,
        tripDayId = "day-1",
        fromItemId = fromItemId,
        toItemId = toItemId,
        recommendedMode = recommendedMode,
        selectedMode = selectedMode,
        status = status,
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        errorCode = errorCode,
        errorKind = errorKind,
        updatedAt = Instant.EPOCH,
    )
}
