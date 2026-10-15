package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.assistant.cityfirst.GuideResultCodec
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class GuideEvidenceCapacityTest {
    @Test fun complete400Evidence100PlacesFitsRegularMulticityAndLongNameRecovery() = runBlocking {
        for (mode in listOf("ordinary", "multicity", "long")) {
            val binding = GuideBatchBinding("11111111-1111-4111-8111-111111111111", 0, "范围")
            fun name(i: Int) = if (mode == "ordinary") "东山第${i + 1}公园"
                else "东山第${i + 1}景区" + (if (mode == "long") "历史文化展览园区".repeat(9) else "") + "南门"
            fun city(i: Int) = if (mode == "ordinary" || i < 50) "甲城市" else "乙城市"
            val evidence = (0 until 400).map { index ->
                val i = index % 100
                val source = "0000000${index / 100}-0000-4000-8000-000000000001"
                val fingerprint = (index / 100).toString().padStart(64, '0')
                val quote = city(i) + name(i)
                GuideEvidence(guideEvidenceId(binding, source, fingerprint, GuideEvidenceKind.AUTHOR_LABEL, quote),
                    source, index / 100 + 1, fingerprint, binding, quote, GuideEvidenceKind.AUTHOR_LABEL,
                    ImageTargetBasis.AUTHOR_MARK, false, "作者标记")
            }
            val arguments = buildJsonObject {
                putJsonArray("evidenceResults") { evidence.forEachIndexed { index, _ -> addJsonObject {
                    val i = index % 100
                    put("e", index); put("status", "PARSED")
                    putJsonArray("mentions") { addJsonObject {
                        put("literal", name(i)); put("occurrence", 0); put("city", city(i))
                        putJsonArray("cityEvidenceRefs") { add(index) }
                        put("role", if (mode == "ordinary") "PLACE" else "DISTINCT_PART")
                        put("parentLiteral", if (mode == "ordinary") "" else name(i).removeSuffix("南门"))
                        put("partKind", if (mode == "ordinary") JsonNull else JsonPrimitive("GATE"))
                        put("uncertain", false)
                        putJsonObject("classification") {
                            put("purpose", "SIGHTSEEING"); put("granularity", "DESTINATION")
                            put("independentVisit", true); put("evidenceQuote", evidence[index].rawQuote)
                        }
                    } }
                } } }
                putJsonArray("groups") { repeat(100) { i -> addJsonObject {
                    putJsonArray("members") { repeat(4) { source ->
                        add(buildJsonArray { add(source * 100 + i); add(0) })
                    } }
                    put("relation", "SAME_LITERAL")
                    putJsonArray("evidenceRefs") { repeat(4) { add(it * 100 + i) } }
                    putJsonArray("frozenAnchors") {}
                } } }
            }
            val response = entityReply(arguments.toString())
            val context = GuideContext("", "", AssistantCityScope(listOf("甲城市", "乙城市")))
            var calls = 0
            val config = ProviderConfig("https://api.deepseek.com", "deepseek-flash", "fixture")
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                calls++
                assertTrue(chain.request().body!!.contentLength() <= 6L * 1024 * 1024)
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body(response.toResponseBody()).build()
            }.build()
            val resolution = ModelGuideEvidenceResolver({ config }, AssistantTransport(client))
                .resolve(evidence, context, emptyList())
            assertEquals("$mode must resolve all entities", 100, resolution.places.size)
            assertEquals(400, resolution.places.sumOf { it.mentions.size })
            assertTrue(resolution.pending.isEmpty())
            assertEquals(1, calls)
            assertTrue(response.toByteArray().size <= GuideEvidenceBudget.transport.responseBytes)
            val sources = evidence.groupBy { it.sourceId }.map { (id, es) ->
                ParsedSource(id, emptyList(), evidence = es)
            }
            val state = PlaceAssistantState(guideVersion = GuideProtocolVersion.EVIDENCE_V2,
                guideCheckpoint = GuideEvidenceCheckpoint(binding, sources, resolution, emptyList()),
                parsedMaterials = sources,
                images = sources.mapIndexed { i, s -> AssistantImage(s.sourceId, i + 1, "image/jpeg", 1, 1, 1) },
                items = resolution.places.mapIndexed { i, p ->
                    IntakeItem("place-$i", AssistantPlacePolicy.label(i), p.intent)
                })
            val saved = GuideResultCodec.encode(state)
            assertTrue("$mode recovery budget", saved.toByteArray().size <= 1024 * 1024)
            assertEquals(400, GuideResultCodec.decode(saved).guideCheckpoint!!.sources.sumOf { it.evidence.size })
            assertEquals(100, GuideResultCodec.decode(saved).items.size)
        }
    }
}
