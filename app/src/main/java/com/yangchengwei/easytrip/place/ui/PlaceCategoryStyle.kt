package com.yangchengwei.easytrip.place.ui

import androidx.annotation.DrawableRes
import com.yangchengwei.easytrip.R
import com.yangchengwei.easytrip.place.domain.PlaceCategory

data class PlaceCategoryStyle(@param:DrawableRes val iconRes: Int, val foregroundArgb: Long, val backgroundArgb: Long)

fun placeCategoryStyle(category: PlaceCategory): PlaceCategoryStyle = when (category) {
    PlaceCategory.ATTRACTION -> PlaceCategoryStyle(R.drawable.ic_place_attraction, 0xFF25744B, 0xFFEDF7F0)
    PlaceCategory.LODGING -> PlaceCategoryStyle(R.drawable.ic_place_lodging, 0xFF7652A3, 0xFFF3EFF9)
    PlaceCategory.FOOD -> PlaceCategoryStyle(R.drawable.ic_place_food, 0xFFA85322, 0xFFFFF3E8)
    PlaceCategory.TRANSPORT -> PlaceCategoryStyle(R.drawable.ic_place_transport, 0xFF3269A7, 0xFFEEF4FC)
    PlaceCategory.OTHER -> PlaceCategoryStyle(R.drawable.ic_place_other, 0xFF596A70, 0xFFF0F3F4)
}
