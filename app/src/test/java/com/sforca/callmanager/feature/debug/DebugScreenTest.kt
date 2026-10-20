package com.sforca.callmanager.feature.debug

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.sforca.callmanager.core.data.rule.ScreeningRuleId
import com.sforca.callmanager.core.data.rule.ScreeningTrace
import com.sforca.callmanager.core.data.rule.TraceOutcome
import com.sforca.callmanager.core.data.rule.TraceStep
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.core.model.ScreeningDecision
import com.sforca.callmanager.testing.ComposeHostActivityRule
import com.sforca.callmanager.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DebugScreenTest {
    private val compose = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(MainDispatcherRule()).around(ComposeHostActivityRule()).around(compose)

    private val blockedTrace =
        ScreeningTrace(
            ScreeningDecision.Reject("On your block list"),
            ScreeningRuleId.entries.mapIndexed { i, id ->
                TraceStep(id, if (i == 0) TraceOutcome.DECIDED else TraceOutcome.NOT_REACHED)
            },
        )

    private fun show(
        state: DebugUiState = DebugUiState(),
        onSimulate: (String, String?) -> Unit = { _, _ -> },
        onClear: () -> Unit = {},
    ) {
        compose.setContent { CallManagerTheme { DebugContent(state, onSimulate, onClear) } }
    }

    @Test
    fun `simulate is disabled until a number is typed and then reports the input`() {
        var simulated: Pair<String, String?>? = null
        show(onSimulate = { n, name -> simulated = n to name })

        compose.onNodeWithText("Test number").assertIsNotEnabled()
        compose.onNodeWithText("Phone number").performTextInput("5550100")
        compose.onNodeWithText("Caller ID name (optional)").performTextInput("Acme")
        compose.onNodeWithText("Test number").assertIsEnabled().performClick()

        assertTrue(simulated!!.first.isNotBlank())
        assertEquals("Acme", simulated!!.second)
    }

    @Test
    fun `trace lists every rule with its outcome`() {
        show(DebugUiState(trace = blockedTrace))

        compose.onNodeWithText("On your block list").assertIsDisplayed()
        compose.onNodeWithText("Block list").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Decided").assertIsDisplayed()
        compose.onNodeWithText("Unknown numbers").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `selecting a history entry refills the form and clear fires`() {
        var cleared = false
        val entry = SimulationEntry("+15550100", "Acme", blockedTrace, 0L)
        show(DebugUiState(trace = null, history = listOf(entry)), onClear = { cleared = true })

        compose.onNodeWithText("+15550100").performScrollTo().performClick()
        compose.onNodeWithText("Test number").assertIsEnabled()
        compose.onNodeWithText("Clear").performScrollTo().performClick()

        assertTrue(cleared)
    }

    @Test
    fun `shows the empty state until a trace exists`() {
        show()

        compose.onNodeWithText("No test yet").assertIsDisplayed()
    }
}
