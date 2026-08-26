package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact
import com.yangchengwei.easytrip.place.domain.PlaceTag
import com.yangchengwei.easytrip.place.domain.SavePlaceResult
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.place.domain.SavedPlaceRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlacePoolViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun zeroImpactDeleteRemovesImmediatelyWithoutConfirmation() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a")), emptyMap())
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()

        model.requestDelete(place("a"))
        advanceUntilIdle()

        assertEquals(listOf("a"), repository.deleted)
        assertNull(model.state.value.deleting)
    }

    @Test fun nonZeroImpactShowsExactConfirmationCounts() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a")), emptyMap(), impact = PlaceDeletionImpact(2, 3))
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()

        model.requestDelete(place("a"))
        advanceUntilIdle()

        assertEquals("a", model.state.value.deleting?.id)
        assertEquals(PlaceDeletionImpact(2, 3), model.state.value.deletionImpact)
        assertEquals(emptyList<String>(), repository.deleted)
    }

    @Test fun dismissDeleteHasNoDeletionSideEffect() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a")), emptyMap(), impact = PlaceDeletionImpact(1, 1))
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()
        model.requestDelete(place("a"))
        advanceUntilIdle()

        model.dismissDelete()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.deleted)
        assertNull(model.state.value.deleting)
    }

    @Test fun repeatedDeleteConfirmationDeletesOnlyOnce() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a")), emptyMap(), impact = PlaceDeletionImpact(1, 1))
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()
        model.requestDelete(place("a"))
        advanceUntilIdle()

        model.confirmDelete()
        model.confirmDelete()
        advanceUntilIdle()

        assertEquals(listOf("a"), repository.deleted)
    }

    @Test fun failedDeleteKeepsConfirmationForRetry() = runTest(dispatcher) {
        val repository = PoolRepository(
            listOf(place("a")),
            emptyMap(),
            impact = PlaceDeletionImpact(1, 2),
            deleteFailure = IllegalStateException("删除失败"),
        )
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()
        model.requestDelete(place("a"))
        advanceUntilIdle()

        model.confirmDelete()
        advanceUntilIdle()

        assertEquals("a", model.state.value.deleting?.id)
        assertEquals(PlaceDeletionImpact(1, 2), model.state.value.deletionImpact)
    }

    @Test fun dismissWhileDeleteImpactLoadsPreventsStaleResult() = runTest(dispatcher) {
        val repository = DelayedRepository()
        val model = PlacePoolViewModel("trip", repository, null)

        model.requestDelete(place("a"))
        model.dismissDelete()
        repository.completeImpact("a", PlaceDeletionImpact(2, 1))
        advanceUntilIdle()

        assertNull(model.state.value.deleting)
        assertNull(model.state.value.deletionImpact)
    }

    @Test fun latestDeleteRequestWinsWhenImpactQueriesCompleteOutOfOrder() = runTest(dispatcher) {
        val repository = DelayedRepository()
        val model = PlacePoolViewModel("trip", repository, null)

        model.requestDelete(place("a"))
        model.requestDelete(place("b"))
        repository.completeImpact("b", PlaceDeletionImpact(3, 2))
        advanceUntilIdle()
        repository.completeImpact("a", PlaceDeletionImpact(1, 1))
        advanceUntilIdle()

        assertEquals("b", model.state.value.deleting?.id)
        assertEquals(PlaceDeletionImpact(3, 2), model.state.value.deletionImpact)
    }

    @Test fun placePoolDistinguishesSavedOnlyFromScheduled() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a"), place("b")), mapOf("a" to 0, "b" to 2))
        val model = PlacePoolViewModel("trip", repository, null)

        advanceUntilIdle()

        assertEquals(
            listOf(
                SavedPlaceRowUi(place("a"), 0, false),
                SavedPlaceRowUi(place("b"), 2, true),
            ),
            model.state.value.rows,
        )
    }

    @Test fun itineraryUsageChangeRefreshesRowsWithoutPlaceChange() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a")), mapOf("a" to 0))
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()

        repository.setUsage("a", 2)
        advanceUntilIdle()

        assertEquals(2, model.state.value.rows.single().itineraryOccurrenceCount)
        assertEquals(true, model.state.value.rows.single().scheduled)
    }

    @Test fun rowsStayEmptyUntilUsageSnapshotIsKnown() = runTest(dispatcher) {
        val repository = DelayedRowsRepository(place("a"))
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()

        assertEquals(emptyList<SavedPlaceRowUi>(), model.state.value.rows)
        repository.publishUsage(0)
        advanceUntilIdle()

        assertEquals("a", model.state.value.rows.single().place.id)
        assertEquals(false, model.state.value.rows.single().scheduled)
    }

    @Test fun editingSavedPlaceCreatesDraftBoundToPlaceId() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a")), emptyMap())
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()

        model.edit(place("a"))

        assertEquals("a", model.state.value.editing?.id)
        assertEquals("a", model.state.value.detailDraft?.placeId)
    }

    @Test fun cancelEditDiscardsDraftWithoutRepositoryCall() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a")), emptyMap())
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()
        model.edit(place("a"))

        model.dismissEdit()

        assertNull(model.state.value.editing)
        assertNull(model.state.value.detailDraft)
        assertEquals(0, repository.updateCalls)
    }

    @Test fun failedDetailSaveKeepsDraft() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a")), emptyMap(), updateFailure = IllegalStateException("保存失败"))
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()
        model.edit(place("a"))

        model.updateDetails("傍晚散步", setOf("景点"))
        advanceUntilIdle()

        assertEquals("a", model.state.value.editing?.id)
        assertEquals("傍晚散步", model.state.value.detailDraft?.note)
        assertEquals(setOf("景点"), model.state.value.detailDraft?.tags)
        assertEquals("保存失败", model.state.value.detailSaveError)
    }

    @Test fun addingValidTagSelectsItAndClearsInput() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a")), emptyMap())
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()
        model.edit(place("a"))

        model.updateNewTagInput("  景点  ")
        model.addNewTag()

        assertEquals(setOf("景点"), model.state.value.detailDraft?.tags)
        assertEquals("", model.state.value.detailDraft?.newTagInput)
    }

    @Test fun fullTagSelectionStillAllowsRemovingTag() = runTest(dispatcher) {
        val tagged = place("a").copy(tags = (1..8).map { PlaceTag("id-$it", "tag-$it") })
        val model = PlacePoolViewModel("trip", PoolRepository(listOf(tagged), emptyMap()), null)
        advanceUntilIdle()
        model.edit(tagged)

        model.removeTag("tag-8")

        assertEquals(7, model.state.value.detailDraft?.tags?.size)
    }

    @Test fun failedSaveKeepsNewTagInput() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a")), emptyMap(), updateFailure = IllegalStateException("保存失败"))
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()
        model.edit(place("a"))
        model.updateDetailDraft("傍晚散步", setOf("景点"))
        model.updateNewTagInput("未添加")

        model.updateDetails()
        advanceUntilIdle()

        assertEquals("傍晚散步", model.state.value.detailDraft?.note)
        assertEquals(setOf("景点"), model.state.value.detailDraft?.tags)
        assertEquals("未添加", model.state.value.detailDraft?.newTagInput)
    }

    @Test fun staleSaveCompletionCannotCloseNewPlaceDraft() = runTest(dispatcher) {
        val repository = DelayedUpdateRepository(listOf(place("a"), place("b")))
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()
        model.edit(place("a"))
        model.updateDetails()
        dispatcher.scheduler.runCurrent()
        model.edit(place("b"))

        repository.complete("a")
        advanceUntilIdle()

        assertEquals("b", model.state.value.detailDraft?.placeId)
    }

    @Test fun deletingEditedPlaceClosesEditAndDoesNotRestoreItAfterConfirmation() = runTest(dispatcher) {
        val repository = PoolRepository(listOf(place("a")), mapOf("a" to 1))
        val model = PlacePoolViewModel("trip", repository, null)
        advanceUntilIdle()
        model.edit(place("a"))

        model.dispatch(PlacePoolAction.Delete(place("a")))

        assertNull(model.state.value.editing)
        assertNull(model.state.value.detailDraft)
        advanceUntilIdle()
        assertEquals("a", model.state.value.deleting?.id)

        model.confirmDelete()
        advanceUntilIdle()

        assertNull(model.state.value.deleting)
        assertNull(model.state.value.editing)
        assertNull(model.state.value.detailDraft)
    }

    private fun place(id: String) = SavedPlace(
        id,
        "trip",
        "poi-$id",
        id.uppercase(),
        "address",
        GeoPoint(39.9, 116.4),
        "",
        emptyList(),
    )

    private class PoolRepository(
        places: List<SavedPlace>,
        usageCounts: Map<String, Int>,
        private val updateFailure: Throwable? = null,
        private val impact: PlaceDeletionImpact? = null,
        private val deleteFailure: Throwable? = null,
    ) : SavedPlaceRepository {
        var updateCalls = 0
        val deleted = mutableListOf<String>()
        private val places = MutableStateFlow(places)
        private val usageCounts = MutableStateFlow(
            places.associate { place -> place.id to (usageCounts[place.id] ?: 0) },
        )

        fun setUsage(placeId: String, count: Int) {
            usageCounts.value = usageCounts.value + (placeId to count)
        }
        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = places
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = emptyFlow()
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = emptyFlow()
        override fun observeUsageCounts(tripId: String): Flow<Map<String, Int>> = usageCounts
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("saved")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) {
            updateCalls += 1
            updateFailure?.let { throw it }
        }
        override suspend fun usageCount(placeId: String) = usageCounts.value[placeId] ?: 0
        override suspend fun deletionImpact(placeId: String) = impact ?: PlaceDeletionImpact(usageCount(placeId), 0)
        override suspend fun deletePlaceAndReferences(placeId: String) {
            deleted += placeId
            deleteFailure?.let { throw it }
        }
    }

    private class DelayedUpdateRepository(private val initialPlaces: List<SavedPlace>) : SavedPlaceRepository {
        private val places = MutableStateFlow(initialPlaces)
        private val completions = mutableMapOf<String, CompletableDeferred<Unit>>()
        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = places
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = emptyFlow()
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = emptyFlow()
        override fun observeUsageCounts(tripId: String): Flow<Map<String, Int>> = MutableStateFlow(initialPlaces.associate { it.id to 0 })
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("saved")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                completions.getOrPut(placeId) { CompletableDeferred() }.await()
            }
        }
        fun complete(placeId: String) = completions.getValue(placeId).complete(Unit)
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(usageCount(placeId), 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class DelayedRowsRepository(place: SavedPlace) : SavedPlaceRepository {
        private val places = MutableStateFlow(listOf(place))
        private val usageCounts = MutableStateFlow<Map<String, Int>?>(null)

        fun publishUsage(count: Int) {
            usageCounts.value = mapOf(places.value.single().id to count)
        }

        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = places
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = emptyFlow()
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = emptyFlow()
        override fun observeUsageCounts(tripId: String): Flow<Map<String, Int>> = kotlinx.coroutines.flow.flow {
            usageCounts.collect { counts -> if (counts != null) emit(counts) }
        }
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("saved")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = usageCounts.value?.get(placeId) ?: 0
        override suspend fun deletionImpact(placeId: String) = com.yangchengwei.easytrip.place.domain.PlaceDeletionImpact(usageCount(placeId), 0)
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }

    private class DelayedRepository : SavedPlaceRepository {
        private val impacts = mutableMapOf<String, CompletableDeferred<PlaceDeletionImpact>>()

        fun completeImpact(id: String, impact: PlaceDeletionImpact) {
            impacts.getOrPut(id) { CompletableDeferred() }.complete(impact)
        }

        override fun observePlaces(tripId: String, tagIds: Set<String>): Flow<List<SavedPlace>> = emptyFlow()
        override fun observeTags(tripId: String): Flow<List<PlaceTag>> = emptyFlow()
        override fun observeSavedPoiIds(tripId: String): Flow<Set<String>> = emptyFlow()
        override suspend fun save(tripId: String, candidate: PlaceCandidate) = SavePlaceResult.Saved("saved")
        override suspend fun updateDetails(placeId: String, note: String, tagNames: Set<String>) = Unit
        override suspend fun usageCount(placeId: String) = 0
        override suspend fun deletionImpact(placeId: String) = impacts.getOrPut(placeId) { CompletableDeferred() }.await()
        override suspend fun deletePlaceAndReferences(placeId: String) = Unit
    }
}
