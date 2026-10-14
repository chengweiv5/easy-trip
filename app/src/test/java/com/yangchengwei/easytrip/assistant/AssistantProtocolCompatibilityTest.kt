package com.yangchengwei.easytrip.assistant

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

/** Exercises production parsers at the HTTP response boundary, without real service calls. */
class AssistantProtocolCompatibilityTest {
    private val config = ProviderConfig("https://example.test", "vision", "private-test-key")
    private val image = PreparedImage("image", "image/jpeg", byteArrayOf(1, 2, 3))
    private val item = """{"query":"北城美术馆","city":"北城","sourceSpan":"北城美术馆","uncertain":false,"targetBasis":"AUTHOR_MARK","targetDetail":"作者标签","entityRole":"PLACE","parentName":""}"""
    private fun response(arguments: String, tool: String = "search_place_batch", finish: String = "tool_calls") =
        """{"choices":[{"finish_reason":"$finish","message":{"tool_calls":[{"type":"function","function":{"name":"$tool","arguments":${JsonPrimitive(arguments)}}}]}}]}"""
    private fun client(reply: (String) -> String) = OkHttpClient.Builder().addInterceptor { chain ->
        val buffer = okio.Buffer()
        chain.request().body!!.writeTo(buffer)
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(reply(buffer.readUtf8()).toResponseBody("application/json".toMediaType())).build()
    }.build()

    @Test fun harmlessExtraFieldsDoNotLoseImagePlacesOrTriggerAnotherRequest() = runBlocking {
        var calls = 0
        val extended = item.dropLast(1) + ""","confidence":0.9}"""
        val parser = ImagePlaceParser({ config }, AssistantTransport(client {
            calls++
            response("""{"items":[$extended],"overflow":false,"note":"额外说明不作为指令"}""")
        }), travelMaterials = true)
        val places = parser.parse(image, 2, "", "北城") {}
        assertEquals(listOf("北城美术馆"), places.map { it.query })
        assertEquals(ImageTargetBasis.AUTHOR_MARK, (places.single().sources.single() as IntakeSource.Image).target!!.basis)
        assertEquals(1, calls)
    }

    @Test fun imageOptionalParentAndKnownEnumCasingAreNormalizedWithoutInventingEvidence() = runBlocking {
        val variants = listOf(
            item.replace(",\"parentName\":\"\"", ""),
            item.replace("\"parentName\":\"\"", "\"parentName\":null"),
            item.replace("\"AUTHOR_MARK\"", "\" author_mark \"").replace("\"PLACE\"", "\"place\""),
        )
        for (variant in variants) {
            var calls = 0
            val parser = ImagePlaceParser({ config }, AssistantTransport(client {
                calls++
                response("""{"items":[$variant],"overflow":false}""")
            }), travelMaterials = true)
            val target = (parser.parse(image, 1, "", "北城") {}.single().sources.single() as IntakeSource.Image).target!!
            assertEquals(ImageTargetBasis.AUTHOR_MARK, target.basis)
            assertEquals(ImageEntityRole.PLACE, target.role)
            assertEquals("", target.parentName)
            assertEquals("作者标签", target.detail)
            assertEquals(1, calls)
        }
    }

    @Test fun textAndNormalizerAcceptCompleteStopEnvelopeAndIgnoreUnknownFields() = runBlocking {
        var textCalls = 0
        val text = DeepSeekPlaceParser({ config }, client {
            textCalls++
            response("""{"items":[{"query":"北城美术馆","city":"北城","sourceSpan":"北城美术馆","confidence":0.9}],"overflow":false,"note":"ignored"}""",
                finish = "stop")
        })
        val candidates = text.parse("北城美术馆", "北城")
        assertEquals(listOf("北城美术馆"), candidates.map { it.query })
        var normalizationCalls = 0
        val normalizer = ModelPlaceNormalizer({ config }, AssistantTransport(client {
            normalizationCalls++
            response("""{"groups":[{"canonical":0,"members":[0],"query":"不应成为新地点"}],"discarded":[],"note":"ignored"}""",
                "normalize_place_candidates", "stop")
        }))
        val normalized = normalizer.normalize(candidates, "北城美术馆", "北城")
        assertEquals(listOf("北城美术馆"), normalized.map { it.query })
        assertEquals(listOf(IntakeSource.Text("北城美术馆")), normalized.single().sources)
        assertEquals(1, textCalls)
        assertEquals(1, normalizationCalls)
    }

