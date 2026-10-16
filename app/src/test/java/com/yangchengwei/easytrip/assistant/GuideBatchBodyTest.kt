package com.yangchengwei.easytrip.assistant

import java.io.File
import java.util.Base64
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GuideBatchBodyTest {
    @get:Rule val temporary = TemporaryFolder()
    private val config = ProviderConfig("https://example.invalid", "fixture", "not-real")
    private val context = GuideContext("北城美术馆\"路线\n古村", "北城", null)
    private val policy = GuideBatchPolicy(6L * 1024 * 1024, 8192)
    private fun images(count: Int, bytes: Int = 601) = (1..count).map { n ->
        GuidePreparedSource(GuideSource("image_$n", "local-$n", n),
            File(temporary.root, "$n.jpg").apply { writeBytes(ByteArray(bytes) { (n + it).toByte() }) })
    }

    @Test fun textIsSeparateWhileTwentySmallImagesStayTogether() = runTest {
        val sources = listOf(GuidePreparedSource(GuideSource("text", "text", null), null)) + images(20)
        val body = GuideBatchBody()
        val plan = body.groups(config, context, sources, policy)
        assertEquals(2, plan.groups.size); assertTrue(plan.failures.isEmpty())
        assertEquals(listOf("text"), plan.groups.first().sources.map { it.source.localId })
        val group = plan.groups.last()
        val file = body.write(config, context, group, policy, false, temporary.newFolder())
        assertEquals(body.size(config, context, group, policy, false), file.length())
        val json = Json.parseToJsonElement(file.readText()).jsonObject
        val blocks = json.getValue("messages").jsonArray.flatMap {
            it.jsonObject["content"] as? JsonArray ?: JsonArray(emptyList())
        }.map { it.jsonObject }
        val urls = blocks.filter { it["type"]?.jsonPrimitive?.content == "image_url" }
        assertEquals(20, urls.size)
        assertArrayEquals(sources[1].file!!.readBytes(), Base64.getDecoder().decode(
            urls[0].getValue("image_url").jsonObject.getValue("url").jsonPrimitive.content.substringAfter(",")))
    }

    @Test fun exactBoundarySplitsByBytesNotImageCount() {
        val sources = images(2)
        val body = GuideBatchBody()
        val group = GuideGroup(1, sources)
        val limit = maxOf(body.size(config, context, group, policy, false),
            body.size(config, context, group, policy, true))
        assertEquals(1, body.groups(config, context, sources, policy.copy(bodyLimit = limit)).groups.size)
        assertEquals(2, body.groups(config, context, sources, policy.copy(bodyLimit = limit - 1)).groups.size)
    }

    @Test fun changedPreparedFileCannotBeSentUsingStaleMeasurements() = runTest {
        val sources = images(2)
        val body = GuideBatchBody()
        val group = body.groups(config, context, sources, policy).groups.single()
        sources.last().file!!.appendBytes(byteArrayOf(0))
        val output = temporary.newFolder()
        assertTrue(runCatching { body.write(config, context, group, policy, false, output) }.isFailure)
        assertTrue(output.listFiles()!!.isEmpty())
        val plan = body.groups(config, context, sources, policy)
        assertEquals("local-2", plan.failures.single().sourceId)
        assertEquals(listOf("local-1"), plan.groups.single().sources.map { it.source.localId })
    }
}
