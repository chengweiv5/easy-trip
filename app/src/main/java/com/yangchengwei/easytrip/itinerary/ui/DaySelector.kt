package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.core.ui.component.SelectablePill
import com.yangchengwei.easytrip.trip.domain.TripDay

@Composable fun DaySelector(days:List<TripDay>,selected:String?,onSelect:(String)->Unit){
    Row(Modifier.horizontalScroll(rememberScrollState())){days.forEach{day->SelectablePill(day.id==selected,{onSelect(day.id)},{Text("Day ${day.index+1}")})}}
}
