package com.yangchengwei.easytrip.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.yangchengwei.easytrip.core.ui.component.CompactSecondaryButton
import com.yangchengwei.easytrip.core.ui.component.EmptyIllustration
import com.yangchengwei.easytrip.core.ui.component.CompactPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EmptyState
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryAction
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryContent
import com.yangchengwei.easytrip.itinerary.ui.DayItineraryUiState
import com.yangchengwei.easytrip.itinerary.ui.WorkspaceItineraryContent
import com.yangchengwei.easytrip.place.ui.PlacePoolAction
import com.yangchengwei.easytrip.place.ui.PlacePoolContent
import com.yangchengwei.easytrip.place.ui.PlacePoolUiState

@Composable
fun TripWorkspaceContent(
    pageState: TripWorkspacePageState,
    mapState: WorkspaceMapState,
    onAction: (TripWorkspaceAction) -> Unit,
    onPageRetry: () -> Unit = { onAction(TripWorkspaceAction.Retry) },
    onOpenConsent: () -> Unit = { onAction(TripWorkspaceAction.OpenPrivacySettings) },
    onMapRetry: () -> Unit = { onAction(TripWorkspaceAction.Retry) },
    placeState: PlacePoolUiState,
    onPlaceAction: (PlacePoolAction) -> Unit,
    itineraryState: DayItineraryUiState,
    onItineraryAction: (DayItineraryAction) -> Unit,
    mapContent: @Composable BoxScope.(WorkspaceMapLayout) -> Unit,
    placeContent: (@Composable () -> Unit)? = null,
    dayItineraryContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    searchReturn: WorkspaceSearchReturn? = null,
    layerFailureMessage: String? = null,
    onLayerFailureMessageDismissed: () -> Unit = {},
    mapBearing: Float = 0f,
) {
    when (pageState) {
        TripWorkspacePageState.Loading -> com.yangchengwei.easytrip.core.ui.component.DeferredLoading { WorkspacePageMessage("旅行加载中") }
        TripWorkspacePageState.NotFound -> WorkspacePageMessage("旅行不存在", "返回旅行列表") { onAction(TripWorkspaceAction.Back) }
        is TripWorkspacePageState.Error -> WorkspacePageMessage(pageState.message, "重试", onPageRetry)
        is TripWorkspacePageState.Ready -> WorkspaceReadyContent(
            pageState.content,
            mapState,
            onAction,
            onOpenConsent,
            onMapRetry,
            placeState,
            onPlaceAction,
            itineraryState,
            onItineraryAction,
            mapContent,
            placeContent,
            dayItineraryContent,
            modifier,
            searchReturn,
            layerFailureMessage,
            onLayerFailureMessageDismissed,
            mapBearing,
        )
    }
}

@Composable
private fun WorkspacePageMessage(message: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message)
        action?.let { CompactSecondaryButton(onAction) { Text(it) } }
    }
}

