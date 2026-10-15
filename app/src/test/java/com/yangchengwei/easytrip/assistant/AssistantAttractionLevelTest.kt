package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AssistantAttractionLevelTest {
    @Test fun lodgingIsExcludedEvenWhenRecommended() {
        val config = ProviderConfig("https://example.test", "model", "fixture")
        listOf(
            BatchToolProtocol.request(config, "住宿建议", "城市", travelMaterials = true),
            ImageToolProtocol.request(config, PreparedImage("i", "image/jpeg", byteArrayOf(1)),
                1, "住宿建议", "城市", travelMaterials = true),
            PlaceNormalizationProtocol.request(config, listOf(marked("旅舍")), "住宿建议", "城市"),
        ).map(::systemPrompt).forEach { assertTrue(it.contains("住宿全部排除")) }
    }

    @Test fun normalizationChecksShortNamesAgainstExistingFullNamesBeforeReturning() {
        val prompt = systemPrompt(PlaceNormalizationProtocol.request(
            ProviderConfig("https://example.test", "model", "fixture"),
            listOf(marked("文昌公园"), marked("文昌")), "文昌公园，住宿打车去文昌很近", "城市"))
        assertTrue(prompt.contains("最后复核专名相同"))
        assertTrue(prompt.contains("独立游览证据"))
    }

    @Test fun mapLabelsKeepIndependentAttractionsButNotStreetFragmentsOfDiningLabels() {
        val request = ImageToolProtocol.request(ProviderConfig("https://example.test", "model", "fixture"),
            PreparedImage("i", "image/jpeg", byteArrayOf(1)), 1, "", "城市", travelMaterials = true)
        val prompt = systemPrompt(request)
        assertTrue(prompt.contains("餐饮标签中的街道名"))
        assertTrue(prompt.contains("正文没有重复"))
        assertTrue(prompt.contains("不能只写"))
    }

    @Test fun guidePromptsDoNotTreatTransportPinsAsIndependentVisits() {
        val config = ProviderConfig("https://example.test", "model", "fixture")
        val image = PreparedImage("i", "image/jpeg", byteArrayOf(1))
        listOf(
            BatchToolProtocol.request(config, "交通说明", "城市", travelMaterials = true),
            ImageToolProtocol.request(config, image, 1, "交通说明", "城市", travelMaterials = true),
            PlaceNormalizationProtocol.request(config, listOf(marked("车站")), "交通说明", "城市"),
        ).map(::systemPrompt).forEach {
            assertTrue(it.contains("只用于交通换乘"))
            assertTrue(it.contains("车站博物馆"))
        }
    }

    @Test fun imageEvidenceRemainsImageOnlyWhileWholeGuideNormalizationHasContext() {
        val request = ImageToolProtocol.request(ProviderConfig("https://example.test", "model", "fixture"),
            PreparedImage("i", "image/jpeg", byteArrayOf(1)), 7,
            "博物馆内的演出时刻表", "徐州市", travelMaterials = true)
        val messages = Json.parseToJsonElement(request).jsonObject.getValue("messages").jsonArray
        assertFalse("Do not misattribute body-only names to an image",
            messages[1].toString().contains("博物馆内的演出时刻表"))
        assertTrue(messages[1].toString().contains("跨图层级由后续统一整理"))
        assertFalse(messages[0].toString().contains("徐州"))
    }

    @Test fun independentMuseumSurvivesAlongsideParkButInternalStopsAndDiningDoNot() = runTest {
        val places = listOf(marked("古城公园"),
            marked("人物纪念馆", "古城公园"), marked("公园南门", "古城公园"),
            marked("六合巷"), marked("云龙让茶"), marked("饮食文化博物馆"))
        val result = normalizer("""{"groups":[{"canonical":0,"members":[0]},
            {"canonical":1,"members":[1]},{"canonical":5,"members":[5]}],"discarded":[
            {"id":2,"reason":"NOT_PLACE"},{"id":3,"reason":"DINING"},{"id":4,"reason":"DINING"}]}""")
            .normalize(places, "公园一站，人物纪念馆独立一站，饮食文化博物馆独立一站；六合巷推荐菜品，云龙让茶买饮品。", "徐州市")
        assertEquals(listOf("古城公园", "人物纪念馆", "饮食文化博物馆"), result.map { it.query })
        assertEquals(places[1].sources, result[1].sources)
    }

    @Test fun internalOnlyGuideDoesNotInventAParentOrReturnReviewRows() = runTest {
        val result = normalizer("""{"groups":[],"discarded":[{"id":0,"reason":"NOT_PLACE"}]}""")
            .normalize(listOf(marked("LED互动舞台")), "场内演出安排，没有场馆名称", "徐州市")
        assertTrue(result.isEmpty())
    }

    @Test fun aParentChildAliasGroupIsRejectedRatherThanForcingTheChildBackIntoResults() = runTest {
        val places = listOf(marked("徐州饮食文化博物馆"),
            marked("北门外广场", "徐州饮食文化博物馆"))
        try {
            normalizer("""{"groups":[{"canonical":0,"members":[0,1]}],"discarded":[]}""")
                .normalize(places, "博物馆内部表演位置", "徐州市")
            fail("A containment relationship is not an alias and must not silently restore a child")
        } catch (error: AssistantFailure) {
            assertEquals(AssistantProtocolIssue.INVALID_GROUPS, error.protocolIssue)
        }
    }

    @Test fun allGuideStagesPrioritizeWholeTripDestinationsOverAuthorMarkedDetails() {
        val config = ProviderConfig("https://example.test", "model", "fixture")
        val image = PreparedImage("i", "image/jpeg", byteArrayOf(1))
        val guideRequests = listOf(
            BatchToolProtocol.request(config, "攻略", "城市", travelMaterials = true),
            ImageToolProtocol.request(config, image, 1, "攻略", "城市", travelMaterials = true),
            PlaceNormalizationProtocol.request(config, listOf(marked("博物馆")), "攻略", "城市"),
        )
        guideRequests.map(::systemPrompt).forEach {
            assertTrue(it.contains("整篇攻略"))
            assertTrue(it.contains("独立游览站点"))
            assertTrue(it.contains("内部小地点"))
            assertTrue(it.contains("包含关系，不是别名"))
            assertFalse(it.contains("作者标签无论地点类别是什么都保留"))
            assertFalse(it.contains("不得因地点类别"))
            assertFalse(it.contains("具体分店、出入口、街道、馆内建筑等不因名称包含而合并"))
        }
        listOf(
            BatchToolProtocol.request(config, "北门外广场", "城市"),
            ImageToolProtocol.request(config, image, 1, "北门外广场", "城市"),
        ).map(::systemPrompt).forEach {
            assertFalse("Manual intake must retain its explicit-name behavior", it.contains("内部小地点"))
        }
    }

    @Test fun wholeGuideKeepsMuseumWithoutRestoringItsAuthorMarkedInternalStops() = runTest {
        val places = listOf(
            marked("徐州饮食文化博物馆"),
            marked("LED互动舞台", "徐州饮食文化博物馆"),
            marked("北门外广场", "徐州饮食文化博物馆"),
            marked("双层舞台", "徐州饮食文化博物馆"),
            marked("粉色打卡墙"),
        )
        val args = """{"groups":[{"canonical":0,"members":[0]}],"discarded":[
            {"id":1,"reason":"NOT_PLACE"},{"id":2,"reason":"NOT_PLACE"},
            {"id":3,"reason":"NOT_PLACE"},{"id":4,"reason":"NOT_PLACE"}]}"""
        val result = normalizer(args).normalize(places,
            "徐州饮食文化博物馆游览说明：演出地点为北门外广场、LED互动舞台、双层舞台。粉色打卡墙为拍照布景。",
            "徐州市")
        assertEquals(listOf("徐州饮食文化博物馆"), result.map { it.query })
    }

    private fun marked(name: String, parent: String = "") = PlaceIntent(name, "徐州市", name,
        sources = listOf(IntakeSource.Image("image", 7, name, ImageTargetEvidence(
            ImageTargetBasis.AUTHOR_MARK, "本图文字：$name",
            if (parent.isEmpty()) ImageEntityRole.PLACE else ImageEntityRole.DISTINCT_PART, parent))))

    private fun systemPrompt(request: String) = Json.parseToJsonElement(request).jsonObject
        .getValue("messages").jsonArray[0].jsonObject.getValue("content").jsonPrimitive.content

    private fun normalizer(args: String): ModelPlaceNormalizer {
        val root = Json.parseToJsonElement(args).jsonObject.toMutableMap()
        root["groups"] = JsonArray(root.getValue("groups").jsonArray.map { value ->
            val group = value.jsonObject.toMutableMap()
            group["classification"] = buildJsonObject {
                put("purpose", "SIGHTSEEING"); put("granularity", "DESTINATION")
                put("independentVisit", true); put("evidenceQuote", when (group.getValue("canonical").jsonPrimitive.int) {
                    1 -> "人物纪念馆"; 5 -> "饮食文化博物馆"; else -> "徐州饮食文化博物馆"
                })
            }
            JsonObject(group)
        })
        root["discarded"] = JsonArray(root.getValue("discarded").jsonArray.map { value ->
            val item = value.jsonObject.toMutableMap()
            val dining = item.getValue("reason").jsonPrimitive.content == "DINING"
            item["classification"] = buildJsonObject {
                put("purpose", if (dining) "DINING" else "SIGHTSEEING")
                put("granularity", if (dining) "DESTINATION" else "INTERNAL_DETAIL")
                put("independentVisit", false)
                put("evidenceQuote", when {
                    dining -> if (item.getValue("id").jsonPrimitive.int == 3) "六合巷" else "云龙让茶"
                    args.contains("\"canonical\":5") -> "公园南门"
                    item.getValue("id").jsonPrimitive.int == 2 -> "北门外广场"
                    item.getValue("id").jsonPrimitive.int == 3 -> "双层舞台"
                    item.getValue("id").jsonPrimitive.int == 4 -> "粉色打卡墙"
                    else -> "LED互动舞台"
                })
            }
            JsonObject(item)
        })
        val classifiedArgs = JsonObject(root).toString()
        val response = """{"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[
            {"type":"function","function":{"name":"normalize_place_candidates","arguments":${JsonPrimitive(classifiedArgs)}}}]}}]}"""
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body(response.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return ModelPlaceNormalizer({ ProviderConfig("https://example.test", "model", "fixture") },
            AssistantTransport(client))
    }
}
