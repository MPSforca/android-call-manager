package com.matheus.callmanager.core.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import com.matheus.callmanager.testing.ComposeHostActivityRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatusPillTest {
    private val composeTestRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(ComposeHostActivityRule()).around(composeTestRule)

    @Test
    fun `renders the given label`() {
        composeTestRule.setContent {
            CallManagerTheme {
                StatusPill(text = "Blocked", tone = StatusTone.BLOCKED)
            }
        }

        composeTestRule.onNodeWithText("Blocked").assertExists()
    }
}
