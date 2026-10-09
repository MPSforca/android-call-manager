package com.matheus.callmanager.core.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.matheus.callmanager.core.designsystem.theme.CallManagerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CallRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

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
