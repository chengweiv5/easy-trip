package com.yangchengwei.easytrip.assistant.cityfirst

import android.net.Uri
import android.util.AtomicFile
import androidx.lifecycle.*
import com.yangchengwei.easytrip.assistant.*
import com.yangchengwei.easytrip.core.model.TravelMode
import com.yangchengwei.easytrip.trip.domain.isTripDateRangeRepresentable
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GuideCreationUiState(
    val input: String = "", val images: List<AssistantImage> = emptyList(), val cityText: String = "",
    val name: String = "", val startDate: LocalDate? = null, val dayCount: Int = 1,
    val travelMode: TravelMode = TravelMode.FLEXIBLE, val confirming: Boolean = false,
    val busy: Boolean = false, val saving: Boolean = false, val recovering: Boolean = true,
    val error: String? = null, val warning: String? = null, val createdTripId: String? = null,
    val deleted: Boolean = false, val locked: Boolean = false, val recoveryFailed: Boolean = false,
) {
    val hasDraft get() = input.isNotBlank() || images.isNotEmpty() || confirming
    val canCreate get() = confirming && !busy && !saving && !recovering && !recoveryFailed && !deleted && createdTripId == null &&
        name.trim().length in 1..100 && name.none(Char::isISOControl) &&
        runCatching { AssistantCityScope.parse(cityText) }.isSuccess &&
        dayCount in 1..30 && isTripDateRangeRepresentable(startDate, dayCount)
}

/** New journal namespace. Old TripCreationJournal is left intact for its original recovery route. */
class GuideCreationJournal(private val file: File) {
    @Synchronized fun read(): String? = try { readGuideFile(file) } catch (_: java.io.FileNotFoundException) { null }
    @Synchronized fun write(text: String) {
        val incoming = Json.parseToJsonElement(text).jsonObject
        require(incoming.getValue("version").jsonPrimitive.int == 1 && validGuideId(incoming.string("session")))
        read()?.let { existing ->
            val current = Json.parseToJsonElement(existing).jsonObject
            require(current.getValue("version").jsonPrimitive.int == 1 &&
                current.string("session") == incoming.string("session")) { "已有未完成草稿，不能覆盖" }
        }
        writeGuideFile(file, text)
    }
    @Synchronized fun clear(sessionId: String) {
        val current = read() ?: return
        require(Json.parseToJsonElement(current).jsonObject.string("session") == sessionId)
        AtomicFile(file).delete()
    }
}

