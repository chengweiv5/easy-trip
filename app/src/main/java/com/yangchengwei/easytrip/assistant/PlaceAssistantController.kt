package com.yangchengwei.easytrip.assistant

import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.place.domain.PlaceCategory
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** One controller per trip back-stack entry. Model text can never call confirm(). */
class PlaceAssistantController(
    private val tripId: String,
    private val scope: CoroutineScope,
    private val parser: PlaceIntentParser,
    source: PlaceSearchDataSource?,
    private val importer: PlaceImport,
    private var resolveDefaultCity: suspend () -> String? = { null },
    private val processor: (() -> AssistantInputProcessor)? = null,
    private val sessionId: String = "",
    private val imageAllowed: () -> Boolean = { true },
) {
    private val mutable = MutableStateFlow(PlaceAssistantState())
    val state = mutable.asStateFlow()
    private var source = source
    private var run = 0L
    private var work: Job? = null
    private var savedIds: Set<String> = emptySet()
    private var active = true
    private var cityRevision = 0L
    private var cityEdited = false
    private var parsedSources = emptyList<ParsedSource>()
    private var inputRevision = 0L
    private var currentParsed = emptyList<ParsedSource>()
    val draftRevision get() = inputRevision
    fun attachmentSelection(busy: Boolean) { mutable.value = mutable.value.copy(attachmentSelectionBusy = busy) }
    fun reportError(message: String) { fail(message) }
    fun addImages(images: List<AssistantImage>) {
        val s = mutable.value
        if (!active || s.busy || s.saving || s.items.isNotEmpty()) return
        if (s.images.size + images.size > 6 || (s.images + images).sumOf { it.byteCount } > AssistantImagePolicy.MAX_TOTAL_BYTES) {
            fail("图片超过本批数量或大小上限。"); return
        }
        parsedSources = emptyList()
        currentParsed = emptyList()
        mutable.value = s.copy(images = s.images + images, sourceProgress = emptyList(), error = null)
    }
    fun removeImage(id: String) {
        if (mutable.value.busy || mutable.value.saving || mutable.value.items.isNotEmpty()) return
        parsedSources = emptyList()
        mutable.value = mutable.value.copy(images = mutable.value.images.filterNot { it.id == id }, sourceProgress = emptyList())
    }

    fun environment(configured: Boolean, consent: Boolean, mapReady: Boolean) {
        val old = mutable.value
        if (old.consent && !consent) {
            cancel()
            mutable.value = mutable.value.copy(items = emptyList(), selected = emptySet(), confirmation = null,
                error = "地图授权已撤回，请重新授权后查询。")
        }
        if (old.configured && !configured) { cancel(); mutable.value = mutable.value.copy(confirmation = null) }
        mutable.value = mutable.value.copy(configured = configured, consent = consent, mapReady = mapReady,
            confirmation = if (!mapReady) null else mutable.value.confirmation)
    }
    fun updateSource(value: PlaceSearchDataSource?) { source = value }
    fun updateCityResolver(value: suspend () -> String?) { resolveDefaultCity = value }
    /** Opening the panel resolves only trip context, never sends user input to the model. */
    suspend fun prepareDefaultCity() {
        val s = mutable.value
        if (!active || !s.consent || s.busy || s.saving || s.resolvingCity ||
            s.city.isNotBlank() || cityEdited || s.items.isNotEmpty()) return
        val revision = ++cityRevision
        mutable.value = s.copy(resolvingCity = true)
        try {
            val city = lookupDefaultCity()
            currentCoroutineContext().ensureActive()
            if (revision == cityRevision && active && mutable.value.consent && !cityEdited) {
                mutable.value = mutable.value.copy(city = city)
            }
        } finally {
            if (revision == cityRevision) mutable.value = mutable.value.copy(resolvingCity = false)
        }
    }

    private suspend fun lookupDefaultCity(): String = try {
        withTimeoutOrNull(12_000) { resolveDefaultCity() }?.trim().orEmpty().take(50)
            .takeUnless { it.any(Char::isISOControl) }.orEmpty()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        ""
    }
    fun configurationChanged() { cancel(); inputRevision++; mutable.value = mutable.value.copy(confirmation = null) }
    fun invalidateTrip() {
        cancel(); inputRevision++; active = false
        mutable.value = mutable.value.copy(items = emptyList(), selected = emptySet(), confirmation = null, error = "目标旅行已失效，请返回旅行列表。")
    }
    fun updateSaved(ids: Set<String>) { savedIds = ids; reconcile() }
    fun editInput(value: String) {
        if (mutable.value.saving || mutable.value.busy || mutable.value.items.isNotEmpty()) return
        if (value.length > 2000) { fail("文字最多 2000 字，请分批输入；原输入保留。"); return }
        parsedSources = emptyList()
        mutable.value = mutable.value.copy(input = value, sourceProgress = emptyList(), error = null)
    }
    fun editCity(value: String) {
        if (mutable.value.saving) return
        cityEdited = true
        cityRevision++
        mutable.value = mutable.value.copy(resolvingCity = false)
        val next = value.take(50)
        if (next == mutable.value.city) return
        cancel()
        inputRevision++
        val affected = mutable.value.items.filter { it.intent.usesDefaultCity || it.intent.city.isBlank() }.map { it.id }.toSet()
        mutable.value = mutable.value.copy(city = next, confirmation = null,
            selected = mutable.value.selected - affected,
            items = mutable.value.items.map {
                if (it.id in affected) it.copy(intent = it.intent.copy(city = next, usesDefaultCity = true),
                    generation = it.generation + 1, status = IntakeStatus.CANCELLED, candidates = emptyList(), poi = null)
                else it
            })
    }
    fun clear() {
        if (mutable.value.saving) return
        cancel()
        inputRevision++
        parsedSources = emptyList()
        val old = mutable.value
        mutable.value = PlaceAssistantState(city = old.city, configured = old.configured,
            consent = old.consent, mapReady = old.mapReady)
    }
    fun supplyMissingCity(value: String) {
        val city = value.trim()
        val s = mutable.value
        if (s.busy || s.saving || city.isBlank() || city.length > 50 || city.any(Char::isISOControl) || !canQuery()) return
        val missing = s.items.filter { it.status == IntakeStatus.NEED_CITY && it.intent.city.isBlank() }.map { it.id }.toSet()
        if (missing.isEmpty()) return
        mutable.value = s.copy(city = s.city.ifBlank { city }, confirmation = null,
            items = s.items.map {
                if (it.id in missing) it.copy(intent = it.intent.copy(city = city, usesDefaultCity = true),
                    generation = it.generation + 1, status = IntakeStatus.CANCELLED, detail = null, cityConflict = false)
                else it
            })
        retry(missing)
    }
    fun dismissError() { mutable.value = mutable.value.copy(error = null) }
    suspend fun recoverReceipt() {
        val receipt = importer.latestReceipt(tripId)
        if (!mutable.value.hasDraft && !mutable.value.saving && receipt != null) {
            mutable.value = mutable.value.copy(receipt = receipt)
        }
    }
    fun dismissReceipt() { mutable.value = mutable.value.copy(receipt = null) }

    fun submit() {
        val s = mutable.value
        if (!canQuery() || s.busy || s.resolvingCity || s.attachmentSelectionBusy || s.items.isNotEmpty()) return
        if (s.input.isBlank() && s.images.isEmpty()) { fail("请先输入地点文字或选择图片。"); return }
        if (s.images.isNotEmpty()) { processImages(null); return }
        val input = s.input
        val generation = ++run
        mutable.value = s.copy(busy = true, error = null, receipt = null)
        work = scope.launch {
            try {
                withTimeout(90_000) {
                    val city = resolveSearchCity(s.city)
                    if (!isCurrent(generation)) return@withTimeout
                    mutable.value = mutable.value.copy(city = city)
                    val intents = parser.parse(input, city)
                    if (!isCurrent(generation)) return@withTimeout
                    require(intents.size in 1..20)
                    val items = intents.mapIndexed { index, intent ->
                        val default = intent.city.isBlank() || (city.isNotBlank() &&
                            sameCity(intent.city, city) && !input.contains(intent.city.removeSuffix("市")))
                        val conflict = city.isNotBlank() && intent.city.isNotBlank() && !sameCity(intent.city, city)
                        IntakeItem(UUID.randomUUID().toString(), ('A' + index).toString(),
                            intent.copy(city = intent.city.ifBlank { city }, usesDefaultCity = default),
                            cityConflict = conflict)
                    }
                    mutable.value = mutable.value.copy(items = items)
                    query(items.map { it.id }.toSet(), generation)
                }
            } catch (_: TimeoutCancellationException) { if (isCurrent(generation)) stopPending("整批查询已超时，已完成结果保留。") }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (isCurrent(generation)) fail(e.safeMessage()) }
            finally { if (isCurrent(generation)) mutable.value = mutable.value.copy(busy = false) }
        }
    }
    fun retryImages(ids: Set<String> = mutable.value.sourceProgress.filter {
        it.stage == SourceStage.FAILED || it.stage == SourceStage.CANCELLED
    }.map { it.sourceId }.toSet()) {
        if (!canQuery() || mutable.value.busy || mutable.value.attachmentSelectionBusy || ids.isEmpty()) return
        val retryable = mutable.value.sourceProgress.filter { it.stage in setOf(SourceStage.FAILED, SourceStage.CANCELLED) }
            .map { it.sourceId }.toSet()
        val allowed = ids.intersect(retryable)
        if (allowed.isNotEmpty()) processImages(allowed)
    }
    private fun processImages(retryIds: Set<String>?) {
        val s = mutable.value
        if (!imageAllowed()) { fail("当前模型的图片能力检查未通过，图片尚未发送。请在助手设置重新检查，素材已保留。"); return }
        val runner = processor?.invoke() ?: run { fail("图片处理暂不可用，请重新打开助手。"); return }
        val generation = ++run
        val input = AssistantInputRequest(sessionId, s.input, s.city.trim(), s.images, retryIds)
        val sourceIds = (if (s.input.isBlank()) emptyList() else listOf("text")) + s.images.map { it.id }
        if (retryIds == null) parsedSources = emptyList()
        currentParsed = emptyList()
        mutable.value = s.copy(busy = true, error = null, receipt = null,
            sourceProgress = sourceIds.map { id ->
                if (retryIds == null || id in retryIds) SourceProgress(id, SourceStage.SELECTED)
                else s.sourceProgress.find { it.sourceId == id } ?: SourceProgress(id, SourceStage.COMPLETE)
            })
        work = scope.launch {
            try {
                withTimeout(540_000) {
                    val request = input.copy(city = resolveSearchCity(input.city))
                    if (!isCurrent(generation)) return@withTimeout
                    mutable.value = mutable.value.copy(city = request.city)
                    val batch = runner.parse(request, onSource = { parsed ->
                        if (isCurrent(generation)) currentParsed = currentParsed + parsed
                    }) { progress ->
                        // OkHttp's body callback is not on the controller dispatcher.
                        scope.launch {
                            if (isCurrent(generation)) {
                                val old = mutable.value.sourceProgress.find { it.sourceId == progress.sourceId }
                                if (old?.stage !in setOf(SourceStage.COMPLETE, SourceStage.FAILED, SourceStage.CANCELLED))
                                    mutable.value = mutable.value.copy(sourceProgress = mutable.value.sourceProgress.map {
                                        if (it.sourceId == progress.sourceId) progress else it
                                    })
                            }
                        }
                    }
                    if (!isCurrent(generation)) return@withTimeout
                    val merged = parsedSources.filterNot { old -> batch.sources.any { it.sourceId == old.sourceId } } + batch.sources
                    if (batch.overflow || merged.sumOf { it.intents.size } > 20) {
                        fail("每批最多 20 个地点，请保留素材重新分批；没有查询或截断新增内容。")
                        mutable.value = mutable.value.copy(sourceProgress = mutable.value.sourceProgress.map {
                            if (retryIds == null || it.sourceId in retryIds) it.copy(stage = SourceStage.FAILED, error = "地点数量超限，请分批") else it
                        })
                        return@withTimeout
                    }
                    parsedSources = merged
                    currentParsed = emptyList()
                    mutable.value = mutable.value.copy(sourceProgress = mutable.value.sourceProgress.map { progress ->
                        batch.sources.find { it.sourceId == progress.sourceId }?.let { parsed ->
                            progress.copy(stage = if (parsed.failure == null) SourceStage.COMPLETE else SourceStage.FAILED,
                                error = parsed.failure?.userMessage, sendEvidence = parsed.failure?.sendEvidence ?: SendEvidence.RESPONSE_RECEIVED)
                        } ?: progress
                    })
                    val fresh = batch.sources.filter { it.failure == null }.flatMap { it.intents }
                    val items = fresh.mapIndexed { index, intent ->
                        val city = request.city
                        IntakeItem(UUID.randomUUID().toString(), ('A' + s.items.size + index).toString(),
                            intent.copy(city = intent.city.ifBlank { city }, usesDefaultCity = intent.city.isBlank() ||
                                (sameCity(intent.city, city) && !intent.sourceSpan.contains(intent.city.removeSuffix("市")))),
                            cityConflict = city.isNotBlank() && intent.city.isNotBlank() && !sameCity(intent.city, city))
                    }
                    mutable.value = mutable.value.copy(items = mutable.value.items + items)
                    if (mutable.value.items.isEmpty()) fail("未识别到明确地点，请换清晰图片或补充文字；素材已保留。")
                    query(items.map { it.id }.toSet(), generation)
                }
            } catch (_: TimeoutCancellationException) {
                if (isCurrent(generation)) { cancel(); fail("整批处理超时，已完成结果保留；已发送内容无法撤回。") }
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (isCurrent(generation)) fail(e.safeMessage()) }
            finally {
                if (isCurrent(generation)) {
                    stopPending(null)
                    mutable.value = mutable.value.copy(busy = false, sourceProgress = mutable.value.sourceProgress.map {
                        if (it.stage in setOf(SourceStage.SELECTED, SourceStage.PREPARING, SourceStage.SENDING, SourceStage.UNDERSTANDING))
                            it.copy(stage = SourceStage.CANCELLED) else it
                    })
                }
            }
        }
    }
    private suspend fun resolveSearchCity(value: String): String =
        value.trim().ifBlank { lookupDefaultCity() }.take(50)

    fun cancel() {
        // A stopped batch must retain completed image extraction without querying an unchecked prefix.
        if (currentParsed.isNotEmpty()) {
            val merged = parsedSources.filterNot { old -> currentParsed.any { it.sourceId == old.sourceId } } + currentParsed
            if (merged.none { it.failure?.code == AssistantFailureCode.OVERFLOW } && merged.sumOf { it.intents.size } <= 20) {
                val represented = mutable.value.items.flatMap { it.intent.sources }.mapNotNull {
                    when (it) { is IntakeSource.Image -> it.imageId; is IntakeSource.Text -> "text" }
                }.toSet()
                val fresh = currentParsed.filter { it.failure == null && it.sourceId !in represented }.flatMap { it.intents }
                val s = mutable.value
                mutable.value = s.copy(items = s.items + fresh.mapIndexed { index, intent ->
                    IntakeItem(UUID.randomUUID().toString(), ('A' + s.items.size + index).toString(),
                        intent.copy(city = intent.city.ifBlank { s.city }, usesDefaultCity = intent.city.isBlank() ||
                            (sameCity(intent.city, s.city) && !intent.sourceSpan.contains(intent.city.removeSuffix("市")))),
                        status = IntakeStatus.CANCELLED, detail = "已理解，地图查询尚未开始；可继续此项。",
                        cityConflict = s.city.isNotBlank() && intent.city.isNotBlank() && !sameCity(intent.city, s.city))
                })
                parsedSources = merged
                mutable.value = mutable.value.copy(sourceProgress = mutable.value.sourceProgress.map { progress ->
                    currentParsed.find { it.sourceId == progress.sourceId }?.let { parsed ->
                        progress.copy(stage = if (parsed.failure == null) SourceStage.COMPLETE else SourceStage.FAILED,
                            error = parsed.failure?.userMessage, sendEvidence = parsed.failure?.sendEvidence ?: SendEvidence.RESPONSE_RECEIVED)
                    } ?: progress
                })
            }
            currentParsed = emptyList()
        }
        run++
        cityRevision++
        work?.cancel(); work = null
        stopPending(null)
        mutable.value = mutable.value.copy(busy = false, resolvingCity = false, sourceProgress = mutable.value.sourceProgress.map {
            if (it.stage in setOf(SourceStage.SELECTED, SourceStage.PREPARING, SourceStage.SENDING, SourceStage.UNDERSTANDING))
                it.copy(stage = SourceStage.CANCELLED) else it
        })
    }
    fun retry(ids: Set<String> = mutable.value.items.filter {
        it.status in setOf(IntakeStatus.FAILED, IntakeStatus.CANCELLED)
    }.map { it.id }.toSet()) {
        if (!canQuery() || mutable.value.busy || ids.isEmpty()) return
        val allowed = mutable.value.items.filter { it.id in ids && it.status !in setOf(IntakeStatus.READY, IntakeStatus.ALREADY_SAVED, IntakeStatus.DUPLICATE) }.map { it.id }.toSet()
        if (allowed.isEmpty()) return
        val generation = ++run
        mutable.value = mutable.value.copy(busy = true, error = null)
        work = scope.launch {
            try { withTimeout(90_000) { query(allowed, generation) } }
            catch (_: TimeoutCancellationException) { if (isCurrent(generation)) stopPending("查询超时，已完成结果保留。") }
            finally { if (isCurrent(generation)) mutable.value = mutable.value.copy(busy = false) }
        }
    }
    fun editItem(id: String, query: String, city: String) {
        if (mutable.value.saving || query.isBlank() || query.length > 100 || city.length > 50) return
        mutable.value = mutable.value.copy(confirmation = null, selected = mutable.value.selected - id)
        changeItem(id) { it.copy(intent = it.intent.copy(query = query.trim(), city = city.trim(), needsReview = false),
            generation = it.generation + 1, status = IntakeStatus.CANCELLED, candidates = emptyList(), poi = null, detail = null, cityConflict = false) }
    }
    fun confirmCity(id: String) {
        if (mutable.value.busy || mutable.value.saving) return
        if (mutable.value.items.find { it.id == id }?.intent?.needsReview == true) return
        changeItem(id) { it.copy(cityConflict = false) }
        retry(setOf(id))
    }
    fun choosePoi(id: String, poiId: String) {
        if (mutable.value.saving) return
        val item = mutable.value.items.find { it.id == id } ?: return
        val poi = item.candidates.find { it.poiId == poiId && it.validPosition() } ?: return
        mutable.value = mutable.value.copy(confirmation = null, selected = mutable.value.selected - id)
        changeItem(id) { it.copy(poi = poi, status = IntakeStatus.READY, generation = it.generation + 1, detail = null) }
        reconcile()
    }
    fun category(id: String, category: PlaceCategory) {
        if (mutable.value.saving) return
        changeItem(id) { it.copy(category = category) }
        mutable.value = mutable.value.copy(confirmation = null)
    }
    fun toggle(id: String) {
        if (mutable.value.saving || mutable.value.eligible.none { it.id == id }) return
        val selected = mutable.value.selected
        mutable.value = mutable.value.copy(selected = if (id in selected) selected - id else selected + id, confirmation = null)
    }
    fun selectAll() {
        if (mutable.value.saving) return
        val eligible = mutable.value.eligible.map { it.id }.toSet()
        mutable.value = mutable.value.copy(selected = if (mutable.value.selected == eligible) emptySet() else eligible, confirmation = null)
    }
    fun review(ids: Set<String> = mutable.value.selected) {
        if (!canSave() || mutable.value.saving) return
        val selected = mutable.value.eligible.filter { it.id in ids }
        if (selected.isEmpty() || selected.size != ids.size) return
        val items = selected.map { ImportItem(it.id, it.generation, requireNotNull(it.poi), it.category) }
        mutable.value = mutable.value.copy(selected = ids, confirmation = ImportConfirmation.create(tripId, items), error = null)
    }
    fun backFromReview() { if (!mutable.value.saving) mutable.value = mutable.value.copy(confirmation = null) }
    fun confirm() {
        val snapshot = mutable.value.confirmation ?: return
        if (!canSave() || mutable.value.saving) return
        val current = mutable.value.eligible.filter { it.id in mutable.value.selected }
            .map { ImportItem(it.id, it.generation, requireNotNull(it.poi), it.category) }
        if (current.isEmpty() || ImportConfirmation.create(tripId, current).digest != snapshot.digest) {
            mutable.value = mutable.value.copy(confirmation = null); fail("所选地点已变化，请重新核对。"); return
        }
        mutable.value = mutable.value.copy(saving = true, error = null)
        scope.launch {
            try {
                val receipt = importer.commit(snapshot)
                savedIds = savedIds + receipt.results.map { it.poiId }
                mutable.value = mutable.value.copy(receipt = receipt, confirmation = null, selected = emptySet())
                reconcile()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { fail("本次未能完成保存，请重试；同一确认会先回读，不重复收藏。") }
            finally { mutable.value = mutable.value.copy(saving = false) }
        }
    }
    private suspend fun query(ids: Set<String>, generation: Long) = coroutineScope {
        val semaphore = Semaphore(2)
        ids.map { id -> launch {
            semaphore.withPermit {
                val item = mutable.value.items.find { it.id == id } ?: return@withPermit
                if (!isCurrent(generation)) return@withPermit
                if (item.intent.needsReview) {
                    changeItem(id) { it.copy(status = IntakeStatus.NEED_CITY, detail = "图片中的名称不够明确，请对照来源核对名称和城市。") }
                    return@withPermit
                }
                if (item.cityConflict) {
                    changeItem(id) { it.copy(status = IntakeStatus.NEED_CITY, detail = "原文城市 ${item.intent.city} 与默认城市不同，请确认此项城市。") }
                    return@withPermit
                }
                if (item.intent.city.isBlank()) { changeItem(id) { it.copy(status = IntakeStatus.NEED_CITY, detail = "请补充搜索城市。") }; return@withPermit }
                changeItem(id) { it.copy(status = IntakeStatus.QUERYING, detail = null) }
                val result = try {
                    val dataSource = source ?: throw AssistantFailure("地图服务未授权。")
                    matchItem(item, withTimeout(15_000) { dataSource.search(item.intent.query, item.intent.city) })
                } catch (_: TimeoutCancellationException) { item.copy(status = IntakeStatus.FAILED, detail = "查询超时，可仅重试此项。") }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { item.copy(status = IntakeStatus.FAILED, detail = "地点查询失败，可仅重试此项。") }
                if (isCurrent(generation) && mutable.value.items.find { it.id == id }?.generation == item.generation) {
                    changeItem(id) { result }; reconcile()
                }
            }
        } }.joinAll()
    }
    private fun reconcile() {
        // Prefer the user's selected representative when an earlier query finishes late.
        val preferred = mutable.value.items.filter { it.id in mutable.value.selected && it.poi != null }
            .associate { requireNotNull(it.poi).poiId to it.id }
        val seen = mutableSetOf<String>()
        val allSources = mutable.value.items.filter { it.poi != null }.groupBy { it.poi!!.poiId }
            .mapValues { (_, items) -> items.flatMap { it.intent.sources }.distinct() }
        val items = mutable.value.items.map { item ->
            val poi = item.poi ?: return@map item
            val reconciled = when {
                poi.poiId in savedIds -> item.copy(status = IntakeStatus.ALREADY_SAVED, detail = "已在地点池，保留已有内容。")
                (preferred[poi.poiId]?.let { it != item.id } == true) || !seen.add(poi.poiId) -> item.copy(status = IntakeStatus.DUPLICATE, detail = "与本批另一项为同一地点，不重复收藏。")
                else -> item.copy(status = IntakeStatus.READY, detail = null)
            }
            reconciled.copy(sources = allSources[poi.poiId].orEmpty())
        }
        val selected = mutable.value.selected.intersect(items.filter { it.status == IntakeStatus.READY }.map { it.id }.toSet())
        mutable.value = mutable.value.copy(items = items, selected = selected,
            confirmation = if (selected != mutable.value.selected && !mutable.value.saving) null else mutable.value.confirmation)
    }
    private fun stopPending(message: String?) {
        mutable.value = mutable.value.copy(items = mutable.value.items.map {
            if (it.status == IntakeStatus.WAITING || it.status == IntakeStatus.QUERYING) it.copy(status = IntakeStatus.CANCELLED, detail = "已停止，可继续此项。") else it
        }, error = message ?: mutable.value.error)
    }
    private fun changeItem(id: String, transform: (IntakeItem) -> IntakeItem) {
        mutable.value = mutable.value.copy(items = mutable.value.items.map { if (it.id == id) transform(it) else it })
    }
    private fun canQuery(): Boolean {
        val s = mutable.value
        val error = when {
            !active -> "目标旅行已失效。"
            s.saving -> return false
            !s.configured -> "请先在助手设置配置模型。"
            !s.consent -> "请先授权地图服务。"
            else -> null
        }
        if (error != null) fail(error)
        return error == null
    }
    private fun canSave(): Boolean = active && mutable.value.mapReady && mutable.value.consent
    private fun isCurrent(value: Long) = active && value == run && mutable.value.consent
    private fun fail(message: String) { mutable.value = mutable.value.copy(error = message) }
    private fun Exception.safeMessage() = (this as? AssistantFailure)?.userMessage ?: "未能解析地点，原输入保留，请重试。"
}