    @Test fun malformedImageArgumentsAreRepairedOnceUsingOriginalMaterialNotBadModelText() = runBlocking {
        var calls = 0
        val requests = mutableListOf<String>()
        val parser = ImagePlaceParser({ config }, AssistantTransport(client { request ->
            requests += request
            calls++
            if (calls == 1) response("""{"items":[ BAD_MODEL_INSTRUCTION""")
            else response("""{"items":[$item],"overflow":false}""")
        }), travelMaterials = true)
        assertEquals("北城美术馆", parser.parse(image, 2, "", "北城") {}.single().query)
        assertEquals(2, calls)
        assertTrue(requests.all { it.contains("data:image/jpeg;base64,AQID") })
        assertFalse(requests.last().contains("BAD_MODEL_INSTRUCTION"))
        assertFalse(requests.last().contains(config.apiKey))
        assertTrue(requests.last().contains("只修正返回格式"))
    }

    @Test fun unrepairedFormatFailsAfterExactlyTwoCallsAndRetainsSpecificCause() = runBlocking {
        var calls = 0
        val parser = ImagePlaceParser({ config }, AssistantTransport(client {
            calls++; response("""{"items":[""")
        }), travelMaterials = true)
        val failure = runCatching { parser.parse(image, 2, "", "北城") {} }.exceptionOrNull() as AssistantFailure
        assertEquals(2, calls)
        assertEquals(AssistantProtocolIssue.INVALID_ARGUMENTS, failure.protocolIssue)
        assertEquals(SendEvidence.RESPONSE_RECEIVED, failure.sendEvidence)
        assertTrue(failure.userMessage.contains("格式"))
        assertFalse(failure.userMessage.contains("不支持工具调用"))
    }

    @Test fun untrustedIncompleteAndAmbiguousResponsesNeverBecomeSuccessfulOrAutomaticallyRepeat() = runBlocking {
        val cases = listOf(
            response("""{"items":[$item],"overflow":false}""", finish = "length") to AssistantProtocolIssue.TRUNCATED,
            response("""{"items":[$item],"overflow":false}""", finish = "content_filter") to AssistantProtocolIssue.UNSUPPORTED_FINISH,
            response("""{"items":[$item],"overflow":false}""", "save_places") to AssistantProtocolIssue.UNEXPECTED_TOOL,
            response("""{"items":[${item.replace("AUTHOR_MARK", "UNKNOWN")}],"overflow":false}""") to AssistantProtocolIssue.TARGET_EVIDENCE,
            response("""{"items":[${item.replace("\"query\":\"北城美术馆\"", "\"query\":\"猜测景点\"")}],"overflow":false}""") to AssistantProtocolIssue.SOURCE_MISMATCH,
            response("""{"items":[${item.replace("\"PLACE\"", "\"DISTINCT_PART\"").replace(",\"parentName\":\"\"", "")}],"overflow":false}""") to AssistantProtocolIssue.TARGET_EVIDENCE,
            response("""{"items":[{"query":"北城美术馆","city":"北城","sourceSpan":"北城美术馆","uncertain":false}],"overflow":false}""") to AssistantProtocolIssue.TARGET_EVIDENCE,
        )
        for ((reply, expected) in cases) {
            var calls = 0
            val parser = ImagePlaceParser({ config }, AssistantTransport(client { calls++; reply }), travelMaterials = true)
            val failure = runCatching { parser.parse(image, 2, "", "北城") {} }.exceptionOrNull() as AssistantFailure
            assertEquals(expected, failure.protocolIssue)
            assertEquals(1, calls)
        }
    }

    @Test fun emptyImageRemainsNoPlacesAndDoesNotSpendARepairCall() = runBlocking {
        var calls = 0
        val parser = ImagePlaceParser({ config }, AssistantTransport(client {
            calls++; response("""{"items":[],"overflow":false}""")
        }), travelMaterials = true)
        val failure = runCatching { parser.parse(image, 2, "", "北城") {} }.exceptionOrNull() as AssistantFailure
        assertEquals(AssistantFailureCode.NO_PLACES, failure.code)
        assertEquals(1, calls)
    }

    @Test fun normalizerKnownReasonCasingIsCompatibleWithoutAcceptingUnknownReasons() = runBlocking {
        val candidates = listOf(PlaceIntent("小吃店", "北城", "小吃店"))
        val normalizer = ModelPlaceNormalizer({ config }, AssistantTransport(client {
            response("""{"groups":[],"discarded":[{"id":0,"reason":" dining ","note":"ignored"}]}""", "normalize_place_candidates")
        }))
        assertTrue(normalizer.normalize(candidates, "小吃店", "北城").isEmpty())
    }

