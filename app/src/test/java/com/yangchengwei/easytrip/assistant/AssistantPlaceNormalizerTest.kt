package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class AssistantPlaceNormalizerTest {
    @Test fun wholeGuideMayDiscardMarkedDetailsWhileKeepingIndependentMuseum() {
        val source = IntakeSource.Image("i", 1, "北城(南门)",
            ImageTargetEvidence(ImageTargetBasis.AUTHOR_MARK, "作者编号4",
                ImageEntityRole.DISTINCT_PART, "北城"))
        val places = listOf(
            PlaceIntent("北城", "北市", "北城"),
            PlaceIntent("北城(南门)", "北市", "北城(南门)", sources = listOf(source)),
            PlaceIntent("北城美术馆", "北市", "北城美术馆", sources = listOf(
                IntakeSource.Image("i", 1, "北城美术馆",
                    ImageTargetEvidence(ImageTargetBasis.AUTHOR_MARK, "作者标签和图钉")))),
        )
        val model = """{"groups":[{"canonical":0,"members":[0]},{"canonical":2,"members":[2]}],"discarded":[{"id":1,"reason":"NOT_PLACE"}]}"""
        val result = PlaceNormalizationProtocol.decode(response(model), places)
        assertEquals(listOf("北城", "北城美术馆"), result.map { it.query })
        assertFalse(result[1].needsReview)
    }
    @Test fun authorEvidenceDoesNotExemptDiningAndProtectedItemsStillRequireExactIdCoverage() {
        val venue = PlaceIntent("北城餐馆", "北市", "北城餐馆", sources = listOf(
            IntakeSource.Image("i", 1, "北城餐馆",
                ImageTargetEvidence(ImageTargetBasis.AUTHOR_MARK, "作者标签"))))
        assertTrue(PlaceNormalizationProtocol.decode(response(
            """{"groups":[],"discarded":[{"id":0,"reason":"DINING"}]}"""), listOf(venue)).isEmpty())
        assertThrows(AssistantFailure::class.java) { PlaceNormalizationProtocol.decode(
            response("""{"groups":[],"discarded":[]}"""), listOf(venue)) }
    }

    @Test fun legacyGateCannotBeAnAliasOfParentButCanBeDiscardedAsInternalAccess() {
        val places = listOf("北城", "北城(南门)", "望月山", "望月山风景区")
            .map { PlaceIntent(it, "北市", it) }
        val args = """{"groups":[{"canonical":0,"members":[0,1]},{"canonical":3,"members":[2,3]}],"discarded":[]}"""
        assertThrows(AssistantFailure::class.java) { PlaceNormalizationProtocol.decode(response(args), places) }
        assertEquals(listOf("北城", "望月山风景区"), PlaceNormalizationProtocol.decode(response(
            """{"groups":[{"canonical":0,"members":[0]},{"canonical":3,"members":[2,3]}],"discarded":[{"id":1,"reason":"NOT_PLACE"}]}"""),
            places).map { it.query })
    }
    @Test fun parentConflictDoesNotSplitSafeAliasesAndPartAliasesStillMerge() {
        val gateEvidence = listOf(IntakeSource.Image("i", 1, "北城南门城楼",
            ImageTargetEvidence(ImageTargetBasis.AUTHOR_MARK, "作者标记", ImageEntityRole.DISTINCT_PART, "北城")))
        val places = listOf(
            PlaceIntent("北城", "北市", "北城"),
            PlaceIntent("北城古城", "北市", "北城古城"),
            PlaceIntent("北城南门", "北市", "北城南门"),
            PlaceIntent("北城南门城楼", "北市", "北城南门城楼", sources = gateEvidence),
        )
        val mixed = """{"groups":[{"canonical":1,"members":[0,1,2,3]}],"discarded":[]}"""
        assertThrows(AssistantFailure::class.java) { PlaceNormalizationProtocol.decode(response(mixed), places) }
        val partOnly = """{"groups":[{"canonical":1,"members":[0,1]}],"discarded":[]}"""
        assertEquals(1, PlaceNormalizationProtocol.decode(response(partOnly), places.takeLast(2)).size)
    }
    @Test fun distinctPartMetadataAloneDoesNotSplitAliasesWhenNoParentIsInGroup() {
        val places = listOf(
            PlaceIntent("纪念馆", "北市", "纪念馆"),
            PlaceIntent("园内人物纪念馆", "北市", "园内人物纪念馆", sources = listOf(
                IntakeSource.Image("i", 1, "园内人物纪念馆",
                    ImageTargetEvidence(ImageTargetBasis.RECOMMENDED, "作者推荐",
                        ImageEntityRole.DISTINCT_PART, "北城公园")))),
        )
        assertEquals(listOf("园内人物纪念馆"), PlaceNormalizationProtocol.decode(response(
            """{"groups":[{"canonical":1,"members":[0,1]}],"discarded":[]}"""), places).map { it.query })
    }
    @Test fun shortNamedGateEvidenceKeepsParentAndMergesLongAliasInEitherOrder() {
        val gate = PlaceIntent("荣华门", "北市", "荣华门", sources = listOf(
            IntakeSource.Image("i", 1, "荣华门",
                ImageTargetEvidence(ImageTargetBasis.AUTHOR_MARK, "作者标记",
                    ImageEntityRole.DISTINCT_PART, "北城"))))
        val alias = PlaceIntent("荣华门城楼", "北市", "荣华门城楼")
        listOf(listOf(gate, alias), listOf(alias, gate)).forEach { gates ->
            val places = listOf(PlaceIntent("北城", "北市", "北城")) + gates
            for (canonical in 1..2) {
                val result = PlaceNormalizationProtocol.decode(response(
                    """{"groups":[{"canonical":0,"members":[0]},{"canonical":$canonical,"members":[1,2]}],"discarded":[]}"""), places)
                assertEquals(2, result.size)
                assertEquals("北城", result.first().query)
                assertTrue(result.last().query.startsWith("荣华门"))
                assertEquals(2, result.last().sources.size)
            }
        }
    }
    private val candidates = listOf(
        PlaceIntent("九龙峪", "青州", "九龙峪", sources = listOf(IntakeSource.Text("九龙峪"))),
        PlaceIntent("九龙峪景区", "青州市", "九龙峪景区", sources = listOf(IntakeSource.Image("i1", 1, "九龙峪景区"))),
        PlaceIntent("博物馆", "青州", "博物馆", needsReview = true),
        PlaceIntent("青州博物馆", "青州", "青州博物馆"),
        PlaceIntent("青州糗糕", "青州", "青州糗糕"),
        PlaceIntent("丁妈妈糗糕", "青州", "丁妈妈糗糕"),
        PlaceIntent("古州往事客栈", "青州", "古州往事客栈"),
    )
    private val valid = """{"groups":[{"canonical":1,"members":[0,1]},{"canonical":3,"members":[2,3]},{"canonical":6,"members":[6]}],"discarded":[{"id":4,"reason":"FOOD"},{"id":5,"reason":"DINING"}]}"""
    private fun response(args: String) = """{"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[{"type":"function","function":{"name":"normalize_place_candidates","arguments":${JsonPrimitive(args)}}}]}}]}"""

    @Test fun modelGroupsAliasesDropsFoodDiningAndLodgingAndRetainsEvidence() = runTest {
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            val buffer = okio.Buffer()
            chain.request().body!!.writeTo(buffer)
            val request = buffer.readUtf8()
            assertTrue(request.contains("九龙峪景区"))
            assertTrue(request.contains("单独食品"))
            assertTrue(request.contains("餐饮地点全部剔除"))
            assertTrue(request.contains("住宿酒店"))
            assertTrue(request.contains("不是为了"))
            assertFalse(request.contains("secret"))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body(response(valid.replace("\"canonical\":1,", "\"classification\":{\"purpose\":\"SIGHTSEEING\",\"granularity\":\"DESTINATION\",\"independentVisit\":true,\"evidenceQuote\":\"九龙峪景区\"},\"canonical\":1,")
                    .replace("\"canonical\":3,", "\"classification\":{\"purpose\":\"SIGHTSEEING\",\"granularity\":\"DESTINATION\",\"independentVisit\":true,\"evidenceQuote\":\"青州博物馆\"},\"canonical\":3,")
                    .replace("\"canonical\":6,", "\"classification\":{\"purpose\":\"LODGING\",\"granularity\":\"DESTINATION\",\"independentVisit\":true,\"evidenceQuote\":\"古州往事客栈\"},\"canonical\":6,")
                    .replace("\"reason\":\"FOOD\"", "\"reason\":\"FOOD\",\"classification\":{\"purpose\":\"DINING\",\"granularity\":\"DESTINATION\",\"independentVisit\":false,\"evidenceQuote\":\"糗糕\"}")
                    .replace("\"reason\":\"DINING\"", "\"reason\":\"DINING\",\"classification\":{\"purpose\":\"DINING\",\"granularity\":\"DESTINATION\",\"independentVisit\":false,\"evidenceQuote\":\"丁妈妈糗糕\"}")
                ).toResponseBody("application/json".toMediaType())).build()
        }.build()
        val normalizer = ModelPlaceNormalizer({ ProviderConfig("https://example.test", "model", "secret") }, AssistantTransport(client))
        val places = normalizer.normalize(candidates, "青州博物馆和九龙峪攻略；糗糕是小吃，丁妈妈糗糕是店名。", "青州")
        assertEquals(listOf("九龙峪景区", "青州博物馆"), places.map { it.query })
        assertEquals(2, places.first().sources.size)
        assertFalse(places[1].needsReview)
        assertEquals(1, calls)
    }

    @Test fun refusesMissingDuplicateInventedOrCrossCityOutputAndIgnoresUnknownFields() {
        assertEquals(PlaceNormalizationProtocol.decode(response(valid), candidates),
            PlaceNormalizationProtocol.decode(response(valid.replace("\"canonical\":1", "\"query\":\"新地点\",\"canonical\":1")), candidates))
        listOf(
            valid.replace("\"members\":[0,1]", "\"members\":[1]"),
            valid.replace("\"members\":[0,1]", "\"members\":[0,1,1]"),
            valid.replace("\"canonical\":1", "\"canonical\":99"),
            valid.replace("\"reason\":\"FOOD\"", "\"reason\":\"TOO_MANY\""),
        ).forEach { args ->
            assertThrows(AssistantFailure::class.java) { PlaceNormalizationProtocol.decode(response(args), candidates) }
        }
        val otherCity = candidates.toMutableList().also { it[1] = it[1].copy(city = "杭州") }
        assertThrows(AssistantFailure::class.java) { PlaceNormalizationProtocol.decode(response(valid), otherCity) }
    }

    @Test fun normalizationDoesNotDiscardValidPlacesToFitFinalThirtyLimit() {
        val many = (0..30).map { PlaceIntent("地点$it", "青州", "地点$it") }
        val groups = many.indices.joinToString(",") { """{"canonical":$it,"members":[$it]}""" }
        assertEquals(31, PlaceNormalizationProtocol.decode(response("""{"groups":[$groups],"discarded":[]}"""), many).size)
    }

    @Test fun normalizationPromptRequiresCanonicalMembershipAndCompleteDisjointPartition() {
        val request = Json.parseToJsonElement(PlaceNormalizationProtocol.request(
            ProviderConfig("https://example.test", "model", "secret"),
            listOf(PlaceIntent("公园", "北城", "公园")), "公园", "北城")).jsonObject
        val system = request.getValue("messages").jsonArray[0].jsonObject.getValue("content").jsonPrimitive.content
        assertTrue(system.contains("canonical必须包含在本组members中"))
        assertTrue(system.contains("不能把同一canonical拆成多个组"))
        val canonical = request.getValue("tools").jsonArray[0].jsonObject.getValue("function").jsonObject
            .getValue("parameters").jsonObject.getValue("properties").jsonObject.getValue("groups").jsonObject
            .getValue("items").jsonObject.getValue("properties").jsonObject.getValue("canonical").jsonObject
        assertTrue(canonical.getValue("description").jsonPrimitive.content.contains("members"))
    }

    @Test fun normalizationRequestsContextualAliasesAndCorroboratedOriginalSpelling() {
        val request = Json.parseToJsonElement(PlaceNormalizationProtocol.request(
            ProviderConfig("https://example.test", "model", "secret"),
            listOf(PlaceIntent("望月公园", "北城", "望月公园")), "望月公园", "北城")).jsonObject
        val system = request.getValue("messages").jsonArray[0].jsonObject.getValue("content").jsonPrimitive.content
        assertTrue(system.contains("同城同一行程区域内唯一对应的专名"))
        assertTrue(system.contains("不能仅因短名可能表示园内建筑就臆测"))
        assertTrue(system.contains("多个独立来源一致支持的清晰原字"))
        assertTrue(system.contains("不按输入先后或正文优先"))
        assertTrue(system.contains("不能自行纠错生成候选中没有的新写法"))
    }

    @Test fun fixedSystemRulesAreTripIndependentAndTravelFactsStayInUserData() {
        val config = ProviderConfig("https://example.test", "model", "secret")
        val image = PreparedImage("id", "image/jpeg", byteArrayOf(1))
        val inputs = listOf("青州" to "九龙峪景区", "临江市" to "望月山公园")
        val requests = inputs.map { (city, name) ->
            listOf(
                BatchToolProtocol.request(config, name, city, 100, travelMaterials = true),
                ImageToolProtocol.request(config, image, 2, name, city, 100, travelMaterials = true),
                PlaceNormalizationProtocol.request(config, listOf(PlaceIntent(name, city, name)), name, city),
            ).map { Json.parseToJsonElement(it).jsonObject.getValue("messages").jsonArray }
        }
        requests.first().indices.forEach { stage ->
            val systems = requests.map { it[stage][0].jsonObject.getValue("content").jsonPrimitive.content }
            assertEquals("Rules must not depend on the current trip", systems.first(), systems.last())
            systems.forEach { system ->
                listOf("青州", "九龙峪", "糗糕", "临江市", "望月山").forEach { travelFact ->
                    assertFalse("Specific travel fact leaked into fixed system rules: $travelFact", system.contains(travelFact))
                }
            }
            requests.forEachIndexed { index, stages ->
                assertTrue(stages[stage][1].jsonObject.getValue("content").toString().contains(inputs[index].first))
            }
        }
        requests.forEachIndexed { index, stages ->
            val payload = Json.parseToJsonElement(stages[2][1].jsonObject.getValue("content").jsonPrimitive.content).jsonObject
            assertEquals(inputs[index].first, payload.getValue("defaultCity").jsonPrimitive.content)
            assertEquals(inputs[index].second, payload.getValue("context").jsonPrimitive.content)
            assertEquals(inputs[index].second, payload.getValue("candidates").jsonArray.single().jsonObject.getValue("name").jsonPrimitive.content)
        }
    }
}
