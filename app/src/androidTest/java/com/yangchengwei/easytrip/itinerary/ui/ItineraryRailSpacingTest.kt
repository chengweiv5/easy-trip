package com.yangchengwei.easytrip.itinerary.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.yangchengwei.easytrip.core.ui.theme.EasyTripTheme
import com.yangchengwei.easytrip.trip.domain.TripDay
import com.yangchengwei.easytrip.workspace.ItineraryScope
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ItineraryRailSpacingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun dateRailKeepsOnlyTwelveDpContentGapWithoutAdditionalDividerWidth() {
        compose.setContent { EasyTripTheme {
            WorkspaceItineraryContent(
                days = (1..3).map { TripDay("day-$it", it - 1) },
                selected = ItineraryScope.Day("day-2"),
                wholeTripDays = emptyList(),
                onSelect = {},
                startDate = LocalDate.of(2026, 10, 4),
                dayContent = { Box(Modifier.fillMaxSize().testTag("spacing-day-content")) { Text("当天行程") } },
                modifier = Modifier.width(390.dp).height(320.dp),
            )
        } }
        val rail = compose.onNodeWithTag("itinerary-scope-rail").getUnclippedBoundsInRoot()
        val content = compose.onNodeWithTag("spacing-day-content").getUnclippedBoundsInRoot()
        assertEquals(12.dp, content.left - rail.right)
        compose.onNodeWithTag("itinerary-scope-day-2").assertIsSelected()
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        java.io.File(compose.activity.getExternalFilesDir(null), "itinerary-without-divider.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