class GuideCreationViewModel(
    val materials: GuideMaterialStore,
    val config: AssistantConfigStore,
    private val creator: GuideTripCreation,
    private val journal: GuideCreationJournal,
    private val destination: suspend (GuideMaterials) -> GuideDestinationResult,
    private val dates: suspend (TripDraftRequest) -> TripDraftMetadata,
) : ViewModel() {
    private val mutable = MutableStateFlow(GuideCreationUiState())
    val state = mutable.asStateFlow()
    var sessionId: String = UUID.randomUUID().toString(); private set
    private var reference = LocalDate.now()
    private var zone = ZoneId.systemDefault().id
    private var revision = 0L
    private var material: GuideMaterials? = null
    private var metadataReady = false
    private var pending: CreateGuideTripCommand? = null
    private var job: Job? = null
    private var acknowledged = false
    private val journalLock = Mutex()
    init { recover() }
    private fun snapshot() = buildJsonObject {
        val s = mutable.value
        put("version", 1); put("session", sessionId); put("reference", reference.toString()); put("zone", zone)
        put("input", s.input); put("images", encodeGuideImages(s.images))
        put("cities", s.cityText); put("name", s.name); put("date", s.startDate?.toString())
        put("days", s.dayCount); put("mode", s.travelMode.name); put("confirming", s.confirming)
        put("metadataReady", metadataReady)
        put("materialId", material?.ref?.id); put("materialDigest", material?.ref?.digest); put("pending", pending?.encode())
    }.toString()
    private fun persist() { viewModelScope.launch { persistNow() } }
    private suspend fun persistNow(): Boolean = journalLock.withLock {
        if (mutable.value.recoveryFailed || acknowledged) return@withLock false
        val record = snapshot()
        try { withContext(Dispatchers.IO) { journal.write(record) }; true }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            mutable.value = mutable.value.copy(error = "草稿保存失败，输入仍保留；请重试后再创建。")
            false
        }
    }
    private fun recover() = viewModelScope.launch {
        mutable.value = mutable.value.copy(recovering = true, recoveryFailed = false)
        try {
            val raw = withContext(Dispatchers.IO) { journal.read() } ?: return@launch
            val r = Json.parseToJsonElement(raw).jsonObject
            require(r.getValue("version").jsonPrimitive.int == 1)
            sessionId = r.string("session"); require(validGuideId(sessionId))
            reference = LocalDate.parse(r.string("reference")); zone = ZoneId.of(r.string("zone")).id
            pending = r["pending"]?.jsonPrimitive?.contentOrNull?.let(CreateGuideTripCommand::decode)
            metadataReady = r["metadataReady"]?.jsonPrimitive?.booleanOrNull
                ?: r.getValue("confirming").jsonPrimitive.boolean
            mutable.value = GuideCreationUiState(input = r.string("input"), images = decodeGuideImages(r.getValue("images")),
                cityText = r.string("cities"), name = r.string("name"),
                startDate = r["date"]?.jsonPrimitive?.contentOrNull?.let(LocalDate::parse),
                dayCount = r.getValue("days").jsonPrimitive.int, travelMode = TravelMode.valueOf(r.string("mode")),
                confirming = r.getValue("confirming").jsonPrimitive.boolean, recovering = true, locked = pending != null)
            require(mutable.value.input.length <= 20_000 && mutable.value.cityText.length <= 1500 &&
                mutable.value.name.length <= 100 && mutable.value.dayCount in 1..30 &&
                isTripDateRangeRepresentable(mutable.value.startDate, mutable.value.dayCount))
            if (pending != null) {
                applyRecovery(creator.recover(requireNotNull(pending)))
            }
            if (mutable.value.createdTripId == null && !mutable.value.deleted) {
                try {
                    r["materialDigest"]?.jsonPrimitive?.contentOrNull?.let { material = materials.read(GuideMaterialRef(r["materialId"]?.jsonPrimitive?.contentOrNull ?: sessionId, it)) }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) {
                    material = null
                    metadataReady = false
                    if (mutable.value.locked) throw IllegalStateException("尚未核对创建结果")
                    pending = null
                    mutable.value = mutable.value.copy(confirming = false,
                        error = "图片或攻略素材已失效，原文字已保留；请重新选择图片或重新识别。")
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.value = mutable.value.copy(locked = true, recoveryFailed = true,
            error = "上次草稿暂不可恢复，原记录已保留，未覆盖或重新创建。请重试恢复。") }
        finally { mutable.value = mutable.value.copy(recovering = false) }
    }
    fun retryRecovery() { if (!mutable.value.recovering && !mutable.value.saving) recover() }
    private fun applyRecovery(r: GuideCreationRecovery) {
        when (r) {
            GuideCreationRecovery.NotSubmitted -> mutable.value = mutable.value.copy(locked = false)
            GuideCreationRecovery.Deleted -> mutable.value = mutable.value.copy(deleted = true, locked = true, error = "这次创建的旅行已被删除，不会重新创建。")
            is GuideCreationRecovery.Committed -> mutable.value = mutable.value.copy(createdTripId = r.receipt.tripId, locked = true)
        }
    }
    private fun editable() = !mutable.value.busy && !mutable.value.saving && !mutable.value.recovering && !mutable.value.locked
    fun editInput(value: String) {
        if (!editable() || value.length > 20_000) return
        revision++; material = null; metadataReady = false; pending = null
        mutable.value = mutable.value.copy(input = value, confirming = false, cityText = "", name = "", error = null)
        persist()
    }
    fun editCities(value: String) = edit { copy(cityText = value.take(1500)) }
    fun editName(value: String) = edit { copy(name = value.take(100)) }
    fun editDays(value: Int) { if (value in 1..30) edit { copy(dayCount = value) } }
    fun editDates(start: LocalDate?, days: Int = mutable.value.dayCount) {
        if (days in 1..30 && isTripDateRangeRepresentable(start, days)) edit { copy(startDate = start, dayCount = days, warning = null) }
    }
    fun editMode(value: TravelMode) = edit { copy(travelMode = value) }
    private fun edit(transform: GuideCreationUiState.() -> GuideCreationUiState) {
        if (!editable()) return
        revision++; pending = null; mutable.value = mutable.value.transform().copy(error = null); persist()
    }
    fun selectImages(uris: List<Uri>) {
        if (!editable()) return
        val generation = ++revision
        mutable.value = mutable.value.copy(busy = true)
        job = viewModelScope.launch {
            var accepted: List<AssistantImage> = emptyList()
            try {
                val s = mutable.value
                val result = materials.images.select(sessionId, uris, s.images)
                accepted = result.accepted
                if (generation != revision) {
                    withContext(NonCancellable) { accepted.forEach { materials.images.remove(sessionId, it.id) } }
                    return@launch
                }
                material = null
                metadataReady = false
                pending = null
                mutable.value = s.copy(images = s.images + result.accepted, confirming = false,
                    error = result.rejected.takeIf { it.isNotEmpty() }?.joinToString("\n"))
                persist()
            } catch (e: CancellationException) {
                withContext(NonCancellable) { accepted.forEach { materials.images.remove(sessionId, it.id) } }
                throw e
            }
            catch (_: Exception) { mutable.value = mutable.value.copy(error = "图片读取失败，已保留输入。") }
            finally { if (generation == revision) mutable.value = mutable.value.copy(busy = false) }
        }
    }
    fun removeImage(id: String) {
        if (!editable()) return
        material = null; metadataReady = false; pending = null; revision++
        mutable.value = mutable.value.copy(images = mutable.value.images.filterNot { it.id == id }, confirming = false)
        persist()
        viewModelScope.launch { materials.images.remove(sessionId, id) }
    }
    fun identify() {
        if (!editable()) return
        val s = mutable.value
        if (s.input.isBlank() && s.images.isEmpty()) return
        if (metadataReady && material != null) {
            mutable.value = s.copy(confirming = true, error = null)
            persist()
            return
        }
        if (config.read() == null) { mutable.value = s.copy(error = "请先配置助手模型。"); return }
        val generation = ++revision
        mutable.value = s.copy(busy = true, error = null)
        job = viewModelScope.launch {
            try {
                if (!persistNow()) return@launch
                withTimeout(AssistantImagePolicy.batchTimeoutMillis(20)) {
                    val prepared = material ?: materials.prepare(sessionId, s.input, s.images) {}
                    if (generation != revision) return@withTimeout
                    material = prepared
                    val result = destination(prepared)
                    if (generation != revision) return@withTimeout
                    material = result.material ?: prepared
                    val dateResult = try {
                        if (prepared.userIntent.isBlank()) TripDraftMetadata(TripDateSuggestion(null))
                        else dates(TripDraftRequest(prepared.userIntent, reference, zone))
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { TripDraftMetadata(TripDateSuggestion(null, warning = "日期未能识别，可手动填写或保持未定。")) }
                    if (generation != revision) return@withTimeout
                    val cityText = result.cities.joinToString("、") { it.name }
                    metadataReady = true
                    mutable.value = mutable.value.copy(cityText = cityText,
                        name = result.suggestedName.ifBlank { cityText.takeIf(String::isNotBlank)?.plus("之旅").orEmpty() },
                        startDate = dateResult.dates.startDate, dayCount = dateResult.dates.dayCount,
                        warning = dateResult.dates.warning, confirming = true,
                        error = if (cityText.isBlank()) "未能确定目的地，请填写城市后创建。" else null)
                    persistNow()
                }
            } catch (_: TimeoutCancellationException) {
                mutable.value = mutable.value.copy(error = "识别超时，原攻略已保留；可以重试。")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = mutable.value.copy(error = (e as? AssistantFailure)?.userMessage ?: "目的地识别失败，原攻略已保留，请重试。") }
            finally { if (generation == revision) mutable.value = mutable.value.copy(busy = false) }
        }
    }
    fun cancel() { if (!mutable.value.saving) { revision++; job?.cancel(); mutable.value = mutable.value.copy(busy = false); persist() } }
    fun backToInput() { if (editable()) { mutable.value = mutable.value.copy(confirming = false); persist() } }
    fun create() {
        val s = mutable.value
        if (!s.canCreate) return
        val command = pending ?: material?.let {
            CreateGuideTripCommand.create(s.name, s.startDate, s.dayCount, s.travelMode,
                AssistantCityScope.parse(s.cityText), it.ref)
        } ?: run { mutable.value = s.copy(error = "攻略素材已失效，请返回重新识别。"); return }
        pending = command
        mutable.value = s.copy(saving = true, locked = true, error = null)
        viewModelScope.launch {
            try {
                if (!persistNow()) {
                    mutable.value = mutable.value.copy(locked = s.locked)
                    return@launch
                }
                when (val r = creator.recover(command)) {
                    GuideCreationRecovery.NotSubmitted -> {
                        val receipt = materials.withRetained(command.material) { creator.commit(command) }
                        applyRecovery(GuideCreationRecovery.Committed(receipt))
                    }
                    else -> applyRecovery(r)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                withContext(NonCancellable) {
                    val recovery = runCatching { creator.recover(command) }.getOrNull()
                    if (recovery != null) applyRecovery(recovery)
                    if (mutable.value.createdTripId == null) mutable.value = mutable.value.copy(error = "创建尚未完成，信息与攻略已保留；重试会先核对回执。")
                }
            } finally { mutable.value = mutable.value.copy(saving = false); persistNow() }
        }
    }
    suspend fun acknowledge() {
        withContext(NonCancellable) { journalLock.withLock {
            withContext(Dispatchers.IO) { journal.clear(sessionId) }
            acknowledged = true
        } }
    }
    suspend fun discard() {
        check(!mutable.value.saving)
        if (pending != null && creator.recover(requireNotNull(pending)) != GuideCreationRecovery.NotSubmitted) {
            acknowledge(); return
        }
        acknowledge(); materials.release(sessionId)
    }
    class Factory(private val create: () -> GuideCreationViewModel) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }
}
