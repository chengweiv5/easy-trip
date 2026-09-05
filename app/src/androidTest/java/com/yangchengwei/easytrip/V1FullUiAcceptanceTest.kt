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
        val executable = scenario.createExecutable()
        check(executable.declaredIdentity == scenario.declaredIdentity)
        check(executable.fixture.id == scenario.declaredIdentity.fixtureId)
        check(executable.fixture.screen == scenario.declaredIdentity.screen)
        check(executable.factoryIdentity == scenario.declaredIdentity.factoryIdentity)
        executable.setup()
        executable.render(compose)
        compose.waitForIdle()
        executable.actions(compose)
        compose.waitForIdle()
        executable.assertions(compose)
    }
    override fun toString(): String = "${scenario.number.toString().padStart(2, '0')} ${scenario.name}"

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun scenarios(): List<Array<V1Scenario>> = V1ScenarioFixtures.scenarios.map { arrayOf(it) }
    }
}
