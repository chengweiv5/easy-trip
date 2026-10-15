package com.yangchengwei.easytrip.assistant

import org.junit.Assert.*
import org.junit.Test

class GuideEntityValidationTest {
    @Test fun locatesExactNonOverlappingOccurrenceAfterEmojiWithoutNormalizingOriginalText() {
        assertEquals(5..6, GuideEntityValidation.locate("🌿北门\n北门（周一关闭）", "北门", 1))
        assertNull(GuideEntityValidation.locate("北门", "南门", 0))
        assertNull(GuideEntityValidation.locate("北门", "北门", 1))
        assertNull(GuideEntityValidation.locate("北門", "北门", 0))
        assertNull(GuideEntityValidation.locate("北门", "", 0))
        assertNull(GuideEntityValidation.locate("北门", "北门", -1))
    }
}
