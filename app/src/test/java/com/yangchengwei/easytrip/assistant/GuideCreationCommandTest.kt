package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.assistant.cityfirst.*
import com.yangchengwei.easytrip.core.model.TravelMode
import org.junit.Assert.*
import org.junit.Test

class GuideCreationCommandTest {
    @Test fun roundTripPreservesScopeAndRejectsTampering() {
        val command = CreateGuideTripCommand.create("甲市之旅", null, 1, TravelMode.FLEXIBLE,
            AssistantCityScope(listOf("甲市")), GuideMaterialRef("c0408b2b-e236-4fa9-930a-b421d16292e2", "a".repeat(64)))
        assertEquals(command, CreateGuideTripCommand.decode(command.encode()))
        assertThrows(IllegalArgumentException::class.java) {
            CreateGuideTripCommand.decode(command.encode().replace("甲市之旅", "另一趟"))
        }
    }
    @Test fun cityEvidenceMustActuallyExistAndNoPlacesCanBeReturned() {
        val result = GuideDestinationProtocol.decodeArguments(
            """{"cities":[{"name":"甲市","sourceId":"text","sourceSpan":"甲市周末"}],"suggestedName":"甲市之旅"}""",
            "推荐甲市周末旅行", emptySet())
        assertEquals(listOf("甲市"), result.cities.map { it.name })
        assertThrows(IllegalArgumentException::class.java) {
            GuideDestinationProtocol.decodeArguments(
                """{"cities":[{"name":"乙市","sourceId":"text","sourceSpan":"乙市"}],"suggestedName":"乙市之旅"}""",
                "甲市周末", emptySet())
        }
    }
}