@Composable
private fun WorkspaceReadyContent(
    state: TripWorkspaceReadyState,
    mapState: WorkspaceMapState,
    onAction: (TripWorkspaceAction) -> Unit,
    onOpenConsent: () -> Unit,
    onMapRetry: () -> Unit,
    placeState: PlacePoolUiState,
    onPlaceAction: (PlacePoolAction) -> Unit,
    itineraryState: DayItineraryUiState,
    onItineraryAction: (DayItineraryAction) -> Unit,
    mapContent: @Composable BoxScope.(WorkspaceMapLayout) -> Unit,
    placeContent: (@Composable () -> Unit)?,
    dayItineraryContent: (@Composable () -> Unit)?,
    modifier: Modifier,
    searchReturn: WorkspaceSearchReturn?,
    layerFailureMessage: String?,
    onLayerFailureMessageDismissed: () -> Unit,
    mapBearing: Float,
) {
    val density = LocalDensity.current
    var calendarBusy by remember { mutableStateOf(false) }
    WorkspaceScaffold(
        sheetLevel = state.sheetLevel,
        sheetGesturesEnabled = !calendarBusy,
        onSheetLevelChange = { if (!calendarBusy) onAction(TripWorkspaceAction.SetSheetLevel(it)) },
        modifier = modifier,
        sheetHeader = { metrics ->
            Column(Modifier.fillMaxWidth()) {
                WorkspaceSheetHandle()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    WorkspaceTabs(
                        selected = state.section,
                        onSelect = { if (!calendarBusy) onAction(TripWorkspaceAction.SelectSection(it)) },
                        modifier = Modifier.weight(1f),
                    )
                    if (!workspaceMapOverlaysFit(metrics) && state.sheetLevel != WorkspaceSheetLevel.EXPANDED) {
                        val recoveryModifier = Modifier
                            .requiredHeight(48.dp)
                            .padding(horizontal = 8.dp)

                        when (mapState) {
                            is WorkspaceMapState.Failed -> CompactPrimaryButton(
                                onClick = onMapRetry,
                                modifier = recoveryModifier.testTag("map-retry"),
                            ) { Text("重试") }
                            WorkspaceMapState.ConsentRequired -> CompactPrimaryButton(
                                onClick = onOpenConsent,
                                modifier = recoveryModifier.testTag("map-consent-open"),
                            ) { Text("授权") }
                            else -> Unit
                        }
                    }
                }
            }
        },
            sheetContentHorizontalPadding = if (state.section == WorkspaceSection.ITINERARY) 0.dp else 16.dp,
        collapsedContentHorizontalPadding = 16.dp,
        collapsedContent = {
                WorkspaceCollapsedSummary(
                    state = state,
                    placeState = placeState,
                    itineraryState = itineraryState,
                )
            },
        sheetContent = { metrics ->
            Column(Modifier.fillMaxSize()) {
                if (state.sheetLevel == WorkspaceSheetLevel.EXPANDED && !workspaceMapOverlaysFit(metrics)) {
                    when (mapState) {
                        is WorkspaceMapState.Failed -> MapRecoveryAction(
                            label = "重试地图",
                            testTag = "map-retry",
                            onClick = onMapRetry,
                        )
                        WorkspaceMapState.ConsentRequired -> MapRecoveryAction(
                            label = "查看并授权",
                            testTag = "map-consent-open",
                            onClick = onOpenConsent,
                        )
                        WorkspaceMapState.Loading -> Text(
                            "正在加载地图 · 地点和行程仍可继续查看",
                            Modifier.testTag("map-loading-compact"),
                        )
                        else -> Unit
                    }
                }
                when (state.section) {
                    WorkspaceSection.PLACE_POOL -> if (state.isWorkspaceAllEmpty) {
                        EmptyState(
                            title = "旅行还是空的",
                            message = "还没有收藏地点，也没有安排任何行程。先搜索想去的地方，收藏后再加入旅行日。",
                            emptyIllustration = EmptyIllustration.Places,
                            modifier = Modifier.weight(1f).testTag("workspace-all-empty"),
                        )
                    } else if (placeContent != null) {
                        Box(Modifier.weight(1f)) { placeContent() }
                    } else PlacePoolContent(
                        state = placeState.copy(
                            rows = placeState.rows.map { row ->
                                row.copy(recentlyCollected = row.recentlyCollected || row.place.amapPoiId in searchReturn?.recentlyCollectedPoiIds.orEmpty())
                            },
                        ),
                        modifier = Modifier.weight(1f),
                        showSearch = false,
                        onAction = onPlaceAction,
                        onSearch = { onAction(TripWorkspaceAction.OpenSearch) },
                        schedulesByPlaceId = state.schedulesByPlaceId,
                        showDialogs = false,
                        contentPadding = PaddingValues(bottom = 4.dp),
                    )
                    WorkspaceSection.ITINERARY -> if (state.isItineraryAllEmpty && !state.calendarMode) {
                        EmptyState(
                            title = "还没有安排行程",
                            message = "当前旅行的所有旅行日都没有行程项。先去地点池收藏地点，再添加到对应旅行日。",
                            emptyIllustration = EmptyIllustration.Itinerary,
                            modifier = Modifier.weight(1f).testTag("itinerary-all-empty"),
                        )
                    } else WorkspaceItineraryContent(
                        days = state.days,
                        selected = state.itineraryScope,
                        wholeTripDays = state.wholeTripDays,
                        onToggleCalendar = { onAction(TripWorkspaceAction.ToggleCalendar) },
                        calendarContent = if (!state.calendarMode) null else ({
                            com.yangchengwei.easytrip.itinerary.calendar.CalendarContent(
                                rawDays = state.calendarDays,
                                selected = state.itineraryScope,
                                saveState = state.calendarSave,
                                focusItemId = state.calendarFocus,
                                startDate = state.startDate,
                                onToggle = { onAction(TripWorkspaceAction.ToggleCalendar) },
                                onFocus = { dayId, itemId -> onAction(TripWorkspaceAction.FocusCalendar(dayId, itemId)) },
                                onEdit = { _, itemId -> onItineraryAction(DayItineraryAction.RequestTiming(itemId)) },
                                onAdd = { onItineraryAction(DayItineraryAction.AddPlaces) },
                                onSave = { onAction(TripWorkspaceAction.SaveCalendar(it)) },
                                onUndo = { onAction(TripWorkspaceAction.UndoCalendar) },
                                onRetry = { onAction(TripWorkspaceAction.RetryCalendar) },
                                onDismissMessage = { onAction(TripWorkspaceAction.DismissCalendarMessage) },
                                onBusy = { calendarBusy = it },
                                onRoute = { onItineraryAction(DayItineraryAction.RequestMode(it)) },
                            )
                        }),
                        onSelect = { if (!calendarBusy) onAction(TripWorkspaceAction.SelectItineraryScope(it)) },
                        onAddDay = { if (!calendarBusy) onAction(TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.AddTripDay)) },
                        onAppendDay = { if (!calendarBusy) onItineraryAction(DayItineraryAction.AppendTripDay) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues.Zero,
                        startDate = state.startDate,
                        dayContent = {
                            if (dayItineraryContent != null) dayItineraryContent() else DayItineraryContent(
                                state = itineraryState,
                                startDate = state.startDate,
                                onAction = onItineraryAction,
                                showDialogs = false,
                                canScheduleAgain = true,
                                onToggleCalendar = { onAction(TripWorkspaceAction.ToggleCalendar) },
                            )
                        },
                    )
                }
            }
        },
        map = { metrics ->
            Box(Modifier.fillMaxSize().testTag("workspace-map")) {
                if (mapState == WorkspaceMapState.Ready || mapState == WorkspaceMapState.Loading) {
                    mapContent(workspaceMapLayout(metrics, density.density))
                }
                if (mapState != WorkspaceMapState.Ready && workspaceMapOverlaysFit(metrics)) {
                    WorkspaceMapFallback(
                        state = mapState,
                        onOpenConsent = onOpenConsent,
                        onRetryMap = onMapRetry,
                        modifier = Modifier.padding(bottom = metrics.sheetHeight),
                    )
                }
            }
        },
        modalOverlay = { metrics ->
            when {
                state.overlay == WorkspaceOverlay.LayerMenu && workspaceLayerMenuFits(metrics) -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag("layer-menu-hit-shield")
                            .clickable { onAction(TripWorkspaceAction.CloseOverlay) },
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .offset(y = 128.dp)
                            .fillMaxWidth()
                            .height((metrics.sheetTop - 128.dp).coerceAtLeast(0.dp))
                            .testTag("layer-menu-scrim")
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                            .clickable { onAction(TripWorkspaceAction.CloseOverlay) },
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-WorkspaceLayerMenuEndInset), y = WorkspaceLayerMenuTopOffset)
                            .width(240.dp)
                            .testTag("layer-menu-picker")
                            .pointerInput(Unit) { detectTapGestures { } },
                    ) {
                        MapLayerMenu(
                            layer = state.mapLayer,
                            onClose = { onAction(TripWorkspaceAction.CloseOverlay) },
                            onSelectLayer = { onAction(TripWorkspaceAction.SelectMapLayer(it)) },
                        )
                    }
                }
                state.overlay == WorkspaceOverlay.MoreMenu && workspaceMoreMenuFits(metrics) -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag("more-menu-hit-shield")
                            .clickable { onAction(TripWorkspaceAction.CloseOverlay) },
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag("more-menu-scrim")
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                            .clickable { onAction(TripWorkspaceAction.CloseOverlay) },
                    )
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-WorkspaceMoreMenuEndInset), y = WorkspaceMoreMenuTopOffset)
                            .width(WorkspaceMoreMenuWidth)
                            .testTag("more-menu-picker")
                            .pointerInput(Unit) { detectTapGestures { } },
                    ) {
                        WorkspaceMoreMenu(
                            onShareItinerary = {
                                onAction(TripWorkspaceAction.CloseOverlay)
                                onAction(TripWorkspaceAction.ShareItinerary)
                            },
                            onOpenSettings = {
                                onAction(TripWorkspaceAction.CloseOverlay)
                                onAction(TripWorkspaceAction.OpenSettings)
                            },
                            onOpenConsent = {
                                onAction(TripWorkspaceAction.CloseOverlay)
                                onAction(TripWorkspaceAction.OpenPrivacySettings)
                            },
                            onBackToTrips = {
                                onAction(TripWorkspaceAction.CloseOverlay)
                                onAction(TripWorkspaceAction.LeaveWorkspace)
                            },
                        )
                    }
                }
            }
        },
        topOverlay = { metrics ->
            layerFailureMessage?.let { message ->
                WorkspaceLayerFailureFeedback(
                    message = message,
                    onDismissed = onLayerFailureMessageDismissed,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 84.dp, start = 20.dp, end = 20.dp),
                )
            }
            val mapOverlaysFit = workspaceMapOverlaysFit(metrics)
            val layerMenuFits = workspaceLayerMenuFits(metrics)
            val moreMenuFits = workspaceMoreMenuFits(metrics)
            LaunchedEffect(state.overlay, layerMenuFits, moreMenuFits) {
                if (
                    (state.overlay == WorkspaceOverlay.LayerMenu && !layerMenuFits) ||
                    (state.overlay == WorkspaceOverlay.MoreMenu && !moreMenuFits)
                ) {
                    onAction(TripWorkspaceAction.CloseOverlay)
                }
            }
            WorkspaceTopBar(
                title = state.tripName,
                dateLabel = state.dateLabel,
                onBack = { onAction(TripWorkspaceAction.Back) },
                onMore = {
                    if (state.overlay == WorkspaceOverlay.None) {
                        onAction(TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.MoreMenu))
                    }
                },
                modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 12.dp),
            )
            if (mapOverlaysFit && mapState == WorkspaceMapState.Ready) {
                if (state.section == WorkspaceSection.PLACE_POOL && placeState.placesReady) {
                    MapCollectionSummary(
                        count = com.yangchengwei.easytrip.place.ui.placePoolCollectionTotal(placeState),
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 70.dp),
                    )
                }
                MapControls(
                    active = state.overlay == WorkspaceOverlay.LayerMenu,
                    onOpenLayerMenu = { onAction(TripWorkspaceAction.OpenOverlay(WorkspaceOverlay.LayerMenu)) },
                    onLocate = { onAction(TripWorkspaceAction.Locate) },
                    bearing = mapBearing,
                    onResetNorth = { onAction(TripWorkspaceAction.ResetNorth) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = minOf(70.dp, (metrics.sheetTop - WorkspaceMapControlsHeight).coerceAtLeast(66.dp)),
                            end = 12.dp,
                        ),
                )
                Row(
                    Modifier.align(Alignment.TopStart)
                        .padding(start = 12.dp, end = 12.dp, top = workspaceLegendTop(metrics))
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MapLegend(scheduledColor = if (state.section == WorkspaceSection.ITINERARY) {
                        val selected = (state.itineraryScope as? ItineraryScope.Day)?.dayId
                        Color(routeColorForDay(state.days.firstOrNull { it.id == selected }?.index ?: 0))
                    } else MaterialTheme.colorScheme.primary)
                    WorkspaceSearchBar(
                        onClick = { onAction(TripWorkspaceAction.OpenSearch) },
                    )
                }
            }
        },
    )
}

