package com.yangchengwei.easytrip.itinerary.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

fun ComposeTestRule.selectArrivalTime(hour: Int, minute: Int) {
    require(minute == 0 || minute == 30)
    require(hour in 0..23)
    onNodeWithTag(if (hour < 12) "arrival-period-am" else "arrival-period-pm").performClick()
    onNodeWithTag("arrival-hour-picker").performSemanticsAction(SemanticsActions.SetProgress) { it((hour % 12 + 1).toFloat()) }
    onNodeWithTag("arrival-minute-picker").performSemanticsAction(SemanticsActions.SetProgress) { it((minute / 30).toFloat()) }
}

fun ComposeTestRule.selectStayHours(hours: Int) {
    val node = onNodeWithTag("stay-hours-picker")
    // Navigate through the public accessibility action, including any legacy fractional entry.
    node.performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
    repeat(30) {
        if (node.fetchSemanticsNode().config[SemanticsProperties.StateDescription] == hours.toString()) return
        val action = node.fetchSemanticsNode().config[SemanticsActions.CustomActions].first()
        runOnIdle { action.action() }
    }
    error("Stay hour $hours was not selectable")
}

fun ComposeTestRule.assertArrivalTime(hour: Int, minute: Int) {
    onNodeWithTag("arrival-hour-picker").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, hour.toString().padStart(2, '0')))
    onNodeWithTag("arrival-minute-picker").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, minute.toString().padStart(2, '0')))
}

fun ComposeTestRule.assertStayHours(hours: String) {
    onNodeWithTag("stay-hours-picker").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, hours))
}
