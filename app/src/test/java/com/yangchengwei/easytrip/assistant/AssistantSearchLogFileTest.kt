package com.yangchengwei.easytrip.assistant

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AssistantSearchLogFileTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun boundedFileSurvivesReopeningAndOnlyWritesStructuredEvents() = runBlocking {
        val directory = temporary.newFolder()
        val event = AssistantSearchEvent("session", 1, 0, "redacted", "redacted", true, SearchOutcome.READY)
        val writer = AssistantSearchLogFile(directory, maxBytes = 2048)
        repeat(30) { writer.record(event.copy(run = it.toLong())) }
        val file = File(directory, "assistant-search.jsonl")
        assertTrue(file.length() in 1..2048)
        AssistantSearchLogFile(directory, maxBytes = 2048).record(event.copy(run = 99))
        val records = file.readLines().map { Json.parseToJsonElement(it).jsonObject }
        assertEquals(99, records.last().getValue("run").jsonPrimitive.int)
        assertTrue(records.size < 30)
        assertEquals(setOf("assistant-search.jsonl"), directory.list()!!.toSet())
    }
}
