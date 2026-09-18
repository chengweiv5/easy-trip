package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangchengwei.easytrip.amap.AmapConsentToken
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.EmptyIllustration
import com.yangchengwei.easytrip.core.ui.component.EmptyIllustrationImage
import com.yangchengwei.easytrip.core.ui.component.EmptyState
import com.yangchengwei.easytrip.core.ui.component.InlineStatus
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.workspace.AmapComposeMap
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.RealAmapMapHost
import kotlinx.coroutines.launch

@Composable
fun PlaceSearchContent(
    state: PlaceSearchUiState,
    onAction: (PlaceSearchAction) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSearch: Boolean = true,
    onAutoFocusConsumed: () -> Unit = {},
    resultsListState: LazyListState = rememberLazyListState(),
    detailContent: (@Composable (PlaceCandidate) -> Unit)? = null,
    consent: AmapConsentToken? = null,
    mapHostFactory: (android.content.Context) -> AmapMapHost = ::RealAmapMapHost,
    onOpenConsent: () -> Unit = {},
) {
    when (val mode = state.displayMode) {
        SearchDisplayMode.Results -> Column(
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .navigationBarsPadding()
                .padding(start = EasyTripTheme.spacing.large, top = EasyTripTheme.spacing.small, end = EasyTripTheme.spacing.large, bottom = EasyTripTheme.spacing.xLarge),
            verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.large),
        ) {
            SearchHeader(state.search.query, onAction, autoFocusSearch, onAutoFocusConsumed)
            SearchBody(
                state = state,
                onAction = onAction,
                resultsListState = resultsListState,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            state.collectionError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        is SearchDisplayMode.MapDetail -> {
            state.search.results.firstOrNull { it.poiId == mode.poiId }?.let { candidate ->
                SearchMapDetail(
                    candidate = candidate,
                    requestId = state.detailMapRequestId,
                    consent = consent,
                    mapHostFactory = mapHostFactory,
                    onRecenter = { onAction(PlaceSearchAction.RecenterDetail) },
                    savedPlace = state.savedPlacesByPoiId[candidate.poiId],
                    editState = state.detailDraft,
                    onAction = onAction,
                    collectionBusy = candidate.poiId in state.collectionBusyPoiIds,
                    collectionError = state.collectionError.takeIf { state.collectionErrorPoiId == candidate.poiId },
                    availableTagNames = state.availableTags.map { it.name },
                    detailContent = detailContent,
                    modifier = modifier,
                    onOpenConsent = onOpenConsent,
                )
            }
        }
    }
}

@Composable
private fun SearchMapDetail(
    candidate: PlaceCandidate,
    requestId: Long,
    consent: AmapConsentToken?,
    mapHostFactory: (android.content.Context) -> AmapMapHost,
    onRecenter: () -> Unit,
    savedPlace: com.yangchengwei.easytrip.place.domain.SavedPlace?,
    editState: PlaceDetailEditState?,
    onAction: (PlaceSearchAction) -> Unit,
    collectionBusy: Boolean,
    collectionError: String?,
    availableTagNames: List<String>,
    detailContent: (@Composable (PlaceCandidate) -> Unit)?,
    modifier: Modifier = Modifier,
    onOpenConsent: () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding(),
    ) {
        val model = searchDetailMapModel(candidate, requestId)
        val mapAttempt = remember(candidate.poiId) { mutableIntStateOf(0) }
        var mapError by remember(candidate.poiId, mapAttempt.intValue) { mutableStateOf<Throwable?>(null) }
        val mapScope = rememberCoroutineScope()
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxSize(0.58f)
                .align(Alignment.TopCenter)
                .testTag("place-search-detail-map-region"),
        ) {
            when {
                model != null && consent != null && mapError == null -> {
                    AmapComposeMap(
                        model = model,
                        onMarkerClick = {},
                        consent = consent,
                        onMapPoiClick = {},
                        modifier = Modifier.fillMaxSize().testTag("place-search-detail-map"),
                        hostFactory = mapHostFactory,
                        onMapError = { error -> mapScope.launch { mapError = error } },
                        onLayerError = { error, _ -> mapScope.launch { mapError = error } },
                        retryKey = mapAttempt.intValue,
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(EasyTripTheme.spacing.medium)
                            .size(EasyTripTheme.sizes.iconButtonSize)
                            .testTag("place-search-detail-recenter")
                            .clickable(onClick = onRecenter)
                            .semantics {
                                contentDescription = "回到${candidate.name}"
                                role = Role.Button
                            },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = EasyTripTheme.elevation.floating,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            LocationIcon(Modifier.size(EasyTripTheme.sizes.iconSize), MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                mapError != null -> InlineStatus(
                    title = "地图加载失败",
                    modifier = Modifier.fillMaxSize().testTag("place-search-detail-map-failure"),
                    actionLabel = "重试",
                    onAction = {
                        mapError = null
                        mapAttempt.intValue++
                    },
                    actionTag = "place-search-detail-retry",
                )
                else -> InlineStatus(
                    title = if (model == null) "地图暂不可用" else "地图服务未启用",
                    modifier = Modifier.fillMaxSize().testTag("place-search-detail-map-recovery"),
                    actionLabel = if (model == null) null else "查看并授权",
                    onAction = if (model == null) null else onOpenConsent,
                    actionTag = if (model == null) null else "search-consent-open",
                )
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.48f)
                .align(Alignment.BottomCenter)
                .testTag("place-search-detail-panel"),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            shadowElevation = EasyTripTheme.elevation.floating,
        ) {
            if (detailContent != null) {
                detailContent(candidate)
            } else {
                PlaceDetailPanel(
                    candidate = candidate,
                    savedPlace = savedPlace,
                    editState = editState,
                    source = PlaceDetailSource.Search,
                    collectionBusy = collectionBusy,
                    collectionError = collectionError,
                    availableTagNames = availableTagNames,
                    onAction = { action ->
                        when (action) {
                            PlaceDetailPanelAction.Dismiss -> onAction(PlaceSearchAction.Back)
                            PlaceDetailPanelAction.ToggleCollection -> onAction(PlaceSearchAction.ToggleCollection(candidate.poiId))
                            PlaceDetailPanelAction.StartEdit -> savedPlace?.let { onAction(PlaceSearchAction.StartEdit(it.id)) }
                            is PlaceDetailPanelAction.NoteChanged -> onAction(PlaceSearchAction.UpdateEditNote(action.value))
                            is PlaceDetailPanelAction.NewTagInputChanged -> onAction(PlaceSearchAction.UpdateNewTagInput(action.value))
                            PlaceDetailPanelAction.AddTag -> onAction(PlaceSearchAction.AddNewTag)
                            is PlaceDetailPanelAction.AddPresetTag -> onAction(PlaceSearchAction.AddPresetTag(action.name))
                            is PlaceDetailPanelAction.RemoveTag -> onAction(PlaceSearchAction.RemoveEditTag(action.name))
                            PlaceDetailPanelAction.SaveEdit -> onAction(PlaceSearchAction.SaveEdit)
                            PlaceDetailPanelAction.CancelEdit -> onAction(PlaceSearchAction.CancelEdit)
                            PlaceDetailPanelAction.Delete,
                            PlaceDetailPanelAction.StartAddToItinerary -> Unit
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onAction: (PlaceSearchAction) -> Unit,
    autoFocusSearch: Boolean,
    onAutoFocusConsumed: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(autoFocusSearch) {
        if (autoFocusSearch) {
            focusRequester.requestFocus()
            keyboardController?.show()
            onAutoFocusConsumed()
        }
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .requiredSize(EasyTripTheme.sizes.searchHeaderBackTouchTarget)
                .testTag("place-search-back")
                .clip(CircleShape)
                .clickable { onAction(PlaceSearchAction.Back) }
                .semantics { contentDescription = "返回地点池"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            BackIcon(Modifier.size(22.dp), MaterialTheme.colorScheme.onSurface)
        }
        BasicTextField(
            value = query,
            onValueChange = { onAction(PlaceSearchAction.QueryChanged(it)) },
            modifier = Modifier
                .weight(1f)
                .height(EasyTripTheme.sizes.searchHeaderBackTouchTarget)
                .focusRequester(focusRequester)
                .testTag("place-search-field")
                .semantics { contentDescription = "搜索地点" },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onAction(PlaceSearchAction.Submit) }),
            decorationBox = { input ->
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .height(EasyTripTheme.sizes.searchHeaderHeight)
                            .fillMaxWidth()
                            .testTag("place-search-field-visual")
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(EasyTripTheme.sizes.searchHeaderCornerRadius),
                            )
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(EasyTripTheme.sizes.searchHeaderCornerRadius),
                            ),
                    )
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = EasyTripTheme.sizes.searchHeaderHorizontalPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.small),
                    ) {
                        SearchIcon(Modifier.size(21.dp), MaterialTheme.colorScheme.primary)
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text("搜索地点", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            input()
                        }
                        if (query.isNotEmpty()) {
                            Box(
                                Modifier
                                    .size(EasyTripTheme.sizes.iconButtonSize)
                                    .testTag("place-search-clear")
                                    .clip(CircleShape)
                                    .clickable { onAction(PlaceSearchAction.QueryChanged("")) }
                                    .semantics { contentDescription = "清空搜索"; role = Role.Button },
                                contentAlignment = Alignment.Center,
                            ) {
                                CloseIcon(Modifier.size(19.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun SearchBody(
    state: PlaceSearchUiState,
    onAction: (PlaceSearchAction) -> Unit,
    resultsListState: LazyListState,
    modifier: Modifier,
) {
    when (val phase = state.search.phase) {
        PlaceSearchPhase.Initial -> SearchMessage(
            modifier = modifier,
            testTagPrefix = "place-search-initial",
            emptyIllustration = EmptyIllustration.Search,
            title = "搜索想去的地方",
            message = "收藏后会停留在搜索页，可继续收藏更多地点。",
        )
        PlaceSearchPhase.ConsentRequired -> ConsentRequiredBody(state.search.savedPlaces, onAction, modifier)
        PlaceSearchPhase.Loading -> SearchMessage(
            modifier = modifier,
            testTagPrefix = "place-search-loading",
            icon = { CircularProgressIndicator(Modifier.size(42.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp) },
            title = "正在搜索地点",
            message = "正在查找“${state.search.query}”相关结果…",
        )
        PlaceSearchPhase.Empty -> SearchMessage(
            modifier = modifier,
            emptyIllustration = EmptyIllustration.Search,
            title = "没有找到相关地点",
            message = "试试更短的关键词，或检查地点名称是否正确。",
            buttonLabel = "清空搜索",
            testTagPrefix = "place-search-empty",
            onButtonClick = { onAction(PlaceSearchAction.QueryChanged("")) },
        )
        is PlaceSearchPhase.NetworkFailure -> SearchMessage(
            modifier = modifier,
            emptyIllustration = EmptyIllustration.Failure,
            title = "网络连接失败",
            message = phase.message.ifBlank { "无法搜索新的地点。请检查网络连接后重试。" },
            buttonLabel = "重新搜索",
            testTagPrefix = "place-search-network-failure",
            onButtonClick = { onAction(PlaceSearchAction.Retry) },
        )
        PlaceSearchPhase.Results -> SearchResults(state, onAction, resultsListState, modifier)
    }
}

@Composable
private fun ConsentRequiredBody(
    savedPlaces: List<com.yangchengwei.easytrip.place.domain.SavedPlace>,
    onAction: (PlaceSearchAction) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.testTag("search-consent-required"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SearchMessage(
            modifier = Modifier.fillMaxWidth(),
            icon = { SearchOffIcon(Modifier.size(48.dp), MaterialTheme.colorScheme.onSurfaceVariant) },
            title = "地图服务未启用",
            message = "在线地点搜索暂不可用，本地收藏仍可使用。",
            buttonLabel = "查看并授权",
            testTagPrefix = "search-consent-required",
            fillAvailableSpace = false,
            onButtonClick = { onAction(PlaceSearchAction.OpenConsent) },
        )
        if (savedPlaces.isNotEmpty()) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("saved-place-list")) {
                items(savedPlaces, key = { it.id }) { place ->
                    Text(
                        place.name,
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    state: PlaceSearchUiState,
    onAction: (PlaceSearchAction) -> Unit,
    resultsListState: LazyListState,
    modifier: Modifier,
) {
    Column(modifier.testTag("place-search-results-container")) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "搜索结果 · 可连续收藏",
                modifier = Modifier.weight(1f).widthIn(min = 0.dp).testTag("place-search-results-title"),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                Modifier
                    .height(26.dp)
                    .widthIn(min = 48.dp)
                    .testTag("place-search-results-count")
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("${state.search.results.size} 个", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
            }
        }
        LazyColumn(Modifier.fillMaxSize(), state = resultsListState) {
            items(state.search.results, key = { it.poiId }) { candidate ->
                SearchResultRow(
                    candidate = candidate,
                    saved = candidate.poiId in state.savedPoiIds,
                    busy = candidate.poiId in state.collectionBusyPoiIds,
                    onOpenDetail = { onAction(PlaceSearchAction.OpenDetail(candidate.poiId)) },
                    onToggleCollection = { onAction(PlaceSearchAction.ToggleCollection(candidate.poiId)) },
                )
            }
        }
    }
}

@Composable
private fun SearchMessage(
    title: String,
    message: String,
    modifier: Modifier,
    icon: (@Composable () -> Unit)? = null,
    emptyIllustration: EmptyIllustration? = null,
    buttonLabel: String? = null,
    testTagPrefix: String? = null,
    fillAvailableSpace: Boolean = true,
    onButtonClick: () -> Unit = {},
) {
    Column(
        modifier
            .then(if (fillAvailableSpace) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
            .then(if (testTagPrefix == null) Modifier else Modifier.testTag("$testTagPrefix-body")),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.then(
                if (testTagPrefix == null) Modifier else Modifier.testTag("$testTagPrefix-icon"),
            ),
        ) {
            emptyIllustration?.let { EmptyIllustrationImage(it) }
            icon?.invoke()
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            Modifier.then(
                if (testTagPrefix == null) Modifier else Modifier.testTag("$testTagPrefix-title"),
            ),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .then(
                    if (testTagPrefix == null) Modifier else Modifier.testTag("$testTagPrefix-description"),
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        if (buttonLabel != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .then(
                        if (testTagPrefix == null) Modifier else Modifier.testTag("$testTagPrefix-action"),
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onButtonClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(buttonLabel, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    candidate: PlaceCandidate,
    saved: Boolean,
    busy: Boolean,
    onOpenDetail: () -> Unit,
    onToggleCollection: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("place-search-result-row-${candidate.poiId}")
            .clickable(onClick = onOpenDetail)
            .semantics {
                contentDescription = "查看${candidate.name}详情"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .testTag("place-search-place-icon-${candidate.poiId}")
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            LocationIcon(Modifier.size(22.dp), MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f).testTag("place-search-result-text-${candidate.poiId}")) {
            Text(candidate.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (candidate.address.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(candidate.address, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            Modifier
                .size(48.dp)
                .testTag("place-search-bookmark-touch-${candidate.poiId}")
                .clip(CircleShape)
                .clickable(enabled = !busy, onClick = onToggleCollection)
                .semantics {
                    contentDescription = if (saved) "取消收藏${candidate.name}" else "收藏${candidate.name}"
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .testTag("place-search-bookmark-visual-${candidate.poiId}")
                    .background(if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                BookmarkIcon(Modifier.size(20.dp), if (saved) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, saved)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
    Spacer(Modifier.height(1.dp))
}

@Composable
private fun BackIcon(modifier: Modifier, color: Color) = Canvas(modifier) {
    drawLine(color, center.copy(x = size.width * .22f), center.copy(x = size.width * .78f), 2.2.dp.toPx(), StrokeCap.Round)
    drawLine(color, center.copy(x = size.width * .22f), center.copy(x = size.width * .46f, y = size.height * .24f), 2.2.dp.toPx(), StrokeCap.Round)
    drawLine(color, center.copy(x = size.width * .22f), center.copy(x = size.width * .46f, y = size.height * .76f), 2.2.dp.toPx(), StrokeCap.Round)
}

@Composable
private fun SearchIcon(modifier: Modifier, color: Color) = Canvas(modifier) {
    drawCircle(color, radius = size.minDimension * .3f, center = center.copy(x = size.width * .43f, y = size.height * .43f), style = Stroke(2.dp.toPx()))
    drawLine(color, size.run { androidx.compose.ui.geometry.Offset(width * .65f, height * .65f) }, size.run { androidx.compose.ui.geometry.Offset(width * .86f, height * .86f) }, 2.dp.toPx(), StrokeCap.Round)
}

@Composable
private fun CloseIcon(modifier: Modifier, color: Color) = Canvas(modifier) {
    drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .25f, size.height * .25f), androidx.compose.ui.geometry.Offset(size.width * .75f, size.height * .75f), 2.dp.toPx(), StrokeCap.Round)
    drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .75f, size.height * .25f), androidx.compose.ui.geometry.Offset(size.width * .25f, size.height * .75f), 2.dp.toPx(), StrokeCap.Round)
}

@Composable
private fun LocationIcon(modifier: Modifier, color: Color) = Canvas(modifier) {
    val path = Path().apply {
        moveTo(size.width * .5f, size.height * .9f)
        cubicTo(size.width * .42f, size.height * .78f, size.width * .2f, size.height * .58f, size.width * .2f, size.height * .4f)
        cubicTo(size.width * .2f, size.height * .08f, size.width * .8f, size.height * .08f, size.width * .8f, size.height * .4f)
        cubicTo(size.width * .8f, size.height * .58f, size.width * .58f, size.height * .78f, size.width * .5f, size.height * .9f)
        close()
    }
    drawPath(path, color, style = Stroke(1.8.dp.toPx()))
    drawCircle(color, size.minDimension * .09f, center.copy(y = size.height * .4f), style = Stroke(1.8.dp.toPx()))
}

@Composable
private fun BookmarkIcon(modifier: Modifier, color: Color, filled: Boolean) = Canvas(modifier) {
    val path = Path().apply {
        moveTo(size.width * .25f, size.height * .12f)
        lineTo(size.width * .75f, size.height * .12f)
        lineTo(size.width * .75f, size.height * .88f)
        lineTo(size.width * .5f, size.height * .7f)
        lineTo(size.width * .25f, size.height * .88f)
        close()
    }
    drawPath(path, color, style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(1.8.dp.toPx()))
}

@Composable
private fun SearchOffIcon(modifier: Modifier, color: Color) = Canvas(modifier) {
    drawCircle(color, radius = size.minDimension * .25f, center = center.copy(x = size.width * .4f, y = size.height * .4f), style = Stroke(2.dp.toPx()))
    drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .58f, size.height * .58f), androidx.compose.ui.geometry.Offset(size.width * .78f, size.height * .78f), 2.dp.toPx(), StrokeCap.Round)
    drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .82f), androidx.compose.ui.geometry.Offset(size.width * .82f, size.height * .18f), 2.5.dp.toPx(), StrokeCap.Round)
}

@Composable
private fun WifiOffIcon(modifier: Modifier, color: Color) = Canvas(modifier) {
    drawArc(
        color = color,
        startAngle = 220f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = androidx.compose.ui.geometry.Offset(size.width * .12f, size.height * .22f),
        size = androidx.compose.ui.geometry.Size(size.width * .76f, size.height * .76f),
        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
    )
    drawCircle(color, size.minDimension * .045f, center.copy(y = size.height * .75f))
    drawLine(color, androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .18f), androidx.compose.ui.geometry.Offset(size.width * .82f, size.height * .82f), 2.5.dp.toPx(), StrokeCap.Round)
}
