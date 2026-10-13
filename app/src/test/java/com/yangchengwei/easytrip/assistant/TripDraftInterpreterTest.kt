package com.yangchengwei.easytrip.assistant

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TripDraftInterpreterTest {
    private val reference = LocalDate.of(2026, 10, 13)
    @Test fun datesBelongToUserIntentNeverShareText() {
        assertEquals(TripDateSuggestion(LocalDate.of(2026, 10, 14), 3), TripDateRules.parse("明天出发，玩三天", reference))
        assertEquals(TripDateSuggestion(LocalDate.of(2026, 10, 24), 3), TripDateRules.parse("2026年10月24日到26日去青州", reference))
        assertEquals(TripDateSuggestion(null, 3), TripDateRules.parse("我想玩三天", reference))
        assertEquals("", tripUserIntent("青州三日游 2026年10月24日 https://xhslink.cn/o/abc 复制去小红书"))
        assertEquals("我计划明天出发，玩三天", tripUserIntent("三日游 https://xhslink.cn/o/abc\n我计划明天出发，玩三天"))
        assertNull(TripDateRules.parse("周末有空去", reference).startDate)
        assertNotNull(TripDateRules.parse("10月1日出发", reference).warning)
        assertNotNull(TripDateRules.parse("2026年2月29日出发", reference).warning)
        assertNotNull(TripDateRules.parse("明天玩三十一天", reference).warning)
        assertEquals(LocalDate.of(2026, 10, 23), TripDateRules.parse("下周五出发", reference).startDate)
    }
    @Test fun alternativeDatesAreNotARange() {
        assertNotNull(TripDateRules.parse("我计划2026年10月24日或2026年10月26日出发", reference).warning)
        assertNotNull(TripDateRules.parse("可能明天出发", reference).warning)
        assertNotNull(TripDateRules.parse("2026年10月24日，2026年10月26日", reference).warning)
        assertEquals(TripDateSuggestion(LocalDate.of(2026, 10, 24), 3),
            TripDateRules.parse("2026年10月24日至2026年10月26日", reference))
    }
    @Test fun validatesCalendarAndDurationBoundaries() {
        assertNotNull(TripDateRules.parse("12月31日至1月2日", reference).warning)
        assertNotNull(TripDateRules.parse("2026年10月26日至2026年10月24日", reference).warning)
        assertNotNull(TripDateRules.parse("2026年10月24日至2026年10月26日玩四天", reference).warning)
        assertEquals(TripDateSuggestion(LocalDate.of(2028, 2, 29)), TripDateRules.parse("2028年2月29日", reference))
        assertEquals(TripDateSuggestion(LocalDate.of(2026, 10, 24)), TripDateRules.parse("10月24日出发", reference))
        assertEquals(TripDateSuggestion(null, 30), TripDateRules.parse("我玩三十天", reference))
    }
}
