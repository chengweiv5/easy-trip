package com.yangchengwei.easytrip.place.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceTagValidationTest {
    @Test fun normalizationTrimsWithoutChangingIdentity() {
        assertEquals("ＣＡＦＥ", normalizePlaceTagName("  ＣＡＦＥ  "))
    }

    @Test fun emptyTagIsRejected() {
        assertEquals(PlaceTagValidation.Empty, validatePlaceTag("   ", emptySet()))
    }

    @Test fun duplicateTrimmedTagIsRejected() {
        assertEquals(PlaceTagValidation.Duplicate, validatePlaceTag(" 景点 ", setOf("景点")))
    }

    @Test fun twelveCjkCharactersFitButThirteenDoNot() {
        assertEquals(24, placeTagUnits("一二三四五六七八九十天地"))
        assertEquals(PlaceTagValidation.Valid("一二三四五六七八九十天地"), validatePlaceTag("一二三四五六七八九十天地", emptySet()))
        assertEquals(PlaceTagValidation.TooLong, validatePlaceTag("一二三四五六七八九十天地人", emptySet()))
    }

    @Test fun twentyFourLatinCharactersFitButTwentyFiveDoNot() {
        assertEquals(PlaceTagValidation.Valid("abcdefghijklmnopqrstuvwx"), validatePlaceTag("abcdefghijklmnopqrstuvwx", emptySet()))
        assertEquals(PlaceTagValidation.TooLong, validatePlaceTag("abcdefghijklmnopqrstuvwxy", emptySet()))
    }

    @Test fun emojiCountsByCodePointRatherThanUtf16Units() {
        assertEquals(1, placeTagUnits("😀"))
    }

    @Test fun eighthTagCanBeAddedButNinthCannot() {
        val seven = (1..7).mapTo(mutableSetOf()) { "tag-$it" }
        assertEquals(PlaceTagValidation.Valid("eighth"), validatePlaceTag("eighth", seven))
        assertEquals(PlaceTagValidation.LimitReached, validatePlaceTag("ninth", seven + "eighth"))
    }

    @Test fun removingAtLimitDoesNotRequireValidation() {
        val eight = (1..8).mapTo(mutableSetOf()) { "tag-$it" }
        assertEquals(sevenTags(), eight - "tag-8")
    }

    private fun sevenTags() = (1..7).mapTo(mutableSetOf()) { "tag-$it" }
}
