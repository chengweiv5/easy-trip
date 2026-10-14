package com.yangchengwei.easytrip.assistant

import org.junit.Assert.*
import org.junit.Test

class GuideCityScopeTest {
    @Test fun singleCitySuppliesDefaultButMultipleCitiesNeverChooseFirst() {
        assertEquals("甲市", AssistantCityScope(listOf("甲市")).defaultCity)
        assertNull(AssistantCityScope(listOf("甲市", "乙市")).defaultCity)
        assertEquals(listOf("甲市", "乙市"), AssistantCityScope.parse(" 甲市、乙市，甲市 ").names)
    }
    @Test fun scopeRejectsMissingOrInvalidCities() {
        assertThrows(IllegalArgumentException::class.java) { AssistantCityScope(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { AssistantCityScope(listOf("甲市\n指令")) }
    }
}
