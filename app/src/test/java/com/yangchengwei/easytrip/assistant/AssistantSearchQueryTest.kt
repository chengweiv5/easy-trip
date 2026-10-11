package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

/** The real controller boundary, with deterministic map responses and no model or database writes. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AssistantSearchQueryTest {
    private val names = listOf("偶园", "昭德古街", "九龙峪景区", "广福寺")
    private val noWrites = object : PlaceImport {
        override suspend fun receipt(operationId: String): ImportReceipt? = null
        override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("Unexpected write")
    }

    private fun TestScope.controller(
        intents: List<PlaceIntent>, calls: MutableList<Pair<String, String?>>,
        defaultCity: String = "青州市",
    ) = PlaceAssistantController("test", this, PlaceIntentParser { _, _ -> intents },
        object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                calls += keyword to city
                return listOf(PlaceCandidate(keyword, keyword, "测试地址", GeoPoint(36.69, 118.47), null, city))
            }
        }, noWrites, resolveDefaultCity = { defaultCity }).also {
        it.environment(true, true, true)
        it.editInput(intents.joinToString("、") { item -> item.sourceSpan })
        it.submit()
    }

    @Test fun firstSearchQualifiesAllFourNamesAndPreservesOriginalInput() = runTest {
        val calls = mutableListOf<Pair<String, String?>>()
        val c = controller(names.map { PlaceIntent(it, "", it) }, calls)
        advanceUntilIdle()
        assertEquals(listOf("青州偶园", "青州昭德古街", "青州九龙峪景区", "青州广福寺"), calls.map { it.first })
        assertTrue(calls.all { it.second == "青州市" })
        assertEquals(names, c.state.value.items.map { it.intent.query })
        assertEquals(names, c.state.value.items.map { it.intent.sourceSpan })
        assertTrue(c.state.value.items.all { it.status == IntakeStatus.READY })
        assertTrue(c.state.value.selected.isEmpty())
    }

    @Test fun existingShortFullAndEmbeddedCityNamesAreNotDuplicated() = runTest {
        val input = listOf("青州偶园", "青州市昭德古街", "山东省青州市广福寺", "  青州 九龙峪景区  ")
        val calls = mutableListOf<Pair<String, String?>>()
        controller(input.map { PlaceIntent(it, "青州", it) }, calls)
        advanceUntilIdle()
        assertEquals(listOf("青州偶园", "青州市昭德古街", "山东省青州市广福寺", "青州 九龙峪景区"), calls.map { it.first })
    }

    @Test fun conflictingItemUsesItsOwnCityOnlyAfterConfirmation() = runTest {
        val calls = mutableListOf<Pair<String, String?>>()
        val c = controller(listOf(PlaceIntent("故宫", "北京市", "北京故宫")), calls)
        advanceUntilIdle()
        assertTrue(calls.isEmpty())
        assertEquals(IntakeStatus.NEED_CITY, c.state.value.items.single().status)
        c.confirmCity(c.state.value.items.single().id)
        advanceUntilIdle()
        assertEquals(listOf("北京故宫" to "北京市"), calls)
        assertEquals("青州市", c.state.value.city)
        assertTrue(c.state.value.selected.isEmpty())
    }

    @Test fun noKnownCityDoesNotSearchOrGuessFromPhoneLocation() = runTest {
        val calls = mutableListOf<Pair<String, String?>>()
        val c = controller(listOf(PlaceIntent("广福寺", "", "广福寺")), calls, defaultCity = "")
        advanceUntilIdle()
        assertTrue(calls.isEmpty())
        assertEquals(IntakeStatus.NEED_CITY, c.state.value.items.single().status)
        c.supplyMissingCity("青州市")
        advanceUntilIdle()
        assertEquals(listOf("青州广福寺" to "青州市"), calls)
    }

    @Test fun retryAndInlineCityCorrectionReuseTheSameQualificationRule() = runTest {
        val calls = mutableListOf<Pair<String, String?>>()
        val c = PlaceAssistantController("test", this,
            PlaceIntentParser { _, _ -> listOf(PlaceIntent("广福寺", "青州市", "广福寺")) },
            object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    calls += keyword to city
                    return emptyList()
                }
            }, noWrites)
        c.environment(true, true, true); c.editInput("广福寺"); c.submit()
        advanceUntilIdle()
        val id = c.state.value.items.single().id
        c.retry(setOf(id)); advanceUntilIdle()
        assertEquals(listOf("青州广福寺" to "青州市", "青州广福寺" to "青州市"), calls)
        c.editItem(id, "广福寺", "济南市"); c.retry(setOf(id)); advanceUntilIdle()
        assertEquals("济南广福寺" to "济南市", calls.last())
        assertEquals("广福寺", c.state.value.items.single().intent.query)
    }
}
