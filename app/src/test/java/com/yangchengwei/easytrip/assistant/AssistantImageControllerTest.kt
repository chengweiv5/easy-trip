package com.yangchengwei.easytrip.assistant

import android.graphics.Bitmap
import android.net.Uri
import com.yangchengwei.easytrip.core.model.GeoPoint
import com.yangchengwei.easytrip.place.amap.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AssistantImageControllerTest {
    private class Images : AssistantImageStore {
        override suspend fun select(sessionId: String, uris: List<Uri>, existing: List<AssistantImage>) = ImageSelectionResult(emptyList(), emptyList())
        override suspend fun prepare(sessionId: String, imageId: String) = PreparedImage(imageId, "image/jpeg", byteArrayOf(1))
        override suspend fun preview(sessionId: String, imageId: String): Bitmap = error("not used")
        override suspend fun remove(sessionId: String, imageId: String) {}
        override suspend fun release(sessionId: String) {}
    }
    @Test fun imageInputInheritsTripCityBeforeParsingAndKeepsManualCityFirst() = runTest {
        for (manualCity in listOf("", "杭州")) {
            var resolutions = 0
            val parsedCities = mutableListOf<String>()
            val queryCities = mutableListOf<String?>()
            val text = PlaceIntentParser { _, _ -> emptyList() }
            val processor = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, city, _ ->
                parsedCities += city
                listOf(PlaceIntent("云龙湖", "", "云龙湖",
                    sources = listOf(IntakeSource.Image(image.id, ordinal, "云龙湖"))))
            }, Images())
            val controller = PlaceAssistantController("trip", this, text, object : PlaceSearchDataSource {
                override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                    queryCities += city
                    return emptyList()
                }
            }, object : PlaceImport {
                override suspend fun receipt(operationId: String): ImportReceipt? = null
                override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("no saves")
            }, resolveDefaultCity = { resolutions++; "徐州" }, processor = { processor }, sessionId = "session")
            controller.environment(true, true, true)
            controller.editCity(manualCity)
            controller.addImages(listOf(AssistantImage("i1", 1, "image/png", 100, 100, 100)))
            controller.submit(); advanceUntilIdle()
            val expected = manualCity.ifBlank { "徐州" }
            assertEquals(listOf(expected), parsedCities)
            assertEquals(listOf(expected), queryCities)
            assertEquals(if (manualCity.isBlank()) 1 else 0, resolutions)
            assertEquals(expected, controller.state.value.city)
        }
    }
    @Test fun imageOnlyInputQueriesWithoutSendingOnSelectionOrSaving() = runTest {
        var imageCalls = 0
        var textCalls = 0
        var queries = 0
        var saves = 0
        val text = PlaceIntentParser { _, _ -> textCalls++; emptyList() }
        val processor = AssistantInputProcessor(text, ImageIntentParser { image, number, _, _, _ ->
            imageCalls++
            listOf(PlaceIntent("灵隐寺", "杭州", "灵隐寺", sources = listOf(IntakeSource.Image(image.id, number, "灵隐寺"))))
        }, Images())
        val c = PlaceAssistantController("trip", this, text, object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                queries++
                return listOf(PlaceCandidate("poi", keyword, "真实地址", GeoPoint(30.2, 120.1), null, "杭州"))
            }
        }, object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt { saves++; error("must not save") }
        }, processor = { processor }, sessionId = "session")
        c.environment(true, true, true)
        c.addImages(listOf(AssistantImage("i1", 1, "image/png", 100, 100, 100)))
        assertTrue(c.state.value.hasDraft)
        assertEquals(0, imageCalls)
        c.submit(); c.submit(); advanceUntilIdle()
        assertEquals(1, imageCalls)
        assertEquals(0, textCalls)
        assertEquals(1, queries)
        assertEquals(0, saves)
        assertEquals(IntakeStatus.READY, c.state.value.items.single().status)
    }
    @Test fun stopRetainsSuccessfulSourceAndRejectsLateImageResult() = runTest {
        val slow = CompletableDeferred<Unit>()
        var calls = 0
        val text = PlaceIntentParser { _, _ -> emptyList() }
        val runner = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            calls++
            if (image.id == "i2") withContext(NonCancellable) { slow.await() }
            listOf(PlaceIntent("灵隐寺", "杭州", "灵隐寺", sources = listOf(IntakeSource.Image(image.id, ordinal, "灵隐寺"))))
        }, Images())
        val c = PlaceAssistantController("trip", this, text, null, object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("no saves")
        }, processor = { runner }, sessionId = "session")
        c.environment(true, true, true)
        c.addImages((1..2).map { AssistantImage("i$it", it, "image/png", 100, 100, 100) })
        c.submit(); runCurrent()
        c.cancel()
        slow.complete(Unit); advanceUntilIdle()
        assertEquals(1, c.state.value.items.size)
        assertEquals(IntakeStatus.CANCELLED, c.state.value.items.single().status)
        assertEquals(1, c.state.value.items.size)
        assertEquals(2, c.state.value.images.size)
        assertFalse(c.state.value.busy)
    }
    @Test fun stoppedImageWithoutCityEvidenceStillRequiresReviewBeforeQuery() = runTest {
        val gate = CompletableDeferred<Unit>()
        var queries = 0
        val text = PlaceIntentParser { _, _ -> emptyList() }
        val processor = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            if (image.id == "i2") withContext(NonCancellable) { gate.await() }
            listOf(PlaceIntent("天坛", "北京", "天坛",
                sources = listOf(IntakeSource.Image(image.id, ordinal, "天坛"))))
        }, Images())
        val controller = PlaceAssistantController("trip", this, text, object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                queries++
                return emptyList()
            }
        }, object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("no saves")
        }, processor = { processor }, sessionId = "session")
        controller.environment(true, true, true)
        controller.editCity("杭州")
        controller.addImages((1..2).map { AssistantImage("i$it", it, "image/png", 100, 100, 100) })
        controller.submit(); runCurrent()
        controller.cancel()
        gate.complete(Unit); advanceUntilIdle()
        controller.retry(); advanceUntilIdle()
        assertEquals(0, queries)
        assertEquals(IntakeStatus.NEED_CITY, controller.state.value.items.single().status)
        assertEquals("北京", controller.state.value.items.single().intent.city)
    }

    @Test fun explicitCityInImageOverridesDefaultWithoutConfirmation() = runTest {
        val requests = mutableListOf<String?>()
        val text = PlaceIntentParser { _, _ -> emptyList() }
        val processor = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            listOf(PlaceIntent("天坛", "北京", "北京 天坛",
                sources = listOf(IntakeSource.Image(image.id, ordinal, "北京 天坛"))))
        }, Images())
        val controller = PlaceAssistantController("trip", this, text, object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                requests += city
                return emptyList()
            }
        }, object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("no saves")
        }, resolveDefaultCity = { "徐州市" }, processor = { processor }, sessionId = "session")
        controller.environment(true, true, true)
        controller.addImages(listOf(AssistantImage("i1", 1, "image/png", 100, 100, 100)))
        controller.submit(); advanceUntilIdle()
        assertEquals(listOf("北京"), requests)
        assertEquals(IntakeStatus.NOT_FOUND, controller.state.value.items.single().status)
    }

    @Test fun retryImageKeepsOriginalBatchCityAfterLocationChanges() = runTest {
        var city = "徐州市"
        val cities = mutableListOf<String>()
        val text = PlaceIntentParser { _, _ -> emptyList() }
        val processor = AssistantInputProcessor(text, ImageIntentParser { image, _, _, default, _ ->
            cities += default
            if (image.id == "i2" && cities.size == 2) throw AssistantFailure("暂时失败")
            listOf(PlaceIntent("博物馆", "", "博物馆"))
        }, Images())
        val controller = PlaceAssistantController("trip", this, text, object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?) = emptyList<PlaceCandidate>()
        }, object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("no saves")
        }, resolveDefaultCity = { city }, processor = { processor }, sessionId = "session")
        controller.environment(true, true, true)
        controller.addImages((1..2).map { AssistantImage("i$it", it, "image/png", 100, 100, 100) })
        controller.submit(); advanceUntilIdle()
        city = "苏州市"
        controller.retryImages(); advanceUntilIdle()
        assertEquals(listOf("徐州市", "徐州市", "徐州市"), cities)
    }
    @Test fun retrySendsOnlyFailedImageAndKeepsSelectionAndAllDuplicateSources() = runTest {
        val calls = mutableListOf<String>()
        val queries = mutableListOf<String>()
        val text = PlaceIntentParser { _, _ -> emptyList() }
        val runner = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            calls += image.id
            if (image.id == "i2" && calls.count { it == "i2" } == 1) throw AssistantFailure("图2失败")
            listOf(PlaceIntent("灵隐寺", "杭州", "灵隐寺", sources = listOf(IntakeSource.Image(image.id, ordinal, "灵隐寺"))))
        }, Images())
        val c = PlaceAssistantController("trip", this, text, object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> {
                queries += keyword
                return listOf(PlaceCandidate("same", keyword, "真实地址", GeoPoint(30.2, 120.1), null, "杭州"))
            }
        }, object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("no saves")
        }, processor = { runner }, sessionId = "session")
        c.environment(true, true, true)
        c.addImages((1..3).map { AssistantImage("i$it", it, "image/png", 100, 100, 100) })
        c.submit(); advanceUntilIdle()
        c.selectAll()
        val selected = c.state.value.selected
        c.retryImages(); advanceUntilIdle()
        assertEquals(listOf("i1","i2","i3","i2"), calls)
        assertEquals(3, queries.size)
        assertEquals(selected, c.state.value.selected)
        assertEquals(1, c.state.value.eligible.size)
        assertEquals(setOf("i1","i2","i3"), c.state.value.eligible.single().sources.filterIsInstance<IntakeSource.Image>().map { it.imageId }.toSet())
    }

    @Test fun twentyOnePlacesNeverQueryATruncatedPrefix() = runTest {
        var queries = 0
        val text = PlaceIntentParser { _, _ -> emptyList() }
        val runner = AssistantInputProcessor(text, ImageIntentParser { image, ordinal, _, _, _ ->
            (1..(if (ordinal == 6) 1 else 4)).map {
                PlaceIntent("地点$ordinal-$it", "杭州", "地点$ordinal-$it",
                    sources = listOf(IntakeSource.Image(image.id, ordinal, "地点")))
            }
        }, Images())
        val c = PlaceAssistantController("trip", this, text, object : PlaceSearchDataSource {
            override suspend fun search(keyword: String, city: String?): List<PlaceCandidate> { queries++; return emptyList() }
        }, object : PlaceImport {
            override suspend fun receipt(operationId: String): ImportReceipt? = null
            override suspend fun commit(confirmation: ImportConfirmation): ImportReceipt = error("no saves")
        }, processor = { runner }, sessionId = "session")
        c.environment(true, true, true)
        c.addImages((1..6).map { AssistantImage("i$it", it, "image/png", 100, 100, 100) })
        c.submit(); advanceUntilIdle()
        assertEquals(0, queries)
        assertTrue(c.state.value.items.isEmpty())
        assertEquals(6, c.state.value.images.size)
        assertTrue(c.state.value.error!!.contains("20"))
    }
    @Test fun unsupportedFirstImageStopsRemainingUploads() = runTest {
        var calls = 0
        val runner = AssistantInputProcessor(PlaceIntentParser { _, _ -> emptyList() },
            ImageIntentParser { _, _, _, _, _ ->
                calls++
                throw AssistantFailure("不支持", AssistantFailureCode.UNSUPPORTED_IMAGE, SendEvidence.RESPONSE_RECEIVED)
            }, Images())
        val progress = mutableListOf<SourceProgress>()
        runner.parse(AssistantInputRequest("session", "", "杭州",
            (1..3).map { AssistantImage("i$it", it, "image/png", 100, 100, 100) }), onProgress = progress::add)
        assertEquals(1, calls)
        assertEquals(SendEvidence.NOT_DISPATCHED, progress.last { it.sourceId == "i3" }.sendEvidence)
    }
}
