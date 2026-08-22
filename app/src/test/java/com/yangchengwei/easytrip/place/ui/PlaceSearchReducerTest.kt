package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.SavedPlace
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSearchReducerTest {
    @Test fun waitsThreeHundredMillisecondsBeforeSearching() = runTest {
        val source = SearchSource()
        val reducer = PlaceSearchReducer(source, this, StandardTestDispatcher(testScheduler))
        reducer.setQuery("故宫")
        advanceTimeBy(299)
        assertEquals(emptyList<String>(), source.queries)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("故宫"), source.queries)
        source.complete("故宫", candidate("done"))
        advanceUntilIdle()
    }

    @Test fun newQueryCancelsOldSearchAndOldResponseCannotOverwrite() = runTest {
        val source = SearchSource(ignoreCancellation = true)
        val reducer = PlaceSearchReducer(source, this, StandardTestDispatcher(testScheduler))
        reducer.setQuery("故")
        advanceTimeBy(300)
        runCurrent()
        reducer.setQuery("故宫")
        advanceTimeBy(300)
        runCurrent()
        source.complete("故宫", candidate("new"))
        advanceUntilIdle()
        source.complete("故", candidate("old"))
        advanceUntilIdle()
        assertTrue("故" in source.cancelled)
        assertEquals(listOf("new"), reducer.state.value.results.map { it.poiId })
    }

    @Test fun failureKeepsSavedPlacePool() = runTest {
        val source = SearchSource()
        val reducer = PlaceSearchReducer(source, this, StandardTestDispatcher(testScheduler))
        val saved = SavedPlace("saved", "trip", "poi", "已收藏", "地址", GeoPoint(1.0, 2.0), "备注", emptyList())
        reducer.setSavedPlaces(listOf(saved))
        reducer.setQuery("失败")
        advanceTimeBy(300)
        runCurrent()
        source.fail("失败", IllegalStateException("network"))
        advanceUntilIdle()
        assertEquals(listOf(saved), reducer.state.value.savedPlaces)
        assertEquals(emptyList<PlaceCandidate>(), reducer.state.value.results)
        assertFalse(reducer.state.value.searching)
        assertEquals("network", reducer.state.value.error)
    }

    @Test fun replacingSourceCancelsOldRequestAndReissuesCurrentQuery() = runTest {
        val oldSource = SearchSource()
        val newSource = SearchSource()
        val reducer = PlaceSearchReducer(oldSource, this, StandardTestDispatcher(testScheduler))
        reducer.setQuery("故宫")
        advanceTimeBy(300)
        runCurrent()

        reducer.setSource(newSource)
        runCurrent()
        assertEquals(listOf("故宫"), oldSource.cancelled)
        advanceTimeBy(300)
        runCurrent()

        assertEquals(listOf("故宫"), newSource.queries)
        newSource.complete("故宫", candidate("new-source"))
        advanceUntilIdle()
        assertEquals(listOf("new-source"), reducer.state.value.results.map { it.poiId })
    }

    @Test fun clearResetsStateAndCancelsActiveSearch() = runTest {
        val source = SearchSource(ignoreCancellation = true)
        val reducer = PlaceSearchReducer(source, this, StandardTestDispatcher(testScheduler))
        reducer.setSavedPlaces(listOf(SavedPlace("saved", "trip", "poi", "已收藏", "地址", GeoPoint(1.0, 2.0), "", emptyList())))
        reducer.setQuery("故宫")
        advanceTimeBy(300)
        runCurrent()
        reducer.clear()
        runCurrent()

        assertEquals(PlaceSearchState(), reducer.state.value)
        assertEquals(listOf("故宫"), source.cancelled)
        source.complete("故宫", candidate("stale"))
        advanceUntilIdle()
        assertEquals(PlaceSearchState(), reducer.state.value)
    }

    @Test fun clearResetsErrorState() = runTest {
        val source = SearchSource()
        val reducer = PlaceSearchReducer(source, this, StandardTestDispatcher(testScheduler))
        reducer.setQuery("失败")
        advanceTimeBy(300)
        runCurrent()
        source.fail("失败", IllegalStateException("network"))
        advanceUntilIdle()
        assertEquals("network", reducer.state.value.error)

        reducer.clear()

        assertEquals(PlaceSearchState(), reducer.state.value)
    }

    private fun candidate(id: String) = PlaceCandidate(id, id, "address", GeoPoint(1.0, 2.0), null)

    private class SearchSource(private val ignoreCancellation: Boolean = false) : PlaceSearchDataSource {
        val queries = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        private val pending = mutableMapOf<String, CompletableDeferred<List<PlaceCandidate>>>()
        override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
            queries += keyword
            val deferred = CompletableDeferred<List<PlaceCandidate>>()
            pending[keyword] = deferred
            return try { deferred.await() } catch (error: kotlinx.coroutines.CancellationException) {
                cancelled += keyword
                if (!ignoreCancellation) throw error
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { deferred.await() }
            }
        }
        fun complete(query: String, value: PlaceCandidate) { pending.getValue(query).complete(listOf(value)) }
        fun fail(query: String, error: Throwable) { pending.getValue(query).completeExceptionally(error) }
    }
}
