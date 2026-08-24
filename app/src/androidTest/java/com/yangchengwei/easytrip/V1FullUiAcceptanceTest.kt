package com.yangchengwei.easytrip

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class V1FullUiAcceptanceTest(private val scenario: V1Scenario) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun executesDeclaredStateAndBehavior() {
        scenario.executable.setup()
        scenario.executable.render(compose)
        scenario.executable.actions(compose)
        scenario.executable.assertions(compose)
    }
    override fun toString(): String = "${scenario.number.toString().padStart(2, '0')} ${scenario.name}"

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun scenarios(): List<Array<V1Scenario>> = V1ScenarioFixtures.scenarios.map { arrayOf(it) }
    }
}
