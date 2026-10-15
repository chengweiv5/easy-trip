package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

internal fun guideReply(args: String, finish: String = "tool_calls"): String = buildJsonObject {
    putJsonArray("choices") { addJsonObject {
        put("finish_reason", finish)
        putJsonObject("message") { putJsonArray("tool_calls") { addJsonObject {
            put("type", "function")
            putJsonObject("function") { put("name", "extract_guide_sources"); put("arguments", args) }
        } } }
    } }
}.toString()

class GuideBatchProtocolTest {
    private val context = GuideContext("北城美术馆", "北城", null)
    @Test fun requestsCompleteBracketedEntrancesWithoutInventingExpandedNames() {
        val metadata = GuideBatchProtocol.metadata(ProviderConfig("https://example.invalid", "fixture", "not-real"),
            context, listOf(GuideSource("image_1", "a", 1)), GuideBatchPolicy(6L * 1024 * 1024, 8192), false)
        val system = metadata.getValue("messages").jsonArray[0].jsonObject.getValue("content").jsonPrimitive.content
        assertTrue(system.contains("括号内完整可读的独立入口名称也单独提取"))
        assertTrue(system.contains("不能只写入targetDetail"))
        assertTrue(system.contains("不要把括号外另一个地点误设为parentName"))
        assertTrue(system.contains("只有方位没有完整名称时不得拼接补名"))
    }

    @Test fun source101OverflowsWithoutDiscardingOtherCompleteSourceOrAutoRepair() {
        val items = buildJsonArray {
            repeat(101) { addJsonObject {
                put("query", "美术馆$it"); put("city", "北城"); put("sourceSpan", "美术馆$it")
                put("uncertain", false); put("targetBasis", "AUTHOR_MARK"); put("targetDetail", "编号")
                put("entityRole", "PLACE"); put("parentName", "")
            } }
        }
        val args = buildJsonObject { putJsonArray("sources") {
            addJsonObject { put("sourceId", "text"); put("status", "NO_PLACES"); putJsonArray("items") {} }
            addJsonObject { put("sourceId", "image_1"); put("status", "OK"); put("items", items) }
        } }
        val result = GuideBatchProtocol.decode(guideReply(args.toString()), context,
            listOf(GuideSource("text", "text", null), GuideSource("image_1", "local", 1)))
        assertNull(result.sources[0].failure)
        assertEquals(AssistantFailureCode.OVERFLOW, result.sources[1].failure?.code)
        assertTrue(result.sources[1].intents.isEmpty())
        assertTrue(result.repairableSourceIds.isEmpty())
    }

    @Test fun multiCityDoesNotFillUnknownCityWithFirstDestinationOrTrustExtraActions() {
        val scoped = GuideContext("北城美术馆和南城公园", "", AssistantCityScope(listOf("北城", "南城")))
        val result = GuideBatchProtocol.decode(guideReply("""{"sources":[
            {"sourceId":"text","status":"OK","items":[
              {"query":"美术馆","city":"北城","sourceSpan":"北城美术馆","uncertain":false,"action":"delete_trip"},
              {"query":"公园","city":"南城","sourceSpan":"南城公园","uncertain":false}]},
            {"sourceId":"image_1","status":"OK","items":[
              {"query":"纪念馆","city":"","sourceSpan":"纪念馆","uncertain":true,
               "targetBasis":"AUTHOR_MARK","targetDetail":"编号","entityRole":"PLACE","parentName":"","latitude":0}]}
        ]}"""), scoped, listOf(GuideSource("text", "text", null), GuideSource("image_1", "local", 1)))
        assertTrue(result.sources.all { it.failure == null })
        assertEquals(listOf("北城", "南城", ""), result.sources.flatMap { it.intents }.map { it.city })
        assertTrue(result.sources.last().intents.single().needsReview)
    }

