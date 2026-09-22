package com.yangchengwei.easytrip.itinerary.calendar

import com.yangchengwei.easytrip.itinerary.domain.ItineraryTimingChange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class CalendarSaveState(
    val saving: Boolean = false,
    val active: ItineraryTimingChange? = null,
    val message: String? = null,
    val undo: ItineraryTimingChange? = null,
    val retry: ItineraryTimingChange? = null,
    val retryIsUndo: Boolean = false,
)

/** No optimistic repository writes: failed drafts disappear; the observed snapshot stays authoritative. */
class CalendarTimingController(private val write: suspend (ItineraryTimingChange) -> ItineraryTimingChange?) {
    private val mutable = MutableStateFlow(CalendarSaveState())
    val state: StateFlow<CalendarSaveState> = mutable

    suspend fun commit(change: ItineraryTimingChange, isUndo: Boolean = false) {
        if (mutable.value.saving || change.before == change.after) return
        mutable.value = CalendarSaveState(saving = true, active = change)
        try {
            val applied = write(change)
            mutable.value = if (applied != null) CalendarSaveState(
                message = if (isUndo) "已撤销时间调整" else "已更新到达时间和停留时长",
                undo = if (isUndo) null else applied.reversed(),
            ) else CalendarSaveState(message = "地点已被移动、删除或改时，请查看最新行程")
        } catch (failure: CancellationException) {
            mutable.value = CalendarSaveState()
            throw failure
        } catch (_: Exception) {
            mutable.value = CalendarSaveState(message = "保存失败，已恢复原时间", retry = change, retryIsUndo = isUndo)
        }
    }
    suspend fun undo() { mutable.value.undo?.let { commit(it, isUndo = true) } }
    suspend fun retry() { val value = mutable.value; value.retry?.let { commit(it, value.retryIsUndo) } }
    fun dismiss() { if (!mutable.value.saving) mutable.value = CalendarSaveState() }
}
