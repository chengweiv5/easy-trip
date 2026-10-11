package com.yangchengwei.easytrip.assistant

import java.security.MessageDigest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class SearchOutcome { STARTED, READY, NOT_FOUND, CITY_MISMATCH, SDK_ERROR, TIMEOUT, ERROR, CANCELLED }

/** Only structured, redacted fields cross the logging boundary; never pass SDK messages or inputs. */
data class AssistantSearchEvent(
    val session: String,
    val run: Long,
    val item: Int,
    val queryId: String,
    val cityId: String,
    val cityQualified: Boolean,
    val outcome: SearchOutcome,
    val elapsedMs: Long = 0,
    val resultCount: Int = 0,
    val sameCityCount: Int = 0,
    val errorCode: Int? = null,
    val timestampMs: Long = System.currentTimeMillis(),
) {
    fun toJsonLine(): String = buildJsonObject {
        put("session", session); put("run", run); put("item", item)
        put("queryId", queryId); put("cityId", cityId); put("cityQualified", cityQualified)
        put("outcome", outcome.name); put("elapsedMs", elapsedMs)
        put("resultCount", resultCount); put("sameCityCount", sameCityCount)
        errorCode?.let { put("errorCode", it) }
        put("timestampMs", timestampMs)
    }.toString()
}

fun interface AssistantSearchDiagnostics {
    suspend fun record(event: AssistantSearchEvent)

    companion object { val NONE = AssistantSearchDiagnostics { } }
}

/** Per-controller random salt prevents cross-session correlation or a static place-name dictionary. */
internal fun searchFingerprint(salt: String, value: String): String =
    MessageDigest.getInstance("SHA-256").digest("$salt\u0000$value".toByteArray(Charsets.UTF_8))
        .take(12).joinToString("") { "%02x".format(it) }
