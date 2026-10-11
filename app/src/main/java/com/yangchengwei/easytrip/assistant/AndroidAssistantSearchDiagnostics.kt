package com.yangchengwei.easytrip.assistant

import android.content.Context
import android.util.Log
import java.io.File

/** ADB-readable app cache where available; no export provider, permissions, backup or upload. */
class AndroidAssistantSearchDiagnostics(context: Context) : AssistantSearchDiagnostics {
    private val file = AssistantSearchLogFile(File(
        context.applicationContext.externalCacheDir ?: context.applicationContext.cacheDir, "diagnostics"))

    override suspend fun record(event: AssistantSearchEvent) {
        Log.i("EasyTripSearch", event.toJsonLine())
        file.record(event)
    }
}
