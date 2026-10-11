package com.yangchengwei.easytrip.assistant

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Bounded best-effort diagnostic cache, not user data. Keep only complete, recent JSON lines. */
class AssistantSearchLogFile(
    private val directory: File,
    private val maxBytes: Int = 64 * 1024,
) : AssistantSearchDiagnostics {
    private val mutex = Mutex()

    override suspend fun record(event: AssistantSearchEvent) = withContext(Dispatchers.IO) {
        mutex.withLock {
            directory.mkdirs()
            val file = File(directory, "assistant-search.jsonl")
            val line = event.toJsonLine() + "\n"
            val bytes = line.toByteArray(Charsets.UTF_8).size
            if (bytes > maxBytes) return@withLock
            if (file.exists() && file.length() + bytes > maxBytes) {
                // Ignore an unexpectedly oversized/corrupt cache instead of reading an unbounded file.
                val previous = if (file.length() <= maxBytes) file.readLines() else emptyList()
                var remaining = maxBytes - bytes
                val retained = previous.asReversed().takeWhile {
                    remaining -= it.toByteArray(Charsets.UTF_8).size + 1
                    remaining >= 0
                }.asReversed()
                file.writeText(retained.joinToString("", transform = { "$it\n" }) + line)
            } else {
                file.appendText(line)
            }
        }
    }
}