    @Test fun repairDiagnosticsPersistOnlyBoundedStructuredCausesWithoutSourceOrCredentials() = runBlocking {
        val directory = java.nio.file.Files.createTempDirectory("assistant-protocol-test").toFile()
        try {
            val diagnostics = AssistantProtocolLogFile(directory, 2048)
            var calls = 0
            val transport = AssistantTransport(client {
                calls++
                if (calls == 1) response("""{"items":[ BAD_MODEL_INSTRUCTION""")
                else response("""{"items":[$item],"overflow":false}""")
            }, diagnostics)
            val parser = ImagePlaceParser({ config }, transport, travelMaterials = true)
            parser.parse(image, 2, "", "北城") {}
            val text = directory.resolve("assistant-protocol.jsonl").readText()
            assertTrue(text.contains("INVALID_ARGUMENTS"))
            assertTrue(text.contains("REPAIRING"))
            assertTrue(text.contains("SUCCESS"))
            assertTrue(text.contains("\"ordinal\":2"))
            listOf(config.apiKey, "BAD_MODEL_INSTRUCTION", "北城", "base64", "confidence").forEach {
                assertFalse(text.contains(it))
            }
            repeat(50) { parser.parse(image, 2, "", "北城") {} }
            val file = directory.resolve("assistant-protocol.jsonl")
            assertTrue(file.length() <= 2048)
            assertTrue(file.readLines().isNotEmpty())
            file.readLines().forEach { Json.parseToJsonElement(it).jsonObject }
        } finally { directory.deleteRecursively() }
    }

    @Test fun transportFailuresNeverAutomaticallyRepeatAndDiagnosticsFailureDoesNotCauseRetry() = runBlocking {
        for (status in listOf(401, 429, 500)) {
            var calls = 0
            val http = OkHttpClient.Builder().addInterceptor { chain ->
                calls++
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("failure")
                    .body("""{"error":{"message":"private-error"}}""".toResponseBody()).build()
            }.build()
            val parser = ImagePlaceParser({ config }, AssistantTransport(http), travelMaterials = true)
            assertTrue(runCatching { parser.parse(image, 2, "", "北城") {} }.exceptionOrNull() is AssistantFailure)
            assertEquals(1, calls)
        }
        var calls = 0
        val parser = ImagePlaceParser({ config }, AssistantTransport(client {
            calls++; response("""{"items":[$item],"overflow":false}""")
        }, AssistantProtocolDiagnostics { throw java.io.IOException("disk full") }), travelMaterials = true)
        assertEquals(1, parser.parse(image, 2, "", "北城") {}.size)
        assertEquals(1, calls)
    }

    @Test fun cancellationBeforeRepairDoesNotSendASecondRequestOrPublishResults() = runBlocking {
        val reachedRepair = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val parser = ImagePlaceParser({ config }, AssistantTransport(client {
            calls++; response("""{"items":[""")
        }, AssistantProtocolDiagnostics { event ->
            if (event.outcome == AssistantProtocolOutcome.REPAIRING) {
                reachedRepair.complete(Unit)
                release.await()
            }
        }), travelMaterials = true)
        val task = async { parser.parse(image, 2, "", "北城") {} }
        withTimeout(5_000) { reachedRepair.await() }
        task.cancelAndJoin()
        release.complete(Unit)
        assertTrue(task.isCancelled)
        assertEquals(1, calls)
    }

    @Test fun textAndNormalizationUseOneFormatRepairButNeverRetryGroundingOrGroupCoverageErrors() = runBlocking {
        var textCalls = 0
        val text = DeepSeekPlaceParser({ config }, client {
            textCalls++
            if (textCalls == 1) response("{")
            else response("""{"items":[{"query":"北城美术馆","city":"北城","sourceSpan":"北城美术馆"}],"overflow":false}""")
        })
        val places = text.parse("北城美术馆", "北城")
        assertEquals(2, textCalls)
        var normalizeCalls = 0
        val normalizer = ModelPlaceNormalizer({ config }, AssistantTransport(client {
            normalizeCalls++
            if (normalizeCalls == 1) response("{", "normalize_place_candidates")
            else response("""{"groups":[{"canonical":0,"members":[0]}],"discarded":[]}""", "normalize_place_candidates")
        }))
        assertEquals(1, normalizer.normalize(places, "北城美术馆", "北城").size)
        assertEquals(2, normalizeCalls)
        var missingCoverageCalls = 0
        val invalid = ModelPlaceNormalizer({ config }, AssistantTransport(client {
            missingCoverageCalls++
            response("""{"groups":[],"discarded":[]}""", "normalize_place_candidates")
        }))
        val failure = runCatching { invalid.normalize(places, "", "北城") }.exceptionOrNull() as AssistantFailure
        assertEquals(AssistantProtocolIssue.INVALID_GROUPS, failure.protocolIssue)
        assertEquals(1, missingCoverageCalls)
        var groundingCalls = 0
        val ungrounded = DeepSeekPlaceParser({ config }, client {
            groundingCalls++
            response("""{"items":[{"query":"另一处景点","city":"异地","sourceSpan":"北城美术馆"}],"overflow":false}""")
        })
        assertEquals(AssistantProtocolIssue.SOURCE_MISMATCH,
            (runCatching { ungrounded.parse("北城美术馆", "北城") }.exceptionOrNull() as AssistantFailure).protocolIssue)
        assertEquals(1, groundingCalls)
    }
}
