package com.yangchengwei.easytrip.place.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yangchengwei.easytrip.place.domain.PlaceCategory

@Composable
fun PlaceCategoryBadge(category: PlaceCategory, modifier: Modifier = Modifier) {
    val style = placeCategoryStyle(category)
    Surface(modifier, color = Color(style.backgroundArgb), contentColor = Color(style.foregroundArgb),
        shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(painterResource(style.iconRes), null, Modifier.size(16.dp))
            Text(category.label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
