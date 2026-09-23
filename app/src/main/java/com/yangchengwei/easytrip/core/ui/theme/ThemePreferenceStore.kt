package com.yangchengwei.easytrip.core.ui.theme

import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface ThemePersistence {
    fun read(): String?
    fun write(id: String)
}

/** Atomic replacement preserves the previous preference if a write fails. */
class AtomicThemePersistence(file: File) : ThemePersistence {
    private val file = AtomicFile(file)
    override fun read(): String? = try {
        this.file.openRead().bufferedReader().use { it.readText().trim() }
    } catch (_: FileNotFoundException) {
        null
    }

    override fun write(id: String) {
        val stream = file.startWrite()
        try {
            stream.write(id.toByteArray(Charsets.UTF_8))
            // AtomicFile's finishWrite can log some IO failures rather than throw them.
            stream.fd.sync()
            file.finishWrite(stream)
            check(read() == id) { "Theme preference was not persisted" }
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }
}

class ThemePreferenceStore(
    private val persistence: ThemePersistence,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    // Constructed before setContent: never paint a transient default palette on cold start.
    private val mutableTheme = MutableStateFlow(ThemePalette.fromId(runCatching { persistence.read() }.getOrNull()))
    val theme: StateFlow<ThemePalette> = mutableTheme.asStateFlow()
    private val mutex = Mutex()

    suspend fun apply(palette: ThemePalette): Result<Unit> = mutex.withLock {
        withContext(io + NonCancellable) {
            runCatching {
                if (palette != mutableTheme.value) persistence.write(palette.id)
                mutableTheme.value = palette
            }
        }
    }
}
