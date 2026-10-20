package com.sforca.callmanager.core.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sforca.callmanager.core.designsystem.theme.CallManagerTheme
import com.sforca.callmanager.testing.ComposeHostActivityRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallRowTest {
    private val composeTestRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(ComposeHostActivityRule()).around(composeTestRule)

    @Test
    fun `renders name, number, time and outcome`() {
        composeTestRule.setContent {
            CallManagerTheme {
                CallRow(
                    displayName = "Ada Lovelace",
                    number = "+1 555 0100",
                    timeLabel = "10:42",
                    outcomeLabel = "Blocked",
                    outcomeTone = StatusTone.BLOCKED,
                )
            }
        }

        composeTestRule.onNodeWithText("Ada Lovelace").assertExists()
        composeTestRule.onNodeWithText("+1 555 0100").assertExists()
        composeTestRule.onNodeWithText("10:42").assertExists()
        composeTestRule.onNodeWithText("Blocked").assertExists()
    }
}
