package com.yangchengwei.easytrip.place.ui

import com.yangchengwei.easytrip.core.ui.formatCount
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.EasyTripPrimaryButton
import com.yangchengwei.easytrip.core.ui.component.EasyTripSecondaryButton
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.place.amap.PlaceCandidate
import com.yangchengwei.easytrip.place.domain.SavedPlace
import com.yangchengwei.easytrip.workspace.PlaceScheduleSummaryUi


enum class PlaceDetailSource { Search, PlacePool }

sealed interface PlaceDetailPanelAction {
    data object Dismiss : PlaceDetailPanelAction
    data object ToggleCollection : PlaceDetailPanelAction
    data object StartEdit : PlaceDetailPanelAction
    data object Delete : PlaceDetailPanelAction
    data class CategoryChanged(val value: com.yangchengwei.easytrip.place.domain.PlaceCategory) : PlaceDetailPanelAction
    data object ConfirmDiscardEdit : PlaceDetailPanelAction
    data object ContinueEditing : PlaceDetailPanelAction
    data class NoteChanged(val value: String) : PlaceDetailPanelAction
    data class NewTagInputChanged(val value: String) : PlaceDetailPanelAction
    data object AddTag : PlaceDetailPanelAction
    data class AddPresetTag(val name: String) : PlaceDetailPanelAction
    data class RemoveTag(val name: String) : PlaceDetailPanelAction
    data object SaveEdit : PlaceDetailPanelAction
    data object CancelEdit : PlaceDetailPanelAction
    data object StartAddToItinerary : PlaceDetailPanelAction
}

@Composable
fun PlaceDetailPanel(
    candidate: PlaceCandidate,
    savedPlace: SavedPlace?,
    editState: PlaceDetailEditState?,
    source: PlaceDetailSource,
    collectionBusy: Boolean,
    collectionError: String?,
    onAction: (PlaceDetailPanelAction) -> Unit,
    modifier: Modifier = Modifier,
    availableTagNames: List<String> = emptyList(),
    schedule: PlaceScheduleSummaryUi = PlaceScheduleSummaryUi(isKnown = false),
    canStartAddToItinerary: Boolean = true,
) {
    if (editState != null) {
        PlaceDetailEditor(candidate, editState, availableTagNames, onAction, modifier)
        return
    }
    val canShowAddToItinerary = source == PlaceDetailSource.PlacePool &&
        canStartAddToItinerary && schedule.isKnown && schedule.totalOccurrences == 0
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(EasyTripTheme.spacing.medium)
            .testTag("place-detail-scroll-content"),
        verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.small),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                candidate.name,
                modifier = Modifier.testTag("place-detail-title").semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(
                onClick = { onAction(PlaceDetailPanelAction.Dismiss) },
                modifier = Modifier.testTag("place-detail-dismiss"),
            ) {
                Text("关闭", modifier = Modifier.semantics { role = Role.Button })
            }
        }
        Text(
            candidate.address.ifBlank { "地址暂不可用" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (source == PlaceDetailSource.PlacePool) {
            PlaceDetailSchedule(schedule)
        }
        savedPlace?.let { PlaceCategoryBadge(it.category) }
        Text(savedPlace?.note?.takeIf(String::isNotBlank) ?: "暂无备注")
        Text(savedPlace?.tags?.map { it.name }?.takeIf(List<String>::isNotEmpty)?.joinToString("、") ?: "暂无标签")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (source) {
                PlaceDetailSource.Search -> {
                    EasyTripPrimaryButton(
                        onClick = { onAction(PlaceDetailPanelAction.ToggleCollection) },
                        enabled = !collectionBusy,
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = if (savedPlace == null) {
                                "收藏${candidate.name}"
                            } else {
                                "取消收藏${candidate.name}"
                            }
                        },
                    ) { Text(if (savedPlace == null) "收藏" else "取消收藏") }
                    if (savedPlace != null) {
                        EasyTripSecondaryButton(
                            onClick = { onAction(PlaceDetailPanelAction.StartEdit) },
                            modifier = Modifier.weight(1f),
                        ) { Text("编辑") }
                    }
                }
                PlaceDetailSource.PlacePool -> {
                    if (canShowAddToItinerary) {
                        EasyTripPrimaryButton(
                            onClick = { onAction(PlaceDetailPanelAction.StartAddToItinerary) },
                            modifier = Modifier.weight(1f).testTag("place-detail-start-add"),
                        ) { Text("加入行程") }
                    }
                    EasyTripSecondaryButton(
                        onClick = { onAction(PlaceDetailPanelAction.StartEdit) },
                        modifier = Modifier.weight(1f),
                    ) { Text("编辑") }
                    EasyTripSecondaryButton(
                        onClick = { onAction(PlaceDetailPanelAction.Delete) },
                        modifier = Modifier.weight(1f),
                    ) { Text("删除") }
                }
            }
        }
        collectionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun PlaceDetailSchedule(schedule: PlaceScheduleSummaryUi) {
    when {
        !schedule.isKnown -> Text(
            "行程安排暂不可用",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("place-detail-schedule-unknown"),
        )
        schedule.totalOccurrences > 0 -> Column(
            modifier = Modifier.testTag("place-detail-schedule"),
            verticalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.xSmall),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(EasyTripTheme.spacing.small)) {
                PlaceDetailBookmark(filled = true)
                Text("已加入行程", style = MaterialTheme.typography.titleSmall)
            }
            schedule.days.forEach { day ->
                Text("第 ${day.dayIndex + 1} 天 · ${formatCount(day.occurrences)} 次")
            }
        }
        else -> Box(modifier = Modifier.testTag("place-detail-bookmark")) {
            PlaceDetailBookmark(filled = false)
        }
    }
}

@Composable
private fun PlaceDetailBookmark(filled: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = Modifier
            .size(32.dp)
            .testTag(if (filled) "place-detail-bookmark-filled" else "place-detail-bookmark-outline")
            .background(if (filled) primary else surface, CircleShape)
            .border(1.dp, primary, CircleShape),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val path = Path().apply {
                moveTo(size.width * .25f, size.height * .12f)
                lineTo(size.width * .75f, size.height * .12f)
                lineTo(size.width * .75f, size.height * .88f)
                lineTo(size.width * .5f, size.height * .7f)
                lineTo(size.width * .25f, size.height * .88f)
                close()
            }
            drawPath(
                path,
                if (filled) onPrimary else primary,
                style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(1.8.dp.toPx()),
            )
        }
    }
}

internal fun SavedPlace.toCandidate() = PlaceCandidate(amapPoiId, name, address, point, cityCode, cityName, cityAdCode, cityMetadataVersion)
