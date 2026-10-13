package com.yangchengwei.easytrip.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangchengwei.easytrip.core.model.TravelMode
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TripDraftState(
    val name: String = "", val startDate: LocalDate? = null, val dayCount: Int = 1,
    val travelMode: TravelMode = TravelMode.FLEXIBLE,
    val confirming: Boolean = false, val metadataBusy: Boolean = false, val saving: Boolean = false,
    val warning: String? = null, val error: String? = null, val createdTripId: String? = null,
    val recovering: Boolean = true, val deleted: Boolean = false, val restored: Boolean = false,
    val selectionCount: Int = 0,
    val submissionLocked: Boolean = false,
    val metadataRetryAvailable: Boolean = false,
)

class TripDraftViewModel(
    private val creator: TripCreation,
    private val journal: PendingTripCreation,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val interpret: suspend (TripDraftRequest) -> TripDraftMetadata,
) : ViewModel() {
    private val mutable = MutableStateFlow(TripDraftState())
    val state = mutable.asStateFlow()
    private var dateEdited = false
    private var nameEdited = false
    private var selected = emptyList<ImportItem>()
    private var sources = emptyList<String>()
    private var pending: TripCreationCommand? = null
    private var metadataJob: Job? = null
    private var metadataRequest: TripDraftRequest? = null
    private var revision = 0L
    private var environmentKey: Triple<Long, com.yangchengwei.easytrip.amap.AmapConsentToken?, Boolean>? = null

    /** Kept in the ViewModel, so rebuilding a composition is not an environment change. */
    fun environment(configuration: Long, token: com.yangchengwei.easytrip.amap.AmapConsentToken?) {
        val next = Triple(configuration, token, token?.isActive() == true)
        if (environmentKey != null && environmentKey != next) {
            val interrupted = mutable.value.metadataBusy
            cancelMetadata()
            if (interrupted) mutable.value = mutable.value.copy(metadataRetryAvailable = true,
                warning = "配置或授权已变化，日期识别已停止；可单独重试或手动设置。")
        }
        environmentKey = next
    }

    init { recover() }
    private fun recover() = viewModelScope.launch {
        try {
            val command = withContext(io) { journal.read() }
            if (command == null) {
                pending = null
                mutable.value = mutable.value.copy(restored = false, submissionLocked = false, error = null)
                return@launch
            }
            pending = command
            mutable.value = mutable.value.copy(submissionLocked = true)
            when (val result = creator.recover(command)) {
                TripCreationRecovery.Deleted -> mutable.value = mutable.value.copy(deleted = true,
                    error = "这次创建的旅行已被删除，不会重新创建。")
                is TripCreationRecovery.Committed -> mutable.value = mutable.value.copy(createdTripId = result.receipt.tripId)
                TripCreationRecovery.NotSubmitted -> {
                    selected = command.items; sources = command.sources; dateEdited = true; nameEdited = true
                    mutable.value = TripDraftState(command.name, command.startDate, command.dayCount, command.travelMode,
                        confirming = true, restored = true, submissionLocked = true, selectionCount = selected.size,
                        warning = "上次未完成创建，请确认后重试；不会自动重新处理攻略。")
                }
            }
        } catch (_: Exception) {
            mutable.value = mutable.value.copy(error = "无法核实上次创建结果，已保留恢复记录，请重试。",
                restored = true, submissionLocked = true)
        } finally { mutable.value = mutable.value.copy(recovering = false) }
    }
    fun retryRecovery() { if (!state.value.saving && !state.value.recovering) { mutable.value = mutable.value.copy(recovering = true, error = null); recover() } }
    fun retryMetadata(canApply: () -> Boolean) {
        metadataRequest?.let { metadata(it.userText, canApply, retry = true) }
    }

    fun metadata(input: String, canApply: () -> Boolean, retry: Boolean = false) {
        if (mutable.value.saving || pending != null || (!retry && metadataRequest?.userText == input)) return
        cancelMetadata()
        val request = if (retry) metadataRequest ?: return else {
            val zone = ZoneId.systemDefault()
            TripDraftRequest(input, LocalDate.now(zone), zone.id).also { metadataRequest = it }
        }
        val generation = revision
        mutable.value = mutable.value.copy(metadataBusy = true, warning = null, metadataRetryAvailable = false)
        metadataJob = viewModelScope.launch {
            try {
                val result = interpret(request)
                if (generation == revision && canApply() && !dateEdited)
                    mutable.value = mutable.value.copy(startDate = result.dates.startDate, dayCount = result.dates.dayCount, warning = result.dates.warning)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (generation == revision && canApply()) mutable.value = mutable.value.copy(
                    warning = "旅行日期未能确认，可单独重试、手动设置或不设日期继续。", metadataRetryAvailable = true)
            } finally { if (generation == revision) mutable.value = mutable.value.copy(metadataBusy = false) }
        }
    }
    fun cancelMetadata() { revision++; metadataJob?.cancel(); mutable.value = mutable.value.copy(metadataBusy = false) }
    fun editName(value: String) { if (canEdit()) { nameEdited = true; mutable.value = mutable.value.copy(name = value.take(100), error = null) } }
    fun editDates(value: LocalDate?, days: Int = mutable.value.dayCount) {
        if (canEdit() && days in 1..30) { dateEdited = true; mutable.value = mutable.value.copy(startDate = value, dayCount = days, warning = null, error = null) }
    }
    fun editDays(value: Int) { if (canEdit() && value in 1..30) { dateEdited = true; mutable.value = mutable.value.copy(dayCount = value, error = null) } }
    fun editMode(value: TravelMode) { if (canEdit()) mutable.value = mutable.value.copy(travelMode = value, error = null) }
    private fun canEdit() = !mutable.value.saving && !mutable.value.recovering &&
        !mutable.value.submissionLocked && pending == null
    fun selectionIsCurrent(items: List<ImportItem>) =
        selected.isNotEmpty() && selected.sortedBy { it.itemId } == items.sortedBy { it.itemId }
    fun recoveredSelection(): List<ImportItem> = if (mutable.value.restored) selected.toList() else emptyList()
    fun confirm(items: List<ImportItem>, evidence: List<String>) {
        if (items.isEmpty() || !canEdit()) return
        selected = items.toList(); sources = evidence.distinct().take(60).map { it.take(500) }
        val suggested = items.mapNotNull { it.poi.cityName }.filter { it.isNotBlank() }.distinct().joinToString("·")
            .take(80).ifBlank { "新的旅行" }.let { if (it == "新的旅行") it else "$it 旅行" }
        mutable.value = mutable.value.copy(confirming = true, selectionCount = items.size,
            name = if (nameEdited) mutable.value.name else suggested, error = null)
    }
    fun backToCandidates() {
        if (pending == null && !mutable.value.saving) mutable.value = mutable.value.copy(confirming = false)
    }
    fun submit(isValidSession: () -> Boolean, lock: (Boolean) -> Unit) {
        val s = mutable.value
        if (s.saving || s.recovering || !s.confirming || s.deleted) return
        if (!isValidSession()) {
            mutable.value = s.copy(error = "配置、地图授权或地点选择已变化，请恢复授权并返回核对地点后重试。")
            return
        }
        val command = try { pending ?: TripCreationCommand.create(s.name, s.startDate, s.dayCount, s.travelMode, selected, sources) }
        catch (e: IllegalArgumentException) { mutable.value = s.copy(error = e.message ?: "请检查旅行信息"); return }
        mutable.value = s.copy(saving = true, error = null)
        lock(true)
        viewModelScope.launch {
            try {
                withContext(io) { journal.write(command) }
                pending = command
                mutable.value = mutable.value.copy(submissionLocked = true)
                if (!isValidSession()) { mutable.value = mutable.value.copy(error = "会话或地图授权已变化，请核对后重试。"); return@launch }
                val receipt = creator.commit(command)
                mutable.value = mutable.value.copy(createdTripId = receipt.tripId)
            } catch (_: Exception) {
                // Reconcile even if the coroutine was canceled immediately after a durable commit.
                withContext(NonCancellable) {
                    when (val result = runCatching { creator.recover(command) }.getOrNull()) {
                        is TripCreationRecovery.Committed -> mutable.value = mutable.value.copy(createdTripId = result.receipt.tripId)
                        TripCreationRecovery.Deleted -> mutable.value = mutable.value.copy(deleted = true, error = "旅行已被删除，不会重新创建。")
                        else -> mutable.value = mutable.value.copy(error = "创建结果尚未确认，已保留内容；重试会先核对回执。")
                    }
                }
            } finally { mutable.value = mutable.value.copy(saving = false); lock(false) }
        }
    }
    /** Called after success is observed by the page, before navigation; never clears an unknown result. */
    suspend fun acknowledge() {
        val command = pending ?: return
        when (creator.recover(command)) {
            is TripCreationRecovery.Committed, TripCreationRecovery.Deleted -> withContext(io) { journal.clear(command) }
            TripCreationRecovery.NotSubmitted -> error("尚未完成创建")
        }
        pending = null
    }
    fun resumeEditing() {
        val command = pending ?: return
        if (mutable.value.saving || mutable.value.recovering) return
        mutable.value = mutable.value.copy(recovering = true)
        viewModelScope.launch {
            try {
                when (val result = creator.recover(command)) {
                    TripCreationRecovery.NotSubmitted -> {
                        withContext(io) { journal.clear(command) }
                        pending = null
                        mutable.value = mutable.value.copy(restored = true, submissionLocked = false, error = null)
                    }
                    is TripCreationRecovery.Committed -> mutable.value = mutable.value.copy(createdTripId = result.receipt.tripId)
                    TripCreationRecovery.Deleted -> mutable.value = mutable.value.copy(deleted = true,
                        error = "这次创建的旅行已被删除，不会重新创建。")
                }
            } catch (_: Exception) { mutable.value = mutable.value.copy(error = "尚未核实创建状态，暂不能修改，请重试。") }
            finally { mutable.value = mutable.value.copy(recovering = false) }
        }
    }
    fun reset() {
        if (pending != null || mutable.value.saving || mutable.value.submissionLocked) return
        cancelMetadata(); metadataRequest = null; dateEdited = false; nameEdited = false
        selected = emptyList(); sources = emptyList(); mutable.value = TripDraftState(recovering = false)
    }
    class Factory(private val creator: TripCreation, private val journal: PendingTripCreation,
        private val interpret: suspend (TripDraftRequest) -> TripDraftMetadata) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TripDraftViewModel(creator, journal, interpret = interpret) as T
    }
}
