package com.yangchengwei.easytrip.place.ui

internal sealed interface PlaceTagValidation {
    data class Valid(val name: String) : PlaceTagValidation
    data object Empty : PlaceTagValidation
    data object Duplicate : PlaceTagValidation
    data object TooLong : PlaceTagValidation
    data object LimitReached : PlaceTagValidation
}

internal fun normalizePlaceTagName(raw: String): String = raw.trim()

internal fun placeTagUnits(value: String): Int {
    var units = 0
    var offset = 0
    while (offset < value.length) {
        val codePoint = value.codePointAt(offset)
        val script = Character.UnicodeScript.of(codePoint)
        units += if (script in cjkScripts) 2 else 1
        offset += Character.charCount(codePoint)
    }
    return units
}

internal fun validatePlaceTag(raw: String, selectedTagNames: Set<String>): PlaceTagValidation {
    val name = normalizePlaceTagName(raw)
    return when {
        name.isEmpty() -> PlaceTagValidation.Empty
        name in selectedTagNames -> PlaceTagValidation.Duplicate
        placeTagUnits(name) > 24 -> PlaceTagValidation.TooLong
        selectedTagNames.size >= 8 -> PlaceTagValidation.LimitReached
        else -> PlaceTagValidation.Valid(name)
    }
}

private val cjkScripts = setOf(
    Character.UnicodeScript.HAN,
    Character.UnicodeScript.HIRAGANA,
    Character.UnicodeScript.KATAKANA,
    Character.UnicodeScript.HANGUL,
)
