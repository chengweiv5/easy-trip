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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.workspace.AmapComposeMap
import com.yangchengwei.easytrip.workspace.AmapMapHost
import com.yangchengwei.easytrip.workspace.RealAmapMapHost

private val SearchBackground = Color(0xFFF5F3EE)
private val SearchSurface = Color.White
private val SearchPrimary = Color(0xFF2D5E3A)
private val SearchPrimaryDark = Color(0xFF1B3A28)
private val SearchMuted = Color(0xFF6E7F72)
private val SearchBorder = Color(0xFFD6DDD0)
private val SearchSoft = Color(0xFFE7EFE2)
private val SearchError = Color(0xFFBA1A1A)

@Composable
fun PlaceSearchContent(
    state: PlaceSearchUiState,
    onAction: (PlaceSearchAction) -> Unit,
    modifier: Modifier = Modifier,
    resultsListState: LazyListState = rememberLazyListState(),
    detailContent: (@Composable (PlaceCandidate) -> Unit)? = null,
    consent: AmapConsentToken? = null,
    mapHostFactory: (android.content.Context) -> AmapMapHost = ::RealAmapMapHost,
) {
    when (val mode = state.displayMode) {
        SearchDisplayMode.Results -> Column(
            modifier
                .fillMaxSize()
                .background(SearchBackground)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .navigationBarsPadding()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            SearchHeader(state.search.query, onAction)
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("place-search-surface"),
                color = SearchSurface,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 0.dp,
            ) {
                SearchBody(state, onAction, resultsListState)
            }
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
                    detailContent = detailContent,
                    modifier = modifier,
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
    detailContent: (@Composable (PlaceCandidate) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(SearchBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding(),
    ) {
        val model = searchDetailMapModel(candidate, requestId)
        if (model != null && consent != null) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                AmapComposeMap(
                    model = model,
                    onMarkerClick = {},
                    consent = consent,
                    onMapPoiClick = {},
                    modifier = Modifier.fillMaxSize().testTag("place-search-detail-map"),
                    hostFactory = mapHostFactory,
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .testTag("place-search-detail-recenter")
                        .clickable(onClick = onRecenter)
                        .semantics {
                            contentDescription = "回到${candidate.name}"
                            role = Role.Button
                        },
                    shape = CircleShape,
                    color = SearchSurface,
                    shadowElevation = 4.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LocationIcon(Modifier.size(22.dp), SearchPrimary)
                    }
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
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
                    onAction = { action ->
                        when (action) {
                            PlaceDetailPanelAction.Dismiss -> onAction(PlaceSearchAction.Back)
                            PlaceDetailPanelAction.ToggleCollection -> onAction(PlaceSearchAction.ToggleCollection(candidate.poiId))
                            PlaceDetailPanelAction.StartEdit -> savedPlace?.let { onAction(PlaceSearchAction.StartEdit(it.id)) }
                            is PlaceDetailPanelAction.NoteChanged -> onAction(PlaceSearchAction.UpdateEditNote(action.value))
                            is PlaceDetailPanelAction.TagsChanged -> onAction(PlaceSearchAction.UpdateEditTags(action.value))
                            PlaceDetailPanelAction.SaveEdit -> onAction(PlaceSearchAction.SaveEdit)
                            PlaceDetailPanelAction.CancelEdit -> onAction(PlaceSearchAction.CancelEdit)
                            PlaceDetailPanelAction.Delete -> Unit
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchHeader(query: String, onAction: (PlaceSearchAction) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .testTag("place-search-back")
                .clip(CircleShape)
                .clickable { onAction(PlaceSearchAction.Back) }
                .semantics { contentDescription = "返回地点池"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            BackIcon(Modifier.size(22.dp), SearchPrimaryDark)
        }
        BasicTextField(
            value = query,
            onValueChange = { onAction(PlaceSearchAction.QueryChanged(it)) },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("place-search-field")
                .semantics { contentDescription = "搜索地点" },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = SearchPrimaryDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onAction(PlaceSearchAction.Submit) }),
            decorationBox = { input ->
                Row(
                    Modifier
                        .fillMaxSize()
                        .background(SearchSurface, CircleShape)
                        .border(2.dp, SearchPrimary, CircleShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SearchIcon(Modifier.size(21.dp), SearchPrimary)
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("搜索地点", color = SearchMuted, fontSize = 14.sp)
                        input()
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .testTag("place-search-clear")
                                .clip(CircleShape)
                                .clickable { onAction(PlaceSearchAction.QueryChanged("")) }
                                .semantics { contentDescription = "清空搜索"; role = Role.Button },
                            contentAlignment = Alignment.Center,
                        ) {
                            CloseIcon(Modifier.size(19.dp), SearchMuted)
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
) {
    when (val phase = state.search.phase) {
        PlaceSearchPhase.Initial -> SearchMessage(
            icon = { SearchIcon(Modifier.size(48.dp), SearchMuted) },
            title = "搜索想去的地方",
            message = "收藏后会停留在搜索页，可继续收藏更多地点。",
        )
        PlaceSearchPhase.Loading -> SearchMessage(
            icon = { CircularProgressIndicator(Modifier.size(42.dp), color = SearchPrimary, strokeWidth = 3.dp) },
            title = "正在搜索地点",
            message = "正在查找“${state.search.query}”相关结果…",
        )
        PlaceSearchPhase.Empty -> SearchMessage(
            icon = { SearchOffIcon(Modifier.size(48.dp), SearchMuted) },
            title = "没有找到相关地点",
            message = "试试更短的关键词，或检查地点名称是否正确。",
            buttonLabel = "清空搜索",
            testTagPrefix = "place-search-empty",
            onButtonClick = { onAction(PlaceSearchAction.QueryChanged("")) },
        )
        is PlaceSearchPhase.NetworkFailure -> SearchMessage(
            icon = { WifiOffIcon(Modifier.size(48.dp), SearchError) },
            title = "网络连接失败",
            message = phase.message.ifBlank { "无法搜索新的地点。请检查网络连接后重试。" },
            buttonLabel = "重新搜索",
            testTagPrefix = "place-search-network-failure",
            onButtonClick = { onAction(PlaceSearchAction.Retry) },
        )
        PlaceSearchPhase.Results -> SearchResults(state, onAction, resultsListState)
    }
}

@Composable
private fun SearchResults(
    state: PlaceSearchUiState,
    onAction: (PlaceSearchAction) -> Unit,
    resultsListState: LazyListState,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "搜索结果 · 可连续收藏",
                color = SearchPrimaryDark,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(
                Modifier.height(26.dp).background(SearchSoft, CircleShape).padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("${state.search.results.size} 个", color = SearchPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    icon: @Composable () -> Unit,
    title: String,
    message: String,
    buttonLabel: String? = null,
    testTagPrefix: String? = null,
    onButtonClick: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().then(
            if (testTagPrefix == null) Modifier else Modifier.testTag("$testTagPrefix-body"),
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.then(
                if (testTagPrefix == null) Modifier else Modifier.testTag("$testTagPrefix-icon"),
            ),
        ) {
            icon()
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            Modifier.then(
                if (testTagPrefix == null) Modifier else Modifier.testTag("$testTagPrefix-title"),
            ),
            color = SearchPrimaryDark,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
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
            color = SearchMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
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
                    .border(1.dp, SearchBorder, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onButtonClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(buttonLabel, color = SearchPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                .background(SearchSoft, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            LocationIcon(Modifier.size(22.dp), SearchPrimary)
        }
        Column(Modifier.weight(1f).testTag("place-search-result-text-${candidate.poiId}")) {
            Text(candidate.name, color = SearchPrimaryDark, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (candidate.address.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(candidate.address, color = SearchMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            Modifier
                .size(48.dp)
                .testTag("place-search-bookmark-touch-${candidate.poiId}")
                .clip(CircleShape)
                .clickable(enabled = candidate.point != null && !busy, onClick = onToggleCollection)
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
                    .background(if (saved) SearchPrimary else SearchSurface, CircleShape)
                    .border(1.dp, SearchPrimary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                BookmarkIcon(Modifier.size(20.dp), if (saved) Color.White else SearchPrimary, saved)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SearchBorder))
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
