package com.yangchengwei.easytrip.settings

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface UpdateService {
    suspend fun latest(): OfficialRelease
    suspend fun download(release: OfficialRelease, progress: (Long, Long) -> Unit): File
    fun discard(file: File)
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Current : UpdateState
    data object Ahead : UpdateState
    data class Available(val release: OfficialRelease) : UpdateState
    data class Downloading(val release: OfficialRelease, val bytes: Long, val total: Long) : UpdateState
    data class Ready(val release: OfficialRelease, val file: File, val notice: String? = null) : UpdateState
    data class Failed(val message: String, val release: OfficialRelease? = null) : UpdateState
}

/** User actions are serialized on the UI dispatcher. Network completions are generation-fenced. */
class UpdateController(
    private val installedVersion: String,
    private val service: UpdateService,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var generation = 0L

    fun check() {
        if (mutableState.value is UpdateState.Checking || mutableState.value is UpdateState.Downloading) return
        (mutableState.value as? UpdateState.Ready)?.let { service.discard(it.file) }
        val request = ++generation
        mutableState.value = UpdateState.Checking
        job = scope.launch {
            try {
                val result = decideRelease(installedVersion, service.latest())
                if (request == generation) mutableState.value = when (result) {
                    ReleaseDecision.Current -> UpdateState.Current
                    ReleaseDecision.Ahead -> UpdateState.Ahead
                    is ReleaseDecision.Available -> UpdateState.Available(result.release)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (request == generation) mutableState.value = UpdateState.Failed("暂时无法检查更新，请检查网络连接或稍后重试。")
            }
        }
    }

    fun download() {
        val release = when (val value = mutableState.value) {
            is UpdateState.Available -> value.release
            is UpdateState.Failed -> value.release ?: return
            else -> return
        }
        val request = ++generation
        mutableState.value = UpdateState.Downloading(release, 0, release.asset!!.size)
        job = scope.launch {
            try {
                val file = service.download(release) { bytes, total ->
                    scope.launch {
                        if (request == generation && mutableState.value is UpdateState.Downloading)
                            mutableState.value = UpdateState.Downloading(release, bytes, total)
                    }
                }
                if (request == generation) mutableState.value = UpdateState.Ready(release, file)
                else service.discard(file)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (request == generation) mutableState.value = UpdateState.Failed("下载或安装包校验失败，请重试。", release)
            }
        }
    }

    fun cancel() {
        val prior = mutableState.value
        if (prior !is UpdateState.Checking && prior !is UpdateState.Downloading) return
        ++generation
        job?.cancel()
        mutableState.value = if (prior is UpdateState.Downloading) UpdateState.Available(prior.release) else UpdateState.Idle
    }

    fun installNotice(message: String) {
        val ready = mutableState.value as? UpdateState.Ready ?: return
        mutableState.value = ready.copy(notice = message)
    }

    fun close() {
        ++generation
        job?.cancel()
        (mutableState.value as? UpdateState.Ready)?.let { service.discard(it.file) }
    }
}