@Composable
private fun MapRecoveryAction(
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        CompactPrimaryButton(
            onClick = onClick,
            modifier = Modifier.height(50.dp).testTag(testTag),
        ) { Text(label) }
    }
}

@Composable
private fun WorkspaceLayerFailureFeedback(
    message: String,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(message) {
        delay(4_000)
        onDismissed()
    }
    Snackbar(modifier = modifier.testTag("map-layer-failure")) { Text(message) }
}

private val WorkspaceMapOverlayRequiredHeight = 226.dp
private val WorkspaceMapControlsHeight = 100.dp
private val WorkspaceLayerMenuTopOffset = 140.dp
private val WorkspaceLayerMenuHeight = 284.dp
private val WorkspaceLayerMenuEndInset = 16.dp
private val WorkspaceMoreMenuTopOffset = 68.dp
private val WorkspaceMoreMenuWidth = 240.dp
private val WorkspaceMoreMenuHeight = 260.dp
private val WorkspaceMoreMenuEndInset = 12.dp

internal fun workspaceMapOverlaysFit(metrics: WorkspaceLayoutMetrics): Boolean =
    metrics.sheetTop >= WorkspaceMapOverlayRequiredHeight

internal fun workspaceLayerMenuFits(metrics: WorkspaceLayoutMetrics): Boolean =
    metrics.availableWidth >= WorkspaceLayerMenuEndInset + 240.dp &&
        metrics.sheetTop >= WorkspaceLayerMenuTopOffset + WorkspaceLayerMenuHeight