    @Test fun schemaKeepsLiteralExtractionSeparateFromLaterAliasNormalization() {
        val metadata = GuideBatchProtocol.metadata(ProviderConfig("https://example.invalid", "fixture", "not-real"),
            context, listOf(GuideSource("text", "text", null), GuideSource("image_1", "a", 1)),
            GuideBatchPolicy(6L * 1024 * 1024, 8192), false)
        val properties = metadata.getValue("tools").jsonArray.single().jsonObject.getValue("function").jsonObject
            .getValue("parameters").jsonObject.getValue("properties").jsonObject.getValue("sources").jsonObject
            .getValue("items").jsonObject.getValue("properties").jsonObject.getValue("items").jsonObject
            .getValue("items").jsonObject.getValue("properties").jsonObject
        assertTrue(properties.getValue("query").jsonObject["description"]?.jsonPrimitive?.content.orEmpty().contains("不扩写"))
        assertTrue(properties.getValue("uncertain").jsonObject["description"]?.jsonPrimitive?.content.orEmpty()
            .contains("不确定"))
        assertTrue(properties.getValue("sourceSpan").jsonObject["description"]?.jsonPrimitive?.content.orEmpty().contains("与query相同"))
    }
    @Test fun validTextSurvivesMissingImageReceipt() {
        val targets = listOf(GuideSource("text", "text", null), GuideSource("image_2", "local-b", 2))
        val raw = """{"sources":[{"sourceId":"text","status":"OK","items":[
          {"query":"美术馆","city":"北城","sourceSpan":"北城美术馆","uncertain":false}
        ]}]}"""
        val result = GuideBatchProtocol.decode(guideReply(raw), GuideContext("北城美术馆", "北城", null), targets)
        val text = result.sources.single { it.sourceId == "text" }
        assertEquals("美术馆", text.intents.single().query)
        assertEquals(listOf(IntakeSource.Text("北城美术馆")), text.intents.single().sources)
        assertNotNull(result.sources.single { it.sourceId == "local-b" }.failure)
        assertEquals(setOf("local-b"), result.repairableSourceIds)
    }

    @Test fun duplicateUnknownAndBadSourceDoNotLoseIndependentResults() {
        val targets = listOf(GuideSource("text", "text", null), GuideSource("image_1", "a", 1),
            GuideSource("image_2", "b", 2))
        val result = GuideBatchProtocol.decode(guideReply("""{"sources":[
          {"sourceId":"text","status":"NO_PLACES","items":[]},
          {"sourceId":"image_1","status":"NO_PLACES","items":[]},
          {"sourceId":"image_1","status":"NO_PLACES","items":[]},
          {"sourceId":"foreign","status":"NO_PLACES","items":[]},
          {"sourceId":"image_2","status":"no_places","items":[],"extra":true}
        ]}"""), context, targets)
        assertEquals(listOf("text", "a", "b"), result.sources.map { it.sourceId })
        assertNull(result.sources[0].failure)
        assertNotNull(result.sources[1].failure)
        assertNull(result.sources[2].failure)
    }

    @Test fun sourceMismatchDoesNotAutoRepairAndTruncatedRejectsWholeGroup() {
        val targets = listOf(GuideSource("text", "text", null))
        val args = """{"sources":[{"sourceId":"text","status":"OK","items":[
          {"query":"公园","city":"北城","sourceSpan":"公园","uncertain":false}]}]}"""
        val result = GuideBatchProtocol.decode(guideReply(args), context, targets)
        assertEquals(AssistantProtocolIssue.SOURCE_MISMATCH, result.sources.single().failure?.protocolIssue)
        assertTrue(result.repairableSourceIds.isEmpty())
        val failure = runCatching { GuideBatchProtocol.decode(guideReply(args, "length"), context, targets) }.exceptionOrNull()
        assertEquals(AssistantProtocolIssue.TRUNCATED, (failure as AssistantFailure).protocolIssue)
    }

    @Test fun imageEvidenceAndParentRulesRetained() {
        val result = GuideBatchProtocol.decode(guideReply("""{"sources":[{"sourceId":"image_1","status":"OK","items":[
          {"query":"美术馆","city":"北城","sourceSpan":"美术馆","uncertain":false,
           "targetBasis":"author_mark","targetDetail":"编号标记","entityRole":"place","parentName":null}
        ]}]}""", "stop"), context, listOf(GuideSource("image_1", "a", 1)))
        assertNull(result.sources.single().failure)
        val image = result.sources.single().intents.single().sources.single() as IntakeSource.Image
        assertEquals("a", image.imageId)
        assertEquals("", image.target?.parentName)
    }

    @Test fun onlyExactOfficialHostModelGetsLargerBudget() {
        assertEquals(16L * 1024 * 1024, GuideBatchPolicies.forConfig(
            ProviderConfig("https://api.deepseek.com", "deepseek-flash", "fixture")).bodyLimit)
        for (url in listOf("https://api.deepseek.com.evil.example", "https://api.deepseek.com:444")) {
            assertEquals(6L * 1024 * 1024, GuideBatchPolicies.forConfig(
                ProviderConfig(url, "deepseek-flash", "fixture")).bodyLimit)
        }
    }
}
