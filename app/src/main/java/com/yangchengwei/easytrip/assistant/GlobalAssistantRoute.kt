package com.yangchengwei.easytrip.assistant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.place.amap.PlaceSearchDataSource
import com.yangchengwei.easytrip.workspace.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

@Composable
fun GlobalAssistantRoute(
    assistant: PlaceAssistantViewModel,
    draft: TripDraftViewModel,
    source: PlaceSearchDataSource?,
    token: AmapConsentToken?,
    onConsent: () -> Unit,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val state by assistant.controller.state.collectAsStateWithLifecycle()
    val form by draft.state.collectAsStateWithLifecycle()
    val focused by assistant.focus.collectAsStateWithLifecycle()
    val detail by assistant.detail.collectAsStateWithLifecycle()
    val configuration by assistant.config.revision.collectAsStateWithLifecycle()
    var settings by rememberSaveable { mutableStateOf(false) }
    var leavePrompt by rememberSaveable { mutableStateOf(false) }
    var mapReady by remember(token) { mutableStateOf(false) }
    var navigationError by remember { mutableStateOf<String?>(null) }
    var navigationAttempt by remember { mutableIntStateOf(0) }
    var confirmedConfiguration by rememberSaveable { mutableLongStateOf(configuration) }
    val consentSnapshot = token?.active?.collectAsStateWithLifecycle()?.value
    val consentActive = token?.isActive() == true
    val scope = rememberCoroutineScope()
    LaunchedEffect(source) { assistant.controller.updateSource(source) }
    LaunchedEffect(configuration, token, consentSnapshot) {
        draft.environment(configuration, token)
    }
    LaunchedEffect(configuration, token, consentSnapshot, mapReady) {
        assistant.controller.environment(assistant.config.read() != null, token?.isActive() == true, mapReady)
    }
    LaunchedEffect(form.restored, form.submissionLocked) {
        if (form.restored && form.confirming && !form.submissionLocked) {
            assistant.controller.restoreCreationSelection(draft.recoveredSelection())
            confirmedConfiguration = configuration
        }
    }
    LaunchedEffect(form.createdTripId, navigationAttempt) {
        form.createdTripId?.let { id ->
            try {
                draft.acknowledge()
                withContext(Dispatchers.Main.immediate) {
                    assistant.clearDraft()
                    onCreated(id)
                }
            } catch (canceled: CancellationException) { throw canceled }
            catch (_: Exception) {
                navigationError = "旅行已创建，但尚未完成打开；请重试，不会重复创建。"
            }
        }
    }
    val back = {
        when {
            form.saving -> Unit
            settings -> settings = false
            form.confirming && !form.submissionLocked -> draft.backToCandidates()
            state.hasDraft || form.submissionLocked -> leavePrompt = true
            else -> onBack()
        }
    }
    BackHandler(onBack = back)
    if (settings) {
        AssistantSettings(assistant.config) { settings = false }
        return
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().testTag("global-assistant")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            IconButton(back, enabled = !form.saving) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            Text(if (form.confirming) "创建旅行" else "旅行助手",
                Modifier.weight(1f).padding(top = 12.dp), style = MaterialTheme.typography.titleLarge)
            IconButton({ settings = true }, enabled = !state.busy && !form.saving && !form.metadataBusy,
                modifier = Modifier.testTag("global-assistant-settings")) { Icon(Icons.Outlined.Settings, "助手设置") }
        }
        when {
            form.recovering -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("正在核对上次创建结果…") }
            form.deleted -> {
                Text(form.error.orEmpty(), Modifier.padding(16.dp))
                navigationError?.let { Text(it) }
                Button({ scope.launch {
                    try { draft.acknowledge(); withContext(Dispatchers.Main.immediate) { onBack() } }
                    catch (canceled: CancellationException) { throw canceled }
                    catch (_: Exception) { navigationError = "恢复记录暂未清理，请重试；不会重新创建旅行。" }
                } }) { Text("返回我的旅行") }
            }
            form.restored && form.submissionLocked && !form.confirming -> {
                Text(form.error.orEmpty(), Modifier.padding(16.dp))
                Button(draft::retryRecovery) { Text("重新核对") }
            }
            navigationError != null -> {
                Text(navigationError.orEmpty())
                Button({ navigationError = null; navigationAttempt++ },
                    modifier = Modifier.testTag("trip-draft-reopen")) { Text("重新打开已创建旅行") }
            }
            form.confirming -> {
                if (!consentActive) TextButton(onConsent, modifier = Modifier.testTag("trip-draft-consent")) { Text("恢复地图授权") }
                if (assistant.config.read() == null) TextButton({ settings = true }) { Text("配置模型") }
                TripCreationForm(draft,
                onSubmit = {
                    val snapshot = assistant.controller.state.value
                    val selected = assistant.controller.selectedImportItems(requireMapReady = false)
                    val recoveredCommand = form.submissionLocked
                    draft.cancelMetadata(); draft.submit(
                    isValidSession = { token?.isActive() == true && assistant.config.read() != null &&
                        (recoveredCommand || (confirmedConfiguration == assistant.config.revision.value &&
                            draft.selectionIsCurrent(selected) && assistant.controller.state.value.let {
                                it.items == snapshot.items && it.selected == snapshot.selected
                            })) },
                    lock = assistant.controller::setExternalSaving) },
                onRetryMetadata = { draft.retryMetadata { token?.isActive() == true && assistant.config.read() != null } },
                onBack = draft::backToCandidates)
            }
            else -> {
                if (state.items.isNotEmpty() && token != null && token.isActive()) {
                    val markers = assistantMarkers(state, focused)
                    val points = assistantViewportPoints(state, focused)
                    val viewport = remember(points) { points.takeIf { it.isNotEmpty() }?.let {
                        MapViewportRequest(System.nanoTime(), ViewportReason.SEARCH_RESULTS, it)
                    } }
                    AmapComposeMap(MapUiModel(markers = markers, viewportRequest = viewport),
                        onMarkerClick = { key -> assistant.focus(state.items.find { key.startsWith("assistant:${it.id}:") }?.id) },
                        consent = token, modifier = Modifier.fillMaxWidth().height(180.dp),
                        onMapReady = { mapReady = true }, onMapError = { mapReady = false })
                }
                val host = AssistantWorkspace(assistant, state, true, focused, "",
                    onSettings = { settings = true }, onConsent = onConsent,
                    onOpen = {}, onViewPool = {}, detail = detail,
                    onCreateTrip = {
                        confirmedConfiguration = configuration
                        val selected = assistant.controller.selectedImportItems()
                        val evidence = state.items.filter { it.id in state.selected }.flatMap { item ->
                            item.sources.map { source -> when (source) {
                                is IntakeSource.Text -> source.span
                                is IntakeSource.Image -> "图${source.ordinal}:${source.excerpt}"
                            } }
                        }
                        draft.confirm(selected, evidence)
                    },
                    onSubmit = {
                        val input = state.input
                        val revision = configuration
                        assistant.controller.submit()
                        if (assistant.controller.state.value.busy)
                            draft.metadata(input, canApply = { assistant.config.revision.value == revision &&
                                assistant.controller.state.value.input == input })
                    },
                    onClear = draft::reset,
                    onCancel = { draft.cancelMetadata(); assistant.controller.cancel() })
                PlaceAssistantPanel(host, Modifier.weight(1f).padding(horizontal = 16.dp))
            }
        }
    }
    if (leavePrompt) AlertDialog(onDismissRequest = { leavePrompt = false }, title = { Text("放弃这次创建？") },
        text = { Text(if (form.submissionLocked) "未完成的最终确认会保留供下次核对；不会自动创建。" else "尚未创建旅行，输入和未收藏候选将被清除。") },
        confirmButton = { TextButton({
            draft.cancelMetadata(); assistant.controller.cancel()
            if (!form.submissionLocked) { assistant.clearDraft(); draft.reset() }
            leavePrompt = false; onBack()
        }) { Text("离开") } },
        dismissButton = { TextButton({ leavePrompt = false }) { Text("继续查看") } })
}
