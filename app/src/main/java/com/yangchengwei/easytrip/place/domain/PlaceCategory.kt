package com.yangchengwei.easytrip.place.domain

enum class PlaceCategory(val storageKey: String, val label: String) {
    ATTRACTION("attraction", "景点"),
    LODGING("lodging", "住宿"),
    FOOD("food", "餐饮"),
    TRANSPORT("transport", "交通"),
    OTHER("other", "其他");

    companion object {
        fun fromStorageKey(key: String?): PlaceCategory =
            entries.firstOrNull { it.storageKey == key } ?: OTHER
    }
}
