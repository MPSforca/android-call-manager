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
class StatusPillTest {
    @get:Rule
    val composeTestRule = createComposeRule()

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
