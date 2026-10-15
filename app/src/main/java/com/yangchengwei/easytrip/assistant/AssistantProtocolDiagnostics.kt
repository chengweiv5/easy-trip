package com.yangchengwei.easytrip.assistant

import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

enum class AssistantProtocolStage { TEXT, IMAGE, NORMALIZATION, GUIDE_BATCH }
enum class AssistantProtocolOutcome { SUCCESS, REPAIRING, FAILED }

/** No URLs, input, image identifiers, arguments, model output or exception messages. */
data class AssistantProtocolEvent(
    val operation: String,
    val stage: AssistantProtocolStage,
    val ordinal: Int?,
    val attempt: Int,
    val outcome: AssistantProtocolOutcome,
    val issue: AssistantProtocolIssue?,
    val failureCode: AssistantFailureCode? = null,
    val timestampMs: Long = System.currentTimeMillis(),
) {
    init { UUID.fromString(operation); require(attempt in 1..2); require(ordinal == null || ordinal in 1..20) }
    fun jsonLine(): String = buildJsonObject {
        put("operation", operation); put("stage", stage.name)
        ordinal?.let { put("ordinal", it) }
        put("attempt", attempt); put("outcome", outcome.name)
        issue?.let { put("issue", it.name) }; failureCode?.let { put("failureCode", it.name) }
        put("timestampMs", timestampMs)
    }.toString()
}

fun interface AssistantProtocolDiagnostics {
    suspend fun record(event: AssistantProtocolEvent)
    companion object { val NONE = AssistantProtocolDiagnostics { } }
}

/** Disposable, bounded diagnostic cache. The production instance is shared by AppContainer. */
class AssistantProtocolLogFile(private val directory: File, private val maxBytes: Int = 64 * 1024) : AssistantProtocolDiagnostics {
    private val mutex = Mutex()
    override suspend fun record(event: AssistantProtocolEvent) = withContext(Dispatchers.IO) {
        mutex.withLock {
            directory.mkdirs()
            val file = File(directory, "assistant-protocol.jsonl")
            val line = event.jsonLine() + "\n"
            if (line.toByteArray(Charsets.UTF_8).size > maxBytes) return@withLock
            if (file.exists() && file.length() + line.toByteArray(Charsets.UTF_8).size > maxBytes) {
                val previous = if (file.length() <= maxBytes) file.readLines() else emptyList()
                var remaining = maxBytes - line.toByteArray(Charsets.UTF_8).size
                val retained = previous.asReversed().takeWhile {
                    remaining -= it.toByteArray(Charsets.UTF_8).size + 1
                    remaining >= 0
                }.asReversed()
                file.writeText(retained.joinToString("") { "$it\n" } + line)
            } else file.appendText(line)
        }
    }
}
