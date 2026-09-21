package com.yangchengwei.easytrip.itinerary.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.itinerary.ui.ItineraryItemUi
import com.yangchengwei.easytrip.itinerary.ui.wholeTripDayDate
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarDetail(day: CalendarDay, item: ItineraryItemUi, startDate: LocalDate?, onDismiss: () -> Unit, onEdit: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).testTag("calendar-detail"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("日程详情", style = MaterialTheme.typography.titleLarge)
            Text("第 ${day.number} 天 · ${wholeTripDayDate(day.number, startDate) ?: "日期待定"} · 第 ${day.sourceItems.indexOfFirst { it.id == item.id } + 1} 站", style = MaterialTheme.typography.bodySmall)
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            if (item.address.isNotBlank()) Text(item.address, style = MaterialTheme.typography.bodySmall)
            Text("到达：${item.arrivalTime ?: "待设置"}")
            Text("结束：${calendarEndLabel(item) ?: "待设置"}")
            Text("停留：${item.stayMinutes?.let { "$it 分钟" } ?: "待设置"}")
            item.note?.takeIf { it.isNotBlank() }?.let { Text(it) }
            if (day.events.any { it.item.id == item.id && it.conflicts.isNotEmpty() }) Text("与其他日程时间重叠", color = MaterialTheme.colorScheme.error)
            if (day.events.any { it.item.id == item.id && it.trafficConflict }) Text("上一段交通可能来不及", color = MaterialTheme.colorScheme.error)
            day.transfers.filter { it.fromId == item.id }.forEach { transfer ->
                Text("下一站赶路：${transfer.minutes?.let { "约 $it 分钟" } ?: "耗时待获取"}${if (transfer.start == null) " · 未设停留，暂不定位" else ""}${if(transfer.conflict) " · 时间不足" else ""}", style = MaterialTheme.typography.bodySmall)
            }
            Button(onEdit, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("calendar-detail-edit")) { Text("编辑时间和备注") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