internal fun workspaceMoreMenuFits(metrics: WorkspaceLayoutMetrics): Boolean =
    metrics.availableWidth >= WorkspaceMoreMenuEndInset + WorkspaceMoreMenuWidth &&
        metrics.availableHeight >= WorkspaceMoreMenuTopOffset + WorkspaceMoreMenuHeight

@Composable
private fun WorkspaceCollapsedSummary(
    state: TripWorkspaceReadyState,
    placeState: PlacePoolUiState,
    itineraryState: DayItineraryUiState,
) {
    val summary = when (state.section) {
        WorkspaceSection.PLACE_POOL -> {
            val count = placeState.savedPoiIds.size.takeIf { it > 0 } ?: placeState.rows.size
            when (count) {
                0 -> "还没有收藏地点"
                else -> "已收藏 $count 个地点"
            }
        }
        WorkspaceSection.ITINERARY -> when (val scope = state.itineraryScope) {
            ItineraryScope.WholeTrip -> "全程 · ${state.wholeTripDays.sumOf { it.items.size }} 个地点"
            is ItineraryScope.Day -> {
                val dayNumber = state.days.indexOfFirst { it.id == scope.dayId }.takeIf { it >= 0 }?.plus(1)
                val placeCount = if (itineraryState.selectedDayId == scope.dayId) itineraryState.items.size else {
                    state.wholeTripDays.firstOrNull { it.dayId == scope.dayId }
                        ?.let { it.items.size + it.collapsedItemCount } ?: 0
                }
                if (dayNumber == null) "$placeCount 个地点" else "第 $dayNumber 天 · $placeCount 个地点"
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().testTag("workspace-collapsed-summary"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WorkspaceSummaryIcon(state.section, Modifier.height(18.dp))
        Text(
            summary,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "上滑展开",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun WorkspaceSummaryIcon(section: WorkspaceSection, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(18.dp)) {
        val stroke = Stroke(width = size.minDimension / 10f, cap = StrokeCap.Round)
        if (section == WorkspaceSection.PLACE_POOL) {
            val path = Path().apply {
                moveTo(size.width * .25f, size.height * .12f)
                lineTo(size.width * .75f, size.height * .12f)
                lineTo(size.width * .75f, size.height * .88f)
                lineTo(size.width * .5f, size.height * .68f)
                lineTo(size.width * .25f, size.height * .88f)
                close()
            }
            drawPath(path, color, style = stroke)
        } else {
            drawLine(color, Offset(size.width * .2f, size.height * .25f), Offset(size.width * .5f, size.height * .5f), stroke.width, StrokeCap.Round)
            drawLine(color, Offset(size.width * .5f, size.height * .5f), Offset(size.width * .8f, size.height * .25f), stroke.width, StrokeCap.Round)
            listOf(.2f, .5f, .8f).forEachIndexed { index, x ->
                drawCircle(color, size.minDimension * .08f, Offset(size.width * x, if (index == 1) size.height * .5f else size.height * .25f))
            }
        }
    }
}

@Composable
internal fun WorkspaceSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(12.dp).testTag("workspace-sheet-handle-control"),
        contentAlignment = Alignment.Center,
    ) {
        Surface(Modifier.width(36.dp).height(4.dp), shape = RoundedCornerShape(2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f)) {}
    }
}

internal fun MapLayer.label() = when (this) {
    MapLayer.STANDARD -> "标准地图"
    MapLayer.SATELLITE -> "卫星地图"
    MapLayer.SATELLITE_ROAD -> "卫星路网"
}
