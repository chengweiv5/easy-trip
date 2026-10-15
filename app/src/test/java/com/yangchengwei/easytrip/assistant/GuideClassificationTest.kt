package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class GuideClassificationTest {
    @Test fun discardedCandidatesNeedAClassifiedGroundedExclusionNotJustNotPlace() {
        val candidates = listOf(PlaceIntent("历史博物馆", "", "独立参观历史博物馆"),
            PlaceIntent("山间客栈", "", "住宿推荐山间客栈"))
        val args = """{"groups":[],"discarded":[
            {"id":0,"reason":"NOT_PLACE","classification":{"purpose":"UNKNOWN","granularity":"UNKNOWN",
                "independentVisit":false,"evidenceQuote":"独立参观历史博物馆"}},
            {"id":1,"reason":"LODGING","classification":{"purpose":"LODGING","granularity":"DESTINATION",
                "independentVisit":false,"evidenceQuote":"住宿推荐山间客栈"}}]}"""
        val result = PlaceNormalizationProtocol.decode(response(args, "normalize_place_candidates"),
            candidates, requireClassification = true)
        assertEquals(listOf("历史博物馆"), result.map { it.query })
        assertTrue(result.single().needsReview)
    }

    @Test fun joinedRouteNamesAreNotAutomaticallyQueriedAsOnePlace() {
        val joined = imageItem("历史行宫.城市博物馆", "SIGHTSEEING", "DESTINATION")
        val ordinary = imageItem("历史博物馆", "SIGHTSEEING", "DESTINATION")
        val result = ImageToolProtocol.decode(response("""{"overflow":false,"items":[$joined,$ordinary]}"""),
            "image", 1, requireTargetEvidence = true)
        assertEquals(listOf(true, false), result.map { it.needsReview })
        assertFalse(ImageToolProtocol.decode(response("""{"overflow":false,"items":[$joined]}"""),
            "image", 1).single().needsReview)
    }

    @Test fun classifiedHundredPlaceRequestsBudgetForEvidenceWithoutChangingManualRequests() {
        val config = ProviderConfig("https://example.test", "model", "test")
        val requests = listOf(
            BatchToolProtocol.request(config, "历史博物馆", "", travelMaterials = true),
            ImageToolProtocol.request(config, PreparedImage("i", "image/jpeg", byteArrayOf(1)),
                1, "", "", travelMaterials = true),
            PlaceNormalizationProtocol.request(config, listOf(PlaceIntent("历史博物馆", "", "历史博物馆")), "", ""),
        )
        requests.forEach {
            assertEquals(32768, Json.parseToJsonElement(it).jsonObject.getValue("max_tokens").jsonPrimitive.int)
            assertEquals(0, Json.parseToJsonElement(it).jsonObject.getValue("temperature").jsonPrimitive.int)
        }
        assertEquals(8192, Json.parseToJsonElement(BatchToolProtocol.request(config, "历史博物馆", ""))
            .jsonObject.getValue("max_tokens").jsonPrimitive.int)
    }

    @Test fun originalTextRecoversOnlyAnAdjacentNameAndCaption() {
        val item = """{"query":"历史博物馆","city":"","sourceSpan":"博物馆免费参观",
            "classification":{"purpose":"SIGHTSEEING","granularity":"DESTINATION","independentVisit":true,
            "evidenceQuote":"博物馆免费参观"}}"""
        val args = response("""{"overflow":false,"items":[$item]}""")
        val input = "步行到历史博物馆入口\n博物馆免费参观"
        assertEquals("历史博物馆入口\n博物馆免费参观",
            BatchToolProtocol.decode(args, input, travelMaterials = true).single().sourceSpan)
        for (unrelated in listOf("历史博物馆\n\n博物馆免费参观", "另一博物馆\n博物馆免费参观")) {
            assertThrows(AssistantFailure::class.java) {
                BatchToolProtocol.decode(args, unrelated, travelMaterials = true)
            }
        }
    }

    @Test fun normalizationSeesOriginalMentionsEvenWhenTextExtractionFails() {
        val context = "推荐美食\n✅南河街水产小馆（好吃）\n\n独立参观历史博物馆"
        val request = PlaceNormalizationProtocol.request(
            ProviderConfig("https://example.com", "test", "test"),
            listOf(PlaceIntent("南河街", "北城", "南河街")), context, "北城")
        val content = kotlinx.serialization.json.Json.parseToJsonElement(request)
            .jsonObject.getValue("messages").jsonArray.last().jsonObject.getValue("content").jsonPrimitive.content
        val candidate = kotlinx.serialization.json.Json.parseToJsonElement(content).jsonObject
            .getValue("candidates").jsonArray.single().jsonObject
        assertEquals("✅南河街水产小馆（好吃）",
            candidate.getValue("contextMentions").jsonArray.single().jsonPrimitive.content)
    }

    @Test fun imageLabelAndDescriptionMayBeSeparateButUnquotedInterpretationIsNotNameEvidence() {
        val item = imageItem("历史博物馆", "SIGHTSEEING", "DESTINATION")
            .replace("作者介绍历史博物馆", "馆藏丰富，建议参观")
            .replace("\"targetDetail\":\"馆藏丰富，建议参观\"",
                "\"targetDetail\":\"作者标签“历史博物馆”，旁边介绍馆藏丰富，建议参观\"")
        val result = ImageToolProtocol.decode(response("""{"overflow":false,"items":[$item]}"""),
            "image", 1, requireTargetEvidence = true).single()
        assertEquals("历史博物馆", result.query)
        assertEquals("历史博物馆", result.sourceSpan)
        assertEquals("馆藏丰富，建议参观", (result.sources.single() as IntakeSource.Image).excerpt)
        assertFalse(result.needsReview)
        assertThrows(AssistantFailure::class.java) {
            ImageToolProtocol.decode(response("""{"overflow":false,"items":[${item.replace("“历史博物馆”", "应当是历史博物馆")}]}"""),
                "image", 1, requireTargetEvidence = true)
        }
    }

    @Test fun imageCityExpansionReturnsOnlyVisibleNameForReviewWithoutLosingGoodItems() {
        val expanded = imageItem("北城古城", "SIGHTSEEING", "DESTINATION")
            .replace("\"city\":\"\"", "\"city\":\"北城市\"")
            .replace("作者介绍北城古城", "吃完正好逛古城")
        val clear = imageItem("历史博物馆", "SIGHTSEEING", "DESTINATION")
        val result = ImageToolProtocol.decode(response("""{"overflow":false,"items":[$expanded,$clear]}"""),
            "image", 1, requireTargetEvidence = true)
        assertEquals(listOf("古城", "历史博物馆"), result.map { it.query })
        assertEquals(listOf(true, false), result.map { it.needsReview })
        assertThrows(AssistantFailure::class.java) {
            ImageToolProtocol.decode(response("""{"overflow":false,"items":[${expanded.replace("北城古城", "凭空景点")}]}"""),
                "image", 1, requireTargetEvidence = true)
        }
    }

    @Test fun navigationEntranceQualifierCannotBorrowItsParentAttractionClassification() {
        val items = listOf(
            imageItem("湖泊(公园南门)", "SIGHTSEEING", "DESTINATION"),
            imageItem("古城（南门）", "SIGHTSEEING", "DESTINATION"),
            imageItem("历史城门", "SIGHTSEEING", "DESTINATION"),
            imageItem("城门博物馆", "SIGHTSEEING", "DESTINATION"),
        )
        assertEquals(listOf("历史城门", "城门博物馆"), ImageToolProtocol.decode(
            response("""{"overflow":false,"items":[${items.joinToString()}]}"""),
            "image", 1, requireTargetEvidence = true).map { it.query })
        assertEquals(4, ImageToolProtocol.decode(
            response("""{"overflow":false,"items":[${items.joinToString()}]}"""), "image", 1).size)
    }

    @Test fun normalizationRemovesCachedNavigationEntranceWithoutTreatingItAsAnAlias() {
        val places = listOf(PlaceIntent("古城", "北城", "古城"),
            PlaceIntent("古城(南门)", "北城", "古城(南门)"))
        val args = """{"groups":[{"canonical":1,"members":[0,1],
            "classification":{"purpose":"SIGHTSEEING","granularity":"DESTINATION",
                "independentVisit":true,"evidenceQuote":"古城"}}],"discarded":[]}"""
        val result = PlaceNormalizationProtocol.decode(response(args, "normalize_place_candidates"),
            places, requireClassification = true).single()
        assertEquals("古城", result.query)
        assertFalse(result.needsReview)
        assertEquals(listOf(IntakeSource.Text("古城")), result.sources)
        assertEquals(places.first().materialIdentities(), result.materialKeys)
    }

    @Test fun textEvidenceIgnoresOnlyLayoutWhitespaceAndPreservesOriginalSpan() {
        val input = "历史博物馆\n免费参观，建议两小时。"
        val args = """{"overflow":false,"items":[
            {"query":"历史博物馆","city":"","sourceSpan":"历史博物馆",
             "classification":{"purpose":"SIGHTSEEING","granularity":"DESTINATION","independentVisit":true,
             "evidenceQuote":"历史博物馆 免费参观，建议两小时。"}}]}"""
        val item = BatchToolProtocol.decode(response(args), input, travelMaterials = true).single()
        assertEquals(input, item.sourceSpan)
        assertFalse(item.needsReview)
        val fabricated = args.replace("建议两小时", "免费导游")
        assertTrue(BatchToolProtocol.decode(response(fabricated), input,
            travelMaterials = true).single().needsReview)
    }

    @Test fun guideTextExcludesRecommendedLodgingButManualTextDoesNot() {
        val input = "独立参观历史博物馆。住宿推荐山间客栈。"
        val args = """{"overflow":false,"items":[
          {"query":"历史博物馆","city":"","sourceSpan":"独立参观历史博物馆",
           "classification":{"purpose":"SIGHTSEEING","granularity":"DESTINATION","independentVisit":true,"evidenceQuote":"独立参观历史博物馆"}},
          {"query":"山间客栈","city":"","sourceSpan":"住宿推荐山间客栈",
           "classification":{"purpose":"LODGING","granularity":"DESTINATION","independentVisit":true,"evidenceQuote":"住宿推荐山间客栈"}}
        ]}"""
        assertEquals(listOf("历史博物馆"), BatchToolProtocol.decode(response(args), input,
            travelMaterials = true).map { it.query })
        assertEquals(listOf("历史博物馆", "山间客栈"),
            BatchToolProtocol.decode(response(args), input).map { it.query })
    }

    @Test fun matchingIndependentEvidenceCanResolveOnlyTheSameNameAndCity() {
        val unknown = PlaceIntent("历史博物馆", "北城", "历史博物馆", needsReview = true,
            sources = listOf(IntakeSource.Text("历史博物馆")))
        val clear = unknown.copy(sourceSpan = "独立参观历史博物馆", needsReview = false,
            sources = listOf(IntakeSource.Image("image", 1, "独立参观历史博物馆")))
        val args = """{"groups":[
            {"canonical":0,"members":[0,1],"classification":{"purpose":"SIGHTSEEING","granularity":"DESTINATION",
                "independentVisit":true,"evidenceQuote":"独立参观历史博物馆"}},
            {"canonical":2,"members":[2],"classification":{"purpose":"UNKNOWN","granularity":"UNKNOWN",
                "independentVisit":false,"evidenceQuote":"历史博物馆"}}
            ],"discarded":[]}"""
        val result = PlaceNormalizationProtocol.decode(response(args, "normalize_place_candidates"),
            listOf(unknown, clear, unknown.copy(city = "南城")), requireClassification = true)
        assertEquals(2, result.size)
        assertFalse(result[0].needsReview)
        assertEquals("独立参观历史博物馆", result[0].sourceSpan)
        assertEquals(2, result[0].sources.size)
        assertTrue(result[1].needsReview)
    }

    @Test fun imageAuthorRecommendationDoesNotOverrideLodgingOrInternalDetail() {
        val items = listOf(
            imageItem("历史博物馆", "SIGHTSEEING", "DESTINATION"),
            imageItem("山间客栈", "LODGING", "DESTINATION"),
            imageItem("LED互动舞台", "SIGHTSEEING", "INTERNAL_DETAIL"),
        )
        assertEquals(listOf("历史博物馆"),
            ImageToolProtocol.decode(response("""{"overflow":false,"items":[${items.joinToString()}]}"""),
                "image", 1, requireTargetEvidence = true).map { it.query })
    }

    @Test fun normalizationCannotKeepAGroupClassifiedAsLodging() {
        val candidates = listOf(PlaceIntent("历史博物馆", "", "独立参观历史博物馆"),
            PlaceIntent("山间客栈", "", "住宿推荐山间客栈"))
        val args = """{"groups":[
          {"canonical":0,"members":[0],"classification":{"purpose":"SIGHTSEEING","granularity":"DESTINATION","independentVisit":true,"evidenceQuote":"独立参观历史博物馆"}},
          {"canonical":1,"members":[1],"classification":{"purpose":"LODGING","granularity":"DESTINATION","independentVisit":true,"evidenceQuote":"住宿推荐山间客栈"}}
        ],"discarded":[]}"""
        assertEquals(listOf("历史博物馆"),
            PlaceNormalizationProtocol.decode(response(args, "normalize_place_candidates"), candidates,
                requireClassification = true).map { it.query })
    }

    @Test fun unknownOrUngroundedClassificationNeedsReviewWithoutDroppingOtherAttractions() {
        val items = listOf(
            imageItem("历史博物馆", "SIGHTSEEING", "DESTINATION"),
            imageItem("场馆", "UNKNOWN", "UNKNOWN"),
            imageItem("古建筑", "SIGHTSEEING", "DESTINATION")
                .replace("\"evidenceQuote\":\"作者介绍古建筑\"", "\"evidenceQuote\":\"别处的原文\""),
        )
        val parsed = ImageToolProtocol.decode(response("""{"overflow":false,"items":[${items.joinToString()}]}"""),
            "image", 1, requireTargetEvidence = true)
        assertEquals(listOf(false, true, true), parsed.map { it.needsReview })
    }

    @Test fun excludedUseWinsOverContradictoryFlagsMissingQuoteOrUnknownLevel() {
        val items = listOf(
            imageItem("山间客栈", "LODGING", "UNKNOWN").replace("\"evidenceQuote\":\"作者介绍山间客栈\"", "\"evidenceQuote\":\"\""),
            imageItem("游客停车场", "UNKNOWN", "INTERNAL_DETAIL"),
            imageItem("餐厅", "DINING", "DESTINATION"),
            imageItem("中转站", "TRANSPORT", "DESTINATION"),
            imageItem("底图村子", "UNKNOWN", "BACKGROUND"),
        )
        assertTrue(ImageToolProtocol.decode(response("""{"overflow":false,"items":[${items.joinToString()}]}"""),
            "image", 1, requireTargetEvidence = true).isEmpty())
    }

    @Test fun missingClassificationCannotSilentlyUseTheLegacyGuideRules() {
        val item = """{"query":"历史博物馆","city":"","sourceSpan":"历史博物馆"}"""
        val error = assertThrows(AssistantFailure::class.java) {
            BatchToolProtocol.decode(response("""{"overflow":false,"items":[$item]}"""),
                "历史博物馆", travelMaterials = true)
        }
        assertEquals(AssistantProtocolIssue.REQUIRED_FIELDS, error.protocolIssue)
        assertEquals("历史博物馆", BatchToolProtocol.decode(
            response("""{"overflow":false,"items":[$item]}"""), "历史博物馆").single().query)
    }

    @Test fun guideClassificationAcceptsHarmlessExtraFieldsAndKnownEnumCase() {
        val item = imageItem("历史博物馆", " sightseeing ", "destination")
            .replace("\"independentVisit\":true", "\"confidence\":0.6,\"independentVisit\":true")
        val result = ImageToolProtocol.decode(response("""{"overflow":false,"items":[$item]}"""),
            "image", 1, requireTargetEvidence = true)
        assertEquals("历史博物馆", result.single().query)
        assertFalse(result.single().needsReview)
    }

    @Test fun originalTextQuoteRepairsAnOverlyShortSpanWithoutInventingEvidence() {
        val input = "参观历史博物馆入口。博物馆免费参观。"
        val item = """{"query":"历史博物馆","city":"","sourceSpan":"博物馆免费参观",
            "classification":{"purpose":"SIGHTSEEING","granularity":"DESTINATION","independentVisit":true,
            "evidenceQuote":"参观历史博物馆入口。博物馆免费参观"}}"""
        val result = BatchToolProtocol.decode(response("""{"overflow":false,"items":[$item]}"""),
            input, travelMaterials = true).single()
        assertEquals("参观历史博物馆入口。博物馆免费参观", result.sourceSpan)
        assertFalse(result.needsReview)
    }

    @Test fun excludedImageItemWithBadNameCannotBlockGoodItemsOrEnterResults() {
        val excluded = imageItem("内部楼", "OTHER", "INTERNAL_DETAIL")
            .replace("\"sourceSpan\":\"作者介绍内部楼\"", "\"sourceSpan\":\"內部樓\"")
        val kept = imageItem("历史博物馆", "SIGHTSEEING", "DESTINATION")
        assertEquals(listOf("历史博物馆"), ImageToolProtocol.decode(
            response("""{"overflow":false,"items":[$kept,$excluded]}"""),
            "image", 1, requireTargetEvidence = true).map { it.query })
    }

    @Test fun imagePreservesFullVisibleQuoteForWholeGuideJudgement() {
        val item = imageItem("历史博物馆", "SIGHTSEEING", "DESTINATION")
            .replace("\"evidenceQuote\":\"作者介绍历史博物馆\"", "\"evidenceQuote\":\"历史博物馆 步行三分钟\"")
        val result = ImageToolProtocol.decode(response("""{"overflow":false,"items":[$item]}"""),
            "image", 1, requireTargetEvidence = true).single()
        assertEquals("历史博物馆 步行三分钟", (result.sources.single() as IntakeSource.Image).excerpt)
        assertFalse(result.needsReview)
    }

    private fun imageItem(name: String, purpose: String, level: String) = """
        {"query":"$name","city":"","sourceSpan":"作者介绍$name","uncertain":false,
         "targetBasis":"AUTHOR_MARK","targetDetail":"作者介绍$name","entityRole":"PLACE","parentName":"",
         "classification":{"purpose":"$purpose","granularity":"$level","independentVisit":true,"evidenceQuote":"作者介绍$name"}}"""

    private fun response(args: String, tool: String = "search_place_batch") =
        """{"choices":[{"finish_reason":"tool_calls","message":{"tool_calls":[{"type":"function","function":{"name":"$tool","arguments":${JsonPrimitive(args)}}}]}}]}"""
}
